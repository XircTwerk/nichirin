package com.xirc.nichirin.client.vfx;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.List;

public final class VfxInstance {
    private final ResourceLocation id;
    private final VfxEffect effect;
    private final Vec3 origin;
    private final Vec3 direction;
    private final float scale;
    private final long seed;
    private final int attachmentEntityId;
    private final int ownerEntityId;
    private final int lifetimeTicks;
    private final Vec3 attachmentOffset;
    private final ArrayDeque<Vec3> originHistory = new ArrayDeque<>();
    private Vec3 currentOrigin;
    private int ageTicks;

    public VfxInstance(ResourceLocation id, VfxEffect effect, Vec3 origin, Vec3 direction, float scale,
                       long seed, int attachmentEntityId, int ownerEntityId, int lifetimeTicks) {
        this.id = id;
        this.effect = effect;
        this.origin = origin;
        this.direction = direction.lengthSqr() > 1.0E-6 ? direction.normalize() : new Vec3(0.0, 0.0, 1.0);
        this.scale = Math.max(0.05f, scale);
        this.seed = seed;
        this.attachmentEntityId = attachmentEntityId;
        this.ownerEntityId = ownerEntityId;
        this.lifetimeTicks = lifetimeTicks > 0 ? lifetimeTicks : effect.lifetimeTicks();
        this.currentOrigin = origin;
        Entity entity = findEntity();
        this.attachmentOffset = entity != null ? origin.subtract(entity.position()) : Vec3.ZERO;
        this.originHistory.add(origin);
    }

    public ResourceLocation id() { return id; }
    public VfxEffect effect() { return effect; }
    public Vec3 origin() { return currentOrigin; }
    public Vec3 origin(float partialTick) {
        Entity entity = findEntity();
        return entity != null ? entity.getPosition(partialTick).add(attachmentOffset) : currentOrigin;
    }
    public List<Vec3> originHistory() { return List.copyOf(originHistory); }
    public Vec3 direction() { return direction; }
    public float scale() { return scale; }
    public long seed() { return seed; }
    public int ageTicks() { return ageTicks; }
    public int ownerEntityId() { return ownerEntityId; }
    public int lifetimeTicks() { return lifetimeTicks; }

    public void tick() {
        Entity entity = findEntity();
        if (entity != null) currentOrigin = entity.position().add(attachmentOffset);
        if (originHistory.peekLast() == null || originHistory.peekLast().distanceToSqr(currentOrigin) > 0.0016) {
            originHistory.addLast(currentOrigin);
            while (originHistory.size() > 18) originHistory.removeFirst();
        }
        effect.tick(this);
        ageTicks++;
    }

    public boolean isFinished() {
        return ageTicks >= lifetimeTicks;
    }

    private Entity findEntity() {
        Minecraft minecraft = Minecraft.getInstance();
        return attachmentEntityId >= 0 && minecraft.level != null ? minecraft.level.getEntity(attachmentEntityId) : null;
    }
}
