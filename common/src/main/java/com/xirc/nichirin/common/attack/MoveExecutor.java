package com.xirc.nichirin.common.attack;

import com.xirc.nichirin.client.gui.CooldownHUD;
import com.xirc.nichirin.client.renderer.effects.AttackHitboxRenderer;
import com.xirc.nichirin.common.attack.component.AbstractBreathingAttack;
import com.xirc.nichirin.common.attack.component.AbstractDemonAttack;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.util.ComboTracker;
import com.xirc.nichirin.registry.NichirinMoveRegistry;
import com.xirc.nichirin.registry.MovesetRegistry;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified attack executor - handles both breathing and demon attacks with the same logic
 * Includes anti-spam detection, stun prevention, and hitbox debugging
 */
public class MoveExecutor {

    // Store active attacks - using thread-safe collections
    private static final ConcurrentHashMap<Player, List<Object>> activeAttacks = new ConcurrentHashMap<>();

    // Packet ID for cooldown display
    private static final ResourceLocation COOLDOWN_PACKET_ID = new ResourceLocation("nichirin", "cooldown_display");

    // Hitbox debugging state
    private static boolean hitboxDebuggingEnabled = false;

    /**
     * Unified execute method for both breathing and demon attacks
     */
    public static void executeAttack(Player player, Object attack, String movesetId, String moveId) {
        // Check if player is currently stunned (prevents move stacking)
        if (player.hasEffect(NichirinEffectRegistry.STUNNED.get())) {
            return;
        }

        // Handle both breathing and demon attacks through unified interface
        if (attack instanceof AbstractBreathingAttack<?, ?> breathingAttack) {
            handleAttack(player, breathingAttack, movesetId, moveId);
        } else if (attack instanceof AbstractDemonAttack<?, ?> demonAttack) {
            handleAttack(player, demonAttack, movesetId, moveId);
        } else {
            // Fallback for other attack types
            handleGenericAttack(player, attack, movesetId, moveId);
        }
    }

    /**
     * Unified handler for breathing attacks
     */
    private static void handleAttack(Player player, AbstractBreathingAttack<?, ?> attack, String movesetId, String moveId) {
        // Check if attack is already configured
        boolean alreadyConfigured = isAttackConfigured(attack);

        if (!alreadyConfigured) {
            configureAttackFromMoveset(player, attack, movesetId, moveId);
        } else {
            applyPreConfiguredEffects(player, attack, moveId);
        }

        executeConfiguredAttack(player, attack, movesetId, moveId);
    }

    /**
     * Unified handler for demon attacks
     */
    private static void handleAttack(Player player, AbstractDemonAttack<?, ?> attack, String movesetId, String moveId) {
        // Check if attack is already configured
        boolean alreadyConfigured = isDemonAttackConfigured(attack);

        if (!alreadyConfigured) {
            configureAttackFromMoveset(player, attack, movesetId, moveId);
        } else {
            applyPreConfiguredDemonEffects(player, attack, moveId);
        }

        executeConfiguredAttack(player, attack, movesetId, moveId);
    }

    /**
     * Configure attack using moveset configuration
     */
    private static void configureAttackFromMoveset(Player player, Object attack, String movesetId, String moveId) {
        AbstractMoveset moveset = MovesetRegistry.getMoveset(movesetId);
        if (moveset == null) return;

        AbstractMoveset.MoveConfiguration config = findMoveConfig(moveset, moveId);
        if (config == null) return;

        // Apply anti-spam detection to hitstun
        int originalHitStun = config.getHitStunOrDefault(0);
        int modifiedHitStun = ComboTracker.getModifiedHitStun(player, moveId, originalHitStun);

        if (modifiedHitStun != originalHitStun) {
            config = createModifiedConfig(config, modifiedHitStun);
        }

        // Apply stun effect to prevent move stacking
        applyMoveStun(player, config);

        // Configure the attack
        if (attack instanceof AbstractBreathingAttack<?, ?> breathingAttack) {
            breathingAttack.configure(config);
        } else if (attack instanceof AbstractDemonAttack<?, ?> demonAttack) {
            demonAttack.configure(config);
        }
    }

