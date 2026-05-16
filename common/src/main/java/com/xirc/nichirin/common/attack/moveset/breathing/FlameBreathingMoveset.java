package com.xirc.nichirin.common.attack.moveset.breathing;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.moves.breathing.flame.*;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.network.s2c.MovesetConfigSyncPacket;
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

// Flame Breathing moveset. RC=Pommel Slash, Crouch+RC=Unknowing Fire, Wheel 0-4=breathing forms.
public class FlameBreathingMoveset extends AbstractMoveset {

    private static final Map<UUID, Map<Integer, Long>> entityCooldowns = new HashMap<>();
    private static final Map<UUID, Boolean> executingMove = new HashMap<>();
    private static final ThreadLocal<FlameBreathingMoveset> CURRENT_MOVESET = new ThreadLocal<>();

    public FlameBreathingMoveset() {
        super("flame_breathing", "Flame Breathing", MovesetType.BREATHING, createBuilder());
        captureInitialConfigs();
    }

    private void captureInitialConfigs() {
        createAndCapturePommelSlashConfig();
        createAndCaptureUnknowingFireConfig();
    }

    private void createAndCapturePommelSlashConfig() {
        MoveConfiguration tempConfig = new MoveBuilder("pommel_slash", "Pommel Slash")
                .withAnimation("nichirin:pommel_slash", 8)
                .withTiming(0, 5, 18)
                .withDamage(3.0f)
                .withRange(4.0f)
                .withKnockback(0f)
                .withBreathCost(15.0f)
                .withHitStun(8)
                .withHitboxSize(2.0f)
                .withDescription("6 rapid slashes in quick succession.")
                .build();
        this.captureRightClickConfig(tempConfig, false);
    }

    private void createAndCaptureUnknowingFireConfig() {
        MoveConfiguration tempConfig = new MoveBuilder("unknowing_fire_quick", "Unknowing Fire")
                .withAnimation("nichirin:unknowing_fire", 9)
                .withTiming(0, 6, 15)
                .withDamage(16.0f)
                .withRange(3.0f)
                .withKnockback(0.4f)
                .withBreathCost(40.0f)
                .withHitStun(20)
                .withHitboxSize(2.0f)
                .withDescription("Overhead slam with high damage and short windup.")
                .build();
        this.captureRightClickConfig(tempConfig, true);
    }

