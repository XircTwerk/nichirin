package com.xirc.nichirin.client.gui.biggui;

import com.xirc.nichirin.client.gui.biggui.skills.AbilitiesTab;
import com.xirc.nichirin.client.gui.biggui.skills.BloodlinesTab;
import com.xirc.nichirin.client.gui.biggui.skills.PerksTab;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Skills section with three sub-tabs: Perks, Abilities, Bloodlines.
 */
public class SkillsSection {

    private static final int TAB_BTN_W = 80;
    private static final int TAB_BTN_H = 16;
    private static final int TAB_SPACING = 4;
    private static final int TAB_Y = 6;

    public enum SkillsTab {
        PERKS("Perks"),
        ABILITIES("Abilities"),
        BLOODLINES("Bloodlines");

        public final String label;
        SkillsTab(String label) { this.label = label; }
    }

    private SkillsTab currentTab = SkillsTab.PERKS;

    private final PerksTab      perksTab      = new PerksTab();
    private final AbilitiesTab  abilitiesTab  = new AbilitiesTab();
    private final BloodlinesTab bloodlinesTab = new BloodlinesTab();

    // ── Render ────────────────────────────────────────────────────────────────

    public void render(GuiGraphics graphics, Player player, Font font, int contentWidth, int contentHeight, int mouseX, int mouseY) {
        renderTabBar(graphics, font, mouseX, mouseY);

        int contentY = TAB_Y + TAB_BTN_H + 4;
        graphics.pose().pushPose();
        graphics.pose().translate(0, contentY, 0);

        int adjMouseY = mouseY - contentY;
        switch (currentTab) {
            case PERKS      -> perksTab.render(graphics, player, font, contentWidth, contentHeight - contentY, mouseX, adjMouseY);
            case ABILITIES  -> abilitiesTab.render(graphics, player, font, contentWidth, contentHeight - contentY, mouseX, adjMouseY);
            case BLOODLINES -> bloodlinesTab.render(graphics, player, font, contentWidth, contentHeight - contentY, mouseX, adjMouseY);
        }

        graphics.pose().popPose();
    }

    /** Legacy overload — called by TheBigGui switch that does not yet pass dimensions. */
    public void render(GuiGraphics graphics, Player player, Font font) {
        render(graphics, player, font, 320, 200, -1, -1);
    }

    // ── Click ─────────────────────────────────────────────────────────────────

    public boolean handleClick(double mouseX, double mouseY, Player player, int contentWidth, int contentHeight) {
        // Tab bar
        int x = 8;
        for (SkillsTab tab : SkillsTab.values()) {
            if (isIn(mouseX, mouseY, x, TAB_Y, TAB_BTN_W, TAB_BTN_H)) {
                currentTab = tab;
                return true;
            }
            x += TAB_BTN_W + TAB_SPACING;
        }

        // Active tab content (adjust Y for tab bar)
        int contentY = TAB_Y + TAB_BTN_H + 4;
        double adjY  = mouseY - contentY;
        return switch (currentTab) {
            case PERKS      -> perksTab.handleClick(mouseX, adjY, player);
            case ABILITIES  -> abilitiesTab.handleClick(mouseX, adjY, player);
            case BLOODLINES -> bloodlinesTab.handleClick(mouseX, adjY, player);
        };
    }

    /** Legacy overload. */
    public boolean handleClick(double mouseX, double mouseY, Player player) {
        return handleClick(mouseX, mouseY, player, 320, 200);
    }

    public boolean handleKeyTyped(char c, int keyCode) {
        return currentTab == SkillsTab.PERKS && perksTab.handleKeyTyped(c, keyCode);
    }

    public boolean handleScroll(double mx, double my, double delta) {
        return currentTab == SkillsTab.PERKS && perksTab.handleScroll(delta);
    }

    // ── Tab bar ───────────────────────────────────────────────────────────────

    private void renderTabBar(GuiGraphics g, Font font, int mx, int my) {
        int x = 8;
        for (SkillsTab tab : SkillsTab.values()) {
            boolean active  = tab == currentTab;
            boolean hovered = !active && isIn(mx, my, x, TAB_Y, TAB_BTN_W, TAB_BTN_H);

            int border = active ? 0xFF4A4A4A : (hovered ? 0xFF404040 : 0xFF303030);
            int bg     = active ? 0xFF2C2C2C : (hovered ? 0xFF282828 : 0xFF1E1E1E);
            int tc     = active ? 0xFFFFFFFF : (hovered ? 0xFFDDDDDD : 0xFF888888);

            g.fill(x - 1, TAB_Y - 1, x + TAB_BTN_W + 1, TAB_Y + TAB_BTN_H + 1, border);
            g.fill(x,     TAB_Y,     x + TAB_BTN_W,     TAB_Y + TAB_BTN_H,     bg);
            if (active) g.fill(x, TAB_Y, x + TAB_BTN_W, TAB_Y + 2, 0xFF666666);

            Component label = Component.literal(tab.label);
            int tx = x + (TAB_BTN_W - font.width(label)) / 2;
            int ty = TAB_Y + (TAB_BTN_H - font.lineHeight) / 2;
            g.drawString(font, label, tx, ty, tc, false);

            x += TAB_BTN_W + TAB_SPACING;
        }
    }

    private static boolean isIn(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
