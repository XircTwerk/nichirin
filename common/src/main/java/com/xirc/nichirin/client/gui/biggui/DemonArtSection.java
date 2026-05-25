package com.xirc.nichirin.client.gui.biggui;

import com.xirc.nichirin.client.data.ClientProgressionCache;
import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.data.ProgressionHelper;
import com.xirc.nichirin.registry.NichirinMovesetRegistry;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import static com.xirc.nichirin.common.data.ProgressionHelper.getUnlockRequirement;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/**
 * Demon Arts section that mirrors the breathing styles system but for demon arts
 */
public class DemonArtSection extends AbstractGuiPage {

    private static final int TOP_MARGIN = 20;

    public void render(GuiGraphics graphics, Player player, int contentWidth, int contentHeight, Font font, int mouseX, int mouseY) {
        int contentX = 20;
        int contentY = TOP_MARGIN + 10;
        int centerX = (contentWidth - 20) / 2;

        // Title
        Component title = Component.translatable("gui.nichirin.demon_arts.title").withStyle(style -> style.withBold(true));
        drawAccentTitle(graphics, font, title, centerX, contentY, COLOR_PALETTE.DANGER.argb());
        contentY += 30;

        // Current demon art - show if player has a demon moveset
        String currentStyle = MovesetHelper.getDemonMovesetId(player);
        if (currentStyle != null && isDemonArt(currentStyle)) {
            Component current = Component.translatable("gui.nichirin.demon_arts.current",
                            Component.literal(formatArtName(currentStyle)))
                    .withStyle(style -> style.withColor(COLOR_PALETTE.DANGER.rgb()));
            graphics.drawString(font, current, contentX, contentY, COLOR_PALETTE.DANGER.rgb());
            contentY += 25;
        }

        // Instructions
        Component instructions = Component.translatable("gui.nichirin.demon_arts.instructions");
        graphics.drawString(font, instructions, contentX, contentY, COLOR_PALETTE.GRAY.rgb());
        contentY += 20;

        // Show demon arts (if any additional ones exist beyond default)
        String[] additionalDemonArts = {
                // Future demon arts would go here
        };

        // Calculate layout
        int gridY = contentY + 10;
        int boxWidth = 140;
        int boxHeight = 80;
        int spacing = 20;

        // Check what's being hovered
        String hoveredLockedArt = null;

        // Render additional demon arts if any exist
        if (additionalDemonArts.length > 0) {
            int cols = Math.min(2, additionalDemonArts.length);
            int totalWidth = (boxWidth * cols) + (spacing * (cols - 1));
            int startX = centerX - totalWidth / 2;

            for (int i = 0; i < additionalDemonArts.length; i++) {
                String artId = additionalDemonArts[i];
                int row = i / cols;
                int col = i % cols;

                int x = startX + col * (boxWidth + spacing);
                int y = gridY + row * (boxHeight + spacing);

                if (mouseX >= x && mouseX <= x + boxWidth && mouseY >= y && mouseY <= y + boxHeight) {
                    if (!isArtUnlocked(player, artId)) {
                        hoveredLockedArt = artId;
                    }
                }

                renderDemonArtBox(graphics, font, player, currentStyle, artId, x, y, boxWidth, boxHeight);
            }

            gridY += ((additionalDemonArts.length - 1) / cols + 1) * (boxHeight + spacing);
        }

        // Show tooltip if hovering over locked art
        if (hoveredLockedArt != null) {
            String requirement = getUnlockRequirement(hoveredLockedArt);
            Component tooltip = Component.literal(requirement).withStyle(style -> style.withColor(COLOR_PALETTE.DANGER.rgb()).withBold(true));
            int tooltipY = 50;
            graphics.drawString(font, tooltip, centerX - font.width(tooltip) / 2, tooltipY, COLOR_PALETTE.DANGER.rgb());
        }

        // "None" button - gives default demon moveset
        int noneButtonY = gridY + 20;
        int noneButtonX = centerX - 75;
        int noneButtonWidth = 150;
        int noneButtonHeight = 20;

        // None button is selected when player has default_demon moveset
        boolean isNoneSelected = "default_demon".equals(currentStyle);
        int noneButtonBg = isNoneSelected ? COLOR_PALETTE.PANEL_HOVER.argb() : COLOR_PALETTE.PANEL_MID.argb();
        int noneButtonBorder = isNoneSelected ? COLOR_PALETTE.DANGER.argb() : COLOR_PALETTE.BORDER_HI.argb();

        if (isNoneSelected) {
            graphics.fill(noneButtonX - 3, noneButtonY - 3,
                    noneButtonX + noneButtonWidth + 3, noneButtonY + noneButtonHeight + 3, withAlpha(noneButtonBorder, 0x24));
        }
        graphics.fill(noneButtonX - 1, noneButtonY - 1,
                noneButtonX + noneButtonWidth + 1, noneButtonY + noneButtonHeight + 1, noneButtonBorder);
        graphics.fill(noneButtonX, noneButtonY,
                noneButtonX + noneButtonWidth, noneButtonY + noneButtonHeight, noneButtonBg);
        graphics.fill(noneButtonX, noneButtonY, noneButtonX + noneButtonWidth, noneButtonY + 2,
                withAlpha(noneButtonBorder, isNoneSelected ? 0xEE : 0x70));

        Component noneText = Component.literal("Basic Demon Arts");
        int noneTextColor = isNoneSelected ? COLOR_PALETTE.DANGER.rgb() : COLOR_PALETTE.GRAY.rgb();
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
            bgColor = COLOR_PALETTE.PANEL_DARK.argb();
            borderColor = COLOR_PALETTE.BORDER.argb();
        } else if (isSelected) {
            bgColor = COLOR_PALETTE.PANEL_HOVER.argb();
            borderColor = COLOR_PALETTE.DANGER.argb();
        } else {
            bgColor = COLOR_PALETTE.PANEL_MID.argb();
            borderColor = COLOR_PALETTE.BORDER_HI.argb();
        }

