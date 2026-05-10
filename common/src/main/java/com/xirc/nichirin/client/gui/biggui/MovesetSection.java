package com.xirc.nichirin.client.gui.biggui;

import com.xirc.nichirin.client.gui.NichirinPalette;
import com.xirc.nichirin.common.data.MovesetHelper;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Moveset section — hosts subtabs for Breathing Styles, Demon Arts, and Data.
 */
public class MovesetSection {

    private static final int TAB_WIDTH   = 120;
    private static final int TAB_HEIGHT  = 25;
    private static final int TAB_SPACING = 5;

    public enum MovesetTab {
        BREATHING_STYLES("gui.nichirin.moveset.tab.breathing_styles"),
        DEMON_ARTS("gui.nichirin.moveset.tab.demon_arts"),
        DATA("gui.nichirin.moveset.tab.data");

        private final String translationKey;
        MovesetTab(String key) { this.translationKey = key; }
        public String getTranslationKey() { return translationKey; }
    }

    private MovesetTab currentTab = MovesetTab.BREATHING_STYLES;

    private final BreathingStylesSection breathingStylesSection = new BreathingStylesSection();
    private final DemonArtSection        demonArtSection        = new DemonArtSection();
    private final MovesetDataSection     dataSection            = new MovesetDataSection();

    // Render
    public void render(GuiGraphics graphics, Player player, Font font,
                       int contentWidth, int contentHeight, int mouseX, int mouseY) {
        // Content first, tabs on top
        switch (currentTab) {
            case BREATHING_STYLES -> breathingStylesSection.render(
                    graphics, player, contentWidth, contentHeight, font, mouseX, mouseY);
            case DEMON_ARTS -> demonArtSection.render(
                    graphics, player, contentWidth, contentHeight, font, mouseX, mouseY);
            case DATA -> dataSection.render(
                    graphics, player, contentWidth, contentHeight, font, mouseX, mouseY);
        }

        renderSubtabs(graphics, font, tabsStartX(contentWidth), tabsY(player), mouseX, mouseY);
    }

    private void renderSubtabs(GuiGraphics graphics, Font font, int startX, int y, int mouseX, int mouseY) {
        int x = startX;
        for (MovesetTab tab : MovesetTab.values()) {
            boolean active  = tab == currentTab;
            boolean hovered = mouseX >= x && mouseX <= x + TAB_WIDTH
                           && mouseY >= y && mouseY <= y + TAB_HEIGHT;

            int bg     = active ? NichirinPalette.BG_BOX_ACTIVE
                       : hovered ? 0xFF2F2F2F : NichirinPalette.BG_BOX;
            int border = active ? NichirinPalette.BORDER_ACCENT
                       : hovered ? NichirinPalette.BORDER_HOVER : NichirinPalette.BORDER_DEFAULT;
            int text   = active ? NichirinPalette.TEXT_ACCENT
                       : hovered ? NichirinPalette.TEXT_WHITE : NichirinPalette.TEXT_MUTED;

            graphics.fill(x - 1, y - 1, x + TAB_WIDTH + 1, y + TAB_HEIGHT + 1, border);
            graphics.fill(x, y, x + TAB_WIDTH, y + TAB_HEIGHT, bg);

            Component label = Component.translatable(tab.getTranslationKey());
            graphics.drawString(font, label,
                    x + (TAB_WIDTH - font.width(label)) / 2,
                    y + (TAB_HEIGHT - font.lineHeight) / 2,
                    text);

            x += TAB_WIDTH + TAB_SPACING;
        }
    }

    // Click handling
    public boolean handleClick(double mouseX, double mouseY, Player player,
                               int contentWidth, int contentHeight) {
        int startX = tabsStartX(contentWidth);
        int y      = tabsY(player);
        int x      = startX;

        for (MovesetTab tab : MovesetTab.values()) {
            if (mouseX >= x && mouseX <= x + TAB_WIDTH && mouseY >= y && mouseY <= y + TAB_HEIGHT) {
                currentTab = tab;
                return true;
            }
            x += TAB_WIDTH + TAB_SPACING;
        }

        return switch (currentTab) {
            case BREATHING_STYLES -> breathingStylesSection.handleClick(mouseX, mouseY, player, contentWidth);
            case DEMON_ARTS       -> demonArtSection.handleClick(mouseX, mouseY, player, contentWidth);
            case DATA             -> dataSection.handleClick(mouseX, mouseY, player, contentWidth);
        };
    }

    // Position helpers — derived from BreathingStylesSection layout constants
    /** X coordinate where the subtab row starts (centred in content area). */
    private int tabsStartX(int contentWidth) {
        int totalTabWidth = TAB_WIDTH * MovesetTab.values().length
                + TAB_SPACING * (MovesetTab.values().length - 1);
        return (contentWidth - 20) / 2 - totalTabWidth / 2;
    }

    /**
     * Y coordinate for the subtab row — sits 40px below the "None" button,
     * which is itself derived from the breathing-styles grid height.
     * Uses the current player's style to match render() exactly.
     */
    private int tabsY(Player player) {
        String current = MovesetHelper.getMovesetId(player);
        return BreathingStylesSection.noneButtonBottomY(current) + 20;
    }

    // Legacy compatibility
    public void render(GuiGraphics graphics, Player player, Font font) {
        render(graphics, player, font, 800, 600, 0, 0);
    }

    public boolean handleClick(double mouseX, double mouseY, Player player) {
        return handleClick(mouseX, mouseY, player, 800, 600);
    }
}
