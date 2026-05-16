package com.xirc.nichirin.common.attack.moveset.breathing;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.moves.breathing.water.*;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.network.s2c.MovesetConfigSyncPacket;
import com.xirc.nichirin.common.util.EntityResources;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Right-click: Water Surface Slash combo (3-stage combo system)
// Crouch + Right-click: Water Wheel (lunging wheel attack)
public class WaterBreathingMoveset extends AbstractMoveset {

    private static final Map<UUID, Map<Integer, Long>> entityCooldowns = new HashMap<>();
    private static final Map<UUID, Boolean> executingMove = new HashMap<>();
    private static final Map<UUID, ComboState> playerComboStates = new HashMap<>();
    private static final ThreadLocal<WaterBreathingMoveset> CURRENT_MOVESET = new ThreadLocal<>();

    private static class ComboState {
        int currentStage = 0;
        long lastAttackTime = 0;
        long comboWindow = 1000; // 1 second window to continue combo

        boolean canContinueCombo() {
            return System.currentTimeMillis() - lastAttackTime <= comboWindow;
        }

        void updateAttackTime() {
            lastAttackTime = System.currentTimeMillis();
        }

        void reset() {
            currentStage = 0;
            lastAttackTime = 0;
        }

        void nextStage() {
            currentStage++;
            updateAttackTime();
        }
    }

    public WaterBreathingMoveset() {
        super("water_breathing", "Water Breathing", MovesetType.BREATHING, createBuilder());
        captureInitialConfigs();
    }

    private void captureInitialConfigs() {
        createAndCaptureWaterSurfaceSlashConfig();
        createAndCaptureWaterWheelConfig();
    }

    private void createAndCaptureWaterSurfaceSlashConfig() {
        MoveConfiguration tempConfig = new MoveBuilder("water_surface_slash", "Water Surface Slash")
                .withAnimation("nichirin:water_surface_slash", 6)
                .withTiming(0, 0, 18)
                .withDamage(5.0f)
                .withRange(3.5f)
                .withKnockback(0f)
                .withBreathCost(8.0f)
                .withHitStun(20)
                .withHitboxSize(3.0f)
                .withDescription("3-hit combo ending in a heavy slam.")
                .build();
        this.captureRightClickConfig(tempConfig, false);
    }

    private void createAndCaptureWaterWheelConfig() {
        MoveConfiguration tempConfig = new MoveBuilder("water_wheel", "Water Wheel")
                .withAnimation("nichirin:water_wheel", 10)
                .withTiming(0, 10, 30)
                .withDamage(6.0f)
                .withRange(4.0f)
                .withKnockback(0.1f)
                .withBreathCost(18.0f)
                .withHitStun(5)
                .withHitboxSize(3.5f)
                .withDashSpeed(4.0f)
                .withDescription("Lunging spinning slash through enemies.")
                .build();
        this.captureRightClickConfig(tempConfig, true);
    }

