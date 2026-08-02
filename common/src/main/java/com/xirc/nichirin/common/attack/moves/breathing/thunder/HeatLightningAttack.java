package com.xirc.nichirin.common.attack.moves.breathing.thunder;

import com.xirc.nichirin.common.effect.ShockedStatusEffect;
import com.xirc.nichirin.common.util.HitboxData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import com.xirc.nichirin.common.vfx.VfxIds;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fifth Form: Heat Lightning
 * Performs an upward slash in the direction the user is looking
 * Then strikes airborne targets with lightning
 */
public class HeatLightningAttack extends ThunderBreathingAttackBase {

    private final Set<LivingEntity> slashHitTargets = new HashSet<>();
    private float launchPower = 1.125f;
    private boolean lightningStruck = false;

    public HeatLightningAttack() {
    }

    @Override
    protected void onStart() {
        slashHitTargets.clear();
        lightningStruck = false;
    }

    @Override
    protected void onActiveStart() {
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.7f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        if (tickCount == windup + 1) {
            performRisingSlash();
        }
        if (tickCount >= windup + 20 && tickCount <= windup + 30 && !lightningStruck && !slashHitTargets.isEmpty()) {
            strikeAllTargetsWithLightning();
            lightningStruck = true;
        }

        if ((lightningStruck && tickCount > windup + 30) || (slashHitTargets.isEmpty() && tickCount > windup + 30)) {
            stop();
        }
    }

    private void performRisingSlash() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();
        Vec3 slashBase = userPos.add(lookDir.scale(2.0));
        playThunderVfx(VfxIds.HEAT_LIGHTNING_RISE, user.position(), lookDir, 1.0f);

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.4f, 2.5f);

        List<LivingEntity> targets = getTargetsAtRange(HitboxData.HitboxShape.CUBE);
        for (LivingEntity target : targets) {
            hitTarget(target);
            launchTarget(target);
            slashHitTargets.add(target);
        }
    }

    private void launchTarget(LivingEntity target) {
        target.setDeltaMovement(Vec3.ZERO);

        if (target.onGround()) {
            target.setPos(target.getX(), target.getY() + 0.1, target.getZ());
        }

        Vec3 launchVelocity = new Vec3(0, launchPower, 0);
        target.setDeltaMovement(launchVelocity);
        target.hurtMarked = true;
        target.hasImpulse = true;
        // Without this, the Shocked effect (applied by the hit) clamps the upward velocity to 0 next
        // tick and the target never leaves the ground.
        ShockedStatusEffect.markRecentLaunch(target);

        if (world instanceof ServerLevel serverLevel) {
            serverLevel.getChunkSource().broadcast(target, new ClientboundSetEntityMotionPacket(target));
        }
        if (target instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(target));
        }
    }

    private void strikeAllTargetsWithLightning() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        for (LivingEntity target : slashHitTargets) {
            Vec3 targetPos = target.position();

            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(world);
            if (lightning != null) {
                lightning.moveTo(targetPos);
                lightning.setCause(user instanceof ServerPlayer sp ? sp : null);
                serverLevel.addFreshEntity(lightning);
            }

            if (target.isAlive()) {
                hitTargetNoImmunity(target);
                playThunderVfxAt(VfxIds.THUNDER_STRIKE, targetPos, user.getLookAngle(), 0.80f);

                target.igniteForSeconds(3);
            }
        }
    }

    @Override
    protected void onStop() {
        slashHitTargets.clear();
        lightningStruck = false;
    }
}
