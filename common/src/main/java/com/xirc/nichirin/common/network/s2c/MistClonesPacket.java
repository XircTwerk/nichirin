package com.xirc.nichirin.common.network.s2c;

import com.xirc.nichirin.client.renderer.effects.MistCloneRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

public class MistClonesPacket {

    private final int    casterEntityId;
    private final double centerX, centerY, centerZ;
    private final float  radius;
    private final int    lifetimeTicks;

    public MistClonesPacket(int casterEntityId, Vec3 center, float radius, int lifetimeTicks) {
        this.casterEntityId = casterEntityId;
        this.centerX        = center.x;
        this.centerY        = center.y;
        this.centerZ        = center.z;
        this.radius         = radius;
        this.lifetimeTicks  = lifetimeTicks;
    }

    public MistClonesPacket(FriendlyByteBuf buf) {
        this.casterEntityId = buf.readInt();
        this.centerX        = buf.readDouble();
        this.centerY        = buf.readDouble();
        this.centerZ        = buf.readDouble();
        this.radius         = buf.readFloat();
        this.lifetimeTicks  = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(casterEntityId);
        buf.writeDouble(centerX);
        buf.writeDouble(centerY);
        buf.writeDouble(centerZ);
        buf.writeFloat(radius);
        buf.writeInt(lifetimeTicks);
    }

    @Environment(EnvType.CLIENT)
    public void handleClient() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        MistCloneRenderer.addClones(casterEntityId, new Vec3(centerX, centerY, centerZ), radius, lifetimeTicks);
    }
}