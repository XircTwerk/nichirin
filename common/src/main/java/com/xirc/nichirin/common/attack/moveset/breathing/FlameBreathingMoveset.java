package com.xirc.nichirin.common.attack.moveset.breathing;

import com.xirc.nichirin.common.attack.moves.breathing.flame.*;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.network.util.CooldownDisplayPacket;
import com.xirc.nichirin.common.util.EntityResources;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FlameBreathingMoveset extends AbstractMoveset {

    private static final Map<UUID, Map<Integer, Long>> entityCooldowns = new HashMap<>();
    private static final Map<UUID, Boolean> executingMove = new HashMap<>();

    public FlameBreathingMoveset() {
        super("flame_breathing", "Flame Breathing", MovesetType.BREATHING, createBuilder());
    }

    private static MovesetBuilder createBuilder() {
        return new MovesetBuilder()
                .withIdleAnimation("nichirin:flame_idle")
                .withSpeedMultiplier(1.15f)

                .withMove(new MoveBuilder("pommel_slash", "Pommel Slash")
                        .withTiming(0, 5, 18)
                        .withDamage(0.5f)
                        .withRange(2.5f)
                        .withKnockback(0f)
                        .withBreathCost(15.0f)
                        .withHitStun(8)
                        .withHitboxSize(2.0f)
                        .withDescription("6 rapid slashes in quick succession.")
                        .asRightClick()
                        .withAttack(PommelSlashAttack::new)
                )

                .withMove(new MoveBuilder("unknowing_fire_quick", "Unknowing Fire")
                        .withTiming(0, 6, 11)
                        .withDamage(10.0f)
                        .withRange(3.0f)
                        .withKnockback(0.4f)
                        .withBreathCost(40.0f)
                        .withHitStun(20)
                        .withHitboxSize(2.0f)
                        .withDescription("Overhead slam with high damage and short windup.")
                        .asCrouchRightClick()
                        .withAttack(UnknowingFireAttack::new)
                )

                // INDEX 0: Rising Scorching Sun — upward arc, launches enemies
                .withMove(new MoveBuilder("rising_scorching_sun", "Scorching Sun")
                        .withTiming(100, 12, 18)
                        .withDamage(9.0f)
                        .withRange(6.0f)
                        .withKnockback(0.6f)
                        .withBreathCost(20.0f)
                        .withHitStun(20)
                        .withHitboxSize(5f)
                        .withSlam()
                        .withArmor(3)
                        .withDescription("Upward arc slash that launches enemies into the air.")
                        .withAttack(RisingScorchingSunAttack::new)
                )

                // INDEX 1: Blazing Universe — charged downward strike, explodes on impact
                .withMove(new MoveBuilder("blazing_universe", "Blazing Universe")
                        .withTiming(160, 13, 35)
                        .withDamage(5.0f)
                        .withRange(4.0f)
                        .withKnockback(0.6f)
                        .withBreathCost(30.0f)
                        .withHitStun(35)
                        .withHitboxSize(3.0f)
                        .withDescription("Charged downward strike that explodes on impact.")
                        .withAttack(BlazingUniverseAttack::new)
                )

                // INDEX 2: Blooming Flame Undulation — 360° defense
                .withMove(new MoveBuilder("blooming_flame_undulation", "Blooming Flame")
                        .withTiming(140, 11, 25)
                        .withDamage(5.0f)
                        .withRange(3.5f)
                        .withKnockback(0f)
                        .withBreathCost(25.0f)
                        .withHitStun(15)
                        .withHitboxSize(3.5f)
                        .withDescription("Full 360° slash hitting all nearby enemies.")
                        .withAttack(BloomingFlameUndulationAttack::new)
                )

                // INDEX 3: Flame Tiger — dashing multi-hit strike
                .withMove(new MoveBuilder("flame_tiger", "Flame Tiger")
                        .withTiming(120, 10, 28)
                        .withDamage(11.0f)
                        .withDashSpeed(35.0f)
                        .withRange(16.0f)
                        .withKnockback(0.2f)
                        .withBreathCost(50.0f)
                        .withHitStun(10)
                        .withHitboxSize(2.0f)
                        .withDescription("Dashing multi-hit strike in a straight line.")
                        .withAttack(FlameTigerAttack::new)
                )

                // INDEX 4: Rengoku
                .withMove(new MoveBuilder("rengoku", "Rengoku")
                        .withTiming(600, 90, 42)
                        .withDamage(30.0f)
                        .withDashSpeed(50.0f)
                        .withRange(20.0f)
                        .withKnockback(0f)
                        .withBreathCost(75.0f)
                        .withHitStun(80)
                        .withHitboxSize(4.0f)
                        .withDescription("Massive-damage dash through enemies. 30-second cooldown.")
                        .withAttack(RengokuAttack::new)
                );
    }

    @Override
    public boolean canPerformMoves(LivingEntity entity) {
        return super.canPerformMoves(entity) && hasSingleKatana(entity);
    }

    @Override
    public int getMoveCount() {
        return 5;
    }

    /** Breathing movesets pace themselves inside their attack classes — skip auto-stun. */
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
                        EntityResources.sendMessage(entity,
                                Component.literal(config.getDisplayName() + " on cooldown! " + remaining + "s remaining")
                                        .withStyle(style -> style.withColor(0xFF5555)), true);
                    }
                }
            }
            return;
        }

        MoveConfiguration config = getMove(moveIndex);
        if (config != null) {
            float breathCost = config.getBreathCostOrDefault(0.0f);

            if (breathCost > 0 && !EntityResources.hasBreath(entity, breathCost + 0.1f)) {
                EntityResources.sendMessage(entity,
                        Component.literal("Not enough breath for " + config.getDisplayName() + "!")
                                .withStyle(style -> style.withColor(0xFF3333)), true);
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
                CooldownDisplayPacket.sendToClient(serverPlayer, "flame_breathing", config);
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
        return isCrouching ? -2 : -1;
    }

    @Override
    public String getRightClickMoveName() {
        return "Pommel Slash";
    }

    @Override
    public String getCrouchRightClickMoveName() {
        return "Unknowing Fire";
    }

    @Override
    public void onMovePerformed(LivingEntity entity, int moveIndex, boolean isCrouching) {}

    public static void resetCooldowns(LivingEntity entity) {
        entityCooldowns.remove(entity.getUUID());
    }

    public static void cleanupPlayer(LivingEntity entity) {
        entityCooldowns.remove(entity.getUUID());
        executingMove.remove(entity.getUUID());
    }
}
