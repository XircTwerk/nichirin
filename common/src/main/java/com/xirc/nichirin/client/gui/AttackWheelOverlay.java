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
 * Simplified attack wheel overlay with clean click detection
 */
public class AttackWheelOverlay {

    private static final int OUTER_RADIUS = 150;
    private static final int INNER_RADIUS = 50;
    private static final int ICON_SIZE = 40;

    private final List<MoveSegment> segments = new ArrayList<>();
    private boolean isActive = false;
    private final Minecraft minecraft;
    private int currentlyHoveredMove = -1;

    public AttackWheelOverlay() {
        this.minecraft = Minecraft.getInstance();
    }

    public void activate() {
        isActive = true;
        rebuildWheel();
        currentlyHoveredMove = -1;
    }

    public void deactivate() {
        isActive = false;
        segments.clear();
        currentlyHoveredMove = -1;
    }

    public boolean isActive() {
        return isActive;
    }

    public int getCurrentlyHoveredMove() {
        return currentlyHoveredMove;
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

        // Build segments in order
        for (int i = 0; i < moveset.getMoveCount(); i++) {
            AbstractMoveset.MoveConfiguration config = moveset.getMove(i);
            if (config != null) {
                MoveSegment segment = new MoveSegment(i, config);
                segments.add(segment);
            }
        }
    }

    public void render(GuiGraphics guiGraphics) {
        if (!isActive) return;

        int centerX = minecraft.getWindow().getGuiScaledWidth() / 2;
        int centerY = minecraft.getWindow().getGuiScaledHeight() / 2;

        // Update hovered move FIRST
        updateHoveredMove(centerX, centerY);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Draw background circles
        drawFilledCircle(guiGraphics, centerX, centerY, OUTER_RADIUS, 0.3f, 0.3f, 0.3f, 0.5f);
        drawFilledCircle(guiGraphics, centerX, centerY, INNER_RADIUS, 0.2f, 0.2f, 0.2f, 0.7f);

        if (segments.isEmpty()) {
            // Draw placeholder
            Font font = minecraft.font;
            guiGraphics.drawCenteredString(font, "No moves available", centerX, centerY - 4, 0xFFFFFF);
        } else {
            // Draw segments
            float segmentAngle = 360f / segments.size();

            for (int i = 0; i < segments.size(); i++) {
                // Start from top (-90°) and go clockwise
                float startAngle = -90f + (i * segmentAngle);
                float endAngle = startAngle + segmentAngle;
                boolean isHovered = (i == currentlyHoveredMove);

                drawSegment(guiGraphics, centerX, centerY, startAngle, endAngle, isHovered);

                // Draw move name
                float midAngle = startAngle + segmentAngle / 2;
                int textRadius = (INNER_RADIUS + OUTER_RADIUS) / 2;
                int textX = centerX + (int)(textRadius * Math.cos(Math.toRadians(midAngle)));
                int textY = centerY + (int)(textRadius * Math.sin(Math.toRadians(midAngle)));

                String moveName = segments.get(i).config.getDisplayName();
                int textColor = isHovered ? 0x55FF55 : 0xFFFFFF;
                Font font = minecraft.font;
                guiGraphics.drawCenteredString(font, moveName, textX, textY - 4, textColor);
            }

            // Draw center icon
            if (currentlyHoveredMove >= 0 && currentlyHoveredMove < segments.size()) {
                MoveSegment selectedSegment = segments.get(currentlyHoveredMove);
                drawCenterIcon(guiGraphics, centerX, centerY, selectedSegment);
            }
        }

        // Draw border circles
        drawCircle(guiGraphics, centerX, centerY, INNER_RADIUS, 0.1f, 0.1f, 0.1f, 1.0f);
        drawCircle(guiGraphics, centerX, centerY, OUTER_RADIUS, 0.1f, 0.1f, 0.1f, 1.0f);

        RenderSystem.disableBlend();
    }

