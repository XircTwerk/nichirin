package com.xirc.nichirin.client.gui.biggui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.xirc.nichirin.client.gui.MoveIcon;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.registry.NichirinMovesetRegistry;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Data section for moveset - shows move icons in a grid with detailed tooltips on hover
 */
public class MovesetDataSection extends AbstractGuiPage {

    private static final int TOP_MARGIN = 20;
    private static final int ICON_SIZE = 32;
    private static final int ICON_SPACING = 8;
    private static final int ICONS_PER_ROW = 6;

    public void render(GuiGraphics graphics, Player player, int contentWidth, int contentHeight, Font font, int mouseX, int mouseY) {
        int contentX = 20;
        int contentY = TOP_MARGIN + 10;
        int centerX = (contentWidth - 20) / 2;

        // Title
        Component title = Component.translatable("gui.nichirin.moveset.data.title").withStyle(style -> style.withBold(true));
        drawAccentTitle(graphics, font, title, centerX, contentY, COLOR_PALETTE.BREATH_CYAN.argb());
        contentY += 30;

        // Get current moveset
        String currentStyle = MovesetHelper.getMovesetId(player);
        if (currentStyle == null) {
            Component noMoveset = Component.translatable("gui.nichirin.moveset.data.no_moveset");
            drawPopPanel(graphics, contentX, contentY, Math.max(160, font.width(noMoveset) + 28), 32, COLOR_PALETTE.BREATH_CYAN.argb());
            graphics.drawString(font, noMoveset, contentX + 14, contentY + 12, COLOR_PALETTE.TEXT_DIM.rgb());
            return;
        }

        // Get the moveset instance
        AbstractMoveset moveset = NichirinMovesetRegistry.getMoveset(currentStyle);
        if (moveset == null) {
            Component invalidMoveset = Component.literal("Invalid moveset: " + currentStyle);
            drawPopPanel(graphics, contentX, contentY, Math.max(174, font.width(invalidMoveset) + 28), 32, COLOR_PALETTE.DANGER.argb());
            graphics.drawString(font, invalidMoveset, contentX + 14, contentY + 12, COLOR_PALETTE.DANGER.rgb());
            return;
        }

        // Display moveset info
        Component styleLabel = Component.translatable("gui.nichirin.moveset.data.current_moveset",
                Component.translatable(getTranslationKey(currentStyle)));
        graphics.fill(contentX - 5, contentY - 2, contentX - 3, contentY + font.lineHeight + 2, COLOR_PALETTE.BREATH_CYAN.argb());
        graphics.drawString(font, styleLabel, contentX, contentY, COLOR_PALETTE.BREATH_CYAN.rgb());
        contentY += 20;

        // Instructions
        Component instructions = Component.literal("Hover over move icons to see detailed information");
        graphics.drawString(font, instructions, contentX, contentY, COLOR_PALETTE.GRAY.rgb());
        contentY += 25;

        // Calculate grid layout
        int totalMoves = moveset.getMoveCount() + 2; // +2 for right click attacks
        int gridStartX = centerX - ((ICONS_PER_ROW * (ICON_SIZE + ICON_SPACING) - ICON_SPACING) / 2);
        int gridStartY = contentY;

        // Track which icon is being hovered for tooltip
        int hoveredMoveIndex = -1;
        boolean hoveredIsRightClick = false;
        boolean hoveredIsCrouchRightClick = false;

        // Render move icons in grid
        int currentX = gridStartX;
        int currentY = gridStartY;
        int iconsInCurrentRow = 0;

        // Right click icon (first)
        if (isIconHovered(mouseX, mouseY, currentX, currentY)) {
            hoveredIsRightClick = true;
        }
        ResourceLocation rightClickIcon = MoveIcon.getIcon(currentStyle, "right_click");
        renderMoveIcon(graphics, currentX, currentY, rightClickIcon, COLOR_PALETTE.SLAYER_BLUE.argb());

        currentX += ICON_SIZE + ICON_SPACING;
        iconsInCurrentRow++;

        // Crouch right click icon (second)
        if (isIconHovered(mouseX, mouseY, currentX, currentY)) {
            hoveredIsCrouchRightClick = true;
        }
        ResourceLocation crouchRightClickIcon = MoveIcon.getIcon(currentStyle, "crouch_right_click");
        renderMoveIcon(graphics, currentX, currentY, crouchRightClickIcon, COLOR_PALETTE.GREEN.argb());

        currentX += ICON_SIZE + ICON_SPACING;
        iconsInCurrentRow++;

        // Moveset-specific moves
        for (int i = 0; i < moveset.getMoveCount(); i++) {
            AbstractMoveset.MoveConfiguration move = moveset.getMove(i);
            if (move == null) continue;

            // Check for new row
            if (iconsInCurrentRow >= ICONS_PER_ROW) {
                currentX = gridStartX;
                currentY += ICON_SIZE + ICON_SPACING;
                iconsInCurrentRow = 0;
            }

            // Check if this icon is being hovered
            if (isIconHovered(mouseX, mouseY, currentX, currentY)) {
                hoveredMoveIndex = i;
            }

            ResourceLocation moveIcon = MoveIcon.getIcon(currentStyle, move.getMoveId());
            renderMoveIcon(graphics, currentX, currentY, moveIcon, COLOR_PALETTE.BORDER_HI.argb());

            currentX += ICON_SIZE + ICON_SPACING;
            iconsInCurrentRow++;
        }

        // Render tooltip if hovering over an icon
        if (hoveredIsRightClick) {
            renderRightClickTooltip(graphics, font, mouseX, mouseY, moveset, contentWidth);
        } else if (hoveredIsCrouchRightClick) {
            renderCrouchRightClickTooltip(graphics, font, mouseX, mouseY, moveset, contentWidth);
        } else if (hoveredMoveIndex >= 0) {
            AbstractMoveset.MoveConfiguration move = moveset.getMove(hoveredMoveIndex);
            if (move != null) {
                renderMoveTooltip(graphics, font, mouseX, mouseY, move, hoveredMoveIndex, contentWidth);
            }
        }
    }

