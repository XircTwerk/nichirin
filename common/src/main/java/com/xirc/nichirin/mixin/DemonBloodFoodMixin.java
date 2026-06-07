package com.xirc.nichirin.mixin;

import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.config.NichirinModConfig;
import com.xirc.nichirin.common.event.system.DemonFoodHandler;
import com.xirc.nichirin.common.system.DemonComponent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class DemonBloodFoodMixin {

    @Inject(method = "startUsingItem", at = @At("HEAD"), cancellable = true)
    private void nichirin$blockDemonEatingWhenBloodFull(InteractionHand hand, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof Player player)) return;
        if (player.isCreative()) return;
        if (!isDemonForFoodUse(player.level(), player)) return;

        ItemStack stack = player.getItemInHand(hand);
        if (!stack.has(DataComponents.FOOD)) return;
        if (!DemonFoodHandler.isBloodFood(stack) || !canAttemptBloodFood(player.level(), player)) {
            ci.cancel();
        }
    }

    @Inject(method = "eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/food/FoodProperties;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"))
    private void nichirin$gainBloodFromRawFood(Level level, ItemStack food, FoodProperties foodProperties,
                                               CallbackInfoReturnable<ItemStack> cir) {
        if (level.isClientSide()) return;
        if (!DemonFoodHandler.isBloodFood(food)) return;

        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof Player player)) return;
        if (!MovesetHelper.hasDemonMoveset(player)) return;
        if (player.isCreative()) return;

        if (DemonFoodHandler.addHalfBloodPoint(player)) {
            player.displayClientMessage(
                    Component.literal("+0.5 Blood")
                            .withStyle(style -> style.withColor(0xAA0000)),
                    true
            );
        }
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
