package com.xirc.nichirin.common.attack.moveset.breathing;

import com.xirc.nichirin.common.attack.moves.breathing.insect.*;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.network.util.CooldownDisplayPacket;
import com.xirc.nichirin.common.util.EntityResources;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Insect Breathing moveset.
 * Right-click: Quick Sting (rapid thrust)
 * Crouch + Right-click: Bee Sting (short dash strike)
 */
public class InsectBreathingMoveset extends AbstractMoveset {

    private static final Map<UUID, Map<Integer, Long>> entityCooldowns = new HashMap<>();
    private static final Map<UUID, Boolean> executingMove = new HashMap<>();

    public InsectBreathingMoveset() {
        super("insect_breathing", "Insect Breathing", MovesetType.BREATHING, createBuilder());
    }

    private static MovesetBuilder createBuilder() {
        return new MovesetBuilder()
                .withIdleAnimation("nichirin:insect_idle")
                .withSpeedMultiplier(1.3f)

                .withMove(new MoveBuilder("quick_sting", "Quick Sting")
                        .withAnimation("nichirin:quick_sting", 6)
                        .withTiming(5, 3, 14)
                        .withDamage(2.0f)
                        .withRange(2.5f)
                        .withKnockback(0f)
                        .withBreathCost(10.0f)
                        .withHitStun(15)
                        .withHitboxSize(0.5f)
                        .withDescription("Fast low-damage thrust that poisons on hit.")
                        .asRightClick()
                        .withAttack(QuickStingAttack::new)
                )

                .withMove(new MoveBuilder("bee_sting", "Bee Sting")
                        .withAnimation("nichirin:bee_sting", 9)
                        .withTiming(0, 6, 9)
                        .withDamage(5.0f)
                        .withDashSpeed(6.0f)
                        .withRange(6.0f)
                        .withKnockback(0.1f)
                        .withBreathCost(20.0f)
                        .withHitStun(10)
                        .withHitboxSize(2.0f)
                        .withDescription("Short dash strike that deals moderate damage.")
                        .asCrouchRightClick()
                        .withAttack(BeeStingAttack::new)
                )

                // First Form: Butterfly - Precision dash strike (INDEX 0 in wheel)
                .withMove(new MoveBuilder("butterfly", "Butterfly")
                        .withAnimation("nichirin:butterfly", 8)
                        .withTiming(120, 8, 28)
                        .withDamage(11.0f)
                        .withDashSpeed(5.0f)
                        .withRange(10.0f)
                        .withKnockback(0.3f)
                        .withBreathCost(20.0f)
                        .withHitStun(30)
                        .withHitboxSize(2f)
                        .withDescription("Lock-on dash strike that deals high single-target damage.")
                        .withAttack(ButterflyAttack::new)
                )

                // Third Form: Dragonfly - Multi-hit lock-on (INDEX 1 in wheel)
                .withMove(new MoveBuilder("dragonfly", "Dragonfly")
                        .withAnimation("nichirin:dragonfly", 12)
                        .withTiming(180, 15, 21)
                        .withDamage(2.0f)
                        .withRange(6.0f)
                        .withKnockback(0.0f)
                        .withBreathCost(25.0f)
                        .withHitStun(5)
                        .withHitboxSize(2.0f)
                        .withDescription("Lock-on multi-hit that lands 6 rapid strikes on a single target.")
                        .withAttack(DragonflyAttack::new)
                )

                // Fourth Form: Centipede - Zigzag dash finisher (INDEX 2 in wheel)
                .withMove(new MoveBuilder("centipede", "Centipede")
                        .withAnimation("nichirin:centipede", 15)
                        .withTiming(240, 20, 35)
                        .withDamage(14.0f)
                        .withDashSpeed(4.0f)
                        .withRange(2.0f)
                        .withKnockback(0.8f)
                        .withBreathCost(45.0f)
                        .withHitStun(40)
                        .withHitboxSize(2.5f)
                        .withDescription("Zigzag multi-dash finisher with high damage and strong knockback.")
                        .withAttack(CentipedeAttack::new)
                );
    }

    @Override
    public int getMoveCount() {
        return 3;
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
                CooldownDisplayPacket.sendToClient(serverPlayer, "insect_breathing", config);
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
        return "Quick Sting";
    }

    @Override
    public String getCrouchRightClickMoveName() {
        return "Bee Sting";
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
