package com.xirc.nichirin.common.attack.moves.breathing.beast;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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

        createAwarenessEffect();

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

    private void createAwarenessEffect() {
        if (!(world instanceof ServerLevel sl)) return;
        Vec3 center = user.position().add(0, user.getBbHeight() / 2, 0);

        for (int ring = 1; ring <= 5; ring++) {
            double r = ring * (range / 5.0);
            int steps = ring * 8;
            for (int i = 0; i < steps; i++) {
                double angle = (i / (double) steps) * 2 * Math.PI;
                double x = center.x + Math.cos(angle) * r;
                double z = center.z + Math.sin(angle) * r;
                sl.sendParticles(ParticleTypes.END_ROD, x, center.y, z, 1, 0.05, 0.3, 0.05, 0.02);
            }
        }

        sl.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z,
                20, 1.0, 1.0, 1.0, 0.3);
    }

    @Override
    protected void onStop() {
        activated = false;
    }
}