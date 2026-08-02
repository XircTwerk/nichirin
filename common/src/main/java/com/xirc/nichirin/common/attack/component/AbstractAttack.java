package com.xirc.nichirin.common.attack.component;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.client.renderer.effects.AttackHitboxRenderer;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset.MoveConfiguration;
import com.xirc.nichirin.common.network.s2c.PlayerAnimationPacket;
import com.xirc.nichirin.common.util.AttackInterruptTracker;
import com.xirc.nichirin.common.util.HitboxData;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared base for all attack types (breathing, demon, CQC).
 * Owns: common combat fields, rotation-aware hitbox geometry, interrupt/armor logic,
 * self-ticking infrastructure, and the shared windup/duration tick loop.
 */
@Getter
public abstract class AbstractAttack {


    protected float damage;
    protected float range;
    protected float knockback;
    protected int hitStun;
    protected int armorHits;
    protected boolean hyperArmor;
    protected boolean slam;
    protected float hitboxSize;
    protected int cooldown;
    protected int windup;
    protected int duration;

    // Movement fields are nullable because not all attacks use these.
    protected Float teleportDistance;
    protected Float dashSpeed;
    protected Integer teleportWindup;


    @Setter protected boolean isActive = false;
    protected int tickCount = 0;
    protected LivingEntity user;
    protected Level world;

    @Setter private Set<UUID> hitEntities = new HashSet<>();
    @Setter private int hitCount = 0;
    private int absorbedInterruptHits = 0;
    private long lastArmorInterruptTick = Long.MIN_VALUE;
    private boolean activeStartFired = false;
    protected boolean configured = false;
    protected long comboSequence = 0L;


    private static final ConcurrentHashMap<UUID, List<AbstractAttack>> selfTickingAttacks =
            new ConcurrentHashMap<>();


    /**
     * Sets all common combat fields from a MoveConfiguration.
     * Subclasses call this, then add type-specific fields.
     * Respects the {@code configured} guard; safe to call multiple times.
     */
    protected void configureCommon(MoveConfiguration config) {
        if (configured) return;
        if (config == null) { configured = true; return; }
        this.damage        = config.getDamageOrDefault(0f);
        this.range         = config.getRangeOrDefault(0f);
        this.knockback     = config.getKnockbackOrDefault(0f);
        this.hitStun       = config.getHitStunOrDefault(0);
        this.armorHits     = config.getArmorOrDefault(0);
        this.hyperArmor    = config.hasHyperArmor();
        this.slam          = config.hasSlam();
        this.hitboxSize    = config.getHitboxSizeOrDefault(0f);
        this.cooldown      = config.getCooldownOrDefault(0);
        this.windup        = config.getWindupOrDefault(0);
        this.duration      = config.getDurationOrDefault(0);
        this.teleportDistance = config.getTeleportDistance();
        this.dashSpeed        = config.getDashSpeed();
        this.teleportWindup   = config.getTeleportWindup();
        this.configured = true;
    }

    public void applyDamageAndStunMultiplier(float multiplier) {
        if (multiplier == 1.0f) return;
        this.damage *= multiplier;
        if (this.hitStun > 0) {
            this.hitStun = Math.max(1, Math.round(this.hitStun * multiplier));
        }
    }


    protected void resetTickState() {
        tickCount = 0;
        activeStartFired = false;
        absorbedInterruptHits = 0;
        lastArmorInterruptTick = Long.MIN_VALUE;
        hitEntities.clear();
        hitCount = 0;
    }

    /**
     * Shared windup/duration tick loop used by Breathing and Demon attacks.
     * CQC uses an external tick(LivingEntity) instead and does not call this.
     */
    public void tick() {
        if (!isActive || user == null || world == null) return;
        if (!user.isAlive()) { stop(); return; }
        if (!shouldContinueTick()) { stop(); return; }

        tickCount++;

        boolean externallyStunned = isExternallyStunned();
        boolean interrupted = AttackInterruptTracker.wasInterruptedThisTick(user);

        if (shouldStopForInterrupt(externallyStunned, false, false)) { stop(true); return; }
        if (tickCount <= windup && windup > 0 && shouldStopForInterrupt(false, interrupted, true)) {
            stop(true); return;
        }

        if (tickCount > windup) {
            if (!activeStartFired) {
                activeStartFired = true;
                try { onActiveStart(); } catch (Exception e) { BreathOfNichirin.LOGGER.error("Error in attack onActiveStart", e); }
            }
            try { perform(); } catch (Exception e) { BreathOfNichirin.LOGGER.error("Error in attack perform", e); }
        }

        if (tickCount >= windup + duration) stop(false);
    }

    public void stop() { stop(true); }

