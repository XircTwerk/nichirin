package com.xirc.nichirin.mixin.client;

import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.item.gun.GenyaDB;
import com.xirc.nichirin.common.item.katana.Katana;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class SwingSuppressionMixin {

    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @At("HEAD"), cancellable = true)
    private void nichirin$suppressModAttackSwing(InteractionHand hand, boolean updateSelf, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity)(Object)this;
        if (!(entity instanceof LocalPlayer player)) return;
        ItemStack item = player.getMainHandItem();
        // Katana, gun, and bare-hand CQC all play their own arm animation, so hide the vanilla swing.
        // Demons are intentionally excluded: their default bare-hand left click has no replacement
        // animation, so suppressing the swing would leave no feedback at all.
        if (item.getItem() instanceof Katana
                || item.getItem() instanceof GenyaDB
                || (item.isEmpty() && MovesetHelper.hasFightingMoveset(player))) {
            ci.cancel();
        }
    }
}
