package com.xirc.nichirin.common.attack.moveset.breathing;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.moves.breathing.beast.*;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.network.util.CooldownDisplayPacket;
import com.xirc.nichirin.common.util.EntityResources;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BeastBreathingMoveset extends AbstractMoveset {

    private static final Map<UUID, Map<Integer, Long>> entityCooldowns = new HashMap<>();
    private static final Map<UUID, Boolean> executingMove = new HashMap<>();
    private static final ThreadLocal<BeastBreathingMoveset> CURRENT_MOVESET = new ThreadLocal<>();

    // Explosive Rush has its own per-player cooldown separate from the wheel-move map.
    private static final Map<UUID, Long> explosiveRushCooldownEnd = new HashMap<>();

    public BeastBreathingMoveset() {
        super("beast_breathing", "Beast Breathing", MovesetType.BREATHING, createBuilder());
    }

    private static MovesetBuilder createBuilder() {
        return new MovesetBuilder()
                .withIdleAnimation("nichirin:beast_idle")
                .withSpeedMultiplier(1.2f)

                .withLeftClickMove(new MoveBuilder("pierce", "Pierce")
                        .withAnimation("nichirin:beast_pierce", 7)
                        .withTiming(0, 0, 6)
                        .withDamage(3.0f)
                        .withRange(4.0f)
                        .withKnockback(0.4f)
                        .withBreathCost(8.0f)
                        .withStaminaCost(10.0f)
                        .withHitStun(10)
                        .withHitboxSize(1.5f)
                        .withDescription("Stabs forward")
                        .withAction(entity -> {
                            BeastPierceAttack attack = new BeastPierceAttack();
                            BeastBreathingMoveset m = getCurrentMoveset();
                            if (m != null) attack.configure(m.getLeftClickConfiguration());
                            MoveExecutor.executeAttack(entity, attack, "beast_breathing", "pierce");
                        })
                )

                .withRightClickMove(new MoveBuilder("x_slice", "Slice")
                        .withAnimation("nichirin:beast_x_slice", 8)
                        .withTiming(0, 0, 8)
                        .withDamage(5.0f)
                        .withRange(6.0f)
                        .withKnockback(0.3f)
                        .withBreathCost(25.0f)
                        .withHitStun(12)
                        .withHitboxSize(2.0f)
                        .withDescription("Two progressing X-shaped slash projectiles.")
                        .withAction(entity -> {
                            BeastXSliceAttack attack = new BeastXSliceAttack();
                            BeastBreathingMoveset m = getCurrentMoveset();
                            if (m != null) attack.configure(m.getRightClickConfiguration());
                            MoveExecutor.executeAttack(entity, attack, "beast_breathing", "x_slice");
                        })
                )

                .withCrouchRightClickMove(new MoveBuilder("explosive_rush", "Explosive Rush")
                        .withAnimation("nichirin:beast_explosive_rush", 12)
                        .withTiming(0, 0, 11)
                        .withDamage(12.0f)
                        .withDashSpeed(40.0f)
                        .withRange(16.0f)
                        .withKnockback(0.5f)
                        .withBreathCost(25.0f)
                        .withHitStun(20)
                        .withHitboxSize(2.5f)
                        .withDescription("Invulnerable dash at blinding speed; deflects all projectiles.")
                        .withAction(entity -> {
                            BeastExplosiveRushAttack attack = new BeastExplosiveRushAttack();
                            BeastBreathingMoveset m = getCurrentMoveset();
                            if (m != null) {
                                attack.configure(m.getCrouchRightClickConfiguration());
                                m.setExplosiveRushCooldown(entity);
                            }
                            MoveExecutor.executeAttack(entity, attack, "beast_breathing", "explosive_rush");
                        })
                )

                // 0 - Third Fang: Devour
                .withMove(new MoveBuilder("devour", "Devour")
                        .withAnimation("nichirin:beast_devour", 8)
                        .withTiming(60, 3, 14)
                        .withDamage(8.0f)
                        .withRange(3.5f)
                        .withKnockback(0.0f)
                        .withBreathCost(10.0f)
                        .withHitStun(40)
                        .withHitboxSize(2.0f)
                        .withDescription("Two simultaneous horizontal slashes. No knockback, heavy stun.")
                        .withAction(entity -> {
                            BeastDevourAttack attack = new BeastDevourAttack();
                            BeastBreathingMoveset m = getCurrentMoveset();
                            if (m != null) attack.configure(m.getMove(0));
                            MoveExecutor.executeAttack(entity, attack, "beast_breathing", "devour");
                        })
                )

                // 1 - Fourth Fang: Slice 'n' Dice
                .withMove(new MoveBuilder("slice_n_dice", "Slice 'n' Dice")
                        .withAnimation("nichirin:beast_slice_dice", 9)
                        .withTiming(70, 2, 14)
                        .withDamage(3.0f)
                        .withRange(2.5f)
                        .withKnockback(0.15f)
                        .withBreathCost(15.0f)
                        .withHitStun(5)
                        .withHitboxSize(1.8f)
                        .withDescription("8 rapid diagonal slashes. Each hit bypasses immunity frames.")
                        .withAction(entity -> {
                            BeastSliceNDiceAttack attack = new BeastSliceNDiceAttack();
                            BeastBreathingMoveset m = getCurrentMoveset();
                            if (m != null) attack.configure(m.getMove(1));
                            MoveExecutor.executeAttack(entity, attack, "beast_breathing", "slice_n_dice");
                        })
                )

                // 2 - Fifth Fang: Crazy Cutting
                .withMove(new MoveBuilder("crazy_cutting", "Crazy Cutting")
                        .withAnimation("nichirin:beast_crazy_cutting", 10)
                        .withTiming(100, 2, 21)
                        .withDamage(5.0f)
                        .withRange(3.5f)
                        .withKnockback(0.2f)
                        .withBreathCost(15.0f)
                        .withHitStun(12)
                        .withHitboxSize(3.5f)
                        .withDescription("Omnidirectional slicing while levitating.")
                        .withAction(entity -> {
                            BeastCrazyCuttingAttack attack = new BeastCrazyCuttingAttack();
                            BeastBreathingMoveset m = getCurrentMoveset();
                            if (m != null) attack.configure(m.getMove(2));
                            MoveExecutor.executeAttack(entity, attack, "beast_breathing", "crazy_cutting");
                        })
                )

                // 3 - Sixth Fang: Palisade Bite
                .withMove(new MoveBuilder("palisade_bite", "Palisade Bite")
                        .withAnimation("nichirin:beast_palisade_bite", 9)
                        .withTiming(80, 2, 14)
                        .withDamage(6.0f)
                        .withRange(5.0f)
                        .withKnockback(0.2f)
                        .withBreathCost(10.0f)
                        .withHitStun(15)
                        .withHitboxSize(2.5f)
                        .withDescription("Four wide progressing slashes in a saw-like pattern.")
                        .withAction(entity -> {
                            BeastPalisadeBiteAttack attack = new BeastPalisadeBiteAttack();
                            BeastBreathingMoveset m = getCurrentMoveset();
                            if (m != null) attack.configure(m.getMove(3));
                            MoveExecutor.executeAttack(entity, attack, "beast_breathing", "palisade_bite");
                        })
                )

                // 4 - Seventh Form: Spatial Awareness
                .withMove(new MoveBuilder("spatial_awareness", "Spatial Awareness")
                        .withAnimation("nichirin:beast_spatial_awareness", 6)
                        .withTiming(200, 0, 4)
                        .withDamage(0.0f)
                        .withRange(30.0f)
                        .withKnockback(0.0f)
                        .withBreathCost(10.0f)
                        .withHitStun(0)
                        .withHitboxSize(30.0f)
                        .withDescription("Sense all enemies through walls via glowing. 10-second cooldown.")
                        .withAction(entity -> {
                            BeastSpatialAwarenessAttack attack = new BeastSpatialAwarenessAttack();
                            BeastBreathingMoveset m = getCurrentMoveset();
                            if (m != null) attack.configure(m.getMove(4));
                            MoveExecutor.executeAttack(entity, attack, "beast_breathing", "spatial_awareness");
                        })
                )

                // 5 - Ninth Fang: Bendy Slash
                .withMove(new MoveBuilder("bendy_slash", "Bendy Slash")
                        .withAnimation("nichirin:beast_bendy_slash", 8)
                        .withTiming(60, 0, 4)
                        .withDamage(9.0f)
                        .withRange(8.0f)
                        .withKnockback(0.3f)
                        .withBreathCost(15.0f)
                        .withHitStun(15)
                        .withHitboxSize(2.5f)
                        .withDescription("Long-range frontal slash. No windup; covers everything in front.")
                        .withAction(entity -> {
                            BeastBendySlashAttack attack = new BeastBendySlashAttack();
                            BeastBreathingMoveset m = getCurrentMoveset();
                            if (m != null) attack.configure(m.getMove(5));
                            MoveExecutor.executeAttack(entity, attack, "beast_breathing", "bendy_slash");
                        })
                )

                // 6 - Tenth Fang: Whirling Fangs
                .withMove(new MoveBuilder("whirling_fangs", "Whirling Fangs")
                        .withAnimation("nichirin:beast_whirling_fangs", 9)
                        .withTiming(80, 0, 14)
                        .withDamage(4.0f)
                        .withRange(3.0f)
                        .withKnockback(0.2f)
                        .withBreathCost(15.0f)
                        .withHitStun(10)
                        .withHitboxSize(3.0f)
                        .withDescription("Rapid spin; deflects all projectiles around you.")
                        .withAction(entity -> {
                            BeastWhirlingFangsAttack attack = new BeastWhirlingFangsAttack();
                            BeastBreathingMoveset m = getCurrentMoveset();
                            if (m != null) attack.configure(m.getMove(6));
                            MoveExecutor.executeAttack(entity, attack, "beast_breathing", "whirling_fangs");
                        })
                )

                // 7 - Eleventh Fang: Throwing Strike
                .withMove(new MoveBuilder("throwing_strike", "Throwing Strike")
                        .withAnimation("nichirin:beast_throwing_strike", 10)
                        .withTiming(120, 3, 7)
                        .withDamage(12.0f)
                        .withRange(15.0f)
                        .withKnockback(0.3f)
                        .withBreathCost(25.0f)
                        .withHitStun(20)
                        .withHitboxSize(0.5f)
                        .withDescription("Throws both katanas as piercing projectiles. Stick to blocks, expire in 10s.")
                        .withAction(entity -> {
                            BeastThrowingStrikeAttack attack = new BeastThrowingStrikeAttack();
                            BeastBreathingMoveset m = getCurrentMoveset();
                            if (m != null) attack.configure(m.getMove(7));
                            MoveExecutor.executeAttack(entity, attack, "beast_breathing", "throwing_strike");
                        })
                );
    }

    @Override
    public int getMoveCount() {
        return 8;
    }

    /** Breathing movesets pace themselves inside their attack classes — skip auto-stun. */
    @Override
    protected boolean shouldAutoStunClickMoves() {
        return false;
    }

    @Override
    public boolean handleLeftClick(LivingEntity entity) {
        if (!canPerformMoves(entity)) return true;
        CURRENT_MOVESET.set(this);
        try {
            return super.handleLeftClick(entity);
        } finally {
            CURRENT_MOVESET.remove();
        }
    }

    @Override
    public boolean handleRightClick(LivingEntity entity, boolean isCrouching) {
        if (!canPerformMoves(entity)) return true;
        // Explosive Rush (crouch-right) has its own cooldown — gate before delegating so the
        // animation/stamina aren't consumed when blocked.
        if (isCrouching && !canUseExplosiveRush(entity)) {
            Long end = explosiveRushCooldownEnd.get(entity.getUUID());
            long remaining = end != null ? (end - entity.level().getGameTime()) / 20 : 0;
            EntityResources.sendMessage(entity,
                    Component.literal("Explosive Rush on cooldown! " + remaining + "s remaining")
                            .withStyle(s -> s.withColor(0xB0C4DE)), true);
            return true;
        }
        CURRENT_MOVESET.set(this);
        try {
            return super.handleRightClick(entity, isCrouching);
        } finally {
            CURRENT_MOVESET.remove();
        }
    }

    @Override
    public void performMove(LivingEntity entity, int moveIndex) {
        if (!canPerformMoves(entity)) {
            return;
        }

        if (!canUseMove(entity, moveIndex)) {
            MoveConfiguration config = getMove(moveIndex);
            if (config != null) {
                Map<Integer, Long> cooldowns = entityCooldowns.get(entity.getUUID());
                if (cooldowns != null) {
                    Long cd = cooldowns.get(moveIndex);
                    if (cd != null) {
                        long remaining = (cd - entity.level().getGameTime()) / 20;
                        EntityResources.sendMessage(entity, Component.literal(config.getDisplayName() + " on cooldown! " + remaining + "s remaining")
                                        .withStyle(s -> s.withColor(0xB0C4DE)), true);
                    }
                }
            }
            return;
        }

        MoveConfiguration config = getMove(moveIndex);
        if (config != null) {
            float breathCost = config.getBreathCostOrDefault(0.0f);
            if (breathCost > 0 && !EntityResources.hasBreath(entity, breathCost + 0.1f)) {
                EntityResources.sendMessage(entity, Component.literal("Not enough breath for " + config.getDisplayName() + "!")
                        .withStyle(s -> s.withColor(0xFF3333)), true);
                return;
            }
        }

        executingMove.put(entity.getUUID(), true);
        CURRENT_MOVESET.set(this);
        try {
            super.performMove(entity, moveIndex);
        } finally {
            CURRENT_MOVESET.remove();
        }

        executingMove.remove(entity.getUUID());
        if (config != null) {
            setMoveCooldown(entity, moveIndex);
            if (!entity.level().isClientSide && entity instanceof ServerPlayer serverPlayer
                    && config.getCooldownOrDefault(0) > 0) {
                CooldownDisplayPacket.sendToClient(serverPlayer, "beast_breathing", config);
            }
        }
    }

    private boolean canUseExplosiveRush(LivingEntity entity) {
        Long end = explosiveRushCooldownEnd.get(entity.getUUID());
        if (end == null) return true;
        return entity.level().getGameTime() >= end;
    }

    private void setExplosiveRushCooldown(LivingEntity entity) {
        MoveConfiguration config = getCrouchRightClickConfiguration();
        long cooldown = config != null ? config.getCooldownOrDefault(0) : 0;
        if (cooldown > 0) {
            explosiveRushCooldownEnd.put(entity.getUUID(), entity.level().getGameTime() + cooldown);
        }
    }

    private boolean canUseMove(LivingEntity entity, int moveIndex) {
        MoveConfiguration config = getMove(moveIndex);
        if (config == null || config.getCooldownOrDefault(0) <= 0) return true;
        Map<Integer, Long> cooldowns = entityCooldowns.get(entity.getUUID());
        if (cooldowns == null) return true;
        Long end = cooldowns.get(moveIndex);
        if (end == null) return true;
        return entity.level().getGameTime() >= end;
    }

    private void setMoveCooldown(LivingEntity entity, int moveIndex) {
        MoveConfiguration config = getMove(moveIndex);
        if (config == null || config.getCooldownOrDefault(0) <= 0) return;
        long end = entity.level().getGameTime() + config.getCooldownOrDefault(0);
        entityCooldowns.computeIfAbsent(entity.getUUID(), k -> new HashMap<>()).put(moveIndex, end);
    }

    @Override
    public String getRightClickMoveName() {
        return "Second Fang: Slice";
    }

    @Override
    public String getCrouchRightClickMoveName() {
        return "Eighth Form: Explosive Rush";
    }

    public static BeastBreathingMoveset getCurrentMoveset() {
        return CURRENT_MOVESET.get();
    }

    public static void resetCooldowns(LivingEntity entity) {
        entityCooldowns.remove(entity.getUUID());
        explosiveRushCooldownEnd.remove(entity.getUUID());
    }

    public static void cleanupPlayer(LivingEntity entity) {
        entityCooldowns.remove(entity.getUUID());
        executingMove.remove(entity.getUUID());
        explosiveRushCooldownEnd.remove(entity.getUUID());
    }
}