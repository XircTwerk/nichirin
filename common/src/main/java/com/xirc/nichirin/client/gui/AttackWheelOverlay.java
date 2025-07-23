package com.xirc.nichirin.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.data.BreathingStyleHelper;
import com.xirc.nichirin.common.data.MovesetRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * HUD-based attack wheel that doesn't block player input
 * Renders as an overlay instead of a Screen
 */
public class AttackWheelOverlay {

    private static final int OUTER_RADIUS = 120;
    private static final int INNER_RADIUS = 40;
    private static final int ICON_SIZE = 32;

    private final List<MoveSegment> segments = new ArrayList<>();
    private float segmentAngle;
    private boolean isActive = false;
    private final Minecraft minecraft;

    public AttackWheelOverlay() {
        this.minecraft = Minecraft.getInstance();
    }

    public void activate() {
        isActive = true;
        rebuildWheel();
    }

    public int deactivate() {
        isActive = false;
        return getSelectedMove();
    }

    public boolean isActive() {
        return isActive;
    }

    private void rebuildWheel() {
        segments.clear();

        if (minecraft.player == null) {
            return;
        }

        Player player = minecraft.player;
        String movesetId = BreathingStyleHelper.getMovesetId(player);

        if (movesetId == null || movesetId.isEmpty()) {
            return;
        }

        AbstractMoveset moveset = MovesetRegistry.getMoveset(movesetId);
        if (moveset == null) {
            return;
        }

        buildWheel(moveset);
    }

    private void buildWheel(AbstractMoveset moveset) {
        segments.clear();

        if (moveset == null) {
            return;
        }

        for (int i = 0; i < moveset.getMoveCount(); i++) {
            AbstractMoveset.MoveConfiguration config = moveset.getMove(i);
            if (config != null) {
                MoveSegment segment = new MoveSegment(i, config);
                segments.add(segment);
            }
        }

        if (!segments.isEmpty()) {
            segmentAngle = 360f / segments.size();
        }
    }

    public int getSelectedMove() {
        if (segments.isEmpty() || minecraft == null) return -1;

        double mouseX = minecraft.mouseHandler.xpos() * minecraft.getWindow().getGuiScaledWidth() / minecraft.getWindow().getScreenWidth();
        double mouseY = minecraft.mouseHandler.ypos() * minecraft.getWindow().getGuiScaledHeight() / minecraft.getWindow().getScreenHeight();

        int centerX = minecraft.getWindow().getGuiScaledWidth() / 2;
        int centerY = minecraft.getWindow().getGuiScaledHeight() / 2;

        float angle = (float) Math.toDegrees(Math.atan2(mouseY - centerY, mouseX - centerX));
        if (angle < 0) angle += 360;

        int segmentIndex = (int)(angle / segmentAngle);
        if (segmentIndex >= 0 && segmentIndex < segments.size()) {
            return segments.get(segmentIndex).index;
        }

        return -1;
    }

