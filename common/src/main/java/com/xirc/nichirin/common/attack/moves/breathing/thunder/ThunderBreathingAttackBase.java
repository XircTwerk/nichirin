package com.xirc.nichirin.common.attack.moves.breathing.thunder;

import com.xirc.nichirin.common.attack.component.AbstractBreathingAttack;
import com.xirc.nichirin.common.attack.component.IBreathingAttacker;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import com.xirc.nichirin.common.vfx.VfxManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

// Base for Thunder Breathing attacks. All hits apply the Shocked status effect.
public abstract class ThunderBreathingAttackBase extends AbstractBreathingAttack<ThunderBreathingAttackBase, IBreathingAttacker<?, ?>> {

    @Override
    protected void hitTarget(LivingEntity target) {
        if (world.isClientSide) return;
        super.hitTarget(target);
        if (hitStun > 0) {
            target.addEffect(new MobEffectInstance(NichirinEffectRegistry.shocked(), hitStun, 0, false, true));
        }
    }

    @Override
    protected void hitTargetNoImmunity(LivingEntity target) {
        if (world.isClientSide) return;
        super.hitTargetNoImmunity(target);
        if (hitStun > 0) {
            target.addEffect(new MobEffectInstance(NichirinEffectRegistry.shocked(), hitStun, 0, false, true));
        }
    }

    public boolean isTeleportAttack() { return hasTeleport(); }
    public boolean isDashAttack() { return hasDash(); }

    public float getSpeedMultiplier() {
        if (hasTeleport()) return Float.MAX_VALUE;
        if (hasDash() && dashSpeed != null) return dashSpeed;
        return 1.0f;
    }

    public int getEffectiveWindup() {
        if (hasTeleportWindup() && teleportWindup != null) return teleportWindup;
        return windup;
    }

    protected void createThunderParticles() {}

    protected void playThunderSound() {}

    protected void playThunderVfx(ResourceLocation effectId, Vec3 origin, Vec3 direction, float scale) {
        if (world instanceof ServerLevel serverLevel) {
            VfxManager.playAttached(serverLevel, user, effectId, origin, direction, scale);
        }
    }

    protected void playThunderVfxAt(ResourceLocation effectId, Vec3 origin, Vec3 direction, float scale) {
        if (world instanceof ServerLevel serverLevel) {
            VfxManager.playOwned(serverLevel, user, effectId, origin, direction, scale);
        }
    }

    @Override
    protected abstract void onStart();

    @Override
    protected abstract void perform();

    @Override
    protected void onStop() {
        super.onStop();
    }
}
