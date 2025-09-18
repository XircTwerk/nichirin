package com.xirc.nichirin.client.gui.biggui;

import com.xirc.nichirin.client.data.ClientProgressionCache;
import com.xirc.nichirin.common.data.MovesetHelper;
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
 * Demon Arts section that mirrors the breathing styles system but for demon arts
 */
public class DemonArtSection {

    private static final int TOP_MARGIN = 20;

    public void render(GuiGraphics graphics, Player player, int contentWidth, int contentHeight, Font font, int mouseX, int mouseY) {
        int contentX = 20;
        int contentY = TOP_MARGIN + 10;
        int centerX = (contentWidth - 20) / 2;

        // Title
        Component title = Component.translatable("gui.nichirin.demon_arts.title").withStyle(style -> style.withBold(true));
        graphics.drawString(font, title,
                centerX - font.width(title) / 2, contentY, 0xFFFFFF);
        contentY += 30;

        // Current demon art - use same data source as breathing styles
        String currentStyle = MovesetHelper.getMovesetId(player);
        if (currentStyle != null && isDemonArt(currentStyle)) {
            Component current = Component.translatable("gui.nichirin.demon_arts.current",
                            Component.translatable("demon_art." + currentStyle))
                    .withStyle(style -> style.withColor(0xFF5555));
            graphics.drawString(font, current, contentX, contentY, 0xFF5555);
            contentY += 25;
        }

        // Instructions
        Component instructions = Component.translatable("gui.nichirin.demon_arts.instructions");
        graphics.drawString(font, instructions, contentX, contentY, 0xAAAAAA);
        contentY += 20;

        // Show all demon arts
        String[] demonArts = {
        };

        if (demonArts.length == 0) {
            Component noArts = Component.literal("No demon arts registered").withStyle(style -> style.withColor(0xFF5555));
            graphics.drawString(font, noArts, centerX - font.width(noArts) / 2, contentY + 50, 0xFF5555);
            return;
        }

        // Art grid - dynamic based on available arts
        int gridY = contentY + 10;
        int boxWidth = 140;
        int boxHeight = 80;
        int spacing = 20;

        // Calculate grid layout
        int cols = Math.min(2, demonArts.length);
        int rows = (int) Math.ceil((double) demonArts.length / cols);
        int totalWidth = (boxWidth * cols) + (spacing * (cols - 1));
        int startX = centerX - totalWidth / 2;

        // Check what's being hovered
        String hoveredLockedArt = null;

        // Render art boxes dynamically
        for (int i = 0; i < demonArts.length; i++) {
            String artId = demonArts[i];
            int row = i / cols;
            int col = i % cols;

            int x = startX + col * (boxWidth + spacing);
            int y = gridY + row * (boxHeight + spacing);

            // Check if this box is being hovered and is locked
            if (mouseX >= x && mouseX <= x + boxWidth && mouseY >= y && mouseY <= y + boxHeight) {
                if (!isArtUnlocked(player, artId)) {
                    hoveredLockedArt = artId;
                }
            }

            renderDemonArtBox(graphics, font, player, currentStyle, artId, x, y, boxWidth, boxHeight);
        }

        // Show tooltip at top if hovering over locked art
        if (hoveredLockedArt != null) {
            String requirement = getUnlockRequirement(hoveredLockedArt);
            Component tooltip = Component.literal(requirement).withStyle(style -> style.withColor(0xFF5555).withBold(true));
            int tooltipY = 50;
            graphics.drawString(font, tooltip, centerX - font.width(tooltip) / 2, tooltipY, 0xFF5555);
        }

        // "None" button - positioned below the grid
        int lastRow = (demonArts.length - 1) / cols;
        int noneButtonY = gridY + (lastRow + 1) * (boxHeight + spacing) + 20;
        int noneButtonX = centerX - 75;
        int noneButtonWidth = 150;
        int noneButtonHeight = 20;

        // None button background
        boolean isNoneSelected = (currentStyle == null || !isDemonArt(currentStyle));
        int noneButtonBg = isNoneSelected ? 0xFF3A3A3A : 0xFF2A2A2A;
        int noneButtonBorder = isNoneSelected ? 0xFF5555 : 0xFF4A4A4A;

        graphics.fill(noneButtonX - 1, noneButtonY - 1,
                noneButtonX + noneButtonWidth + 1, noneButtonY + noneButtonHeight + 1, noneButtonBorder);
        graphics.fill(noneButtonX, noneButtonY,
                noneButtonX + noneButtonWidth, noneButtonY + noneButtonHeight, noneButtonBg);

        Component noneText = Component.translatable("gui.nichirin.demon_arts.none");
        int noneTextColor = isNoneSelected ? 0xFF5555 : 0xAAAAAA;
        graphics.drawString(font, noneText,
                noneButtonX + (noneButtonWidth - font.width(noneText)) / 2,
                noneButtonY + 6, noneTextColor);
    }