    public void render(GuiGraphics guiGraphics) {
        if (!isActive) return;

        int centerX = minecraft.getWindow().getGuiScaledWidth() / 2;
        int centerY = minecraft.getWindow().getGuiScaledHeight() / 2;

        double mouseX = minecraft.mouseHandler.xpos() * minecraft.getWindow().getGuiScaledWidth() / minecraft.getWindow().getScreenWidth();
        double mouseY = minecraft.mouseHandler.ypos() * minecraft.getWindow().getGuiScaledHeight() / minecraft.getWindow().getScreenHeight();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Use your existing working methods but with better colors
        drawFilledCircle(guiGraphics, centerX, centerY, OUTER_RADIUS, 0.3f, 0.3f, 0.3f, 0.5f); // Dark gray background
        drawFilledCircle(guiGraphics, centerX, centerY, INNER_RADIUS, 0.2f, 0.2f, 0.2f, 0.7f);   // Darker center

        if (segments.isEmpty()) {
            for (int i = 0; i < 4; i++) {
                float startAngle = i * 90f;
                float endAngle = (i + 1) * 90f;
                drawSegment(guiGraphics, centerX, centerY, startAngle, endAngle, false);
            }
            Font font = minecraft.font;
            guiGraphics.drawCenteredString(font, "No moves available", centerX, centerY - 4, 0xFFFFFF);

            // Draw circles at the end
            drawCircle(guiGraphics, centerX, centerY, INNER_RADIUS, 0.0f, 0.0f, 0.0f, 1.0f);
            drawCircle(guiGraphics, centerX, centerY, OUTER_RADIUS, 0.0f, 0.0f, 0.0f, 1.0f);
        } else {
            // Calculate hovered segment
            float angle = (float) Math.toDegrees(Math.atan2(mouseY - centerY, mouseX - centerX));
            if (angle < 0) angle += 360;

            int hoveredSegment = -1;
            float distance = getDistanceFromCenter((int)mouseX, (int)mouseY, centerX, centerY);
            if (distance <= OUTER_RADIUS && distance >= INNER_RADIUS) {
                hoveredSegment = (int)(angle / segmentAngle);
                if (hoveredSegment >= segments.size()) hoveredSegment = segments.size() - 1;

                drawCircle(guiGraphics, centerX, centerY, INNER_RADIUS, 0.0f, 0.0f, 0.0f, 1.0f);
                drawCircle(guiGraphics, centerX, centerY, OUTER_RADIUS, 0.0f, 0.0f, 0.0f, 1.0f);

            }

            // Draw segments
            for (int i = 0; i < segments.size(); i++) {
                float startAngle = i * segmentAngle;
                float endAngle = (i + 1) * segmentAngle;
                boolean isHovered = (i == hoveredSegment);
                drawSegment(guiGraphics, centerX, centerY, startAngle, endAngle, isHovered);
            }

            // Draw move names
            for (int i = 0; i < segments.size(); i++) {
                float startAngle = i * segmentAngle;
                boolean isHovered = (i == hoveredSegment);

                float midAngle = startAngle + segmentAngle / 2;
                int textX = centerX + (int)((INNER_RADIUS + OUTER_RADIUS) / 2 * Math.cos(Math.toRadians(midAngle)));
                int textY = centerY + (int)((INNER_RADIUS + OUTER_RADIUS) / 2 * Math.sin(Math.toRadians(midAngle)));

                String moveName = segments.get(i).config.getDisplayName();
                int textColor = isHovered ? 0x55FF55 : 0xFFFFFF; // GREEN when hovered
                Font font = minecraft.font;
                guiGraphics.drawCenteredString(font, moveName, textX, textY - 4, textColor);
            }

            // Draw center icon
            if (hoveredSegment >= 0 && hoveredSegment < segments.size()) {
                MoveSegment selectedSegment = segments.get(hoveredSegment);
                drawCenterIcon(guiGraphics, centerX, centerY, selectedSegment);
            }
        }

        RenderSystem.disableBlend();
    }

    private void drawCenterIcon(GuiGraphics guiGraphics, int centerX, int centerY, MoveSegment segment) {
        if (segment.config.getIconLocation() != null) {
            int iconX = centerX - ICON_SIZE / 2;
            int iconY = centerY - ICON_SIZE / 2;

            RenderSystem.setShaderTexture(0, segment.config.getIconLocation());
            guiGraphics.blit(segment.config.getIconLocation(), iconX, iconY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        } else {
            Font font = minecraft.font;
            guiGraphics.drawCenteredString(font, "?", centerX, centerY - 4, 0xFFFFFF);
        }
    }

    private void drawFilledCircle(GuiGraphics guiGraphics, int centerX, int centerY, int radius, float r, float g, float b, float a) {
        int segments = 64;
        float angleStep = 360f / segments;

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();

        bufferBuilder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < segments; i++) {
            float angle1 = (float) Math.toRadians(i * angleStep);
            float angle2 = (float) Math.toRadians((i + 1) * angleStep);

            bufferBuilder.vertex(centerX, centerY, 0).color(r, g, b, a).endVertex();

            float x1 = centerX + radius * (float) Math.cos(angle1);
            float y1 = centerY + radius * (float) Math.sin(angle1);
            float x2 = centerX + radius * (float) Math.cos(angle2);
            float y2 = centerY + radius * (float) Math.sin(angle2);

            bufferBuilder.vertex(x1, y1, 0).color(r, g, b, a).endVertex();
            bufferBuilder.vertex(x2, y2, 0).color(r, g, b, a).endVertex();
        }

        tesselator.end();
    }