    /**
     * Check if mouse is hovering over an icon
     */
    private boolean isIconHovered(int mouseX, int mouseY, int iconX, int iconY) {
        return mouseX >= iconX && mouseX <= iconX + ICON_SIZE &&
                mouseY >= iconY && mouseY <= iconY + ICON_SIZE;
    }

    /**
     * Render a move icon using your MoveIcon system
     */
    private void renderMoveIcon(GuiGraphics graphics, int x, int y, ResourceLocation iconTexture, int borderColor) {
        graphics.fill(x - 2, y - 2, x + ICON_SIZE + 2, y + ICON_SIZE + 2, withAlpha(borderColor, 0x26));
        // Draw border
        graphics.fill(x - 1, y - 1, x + ICON_SIZE + 1, y + ICON_SIZE + 1, borderColor);
        graphics.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, COLOR_PALETTE.PANEL_MID.argb());
        graphics.fill(x, y, x + ICON_SIZE, y + 2, withAlpha(borderColor, 0xCC));

        // Draw the actual icon texture — enable blend before blit for transparency support
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(iconTexture, x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        RenderSystem.disableBlend();
    }

    /**
     * Render tooltip for right click - NOW PROPERLY DISPLAYS STATS
     */
    private void renderRightClickTooltip(GuiGraphics graphics, Font font, int mouseX, int mouseY, AbstractMoveset moveset, int contentWidth) {
        List<String> tooltipLines = new ArrayList<>();

        String moveName = moveset.getRightClickMoveName();
        tooltipLines.add("Right Click - " + moveName);
        tooltipLines.add("");

        // Add description if available
        String description = moveset.getRightClickDescription();
        if (description != null && !description.isEmpty()) {
            tooltipLines.add(description);
            tooltipLines.add("");
        }

        // Get right click stats if the moveset exposes them
        AbstractMoveset.MoveConfiguration rightClickConfig = moveset.getRightClickConfiguration();
        if (rightClickConfig != null) {
            // Actually add the config data to tooltip lines
            addConfigTooltipLines(tooltipLines, rightClickConfig);
        } else {
            tooltipLines.add("Special moveset ability");
            tooltipLines.add("Stats vary by breathing style");
            tooltipLines.add("");
            tooltipLines.add("Use this move to capture configuration data");
        }

        renderTooltip(graphics, font, mouseX, mouseY, tooltipLines.toArray(new String[0]), contentWidth);
    }

    /**
     * Render tooltip for crouch right click - NOW PROPERLY DISPLAYS STATS
     */
    private void renderCrouchRightClickTooltip(GuiGraphics graphics, Font font, int mouseX, int mouseY, AbstractMoveset moveset, int contentWidth) {
        List<String> tooltipLines = new ArrayList<>();

        String moveName = moveset.getCrouchRightClickMoveName();
        tooltipLines.add("Crouch + Right Click - " + moveName);
        tooltipLines.add("");

        // Add description if available
        String description = moveset.getCrouchRightClickDescription();
        if (description != null && !description.isEmpty()) {
            tooltipLines.add(description);
            tooltipLines.add("");
        }

        // Get crouch right click stats if the moveset exposes them
        AbstractMoveset.MoveConfiguration crouchRightClickConfig = moveset.getCrouchRightClickConfiguration();
        if (crouchRightClickConfig != null) {
            // Actually add the config data to tooltip lines
            addConfigTooltipLines(tooltipLines, crouchRightClickConfig);
        } else {
            tooltipLines.add("Special crouch ability");
            tooltipLines.add("Stats vary by breathing style");
            tooltipLines.add("");
            tooltipLines.add("Use this move to capture configuration data");
        }

        renderTooltip(graphics, font, mouseX, mouseY, tooltipLines.toArray(new String[0]), contentWidth);
    }

