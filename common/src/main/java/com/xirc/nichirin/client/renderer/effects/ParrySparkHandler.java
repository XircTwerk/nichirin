package com.xirc.nichirin.client.renderer.effects;

import com.xirc.nichirin.client.config.NichirinClientConfig;
import com.xirc.nichirin.registry.NichirinParticleRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;

public class ParrySparkHandler {

    public static void spawnSparks(double x, double y, double z) {
        if (!NichirinClientConfig.get().visual.enableParrySparks) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        RandomSource rand = mc.level.random;

        // Central white flash
        mc.level.addParticle(ParticleTypes.FLASH, x, y, z, 0, 0, 0);

        // Radial crit sparks
        for (int i = 0; i < 24; i++) {
            double vx = (rand.nextDouble() - 0.5) * 2.4;
            double vy = (rand.nextDouble() - 0.5) * 2.4;
            double vz = (rand.nextDouble() - 0.5) * 2.4;
            mc.level.addParticle(ParticleTypes.CRIT, x, y, z, vx, vy, vz);
        }

        // Enchanted sparkles overlay
        for (int i = 0; i < 12; i++) {
            double vx = (rand.nextDouble() - 0.5) * 1.5;
            double vy = (rand.nextDouble() - 0.5) * 1.5;
            double vz = (rand.nextDouble() - 0.5) * 1.5;
            mc.level.addParticle(ParticleTypes.ENCHANTED_HIT, x, y, z, vx, vy, vz);
        }

        // Sweep arc
        mc.level.addParticle(ParticleTypes.SWEEP_ATTACK, x, y, z, 1, 0, 0);

        // Slash impact spark at clash point
        mc.level.addParticle(NichirinParticleRegistry.SLASH_IMPACT_SPARK.get(), x, y, z, 0, 0, 0);
    }
}
