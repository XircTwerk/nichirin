package com.xirc.nichirin.common.attack.moves.demon.destructive;

import com.xirc.nichirin.common.attack.component.AbstractBreathingAttack;
import com.xirc.nichirin.common.entity.MovesetCapableNPC;
import com.xirc.nichirin.common.entity.attack.ShockwaveEntity;
import com.xirc.nichirin.common.entity.effect.CloneRing;
import com.xirc.nichirin.common.entity.effect.PlayerCloneEntity;
import com.xirc.nichirin.common.entity.npc.AkazaEntity;
import com.xirc.nichirin.common.network.s2c.PlayerAnimationPacket;
import com.xirc.nichirin.common.network.s2c.TriggerShaderPacket;
import com.xirc.nichirin.registry.NichirinEntityRegistry;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import com.xirc.nichirin.registry.NichirinParticleRegistry;
import com.xirc.nichirin.registry.NichirinSoundRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Destructive Death final form — Blue Silver Chaotic Afterglow.
 *
 * <p>Wiki: "An omni-directional barrage of countless thin and sharp shockwaves that deliver a
 * hundred blows to the target coming from everywhere almost at the same time." The Infinity
 * Castle adaptation renders it as twelve energy clones of Akaza manifesting at the points of his
 * snowflake compass, punching in unison.</p>
 *
 * <p>A 10-second rooted channel: windup (heartbeats + rising chimes), twelve SENTIENT player
 * clones ({@link PlayerCloneEntity} sentinels with the caster's skin) snapping in one by one
 * around a wide ring, a 6-second rapid-fire shockwave barrage where each clone visibly punches
 * and tracks its victim with its whole body, then a unison volley and convergence finisher with
 * sonic boom + impact frame.</p>
 *
 * <p>Locks onto EVERY Compass-tracked target in range and divides the clones between them in
 * contiguous blocks (two targets → six clones each). Requires Compass Needle + Overdrive active.
 * The first volley force-cancels each target's channeled breathing attack (canon: this move broke
 * Dead Calm through sheer volume). All shockwave effects render red; no knockback so victims stay
 * pinned in the storm. Tuned to delete roughly three Temple Demons in one cast.</p>
 */
public class BlueSilverChaoticAfterglowAttack extends DestructiveDeathAttackBase {

    public static final int TOTAL_TICKS = 200;
    private static final int CLONE_SPAWN_TICK = 40;
    private static final int BARRAGE_START_TICK = 64;
    private static final int BARRAGE_END_TICK = 178;
    private static final int UNISON_TICK = 180;
    private static final int CONVERGE_TICK = 186;
    private static final int VOLLEY_INTERVAL = 2;
    private static final int PUNCHERS_PER_VOLLEY = 3;

    private static final int CLONE_COUNT = 12;
    private static final float RING_RADIUS = 9.0f;
    private static final int CLONE_STAGGER_TICKS = 2;

    private static final float VOLLEY_DAMAGE = 2.0f;
    private static final float VOLLEY_SPEED = 4.5f;
    private static final int VOLLEY_LIFE_TICKS = 10;
    private static final float VOLLEY_HITBOX = 0.55f;
    private static final int VOLLEY_HIT_STUN = 28;
    private static final float UNISON_DAMAGE = 3.0f;

    private static final int STREAK_LIFETIME_TICKS = 8;
    private static final int STREAK_COPIES = 2;
    private static final float STREAK_ALPHA = 0.5f;

    private static final String IMPACT_SHAKE_EFFECT = "com.xirc.nichirin.client.shader.ImpactShakeShaderEffect";
    private static final double SHAKE_AUDIENCE_RANGE_SQR = 40.0 * 40.0;

    private int phaseTick = -1;
    private Vec3 ringCenter;
    private final List<LivingEntity> targets = new ArrayList<>();
    private final CloneRing cloneRing = new CloneRing();
    private boolean failed;

    @Override
    protected void onStart() {
        phaseTick = -1;
        ringCenter = null;
        targets.clear();
        cloneRing.dismiss(world);
        failed = false;
    }

