package com.xirc.nichirin.common.attack.moveset.breathing;

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

    // Explosive Rush has its own per-player cooldown separate from the wheel-move map.
    private static final Map<UUID, Long> explosiveRushCooldownEnd = new HashMap<>();

    public BeastBreathingMoveset() {
        super("beast_breathing", "Beast Breathing", MovesetType.BREATHING, createBuilder());
    }

    private static MovesetBuilder createBuilder() {
        return new MovesetBuilder()
                .withIdleAnimation("nichirin:beast_idle")
                .withSpeedMultiplier(1.2f)

                .withMove(new MoveBuilder("pierce", "Pierce")
                        .withTiming(0, 0, 6)
                        .withDamage(3.0f)
                        .withRange(4.0f)
                        .withKnockback(0.4f)
                        .withBreathCost(8.0f)
                        .withStaminaCost(10.0f)
                        .withHitStun(10)
                        .withHitboxSize(1.5f)
                        .withDescription("Stabs forward")
                        .asLeftClick()
                        .withAttack(BeastPierceAttack::new)
                )

                .withMove(new MoveBuilder("x_slice", "Slice")
                        .withTiming(0, 0, 8)
                        .withDamage(5.0f)
                        .withRange(6.0f)
                        .withKnockback(0.3f)
                        .withBreathCost(25.0f)
                        .withHitStun(12)
                        .withHitboxSize(2.0f)
                        .withDescription("Two progressing X-shaped slash projectiles.")
                        .asRightClick()
                        .withAttack(BeastXSliceAttack::new)
                )

                .withMove(new MoveBuilder("explosive_rush", "Explosive Rush")
                        .withTiming(0, 0, 11)
                        .withDamage(12.0f)
                        .withDashSpeed(40.0f)
                        .withRange(16.0f)
                        .withKnockback(0.5f)
                        .withBreathCost(25.0f)
                        .withHitStun(20)
                        .withHitboxSize(2.5f)
                        .withDescription("Invulnerable dash at blinding speed; deflects all projectiles.")
                        .asCrouchRightClick()
                        .withAttack(BeastExplosiveRushAttack::new)
                )

                // 0 - Third Fang: Devour
                .withMove(new MoveBuilder("devour", "Devour")
                        .withTiming(60, 3, 14)
                        .withDamage(8.0f)
                        .withRange(3.5f)
                        .withKnockback(0.0f)
                        .withBreathCost(10.0f)
                        .withHitStun(40)
                        .withHitboxSize(2.0f)
                        .withDescription("Two simultaneous horizontal slashes. No knockback, heavy stun.")
                        .withAttack(BeastDevourAttack::new)
                )

                // 1 - Fourth Fang: Slice 'n' Dice
                .withMove(new MoveBuilder("slice_n_dice", "Slice 'n' Dice")
                        .withTiming(70, 2, 14)
                        .withDamage(3.0f)
                        .withRange(2.5f)
                        .withKnockback(0.15f)
                        .withBreathCost(15.0f)
                        .withHitStun(5)
                        .withHitboxSize(1.8f)
                        .withDescription("8 rapid diagonal slashes. Each hit bypasses immunity frames.")
                        .withAttack(BeastSliceNDiceAttack::new)
                )

                // 2 - Fifth Fang: Crazy Cutting
                .withMove(new MoveBuilder("crazy_cutting", "Crazy Cutting")
                        .withTiming(100, 2, 21)
                        .withDamage(5.0f)
                        .withRange(3.5f)
                        .withKnockback(0.2f)
                        .withBreathCost(15.0f)
                        .withHitStun(12)
                        .withHitboxSize(3.5f)
                        .withDescription("Omnidirectional slicing while levitating.")
                        .withAttack(BeastCrazyCuttingAttack::new)
                )

                // 3 - Sixth Fang: Palisade Bite
                .withMove(new MoveBuilder("palisade_bite", "Palisade Bite")
                        .withTiming(80, 2, 14)
                        .withDamage(6.0f)
                        .withRange(5.0f)
                        .withKnockback(0.2f)
                        .withBreathCost(10.0f)
                        .withHitStun(15)
                        .withHitboxSize(2.5f)
                        .withDescription("Four wide progressing slashes in a saw-like pattern.")
                        .withAttack(BeastPalisadeBiteAttack::new)
                )

                // 4 - Seventh Form: Spatial Awareness
                .withMove(new MoveBuilder("spatial_awareness", "Spatial Awareness")
                        .withTiming(200, 0, 4)
                        .withDamage(0.0f)
                        .withRange(30.0f)
                        .withKnockback(0.0f)
                        .withBreathCost(10.0f)
                        .withHitStun(0)
                        .withHitboxSize(30.0f)
                        .withDescription("Sense all enemies through walls via glowing. 10-second cooldown.")
                        .withAttack(BeastSpatialAwarenessAttack::new)
                )

                // 5 - Ninth Fang: Bendy Slash
                .withMove(new MoveBuilder("bendy_slash", "Bendy Slash")
                        .withTiming(60, 0, 4)
                        .withDamage(9.0f)
                        .withRange(8.0f)
                        .withKnockback(0.3f)
                        .withBreathCost(15.0f)
                        .withHitStun(15)
                        .withHitboxSize(2.5f)
                        .withDescription("Long-range frontal slash. No windup; covers everything in front.")
                        .withAttack(BeastBendySlashAttack::new)
                )

                // 6 - Tenth Fang: Whirling Fangs
                .withMove(new MoveBuilder("whirling_fangs", "Whirling Fangs")
                        .withTiming(80, 0, 14)
                        .withDamage(4.0f)
                        .withRange(3.0f)
                        .withKnockback(0.2f)
                        .withBreathCost(15.0f)
                        .withHitStun(10)
                        .withHitboxSize(3.0f)
                        .withDescription("Rapid spin; deflects all projectiles around you.")
                        .withAttack(BeastWhirlingFangsAttack::new)
                )

                // 7 - Eleventh Fang: Throwing Strike
                .withMove(new MoveBuilder("throwing_strike", "Throwing Strike")
                        .withTiming(120, 3, 7)
                        .withDamage(12.0f)
                        .withRange(15.0f)
                        .withKnockback(0.3f)
                        .withBreathCost(25.0f)
                        .withHitStun(20)
                        .withHitboxSize(0.5f)
                        .withDescription("Throws both katanas as piercing projectiles. Stick to blocks, expire in 10s.")
                        .withAttack(BeastThrowingStrikeAttack::new)
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
        return super.handleLeftClick(entity);
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
                            .withStyle(s -> s.withColor(0xFF5555)), true);
            return true;
        }
        // Explosive Rush starts its cooldown only when the move actually executes — mirror the base
        // handler's resource gate so a blocked cast doesn't burn the cooldown.
        MoveConfiguration crouchConfig = getCrouchRightClickConfiguration();
        boolean willExecuteRush = isCrouching && crouchConfig != null
                && crouchConfig.hasExecutable() && hasResourcesForMove(entity, crouchConfig);
        boolean result = super.handleRightClick(entity, isCrouching);
        if (willExecuteRush) {
            setExplosiveRushCooldown(entity);
        }
        return result;
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
                                        .withStyle(s -> s.withColor(0xFF5555)), true);
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
        super.performMove(entity, moveIndex);

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
