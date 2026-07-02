package com.xirc.nichirin.common.attack.moves.breathing.thunder;

import com.xirc.nichirin.common.effect.ShockedStatusEffect;
import com.xirc.nichirin.common.network.s2c.PlayerAnimationPacket;
import com.xirc.nichirin.common.system.blocking.HandToHandBlock;
import com.xirc.nichirin.common.system.blocking.KatanaBlock;
import com.xirc.nichirin.common.util.AttackInterruptTracker;
import com.xirc.nichirin.common.util.BreathingManager;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import com.xirc.nichirin.registry.NichirinParticleRegistry;
import com.xirc.nichirin.registry.NichirinSoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;

/**
 * First Form: Thunderclap and Flash — hold-to-charge multi-bounce dash.
 *
 * <p>Charging: while RMB+LMB are held, fold increments every {@code (8s / maxFold)} seconds.
 * maxFold = floor(maxBreath / 8). Each fold past the first costs 8 breath.</p>
 *
 * <p>Release: executes {@code fold} dash segments spaced {@link #FOLD_INTERVAL_TICKS} ticks
 * apart. Each segment picks a nearby living entity and teleports the player to a point just past
 * them (so the player passes through, knocking them up), falling back to a random direction when no
 * enemy is reachable. Segments are raycast-clipped against blocks, damage the entity in the swept
 * path, and spawn an afterimage trail.</p>
 */
public class ThunderClapFlashAttack extends ThunderBreathingAttackBase {

    private static final int CHARGE_DURATION_TICKS = 160; // 8 seconds
    private static final int CHARGE_PARTICLE_TICK = 30; // 1.5s into each 8s charge loop
    private static final float BREATH_PER_FOLD = 8.0f;
    private static final float DEFAULT_SEGMENT_DISTANCE = 12.0f;
    private static final int SEGMENT_DASH_TICKS = 3; // each fold-segment is a 3-tick velocity dash (2x previous speed)
    private static final int FOLD_INTERVAL_TICKS = 20; // 1 second between fold bumps
    private static final float BASE_HIT_DAMAGE = 6.0f;
    private static final float DAMAGE_PER_FOLD_SCALAR = 0.05f;
    private static final float AFTERIMAGE_ALPHA = 0.58f;
    private static final int AFTERIMAGE_LIFETIME_TICKS = 14;
    // Drop one afterimage every this-many blocks along the dash path.
    private static final float AFTERIMAGE_SPACING = 2.0f;
    // Past-target overshoot: waypoint = enemyPos + dir × OVERSHOOT so the player passes through.
    private static final float OVERSHOOT_DISTANCE = 1.75f;
    // Extra vertical lift on top of the entity's CURRENT y-position when overshooting through them.
    private static final float ENEMY_WAYPOINT_LIFT = 1.4f;
    private static final float SWEEP_RADIUS = 1.5f;

    private enum Phase { CHARGING, DASHING, DONE }

    private final Random random = new Random();

    private Phase phase = Phase.CHARGING;
    private int chargeTicks = 0;
    private int dashTicks = 0;
    private int fold = 1;
    private int maxFold = 1;
    private int foldIntervalTicks = FOLD_INTERVAL_TICKS;
    private float breathSpent = 0f;
    private boolean releaseRequested = false;

    private int totalSegments = 0;
    private int segmentsExecuted = 0;
    private int segmentTickProgress = 0;
    private Vec3 segmentStart = Vec3.ZERO;
    private Vec3 segmentEnd = Vec3.ZERO;
    private double nextAfterimageDistance = AFTERIMAGE_SPACING;
    // Target locked in at segment plan time so beginSegment can guarantee a hit on it even when the
    // sweep AABB misses (target launched out of the swept volume between plan and hit).
    private LivingEntity plannedTarget = null;

    /** Called by ThunderclapChargeManager when the client signals RMB/LMB release. */
    public void signalRelease() {
        this.releaseRequested = true;
    }

