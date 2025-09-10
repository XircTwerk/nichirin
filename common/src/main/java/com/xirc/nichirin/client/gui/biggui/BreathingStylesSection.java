package com.xirc.nichirin.client.gui.biggui;

import com.xirc.nichirin.common.data.BreathingStyleHelper;
import com.xirc.nichirin.common.data.ProgressionHelper;
import com.xirc.nichirin.registry.MovesetRegistry;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import static com.xirc.nichirin.common.data.ProgressionHelper.getUnlockRequirement;

/**
 * Breathing Styles section that uses the same data sources as the BreathingCommand
 * Now properly checks MovesetRegistry for available styles and ProgressionHelper for unlocks
 */
public class BreathingStylesSection {

    private static final int TOP_MARGIN = 20;

    public void render(GuiGraphics graphics, Player player, int contentWidth, int contentHeight, Font font, int mouseX, int mouseY) {
        int contentX = 20;
        int contentY = TOP_MARGIN + 10;
        int centerX = (contentWidth - 20) / 2;

        // Title
        Component title = Component.translatable("gui.nichirin.breathing_styles.title").withStyle(style -> style.withBold(true));
        graphics.drawString(font, title,
                centerX - font.width(title) / 2, contentY, 0xFFFFFF);
        contentY += 30;

        // Current style - use same data source as command
        String currentStyle = BreathingStyleHelper.getMovesetId(player);
        if (currentStyle != null) {
            Component current = Component.translatable("gui.nichirin.breathing_styles.current",
                            Component.translatable("breathing_style." + currentStyle))
                    .withStyle(style -> style.withColor(0x55FFFF));
            graphics.drawString(font, current, contentX, contentY, 0x55FFFF);
            contentY += 25;
        }

        // Instructions
        Component instructions = Component.translatable("gui.nichirin.breathing_styles.instructions");
        graphics.drawString(font, instructions, contentX, contentY, 0xAAAAAA);
        contentY += 20;

        // Get all registered breathing styles from MovesetRegistry (same as command)
        var allStyles = MovesetRegistry.getAllMovesetIds();

        // Filter to only breathing styles (assuming they follow naming convention)
        var breathingStyles = allStyles.stream()
                .filter(styleId -> styleId.contains("breathing"))
                .limit(4) // Show max 4 for now
                .toArray(String[]::new);

        if (breathingStyles.length == 0) {
            Component noStyles = Component.literal("No breathing styles registered").withStyle(style -> style.withColor(0xFF5555));
            graphics.drawString(font, noStyles, centerX - font.width(noStyles) / 2, contentY + 50, 0xFF5555);
            return;
        }

        // Style grid - dynamic based on available styles
        int gridY = contentY + 10;
        int boxWidth = 140;
        int boxHeight = 80;
        int spacing = 20;

        // Calculate grid layout
        int cols = Math.min(2, breathingStyles.length);
        int rows = (int) Math.ceil((double) breathingStyles.length / cols);
        int totalWidth = (boxWidth * cols) + (spacing * (cols - 1));
        int startX = centerX - totalWidth / 2;

        // Check what's being hovered
        String hoveredLockedStyle = null;

        // Render style boxes dynamically
        for (int i = 0; i < breathingStyles.length; i++) {
            String styleId = breathingStyles[i];
            int row = i / cols;
            int col = i % cols;

            int x = startX + col * (boxWidth + spacing);
            int y = gridY + row * (boxHeight + spacing);

            // Check if this box is being hovered and is locked
            if (mouseX >= x && mouseX <= x + boxWidth && mouseY >= y && mouseY <= y + boxHeight) {
                if (!isStyleUnlocked(player, styleId)) {
                    hoveredLockedStyle = styleId;
                }
            }

            renderBreathingStyleBox(graphics, font, player, currentStyle, styleId, x, y, boxWidth, boxHeight);
        }

        // Show tooltip at top if hovering over locked style
        if (hoveredLockedStyle != null) {
            String requirement = getUnlockRequirement(hoveredLockedStyle);
            Component tooltip = Component.literal(requirement).withStyle(style -> style.withColor(0xFFAA00).withBold(true));
            int tooltipY = 50;
            graphics.drawString(font, tooltip, centerX - font.width(tooltip) / 2, tooltipY, 0xFFAA00);
        }

        // "None" button - positioned below the grid
        int lastRow = (breathingStyles.length - 1) / cols;
        int noneButtonY = gridY + (lastRow + 1) * (boxHeight + spacing) + 20;
        int noneButtonX = centerX - 75;
        int noneButtonWidth = 150;
        int noneButtonHeight = 20;

        // None button background
        int noneButtonBg = (currentStyle == null) ? 0xFF3A3A3A : 0xFF2A2A2A;
        int noneButtonBorder = (currentStyle == null) ? 0xFF55FFFF : 0xFF4A4A4A;

        graphics.fill(noneButtonX - 1, noneButtonY - 1,
                noneButtonX + noneButtonWidth + 1, noneButtonY + noneButtonHeight + 1, noneButtonBorder);
        graphics.fill(noneButtonX, noneButtonY,
                noneButtonX + noneButtonWidth, noneButtonY + noneButtonHeight, noneButtonBg);

        Component noneText = Component.translatable("gui.nichirin.breathing_styles.none");
        int noneTextColor = (currentStyle == null) ? 0x55FFFF : 0xAAAAAA;
        graphics.drawString(font, noneText,
                noneButtonX + (noneButtonWidth - font.width(noneText)) / 2,
                noneButtonY + 6, noneTextColor);
    }

