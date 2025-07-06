package com.xirc.nichirin.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.data.BreathingStyleHelper;
import com.xirc.nichirin.common.util.enums.MoveClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Circular attack wheel GUI for selecting moves
 */
public class AttackWheelGui extends Screen {

    private static final int WHEEL_RADIUS = 100;
    private static final int INNER_RADIUS = 30;
    private static final int ICON_SIZE = 32;

    // Colors
    private static final int BACKGROUND_COLOR = 0x80000000;
    private static final int SEGMENT_COLOR = 0x60202020;
    private static final int HOVER_COLOR = 0x8055FFFF;
    private static final int SELECTED_COLOR = 0xA055FFFF;
    private static final int BORDER_COLOR = 0xFF303030;

    private final List<WheelSegment> segments = new ArrayList<>();
    private int selectedSegment = -1;
    private AbstractMoveset currentMoveset;

    public AttackWheelGui() {
        super(Component.empty());
    }

    @Override
    protected void init() {
        super.init();

        // Get player's current moveset
        if (minecraft != null && minecraft.player != null) {
            currentMoveset = BreathingStyleHelper.getMoveset(minecraft.player);

            // Debug logging
            String movesetId = BreathingStyleHelper.getMovesetId(minecraft.player);
            System.out.println("DEBUG: Attack Wheel - Moveset ID: " + movesetId);
            System.out.println("DEBUG: Attack Wheel - Moveset object: " + currentMoveset);

            if (currentMoveset != null) {
                buildWheel();
                System.out.println("DEBUG: Built wheel with " + segments.size() + " segments");
            }
        }
    }

    /**
     * Builds the wheel segments based on available moves
     */
    private void buildWheel() {
        segments.clear();

        Map<MoveClass, AbstractMoveset.MoveConfiguration> moves = currentMoveset.getAllMoves();
        if (moves.isEmpty()) return;

        int moveCount = moves.size();
        float anglePerSegment = 360.0f / moveCount;
        float currentAngle = -90; // Start at top

        int index = 0;
        for (Map.Entry<MoveClass, AbstractMoveset.MoveConfiguration> entry : moves.entrySet()) {
            WheelSegment segment = new WheelSegment(
                    index,
                    entry.getKey(),
                    entry.getValue(),
                    currentAngle,
                    currentAngle + anglePerSegment
            );
            segments.add(segment);

            currentAngle += anglePerSegment;
            index++;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (currentMoveset == null || segments.isEmpty()) {
            // No moveset or no moves
            graphics.drawCenteredString(this.font, "No breathing style selected",
                    this.width / 2, this.height / 2, 0xFF5555);
            return;
        }

        // Draw semi-transparent background
        graphics.fill(0, 0, this.width, this.height, BACKGROUND_COLOR);

        // Calculate center
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Update selected segment based on mouse position
        updateSelection(mouseX, mouseY, centerX, centerY);

        // Draw wheel segments
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(centerX, centerY, 0);

        for (WheelSegment segment : segments) {
            drawSegment(graphics, segment, segment.index == selectedSegment);
        }

        // Draw center circle
        drawCircle(graphics, 0, 0, INNER_RADIUS, 32, BORDER_COLOR, true);

        poseStack.popPose();

        // Draw move info for selected segment
        if (selectedSegment >= 0 && selectedSegment < segments.size()) {
            drawMoveInfo(graphics, segments.get(selectedSegment), centerX, centerY);
        }

        // Draw instructions
        Component instructions = Component.literal("Hold R and move mouse to select • Release to cast");
        graphics.drawCenteredString(this.font, instructions,
                centerX, this.height - 30, 0xAAAAAA);
    }

    /**
     * Updates which segment is selected based on mouse position
     */
    private void updateSelection(int mouseX, int mouseY, int centerX, int centerY) {
        float dx = mouseX - centerX;
        float dy = mouseY - centerY;
        float distance = Mth.sqrt(dx * dx + dy * dy);

        // Check if mouse is within wheel bounds
        if (distance < INNER_RADIUS || distance > WHEEL_RADIUS) {
            selectedSegment = -1;
            return;
        }

        // Calculate angle (0 = right, 90 = down, 180 = left, 270 = up)
        float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
        if (angle < 0) angle += 360;

        // Find which segment contains this angle
        for (WheelSegment segment : segments) {
            if (segment.containsAngle(angle)) {
                selectedSegment = segment.index;
                break;
            }
        }
    }

    /**
     * Draws a wheel segment
     */
    private void drawSegment(GuiGraphics graphics, WheelSegment segment, boolean isSelected) {
        Matrix4f matrix = graphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        // Draw segment fill
        int color = isSelected ? SELECTED_COLOR : SEGMENT_COLOR;
        if (!isSelected && segment.index == selectedSegment) {
            color = HOVER_COLOR;
        }

        buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

        // Center point
        buffer.vertex(matrix, 0, 0, 0)
                .color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF)
                .endVertex();

        // Draw arc
        int steps = 32;
        float startRad = (float) Math.toRadians(segment.startAngle);
        float endRad = (float) Math.toRadians(segment.endAngle);
        float stepAngle = (endRad - startRad) / steps;

        for (int i = 0; i <= steps; i++) {
            float angle = startRad + (stepAngle * i);
            float x = Mth.cos(angle) * WHEEL_RADIUS;
            float y = Mth.sin(angle) * WHEEL_RADIUS;

            buffer.vertex(matrix, x, y, 0)
                    .color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF)
                    .endVertex();
        }

        tesselator.end();

