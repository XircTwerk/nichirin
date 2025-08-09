package com.xirc.nichirin.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.data.BreathingStyleHelper;
import com.xirc.nichirin.registry.MovesetRegistry;
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
 * FIXED SCALE: Always renders at GUI scale 2 regardless of user's GUI scale setting
 * FIXED: Square center for better icon display, reliable click detection
 * FIXED: Connected divider lines and proper layering (lines -> square/outline -> icon)
 */
public class AttackWheelOverlay {

    private static final int OUTER_RADIUS = 150;
    private static final int INNER_RADIUS = 50;
    private static final int ICON_SIZE = 72;
    private static final int CENTER_SQUARE_SIZE = 80; // Size of center square

    // Fixed scale constant
    private static final double FIXED_GUI_SCALE = 2.0;

    private final List<MoveSegment> segments = new ArrayList<>();
    private boolean isActive = false;
    private final Minecraft minecraft;
    private int currentlyHoveredMove = -1;

    // Scaled dimensions
    private int scaledWidth;
    private int scaledHeight;
    private int scaledCenterX;
    private int scaledCenterY;

    public AttackWheelOverlay() {
        this.minecraft = Minecraft.getInstance();
    }

    public void activate() {
        isActive = true;
        rebuildWheel();
        currentlyHoveredMove = -1;
        calculateScaledDimensions();
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

    /**
     * Calculate dimensions for fixed GUI scale of 2
     */
    private void calculateScaledDimensions() {
        // Get the window's framebuffer dimensions
        int framebufferWidth = minecraft.getWindow().getWidth();
        int framebufferHeight = minecraft.getWindow().getHeight();

        // Calculate what the dimensions would be at GUI scale 2
        this.scaledWidth = (int) (framebufferWidth / FIXED_GUI_SCALE);
        this.scaledHeight = (int) (framebufferHeight / FIXED_GUI_SCALE);

        // Calculate center points
        this.scaledCenterX = scaledWidth / 2;
        this.scaledCenterY = scaledHeight / 2;
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

        // Calculate current scale factor and scaling ratio
        double currentScale = minecraft.getWindow().getGuiScale();
        double scaleRatio = FIXED_GUI_SCALE / currentScale;

        // Apply our fixed scaling
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale((float) scaleRatio, (float) scaleRatio, 1.0f);

        // Recalculate scaled dimensions in case of window resize
        calculateScaledDimensions();

        // Update hovered move FIRST (using scaled coordinates)
        updateHoveredMove(scaledCenterX, scaledCenterY, scaleRatio);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Draw background circles
        drawFilledCircle(guiGraphics, scaledCenterX, scaledCenterY, OUTER_RADIUS, 0.3f, 0.3f, 0.3f, 0.5f);

        // Draw center square instead of circle
        drawCenterSquare(guiGraphics, scaledCenterX, scaledCenterY, CENTER_SQUARE_SIZE, 0.2f, 0.2f, 0.2f, 0.7f);

        if (segments.isEmpty()) {
            // Draw placeholder
            Font font = minecraft.font;
            guiGraphics.drawCenteredString(font, "No moves available", scaledCenterX, scaledCenterY - 4, 0xFFFFFF);
        } else {
            // Draw segments
            float segmentAngle = 360f / segments.size();

            for (int i = 0; i < segments.size(); i++) {
                // Start from top (-90°) and go clockwise
                float startAngle = -90f + (i * segmentAngle);
                float endAngle = startAngle + segmentAngle;
                boolean isHovered = (i == currentlyHoveredMove);

                drawSegment(guiGraphics, scaledCenterX, scaledCenterY, startAngle, endAngle, isHovered);

                // Draw divider line
                drawDividerLine(guiGraphics, scaledCenterX, scaledCenterY, startAngle);

                // Draw move name
                float midAngle = startAngle + segmentAngle / 2;
                int textRadius = (INNER_RADIUS + OUTER_RADIUS) / 2;
                int textX = scaledCenterX + (int)(textRadius * Math.cos(Math.toRadians(midAngle)));
                int textY = scaledCenterY + (int)(textRadius * Math.sin(Math.toRadians(midAngle)));

                String moveName = segments.get(i).config.getDisplayName();
                int textColor = isHovered ? 0x55FF55 : 0xFFFFFF;
                Font font = minecraft.font;
                guiGraphics.drawCenteredString(font, moveName, textX, textY - 4, textColor);
            }

            // Draw center icon
            if (currentlyHoveredMove >= 0 && currentlyHoveredMove < segments.size()) {
                MoveSegment selectedSegment = segments.get(currentlyHoveredMove);
                drawCenterIcon(guiGraphics, scaledCenterX, scaledCenterY, selectedSegment);
            }
        }

        // Draw border square instead of inner circle
        drawSquareBorder(guiGraphics, scaledCenterX, scaledCenterY, CENTER_SQUARE_SIZE, 0.1f, 0.1f, 0.1f, 1.0f);
        drawCircle(guiGraphics, scaledCenterX, scaledCenterY, OUTER_RADIUS, 0.1f, 0.1f, 0.1f, 1.0f);

        RenderSystem.disableBlend();

        // Restore pose stack
        guiGraphics.pose().popPose();
    }

    private void updateHoveredMove(int centerX, int centerY, double scaleRatio) {
        if (segments.isEmpty()) {
            currentlyHoveredMove = -1;
            return;
        }

        // Get raw mouse position and adjust for our scaling
        double rawMouseX = minecraft.mouseHandler.xpos() * minecraft.getWindow().getGuiScaledWidth() / minecraft.getWindow().getScreenWidth();
        double rawMouseY = minecraft.mouseHandler.ypos() * minecraft.getWindow().getGuiScaledHeight() / minecraft.getWindow().getScreenHeight();

        // Adjust mouse coordinates for our fixed scaling
        double mouseX = rawMouseX / scaleRatio;
        double mouseY = rawMouseY / scaleRatio;

        // Calculate distance from center
        double deltaX = mouseX - centerX;
        double deltaY = mouseY - centerY;
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

        // Don't highlight anything if mouse is outside the wheel entirely
        if (distance > OUTER_RADIUS) {
            currentlyHoveredMove = -1;
            return;
        }

        // Don't highlight anything if mouse is in the center area (keep center unclickable)
        if (distance < INNER_RADIUS) {
            currentlyHoveredMove = -1;
            return;
        }

        // Mouse is in the selectable ring area - calculate which segment
        // Calculate angle from center, starting from top (-90°) going clockwise
        double angle = Math.toDegrees(Math.atan2(deltaY, deltaX));

        // Convert to our coordinate system: top = 0°, clockwise = positive
        double adjustedAngle = angle + 90; // Shift so top = 0°
        if (adjustedAngle < 0) adjustedAngle += 360; // Normalize to 0-360
        if (adjustedAngle >= 360) adjustedAngle -= 360;

        // Calculate segment with more precise floating point math
        float segmentAngle = 360f / segments.size();
        int segmentIndex = (int) Math.floor(adjustedAngle / segmentAngle);

        // Ensure valid range with proper bounds checking
        segmentIndex = Math.max(0, Math.min(segmentIndex, segments.size() - 1));

        currentlyHoveredMove = segmentIndex;
    }

    private void drawDividerLine(GuiGraphics guiGraphics, int centerX, int centerY, float angle) {
        float angleRad = (float) Math.toRadians(angle);

        // Calculate where the line intersects the square's edge
        int halfSquare = CENTER_SQUARE_SIZE / 2;

        // For a square, we need to find which edge the line hits based on the angle
        double absX = Math.abs(Math.cos(angleRad));
        double absY = Math.abs(Math.sin(angleRad));

        int xStart, yStart;
        if (absX > absY) {
            // Line hits left or right edge of square
            xStart = centerX + (Math.cos(angleRad) > 0 ? halfSquare : -halfSquare);
            yStart = centerY + (int)(halfSquare * Math.tan(angleRad) * (Math.cos(angleRad) > 0 ? 1 : -1));
        } else {
            // Line hits top or bottom edge of square
            yStart = centerY + (Math.sin(angleRad) > 0 ? halfSquare : -halfSquare);
            xStart = centerX + (int)(halfSquare / Math.tan(angleRad) * (Math.sin(angleRad) > 0 ? 1 : -1));
        }

        // End point at outer circle
        int xEnd = centerX + (int)(OUTER_RADIUS * Math.cos(angleRad));
        int yEnd = centerY + (int)(OUTER_RADIUS * Math.sin(angleRad));

        // Draw the line
        int dividerColor = 0xFF1A1A1A; // Dark gray
        int steps = Math.max(Math.abs(xEnd - xStart), Math.abs(yEnd - yStart));

        for (int i = 0; i <= steps; i++) {
            float t = (float) i / steps;
            int x = (int) (xStart + t * (xEnd - xStart));
            int y = (int) (yStart + t * (yEnd - yStart));

            guiGraphics.fill(x, y, x + 1, y + 1, dividerColor);
        }
    }

    /**
     * Draw a filled square in the center
     */
    private void drawCenterSquare(GuiGraphics guiGraphics, int centerX, int centerY, int size, float r, float g, float b, float a) {
        int halfSize = size / 2;
        int x1 = centerX - halfSize;
        int y1 = centerY - halfSize;
        int x2 = centerX + halfSize;
        int y2 = centerY + halfSize;

        int red = (int)(r * 255);
        int green = (int)(g * 255);
        int blue = (int)(b * 255);
        int alpha = (int)(a * 255);
        int color = (alpha << 24) | (red << 16) | (green << 8) | blue;

        guiGraphics.fill(x1, y1, x2, y2, color);
    }

    /**
     * Draw square border with thicker outline
     */
    private void drawSquareBorder(GuiGraphics guiGraphics, int centerX, int centerY, int size, float r, float g, float b, float a) {
        int halfSize = size / 2;
        int x1 = centerX - halfSize;
        int y1 = centerY - halfSize;
        int x2 = centerX + halfSize;
        int y2 = centerY + halfSize;

        int red = (int)(r * 255);
        int green = (int)(g * 255);
        int blue = (int)(b * 255);
        int alpha = (int)(a * 255);
        int color = (alpha << 24) | (red << 16) | (green << 8) | blue;

        int borderThickness = 2; // Make outline thicker and more visible

        // Draw thicker border lines
        guiGraphics.fill(x1, y1, x2, y1 + borderThickness, color); // Top
        guiGraphics.fill(x1, y2 - borderThickness, x2, y2, color); // Bottom
        guiGraphics.fill(x1, y1, x1 + borderThickness, y2, color); // Left
        guiGraphics.fill(x2 - borderThickness, y1, x2, y2, color); // Right
    }

    private void drawCenterIcon(GuiGraphics guiGraphics, int centerX, int centerY, MoveSegment segment) {
        ResourceLocation iconLocation = null;

        // Get the breathing style and move name for the icon using MoveIcon system only
        if (minecraft.player != null && segment.config != null) {
            String movesetId = BreathingStyleHelper.getMovesetId(minecraft.player);
            if (movesetId != null) {
                String moveName = segment.config.getMoveId();
                if (moveName != null) {
                    iconLocation = MoveIcon.getIcon(movesetId, moveName);
                }
            }
        }

        // Draw the icon or fallback
        if (iconLocation != null && textureExists(iconLocation)) {
            int iconX = centerX - ICON_SIZE / 2;
            int iconY = centerY - ICON_SIZE / 2;

            RenderSystem.setShaderTexture(0, iconLocation);
            guiGraphics.blit(iconLocation, iconX, iconY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        } else {
            drawMoveNameFallback(guiGraphics, centerX, centerY, segment);
        }
    }

    private void drawMoveNameFallback(GuiGraphics guiGraphics, int centerX, int centerY, MoveSegment segment) {
        Font font = minecraft.font;
        String displayName = segment.config.getDisplayName();
        if (displayName != null && displayName.length() > 10) {
            displayName = displayName.substring(0, 10) + "...";
        }
        guiGraphics.drawCenteredString(font, displayName != null ? displayName : "?", centerX, centerY - 4, 0xFFFFFF);
    }

    /**
     * Check if a texture exists
     */
    private boolean textureExists(ResourceLocation location) {
        try {
            return minecraft.getResourceManager().getResource(location).isPresent();
        } catch (Exception e) {
            return false;
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