package com.xirc.nichirin.common.attack.moves.demon.destructive;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Destructive Death Crouch M2 — Compass Overdrive.
 *
 * <p>Same tracker as {@link CompassNeedleAttack} but flips the compass-overdrive flag so
 * shockwaves render red and the user gets Speed I + Strength I for the duration. Compass-aware
 * attacks treat the closest tracked entity as the "arrow" target (rendered client-side).</p>
 */
public class CompassOverdriveAttack extends CompassNeedleAttack {

    @Override
    protected void onCompassActivated(ServerPlayer player, int activeTicks) {
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, activeTicks, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, activeTicks, 0, false, false, true));
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0f, 0.7f);
    }

    @Override
    protected boolean isOverdriveMode() {
        return true;
    }
}
