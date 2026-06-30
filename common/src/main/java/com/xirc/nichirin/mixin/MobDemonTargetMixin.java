package com.xirc.nichirin.mixin;

import com.xirc.nichirin.common.entity.npc.DemonNPCEntity;
import com.xirc.nichirin.common.system.DemonManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes ordinary hostile mobs treat demon players as neutral: they won't acquire a demon as a
 * target on their own, but still retaliate if the demon attacks them first (provoked). The mod's
 * own demon NPCs are excluded so scripted demon encounters keep working.
 *
 * <p>{@code canAttack} is the gate vanilla target goals use to decide whether a mob may target an
 * entity, so returning {@code false} here stops unprovoked aggro at the source rather than fighting
 * the AI each tick. Injected with {@code require = 0} so a mapping mismatch degrades gracefully
 * (feature simply doesn't apply) instead of crashing at load.</p>
 */
@Mixin(Mob.class)
public abstract class MobDemonTargetMixin {

    @Inject(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void nichirin$hostilesIgnoreDemons(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        Mob self = (Mob) (Object) this;
        if (self instanceof Monster
                && !(self instanceof DemonNPCEntity)
                && target instanceof Player player
                && DemonManager.isDemon(player)
                && self.getLastHurtByMob() != target) {
            cir.setReturnValue(false);
        }
    }
}
