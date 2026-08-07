package com.xirc.nichirin.common.entity.projectile;

import com.xirc.nichirin.common.util.TeleportUtil;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import org.jspecify.annotations.NonNull;

public class SmokeBombEntity extends ThrowableItemProjectile {

    private boolean landed = false;
    private int smokeAge = 0;
    private static final int MAX_SMOKE_AGE = 140; // 7 seconds (20 TPS)
    private static final double SMOKE_RADIUS = 4.0;

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
    protected @NonNull Item getDefaultItem() {
        return NichirinItemRegistry.SMOKE_BOMB.get();
    }

    @Override
    public @NonNull ItemStack getItem() {
        if (this.level().isClientSide && this.isInvisible()) {
            return ItemStack.EMPTY;
        }
        return super.getItem();
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);

        if (!this.level().isClientSide && !this.landed) {
            this.landed = true;
            Vec3 impactPos = this.position();

            // Play impact sound
            this.level().playSound(null, impactPos.x, impactPos.y, impactPos.z,
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 1.0F, 1.0F);

            // Backstep the thrower now that the bomb has landed
            if (this.getOwner() instanceof LivingEntity owner) {
                TeleportUtil.teleportBackward(owner, 5.0F, new TeleportUtil.TeleportOptions()
                        .noTrail()
                        .withParticles(null, null)
                        .withSounds(null, null));
            }

            // Hide the projectile item renderer & stop movement while the smoke cloud persists
            this.setNoGravity(true);
            this.setDeltaMovement(Vec3.ZERO);
            this.setInvisible(true);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level() instanceof ServerLevel serverLevel) {
            // emit tiny black particles
            if (!this.landed) {
                serverLevel.sendParticles(
                        ParticleTypes.ASH,
                        this.getX(), this.getY() + 0.2, this.getZ(),
                        4,       // count
                        0.08, 0.08, 0.08, // offset / spread
                        0.01     // speed
                );
            }
        }

        // Process active smoke cloud on the server side after landing
        if (!this.level().isClientSide && this.landed) {
            this.setDeltaMovement(Vec3.ZERO); // Lock position

            Vec3 position = this.position();

            // Scan for any entities inside the cloud every tick
            AABB effectArea = new AABB(
                    position.x - SMOKE_RADIUS, position.y - SMOKE_RADIUS, position.z - SMOKE_RADIUS,
                    position.x + SMOKE_RADIUS, position.y + SMOKE_RADIUS, position.z + SMOKE_RADIUS
            );

            List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class, effectArea);
            for (LivingEntity entity : entities) {
                if (entity.distanceToSqr(position) <= SMOKE_RADIUS * SMOKE_RADIUS) {
                    // Apply a 30 tick blindness duration so it continually refreshes
                    // while inside and clears quickly after stepping out of the smoke
                    entity.addEffect(new MobEffectInstance(
                            MobEffects.BLINDNESS,
                            30,
                            0,
                            false,
                            false,
                            true
                    ));
                }
            }

            // Spawn ambient smoke particles every 2 ticks
            if (this.level() instanceof ServerLevel serverLevel && this.smokeAge % 2 == 0) {
                for (int i = 0; i < 25; i++) {
                    double offsetX = (serverLevel.random.nextDouble() - 0.5) * 2 * SMOKE_RADIUS * 0.9;
                    double offsetY = serverLevel.random.nextDouble() * SMOKE_RADIUS;
                    double offsetZ = (serverLevel.random.nextDouble() - 0.5) * 2 * SMOKE_RADIUS * 0.9;

                    if (offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ <= SMOKE_RADIUS * SMOKE_RADIUS) {
                        serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                                position.x + offsetX, position.y + offsetY, position.z + offsetZ,
                                2,
                                0, 0, 0,
                                0.02);
                    }
                }
            }

            // Despawn entity after 7 seconds
            this.smokeAge++;
            if (this.smokeAge >= MAX_SMOKE_AGE) {
                this.discard();
            }
        }
    }
}