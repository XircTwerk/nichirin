package com.xirc.nichirin.common.entity.effect;

import com.xirc.nichirin.registry.NichirinEntityRegistry;
import com.xirc.nichirin.registry.NichirinParticleRegistry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

/**
 * Server-side manager for a ring of sentinel {@link PlayerCloneEntity} clones — the kind that
 * stand anchored, wear the caster's skin/equipment, and track a watch target with their whole
 * body. Encapsulates the spawn / target-assignment / swing / dismissal bookkeeping so attack code
 * (e.g. Blue Silver Chaotic Afterglow) doesn't juggle raw entities.
 *
 * <p>Slots are indexed {@code 0..size-1}. A slot may be empty (not yet manifested) until
 * {@link #spawn} is called for it; spawning out of order is fine.</p>
 */
public final class CloneRing {

    private final List<PlayerCloneEntity> clones = new ArrayList<>();
    private final ParticleOptions flashParticle;

    public CloneRing() {
        this(NichirinParticleRegistry.FLASH1.get());
    }

    public CloneRing(ParticleOptions flashParticle) {
        this.flashParticle = flashParticle;
    }

    public int size() {
        return clones.size();
    }

    public boolean isEmpty() {
        return clones.isEmpty();
    }

    /**
     * Manifests one sentinel clone at {@code pos} watching {@code watchTarget}, copies the
     * source's skin parts + equipment, adds it to the world, tracks it, and pops a flash.
     */
    public PlayerCloneEntity spawn(Level level, LivingEntity source, Vec3 pos,
                                   LivingEntity watchTarget, int lifetimeTicks) {
        PlayerCloneEntity clone = PlayerCloneEntity.createSentinel(
                NichirinEntityRegistry.PLAYER_CLONE.get(), level, source, pos, lifetimeTicks);
        clone.setWatchTarget(watchTarget);
        level.addFreshEntity(clone);
        clone.copyEquipmentFrom(source); // after spawn so the equipment packet reaches clients
        clones.add(clone);
        flash(level, pos);
        return clone;
    }

    /** The clone occupying {@code index}, or null when empty / despawned. */
    public PlayerCloneEntity get(int index) {
        if (index < 0 || index >= clones.size()) return null;
        PlayerCloneEntity clone = clones.get(index);
        return clone.isAlive() ? clone : null;
    }

    /** Punch origin of clone {@code index} (fist height), or {@code fallback} if it's gone. */
    public Vec3 punchOrigin(int index, Vec3 fallback) {
        PlayerCloneEntity clone = get(index);
        return clone != null ? clone.position().add(0, clone.getBbHeight() * 0.8, 0) : fallback;
    }

    /** Triggers a visible swing on clone {@code index} (no-op if it's gone). */
    public void swing(int index) {
        PlayerCloneEntity clone = get(index);
        if (clone != null) clone.triggerSwing();
    }

    /** Re-aims every living clone using {@code targetFor(slotIndex)} (e.g. after a target dies). */
    public void reassignTargets(IntFunction<LivingEntity> targetFor) {
        for (int i = 0; i < clones.size(); i++) {
            PlayerCloneEntity clone = clones.get(i);
            if (clone.isAlive()) clone.setWatchTarget(targetFor.apply(i));
        }
    }

    /** Flash-bursts and discards every living clone, then clears the ring. */
    public void dismiss(Level level) {
        for (PlayerCloneEntity clone : clones) {
            if (clone.isAlive()) {
                flash(level, clone.position());
                clone.discard();
            }
        }
        clones.clear();
    }

    private void flash(Level level, Vec3 pos) {
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(flashParticle, pos.x, pos.y + 1.0, pos.z, 4, 0.25, 0.5, 0.25, 0.0);
        }
    }
}