    @Override
    protected void perform() {
        if (world.isClientSide || failed) return;
        LivingEntity caster = user;
        if (caster == null) return;
        phaseTick++;
        int t = phaseTick;

        if (t == 0 && !beginChannel(caster)) {
            failed = true;
            stop();
            return;
        }

        // Drop dead targets; if a clone squad's victim falls, the squad re-aims at survivors.
        int before = targets.size();
        targets.removeIf(e -> e == null || !e.isAlive());
        if (targets.isEmpty()) {
            endEarly();
            return;
        }
        if (targets.size() != before) {
            cloneRing.reassignTargets(this::targetForClone);
        }

        // Rooted for the whole performance (the finisher window lets momentum through).
        if (t < CONVERGE_TICK) {
            rootCaster(caster);
        }

        if (t < CLONE_SPAWN_TICK) {
            tickWindup(caster, t);
        } else if (t == CLONE_SPAWN_TICK) {
            world.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2f, 1.5f);
            tickManifestation(caster, t);
        } else if (t < BARRAGE_START_TICK) {
            tickManifestation(caster, t);
        } else if (t < BARRAGE_END_TICK) {
            tickBarrage(caster, t);
        } else if (t == UNISON_TICK) {
            unisonVolley(caster);
        } else if (t == CONVERGE_TICK) {
            convergeFinisher(caster);
        } else if (t > CONVERGE_TICK) {
            tickAfterglow(caster, t);
        }
    }

    /**
     * Pre-execution gate, called from the moveset action BEFORE the move executes so a failed
     * requirement never burns the long cooldown. Sends the failure reason to the player.
     */
    public static boolean canCast(LivingEntity entity) {
        if (entity instanceof AkazaEntity akaza) {
            LivingEntity target = akaza.getTarget();
            return target != null && target.isAlive();
        }
        if (!(entity instanceof ServerPlayer sp)) return false;
        long now = sp.level().getGameTime();
        if (!DestructiveDeathState.isCompassActive(sp.getUUID(), now)) {
            sp.displayClientMessage(Component.literal("Compass Needle must be active!")
                    .withStyle(s -> s.withColor(0xFF5555)), true);
            return false;
        }
        if (!DestructiveDeathState.isOverdriveEnabled(sp.getUUID(), now)) {
            sp.displayClientMessage(Component.literal("Overdrive must be active!")
                    .withStyle(s -> s.withColor(0xFF5555)), true);
            return false;
        }
        if (trackedTargetsInRange(sp).isEmpty()) {
            sp.displayClientMessage(Component.literal("No tracked target in range!")
                    .withStyle(s -> s.withColor(0xFF5555)), true);
            return false;
        }
        return true;
    }

    /** Re-validates (state may have expired between press and tick) and locks every target. */
    private boolean beginChannel(LivingEntity caster) {
        if (!canCast(caster)) return false;
        if (caster instanceof ServerPlayer sp) {
            targets.addAll(trackedTargetsInRange(sp));
        } else if (caster instanceof AkazaEntity akaza) {
            targets.add(akaza.getTarget());
        }
        ringCenter = caster.position();
        world.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.4f, 0.7f);
        return true;
    }

    private void tickWindup(LivingEntity caster, int t) {
        if (t == 20) {
            world.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.4f, 0.9f);
        }
        // Rising chime ladder — the audible "something terrible is charging" cue.
        if (t % 8 == 0) {
            world.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
                    1.2f, 0.6f + (t / (float) CLONE_SPAWN_TICK) * 1.2f);
        }
        if (world instanceof ServerLevel sl) {
            // Energy gathering: motes spiral in toward the caster's chest.
            double angle = t * 0.7;
            double r = 2.2 - (t / (float) CLONE_SPAWN_TICK) * 1.6;
            sl.sendParticles(NichirinParticleRegistry.BLUE_FLASH1.get(),
                    caster.getX() + Math.cos(angle) * r,
                    caster.getY() + 0.4 + (t % 20) * 0.06,
                    caster.getZ() + Math.sin(angle) * r,
                    2, 0.05, 0.05, 0.05, 0.0);
        }
    }

    /** Sentinel clones snap in one by one — real entities that track their victims. */
    private void tickManifestation(LivingEntity caster, int t) {
        int sinceSpawn = t - CLONE_SPAWN_TICK;
        if (sinceSpawn % CLONE_STAGGER_TICKS != 0) return;
        int cloneIndex = sinceSpawn / CLONE_STAGGER_TICKS;
        if (cloneIndex >= CLONE_COUNT) return;

        Vec3 pos = ringSlot(cloneIndex);
        if (caster instanceof AkazaEntity) {
            NichirinPacketRegistry.sendAfterimageTrail(caster, pos, pos,
                    BARRAGE_START_TICK - t + 8, 1, 0.65f);
            if (world instanceof ServerLevel sl) {
                sl.sendParticles(NichirinParticleRegistry.BLUE_FLASH1.get(),
                        pos.x, pos.y + 1.0, pos.z, 4, 0.25, 0.5, 0.25, 0.0);
            }
        } else {
            cloneRing.spawn(world, caster, pos, targetForClone(cloneIndex), TOTAL_TICKS - t + 10);
        }
        world.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.9f,
                1.0f + cloneIndex * 0.08f);
    }

    private void tickBarrage(LivingEntity caster, int t) {
        int sinceBarrage = t - BARRAGE_START_TICK;
        if (sinceBarrage % VOLLEY_INTERVAL != 0) return;
        int volleyIndex = sinceBarrage / VOLLEY_INTERVAL;
        float progress = sinceBarrage / (float) (BARRAGE_END_TICK - BARRAGE_START_TICK);

        // The first volley of the storm breaks every target's channel — canon: this move
        // overwhelmed Dead Calm through volume alone.
        if (volleyIndex == 0) {
            for (LivingEntity target : targets) {
                if (target instanceof Player targetPlayer
                        && AbstractBreathingAttack.cancelActiveAttack(targetPlayer)) {
                    world.playSound(null, target.getX(), target.getY(), target.getZ(),
                            NichirinSoundRegistry.PARRY_CLASH_2.get(), SoundSource.PLAYERS, 1.5f, 0.5f);
                }
            }
        }

        for (int k = 0; k < PUNCHERS_PER_VOLLEY; k++) {
            // ×5 stride is coprime with 12, so the punch order cycles every clone before repeating.
            int cloneIndex = (volleyIndex * 5 + k * 7) % CLONE_COUNT;
            punchFromClone(caster, cloneIndex, VOLLEY_DAMAGE, VOLLEY_SPEED, progress);
        }

        // Screen shake pulses ramp with the storm.
        if (volleyIndex % 8 == 0) {
            sendImpactShake(0.25f + progress * 0.55f);
        }
        // The caster throws snap punches in place, synced to the storm.
        if (sinceBarrage % 8 == 0) {
            if (caster instanceof ServerPlayer sp) {
                NichirinPacketRegistry.broadcastPlayerAnimation(sp,
                        new PlayerAnimationPacket(sp.getId(), "snap_punch"));
            } else if (caster instanceof MovesetCapableNPC npc) {
                npc.triggerMovesetAnimation("snap_punch");
            }
        }
    }

    /** One clone punch: visible swing + afterimage streak + a thin red shockwave at ITS target. */
    private void punchFromClone(LivingEntity caster, int cloneIndex,
                                float damage, float speed, float pitchProgress) {
        LivingEntity target = targetForClone(cloneIndex);
        if (target == null) return;
        Vec3 origin = cloneRing.punchOrigin(cloneIndex, ringSlot(cloneIndex).add(0, 1.4, 0));
        Vec3 targetPos = target.position().add(0, target.getBbHeight() * 0.55, 0);
        Vec3 dir = targetPos.subtract(origin).normalize();

        cloneRing.swing(cloneIndex);
        NichirinPacketRegistry.sendAfterimageTrail(caster, origin, targetPos,
                STREAK_LIFETIME_TICKS, STREAK_COPIES, STREAK_ALPHA);

        new ShockwaveEntity.Builder()
                .owner(caster)
                .origin(origin)
                .direction(dir)
                .damage(damage * compassFinalScalar())
                .knockback(0.0f)       // pinned in the storm — no clone-aimed push
                .noVanillaKnockback()  // ...and suppress vanilla hurt knockback too
                .hitStun(VOLLEY_HIT_STUN)
                .speed(speed)
                .lifeTicks(VOLLEY_LIFE_TICKS)
                .hitboxRadius(VOLLEY_HITBOX)
                .pierces(0)
                .phaseWalls(true)
                .red(true)
                .spawn(NichirinEntityRegistry.SHOCKWAVE.get(), world);

        var whoosh = (cloneIndex % 2 == 0)
                ? NichirinSoundRegistry.SLASH_WHOOSH_1.get()
                : NichirinSoundRegistry.SLASH_WHOOSH_2.get();
        world.playSound(null, origin.x, origin.y, origin.z,
                whoosh, SoundSource.PLAYERS, 0.9f, 1.2f + pitchProgress * 0.7f);
    }

    /** All twelve clones fire at once — the adaptation's unison shot. */
    private void unisonVolley(LivingEntity caster) {
        for (int i = 0; i < CLONE_COUNT; i++) {
            punchFromClone(caster, i, UNISON_DAMAGE, VOLLEY_SPEED + 0.5f, 1.0f);
        }
        LivingEntity loudest = targets.get(0);
        world.playSound(null, loudest.getX(), loudest.getY(), loudest.getZ(),
                NichirinSoundRegistry.PARRY_CLASH.get(), SoundSource.PLAYERS, 1.6f, 0.6f);
        sendImpactShake(1.0f);
    }

    /** Clones collapse into the caster; the finisher lands on every target at once. */
    private void convergeFinisher(LivingEntity caster) {
        Vec3 home = caster.position().add(0, caster.getBbHeight() * 0.55, 0);
        for (int i = 0; i < CLONE_COUNT; i += 3) {
            PlayerCloneEntity clone = cloneRing.get(i);
            if (clone != null) {
                NichirinPacketRegistry.sendAfterimageTrail(caster,
                        clone.position().add(0, 1.0, 0), home,
                        STREAK_LIFETIME_TICKS, STREAK_COPIES, STREAK_ALPHA);
            }
        }
        cloneRing.dismiss(world);
        sendImpactShake(1.6f);

        float finisherDamage = this.damage * compassFinalScalar();
        for (LivingEntity target : new ArrayList<>(targets)) {
            Vec3 targetPos = target.position().add(0, target.getBbHeight() * 0.55, 0);
            world.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                    SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.8f, 0.7f);
            if (world instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SONIC_BOOM,
                        targetPos.x, targetPos.y, targetPos.z, 1, 0.0, 0.0, 0.0, 0.0);
            }

            this.damage = finisherDamage;
            hitTargetNoImmunity(target);
            target.setDeltaMovement(target.getDeltaMovement().x * 0.2, 0.95, target.getDeltaMovement().z * 0.2);
            target.hurtMarked = true;
            target.hasImpulse = true;
            if (target instanceof ServerPlayer tp) {
                tp.connection.send(new ClientboundSetEntityMotionPacket(target));
            }
        }
    }

    /** Red motes drifting upward around targets and caster — the afterglow settles. */
    private void tickAfterglow(LivingEntity caster, int t) {
        if (!(world instanceof ServerLevel sl) || t % 2 != 0) return;
        for (LivingEntity target : targets) {
            sl.sendParticles(NichirinParticleRegistry.FLASH1.get(),
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    3, 0.8, 1.0, 0.8, 0.01);
        }
        sl.sendParticles(NichirinParticleRegistry.FLASH1.get(),
                caster.getX(), caster.getY() + 1.2, caster.getZ(),
                2, 0.6, 0.8, 0.6, 0.01);
    }

    /**
     * Contiguous-block clone assignment: clone i belongs to target (i × n / 12), so two targets
     * split the ring six and six, three targets four each, and so on.
     */
    private LivingEntity targetForClone(int cloneIndex) {
        if (targets.isEmpty()) return null;
        int targetIndex = cloneIndex * targets.size() / CLONE_COUNT;
        return targets.get(Math.min(targetIndex, targets.size() - 1));
    }

    /** Fixed ring position for clone {@code index} around the cast point. */
    private Vec3 ringSlot(int index) {
        double angle = 2.0 * Math.PI * index / CLONE_COUNT;
        return ringCenter.add(Math.cos(angle) * RING_RADIUS, 0, Math.sin(angle) * RING_RADIUS);
    }

    private void rootCaster(LivingEntity caster) {
        caster.setDeltaMovement(0, Math.min(caster.getDeltaMovement().y, 0), 0);
        caster.hurtMarked = true;
        if (caster instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
        }
    }

    /** Every target gone mid-channel: dismiss the clones and end without the finisher. */
    private void endEarly() {
        cloneRing.dismiss(world);
        stop();
    }

    private void sendImpactShake(float magnitude) {
        if (!(world instanceof ServerLevel serverLevel)) return;
        Vec3 epicentre = user.position();
        for (ServerPlayer viewer : serverLevel.players()) {
            if (viewer.distanceToSqr(epicentre) <= SHAKE_AUDIENCE_RANGE_SQR) {
                NichirinPacketRegistry.sendToPlayer(
                        new TriggerShaderPacket(IMPACT_SHAKE_EFFECT, true, magnitude), viewer);
            }
        }
    }

    /** Every Compass-tracked living entity in range, nearest first, capped at one per clone. */
    private static List<LivingEntity> trackedTargetsInRange(ServerPlayer sp) {
        Set<UUID> tracked = CompassNeedleTracker.getTrackedTargets(sp.getUUID());
        if (tracked.isEmpty()) return List.of();
        AABB search = sp.getBoundingBox().inflate(30.0);
        List<LivingEntity> candidates = sp.level().getEntitiesOfClass(LivingEntity.class, search,
                e -> e != sp && e.isAlive() && tracked.contains(e.getUUID()));
        candidates.sort(Comparator.comparingDouble(e -> e.distanceToSqr(sp)));
        return candidates.size() > CLONE_COUNT ? candidates.subList(0, CLONE_COUNT) : candidates;
    }

    /** +35% damage scalar while Compass Overdrive is on. */
    private float compassFinalScalar() {
        if (!(user instanceof ServerPlayer sp)) return 1.0f;
        return DestructiveDeathState.isCompassOverdriveActive(sp.getUUID(), world.getGameTime()) ? 1.35f : 1.0f;
    }

    @Override
    protected void onStop() {
        // Safety: never leave clones behind if the channel is interrupted.
        if (!world.isClientSide) {
            cloneRing.dismiss(world);
        }
        phaseTick = -1;
        targets.clear();
        ringCenter = null;
        if (user instanceof AkazaEntity akaza) {
            akaza.completeFinalAfterglow();
        }
    }
}