    private void updateHoveredMove(int centerX, int centerY) {
        if (segments.isEmpty()) {
            currentlyHoveredMove = -1;
            return;
        }

        // Get mouse position in GUI coordinates
        double mouseX = minecraft.mouseHandler.xpos() * minecraft.getWindow().getGuiScaledWidth() / minecraft.getWindow().getScreenWidth();
        double mouseY = minecraft.mouseHandler.ypos() * minecraft.getWindow().getGuiScaledHeight() / minecraft.getWindow().getScreenHeight();

        // Calculate distance from center
        double deltaX = mouseX - centerX;
        double deltaY = mouseY - centerY;
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

        // ✅ FIXED: Only check if mouse is outside the wheel entirely
        // Remove the INNER_RADIUS check so clicks work anywhere in the segment
        if (distance > OUTER_RADIUS) {
            currentlyHoveredMove = -1;
            return;
        }

        // ✅ ALSO: Allow clicks in the center area (inner circle)
        // This makes the entire pie slice clickable, including the center

        // SIMPLIFIED ANGLE CALCULATION
        // Calculate angle from center, starting from top (-90°) going clockwise
        double angle = Math.toDegrees(Math.atan2(deltaY, deltaX));

        // Convert to our coordinate system: top = 0°, clockwise = positive
        // atan2 gives: right = 0°, counter-clockwise = positive
        // We want: top = 0°, clockwise = positive
        double adjustedAngle = angle + 90; // Shift so top = 0°
        if (adjustedAngle < 0) adjustedAngle += 360; // Normalize to 0-360
        if (adjustedAngle >= 360) adjustedAngle -= 360;

        // Calculate segment
        float segmentAngle = 360f / segments.size();
        int segmentIndex = (int) (adjustedAngle / segmentAngle);

        // Ensure valid range
        if (segmentIndex >= segments.size()) {
            segmentIndex = segments.size() - 1;
        }
        if (segmentIndex < 0) {
            segmentIndex = 0;
        }

        currentlyHoveredMove = segmentIndex;

        // Debug output (less frequent)
        if (minecraft.level != null && minecraft.level.getGameTime() % 10 == 0) {
            System.out.println("DEBUG: Mouse(" + (int)mouseX + "," + (int)mouseY + ") -> " +
                    "Angle:" + (int)angle + "° -> Adjusted:" + (int)adjustedAngle + "° -> " +
                    "Segment:" + segmentIndex + "/" + segments.size());
        }
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

        float r = isHovered ? 0.4f : 0.3f;
        float g = isHovered ? 0.4f : 0.3f;
        float b = isHovered ? 0.6f : 0.3f; // Make hovered segments more blue
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

            // Triangle 1
            bufferBuilder.vertex(x1Inner, y1Inner, 0).color(r, g, b, a).endVertex();
            bufferBuilder.vertex(x1Outer, y1Outer, 0).color(r, g, b, a).endVertex();
            bufferBuilder.vertex(x2Outer, y2Outer, 0).color(r, g, b, a).endVertex();

            // Triangle 2
            bufferBuilder.vertex(x1Inner, y1Inner, 0).color(r, g, b, a).endVertex();
            bufferBuilder.vertex(x2Outer, y2Outer, 0).color(r, g, b, a).endVertex();
            bufferBuilder.vertex(x2Inner, y2Inner, 0).color(r, g, b, a).endVertex();
        }

        tesselator.end();

        // Draw divider line
        drawDividerLine(guiGraphics, centerX, centerY, startAngle);
    }

    private void drawDividerLine(GuiGraphics guiGraphics, int centerX, int centerY, float angle) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        float angleRad = (float) Math.toRadians(angle);
        float perpX = -(float) Math.sin(angleRad) * 0.5f;
        float perpY = (float) Math.cos(angleRad) * 0.5f;

        float xInner = centerX + INNER_RADIUS * (float) Math.cos(angleRad);
        float yInner = centerY + INNER_RADIUS * (float) Math.sin(angleRad);
        float xOuter = centerX + OUTER_RADIUS * (float) Math.cos(angleRad);
        float yOuter = centerY + OUTER_RADIUS * (float) Math.sin(angleRad);

        bufferBuilder.vertex(xInner - perpX, yInner - perpY, 0).color(0.1f, 0.1f, 0.1f, 1.0f).endVertex();
        bufferBuilder.vertex(xInner + perpX, yInner + perpY, 0).color(0.1f, 0.1f, 0.1f, 1.0f).endVertex();
        bufferBuilder.vertex(xOuter + perpX, yOuter + perpY, 0).color(0.1f, 0.1f, 0.1f, 1.0f).endVertex();

        bufferBuilder.vertex(xInner - perpX, yInner - perpY, 0).color(0.1f, 0.1f, 0.1f, 1.0f).endVertex();
        bufferBuilder.vertex(xOuter + perpX, yOuter + perpY, 0).color(0.1f, 0.1f, 0.1f, 1.0f).endVertex();
        bufferBuilder.vertex(xOuter - perpX, yOuter - perpY, 0).color(0.1f, 0.1f, 0.1f, 1.0f).endVertex();

        tesselator.end();
    }

    private void drawCircle(GuiGraphics guiGraphics, int centerX, int centerY, int radius, float r, float g, float b, float a) {
        int segments = 2048;
        int red = (int)(r * 255);
        int green = (int)(g * 255);
        int blue = (int)(b * 255);
        int alpha = (int)(a * 255);
        int color = (alpha << 24) | (red << 16) | (green << 8) | blue;

        for (int i = 0; i < segments; i++) {
            float angle = (float) Math.toRadians(i * (360.0f / segments));
            int x = centerX + (int)(radius * Math.cos(angle));
            int y = centerY + (int)(radius * Math.sin(angle));
            guiGraphics.fill(x, y, x + 1, y + 1, color);
        }
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