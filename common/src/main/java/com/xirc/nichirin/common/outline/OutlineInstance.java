package com.xirc.nichirin.common.outline;

import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

/**
 * One outline visual attached to one entity. Network-serialisable.
 *
 * `lifetimeTicks == -1` means permanent until explicitly removed.
 * `seeThroughWalls` controls whether the outline renders even when the entity is behind
 * blocks (true) or only when the entity itself would be visible (false).
 * `thickness` is the scale-up factor applied to the expanded outline mesh — 1.04 = subtle,
 * 1.10 = chunky.
 */
public final class OutlineInstance {
    private final UUID id;
    private final float r, g, b, a;
    private final float thickness;
    private final boolean seeThroughWalls;
    private final int lifetimeTicks;
    private long startTimeMs;

    public OutlineInstance(UUID id, float r, float g, float b, float a,
                           float thickness, boolean seeThroughWalls, int lifetimeTicks) {
        this.id = id;
        this.r = r; this.g = g; this.b = b; this.a = a;
        this.thickness = thickness;
        this.seeThroughWalls = seeThroughWalls;
        this.lifetimeTicks = lifetimeTicks;
    }

    public UUID id() { return id; }
    public float r() { return r; }
    public float g() { return g; }
    public float b() { return b; }
    public float a() { return a; }
    public float thickness() { return thickness; }
    public boolean seeThroughWalls() { return seeThroughWalls; }
    public int lifetimeTicks() { return lifetimeTicks; }
    public long startTimeMs() { return startTimeMs; }
    public void setStartTimeMs(long t) { this.startTimeMs = t; }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(id);
        buf.writeFloat(r); buf.writeFloat(g); buf.writeFloat(b); buf.writeFloat(a);
        buf.writeFloat(thickness);
        buf.writeBoolean(seeThroughWalls);
        buf.writeVarInt(lifetimeTicks);
    }

    public static OutlineInstance read(FriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        float r = buf.readFloat(), g = buf.readFloat(), b = buf.readFloat(), a = buf.readFloat();
        float thickness = buf.readFloat();
        boolean stw = buf.readBoolean();
        int lifetime = buf.readVarInt();
        return new OutlineInstance(id, r, g, b, a, thickness, stw, lifetime);
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id = UUID.randomUUID();
        private float r = 1.0f, g = 0.85f, b = 0.2f, a = 1.0f;
        private float thickness = 1.03f;
        private boolean seeThroughWalls = false;
        private int lifetimeTicks = -1;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder color(float r, float g, float b, float a) {
            this.r = r; this.g = g; this.b = b; this.a = a; return this;
        }
        public Builder thickness(float v) { this.thickness = v; return this; }
        public Builder seeThroughWalls(boolean v) { this.seeThroughWalls = v; return this; }
        public Builder lifetimeTicks(int v) { this.lifetimeTicks = v; return this; }
        public OutlineInstance build() {
            return new OutlineInstance(id, r, g, b, a, thickness, seeThroughWalls, lifetimeTicks);
        }
    }
}
