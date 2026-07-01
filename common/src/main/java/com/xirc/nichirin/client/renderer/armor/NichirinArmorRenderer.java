package com.xirc.nichirin.client.renderer.armor;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.client.renderer.armor.core.NichirinArmorAnimator;
import com.xirc.nichirin.client.renderer.armor.core.NichirinArmorBoneProvider;
import mod.azure.azurelib.common.animation.dispatch.AzDispatchSide;
import mod.azure.azurelib.common.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.common.animation.play_behavior.AzPlayBehaviors;
import mod.azure.azurelib.common.model.AzBakedModel;
import mod.azure.azurelib.common.model.AzBone;
import mod.azure.azurelib.common.render.AzRendererConfig;
import mod.azure.azurelib.common.render.AzRendererPipelineContext;
import mod.azure.azurelib.common.render.armor.AzArmorRenderer;
import mod.azure.azurelib.common.render.armor.AzArmorRendererConfig;
import mod.azure.azurelib.common.render.armor.AzArmorRendererPipeline;
import mod.azure.azurelib.common.render.armor.AzArmorRendererPipelineContext;
import mod.azure.azurelib.common.render.armor.bone.AzArmorBoneContext;
import mod.azure.azurelib.common.render.armor.bone.AzArmorBoneProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import com.xirc.nichirin.mixin.client.PlayerModelAccessor;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import java.util.Base64;

/**
 * Base armor renderer for Nichirin armors using AzureLib 3.x API
 */
public class NichirinArmorRenderer extends AzArmorRenderer {

    private static final AzCommand ANIM_WALKING = AzCommand.create("movement_controller", "walking", AzPlayBehaviors.LOOP);
    private static final AzCommand ANIM_IDLE = AzCommand.create("movement_controller", "idle", AzPlayBehaviors.LOOP);
    private static final AzCommand ANIM_CROUCHING = AzCommand.create("movement_controller", "crouching", AzPlayBehaviors.LOOP);

    protected Entity currentEntity;
    protected EquipmentSlot currentSlot;
    protected HumanoidModel<?> currentBaseModel;
    protected AzBakedModel currentModel;
    protected ItemStack currentStack;

    @Nullable private final String animationName;
    private final AzArmorBoneProvider armorBoneProvider;
    // Per-entity last-known animation state. The renderer instance is shared across every wearer,
    // so a single field would let one entity's movement corrupt another's animation state.
    // null = not yet seen this entity (forces an initial dispatch so idle starts at rest).
    private final Map<UUID, MovementAnimation> lastAnimationByEntity = new WeakHashMap<>();

    private enum MovementAnimation {
        IDLE,
        WALKING,
        CROUCHING
    }

    /** For uniforms: uses NichirinArmorBoneProvider which maps our geo bone names to standard slots */
    protected NichirinArmorRenderer(String armorName) {
        this(armorName, armorName, NichirinArmorBoneProvider.INSTANCE, null);
    }

    protected NichirinArmorRenderer(String modelName, String textureName) {
        this(modelName, textureName, NichirinArmorBoneProvider.INSTANCE, null);
    }

    /** For capes/accessories: pass a custom bone provider */
    protected NichirinArmorRenderer(String modelName, String textureName, AzArmorBoneProvider boneProvider) {
        this(modelName, textureName, boneProvider, null);
    }

    /** For capes with animations: pass a custom bone provider and animation name */
    protected NichirinArmorRenderer(String modelName, String textureName, AzArmorBoneProvider boneProvider, @Nullable String animationName) {
        super(createConfig(modelName, textureName, boneProvider, animationName));
        this.animationName = animationName;
        this.armorBoneProvider = boneProvider;
    }

    private static AzArmorRendererConfig createConfig(String modelName, String textureName, AzArmorBoneProvider boneProvider, @Nullable String animName) {
        ResourceLocation geoModel = BreathOfNichirin.id("geo/" + modelName + ".geo.json");
        ResourceLocation texture = BreathOfNichirin.id("textures/armor/" + textureName + ".png");

        var builder = AzArmorRendererConfig.builder(geoModel, texture)
                .setBoneProvider(boneProvider)
                .setPrerenderEntry(NichirinArmorRenderer::scaleArmsToSkin);

        if (animName != null) {
            final String name = animName;
            builder.setAnimatorProvider(() -> new NichirinArmorAnimator(name));
        }

        return builder.build();
    }

