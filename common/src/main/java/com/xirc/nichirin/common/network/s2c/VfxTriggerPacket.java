package com.xirc.nichirin.common.network.s2c;

import com.xirc.nichirin.client.vfx.VfxEngine;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public record VfxTriggerPacket(ResourceLocation effectId, Vec3 origin, Vec3 direction, float scale,
                               long seed, int attachmentEntityId, int ownerEntityId) {
    public VfxTriggerPacket(FriendlyByteBuf buf) {
        this(buf.readResourceLocation(), readVec3(buf), readVec3(buf), buf.readFloat(), buf.readLong(),
                buf.readVarInt(), buf.readVarInt());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeResourceLocation(effectId);
        writeVec3(buf, origin);
        writeVec3(buf, direction);
        buf.writeFloat(scale);
        buf.writeLong(seed);
        buf.writeVarInt(attachmentEntityId);
        buf.writeVarInt(ownerEntityId);
    }

    @Environment(EnvType.CLIENT)
    public void handleClient() {
        VfxEngine.spawn(effectId, origin, direction, scale, seed, attachmentEntityId, ownerEntityId);
    }

    private static Vec3 readVec3(FriendlyByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    private static void writeVec3(FriendlyByteBuf buf, Vec3 value) {
        buf.writeDouble(value.x);
        buf.writeDouble(value.y);
        buf.writeDouble(value.z);
    }
}