    protected void stop(boolean stopAnimation) {
        if (isActive) {
            isActive = false;
            unregisterFromTicking();
            try { onStop(); } catch (Exception e) { BreathOfNichirin.LOGGER.error("Error in attack onStop", e); }
            if (stopAnimation) stopPlayerAnimation();
        }
    }

    protected void stopPlayerAnimation() {
        if (world != null && !world.isClientSide && user instanceof ServerPlayer sp) {
            NichirinPacketRegistry.broadcastPlayerAnimation(sp, new PlayerAnimationPacket(sp.getId(), ""));
        }
    }


    /** Called at attack start (after resource checks). */
    protected void onStart() {}

    /** Called once when windup completes and active phase begins. */
    protected void onActiveStart() {}

    /** Called when attack ends. */
    protected void onStop() {}

    /**
     * Return false to stop the attack mid-tick.
     * Demon attacks override this to check isDemon.
     */
    protected boolean shouldContinueTick() { return true; }

    /** Default perform: hits all targets at range. Override for custom patterns. */
    protected void perform() {
        for (LivingEntity t : getTargetsAtRange()) hitTarget(t);
    }

    /** Apply damage and effects to a target, respecting immunity frames. */
    protected void hitTarget(LivingEntity target) {}

    /** Apply damage to a target, bypassing immunity frames. */
    protected void hitTargetNoImmunity(LivingEntity target) {}


    protected boolean isExternallyStunned() {
        MobEffectInstance stun = user.getEffect(NichirinEffectRegistry.stunned());
        return stun != null && stun.getAmplifier() > 0;
    }

    protected boolean shouldStopForInterrupt(boolean externallyStunned,
                                             boolean interruptedThisTick,
                                             boolean windupOnly) {
        if (!externallyStunned && !interruptedThisTick) return false;
        if (windupOnly && !interruptedThisTick) return false;
        if (hyperArmor) {
            if (externallyStunned) user.removeEffect(NichirinEffectRegistry.stunned());
            return false;
        }
        if (armorHits > 0) {
            long now = user.level().getGameTime();
            if (lastArmorInterruptTick != now) {
                absorbedInterruptHits++;
                lastArmorInterruptTick = now;
            }
            if (absorbedInterruptHits <= armorHits) {
                if (externallyStunned) user.removeEffect(NichirinEffectRegistry.stunned());
                return false;
            }
        }
        return true;
    }


    /** Applies slam or stun effect for hitStun ticks. */
    protected void applyHitStun(LivingEntity target) {
        if (hitStun <= 0) return;
        target.invulnerableTime = hitStun;
        target.addEffect(new MobEffectInstance(
                slam ? NichirinEffectRegistry.slammed() : NichirinEffectRegistry.stunned(),
                hitStun, slam ? 0 : 2, false, false, true));
    }

    protected void applyKnockback(LivingEntity target) {
        if (knockback > 0 && user != null) {
            Vec3 dir = target.position().subtract(user.position()).normalize();
            target.push(dir.x * knockback, 0.1, dir.z * knockback);
        }
    }


    protected List<LivingEntity> getTargetsInHitbox(Vec3 center) {
        Vec3 look = user.getLookAngle();
        float yaw = (float) Math.toRadians(user.getYRot());
        AABB hitbox = new HitboxData(hitboxSize).createAABB(center, look, yaw);
        sendHitbox(hitbox);
        return world.getEntitiesOfClass(LivingEntity.class, hitbox, e -> e != user && e.isAlive());
    }

    protected List<LivingEntity> getTargetsInCustomHitbox(Vec3 center, float size, HitboxData.HitboxShape shape) {
        Vec3 look = user.getLookAngle();
        float yaw = (float) Math.toRadians(user.getYRot());
        AABB hitbox = new HitboxData(size, shape).createAABB(center, look, yaw);
        sendHitbox(hitbox);
        return world.getEntitiesOfClass(LivingEntity.class, hitbox, e -> e != user && e.isAlive());
    }

    protected List<LivingEntity> getTargetsInCustomHitbox(Vec3 center, double width, double height, double depth) {
        return getTargetsInCustomHitbox(center,
                (float) Math.max(width, Math.max(height, depth)), HitboxData.HitboxShape.CUBE);
    }

    protected List<LivingEntity> getTargetsInLine(Vec3 start, Vec3 end, double thickness) {
        double dist = start.distanceTo(end);
        int count = Math.max(1, (int) Math.ceil(dist / Math.max(thickness * 0.5, 0.1)));
        Set<LivingEntity> all = new HashSet<>();
        Set<AABB> boxes = new HashSet<>();
        Vec3 dir = end.subtract(start).normalize();
        float lineYaw = (float) Math.atan2(-dir.x, dir.z);
        for (int i = 0; i <= count; i++) {
            Vec3 mid = start.lerp(end, count > 0 ? (double) i / count : 0);
            AABB box = new HitboxData((float) thickness, HitboxData.HitboxShape.CUBE)
                    .createAABB(mid, dir, lineYaw);
            boxes.add(box);
            all.addAll(world.getEntitiesOfClass(LivingEntity.class, box, e -> e != user && e.isAlive()));
        }
        sendHitboxes(boxes);
        return new ArrayList<>(all);
    }

