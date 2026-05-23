package com.xirc.nichirin.client.gui.biggui;

import com.xirc.nichirin.client.data.ClientPerkCache;
import com.xirc.nichirin.client.gui.biggui.skills.AbilitiesTab;
import com.xirc.nichirin.client.gui.biggui.skills.BloodlinesTab;
import com.xirc.nichirin.client.gui.biggui.skills.PerksTab;
import com.xirc.nichirin.common.system.perks.NichirinPerkRegistry;
import com.xirc.nichirin.common.system.perks.PerkData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Skills section shell with a persistent header, sub-tab bar, and status strip.
 */
public class SkillsSection extends AbstractGuiPage {

    private static final int HEADER_H = 64;
    private static final int FOOTER_H = 28;
    private static final int DIVIDER = COLOR_PALETTE.BORDER.argb();
    private static final int BG = COLOR_PALETTE.BG.argb();
    private static final int PANEL = COLOR_PALETTE.PANEL.argb();
    private static final int PANEL_HOVER = COLOR_PALETTE.PANEL_HOVER.argb();
    private static final int TEXT = COLOR_PALETTE.TEXT.rgb();
    private static final int TEXT_DIM = COLOR_PALETTE.TEXT_DIM.rgb();
    private static final int ACCENT = COLOR_PALETTE.ACCENT.argb();
    private static final int GREEN = COLOR_PALETTE.GREEN.argb();

    public enum SkillsTab {
        PERKS("Perks"),
        ABILITIES("Abilities"),
        BLOODLINES("Bloodlines");

        public final String label;

        SkillsTab(String label) {
            this.label = label;
        }
    }

    private SkillsTab currentTab = SkillsTab.PERKS;

    private final PerksTab perksTab = new PerksTab();
    private final AbilitiesTab abilitiesTab = new AbilitiesTab();
    private final BloodlinesTab bloodlinesTab = new BloodlinesTab();

    public void render(GuiGraphics graphics, Player player, Font font, int contentWidth, int contentHeight, int mouseX, int mouseY) {
        PerkData data = ClientPerkCache.get();
        graphics.fill(0, 0, contentWidth, contentHeight, BG);
        renderHeader(graphics, player, font, data, contentWidth, mouseX, mouseY);

        int bodyY = HEADER_H;
        int bodyH = Math.max(0, contentHeight - HEADER_H - FOOTER_H);
        graphics.pose().pushPose();
        graphics.pose().translate(0, bodyY, 0);
        int adjMouseY = mouseY - bodyY;

        switch (currentTab) {
            case PERKS -> perksTab.render(graphics, player, font, contentWidth, bodyH, mouseX, adjMouseY);
            case ABILITIES -> renderPlaceholder(graphics, font, contentWidth, bodyH, "Abilities", "Slayer Mark, Red Blade, and awakened techniques will appear here.");
            case BLOODLINES -> renderPlaceholder(graphics, font, contentWidth, bodyH, "Bloodlines", "Rare bloodline traits and passive bonuses will appear here.");
        }

        graphics.pose().popPose();
        renderFooter(graphics, font, data, contentWidth, contentHeight);
    }

    /** Legacy overload called by older section switches. */
    public void render(GuiGraphics graphics, Player player, Font font) {
        render(graphics, player, font, 320, 200, -1, -1);
    }

    public boolean handleClick(double mouseX, double mouseY, Player player, int contentWidth, int contentHeight) {
        if (mouseY >= 0 && mouseY < HEADER_H) {
            int tabW = 86;
            int gap = 8;
            int totalW = SkillsTab.values().length * tabW + (SkillsTab.values().length - 1) * gap;
            int x = Math.max(154, (contentWidth - totalW) / 2);
            int y = 22;
            for (SkillsTab tab : SkillsTab.values()) {
                if (isIn(mouseX, mouseY, x, y, tabW, 24)) {
                    currentTab = tab;
                    return true;
                }
                x += tabW + gap;
            }
            return false;
        }

        int bodyY = HEADER_H;
        int bodyH = Math.max(0, contentHeight - HEADER_H - FOOTER_H);
        if (mouseY >= bodyY && mouseY < bodyY + bodyH) {
            double adjY = mouseY - bodyY;
            return switch (currentTab) {
                case PERKS -> perksTab.handleClick(mouseX, adjY, player);
                case ABILITIES -> abilitiesTab.handleClick(mouseX, adjY, player);
                case BLOODLINES -> bloodlinesTab.handleClick(mouseX, adjY, player);
            };
        }

        return false;
    }

    /** Legacy overload. */
    public boolean handleClick(double mouseX, double mouseY, Player player) {
        return handleClick(mouseX, mouseY, player, 320, 200);
    }