    /**
     * Apply effects for pre-configured breathing attacks
     */
    private static void applyPreConfiguredEffects(Player player, AbstractBreathingAttack<?, ?> attack, String moveId) {
        // Apply anti-spam detection
        int originalHitStun = attack.getHitStun();
        ComboTracker.getModifiedHitStun(player, moveId, originalHitStun);

        // Apply stun effect
        applyPreConfiguredMoveStun(player, attack);
    }

    /**
     * Apply effects for pre-configured demon attacks
     */
    private static void applyPreConfiguredDemonEffects(Player player, AbstractDemonAttack<?, ?> attack, String moveId) {
        // Apply anti-spam detection
        int originalHitStun = getHitStunFromAttack(attack);
        ComboTracker.getModifiedHitStun(player, moveId, originalHitStun);

        // Apply stun effect
        applyPreConfiguredDemonMoveStun(player, attack);
    }

    /**
     * Execute the configured attack
     */
    private static void executeConfiguredAttack(Player player, Object attack, String movesetId, String moveId) {
        // Get display name from registry
        NichirinMoveRegistry.MoveInfo moveInfo = NichirinMoveRegistry.getMove(movesetId, moveId);
        String displayName = moveInfo != null ? moveInfo.displayName : attack.getClass().getSimpleName();

        // Get cooldown from attack
        int cooldown = getCooldownForAttack(attack);

        // Execute the attack
        executeAttackInternal(player, attack, displayName, cooldown);
    }

    /**
     * Fallback for generic attack types
     */
    private static void handleGenericAttack(Player player, Object attack, String movesetId, String moveId) {
        // Basic execution for unknown attack types
        String displayName = attack.getClass().getSimpleName();
        int cooldown = getCooldownForAttack(attack);
        executeAttackInternal(player, attack, displayName, cooldown);
    }

    /**
     * Execute attack with visual hitbox debugging
     */
    public static void executeAttackWithVisuals(Player player, Object attack, String movesetId, String moveId) {
        if (player.hasEffect(NichirinEffectRegistry.STUNNED.get())) {
            return;
        }

        // Clear existing hitboxes for clean visuals
        if (player.level().isClientSide) {
            AttackHitboxRenderer.clearAll();
        }

        executeAttack(player, attack, movesetId, moveId);
    }

