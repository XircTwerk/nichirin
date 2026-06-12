package com.xirc.nichirin.mixin.client;

import com.xirc.nichirin.client.outline.OutlineTracker;
import com.xirc.nichirin.common.outline.OutlineInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Routes our outline-tracker entities through MC's built-in outline framebuffer + edge-
 * detection post-shader — the same system spectator mode uses. Produces a clean 1-pixel
 * cel-shader edge with no z-fighting (it's pure screen-space, not depth-tested geometry).
 *
 * Two injections:
 *   isCurrentlyGlowing → force true for any entity in OutlineTracker → triggers MC's
 *                        outline pipeline to capture that entity's silhouette and run
 *                        the outline post-shader on it.
 *   getTeamColor       → return our configured RGBA so the outline post-shader uses our
 *                        colour instead of the entity's team colour.
 *
 * The vanilla outline post-shader is screen-space and ignores depth, so by itself it
 * shows through walls. For instances with seeThroughWalls=false we gate the glow on a
 * camera→entity line-of-sight raycast: fully occluded entities don't glow at all.
 */
@Mixin(Entity.class)
public abstract class EntityOutlineMixin {

    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void nichirin$outlineForceGlow(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self.level() == null || !self.level().isClientSide) return;
        List<OutlineInstance> outlines = OutlineTracker.getOutlines(self.getUUID());
        if (outlines.isEmpty()) return;

        boolean seeThrough = false;
        for (OutlineInstance inst : outlines) {
            if (inst.seeThroughWalls()) {
                seeThrough = true;
                break;
            }
        }
        if (seeThrough || nichirin$visibleFromCamera(self)) {
            cir.setReturnValue(true);
        }
        // Occluded + not see-through: fall through to vanilla (no forced glow).
    }

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void nichirin$outlineCustomColor(CallbackInfoReturnable<Integer> cir) {
        Entity self = (Entity) (Object) this;
        if (self.level() == null || !self.level().isClientSide) return;
        List<OutlineInstance> outlines = OutlineTracker.getOutlines(self.getUUID());
        if (outlines.isEmpty()) return;
        OutlineInstance inst = outlines.get(0);
        int r = clamp(Math.round(inst.r() * 255f));
        int g = clamp(Math.round(inst.g() * 255f));
        int b = clamp(Math.round(inst.b() * 255f));
        cir.setReturnValue((r << 16) | (g << 8) | b);
    }

    /** True when any sample point on the entity (eyes, center) has clear line of sight to the camera. */
    private static boolean nichirin$visibleFromCamera(Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameRenderer == null || mc.level == null) return true;
        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
        // The local player's own outline: camera is inside/at the entity, always visible.
        if (entity == mc.cameraEntity) return true;

        Vec3 eye = entity.getEyePosition();
        Vec3 center = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
        return nichirin$clearPath(entity, camPos, eye) || nichirin$clearPath(entity, camPos, center);
    }

    private static boolean nichirin$clearPath(Entity entity, Vec3 from, Vec3 to) {
        HitResult hit = entity.level().clip(new ClipContext(
                from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, entity));
        return hit.getType() == HitResult.Type.MISS;
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
}
