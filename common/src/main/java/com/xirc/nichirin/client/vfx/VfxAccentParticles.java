package com.xirc.nichirin.client.vfx;

import com.xirc.nichirin.common.vfx.VfxIds;
import com.xirc.nichirin.registry.NichirinParticleRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Blocks;

import java.util.Set;

/** Sparse vanilla-particle accents that reinforce, rather than replace, the authored VFX geometry. */
@Environment(EnvType.CLIENT)
final class VfxAccentParticles {
    private static final Set<net.minecraft.resources.ResourceLocation> FLAME_EFFECTS = Set.of(
            VfxIds.UNKNOWING_FIRE, VfxIds.RISING_SCORCHING_SUN, VfxIds.BLAZING_UNIVERSE,
            VfxIds.BLAZING_UNIVERSE_IMPACT,
            VfxIds.BLOOMING_FLAME_UNDULATION, VfxIds.FLAME_TIGER, VfxIds.RENGOKU,
            VfxIds.FLAME_POMMEL_SLASH
    );
    private static final Set<net.minecraft.resources.ResourceLocation> THUNDER_EFFECTS = Set.of(
            VfxIds.THUNDERCLAP_FLASH, VfxIds.GODSPEED, VfxIds.RICE_SPIRIT_SLASH,
            VfxIds.THUNDER_SWARM_SLASH, VfxIds.DISTANT_THUNDER_CHARGE,
            VfxIds.HEAT_LIGHTNING_RISE, VfxIds.THUNDER_STRIKE_WARNING,
            VfxIds.THUNDER_STRIKE, VfxIds.HONOIKAZUCHI_NO_KAMI, VfxIds.HONOIKAZUCHI_IMPACT
    );
    private static final Set<net.minecraft.resources.ResourceLocation> MIST_EFFECTS = Set.of(
            VfxIds.LOW_CLOUDS_DISTANT_HAZE, VfxIds.EIGHT_LAYERED_MIST,
            VfxIds.SCATTERING_MIST_SPLASH, VfxIds.SHIFTING_FLOW_SLASH,
            VfxIds.SEA_OF_CLOUDS_AND_HAZE, VfxIds.LUNAR_DISPERSING_MIST,
            VfxIds.MIST_FINISHER, VfxIds.OBSCURING_CLOUDS
    );
    private static final Set<net.minecraft.resources.ResourceLocation> BEAST_EFFECTS = Set.of(
            VfxIds.BEAST_PIERCE, VfxIds.BEAST_X_SLICE, VfxIds.BEAST_EXPLOSIVE_RUSH,
            VfxIds.BEAST_DEVOUR, VfxIds.BEAST_RAPID_SLASH, VfxIds.BEAST_CRAZY_CUTTING,
            VfxIds.BEAST_PALISADE_BITE, VfxIds.BEAST_SPATIAL_AWARENESS,
            VfxIds.BEAST_BENDY_SLASH, VfxIds.BEAST_WHIRLING_FANGS, VfxIds.BEAST_THROWING_STRIKE
    );
    private static final Set<net.minecraft.resources.ResourceLocation> SOUND_EFFECTS = Set.of(
            VfxIds.SOUND_RESONDING_SLASHES, VfxIds.SOUND_RHYTHMIC_STEP, VfxIds.SOUND_ROAR,
            VfxIds.SOUND_STRING_PERFORMANCE, VfxIds.SOUND_TEMPO_BREAKER, VfxIds.SOUND_IMPACT
    );
    private static final Set<net.minecraft.resources.ResourceLocation> INSECT_EFFECTS = Set.of(
            VfxIds.INSECT_QUICK_STING, VfxIds.INSECT_BEE_STING, VfxIds.INSECT_BUTTERFLY_DANCE,
            VfxIds.INSECT_BUTTERFLY_DASH,
            VfxIds.INSECT_DRAGONFLY, VfxIds.INSECT_CENTIPEDE, VfxIds.INSECT_IMPACT
    );

    private VfxAccentParticles() {}

    static void tick(VfxInstance instance) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        if (instance.id().equals(VfxIds.SOUND_ROAR) && instance.ageTicks() % 2 == 0) {
            spawnRoarDebris(minecraft, instance);
        }
        if (instance.ageTicks() % 6 != 0) return;

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
        } else if (THUNDER_EFFECTS.contains(instance.id())) {
            Vec3 velocity = right.scale((random.nextDouble() - 0.5) * 0.07)
                    .add(up.scale(0.02 + random.nextDouble() * 0.06));
            minecraft.level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                    position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
        } else if (MIST_EFFECTS.contains(instance.id())) {
            Vec3 velocity = forward.scale(0.012).add(right.scale((random.nextDouble() - 0.5) * 0.018))
                    .add(up.scale(0.008 + random.nextDouble() * 0.018));
            minecraft.level.addParticle(random.nextInt(4) == 0 ? ParticleTypes.WHITE_ASH : ParticleTypes.CLOUD,
                    position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
        } else if (BEAST_EFFECTS.contains(instance.id())) {
            if (instance.ageTicks() % 12 == 0) {
                Vec3 velocity = right.scale((random.nextDouble() - 0.5) * 0.09)
                        .add(up.scale(0.03 + random.nextDouble() * 0.04));
                minecraft.level.addParticle(ParticleTypes.CRIT,
                        position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
            }
        } else if (SOUND_EFFECTS.contains(instance.id())) {
            if (instance.ageTicks() % 12 == 0) {
                Vec3 velocity = right.scale((random.nextDouble() - 0.5) * 0.06)
                        .add(up.scale(0.025 + random.nextDouble() * 0.035));
                minecraft.level.addParticle(random.nextBoolean() ? ParticleTypes.CRIT : ParticleTypes.END_ROD,
                        position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
            }
        } else if (INSECT_EFFECTS.contains(instance.id())) {
            if (instance.ageTicks() % 12 == 0) {
                Vec3 velocity = right.scale((random.nextDouble() - 0.5) * 0.035)
                        .add(up.scale(0.02 + random.nextDouble() * 0.025));
                minecraft.level.addParticle(NichirinParticleRegistry.BUTTERFLY.get(),
                        position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
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

    private static void spawnRoarDebris(Minecraft minecraft, VfxInstance instance) {
        RandomSource random = RandomSource.create(instance.seed() + instance.ageTicks() * 97L);
        Vec3 origin = instance.origin();
        var state = minecraft.level.getBlockState(BlockPos.containing(origin.x, origin.y - 0.35, origin.z));
        if (state.isAir()) state = Blocks.DIRT.defaultBlockState();
        BlockParticleOption debris = new BlockParticleOption(ParticleTypes.BLOCK, state);
        for (int i = 0; i < 5; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = 0.35 + random.nextDouble() * 1.8;
            double speed = 0.15 + random.nextDouble() * 0.22;
            minecraft.level.addParticle(debris,
                    origin.x + Math.cos(angle) * radius,
                    origin.y + 0.12 + random.nextDouble() * 0.22,
                    origin.z + Math.sin(angle) * radius,
                    Math.cos(angle) * speed,
                    0.20 + random.nextDouble() * 0.34,
                    Math.sin(angle) * speed);
        }
    }
}