    /**
     * Renders a single breathing style selection box
     */
    private void renderBreathingStyleBox(GuiGraphics graphics, Font font, Player player, String currentStyle,
                                         String styleName, int x, int y, int width, int height) {

        // Use same unlock check as command
        boolean isUnlocked = isStyleUnlocked(player, styleName);
        boolean isSelected = styleName.equals(currentStyle);

        // Draw box with different colors based on unlock status
        int bgColor;
        int borderColor;

        if (!isUnlocked) {
            bgColor = 0xFF1A1A1A; // Darker for locked
            borderColor = 0xFF666666; // Gray border for locked
        } else if (isSelected) {
            bgColor = 0xFF3A3A3A;
            borderColor = 0xFF55FFFF; // Cyan for selected
        } else {
            bgColor = 0xFF2A2A2A;
            borderColor = 0xFF4A4A4A; // Normal border
        }

        // Border
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, borderColor);
        // Background
        graphics.fill(x, y, x + width, y + height, bgColor);

        // Style name - format same as command
        Component displayName = Component.literal(formatStyleName(styleName));
        int nameColor = isUnlocked ? 0xFFFFFF : 0x888888;
        graphics.drawString(font, displayName,
                x + (width - font.width(displayName)) / 2,
                y + 10, nameColor);

        // Status
        if (!isUnlocked) {
            Component locked = Component.translatable("gui.nichirin.breathing_styles.locked_status")
                    .withStyle(style -> style.withColor(0xFF5555));
            graphics.drawString(font, locked,
                    x + (width - font.width(locked)) / 2,
                    y + 30, 0xFF5555);
        } else if (isSelected) {
            Component equipped = Component.translatable("gui.nichirin.breathing_styles.equipped")
                    .withStyle(style -> style.withColor(0x55FFFF));
            graphics.drawString(font, equipped,
                    x + (width - font.width(equipped)) / 2,
                    y + 30, 0x55FFFF);
        } else {
            Component clickToSelect = Component.translatable("gui.nichirin.breathing_styles.click_to_select")
                    .withStyle(style -> style.withColor(0xAAAAAA));
            graphics.drawString(font, clickToSelect,
                    x + (width - font.width(clickToSelect)) / 2,
                    y + 30, 0xAAAAAA);
        }