    public boolean handleKeyTyped(char c, int keyCode) {
        return currentTab == SkillsTab.PERKS && perksTab.handleKeyTyped(c, keyCode);
    }

    public boolean handleScroll(double mx, double my, double delta) {
        if (currentTab != SkillsTab.PERKS || my < HEADER_H) return false;
        return perksTab.handleScroll(mx, my - HEADER_H, delta);
    }

    private void renderHeader(GuiGraphics g, Player player, Font font, PerkData data, int w, int mx, int my) {
        g.fill(0, 0, w, HEADER_H, PANEL);

        int nameX = 16;
        int nameY = 9;
        String name = player != null ? player.getName().getString() : "Unknown Slayer";
        g.drawString(font, trimToWidth(font, name, 120), nameX, nameY, TEXT, false);

        int tabW = 86;
        int gap = 8;
        int totalW = SkillsTab.values().length * tabW + (SkillsTab.values().length - 1) * gap;
        int x = Math.max(154, (w - totalW) / 2);
        int y = 22;
        for (SkillsTab tab : SkillsTab.values()) {
            boolean active = tab == currentTab;
            boolean hovered = !active && isIn(mx, my, x, y, tabW, 24);
            g.fill(x, y, x + tabW, y + 24, hovered ? PANEL_HOVER : PANEL);
            int tc = active ? TEXT : TEXT_DIM;
            g.drawString(font, tab.label, x + (tabW - font.width(tab.label)) / 2, y + 7, tc, false);
            if (active) {
                g.fill(x + 12, y + 22, x + tabW - 12, y + 24, ACCENT);
            }
            x += tabW + gap;
        }

        int statX = Math.max(4, w - 116);
        int statY = 10;
        int slots = data.getPerkSlots();
        int maxSlots = 5;
        String slotText = "Slots " + data.equippedCount() + "/" + slots;
        g.drawString(font, slotText, statX, statY, TEXT, false);
        if (slots < maxSlots) {
            String unlockHint = "(" + (maxSlots - slots) + " more unlockable)";
            g.drawString(font, unlockHint, statX, statY + 8, TEXT_DIM, false);
        }
        g.drawString(font, "Perks " + data.getDiscoveredIds().size() + "/" + NichirinPerkRegistry.allPerks().size(), statX, statY + 18, TEXT, false);
        g.drawString(font, "Flaws " + data.equippedFlawCount(), statX, statY + 34, TEXT, false);

        g.fill(0, HEADER_H - 1, w, HEADER_H, DIVIDER);
    }

    private void renderFooter(GuiGraphics g, Font font, PerkData data, int w, int h) {
        int y = h - FOOTER_H;
        g.fill(0, y, w, h, PANEL);
        g.fill(0, y, w, y + 1, DIVIDER);

        int x = 14;
        x = drawHint(g, font, x, y + 8, "E equip");
        x = drawHint(g, font, x + 5, y + 8, "U upgrade");
        x = drawHint(g, font, x + 5, y + 8, "F flaws");
        drawHint(g, font, x + 5, y + 8, "Esc close");

        String build = data.isPerksEnabled() ? "perks enabled" : "perks disabled";
        int dotX = w - 18;
        g.fill(dotX, y + 12, dotX + 5, y + 17, GREEN);
        g.drawString(font, build, dotX - 8 - font.width(build), y + 9, TEXT_DIM, false);
    }

    private void renderPlaceholder(GuiGraphics graphics, Font font, int w, int h, String title, String body) {
        graphics.fill(0, 0, w, h, BG);
        int x = w / 2;
        int y = h / 2 - 14;
        graphics.drawString(font, title, x - font.width(title) / 2, y, TEXT, false);
        graphics.drawString(font, body, x - font.width(body) / 2, y + 16, TEXT_DIM, false);
    }

    private int drawHint(GuiGraphics g, Font font, int x, int y, String label) {
        int w = font.width(label) + 10;
        g.fill(x, y - 3, x + w, y + 10, COLOR_PALETTE.HINT_BG.argb());
        g.fill(x, y - 3, x + w, y - 2, COLOR_PALETTE.HINT_TOP.argb());
        g.drawString(font, label, x + 5, y, TEXT_DIM, false);
        return x + w;
    }

    private void drawPill(GuiGraphics g, Font font, int x, int y, int w, int h, String label, int border, int text) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, border);
        g.fill(x, y, x + w, y + h, COLOR_PALETTE.PILL_BG.argb());
        g.drawString(font, label, x + (w - font.width(label)) / 2, y + 3, text, false);
    }

    private String trimToWidth(Font font, String text, int maxW) {
        if (font.width(text) <= maxW) return text;
        return font.plainSubstrByWidth(text, maxW - font.width("...")) + "...";
    }

    private static boolean isIn(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
