package com.xirc.nichirin.common.attack.moves.demon.destructive;

import com.xirc.nichirin.common.attack.component.AbstractDemonAttack;
import com.xirc.nichirin.common.attack.component.IDemonAttacker;
import com.xirc.nichirin.common.entity.attack.ShockwaveEntity;
import com.xirc.nichirin.registry.NichirinEntityRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Base class for all Destructive Death (Akaza) BDA attacks.
 *
 * <p>Provides shared helpers: spawn-shockwave-in-front-of-user that auto-tints red when the user
 * has Overdrive enabled, and a check for whether the shockwave toggle is currently on.</p>
 */
public abstract class DestructiveDeathAttackBase extends AbstractDemonAttack<DestructiveDeathAttackBase, IDemonAttacker> {

    /** True if this user's "spawn shockwave on CQC/M1 hit" toggle is on. */
    protected boolean isShockwaveEnabled() {
        if (!(user instanceof ServerPlayer sp)) return false;
        return DestructiveDeathState.isShockwaveEnabled(sp.getUUID());
    }

    /** True if Overdrive buff is on for this user — used to flip shockwave tint red. */
    protected boolean isOverdriveActive() {
        if (!(user instanceof ServerPlayer sp)) return false;
        return DestructiveDeathState.isOverdriveEnabled(sp.getUUID());
    }

    /** Spawns a single shockwave forward from the user. Tint follows Overdrive. */
    protected ShockwaveEntity spawnForwardShockwave(float damage, float speed, float lifeTicks,
                                                    float hitboxRadius, int pierces) {
        if (world.isClientSide || user == null) return null;
        Vec3 origin = user.position().add(0, user.getBbHeight() * 0.55, 0)
                .add(user.getLookAngle().scale(0.6));
        Vec3 direction = user.getLookAngle().normalize();
        return new ShockwaveEntity.Builder()
                .owner(user)
                .origin(origin)
                .direction(direction)
                .damage(damage)
                .speed(speed)
                .lifeTicks(Math.max(4, (int) lifeTicks))
                .hitboxRadius(hitboxRadius)
                .pierces(pierces)
                .red(isOverdriveActive())
                .spawn(NichirinEntityRegistry.SHOCKWAVE.get(), world);
    }

    /** True if Compass Needle's damage buff applies to the given target. */
    protected boolean compassBuffsTarget(LivingEntity target) {
        if (!(user instanceof ServerPlayer sp)) return false;
        UUID id = sp.getUUID();
        long t = world.getGameTime();
        if (!DestructiveDeathState.isCompassActive(id, t)) return false;
        return CompassNeedleTracker.isTracked(id, target.getUUID());
    }
}
