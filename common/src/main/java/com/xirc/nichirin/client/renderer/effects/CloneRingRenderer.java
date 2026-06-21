package com.xirc.nichirin.client.renderer.effects;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.xirc.nichirin.client.afterimage.AfterimageRenderState;
import com.xirc.nichirin.common.network.s2c.CloneRingPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders a ring of tinted ghost clones of a caster — the generalized successor to
 * {@link MistCloneRenderer}. Each ring supports: a colour tint, staggered one-by-one
 * appearance, slow spin, and per-frame facing toward a target entity (so the clones
 * visibly track their victim). Activated server-side via CloneRingPacket.
 *
 * <p>The server mirrors {@link CloneRingPacket#cloneAngle(int, int, long, float)} when it
 * needs the world position of a clone (e.g. to fire a shockwave "from" a clone's fists), so
 * the spin formula must stay in sync between both sides.</p>
 */
@Environment(EnvType.CLIENT)
public class CloneRingRenderer {

    private static final double MAX_RENDER_DIST_SQR = 96.0 * 96.0;

    private record RingEntry(int casterId, Vec3 center, float radius, int count, long spawnTick,
                             long lifetimeTicks, float spinSpeed, int staggerTicks, int[] cloneTargetIds,
                             float r, float g, float b, float a) {}

    private static final List<RingEntry> ENTRIES = new ArrayList<>();

    private CloneRingRenderer() {}

    public static void set(int casterId, Vec3 center, float radius, int count, long lifetimeTicks,
                           float spinSpeed, int staggerTicks, int[] cloneTargetIds,
                           float r, float g, float b, float a) {
        clear(casterId);
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || count <= 0) return;
        ENTRIES.add(new RingEntry(casterId, center, radius, count, mc.level.getGameTime(),
                lifetimeTicks, spinSpeed, staggerTicks, cloneTargetIds, r, g, b, a));
    }

    public static void clear(int casterId) {
        ENTRIES.removeIf(e -> e.casterId() == casterId);
    }

    public static void render(PoseStack poseStack, Camera camera, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || ENTRIES.isEmpty()) return;

        long now = mc.level.getGameTime();
        ENTRIES.removeIf(e -> now - e.spawnTick() >= e.lifetimeTicks());
        if (ENTRIES.isEmpty()) return;

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        Vec3 camPos = camera.getPosition();

        // Reuse the afterimage render flag: hides nameplates and keeps other mixins treating
        // these dispatches as ghost renders, not real entities.
        AfterimageRenderState.setRendering(true);
        mc.getEntityRenderDispatcher().setRenderShadow(false);
        try {
            for (RingEntry entry : ENTRIES) {
                if (entry.center().distanceToSqr(camPos) > MAX_RENDER_DIST_SQR) continue;
                Entity casterEntity = mc.level.getEntity(entry.casterId());
                if (!(casterEntity instanceof AbstractClientPlayer caster)) continue;

                long elapsed = now - entry.spawnTick();
                int visible = entry.staggerTicks() <= 0
                        ? entry.count()
                        : (int) Math.min(entry.count(), elapsed / entry.staggerTicks() + 1);

                EntityRenderer<? super AbstractClientPlayer> renderer =
                        mc.getEntityRenderDispatcher().getRenderer(caster);
                ResourceLocation texture = renderer.getTextureLocation(caster);
                float bodyYaw = caster.yBodyRotO + (caster.yBodyRot - caster.yBodyRotO) * partialTick;

                for (int i = 0; i < visible; i++) {
                    double angle = CloneRingPacket.cloneAngle(i, entry.count(), elapsed, entry.spinSpeed());
                    double wx = entry.center().x + Math.cos(angle) * entry.radius();
                    double wz = entry.center().z + Math.sin(angle) * entry.radius();
                    double wy = entry.center().y;

                    // Each clone faces ITS OWN assigned target (per-clone id from the packet).
                    int targetId = i < entry.cloneTargetIds().length ? entry.cloneTargetIds()[i] : -1;
                    Entity target = targetId >= 0 ? mc.level.getEntity(targetId) : null;
                    Vec3 facePoint = target != null && target.isAlive()
                            ? target.position().add(0, target.getBbHeight() * 0.5, 0)
                            : entry.center();

                    // Extra Y-rotation about the clone's own origin that swings the caster's
                    // current body yaw onto the desired yaw.
                    double dx = facePoint.x - wx;
                    double dz = facePoint.z - wz;
                    float desiredYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));

                    poseStack.pushPose();
                    poseStack.translate(wx - camPos.x, wy - camPos.y, wz - camPos.z);
                    poseStack.mulPose(Axis.YP.rotationDegrees(bodyYaw - desiredYaw));
                    mc.getEntityRenderDispatcher().render(
                            caster, 0, 0, 0,
                            desiredYaw, partialTick,
                            poseStack,
                            new TintBufferSource(bufferSource, texture, entry.r(), entry.g(), entry.b(), entry.a()),
                            LightTexture.FULL_BRIGHT
                    );
                    poseStack.popPose();
                }
            }
        } finally {
            AfterimageRenderState.setRendering(false);
            mc.getEntityRenderDispatcher().setRenderShadow(true);
            bufferSource.endBatch();
        }
    }

    /** Forces every layer through a translucent type and multiplies vertex colour by the ring tint. */
    private record TintBufferSource(MultiBufferSource delegate, ResourceLocation texture,
                                    float r, float g, float b, float a) implements MultiBufferSource {
        @Override
        public VertexConsumer getBuffer(RenderType renderType) {
            return new TintVertexConsumer(delegate.getBuffer(RenderType.entityTranslucent(texture)), r, g, b, a);
        }
    }

    private static final class TintVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float r, g, b, a;

        TintVertexConsumer(VertexConsumer delegate, float r, float g, float b, float a) {
            this.delegate = delegate;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int cr, int cg, int cb, int ca) {
            delegate.setColor(
                    Math.round(cr * r), Math.round(cg * g), Math.round(cb * b), Math.round(ca * a));
            return this;
        }

        @Override
        public VertexConsumer setColor(float cr, float cg, float cb, float ca) {
            delegate.setColor(cr * r, cg * g, cb * b, ca * a);
            return this;
        }

        @Override
        public VertexConsumer setColor(int packed) {
            int cr = packed >> 16 & 0xFF;
            int cg = packed >> 8 & 0xFF;
            int cb = packed & 0xFF;
            return setColor(cr, cg, cb, 0xFF);
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            delegate.setNormal(x, y, z);
            return this;
        }
    }
}
