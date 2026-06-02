package com.xirc.nichirin.client.gui.biggui;

import com.xirc.nichirin.common.data.MovesetHelper;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Moveset section - hosts subtabs for Breathing Styles, Demon Arts, and Data.
 */
public class MovesetSection extends AbstractGuiPage {

    private static final int TAB_WIDTH = 120;
    private static final int TAB_HEIGHT = 25;
    private static final int TAB_SPACING = 5;

    public enum MovesetTab {
        BREATHING_STYLES("gui.nichirin.moveset.tab.breathing_styles"),
        DEMON_ARTS("gui.nichirin.moveset.tab.demon_arts"),
        OBTAINMENT("gui.nichirin.moveset.tab.obtainment"),
        DATA("gui.nichirin.moveset.tab.data");

        private final String translationKey;

        MovesetTab(String key) {
            this.translationKey = key;
        }

        public String getTranslationKey() {
            return translationKey;
        }
    }

    private MovesetTab currentTab = MovesetTab.BREATHING_STYLES;

    private final BreathingStylesSection breathingStylesSection = new BreathingStylesSection();
    private final DemonArtSection demonArtSection = new DemonArtSection();
    private final MovesetDataSection dataSection = new MovesetDataSection();
    private final ObtainmentSection obtainmentSection = new ObtainmentSection();
    // SheathingSection removed — the sheathing GUI page exposed only default-static info
    // that the user never wanted to tune from here. The sheathing system itself is unaffected.

    // Render
    public void render(GuiGraphics graphics, Player player, Font font,
                       int contentWidth, int contentHeight, int mouseX, int mouseY) {
        drawWorkspaceChrome(graphics, font, contentWidth, contentHeight,
                "Moveset", "Breathing styles, demon arts, and move data.",
                currentTabLabel(), "", "");
        renderSubtabs(graphics, font, tabsStartX(contentWidth), 22, mouseX, mouseY);

        int bodyY = workspaceBodyY();
        int bodyH = workspaceBodyHeight(contentHeight);
        int bodyMouseY = mouseY - bodyY;
        graphics.pose().pushPose();
        graphics.pose().translate(0, bodyY, 0);
        switch (currentTab) {
            case BREATHING_STYLES -> breathingStylesSection.render(
                    graphics, player, contentWidth, bodyH, font, mouseX, bodyMouseY);
            case DEMON_ARTS -> demonArtSection.render(
                    graphics, player, contentWidth, bodyH, font, mouseX, bodyMouseY);
            case OBTAINMENT -> obtainmentSection.render(
                    graphics, player, font, contentWidth, bodyH, mouseX, bodyMouseY);
            case DATA -> dataSection.render(
                    graphics, player, contentWidth, bodyH, font, mouseX, bodyMouseY);
        }
        graphics.pose().popPose();
    }

    private void renderSubtabs(GuiGraphics graphics, Font font, int startX, int y, int mouseX, int mouseY) {
        int x = startX;
        for (MovesetTab tab : MovesetTab.values()) {
            boolean active = tab == currentTab;
            boolean hovered = mouseX >= x && mouseX <= x + TAB_WIDTH
                    && mouseY >= y && mouseY <= y + TAB_HEIGHT;

            int bg = active ? COLOR_PALETTE.PILL_BG.argb()
                    : hovered ? COLOR_PALETTE.PANEL_HOVER.argb() : COLOR_PALETTE.PANEL_MID.argb();
            int border = active ? COLOR_PALETTE.ACCENT.argb()
                    : hovered ? COLOR_PALETTE.BORDER_HI.argb() : COLOR_PALETTE.BORDER.argb();
            int text = active ? COLOR_PALETTE.ACCENT_LIGHT.rgb()
                    : hovered ? COLOR_PALETTE.TEXT.rgb() : COLOR_PALETTE.TEXT_DIM.rgb();

            if (active) {
                graphics.fill(x - 3, y - 3, x + TAB_WIDTH + 3, y + TAB_HEIGHT + 3, withAlpha(border, 0x22));
            }
            graphics.fill(x - 1, y - 1, x + TAB_WIDTH + 1, y + TAB_HEIGHT + 1, border);
            graphics.fill(x, y, x + TAB_WIDTH, y + TAB_HEIGHT, bg);
            if (active || hovered) {
                graphics.fill(x, y, x + TAB_WIDTH, y + 2, border);
            }

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
        int y = 22;
        int x = startX;

        for (MovesetTab tab : MovesetTab.values()) {
            if (mouseX >= x && mouseX <= x + TAB_WIDTH && mouseY >= y && mouseY <= y + TAB_HEIGHT) {
                currentTab = tab;
                return true;
            }
            x += TAB_WIDTH + TAB_SPACING;
        }

        int bodyY = workspaceBodyY();
        if (mouseY < bodyY || mouseY >= contentHeight - WORKSPACE_FOOTER_H) return false;
        double bodyMouseY = mouseY - bodyY;
        return switch (currentTab) {
            case BREATHING_STYLES -> breathingStylesSection.handleClick(mouseX, bodyMouseY, player, contentWidth);
            case DEMON_ARTS -> demonArtSection.handleClick(mouseX, bodyMouseY, player, contentWidth);
            case OBTAINMENT -> obtainmentSection.handleClick(mouseX, bodyMouseY, player);
            case DATA -> dataSection.handleClick(mouseX, bodyMouseY, player, contentWidth);
        };
    }

    // Position helpers - derived from BreathingStylesSection layout constants
    /** X coordinate where the subtab row starts (centred in content area). */
    private int tabsStartX(int contentWidth) {
        int totalTabWidth = TAB_WIDTH * MovesetTab.values().length
                + TAB_SPACING * (MovesetTab.values().length - 1);
        return (contentWidth - 20) / 2 - totalTabWidth / 2;
    }

    /**
     * Y coordinate for the subtab row - sits 40px below the "None" button,
     * which is itself derived from the breathing-styles grid height.
     * Uses the current player's style to match render() exactly.
     */
    private int tabsY(Player player) {
        String current = MovesetHelper.getBreathingMovesetId(player);
        return BreathingStylesSection.noneButtonBottomY(current) + 20;
    }

    private String currentTabLabel() {
        return switch (currentTab) {
            case BREATHING_STYLES -> "Breathing Styles";
            case DEMON_ARTS -> "Demon Arts";
            case OBTAINMENT -> "Obtainment";
            case DATA -> "Move Data";
        };
    }

    // Legacy compatibility
    public void render(GuiGraphics graphics, Player player, Font font) {
        render(graphics, player, font, 800, 600, 0, 0);
    }

    public boolean handleClick(double mouseX, double mouseY, Player player) {
        return handleClick(mouseX, mouseY, player, 800, 600);
    }
}