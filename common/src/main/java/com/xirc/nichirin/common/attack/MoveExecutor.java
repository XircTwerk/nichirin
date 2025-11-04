package com.xirc.nichirin.common.attack;

import com.xirc.nichirin.client.gui.CooldownHUD;
import com.xirc.nichirin.client.renderer.effects.AttackHitboxRenderer;
import com.xirc.nichirin.common.attack.component.AbstractBreathingAttack;
import com.xirc.nichirin.common.attack.component.AbstractDemonAttack;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.entity.MovesetCapableNPC;
import com.xirc.nichirin.common.system.NPCResourceManager;
import com.xirc.nichirin.common.util.ComboTracker;
import com.xirc.nichirin.common.util.BreathingManager;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified attack executor - handles both breathing and demon attacks for PLAYERS AND NPCs
 * Includes anti-spam detection, stun prevention, and hitbox debugging
 * REFACTORED: Now accepts LivingEntity instead of just Player
 */
public class MoveExecutor {

    // Store active attacks - using thread-safe collections (UUID for both players and NPCs)
    private static final ConcurrentHashMap<UUID, List<Object>> activeAttacks = new ConcurrentHashMap<>();

    // Packet ID for cooldown display
    private static final ResourceLocation COOLDOWN_PACKET_ID = new ResourceLocation("nichirin", "cooldown_display");

    // Hitbox debugging state
    private static boolean hitboxDebuggingEnabled = false;

    /**
     * Unified execute method for both breathing and demon attacks
     * NOW WORKS WITH LIVINGENTITY (Players and NPCs)
     */
    public static void executeAttack(LivingEntity entity, Object attack, String movesetId, String moveId) {
        // Check if entity is currently stunned (prevents move stacking)
        if (entity.hasEffect(NichirinEffectRegistry.STUNNED.get())) {
            return;
        }

        // Handle both breathing and demon attacks through unified interface
        if (attack instanceof AbstractBreathingAttack<?, ?> breathingAttack) {
            handleAttack(entity, breathingAttack, movesetId, moveId);
        } else if (attack instanceof AbstractDemonAttack<?, ?> demonAttack) {
            handleAttack(entity, demonAttack, movesetId, moveId);
        } else {
            // Fallback for other attack types
            handleGenericAttack(entity, attack, movesetId, moveId);
        }
    }

    /**
     * LEGACY METHOD: Kept for backward compatibility with existing code
     */
    public static void executeAttack(Player player, Object attack, String movesetId, String moveId) {
        executeAttack((LivingEntity) player, attack, movesetId, moveId);
    }

    /**
     * Unified handler for breathing attacks
     */
    private static void handleAttack(LivingEntity entity, AbstractBreathingAttack<?, ?> attack, String movesetId, String moveId) {
        // Check if attack is already configured
        boolean alreadyConfigured = isAttackConfigured(attack);

        if (!alreadyConfigured) {
            configureAttackFromMoveset(entity, attack, movesetId, moveId);
        } else {
            applyPreConfiguredEffects(entity, attack, moveId);
        }

        executeConfiguredAttack(entity, attack, movesetId, moveId);
    }

    /**
     * Unified handler for demon attacks
     */
    private static void handleAttack(LivingEntity entity, AbstractDemonAttack<?, ?> attack, String movesetId, String moveId) {
        // Check if attack is already configured
        boolean alreadyConfigured = isDemonAttackConfigured(attack);

        if (!alreadyConfigured) {
            configureAttackFromMoveset(entity, attack, movesetId, moveId);
        } else {
            applyPreConfiguredDemonEffects(entity, attack, moveId);
        }

        executeConfiguredAttack(entity, attack, movesetId, moveId);
    }

