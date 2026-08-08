package com.xirc.nichirin.common.attack.moves.demon.destructive;

import com.xirc.nichirin.common.vfx.VfxIds;
import com.xirc.nichirin.common.vfx.VfxManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;

/** Hold-to-charge activation for Akaza's six-second-plus Compass Needle combat state. */
public class CompassNeedleAttack extends DestructiveDeathAttackBase {

    public static final int DEFAULT_DURATION_TICKS = 120; // 6 seconds
    public static final int MAX_ACTIVE_DURATION_TICKS = 1200; // 60 seconds
    public static final int MAX_CHARGE_TICKS = 200; // 10 seconds
    public static final int COOLDOWN_TICKS = 600; // starts when the active Compass ends

    protected boolean activated;
    private boolean releaseRequested;
    private int chargeTicks;
    private Vec3 chargeAnchor = Vec3.ZERO;
    private boolean chargeVisualActive;

    @Override
    protected void onStart() {
        activated = false;
        releaseRequested = false;
        chargeTicks = 0;
        chargeVisualActive = false;
        if (!(user instanceof ServerPlayer player)) return;
        long now = player.level().getGameTime();
        if (DestructiveDeathState.hasCompassSession(player.getUUID())
                || DestructiveDeathState.isCompassOnCooldown(player.getUUID(), now)) {
            stop(true);
            return;
        }
        chargeAnchor = player.position();
        if (!CompassNeedleChargeManager.register(player.getUUID(), this)) {
            stop(true);
            return;
        }
        VfxManager.playAttached(player.serverLevel(), player, player, VfxIds.COMPASS_NEEDLE,
                player.position().add(0.0, 0.055, 0.0), player.getLookAngle(), 1.8f,
                MAX_CHARGE_TICKS + 10);
        chargeVisualActive = true;
        showCharge(player);
    }

    @Override
    protected void perform() {
        if (!(user instanceof ServerPlayer player)) {
            stop(true);
            return;
        }

        // The manifestation stance is committed: movement input releases the charge client-side,
        // while this server anchor prevents latency or packet ordering from moving the caster.
        // Only CORRECT when the player has actually drifted — teleporting every tick spams position
        // packets that reset the client's charge animation (it kept restarting) and cause hitching.
        if (player.position().distanceToSqr(chargeAnchor) > 0.0025) {
            player.teleportTo(chargeAnchor.x, chargeAnchor.y, chargeAnchor.z);
            player.setDeltaMovement(Vec3.ZERO);
            player.hurtMarked = true;
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
        }

        chargeTicks = Math.min(MAX_CHARGE_TICKS, chargeTicks + 1);
        showCharge(player);
        if (releaseRequested || chargeTicks >= MAX_CHARGE_TICKS) {
            activateCompass(player);
            stop(true);
        }
    }

    public void signalRelease() {
        releaseRequested = true;
    }

    private void activateCompass(ServerPlayer player) {
        collapseChargeVisual(player);
        int activeTicks = chargedDurationTicks();
        DestructiveDeathState.activateCompass(player, activeTicks, isOverdriveMode());
        CompassNeedleTracker.activate(player, activeTicks);
        activated = true;
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2f, 1.45f);
        player.displayClientMessage(Component.empty(), true);
        onCompassActivated(player, activeTicks);
    }

    private void showCharge(ServerPlayer player) {
        int activeTicks = chargedDurationTicks();
        player.displayClientMessage(
                Component.literal(String.format("Charging Compass: %.1fs", activeTicks / 20.0f))
                        .withStyle(style -> style.withColor(0x83E8FF)), true);
    }

    private int chargedDurationTicks() {
        float chargeProgress = chargeTicks / (float) MAX_CHARGE_TICKS;
        return Math.min(MAX_ACTIVE_DURATION_TICKS, DEFAULT_DURATION_TICKS
                + Math.round((MAX_ACTIVE_DURATION_TICKS - DEFAULT_DURATION_TICKS) * chargeProgress));
    }

    protected void onCompassActivated(ServerPlayer player, int activeTicks) {}

    @Override
    protected void onStop() {
        if (user instanceof ServerPlayer player) {
            CompassNeedleChargeManager.unregister(player.getUUID(), this);
            collapseChargeVisual(player);
        }
    }

    private void collapseChargeVisual(ServerPlayer player) {
        if (!chargeVisualActive) return;
        VfxManager.playAttached(player.serverLevel(), player, player, VfxIds.COMPASS_NEEDLE_COLLAPSE,
                player.position().add(0.0, 0.055, 0.0), player.getLookAngle(), 1.8f, 10);
        chargeVisualActive = false;
    }

    protected boolean isOverdriveMode() {
        return false;
    }
}
