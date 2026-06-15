package com.xirc.nichirin.mixin.client;

import com.xirc.nichirin.client.outline.OutlineTracker;
import com.xirc.nichirin.common.outline.OutlineInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Routes SEE-THROUGH outline instances through MC's built-in outline framebuffer + edge-
 * detection post-shader — the same system spectator mode uses. That pipeline is screen-space
 * and ignores depth, which is exactly right for outlines meant to show through walls.
 *
 * Depth-tested outlines (seeThroughWalls=false) do NOT use this path: they render through
 * {@link com.xirc.nichirin.client.outline.OutlineRenderer}'s geometry pass, whose LEQUAL depth
 * test occludes the outline per-pixel behind world geometry — only the hidden part disappears.
 *
 * Two injections:
 *   isCurrentlyGlowing → force true for see-through outline hosts → triggers MC's outline
 *                        pipeline to capture that entity's silhouette.
 *   getTeamColor       → return our configured RGB so the outline post-shader uses our
 *                        colour instead of the entity's team colour.
 */
@Mixin(Entity.class)
public abstract class EntityOutlineMixin {

    // Within this distance the host's outlined silhouette fills the screen — don't force the glow.
    private static final double OUTLINE_CAMERA_INSIDE_DIST_SQR = 2.0 * 2.0;

    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void nichirin$outlineForceGlow(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self.level() == null || !self.level().isClientSide) return;
        // Don't force-glow a host the camera is essentially inside: MC's outline pipeline renders
        // its silhouette filling the screen — your own body in first person, or another player
        // standing on top of you.
        Vec3 camPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        if (self.distanceToSqr(camPos.x, camPos.y, camPos.z) < OUTLINE_CAMERA_INSIDE_DIST_SQR) return;
        List<OutlineInstance> outlines = OutlineTracker.getOutlines(self.getUUID());
        if (outlines.isEmpty()) return;
        for (OutlineInstance inst : outlines) {
            if (inst.seeThroughWalls()) {
                cir.setReturnValue(true);
                return;
            }
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
