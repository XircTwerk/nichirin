package com.xirc.nichirin.client.gui.biggui;

import com.xirc.nichirin.common.data.BreathingStyleHelper;
import com.xirc.nichirin.common.data.ProgressionHelper;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Breathing Styles section - handles style selection and unlock display
 */
public class BreathingStylesSection {

    private static final int TOP_MARGIN = 20;

    public void render(GuiGraphics graphics, Player player, int contentWidth, int contentHeight, Font font) {
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

        // Style grid - Only Thunder Breathing for now
        int gridY = contentY + 10;
        int boxWidth = 150;
        int boxHeight = 80;

        // Thunder Breathing
        String styleName = "thunder_breathing";
        boolean isUnlocked = ProgressionHelper.isStyleUnlocked(player, styleName);
        boolean isSelected = styleName.equals(currentStyle);

        // Center the single box
        int x = centerX - boxWidth / 2;
        int y = gridY;

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
        graphics.fill(x - 1, y - 1, x + boxWidth + 1, y + boxHeight + 1, borderColor);
        // Background
        graphics.fill(x, y, x + boxWidth, y + boxHeight, bgColor);

        // Style name
        Component displayName = Component.translatable("breathing_style.thunder_breathing");
        int nameColor = isUnlocked ? 0xFFFFFF : 0x888888;
        graphics.drawString(font, displayName,
                x + (boxWidth - font.width(displayName)) / 2,
                y + 10, nameColor);

        // Status
        if (!isUnlocked) {
            Component locked = Component.translatable("gui.nichirin.breathing_styles.locked_status").withStyle(style -> style.withColor(0xFF5555));
            graphics.drawString(font, locked,
                    x + (boxWidth - font.width(locked)) / 2,
                    y + 30, 0xFF5555);
        } else if (isSelected) {
            Component equipped = Component.translatable("gui.nichirin.breathing_styles.equipped").withStyle(style -> style.withColor(0x55FFFF));
            graphics.drawString(font, equipped,
                    x + (boxWidth - font.width(equipped)) / 2,
                    y + 30, 0x55FFFF);
        } else {
            Component clickToSelect = Component.translatable("gui.nichirin.breathing_styles.click_to_select").withStyle(style -> style.withColor(0xAAAAAA));
            graphics.drawString(font, clickToSelect,
                    x + (boxWidth - font.width(clickToSelect)) / 2,
                    y + 30, 0xAAAAAA);
        }

        // Icon placeholder (thunder icon)
        int iconColor = isUnlocked ? 0xFF3A3A3A : 0xFF2A2A2A;
        graphics.fill(x + boxWidth/2 - 16, y + 50, x + boxWidth/2 + 16, y + 75, iconColor);

        // Show unlock requirements if locked
        if (!isUnlocked) {
            int reqY = y + boxHeight + 15;
            Component reqTitle = Component.translatable("gui.nichirin.breathing_styles.unlock_requirements").withStyle(style -> style.withBold(true));
            graphics.drawString(font, reqTitle,
                    centerX - font.width(reqTitle) / 2, reqY, 0xFFFFFF);
            reqY += 15;

            String requirement = ProgressionHelper.getUnlockRequirement(styleName);
            graphics.drawString(font, requirement,
                    centerX - font.width(requirement) / 2, reqY, 0xFFAA00);
            reqY += 20;
        }

        // "None" button
        int noneButtonY = y + boxHeight + (isUnlocked ? 15 : 55);
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

        // Coming soon text
        Component comingSoon = Component.translatable("gui.nichirin.breathing_styles.coming_soon")
                .withStyle(style -> style.withColor(0x777777).withItalic(true));
        graphics.drawString(font, comingSoon,
                centerX - font.width(comingSoon) / 2,
                contentHeight - 60, 0x777777);
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

        // Calculate click area for Thunder Breathing box
        int boxWidth = 150;
        int boxHeight = 80;
        int x = centerX - boxWidth / 2;
        int y = TOP_MARGIN + 10 + 30 + 25 + 20 + 10;

        // Check if click is within Thunder Breathing box
        if (mouseX >= x && mouseX <= x + boxWidth && mouseY >= y && mouseY <= y + boxHeight) {
            String styleName = "thunder_breathing";

            // Check if the style is unlocked
            if (!ProgressionHelper.isStyleUnlocked(player, styleName)) {
                // Style is locked - just play error sound, no message
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

        // Check for "None" button click
        boolean isUnlocked = ProgressionHelper.isStyleUnlocked(player, "thunder_breathing");
        int noneButtonX = centerX - 75;
        int noneButtonY = y + boxHeight + (isUnlocked ? 15 : 55);
        int noneButtonWidth = 150;
        int noneButtonHeight = 20;

        if (mouseX >= noneButtonX && mouseX <= noneButtonX + noneButtonWidth &&
                mouseY >= noneButtonY && mouseY <= noneButtonY + noneButtonHeight) {

            // Use packet to request clearing breathing style
            if (currentStyle != null) {
                NichirinPacketRegistry.requestStyleChange(null);
            } else {
                // Find any unlocked style and set it
                if (ProgressionHelper.isStyleUnlocked(player, "thunder_breathing")) {
                    NichirinPacketRegistry.requestStyleChange("thunder_breathing");
                }
            }

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
}