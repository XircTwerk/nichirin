package com.xirc.nichirin.client.renderer.item;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.client.animation.GenyaDBAnimator;
import com.xirc.nichirin.common.item.gun.GenyaDB;
import com.xirc.nichirin.common.util.ItemStackData;
import com.mojang.blaze3d.platform.Lighting;
import mod.azure.azurelib.common.model.AzBone;
import mod.azure.azurelib.common.render.AzRendererPipelineContext;
import mod.azure.azurelib.common.render.item.AzItemRenderer;
import mod.azure.azurelib.common.render.item.AzItemRendererConfig;
import mod.azure.azurelib.common.render.item.AzItemRendererPipeline;
import mod.azure.azurelib.common.render.item.AzItemRendererPipelineContext;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.Set;
import java.util.UUID;

/**
 * Item renderer for the gun. Identical to {@link KatanaItemRenderer} except it hides the
 * muzzle-flash / effect bones, which are only meant to appear while firing — otherwise they
 * render on the held/GUI item as huge offset quads.
 */
public class GunItemRenderer extends AzItemRenderer {

    // Effect bones in genya_db.geo.json (large flash/pressure quads).
    private static final String[] EFFECT_BONES = {"fireL", "fireR", "pressure"};
    private static final Set<String> EFFECT_BONE_SET = Set.of(EFFECT_BONES);
    // Demon-only flesh bone — shown only when a demon holds the gun.
    private static final String FLESH_BONE = "flesh";
    // The muzzle-flash/pressure art lives on a separate texture; the main texture is blank there.
    private static final ResourceLocation EFFECTS_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "textures/item/genya_db/effects.png");
    // Render type bound to effects.png for the flash bones. A bare texture override is NOT enough:
    // AzureLib's getDefaultRenderType ignores the override texture for non-translucent items, so the
    // bones would fall back to the (blank) main texture. Supplying a full render type bypasses that.
    // We use the "eyes" type so the muzzle flash renders full-bright (emissive) and ignores world
    // lighting — like glowing mob eyes — instead of being dimmed by the surrounding light level.
    private static final RenderType EFFECTS_RENDER_TYPE = RenderType.eyes(EFFECTS_TEXTURE);

    public GunItemRenderer(ResourceLocation geoModel, ResourceLocation texture) {
        super(AzItemRendererConfig.builder(geoModel, texture)
                .setAnimatorProvider(() -> new GenyaDBAnimator())
                .setBoneTextureOverrideProvider(
                        bone -> EFFECT_BONE_SET.contains(bone.getName()) ? EFFECTS_TEXTURE : null)
                .setBoneRenderTypeOverrideProvider(
                        bone -> EFFECT_BONE_SET.contains(bone.getName()) ? EFFECTS_RENDER_TYPE : null)
                .build());
    }

    @Override
    protected AzItemRendererPipeline createPipeline(AzItemRendererConfig config) {
        return new AzItemRendererPipeline(config, this) {
            @Override
            public void preRender(AzRendererPipelineContext<UUID, ItemStack> context, boolean isReRender) {
                var itemContext = (AzItemRendererPipelineContext) context;
                if (itemContext.getTransformType() == ItemDisplayContext.GUI) {
                    context.poseStack().translate(-0.2, -0.2, 0.05);
                }
                super.preRender(context, isReRender);
                if (itemContext.getTransformType() == ItemDisplayContext.GUI) {
                    Lighting.setupFor3DItems();
                }
                hideEffectBones(context);
            }
        };
    }

    private void hideEffectBones(AzRendererPipelineContext<UUID, ItemStack> context) {
        var model = context.bakedModel();
        if (model == null) return;

        // Show the muzzle-flash / pressure bones only while a fire animation is the active one. We read
        // the controller's actual current animation rather than a shared flag so this can't get out of
        // sync across the GUI / first-person / third-person render passes.
        boolean firing = false;
        if (getAnimator() instanceof GenyaDBAnimator gun) {
            String anim = gun.currentAnimationName();
            firing = anim != null && anim.startsWith("fire");
        }
        final boolean visible = firing;
        for (String boneName : EFFECT_BONES) {
            model.getBone(boneName).ifPresent(b -> b.setHidden(!visible));
        }
        if (visible) {
            // The effects bone is the parent of the flash bones; idle keeps its scale at 0. Force it to 1
            // here so the children are visible even on the transition frame before the fire keyframes apply.
            model.getBone("effects").ifPresent(b -> {
                b.setScaleX(1f);
                b.setScaleY(1f);
                b.setScaleZ(1f);
            });
        }

        ItemStack stack = context.animatable();
        boolean demon = stack != null && ItemStackData.get(stack).getBoolean(GenyaDB.DEMON_RENDER_KEY);
        model.getBone(FLESH_BONE).ifPresent(b -> b.setHidden(!demon));
    }

    public static GunItemRenderer create(String geoName, String textureName) {
        return new GunItemRenderer(
                ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "geo/" + geoName + ".geo.json"),
                ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "textures/item/" + textureName + ".png")
        );
    }
}
