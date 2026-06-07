package com.xirc.nichirin.mixin;

import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.event.system.DemonFoodHandler;
import com.xirc.nichirin.common.config.NichirinModConfig;
import com.xirc.nichirin.common.system.DemonComponent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class DemonBloodFoodUseMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void nichirin$useDemonBloodFood(Level level, Player player, InteractionHand hand,
                                            CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (!isDemonForFoodUse(level, player) || player.isCreative()) {
            return;
        }

        ItemStack stack = player.getItemInHand(hand);
        if (!stack.has(DataComponents.FOOD)) {
            return;
        }

        if (!DemonFoodHandler.isBloodFood(stack)) {
            cir.setReturnValue(InteractionResultHolder.fail(stack));
            return;
        }

        if (!canAttemptBloodFood(level, player)) {
            cir.setReturnValue(InteractionResultHolder.fail(stack));
            return;
        }

        player.startUsingItem(hand);
        cir.setReturnValue(InteractionResultHolder.consume(stack));
    }

    private boolean isDemonForFoodUse(Level level, Player player) {
        return level.isClientSide()
                ? DemonComponent.isClientDemon() || MovesetHelper.hasDemonMoveset(player)
                : MovesetHelper.hasDemonMoveset(player);
    }

    private boolean canAttemptBloodFood(Level level, Player player) {
        if (!level.isClientSide()) {
            return DemonFoodHandler.canGainBlood(player);
        }

        int maxBlood = NichirinModConfig.get().demon.maxBloodPoints;
        double actualBlood = DemonComponent.getClientBloodPoints() - DemonComponent.getClientHalfBloodPoints() * 0.5D;
        return actualBlood <= maxBlood * 0.5D;
    }
}