    protected List<LivingEntity> getTargetsAtRange() {
        Vec3 pos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 look = user.getLookAngle();
        Vec3 center = pos.add(look.scale(range));
        float yaw = (float) Math.toRadians(user.getYRot());
        AABB hitbox = new HitboxData(hitboxSize).createAABB(center, look, yaw);
        sendHitbox(hitbox);
        return world.getEntitiesOfClass(LivingEntity.class, hitbox, e -> e != user && e.isAlive());
    }

    protected List<LivingEntity> getTargetsAtRange(HitboxData.HitboxShape shape) {
        Vec3 pos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 center = pos.add(user.getLookAngle().scale(range));
        return getTargetsInCustomHitbox(center, hitboxSize, shape);
    }

    protected List<LivingEntity> getTargetsInRangeLine(float spacing) {
        Vec3 pos = user.position().add(0, user.getBbHeight() / 2, 0);
        return getTargetsInLine(pos, pos.add(user.getLookAngle().scale(range)), spacing);
    }

    protected List<LivingEntity> getTargetsInCone(float coneAngle, int hitboxCount) {
        Vec3 pos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 look = user.getLookAngle();
        float playerYaw = (float) Math.toRadians(user.getYRot());
        Set<LivingEntity> all = new HashSet<>();
        Set<AABB> boxes = new HashSet<>();
        float step = hitboxCount > 1 ? coneAngle / (hitboxCount - 1) : 0;
        float startAngle = -coneAngle / 2;
        for (int i = 0; i < hitboxCount; i++) {
            float angle = startAngle + i * step;
            float boxYaw = playerYaw + (float) Math.toRadians(angle);
            Vec3 rotDir = rotateVectorY(look, Math.toRadians(angle));
            Vec3 center = pos.add(rotDir.scale(range));
            AABB box = new HitboxData(hitboxSize).createAABB(center, rotDir, boxYaw);
            boxes.add(box);
            all.addAll(world.getEntitiesOfClass(LivingEntity.class, box, e -> e != user && e.isAlive()));
        }
        sendHitboxes(boxes);
        return new ArrayList<>(all);
    }

    protected List<LivingEntity> getTargetsInSweep(float sweepAngle, float sweepRange, int hitboxCount) {
        Vec3 pos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 look = user.getLookAngle();
        float playerYaw = (float) Math.toRadians(user.getYRot());
        Set<LivingEntity> all = new HashSet<>();
        Set<AABB> boxes = new HashSet<>();
        float step = hitboxCount > 1 ? sweepAngle / (hitboxCount - 1) : 0;
        float startAngle = -sweepAngle / 2;
        for (int i = 0; i < hitboxCount; i++) {
            float angle = startAngle + i * step;
            float boxYaw = playerYaw + (float) Math.toRadians(angle);
            Vec3 rotDir = rotateVectorY(look, Math.toRadians(angle));
            Vec3 center = pos.add(rotDir.scale(sweepRange));
            AABB box = new HitboxData(hitboxSize, HitboxData.HitboxShape.CUBE).createAABB(center, rotDir, boxYaw);
            boxes.add(box);
            all.addAll(world.getEntitiesOfClass(LivingEntity.class, box, e -> e != user && e.isAlive()));
        }
        sendHitboxes(boxes);
        return new ArrayList<>(all);
    }

    protected List<LivingEntity> getTargetsInThrust() {
        Vec3 pos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 look = user.getLookAngle();
        Vec3 center = pos.add(look.scale(range / 2));
        float yaw = (float) Math.toRadians(user.getYRot());
        AABB hitbox = new HitboxData(range, HitboxData.HitboxShape.CUBE).createAABB(center, look, yaw);
        sendHitbox(hitbox);
        return world.getEntitiesOfClass(LivingEntity.class, hitbox, e -> e != user && e.isAlive());
    }

