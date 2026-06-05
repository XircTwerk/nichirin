package com.xirc.nichirin.common.attack.moveset.breathing;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.moves.breathing.mist.*;
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
import java.util.Map;
import java.util.UUID;

public class MistBreathingMoveset extends AbstractMoveset {

    private static final Map<UUID, Map<Integer, Long>> entityCooldowns = new HashMap<>();
    private static final Map<UUID, Boolean> executingMove = new HashMap<>();
    private static final ThreadLocal<MistBreathingMoveset> CURRENT_MOVESET = new ThreadLocal<>();

    public MistBreathingMoveset() {
        super("mist_breathing", "Mist Breathing", MovesetType.BREATHING, createBuilder());
        captureInitialConfigs();
    }

    private void captureInitialConfigs() {
        captureRightClickConfig(buildEightLayeredMistConfig(), false);
        captureRightClickConfig(buildLowCloudsConfig(), true);
    }

    private MoveConfiguration buildEightLayeredMistConfig() {
        return new MoveBuilder("eight_layered_mist", "Eight-Layered Mist")
                .withAnimation("nichirin:mist_rapid_slash", 6)
                .withTiming(0, 9, 17)
                .withDamage(1.0f)
                .withRange(2.5f)
                .withKnockback(0.05f)
                .withBreathCost(15.0f)
                .withHitStun(12)
                .withHitboxSize(2.5f)
                .withDescription("Eight rapid slashes that ignore immunity frames.")
                .build();
    }

    private MoveConfiguration buildLowCloudsConfig() {
        return new MoveBuilder("low_clouds_distant_haze", "Low Clouds, Distant Haze")
                .withAnimation("nichirin:mist_thrust", 7)
                .withTiming(0, 5, 6)
                .withDamage(7.0f)
                .withRange(1.5f)
                .withKnockback(0.3f)
                .withDashSpeed(12.0f)
                .withBreathCost(10.0f)
                .withHitStun(16)
                .withHitboxSize(1.5f)
                .withDescription("Lightning-fast thrusting lunge that pierces multiple enemies.")
                .build();
    }

    private static MovesetBuilder createBuilder() {
        return new MovesetBuilder()
                .withIdleAnimation("nichirin:mist_idle")
                .withSpeedMultiplier(1.2f) // Fast and fluid

                // Form 3: Scattering Mist Splash (INDEX 0 in wheel)
                .withMove(new MoveBuilder("scattering_mist_splash", "Scattering Mist Splash")
                        .withAnimation("nichirin:mist_spin", 10)
                        .withTiming(144, 8, 14)
                        .withDamage(10.0f)
                        .withRange(3.5f)
                        .withKnockback(0.4f)
                        .withBreathCost(22.0f)
                        .withHitStun(22)
                        .withHitboxSize(3.5f)
                        .withDescription("360° circular slash that deflects projectiles and knocks back enemies.")
                        .withAction(entity -> {
                            MistBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset == null) return;
                            ScatteringMistSplashAttack attack = new ScatteringMistSplashAttack();
                            attack.configure(moveset.getMove(0));
                            MoveExecutor.executeAttack(entity, attack, "mist_breathing", "scattering_mist_splash");
                        })
                )

