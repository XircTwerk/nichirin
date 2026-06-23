package com.xirc.nichirin.common.attack.moveset.demon;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.moves.demon.basic.DemonBiteAttack;
import com.xirc.nichirin.common.attack.moves.demon.basic.DemonDashStrikeAttack;
import com.xirc.nichirin.common.attack.moves.demon.basic.DemonGrabAttack;
import com.xirc.nichirin.common.attack.moves.demon.basic.DemonGutPunchAttack;
import com.xirc.nichirin.common.attack.moves.demon.basic.DemonKickAttack;
import com.xirc.nichirin.common.attack.moves.demon.basic.DemonSlashAttack;
import com.xirc.nichirin.common.attack.moves.demon.basic.DemonStompAttack;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.network.util.CooldownDisplayPacket;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Default demon moveset
 * - Left-click: Gut Punch
 * - Right-click: 2-stage Slash combo
 * - Crouch + Right-click: High Jump (then crouch again mid-air for Stomp)
 * - Wheel moves: Kick, Dashing Strike, Bite, Grab
 */
public class TempleDemonMoveset extends AbstractMoveset {

    // ONE declaration per move. Both the builder (so AbstractMoveset can resolve
    // no duplicated stats, no drift bugs.
    private static final MoveConfiguration GUT_PUNCH_CONFIG = new MoveBuilder("demon_gut_punch", "Gut Punch")
            .withAnimation("nichirin:demon_gut_punch", 6)
            .withTiming(15, 1, 7)
            .withDamage(6.0f)
            .withRange(2.0f)
            .withKnockback(0.1f)
            .withHitStun(15)
            .withHitboxSize(2.8f)
            .withDescription("Powerful close-range punch that stuns enemies")
            .asLeftClick()
            .build();

    private static final MoveConfiguration SLASH_1_CONFIG = new MoveBuilder("demon_slash", "Slash")
            .withAnimation("nichirin:demon_slash", 6)
            .withTiming(0, 0, 14)
            .withDamage(4.0f)
            .withRange(3.0f)
            .withKnockback(0f)
            .withHitStun(10)
            .withHitboxSize(2.0f)
            .withDescription("Basic claw slash - press again for finisher")
            .asRightClick()
            .build();

    private static final MoveConfiguration SLASH_2_CONFIG = new MoveBuilder("demon_slash_2", "Slash Finisher")
            .withAnimation("nichirin:demon_slash_2", 6)
            .withTiming(5, 0, 18)
            .withDamage(6.0f)
            .withRange(3.0f)
            .withKnockback(0.5f)
            .withHitStun(5)
            .withHitboxSize(2.2f)
            .withDescription("Finishing slash after the initial claw")
            .build();

    private static final MoveConfiguration HIGH_JUMP_CONFIG = new MoveBuilder("high_jump", "High Jump")
            .withAnimation("nichirin:demon_high_jump", 8)
            .withTiming(220, 0, 4)
            .withDescription("Launch into the air, crouch mid-air to stomp down")
            .asCrouchRightClick()
            .build();

    private static final MoveConfiguration STOMP_CONFIG = new MoveBuilder("demon_stomp", "Stomp")
            .withAnimation("nichirin:demon_stomp", 6)
            .withTiming(60, 0, 11)
            .withDamage(10.0f)
            .withRange(4.0f)
            .withKnockback(0.8f)
            .withHitStun(30)
            .withHitboxSize(3.0f)
            .withDescription("Stomp down on enemies after high jumping")
            .build();

    private static final Map<UUID, Map<Integer, Long>> entityCooldowns = new HashMap<>();
    private static final Map<UUID, Boolean> executingHighJump = new HashMap<>();
    private static final Map<UUID, SlashComboState> entitySlashStates = new HashMap<>();
    private static final Map<UUID, Boolean> canStompAfterHighJump = new HashMap<>();
    private static final Map<UUID, Boolean> hasUsedHighJumpInAir = new HashMap<>();
    private static final Map<UUID, Long> lastHighJumpTick = new HashMap<>();
    private static final int DEMON_COOLDOWN_COLOR = 0xFFDDDDDD;

    private static class SlashComboState {
        int currentStage = 0;
        long slash1GameTick = -1;

        // Minimum ticks after slash1 before slash2 is valid (prevents instant double-tap)
        static final long MIN_FOLLOWUP_TICKS = 8;
        // Maximum ticks after slash1 that slash2 can still be triggered
        static final long MAX_FOLLOWUP_TICKS = 40;

        boolean isReadyForSlash2(long currentTick) {
            if (slash1GameTick < 0) return false;
            long elapsed = currentTick - slash1GameTick;
            return elapsed >= MIN_FOLLOWUP_TICKS && elapsed <= MAX_FOLLOWUP_TICKS;
        }

