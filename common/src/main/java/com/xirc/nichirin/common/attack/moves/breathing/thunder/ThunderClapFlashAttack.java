package com.xirc.nichirin.common.attack.moves.breathing.thunder;

import com.xirc.nichirin.common.util.TeleportUtil;
import com.xirc.nichirin.common.effect.ShockedStatusEffect;
import com.xirc.nichirin.registry.NichirinParticleRegistry;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// First Form: Thunderclap and Flash. Instant teleport dash hitting all enemies in path.
public class ThunderClapFlashAttack extends ThunderBreathingAttackBase {

    private Vec3 startPosition = null;
    private static final Map<UUID, Boolean> CROUCH_DASH_MAP = new HashMap<>();

    public static void setCrouchDash(Player player, boolean crouchDash) {
        if (crouchDash) {
            CROUCH_DASH_MAP.put(player.getUUID(), true);
        } else {
            CROUCH_DASH_MAP.remove(player.getUUID());
        }
    }

    @Override
    protected void onStart() {
        startPosition = user.getEyePosition();
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS,
                1f, 2.0f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        if (tickCount == windup + 1) {
            executeTeleportDash();

            Boolean shouldTurnBackwards = CROUCH_DASH_MAP.get(user.getUUID());
            if (shouldTurnBackwards != null && shouldTurnBackwards && startPosition != null) {
                user.lookAt(EntityAnchorArgument.Anchor.EYES, startPosition);
                if (user instanceof ServerPlayer serverPlayer) {
                    serverPlayer.connection.teleport(user.getX(), user.getY(), user.getZ(),
                            user.getYRot(), user.getXRot());
                }
            }
        }
    }

    private void executeTeleportDash() {
        float dashDistance = teleportDistance != null ? teleportDistance : range;

        TeleportUtil.TeleportOptions options = new TeleportUtil.TeleportOptions()
                .withParticles(NichirinParticleRegistry.THUNDER.get(), NichirinParticleRegistry.THUNDER.get())
                .withTrail(NichirinParticleRegistry.THUNDER.get(), 1.0f)
                .withSounds(SoundEvents.LIGHTNING_BOLT_THUNDER, null)
                .withDamage(damage)
                .withDamageCallback(target -> {
                    hitTargetNoImmunity(target);

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
    }

    @Override
    protected void onStop() {
        CROUCH_DASH_MAP.remove(user.getUUID());
    }
}