    /**
     * Override the pipeline to:
     * 1. Re-apply our custom bone visibility AFTER the pipeline's default visibility,
     *    so the chestplate bone is visible when leggings are worn, etc.
     * 2. Guard against NPE in scaleBoneWithModelPart when getBodyBone() returns null
     *    (AzureLib 3.1.2 does not null-check the body bone before scaling).
     */
    @Override
    @SuppressWarnings("rawtypes")
    protected AzArmorRendererPipeline createPipeline(AzRendererConfig config) {
        return new AzArmorRendererPipeline(config, this) {
            @Override
            public void preRender(AzRendererPipelineContext<UUID, ItemStack> context, boolean isReRender) {
                super.preRender(context, isReRender);
                // applyBaseTransformations ran inside super.preRender above.
                // Re-apply our bone transforms NOW so they take effect last, before rendering.
                // This is the actual fix for arms not moving: applyBaseTransformations may not
                // handle our custom bone names / null body bone correctly, so we override it here.

                // The pipeline copies body ROTATION to the provider's body bone but not POSITION.
                // Without this, capes and accessories don't shift down when the player crouches.
                // Apply body position+rotation here so all child animation bones inherit the shift.
                // Renderers that need an additional offset (e.g. Rengoku's offsetBodyBone) run in
                // applyBoneTransformations() below and override this call.
                if (NichirinArmorRenderer.this.currentBaseModel != null
                        && NichirinArmorRenderer.this.currentBaseModel.body != null
                        && NichirinArmorRenderer.this.currentModel != null) {
                    AzBone providerBodyBone = NichirinArmorRenderer.this.armorBoneProvider
                            .getBodyBone(NichirinArmorRenderer.this.currentModel);
                    if (providerBodyBone != null) {
                        NichirinArmorRenderer.this.matchBodyBone(
                                NichirinArmorRenderer.this.currentBaseModel.body, providerBodyBone);
                    }
                }

                NichirinArmorRenderer.this.applyBoneTransformations();
                // Override pipeline visibility so we control which bones show per slot.
                var armorCtx = (AzArmorRendererPipelineContext) context;
                NichirinArmorRenderer.this.applyBoneVisibilityBySlot(armorCtx.currentSlot());
            }

            @Override
            public void scaleBoneWithModelPart(AzArmorRendererPipelineContext context, AzArmorBoneContext boneContext, boolean isReRender) {
                // Null-safe: getBodyBone() intentionally returns null; the super call NPEs in
                // AzureLib 3.1.2 when it tries to scale a null body bone.
                try {
                    super.scaleBoneWithModelPart(context, boneContext, isReRender);
                } catch (NullPointerException ignored) {
                    // Body bone is null by design — skip body scaling.
                }
            }
        };
    }

    @Override
    public void prepForRender(@Nullable Entity entity, ItemStack stack, @Nullable EquipmentSlot slot, @Nullable HumanoidModel<?> baseModel) {
        super.prepForRender(entity, stack, slot, baseModel);

        this.currentEntity = entity;
        this.currentSlot = slot;
        this.currentBaseModel = baseModel;
        this.currentStack = stack;

        // Get the current baked model from the renderer after super call
        this.currentModel = getCurrentBakedModel();

        // Apply body rotation manually (arms/legs/boots are handled by the pipeline via bone provider).
        if (entity != null && slot != null && baseModel != null && this.currentModel != null) {
            applyBoneTransformations();
        }

        // Dispatch the looping animation when the wearer's movement state changes (avoids snap/restart
        // every frame). On first sight of an entity (lastAnimation == null) we always dispatch so the idle
        // loop actually starts at rest instead of waiting for the first movement transition.
        if (animationName != null && entity instanceof LivingEntity living && animator() != null) {
            MovementAnimation animation = living.isCrouching()
                    ? MovementAnimation.CROUCHING
                    : living.getDeltaMovement().horizontalDistanceSqr() > 0.0001
                            ? MovementAnimation.WALKING
                            : MovementAnimation.IDLE;
            MovementAnimation lastAnimation = lastAnimationByEntity.get(living.getUUID());
            if (lastAnimation != animation) {
                AzCommand cmd = switch (animation) {
                    case IDLE -> ANIM_IDLE;
                    case WALKING -> ANIM_WALKING;
                    case CROUCHING -> ANIM_CROUCHING;
                };
                cmd.actions().forEach(action -> action.handle(AzDispatchSide.CLIENT, animator()));
                lastAnimationByEntity.put(living.getUUID(), animation);
            }
        }
    }