                // Form 4: Shifting Flow Slash (INDEX 1 in wheel)
                .withMove(new MoveBuilder("shifting_flow_slash", "Shifting Flow Slash")
                        .withAnimation("nichirin:mist_dash_slash", 12)
                        .withTiming(96, 10, 8)
                        .withDamage(7.0f)
                        .withRange(18.0f)
                        .withKnockback(0.35f)
                        .withDashSpeed(18.0f)
                        .withBreathCost(28.0f)
                        .withHitStun(25)
                        .withHitboxSize(2.5f)
                        .withDescription("10-tick low-stance windup into an 18-block dash. Slashes all enemies in path and delivers a powerful finisher.")
                        .withAction(entity -> {
                            MistBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset == null) return;
                            ShiftingFlowSlashAttack attack = new ShiftingFlowSlashAttack();
                            attack.configure(moveset.getMove(1));
                            MoveExecutor.executeAttack(entity, attack, "mist_breathing", "shifting_flow_slash");
                        })
                )

                // Form 5: Sea of Clouds and Haze (INDEX 2 in wheel)
                .withMove(new MoveBuilder("sea_of_clouds_and_haze", "Sea of Clouds")
                        .withAnimation("nichirin:mist_zigzag", 13)
                        .withTiming(128, 8, 56)
                        .withDamage(6.0f)
                        .withRange(12.0f)
                        .withKnockback(0.25f)
                        .withDashSpeed(10.0f)
                        .withBreathCost(25.0f)
                        .withHitStun(15)
                        .withHitboxSize(7.5f)
                        .withDescription("5-hop zigzag charge with large hitboxes. Drags and slashes enemies into a powerful finisher.")
                        .withAction(entity -> {
                            MistBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset == null) return;
                            SeaOfCloudsAndHazeAttack attack = new SeaOfCloudsAndHazeAttack();
                            attack.configure(moveset.getMove(2));
                            MoveExecutor.executeAttack(entity, attack, "mist_breathing", "sea_of_clouds_and_haze");
                        })
                )

                // Form 6: Lunar Dispersing Mist (INDEX 3 in wheel)
                .withMove(new MoveBuilder("lunar_dispersing_mist", "Lunar Dispersing Mist")
                        .withAnimation("nichirin:mist_aerial", 14)
                        .withTiming(160, 10, 25)
                        .withDamage(6.0f)
                        .withRange(14.0f)
                        .withKnockback(0.35f)
                        .withDashSpeed(8.0f)
                        .withBreathCost(25.0f)
                        .withHitStun(20)
                        .withHitboxSize(3.0f)
                        .withDescription("Aerial charge with multiple slashes. Ends with a devastating vertical circular slash.")
                        .withAction(entity -> {
                            MistBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset == null) return;
                            LunarDispersingMistAttack attack = new LunarDispersingMistAttack();
                            attack.configure(moveset.getMove(3));
                            MoveExecutor.executeAttack(entity, attack, "mist_breathing", "lunar_dispersing_mist");
                        })
                )

                // Form 7: Obscuring Clouds (INDEX 4 in wheel)
                .withMove(new MoveBuilder("obscuring_clouds", "Obscuring Clouds")
                        .withAnimation("nichirin:mist_vanish", 16)
                        .withTiming(640, 5, 210)
                        .withDamage(2.0f)
                        .withRange(8.0f)
                        .withKnockback(0.0f)
                        .withBreathCost(55.0f)
                        .withHitStun(8)
                        .withHitboxSize(5.0f)
                        .withDescription("Become invisible and teleport around all enemies in 8 blocks for 5 seconds. 32-second cooldown.")
                        .withAction(entity -> {
                            MistBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset == null) return;
                            ObscuringCloudsAttack attack = new ObscuringCloudsAttack();
                            attack.configure(moveset.getMove(4));
                            MoveExecutor.executeAttack(entity, attack, "mist_breathing", "obscuring_clouds");
                        })
                );
    }

    @Override
    public int getMoveCount() {
        return 5;
    }

    @Override
    public boolean handleRightClick(LivingEntity entity, boolean isCrouching) {
        if (!canPerformMoves(entity)) return true;

        if (isCrouching) {
            return executeLowClouds(entity);
        } else {
            return executeEightLayeredMist(entity);
        }
    }

    private boolean executeEightLayeredMist(LivingEntity entity) {
        captureRightClickConfig(buildEightLayeredMistConfig(), false);
        MoveConfiguration tempConfig = getRightClickConfiguration();
        if (tempConfig == null) return false;
        if (!hasResourcesForMove(entity, tempConfig)) return true;

        syncConfigToClient(entity, tempConfig);

        triggerAnimation(entity, "eight_layered_mist");
        EightLayeredMistAttack attack = new EightLayeredMistAttack();
        attack.configure(tempConfig);
        MoveExecutor.executeAttack(entity, attack, "mist_breathing", "eight_layered_mist");
        onMovePerformed(entity, -1, false);
        return true;
    }

    private boolean executeLowClouds(LivingEntity entity) {
        captureRightClickConfig(buildLowCloudsConfig(), true);
        MoveConfiguration tempConfig = getCrouchRightClickConfiguration();
        if (tempConfig == null) return false;
        if (!hasResourcesForMove(entity, tempConfig)) return true;

        syncConfigToClient(entity, tempConfig);

        triggerAnimation(entity, "low_clouds_distant_haze");
        LowCloudsDistantHazeAttack attack = new LowCloudsDistantHazeAttack();
        attack.configure(tempConfig);
        MoveExecutor.executeAttack(entity, attack, "mist_breathing", "low_clouds_distant_haze");
        onMovePerformed(entity, -2, true);
        return true;
    }

    private void syncConfigToClient(LivingEntity entity, MoveConfiguration config) {
        if (!entity.level().isClientSide && entity instanceof ServerPlayer serverPlayer) {
            MovesetConfigSyncPacket packet = new MovesetConfigSyncPacket(
                    "mist_breathing",
                    getRightClickConfiguration(),
                    getCrouchRightClickConfiguration()
            );
            NichirinPacketRegistry.sendToPlayer(packet, serverPlayer);
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
                                        .withStyle(style -> style.withColor(0xB0C4DE)),
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
                CooldownDisplayPacket.sendToClient(serverPlayer, "mist_breathing", config);
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
        return "Eight-Layered Mist";
    }

    @Override
    public String getCrouchRightClickMoveName() {
        return "Low Clouds, Distant Haze";
    }

    @Override
    public void onMovePerformed(LivingEntity entity, int moveIndex, boolean isCrouching) {}

    public static MistBreathingMoveset getCurrentMoveset() {
        return CURRENT_MOVESET.get();
    }

    public static void resetCooldowns(LivingEntity entity) {
        entityCooldowns.remove(entity.getUUID());
    }

    public static void cleanupPlayer(LivingEntity entity) {
        entityCooldowns.remove(entity.getUUID());
        executingMove.remove(entity.getUUID());
    }
}
