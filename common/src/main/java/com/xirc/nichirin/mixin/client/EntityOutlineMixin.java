package com.xirc.nichirin.mixin.client;

import com.xirc.nichirin.client.outline.OutlineTracker;
import com.xirc.nichirin.common.outline.OutlineInstance;
import net.minecraft.world.entity.Entity;
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
 */
@Mixin(Entity.class)
public abstract class EntityOutlineMixin {

    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void nichirin$outlineForceGlow(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self.level() == null || !self.level().isClientSide) return;
        if (!OutlineTracker.getOutlines(self.getUUID()).isEmpty()) {
            cir.setReturnValue(true);
        }
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

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
}
