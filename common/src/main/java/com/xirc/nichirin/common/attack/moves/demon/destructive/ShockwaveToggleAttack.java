package com.xirc.nichirin.common.attack.moves.demon.destructive;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Destructive Death wheel slot — toggles the "M1/CQC attacks spawn shockwaves" flag. Fires
 * instantly (no windup/active/recovery work) and feeds the new state back to the client HUD via
 */
public class ShockwaveToggleAttack extends DestructiveDeathAttackBase {

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

        boolean next = !DestructiveDeathState.isShockwaveEnabled(sp.getUUID());
        DestructiveDeathState.setShockwave(sp, next);

        sp.displayClientMessage(
                Component.literal("Shockwaves: " + (next ? "ON" : "OFF"))
                        .withStyle(s -> s.withColor(next ? 0x88BBFF : 0xAAAAAA)), true);
        world.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                next ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.AMETHYST_CLUSTER_BREAK,
                SoundSource.PLAYERS, 0.6f, next ? 1.4f : 0.9f);
    }

    @Override
    protected void onStop() {
        hasExecuted = false;
    }
}
