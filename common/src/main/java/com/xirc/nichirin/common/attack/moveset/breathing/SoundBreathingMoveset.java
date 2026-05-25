package com.xirc.nichirin.common.attack.moveset.breathing;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.moves.breathing.sound.*;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.network.s2c.MovesetConfigSyncPacket;
import com.xirc.nichirin.common.network.util.CooldownDisplayPacket;
import com.xirc.nichirin.common.util.EntityResources;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.phys.AABB;

public class SoundBreathingMoveset extends AbstractMoveset {

    private static final Map<UUID, Map<Integer, Long>> entityCooldowns = new HashMap<>();
    private static final Map<UUID, Boolean> executingMove = new HashMap<>();
    private static final ThreadLocal<SoundBreathingMoveset> CURRENT_MOVESET = new ThreadLocal<>();

    public SoundBreathingMoveset() {
        super("sound_breathing", "Sound Breathing", MovesetType.BREATHING, createBuilder());
        captureInitialConfigs();
    }

    private void captureInitialConfigs() {
        createAndCaptureTempoBreakerConfig();
        createAndCaptureRhythmicStepConfig();
    }

    private void createAndCaptureTempoBreakerConfig() {
        MoveConfiguration tempConfig = new MoveBuilder("tempo_breaker", "Tempo Breaker")
                .withAnimation("nichirin:tempo_breaker", 8)
                .withTiming(0, 8, 60) // Extended duration to allow delayed explosions
                .withDamage(0f) //explosion is what deals the damage
                .withRange(5.0f) // Wide sweep range
                .withKnockback(0.8f) // Reduced from 1.2f - still too strong
                .withBreathCost(20.0f) // Moderate cost
                .withHitStun(10)
                .withHitboxSize(3.0f)
                .withDescription("Wide sweep that triggers a delayed explosion dealing area damage.")
                .build();
        this.captureRightClickConfig(tempConfig, false);
    }

    private void createAndCaptureRhythmicStepConfig() {
        MoveConfiguration tempConfig = new MoveBuilder("rhythmic_step", "Rhythmic Step")
                .withAnimation("nichirin:rhythmic_step", 9)
                .withTiming(0, 0, 20) // Fast dash with finishing duration
                .withDamage(12.0f) // Moderate damage but hits multiple times
                .withDashSpeed(4.0f) // 4 block dash (halved from 8)
                .withRange(4.0f) // Dash distance (halved from 8)
                .withKnockback(0.5f) // Light knockback during dash
                .withBreathCost(25.0f) // Mobility move cost
                .withHitStun(15) // Good stun for finishing slash
                .withHitboxSize(3.0f)
                .withDescription("Short dash that damages enemies you pass through.")
                .build();
        this.captureRightClickConfig(tempConfig, true);
    }