    /**
     * Configure attack using moveset configuration
     */
    private static void configureAttackFromMoveset(LivingEntity entity, Object attack, String movesetId, String moveId) {
        AbstractMoveset moveset = MovesetRegistry.getMoveset(movesetId);
        if (moveset == null) return;

        AbstractMoveset.MoveConfiguration config = findMoveConfig(moveset, moveId);
        if (config == null) return;

        // Apply anti-spam detection to hitstun (ONLY for players)
        int originalHitStun = config.getHitStunOrDefault(0);
        int modifiedHitStun = originalHitStun;

        if (entity instanceof Player player) {
            modifiedHitStun = ComboTracker.getModifiedHitStun(player, moveId, originalHitStun);
        }

        if (modifiedHitStun != originalHitStun) {
            config = createModifiedConfig(config, modifiedHitStun);
        }

        // Apply stun effect to prevent move stacking
        applyMoveStun(entity, config);

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
    private static void applyPreConfiguredEffects(LivingEntity entity, AbstractBreathingAttack<?, ?> attack, String moveId) {
        // Apply anti-spam detection (ONLY for players)
        if (entity instanceof Player player) {
            int originalHitStun = attack.getHitStun();
            ComboTracker.getModifiedHitStun(player, moveId, originalHitStun);
        }

        // Apply stun effect
        applyPreConfiguredMoveStun(entity, attack);
    }

    /**
     * Apply effects for pre-configured demon attacks
     */
    private static void applyPreConfiguredDemonEffects(LivingEntity entity, AbstractDemonAttack<?, ?> attack, String moveId) {
        // Apply anti-spam detection (ONLY for players)
        if (entity instanceof Player player) {
            int originalHitStun = getHitStunFromAttack(attack);
            ComboTracker.getModifiedHitStun(player, moveId, originalHitStun);
        }

        // Apply stun effect
        applyPreConfiguredDemonMoveStun(entity, attack);
    }

    /**
     * Execute the configured attack
     */
    private static void executeConfiguredAttack(LivingEntity entity, Object attack, String movesetId, String moveId) {
        // Get display name from registry
        NichirinMoveRegistry.MoveInfo moveInfo = NichirinMoveRegistry.getMove(movesetId, moveId);
        String displayName = moveInfo != null ? moveInfo.displayName : attack.getClass().getSimpleName();

        // Get cooldown from attack
        int cooldown = getCooldownForAttack(attack);

        // Execute the attack
        executeAttackInternal(entity, attack, displayName, cooldown);
    }

    /**
     * Fallback for generic attack types
     */
    private static void handleGenericAttack(LivingEntity entity, Object attack, String movesetId, String moveId) {
        // Basic execution for unknown attack types
        String displayName = attack.getClass().getSimpleName();
        int cooldown = getCooldownForAttack(attack);
        executeAttackInternal(entity, attack, displayName, cooldown);
    }

    /**
     * Execute attack with visual hitbox debugging
     */
    public static void executeAttackWithVisuals(LivingEntity entity, Object attack, String movesetId, String moveId) {
        if (entity.hasEffect(NichirinEffectRegistry.STUNNED.get())) {
            return;
        }

        // Clear existing hitboxes for clean visuals (client-side only)
        if (entity.level().isClientSide) {
            AttackHitboxRenderer.clearAll();
        }

        executeAttack(entity, attack, movesetId, moveId);
    }

    /**
     * LEGACY METHOD: Kept for backward compatibility
     */
    public static void executeAttackWithVisuals(Player player, Object attack, String movesetId, String moveId) {
        executeAttackWithVisuals((LivingEntity) player, attack, movesetId, moveId);
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
    private static void applyMoveStun(LivingEntity entity, AbstractMoveset.MoveConfiguration config) {
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
            entity.addEffect(stunEffect);
        }
    }

    /**
     * Apply stun effect for pre-configured breathing attacks
     */
    private static void applyPreConfiguredMoveStun(LivingEntity entity, AbstractBreathingAttack<?, ?> attack) {
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
            entity.addEffect(stunEffect);
        }
    }

