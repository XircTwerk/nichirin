package com.xirc.nichirin.common.attack.moves.demon.destructive;

import com.xirc.nichirin.common.aura.AuraAudience;
import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.network.s2c.TriggerShaderPacket;
import com.xirc.nichirin.common.network.s2c.PlayerAnimationPacket;
import com.xirc.nichirin.common.outline.OutlineInstance;
import com.xirc.nichirin.common.outline.OutlineManager;
import com.xirc.nichirin.common.vfx.VfxIds;
import com.xirc.nichirin.common.vfx.VfxManager;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Six-second, server-authoritative runtime for Akaza's Compass Needle. */
public final class CompassNeedleTracker {
    private static final String SHADER = "com.xirc.nichirin.client.shader.CompassNeedleShaderEffect";
    private static final double TRACK_RADIUS = 20.0;
    private static final int SCAN_INTERVAL = 2;
    private static final int PREDICTION_INTERVAL = 10;
    private static final int ARROW_INTERVAL = 4;
    private static final int MAX_PREDICTIONS = 8;

    private static final Map<UUID, Set<UUID>> TRACKED = new ConcurrentHashMap<>();
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private CompassNeedleTracker() {}

    public static void activate(ServerPlayer owner, int durationTicks) {
        clear(owner);
        long now = owner.level().getGameTime();
        SESSIONS.put(owner.getUUID(), new Session(now, now + durationTicks));
        TRACKED.put(owner.getUUID(), Set.of());
        NichirinPacketRegistry.sendToPlayer(
                new TriggerShaderPacket(SHADER, true, durationTicks / 20.0f), owner);
    }

    public static void tick(MinecraftServer server) {
        for (UUID ownerId : new ArrayList<>(SESSIONS.keySet())) {
            ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
            Session session = SESSIONS.get(ownerId);
            if (session == null) continue;
            if (owner == null || !owner.isAlive()
                    || owner.level().getGameTime() >= session.expiryTick
                    || !DestructiveDeathState.isCompassActive(ownerId, owner.level().getGameTime())) {
                if (owner != null) {
                    finish(owner);
                } else {
                    clear(ownerId);
                }
                continue;
            }

            long age = owner.level().getGameTime() - session.startTick;
            if (age % SCAN_INTERVAL == 0) scan(owner, session);
            if (age % PREDICTION_INTERVAL == 0) sendPredictions(owner, session);
            if (age % ARROW_INTERVAL == 0) pointAtPriorityTarget(owner, session);
        }
    }

    private static void scan(ServerPlayer owner, Session session) {
        AABB area = owner.getBoundingBox().inflate(TRACK_RADIUS);
        var targets = owner.serverLevel().getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != owner && entity.isAlive() && !FightingSpirit.isSelfless(entity));
        Set<UUID> present = new HashSet<>();
        for (LivingEntity target : targets) {
            present.add(target.getUUID());
            TargetVisual visual = session.targets.get(target.getUUID());
            TargetKind kind = classify(target);
            if (visual == null || visual.kind != kind) {
                if (visual != null) OutlineManager.removeOutline(visual.entity, visual.outlineId);
                UUID outlineId = UUID.randomUUID();
                float strength = fightingSpiritStrength(target);
                OutlineManager.addOutline(target, OutlineInstance.builder()
                                .id(outlineId)
                                .color(kind.r, kind.g, kind.b, 0.80f)
                                .thickness(1.035f + strength * 0.022f)
                                .seeThroughWalls(true)
                                .lifetimeTicks(-1)
                                .build(),
                        AuraAudience.players(Set.of(owner.getUUID())));
                session.targets.put(target.getUUID(),
                        new TargetVisual(target, outlineId, kind, owner.level().getGameTime()));
            } else {
                visual.entity = target;
            }
        }

