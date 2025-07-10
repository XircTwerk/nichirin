package com.xirc.nichirin.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.xirc.nichirin.client.registry.NichirinKeybindRegistry;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.data.BreathingStyleHelper;
import com.xirc.nichirin.common.data.MovesetRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Circular attack wheel GUI for selecting breathing technique moves
 */
public class AttackWheelGui extends Screen {

    private static final int OUTER_RADIUS = 120;
    private static final int INNER_RADIUS = 40;

    private final List<MoveSegment> segments = new ArrayList<>();
    private float segmentAngle;
    private boolean isActive = false;

    public AttackWheelGui() {
        super(Component.literal("Attack Wheel"));
    }

    /**
     * Rebuilds the wheel based on current moveset
     */
    private void rebuildWheel() {
        segments.clear();

        // Check if minecraft is initialized
        if (minecraft == null || minecraft.player == null) {
            System.out.println("DEBUG: Attack Wheel - Minecraft not initialized yet");
            return;
        }

        Player player = minecraft.player;

        // Get player's current breathing style
        String movesetId = BreathingStyleHelper.getMovesetId(player);
        System.out.println("DEBUG: Attack Wheel - Moveset ID: " + movesetId);

        if (movesetId == null || movesetId.isEmpty()) {
            System.out.println("DEBUG: Attack Wheel - No moveset ID found");
            return;
        }

        // Get the moveset
        AbstractMoveset moveset = MovesetRegistry.getMoveset(movesetId);
        System.out.println("DEBUG: Attack Wheel - Moveset object: " + moveset);

        if (moveset == null) {
            System.out.println("DEBUG: Attack Wheel - Moveset not found in registry");
            return;
        }

        buildWheel(moveset);
    }

    /**
     * Builds wheel segments from moveset
     */
    private void buildWheel(AbstractMoveset moveset) {
        segments.clear();

        if (moveset == null) {
            System.out.println("DEBUG: Built wheel with 0 segments (null moveset)");
            return;
        }

        // Create segments for each move
        for (int i = 0; i < moveset.getMoveCount(); i++) {
            AbstractMoveset.MoveConfiguration config = moveset.getMove(i);
            if (config != null) {
                MoveSegment segment = new MoveSegment(i, config);
                segments.add(segment);
            }
        }

        System.out.println("DEBUG: Built wheel with " + segments.size() + " segments");

        if (!segments.isEmpty()) {
            segmentAngle = 360f / segments.size();
        }
    }

    /**
     * Gets the player's current moveset
     */
    private AbstractMoveset getPlayerMoveset() {
        if (minecraft == null || minecraft.player == null) return null;

        String movesetId = BreathingStyleHelper.getMovesetId(minecraft.player);
        if (movesetId == null || movesetId.isEmpty()) return null;

        return MovesetRegistry.getMoveset(movesetId);
    }

    /**
     * Activates the wheel
     */
    public void activate() {
        isActive = true;
        // Don't rebuild here since minecraft might not be initialized yet
        // The init() method will call rebuildWheel() when the screen is properly set up
    }

    /**
     * Deactivates the wheel and returns selected move
     */
    public int deactivate() {
        isActive = false;
        return getSelectedMove();
    }

    /**
     * Gets the currently selected move based on mouse position
     */
    public int getSelectedMove() {
        if (segments.isEmpty() || minecraft == null) return -1;

        int mouseX = (int)(minecraft.mouseHandler.xpos() * minecraft.getWindow().getGuiScaledWidth() / minecraft.getWindow().getScreenWidth());
        int mouseY = (int)(minecraft.mouseHandler.ypos() * minecraft.getWindow().getGuiScaledHeight() / minecraft.getWindow().getScreenHeight());

        int centerX = width / 2;
        int centerY = height / 2;

        float angle = (float) Math.toDegrees(Math.atan2(mouseY - centerY, mouseX - centerX));
        if (angle < 0) angle += 360;

        int segmentIndex = (int)(angle / segmentAngle);
        if (segmentIndex >= 0 && segmentIndex < segments.size()) {
            return segments.get(segmentIndex).index;
        }

        return -1;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!isActive) return;

        int centerX = width / 2;
        int centerY = height / 2;

