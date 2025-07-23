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

    private static final int OUTER_RADIUS = 150; // 120 * 1.25 = 150
    private static final int INNER_RADIUS = 50;  // 40 * 1.25 = 50
    private static final int ICON_SIZE = 40;     // 32 * 1.25 = 40

    private final List<MoveSegment> segments = new ArrayList<>();
    private float segmentAngle;
    private boolean isActive = false;
    private final Minecraft minecraft;
    private int currentlyHoveredMove = -1; // Store the currently hovered move

    public AttackWheelOverlay() {
        this.minecraft = Minecraft.getInstance();
    }

    public void activate() {
        isActive = true;
        rebuildWheel();
    }

    public int deactivate() {
        isActive = false;
        int moveToReturn = currentlyHoveredMove;
        currentlyHoveredMove = -1; // Reset after storing
        return moveToReturn;
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

        // Get CURRENT mouse handler state
        System.out.println("DEBUG: Mouse handler grabbed state: " + minecraft.mouseHandler.isMouseGrabbed());

        // Get mouse position - try multiple methods
        double rawMouseX = minecraft.mouseHandler.xpos();
        double rawMouseY = minecraft.mouseHandler.ypos();

        // Convert to GUI coordinates
        double mouseX = rawMouseX * minecraft.getWindow().getGuiScaledWidth() / minecraft.getWindow().getScreenWidth();
        double mouseY = rawMouseY * minecraft.getWindow().getGuiScaledHeight() / minecraft.getWindow().getScreenHeight();

        int centerX = minecraft.getWindow().getGuiScaledWidth() / 2;
        int centerY = minecraft.getWindow().getGuiScaledHeight() / 2;

        // Calculate angle from mouse position - FLIP X coordinate to fix left-right inversion
        double deltaX = -(mouseX - centerX); // Negative to flip horizontally
        double deltaY = mouseY - centerY;

        System.out.println("DEBUG: Raw mouse: (" + rawMouseX + ", " + rawMouseY + ")");
        System.out.println("DEBUG: Scaled mouse: (" + mouseX + ", " + mouseY + ")");
        System.out.println("DEBUG: Delta from center: (" + deltaX + ", " + deltaY + ")");

        // Convert to angle in degrees (0-360)
        float rawAngle = (float) Math.toDegrees(Math.atan2(deltaY, deltaX));
        System.out.println("DEBUG: Raw angle from atan2: " + rawAngle);

        // Normalize angle to 0-360 range
        float normalizedAngle = rawAngle;
        if (normalizedAngle < 0) {
            normalizedAngle += 360;
        }
        System.out.println("DEBUG: Normalized angle (0-360): " + normalizedAngle);

        // Convert to wheel coordinates that match rendering
        // Rendering starts at 90° (bottom) and goes clockwise
        // Mouse angle: 0° = right, increases counter-clockwise
        // We need to flip horizontally: wheel_angle = (90 - mouse_angle + 360) % 360
        float wheelAngle = (90 - normalizedAngle + 360) % 360;
        System.out.println("DEBUG: Wheel angle: " + wheelAngle);

        // Calculate which segment this angle falls into
        int rawSegmentIndex = (int) (wheelAngle / segmentAngle);

        // Ensure we don't go out of bounds
        if (rawSegmentIndex >= segments.size()) {
            rawSegmentIndex = segments.size() - 1;
        }

        // Use raw segment index - no flipping needed if we fix the visual rendering
        int segmentIndex = rawSegmentIndex;

        System.out.println("DEBUG: Raw segment index: " + rawSegmentIndex + " -> Segment index: " + segmentIndex + " out of " + segments.size());
        System.out.println("DEBUG: Final move index: " + (segmentIndex >= 0 && segmentIndex < segments.size() ? segments.get(segmentIndex).index : "INVALID"));

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

        // Dark gray background
        drawFilledCircle(guiGraphics, centerX, centerY, OUTER_RADIUS, 0.3f, 0.3f, 0.3f, 0.5f);
        drawFilledCircle(guiGraphics, centerX, centerY, INNER_RADIUS, 0.2f, 0.2f, 0.2f, 0.7f);

        if (segments.isEmpty()) {
            for (int i = 0; i < 4; i++) {
                float startAngle = i * 90f;
                float endAngle = (i + 1) * 90f;
                drawSegment(guiGraphics, centerX, centerY, startAngle, endAngle, false);
            }
            Font font = minecraft.font;
            guiGraphics.drawCenteredString(font, "No moves available", centerX, centerY - 4, 0xFFFFFF);

            drawCircle(guiGraphics, centerX, centerY, INNER_RADIUS, 0.1f, 0.1f, 0.1f, 1.0f);
            drawCircle(guiGraphics, centerX, centerY, OUTER_RADIUS, 0.1f, 0.1f, 0.1f, 1.0f);
        } else {
            // Calculate hovered segment using same logic as getSelectedMove()
            double deltaX = -(mouseX - centerX); // Negative to flip horizontally
            double deltaY = mouseY - centerY;
            float angle = (float) Math.toDegrees(Math.atan2(deltaY, deltaX));
            if (angle < 0) angle += 360;
            angle = (90 - angle + 360) % 360; // Convert to wheel coordinates

            int hoveredSegment = -1;
            float distance = getDistanceFromCenter((int)mouseX, (int)mouseY, centerX, centerY);
            if (distance <= OUTER_RADIUS && distance >= INNER_RADIUS) {
                int rawHoveredSegment = (int) (angle / segmentAngle);
                if (rawHoveredSegment >= segments.size()) rawHoveredSegment = segments.size() - 1;

                // Use raw hovered segment - no flipping
                hoveredSegment = rawHoveredSegment;

                // Update the currently hovered move
                if (hoveredSegment >= 0 && hoveredSegment < segments.size()) {
                    currentlyHoveredMove = segments.get(hoveredSegment).index;
                } else {
                    currentlyHoveredMove = -1;
                }

                // Debug the current selection every few frames
                if (minecraft.level != null && minecraft.level.getGameTime() % 20 == 0) { // Every second
                    System.out.println("DEBUG: RENDER - Mouse at (" + mouseX + ", " + mouseY + "), angle: " + angle + ", raw segment: " + rawHoveredSegment + ", segment: " + hoveredSegment + ", move: " + currentlyHoveredMove);
                    System.out.println("DEBUG: RENDER - Should match getSelectedMove() result: " + getSelectedMove());
                }
            } else {
                currentlyHoveredMove = -1;
            }

            // Draw segments - start from 90 degrees (bottom) instead of -90 (top) to flip visually
            for (int i = 0; i < segments.size(); i++) {
                float startAngle = 90f + (i * segmentAngle);
                float endAngle = 90f + ((i + 1) * segmentAngle);
                boolean isHovered = (i == hoveredSegment);
                drawSegment(guiGraphics, centerX, centerY, startAngle, endAngle, isHovered);
            }

            // Draw move names
            for (int i = 0; i < segments.size(); i++) {
                float startAngle = 90f + (i * segmentAngle);
                boolean isHovered = (i == hoveredSegment);

                float midAngle = startAngle + segmentAngle / 2;
                int textX = centerX + (int)((INNER_RADIUS + OUTER_RADIUS) / 2 * Math.cos(Math.toRadians(midAngle)));
                int textY = centerY + (int)((INNER_RADIUS + OUTER_RADIUS) / 2 * Math.sin(Math.toRadians(midAngle)));

                String moveName = segments.get(i).config.getDisplayName();
                int textColor = isHovered ? 0x55FF55 : 0xFFFFFF;
                Font font = minecraft.font;
                guiGraphics.drawCenteredString(font, moveName, textX, textY - 4, textColor);
            }

            // Draw center icon
            if (hoveredSegment >= 0 && hoveredSegment < segments.size()) {
                // hoveredSegment is already the correct index - don't flip it again!
                MoveSegment selectedSegment = segments.get(hoveredSegment);
                drawCenterIcon(guiGraphics, centerX, centerY, selectedSegment);
            }

            drawCircle(guiGraphics, centerX, centerY, INNER_RADIUS, 0.1f, 0.1f, 0.1f, 1.0f);
            drawCircle(guiGraphics, centerX, centerY, OUTER_RADIUS, 0.1f, 0.1f, 0.1f, 1.0f);
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

        float r = isHovered ? 0.4f : 0.3f;
        float g = isHovered ? 0.4f : 0.3f;
        float b = isHovered ? 0.4f : 0.3f;
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

        // Draw thin divider line
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