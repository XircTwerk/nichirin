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
import com.xirc.nichirin.client.renderer.armor.ZenitsuUniformRenderer;
import com.xirc.nichirin.client.renderer.armor.ZenitsuCapeRenderer;
import com.xirc.nichirin.client.renderer.armor.RengokuCapeRenderer;
import com.xirc.nichirin.client.renderer.armor.RengokuUniformRenderer;
import com.xirc.nichirin.client.renderer.armor.TengenAccessoriesRenderer;
import com.xirc.nichirin.client.renderer.armor.TengenUniformRenderer;
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

    // Animation constants
    private static final RawAnimation WALK_START = RawAnimation.begin().thenPlay("walk_start").thenLoop("walking");
    private static final RawAnimation WALK_STOP = RawAnimation.begin().thenPlay("walk_stop");
    private static final RawAnimation WALKING_LOOP = RawAnimation.begin().thenLoop("walking");

    // Fallback animation in case the main animations aren't found
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");

    // Movement state tracking
    private boolean wasMovingLastTick = false;

    public NichirinArmorItem(ArmorMaterial material, Type armorType, Properties properties) {
        super(material, armorType, properties);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 5, this::handleMovementAnimation));
    }

    private PlayState handleMovementAnimation(AnimationState<NichirinArmorItem> animationState) {
        Entity wearer = animationState.getData(DataTickets.ENTITY);
        if (wearer == null) {
            return PlayState.CONTINUE;
        }

        boolean isMoving = determineMovementState(wearer);

        try {
            // Check for movement state changes
            if (isMoving && !wasMovingLastTick) {
                // Player just started moving - play walk_start then loop walking
                animationState.getController().setAnimation(WALK_START);
                wasMovingLastTick = true;
            } else if (!isMoving && wasMovingLastTick) {
                // Player just stopped moving - play walk_stop animation
                animationState.getController().setAnimation(WALK_STOP);
                wasMovingLastTick = false;
            } else if (isMoving && wasMovingLastTick) {
                // Player is continuing to move - ensure we're in walking loop
                // Only set if not already playing the correct animation to avoid interrupting walk_start
                mod.azure.azurelib.core.animation.AnimationProcessor.QueuedAnimation currentAnim = animationState.getController().getCurrentAnimation();
                if (currentAnim != null && currentAnim.animation() != null) {
                    String currentAnimName = currentAnim.animation().name();
                    if (!currentAnimName.equals("walk_start") && !currentAnimName.equals("walking")) {
                        animationState.getController().setAnimation(WALKING_LOOP);
                    }
                } else {
                    // If no animation is playing, start the walking loop
                    animationState.getController().setAnimation(WALKING_LOOP);
                }
            }
            // If not moving and wasn't moving last tick, do nothing (stay in idle/stopped state)
        } catch (Exception e) {
            // Fallback to prevent crashes - just continue without setting animations
            System.out.println("Animation error in NichirinArmorItem: " + e.getMessage());
        }

        return PlayState.CONTINUE;
    }

    private boolean determineMovementState(Entity entity) {
        if (entity instanceof Player player) {
            // More sensitive movement detection
            return Math.abs(player.walkDist - player.walkDistO) > 0.001f;
        } else if (entity instanceof LivingEntity livingEntity) {
            // For other living entities, check velocity
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

        if (armorStack.is(NichirinItemRegistry.ZENITSU_HEADPIECE.get())) {
            return new ZenitsuUniformRenderer();
        }
        else if (armorStack.is(NichirinItemRegistry.ZENITSU_CAPE.get())) {
            return new ZenitsuCapeRenderer();
        }
        else if (armorStack.is(NichirinItemRegistry.ZENITSU_LEGGINGS.get())) {
            return new ZenitsuUniformRenderer();
        }
        else if (armorStack.is(NichirinItemRegistry.ZENITSU_BOOTS.get())) {
            return new ZenitsuUniformRenderer();
        }

        if (armorStack.is(NichirinItemRegistry.RENGOKU_HEADPIECE.get())) {
            return new RengokuUniformRenderer();
        }
        else if (armorStack.is(NichirinItemRegistry.RENGOKU_CAPE.get())) {
            return new RengokuCapeRenderer();
        }
        else if (armorStack.is(NichirinItemRegistry.RENGOKU_LEGGINGS.get())) {
            return new RengokuUniformRenderer();
        }
        else if (armorStack.is(NichirinItemRegistry.RENGOKU_BOOTS.get())) {
            return new RengokuUniformRenderer();
        }

        if (armorStack.is(NichirinItemRegistry.TENGEN_HEADPIECE.get())) {
            return new TengenUniformRenderer();
        }
        else if (armorStack.is(NichirinItemRegistry.TENGEN_ACCESSORIES.get())) {
            return new TengenAccessoriesRenderer();
        }
        else if (armorStack.is(NichirinItemRegistry.TENGEN_LEGGINGS.get())) {
            return new TengenUniformRenderer();
        }
        else if (armorStack.is(NichirinItemRegistry.TENGEN_BOOTS.get())) {
            return new TengenUniformRenderer();
        }

        return new NichirinArmorRenderer<>(new NichirinArmorModel<>("default_armor"));
    }

    @Override
    public Supplier<Object> getRenderProvider() {
        return renderProvider;
    }
}