package com.xirc.nichirin.common.attack.moveset.breathing;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.moves.breathing.thunder.*;
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
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Thunder Clap Flash is right-click only; remaining 6 forms go in the attack wheel.
public class ThunderBreathingMoveset extends AbstractMoveset {

    private static final Map<UUID, Map<Integer, Long>> entityCooldowns = new HashMap<>();
    private static final Map<UUID, Boolean> executingMove = new HashMap<>();
    private static final ThreadLocal<ThunderBreathingMoveset> CURRENT_MOVESET = new ThreadLocal<>();

    public ThunderBreathingMoveset() {
        super("thunder_breathing", "Thunder Breathing", MovesetType.BREATHING, createBuilder());
        captureInitialConfigs();
    }

    private void captureInitialConfigs() {
        createAndCaptureThunderclapFlashConfig();
    }

    private void createAndCaptureThunderclapFlashConfig() {
        MoveConfiguration tempConfig = new MoveBuilder("thunderclap_flash", "Thunderclap and Flash")
                .withAnimation("nichirin:thunderclap_flash", 10)
                .withTiming(0, 1, 15)
                .withDamage(11.0f)
                .withTeleportDistance(12.0f)
                .withKnockback(0.2f)
                .withBreathCost(12.0f)
                .withHitStun(10)
                .withHitboxSize(2.0f)
                .withDescription("Teleport dash forward, hitting anything in the way.")
                .build();
        this.captureRightClickConfig(tempConfig, false);
    }