    private void drawSegment(GuiGraphics guiGraphics, int centerX, int centerY, float startAngle, float endAngle, boolean isHovered) {
        int segments = 32;
        float angleStep = (endAngle - startAngle) / segments;

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();

        bufferBuilder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        float r = 0.3f;
        float g = 0.3f;
        float b = 0.3f;
        float a = 0.6f;

        for (int i = 0; i < segments; i++) {
            float angle1 = (float) Math.toRadians(startAngle + i * angleStep);
            float angle2 = (float) Math.toRadians(startAngle + (i + 1) * angleStep);

            float x1Inner = centerX + INNER_RADIUS * (float) Math.cos(angle1);
            float y1Inner = centerY + INNER_RADIUS * (float) Math.sin(angle1);
            float x2Inner = centerX + INNER_RADIUS * (float) Math.cos(angle2);
            float y2Inner = centerY + INNER_RADIUS * (float) Math.sin(angle2);

            float x1Outer = centerX + OUTER_RADIUS * (float) Math.cos(angle1);
            float y1Outer = centerY + OUTER_RADIUS * (float) Math.sin(angle1);
            float x2Outer = centerX + OUTER_RADIUS * (float) Math.cos(angle2);
            float y2Outer = centerY + OUTER_RADIUS * (float) Math.sin(angle2);

            bufferBuilder.vertex(x1Inner, y1Inner, 0).color(r, g, b, a).endVertex();
            bufferBuilder.vertex(x1Outer, y1Outer, 0).color(r, g, b, a).endVertex();
            bufferBuilder.vertex(x2Outer, y2Outer, 0).color(r, g, b, a).endVertex();

            bufferBuilder.vertex(x1Inner, y1Inner, 0).color(r, g, b, a).endVertex();
            bufferBuilder.vertex(x2Outer, y2Outer, 0).color(r, g, b, a).endVertex();
            bufferBuilder.vertex(x2Inner, y2Inner, 0).color(r, g, b, a).endVertex();
        }

        tesselator.end();

        // Draw thicker outlines
        RenderSystem.lineWidth(72.0f);

        bufferBuilder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        float startRad = (float) Math.toRadians(startAngle);
        float xInnerStart = centerX + INNER_RADIUS * (float) Math.cos(startRad);
        float yInnerStart = centerY + INNER_RADIUS * (float) Math.sin(startRad);
        float xOuterStart = centerX + OUTER_RADIUS * (float) Math.cos(startRad);
        float yOuterStart = centerY + OUTER_RADIUS * (float) Math.sin(startRad);

        bufferBuilder.vertex(xInnerStart, yInnerStart, 0).color(0.0f, 0.0f, 0.0f, 1.0f).endVertex(); // Pure black
        bufferBuilder.vertex(xOuterStart, yOuterStart, 0).color(0.0f, 0.0f, 0.0f, 1.0f).endVertex(); // Pure black

        tesselator.end();

        RenderSystem.lineWidth(1.0f);
    }

    private void drawCircle(GuiGraphics guiGraphics, int centerX, int centerY, int radius, float r, float g, float b, float a) {
        int segments = 64;
        int thickness = 15; // Thick ring

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();

        bufferBuilder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < segments; i++) {
            float angle1 = (float) Math.toRadians(i * (360.0f / segments));
            float angle2 = (float) Math.toRadians((i + 1) * (360.0f / segments));

            // Inner ring
            float x1Inner = centerX + (radius - thickness) * (float) Math.cos(angle1);
            float y1Inner = centerY + (radius - thickness) * (float) Math.sin(angle1);
            float x2Inner = centerX + (radius - thickness) * (float) Math.cos(angle2);
            float y2Inner = centerY + (radius - thickness) * (float) Math.sin(angle2);

            // Outer ring
            float x1Outer = centerX + (radius + thickness) * (float) Math.cos(angle1);
            float y1Outer = centerY + (radius + thickness) * (float) Math.sin(angle1);
            float x2Outer = centerX + (radius + thickness) * (float) Math.cos(angle2);
            float y2Outer = centerY + (radius + thickness) * (float) Math.sin(angle2);

            // Two triangles to form the thick ring segment
            bufferBuilder.vertex(x1Inner, y1Inner, 0).color(r, g, b, a).endVertex();
            bufferBuilder.vertex(x1Outer, y1Outer, 0).color(r, g, b, a).endVertex();
            bufferBuilder.vertex(x2Outer, y2Outer, 0).color(r, g, b, a).endVertex();

            bufferBuilder.vertex(x1Inner, y1Inner, 0).color(r, g, b, a).endVertex();
            bufferBuilder.vertex(x2Outer, y2Outer, 0).color(r, g, b, a).endVertex();
            bufferBuilder.vertex(x2Inner, y2Inner, 0).color(r, g, b, a).endVertex();
        }

        tesselator.end();
    }

    private float getDistanceFromCenter(int x, int y, int centerX, int centerY) {
        float dx = x - centerX;
        float dy = y - centerY;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public boolean isMouseOverWheel() {
        if (!isActive) return false;

        double mouseX = minecraft.mouseHandler.xpos() * minecraft.getWindow().getGuiScaledWidth() / minecraft.getWindow().getScreenWidth();
        double mouseY = minecraft.mouseHandler.ypos() * minecraft.getWindow().getGuiScaledHeight() / minecraft.getWindow().getScreenHeight();

        int centerX = minecraft.getWindow().getGuiScaledWidth() / 2;
        int centerY = minecraft.getWindow().getGuiScaledHeight() / 2;

        float distance = getDistanceFromCenter((int)mouseX, (int)mouseY, centerX, centerY);
        return distance >= INNER_RADIUS && distance <= OUTER_RADIUS;
    }

    private static class MoveSegment {
        final int index;
        final AbstractMoveset.MoveConfiguration config;

        MoveSegment(int index, AbstractMoveset.MoveConfiguration config) {
            this.index = index;
            this.config = config;
        }
    }
}