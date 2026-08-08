package com.xirc.nichirin.mixin.client;

import com.xirc.nichirin.client.renderer.entity.layer.ArmTrailLayer;
import com.xirc.nichirin.client.renderer.entity.layer.SheathedKatanaLayer;
import com.xirc.nichirin.registry.NichirinItemRegistry;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public PlayerRendererMixin(EntityRendererProvider.Context context, PlayerModel<AbstractClientPlayer> model, float shadowRadius) {
        super(context, model, shadowRadius);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void nichirin$addSheathedKatanaLayer(EntityRendererProvider.Context context, boolean useSlimModel, CallbackInfo ci) {
        this.addLayer(new SheathedKatanaLayer(this));
        this.addLayer(new ArmTrailLayer(this));
    }

    @Inject(method = "setModelProperties", at = @At("TAIL"))
    private void nichirin$hideRightLegForJigoroBoots(AbstractClientPlayer player, CallbackInfo ci) {
        if (player.getItemBySlot(EquipmentSlot.FEET).is(NichirinItemRegistry.JIGORO_BOOTS.get())) {
            // Hide BOTH the base right leg and its separate outer/jacket layer (rightPants) — the peg
            // leg replaces it, and the outer layer otherwise renders on top of the peg.
            this.getModel().rightLeg.visible = false;
            this.getModel().rightPants.visible = false;
        }
    }
}