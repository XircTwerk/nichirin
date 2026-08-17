package com.xirc.nichirin.common.attack.moveset.breathing;

import com.xirc.nichirin.common.attack.moves.breathing.thunder.*;
import com.xirc.nichirin.common.attack.ServerCooldownManager;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.network.util.CooldownDisplayPacket;
import com.xirc.nichirin.common.util.EntityResources;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ThunderBreathingMoveset extends AbstractMoveset {

    private static final Map<UUID, Boolean> executingMove = new HashMap<>();

    public ThunderBreathingMoveset() {
        super("thunder_breathing", "Thunder Breathing", MovesetType.BREATHING, createBuilder());
    }

    private static MovesetBuilder createBuilder() {
        // Right-click: hold-to-charge Thunderclap & Flash (multi-bounce dash, fold-scaled).
        // Crouch right-click: Godspeed (straight 100-block dash).
        return new MovesetBuilder()
                .withIdleAnimation("nichirin:thunder_idle")
                .withSpeedMultiplier(1.5f)

                .withMove(new MoveBuilder("thunderclap_flash", "Thunderclap and Flash")
                        .withAnimation("nichirin:thunderclap_charge", 10)
                        // Long duration window so the framework keeps the attack alive while charging
                        // and dashing. Breath is drained manually inside the attack per fold.
                        .withTiming(0, 0, 800)
                        .withDamage(6.0f)
                        .withTeleportDistance(12.0f)
                        .withKnockback(0.2f)
                        .withBreathCost(0.0f)
                        .withHitStun(14)
                        .withHitboxSize(2.0f)
                        .withDescription("Hold to charge folds (8 breath each, max scales with breath); release for a multi-bounce dash.")
                        .asRightClick()
                        .withAttack(ThunderClapFlashAttack::new)
                )

                .withMove(new MoveBuilder("godspeed", "Godspeed")
                        .withAnimation("nichirin:thunderclap_flash", 10)
                        // windup=10, duration=25 ticks → 10-tick wind-up then drag-dash active window.
                        .withTiming(0, 10, 25)
                        .withDamage(2.0f)
                        .withTeleportDistance(300.0f)
                        .withKnockback(0.3f)
                        .withBreathCost(40.0f)
                        .withHitStun(10)
                        .withHitboxSize(2.0f)
                        .withDescription("3x-longer hyper dash. Drags enemies and hits every 5 ticks for 2 damage each.")
                        .asCrouchRightClick()
                        .withAttack(GodspeedAttack::new)
                )

                // Second Form: Rice Spirit - 5 quick slashes (INDEX 0 in wheel)
                .withMove(new MoveBuilder("rice_spirit", "Rice Spirit")
                        .withTiming(120, 8, 84)
                        .withDamage(2.0f)
                        .withRange(10.0f)
                        .withKnockback(0.2f)
                        .withBreathCost(30.0f)
                        .withHitStun(4)
                        .withHitboxSize(2.0f)
                        .withDescription("5 rapid slashes spread in a wide arc around the player.")
                        .withAttack(RiceSpiritAttack::new)
                )

                // Third Form: Thunder Swarm - AOE slashes (INDEX 1 in wheel)
                .withMove(new MoveBuilder("thunder_swarm", "Thunder Swarm")
                        .withTiming(140, 12, 25)
                        .withDamage(3.0f)
                        .withRange(7.0f)
                        .withKnockback(0.4f)
                        .withBreathCost(45.0f)
                        .withHitStun(14)
                        .withHitboxSize(2.5f)
                        .withDescription("4 AOE slashes around the player in quick succession.")
                        .withAttack(ThunderSwarmAttack::new)
                )

                // Fourth Form: Distant Thunder - Lightning over time (INDEX 2 in wheel)
                .withMove(new MoveBuilder("distant_thunder", "Distant Thunder")
                        .withTiming(320, 7, 84)
                        .withDamage(8.0f)
                        .withRange(15.0f)
                        .withKnockback(0.3f)
                        .withBreathCost(45.0f)
                        .withHitStun(25)
                        .withDescription("Calls down 3 delayed lightning strikes over a large area.")
                        .withAttack(DistantThunderAttack::new)
                )

                // Fifth Form: Heat Lightning - Anti-air combo (INDEX 3 in wheel)
                .withMove(new MoveBuilder("heat_lightning", "Heat Lightning")
                        .withTiming(180, 10, 40)
                        .withDamage(4.0f)
                        .withRange(2.5f)
                        .withKnockback(0.1f)
                        .withBreathCost(30.0f)
                        .withHitStun(25)
                        .withHitboxSize(3.0f)
                        .withDescription("Upward slash that launches the target into the air.")
                        .withAttack(HeatLightningAttack::new)
                )

                // Sixth Form: Rumble and Flash - Long range precision (INDEX 4 in wheel)
                .withMove(new MoveBuilder("rumble_flash", "Rumble and Flash")
                        .withTiming(180, 9, 18)
                        .withDamage(7.5f)
                        .withRange(20.0f)
                        .withKnockback(0.6f)
                        .withBreathCost(40.0f)
                        .withHitStun(35)
                        .withDescription("Long-range dash slash with 20-block reach.")
                        .withAttack(RumbleFlashAttack::new)
                )

                // Seventh Form: Honoikazuchi no Kami - Ultimate finisher (INDEX 5 in wheel)
                .withMove(new MoveBuilder("honoikazuchi_no_kami", "Honoikazuchi no Kami")
                        .withTiming(600, 120, 7)
                        .withDamage(48.0f)
                        .withTeleportDistance(20.0f)
                        .withKnockback(2.0f)
                        .withBreathCost(70.0f)
                        .withHitStun(60)
                        .withHitboxSize(3.5f)
                        .withDescription("Massive-damage teleport slash. 30-second cooldown.")
                        .withAttack(HonoikazuchiNoKamiAttack::new)
                );
    }

    @Override
    public boolean canPerformMoves(LivingEntity entity) {
        return super.canPerformMoves(entity) && requireSingleKatana(entity);
    }

    @Override
    public int getMoveCount() {
        return 6;
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
        if (!canUseMove(entity, moveIndex)) {
            MoveConfiguration config = getMove(moveIndex);
            if (config != null) {
                long remaining = ServerCooldownManager.getRemaining(entity, config.getMoveId()) / 20;
                EntityResources.sendMessage(entity,
                        Component.literal(config.getDisplayName() + " on cooldown! " + remaining + "s remaining")
                                .withStyle(style -> style.withColor(0xFF5555)), true);
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
        super.performMove(entity, moveIndex);

        boolean moveExecuted = !executingMove.getOrDefault(entity.getUUID(), false);
        executingMove.remove(entity.getUUID());

        if (moveExecuted && config != null) {
            setMoveCooldown(entity, moveIndex);

            if (!entity.level().isClientSide && entity instanceof ServerPlayer serverPlayer
                    && config.getCooldownOrDefault(0) > 0) {
                CooldownDisplayPacket.sendToClient(serverPlayer, "thunder_breathing", config);
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

    private boolean canUseMove(LivingEntity entity, int moveIndex) {
        MoveConfiguration config = getMove(moveIndex);
        if (config == null || config.getCooldownOrDefault(0) <= 0) return true;
        return !ServerCooldownManager.isOnCooldown(entity, config.getMoveId());
    }

    private void setMoveCooldown(LivingEntity entity, int moveIndex) {
        MoveConfiguration config = getMove(moveIndex);
        if (config == null || config.getCooldownOrDefault(0) <= 0) return;
        ServerCooldownManager.set(entity, config.getMoveId(), config.getCooldownOrDefault(0));
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
        return "Godspeed";
    }

    @Override
    public void onMovePerformed(LivingEntity entity, int moveIndex, boolean isCrouching) {
    }

    public static void resetCooldowns(LivingEntity entity) {
        ServerCooldownManager.clear(entity.getUUID());
    }

    public static void cleanupPlayer(LivingEntity entity) {
        ServerCooldownManager.clear(entity.getUUID());
        executingMove.remove(entity.getUUID());
    }
}
