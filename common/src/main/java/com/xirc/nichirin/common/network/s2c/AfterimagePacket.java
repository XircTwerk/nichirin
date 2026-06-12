package com.xirc.nichirin.common.network.s2c;

import com.xirc.nichirin.client.afterimage.AfterimageRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

public class AfterimagePacket {
    private final int entityId;
    private final Vec3 from;
    private final Vec3 to;
    private final int lifetimeTicks;
    private final int copies;
    private final float alpha;

    public AfterimagePacket(int entityId, Vec3 from, Vec3 to, int lifetimeTicks, int copies, float alpha) {
        this.entityId = entityId;
        this.from = from;
        this.to = to;
        this.lifetimeTicks = lifetimeTicks;
        this.copies = copies;
        this.alpha = alpha;
    }

    public AfterimagePacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.from = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        this.to = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        this.lifetimeTicks = buf.readVarInt();
        this.copies = buf.readVarInt();
        this.alpha = buf.readFloat();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeDouble(from.x);
        buf.writeDouble(from.y);
        buf.writeDouble(from.z);
        buf.writeDouble(to.x);
        buf.writeDouble(to.y);
        buf.writeDouble(to.z);
        buf.writeVarInt(lifetimeTicks);
        buf.writeVarInt(copies);
        buf.writeFloat(alpha);
    }

    @Environment(EnvType.CLIENT)
    public void handleClient() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        mc.execute(() -> {
            if (Minecraft.getInstance().level == null) {
                return;
            }
            AfterimageRenderer.add(entityId, from, to, lifetimeTicks, copies, alpha);
        });
    }
}