    @Override
    protected void onStart() {
        // Reset state in case this instance is reused.
        phase = Phase.CHARGING;
        chargeTicks = 0;
        dashTicks = 0;
        fold = 1;
        breathSpent = 0f;
        releaseRequested = false;
        totalSegments = 0;
        segmentsExecuted = 0;
        segmentTickProgress = 0;
        segmentStart = Vec3.ZERO;
        segmentEnd = Vec3.ZERO;
        nextAfterimageDistance = AFTERIMAGE_SPACING;
        plannedTarget = null;

        if (world.isClientSide || !(user instanceof Player player)) return;

        // Spam guard: if a charge is already running for this player, don't stack a second instance
        // (each would tick independently and fight over movement/breath/animation).
        if (ThunderclapChargeManager.isCharging(player.getUUID())) {
            this.isActive = false;
            return;
        }

        float maxBreath = BreathingManager.getMaxBreath(player);
        maxFold = Math.max(1, (int) Math.floor(maxBreath / BREATH_PER_FOLD));
        foldIntervalTicks = FOLD_INTERVAL_TICKS;

        // First fold is free in terms of charge time — pay its 8 breath on dash release instead of
        // up front so that a quick tap still spends the right amount.
        if (!BreathingManager.hasBreath(player, BREATH_PER_FOLD)) {
            // Not enough for even one fold — abort cleanly.
            this.isActive = false;
            return;
        }

        // Re-broadcast the charge animation at a speed that stretches it to fill the full charge
        // window so the loop doesn't reset early for players with high max breath.
        if (player instanceof ServerPlayer sp) {
            float chargeSpeed = (float) CHARGE_DURATION_TICKS / (maxFold * (float) FOLD_INTERVAL_TICKS);
            broadcastAnimation(sp, "thunderclap_charge", chargeSpeed);
        }

        ThunderclapChargeManager.register(player.getUUID(), this);
        showFoldStatus(player);
    }

    /** Player-initiated cancel via crouch — drops guard so the charge anim doesn't snap to block. */
    private void cancelChargeFromCrouch(Player player) {
        ThunderclapChargeManager.unregister(player.getUUID());
        KatanaBlock.stopBlocking(player);
        HandToHandBlock.stopBlocking(player);
        if (player instanceof ServerPlayer sp) {
            sp.displayClientMessage(
                    Component.literal("Thunderclap cancelled")
                            .withStyle(style -> style.withColor(0xAAAAAA)),
                    true);
        }
        stop();
    }

    /**
     * Hit during charge: drop the guard so the BlockingInputHandler doesn't immediately re-assert
     * the block animation, unregister the charge, and stop the attack. Player must release and
     * re-press RMB to start blocking again — fair penalty for getting interrupted.
     */
    private void cancelChargeFromHit(Player player) {
        ThunderclapChargeManager.unregister(player.getUUID());
        KatanaBlock.stopBlocking(player);
        HandToHandBlock.stopBlocking(player);
        if (player instanceof ServerPlayer sp) {
            sp.displayClientMessage(
                    Component.literal("Thunderclap interrupted!")
                            .withStyle(style -> style.withColor(0xFF5555)),
                    true);
        }
        stop();
    }

    /** Action-bar feedback so the player can see what fold they're charging at. */
    private void showFoldStatus(Player player) {
        if (!(player instanceof ServerPlayer sp)) return;
        sp.displayClientMessage(
                Component.literal(fold + "-fold / max " + maxFold)
                        .withStyle(style -> style.withColor(0xFFE066)),
                true);
    }

    @Override
    public void tick() {
        if (!isActive || user == null || world == null) return;
        if (world.isClientSide) return;
        if (!(user instanceof Player player)) {
            stop();
            return;
        }

        tickCount++;

        switch (phase) {
            case CHARGING -> tickCharge(player);
            case DASHING -> tickDash(player);
            case DONE -> stop();
        }
    }

    private void tickCharge(Player player) {
        // No hyper-armor: interrupting damage cancels the charge. Check both the interrupt tracker
        // (same-tick hits) and hurtTime > 0 (catches hits that fired after this tick's check).
        if (AttackInterruptTracker.wasInterruptedThisTick(user) || user.hurtTime > 0) {
            cancelChargeFromHit(player);
            return;
        }

        // Crouching mid-charge cancels the attack (player-initiated bail-out).
        if (player.isShiftKeyDown() || player.isCrouching()) {
            cancelChargeFromCrouch(player);
            return;
        }

        // Lock the player in place while charging — no walking, no horizontal momentum.
        player.setDeltaMovement(0, Math.min(player.getDeltaMovement().y, 0), 0);
        player.hurtMarked = true;
        if (player instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetEntityMotionPacket(player));
        }

        chargeTicks++;

        // Emit thunder particles at the right hand once per 8-second animation cycle, at t=1.5s.
        if (chargeTicks % CHARGE_DURATION_TICKS == CHARGE_PARTICLE_TICK) {
            spawnRightHandThunder(player);
        }