    private static MovesetBuilder createBuilder() {
        return new MovesetBuilder()
                .withIdleAnimation("nichirin:water_idle")
                .withSpeedMultiplier(1.1f)

                // Third Form: Flowing Dance - Empowerment and trail attack (INDEX 0)
                .withMove(new MoveBuilder("flowing_dance", "Flowing Dance")
                        .withAnimation("nichirin:flowing_dance", 12)
                        .withTiming(240, 15, 60) // 6 second cooldown, 0.75s windup, 3s duration
                        .withDamage(4.0f) // Continuous damage
                        .withRange(3.0f) // Close range continuous
                        .withKnockback(0.05f) // Very light knockback
                        .withBreathCost(25.0f)
                        .withHitStun(3) // Very short for continuous hits
                        .withHitboxSize(2.5f)
                        .withDescription("Continuous attack stance hitting nearby enemies for several seconds.")
                        .withAction(entity -> {
                            FlowingDanceAttack attack = new FlowingDanceAttack();
                            WaterBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(0));
                            }
                            MoveExecutor.executeAttack(entity, attack, "water_breathing", "flowing_dance");
                        })
                )

                // Fourth Form: Striking Tide - Omnidirectional slashes (INDEX 1)
                .withMove(new MoveBuilder("striking_tide", "Striking Tide")
                        .withAnimation("nichirin:striking_tide", 14)
                        .withTiming(360, 12, 40) // 7 second cooldown, windup, 2s execution
                        .withDamage(10.0f) // Good damage for 360° attack
                        .withRange(4.5f) // Large omnidirectional range
                        .withKnockback(0.4f)
                        .withBreathCost(25.0f)
                        .withHitStun(8)
                        .withHitboxSize(4.5f) // Full radius
                        .withDescription("360° slash hitting all surrounding enemies at once.")
                        .withAction(entity -> {
                            StrikingTideAttack attack = new StrikingTideAttack();
                            WaterBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(1));
                            }
                            MoveExecutor.executeAttack(entity, attack, "water_breathing", "striking_tide");
                        })
                )

                // Fifth Form: Blessed Rain After the Drought - Ultimate precision dash (INDEX 2)
                .withMove(new MoveBuilder("blessed_rain", "Blessed Rain")
                        .withAnimation("nichirin:blessed_rain", 18)
                        .withTiming(500, 9, 25) // 10 second cooldown, windup, 1.25s duration
                        .withDamage(20.0f) // Drops half a health bar
                        .withRange(8.0f) // Long dash range
                        .withKnockback(0.8f)
                        .withBreathCost(45.0f)
                        .withHitStun(30)
                        .withHitboxSize(2.0f) // Minimum hitbox size
                        .withDashSpeed(12.0f) // Fast dash
                        .withDescription("High-speed precision dash strike dealing 20 damage.")
                        .withAction(entity -> {
                            BlessedRainAttack attack = new BlessedRainAttack();
                            WaterBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(2));
                            }
                            MoveExecutor.executeAttack(entity, attack, "water_breathing", "blessed_rain");
                        })
                )

                // Sixth Form: Whirlpool - Rising whirlpool attack (INDEX 3)
                .withMove(new MoveBuilder("whirlpool", "Whirlpool")
                        .withAnimation("nichirin:whirlpool", 15)
                        .withTiming(380, 14, 70) // 8 second cooldown, windup, 2.5s duration
                        .withDamage(3.5f) // Multi-hit spinning damage
                        .withRange(3.0f) // Whirlpool radius
                        .withKnockback(0.1f) // Light knockback, enemies spin around
                        .withBreathCost(20.0f)
                        .withHitStun(7) // Short for spinning effect
                        .withHitboxSize(3.0f)
                        .withDescription("Spinning attack that hits trapped enemies multiple times.")
                        .withAction(entity -> {
                            WhirlpoolAttack attack = new WhirlpoolAttack();
                            WaterBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(3));
                            }
                            MoveExecutor.executeAttack(entity, attack, "water_breathing", "whirlpool");
                        })
                )

                // Seventh Form: Drop Ripple Thrust - Shield and thrust attack (INDEX 4)
                .withMove(new MoveBuilder("drop_ripple_thrust", "Drop Ripple Thrust")
                        .withAnimation("nichirin:drop_ripple_thrust", 13)
                        .withTiming(300, 10, 35) // 5 second cooldown, 0.5s windup, 1.75s duration
                        .withDamage(10.0f) // Good thrust damage
                        .withRange(5.0f) // Thrust range
                        .withKnockback(0f)
                        .withBreathCost(15.0f)
                        .withHitStun(20)
                        .withHitboxSize(4.0f) // Wall of ripples
                        .withDescription("Forward thrust with long reach.")
                        .withAction(entity -> {
                            DropRippleThrustAttack attack = new DropRippleThrustAttack();
                            WaterBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(4));
                            }
                            MoveExecutor.executeAttack(entity, attack, "water_breathing", "drop_ripple_thrust");
                        })
                )

                // Eighth Form: Waterfall Basin - BIG ASS MULTIHIT (INDEX 5)
                .withMove(new MoveBuilder("waterfall_basin", "Waterfall Basin")
                        .withAnimation("nichirin:waterfall_basin", 16)
                        .withTiming(400, 16, 60) // 9 second cooldown, windup, 3s duration
                        .withDamage(2.5f) // High DPS multi-hit
                        .withRange(6.0f) // Large waterfall area
                        .withKnockback(0.0f) // Light knockback to keep enemies in waterfall
                        .withBreathCost(35.0f)
                        .withHitStun(8) // Medium stun for multi-hit
                        .withHitboxSize(6.0f) // BIG ASS HITBOX
                        .withDescription("Multi-hit barrage over a large area lasting several seconds.")
                        .withAction(entity -> {
                            WaterfallBasinAttack attack = new WaterfallBasinAttack();
                            WaterBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(5));
                            }
                            MoveExecutor.executeAttack(entity, attack, "water_breathing", "waterfall_basin");
                        })
                )

                // Ninth Form: Splashing Water Flow - Zigzag dash attack (INDEX 6)
                .withMove(new MoveBuilder("splashing_water_flow", "Splashing Water Flow")
                        .withAnimation("nichirin:splashing_water_flow", 14)
                        .withTiming(380, 10, 40) // 7 second cooldown, 0.5s windup, 2s duration
                        .withDamage(8.0f) // Good dash damage
                        .withRange(5.0f) // 10 block zigzag range
                        .withKnockback(0.4f)
                        .withBreathCost(25.0f)
                        .withHitStun(18)
                        .withHitboxSize(6f)
                        .withDashSpeed(4.0f) // Fast zigzag speed
                        .withDescription("Zigzag dash that hits enemies along the path.")
                        .withAction(entity -> {
                            SplashingWaterFlowAttack attack = new SplashingWaterFlowAttack();
                            WaterBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(6));
                            }
                            MoveExecutor.executeAttack(entity, attack, "water_breathing", "splashing_water_flow");
                        })
                )

                // Tenth Form: Constant Flux - 5-hit combo with dragon finisher (INDEX 7)
                .withMove(new MoveBuilder("constant_flux", "Constant Flux")
                        .withAnimation("nichirin:constant_flux", 20)
                        .withTiming(420, 20, 80) // 15 second cooldown, 1s windup, 4s duration
                        .withDamage(24.0f) // Strong combo damage
                        .withRange(5.0f) // Drag range
                        .withKnockback(0.2f) // Light knockback for dragging
                        .withBreathCost(50.0f)
                        .withHitStun(12)
                        .withHitboxSize(4.0f)
                        .withDescription("5-hit combo ending in a heavy finishing slash.")
                        .withAction(entity -> {
                            ConstantFluxAttack attack = new ConstantFluxAttack();
                            WaterBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(7));
                            }
                            MoveExecutor.executeAttack(entity, attack, "water_breathing", "constant_flux");
                        })
                )

                // Eleventh Form: Dead Calm - Auto-target AoE field (INDEX 8)
                .withMove(new MoveBuilder("dead_calm", "Dead Calm")
                        .withAnimation("nichirin:dead_calm", 17)
                        .withTiming(700, 12, 200) // 11 second cooldown, 1.25s windup, 10s duration
                        .withDamage(1.0f) // Persistent area damage
                        .withRange(12.0f) // Large persistent area
                        .withKnockback(0f)
                        .withBreathCost(55.0f)
                        .withHitStun(6)
                        .withHitboxSize(6.0f) // Large area field
                        .withDescription("Deploys a large auto-targeting field that damages nearby enemies for 10 seconds.")
                        .withAction(entity -> {
                            DeadCalmAttack attack = new DeadCalmAttack();
                            WaterBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(8));
                            }
                            MoveExecutor.executeAttack(entity, attack, "water_breathing", "dead_calm");
                        })
                );
    }

    @Override
    public int getMoveCount() {
        return 9;
    }

    @Override
    public boolean handleRightClick(LivingEntity entity, boolean isCrouching) {
        if (entity.hasEffect(NichirinEffectRegistry.STUNNED.get())) {
            return true;
        }

        if (isCrouching) {
            return executeWaterWheel(entity);
        } else {
            return executeWaterSurfaceSlashCombo(entity);
        }
    }

    private boolean executeWaterSurfaceSlashCombo(LivingEntity entity) {
        ComboState comboState = playerComboStates.computeIfAbsent(entity.getUUID(), k -> new ComboState());

        int nextStage;

        if (comboState.currentStage == 0 || !comboState.canContinueCombo()) {
            nextStage = 1;
            comboState.reset();
        } else {
            nextStage = comboState.currentStage + 1;
        }

        if (nextStage > 3) {
            comboState.reset();
            nextStage = 1;
        }

        boolean success = executeWaterSurfaceSlashStage(entity, nextStage);

        if (success) {
            comboState.currentStage = nextStage;
            comboState.updateAttackTime();

            if (nextStage == 3) {
                comboState.reset();
            }
        }

        return success;
    }

    private boolean executeWaterSurfaceSlashStage(LivingEntity entity, int stage) {
        String animName = switch (stage) {
            case 2 -> "water_surface_slash_2";
            case 3 -> "water_slam";
            default -> "water_surface_slash";
        };
        triggerAnimation(entity, animName);

        WaterSurfaceSlashAttack attack = new WaterSurfaceSlashAttack();
        attack.setComboStage(stage);

        MoveConfiguration config = createStageConfig(stage);

        createAndCaptureWaterSurfaceSlashConfig();
        if (!entity.level().isClientSide && entity instanceof ServerPlayer serverPlayer) {
            MovesetConfigSyncPacket packet = new MovesetConfigSyncPacket(
                    "water_breathing",
                    this.getRightClickConfiguration(),
                    this.getCrouchRightClickConfiguration()
            );
            NichirinPacketRegistry.sendToPlayer(packet, serverPlayer);
        }

        attack.configure(config);
        MoveExecutor.executeAttack(entity, attack, "water_breathing", "water_surface_slash_stage_" + stage);
        onMovePerformed(entity, -1, false);

        return true;
    }

    private MoveConfiguration createStageConfig(int stage) {
        MoveConfiguration baseConfig = getRightClickConfiguration();
        float baseDmg = baseConfig != null ? baseConfig.getDamageOrDefault(5.0f) : 5.0f;
        switch (stage) {
            case 1 -> {
                return new MoveBuilder("water_surface_slash_1", "Water Surface Slash I")
                        .withAnimation("nichirin:water_surface_slash", 6)
                        .withTiming(0, 0, 18)
                        .withDamage(baseDmg)
                        .withRange(3.5f)
                        .withKnockback(0f)
                        .withBreathCost(8.0f)
                        .withHitStun(20)
                        .withHitboxSize(3.0f)
                        .build();
            }
            case 2 -> {
                return new MoveBuilder("water_surface_slash_2", "Water Surface Slash II")
                        .withAnimation("nichirin:water_surface_slash_2", 6)
                        .withTiming(0, 0, 18)
                        .withDamage(baseDmg * 1.2f)
                        .withRange(3.5f)
                        .withKnockback(0f)
                        .withBreathCost(10.0f)
                        .withHitStun(22)
                        .withHitboxSize(3.0f)
                        .build();
            }
            case 3 -> {
                return new MoveBuilder("water_slam_finisher", "Water Slam")
                        .withAnimation("nichirin:water_slam", 10)
                        .withTiming(0, 0, 25)
                        .withDamage(baseDmg * 2.4f)
                        .withRange(4.0f)
                        .withKnockback(0.8f)
                        .withBreathCost(15.0f)
                        .withHitStun(30)
                        .withHitboxSize(4.0f)
                        .build();
            }
            default -> throw new IllegalArgumentException("Invalid stage: " + stage);
        }
    }

    private boolean executeWaterWheel(LivingEntity entity) {
        triggerAnimation(entity, "water_wheel");
        WaterWheelAttack attack = new WaterWheelAttack();

        createAndCaptureWaterWheelConfig();
        MoveConfiguration tempConfig = getCrouchRightClickConfiguration();

        if (!entity.level().isClientSide && entity instanceof ServerPlayer serverPlayer) {
            MovesetConfigSyncPacket packet = new MovesetConfigSyncPacket(
                    "water_breathing",
                    this.getRightClickConfiguration(),
                    this.getCrouchRightClickConfiguration()
            );
            NichirinPacketRegistry.sendToPlayer(packet, serverPlayer);
        }

        attack.configure(tempConfig);
        MoveExecutor.executeAttack(entity, attack, "water_breathing", "water_wheel");
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
                                        .withStyle(style -> style.withColor(0x4A90E2)),
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
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                buf.writeUtf(config.getDisplayName());
                buf.writeInt(config.getCooldownOrDefault(0));

                NetworkManager.sendToPlayer(serverPlayer, new ResourceLocation("nichirin", "cooldown_display"), buf);
            }
        }
    }

    public static WaterBreathingMoveset getCurrentMoveset() {
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
        return "Water Surface Slash";
    }

    @Override
    public String getCrouchRightClickMoveName() {
        return "Water Wheel";
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
        playerComboStates.remove(entity.getUUID());
    }
}