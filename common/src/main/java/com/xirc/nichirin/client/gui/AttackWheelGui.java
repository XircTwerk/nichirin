package com.xirc.nichirin.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset.MoveConfiguration;
import com.xirc.nichirin.common.data.BreathingStyleHelper;
import com.xirc.nichirin.common.data.MovesetRegistry;
import com.xirc.nichirin.common.util.enums.MoveClass;
import com.xirc.nichirin.common.util.enums.MoveInputType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    @Override
    protected void init() {
        super.init();
        rebuildWheel();
    }

    /**
     * Rebuilds the wheel based on current moveset
     */
    private void rebuildWheel() {
        segments.clear();

        Player player = minecraft.player;
        if (player == null) return;

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

        // Get all moves from the moveset
        Map<MoveClass, MoveConfiguration> moves = moveset.getAllMoves();

        if (moves.isEmpty()) {
            System.out.println("DEBUG: Built wheel with 0 segments (no moves)");
            return;
        }

        // Create segments for each move
        for (Map.Entry<MoveClass, MoveConfiguration> entry : moves.entrySet()) {
            MoveSegment segment = new MoveSegment(entry.getKey(), entry.getValue());
            segments.add(segment);
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
        rebuildWheel();
    }

    /**
     * Deactivates the wheel and returns selected move
     */
    public MoveClass deactivate() {
        isActive = false;
        return getSelectedMove();
    }

    /**
     * Gets the currently selected move based on mouse position
     */
    public MoveClass getSelectedMove() {
        if (segments.isEmpty()) return null;

        int mouseX = (int)(minecraft.mouseHandler.xpos() * minecraft.getWindow().getGuiScaledWidth() / minecraft.getWindow().getScreenWidth());
        int mouseY = (int)(minecraft.mouseHandler.ypos() * minecraft.getWindow().getGuiScaledHeight() / minecraft.getWindow().getScreenHeight());

        int centerX = width / 2;
        int centerY = height / 2;

        float angle = (float) Math.toDegrees(Math.atan2(mouseY - centerY, mouseX - centerX));
        if (angle < 0) angle += 360;

        int segmentIndex = (int)(angle / segmentAngle);
        if (segmentIndex >= 0 && segmentIndex < segments.size()) {
            return segments.get(segmentIndex).moveClass;
        }

        return null;
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
            RenderSystem.setShaderColor(0.3f, 0.3f, 0.3f, 0.6f);

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
            if (getDistanceFromCenter(mouseX, mouseY, centerX, centerY) <= OUTER_RADIUS &&
                    getDistanceFromCenter(mouseX, mouseY, centerX, centerY) >= INNER_RADIUS) {
                hoveredSegment = (int)(angle / segmentAngle);
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

                String moveName = formatMoveName(segments.get(i).moveClass);
                guiGraphics.drawCenteredString(font, moveName, textX, textY - 4, 0xFFFFFF);
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

        bufferBuilder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        // Dark gray color scheme
        float r = isHovered ? 0.4f : 0.2f;
        float g = isHovered ? 0.4f : 0.2f;
        float b = isHovered ? 0.4f : 0.2f;
        float a = 0.6f; // Semi-transparent

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

        // Draw separator lines
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        bufferBuilder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        // Draw line at start angle
        float angleRad = (float) Math.toRadians(startAngle);
        float xInner = centerX + INNER_RADIUS * (float) Math.cos(angleRad);
        float yInner = centerY + INNER_RADIUS * (float) Math.sin(angleRad);
        float xOuter = centerX + OUTER_RADIUS * (float) Math.cos(angleRad);
        float yOuter = centerY + OUTER_RADIUS * (float) Math.sin(angleRad);

        bufferBuilder.vertex(xInner, yInner, 0).color(0.5f, 0.5f, 0.5f, 0.8f).endVertex();
        bufferBuilder.vertex(xOuter, yOuter, 0).color(0.5f, 0.5f, 0.5f, 0.8f).endVertex();

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

    /**
     * Formats move name for display
     */
    private String formatMoveName(MoveClass moveClass) {
        String name = moveClass.name();
        // Convert SPECIAL1 -> Form 1, BASIC -> Basic, etc.
        if (name.startsWith("SPECIAL")) {
            return "Form " + name.substring(7);
        }
        // Capitalize first letter, lowercase rest
        return name.charAt(0) + name.substring(1).toLowerCase();
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
     * Represents a segment in the wheel
     */
    private static class MoveSegment {
        final MoveClass moveClass;
        final MoveConfiguration config;

        MoveSegment(MoveClass moveClass, MoveConfiguration config) {
            this.moveClass = moveClass;
            this.config = config;
        }
    }
}