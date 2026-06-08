package com.xirc.nichirin.common.aura;

import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

/**
 * One aura visual attached to one entity. Network-serialisable.
 *
 * `lifetimeTicks == -1` means permanent until explicitly removed.
 * `startTimeMs` is set on the client when the packet arrives and drives the animation phase.
 * `jitterAmount` controls how much the surface SHAPE morphs over time (renamed from
 * shapeMorphAmount, now per-aura instead of global).
 */
public final class AuraInstance {
    private final UUID id;
    private final float r, g, b, a;
    private final float radius;
    private final float pulseSpeed;
    private final float distortionStrength;
    private final float rotationSpeed;
    private final float jitterAmount;
    private final int lifetimeTicks;       // -1 = permanent
    private long startTimeMs;              // client-set on receive; not sent

    public AuraInstance(UUID id,
                        float r, float g, float b, float a,
                        float radius,
                        float pulseSpeed,
                        float distortionStrength,
                        float rotationSpeed,
                        float jitterAmount,
                        int lifetimeTicks) {
        this.id = id;
        this.r = r; this.g = g; this.b = b; this.a = a;
        this.radius = radius;
        this.pulseSpeed = pulseSpeed;
        this.distortionStrength = distortionStrength;
        this.rotationSpeed = rotationSpeed;
        this.jitterAmount = jitterAmount;
        this.lifetimeTicks = lifetimeTicks;
    }

    public UUID id() { return id; }
    public float r() { return r; }
    public float g() { return g; }
    public float b() { return b; }
    public float a() { return a; }
    public float radius() { return radius; }
    public float pulseSpeed() { return pulseSpeed; }
    public float distortionStrength() { return distortionStrength; }
    public float rotationSpeed() { return rotationSpeed; }
    public float jitterAmount() { return jitterAmount; }
    public int lifetimeTicks() { return lifetimeTicks; }
    public long startTimeMs() { return startTimeMs; }
    public void setStartTimeMs(long t) { this.startTimeMs = t; }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(id);
        buf.writeFloat(r); buf.writeFloat(g); buf.writeFloat(b); buf.writeFloat(a);
        buf.writeFloat(radius);
        buf.writeFloat(pulseSpeed);
        buf.writeFloat(distortionStrength);
        buf.writeFloat(rotationSpeed);
        buf.writeFloat(jitterAmount);
        buf.writeVarInt(lifetimeTicks);
    }

    public static AuraInstance read(FriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        float r = buf.readFloat(), g = buf.readFloat(), b = buf.readFloat(), a = buf.readFloat();
        float radius = buf.readFloat();
        float pulse = buf.readFloat();
        float distortion = buf.readFloat();
        float rotation = buf.readFloat();
        float jitter = buf.readFloat();
        int lifetime = buf.readVarInt();
        return new AuraInstance(id, r, g, b, a, radius, pulse, distortion, rotation, jitter, lifetime);
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id = UUID.randomUUID();
        private float r = 0.65f, g = 0.95f, b = 1.0f, a = 0.45f;
        private float radius = 1.5f;
        private float pulseSpeed = 0.8f;
        private float distortionStrength = 0.18f;
        private float rotationSpeed = 0.25f;
        private float jitterAmount = 2.2f;
        private int lifetimeTicks = -1;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder color(float r, float g, float b, float a) {
            this.r = r; this.g = g; this.b = b; this.a = a; return this;
        }
        public Builder radius(float v) { this.radius = v; return this; }
        public Builder pulseSpeed(float v) { this.pulseSpeed = v; return this; }
        public Builder distortion(float v) { this.distortionStrength = v; return this; }
        public Builder rotationSpeed(float v) { this.rotationSpeed = v; return this; }
        public Builder jitter(float v) { this.jitterAmount = v; return this; }
        public Builder lifetimeTicks(int v) { this.lifetimeTicks = v; return this; }
        public AuraInstance build() {
            return new AuraInstance(id, r, g, b, a, radius, pulseSpeed, distortionStrength,
                    rotationSpeed, jitterAmount, lifetimeTicks);
        }
    }
}
