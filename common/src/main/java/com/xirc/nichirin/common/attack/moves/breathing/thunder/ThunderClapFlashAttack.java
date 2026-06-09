package com.xirc.nichirin.common.attack.moves.breathing.thunder;

import com.xirc.nichirin.common.util.TeleportUtil;
import com.xirc.nichirin.common.effect.ShockedStatusEffect;
import com.xirc.nichirin.registry.NicirinSoundRegistry;
import com.xirc.nichirin.registry.NichirinParticleRegistry;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

// First Form: Thunderclap and Flash. Instant teleport dash hitting all enemies in path.
public class ThunderClapFlashAttack extends ThunderBreathingAttackBase {

    private Vec3 startPosition = null;
    private boolean turnBackwards = false;

    public void setTurnBackwards(boolean turnBackwards) {
        this.turnBackwards = turnBackwards;
    }

    @Override
    protected void onStart() {
        startPosition = user.getEyePosition();
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                NicirinSoundRegistry.THUNDERCLAP_FLASH.get(), SoundSource.PLAYERS,
                1f, 2.0f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        if (tickCount == windup + 1) {
            executeTeleportDash();

            if (turnBackwards && startPosition != null) {
                flipUserBackwards();
            }
        }
    }

    private void flipUserBackwards() {
        float flippedYaw = user.getYRot() + 180.0f;
        user.setYRot(flippedYaw);
        user.setYHeadRot(flippedYaw);
        user.setYBodyRot(flippedYaw);

        if (user instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.teleport(user.getX(), user.getY(), user.getZ(),
                    flippedYaw, user.getXRot());
        } else {
            user.absMoveTo(user.getX(), user.getY(), user.getZ(), flippedYaw, user.getXRot());
        }
    }

    private void executeTeleportDash() {
        float dashDistance = teleportDistance != null ? teleportDistance : range;
        Vec3 from = user.position();

        TeleportUtil.TeleportOptions options = new TeleportUtil.TeleportOptions()
                .withParticles(NichirinParticleRegistry.THUNDER.get(), NichirinParticleRegistry.THUNDER.get())
                .withTrail(NichirinParticleRegistry.THUNDER.get(), 1.0f)
                .withSounds(NicirinSoundRegistry.THUNDERCLAP_FLASH.get(), null)
                .withDamage(damage)
                .withDamageCallback(target -> {
                    hitTargetNoImmunity(target);
                    target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 12, 1, false, false, true));

                    target.setDeltaMovement(
                            target.getDeltaMovement().x * 0.1,
                            0.45,
                            target.getDeltaMovement().z * 0.1
                    );
                    target.hurtMarked = true;
                    target.hasImpulse = true;
                    ShockedStatusEffect.markRecentLaunch(target);

                    if (world instanceof ServerLevel serverLevel) {
                        serverLevel.getChunkSource().broadcast(target, new ClientboundSetEntityMotionPacket(target));
                    }

                    if (target instanceof ServerPlayer serverPlayer) {
                        serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(target));
                    }
                });

        options.soundVolume = 1f;
        options.soundPitch = 2.0f;

        TeleportUtil.teleportInDirection(user, dashDistance, options);
        NichirinPacketRegistry.sendAfterimageTrail(user, from, user.position(), 14, 7, 0.58f);
    }

    @Override
    protected void onStop() {
        turnBackwards = false;
    }
}
