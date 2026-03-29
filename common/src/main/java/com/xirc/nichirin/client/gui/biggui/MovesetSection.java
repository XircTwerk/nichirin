package com.xirc.nichirin.client.gui.biggui;

import com.xirc.nichirin.common.data.MovesetHelper;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Moveset section with subtabs for breathing styles, demon arts, and data
 */
public class MovesetSection {

    private static final int TOP_MARGIN = 20;
    private static final int TAB_BUTTON_WIDTH = 120;
    private static final int TAB_BUTTON_HEIGHT = 25;
    private static final int TAB_SPACING = 5;

    // Subtab enum
    public enum MovesetTab {
        BREATHING_STYLES("gui.nichirin.moveset.tab.breathing_styles"),
        DEMON_ARTS("gui.nichirin.moveset.tab.demon_arts"),
        DATA("gui.nichirin.moveset.tab.data");

        private final String translationKey;

        MovesetTab(String translationKey) {
            this.translationKey = translationKey;
        }

        public String getTranslationKey() {
            return translationKey;
        }
    }

    // Current active tab
    private MovesetTab currentTab = MovesetTab.BREATHING_STYLES;

    // Section instances
    private final BreathingStylesSection breathingStylesSection = new BreathingStylesSection();
    private final DemonArtSection demonArtSection = new DemonArtSection();
    private final MovesetDataSection dataSection = new MovesetDataSection();

    public void render(GuiGraphics graphics, Player player, Font font, int contentWidth, int contentHeight, int mouseX, int mouseY) {
        // Render current subtab content FIRST
        switch (currentTab) {
            case BREATHING_STYLES -> breathingStylesSection.render(graphics, player, contentWidth, contentHeight, font, mouseX, mouseY);
            case DEMON_ARTS -> demonArtSection.render(graphics, player, contentWidth, contentHeight, font, mouseX, mouseY);
            case DATA -> dataSection.render(graphics, player, contentWidth, contentHeight, font, mouseX, mouseY);
        }

        // Render subtab buttons AFTER content (below the "None" button)
        // Calculate position based on breathing styles layout
        int centerX = (contentWidth - 20) / 2;
        int breathingStylesCount = 5; // thunder, flame, insect, sound, water
        int cols = Math.min(2, breathingStylesCount);
        int lastRow = (breathingStylesCount - 1) / cols;
        int boxHeight = 80;
        int spacing = 20;
        int gridStartY = TOP_MARGIN + 10 + 30 + 25 + 20 + 10; // Same as breathing styles
        int noneButtonY = gridStartY + (lastRow + 1) * (boxHeight + spacing) + 20;
        int tabsY = noneButtonY + 40; // Below the "None" button

        renderSubtabs(graphics, font, centerX - (getTotalTabWidth() / 2), tabsY, mouseX, mouseY);
    }

    private int getTotalTabWidth() {
        return (TAB_BUTTON_WIDTH * MovesetTab.values().length) + (TAB_SPACING * (MovesetTab.values().length - 1));
    }

    private int renderSubtabs(GuiGraphics graphics, Font font, int startX, int startY, int mouseX, int mouseY) {
        int currentX = startX;

        for (MovesetTab tab : MovesetTab.values()) {
            boolean isActive = (tab == currentTab);
            boolean isHovered = mouseX >= currentX && mouseX <= currentX + TAB_BUTTON_WIDTH &&
                    mouseY >= startY && mouseY <= startY + TAB_BUTTON_HEIGHT;

            // Colors
            int bgColor = isActive ? 0xFF3A3A3A : (isHovered ? 0xFF2F2F2F : 0xFF2A2A2A);
            int borderColor = isActive ? 0xFF55FFFF : (isHovered ? 0xFF4A4A4A : 0xFF3A3A3A);
            int textColor = isActive ? 0x55FFFF : (isHovered ? 0xFFFFFF : 0xAAAAAA);

            // Draw tab button
            graphics.fill(currentX - 1, startY - 1, currentX + TAB_BUTTON_WIDTH + 1, startY + TAB_BUTTON_HEIGHT + 1, borderColor);
            graphics.fill(currentX, startY, currentX + TAB_BUTTON_WIDTH, startY + TAB_BUTTON_HEIGHT, bgColor);

            // Draw tab text
            Component tabText = Component.translatable(tab.getTranslationKey());
            int textX = currentX + (TAB_BUTTON_WIDTH - font.width(tabText)) / 2;
            int textY = startY + (TAB_BUTTON_HEIGHT - font.lineHeight) / 2;
            graphics.drawString(font, tabText, textX, textY, textColor);

            currentX += TAB_BUTTON_WIDTH + TAB_SPACING;
        }

        return startY + TAB_BUTTON_HEIGHT;
    }

    public boolean handleClick(double mouseX, double mouseY, Player player, int contentWidth, int contentHeight) {
        // Calculate tab position (same as render method)
        int centerX = (contentWidth - 20) / 2;
        int breathingStylesCount = 5;
        int cols = Math.min(2, breathingStylesCount);
        int lastRow = (breathingStylesCount - 1) / cols;
        int boxHeight = 80;
        int spacing = 20;
        int gridStartY = TOP_MARGIN + 10 + 30 + 25 + 20 + 10;
        int noneButtonY = gridStartY + (lastRow + 1) * (boxHeight + spacing) + 20;
        int tabsY = noneButtonY + 40;

        // Check subtab clicks
        int tabStartX = centerX - (getTotalTabWidth() / 2);
        int currentX = tabStartX;
        for (MovesetTab tab : MovesetTab.values()) {
            if (mouseX >= currentX && mouseX <= currentX + TAB_BUTTON_WIDTH &&
                    mouseY >= tabsY && mouseY <= tabsY + TAB_BUTTON_HEIGHT) {
                currentTab = tab;
                return true;
            }
            currentX += TAB_BUTTON_WIDTH + TAB_SPACING;
        }

        // Handle clicks in current subtab content (pass through original mouse coordinates)
        return switch (currentTab) {
            case BREATHING_STYLES -> breathingStylesSection.handleClick(mouseX, mouseY, player, contentWidth);
            case DEMON_ARTS -> demonArtSection.handleClick(mouseX, mouseY, player, contentWidth);
            case DATA -> dataSection.handleClick(mouseX, mouseY, player, contentWidth);
        };
    }

    // Legacy method for compatibility
    public void render(GuiGraphics graphics, Player player, Font font) {
        render(graphics, player, font, 800, 600, 0, 0);
    }

    // Legacy method for compatibility
    public boolean handleClick(double mouseX, double mouseY, Player player) {
        return handleClick(mouseX, mouseY, player, 800, 600);
    }
}