        // Enable blending for transparency
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Draw the wheel even if there are no moves
        if (segments.isEmpty()) {
            // Draw empty wheel with 8 placeholder segments
            for (int i = 0; i < 8; i++) {
                float startAngle = i * 45f;
                float endAngle = (i + 1) * 45f;
                drawSegment(guiGraphics, centerX, centerY, startAngle, endAngle, false);
            }

            // Draw "No moves available" text
            guiGraphics.drawCenteredString(font, "No moves available", centerX, centerY - 4, 0xAAAAAA);
        } else {
            // Calculate which segment the mouse is hovering over
            float angle = (float) Math.toDegrees(Math.atan2(mouseY - centerY, mouseX - centerX));
            if (angle < 0) angle += 360;

            int hoveredSegment = -1;
            float distance = getDistanceFromCenter(mouseX, mouseY, centerX, centerY);
            if (distance <= OUTER_RADIUS && distance >= INNER_RADIUS) {
                hoveredSegment = (int)(angle / segmentAngle);
                if (hoveredSegment >= segments.size()) hoveredSegment = segments.size() - 1;
            }

            // Draw segments with moves
            for (int i = 0; i < segments.size(); i++) {
                float startAngle = i * segmentAngle;
                float endAngle = (i + 1) * segmentAngle;

                boolean isHovered = (i == hoveredSegment);
                drawSegment(guiGraphics, centerX, centerY, startAngle, endAngle, isHovered);

                // Draw move name
                float midAngle = startAngle + segmentAngle / 2;
                int textX = centerX + (int)((INNER_RADIUS + OUTER_RADIUS) / 2 * Math.cos(Math.toRadians(midAngle)));
                int textY = centerY + (int)((INNER_RADIUS + OUTER_RADIUS) / 2 * Math.sin(Math.toRadians(midAngle)));

                String moveName = segments.get(i).config.getDisplayName();
                // Make hovered text brighter
                int textColor = isHovered ? 0xFFFFFF : 0xCCCCCC;
                guiGraphics.drawCenteredString(font, moveName, textX, textY - 4, textColor);
            }
        }