        void recordSlash1(long currentTick) {
            slash1GameTick = currentTick;
            currentStage = 1;
        }

        void reset() {
            currentStage = 0;
            slash1GameTick = -1;
        }
    }

    public TempleDemonMoveset() {
        super("default_demon", "Demon Arts", MovesetType.DEMON, createBuilder());
    }

    private static MovesetBuilder createBuilder() {
        return new MovesetBuilder()
                .withIdleAnimation("nichirin:demon_idle")
                .withSpeedMultiplier(1.05f)

                // Click moves: stats come from the static MoveConfiguration constants above,
                // which carry their own click-slot tag (asLeftClick/asRightClick/asCrouchRightClick).
                .withMove(GUT_PUNCH_CONFIG)
                .withMove(SLASH_1_CONFIG)
                .withMove(HIGH_JUMP_CONFIG)

                .withMove(new MoveBuilder("demon_kick", "Kick")
                        .withAnimation("nichirin:demon_kick", 8)
                        .withTiming(60, 5, 11)
                        .withDamage(6.0f)
                        .withRange(2.5f)
                        .withKnockback(1f)
                        .withHitStun(25)
                        .withHitboxSize(2.0f)
                        .withDescription("Powerful front kick with high knockback")
                        .withAttack(DemonKickAttack::new)
                )

                .withMove(new MoveBuilder("dashing_strike", "Dashing Strike")
                        .withAnimation("nichirin:demon_dash_strike", 10)
                        .withTiming(140, 8, 14)
                        .withDamage(12.0f)
                        .withDashSpeed(6.0f)
                        .withRange(5.5f)
                        .withKnockback(0.2f)
                        .withHitStun(20)
                        .withHitboxSize(2)
                        .withDescription("Dash forward and deliver a devastating punch")
                        .withAttack(DemonDashStrikeAttack::new)
                )

                .withMove(new MoveBuilder("demon_bite", "Bite")
                        .withAnimation("nichirin:demon_bite", 9)
                        .withTiming(100, 5, 11)
                        .withDamage(8.0f)
                        .withRange(2.0f)
                        .withKnockback(0.1f)
                        .withHitStun(20)
                        .withHitboxSize(2.0f)
                        .withDescription("Bite attack that steals blood")
                        .withAttack(DemonBiteAttack::new)
                )

                .withMove(new MoveBuilder("demon_grab", "Throw")
                        .withAnimation("nichirin:demon_grab", 5)
                        .withTiming(80, 3, 8)
                        .withDescription("Grab and instantly throw the target forward")
                        .withAction(entity -> {
                            new DemonGrabAttack().execute(entity);
                        })
                );
    }

    @Override
    public boolean handleLeftClick(LivingEntity entity) {
        if (entity.level().isClientSide) return false;
        if (entity.hasEffect(NichirinEffectRegistry.stunned())) return true;
        if (!canUseMove(entity, -3)) {
            if (entity instanceof Player player) showCooldownMessage(player, -3, GUT_PUNCH_CONFIG.getDisplayName());
            return true;
        }

        triggerAnimation(entity, "demon_gut_punch");

        DemonGutPunchAttack gutPunchAttack = new DemonGutPunchAttack();
        gutPunchAttack.configure(GUT_PUNCH_CONFIG);
        MoveExecutor.executeAttack(entity, gutPunchAttack, "default_demon", "demon_gut_punch");

        setMoveCooldown(entity, -3, GUT_PUNCH_CONFIG.getCooldownOrDefault(0));
        return true;
    }

    @Override
    public boolean handleRightClick(LivingEntity entity, boolean isCrouching) {
        if (entity.level().isClientSide) return false;
        if (entity.hasEffect(NichirinEffectRegistry.stunned())) return true;
        return isCrouching ? handleCrouchRightClick(entity) : handleSlashCombo(entity);
    }

    private boolean handleSlashCombo(LivingEntity entity) {
        UUID entityUUID = entity.getUUID();
        long currentTick = entity.level().getGameTime();
        SlashComboState comboState = entitySlashStates.computeIfAbsent(entityUUID, k -> new SlashComboState());

        if (comboState.currentStage == 1) {
            if (comboState.isReadyForSlash2(currentTick)) {
                return executeSlashStage(entity, 1, comboState, currentTick);
            } else if (currentTick - comboState.slash1GameTick <= SlashComboState.MAX_FOLLOWUP_TICKS) {
                return true;
            }
            comboState.reset();
        }

        if (!canUseMove(entity, -1)) {
            if (entity instanceof Player player) showCooldownMessage(player, -1, SLASH_1_CONFIG.getDisplayName());
            return true;
        }
        return executeSlashStage(entity, 0, comboState, currentTick);
    }

