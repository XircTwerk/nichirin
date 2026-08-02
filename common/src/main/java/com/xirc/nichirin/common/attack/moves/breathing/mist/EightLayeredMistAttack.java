package com.xirc.nichirin.common.attack.moves.breathing.mist;

import com.xirc.nichirin.common.vfx.VfxIds;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

// Form 2: 8 rapid slashes bypassing immunity frames. Right Click.
public class EightLayeredMistAttack extends MistBreathingAttackBase {

    private static final int SLASH_INTERVAL = 2; // ticks between each slash
    private static final int TOTAL_SLASHES  = 8; // "Eight-Layered" — always eight hits

    private int slashesPerformed = 0;

    @Override
    protected void onStart() {
        slashesPerformed = 0;
    }

    @Override
    protected void onActiveStart() {
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 1.5f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        int ticksSinceWindup = tickCount - windup;

        // Fire one slash every SLASH_INTERVAL ticks starting on the first active tick, until all
        // eight have landed. (The old formula skipped index 0 and the final index, which is why
        // only four ever connected.)
        if (ticksSinceWindup >= 1
                && (ticksSinceWindup - 1) % SLASH_INTERVAL == 0
                && slashesPerformed < TOTAL_SLASHES) {
            performSlash(slashesPerformed);
            slashesPerformed++;
        }
    }

    private void performSlash(int slashIndex) {
        boolean isLeftSlash = slashIndex % 2 == 0;

        if (slashIndex == 0) {
            Vec3 vfxDirection = rotateDirection(user.getLookAngle(), isLeftSlash ? -12 : 12);
            playMistVfx(VfxIds.EIGHT_LAYERED_MIST,
                    user.position().add(0, user.getBbHeight() * 0.5, 0), vfxDirection, hitboxSize / 3.0f);
        }

        // Spherical hit pattern centred on the user: every slash strikes all enemies within
        // hitboxSize of the player (a true sphere, not a forward box), bypassing immunity frames
        // so all eight slashes connect.
        Vec3 center = user.position().add(0, user.getBbHeight() / 2, 0);
        double r = hitboxSize;
        double rSq = r * r;
        AABB box = new AABB(center.subtract(r, r, r), center.add(r, r, r));
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != user && e.isAlive()
                        && e.position().add(0, e.getBbHeight() / 2, 0).distanceToSqr(center) <= rSq);

        for (LivingEntity target : targets) {
            hitTargetNoImmunity(target);

            // fake pressure — barely any displacement
            Vec3 tinyKnockback = target.position().subtract(center).normalize().scale(knockback * 0.15f);
            target.push(tinyKnockback.x, 0, tinyKnockback.z);
        }

        float pitch = 1.2f + (slashIndex * 0.08f);
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.6f, pitch);
    }

    @Override
    protected void onStop() {
        slashesPerformed = 0;
    }
}