    protected List<LivingEntity> getTargetsInCircle(float radius, int hitboxCount) {
        Vec3 pos = user.position().add(0, user.getBbHeight() / 2, 0);
        Set<LivingEntity> all = new HashSet<>();
        Set<AABB> boxes = new HashSet<>();
        float step = 360f / hitboxCount;
        for (int i = 0; i < hitboxCount; i++) {
            float angle = (float) Math.toRadians(i * step);
            Vec3 center = pos.add(radius * Math.cos(angle), 0, radius * Math.sin(angle));
            Vec3 dir = center.subtract(pos).normalize();
            AABB box = new HitboxData(hitboxSize).createAABB(center, dir, angle);
            boxes.add(box);
            all.addAll(world.getEntitiesOfClass(LivingEntity.class, box, e -> e != user && e.isAlive()));
        }
        sendHitboxes(boxes);
        return new ArrayList<>(all);
    }

    protected Vec3 rotateVectorY(Vec3 v, double rad) {
        double cos = Math.cos(rad), sin = Math.sin(rad);
        return new Vec3(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos);
    }

    private void sendHitbox(AABB box) {
        if (world == null || user == null) return;
        if (world.isClientSide) AttackHitboxRenderer.addHitbox(box);
        else NichirinPacketRegistry.sendHitboxToTracking(user, box, 2500L);
    }

    private void sendHitboxes(Set<AABB> boxes) {
        if (world == null || user == null) return;
        if (world.isClientSide) AttackHitboxRenderer.addHitboxes(boxes);
        else for (AABB b : boxes) NichirinPacketRegistry.sendHitboxToTracking(user, b, 2500L);
    }


    public int getTotalDuration() { return windup + duration; }
    public boolean isInWindup() { return isActive && tickCount <= windup; }
    public boolean isInActivePhase() { return isActive && tickCount > windup && tickCount < windup + duration; }
    public boolean hasTeleport() { return teleportDistance != null && teleportDistance > 0; }
    public boolean hasDash() { return dashSpeed != null && dashSpeed > 0; }
    public boolean hasTeleportWindup() { return teleportWindup != null; }


    protected void registerForTicking() {
        if (user != null)
            selfTickingAttacks.computeIfAbsent(user.getUUID(), k -> new ArrayList<>()).add(this);
    }

    protected void unregisterFromTicking() {
        if (user != null) {
            var list = selfTickingAttacks.get(user.getUUID());
            if (list != null) {
                list.remove(this);
                if (list.isEmpty()) selfTickingAttacks.remove(user.getUUID());
            }
        }
    }

    public static void tickAllActiveAttacks(MinecraftServer server) {
        if (selfTickingAttacks.isEmpty()) return;
        List<UUID> toClean = new ArrayList<>();
        for (var entry : selfTickingAttacks.entrySet()) {
            UUID uuid = entry.getKey();
            var attacks = entry.getValue();
            if (attacks.isEmpty()) { toClean.add(uuid); continue; }
            LivingEntity entity = findEntityByUUID(server, uuid);
            if (entity == null || !entity.isAlive()) { toClean.add(uuid); continue; }
            List<AbstractAttack> toRemove = new ArrayList<>();
            synchronized (attacks) {
                for (var attack : new ArrayList<>(attacks)) {
                    try {
                        if (attack.isActive()) attack.tick();
                        else toRemove.add(attack);
                    } catch (Exception e) {
                        BreathOfNichirin.LOGGER.error("Error ticking attack", e);
                        toRemove.add(attack);
                    }
                }
                attacks.removeAll(toRemove);
            }
        }
        for (UUID uuid : toClean) selfTickingAttacks.remove(uuid);
    }

    private static LivingEntity findEntityByUUID(MinecraftServer server, UUID uuid) {
        for (var level : server.getAllLevels()) {
            var e = level.getEntity(uuid);
            if (e instanceof LivingEntity le) return le;
        }
        return null;
    }

    public static void clearSelfTickingAttacks(LivingEntity entity) {
        var attacks = selfTickingAttacks.remove(entity.getUUID());
        if (attacks != null)
            for (var a : attacks) try { a.stop(); } catch (Exception ignored) {}
    }

    public static void clearSelfTickingAttacks(Player player) {
        clearSelfTickingAttacks((LivingEntity) player);
    }

    public static boolean cancelActiveAttack(Player player) {
        var attacks = selfTickingAttacks.get(player.getUUID());
        if (attacks == null || attacks.isEmpty()) return false;
        boolean cancelled = false;
        for (var attack : new ArrayList<>(attacks)) {
            if (attack.isActive()) { attack.stop(); cancelled = true; }
        }
        return cancelled;
    }

    public void setHitStun(int hitStun) {
        this.hitStun = Math.max(0, hitStun);
    }

    public void setComboSequence(long comboSequence) {
        this.comboSequence = comboSequence;
    }

    public static boolean hasActiveAttack(LivingEntity entity) {
        var attacks = selfTickingAttacks.get(entity.getUUID());
        if (attacks == null) return false;
        for (var attack : attacks) {
            if (attack.isActive()) return true;
        }
        return false;
    }
}
