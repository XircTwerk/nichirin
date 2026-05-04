package com.xirc.nichirin.common.attack.moveset.demon;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.attack.moves.demon.basic.*;
import com.xirc.nichirin.common.system.GrabManager;
import com.xirc.nichirin.common.entity.MovesetCapableNPC;
import com.xirc.nichirin.common.network.s2c.PlayerAnimationPacket;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
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
 * - Wheel moves: Kick, Dashing Strike, Bite
 */
public class DefaultDemonMoveset extends AbstractMoveset {

    private static final Map<UUID, Map<Integer, Long>> entityCooldowns = new HashMap<>();
    private static final Map<UUID, Boolean> executingHighJump = new HashMap<>();
    private static final Map<UUID, SlashComboState> entitySlashStates = new HashMap<>();
    private static final Map<UUID, Boolean> canStompAfterHighJump = new HashMap<>();
    private static final Map<UUID, Boolean> hasUsedHighJumpInAir = new HashMap<>();
    private static final Map<UUID, Long> lastHighJumpTick = new HashMap<>();
    private static final ThreadLocal<DefaultDemonMoveset> CURRENT_MOVESET = new ThreadLocal<>();

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

    public DefaultDemonMoveset() {
        super("default_demon", "Demon Arts", MovesetType.DEMON, createBuilder());
    }

    private static MovesetBuilder createBuilder() {
        return new MovesetBuilder()
                .withIdleAnimation("nichirin:demon_idle")
                .withSpeedMultiplier(1.05f)

                .withLeftClickMove(new MoveBuilder("demon_gut_punch", "Gut Punch")
                        .withAnimation("nichirin:demon_gut_punch", 6)
                        .withTiming(15, 1, 10)
                        .withDamage(8.0f)
                        .withRange(2.0f)
                        .withKnockback(0.1f)
                        .withHitStun(15)
                        .withHitboxSize(2.8f)
                        .withDescription("Powerful close-range punch that stuns enemies")
                )

                .withRightClickMove(new MoveBuilder("demon_slash", "Slash")
                        .withAnimation("nichirin:demon_slash", 6)
                        .withTiming(0, 0, 20)
                        .withDamage(4.0f)
                        .withRange(3.0f)
                        .withKnockback(0f)
                        .withHitStun(15)
                        .withHitboxSize(2.0f)
                        .withDescription("Basic claw slash - press again for finisher")
                )

                .withCrouchRightClickMove(new MoveBuilder("high_jump", "High Jump")
                        .withAnimation("nichirin:demon_high_jump", 8)
                        .withTiming(140, 0, 5)
                        .withDescription("Launch into the air, crouch mid-air to stomp down")
                )

                .withMove(new MoveBuilder("demon_kick", "Kick")
                        .withAnimation("nichirin:demon_kick", 8)
                        .withTiming(60, 5, 15)
                        .withDamage(6.0f)
                        .withRange(2.5f)
                        .withKnockback(1f)
                        .withHitStun(25)
                        .withHitboxSize(2.0f)
                        .withDescription("Powerful front kick with high knockback")
                        .withAction(entity -> {
                            DemonKickAttack kickAttack = new DemonKickAttack();
                            DefaultDemonMoveset moveset = getCurrentMoveset();
                            if (moveset != null) kickAttack.configure(moveset.getMove(0));
                            MoveExecutor.executeAttack(entity, kickAttack, "default_demon", "demon_kick");
                        })
                )

                .withMove(new MoveBuilder("dashing_strike", "Dashing Strike")
                        .withAnimation("nichirin:demon_dash_strike", 10)
                        .withTiming(80, 8, 20)
                        .withDamage(12.0f)
                        .withDashSpeed(4.0f)
                        .withRange(4.0f)
                        .withKnockback(0.2f)
                        .withHitStun(20)
                        .withHitboxSize(2)
                        .withDescription("Dash forward and deliver a devastating punch")
                        .withAction(entity -> {
                            DemonDashStrikeAttack dashStrikeAttack = new DemonDashStrikeAttack();
                            DefaultDemonMoveset moveset = getCurrentMoveset();
                            if (moveset != null) dashStrikeAttack.configure(moveset.getMove(1));
                            MoveExecutor.executeAttack(entity, dashStrikeAttack, "default_demon", "dashing_strike");
                        })
                )

                .withMove(new MoveBuilder("demon_bite", "Bite")
                        .withAnimation("nichirin:demon_bite", 9)
                        .withTiming(100, 5, 15)
                        .withDamage(8.0f)
                        .withRange(2.0f)
                        .withKnockback(0.1f)
                        .withHitStun(20)
                        .withHitboxSize(2.0f)
                        .withDescription("Bite attack that steals blood")
                        .withAction(entity -> {
                            DemonBiteAttack biteAttack = new DemonBiteAttack();
                            DefaultDemonMoveset moveset = getCurrentMoveset();
                            if (moveset != null) biteAttack.configure(moveset.getMove(2));
                            MoveExecutor.executeAttack(entity, biteAttack, "default_demon", "demon_bite");
                        })
                )

                .withMove(new MoveBuilder("demon_grab", "Grab")
                        .withAnimation("nichirin:demon_grab", 5)
                        .withTiming(80, 3, 12)
                        .withDescription("Grab and instantly throw the target forward")
                        .withAction(entity -> {
                            new DemonGrabAttack().execute(entity);
                        })
                );
    }

