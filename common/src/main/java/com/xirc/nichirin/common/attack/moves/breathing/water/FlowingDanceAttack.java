package com.xirc.nichirin.common.attack.moves.breathing.water;

import com.xirc.nichirin.registry.NichirinEffectRegistry;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import com.xirc.nichirin.common.vfx.VfxIds;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Third Form: Flowing Dance
 * Empowers the user with strength and speed while they slash forth
 * Creates a trail behind the user and constantly damages in front during active duration
 * Allows for comboing with other moves
 */
public class FlowingDanceAttack extends WaterBreathingAttackBase {

    private boolean danceStarted = false;
    private boolean empowered = false;
    private Set<LivingEntity> hitEntities = new HashSet<>();
    private int danceTicks = 0;

    public FlowingDanceAttack() {
    }

    @Override
    protected void onStart() {
        danceStarted = false;
        empowered = false;
        hitEntities.clear();
        danceTicks = 0;
    }

    @Override
    protected void onActiveStart() {
        // Flowing dance startup sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Start dance after windup
        if (!danceStarted && tickCount == windup + 1) {
            startDance();
            danceStarted = true;
        }

        // Continue dance effects during duration
        if (danceStarted && tickCount > windup && tickCount < windup + duration) {
            danceTicks++;
            refreshEmpowermentEffects(); // Re-apply every tick so effects expire naturally on stop
            performDance();
        }
    }

    private void startDance() {
        playWaterVfx(VfxIds.FLOWING_DANCE, user.position(), user.getLookAngle(), 1.0f);
        // Unlock the player from the move-stun so they can combo into other attacks
        user.removeEffect(NichirinEffectRegistry.stunned());

        // Apply empowerment effects to user
        applyEmpowerment();
        empowered = true;

        // Dance start sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.5f);

        // Create initial empowerment effect
    }

    private void applyEmpowerment() {
        // Effects are applied for 3 ticks only and refreshed each tick in performDance()
        // so they expire naturally when the dance ends — no lingering effects
        refreshEmpowermentEffects();
    }

    private void refreshEmpowermentEffects() {
        user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 3, 1, false, true));
        user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 3, 1, false, true));
        user.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 3, 0, false, false));
    }

    private void performDance() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();

        // Create flowing trail behind user

        // Constantly damage enemies in front every few ticks
        if (danceTicks % 8 == 0) { // Hit every 8 ticks (0.4 seconds)
            List<LivingEntity> targets = getTargetsAtRange();

            for (LivingEntity target : targets) {
                // Allow re-hitting the same target but track for spacing
                if (!hasRecentlyHit(target)) {
                    hitTargetNoImmunity(target);
                    trackRecentHit(target);

                    // Very light knockback to keep enemies close for comboing
                    Vec3 lightKnockback = target.position().subtract(userPos).normalize();
                    target.push(lightKnockback.x * knockback * 0.5, 0.02, lightKnockback.z * knockback * 0.5);

                    // Individual hit sound
                    world.playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 0.4f, 1.3f + danceTicks * 0.02f);
                }
            }

            // Create forward slashing effect
        }

        // Continuous flowing sound
        if (danceTicks % 15 == 0) {
            playWaterFlowSound(userPos);
        }

    }

    private boolean hasRecentlyHit(LivingEntity target) {
        // Allow hitting the same target again after 16 ticks (0.8 seconds)
        return hitEntities.contains(target) && danceTicks % 16 > 8;
    }

    private void trackRecentHit(LivingEntity target) {
        hitEntities.add(target);
        // Clear old hits periodically to allow re-hitting
        if (danceTicks % 32 == 0) {
            hitEntities.clear();
        }
    }


    @Override
    protected void onStop() {
        user.removeEffect(MobEffects.MOVEMENT_SPEED);
        user.removeEffect(MobEffects.DAMAGE_BOOST);
        user.removeEffect(MobEffects.REGENERATION);

        // Clear hit tracking
        hitEntities.clear();
        danceTicks = 0;
        danceStarted = false;
        empowered = false;

        // Final empowerment sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.0f);
    }
}