        // Border
        if (isSelected) {
            graphics.fill(x - 3, y - 3, x + width + 3, y + height + 3, withAlpha(borderColor, 0x24));
        }
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, borderColor);
        // Background
        graphics.fill(x, y, x + width, y + height, bgColor);
        if (isUnlocked) {
            graphics.fill(x, y, x + width, y + 2, withAlpha(borderColor, isSelected ? 0xEE : 0x80));
            graphics.fill(x, y, x + 2, y + height, withAlpha(borderColor, isSelected ? 0xDD : 0x55));
        }

        // Art name
        Component displayName = Component.literal(formatArtName(artName));
        int nameColor = isUnlocked ? COLOR_PALETTE.WHITE.rgb() : COLOR_PALETTE.TEXT_DIM.rgb();
        graphics.drawString(font, displayName,
                x + (width - font.width(displayName)) / 2,
                y + 10, nameColor);

        // Status
        if (!isUnlocked) {
            Component locked = Component.translatable("gui.nichirin.demon_arts.locked_status")
                    .withStyle(style -> style.withColor(COLOR_PALETTE.DANGER.rgb()));
            graphics.drawString(font, locked,
                    x + (width - font.width(locked)) / 2,
                    y + 30, COLOR_PALETTE.DANGER.rgb());
        } else if (isSelected) {
            Component equipped = Component.translatable("gui.nichirin.demon_arts.equipped")
                    .withStyle(style -> style.withColor(COLOR_PALETTE.DANGER.rgb()));
            graphics.drawString(font, equipped,
                    x + (width - font.width(equipped)) / 2,
                    y + 30, COLOR_PALETTE.DANGER.rgb());
        } else {
            Component clickToSelect = Component.translatable("gui.nichirin.demon_arts.click_to_select")
                    .withStyle(style -> style.withColor(COLOR_PALETTE.GRAY.rgb()));
            graphics.drawString(font, clickToSelect,
                    x + (width - font.width(clickToSelect)) / 2,
                    y + 30, COLOR_PALETTE.GRAY.rgb());
        }

        // Icon placeholder
        int iconColor = isUnlocked ? COLOR_PALETTE.PANEL_HOVER.argb() : COLOR_PALETTE.PANEL_MID.argb();
        graphics.fill(x + width / 2 - 17, y + 49, x + width / 2 + 17, y + 76,
                isUnlocked ? withAlpha(borderColor, 0xAA) : COLOR_PALETTE.BORDER.argb());
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
        if (!NichirinMovesetRegistry.isRegistered(artId)) {
            return false;
        }
        return ProgressionHelper.isStyleUnlocked(player, artId);
    }

    /**
     * Check if a moveset ID represents a demon art
     */
    private boolean isDemonArt(String movesetId) {
        if (movesetId == null) return false;
        // Check for demon moveset IDs
        return movesetId.equals("default_demon") || movesetId.contains("demon");
    }

    /**
     * Format art name
     */
    private String formatArtName(String artId) {
        if (artId.equals("default_demon")) {
            return "Demon Arts";
        }

        String[] parts = artId.split("_");
        StringBuilder formatted = new StringBuilder();
        for (String part : parts) {
            if (formatted.length() > 0) formatted.append(" ");
            formatted.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
        }
        return formatted.toString();
    }

    private long lastClickTime = 0;
    private static final long CLICK_COOLDOWN = 500; // 500ms cooldown

    public boolean handleClick(double mouseX, double mouseY, Player player, int contentWidth) {
        // Prevent click spam
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClickTime < CLICK_COOLDOWN) {
            return false;
        }

        String currentStyle = MovesetHelper.getDemonMovesetId(player);
        int centerX = (contentWidth - 20) / 2;

        // Additional demon arts (future ones beyond default)
        String[] additionalDemonArts = {
                // Future demon arts would go here
        };

        // Base Y matches render(): TOP_MARGIN+10 (30) + title (30) + instructions (20) + grid offset (10)
        // Only add 25 when the current-art line is drawn (same condition as render())
        int topRowY = TOP_MARGIN + 10 + 30 + 20 + 10;
        if (currentStyle != null && isDemonArt(currentStyle)) topRowY += 25;

        // Check clicks on additional demon art boxes (if any exist)
        if (additionalDemonArts.length > 0) {
            int boxWidth = 140;
            int boxHeight = 80;
            int spacing = 20;
            int cols = Math.min(2, additionalDemonArts.length);
            int totalWidth = (boxWidth * cols) + (spacing * (cols - 1));
            int startX = centerX - totalWidth / 2;

            for (int i = 0; i < additionalDemonArts.length; i++) {
                String artId = additionalDemonArts[i];
                int row = i / cols;
                int col = i % cols;

                int x = startX + col * (boxWidth + spacing);
                int y = topRowY + row * (boxHeight + spacing);

                if (mouseX >= x && mouseX <= x + boxWidth && mouseY >= y && mouseY <= y + boxHeight) {
                    return handleArtClick(player, artId, currentStyle, currentTime);
                }
            }

            topRowY += ((additionalDemonArts.length - 1) / cols + 1) * (boxHeight + spacing);
        }

        // Check for "None" button click - sets default_demon moveset
        int noneButtonY = topRowY + 20;
        int noneButtonX = centerX - 75;
        int noneButtonWidth = 150;
        int noneButtonHeight = 20;

        if (mouseX >= noneButtonX && mouseX <= noneButtonX + noneButtonWidth &&
                mouseY >= noneButtonY && mouseY <= noneButtonY + noneButtonHeight) {

            // Set default demon moveset (basic demon abilities)
            NichirinPacketRegistry.requestStyleChange("default_demon");

            // Play click sound
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(
                            SoundEvents.UI_BUTTON_CLICK.value(), 1.0F
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
                    SimpleSoundInstance.forUI(
                            SoundEvents.UI_BUTTON_CLICK.value(), 0.5F, 0.8F
                    )
            );
            lastClickTime = currentTime;
            return true;
        }

        // Art is unlocked - only set if not already selected
        if (!artName.equals(currentStyle)) {
            // Set demon art (becomes a demon)
            NichirinPacketRegistry.requestStyleChange(artName);

            // Play success sound
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(
                            SoundEvents.UI_BUTTON_CLICK.value(), 1.0F
                    )
            );
        }

        lastClickTime = currentTime;
        return true;
    }
}