    @Override
    public boolean handleLeftClick(LivingEntity entity) {
        if (entity.level().isClientSide) return false;
        if (entity.hasEffect(NichirinEffectRegistry.STUNNED.get())) return true;
        if (!canUseMove(entity, -3)) {
            if (entity instanceof Player player) showCooldownMessage(player, -3, "Gut Punch");
            return true;
        }

        MoveConfiguration gutPunchConfig = getLeftClickConfiguration();
        if (gutPunchConfig == null) return false;

        triggerAnimation(entity, "demon_gut_punch");

        DemonGutPunchAttack gutPunchAttack = new DemonGutPunchAttack();
        gutPunchAttack.configure(gutPunchConfig);
        MoveExecutor.executeAttack(entity, gutPunchAttack, "default_demon", "demon_gut_punch");

        setMoveCooldown(entity, -3, gutPunchConfig.getCooldownOrDefault(0));
        return true;
    }

    @Override
    public boolean handleRightClick(LivingEntity entity, boolean isCrouching) {
        if (entity.level().isClientSide) return false;
        if (entity.hasEffect(NichirinEffectRegistry.STUNNED.get())) return true;
        return isCrouching ? handleCrouchRightClick(entity) : handleSlashCombo(entity);
    }

    private boolean handleSlashCombo(LivingEntity entity) {
        UUID entityUUID = entity.getUUID();
        long currentTick = entity.level().getGameTime();
        SlashComboState comboState = entitySlashStates.computeIfAbsent(entityUUID, k -> new SlashComboState());

        if (comboState.currentStage == 1) {
            if (comboState.isReadyForSlash2(currentTick)) {
                // Player clicked again within the followup window — fire slash2
                return executeSlashStage(entity, 1, comboState, currentTick);
            } else if (currentTick - comboState.slash1GameTick <= SlashComboState.MAX_FOLLOWUP_TICKS) {
                // Inside the window but too early — absorb the click, don't restart slash1
                return true;
            }
            // Window expired — fall through to fresh slash1
            comboState.reset();
        }

        // Fresh start — start slash1
        if (!canUseMove(entity, -1)) {
            if (entity instanceof Player player) showCooldownMessage(player, -1, "Slash");
            return true;
        }
        return executeSlashStage(entity, 0, comboState, currentTick);
    }

