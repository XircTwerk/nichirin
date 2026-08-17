package com.xirc.nichirin.common.attack.moves.demon.destructive;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Destructive Death wheel slot — toggles Overdrive. While on, every Destructive Death attack (and
 * its shockwaves) renders red, Strength + Speed buffs are applied, and damage scales up.
 *
 * <p>This toggle is the buff layer — the actual red-shockwave colour is read from
 * {@link DestructiveDeathState} at spawn time inside {@link DestructiveDeathAttackBase}.</p>
 */
public class OverdriveToggleAttack extends DestructiveDeathAttackBase {

    private static final int BUFF_DURATION_TICKS = 20 * 30; // 30s rolling refresh

    private boolean hasExecuted = false;

    @Override
    protected void onStart() {
        hasExecuted = false;
    }

    @Override
    protected void perform() {
        if (hasExecuted) return;
        hasExecuted = true;
        if (!(user instanceof ServerPlayer sp)) return;

        DestructiveDeathState.activateOverdrive(sp, BUFF_DURATION_TICKS);
        sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, BUFF_DURATION_TICKS, 0, false, false, true));
        sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, BUFF_DURATION_TICKS, 0, false, false, true));

        sp.displayClientMessage(
                Component.literal("Overdrive: ON (30s) — shockwaves auto-fire, pierce, and leech health")
                        .withStyle(s -> s.withColor(0xFF4444)), true);
        world.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.7f, 1.2f);
    }

    @Override
    protected void onStop() {
        hasExecuted = false;
    }
}
