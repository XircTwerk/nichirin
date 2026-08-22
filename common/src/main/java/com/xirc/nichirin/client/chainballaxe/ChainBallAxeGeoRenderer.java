package com.xirc.nichirin.client.chainballaxe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.chainballaxe.ChainBallAxeWeaponSimulation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders the two ends (axe + flail) as their authored geo models at the simulated positions. The chain-ball-axe
 * models are axis-aligned box-UV cubes with no rotations, so this is a tiny self-contained Bedrock-cube
 * renderer (no AzureLib) — which lets the held item be a plain 2D icon while the 3D weapon is drawn by
 * the sim. The axe's chain-socket is placed exactly at the sim's axe point, so the chain connects to it.
 */
@Environment(EnvType.CLIENT)
public final class ChainBallAxeGeoRenderer {

    private static final ResourceLocation FLAIL_GEO = mod("geo/chain_ball_axe_flail.geo.json");
    private static final ResourceLocation FLAIL_TEX = mod("textures/item/chain_ball_axe_flail.png");

    private ChainBallAxeGeoRenderer() {}

    private static ResourceLocation mod(String p) {
        return ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, p);
    }

    public static void renderAll(PoseStack poseStack, Camera camera, float partialTick) {
        if (!ClientChainBallAxeWeaponManager.isEnabled()) return;
        ChainBallAxeWeaponSimulation sim = ClientChainBallAxeWeaponManager.sim();
        if (sim == null) return;

        Vec3 cam = camera.getPosition();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();

        // The axe is the conventionally-held item (MC renders it in-hand like any vanilla item); the sim
        // only draws the flail (ball). Its socket points back up the chain toward the previous node.
        Vec3 flailPos = sim.flail.renderPosition(partialTick);
        Vec3 flailUp = sim.point(sim.pointCount() - 2).renderPosition(partialTick).subtract(flailPos);
        renderModel(FLAIL_GEO, FLAIL_TEX, poseStack, buffers, cam, flailPos, upRotation(flailUp), Vec3.ZERO);

        buffers.endBatch();
    }

    /** Rotation that stands a model upright (model +Y along {@code upDir}). */
    private static Quaternionf upRotation(Vec3 upDir) {
        if (upDir.lengthSqr() < 1.0e-6) return new Quaternionf();
        Vector3f up = new Vector3f((float) upDir.x, (float) upDir.y, (float) upDir.z).normalize();
        return new Quaternionf().rotationTo(new Vector3f(0, 1, 0), up);
    }

    private static void renderModel(ResourceLocation geo, ResourceLocation tex, PoseStack poseStack,
                                    MultiBufferSource buffers, Vec3 cam, Vec3 worldPos, Quaternionf rotation, Vec3 pivot) {
        Model m = load(geo);
        if (m.cubes.isEmpty()) return;

        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(tex));
        poseStack.pushPose();
        poseStack.translate(worldPos.x - cam.x, worldPos.y - cam.y, worldPos.z - cam.z);
        poseStack.mulPose(rotation);
        poseStack.scale(1 / 16f, 1 / 16f, 1 / 16f);
        poseStack.translate(-pivot.x, -pivot.y, -pivot.z);

        Matrix4f mat = poseStack.last().pose();
        for (Cube c : m.cubes) renderCube(vc, mat, c, m.texW, m.texH);
        poseStack.popPose();
    }

    private static void renderCube(VertexConsumer vc, Matrix4f mat, Cube c, float tw, float th) {
        float x0 = c.origin[0], y0 = c.origin[1], z0 = c.origin[2];
        float sx = c.size[0], sy = c.size[1], sz = c.size[2];
        float x1 = x0 + sx, y1 = y0 + sy, z1 = z0 + sz;
        float u = c.uv[0], v = c.uv[1];
        int light = LightTexture.FULL_BRIGHT;
        // Standard Bedrock box-UV net.
        face(vc, mat, light, x0,y1,z1, x1,y1,z1, x1,y1,z0, x0,y1,z0, u+sz,v,      sx,sz, tw,th, 0,1,0);   // up
        face(vc, mat, light, x0,y0,z0, x1,y0,z0, x1,y0,z1, x0,y0,z1, u+sz+sx,v,   sx,sz, tw,th, 0,-1,0);  // down
        face(vc, mat, light, x1,y1,z0, x0,y1,z0, x0,y0,z0, x1,y0,z0, u+sz,v+sz,   sx,sy, tw,th, 0,0,-1);  // north
        face(vc, mat, light, x0,y1,z1, x1,y1,z1, x1,y0,z1, x0,y0,z1, u+2*sz+sx,v+sz, sx,sy, tw,th, 0,0,1);// south
        face(vc, mat, light, x1,y1,z1, x1,y1,z0, x1,y0,z0, x1,y0,z1, u,v+sz,      sz,sy, tw,th, 1,0,0);   // east
        face(vc, mat, light, x0,y1,z0, x0,y1,z1, x0,y0,z1, x0,y0,z0, u+sz+sx,v+sz,sz,sy, tw,th, -1,0,0);  // west
    }

    private static void face(VertexConsumer vc, Matrix4f mat, int light,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float uMin, float vMin, float uw, float vh, float tw, float th,
                             float nx, float ny, float nz) {
        float u0 = uMin / tw, u1 = (uMin + uw) / tw, v0 = vMin / th, v1 = (vMin + vh) / th;
        vertex(vc, mat, ax, ay, az, u0, v0, light, nx, ny, nz);
        vertex(vc, mat, bx, by, bz, u1, v0, light, nx, ny, nz);
        vertex(vc, mat, cx, cy, cz, u1, v1, light, nx, ny, nz);
        vertex(vc, mat, dx, dy, dz, u0, v1, light, nx, ny, nz);
    }

    private static void vertex(VertexConsumer vc, Matrix4f mat, float x, float y, float z,
                               float u, float v, int light, float nx, float ny, float nz) {
        vc.addVertex(mat, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(nx, ny, nz);
    }

    // --- Geo parsing (cached) ---

    private record Cube(float[] origin, float[] size, float[] uv) {}

    private static final class Model {
        int texW = 64, texH = 64;
        final List<Cube> cubes = new ArrayList<>();
    }

    private static final Map<ResourceLocation, Model> CACHE = new HashMap<>();

    private static Model load(ResourceLocation geo) {
        return CACHE.computeIfAbsent(geo, rl -> {
            Model m = new Model();
            try {
                var opt = Minecraft.getInstance().getResourceManager().getResource(rl);
                if (opt.isEmpty()) return m;
                try (var reader = new InputStreamReader(opt.get().open())) {
                    JsonObject geoObj = JsonParser.parseReader(reader).getAsJsonObject()
                            .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
                    JsonObject desc = geoObj.getAsJsonObject("description");
                    if (desc.has("texture_width")) m.texW = desc.get("texture_width").getAsInt();
                    if (desc.has("texture_height")) m.texH = desc.get("texture_height").getAsInt();
                    for (var boneEl : geoObj.getAsJsonArray("bones")) {
                        JsonObject bone = boneEl.getAsJsonObject();
                        if (!bone.has("cubes")) continue;
                        for (var cubeEl : bone.getAsJsonArray("cubes")) {
                            JsonObject c = cubeEl.getAsJsonObject();
                            float[] uv = c.get("uv").isJsonArray() ? arr2(c.getAsJsonArray("uv")) : new float[]{0, 0};
                            m.cubes.add(new Cube(arr3(c.getAsJsonArray("origin")), arr3(c.getAsJsonArray("size")), uv));
                        }
                    }
                }
            } catch (Exception ignored) {
            }
            return m;
        });
    }

    private static float[] arr3(JsonArray a) {
        return new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()};
    }

    private static float[] arr2(JsonArray a) {
        return new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat()};
    }
}
