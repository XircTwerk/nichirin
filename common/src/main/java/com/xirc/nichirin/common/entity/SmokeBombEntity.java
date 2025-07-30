package com.xirc.nichirin.common.entity;

import com.xirc.nichirin.registry.NichirinEntityRegistry;
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

    private boolean hasLanded = false;
    private int smokeTicks = 0;
    private final int SMOKE_DURATION = 140; // 7 seconds at 20 TPS
    private final double SMOKE_RADIUS = 4.5;

    public SmokeBombEntity(EntityType<? extends SmokeBombEntity> entityType, Level level) {
        super(entityType, level);
    }

    public SmokeBombEntity(Level level, LivingEntity shooter) {
        super(NichirinEntityRegistry.SMOKE_BOMB.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return com.xirc.nichirin.registry.NichirinItemRegistry.SMOKE_BOMB.get();
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);

        if (!this.level().isClientSide && !hasLanded) {
            hasLanded = true;

            // Play impact sound
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);

            // Stop the projectile motion
            this.setDeltaMovement(Vec3.ZERO);
            this.setNoGravity(true);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (hasLanded) {
            smokeTicks++;

            // Remove after smoke duration
            if (smokeTicks >= SMOKE_DURATION) {
                this.discard();
                return;
            }

            if (!this.level().isClientSide) {
                ServerLevel serverLevel = (ServerLevel) this.level();

                // Apply blindness to entities in range
                AABB effectArea = new AABB(
                        this.getX() - SMOKE_RADIUS, this.getY() - SMOKE_RADIUS, this.getZ() - SMOKE_RADIUS,
                        this.getX() + SMOKE_RADIUS, this.getY() + SMOKE_RADIUS, this.getZ() + SMOKE_RADIUS
                );

                List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class, effectArea);
                for (LivingEntity entity : entities) {
                    double distance = entity.distanceTo(this);
                    if (distance <= SMOKE_RADIUS) {
                        // Apply blindness effect
                        MobEffectInstance blindness = new MobEffectInstance(
                                MobEffects.BLINDNESS,
                                30, // 1.5 seconds (refreshed while in smoke)
                                0,  // Level 1
                                false, // Ambient
                                true,  // Show particles
                                true   // Show icon
                        );
                        entity.addEffect(blindness);
                    }
                }

                // Spawn particles continuously (dense smoke)
                if (smokeTicks % 2 == 0) { // Every other tick
                    for (int i = 0; i < 20; i++) { // Increased particle count
                        double offsetX = (this.random.nextDouble() - 0.5) * SMOKE_RADIUS * 2;
                        double offsetY = this.random.nextDouble() * SMOKE_RADIUS;
                        double offsetZ = (this.random.nextDouble() - 0.5) * SMOKE_RADIUS * 2;

                        if (offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ <= SMOKE_RADIUS * SMOKE_RADIUS) {
                            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                                    this.getX() + offsetX, this.getY() + offsetY, this.getZ() + offsetZ,
                                    1, 0.05, 0.05, 0.05, 0.01);
                        }
                    }
                }
            }
        }
    }
}