        // Draw segment border
        drawArc(graphics, 0, 0, WHEEL_RADIUS, segment.startAngle, segment.endAngle, BORDER_COLOR);
        drawLine(graphics, 0, 0,
                Mth.cos((float)Math.toRadians(segment.startAngle)) * WHEEL_RADIUS,
                Mth.sin((float)Math.toRadians(segment.startAngle)) * WHEEL_RADIUS,
                BORDER_COLOR);
        drawLine(graphics, 0, 0,
                Mth.cos((float)Math.toRadians(segment.endAngle)) * WHEEL_RADIUS,
                Mth.sin((float)Math.toRadians(segment.endAngle)) * WHEEL_RADIUS,
                BORDER_COLOR);

        // Draw icon
        float midAngle = (segment.startAngle + segment.endAngle) / 2;
        float iconDist = (WHEEL_RADIUS + INNER_RADIUS) / 2;
        int iconX = (int)(Mth.cos((float)Math.toRadians(midAngle)) * iconDist);
        int iconY = (int)(Mth.sin((float)Math.toRadians(midAngle)) * iconDist);

        // Draw icon placeholder (replace with actual icon rendering)
        graphics.fill(iconX - ICON_SIZE/2, iconY - ICON_SIZE/2,
                iconX + ICON_SIZE/2, iconY + ICON_SIZE/2,
                0xFF404040);

        RenderSystem.disableBlend();
    }

    /**
     * Draws move information for the selected segment
     */
    private void drawMoveInfo(GuiGraphics graphics, WheelSegment segment, int centerX, int centerY) {
        // Move name
        String moveName = formatMoveName(segment.moveClass.name());
        graphics.drawCenteredString(this.font, moveName, centerX, centerY + WHEEL_RADIUS + 20, 0xFFFFFF);

        // Damage info
        if (segment.config != null) {
            String damage = String.format("Damage: %.1f", segment.config.getDamage());
            graphics.drawCenteredString(this.font, damage, centerX, centerY + WHEEL_RADIUS + 35, 0xFF5555);

            String cooldown = String.format("Cooldown: %d", segment.config.getCooldown());
            graphics.drawCenteredString(this.font, cooldown, centerX, centerY + WHEEL_RADIUS + 50, 0x5555FF);
        }
    }

    /**
     * Helper to draw a circle
     */
    private void drawCircle(GuiGraphics graphics, int x, int y, int radius, int segments, int color, boolean filled) {
        // Implementation for drawing circles
        // This is simplified - you might want a more sophisticated implementation
        for (int i = 0; i < segments; i++) {
            float angle1 = (float)(i * 2 * Math.PI / segments);
            float angle2 = (float)((i + 1) * 2 * Math.PI / segments);

            int x1 = x + (int)(Math.cos(angle1) * radius);
            int y1 = y + (int)(Math.sin(angle1) * radius);
            int x2 = x + (int)(Math.cos(angle2) * radius);
            int y2 = y + (int)(Math.sin(angle2) * radius);

            drawLine(graphics, x1, y1, x2, y2, color);
        }
    }

    /**
     * Helper to draw an arc
     */
    private void drawArc(GuiGraphics graphics, int x, int y, int radius, float startAngle, float endAngle, int color) {
        int steps = 32;
        float startRad = (float) Math.toRadians(startAngle);
        float endRad = (float) Math.toRadians(endAngle);
        float stepAngle = (endRad - startRad) / steps;

        for (int i = 0; i < steps; i++) {
            float angle1 = startRad + (stepAngle * i);
            float angle2 = startRad + (stepAngle * (i + 1));

            int x1 = x + (int)(Math.cos(angle1) * radius);
            int y1 = y + (int)(Math.sin(angle1) * radius);
            int x2 = x + (int)(Math.cos(angle2) * radius);
            int y2 = y + (int)(Math.sin(angle2) * radius);

            drawLine(graphics, x1, y1, x2, y2, color);
        }
    }

    /**
     * Helper to draw a line
     */
    private void drawLine(GuiGraphics graphics, float x1, float y1, float x2, float y2, int color) {
        // Simple line drawing - you might want to use a more sophisticated method
        graphics.fill((int)x1, (int)y1, (int)x2 + 1, (int)y2 + 1, color);
    }

    /**
     * Formats move name for display
     */
    private String formatMoveName(String name) {
        return name.replace("_", " ").replace("SPECIAL", "Form");
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    /**
     * Gets the currently selected move
     */
    public MoveClass getSelectedMove() {
        if (selectedSegment >= 0 && selectedSegment < segments.size()) {
            return segments.get(selectedSegment).moveClass;
        }
        return null;
    }

    /**
     * Represents a segment of the wheel
     */
    private static class WheelSegment {
        final int index;
        final MoveClass moveClass;
        final AbstractMoveset.MoveConfiguration config;
        final float startAngle;
        final float endAngle;

        WheelSegment(int index, MoveClass moveClass, AbstractMoveset.MoveConfiguration config,
                     float startAngle, float endAngle) {
            this.index = index;
            this.moveClass = moveClass;
            this.config = config;
            this.startAngle = startAngle;
            this.endAngle = endAngle;
        }

        boolean containsAngle(float angle) {
            // Normalize angles
            float start = startAngle;
            float end = endAngle;

            if (start < 0) start += 360;
            if (end < 0) end += 360;

            if (start > end) {
                // Wraps around 0
                return angle >= start || angle <= end;
            } else {
                return angle >= start && angle <= end;
            }
        }
    }
}