    private static MovesetBuilder createBuilder() {
        return new MovesetBuilder()
                .withIdleAnimation("nichirin:flame_idle")
                .withSpeedMultiplier(1.15f)

                // INDEX 0: Rising Scorching Sun — upward arc, launches enemies
                .withMove(new MoveBuilder("rising_scorching_sun", "Scorching Sun")
                        .withAnimation("nichirin:rising_scorching_sun", 8)
                        .withTiming(100, 12, 25) // 5 second cooldown
                        .withDamage(15.0f) // Good damage + bonus vs airborne
                        .withRange(6.0f) // Upward arc range
                        .withKnockback(0.6f) // Strong upward knockback
                        .withBreathCost(20.0f)
                        .withHitStun(20)
                        .withHitboxSize(5f) // Larger for arc
                        .withDescription("Upward arc slash that launches enemies into the air.")
                        .withAction(entity -> {
                            RisingScorchingSunAttack attack = new RisingScorchingSunAttack();
                            FlameBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(0));
                            }
                            MoveExecutor.executeAttack(entity, attack, "flame_breathing", "rising_scorching_sun");
                        })
                )

                // INDEX 1: Blazing Universe — charged downward strike, explodes on impact
                .withMove(new MoveBuilder("blazing_universe", "Blazing Universe")
                        .withAnimation("nichirin:blazing_universe", 12)
                        .withTiming(160, 13, 50) // 8 second cooldown, windup, explosive finish
                        .withDamage(12.0f) // Very high damage
                        .withRange(4.0f) // Large AOE
                        .withKnockback(0.6f)
                        .withBreathCost(30.0f) // Expensive for heavy attack
                        .withHitStun(35)
                        .withHitboxSize(3.0f) // Large explosion hitbox
                        .withDescription("Charged downward strike that explodes on impact.")
                        .withAction(entity -> {
                            BlazingUniverseAttack attack = new BlazingUniverseAttack();
                            FlameBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(1));
                            }
                            MoveExecutor.executeAttack(entity, attack, "flame_breathing", "blazing_universe");
                        })
                )

                // INDEX 2: Blooming Flame Undulation — 360° defense
                .withMove(new MoveBuilder("blooming_flame_undulation", "Blooming Flame")
                        .withAnimation("nichirin:blooming_flame_undulation", 10)
                        .withTiming(140, 11, 35) // 7 second cooldown
                        .withDamage(12.0f) // Multiple hits around user
                        .withRange(3.5f) // 3.5 block radius
                        .withKnockback(0.3f)
                        .withBreathCost(25.0f)
                        .withHitStun(15)
                        .withHitboxSize(3.5f) // Full radius
                        .withDescription("Full 360° slash hitting all nearby enemies.")
                        .withAction(entity -> {
                            BloomingFlameUndulationAttack attack = new BloomingFlameUndulationAttack();
                            FlameBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(2));
                            }
                            MoveExecutor.executeAttack(entity, attack, "flame_breathing", "blooming_flame_undulation");
                        })
                )

                // INDEX 3: Flame Tiger — dashing multi-hit strike
                .withMove(new MoveBuilder("flame_tiger", "Flame Tiger")
                        .withAnimation("nichirin:flame_tiger", 11)
                        .withTiming(120, 10, 40) // 6 second cooldown, dash duration
                        .withDamage(18.0f)
                        .withDashSpeed(8.0f) // 8 block dash
                        .withRange(8.0f) // Dash distance
                        .withKnockback(0.2f) // Light knockback to keep enemies close
                        .withBreathCost(50.0f)
                        .withHitStun(10) // Short stun for combo potential
                        .withHitboxSize(2.0f)
                        .withDescription("Dashing multi-hit strike in a straight line.")
                        .withAction(entity -> {
                            FlameTigerAttack attack = new FlameTigerAttack();
                            FlameBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(3));
                            }
                            MoveExecutor.executeAttack(entity, attack, "flame_breathing", "flame_tiger");
                        })
                )

                // INDEX 4: Rengoku — ultimate dragon dash, 30-second cooldown
                .withMove(new MoveBuilder("rengoku", "Rengoku")
                        .withAnimation("nichirin:rengoku", 20)
                        .withTiming(600, 40, 60) // 30 second cooldown, windup, dragon dash
                        .withDamage(50.0f) // Massive damage
                        .withDashSpeed(25.0f) // Very fast dash
                        .withRange(20.0f) // Long range dash
                        .withKnockback(0f) // Massive knockback
                        .withBreathCost(75.0f)
                        .withHitStun(80) // 4 second stun
                        .withHitboxSize(4.0f) // Large dragon hitbox
                        .withDescription("Massive-damage dash through enemies. 30-second cooldown.")
                        .withAction(entity -> {
                            RengokuAttack attack = new RengokuAttack();
                            FlameBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(4));
                            }
                            MoveExecutor.executeAttack(entity, attack, "flame_breathing", "rengoku");
                        })
                );
    }

    @Override
    public int getMoveCount() {
        return 5;
    }

    @Override
    public boolean handleRightClick(LivingEntity entity, boolean isCrouching) {
        if (isCrouching) {
            return executeUnknowingFire(entity);
        } else {
            return executePommelSlash(entity);
        }
    }

    private boolean executePommelSlash(LivingEntity entity) {
        triggerAnimation(entity, "pommel_slash");
        PommelSlashAttack attack = new PommelSlashAttack();

        createAndCapturePommelSlashConfig();
        MoveConfiguration tempConfig = getRightClickConfiguration();

        if (!entity.level().isClientSide && entity instanceof ServerPlayer serverPlayer) {
            MovesetConfigSyncPacket packet = new MovesetConfigSyncPacket(
                    "flame_breathing",
                    this.getRightClickConfiguration(),
                    this.getCrouchRightClickConfiguration()
            );
            NichirinPacketRegistry.sendToPlayer(packet, serverPlayer);
        }

        attack.configure(tempConfig);
        MoveExecutor.executeAttack(entity, attack, "flame_breathing", "pommel_slash");
        onMovePerformed(entity, -1, false);
        return true;
    }

    private boolean executeUnknowingFire(LivingEntity entity) {
        triggerAnimation(entity, "unknowing_fire");
        UnknowingFireAttack attack = new UnknowingFireAttack();

        createAndCaptureUnknowingFireConfig();
        MoveConfiguration tempConfig = getCrouchRightClickConfiguration();

        if (!entity.level().isClientSide && entity instanceof ServerPlayer serverPlayer) {
            MovesetConfigSyncPacket packet = new MovesetConfigSyncPacket(
                    "flame_breathing",
                    this.getRightClickConfiguration(),
                    this.getCrouchRightClickConfiguration()
            );
            NichirinPacketRegistry.sendToPlayer(packet, serverPlayer);
        }

        attack.configure(tempConfig);
        MoveExecutor.executeAttack(entity, attack, "flame_breathing", "unknowing_fire_quick");
        onMovePerformed(entity, -2, true);
        return true;
    }

    @Override
    public void performMove(LivingEntity entity, int moveIndex) {
        // Check cooldown before allowing move
        if (!canUseMove(entity, moveIndex)) {
            // Show cooldown message
            MoveConfiguration config = getMove(moveIndex);
            if (config != null) {
                Map<Integer, Long> cooldowns = entityCooldowns.get(entity.getUUID());
                if (cooldowns != null) {
                    Long cooldownEnd = cooldowns.get(moveIndex);
                    if (cooldownEnd != null) {
                        long remaining = (cooldownEnd - entity.level().getGameTime()) / 20;
                        EntityResources.sendMessage(entity,
                                Component.literal(config.getDisplayName() + " on cooldown! " + remaining + "s remaining")
                                        .withStyle(style -> style.withColor(0xFF6600)), true);
                    }
                }
            }
            return;
        }

        // Check breath BEFORE executing
        MoveConfiguration config = getMove(moveIndex);
        if (config != null) {
            float breathCost = config.getBreathCostOrDefault(0.0f);

            // Add small buffer to prevent race conditions
            if (breathCost > 0 && !EntityResources.hasBreath(entity, breathCost + 0.1f)) {
                EntityResources.sendMessage(entity,
                        Component.literal("Not enough breath for " + config.getDisplayName() + "!")
                                .withStyle(style -> style.withColor(0xFF3333)), true);
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

        // breath consumption is how we detect if the move actually fired
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

    private boolean hasTargetsInRange(LivingEntity entity, float range) {
        net.minecraft.world.phys.AABB searchBox = new net.minecraft.world.phys.AABB(
                entity.getX() - range, entity.getY() - range, entity.getZ() - range,
                entity.getX() + range, entity.getY() + range, entity.getZ() + range
        );

        List<LivingEntity> entities = entity.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != entity && entity.isAlive() && !entity.isSpectator());
        return !entities.isEmpty();
    }

    public static FlameBreathingMoveset getCurrentMoveset() {
        return CURRENT_MOVESET.get();
    }

    private boolean canUseMove(LivingEntity entity, int moveIndex) {
        MoveConfiguration config = getMove(moveIndex);
        if (config == null || config.getCooldownOrDefault(0) <= 0) {
            return true; // No cooldown
        }

        Map<Integer, Long> cooldowns = entityCooldowns.get(entity.getUUID());
        if (cooldowns == null) {
            return true; // No cooldowns tracked yet
        }

        Long cooldownEnd = cooldowns.get(moveIndex);
        if (cooldownEnd == null) {
            return true; // Move never used
        }

        long currentTime = entity.level().getGameTime();
        return currentTime >= cooldownEnd;
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