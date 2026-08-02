package com.xirc.nichirin.client.vfx;

import com.xirc.nichirin.common.vfx.VfxIds;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/** Sparse vanilla-particle accents that reinforce, rather than replace, the authored VFX geometry. */
@Environment(EnvType.CLIENT)
final class VfxAccentParticles {
    private static final Set<net.minecraft.resources.ResourceLocation> FLAME_EFFECTS = Set.of(
            VfxIds.UNKNOWING_FIRE, VfxIds.RISING_SCORCHING_SUN, VfxIds.BLAZING_UNIVERSE,
            VfxIds.BLOOMING_FLAME_UNDULATION, VfxIds.FLAME_TIGER, VfxIds.RENGOKU,
            VfxIds.FLAME_POMMEL_SLASH
    );

    private VfxAccentParticles() {}

    static void tick(VfxInstance instance) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || instance.ageTicks() % 6 != 0) return;

        RandomSource random = RandomSource.create(instance.seed() + instance.ageTicks() * 31L);
        Vec3 forward = instance.direction().normalize();
        Vec3 right = rightOf(forward);
        Vec3 up = right.cross(forward).normalize();
        float scale = instance.scale();
        double travel = (0.45 + random.nextDouble() * 2.4) * scale;
        double side = (random.nextDouble() - 0.5) * 1.25 * scale;
        Vec3 position = instance.origin().add(forward.scale(travel)).add(right.scale(side))
                .add(up.scale(0.25 + random.nextDouble() * 1.25 * scale));

        if (FLAME_EFFECTS.contains(instance.id())) {
            Vec3 drift = forward.scale(0.025).add(0.0, 0.025 + random.nextDouble() * 0.025, 0.0);
            minecraft.level.addParticle(random.nextInt(4) == 0 ? ParticleTypes.SMOKE : ParticleTypes.SMALL_FLAME,
                    position.x, position.y, position.z, drift.x, drift.y, drift.z);
            if (instance.ageTicks() % 12 == 0) {
                Vec3 ember = position.add(right.scale((random.nextDouble() - 0.5) * 0.45));
                minecraft.level.addParticle(ParticleTypes.FLAME, ember.x, ember.y, ember.z,
                        drift.x * 0.5, drift.y * 1.35, drift.z * 0.5);
            }
        } else {
            Vec3 velocity = forward.scale(0.035).add(right.scale((random.nextDouble() - 0.5) * 0.025))
                    .add(0.0, 0.035 + random.nextDouble() * 0.035, 0.0);
            minecraft.level.addParticle(ParticleTypes.SPLASH, position.x, position.y, position.z,
                    velocity.x, velocity.y, velocity.z);
            if (instance.ageTicks() % 12 == 0) {
                minecraft.level.addParticle(ParticleTypes.FALLING_WATER,
                        position.x, position.y + 0.25, position.z, 0.0, -0.01, 0.0);
            }
        }
    }

    private static Vec3 rightOf(Vec3 forward) {
        Vec3 right = forward.cross(new Vec3(0.0, 1.0, 0.0));
        return right.lengthSqr() > 1.0E-6 ? right.normalize() : new Vec3(1.0, 0.0, 0.0);
    }
}
