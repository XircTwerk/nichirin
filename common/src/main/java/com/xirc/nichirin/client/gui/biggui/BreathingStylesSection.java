package com.xirc.nichirin.client.gui.biggui;

import com.xirc.nichirin.common.data.BreathingStyleHelper;
import com.xirc.nichirin.common.data.ProgressionHelper;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;

import static com.xirc.nichirin.common.data.ProgressionHelper.getUnlockRequirement;

/**
 * Breathing Styles section with all 4 breathing styles and fixed "none" button
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

        // Current style
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

        // Style grid - 2x2 grid for 4 breathing styles
        int gridY = contentY + 10;
        int boxWidth = 140;
        int boxHeight = 80;
        int spacing = 20;

        // Calculate starting X to center the 2x2 grid
        int totalWidth = (boxWidth * 2) + spacing;
        int startX = centerX - totalWidth / 2;

        // Check what's being hovered
        String hoveredLockedStyle = null;

        // Top row - Thunder and Flame
        if (mouseX >= startX && mouseX <= startX + boxWidth && mouseY >= gridY && mouseY <= gridY + boxHeight) {
            if (!isStyleUnlockedWithFallback(player, "thunder_breathing")) {
                hoveredLockedStyle = "thunder_breathing";
            }
        }

        int flameX = startX + boxWidth + spacing;
        if (mouseX >= flameX && mouseX <= flameX + boxWidth && mouseY >= gridY && mouseY <= gridY + boxHeight) {
            if (!isStyleUnlockedWithFallback(player, "flame_breathing")) {
                hoveredLockedStyle = "flame_breathing";
            }
        }

        // Bottom row - Insect and Sound
        int bottomRowY = gridY + boxHeight + spacing;
        if (mouseX >= startX && mouseX <= startX + boxWidth && mouseY >= bottomRowY && mouseY <= bottomRowY + boxHeight) {
            if (!isStyleUnlockedWithFallback(player, "insect_breathing")) {
                hoveredLockedStyle = "insect_breathing";
            }
        }

        int soundX = startX + boxWidth + spacing;
        if (mouseX >= soundX && mouseX <= soundX + boxWidth && mouseY >= bottomRowY && mouseY <= bottomRowY + boxHeight) {
            if (!isStyleUnlockedWithFallback(player, "sound_breathing")) {
                hoveredLockedStyle = "sound_breathing";
            }
        }

        // Show tooltip at top if hovering over locked style
        if (hoveredLockedStyle != null) {
            String requirement = getUnlockRequirement(hoveredLockedStyle);
            Component tooltip = Component.literal(requirement).withStyle(style -> style.withColor(0xFFAA00).withBold(true));
            int tooltipY = 50; // Lower position
            graphics.drawString(font, tooltip, centerX - font.width(tooltip) / 2, tooltipY, 0xFFAA00);
        }

        // Render all 4 breathing style boxes
        // Top row
        renderBreathingStyleBox(graphics, font, player, currentStyle,
                "thunder_breathing",
                startX, gridY, boxWidth, boxHeight);

        renderBreathingStyleBox(graphics, font, player, currentStyle,
                "flame_breathing",
                flameX, gridY, boxWidth, boxHeight);

        // Bottom row
        renderBreathingStyleBox(graphics, font, player, currentStyle,
                "insect_breathing",
                startX, bottomRowY, boxWidth, boxHeight);

        renderBreathingStyleBox(graphics, font, player, currentStyle,
                "sound_breathing",
                soundX, bottomRowY, boxWidth, boxHeight);

        // "None" button - positioned below the grid
        int noneButtonY = bottomRowY + boxHeight + 20;
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

        boolean isUnlocked = isStyleUnlockedWithFallback(player, styleName);
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

        // Style name
        Component displayName = Component.translatable("breathing_style." + styleName);
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
     * Check if a style is unlocked
     */
    private boolean isStyleUnlockedWithFallback(Player player, String styleId) {
        return ProgressionHelper.isStyleUnlocked(player, styleId);
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

        // Calculate click areas for all 4 breathing style boxes
        int boxWidth = 140;
        int boxHeight = 80;
        int spacing = 20;
        int totalWidth = (boxWidth * 2) + spacing;
        int startX = centerX - totalWidth / 2;
        int topRowY = TOP_MARGIN + 10 + 30 + 25 + 20 + 10;
        int bottomRowY = topRowY + boxHeight + spacing;

        // Top row - Thunder Breathing (left)
        if (mouseX >= startX && mouseX <= startX + boxWidth && mouseY >= topRowY && mouseY <= topRowY + boxHeight) {
            return handleStyleClick(player, "thunder_breathing", currentStyle, currentTime);
        }

        // Top row - Flame Breathing (right)
        int flameX = startX + boxWidth + spacing;
        if (mouseX >= flameX && mouseX <= flameX + boxWidth && mouseY >= topRowY && mouseY <= topRowY + boxHeight) {
            return handleStyleClick(player, "flame_breathing", currentStyle, currentTime);
        }

        // Bottom row - Insect Breathing (left)
        if (mouseX >= startX && mouseX <= startX + boxWidth && mouseY >= bottomRowY && mouseY <= bottomRowY + boxHeight) {
            return handleStyleClick(player, "insect_breathing", currentStyle, currentTime);
        }

        // Bottom row - Sound Breathing (right)
        int soundX = startX + boxWidth + spacing;
        if (mouseX >= soundX && mouseX <= soundX + boxWidth && mouseY >= bottomRowY && mouseY <= bottomRowY + boxHeight) {
            return handleStyleClick(player, "sound_breathing", currentStyle, currentTime);
        }

        // Check for "None" button click - FIXED positioning
        int noneButtonY = bottomRowY + boxHeight + 20;
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
        // Check if the style is unlocked
        if (!isStyleUnlockedWithFallback(player, styleName)) {
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