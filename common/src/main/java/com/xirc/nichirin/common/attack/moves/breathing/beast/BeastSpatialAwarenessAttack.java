package com.xirc.nichirin.common.attack.moves.breathing.beast;

import com.xirc.nichirin.common.vfx.VfxIds;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

// Seventh Form: Spatial Awareness. Applies glowing to all nearby enemies so the user can track them through walls.
public class BeastSpatialAwarenessAttack extends BeastBreathingAttackBase {

    private static final int GLOW_DURATION = 200;
    private boolean activated = false;

    @Override
    protected void onStart() {
        activated = false;
    }

    @Override
    protected void onActiveStart() {
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.5f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide || activated) return;

        activateSpatialAwareness();
        activated = true;
    }

    private void activateSpatialAwareness() {
        AABB searchBox = new AABB(
                user.getX() - range, user.getY() - range, user.getZ() - range,
                user.getX() + range, user.getY() + range, user.getZ() + range
        );

        List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != user && e.isAlive());

        int detected = 0;
        for (LivingEntity entity : entities) {
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_DURATION, 0, false, false, false));
            detected++;
        }

        playBeastVfx(VfxIds.BEAST_SPATIAL_AWARENESS,
                user.position().add(0, user.getBbHeight() * 0.4, 0), user.getLookAngle(), range / 6.0f);

        if (user instanceof Player) {
            Player player = (Player) user;
            player.displayClientMessage(
                    Component.literal("Spatial Awareness: " + detected + " entities detected")
                            .withStyle(s -> s.withColor(0x88FF88)),
                    true
            );
        }

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.0f, 0.8f);
    }

    @Override
    protected void onStop() {
        activated = false;
    }
}
