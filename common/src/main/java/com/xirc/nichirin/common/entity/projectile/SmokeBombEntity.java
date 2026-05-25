package com.xirc.nichirin.common.entity.projectile;

import com.xirc.nichirin.registry.NichirinEntityRegistry;
import com.xirc.nichirin.registry.NichirinItemRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class SmokeBombEntity extends ThrowableItemProjectile {

    public SmokeBombEntity(EntityType<? extends SmokeBombEntity> entityType, Level level) {
        super(entityType, level);
    }

    public SmokeBombEntity(Level level, LivingEntity shooter) {
        super(NichirinEntityRegistry.SMOKE_BOMB.get(), shooter, level);
    }

    /** Constructor used by dispenser projectile behavior. */
    public SmokeBombEntity(Level level, double x, double y, double z) {
        super(NichirinEntityRegistry.SMOKE_BOMB.get(), x, y, z, level);
    }

    @Override
    protected Item getDefaultItem() {
        return NichirinItemRegistry.SMOKE_BOMB.get();
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);

        if (!this.level().isClientSide) {
            Vec3 impactPos = this.position();

            // Play impact sound
            this.level().playSound(null, impactPos.x, impactPos.y, impactPos.z,
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 1.0F, 1.0F);

            // Create the smoke effect directly
            createSmokeEffect(impactPos);

            // Remove the smoke bomb projectile immediately
            this.discard();
        }
    }

    private void createSmokeEffect(Vec3 position) {
        if (this.level() instanceof ServerLevel serverLevel) {
            final double SMOKE_RADIUS = 6;
            final int SMOKE_DURATION = 140; // 7 seconds at 20 TPS

            // Schedule the smoke effect to run every tick for constant blindness
            for (int tick = 0; tick < SMOKE_DURATION; tick++) { // Every tick instead of every other tick
                int finalTick = tick;
                serverLevel.getServer().tell(new net.minecraft.server.TickTask(
                        serverLevel.getServer().getTickCount() + tick, () -> {

                    // Apply blindness to entities in range
                    AABB effectArea = new AABB(
                            position.x - SMOKE_RADIUS, position.y - SMOKE_RADIUS, position.z - SMOKE_RADIUS,
                            position.x + SMOKE_RADIUS, position.y + SMOKE_RADIUS, position.z + SMOKE_RADIUS
                    );

                    List<LivingEntity> entities = serverLevel.getEntitiesOfClass(LivingEntity.class, effectArea);
                    for (LivingEntity entity : entities) {
                        double distance = Math.sqrt(
                                Math.pow(entity.getX() - position.x, 2) +
                                        Math.pow(entity.getY() - position.y, 2) +
                                        Math.pow(entity.getZ() - position.z, 2)
                        );
                        if (distance <= SMOKE_RADIUS) {
                            // Apply blindness for 2 seconds to ensure constant coverage
                            MobEffectInstance blindness = new MobEffectInstance(
                                    MobEffects.BLINDNESS,
                                    120,
                                    1,
                                    false,
                                    false,
                                    true
                            );
                            entity.addEffect(blindness);
                        }
                    }

                    // Spawn particles every other tick to reduce particle spam
                    if (finalTick % 2 == 0) {
                        // Spawn much larger particles
                        for (int i = 0; i < 25; i++) {
                            double offsetX = (serverLevel.random.nextDouble() - 0.5) * SMOKE_RADIUS * 2;
                            double offsetY = serverLevel.random.nextDouble() * SMOKE_RADIUS;
                            double offsetZ = (serverLevel.random.nextDouble() - 0.5) * SMOKE_RADIUS * 2;

                            if (offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ <= SMOKE_RADIUS * SMOKE_RADIUS) {
                                serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                                        position.x + offsetX, position.y + offsetY, position.z + offsetZ,
                                        6, // More particles per spawn
                                        0, 0, 0, // Much larger spread
                                        0.02); // Higher velocity
                            }
                        }
                    }
                }
                ));
            }
        }
    }
}