    /**
     * Dispatch a command directly to the current item animator (client-side).
     */
    protected void dispatchAnimation(AzCommand command) {
        if (animator() != null) {
            command.actions().forEach(action -> action.handle(AzDispatchSide.CLIENT, animator()));
        }
    }

    /**
     * Gets the baked model for the current item stack using the provider API.
     */
    protected AzBakedModel getCurrentBakedModel() {
        if (currentStack == null) return null;
        return provider().provideBakedModel(currentEntity, currentStack);
    }

    /**
     * Apply custom bone transformations.
     * Arms, legs, and boots rotations are handled automatically by the pipeline via
     * NichirinArmorBoneProvider. Only the body/chestplate needs manual rotation + position since
     * getBodyBone() returns null (to avoid AzureLib hiding it for non-CHEST slots).
     * Override in subclasses to add character-specific transforms.
     */
    protected void applyBoneTransformations() {
        if (currentBaseModel == null) return;
        // Chestplate is not in the bone provider, so apply body rotation + position manually.
        // This mirrors what applyBaseTransformations does for body bones.
        matchBodyBone(currentBaseModel.body, getBone("chestplate"));
        // Belt-and-suspenders: explicitly apply arm/leg rotations in case the pipeline's
        // applyBaseTransformations doesn't update them (null body bone path or caching issues).
        matchRotation(currentBaseModel.rightArm, getBone("rightArm"));
        matchRotation(currentBaseModel.leftArm, getBone("leftArm"));
        matchRotation(currentBaseModel.rightLeg, getBone("rightLeg"));
        matchRotation(currentBaseModel.leftLeg, getBone("leftLeg"));
        matchRotation(currentBaseModel.rightLeg, getBone("rightBoot"));
        matchRotation(currentBaseModel.leftLeg, getBone("leftBoot"));

        // Arm-width scaling is applied via the prerender hook (scaleArmsToSkin) so the pipeline
        // doesn't overwrite it afterwards.
    }

    private static final float REGULAR_ARM_SCALE_X = (4.0f / 3.0f) + 0.05f;

    /**
     * Prerender hook: scales the armor arm bones to the wearer's skin width. Geo arms are modeled
     * slim (3px), so regular (Steve) arms widen to {@link #REGULAR_ARM_SCALE_X}; slim (Alex) arms
     * render at 0.75x that. Applied here rather than in applyBoneTransformations because the pipeline
     * resets bone scale after preRender, which silently undid the old approach.
     */
    private static AzRendererPipelineContext<UUID, ItemStack> scaleArmsToSkin(
            AzRendererPipelineContext<UUID, ItemStack> ctx) {
        AzBakedModel model = ctx.bakedModel();
        if (model == null) return ctx;

        AzBone leftArm = model.getBoneOrNull("leftArm");
        AzBone rightArm = model.getBoneOrNull("rightArm");
        if (leftArm == null || rightArm == null) return ctx;

        float scaleX = isSlimSkin(ctx.currentEntity()) ? REGULAR_ARM_SCALE_X * 0.75f : REGULAR_ARM_SCALE_X;
        leftArm.setScaleX(scaleX);
        rightArm.setScaleX(scaleX);
        return ctx;
    }

    /**
     * Scales the given arm bones to match the player's skin width (Steve = 4px wide, Alex = 3px).
     * Assumes the geo arm bones are 3px wide (Alex-sized): Steve gets 4/3, Alex stays at 1.0.
     * Call this from cape/accessory renderers that have their own arm bone names.
     */
    protected void scaleArmBones(AzBone leftArmBone, AzBone rightArmBone) {
        scaleArmBones(leftArmBone, rightArmBone, 1.0f);
    }

