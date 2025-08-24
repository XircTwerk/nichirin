package com.xirc.nichirin.common.item.armor;

import com.xirc.nichirin.client.renderer.armor.ArmorRendererManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import mod.azure.azurelib.animatable.GeoItem;
import mod.azure.azurelib.constant.DataTickets;
import mod.azure.azurelib.core.animatable.instance.AnimatableInstanceCache;
import mod.azure.azurelib.core.animation.AnimatableManager;
import mod.azure.azurelib.core.animation.AnimationController;
import mod.azure.azurelib.core.animation.AnimationState;
import mod.azure.azurelib.core.animation.RawAnimation;
import mod.azure.azurelib.core.object.PlayState;
import mod.azure.azurelib.util.AzureLibUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class NichirinArmorItem extends ArmorItem implements GeoItem {
    private final AnimatableInstanceCache cache = AzureLibUtil.createInstanceCache(this);
    private final Supplier<Object> renderProvider = GeoItem.makeRenderer(this);

    public NichirinArmorItem(ArmorMaterial material, Type armorType, Properties properties) {
        super(material, armorType, properties);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 10, this::handleMovementAnimation));
    }

    private PlayState handleMovementAnimation(AnimationState<NichirinArmorItem> animationState) {
        Entity entity = animationState.getData(DataTickets.ENTITY);
        boolean moving = determineMovementState(entity);
        animationState.getController().setAnimation(RawAnimation.begin().thenLoop(moving ? "walking" : "idle"));
        return PlayState.CONTINUE;
    }

    private boolean determineMovementState(Entity entity) {
        if (entity instanceof Player player) {
            return Math.abs(player.walkDist - player.walkDistO) > 0.001f;
        } else if (entity instanceof LivingEntity livingEntity) {
            return livingEntity.getDeltaMovement().horizontalDistanceSqr() > 0.001;
        }
        return false;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void createRenderer(Consumer<Object> consumer) {
        if (Platform.getEnvironment() == Env.CLIENT) {
            consumer.accept(ArmorRendererManager.createRenderProvider());
        }
    }

    @Override
    public Supplier<Object> getRenderProvider() {
        return renderProvider;
    }
}