    /**
     * Apply stun effect for pre-configured demon attacks
     */
    private static void applyPreConfiguredDemonMoveStun(LivingEntity entity, AbstractDemonAttack<?, ?> attack) {
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
            entity.addEffect(stunEffect);
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
    public static void executeAttackWithInfo(LivingEntity entity, Object attack, String displayName, int cooldown) {
        if (entity.hasEffect(NichirinEffectRegistry.STUNNED.get())) {
            return;
        }
        executeAttackInternal(entity, attack, displayName, cooldown);
    }

    /**
     * LEGACY METHOD: Kept for backward compatibility
     */
    public static void executeAttackWithInfo(Player player, Object attack, String displayName, int cooldown) {
        executeAttackWithInfo((LivingEntity) player, attack, displayName, cooldown);
    }

    /**
     * Internal execution method
     */
    private static void executeAttackInternal(LivingEntity entity, Object attack, String displayName, int cooldown) {
        if (!isAttackActive(attack)) {
            startAttack(entity, attack);

            if (isAttackActive(attack)) {
                trackAttack(entity, attack);

                // Send cooldown to client if on server AND entity is a player
                if (!entity.level().isClientSide && entity instanceof ServerPlayer serverPlayer && cooldown > 0) {
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
     * Generic method to start an attack - NOW WORKS WITH LivingEntity
     */
    private static void startAttack(LivingEntity entity, Object attack) {
        if (attack instanceof AbstractBreathingAttack<?, ?> breathingAttack) {
            // Check if start() accepts LivingEntity or Player
            if (entity instanceof Player player) {
                breathingAttack.start(player, entity.level());
            } else {
                // For NPCs, try to call start with LivingEntity
                try {
                    var startMethod = attack.getClass().getMethod("start", LivingEntity.class, Level.class);
                    try {
                        startMethod.invoke(attack, entity, entity.level());
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                } catch (NoSuchMethodException e) {
                    // Fallback to Player version if LivingEntity not supported
                    // This means the attack doesn't support NPCs yet
                }
            }
            return;
        }

        try {
            // Try LivingEntity version first
            try {
                var startMethod = attack.getClass().getMethod("start", LivingEntity.class, Level.class);
                startMethod.invoke(attack, entity, entity.level());
                return;
            } catch (NoSuchMethodException e1) {
                // Fall back to Player version
                if (entity instanceof Player player) {
                    try {
                        var startMethod = attack.getClass().getMethod("start", Player.class, Level.class);
                        startMethod.invoke(attack, player, entity.level());
                        return;
                    } catch (NoSuchMethodException e2) {
                        try {
                            var startMethod = attack.getClass().getMethod("start", Player.class);
                            startMethod.invoke(attack, player);
                            return;
                        } catch (NoSuchMethodException e3) {
                            var startMethod = attack.getClass().getMethod("start");
                            startMethod.invoke(attack);
                        }
                    }
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
     * PLAYER-ONLY method
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
     * Check if an entity can execute a move (not stunned)
     */
    public static boolean canExecuteMove(LivingEntity entity) {
        return !entity.hasEffect(NichirinEffectRegistry.STUNNED.get());
    }

    /**
     * LEGACY METHOD: Player version
     */
    public static boolean canExecuteMove(Player player) {
        return canExecuteMove((LivingEntity) player);
    }

    /**
     * Force remove move stun
     */
    public static void removeMoveStun(LivingEntity entity) {
        entity.removeEffect(NichirinEffectRegistry.STUNNED.get());
    }

    /**
     * LEGACY METHOD: Player version
     */
    public static void removeMoveStun(Player player) {
        removeMoveStun((LivingEntity) player);
    }

    /**
     * Get remaining stun duration in ticks
     */
    public static int getRemainingStunTicks(LivingEntity entity) {
        MobEffectInstance stunEffect = entity.getEffect(NichirinEffectRegistry.STUNNED.get());
        return stunEffect != null ? stunEffect.getDuration() : 0;
    }

    /**
     * LEGACY METHOD: Player version
     */
    public static int getRemainingStunTicks(Player player) {
        return getRemainingStunTicks((LivingEntity) player);
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
     * Tick all active attacks for an entity
     */
    public static void tickAttacks(LivingEntity entity) {
        var attacks = activeAttacks.get(entity.getUUID());
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
                boolean stillActive = tickAndCheckActive(entity, attack);
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
                    activeAttacks.remove(entity.getUUID());
                }
            }
        }
    }

    /**
     * LEGACY METHOD: Player version
     */
    public static void tickAttacks(Player player) {
        tickAttacks((LivingEntity) player);
    }

    /**
     * Tick an attack and return whether it's still active
     */
    private static boolean tickAndCheckActive(LivingEntity entity, Object attack) throws Exception {
        if (attack instanceof AbstractBreathingAttack<?, ?> breathingAttack) {
            breathingAttack.tick();
            return breathingAttack.isActive();
        }

        // Generic reflection-based handling
        try {
            var tickMethod = attack.getClass().getMethod("tick");
            tickMethod.invoke(attack);
        } catch (NoSuchMethodException e) {
            // Try with entity parameter
            try {
                var tickMethod = attack.getClass().getMethod("tick", LivingEntity.class);
                tickMethod.invoke(attack, entity);
            } catch (NoSuchMethodException e2) {
                // Try with Player parameter (legacy)
                if (entity instanceof Player player) {
                    try {
                        var tickMethod = attack.getClass().getMethod("tick", Player.class);
                        tickMethod.invoke(attack, player);
                    } catch (NoSuchMethodException e3) {
                        throw new Exception("Could not tick attack: " + attack.getClass().getName());
                    }
                }
            }
        }

        var isActiveMethod = attack.getClass().getMethod("isActive");
        return (boolean) isActiveMethod.invoke(attack);
    }

    /**
     * Track an attack for an entity
     */
    private static void trackAttack(LivingEntity entity, Object attack) {
        activeAttacks.computeIfAbsent(entity.getUUID(), k -> new ArrayList<>()).add(attack);
    }

    /**
     * Clear all attacks for an entity
     */
    public static void clearAttacks(LivingEntity entity) {
        var attacks = activeAttacks.remove(entity.getUUID());
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
     * LEGACY METHOD: Player version
     */
    public static void clearAttacks(Player player) {
        clearAttacks((LivingEntity) player);
    }

    /**
     * Check if an entity has any active attacks
     */
    public static boolean hasActiveAttacks(LivingEntity entity) {
        var attacks = activeAttacks.get(entity.getUUID());
        return attacks != null && !attacks.isEmpty();
    }

    /**
     * LEGACY METHOD: Player version
     */
    public static boolean hasActiveAttacks(Player player) {
        return hasActiveAttacks((LivingEntity) player);
    }

    /**
     * Get the number of active attacks for an entity
     */
    public static int getActiveAttackCount(LivingEntity entity) {
        var attacks = activeAttacks.get(entity.getUUID());
        return attacks != null ? attacks.size() : 0;
    }

    /**
     * LEGACY METHOD: Player version
     */
    public static int getActiveAttackCount(Player player) {
        return getActiveAttackCount((LivingEntity) player);
    }

    /**
     * Get all active attacks for an entity (defensive copy)
     */
    public static List<Object> getActiveAttacks(LivingEntity entity) {
        var attacks = activeAttacks.get(entity.getUUID());
        return attacks != null ? new ArrayList<>(attacks) : new ArrayList<>();
    }

    /**
     * LEGACY METHOD: Player version
     */
    public static List<Object> getActiveAttacks(Player player) {
        return getActiveAttacks((LivingEntity) player);
    }

    /**
     * Force stop a specific attack for an entity
     */
    public static boolean stopAttack(LivingEntity entity, Object attack) {
        var attacks = activeAttacks.get(entity.getUUID());
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
                    activeAttacks.remove(entity.getUUID());
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
     * LEGACY METHOD: Player version
     */
    public static boolean stopAttack(Player player, Object attack) {
        return stopAttack((LivingEntity) player, attack);
    }

    /**
     * Stop all attacks of a specific type for an entity
     */
    public static int stopAttacksOfType(LivingEntity entity, Class<?> attackType) {
        var attacks = activeAttacks.get(entity.getUUID());
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
            if (stopAttack(entity, attack)) {
                stopped++;
            }
        }
        return stopped;
    }

    /**
     * LEGACY METHOD: Player version
     */
    public static int stopAttacksOfType(Player player, Class<?> attackType) {
        return stopAttacksOfType((LivingEntity) player, attackType);
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