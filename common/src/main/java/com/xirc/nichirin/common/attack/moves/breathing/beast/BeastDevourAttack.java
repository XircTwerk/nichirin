package com.xirc.nichirin.common.attack.moves.breathing.beast;

import com.xirc.nichirin.common.util.HitboxData;
import com.xirc.nichirin.common.vfx.VfxIds;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

// Third Fang: Devour. Two horizontal slashes at the throat — no knockback, strong stun instead.
public class BeastDevourAttack extends BeastBreathingAttackBase {

    private boolean slash1Done = false;
    private boolean slash2Done = false;

    @Override
    protected void onStart() {
        slash1Done = false;
        slash2Done = false;
    }

    @Override
    protected void onActiveStart() {
        playSlashSound();
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        if (!slash1Done && tickCount == windup + 1) {
            executeHorizontalSlash(0.2f);
            slash1Done = true;
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.8f);
        }

        if (!slash2Done && tickCount == windup + 4) {
            executeHorizontalSlash(-0.1f);
            slash2Done = true;
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.75f);
        }
    }

    private void executeHorizontalSlash(float yOffset) {
        Vec3 origin = user.position().add(0, user.getBbHeight() * 0.7 + yOffset, 0);
        Vec3 look = user.getLookAngle();
        Vec3 perp = new Vec3(-look.z, 0, look.x).normalize();
        if (yOffset > 0.0f) playBeastVfx(VfxIds.BEAST_DEVOUR, origin, look, 1.0f);

        for (int i = -3; i <= 3; i++) {
            Vec3 center = origin.add(perp.scale(i * 0.6)).add(look.scale(1.5));
            List<LivingEntity> targets = getTargetsInCustomHitbox(center, hitboxSize, HitboxData.HitboxShape.CUBE);
            for (LivingEntity target : targets) {
                // Suppress knockback so stun pins the target in place
                float savedKnockback = knockback;
                knockback = 0;
                hitTarget(target);
                knockback = savedKnockback;
            }
        }

    }

    @Override
    protected void onStop() {
        slash1Done = false;
        slash2Done = false;
    }
}