        // Try to bump fold based on elapsed charge time.
        int targetFold = Math.min(maxFold, 1 + (chargeTicks / foldIntervalTicks));
        while (fold < targetFold) {
            float cost = BREATH_PER_FOLD; // each additional fold costs 8
            if (!BreathingManager.hasBreath(player, cost)) {
                // Can't afford another fold; lock at current value.
                releaseRequested = true;
                break;
            }
            if (!BreathingManager.consume(player, cost)) {
                releaseRequested = true;
                break;
            }
            breathSpent += cost;
            fold++;
            showFoldStatus(player);
            spawnFoldParticles(player, fold);
        }

        // Auto-release once we hit the max fold the player's breath can sustain.
        if (fold >= maxFold) {
            releaseRequested = true;
        }

        if (releaseRequested) {
            // Pay the base fold's 8 breath cost now (it wasn't drained during charging).
            if (BreathingManager.hasBreath(player, BREATH_PER_FOLD) && BreathingManager.consume(player, BREATH_PER_FOLD)) {
                breathSpent += BREATH_PER_FOLD;
            }
            totalSegments = fold;
            phase = Phase.DASHING;
            // Stay registered through the dash so the spam guard in onStart keeps blocking new
            // charges until this attack fully ends (onStop unregisters).
            broadcastAnimation(player, "thunderclap_flash");
        }
    }

    /** Past-the-entity overshoot with a vertical lift so the player follows the knock-up. */
    private Vec3 overshootPastEntity(Vec3 from, LivingEntity entity) {
        Vec3 entityPos = entity.position().add(0, ENEMY_WAYPOINT_LIFT, 0);
        Vec3 dir = entityPos.subtract(from);
        if (dir.lengthSqr() < 1e-4) return entityPos;
        return entityPos.add(dir.normalize().scale(OVERSHOOT_DISTANCE));
    }

    /** Returns the closest living entity within the forward cone, or null. */
    private LivingEntity findTargetInDirection(Vec3 origin, Vec3 look, float radius, Player self) {
        List<LivingEntity> candidates = world.getEntitiesOfClass(LivingEntity.class,
                aabbAround(origin, radius), e -> e != self && e.isAlive());
        LivingEntity best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (LivingEntity e : candidates) {
            Vec3 toEntity = e.position().subtract(origin);
            double len = toEntity.length();
            if (len < 0.5) continue;
            double dot = toEntity.normalize().dot(look);
            // Prefer entities ahead of the player; score by alignment minus distance penalty.
            double score = dot - (len / radius) * 0.3;
            if (dot > 0.2 && score > bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }

    /** Picks a random living entity within {@code radius} of {@code origin}, or null. */
    private LivingEntity pickRandomNearby(Vec3 origin, float radius, Player self) {
        List<LivingEntity> candidates = world.getEntitiesOfClass(LivingEntity.class,
                aabbAround(origin, radius), e -> e != self && e.isAlive()
                        && e.position().distanceTo(origin) > 0.5);
        if (candidates.isEmpty()) return null;
        return candidates.get(random.nextInt(candidates.size()));
    }

    private AABB aabbAround(Vec3 center, float r) {
        return new AABB(center.x - r, center.y - r, center.z - r,
                center.x + r, center.y + r, center.z + r);
    }

    /** Spawns thunder particles at the approximate world position of the player's right hand. */
    private void spawnRightHandThunder(Player player) {
        if (!(world instanceof ServerLevel serverLevel)) return;
        double yawRad = Math.toRadians(player.getYRot());
        // Right-of-facing vector in world space (yaw 0 faces +Z; right is +X).
        Vec3 right = new Vec3(Math.cos(yawRad), 0, Math.sin(yawRad));
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
        // Approximate right-hand-low pose: shoulder + arm hanging down and slightly forward.
        Vec3 handPos = player.position()
                .add(0, 0.8, 0)
                .add(right.scale(0.35))
                .add(forward.scale(0.2));
        serverLevel.sendParticles(NichirinParticleRegistry.THUNDER.get(),
                handPos.x, handPos.y, handPos.z, 8, 0.1, 0.1, 0.1, 0.02);
    }

    private Vec3 randomUnitVector() {
        // Uniform sphere via rejection sampling on a cube.
        double x, y, z, len;
        do {
            x = random.nextDouble() * 2 - 1;
            y = random.nextDouble() * 2 - 1;
            z = random.nextDouble() * 2 - 1;
            len = x * x + y * y + z * z;
        } while (len > 1.0 || len < 0.0001);
        len = Math.sqrt(len);
        return new Vec3(x / len, y / len, z / len);
    }

    /**
     * Steps along the segment in 0.5-block increments; stops just before the first solid block face
     * encountered so the player doesn't end up clipped inside terrain.
     */
    private Vec3 clipSegment(Vec3 from, Vec3 to) {
        Vec3 dir = to.subtract(from);
        double total = dir.length();
        if (total < 1e-4) return to;
        Vec3 step = dir.normalize().scale(0.5);
        Vec3 cursor = from;
        Vec3 lastSafe = from;
        for (double traveled = 0; traveled < total; traveled += 0.5) {
            cursor = cursor.add(step);
            BlockPos bp = BlockPos.containing(cursor.x, cursor.y + 1.0, cursor.z);
            BlockState state = world.getBlockState(bp);
            if (!state.isAir() && state.isSolidRender(world, bp)) {
                return lastSafe;
            }
            lastSafe = cursor;
        }
        return lastSafe;
    }

    private void tickDash(Player player) {
        dashTicks++;
        if (segmentsExecuted >= totalSegments) {
            applyPostDashSlowFalling(player);
            phase = Phase.DONE;
            return;
        }

        if (segmentTickProgress == 0) {
            beginSegment(player);
        }

        Vec3 fullDelta = segmentEnd.subtract(segmentStart);
        double segDist = fullDelta.length();
        double previousDistance = segDist * segmentTickProgress / SEGMENT_DASH_TICKS;
        segmentTickProgress++;
        double currentDistance = segDist * segmentTickProgress / SEGMENT_DASH_TICKS;
        Vec3 perTickVelocity = fullDelta.scale(1.0 / SEGMENT_DASH_TICKS);
        player.setDeltaMovement(perTickVelocity);
        player.hurtMarked = true;
        player.hasImpulse = true;
        if (player instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetEntityMotionPacket(player));
        }
        sendAfterimagesCrossed(previousDistance, currentDistance, fullDelta, player);

        if (segmentTickProgress >= SEGMENT_DASH_TICKS) {
            segmentTickProgress = 0;
            segmentsExecuted++;
        }
    }

    /** Grants the player 1s of Slow Falling per fold they used, applied when the dash finishes. */
    private void applyPostDashSlowFalling(Player player) {
        int duration = Math.max(1, fold) * 20;
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, duration, 0, false, false, true));
    }

    /**
     * Plans the next segment dynamically using current enemy positions, then runs the start-of-
     * segment effects: face target, sound, telegraph, damage sweep.
     */
    private void beginSegment(Player player) {
        segmentStart = player.position();
        segmentEnd = planNextWaypoint(player);
        nextAfterimageDistance = AFTERIMAGE_SPACING;
        faceTarget(player, segmentEnd);
        playFoldSound(player);
        telegraphSegmentImpact(segmentEnd);
        damageEntitiesAlongSegment(player, segmentStart, segmentEnd);
        // Guarantee the locked-in target eats the hit even when the swept AABB missed (happens after
        // the first launch: the target is mid-air and can drift out of the sweep volume between
        // plan and hit).
        if (plannedTarget != null && plannedTarget.isAlive()) {
            applyFoldHit(player, plannedTarget);
        }
    }

    private void sendAfterimagesCrossed(double previousDistance, double currentDistance, Vec3 path, Player player) {
        double segDist = path.length();
        if (segDist < 1e-4) return;
        Vec3 direction = path.normalize();
        while (nextAfterimageDistance <= currentDistance && nextAfterimageDistance <= segDist) {
            if (nextAfterimageDistance > previousDistance) {
                Vec3 pos = segmentStart.add(direction.scale(nextAfterimageDistance));
                NichirinPacketRegistry.sendAfterimageTrail(player, pos, pos,
                        AFTERIMAGE_LIFETIME_TICKS, 1, AFTERIMAGE_ALPHA);
            }
            nextAfterimageDistance += AFTERIMAGE_SPACING;
        }
    }

    /** Picks a fresh waypoint at segment-start time so we chase enemies as they're flung up. */
    private Vec3 planNextWaypoint(Player player) {
        float segmentDistance = teleportDistance != null ? teleportDistance : DEFAULT_SEGMENT_DISTANCE;
        Vec3 origin = player.position();
        LivingEntity target;
        if (segmentsExecuted == 0) {
            // First segment: prefer an enemy in the player's look direction so the opener feels aimed.
            target = findTargetInDirection(origin, player.getLookAngle().normalize(), segmentDistance, player);
        } else {
            target = pickRandomNearby(origin, segmentDistance, player);
        }
        plannedTarget = target;
        Vec3 unclipped;
        if (target != null) {
            unclipped = overshootPastEntity(origin, target);
        } else {
            Vec3 dir = segmentsExecuted == 0 ? player.getLookAngle().normalize() : randomUnitVector();
            unclipped = origin.add(dir.scale(segmentDistance));
        }
        return clipSegment(origin, unclipped);
    }

    private void faceTarget(Player player, Vec3 target) {
        Vec3 dir = target.subtract(player.position());
        if (dir.lengthSqr() < 1e-4) return;
        double horizDist = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        float yaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float pitch = (float) Math.toDegrees(-Math.atan2(dir.y, horizDist));
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.setYBodyRot(yaw);
        player.setXRot(pitch);
        if (player instanceof ServerPlayer sp) {
            sp.connection.teleport(player.getX(), player.getY(), player.getZ(), yaw, pitch);
        }
    }

    private void playFoldSound(Player player) {
        // Slight pitch variation per fold-segment so the chain reads as multiple distinct strikes.
        float pitch = 1.8f + (segmentsExecuted % 4) * 0.05f;
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                NichirinSoundRegistry.THUNDERCLAP_FLASH.get(), SoundSource.PLAYERS, 0.7f, pitch);
    }

    /** Burst of particles right at the upcoming waypoint so the player sees where they're about to land. */
    private void telegraphSegmentImpact(Vec3 target) {
        if (!(world instanceof ServerLevel serverLevel)) return;
        serverLevel.sendParticles(NichirinParticleRegistry.THUNDER.get(),
                target.x, target.y + 1.0, target.z, 10, 0.3, 0.6, 0.3, 0.02);
    }

    private void damageEntitiesAlongSegment(Player player, Vec3 from, Vec3 to) {
        AABB sweep = new AABB(from, to).inflate(SWEEP_RADIUS);
        // Visual hitbox parity with the standard breathing-attack hit path.
        NichirinPacketRegistry.sendHitboxToTracking(player, sweep, 2500L);
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, sweep,
                e -> e != player && e.isAlive());
        for (LivingEntity target : targets) {
            // Skip the planned target here so beginSegment's guaranteed hit doesn't double up.
            if (target == plannedTarget) continue;
            applyFoldHit(player, target);
        }
    }

    /** Single source of truth for what a fold-hit does: damage + launch + shocked tag. */
    private void applyFoldHit(Player player, LivingEntity target) {
        float perHitDamage = BASE_HIT_DAMAGE * (1.0f + (fold - 1) * DAMAGE_PER_FOLD_SCALAR);
        this.damage = perHitDamage;
        hitTargetNoImmunity(target);
        target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 12, 1, false, false, true));
        target.setDeltaMovement(
                target.getDeltaMovement().x * 0.1,
                0.55,
                target.getDeltaMovement().z * 0.1
        );
        target.hurtMarked = true;
        target.hasImpulse = true;
        ShockedStatusEffect.markRecentLaunch(target);

        if (world instanceof ServerLevel serverLevel) {
            serverLevel.getChunkSource().broadcast(target, new ClientboundSetEntityMotionPacket(target));
        }
        if (target instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetEntityMotionPacket(target));
        }
    }

    /** Spawns {@code fold * 3} thunder particles in a ring around the player on fold gain. */
    private void spawnFoldParticles(Player player, int newFold) {
        if (!(world instanceof ServerLevel serverLevel)) return;
        int count = newFold * 3;
        double radius = 1.0 + newFold * 0.2;
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI * i) / count;
            double px = player.getX() + radius * Math.cos(angle);
            double pz = player.getZ() + radius * Math.sin(angle);
            serverLevel.sendParticles(NichirinParticleRegistry.THUNDER.get(),
                    px, player.getY() + 1.0, pz, 2, 0.1, 0.2, 0.1, 0.03);
        }
    }

    private void broadcastAnimation(Player player, String animationName) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        broadcastAnimation(serverPlayer, animationName, 1.0f);
    }

    private void broadcastAnimation(ServerPlayer serverPlayer, String animationName) {
        broadcastAnimation(serverPlayer, animationName, 1.0f);
    }

    private void broadcastAnimation(ServerPlayer serverPlayer, String animationName, float speed) {
        NichirinPacketRegistry.broadcastPlayerAnimation(serverPlayer,
                new PlayerAnimationPacket(serverPlayer.getId(), animationName, speed));
    }

    @Override
    protected void perform() {
        // Unused — tick() is overridden to drive the state machine directly.
    }

    @Override
    protected void onStop() {
        if (user instanceof Player player) {
            ThunderclapChargeManager.unregister(player.getUUID());
        }
        super.onStop();
    }
}