    private boolean executeSlashStage(LivingEntity entity, int stage, SlashComboState comboState, long currentTick) {
        MoveConfiguration slashConfig;
        String animationName;

        if (stage == 0) {
            slashConfig = new MoveBuilder("demon_slash_1", "Slash")
                    .withTiming(0, 0, 20)
                    .withDamage(4.0f)
                    .withRange(3.0f)
                    .withKnockback(0f)
                    .withHitStun(15)
                    .withHitboxSize(2.0f)
                    .build();
            animationName = "demon_slash";
        } else {
            slashConfig = new MoveBuilder("demon_slash_2", "Slash Finisher")
                    .withTiming(5, 0, 25)
                    .withDamage(6.0f)
                    .withRange(3.0f)
                    .withKnockback(0.5f)
                    .withHitStun(20)
                    .withHitboxSize(2.2f)
                    .build();
            animationName = "demon_slash_2";
        }

        triggerAnimation(entity, animationName);

        DemonSlashAttack slashAttack = new DemonSlashAttack();
        slashAttack.setSlashStage(stage + 1);
        slashAttack.configure(slashConfig);

        String displayName = stage == 0 ? "Slash" : "Slash Finisher";
        MoveExecutor.executeAttackWithInfo(entity, slashAttack, displayName, slashConfig.getCooldownOrDefault(0));

        if (stage == 0) {
            comboState.recordSlash1(currentTick);
        } else {
            comboState.reset();
            setMoveCooldown(entity, -1, slashConfig.getCooldownOrDefault(0));
            if (entity instanceof ServerPlayer sp) {
                sendCooldownPacket(sp, "Slash Finisher", slashConfig.getCooldownOrDefault(0));
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
            if (entity instanceof Player player) showCooldownMessage(player, -2, "High Jump");
            return true;
        }

        if (executingHighJump.getOrDefault(entityUUID, false)) return true;

        executingHighJump.put(entityUUID, true);
        lastHighJumpTick.put(entityUUID, currentTick);

        try {
            MoveConfiguration highJumpConfig = getCrouchRightClickConfiguration();
            if (highJumpConfig == null) return false;

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

            setMoveCooldown(entity, -2, highJumpConfig.getCooldownOrDefault(0));
            if (entity instanceof ServerPlayer serverPlayer) {
                sendCooldownPacket(serverPlayer, "High Jump", highJumpConfig.getCooldownOrDefault(0));
            }
            return true;
        } finally {
            executingHighJump.remove(entityUUID);
        }
    }

    private boolean executeStompAttack(LivingEntity entity) {
        UUID entityUUID = entity.getUUID();

        MoveConfiguration stompConfig = new MoveBuilder("demon_stomp", "Stomp")
                .withTiming(0, 0, 15)
                .withDamage(10.0f)
                .withRange(4.0f)
                .withKnockback(0.8f)
                .withHitStun(30)
                .withHitboxSize(3.0f)
                .build();

        triggerAnimation(entity, "demon_stomp");

        DemonStompAttack stompAttack = new DemonStompAttack();
        stompAttack.configure(stompConfig);
        MoveExecutor.executeAttack(entity, stompAttack, "default_demon", "demon_stomp");

        canStompAfterHighJump.remove(entityUUID);
        setMoveCooldown(entity, -4, 60); // ~3 second stomp cooldown
        if (entity instanceof ServerPlayer serverPlayer) {
            sendCooldownPacket(serverPlayer, "Stomp", 60);
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
        sendCooldownPacket(player, config.getDisplayName(), config.getCooldownOrDefault(0));
    }

    private void sendCooldownPacket(ServerPlayer player, String moveName, int cooldownTicks) {
        if (cooldownTicks > 0) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeUtf(moveName);
            buf.writeInt(cooldownTicks);
            NetworkManager.sendToPlayer(player, new ResourceLocation("nichirin", "cooldown_display"), buf);
        }
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
        if (entity.hasEffect(NichirinEffectRegistry.STUNNED.get())) return;

        CURRENT_MOVESET.set(this);
        try {
            super.performMove(entity, moveIndex);
        } finally {
            CURRENT_MOVESET.remove();
        }

        MoveConfiguration config = getMove(moveIndex);
        if (config != null) {
            int cooldown = config.getCooldownOrDefault(0);
            setMoveCooldown(entity, moveIndex, cooldown);
            if (entity instanceof ServerPlayer serverPlayer && cooldown > 0) {
                sendCooldownPacket(serverPlayer, config);
            }
        }
    }

    public static DefaultDemonMoveset getCurrentMoveset() {
        return CURRENT_MOVESET.get();
    }

    @Override
    public int getLeftClickMoveIndex() { return super.getLeftClickMoveIndex(); }

    @Override
    public int getRightClickMoveIndex(boolean isCrouching) { return isCrouching ? -2 : -1; }

    @Override
    public String getLeftClickMoveName() { return "Gut Punch"; }

    @Override
    public String getRightClickMoveName() { return "Slash"; }

    @Override
    public String getCrouchRightClickMoveName() { return "High Jump"; }

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