    /**
     * Renders a single demon art selection box
     */
    private void renderDemonArtBox(GuiGraphics graphics, Font font, Player player, String currentStyle,
                                   String artName, int x, int y, int width, int height) {

        boolean isUnlocked = isArtUnlocked(player, artName);
        boolean isSelected = artName.equals(currentStyle);

        // Draw box with different colors based on unlock status (red theme for demon arts)
        int bgColor;
        int borderColor;

        if (!isUnlocked) {
            bgColor = 0xFF1A1A1A; // Darker for locked
            borderColor = 0xFF666666; // Gray border for locked
        } else if (isSelected) {
            bgColor = 0xFF3A1A1A; // Dark red for selected
            borderColor = 0xFFFF5555; // Red for selected
        } else {
            bgColor = 0xFF2A2A2A;
            borderColor = 0xFF4A4A4A; // Normal border
        }

        // Border
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, borderColor);
        // Background
        graphics.fill(x, y, x + width, y + height, bgColor);

        // Art name
        Component displayName = Component.literal(formatArtName(artName));
        int nameColor = isUnlocked ? 0xFFFFFF : 0x888888;
        graphics.drawString(font, displayName,
                x + (width - font.width(displayName)) / 2,
                y + 10, nameColor);

        // Status
        if (!isUnlocked) {
            Component locked = Component.translatable("gui.nichirin.demon_arts.locked_status")
                    .withStyle(style -> style.withColor(0xFF5555));
            graphics.drawString(font, locked,
                    x + (width - font.width(locked)) / 2,
                    y + 30, 0xFF5555);
        } else if (isSelected) {
            Component equipped = Component.translatable("gui.nichirin.demon_arts.equipped")
                    .withStyle(style -> style.withColor(0xFF5555));
            graphics.drawString(font, equipped,
                    x + (width - font.width(equipped)) / 2,
                    y + 30, 0xFF5555);
        } else {
            Component clickToSelect = Component.translatable("gui.nichirin.demon_arts.click_to_select")
                    .withStyle(style -> style.withColor(0xAAAAAA));
            graphics.drawString(font, clickToSelect,
                    x + (width - font.width(clickToSelect)) / 2,
                    y + 30, 0xAAAAAA);
        }

        // Icon placeholder (red tinted for demon arts)
        int iconColor = isUnlocked ? 0xFF3A1A1A : 0xFF2A1A1A;
        graphics.fill(x + width/2 - 16, y + 50, x + width/2 + 16, y + 75, iconColor);
    }

    /**
     * Check if an art is unlocked
     */
    private boolean isArtUnlocked(Player player, String artId) {
        if (player.level().isClientSide()) {
            return ClientProgressionCache.isUnlocked(artId);
        }

        // Server side
        if (!MovesetRegistry.isRegistered(artId)) {
            return false;
        }
        return ProgressionHelper.isStyleUnlocked(player, artId);
    }

    /**
     * Check if a moveset ID represents a demon art
     */
    private boolean isDemonArt(String movesetId) {
        if (movesetId == null) return false;
        return movesetId.contains("demon_art");
    }

    /**
     * Format art name
     */
    private String formatArtName(String artId) {
        String[] parts = artId.split("_");
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

        String currentStyle = MovesetHelper.getMovesetId(player);
        int centerX = (contentWidth - 20) / 2;

        String[] demonArts = {
        };

        if (demonArts.length == 0) {
            return false;
        }

        // Calculate click areas
        int boxWidth = 140;
        int boxHeight = 80;
        int spacing = 20;
        int cols = Math.min(2, demonArts.length);
        int totalWidth = (boxWidth * cols) + (spacing * (cols - 1));
        int startX = centerX - totalWidth / 2;
        int topRowY = TOP_MARGIN + 10 + 30 + 25 + 20 + 10;

        // Check clicks on art boxes
        for (int i = 0; i < demonArts.length; i++) {
            String artId = demonArts[i];
            int row = i / cols;
            int col = i % cols;

            int x = startX + col * (boxWidth + spacing);
            int y = topRowY + row * (boxHeight + spacing);

            if (mouseX >= x && mouseX <= x + boxWidth && mouseY >= y && mouseY <= y + boxHeight) {
                return handleArtClick(player, artId, currentStyle, currentTime);
            }
        }

        // Check for "None" button click
        int lastRow = (demonArts.length - 1) / cols;
        int noneButtonY = topRowY + (lastRow + 1) * (boxHeight + spacing) + 20;
        int noneButtonX = centerX - 75;
        int noneButtonWidth = 150;
        int noneButtonHeight = 20;

        if (mouseX >= noneButtonX && mouseX <= noneButtonX + noneButtonWidth &&
                mouseY >= noneButtonY && mouseY <= noneButtonY + noneButtonHeight) {

            // Clear demon art
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
     * Handle clicking on a demon art
     */
    private boolean handleArtClick(Player player, String artName, String currentStyle, long currentTime) {
        // Check if the art is unlocked
        if (!isArtUnlocked(player, artName)) {
            // Art is locked - play error sound
            Minecraft.getInstance().getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.5F, 0.8F
                    )
            );
            lastClickTime = currentTime;
            return true;
        }

        // Art is unlocked - only set if not already selected
        if (!artName.equals(currentStyle)) {
            // Use packet to request style change from server
            NichirinPacketRegistry.requestStyleChange(artName);

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