    /**
     * Render detailed tooltip for a specific move - ONLY CONFIGURED VALUES
     */
    private void renderMoveTooltip(GuiGraphics graphics, Font font, int mouseX, int mouseY,
                                   AbstractMoveset.MoveConfiguration move, int moveIndex, int contentWidth) {
        List<String> tooltipLines = new ArrayList<>();

        // Move name and number
        tooltipLines.add("Move " + (moveIndex + 1) + ": " + move.getDisplayName());
        tooltipLines.add("");

        // Add description if available
        String description = move.getDescription();
        if (description != null && !description.isEmpty()) {
            tooltipLines.add(description);
            tooltipLines.add("");
        }

        addConfigTooltipLines(tooltipLines, move);
        renderTooltip(graphics, font, mouseX, mouseY, tooltipLines.toArray(new String[0]), contentWidth);
    }

    /**
     * Add configuration data to tooltip lines - only configured values
     */
    private void addConfigTooltipLines(List<String> tooltipLines, AbstractMoveset.MoveConfiguration config) {
        boolean hasStats = false;

        // Combat stats - only show if configured
        if (config.hasDamage()) {
            tooltipLines.add("Damage: " + String.format("%.1f", config.getDamage()));
            hasStats = true;
        }
        if (config.hasRange()) {
            tooltipLines.add("Range: " + String.format("%.1f blocks", config.getRange()));
            hasStats = true;
        }
        if (config.hasKnockback()) {
            tooltipLines.add("Knockback: " + String.format("%.1f", config.getKnockback()));
            hasStats = true;
        }

        // Timing - only show if configured
        if (config.hasCooldown()) {
            tooltipLines.add("Cooldown: " + (config.getCooldown() / 20) + "s");
            hasStats = true;
        }
        if (config.hasWindup()) {
            tooltipLines.add("Windup: " + String.format("%.1fs", config.getWindup() / 20.0f));
            hasStats = true;
        }
        if (config.hasDuration()) {
            tooltipLines.add("Duration: " + String.format("%.1fs", config.getDuration() / 20.0f));
            hasStats = true;
        }

        // Resource costs - only show if configured
        if (config.hasBreathCost()) {
            tooltipLines.add("Breath Cost: " + String.format("%.0f", config.getBreathCost()));
            hasStats = true;
        }
        if (config.hasStaminaCost()) {
            tooltipLines.add("Stamina Cost: " + String.format("%.0f", config.getStaminaCost()));
            hasStats = true;
        }

        // Additional effects - only show if configured
        if (config.hasHitStun()) {
            tooltipLines.add("Hit Stun: " + String.format("%.1fs", config.getHitStun() / 20.0f));
            hasStats = true;
        }
        if (config.hasHitboxSize()) {
            tooltipLines.add("Hitbox Size: " + String.format("%.1f", config.getHitboxSize()));
            hasStats = true;
        }
        if (config.hasTeleportDistance()) {
            tooltipLines.add("Teleport Distance: " + String.format("%.1f blocks", config.getTeleportDistance()));
            hasStats = true;
        }
        if (config.hasDashSpeed()) {
            tooltipLines.add("Dash Speed: " + String.format("%.1f blocks", config.getDashSpeed()));
            hasStats = true;
        }

        // Add separator if we added stats
        if (hasStats) {
            tooltipLines.add(""); // Empty line for spacing
        }
    }

    /**
     * Render a tooltip box with multiple lines
     */
    private void renderTooltip(GuiGraphics graphics, Font font, int mouseX, int mouseY, String[] lines, int contentWidth) {
        if (lines.length == 0) return;

        // Calculate tooltip size
        int maxWidth = 0;
        for (String line : lines) {
            maxWidth = Math.max(maxWidth, font.width(line));
        }

        int tooltipWidth = maxWidth + 8;
        int tooltipHeight = lines.length * font.lineHeight + 6;

        // Position tooltip (avoid going off screen)
        int tooltipX = mouseX + 10;
        int tooltipY = mouseY - 10;

        if (tooltipX + tooltipWidth > contentWidth - 20) {
            tooltipX = mouseX - tooltipWidth - 10;
        }
        tooltipX = Math.max(5, tooltipX); // Prevent going off the left edge
        if (tooltipY < 10) {
            tooltipY = mouseY + 20;
        }

        // Draw tooltip background
        graphics.fill(tooltipX - 1, tooltipY - 1, tooltipX + tooltipWidth + 1, tooltipY + tooltipHeight + 1, COLOR_PALETTE.BLACK.argb());
        graphics.fill(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight, COLOR_PALETTE.TOOLTIP_FILL.argb());

        // Draw tooltip text
        int textY = tooltipY + 3;
        for (String line : lines) {
            if (!line.isEmpty()) {
                graphics.drawString(font, line, tooltipX + 4, textY, COLOR_PALETTE.WHITE.rgb());
            }
            textY += font.lineHeight;
        }
    }

    /**
     * Get appropriate translation key for moveset
     */
    private String getTranslationKey(String movesetId) {
        if (movesetId.contains("demon_art")) {
            return "demon_art." + movesetId;
        } else {
            return "breathing_style." + movesetId;
        }
    }

    public boolean handleClick(double mouseX, double mouseY, Player player, int contentWidth) {
        // No click handling needed for data section
        return false;
    }
}