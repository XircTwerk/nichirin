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
 * `wavinessAmount` adds a rippling sinusoidal undulation to the silhouette edge on top of jitter;
 * 0 (the default) leaves the shape unchanged.
 * `cameraFacing == true` billboards the disc to the observer's camera instead of following the
 * host's body yaw — used by projectiles (shockwaves) so the aura always reads as a full disc.
 */
public final class AuraInstance {
    private final UUID id;
    private final float r, g, b, a;
    private final float radius;
    private final float pulseSpeed;
    private final float distortionStrength;
    private final float rotationSpeed;
    private final float jitterAmount;
    private final float wavinessAmount;
    private final int materialProfile;
    private final boolean cameraFacing;
    private final int lifetimeTicks;       // -1 = permanent
    private long startTimeMs;              // client-set on receive; not sent
    private long removalTimeMs;            // client-set when a remove arrives; 0 = active

    public AuraInstance(UUID id,
                        float r, float g, float b, float a,
                        float radius,
                        float pulseSpeed,
                        float distortionStrength,
                        float rotationSpeed,
                        float jitterAmount,
                        float wavinessAmount,
                        int materialProfile,
                        boolean cameraFacing,
                        int lifetimeTicks) {
        this.id = id;
        this.r = r; this.g = g; this.b = b; this.a = a;
        this.radius = radius;
        this.pulseSpeed = pulseSpeed;
        this.distortionStrength = distortionStrength;
        this.rotationSpeed = rotationSpeed;
        this.jitterAmount = jitterAmount;
        this.wavinessAmount = wavinessAmount;
        this.materialProfile = materialProfile;
        this.cameraFacing = cameraFacing;
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
    public float wavinessAmount() { return wavinessAmount; }
    public int materialProfile() { return materialProfile; }
    public boolean cameraFacing() { return cameraFacing; }
    public int lifetimeTicks() { return lifetimeTicks; }
    public long startTimeMs() { return startTimeMs; }
    public void setStartTimeMs(long t) { this.startTimeMs = t; }

    /** Marks this aura for fade-out instead of vanishing instantly. Idempotent. */
    public void markRemoved(long nowMs) {
        if (removalTimeMs == 0) removalTimeMs = nowMs;
    }

    /**
     * Eased 0..1 visibility factor combining the spawn fade-in and the removal fade-out, so
     * auras never pop — they ease in when published and ease out when removed (e.g. when the
     * player sheathes or draws a katana and the breathing/demon auras swap).
     */
    public float fadeFactor(long nowMs, long fadeMs) {
        float in = Math.min(1f, (nowMs - startTimeMs) / (float) fadeMs);
        float out = removalTimeMs > 0
                ? 1f - Math.min(1f, (nowMs - removalTimeMs) / (float) fadeMs)
                : 1f;
        float f = Math.max(0f, in * out);
        return f * f * (3f - 2f * f); // smoothstep ease
    }

    /** True once the fade-out finished and the instance can be purged. */
    public boolean fadeComplete(long nowMs, long fadeMs) {
        return removalTimeMs > 0 && nowMs - removalTimeMs >= fadeMs;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(id);
        buf.writeFloat(r); buf.writeFloat(g); buf.writeFloat(b); buf.writeFloat(a);
        buf.writeFloat(radius);
        buf.writeFloat(pulseSpeed);
        buf.writeFloat(distortionStrength);
        buf.writeFloat(rotationSpeed);
        buf.writeFloat(jitterAmount);
        buf.writeFloat(wavinessAmount);
        buf.writeInt(materialProfile);
        buf.writeBoolean(cameraFacing);
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
        float waviness = buf.readFloat();
        int materialProfile = buf.readInt();
        boolean cameraFacing = buf.readBoolean();
        int lifetime = buf.readVarInt();
        return new AuraInstance(id, r, g, b, a, radius, pulse, distortion, rotation, jitter, waviness,
                materialProfile, cameraFacing, lifetime);
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
        private float wavinessAmount = 0.0f;
        private int materialProfile = 0;
        private boolean cameraFacing = false;
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
        public Builder waviness(float v) { this.wavinessAmount = v; return this; }
        public Builder materialProfile(int v) { this.materialProfile = v; return this; }
        public Builder cameraFacing(boolean v) { this.cameraFacing = v; return this; }
        public Builder lifetimeTicks(int v) { this.lifetimeTicks = v; return this; }
        public AuraInstance build() {
            return new AuraInstance(id, r, g, b, a, radius, pulseSpeed, distortionStrength,
                    rotationSpeed, jitterAmount, wavinessAmount, materialProfile, cameraFacing, lifetimeTicks);
        }
    }
}
