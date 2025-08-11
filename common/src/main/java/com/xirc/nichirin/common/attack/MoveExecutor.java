package com.xirc.nichirin.common.attack;

import com.xirc.nichirin.client.gui.CooldownHUD;
import com.xirc.nichirin.common.attack.component.AbstractBreathingAttack;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.registry.NichirinMoveRegistry;
import com.xirc.nichirin.registry.MovesetRegistry;
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
 * Generic attack executor - handles all types of breathing attacks with automatic configuration
 */
public class MoveExecutor {

    // Store active attacks - using thread-safe collections
    private static final ConcurrentHashMap<Player, List<Object>> activeAttacks = new ConcurrentHashMap<>();

    // Packet ID for cooldown display
    private static final ResourceLocation COOLDOWN_PACKET_ID = new ResourceLocation("nichirin", "cooldown_display");

    /**
     * Execute any breathing attack with metadata lookup and automatic configuration
     */
    public static void executeAttack(Player player, Object attack, String movesetId, String moveId) {
        // Handle all breathing attacks through the unified interface
        if (attack instanceof AbstractBreathingAttack<?, ?> breathingAttack) {
            // Get the moveset for configuration
            AbstractMoveset moveset = MovesetRegistry.getMoveset(movesetId);
            if (moveset != null) {
                // Find the move configuration
                AbstractMoveset.MoveConfiguration config = findMoveConfig(moveset, moveId);
                if (config != null) {
                    breathingAttack.configure(config);
                }
            }
        }

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
     * Find the move configuration for a given moveId in any moveset
     */
    private static AbstractMoveset.MoveConfiguration findMoveConfig(AbstractMoveset moveset, String moveId) {
        for (int i = 0; i < moveset.getMoveCount(); i++) {
            var config = moveset.getMove(i);
            if (config != null && config.getMoveId().equals(moveId)) {
                return config;
            }
        }
        return null;
    }

    /**
     * Generic method to check if attack is active
     */
    private static boolean isAttackActive(Object attack) {
        // Handle AbstractBreathingAttack directly
        if (attack instanceof AbstractBreathingAttack<?, ?> breathingAttack) {
            return breathingAttack.isActive();
        }

        // Fallback to reflection for other types
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
        // Handle AbstractBreathingAttack directly
        if (attack instanceof AbstractBreathingAttack<?, ?> breathingAttack) {
            breathingAttack.start(player, player.level());
            return;
        }

        // Fallback to reflection for other types
        try {
            // Try different start method signatures
            try {
                var startMethod = attack.getClass().getMethod("start", Player.class, Level.class);
                startMethod.invoke(attack, player, player.level());
            } catch (NoSuchMethodException e1) {
                try {
                    var startMethod = attack.getClass().getMethod("start", Player.class);
                    startMethod.invoke(attack, player);
                } catch (NoSuchMethodException e2) {
                    // Try parameterless start
                    var startMethod = attack.getClass().getMethod("start");
                    startMethod.invoke(attack);
                }
            }
        } catch (Exception e) {
            System.err.println("Could not start attack: " + attack.getClass().getName() + " - " + e.getMessage());
        }
    }

    /**
     * Generic method to get cooldown from attack
     */
    private static int getCooldownForAttack(Object attack) {
        // Handle AbstractBreathingAttack directly
        if (attack instanceof AbstractBreathingAttack<?, ?> breathingAttack) {
            return breathingAttack.getCooldown();
        }

        // Fallback to reflection for other types
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
     */
    public static void tickAttacks(Player player) {
        var attacks = activeAttacks.get(player);
        if (attacks != null && !attacks.isEmpty()) {  // Add !attacks.isEmpty() check
            // Create a copy to avoid concurrent modification - with null safety
            List<Object> attacksCopy;
            List<Object> toRemove = new ArrayList<>();

            // Thread-safe copy creation
            synchronized (attacks) {
                if (attacks.isEmpty()) {
                    return; // Exit early if empty
                }
                attacksCopy = new ArrayList<>(attacks);
            }

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

            // Remove all inactive attacks - with synchronization
            if (!toRemove.isEmpty()) {
                synchronized (attacks) {
                    attacks.removeAll(toRemove);

                    // Clean up empty lists
                    if (attacks.isEmpty()) {
                        activeAttacks.remove(player);
                    }
                }
            }
        }
    }

    /**
     * Tick an attack and return whether it's still active
     */
    private static boolean tickAndCheckActive(Player player, Object attack) throws Exception {
        // Handle AbstractBreathingAttack directly
        if (attack instanceof AbstractBreathingAttack<?, ?> breathingAttack) {
            breathingAttack.tick();
            return breathingAttack.isActive();
        }

        // Generic reflection-based handling for other types
        try {
            var tickMethod = attack.getClass().getMethod("tick");
            tickMethod.invoke(attack);
        } catch (NoSuchMethodException e) {
            try {
                // Try with player parameter
                var tickMethod = attack.getClass().getMethod("tick", Player.class);
                tickMethod.invoke(attack, player);
            } catch (NoSuchMethodException e2) {
                // Try with no parameters but set user field if it exists
                try {
                    var userField = attack.getClass().getDeclaredField("user");
                    userField.setAccessible(true);
                    userField.set(attack, player);

                    var tickMethod = attack.getClass().getMethod("tick");
                    tickMethod.invoke(attack);
                } catch (Exception e3) {
                    throw new Exception("Could not tick attack: " + attack.getClass().getName());
                }
            }
        }

        var isActiveMethod = attack.getClass().getMethod("isActive");
        return (boolean) isActiveMethod.invoke(attack);
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
        var attacks = activeAttacks.remove(player);
        if (attacks != null) {
            // Stop all attacks gracefully
            for (Object attack : attacks) {
                try {
                    if (attack instanceof AbstractBreathingAttack<?, ?> breathingAttack) {
                        breathingAttack.stop();
                    } else {
                        // Try to call stop method via reflection
                        try {
                            var stopMethod = attack.getClass().getMethod("stop");
                            stopMethod.invoke(attack);
                        } catch (Exception e) {
                            // Ignore if no stop method
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error stopping attack on clear: " + e.getMessage());
                }
            }
        }
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

    /**
     * Get all active attacks for a player (defensive copy)
     */
    public static List<Object> getActiveAttacks(Player player) {
        var attacks = activeAttacks.get(player);
        return attacks != null ? new ArrayList<>(attacks) : new ArrayList<>();
    }

    /**
     * Force stop a specific attack for a player
     */
    public static boolean stopAttack(Player player, Object attack) {
        var attacks = activeAttacks.get(player);
        if (attacks != null && attacks.contains(attack)) {
            try {
                if (attack instanceof AbstractBreathingAttack<?, ?> breathingAttack) {
                    breathingAttack.stop();
                } else {
                    // Try to call stop method via reflection
                    var stopMethod = attack.getClass().getMethod("stop");
                    stopMethod.invoke(attack);
                }
                attacks.remove(attack);

                // Clean up empty lists
                if (attacks.isEmpty()) {
                    activeAttacks.remove(player);
                }
                return true;
            } catch (Exception e) {
                System.err.println("Error force stopping attack: " + e.getMessage());
                // Remove it anyway
                attacks.remove(attack);
                return false;
            }
        }
        return false;
    }

    /**
     * Stop all attacks of a specific type for a player
     */
    public static int stopAttacksOfType(Player player, Class<?> attackType) {
        var attacks = activeAttacks.get(player);
        if (attacks == null) {
            return 0;
        }

        List<Object> toStop = new ArrayList<>();
        for (Object attack : attacks) {
            if (attackType.isInstance(attack)) {
                toStop.add(attack);
            }
        }

        int stopped = 0;
        for (Object attack : toStop) {
            if (stopAttack(player, attack)) {
                stopped++;
            }
        }

        return stopped;
    }
}