package com.xirc.nichirin.common.system.abilities;

import com.xirc.nichirin.common.util.StaminaManager;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Wall jump: a double-jump input while right next to a block bounces the player up and away
 * from it. Unlike the double jump it has no cooldown and does NOT consume the double jump.
 * Each chained wall jump costs 15 more stamina than the last (10, 25, 40, ...) until the
 * player touches the ground, which resets the chain.
 */
public final class PlayerWallJump {

    private static final float BASE_STAMINA_COST = 10.0f;
    private static final float STAMINA_COST_STEP = 15.0f;
    // "Only a bit": a normal-jump-ish hop up and a modest push away from the wall.
    private static final double UP_VELOCITY = 0.45;
    private static final double AWAY_VELOCITY = 0.32;
    // How far beyond the bounding box a block counts as "right next to" the player.
    private static final double WALL_DISTANCE = 0.1;

    // Wall jumps chained since last touching the ground, per player. Both sides tick this via
    // PlayerDoubleJump.tickPlayer -> onLanded, so client prediction sees the same cost.
    private static final Map<UUID, Integer> CHAIN_COUNTS = new HashMap<>();

    private PlayerWallJump() {}

    /** Stamina cost of the player's NEXT wall jump: 10, then 25, then 40, ... */
    public static float getCost(Player player) {
        return BASE_STAMINA_COST + STAMINA_COST_STEP * CHAIN_COUNTS.getOrDefault(player.getUUID(), 0);
    }

    /** Called (both sides) when the player lands — resets the escalating cost chain. */
    public static void onLanded(Player player) {
        CHAIN_COUNTS.remove(player.getUUID());
    }

    /**
     * Direction pointing away from the adjacent wall(s), or {@link Vec3#ZERO} when no wall is in
     * range. Checks the four horizontal sides; touching two walls (a corner) pushes out diagonally.
     */
    public static Vec3 findAwayFromWall(Player player) {
        AABB bb = player.getBoundingBox();
        // Shrink vertically a touch so floor/ceiling seams don't read as walls.
        AABB probe = bb.deflate(0.0, 0.05, 0.0);
        double away = 0.0;
        double awayX = 0.0;
        double awayZ = 0.0;
        if (!player.level().noCollision(player, probe.move(WALL_DISTANCE, 0, 0)))  awayX -= 1.0;
        if (!player.level().noCollision(player, probe.move(-WALL_DISTANCE, 0, 0))) awayX += 1.0;
        if (!player.level().noCollision(player, probe.move(0, 0, WALL_DISTANCE)))  awayZ -= 1.0;
        if (!player.level().noCollision(player, probe.move(0, 0, -WALL_DISTANCE))) awayZ += 1.0;
        Vec3 result = new Vec3(awayX, 0, awayZ);
        return result.lengthSqr() > 0 ? result.normalize() : Vec3.ZERO;
    }

    /**
     * All conditions except the wall check: in the double-jump input window (airborne, left the
     * ground, past the grace ticks), not control-locked, and enough stamina for the current
     * chain cost.
     */
    public static boolean canWallJump(Player player) {

        if (!PlayerDoubleJump.hasAirJumpTiming(player)) return false;

        if (player.hasEffect(NichirinEffectRegistry.stunned())
                || player.hasEffect(NichirinEffectRegistry.disoriented())) return false;

        if (player.getAbilities().flying) return false;

        return StaminaManager.hasStamina(player, getCost(player));
    }

    /**
     * SERVER: validates and executes the bounce. Returns true if the jump fired (stamina was
     * consumed and velocity applied).
     */
    public static boolean tryWallJump(Player player) {
        if (player.level().isClientSide) return false;
        if (!canWallJump(player)) return false;
        Vec3 away = findAwayFromWall(player);
        if (away.equals(Vec3.ZERO)) return false;
        if (!StaminaManager.consume(player, getCost(player))) return false;
        CHAIN_COUNTS.merge(player.getUUID(), 1, Integer::sum);

        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(
                away.x * AWAY_VELOCITY,
                Math.max(velocity.y, 0) * 0.2 + UP_VELOCITY,
                away.z * AWAY_VELOCITY);
        player.fallDistance = 0.0f;
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(player));
        }
        playEffects(player, away);
        return true;
    }

    private static void playEffects(Player player, Vec3 away) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.35f, 2.0f);
        if (player.level() instanceof ServerLevel serverLevel) {
            // Small puff at the contact side (opposite of the away direction).
            Vec3 contact = player.position()
                    .add(-away.x * 0.4, player.getBbHeight() * 0.4, -away.z * 0.4);
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    contact.x, contact.y, contact.z,
                    8, 0.1, 0.25, 0.1, 0.05);
        }
    }
}
