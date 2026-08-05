package com.xirc.nichirin.common.attack.moveset.breathing;

import com.xirc.nichirin.common.attack.moves.breathing.sound.*;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.network.util.CooldownDisplayPacket;
import com.xirc.nichirin.common.util.EntityResources;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SoundBreathingMoveset extends AbstractMoveset {

    private static final Map<UUID, Map<Integer, Long>> entityCooldowns = new HashMap<>();
    private static final Map<UUID, Boolean> executingMove = new HashMap<>();

    public SoundBreathingMoveset() {
        super("sound_breathing", "Sound Breathing", MovesetType.BREATHING, createBuilder());
    }

    private static MovesetBuilder createBuilder() {
        return new MovesetBuilder()
                .withIdleAnimation("nichirin:sound_idle")
                .withSpeedMultiplier(1.1f)

                .withMove(new MoveBuilder("tempo_breaker", "Tempo Breaker")
                        .withTiming(0, 8, 42)
                        .withDamage(0f) // explosion is what deals the damage
                        .withRange(5.0f)
                        .withKnockback(0.8f)
                        .withBreathCost(20.0f)
                        .withHitStun(10)
                        .withHitboxSize(3.0f)
                        .withDescription("Wide sweep that triggers a delayed explosion dealing area damage.")
                        .asRightClick()
                        .withAttack(TempoBreakerAttack::new)
                )

                .withMove(new MoveBuilder("rhythmic_step", "Rhythmic Step")
                        .withTiming(0, 0, 14)
                        .withDamage(8.0f)
                        .withDashSpeed(4.0f)
                        .withRange(4.0f)
                        .withKnockback(0.5f)
                        .withBreathCost(25.0f)
                        .withHitStun(15)
                        .withHitboxSize(3.0f)
                        .withDescription("Short dash that damages enemies you pass through.")
                        .asCrouchRightClick()
                        .withAttack(RhythmicStepAttack::new)
                )

                // First Form: Roar - AOE slam (INDEX 0 in wheel)
                .withMove(new MoveBuilder("roar", "Roar")
                        .withTiming(160, 50, 14)
                        .withDamage(20.0f)
                        .withRange(13.5f)
                        .withKnockback(0.3f)
                        .withBreathCost(25.0f)
                        .withHitStun(20)
                        .withHitboxSize(13.5f)
                        .withHyperArmor()
                        .withDescription("AOE slam that hits all enemies in a large radius.")
                        .withAttack(RoarAttack::new)
                )

                // Fourth Form: Constant Resounding Slashes - 360° defense (INDEX 1 in wheel)
                .withMove(new MoveBuilder("constant_resounding_slashes", "Constant Resounding Slashes")
                        .withTiming(180, 5, 140)
                        .withDamage(6.0f)
                        .withRange(20.0f)
                        .withKnockback(0f)
                        .withBreathCost(25.0f)
                        .withHitStun(40)
                        .withHitboxSize(12.25f)
                        .withArmor(4)
                        .withDescription("Spinning 360° attack that hits all nearby enemies multiple times.")
                        .withAttack(ConstantResoundingSlashesAttack::new)
                )

                // Fifth Form: String Performance - Multi-segment dash (INDEX 2 in wheel)
                .withMove(new MoveBuilder("string_performance", "String Performance")
                        .withTiming(160, 14, 56)
                        .withDamage(14.0f)
                        .withDashSpeed(16.0f)
                        .withRange(16.0f)
                        .withKnockback(0f)
                        .withBreathCost(40.0f)
                        .withHitStun(30)
                        .withHitboxSize(7f)
                        .withDescription("Multi-segment dash that strikes everything along a 16-block path.")
                        .withAttack(StringPerformanceAttack::new)
                );
    }

    @Override
    public boolean canPerformMoves(LivingEntity entity) {
        return super.canPerformMoves(entity) && requireDualKatanas(entity);
    }

    @Override
    public int getMoveCount() {
        return 3;
    }

    /**
     * Breathing movesets manage their own pacing inside the attack classes — they shouldn't
     * eat the global STUNNED effect that the default click-handler would otherwise apply.
     */
    @Override
    protected boolean shouldAutoStunClickMoves() {
        return false;
    }

    @Override
    public boolean handleRightClick(LivingEntity entity, boolean isCrouching) {
        if (!canPerformMoves(entity)) return true;
        return super.handleRightClick(entity, isCrouching);
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
                    Long cooldownEnd = cooldowns.get(moveIndex);
                    if (cooldownEnd != null) {
                        long remaining = (cooldownEnd - entity.level().getGameTime()) / 20;
                        EntityResources.sendMessage(entity, Component.literal(config.getDisplayName() + " on cooldown! " + remaining + "s remaining")
                                        .withStyle(style -> style.withColor(0xFF5555)),
                                true
                        );
                    }
                }
            }
            return;
        }

        MoveConfiguration config = getMove(moveIndex);
        if (config != null) {
            float breathCost = config.getBreathCostOrDefault(0.0f);

            // Small buffer to prevent race conditions
            if (breathCost > 0 && !EntityResources.hasBreath(entity, breathCost + 0.1f)) {
                EntityResources.sendMessage(entity, Component.literal("Not enough breath for " + config.getDisplayName() + "!")
                                .withStyle(style -> style.withColor(0xFF3333)),
                        true
                );
                return;
            }
        }

        executingMove.put(entity.getUUID(), true);
        super.performMove(entity, moveIndex);

        boolean moveExecuted = !executingMove.getOrDefault(entity.getUUID(), false);
        executingMove.remove(entity.getUUID());

        if (moveExecuted && config != null) {
            setMoveCooldown(entity, moveIndex);

            if (!entity.level().isClientSide && entity instanceof ServerPlayer serverPlayer
                    && config.getCooldownOrDefault(0) > 0) {
                CooldownDisplayPacket.sendToClient(serverPlayer, "sound_breathing", config);
            }
        }
    }

    private boolean canUseMove(LivingEntity entity, int moveIndex) {
        MoveConfiguration config = getMove(moveIndex);
        if (config == null || config.getCooldownOrDefault(0) <= 0) return true;

        Map<Integer, Long> cooldowns = entityCooldowns.get(entity.getUUID());
        if (cooldowns == null) return true;

        Long cooldownEnd = cooldowns.get(moveIndex);
        if (cooldownEnd == null) return true;

        return entity.level().getGameTime() >= cooldownEnd;
    }

    private void setMoveCooldown(LivingEntity entity, int moveIndex) {
        MoveConfiguration config = getMove(moveIndex);
        if (config == null || config.getCooldownOrDefault(0) <= 0) return;

        long cooldownEnd = entity.level().getGameTime() + config.getCooldownOrDefault(0);
        entityCooldowns.computeIfAbsent(entity.getUUID(), k -> new HashMap<>())
                .put(moveIndex, cooldownEnd);
    }

    @Override
    public int getRightClickMoveIndex(boolean isCrouching) {
        return isCrouching ? -2 : -1; // Not in attack wheel, handled separately
    }

    @Override
    public String getRightClickMoveName() {
        return "Tempo Breaker";
    }

    @Override
    public String getCrouchRightClickMoveName() {
        return "Rhythmic Step";
    }

    @Override
    public void onMovePerformed(LivingEntity entity, int moveIndex, boolean isCrouching) {
    }

    public static void resetCooldowns(LivingEntity entity) {
        entityCooldowns.remove(entity.getUUID());
    }

    public static void cleanupPlayer(LivingEntity entity) {
        entityCooldowns.remove(entity.getUUID());
        executingMove.remove(entity.getUUID());
        SoundBreathingAttackBase.resetCombo(entity.getUUID());
    }

    public static void resetPlayerCombo(LivingEntity entity) {
        SoundBreathingAttackBase.resetCombo(entity.getUUID());
    }
}
