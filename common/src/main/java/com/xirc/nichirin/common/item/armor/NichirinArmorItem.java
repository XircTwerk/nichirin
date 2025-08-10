package com.xirc.nichirin.common.item.armor;

import com.xirc.nichirin.client.renderer.armor.ShinobuCapeRenderer;
import lombok.NonNull;
import mod.azure.azurelib.animatable.GeoItem;
import mod.azure.azurelib.animatable.client.RenderProvider;
import mod.azure.azurelib.constant.DataTickets;
import mod.azure.azurelib.core.animatable.instance.AnimatableInstanceCache;
import mod.azure.azurelib.core.animation.AnimatableManager;
import mod.azure.azurelib.core.animation.AnimationController;
import mod.azure.azurelib.core.animation.AnimationState;
import mod.azure.azurelib.core.animation.RawAnimation;
import mod.azure.azurelib.core.object.PlayState;
import mod.azure.azurelib.renderer.GeoArmorRenderer;
import mod.azure.azurelib.util.AzureLibUtil;
import com.xirc.nichirin.client.renderer.armor.NichirinArmorRenderer;
import com.xirc.nichirin.client.renderer.armor.ShinobuUniformRenderer;
import com.xirc.nichirin.client.model.NichirinArmorModel;
import com.xirc.nichirin.registry.NichirinItemRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

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
        Entity wearer = animationState.getData(DataTickets.ENTITY);
        boolean isMoving = determineMovementState(wearer);

        if (isMoving) {
            animationState.getController().setAnimation(RawAnimation.begin().thenLoop("Walking"));
        } else {
            animationState.getController().stop();
        }

        return PlayState.CONTINUE;
    }

    private boolean determineMovementState(Entity entity) {
        if (entity instanceof Player player) {
            return player.walkDist != player.walkDistO;
        } else {
            return entity.getDeltaMovement().horizontalDistanceSqr() > 0.01;
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void createRenderer(Consumer<Object> consumer) {
        consumer.accept(new RenderProvider() {
            private GeoArmorRenderer<?> renderer;

            @SuppressWarnings("unchecked")
            @Override
            public @NonNull HumanoidModel<LivingEntity> getHumanoidArmorModel(
                    LivingEntity entity, ItemStack armorStack, EquipmentSlot slot, HumanoidModel<LivingEntity> baseModel) {

                if (this.renderer == null) {
                    this.renderer = createRendererForArmor(armorStack);
                }

                renderer.prepForRender(entity, armorStack, slot, baseModel);
                return renderer;
            }
        });
    }

    private GeoArmorRenderer<?> createRendererForArmor(ItemStack armorStack) {
        if (armorStack.is(NichirinItemRegistry.SHINOBU_HEADPIECE.get())) {
            return new ShinobuUniformRenderer();
        }
        else if (armorStack.is(NichirinItemRegistry.SHINOBU_CAPE.get())) {
            return new ShinobuCapeRenderer();
        }
        else if (armorStack.is(NichirinItemRegistry.SHINOBU_LEGGINGS.get())) {
            return new ShinobuUniformRenderer();
        }
        else if (armorStack.is(NichirinItemRegistry.SHINOBU_BOOTS.get())) {
            return new ShinobuUniformRenderer();
        }

        return new NichirinArmorRenderer<>(new NichirinArmorModel<>("default_armor"));
    }

    @Override
    public Supplier<Object> getRenderProvider() {
        return renderProvider;
    }
}