    private boolean executeSlashStage(LivingEntity entity, int stage, SlashComboState comboState, long currentTick) {
        MoveConfiguration slashConfig = stage == 0 ? SLASH_1_CONFIG : SLASH_2_CONFIG;
        String animationName = stage == 0 ? "demon_slash" : "demon_slash_2";

        triggerAnimation(entity, animationName);

        DemonSlashAttack slashAttack = new DemonSlashAttack();
        slashAttack.setSlashStage(stage + 1);
        slashAttack.configure(slashConfig);

        MoveExecutor.executeAttackWithInfo(entity, slashAttack, slashConfig.getDisplayName(), slashConfig.getCooldownOrDefault(0));

        if (stage == 0) {
            comboState.recordSlash1(currentTick);
        } else {
            comboState.reset();
            setMoveCooldown(entity, -1, slashConfig.getCooldownOrDefault(0));
            if (entity instanceof ServerPlayer sp) {
                sendCooldownPacket(sp, slashConfig.getDisplayName(), slashConfig.getCooldownOrDefault(0));
            }
        }
        return true;
    }

    private boolean handleCrouchRightClick(LivingEntity entity) {
        UUID entityUUID = entity.getUUID();

        // Stomp if: did a high jump AND currently in the air
        if (canStompAfterHighJump.getOrDefault(entityUUID, false) && !entity.onGround()) {
            if (!canUseMove(entity, -4)) return true; // stomp on cooldown
            return executeStompAttack(entity);
        }
        return executeHighJump(entity);
    }

    private boolean executeHighJump(LivingEntity entity) {
        UUID entityUUID = entity.getUUID();

        long currentTick = entity.level().getGameTime();
        Long lastTick = lastHighJumpTick.get(entityUUID);
        if (lastTick != null && lastTick == currentTick) return true;

        if (!entity.onGround() && hasUsedHighJumpInAir.getOrDefault(entityUUID, false)) return true;

        if (!canUseMove(entity, -2)) {
            if (entity instanceof Player player) showCooldownMessage(player, -2, HIGH_JUMP_CONFIG.getDisplayName());
            return true;
        }

        if (executingHighJump.getOrDefault(entityUUID, false)) return true;

        executingHighJump.put(entityUUID, true);
        lastHighJumpTick.put(entityUUID, currentTick);

        try {
            triggerAnimation(entity, "demon_high_jump");

            Vec3 currentMotion = entity.getDeltaMovement();
            entity.setDeltaMovement(currentMotion.x, 1.5, currentMotion.z);
            entity.hurtMarked = true;
            entity.hasImpulse = true;

            // Lift nearby enemies into the air with the demon
            AABB liftBox = entity.getBoundingBox().inflate(3.0);
            entity.level().getEntitiesOfClass(LivingEntity.class, liftBox, e -> e != entity && e.isAlive())
                    .forEach(nearby -> {
                        Vec3 nearbyMotion = nearby.getDeltaMovement();
                        nearby.setDeltaMovement(nearbyMotion.x, 1.2, nearbyMotion.z);
                        nearby.hurtMarked = true;
                        nearby.hasImpulse = true;
                    });

            hasUsedHighJumpInAir.put(entityUUID, true);
            canStompAfterHighJump.put(entityUUID, true);

            setMoveCooldown(entity, -2, HIGH_JUMP_CONFIG.getCooldownOrDefault(0));
            if (entity instanceof ServerPlayer serverPlayer) {
                sendCooldownPacket(serverPlayer, HIGH_JUMP_CONFIG.getDisplayName(), HIGH_JUMP_CONFIG.getCooldownOrDefault(0));
            }
            return true;
        } finally {
            executingHighJump.remove(entityUUID);
        }
    }

    private boolean executeStompAttack(LivingEntity entity) {
        UUID entityUUID = entity.getUUID();

        triggerAnimation(entity, "demon_stomp");

        DemonStompAttack stompAttack = new DemonStompAttack();
        stompAttack.configure(STOMP_CONFIG);
        MoveExecutor.executeAttack(entity, stompAttack, "default_demon", "demon_stomp");

        canStompAfterHighJump.remove(entityUUID);
        setMoveCooldown(entity, -4, STOMP_CONFIG.getCooldownOrDefault(0));
        if (entity instanceof ServerPlayer serverPlayer) {
            sendCooldownPacket(serverPlayer, STOMP_CONFIG.getDisplayName(), STOMP_CONFIG.getCooldownOrDefault(0));
        }
        return true;
    }


