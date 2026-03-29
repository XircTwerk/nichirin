package com.xirc.nichirin.common.attack.moves.demon.basic;

import com.xirc.nichirin.common.attack.component.AbstractDemonAttack;
import com.xirc.nichirin.common.attack.component.IDemonAttacker;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Simple gut punch attack for demons
 * High damage, no knockback, causes stun
 * Close range punch that hits immediately
 */
public class DemonGutPunchAttack extends AbstractDemonAttack<DemonGutPunchAttack, IDemonAttacker> {

    private boolean hasExecuted = false;

    @Override
    protected void onStart() {
        hasExecuted = false;

        // Play punch startup sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0f, 0.8f);

    }

    @Override
    protected void perform() {
        if (hasExecuted) return; // Only execute once
        hasExecuted = true;

        // Simple close-range attack - hit targets right in front of player
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();
        Vec3 punchCenter = userPos.add(lookDir.scale(1.5)); // Very close range

        // Get targets in a small area in front of player
        List<LivingEntity> targets = getTargetsInHitbox(punchCenter);

        for (LivingEntity target : targets) {
            hitTargetNoImmunity(target); // Use no immunity for guaranteed hit
        }

        // Impact effects
        createImpactEffects(punchCenter);

        // Impact sound
        world.playSound(null, punchCenter.x, punchCenter.y, punchCenter.z,
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.2f, 0.6f);
    }

    private void createImpactEffects(Vec3 impactPos) {
    }

    @Override
    protected void onStop() {
        hasExecuted = false; // Reset for next use
    }
}