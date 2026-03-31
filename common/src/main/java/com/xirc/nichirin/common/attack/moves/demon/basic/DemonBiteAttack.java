package com.xirc.nichirin.common.attack.moves.demon.basic;

import com.xirc.nichirin.common.attack.component.AbstractDemonAttack;
import com.xirc.nichirin.common.attack.component.IDemonAttacker;
import com.xirc.nichirin.common.system.DemonManager;
import com.xirc.nichirin.registry.NicirinSoundRegistry;
import com.xirc.nichirin.registry.NichirinParticleRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Demon bite attack - powerful bite that steals blood from enemies
 * High damage, close range, high stun, steals blood on hit
 * No knockback to keep enemies in bite range
 *
 * REFACTORED: Now works with both Player and NPC users
 * - Players: Steal blood via DemonManager
 * - NPCs: Just deal damage (they rely on natural regen, no blood stealing)
 */
public class DemonBiteAttack extends AbstractDemonAttack<DemonBiteAttack, IDemonAttacker> {

    private boolean biteExecuted = false;
    private boolean biteConnected = false;

    public DemonBiteAttack() {
        setBloodOnKill(5);
        setHitsForBlood(1);
    }

    @Override
    protected void onStart() {
        biteExecuted = false;
        biteConnected = false;

    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Execute bite after windup
        if (!biteExecuted && tickCount >= windup) {
            executeBite();
            biteExecuted = true;
        }
    }

    private void executeBite() {
        if (user == null) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();

        // Create bite effect
        createBiteEffect(userPos, lookDir);

        // Hit enemies in close range (very precise hitbox)
        List<LivingEntity> targets = getTargetsAtRange();

        if (!targets.isEmpty()) {
            biteConnected = true;

            for (LivingEntity target : targets) {
                // High damage bite
                hitTarget(target);

                // No knockback - keep them close for blood drain
                // Apply extended stun instead
                applyExtendedStun(target);

                // Blood steal - ONLY for Players
                // NPCs don't need blood stealing, they just use their natural regen
                if (user instanceof Player) {
                    stealBlood((Player) user, target);
                }

                // Bite impact sound
                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                        NicirinSoundRegistry.BITE_CRUNCH.get(), SoundSource.PLAYERS, 1.0f, 0.85f + world.random.nextFloat() * 0.15f);
            }
        }

        // Main bite sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0f, 0.6f);

        if (biteConnected) {
            // Successful bite sound
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.6f, 1.3f);
        }
    }

    private void applyExtendedStun(LivingEntity target) {
        // Apply extra stun effect beyond base hit stun
        // This simulates the target being held in the bite
        // The base hitStun from configuration handles the main stun duration
    }

    /**
     * Handle blood stealing for Player demons only
     */
    private void stealBlood(Player demonPlayer, LivingEntity target) {
        // Check if target has blood (not undead, not construct, etc.)
        if (targetHasBlood(target)) {
            // Call DemonManager to handle blood stealing
            DemonManager.onBiteHit(demonPlayer, target);

            }
    }

    private boolean targetHasBlood(LivingEntity target) {
        // Check if entity has blood that can be stolen
        // Exclude undead, constructs, etc.
        String entityType = target.getType().toString().toLowerCase();

        // No blood for undead creatures
        if (entityType.contains("zombie") || entityType.contains("skeleton") ||
                entityType.contains("wither") || entityType.contains("ghost") ||
                entityType.contains("phantom") || entityType.contains("vex")) {
            return false;
        }

        // No blood for constructs
        if (entityType.contains("golem") || entityType.contains("blaze") ||
                entityType.contains("magma_cube") || entityType.contains("slime")) {
            return false;
        }

        // Players and most living mobs have blood
        return true;
    }

    private void createBiteEffect(Vec3 userPos, Vec3 lookDir) {
        if (!(world instanceof ServerLevel sl)) return;
        Vec3 bitePos = userPos.add(lookDir.scale(range));
        sl.sendParticles(NichirinParticleRegistry.BLOOD_SPLAT.get(),
                bitePos.x, bitePos.y, bitePos.z, 10, 0.3, 0.3, 0.3, 0.0);
        sl.sendParticles(ParticleTypes.CRIT,
                bitePos.x, bitePos.y, bitePos.z, 5, 0.2, 0.2, 0.2, 0.1);
    }

    @Override
    protected void onStop() {
        biteExecuted = false;
        biteConnected = false;
    }
}