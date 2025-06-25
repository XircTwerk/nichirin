package com.xirc.nichirin.common.attack.moves.thunder;

import com.xirc.nichirin.common.attack.component.AbstractBlitzAttack;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Creates chained hitboxes that jump between enemies
 */
public class ChainLightningAttack extends AbstractBlitzAttack {

    private LivingEntity lastTarget = null;

    public ChainLightningAttack() {
        withTiming(1, 10, 50)
                .withDamage(10.0f)
                .withBreathCost(30.0f)
                .withHitboxTiming(5, 8) // 8 hitboxes, one every 5 ticks
                .withHitboxProperties(2.0f, 0.0f)
                .withChainedHitboxes()
                .withHitboxEffects(ParticleTypes.ELECTRIC_SPARK, SoundEvents.LIGHTNING_BOLT_IMPACT)
                .withForm(3, "Chain Lightning");
    }

    @Override
    protected Vec3 calculateHitboxPosition(Player user, int index) {
        if (index == 0 || lastTarget == null) {
            // First hitbox is in front of player
            Vec3 look = user.getLookAngle();
            return user.position().add(look.scale(3)).add(0, 1, 0);
        } else {
            // Subsequent hitboxes center on last hit target
            return lastTarget.position().add(0, lastTarget.getBbHeight() / 2, 0);
        }
    }

    @Override
    protected void applyHitboxEffects(Player user, LivingEntity target, ActiveHitbox hitbox) {
        super.applyHitboxEffects(user, target, hitbox);

        // Remember last target for chaining
        lastTarget = target;

        // Create lightning effect from previous position
        if (hitbox.getIndex() > 0) {
            Vec3 prevPos = getActiveHitboxes().stream()
                    .filter(h -> h.getIndex() == hitbox.getIndex() - 1)
                    .findFirst()
                    .map(ActiveHitbox::getPosition)
                    .orElse(user.position());

            // Draw lightning line
            Vec3 diff = target.position().subtract(prevPos);
            for (double d = 0; d < 1; d += 0.1) {
                Vec3 pos = prevPos.add(diff.scale(d));
                target.level().addParticle(ParticleTypes.ELECTRIC_SPARK,
                        pos.x, pos.y + 1, pos.z, 0, 0, 0);
            }
        }
    }

    @Override
    public void onStart(Player user, Level world) {
        super.onStart(user, world);
        lastTarget = null;
    }

    @Override
    public int getWindupPoint() {
        return 0;
    }
}