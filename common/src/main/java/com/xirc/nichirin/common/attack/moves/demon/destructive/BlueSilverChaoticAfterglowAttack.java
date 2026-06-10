package com.xirc.nichirin.common.attack.moves.demon.destructive;

import com.xirc.nichirin.common.entity.attack.ShockwaveEntity;
import com.xirc.nichirin.registry.NichirinEntityRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Destructive Death ultimate — Blue Silver Chaotic Afterglow.
 *
 * <p>Requires Compass Needle to be active (per the lore). On execute, spawns 12 shockwaves spread
 * across the horizon around the user, each travelling outward at high speed with deep damage and a
 * single pierce so they can each hit several enemies along their path.</p>
 */
public class BlueSilverChaoticAfterglowAttack extends DestructiveDeathAttackBase {

    private static final int CLONE_COUNT = 12;
    private static final float CLONE_DAMAGE = 9.0f;
    private static final float CLONE_SPEED = 0.85f;
    private static final int CLONE_LIFE_TICKS = 30;
    private static final float CLONE_HITBOX = 1.1f;
    private static final int CLONE_PIERCES = 4;

    private boolean hasExecuted = false;

    @Override
    protected void onStart() {
        hasExecuted = false;
    }

    @Override
    protected void perform() {
        if (hasExecuted) return;
        hasExecuted = true;
        if (!(user instanceof ServerPlayer sp)) return;

        if (!DestructiveDeathState.isCompassActive(sp.getUUID(), world.getGameTime())) {
            sp.displayClientMessage(
                    Component.literal("Compass Needle must be active!")
                            .withStyle(s -> s.withColor(0xFF5555)), true);
            return;
        }

        Vec3 centre = sp.position().add(0, sp.getBbHeight() * 0.55, 0);
        for (int i = 0; i < CLONE_COUNT; i++) {
            double angle = (i / (double) CLONE_COUNT) * Math.PI * 2.0;
            Vec3 dir = new Vec3(Math.cos(angle), 0, Math.sin(angle));
            Vec3 spawn = centre.add(dir.scale(0.6));
            new ShockwaveEntity.Builder()
                    .owner(sp)
                    .origin(spawn)
                    .direction(dir)
                    .damage(CLONE_DAMAGE * (compassFinalScalar()))
                    .speed(CLONE_SPEED)
                    .lifeTicks(CLONE_LIFE_TICKS)
                    .hitboxRadius(CLONE_HITBOX)
                    .pierces(CLONE_PIERCES)
                    .red(isOverdriveActive())
                    .spawn(NichirinEntityRegistry.SHOCKWAVE.get(), world);
        }

        world.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.5f, 0.6f);
    }

    /** Compass Overdrive boost: tracked-only enhancement multiplier. */
    private float compassFinalScalar() {
        if (!(user instanceof ServerPlayer sp)) return 1.0f;
        return DestructiveDeathState.isCompassOverdriveActive(sp.getUUID(), world.getGameTime()) ? 1.35f : 1.0f;
    }

    @Override
    protected void onStop() {
        hasExecuted = false;
    }
}