    /**
     * Same as scaleArmBones but multiplies the arm-width correction by an additional factor.
     * Use when you also want a uniform scale on top of the slim/wide correction (e.g. 1.2f for Rengoku).
     */
    protected void scaleArmBones(AzBone leftArmBone, AzBone rightArmBone, float multiplier) {
        // Reliable slim detection isn't available here (AzureLib passes plain HumanoidModel,
        // not PlayerModel, and getModelName() is unreliable for offline/dev players).
        // Default to Steve width (4/3) for everyone — slightly wide for Alex but acceptable.
        float scaleX = multiplier * (4.0f / 3.0f);
        if (leftArmBone  != null) leftArmBone.setScaleX(scaleX);
        if (rightArmBone != null) rightArmBone.setScaleX(scaleX);
    }

    /**
     * Scales every bone in the model (including children recursively) uniformly on all three axes.
     * Call scaleArmBones AFTER this to correctly override the arm bone X scale.
     */
    protected void scaleAllBones(float scale) {
        if (this.currentModel != null) {
            for (AzBone bone : this.currentModel.getTopLevelBones()) {
                scaleBonesRecursive(bone, scale);
            }
        }
    }

    private void scaleBonesRecursive(AzBone bone, float scale) {
        bone.setScaleX(scale);
        bone.setScaleY(scale);
        bone.setScaleZ(scale);
        for (AzBone child : bone.getChildBones()) {
            scaleBonesRecursive(child, scale);
        }
    }

    /**
     * Scales the given bone and all of its descendants uniformly. Use for "make this body
     * part X times larger" cases (e.g. the inosuke boar helmet).
     */
    protected void scaleBoneTree(@Nullable AzBone bone, float scale) {
        if (bone == null) return;
        scaleBonesRecursive(bone, scale);
    }

    /**
     * Scales a single bone (NOT its children). Child bones inherit the parent's scale through the
     * bone matrix, so to grow a whole body part uniformly you scale only its root bone — scaling
     * every descendant as well compounds (parent × child) and distorts the part.
     */
    protected void scaleBone(@Nullable AzBone bone, float scale) {
        if (bone == null) return;
        bone.setScaleX(scale);
        bone.setScaleY(scale);
        bone.setScaleZ(scale);
    }

    /**
     * Offsets a body-following bone (one set up by NichirinCapeArmorBoneProvider) by upOffset pixels
     * upward and backOffset pixels toward the back of the player.
     */
    protected void offsetBodyBone(String boneName, float upOffset, float backOffset) {
        AzBone bone = getBone(boneName);
        if (bone != null && currentBaseModel != null && currentBaseModel.body != null) {
            bone.updatePosition(
                    currentBaseModel.body.x,
                    -currentBaseModel.body.y + upOffset,
                    currentBaseModel.body.z + backOffset
            );
        }
    }

    /**
     * Matches rotation AND position of a body ModelPart to a geo bone.
     * Mirrors what AzArmorBoneContext.applyBaseTransformations does for body bones.
     */
    protected void matchBodyBone(ModelPart bodyPart, AzBone bone) {
        if (bodyPart != null && bone != null) {
            matchRotation(bodyPart, bone);
            bone.updatePosition(bodyPart.x, -bodyPart.y, bodyPart.z);
        }
    }

    /** Matches the kimono torso root to the player's body. */
    protected void matchKimonoBone(AzBone bone) {
        if (bone == null || currentBaseModel == null || currentBaseModel.body == null) return;
        matchBodyBone(currentBaseModel.body, bone);
    }

    /**
     * Matches rotation AND position of an arm ModelPart to a geo bone.
     * Mirrors what AzArmorBoneContext.applyBaseTransformations does for arm bones.
     * @param isLeft true for the left arm (uses x-5 offset), false for right arm (uses x+5 offset)
     */
    protected void matchArmBone(ModelPart armPart, AzBone bone, boolean isLeft) {
        if (armPart != null && bone != null) {
            matchRotation(armPart, bone);
            float posX = isLeft ? (armPart.x - 5f) : (armPart.x + 5f);
            float posY = 2f - armPart.y;
            bone.updatePosition(posX, posY, armPart.z);
        }
    }

