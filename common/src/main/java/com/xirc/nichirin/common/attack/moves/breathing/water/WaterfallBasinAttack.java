package com.xirc.nichirin.common.attack.moves.breathing.water;

import com.xirc.nichirin.registry.NichirinEffectRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Eighth Form: Waterfall Basin
 * BIG ASS MULTIHIT
 * Erects a waterfall in front of the user, may be divided into several thinner waterfalls
 * Creates massive continuous damage in a large area
 */
public class WaterfallBasinAttack extends WaterBreathingAttackBase {

    private boolean waterfallStarted = false;
    private Set<LivingEntity> hitEntities = new HashSet<>();
    private int waterfallTicks = 0;
    private static final int WATERFALL_STREAMS = 5; // Multiple waterfall streams
    private static final float WATERFALL_HEIGHT = 6.0f; // BIG ASS WATERFALL

    public WaterfallBasinAttack() {
    }

    @Override
    protected void onStart() {
        waterfallStarted = false;
        hitEntities.clear();
        waterfallTicks = 0;

        // BIG ASS waterfall startup sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.WATER_AMBIENT, SoundSource.PLAYERS, 1.5f, 0.5f);

        // Initial water gathering - BIG ASS particles
        createMassiveWaterGathering();
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Start BIG ASS waterfall after windup
        if (!waterfallStarted && tickCount == windup + 1) {
            startWaterfall();
            waterfallStarted = true;
        }

        // Continue BIG ASS MULTIHIT during duration
        if (waterfallStarted && tickCount > windup && tickCount < windup + duration) {
            waterfallTicks++;
            performBigAssMultihit();
        }
    }

