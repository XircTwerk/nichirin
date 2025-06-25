package com.xirc.nichirin.common.attack.moves;

import com.xirc.nichirin.common.attack.component.AbstractSimpleAttack;
import com.xirc.nichirin.common.attack.component.IPhysicalAttacker;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Basic slash attack for all sword weapons.
 * Simple horizontal slash with optional followup.
 */
public class BasicSlashAttack<A extends IPhysicalAttacker<A, ?>> extends AbstractSimpleAttack<BasicSlashAttack<A>, A> {

    public BasicSlashAttack() {
        withInfo(
                Component.literal("Basic Slash"),
                Component.literal("A simple horizontal sword slash")
        );

        // Default particle for slashes
        withParticle(ParticleTypes.SWEEP_ATTACK);
    }

    @Override
    protected boolean canStart(A attacker) {
        return false;
    }

    @Override
    protected void onStart(Player user, Level world) {
        // Could add sword swoosh particles here
        createSlashParticles(user, world);
    }

    @Override
    protected void onHit(Player user, LivingEntity target) {
        // Could add special hit effects for critical hits, etc
    }

    @Override
    public boolean canStart(Player physAttacker) {
        return false;
    }

    /**
     * Creates visual slash effect
     */
    private void createSlashParticles(Player user, Level world) {
        if (world.isClientSide()) return;

        double radius = getRange();
        Vec3 userPos = user.position().add(0, user.getBbHeight() * 0.75, 0);
        Vec3 lookDir = user.getLookAngle();

        // Create arc of particles
        for (int i = -30; i <= 30; i += 10) {
            double angle = Math.toRadians(i);
            Vec3 offset = lookDir.yRot((float)angle).scale(radius);
            Vec3 particlePos = userPos.add(offset);

            world.addParticle(ParticleTypes.SWEEP_ATTACK,
                    particlePos.x, particlePos.y, particlePos.z,
                    0, 0, 0);
        }
    }

    @SuppressWarnings("unchecked")
    protected BasicSlashAttack<A> getThis() {
        return this;
    }
}