    /**
     * Apply bone visibility based on equipment slot.
     * Called AFTER the pipeline's own visibility logic so our choices win
     * (e.g., chestplate visible when leggings are worn).
     * Override in subclasses.
     */
    protected void applyBoneVisibilityBySlot(EquipmentSlot slot) {
        // Base implementation — override in subclasses
        setAllVisible(false);
    }

    /**
     * Apply body transformation helper
     */
    protected void applyBodyTransform(AzBone bodyBone) {
        if (bodyBone != null && currentBaseModel != null && currentBaseModel.body != null) {
            matchRotation(currentBaseModel.body, bodyBone);
        }
    }

    /**
     * Apply arm transformation helper
     */
    protected void applyArmTransform(AzBone armBone, ModelPart armPart, boolean isLeft) {
        if (armBone != null && armPart != null) {
            matchRotation(armPart, armBone);
        }
    }

    /**
     * Reliable slim-skin check for the armor wearer. AzureLib passes a plain {@link HumanoidModel}
     * here (no slim flag), and skin-string checks are unreliable in singleplayer/dev, so we read
     * the player's actual {@link PlayerModel} straight from its renderer and check the {@code slim}
     * field via {@link PlayerModelAccessor}.
     */
    protected static boolean isSlimSkin(@Nullable Entity entity) {
        if (!(entity instanceof AbstractClientPlayer player)) return false;
        var renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        if (!(renderer instanceof PlayerRenderer playerRenderer)) return false;
        PlayerModel<AbstractClientPlayer> playerModel = playerRenderer.getModel();
        return ((PlayerModelAccessor) playerModel).isSlim();
    }

    /**
     * Utility method to check if a player has a slim (Alex) skin model
     */
    protected boolean isSlimPlayer(AbstractClientPlayer player) {
        try {
            // Method 1: Direct GameProfile access
            GameProfile profile = player.getGameProfile();
            if (profile != null) {
                PropertyMap properties = profile.getProperties();
                if (properties.containsKey("textures")) {
                    Property textureProperty = properties.get("textures").iterator().next();
                    String texturesJson = new String(Base64.getDecoder().decode(textureProperty.value()));
                    if (texturesJson.contains("\"slim\"")) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {}

        try {
            // Method 2: Check the player's model name
            if (player.getSkin().texture() != null) {
                return "slim".equals(player.getSkin().model().id());
            }
        } catch (Exception ignored) {}

        // Method 3: UUID fallback (Minecraft's default algorithm)
        return (player.getUUID().hashCode() & 1) == 1;
    }

    /**
     * Utility method to set bone visibility
     */
    protected void setBoneVisible(@Nullable AzBone bone, boolean visible) {
        if (bone != null) {
            bone.setHidden(!visible);
        }
    }

    /**
     * Utility method to set all bones invisible/visible
     */
    protected void setAllVisible(boolean visible) {
        if (this.currentModel != null) {
            for (AzBone bone : this.currentModel.getTopLevelBones()) {
                setAllBonesRecursive(bone, visible);
            }
        }
    }

    /**
     * Recursively set visibility for bone and all children
     */
    protected void setAllBonesRecursive(AzBone bone, boolean visible) {
        bone.setHidden(!visible);
        for (AzBone child : bone.getChildBones()) {
            setAllBonesRecursive(child, visible);
        }
    }

    /**
     * Helper to get a bone by name
     */
    protected @Nullable AzBone getBone(String boneName) {
        return this.currentModel != null ? this.currentModel.getBone(boneName).orElse(null) : null;
    }

    /**
     * Helper to match rotation from a vanilla ModelPart to a geo bone.
     * Mirrors AzureLib's RenderUtils.matchModelPartRot behaviour: xRot and yRot are
     * negated so the geo bone faces the correct direction.
     */
    protected void matchRotation(ModelPart modelPart, AzBone bone) {
        if (modelPart != null && bone != null) {
            bone.setRotX(-modelPart.xRot);
            bone.setRotY(-modelPart.yRot);
            bone.setRotZ(modelPart.zRot);
        }
    }
}