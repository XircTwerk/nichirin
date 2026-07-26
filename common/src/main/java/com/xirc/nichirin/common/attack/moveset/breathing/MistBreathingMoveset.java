package com.xirc.nichirin.common.attack.moveset.breathing;

import com.xirc.nichirin.common.attack.moves.breathing.mist.*;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.network.util.CooldownDisplayPacket;
import com.xirc.nichirin.common.util.EntityResources;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MistBreathingMoveset extends AbstractMoveset {

    private static final Map<UUID, Map<Integer, Long>> entityCooldowns = new HashMap<>();
    private static final Map<UUID, Boolean> executingMove = new HashMap<>();

    public MistBreathingMoveset() {
        super("mist_breathing", "Mist Breathing", MovesetType.BREATHING, createBuilder());
    }

    private static MovesetBuilder createBuilder() {
        return new MovesetBuilder()
                .withIdleAnimation("nichirin:mist_idle")
                .withSpeedMultiplier(1.2f)

                //Mist breathing second form
                .withMove(new MoveBuilder("eight_layered_mist", "Eight-Layered Mist")
                        .withTiming(0, 9, 17)
                        .withDamage(1.0f)
                        .withRange(2.5f)
                        .withKnockback(0.05f)
                        .withBreathCost(15.0f)
                        .withHitStun(12)
                        .withHitboxSize(4.5f)
                        .withDescription("Eight rapid slashes that ignore immunity frames.")
                        .asRightClick()
                        .withAttack(EightLayeredMistAttack::new)
                )

                //First form
                .withMove(new MoveBuilder("low_clouds_distant_haze", "Low Clouds, Distant Haze")
                        .withTiming(0, 5, 12)
                        .withDamage(7.0f)
                        .withRange(4.0f)
                        .withKnockback(0.3f)
                        .withDashSpeed(28.0f)
                        .withBreathCost(10.0f)
                        .withHitStun(16)
                        .withHitboxSize(2.5f)
                        .withDescription("Lightning-fast thrusting lunge that pierces multiple enemies.")
                        .asCrouchRightClick()
                        .withAttack(LowCloudsDistantHazeAttack::new)
                )

                // Form 3: Scattering Mist Splash (INDEX 0 in wheel)
                .withMove(new MoveBuilder("scattering_mist_splash", "Scattering Mist Splash")
                        .withTiming(144, 8, 14)
                        .withDamage(10.0f)
                        .withRange(3.5f)
                        .withKnockback(0.4f)
                        .withBreathCost(22.0f)
                        .withHitStun(22)
                        .withHitboxSize(3.5f)
                        .withDescription("360° circular slash that deflects projectiles and knocks back enemies.")
                        .withAttack(ScatteringMistSplashAttack::new)
                )

                // Form 4: Shifting Flow Slash (INDEX 1 in wheel)
                .withMove(new MoveBuilder("shifting_flow_slash", "Shifting Flow Slash")
                        // Dash tuned to match Sound Breathing's Rhythmic Step (crouch-M2):
                        // dashSpeed 4 blocks/tick over a 14-tick window = the same smooth glide.
                        .withTiming(96, 10, 14)
                        .withDamage(7.0f)
                        .withRange(9.0f)
                        .withKnockback(0.35f)
                        .withDashSpeed(4.0f)
                        .withBreathCost(28.0f)
                        .withHitStun(25)
                        .withHitboxSize(2.5f)
                        .withDescription("10-tick low-stance windup into a 9-block dash. Slashes all enemies in path and delivers a powerful finisher.")
                        .withAttack(ShiftingFlowSlashAttack::new)
                )

                // Form 5: Sea of Clouds and Haze (INDEX 2 in wheel)
                .withMove(new MoveBuilder("sea_of_clouds_and_haze", "Sea of Clouds")
                        .withTiming(128, 8, 56)
                        .withDamage(4.0f)
                        .withRange(12.0f)
                        .withKnockback(0.25f)
                        .withDashSpeed(5.0f)
                        .withBreathCost(25.0f)
                        .withHitStun(25)
                        .withHitboxSize(7.5f)
                        .withDescription("5-hop zigzag charge with large hitboxes. Drags and slashes enemies into a powerful finisher.")
                        .withAttack(SeaOfCloudsAndHazeAttack::new)
                )

                // Form 6: Lunar Dispersing Mist (INDEX 3 in wheel)
                .withMove(new MoveBuilder("lunar_dispersing_mist", "Lunar Dispersing Mist")
                        .withTiming(160, 10, 25)
                        .withDamage(6.0f)
                        .withRange(14.0f)
                        .withKnockback(0.35f)
                        .withDashSpeed(4.0f)
                        .withBreathCost(25.0f)
                        .withHitStun(20)
                        .withHitboxSize(3.0f)
                        .withDescription("Aerial charge with multiple slashes. Ends with a devastating vertical circular slash.")
                        .withAttack(LunarDispersingMistAttack::new)
                )

                // Form 7: Obscuring Clouds (INDEX 4 in wheel)
                .withMove(new MoveBuilder("obscuring_clouds", "Obscuring Clouds")
                        .withTiming(640, 5, 210)
                        .withDamage(4.0f)
                        .withRange(8.0f)
                        .withKnockback(0.0f)
                        .withBreathCost(55.0f)
                        .withHitStun(8)
                        .withHitboxSize(5.0f)
                        .withDescription("Become invisible and teleport around all enemies in 8 blocks for 5 seconds. 32-second cooldown.")
                        .withAttack(ObscuringCloudsAttack::new)
                );
    }

    @Override
    public boolean canPerformMoves(LivingEntity entity) {
        return super.canPerformMoves(entity) && requireSingleKatana(entity);
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

    public static void resetCooldowns(LivingEntity entity) {
        entityCooldowns.remove(entity.getUUID());
    }

    public static void cleanupPlayer(LivingEntity entity) {
        entityCooldowns.remove(entity.getUUID());
        executingMove.remove(entity.getUUID());
    }
}