        RenderSystem.disableBlend();
    }

    /**
     * Draws a segment of the wheel
     */
    private void drawSegment(GuiGraphics guiGraphics, int centerX, int centerY, float startAngle, float endAngle, boolean isHovered) {
        int segments = 32; // Number of triangles to approximate the arc
        float angleStep = (endAngle - startAngle) / segments;

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();

        // First, draw the filled segment
        bufferBuilder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        // Dark gray color scheme - more visible and less transparent
        float r = isHovered ? 0.7f : 0.4f;
        float g = isHovered ? 0.7f : 0.4f;
        float b = isHovered ? 0.8f : 0.4f; // Slight blue tint when hovered
        float a = 1.0f; // Fully opaque

        for (int i = 0; i < segments; i++) {
            float angle1 = (float) Math.toRadians(startAngle + i * angleStep);
            float angle2 = (float) Math.toRadians(startAngle + (i + 1) * angleStep);

            // Inner arc
            float x1Inner = centerX + INNER_RADIUS * (float) Math.cos(angle1);
            float y1Inner = centerY + INNER_RADIUS * (float) Math.sin(angle1);
            float x2Inner = centerX + INNER_RADIUS * (float) Math.cos(angle2);
            float y2Inner = centerY + INNER_RADIUS * (float) Math.sin(angle2);

            // Outer arc
            float x1Outer = centerX + OUTER_RADIUS * (float) Math.cos(angle1);
            float y1Outer = centerY + OUTER_RADIUS * (float) Math.sin(angle1);
            float x2Outer = centerX + OUTER_RADIUS * (float) Math.cos(angle2);
            float y2Outer = centerY + OUTER_RADIUS * (float) Math.sin(angle2);

            // First triangle
            bufferBuilder.vertex(x1Inner, y1Inner, 0).color(r, g, b, a).endVertex();
            bufferBuilder.vertex(x1Outer, y1Outer, 0).color(r, g, b, a).endVertex();
            bufferBuilder.vertex(x2Outer, y2Outer, 0).color(r, g, b, a).endVertex();

            // Second triangle
            bufferBuilder.vertex(x1Inner, y1Inner, 0).color(r, g, b, a).endVertex();
            bufferBuilder.vertex(x2Outer, y2Outer, 0).color(r, g, b, a).endVertex();
            bufferBuilder.vertex(x2Inner, y2Inner, 0).color(r, g, b, a).endVertex();
        }

        tesselator.end();

        // Draw MUCH thicker outlines
        RenderSystem.lineWidth(12.0f); // 4x thickness

        // Draw the segment divider lines
        bufferBuilder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        float startRad = (float) Math.toRadians(startAngle);

        // Only draw the start line (the end line will be drawn by the next segment)
        float xInnerStart = centerX + INNER_RADIUS * (float) Math.cos(startRad);
        float yInnerStart = centerY + INNER_RADIUS * (float) Math.sin(startRad);
        float xOuterStart = centerX + OUTER_RADIUS * (float) Math.cos(startRad);
        float yOuterStart = centerY + OUTER_RADIUS * (float) Math.sin(startRad);

        bufferBuilder.vertex(xInnerStart, yInnerStart, 0).color(0.1f, 0.1f, 0.1f, 1.0f).endVertex();
        bufferBuilder.vertex(xOuterStart, yOuterStart, 0).color(0.1f, 0.1f, 0.1f, 1.0f).endVertex();

        tesselator.end();

        // Draw the circular outlines (only once, not for each segment)
        if (startAngle == 0) { // Only draw circles for the first segment
            // Inner circle
            drawCircle(guiGraphics, centerX, centerY, INNER_RADIUS, 0.1f, 0.1f, 0.1f, 1.0f);
            // Outer circle
            drawCircle(guiGraphics, centerX, centerY, OUTER_RADIUS, 0.1f, 0.1f, 0.1f, 1.0f);
        }

        RenderSystem.lineWidth(1.0f);
    }

    /**
     * Draws a complete circle outline
     */
    private void drawCircle(GuiGraphics guiGraphics, int centerX, int centerY, int radius, float r, float g, float b, float a) {
        int segments = 64;
        float angleStep = 360f / segments;

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();

        bufferBuilder.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i <= segments; i++) {
            float angle = (float) Math.toRadians(i * angleStep);
            float x = centerX + radius * (float) Math.cos(angle);
            float y = centerY + radius * (float) Math.sin(angle);
            bufferBuilder.vertex(x, y, 0).color(r, g, b, a).endVertex();
        }

        tesselator.end();
    }

    /**
     * Gets distance from center point
     */
    private float getDistanceFromCenter(int x, int y, int centerX, int centerY) {
        float dx = x - centerX;
        float dy = y - centerY;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Allow game to continue and player to move
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true; // Allow ESC to close
    }

    @Override
    public void onClose() {
        super.onClose();
        // Notify the handler that the wheel was closed
        if (com.xirc.nichirin.client.handler.AttackWheelHandler.isWheelOpen()) {
            com.xirc.nichirin.client.handler.AttackWheelHandler.forceCloseWheel();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left click
            // Check if click is on a segment
            int centerX = width / 2;
            int centerY = height / 2;

            float distance = getDistanceFromCenter((int)mouseX, (int)mouseY, centerX, centerY);
            if (distance >= INNER_RADIUS && distance <= OUTER_RADIUS) {
                // Click is within the wheel, execute the move
                int selectedMove = getSelectedMove();
                if (selectedMove != -1) {
                    // Execute through the handler
                    com.xirc.nichirin.client.handler.AttackWheelHandler.executeAndCloseWheel();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Don't handle the attack wheel key here - let the handler manage it
        // This prevents the double-toggle issue

        // ESC key closes the GUI
        if (keyCode == 256) { // ESC key code
            this.onClose();
            return true;
        }

        // Don't consume any other keys - allow all input to pass through
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return false; // Don't consume character input
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return false; // Don't consume key releases
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        // Only capture mouse when actually over the wheel
        int centerX = width / 2;
        int centerY = height / 2;
        float distance = getDistanceFromCenter((int)mouseX, (int)mouseY, centerX, centerY);
        return distance >= INNER_RADIUS && distance <= OUTER_RADIUS;
    }

    @Override
    protected void init() {
        super.init();
        rebuildWheel();
        // Don't grab or release mouse - let the game handle it normally
    }

    /**
     * Represents a segment in the wheel
     */
    private static class MoveSegment {
        final int index;
        final AbstractMoveset.MoveConfiguration config;

        MoveSegment(int index, AbstractMoveset.MoveConfiguration config) {
            this.index = index;
            this.config = config;
        }
    }
}