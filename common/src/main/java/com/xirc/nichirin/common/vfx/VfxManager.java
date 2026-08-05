package com.xirc.nichirin.common.vfx;

import com.xirc.nichirin.common.network.s2c.VfxTriggerPacket;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ThreadLocalRandom;

public final class VfxManager {
    private static final double TRACKING_RANGE_SQR = 128.0 * 128.0;

    private VfxManager() {}

    public static void play(ServerLevel level, ResourceLocation effectId, Vec3 origin, Vec3 direction, float scale) {
        Vec3 facing = direction.lengthSqr() > 1.0E-6 ? direction.normalize() : new Vec3(0.0, 0.0, 1.0);
        VfxTriggerPacket packet = new VfxTriggerPacket(
                effectId, origin, facing, scale, ThreadLocalRandom.current().nextLong(), -1, -1);
        level.players().stream()
                .filter(player -> player.distanceToSqr(origin) <= TRACKING_RANGE_SQR)
                .forEach(player -> NichirinPacketRegistry.sendToPlayer(packet, player));
    }

    public static void playAttached(ServerLevel level, Entity entity, ResourceLocation effectId,
                                    Vec3 origin, Vec3 direction, float scale) {
        playAttached(level, entity, entity, effectId, origin, direction, scale);
    }

    public static void playAttached(ServerLevel level, Entity attachment, Entity owner,
                                    ResourceLocation effectId, Vec3 origin, Vec3 direction, float scale) {
        playAttached(level, attachment, owner, effectId, origin, direction, scale, -1);
    }

    public static void playAttached(ServerLevel level, Entity attachment, Entity owner,
                                    ResourceLocation effectId, Vec3 origin, Vec3 direction, float scale,
                                    int lifetimeTicks) {
        Vec3 facing = direction.lengthSqr() > 1.0E-6 ? direction.normalize() : new Vec3(0.0, 0.0, 1.0);
        VfxTriggerPacket packet = new VfxTriggerPacket(
                effectId, origin, facing, scale, ThreadLocalRandom.current().nextLong(),
                attachment.getId(), owner != null ? owner.getId() : -1, lifetimeTicks);
        level.players().stream()
                .filter(player -> player.distanceToSqr(origin) <= TRACKING_RANGE_SQR)
                .forEach(player -> NichirinPacketRegistry.sendToPlayer(packet, player));
    }

    public static void playOwned(ServerLevel level, Entity owner, ResourceLocation effectId,
                                 Vec3 origin, Vec3 direction, float scale) {
        Vec3 facing = direction.lengthSqr() > 1.0E-6 ? direction.normalize() : new Vec3(0.0, 0.0, 1.0);
        VfxTriggerPacket packet = new VfxTriggerPacket(
                effectId, origin, facing, scale, ThreadLocalRandom.current().nextLong(), -1, owner.getId());
        level.players().stream()
                .filter(player -> player.distanceToSqr(origin) <= TRACKING_RANGE_SQR)
                .forEach(player -> NichirinPacketRegistry.sendToPlayer(packet, player));
    }
}
