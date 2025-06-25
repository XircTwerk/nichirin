package com.xirc.nichirin.common.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

/**
 * Sent to clients to display breathing technique effects
 */
public class BreathingEffectPacket {
    private final Vec3 position;
    private final int particleType;
    private final int count;

    public BreathingEffectPacket(Vec3 position, int particleType, int count) {
        this.position = position;
        this.particleType = particleType;
        this.count = count;
    }

    public BreathingEffectPacket(FriendlyByteBuf buf) {
        this.position = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        this.particleType = buf.readInt();
        this.count = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeDouble(position.x);
        buf.writeDouble(position.y);
        buf.writeDouble(position.z);
        buf.writeInt(particleType);
        buf.writeInt(count);
    }

    @Environment(EnvType.CLIENT)
    public void handleClient() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            ParticleOptions particle = getParticleFromType(particleType);
            for (int i = 0; i < count; i++) {
                mc.level.addParticle(particle,
                        position.x + (mc.level.random.nextDouble() - 0.5),
                        position.y + (mc.level.random.nextDouble() - 0.5),
                        position.z + (mc.level.random.nextDouble() - 0.5),
                        0, 0.1, 0
                );
            }
        }
    }

    private ParticleOptions getParticleFromType(int type) {
        return switch (type) {
            case 0 -> ParticleTypes.FLAME;
            case 1 -> ParticleTypes.SPLASH;
            case 2 -> ParticleTypes.ELECTRIC_SPARK;
            case 3 -> ParticleTypes.CLOUD;
            default -> ParticleTypes.SWEEP_ATTACK;
        };
    }
}