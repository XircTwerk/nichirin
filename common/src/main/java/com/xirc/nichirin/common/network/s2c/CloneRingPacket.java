package com.xirc.nichirin.common.network.s2c;

import com.xirc.nichirin.client.renderer.effects.CloneRingRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

/**
 * S2C: spawns (or clears, when lifetimeTicks <= 0) a ring of tinted ghost clones of the caster.
 * Generalized version of the Sea of Clouds mist clones — supports per-ring tint colour, staggered
 * appearance, ring spin, and per-clone target facing (each clone tracks its own victim). Used by
 * Blue Silver Chaotic Afterglow.
 */
public class CloneRingPacket {

    private final int casterEntityId;
    private final double centerX, centerY, centerZ;
    private final float radius;
    private final int count;
    private final int lifetimeTicks;
    private final float spinSpeed;
    private final int staggerTicks;
    private final int[] cloneTargetIds; // per-clone entity id to face; -1 = face the ring centre
    private final float r, g, b, a;

    public CloneRingPacket(int casterEntityId, Vec3 center, float radius, int count, int lifetimeTicks,
                           float spinSpeed, int staggerTicks, int[] cloneTargetIds,
                           float r, float g, float b, float a) {
        this.casterEntityId = casterEntityId;
        this.centerX = center.x;
        this.centerY = center.y;
        this.centerZ = center.z;
        this.radius = radius;
        this.count = count;
        this.lifetimeTicks = lifetimeTicks;
        this.spinSpeed = spinSpeed;
        this.staggerTicks = staggerTicks;
        this.cloneTargetIds = cloneTargetIds != null ? cloneTargetIds : new int[0];
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public CloneRingPacket(FriendlyByteBuf buf) {
        this.casterEntityId = buf.readInt();
        this.centerX = buf.readDouble();
        this.centerY = buf.readDouble();
        this.centerZ = buf.readDouble();
        this.radius = buf.readFloat();
        this.count = buf.readVarInt();
        this.lifetimeTicks = buf.readVarInt();
        this.spinSpeed = buf.readFloat();
        this.staggerTicks = buf.readVarInt();
        this.cloneTargetIds = buf.readVarIntArray();
        this.r = buf.readFloat();
        this.g = buf.readFloat();
        this.b = buf.readFloat();
        this.a = buf.readFloat();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(casterEntityId);
        buf.writeDouble(centerX);
        buf.writeDouble(centerY);
        buf.writeDouble(centerZ);
        buf.writeFloat(radius);
        buf.writeVarInt(count);
        buf.writeVarInt(lifetimeTicks);
        buf.writeFloat(spinSpeed);
        buf.writeVarInt(staggerTicks);
        buf.writeVarIntArray(cloneTargetIds);
        buf.writeFloat(r);
        buf.writeFloat(g);
        buf.writeFloat(b);
        buf.writeFloat(a);
    }

    /**
     * Ring angle of clone {@code index} after {@code elapsedTicks} of spin. Lives here (common)
     * because the SERVER mirrors it to aim shockwaves "from" a clone's position while the CLIENT
     * uses it to draw the clone — both sides must agree or punches visibly come from nowhere.
     */
    public static double cloneAngle(int index, int count, long elapsedTicks, float spinSpeed) {
        return (2.0 * Math.PI * index / count) + elapsedTicks * spinSpeed;
    }

    @Environment(EnvType.CLIENT)
    public void handleClient() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (lifetimeTicks <= 0) {
            CloneRingRenderer.clear(casterEntityId);
            return;
        }
        CloneRingRenderer.set(casterEntityId, new Vec3(centerX, centerY, centerZ), radius, count,
                lifetimeTicks, spinSpeed, staggerTicks, cloneTargetIds, r, g, b, a);
    }
}