    private static MovesetBuilder createBuilder() {
        return new MovesetBuilder()
                .withIdleAnimation("nichirin:thunder_idle")
                .withSpeedMultiplier(1.5f)

                // SKIP INDEX 0 - Thunder Clap Flash is right-click only, not in attack wheel

                // Second Form: Rice Spirit - 5 quick slashes (INDEX 0 in wheel)
                .withMove(new MoveBuilder("rice_spirit", "Rice Spirit")
                        .withAnimation("nichirin:rice_spirit", 8)
                        .withTiming(120, 8, 120) // 5 second cooldown, windup, duration
                        .withDamage(2.5f)
                        .withRange(10.0f) // Medium range
                        .withKnockback(0.2f)
                        .withBreathCost(30.0f)
                        .withHitStun(4)
                        .withHitboxSize(2.0f)
                        .withDescription("5 rapid slashes spread in a wide arc around the player.")
                        .withAction(entity -> {
                            RiceSpiritAttack attack = new RiceSpiritAttack();
                            ThunderBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(0));
                            }
                            MoveExecutor.executeAttack(entity, attack, "thunder_breathing", "rice_spirit");
                        })
                )

                // Third Form: Thunder Swarm - AOE slashes (INDEX 1 in wheel)
                .withMove(new MoveBuilder("thunder_swarm", "Thunder Swarm")
                        .withAnimation("nichirin:thunder_swarm", 9)
                        .withTiming(140, 12, 35) // 7 second cooldown, windup, duration
                        .withDamage(4.5f) // 4 slashes = 24 damage total (was 8.0f = 32 total)
                        .withRange(7.0f) // Large area around player
                        .withKnockback(0.4f)
                        .withBreathCost(45.0f) // Higher cost for AOE
                        .withHitStun(14)
                        .withHitboxSize(2.5f) // Large hitbox for AOE
                        .withDescription("4 AOE slashes around the player in quick succession.")
                        .withAction(entity -> {
                            ThunderSwarmAttack attack = new ThunderSwarmAttack();
                            ThunderBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(1));
                            }
                            MoveExecutor.executeAttack(entity, attack, "thunder_breathing", "thunder_swarm");
                        })
                )

                // Fourth Form: Distant Thunder - Lightning over time (INDEX 2 in wheel)
                .withMove(new MoveBuilder("distant_thunder", "Distant Thunder")
                        .withAnimation("nichirin:distant_thunder", 7)
                        .withTiming(320, 7, 120) // 10 second cooldown, windup, 6 second duration
                        .withDamage(8.0f) // 3 strikes = 27 damage over time (was 12.0f = 36 total)
                        .withRange(15.0f) // Large AOE radius
                        .withKnockback(0.3f)
                        .withBreathCost(45.0f) // High cost for area denial
                        .withHitStun(25)
                        .withDescription("Calls down 3 delayed lightning strikes over a large area.")
                        .withAction(entity -> {
                            DistantThunderAttack attack = new DistantThunderAttack();
                            ThunderBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(2));
                            }
                            MoveExecutor.executeAttack(entity, attack, "thunder_breathing", "distant_thunder");
                        })
                )

                // Fifth Form: Heat Lightning - Anti-air combo (INDEX 3 in wheel)
                .withMove(new MoveBuilder("heat_lightning", "Heat Lightning")
                        .withAnimation("nichirin:heat_lightning", 9)
                        .withTiming(180, 10, 20)
                        .withDamage(4.0f) // Single hit + lightning follow-up (was 18.0f)
                        .withRange(2.0f)
                        .withKnockback(0.1f) // Minimal horizontal, focuses on launch
                        .withBreathCost(30.0f)
                        .withHitStun(25) // Good combo potential
                        .withHitboxSize(2.0f)
                        .withDescription("Upward slash that launches the target into the air.")
                        .withAction(entity -> {
                            HeatLightningAttack attack = new HeatLightningAttack();
                            ThunderBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(3));
                            }
                            MoveExecutor.executeAttack(entity, attack, "thunder_breathing", "heat_lightning");
                        })
                )

                // Sixth Form: Rumble and Flash - Long range precision (INDEX 4 in wheel)
                .withMove(new MoveBuilder("rumble_flash", "Rumble and Flash")
                        .withAnimation("nichirin:rumble_flash", 8)
                        .withTiming(180, 9, 25) // 9 second cooldown, windup, duration
                        .withDamage(16.5f) // High damage for long range precision (was 22.0f)
                        .withRange(20.0f) // Very long range
                        .withKnockback(0.6f)
                        .withBreathCost(40.0f) // High cost for range and damage
                        .withHitStun(35) // Good stun for follow-up
                        .withDescription("Long-range dash slash with 20-block reach.")
                        .withAction(entity -> {
                            RumbleFlashAttack attack = new RumbleFlashAttack();
                            ThunderBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(4));
                            }
                            MoveExecutor.executeAttack(entity, attack, "thunder_breathing", "rumble_flash");
                        })
                )

                // Seventh Form: Honoikazuchi no Kami - Ultimate finisher (INDEX 5 in wheel)
                .withMove(new MoveBuilder("honoikazuchi_no_kami", "Honoikazuchi no Kami")
                        .withAnimation("nichirin:honoikazuchi_no_kami", 15)
                        .withTiming(600, 120, 40) // 30 second cooldown, windup, execution
                        .withDamage(80.0f) // Very high damage ultimate
                        .withTeleportDistance(20.0f) // Long dash
                        .withKnockback(2.0f) // High knockback
                        .withBreathCost(70.0f) // Very expensive ultimate
                        .withHitStun(60) // 3 second stun
                        .withHitboxSize(3.5f) // Large hitbox for ultimate
                        .withDescription("Massive-damage teleport slash. 30-second cooldown.")
                        .withAction(entity -> {
                            HonoikazuchiNoKamiAttack attack = new HonoikazuchiNoKamiAttack();
                            ThunderBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(5));
                            }
                            MoveExecutor.executeAttack(entity, attack, "thunder_breathing", "honoikazuchi_no_kami");
                        })
                );
    }

    @Override
    public int getMoveCount() {
        return 6;
    }

    @Override
    public boolean handleRightClick(LivingEntity entity, boolean isCrouching) {
        if (entity instanceof Player player) {
            ThunderClapFlashAttack.setCrouchDash(player, isCrouching);
        }

        triggerAnimation(entity, "thunderclap_flash");
        ThunderClapFlashAttack attack = new ThunderClapFlashAttack();

        createAndCaptureThunderclapFlashConfig();
        MoveConfiguration tempConfig = getRightClickConfiguration();

        if (!entity.level().isClientSide && entity instanceof ServerPlayer serverPlayer) {
            MovesetConfigSyncPacket packet = new MovesetConfigSyncPacket(
                    "thunder_breathing",
                    this.getRightClickConfiguration(),
                    this.getCrouchRightClickConfiguration()
            );
            NichirinPacketRegistry.sendToPlayer(packet, serverPlayer);
        }

        attack.configure(tempConfig);
        MoveExecutor.executeAttack(entity, attack, "thunder_breathing", "thunderclap_flash");
        onMovePerformed(entity, -1, isCrouching);
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
                        EntityResources.sendMessage(entity,
                                Component.literal(config.getDisplayName() + " on cooldown! " + remaining + "s remaining")
                                        .withStyle(style -> style.withColor(0xFFFF00)), true);
                    }
                }
            }
            return;
        }

        MoveConfiguration config = getMove(moveIndex);
        if (config != null) {
            float breathCost = config.getBreathCostOrDefault(0.0f);

            if (breathCost > 0 && !EntityResources.hasBreath(entity, breathCost)) {
                EntityResources.sendMessage(entity,
                        Component.literal("Not enough breath for " + config.getDisplayName() + "!")
                                .withStyle(style -> style.withColor(0xFF5555)), true);
                return;
            }
        }

        // SPECIAL CHECK FOR RICE SPIRIT - Don't execute if no targets in range
        if (moveIndex == 0) {
            if (!hasTargetsInRange(entity, config.getRangeOrDefault(5.0f))) {
                EntityResources.sendMessage(entity,
                        Component.literal("Rice Spirit: No enemies in range!")
                                .withStyle(style -> style.withColor(0xFFAA00)), true);
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

    private boolean hasTargetsInRange(LivingEntity entity, float range) {
        net.minecraft.world.phys.AABB searchBox = new net.minecraft.world.phys.AABB(
                entity.getX() - range, entity.getY() - range, entity.getZ() - range,
                entity.getX() + range, entity.getY() + range, entity.getZ() + range
        );

        List<LivingEntity> entities = entity.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != entity && entity.isAlive() && !entity.isSpectator());
        return !entities.isEmpty();
    }

    public static ThunderBreathingMoveset getCurrentMoveset() {
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
        return -1; // Not in attack wheel, handled separately
    }

    @Override
    public String getRightClickMoveName() {
        return "Thunderclap and Flash";
    }

    @Override
    public String getCrouchRightClickMoveName() {
        return "Thunderclap and Flash (Backwards)";
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
    }

}