package com.xirc.nichirin.common.attack;

import com.xirc.nichirin.client.gui.CooldownHUD;
import com.xirc.nichirin.common.attack.component.AbstractBreathingAttack;
import com.xirc.nichirin.registry.NichirinMoveRegistry;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic attack executor - knows nothing about specific breathing styles
 */
public class MoveExecutor {

    // Store active attacks - using thread-safe collections
    private static final ConcurrentHashMap<Player, List<Object>> activeAttacks = new ConcurrentHashMap<>();

    // Packet ID for cooldown display
    private static final ResourceLocation COOLDOWN_PACKET_ID = new ResourceLocation("nichirin", "cooldown_display");

    /**
     * Execute any attack with metadata lookup
     */
    public static void executeAttack(Player player, Object attack, String movesetId, String moveId) {
        // Get move info from registry for the display name
        NichirinMoveRegistry.MoveInfo moveInfo = NichirinMoveRegistry.getMove(movesetId, moveId);
        String displayName = moveInfo != null ? moveInfo.displayName : attack.getClass().getSimpleName();

        // Get cooldown from the attack object
        int cooldown = getCooldownForAttack(attack);

        // Execute with proper display name
        executeAttackInternal(player, attack, displayName, cooldown);
    }

    /**
     * Execute an attack with explicit name and cooldown
     */
    public static void executeAttackWithInfo(Player player, Object attack, String displayName, int cooldown) {
        executeAttackInternal(player, attack, displayName, cooldown);
    }

    /**
     * Internal execution method
     */
    private static void executeAttackInternal(Player player, Object attack, String displayName, int cooldown) {
        if (!isAttackActive(attack)) {
            startAttack(player, attack);
            trackAttack(player, attack);

            // Send cooldown to client if on server
            if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer && cooldown > 0) {
                sendCooldownToClient(serverPlayer, displayName, cooldown);
            }
        }
    }

    /**
     * Generic method to check if attack is active
     */
    private static boolean isAttackActive(Object attack) {
        try {
            var isActiveMethod = attack.getClass().getMethod("isActive");
            return (boolean) isActiveMethod.invoke(attack);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Generic method to start an attack
     */
    private static void startAttack(Player player, Object attack) {
        try {
            // Try different start method signatures
            try {
                var startMethod = attack.getClass().getMethod("start", Player.class, Level.class);
                startMethod.invoke(attack, player, player.level());
            } catch (NoSuchMethodException e1) {
                var startMethod = attack.getClass().getMethod("start", Player.class);
                startMethod.invoke(attack, player);
            }
        } catch (Exception e) {
            System.err.println("Could not start attack: " + attack.getClass().getName());
        }
    }

    /**
     * Generic method to get cooldown from attack
     */
    private static int getCooldownForAttack(Object attack) {
        try {
            var getCooldownMethod = attack.getClass().getMethod("getCooldown");
            return (int) getCooldownMethod.invoke(attack);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Execute a move by name with cooldown
     */
    public static void executeMove(Player player, String moveName, Runnable moveExecution, int cooldownTicks) {
        // Execute the move
        moveExecution.run();

        // Send cooldown to client if on server
        if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer && cooldownTicks > 0) {
            sendCooldownToClient(serverPlayer, moveName, cooldownTicks);
        }
    }

    /**
     * Send cooldown display info to client
     */
    private static void sendCooldownToClient(ServerPlayer player, String moveName, int cooldownTicks) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf(moveName);
        buf.writeInt(cooldownTicks);

        NetworkManager.sendToPlayer(player, COOLDOWN_PACKET_ID, buf);
    }

    /**
     * Register the client-side packet handler (call this in client init)
     */
    public static void registerClientHandler() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, COOLDOWN_PACKET_ID, (buf, context) -> {
            String moveName = buf.readUtf();
            int cooldownTicks = buf.readInt();

            context.queue(() -> {
                // Display the cooldown on client
                CooldownHUD.setCooldown(moveName, cooldownTicks);
            });
        });
    }

    /**
     * Tick all active attacks for a player
     * FIXED: Create a copy for safe iteration
     */
    public static void tickAttacks(Player player) {
        var attacks = activeAttacks.get(player);
        if (attacks != null) {
            // Create a copy to avoid concurrent modification
            List<Object> attacksCopy = new ArrayList<>(attacks);
            List<Object> toRemove = new ArrayList<>();

            for (Object attack : attacksCopy) {
                try {
                    boolean shouldRemove = !tickAndCheckActive(player, attack);

                    if (shouldRemove) {
                        toRemove.add(attack);
                    }
                } catch (Exception e) {
                    // Remove if we can't tick it
                    System.err.println("Error ticking attack: " + e.getMessage());
                    toRemove.add(attack);
                }
            }

            // Remove all inactive attacks
            attacks.removeAll(toRemove);

            // Clean up empty lists
            if (attacks.isEmpty()) {
                activeAttacks.remove(player);
            }
        }
    }

    /**
     * Tick an attack and return whether it's still active
     */
    private static boolean tickAndCheckActive(Player player, Object attack) throws Exception {
        // Try to tick the attack
        if (attack instanceof AbstractBreathingAttack<?, ?> breathing) {
            breathing.tick(player);
            return breathing.isActive();
        } else {
            // Try reflection for other types
            try {
                var tickMethod = attack.getClass().getMethod("tick");
                tickMethod.invoke(attack);
            } catch (NoSuchMethodException e) {
                // Try with player parameter
                var tickMethod = attack.getClass().getMethod("tick", Player.class);
                tickMethod.invoke(attack, player);
            }

            var isActiveMethod = attack.getClass().getMethod("isActive");
            return (boolean) isActiveMethod.invoke(attack);
        }
    }

    /**
     * Track an attack for a player
     */
    private static void trackAttack(Player player, Object attack) {
        activeAttacks.computeIfAbsent(player, k -> new ArrayList<>()).add(attack);
    }

    /**
     * Clear all attacks for a player (on death, disconnect, etc.)
     */
    public static void clearAttacks(Player player) {
        activeAttacks.remove(player);
    }

    /**
     * Check if a player has any active attacks
     */
    public static boolean hasActiveAttacks(Player player) {
        var attacks = activeAttacks.get(player);
        return attacks != null && !attacks.isEmpty();
    }

    /**
     * Get the number of active attacks for a player
     */
    public static int getActiveAttackCount(Player player) {
        var attacks = activeAttacks.get(player);
        return attacks != null ? attacks.size() : 0;
    }
}