    private static MovesetBuilder createBuilder() {
        return new MovesetBuilder()
                .withIdleAnimation("nichirin:sound_idle")
                .withSpeedMultiplier(1.1f)

                // First Form: Roar - AOE slam (INDEX 0 in wheel)
                .withMove(new MoveBuilder("roar", "Roar")
                        .withAnimation("nichirin:roar", 10)
                        .withTiming(160, 50, 20) // 5 second cooldown, windup
                        .withDamage(23.0f) // Good AOE damage
                        .withRange(13.5f) // Tripled from 4.5f (4.5 * 3 = 13.5)
                        .withKnockback(0.3f) // Strong knockback
                        .withBreathCost(25.0f)
                        .withHitStun(10) // 0.5 second stun
                        .withHitboxSize(13.5f) // Full radius
                        .withDescription("AOE slam that hits all enemies in a large radius.")
                        .withAction(entity -> {
                            RoarAttack attack = new RoarAttack();
                            SoundBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(0));
                            }
                            MoveExecutor.executeAttack(entity, attack, "sound_breathing", "roar");
                        })
                )

                // Fourth Form: Constant Resounding Slashes - 360Â° defense (INDEX 1 in wheel)
                .withMove(new MoveBuilder("constant_resounding_slashes", "Constant Resounding Slashes")
                        .withAnimation("nichirin:constant_resounding_slashes", 12)
                        .withTiming(180, 5, 50)
                        .withDamage(10.0f)
                        .withRange(20.0f) // Increased from 5.5f (1.5x = 8.25f)
                        .withKnockback(0f) // Light knockback to keep enemies close
                        .withBreathCost(25.0f)
                        .withHitStun(10) // Brief stun per hit
                        .withHitboxSize(12.25f) // Full 360Â° radius
                        .withDescription("Spinning 360Â° attack that hits all nearby enemies multiple times.")
                        .withAction(entity -> {
                            ConstantResoundingSlashesAttack attack = new ConstantResoundingSlashesAttack();
                            SoundBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(1));
                            }
                            MoveExecutor.executeAttack(entity, attack, "sound_breathing", "constant_resounding_slashes");
                        })
                )

                // Fifth Form: String Performance - Multi-segment dash (INDEX 2 in wheel)
                .withMove(new MoveBuilder("string_performance", "String Performance")
                        .withAnimation("nichirin:string_performance", 15)
                        .withTiming(160, 14, 80) // 8 second cooldown, windup, 4s duration
                        .withDamage(22.0f) // High damage for finale
                        .withDashSpeed(16.0f) // 16 block total dash
                        .withRange(16.0f) // Dash distance
                        .withKnockback(0f) // Light knockback during dash
                        .withBreathCost(40.0f) // Expensive ultimate-style move
                        .withHitStun(20) // Good stun
                        .withHitboxSize(3.5f) // Wide chain hitbox
                        .withDescription("Multi-segment dash that strikes everything along a 16-block path.")
                        .withAction(entity -> {
                            StringPerformanceAttack attack = new StringPerformanceAttack();
                            SoundBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(2)); // Index 2 for String Performance
                            }
                            MoveExecutor.executeAttack(entity, attack, "sound_breathing", "string_performance");
                        })
                );
    }

    @Override
    public int getMoveCount() {
        return 3;
    }

    @Override
    public boolean handleRightClick(LivingEntity entity, boolean isCrouching) {
        if (isCrouching) {
            return executeRhythmicStep(entity);
        } else {
            return executeTempoBreaker(entity);
        }
    }

    private boolean executeTempoBreaker(LivingEntity entity) {
        triggerAnimation(entity, "tempo_breaker");
        TempoBreakerAttack attack = new TempoBreakerAttack();

        createAndCaptureTempoBreakerConfig();
        MoveConfiguration tempConfig = getRightClickConfiguration();

        if (!entity.level().isClientSide && entity instanceof ServerPlayer serverPlayer) {
            MovesetConfigSyncPacket packet = new MovesetConfigSyncPacket(
                    "sound_breathing",
                    this.getRightClickConfiguration(),
                    this.getCrouchRightClickConfiguration()
            );
            NichirinPacketRegistry.sendToPlayer(packet, serverPlayer);
        }

        attack.configure(tempConfig);
        MoveExecutor.executeAttack(entity, attack, "sound_breathing", "tempo_breaker");
        onMovePerformed(entity, -1, false);
        return true;
    }

    private boolean executeRhythmicStep(LivingEntity entity) {
        triggerAnimation(entity, "rhythmic_step");
        RhythmicStepAttack attack = new RhythmicStepAttack();

        createAndCaptureRhythmicStepConfig();
        MoveConfiguration tempConfig = getCrouchRightClickConfiguration();

        if (!entity.level().isClientSide && entity instanceof ServerPlayer serverPlayer) {
            MovesetConfigSyncPacket packet = new MovesetConfigSyncPacket(
                    "sound_breathing",
                    this.getRightClickConfiguration(),
                    this.getCrouchRightClickConfiguration()
            );
            NichirinPacketRegistry.sendToPlayer(packet, serverPlayer);
        }

        attack.configure(tempConfig);
        MoveExecutor.executeAttack(entity, attack, "sound_breathing", "rhythmic_step");
        onMovePerformed(entity, -2, true);
        return true;
    }

    @Override
    public void performMove(LivingEntity entity, int moveIndex) {
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

    private boolean hasTargetsInRange(LivingEntity entity, float range) {
        AABB searchBox = new AABB(
                entity.getX() - range, entity.getY() - range, entity.getZ() - range,
                entity.getX() + range, entity.getY() + range, entity.getZ() + range
        );

        List<LivingEntity> entities = entity.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != entity && entity.isAlive() && !entity.isSpectator());
        return !entities.isEmpty();
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