    /**
     * Check if a breathing attack is configured
     */
    private static boolean isAttackConfigured(AbstractBreathingAttack<?, ?> attack) {
        try {
            return attack.getDuration() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if a demon attack is configured
     */
    private static boolean isDemonAttackConfigured(AbstractDemonAttack<?, ?> attack) {
        try {
            return attack.getDuration() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get hitstun from attack using reflection
     */
    private static int getHitStunFromAttack(Object attack) {
        try {
            var getHitStunMethod = attack.getClass().getMethod("getHitStun");
            return (int) getHitStunMethod.invoke(attack);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Apply stun effect during move execution to prevent stacking
     */
    private static void applyMoveStun(Player player, AbstractMoveset.MoveConfiguration config) {
        int windupTicks = config.getWindupOrDefault(0);
        int durationTicks = config.getDurationOrDefault(0);
        int totalStunTicks = windupTicks + durationTicks;

        if (totalStunTicks > 0) {
            MobEffectInstance stunEffect = new MobEffectInstance(
                    NichirinEffectRegistry.STUNNED.get(),
                    totalStunTicks,
                    0,
                    false,
                    false,
                    false
            );
            player.addEffect(stunEffect);
        }
    }

    /**
     * Apply stun effect for pre-configured breathing attacks
     */
    private static void applyPreConfiguredMoveStun(Player player, AbstractBreathingAttack<?, ?> attack) {
        int windupTicks = getWindupFromAttack(attack);
        if (windupTicks > 0) {
            MobEffectInstance stunEffect = new MobEffectInstance(
                    NichirinEffectRegistry.STUNNED.get(),
                    windupTicks,
                    0,
                    false,
                    false,
                    false
            );
            player.addEffect(stunEffect);
        }
    }

    /**
     * Apply stun effect for pre-configured demon attacks
     */
    private static void applyPreConfiguredDemonMoveStun(Player player, AbstractDemonAttack<?, ?> attack) {
        int windupTicks = getWindupFromAttack(attack);
        if (windupTicks > 0) {
            MobEffectInstance stunEffect = new MobEffectInstance(
                    NichirinEffectRegistry.STUNNED.get(),
                    windupTicks,
                    0,
                    false,
                    false,
                    false
            );
            player.addEffect(stunEffect);
        }
    }

    /**
     * Get windup from attack using reflection
     */
    private static int getWindupFromAttack(Object attack) {
        try {
            var getWindupMethod = attack.getClass().getMethod("getWindup");
            return (int) getWindupMethod.invoke(attack);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Create a modified configuration with different hitstun
     */
    private static AbstractMoveset.MoveConfiguration createModifiedConfig(AbstractMoveset.MoveConfiguration originalConfig, int newHitStun) {
        // TODO: Implement proper configuration modification
        // For now, return the original config
        return originalConfig;
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
     * Execute an attack with explicit name and cooldown
     */
    public static void executeAttackWithInfo(Player player, Object attack, String displayName, int cooldown) {
        if (player.hasEffect(NichirinEffectRegistry.STUNNED.get())) {
            return;
        }
        executeAttackInternal(player, attack, displayName, cooldown);
    }

    /**
     * Internal execution method
     */
    private static void executeAttackInternal(Player player, Object attack, String displayName, int cooldown) {
        if (!isAttackActive(attack)) {
            startAttack(player, attack);

            if (isAttackActive(attack)) {
                trackAttack(player, attack);

                // Send cooldown to client if on server
                if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer && cooldown > 0) {
                    sendCooldownToClient(serverPlayer, displayName, cooldown);
                }
            }
        }
    }

    /**
     * Generic method to check if attack is active
     */
    private static boolean isAttackActive(Object attack) {
        if (attack instanceof AbstractBreathingAttack<?, ?> breathingAttack) {
            return breathingAttack.isActive();
        }

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
        if (attack instanceof AbstractBreathingAttack<?, ?> breathingAttack) {
            breathingAttack.start(player, player.level());
            return;
        }

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
                    var startMethod = attack.getClass().getMethod("start");
                    startMethod.invoke(attack);
                }
            }
        } catch (Exception e) {
            // Silent failure for unknown attack types
        }
    }

    /**
     * Generic method to get cooldown from attack
     */
    private static int getCooldownForAttack(Object attack) {
        if (attack instanceof AbstractBreathingAttack<?, ?> breathingAttack) {
            return breathingAttack.getCooldown();
        }

        try {
            var getCooldownMethod = attack.getClass().getMethod("getCooldown");
            return (int) getCooldownMethod.invoke(attack);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Execute a move by name with cooldown and stun prevention
     */
    public static void executeMove(Player player, String moveName, Runnable moveExecution, int cooldownTicks, int stunDurationTicks) {
        if (player.hasEffect(NichirinEffectRegistry.STUNNED.get())) {
            return;
        }

        if (stunDurationTicks > 0) {
            MobEffectInstance stunEffect = new MobEffectInstance(
                    NichirinEffectRegistry.STUNNED.get(),
                    stunDurationTicks,
                    0,
                    false,
                    false,
                    false
            );
            player.addEffect(stunEffect);
        }

        ComboTracker.getModifiedHitStun(player, moveName, 0);
        moveExecution.run();

        if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer && cooldownTicks > 0) {
            sendCooldownToClient(serverPlayer, moveName, cooldownTicks);
        }
    }

    /**
     * Overloaded method for backward compatibility
     */
    public static void executeMove(Player player, String moveName, Runnable moveExecution, int cooldownTicks) {
        executeMove(player, moveName, moveExecution, cooldownTicks, 20);
    }

    /**
     * Check if a player can execute a move (not stunned)
     */
    public static boolean canExecuteMove(Player player) {
        return !player.hasEffect(NichirinEffectRegistry.STUNNED.get());
    }

    /**
     * Force remove move stun
     */
    public static void removeMoveStun(Player player) {
        player.removeEffect(NichirinEffectRegistry.STUNNED.get());
    }

    /**
     * Get remaining stun duration in ticks
     */
    public static int getRemainingStunTicks(Player player) {
        MobEffectInstance stunEffect = player.getEffect(NichirinEffectRegistry.STUNNED.get());
        return stunEffect != null ? stunEffect.getDuration() : 0;
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
     * Register the client-side packet handler
     */
    public static void registerClientHandler() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, COOLDOWN_PACKET_ID, (buf, context) -> {
            String moveName = buf.readUtf();
            int cooldownTicks = buf.readInt();
            context.queue(() -> CooldownHUD.setCooldown(moveName, cooldownTicks));
        });
    }

    /**
     * Render hitboxes
     */
    public static void renderHitboxes(PoseStack poseStack, MultiBufferSource bufferSource,
                                      LevelRenderer levelRenderer, Vec3 cameraPosition) {
        AttackHitboxRenderer.render(poseStack, cameraPosition, levelRenderer, bufferSource);
    }

    // === ATTACK TRACKING METHODS ===

    /**
     * Tick all active attacks for a player
     */
    public static void tickAttacks(Player player) {
        var attacks = activeAttacks.get(player);
        if (attacks == null || attacks.isEmpty()) {
            return;
        }

        List<Object> toRemove = new ArrayList<>();
        List<Object> attacksCopy;
        synchronized (attacks) {
            attacksCopy = new ArrayList<>(attacks);
        }

        for (Object attack : attacksCopy) {
            try {
                boolean stillActive = tickAndCheckActive(player, attack);
                if (!stillActive) {
                    toRemove.add(attack);
                }
            } catch (Exception e) {
                e.printStackTrace();
                toRemove.add(attack);
            }
        }

        if (!toRemove.isEmpty()) {
            synchronized (attacks) {
                attacks.removeAll(toRemove);
                if (attacks.isEmpty()) {
                    activeAttacks.remove(player);
                }
            }
        }
    }

    /**
     * Tick an attack and return whether it's still active
     */
    private static boolean tickAndCheckActive(Player player, Object attack) throws Exception {
        if (attack instanceof AbstractBreathingAttack<?, ?> breathingAttack) {
            breathingAttack.tick();
            return breathingAttack.isActive();
        }


        // Generic reflection-based handling
        try {
            var tickMethod = attack.getClass().getMethod("tick");
            tickMethod.invoke(attack);
        } catch (NoSuchMethodException e) {
            try {
                var tickMethod = attack.getClass().getMethod("tick", Player.class);
                tickMethod.invoke(attack, player);
            } catch (NoSuchMethodException e2) {
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
     * Clear all attacks for a player
     */
    public static void clearAttacks(Player player) {
        var attacks = activeAttacks.remove(player);
        if (attacks != null) {
            for (Object attack : attacks) {
                try {
                    if (attack instanceof AbstractBreathingAttack<?, ?> breathingAttack) {
                        breathingAttack.stop();
                    } else {
                        try {
                            var stopMethod = attack.getClass().getMethod("stop");
                            stopMethod.invoke(attack);
                        } catch (Exception e) {
                            // Ignore if no stop method
                        }
                    }
                } catch (Exception e) {
                    // Ignore errors during cleanup
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
                    try {
                        var stopMethod = attack.getClass().getMethod("stop");
                        stopMethod.invoke(attack);
                    } catch (Exception e) {
                        // Ignore if no stop method
                    }
                }
                attacks.remove(attack);
                if (attacks.isEmpty()) {
                    activeAttacks.remove(player);
                }
                return true;
            } catch (Exception e) {
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

    /**
     * Tick all active attacks for all players
     */
    public static void tickAllAttacks(net.minecraft.server.MinecraftServer server) {
        if (server == null) return;

        for (var player : server.getPlayerList().getPlayers()) {
            tickAttacks(player);
        }

        AbstractBreathingAttack.tickAllActiveAttacks(server);
        AbstractDemonAttack.tickAllActiveAttacks(server);
    }
}