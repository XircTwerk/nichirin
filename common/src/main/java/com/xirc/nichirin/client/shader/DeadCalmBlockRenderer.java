package com.xirc.nichirin.client.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders blue overlay matching the DeadCalmAttack circle
 * 80% blue tint + enhanced contrast
 */
public class DeadCalmBlockRenderer {
    private static final int TOTAL_TICKS = 240;

    private boolean active = false;
    private int ticks = 0;
    private Vec3 centerPos = Vec3.ZERO;
    private float attackRange = 10.0f; // Will be set from attack
    private final List<BlockPos> affectedBlocks = new ArrayList<>();

    // Blue tint - 80% opacity
    private static final float WATER_R = 0.4f;
    private static final float WATER_G = 0.7f;
    private static final float WATER_B = 0.95f;
    private static final float WATER_ALPHA = 0.8f; // 80% blue

    // Contrast enhancement
    private static final float CONTRAST_BOOST = 1.3f;

    public void activate(Vec3 center, float range) {
        active = true;
        ticks = 0;
        centerPos = center;
        attackRange = range;
        affectedBlocks.clear();

        // IMMEDIATELY find all blocks in range
        updateAffectedBlocks();

        System.out.println("DEBUG: Block renderer activated - " + affectedBlocks.size() + " blocks in " + range + " block radius!");
    }

    public void activate(Vec3 center) {
        activate(center, 10.0f); // Default range
    }

    public void deactivate() {
        active = false;
        ticks = 0;
        affectedBlocks.clear();
        System.out.println("DEBUG: Block renderer deactivated");
    }

    public boolean isActive() {
        return active;
    }

    public void tick() {
        if (!active) return;

        ticks++;

        if (ticks >= TOTAL_TICKS) {
            deactivate();
            return;
        }

        // Update blocks every few ticks to catch any changes
        if (ticks % 5 == 0) {
            updateAffectedBlocks();
        }
    }

    public void setRange(float range) {
        this.attackRange = range;
        updateAffectedBlocks();
    }

    /**
     * Check if block is a full cube that should be affected
     */
    private boolean isFullCubeBlock(BlockState state, Level level, BlockPos pos) {
        // Check if block has collision (solid)
        if (!state.isSolidRender(level, pos)) {
            return false;
        }

        // Check if block is a full cube shape
        return state.isCollisionShapeFullBlock(level, pos);
    }

    private void updateAffectedBlocks() {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;

        affectedBlocks.clear();

        // Use EXACT center position (not rounded)
        double exactCenterX = centerPos.x;
        double exactCenterZ = centerPos.z;

        // Match the attack's circle exactly
        int minX = (int) Math.floor(exactCenterX - attackRange);
        int maxX = (int) Math.ceil(exactCenterX + attackRange);
        int minZ = (int) Math.floor(exactCenterZ - attackRange);
        int maxZ = (int) Math.ceil(exactCenterZ + attackRange);
        int centerY = (int) centerPos.y;

        // Check blocks in the circle (not sphere)
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                // Distance on XZ plane only (2D circle) from EXACT center
                double distXZ = Math.sqrt(
                        (x + 0.5 - exactCenterX) * (x + 0.5 - exactCenterX) +
                                (z + 0.5 - exactCenterZ) * (z + 0.5 - exactCenterZ)
                );

                if (distXZ <= attackRange) {
                    // Add blocks above and below center
                    for (int y = centerY - 3; y <= centerY + 5; y++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState state = level.getBlockState(pos);

                        // Only affect full cube blocks
                        if (!state.isAir() && isFullCubeBlock(state, level, pos)) {
                            affectedBlocks.add(pos);
                        }
                    }
                }
            }
        }
    }

    /**
     * Render 80% blue overlay + contrast enhancement
     */
    public void render(PoseStack poseStack) {
        if (!active || affectedBlocks.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        poseStack.pushPose();

        // Translate relative to camera
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        Matrix4f matrix = poseStack.last().pose();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (BlockPos pos : affectedBlocks) {
            renderBlockOverlay(buffer, matrix, pos);
        }

        tesselator.end();

        poseStack.popPose();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private void renderBlockOverlay(BufferBuilder buffer, Matrix4f matrix, BlockPos pos) {
        float x = pos.getX();
        float y = pos.getY();
        float z = pos.getZ();

        // 80% blue overlay
        float alpha = WATER_ALPHA;

        // Enhanced colors with contrast boost
        float r = WATER_R * CONTRAST_BOOST;
        float g = WATER_G * CONTRAST_BOOST;
        float b = WATER_B * CONTRAST_BOOST;

        // Clamp to valid range
        r = Math.min(r, 1.0f);
        g = Math.min(g, 1.0f);
        b = Math.min(b, 1.0f);

        // Top face - slightly above block for visibility
        float topY = y + 1.005f;
        buffer.vertex(matrix, x, topY, z).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, x, topY, z + 1).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, x + 1, topY, z + 1).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, x + 1, topY, z).color(r, g, b, alpha).endVertex();

        // Side faces with slightly less contrast
        float sideAlpha = alpha * 0.9f;
        float sideR = r * 0.95f;
        float sideG = g * 0.95f;
        float sideB = b * 0.95f;

        // North face
        buffer.vertex(matrix, x, y, z).color(sideR, sideG, sideB, sideAlpha).endVertex();
        buffer.vertex(matrix, x, y + 1, z).color(sideR, sideG, sideB, sideAlpha).endVertex();
        buffer.vertex(matrix, x + 1, y + 1, z).color(sideR, sideG, sideB, sideAlpha).endVertex();
        buffer.vertex(matrix, x + 1, y, z).color(sideR, sideG, sideB, sideAlpha).endVertex();

        // South face
        buffer.vertex(matrix, x + 1, y, z + 1).color(sideR, sideG, sideB, sideAlpha).endVertex();
        buffer.vertex(matrix, x + 1, y + 1, z + 1).color(sideR, sideG, sideB, sideAlpha).endVertex();
        buffer.vertex(matrix, x, y + 1, z + 1).color(sideR, sideG, sideB, sideAlpha).endVertex();
        buffer.vertex(matrix, x, y, z + 1).color(sideR, sideG, sideB, sideAlpha).endVertex();

        // East face
        buffer.vertex(matrix, x + 1, y, z).color(sideR, sideG, sideB, sideAlpha).endVertex();
        buffer.vertex(matrix, x + 1, y + 1, z).color(sideR, sideG, sideB, sideAlpha).endVertex();
        buffer.vertex(matrix, x + 1, y + 1, z + 1).color(sideR, sideG, sideB, sideAlpha).endVertex();
        buffer.vertex(matrix, x + 1, y, z + 1).color(sideR, sideG, sideB, sideAlpha).endVertex();

        // West face
        buffer.vertex(matrix, x, y, z + 1).color(sideR, sideG, sideB, sideAlpha).endVertex();
        buffer.vertex(matrix, x, y + 1, z + 1).color(sideR, sideG, sideB, sideAlpha).endVertex();
        buffer.vertex(matrix, x, y + 1, z).color(sideR, sideG, sideB, sideAlpha).endVertex();
        buffer.vertex(matrix, x, y, z).color(sideR, sideG, sideB, sideAlpha).endVertex();
    }
}