        for (UUID targetId : new ArrayList<>(session.targets.keySet())) {
            if (!present.contains(targetId)) {
                TargetVisual removed = session.targets.remove(targetId);
                if (removed != null) OutlineManager.removeOutline(removed.entity, removed.outlineId);
            }
        }
        TRACKED.put(owner.getUUID(), Set.copyOf(present));
    }

    private static void sendPredictions(ServerPlayer owner, Session session) {
        session.targets.values().stream()
                .map(visual -> visual.entity)
                .filter(LivingEntity::isAlive)
                .sorted(Comparator.comparingDouble(owner::distanceToSqr))
                .limit(MAX_PREDICTIONS)
                .forEach(target -> {
                    Vec3 from = target.position();
                    Vec3 velocity = target.getDeltaMovement();
                    Vec3 to = from.add(velocity.scale(10.0));
                    if (to.distanceToSqr(from) < 0.0025) to = from;
                    NichirinPacketRegistry.sendAfterimageTo(owner, target, to, to,
                            12, 1, 0.34f);
                });
    }

    private static void pointAtPriorityTarget(ServerPlayer owner, Session session) {
        LivingEntity target = session.targets.values().stream()
                .map(visual -> visual.entity)
                .filter(LivingEntity::isAlive)
                .filter(entity -> !FightingSpirit.isSelfless(entity))
                .filter(entity -> !(entity instanceof Player player
                        && (FightingSpirit.hasTransparentWorld(player) || player.isInvisible())))
                .min(Comparator.comparingDouble(owner::distanceToSqr))
                .orElse(null);
        if (target == null) return;
        Vec3 direction = target.position().subtract(owner.position());
        float distance = (float) Math.sqrt(owner.distanceToSqr(target));
        VfxManager.playAttached(owner.serverLevel(), owner, owner, VfxIds.COMPASS_NEEDLE_ARROW,
                owner.position().add(0.0, 0.075, 0.0), direction, distance);
    }

    public static Set<UUID> getTrackedTargets(UUID owner) {
        return TRACKED.getOrDefault(owner, Collections.emptySet());
    }

    public static boolean isTracked(UUID owner, UUID target) {
        return TRACKED.getOrDefault(owner, Collections.emptySet()).contains(target);
    }

    /** Precision grows as Compass learns the target and responds harder to strong fighting spirit. */
    public static float precisionMultiplier(UUID owner, LivingEntity target, long gameTime) {
        Session session = SESSIONS.get(owner);
        if (session == null || gameTime >= session.expiryTick || FightingSpirit.isSelfless(target)) {
            return 1.0f;
        }
        TargetVisual visual = session.targets.get(target.getUUID());
        if (visual == null) return 1.0f;
        float learned = Math.min(1.0f, (gameTime - visual.firstSeenTick) / 120.0f);
        return Math.min(1.45f, 1.10f + learned * 0.20f + fightingSpiritStrength(target) * 0.15f);
    }

    private static float fightingSpiritStrength(LivingEntity target) {
        double health = target.getMaxHealth() / 80.0;
        double attack = Math.max(0.0, attributeValue(target, Attributes.ATTACK_DAMAGE)) / 24.0;
        double armor = Math.max(0.0, attributeValue(target, Attributes.ARMOR)) / 30.0;
        return (float) Math.min(1.0, health * 0.45 + attack * 0.35 + armor * 0.20);
    }

    private static double attributeValue(LivingEntity target,
                                         net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute) {
        AttributeInstance instance = target.getAttribute(attribute);
        return instance != null ? instance.getValue() : 0.0;
    }

    private static TargetKind classify(LivingEntity target) {
        if (target instanceof Player || target instanceof AbstractVillager) return TargetKind.IMPORTANT;
        if (target instanceof Enemy) return TargetKind.HOSTILE;
        if (target instanceof Animal) return TargetKind.PASSIVE;
        return TargetKind.NEUTRAL;
    }

    public static void clear(ServerPlayer owner) {
        Session session = SESSIONS.remove(owner.getUUID());
        if (session != null) {
            for (TargetVisual visual : session.targets.values()) {
                OutlineManager.removeOutline(visual.entity, visual.outlineId);
            }
        }
        TRACKED.remove(owner.getUUID());
        NichirinPacketRegistry.sendToPlayer(new TriggerShaderPacket(SHADER, false), owner);
    }

    public static void finish(ServerPlayer owner) {
        cancel(owner);
        DestructiveDeathState.deactivateCompass(owner);
        DestructiveDeathState.startCompassCooldown(owner, CompassNeedleAttack.COOLDOWN_TICKS);
        MoveExecutor.sendCooldownDisplay(owner, "Compass Needle", CompassNeedleAttack.COOLDOWN_TICKS);
    }

    public static void cancel(ServerPlayer owner) {
        clear(owner);
        NichirinPacketRegistry.broadcastPlayerAnimation(owner,
                new PlayerAnimationPacket(owner.getId(), ""));
    }

    public static void syncHud(ServerPlayer owner, int remainingTicks) {
        if (remainingTicks <= 0) return;
        NichirinPacketRegistry.sendToPlayer(
                new TriggerShaderPacket(SHADER, true, remainingTicks / 20.0f), owner);
    }

    public static void clear(UUID owner) {
        SESSIONS.remove(owner);
        TRACKED.remove(owner);
    }

    public static void clearAll() {
        SESSIONS.clear();
        TRACKED.clear();
    }

    private enum TargetKind {
        HOSTILE(1.0f, 0.16f, 0.18f),
        NEUTRAL(0.22f, 0.58f, 1.0f),
        PASSIVE(0.25f, 1.0f, 0.40f),
        IMPORTANT(1.0f, 0.86f, 0.16f);

        final float r, g, b;
        TargetKind(float r, float g, float b) { this.r = r; this.g = g; this.b = b; }
    }

    private static final class Session {
        final long startTick;
        final long expiryTick;
        final Map<UUID, TargetVisual> targets = new HashMap<>();

        Session(long startTick, long expiryTick) {
            this.startTick = startTick;
            this.expiryTick = expiryTick;
        }
    }

    private static final class TargetVisual {
        LivingEntity entity;
        final UUID outlineId;
        final TargetKind kind;
        final long firstSeenTick;

        TargetVisual(LivingEntity entity, UUID outlineId, TargetKind kind, long firstSeenTick) {
            this.entity = entity;
            this.outlineId = outlineId;
            this.kind = kind;
            this.firstSeenTick = firstSeenTick;
        }
    }
}
