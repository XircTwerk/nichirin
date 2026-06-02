package com.xirc.nichirin.mixin.client;

import com.xirc.nichirin.client.util.ClientInputHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftUseItemMixin {

    @Unique
    private boolean nichirin$vanillaUseClaimed;

    @Inject(method = "startUseItem", at = @At("HEAD"))
    private void nichirin$resetVanillaUseClaim(CallbackInfo ci) {
        nichirin$vanillaUseClaimed = false;
    }

    @Redirect(method = "startUseItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;interactAt(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/EntityHitResult;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult nichirin$recordInteractAt(MultiPlayerGameMode gameMode, Player player, Entity entity,
                                                        EntityHitResult hitResult, InteractionHand hand) {
        return nichirin$recordVanillaUse(gameMode.interactAt(player, entity, hitResult, hand));
    }

    @Redirect(method = "startUseItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;interact(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult nichirin$recordInteract(MultiPlayerGameMode gameMode, Player player, Entity entity,
                                                      InteractionHand hand) {
        return nichirin$recordVanillaUse(gameMode.interact(player, entity, hand));
    }

    @Redirect(method = "startUseItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;useItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult nichirin$recordUseItemOn(MultiPlayerGameMode gameMode, LocalPlayer player,
                                                       InteractionHand hand, BlockHitResult hitResult) {
        return nichirin$recordVanillaUse(gameMode.useItemOn(player, hand, hitResult));
    }

    @Redirect(method = "startUseItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;useItem(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult nichirin$recordUseItem(MultiPlayerGameMode gameMode, Player player,
                                                     InteractionHand hand) {
        return nichirin$recordVanillaUse(gameMode.useItem(player, hand));
    }

    @Inject(method = "startUseItem", at = @At("RETURN"))
    private void nichirin$sendDemonRightClickFallback(CallbackInfo ci) {
        if (!nichirin$vanillaUseClaimed) {
            ClientInputHandler.sendDemonRightClickFallback();
        }
    }

    @Unique
    private InteractionResult nichirin$recordVanillaUse(InteractionResult result) {
        if (result != InteractionResult.PASS) {
            nichirin$vanillaUseClaimed = true;
        }
        return result;
    }
}