    private void showCooldownMessage(Player player, int moveIndex, String moveName) {
        Map<Integer, Long> cooldowns = entityCooldowns.get(player.getUUID());
        if (cooldowns != null) {
            Long cooldownEnd = cooldowns.get(moveIndex);
            if (cooldownEnd != null) {
                long remaining = cooldownEnd - player.level().getGameTime();
                player.displayClientMessage(
                        Component.literal(moveName + " on cooldown! " + (remaining / 20.0f) + "s remaining")
                                .withStyle(style -> style.withColor(0xFF5555)),
                        true
                );
            }
        }
    }

    private void sendCooldownPacket(ServerPlayer player, MoveConfiguration config) {
        if (config.getCooldownOrDefault(0) > 0) {
            CooldownDisplayPacket.sendToClient(player, getMovesetId(), config);
        }
    }

    private void sendCooldownPacket(ServerPlayer player, String moveName, int cooldownTicks) {
        if (cooldownTicks > 0) {
            CooldownDisplayPacket.sendToClient(player, moveName, cooldownTicks,
                    getSpecialMoveIcon(moveName), DEMON_COOLDOWN_COLOR);
        }
    }

    private ResourceLocation getSpecialMoveIcon(String moveName) {
        String moveId = switch (moveName) {
            case "High Jump" -> "high_jump";
            case "Stomp" -> "demon_stomp";
            default -> "default_move";
        };

        if ("default_move".equals(moveId)) {
            return ResourceLocation.fromNamespaceAndPath("nichirin", "textures/icons/default_move.png");
        }
        return ResourceLocation.fromNamespaceAndPath("nichirin", "textures/icons/default_demon/" + moveId + ".png");
    }

    private void setMoveCooldown(LivingEntity entity, int moveIndex, int cooldownTicks) {
        if (cooldownTicks <= 0) return;
        long cooldownEnd = entity.level().getGameTime() + cooldownTicks;
        entityCooldowns.computeIfAbsent(entity.getUUID(), k -> new HashMap<>())
                .put(moveIndex, cooldownEnd);
    }

    private boolean canUseMove(LivingEntity entity, int moveIndex) {
        Map<Integer, Long> cooldowns = entityCooldowns.get(entity.getUUID());
        if (cooldowns == null) return true;
        Long cooldownEnd = cooldowns.get(moveIndex);
        if (cooldownEnd == null) return true;
        return entity.level().getGameTime() >= cooldownEnd;
    }

    @Override
    public void performMove(LivingEntity entity, int moveIndex) {
        if (!canUseMove(entity, moveIndex)) {
            if (entity instanceof Player player) {
                MoveConfiguration config = getMove(moveIndex);
                if (config != null) showCooldownMessage(player, moveIndex, config.getDisplayName());
            }
            return;
        }

        // Don't execute or consume cooldown if stunned
        if (entity.hasEffect(NichirinEffectRegistry.stunned())) return;

        super.performMove(entity, moveIndex);

        MoveConfiguration config = getMove(moveIndex);
        if (config != null) {
            int cooldown = config.getCooldownOrDefault(0);
            setMoveCooldown(entity, moveIndex, cooldown);
            if (entity instanceof ServerPlayer serverPlayer && cooldown > 0) {
                sendCooldownPacket(serverPlayer, config);
            }
        }
    }

    @Override
    public int getLeftClickMoveIndex() { return super.getLeftClickMoveIndex(); }

    @Override
    public int getRightClickMoveIndex(boolean isCrouching) { return isCrouching ? -2 : -1; }

    @Override
    public String getLeftClickMoveName() { return GUT_PUNCH_CONFIG.getDisplayName(); }

    @Override
    public String getRightClickMoveName() { return SLASH_1_CONFIG.getDisplayName(); }

    @Override
    public String getCrouchRightClickMoveName() { return HIGH_JUMP_CONFIG.getDisplayName(); }

    public static void tickEntity(LivingEntity entity) {
        if (entity.onGround()) {
            UUID uuid = entity.getUUID();
            hasUsedHighJumpInAir.remove(uuid);
            canStompAfterHighJump.remove(uuid); // Clear stomp eligibility on landing
        }
    }

    public static void tickPlayer(Player player) {
        tickEntity(player);
    }

    public static void resetCooldowns(LivingEntity entity) {
        entityCooldowns.remove(entity.getUUID());
    }

    public static void cleanupEntity(LivingEntity entity) {
        UUID entityUUID = entity.getUUID();
        entityCooldowns.remove(entityUUID);
        entitySlashStates.remove(entityUUID);
        canStompAfterHighJump.remove(entityUUID);
        hasUsedHighJumpInAir.remove(entityUUID);
        executingHighJump.remove(entityUUID);
        lastHighJumpTick.remove(entityUUID);
    }

    public static void cleanupPlayer(Player player) {
        cleanupEntity(player);
    }
}
