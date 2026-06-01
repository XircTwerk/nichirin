package com.xirc.nichirin.common.attack.moveset.breathing;

import com.xirc.nichirin.common.attack.MoveExecutor;
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
    private static final ThreadLocal<SoundBreathingMoveset> CURRENT_MOVESET = new ThreadLocal<>();

    public SoundBreathingMoveset() {
        super("sound_breathing", "Sound Breathing", MovesetType.BREATHING, createBuilder());
    }

    private static MovesetBuilder createBuilder() {
        return new MovesetBuilder()
                .withIdleAnimation("nichirin:sound_idle")
                .withSpeedMultiplier(1.1f)

                .withRightClickMove(new MoveBuilder("tempo_breaker", "Tempo Breaker")
                        .withAnimation("nichirin:tempo_breaker", 8)
                        .withTiming(0, 8, 42)
                        .withDamage(0f) // explosion is what deals the damage
                        .withRange(5.0f)
                        .withKnockback(0.8f)
                        .withBreathCost(20.0f)
                        .withHitStun(10)
                        .withHitboxSize(3.0f)
                        .withDescription("Wide sweep that triggers a delayed explosion dealing area damage.")
                        .withAction(entity -> {
                            TempoBreakerAttack attack = new TempoBreakerAttack();
                            SoundBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) attack.configure(moveset.getRightClickConfiguration());
                            MoveExecutor.executeAttack(entity, attack, "sound_breathing", "tempo_breaker");
                        })
                )

                .withCrouchRightClickMove(new MoveBuilder("rhythmic_step", "Rhythmic Step")
                        .withAnimation("nichirin:rhythmic_step", 9)
                        .withTiming(0, 0, 14)
                        .withDamage(8.0f)
                        .withDashSpeed(4.0f)
                        .withRange(4.0f)
                        .withKnockback(0.5f)
                        .withBreathCost(25.0f)
                        .withHitStun(15)
                        .withHitboxSize(3.0f)
                        .withDescription("Short dash that damages enemies you pass through.")
                        .withAction(entity -> {
                            RhythmicStepAttack attack = new RhythmicStepAttack();
                            SoundBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) attack.configure(moveset.getCrouchRightClickConfiguration());
                            MoveExecutor.executeAttack(entity, attack, "sound_breathing", "rhythmic_step");
                        })
                )

                // First Form: Roar - AOE slam (INDEX 0 in wheel)
                .withMove(new MoveBuilder("roar", "Roar")
                        .withAnimation("nichirin:roar", 10)
                        .withTiming(160, 50, 14)
                        .withDamage(20.0f)
                        .withRange(13.5f)
                        .withKnockback(0.3f)
                        .withBreathCost(25.0f)
                        .withHitStun(20)
                        .withHitboxSize(13.5f)
                        .withDescription("AOE slam that hits all enemies in a large radius.")
                        .withAction(entity -> {
                            RoarAttack attack = new RoarAttack();
                            SoundBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) attack.configure(moveset.getMove(0));
                            MoveExecutor.executeAttack(entity, attack, "sound_breathing", "roar");
                        })
                )

                // Fourth Form: Constant Resounding Slashes - 360° defense (INDEX 1 in wheel)
                .withMove(new MoveBuilder("constant_resounding_slashes", "Constant Resounding Slashes")
                        .withAnimation("nichirin:constant_resounding_slashes", 12)
                        .withTiming(180, 5, 70)
                        .withDamage(6.0f)
                        .withRange(20.0f)
                        .withKnockback(0f)
                        .withBreathCost(25.0f)
                        .withHitStun(40)
                        .withHitboxSize(12.25f)
                        .withDescription("Spinning 360° attack that hits all nearby enemies multiple times.")
                        .withAction(entity -> {
                            ConstantResoundingSlashesAttack attack = new ConstantResoundingSlashesAttack();
                            SoundBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) attack.configure(moveset.getMove(1));
                            MoveExecutor.executeAttack(entity, attack, "sound_breathing", "constant_resounding_slashes");
                        })
                )

                // Fifth Form: String Performance - Multi-segment dash (INDEX 2 in wheel)
                .withMove(new MoveBuilder("string_performance", "String Performance")
                        .withAnimation("nichirin:string_performance", 15)
                        .withTiming(160, 14, 56)
                        .withDamage(14.0f)
                        .withDashSpeed(16.0f)
                        .withRange(16.0f)
                        .withKnockback(0f)
                        .withBreathCost(40.0f)
                        .withHitStun(30)
                        .withHitboxSize(7f)
                        .withDescription("Multi-segment dash that strikes everything along a 16-block path.")
                        .withAction(entity -> {
                            StringPerformanceAttack attack = new StringPerformanceAttack();
                            SoundBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) attack.configure(moveset.getMove(2));
                            MoveExecutor.executeAttack(entity, attack, "sound_breathing", "string_performance");
                        })
                );
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

    /**
     * Click moves run their .withAction lambda via AbstractMoveset's default handler, which doesn't
     * set CURRENT_MOVESET. The lambdas look up the moveset via that threadlocal — so wrap the
     * default handler to set it.
     */
    @Override
    public boolean handleRightClick(LivingEntity entity, boolean isCrouching) {
        if (!canPerformMoves(entity)) return true;
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
                    Long cooldownEnd = cooldowns.get(moveIndex);
                    if (cooldownEnd != null) {
                        long remaining = (cooldownEnd - entity.level().getGameTime()) / 20;
                        EntityResources.sendMessage(entity, Component.literal(config.getDisplayName() + " on cooldown! " + remaining + "s remaining")
                                        .withStyle(style -> style.withColor(0x9900FF)),
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
        CURRENT_MOVESET.set(this);

        try {
            super.performMove(entity, moveIndex);
        } finally {
            CURRENT_MOVESET.remove();
        }

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

    public static SoundBreathingMoveset getCurrentMoveset() {
        return CURRENT_MOVESET.get();
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
