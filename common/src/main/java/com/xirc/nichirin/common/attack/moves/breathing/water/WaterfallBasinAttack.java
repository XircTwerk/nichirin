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
    }

    @Override
    protected void onActiveStart() {
        // BIG ASS waterfall startup sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.WATER_AMBIENT, SoundSource.PLAYERS, 1.5f, 0.5f);

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


    private void startWaterfall() {
        // Erect the waterfall in FRONT (horizontal facing) without pitching it up/down with aim.
        // A zero direction would make the effect default to due-east, i.e. spawn on the user's right.
        Vec3 look = user.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0.0, look.z);
        forward = forward.lengthSqr() > 1.0E-6 ? forward.normalize() : new Vec3(0.0, 0.0, 1.0);
        playWaterVfxAt(VfxIds.WATERFALL_BASIN,
                user.position().add(forward.scale(range * 0.35)), forward, 1.15f);
        // BIG ASS waterfall start sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 2.0f, 0.4f);

        // Create initial BIG ASS waterfall formation
    }

    private void performBigAssMultihit() {

        applySlowdown();
        // Re-apply stun each tick so the player cannot cancel into other moves
        user.addEffect(new MobEffectInstance(NichirinEffectRegistry.stunned(), 5, 0, false, false));

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();

        // Create continuous BIG ASS waterfall effect

        // BIG ASS MULTIHIT - hit enemies constantly
        if (waterfallTicks % 2 == 0) { // Hit every 2 ticks (halved from 4 after the double-tick dedup)
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
            }
        }

        // BIG ASS waterfall sound every few ticks
        if (waterfallTicks % 8 == 0) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.WATER_AMBIENT, SoundSource.PLAYERS,
                    1.2f, 0.6f + waterfallTicks * 0.01f);
        }
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

        // BIG ASS final waterfall sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 2.0f, 0.3f);
    }
}