        // Icon placeholder
        int iconColor = isUnlocked ? 0xFF3A3A3A : 0xFF2A2A2A;
        graphics.fill(x + width/2 - 16, y + 50, x + width/2 + 16, y + 75, iconColor);
    }

    /**
     * Check if a style is unlocked - uses same method as command
     */
    private boolean isStyleUnlocked(Player player, String styleId) {
        // First check if the style is even registered
        if (!MovesetRegistry.isRegistered(styleId)) {
            return false;
        }

        // Then check if player has unlocked it
        return ProgressionHelper.isStyleUnlocked(player, styleId);
    }

    /**
     * Format style name same as command
     */
    private String formatStyleName(String styleId) {
        String[] parts = styleId.split("_");
        StringBuilder formatted = new StringBuilder();
        for (String part : parts) {
            if (formatted.length() > 0) formatted.append(" ");
            formatted.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
        }
        return formatted.toString();
    }

    private static long lastClickTime = 0;
    private static final long CLICK_COOLDOWN = 500; // 500ms cooldown

    public boolean handleClick(double mouseX, double mouseY, Player player, int contentWidth) {
        // Prevent click spam
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClickTime < CLICK_COOLDOWN) {
            return false;
        }

        String currentStyle = BreathingStyleHelper.getMovesetId(player);
        int centerX = (contentWidth - 20) / 2;

        // Get all registered breathing styles (same as render method)
        var allStyles = MovesetRegistry.getAllMovesetIds();
        var breathingStyles = allStyles.stream()
                .filter(styleId -> styleId.contains("breathing"))
                .limit(4)
                .toArray(String[]::new);

        if (breathingStyles.length == 0) {
            return false;
        }

        // Calculate click areas dynamically
        int boxWidth = 140;
        int boxHeight = 80;
        int spacing = 20;
        int cols = Math.min(2, breathingStyles.length);
        int totalWidth = (boxWidth * cols) + (spacing * (cols - 1));
        int startX = centerX - totalWidth / 2;
        int topRowY = TOP_MARGIN + 10 + 30 + 25 + 20 + 10;

        // Check clicks on style boxes
        for (int i = 0; i < breathingStyles.length; i++) {
            String styleId = breathingStyles[i];
            int row = i / cols;
            int col = i % cols;

            int x = startX + col * (boxWidth + spacing);
            int y = topRowY + row * (boxHeight + spacing);

            if (mouseX >= x && mouseX <= x + boxWidth && mouseY >= y && mouseY <= y + boxHeight) {
                return handleStyleClick(player, styleId, currentStyle, currentTime);
            }
        }

        // Check for "None" button click
        int lastRow = (breathingStyles.length - 1) / cols;
        int noneButtonY = topRowY + (lastRow + 1) * (boxHeight + spacing) + 20;
        int noneButtonX = centerX - 75;
        int noneButtonWidth = 150;
        int noneButtonHeight = 20;

        if (mouseX >= noneButtonX && mouseX <= noneButtonX + noneButtonWidth &&
                mouseY >= noneButtonY && mouseY <= noneButtonY + noneButtonHeight) {

            // Clear breathing style regardless of current state
            NichirinPacketRegistry.requestStyleChange(null);

            // Play click sound
            Minecraft.getInstance().getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.0F
                    )
            );

            lastClickTime = currentTime;
            return true;
        }

        return false;
    }

    /**
     * Handle clicking on a breathing style
     */
    private boolean handleStyleClick(Player player, String styleName, String currentStyle, long currentTime) {
        // Check if the style is unlocked (same as command logic)
        if (!isStyleUnlocked(player, styleName)) {
            // Style is locked - just play error sound
            Minecraft.getInstance().getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.5F, 0.8F
                    )
            );
            lastClickTime = currentTime;
            return true;
        }

        // Style is unlocked - only set if not already selected
        if (!styleName.equals(currentStyle)) {
            // Use packet to request style change from server
            NichirinPacketRegistry.requestStyleChange(styleName);

            // Play success sound
            Minecraft.getInstance().getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.0F
                    )
            );
        }

        lastClickTime = currentTime;
        return true;
    }
}