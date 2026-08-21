package com.xirc.nichirin.client.gyomei;

import com.xirc.nichirin.common.gyomei.GripMode;
import com.xirc.nichirin.common.gyomei.GyomeiAttackController;
import com.xirc.nichirin.common.gyomei.GyomeiAttackController.Attack;
import com.xirc.nichirin.common.gyomei.GyomeiCollision;
import com.xirc.nichirin.common.gyomei.GyomeiWeaponManager;
import com.xirc.nichirin.common.gyomei.GyomeiWeaponSimulation;
import com.xirc.nichirin.common.util.NetworkBufferUtils;
import com.xirc.nichirin.registry.NichirinKeybindRegistry;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Stage 1-2 debug driver: owns a {@link GyomeiWeaponSimulation} for the local player, anchors the AXE
 * end at the player's hand each client tick, and steps the physics so the chain and flail swing off the
 * hand in world space. Toggled with the Gyomei-debug keybind. No combat, no netcode yet — this is the
 * runnable foundation everything else stacks on.
 */
@Environment(EnvType.CLIENT)
public final class ClientGyomeiWeaponManager {

    private static GyomeiWeaponSimulation sim;
    private static boolean enabled;
    private static Vec3 smoothedHand;
    private static GyomeiAttackController clientAttack = new GyomeiAttackController();
    private static boolean wasAttackDown;
    private static boolean wasUseDown;
    private static boolean wasSheatheDown;
    // Stance + reel, mirrored to the server via GYOMEI_STATE so the authoritative sim matches the visual.
    private static boolean flailMode;
    private static boolean reeling;

    private ClientGyomeiWeaponManager() {}

    public static boolean isEnabled() { return enabled && sim != null; }
    public static GyomeiWeaponSimulation sim() { return sim; }
    public static boolean isFlailMode() { return flailMode; }

    public static void clientTick(Minecraft mc) {
        LocalPlayer player = mc.player;
        // Holding the Gyomei item activates the weapon (mirrors the server's held-item activation).
        boolean holding = player != null
                && player.getMainHandItem().getItem() instanceof com.xirc.nichirin.common.item.gyomei.GyomeiWeapon;
        if (!holding || mc.level == null || mc.isPaused()) {
            enabled = false;
            sim = null;
            flailMode = false;
            reeling = false;
            return;
        }
        enabled = true;

        boolean shift = player.isShiftKeyDown();
        boolean atkDown = mc.screen == null && mc.options.keyAttack.isDown();
        boolean useDown = mc.screen == null && mc.options.keyUse.isDown();
        boolean sheatheDown = mc.screen == null && NichirinKeybindRegistry.SHEATHE_KEY.isDown();

        // Sheathing with the weapon in hand toggles the flail stance (grip the middle of the chain).
        if (sheatheDown && !wasSheatheDown) {
            flailMode = !flailMode;
            sendState();
        }
        wasSheatheDown = sheatheDown;

        // Rising-edge attacks. Slot: 0=M1, 1=M2, 2=crouch-M2, 3=crouch-M1. In the axe stance crouch+M2 is
        // the reel-in HOLD (handled below), not a strike, so it doesn't fire an attack.
        boolean m2Reel = !flailMode && shift;
        if (atkDown && !wasAttackDown) fireAttack(shift ? 3 : 0);
        if (useDown && !wasUseDown && !m2Reel) fireAttack(shift ? 2 : 1);
        wasAttackDown = atkDown;
        wasUseDown = useDown;

        // Reel-in state (crouch+M2 held in the axe stance).
        boolean nowReeling = m2Reel && useDown;
        if (nowReeling != reeling) {
            reeling = nowReeling;
            sendState();
        }

        // Smooth the hand anchor: snapping it straight to the look direction each frame injected huge
        // velocity into the chain when you flicked the mouse, which is what flung the flail around.
        Vec3 targetHand = handPosition(player);
        smoothedHand = smoothedHand == null ? targetHand : smoothedHand.lerp(targetHand, 0.5);
        Vec3 hand = smoothedHand;
        if (sim == null) {
            smoothedHand = targetHand;
            hand = targetHand;
            clientAttack = new GyomeiAttackController();
            sim = new GyomeiWeaponSimulation(hand, player.getLookAngle());
            sim.gripMode = GripMode.AXE;
            // Terrain collision — resolved against whatever level is current, so the chain and both
            // ends drag along blocks instead of clipping through them.
            sim.setCollider((from, to, radius) -> {
                Level lvl = Minecraft.getInstance().level;
                return lvl == null ? to : GyomeiCollision.resolveSwept(lvl, from, to, radius);
            });
        }
        // Mirror the server: reel the ball in/out, and hold the axe (axe stance) or the middle of the
        // chain (flail stance) when not mid-attack.
        sim.lengthScale = approach(sim.lengthScale, reeling ? 0.35 : 1.0, 0.06);
        if (clientAttack.driveGrip(sim, hand, player.getLookAngle())) {
            sim.gripMode = GripMode.CHAIN;
        } else if (flailMode) {
            sim.gripMode = GripMode.CHAIN;
            sim.setGrip(sim.pointCount() / 2, hand);
        } else {
            sim.gripMode = GripMode.AXE;
            sim.setGrip(sim.axeIndex(), hand);
        }
        sim.step();
    }

    /** Resolve the slot for the local stance, play the visual attack, and tell the server to run it. */
    private static void fireAttack(int slot) {
        Attack atk = GyomeiWeaponManager.resolveAttack(slot, flailMode);
        if (atk == Attack.NONE) return;
        clientAttack.trigger(atk); // local visual
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeInt(slot);    // authoritative damage — server resolves against its own stance
            NetworkManager.sendToServer(NichirinPacketRegistry.GYOMEI_ATTACK_ID, NetworkBufferUtils.client(buf));
        } catch (Exception ignored) {
        }
    }

    private static void sendState() {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeBoolean(flailMode);
            buf.writeBoolean(reeling);
            NetworkManager.sendToServer(NichirinPacketRegistry.GYOMEI_STATE_ID, NetworkBufferUtils.client(buf));
        } catch (Exception ignored) {
        }
    }

    private static double approach(double v, double target, double step) {
        if (v < target) return Math.min(target, v + step);
        if (v > target) return Math.max(target, v - step);
        return v;
    }

    /** Body-anchored socket position — shared with the server sim so render and damage agree. */
    private static Vec3 handPosition(LocalPlayer player) {
        return com.xirc.nichirin.common.gyomei.GyomeiWeaponManager.handAnchor(player);
    }
}
