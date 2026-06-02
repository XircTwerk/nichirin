package com.xirc.nichirin.common.system;

import com.xirc.nichirin.registry.NichirinEffectRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks active demon grabs server-side.
 * <p>
 * Lifecycle:
 * <ol>
 *   <li>{@link #startGrab} — demon grabs a target; target is held in place each tick.</li>
 *   <li>{@link #tick} — called every server tick per demon; repositions target and applies stun.</li>
 *   <li>{@link #releaseGrab} — ends the grab; optionally launches the target.</li>
 * </ol>
 */
public class GrabManager {

    private static final Map<UUID, GrabData> activeGrabs = new HashMap<>();

    public static class GrabData {
        public final UUID grabbedEntityId;
        public int ticksRemaining;

        GrabData(UUID grabbedId, int duration) {
            this.grabbedEntityId = grabbedId;
            this.ticksRemaining  = duration;
        }
    }

    // Public API

    public static void startGrab(LivingEntity demon, LivingEntity target, int durationTicks) {
        releaseGrab(demon, false); // clean up any previous grab first
        activeGrabs.put(demon.getUUID(), new GrabData(target.getUUID(), durationTicks));
        target.setDeltaMovement(0, 0, 0);
    }

    public static boolean isGrabbing(LivingEntity demon) {
        return activeGrabs.containsKey(demon.getUUID());
    }

    public static LivingEntity getGrabbedTarget(LivingEntity demon) {
        GrabData data = activeGrabs.get(demon.getUUID());
        if (data == null) return null;
        return resolveTarget(demon, data);
    }

    /**
     * Releases the grab. If {@code launch} is true the grabbed entity is thrown forward-upward.
     */
    public static void releaseGrab(LivingEntity demon, boolean launch) {
        GrabData data = activeGrabs.remove(demon.getUUID());
        if (data == null) return;
        LivingEntity target = findEntity(demon, data.grabbedEntityId);
        if (target == null) return;
        if (launch) {
            // Remove stun before launching so it doesn't suppress the velocity
            target.removeEffect(NichirinEffectRegistry.stunned());
            Vec3 forward = demon.getLookAngle();
            target.setDeltaMovement(forward.x * 1.5, 1.2, forward.z * 1.5);
            target.hurtMarked  = true;
            target.hasImpulse  = true;
        }
    }

    /**
     * Must be called once per server tick for every TempleDemonEntity that is alive.
     */
    public static void tick(LivingEntity demon) {
        GrabData data = activeGrabs.get(demon.getUUID());
        if (data == null) return;

        LivingEntity target = resolveTarget(demon, data);
        if (target == null) return; // resolveTarget already cleaned up

        // Pin target just in front of the demon at chest height
        Vec3 holdPos = demon.position()
                .add(demon.getLookAngle().scale(1.3))
                .add(0, demon.getBbHeight() * 0.35, 0);
        target.teleportTo(holdPos.x, holdPos.y, holdPos.z);
        target.setDeltaMovement(0, 0, 0);

        // Continuously stun (short duration re-applies each tick)
        target.addEffect(new MobEffectInstance(
                NichirinEffectRegistry.stunned(), 10, 1, false, false));

        data.ticksRemaining--;
        if (data.ticksRemaining <= 0) {
            releaseGrab(demon, true); // auto-release with launch after hold expires
        }
    }

    /** Call when any entity is removed from the world. */
    public static void onEntityRemoved(LivingEntity entity) {
        UUID id = entity.getUUID();
        activeGrabs.remove(id);
        activeGrabs.entrySet().removeIf(e -> e.getValue().grabbedEntityId.equals(id));
    }

    // Helpers

    private static LivingEntity resolveTarget(LivingEntity demon, GrabData data) {
        LivingEntity target = findEntity(demon, data.grabbedEntityId);
        if (target == null || !target.isAlive()) {
            activeGrabs.remove(demon.getUUID());
            return null;
        }
        return target;
    }

    private static LivingEntity findEntity(LivingEntity from, UUID id) {
        if (!(from.level() instanceof ServerLevel serverLevel)) return null;
        Entity e = serverLevel.getEntity(id);
        return (e instanceof LivingEntity living) ? living : null;
    }
}