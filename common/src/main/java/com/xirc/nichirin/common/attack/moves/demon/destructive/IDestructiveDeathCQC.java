package com.xirc.nichirin.common.attack.moves.demon.destructive;

import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.entity.attack.ShockwaveEntity;
import com.xirc.nichirin.registry.NichirinEntityRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Marker + helper interface for CQC attacks that are enhanced when the user has Destructive Death
 * equipped as their BDA.
 *
 * <p>Java doesn't allow multi-extends, so the "extends CQC and extends DD" effectively becomes
 * {@code extends AbstractCqcAttack implements IDestructiveDeathCQC}. The default helper methods
 * read the user's {@link DestructiveDeathState} and provide the shockwave-spawn / compass-aware
 * damage building blocks.</p>
 *
 * <p>Implementing this interface also signals to the CQC GUI that the move should render with the
 * "Destructive Death" light-blue border — wiring for that UI tag is done from the GUI side via
 * {@code instanceof}.</p>
 */
public interface IDestructiveDeathCQC {

    /**
     * Destructive Death must be the equipped BDA AND the Shockwave Toggle must be on. If the user
     * doesn't have DD equipped, shockwaves never fire regardless of the toggle — prevents the
     * persistent toggle state from carrying over to non-DD players.
     */
    default boolean isShockwaveEnabled(LivingEntity user) {
        if (!hasDestructiveDeathEquipped(user)) return false;
        ServerPlayer sp = (ServerPlayer) user;
        return DestructiveDeathState.isShockwaveEnabled(sp.getUUID());
    }

    default boolean isOverdriveActive(LivingEntity user) {
        if (!hasDestructiveDeathEquipped(user)) return false;
        ServerPlayer sp = (ServerPlayer) user;
        return DestructiveDeathState.isOverdriveEnabled(sp.getUUID(), sp.level().getGameTime());
    }

    /** True when the user actually has Destructive Death equipped as their BDA. */
    default boolean hasDestructiveDeathEquipped(LivingEntity user) {
        if (!(user instanceof ServerPlayer sp)) return false;
        return user instanceof Player p
                && "destructive_death".equals(MovesetHelper.getDemonMovesetId(p));
    }

    default boolean compassBuffsTarget(LivingEntity user, LivingEntity target) {
        if (!(user instanceof ServerPlayer sp)) return false;
        UUID id = sp.getUUID();
        long t = user.level().getGameTime();
        if (!DestructiveDeathState.isCompassActive(id, t)) return false;
        return CompassNeedleTracker.isTracked(id, target.getUUID());
    }

    /** Spawns a forward shockwave from {@code user}. Tint follows the user's Overdrive flag. */
    default ShockwaveEntity spawnForwardShockwave(LivingEntity user, Level world,
                                                  float damage, float speed, int lifeTicks,
                                                  float hitboxRadius, int pierces) {
        if (world.isClientSide || user == null) return null;
        Vec3 origin = user.position()
                .add(0, user.getBbHeight() * 0.55, 0)
                .add(user.getLookAngle().scale(0.6));
        Vec3 direction = user.getLookAngle().normalize();
        return new ShockwaveEntity.Builder()
                .owner(user)
                .origin(origin)
                .direction(direction)
                .damage(damage)
                .speed(speed)
                .lifeTicks(Math.max(4, lifeTicks))
                .hitboxRadius(hitboxRadius)
                .pierces(pierces)
                .destructiveDeathCqcDamage()
                .red(isOverdriveActive(user))
                .spawn(NichirinEntityRegistry.SHOCKWAVE.get(), world);
    }
}