    private void createMassiveWaterGathering() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // BIG ASS water gathering above user
        for (int height = 0; height < 8; height++) {
            for (int i = 0; i < 20; i++) {
                double angle = (i / 20.0) * 2 * Math.PI;
                double radius = 3.0 + height * 0.3;
                double y = userPos.y + 4 + height * 0.8;

                double x = userPos.x + Math.cos(angle) * radius;
                double z = userPos.z + Math.sin(angle) * radius;

                // Massive cloud gathering
                serverLevel.sendParticles(ParticleTypes.CLOUD,
                        x, y, z, 2, 0.2, 0.2, 0.2, 0.05);

                if (i % 4 == 0) {
                    serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER,
                            x, y, z, 1, 0.1, 0.1, 0.1, 0.03);
                }
            }
        }
    }

    private void startWaterfall() {
        // BIG ASS waterfall start sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 2.0f, 0.4f);

        // Create initial BIG ASS waterfall formation
        createInitialWaterfallFormation();
    }

    private void performBigAssMultihit() {

        applySlowdown();
        // Re-apply stun each tick so the player cannot cancel into other moves
        user.addEffect(new MobEffectInstance(NichirinEffectRegistry.STUNNED.get(), 5, 0, false, false));

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();

        // Create continuous BIG ASS waterfall effect
        createContinuousWaterfallEffect();

        // BIG ASS MULTIHIT - hit enemies constantly
        if (waterfallTicks % 4 == 0) { // Hit every 4 ticks (5 hits per second)
            // Hit all enemies in the BIG ASS waterfall area
            List<LivingEntity> waterfallTargets = getTargetsInCustomHitbox(
                    userPos.add(lookDir.scale(range * 0.6)),
                    hitboxSize, // BIG ASS width
                    WATERFALL_HEIGHT + 2, // BIG ASS height
                    hitboxSize * 0.8  // BIG ASS depth
            );

            for (LivingEntity target : waterfallTargets) {
                // BIG ASS MULTIHIT - no immunity frames for continuous damage
                hitTargetNoImmunity(target);

                // Light knockback to keep enemies in waterfall for MORE HITS
                Vec3 waterfallKnockback = lookDir.scale(knockback * 0.2);
                target.push(waterfallKnockback.x, 0.05, waterfallKnockback.z);

                // BIG ASS hit sound
                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS,
                        0.6f, 0.8f + waterfallTicks * 0.02f);

                // Create BIG ASS impact effect
                createWaterfallImpactEffect(target.position());
            }
        }

        // BIG ASS waterfall sound every few ticks
        if (waterfallTicks % 8 == 0) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.WATER_AMBIENT, SoundSource.PLAYERS,
                    1.2f, 0.6f + waterfallTicks * 0.01f);
        }
    }

    private void createInitialWaterfallFormation() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();

        // Create BIG ASS waterfall formation in front of user
        Vec3 waterfallBase = userPos.add(lookDir.scale(range * 0.6));

        // Create multiple waterfall streams for BIG ASS effect
        for (int stream = 0; stream < WATERFALL_STREAMS; stream++) {
            float streamOffset = (stream - WATERFALL_STREAMS / 2.0f) * (hitboxSize / WATERFALL_STREAMS);
            Vec3 rightDir = lookDir.cross(new Vec3(0, 1, 0)).normalize();
            Vec3 streamBase = waterfallBase.add(rightDir.scale(streamOffset));

            // Create each BIG ASS waterfall stream
            createWaterfall(streamBase, hitboxSize / WATERFALL_STREAMS, WATERFALL_HEIGHT, 1);
        }

        // BIG ASS initial explosion
        createWaterExplosion(waterfallBase, 3.0f);
    }

    private void createContinuousWaterfallEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();
        Vec3 waterfallBase = userPos.add(lookDir.scale(range * 0.6));

        // Continuous BIG ASS waterfall streams
        for (int stream = 0; stream < WATERFALL_STREAMS; stream++) {
            float streamOffset = (stream - WATERFALL_STREAMS / 2.0f) * (hitboxSize / WATERFALL_STREAMS);
            Vec3 rightDir = lookDir.cross(new Vec3(0, 1, 0)).normalize();
            Vec3 streamCenter = waterfallBase.add(rightDir.scale(streamOffset));

            // Create falling water particles for each stream
            for (int height = 0; height < (int)WATERFALL_HEIGHT; height++) {
                Vec3 streamPos = streamCenter.add(0, WATERFALL_HEIGHT - height, 0);

                // BIG ASS falling water particles
                serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER,
                        streamPos.x, streamPos.y, streamPos.z,
                        4, 0.2, 0, 0.2, 0.1);

                // BIG ASS splash particles every few blocks
                if (height % 2 == 0) {
                    serverLevel.sendParticles(ParticleTypes.SPLASH,
                            streamPos.x, streamPos.y, streamPos.z,
                            6, 0.4, 0.3, 0.4, 0.15);
                }

                // Mist effect for BIG ASS waterfall
                if (waterfallTicks % 3 == 0) {
                    serverLevel.sendParticles(ParticleTypes.CLOUD,
                            streamPos.x, streamPos.y, streamPos.z,
                            2, 0.3, 0.3, 0.3, 0.08);
                }
            }

            // BIG ASS pool at the bottom of each stream
            Vec3 poolCenter = streamCenter.add(0, -0.5, 0);
            if (waterfallTicks % 2 == 0) {
                serverLevel.sendParticles(ParticleTypes.SPLASH,
                        poolCenter.x, poolCenter.y, poolCenter.z,
                        8, hitboxSize * 0.15, 0.2, hitboxSize * 0.15, 0.2);
            }
        }

        // BIG ASS mist cloud around entire waterfall
        if (waterfallTicks % 5 == 0) {
            for (int i = 0; i < 15; i++) {
                double x = waterfallBase.x + (Math.random() - 0.5) * hitboxSize * 1.5;
                double z = waterfallBase.z + (Math.random() - 0.5) * hitboxSize * 1.5;
                double y = waterfallBase.y + Math.random() * WATERFALL_HEIGHT * 0.8;

                serverLevel.sendParticles(ParticleTypes.CLOUD,
                        x, y, z, 1, 0.2, 0.2, 0.2, 0.05);
            }
        }

        // BIG ASS spray effects around waterfall
        if (waterfallTicks % 6 == 0) {
            createWaterCircle(waterfallBase, hitboxSize * 0.8f, 20);
        }
    }

    private void createWaterfallImpactEffect(Vec3 impactPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 targetPos = impactPos.add(0, 1, 0);

        // BIG ASS impact particles
        serverLevel.sendParticles(ParticleTypes.SPLASH,
                targetPos.x, targetPos.y, targetPos.z,
                15, 0.6, 0.6, 0.6, 0.3);

        // BIG ASS crit effect
        serverLevel.sendParticles(ParticleTypes.CRIT,
                targetPos.x, targetPos.y, targetPos.z,
                8, 0.4, 0.4, 0.4, 0.2);

        // Waterfall pressure effect
        serverLevel.sendParticles(ParticleTypes.BUBBLE,
                targetPos.x, targetPos.y, targetPos.z,
                10, 0.3, 0.3, 0.3, 0.15);
    }
    private void applySlowdown() {
        int slowDuration = 1;
        user.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                slowDuration,
                255, // Max slowness (can't move but can't be knocked back)
                false, // Not ambient
                false  // Don't show particles (too much visual noise)
        ));
    }
    @Override
    protected void onStop() {
        // Clear state
        hitEntities.clear();
        waterfallTicks = 0;
        waterfallStarted = false;

        // BIG ASS final waterfall collapse effect
        if (world instanceof ServerLevel serverLevel) {
            Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
            Vec3 lookDir = user.getLookAngle();
            Vec3 waterfallBase = userPos.add(lookDir.scale(range * 0.6));

            // BIG ASS final explosion
            createWaterExplosion(waterfallBase, 4.0f);

            // BIG ASS waterfall collapse - water spreads everywhere
            for (int ring = 1; ring <= 5; ring++) {
                float ringRadius = ring * hitboxSize * 0.4f;
                int particlesInRing = 15 * ring;

                for (int i = 0; i < particlesInRing; i++) {
                    double angle = (2 * Math.PI * i) / particlesInRing;
                    double x = waterfallBase.x + Math.cos(angle) * ringRadius;
                    double z = waterfallBase.z + Math.sin(angle) * ringRadius;
                    double y = waterfallBase.y;

                    serverLevel.sendParticles(ParticleTypes.SPLASH,
                            x, y, z, 6, 0.8, 0.8, 0.8, 0.4);
                }
            }

            // BIG ASS rain effect from collapsed waterfall
            for (int i = 0; i < 80; i++) {
                double x = waterfallBase.x + (Math.random() - 0.5) * hitboxSize * 2;
                double z = waterfallBase.z + (Math.random() - 0.5) * hitboxSize * 2;
                double y = waterfallBase.y + WATERFALL_HEIGHT + Math.random() * 3;

                serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER,
                        x, y, z, 1, 0, -0.5, 0, 0.2);
            }
        }

        // BIG ASS final waterfall sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 2.0f, 0.3f);
    }
}