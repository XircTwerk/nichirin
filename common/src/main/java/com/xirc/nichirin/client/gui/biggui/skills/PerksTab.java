package com.xirc.nichirin.client.gui.biggui.skills;

import com.mojang.blaze3d.systems.RenderSystem;
import com.xirc.nichirin.client.data.ClientPerkCache;
import com.xirc.nichirin.client.gui.PerkIcon;
import com.xirc.nichirin.client.gui.biggui.AbstractGuiPage;
import com.xirc.nichirin.common.network.c2s.PerkActionPacket;
import com.xirc.nichirin.common.system.perks.FlawDefinition;
import com.xirc.nichirin.common.system.perks.NichirinPerkRegistry;
import com.xirc.nichirin.common.system.perks.PerkData;
import com.xirc.nichirin.common.system.perks.PerkDefinition;
import com.xirc.nichirin.common.system.perks.PerkTag;
import com.xirc.nichirin.common.system.perks.PerkTier;
import com.xirc.nichirin.common.system.perks.PerkUpgradeCost;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Perks tab rendered as a three-column workspace.
 */
public class PerksTab extends AbstractGuiPage {

    private static final int PAD = 16;
    private static final int GAP = 14;
    private static final int LEFT_W = 200;
    private static final int RIGHT_W = 340;
    private static final int MIN_MID_W = 280;
    private static final int SUBHEADER_H = 46;
    private static final int ROW_H = 70;
    private static final int DETAIL_FOOTER_H = 36;

    private static final int BG = COLOR_PALETTE.BG.argb();
    private static final int PANEL = COLOR_PALETTE.PANEL.argb();
    private static final int PANEL_2 = COLOR_PALETTE.PANEL_MID.argb();
    private static final int PANEL_3 = COLOR_PALETTE.PANEL_HOVER.argb();
    private static final int BORDER = COLOR_PALETTE.BORDER.argb();
    private static final int BORDER_HI = COLOR_PALETTE.BORDER_HI.argb();
    private static final int TEXT = COLOR_PALETTE.TEXT.rgb();
    private static final int TEXT_DIM = COLOR_PALETTE.TEXT_DIM.rgb();
    private static final int TEXT_FAINT = COLOR_PALETTE.TEXT_FAINT.rgb();
    private static final int ACCENT = COLOR_PALETTE.ACCENT.argb();
    private static final int RED = COLOR_PALETTE.RED.argb();
    private static final int GREEN = COLOR_PALETTE.GREEN.argb();

    private Category activeCategory = Category.ALL;
    private SortMode sort = SortMode.TIER;
    private FilterMode filter = FilterMode.ALL;
    private boolean searchActive;
    private String searchText = "";
    private String selectedId;
    private int activePresetIndex;

    private int categoryScroll;
    private int listScroll;
    private int detailScroll;

    private int cachedW;
    private int cachedH;
    private int leftW;
    private int middleW;
    private int rightW;
    private int middleX;
    private int rightX;
    private int listViewportY;
    private int listViewportH;
    private int detailMaxScroll;

    private enum SortMode {
        TIER("Tier"),
        NAME("Name"),
        TAG("Tag");

        final String label;

        SortMode(String label) {
            this.label = label;
        }

        SortMode next() {
            SortMode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    private enum FilterMode {
        ALL("All"),
        EQUIPPED("Equipped");

        final String label;

        FilterMode(String label) {
            this.label = label;
        }

        FilterMode next() {
            FilterMode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    private enum Category {
        ALL("A", "All Perks", null, false, false),
        BREATH("B", "Breath", PerkTag.BREATH, false, false),
        COMBAT("C", "Combat", PerkTag.COMBAT, false, false),
        MOVEMENT("M", "Movement", PerkTag.MOVEMENT, false, false),
        DEFENSE("D", "Defense", PerkTag.DEFENSE, false, false),
        SURVIVAL("S", "Survival", PerkTag.SURVIVAL, false, false),
        SPECIAL("*", "Special", PerkTag.SPECIAL, false, false),
        DEMON("X", "Demon", PerkTag.DEMON, false, false),
        FLAWS("F", "Flaws", null, true, false),
        UNDISCOVERED("?", "Undiscovered", null, false, true);

        final String glyph;
        final String label;
        final PerkTag tag;
        final boolean flaws;
        final boolean locked;

        Category(String glyph, String label, PerkTag tag, boolean flaws, boolean locked) {
            this.glyph = glyph;
            this.label = label;
            this.tag = tag;
            this.flaws = flaws;
            this.locked = locked;
        }
    }

    private static class RowEntry {
        final PerkDefinition perk;
        final FlawDefinition flaw;
        final boolean locked;

        RowEntry(PerkDefinition perk, boolean locked) {
            this.perk = perk;
            this.flaw = null;
            this.locked = locked;
        }

        RowEntry(FlawDefinition flaw) {
            this.perk = null;
            this.flaw = flaw;
            this.locked = false;
        }

        String id() {
            return flaw != null ? "flaw:" + flaw.id : perk.id;
        }
    }

    public void render(GuiGraphics g, Player player, Font font, int cw, int ch, int mx, int my) {
        cachedW = cw;
        cachedH = ch;
        computeColumns(cw);
        PerkData data = ClientPerkCache.get();

        g.fill(0, 0, cw, ch, BG);
        g.fill(leftW, 0, leftW + 1, ch, BORDER);
        g.fill(rightX - 1, 0, rightX, ch, BORDER);

        renderCategoryRail(g, font, data, 0, 0, leftW, ch, mx, my);
        renderPerkList(g, font, data, middleX, 0, middleW, ch, mx, my);
        renderDetailRail(g, font, data, rightX, 0, rightW, ch, mx, my);
    }

    public boolean handleClick(double mx, double my, Player player) {
        PerkData data = ClientPerkCache.get();

        if (mx >= 0 && mx < leftW) {
            if (handleLeftClick(mx, my, data)) return true;
        }

        if (mx >= middleX && mx < rightX) {
            if (handleMiddleClick(mx, my, data)) return true;
        }

        if (mx >= rightX && mx < cachedW) {
            if (handleDetailClick(mx, my, data)) return true;
        }

        searchActive = false;
        return false;
    }

    public boolean handleKeyTyped(char c, int keyCode) {
        if (searchActive) {
            if (keyCode == 259) {
                if (!searchText.isEmpty()) searchText = searchText.substring(0, searchText.length() - 1);
            } else if (keyCode == 256) {
                searchActive = false;
            } else if (c >= 32 && c < 127 && searchText.length() < 28) {
                searchText += c;
            }
            listScroll = 0;
            return true;
        }

        if (keyCode == 70) {
            activeCategory = Category.FLAWS;
            listScroll = 0;
            return true;
        }

        if (selectedId != null && keyCode == 69) {
            PerkData data = ClientPerkCache.get();
            PerkDefinition def = selectedPerk();
            if (def != null && !data.isEquipped(def.id) && data.hasDiscovered(def.id)) {
                sendAction(new PerkActionPacket(PerkActionPacket.Action.EQUIP, def.id, def.minTier.level));
                return true;
            }
            FlawDefinition flaw = selectedFlaw();
            if (flaw != null && !data.hasFlaw(flaw.id)) {
                sendAction(new PerkActionPacket(PerkActionPacket.Action.EQUIP_FLAW, flaw.id));
                return true;
            }
        }

        if (selectedId != null && keyCode == 85) {
            PerkData data = ClientPerkCache.get();
            PerkDefinition def = selectedPerk();
            if (def != null && data.isEquipped(def.id)) {
                sendAction(new PerkActionPacket(PerkActionPacket.Action.UPGRADE, def.id));
                return true;
            }
        }

        return false;
    }

    public boolean handleScroll(double mx, double my, double delta) {
        int amount = (int) Math.signum(delta) * 22;
        if (mx < leftW) {
            categoryScroll = Math.max(0, categoryScroll - amount);
            return true;
        }
        if (mx >= rightX) {
            detailScroll = clamp(detailScroll - amount, 0, detailMaxScroll);
            return true;
        }
        listScroll = Math.max(0, listScroll - amount);
        return true;
    }

    private void computeColumns(int cw) {
        leftW = Math.min(LEFT_W, Math.max(168, cw / 4));
        rightW = Math.min(RIGHT_W, Math.max(230, cw / 3));
        int availableMiddle = cw - leftW - rightW - 2;
        if (availableMiddle < MIN_MID_W) {
            int deficit = MIN_MID_W - availableMiddle;
            int rightReduce = Math.min(deficit, Math.max(0, rightW - 230));
            rightW -= rightReduce;
            deficit -= rightReduce;
            leftW -= Math.min(deficit, Math.max(0, leftW - 168));
        }
        middleX = leftW + 1;
        rightX = cw - rightW;
        middleW = Math.max(160, rightX - middleX - 1);
    }

    private void renderCategoryRail(GuiGraphics g, Font font, PerkData data, int x, int y, int w, int h, int mx, int my) {
        g.fill(x, y, x + w, y + h, COLOR_PALETTE.PANEL_DARK.argb());
        g.enableScissor(x, y, x + w, y + h);

        int cy = y + PAD - categoryScroll;
        cy = renderLoadoutCard(g, font, data, x + PAD, cy, w - PAD * 2, mx, my);
        cy += GAP;

        for (Category category : Category.values()) {
            if (category == Category.FLAWS) {
                g.fill(x + PAD, cy + 4, x + w - PAD, cy + 5, BORDER);
                cy += 13;
            }
            renderCategoryButton(g, font, data, category, x + PAD, cy, w - PAD * 2, mx, my);
            cy += 26;
        }

        g.disableScissor();
    }

    private int renderLoadoutCard(GuiGraphics g, Font font, PerkData data, int x, int y, int w, int mx, int my) {
        int h = 104;
        g.fill(x, y, x + w, y + h, PANEL);
        g.fill(x, y, x + w, y + 1, BORDER_HI);
        g.fill(x, y + h - 1, x + w, y + h, BORDER);

        String presetName = activePresetName(data);
        g.drawString(font, "Loadout", x + 9, y + 8, TEXT_DIM, false);
        g.drawString(font, trim(font, presetName, w - 18), x + 9, y + 20, TEXT, false);

        List<Map.Entry<String, PerkTier>> equipped = new ArrayList<>(data.getEquippedPerks().entrySet());
        int cellW = (w - 18 - 4 * 4) / 5;
        int sx = x + 9;
        int sy = y + 38;
        for (int i = 0; i < 5; i++) {
            int cx = sx + i * (cellW + 4);
            if (i >= data.getPerkSlots()) {
                drawRect(g, cx, sy, cellW, 16, BORDER, 0xFF0A0A0A);
                g.fill(cx, sy, cx + cellW, sy + 16, COLOR_PALETTE.BLACK.withAlpha(0x99));
            } else if (i < equipped.size()) {
                PerkTier tier = equipped.get(i).getValue();
                drawRect(g, cx, sy, cellW, 16, tierColor(tier), tierFill(tier, 0x46));
            } else {
                drawRect(g, cx, sy, cellW, 16, BORDER_HI, 0xFF111111);
            }
        }

        String stats = data.equippedCount() + "/" + data.getPerkSlots() + " . " + data.equippedFlawCount() + " flaw";
        g.drawString(font, stats, x + 9, y + 59, TEXT_DIM, false);

        int bw = (w - 18 - 2 * 5) / 3;
        int by = y + 77;
        for (int i = 0; i < 3; i++) {
            int bx = x + 9 + i * (bw + 5);
            boolean active = i == activePresetIndex;
            boolean hasPreset = i < data.getPresets().size();
            int border = active ? ACCENT : BORDER;
            int bg = active ? 0xFF2A2115 : PANEL_2;
            drawRect(g, bx, by, bw, 16, border, bg);
            String label = hasPreset ? trim(font, data.getPresets().get(i).name, bw - 8) : roman(i + 1);
            g.drawString(font, label, bx + (bw - font.width(label)) / 2, by + 4, active ? COLOR_PALETTE.ACCENT_LIGHT.rgb() : TEXT_DIM, false);
        }

        return y + h;
    }

    private void renderCategoryButton(GuiGraphics g, Font font, PerkData data, Category category, int x, int y, int w, int mx, int my) {
        boolean active = category == activeCategory;
        boolean hovered = inRect(mx, my, x, y, w, 20);
        int bg = active ? 0xFF211B14 : hovered ? PANEL_3 : 0xFF151515;
        g.fill(x, y, x + w, y + 20, bg);
        if (active) {
            g.fill(x, y, x + 2, y + 20, ACCENT);
            for (int i = 0; i < Math.min(56, w); i++) {
                g.fill(x + i, y, x + i + 1, y + 20, (int)(0x28 * (1.0 - i / 56.0)) << 24 | (ACCENT & 0xFFFFFF));
            }
        }
        g.drawString(font, category.glyph, x + 8, y + 6, active ? ACCENT : TEXT_DIM, false);
        g.drawString(font, category.label, x + 25, y + 6, active ? TEXT : TEXT_DIM, false);
        String count = categoryCount(data, category);
        g.drawString(font, count, x + w - 8 - font.width(count), y + 6, active ? TEXT : TEXT_FAINT, false);
    }

    private void renderPerkList(GuiGraphics g, Font font, PerkData data, int x, int y, int w, int h, int mx, int my) {
        g.fill(x, y, x + w, y + h, BG);
        renderListSubheader(g, font, data, x, y, w, mx, my);

        listViewportY = y + SUBHEADER_H;
        listViewportH = Math.max(0, h - SUBHEADER_H);
        List<RowEntry> rows = buildRows(data);
        int contentH = rows.size() * (ROW_H + 8);
        int maxScroll = Math.max(0, contentH - listViewportH + PAD);
        listScroll = clamp(listScroll, 0, maxScroll);

        g.enableScissor(x, listViewportY, x + w, y + h);
        int ry = listViewportY + PAD - listScroll;
        for (RowEntry row : rows) {
            if (ry + ROW_H >= listViewportY && ry <= y + h) {
                renderRow(g, font, data, row, x + PAD, ry, w - PAD * 2, ROW_H, mx, my);
            }
            ry += ROW_H + 8;
        }
        if (rows.isEmpty()) {
            String empty = searchText.isEmpty() ? "No perks in this category." : "No matches.";
            g.drawString(font, empty, x + (w - font.width(empty)) / 2, listViewportY + listViewportH / 2, TEXT_DIM, false);
        }
        g.disableScissor();

        if (maxScroll > 0) renderScrollBar(g, x + w - 4, listViewportY + 3, 2, listViewportH - 6, listScroll, maxScroll);
    }

    private void renderListSubheader(GuiGraphics g, Font font, PerkData data, int x, int y, int w, int mx, int my) {
        g.fill(x, y, x + w, y + SUBHEADER_H, COLOR_PALETTE.PANEL_DEEP.argb());
        g.fill(x, y + SUBHEADER_H - 1, x + w, y + SUBHEADER_H, BORDER);
        g.drawString(font, activeCategory.label, x + PAD, y + 13, TEXT, false);

        int searchW = Math.min(110, Math.max(70, w / 5));
        int searchX = x + w - PAD - searchW;
        int searchY = y + 12;
        boolean searchHover = inRect(mx, my, searchX, searchY, searchW, 17);
        drawRect(g, searchX, searchY, searchW, 17, searchActive ? ACCENT : BORDER, searchHover ? PANEL_3 : PANEL);
        String shown = searchText.isEmpty() && !searchActive ? "Search" : searchText + (searchActive ? "|" : "");
        g.drawString(font, trim(font, shown, searchW - 10), searchX + 5, searchY + 5, searchText.isEmpty() ? TEXT_FAINT : TEXT, false);

        String filterLabel = "Filter: " + filter.label + " v";
        int filterW = font.width(filterLabel) + 12;
        int filterX = searchX - 6 - filterW;
        drawRect(g, filterX, searchY, filterW, 17, BORDER, inRect(mx, my, filterX, searchY, filterW, 17) ? PANEL_3 : PANEL);
        g.drawString(font, filterLabel, filterX + 6, searchY + 5, TEXT_DIM, false);

        String sortLabel = "Sort: " + sort.label + " v";
        int sortW = font.width(sortLabel) + 12;
        int sortX = filterX - 6 - sortW;
        drawRect(g, sortX, searchY, sortW, 17, BORDER, inRect(mx, my, sortX, searchY, sortW, 17) ? PANEL_3 : PANEL);
        g.drawString(font, sortLabel, sortX + 6, searchY + 5, TEXT_DIM, false);
    }

    private void renderRow(GuiGraphics g, Font font, PerkData data, RowEntry row, int x, int y, int w, int h, int mx, int my) {
        boolean selected = row.id().equals(selectedId);
        boolean hovered = inRect(mx, my, x, y, w, h);
        int tier = row.locked ? COLOR_PALETTE.DARK_GRAY.argb() : row.flaw != null ? RED : tierColor(rowTier(data, row.perk));
        int border = selected ? tier : hovered ? dimColor(tier, 0xB0) : BORDER;
        int bg = selected ? 0xFF24211D : hovered ? PANEL_3 : PANEL_2;

        if (selected) g.fill(x - 3, y - 3, x + w + 3, y + h + 3, dimColor(tier, 0x30));
        g.fill(x, y, x + w, y + h, border);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, bg);
        g.fill(x, y, x + 2, y + h, tier);

        int iconX = x + 12;
        int iconY = y + 11;
        drawRect(g, iconX, iconY, 48, 48, row.locked ? BORDER : tier, row.locked ? COLOR_PALETTE.PANEL_DARK.argb() : PANEL);
        if (row.perk != null && !row.locked) {
            renderPerkIcon(g, row.perk.id, rowTier(data, row.perk), iconX + 8, iconY + 8);
        } else {
            g.drawString(font, row.flaw != null ? "!" : "?", iconX + 22, iconY + 20, row.flaw != null ? RED : TEXT_FAINT, false);
        }

        int mainX = iconX + 60;
        int rightColW = 82;
        int textW = w - (mainX - x) - rightColW - 8;
        String name = row.locked ? "???" : row.flaw != null ? row.flaw.name : row.perk.name;
        g.drawString(font, trim(font, name, textW), mainX, y + 10, row.locked ? TEXT_FAINT : TEXT, false);

        int badgeX = mainX;
        if (!row.locked && row.perk != null) {
            badgeX = drawBadge(g, font, badgeX, y + 25, rowTier(data, row.perk).displayName.toUpperCase(Locale.ROOT), tier, false);
            if (data.isEquipped(row.perk.id)) badgeX = drawBadge(g, font, badgeX + 4, y + 25, "EQUIPPED", GREEN, false);
        } else if (row.flaw != null) {
            badgeX = drawBadge(g, font, badgeX, y + 25, data.hasFlaw(row.flaw.id) ? "EQUIPPED" : "FLAW", RED, true);
        }

        String desc = row.locked ? "Undiscovered" : row.flaw != null ? row.flaw.description : row.perk.getDescriptionForTier(rowTier(data, row.perk));
        g.drawString(font, trim(font, desc, textW), mainX, y + 41, row.locked ? TEXT_FAINT : TEXT_DIM, false);

        if (!row.locked && row.perk != null) {
            int tx = mainX;
            for (PerkTag tag : row.perk.tags) {
                tx = drawChip(g, font, tx, y + 57, tag.displayName, tag.color);
                if (tx > x + w - rightColW - 20) break;
            }
        }

        renderTierDots(g, row, data, x + w - 66, y + 25);
    }

    private void renderTierDots(GuiGraphics g, RowEntry row, PerkData data, int x, int y) {
        for (int i = 0; i < PerkTier.values().length; i++) {
            PerkTier tier = PerkTier.fromLevel(i);
            int dx = x + i * 12;
            if (row.locked || row.flaw != null || !row.perk.hasTier(tier)) {
                g.fill(dx, y, dx + 6, y + 6, BORDER);
                g.fill(dx + 1, y + 1, dx + 5, y + 5, PANEL_2);
                continue;
            }
            PerkTier current = rowTier(data, row.perk);
            boolean owned = tier.level <= current.level;
            boolean active = tier.level == current.level;
            int color = tierColor(tier);
            g.fill(dx - (active ? 1 : 0), y - (active ? 1 : 0), dx + 6 + (active ? 1 : 0), y + 6 + (active ? 1 : 0), active ? dimColor(color, 0xA0) : BORDER);
            g.fill(dx + 1, y + 1, dx + 5, y + 5, owned ? color : PANEL_2);
        }
    }

    private void renderDetailRail(GuiGraphics g, Font font, PerkData data, int x, int y, int w, int h, int mx, int my) {
        g.fill(x, y, x + w, y + h, COLOR_PALETTE.PANEL_DARK.argb());
        PerkDefinition perk = selectedPerk();
        FlawDefinition flaw = selectedFlaw();

        if (perk == null && flaw == null) {
            String msg = "Select a perk to view details";
            g.drawString(font, msg, x + (w - font.width(msg)) / 2, y + h / 2, TEXT_DIM, false);
            return;
        }

        int strip = flaw != null ? RED : tierColor(rowTier(data, perk));
        g.fill(x, y, x + w, y + 3, strip);
        g.fill(x, y + 3, x + w, y + 8, dimColor(strip, 0x28));

        detailScroll = clamp(detailScroll, 0, detailMaxScroll);

        int contentBottom = y + h - DETAIL_FOOTER_H;
        g.enableScissor(x, y + 3, x + w, contentBottom);
        int cy = y + PAD - detailScroll;
        if (perk != null) {
            cy = renderPerkDetailContent(g, font, data, perk, x + PAD, cy, w - PAD * 2);
        } else {
            cy = renderFlawDetailContent(g, font, data, flaw, x + PAD, cy, w - PAD * 2);
        }
        g.disableScissor();

        detailMaxScroll = Math.max(0, cy + detailScroll - (contentBottom - PAD * 3));
        detailScroll = clamp(detailScroll, 0, detailMaxScroll);
        if (detailMaxScroll > 0) renderScrollBar(g, x + w - 4, y + 8, 2, h - DETAIL_FOOTER_H - 12, detailScroll, detailMaxScroll);

        renderActionFooter(g, font, data, perk, flaw, x, y + h - DETAIL_FOOTER_H, w, DETAIL_FOOTER_H, mx, my);
    }

    private int renderPerkDetailContent(GuiGraphics g, Font font, PerkData data, PerkDefinition def, int x, int y, int w) {
        PerkTier tier = rowTier(data, def);
        int color = tierColor(tier);

        g.drawString(font, trim(font, def.name, w), x, y, TEXT, false);
        y += 20;
        int tx = x;
        for (PerkTag tag : def.tags) tx = drawChip(g, font, tx, y, tag.displayName, tag.color);
        y += 22;

        int blockY = y;
        drawRect(g, x, blockY, 64, 64, color, PANEL_2);
        renderPerkIcon(g, def.id, tier, x + 16, blockY + 16);
        int ex = x + 76;
        g.drawString(font, tier.displayName.toUpperCase(Locale.ROOT), ex, blockY + 3, color, false);
        for (String line : wordWrap(font, def.getDescriptionForTier(tier), w - 76)) {
            g.drawString(font, line, ex, y + 17, TEXT_DIM, false);
            y += font.lineHeight + 1;
        }
        y = Math.max(blockY + 64, y + 20);
        y += GAP;

        y = renderQuote(g, font, x, y, w, color, humanPerkNote(def, tier), "LOADOUT NOTE");
        y += GAP;

        g.drawString(font, "Lore", x, y, TEXT, false);
        y += 13;
        String lore = "In practice, this perk changes how a fight feels more than how a menu reads. The current tier is the version your character can actually rely on right now; higher tiers are shown so the next step is clear without pretending you already have it.";
        for (String line : wordWrap(font, lore, w)) {
            g.drawString(font, line, x, y, TEXT_DIM, false);
            y += font.lineHeight + 2;
        }
        y += GAP;

        g.drawString(font, "Tier Progression", x, y, TEXT, false);
        y += 15;
        for (PerkTier t : PerkTier.values()) {
            boolean available = def.hasTier(t);
            boolean current = t == tier;
            int rowAlpha = available ? 0xFF : 0x59;
            int c = dimColor(tierColor(t), rowAlpha);
            g.fill(x, y + 3, x + 6, y + 9, c);
            g.drawString(font, t.displayName.toUpperCase(Locale.ROOT), x + 13, y, available ? c : TEXT_FAINT, false);
            int barX = x + 96;
            int barW = w - 96;
            g.fill(barX, y + 4, barX + barW, y + 7, BORDER);
            if (current) {
                g.fill(barX, y + 4, barX + barW, y + 7, c);
                g.fill(barX, y + 3, barX + barW, y + 8, dimColor(c, 0x35));
            } else if (available && t.level < tier.level) {
                g.fill(barX, y + 4, barX + barW, y + 7, dimColor(c, 0x95));
            }
            y += 17;
        }
        y += GAP;

        y = renderUpgradeCost(g, font, data, def, tier, x, y, w);
        y += GAP;

        g.drawString(font, "WHERE TO FIND", x, y, TEXT_DIM, false);
        y += 13;
        for (String line : wordWrap(font, def.locationHint, w)) {
            g.drawString(font, line, x, y, TEXT_DIM, false);
            y += font.lineHeight + 2;
        }
        return y + PAD;
    }

    private int renderFlawDetailContent(GuiGraphics g, Font font, PerkData data, FlawDefinition flaw, int x, int y, int w) {
        g.drawString(font, trim(font, flaw.name, w), x, y, TEXT, false);
        y += 22;
        int blockY = y;
        drawRect(g, x, blockY, 64, 64, RED, 0xFF1D1010);
        g.drawString(font, "!", x + 29, blockY + 27, RED, false);
        int ex = x + 76;
        g.drawString(font, data.hasFlaw(flaw.id) ? "EQUIPPED FLAW" : "FLAW", ex, blockY + 3, RED, false);
        for (String line : wordWrap(font, flaw.description, w - 76)) {
            g.drawString(font, line, ex, y + 17, TEXT_DIM, false);
            y += font.lineHeight + 1;
        }
        y = Math.max(blockY + 64, y + 20);
        y = renderQuote(g, font, x, y, w, RED, "You take this because the extra power has to cost something.", "LOADOUT NOTE");
        y += GAP;
        g.drawString(font, "Lore", x, y, TEXT, false);
        y += 13;
        for (String line : wordWrap(font, "A flaw is not a bonus wearing a scary mask. It is a real drawback you accept so a heavier perk setup can stay balanced.", w)) {
            g.drawString(font, line, x, y, TEXT_DIM, false);
            y += font.lineHeight + 2;
        }
        return y + PAD;
    }

    private int renderQuote(GuiGraphics g, Font font, int x, int y, int w, int color, String quote, String attr) {
        g.fill(x, y, x + 2, y + 42, color);
        g.drawString(font, "\"", x + 10, y + 3, dimColor(color, 0x99), false);
        g.drawString(font, trim(font, quote, w - 28), x + 24, y + 9, TEXT_DIM, false);
        g.drawString(font, attr, x + 24, y + 25, TEXT_FAINT, false);
        return y + 42;
    }

    private int renderUpgradeCost(GuiGraphics g, Font font, PerkData data, PerkDefinition def, PerkTier tier, int x, int y, int w) {
        g.drawString(font, "Upgrade Cost", x, y, TEXT, false);
        y += 15;
        PerkUpgradeCost cost = def.getUpgradeCost(tier);
        if (cost == null) {
            g.drawString(font, "Max tier reached.", x, y, TEXT_DIM, false);
            return y + 14;
        }

        int cellW = (w - 12) / 3;
        int cellH = 48;
        int playerXp = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.experienceLevel : 0;
        renderCostCell(g, font, x, y, cellW, cellH, "XP", cost.xpLevels, playerXp, playerXp >= cost.xpLevels, true);

        int index = 1;
        for (ItemStack stack : cost.requiredItems) {
            int cx = x + (index % 3) * (cellW + 6);
            int cy = y + (index / 3) * (cellH + 6);
            int have = countClientItem(stack.getItem());
            renderCostCell(g, font, cx, cy, cellW, cellH, stack.getHoverName().getString(), stack.getCount(), have, have >= stack.getCount(), false, stack);
            index++;
        }
        int rows = Math.max(1, (index + 2) / 3);
        return y + rows * cellH + (rows - 1) * 6;
    }

    private void renderCostCell(GuiGraphics g, Font font, int x, int y, int w, int h, String name, int need, int have, boolean ok, boolean xp) {
        renderCostCell(g, font, x, y, w, h, name, need, have, ok, xp, ItemStack.EMPTY);
    }

    private void renderCostCell(GuiGraphics g, Font font, int x, int y, int w, int h, String name, int need, int have, boolean ok, boolean xp, ItemStack stack) {
        drawRect(g, x, y, w, h, ok ? GREEN : RED, PANEL);
        int iconX = x + 6;
        int iconY = y + 6;
        drawRect(g, iconX, iconY, 22, 22, xp ? ACCENT : BORDER, xp ? 0xFF211A10 : PANEL_2);
        if (xp) {
            g.drawString(font, "XP", iconX + 5, iconY + 7, ACCENT, false);
        } else if (!stack.isEmpty()) {
            g.renderItem(stack, iconX + 3, iconY + 3);
        }
        g.drawString(font, String.valueOf(need), x + 34, y + 8, TEXT, false);
        g.drawString(font, "have: " + have, x + 6, y + 33, ok ? GREEN : RED, false);
        g.drawString(font, trim(font, name, w - 40), x + 34, y + 21, TEXT_FAINT, false);
    }

    private void renderActionFooter(GuiGraphics g, Font font, PerkData data, PerkDefinition perk, FlawDefinition flaw, int x, int y, int w, int h, int mx, int my) {
        g.fill(x, y, x + w, y + h, PANEL);
        g.fill(x, y, x + w, y + 1, BORDER);
        int bw = (w - PAD * 2 - 8) / 3;
        int by = y + 9;
        boolean canEquip = (perk != null && data.hasDiscovered(perk.id) && !data.isEquipped(perk.id))
                || (flaw != null && !data.hasFlaw(flaw.id));
        boolean canUnequip = (perk != null && data.isEquipped(perk.id)) || (flaw != null && data.hasFlaw(flaw.id));
        boolean canUpgrade = perk != null && data.isEquipped(perk.id) && perk.getUpgradeCost(data.getTier(perk.id)) != null;
        drawAction(g, font, x + PAD, by, bw, 17, "Equip", GREEN, canEquip, mx, my);
        drawAction(g, font, x + PAD + bw + 4, by, bw, 17, "Unequip", RED, canUnequip, mx, my);
        drawAction(g, font, x + PAD + (bw + 4) * 2, by, bw, 17, "Upgrade ^", ACCENT, canUpgrade, mx, my);
    }

    private void drawAction(GuiGraphics g, Font font, int x, int y, int w, int h, String label, int color, boolean enabled, int mx, int my) {
        boolean hover = enabled && inRect(mx, my, x, y, w, h);
        drawRect(g, x, y, w, h, enabled ? color : BORDER, hover ? PANEL_3 : PANEL_2);
        g.drawString(font, label, x + (w - font.width(label)) / 2, y + 5, enabled ? TEXT : TEXT_FAINT, false);
    }

    private boolean handleLeftClick(double mx, double my, PerkData data) {
        int x = PAD;
        int y = PAD - categoryScroll;
        int w = leftW - PAD * 2;
        int cardH = 104;
        int bw = (w - 18 - 2 * 5) / 3;
        int by = y + 77;
        for (int i = 0; i < 3; i++) {
            int bx = x + 9 + i * (bw + 5);
            if (inRect(mx, my, bx, by, bw, 16)) {
                activePresetIndex = i;
                if (i < data.getPresets().size()) {
                    sendAction(new PerkActionPacket(PerkActionPacket.Action.LOAD_PRESET, data.getPresets().get(i).name));
                }
                return true;
            }
        }

        int cy = y + cardH + GAP;
        for (Category category : Category.values()) {
            if (category == Category.FLAWS) cy += 13;
            if (inRect(mx, my, x, cy, w, 20)) {
                activeCategory = category;
                listScroll = 0;
                searchActive = false;
                return true;
            }
            cy += 26;
        }
        return false;
    }

    private boolean handleMiddleClick(double mx, double my, PerkData data) {
        int x = middleX;
        int w = middleW;
        int searchW = Math.min(110, Math.max(70, w / 5));
        int searchX = x + w - PAD - searchW;
        int searchY = 12;
        if (inRect(mx, my, searchX, searchY, searchW, 17)) {
            searchActive = true;
            return true;
        }

        String filterLabel = "Filter: " + filter.label + " v";
        int filterW = approxW(filterLabel) + 12;
        int filterX = searchX - 6 - filterW;
        if (inRect(mx, my, filterX, searchY, filterW, 17)) {
            filter = filter.next();
            listScroll = 0;
            return true;
        }

        String sortLabel = "Sort: " + sort.label + " v";
        int sortW = approxW(sortLabel) + 12;
        int sortX = filterX - 6 - sortW;
        if (inRect(mx, my, sortX, searchY, sortW, 17)) {
            sort = sort.next();
            listScroll = 0;
            return true;
        }

        if (my >= listViewportY && my < listViewportY + listViewportH) {
            List<RowEntry> rows = buildRows(data);
            int ry = listViewportY + PAD - listScroll;
            for (RowEntry row : rows) {
                if (inRect(mx, my, x + PAD, ry, w - PAD * 2, ROW_H)) {
                    if (!row.locked) {
                        selectedId = row.id().equals(selectedId) ? null : row.id();
                        detailScroll = 0;
                    }
                    return true;
                }
                ry += ROW_H + 8;
            }
        }

        searchActive = false;
        return false;
    }

    private boolean handleDetailClick(double mx, double my, PerkData data) {
        PerkDefinition perk = selectedPerk();
        FlawDefinition flaw = selectedFlaw();
        if (perk == null && flaw == null) return false;

        int bw = (rightW - PAD * 2 - 8) / 3;
        int by = cachedH - DETAIL_FOOTER_H + 9;
        int x = rightX + PAD;
        if (inRect(mx, my, x, by, bw, 17)) {
            if (perk != null && data.hasDiscovered(perk.id) && !data.isEquipped(perk.id)) {
                sendAction(new PerkActionPacket(PerkActionPacket.Action.EQUIP, perk.id, perk.minTier.level));
                return true;
            }
            if (flaw != null && !data.hasFlaw(flaw.id)) {
                sendAction(new PerkActionPacket(PerkActionPacket.Action.EQUIP_FLAW, flaw.id));
                return true;
            }
        }
        if (inRect(mx, my, x + bw + 4, by, bw, 17)) {
            if (perk != null && data.isEquipped(perk.id)) {
                sendAction(new PerkActionPacket(PerkActionPacket.Action.UNEQUIP, perk.id));
                return true;
            }
            if (flaw != null && data.hasFlaw(flaw.id)) {
                sendAction(new PerkActionPacket(PerkActionPacket.Action.UNEQUIP_FLAW, flaw.id));
                return true;
            }
        }
        if (inRect(mx, my, x + (bw + 4) * 2, by, bw, 17) && perk != null && data.isEquipped(perk.id)) {
            sendAction(new PerkActionPacket(PerkActionPacket.Action.UPGRADE, perk.id));
            return true;
        }
        return false;
    }

    private List<RowEntry> buildRows(PerkData data) {
        List<RowEntry> rows = new ArrayList<>();
        String search = searchText.toLowerCase(Locale.ROOT);

        if (activeCategory.flaws) {
            for (FlawDefinition flaw : NichirinPerkRegistry.allFlaws()) {
                if (!search.isEmpty() && !matchesFlaw(flaw, search)) continue;
                rows.add(new RowEntry(flaw));
            }
            rows.sort(Comparator.comparing(row -> row.flaw.name));
            return rows;
        }

        boolean lockedMode = activeCategory.locked;
        for (PerkDefinition def : NichirinPerkRegistry.allPerks()) {
            boolean discovered = data.hasDiscovered(def.id);
            if (lockedMode != !discovered) continue;
            if (activeCategory.tag != null && Arrays.stream(def.tags).noneMatch(t -> t == activeCategory.tag)) continue;
            if (!lockedMode) {
                if (filter == FilterMode.EQUIPPED && !data.isEquipped(def.id)) continue;
            }
            if (!search.isEmpty() && !matchesPerk(def, search)) continue;
            rows.add(new RowEntry(def, lockedMode));
        }

        sortRows(rows, data);
        return rows;
    }

    private void sortRows(List<RowEntry> rows, PerkData data) {
        switch (sort) {
            case NAME -> rows.sort(Comparator.comparing(row -> row.perk.name));
            case TAG -> rows.sort(Comparator.comparing(row -> row.perk.tags.length == 0 ? "" : row.perk.tags[0].displayName));
            case TIER -> rows.sort((a, b) -> {
                boolean ae = data.isEquipped(a.perk.id);
                boolean be = data.isEquipped(b.perk.id);
                if (ae != be) return ae ? -1 : 1;
                return rowTier(data, b.perk).level - rowTier(data, a.perk).level;
            });
        }
    }

    private String categoryCount(PerkData data, Category category) {
        if (category.flaws) return data.equippedFlawCount() + "/" + NichirinPerkRegistry.allFlaws().size();
        if (category.locked) return String.valueOf(countPerks(data, category, false));
        return String.valueOf(countPerks(data, category, true));
    }

    private int countPerks(PerkData data, Category category, boolean discovered) {
        int count = 0;
        for (PerkDefinition def : NichirinPerkRegistry.allPerks()) {
            if (data.hasDiscovered(def.id) != discovered) continue;
            if (category.tag != null && Arrays.stream(def.tags).noneMatch(t -> t == category.tag)) continue;
            count++;
        }
        return count;
    }

    private PerkDefinition selectedPerk() {
        if (selectedId == null || selectedId.startsWith("flaw:")) return null;
        return NichirinPerkRegistry.getPerk(selectedId);
    }

    private FlawDefinition selectedFlaw() {
        if (selectedId == null || !selectedId.startsWith("flaw:")) return null;
        return NichirinPerkRegistry.getFlaw(selectedId.substring("flaw:".length()));
    }

    private PerkTier rowTier(PerkData data, PerkDefinition def) {
        return data.isEquipped(def.id) ? data.getTier(def.id) : def.minTier;
    }

    private String activePresetName(PerkData data) {
        if (activePresetIndex < data.getPresets().size()) return data.getPresets().get(activePresetIndex).name;
        return "Preset " + roman(activePresetIndex + 1);
    }

    private boolean matchesPerk(PerkDefinition def, String search) {
        return def.name.toLowerCase(Locale.ROOT).contains(search)
                || def.description.toLowerCase(Locale.ROOT).contains(search)
                || Arrays.stream(def.tags).anyMatch(tag -> tag.displayName.toLowerCase(Locale.ROOT).contains(search));
    }

    private boolean matchesFlaw(FlawDefinition flaw, String search) {
        return flaw.name.toLowerCase(Locale.ROOT).contains(search)
                || flaw.description.toLowerCase(Locale.ROOT).contains(search);
    }

    private String humanPerkNote(PerkDefinition def, PerkTier tier) {
        String desc = def.getDescriptionForTier(tier);
        if (desc == null || desc.isBlank()) {
            desc = def.description;
        }
        return trimPlain(desc, 86);
    }

    private static void renderPerkIcon(GuiGraphics g, String perkId, PerkTier tier, int x, int y) {
        ResourceLocation tex = PerkIcon.get(perkId, tier);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(tex, x, y, 0, 0, 32, 32, 32, 32);
        RenderSystem.disableBlend();
    }

    private int drawBadge(GuiGraphics g, Font font, int x, int y, String label, int color, boolean outlineOnly) {
        int w = font.width(label) + 8;
        g.fill(x, y, x + w, y + 11, color);
        g.fill(x + 1, y + 1, x + w - 1, y + 10, outlineOnly ? PANEL_2 : dimColor(color, 0x38));
        g.drawString(font, label, x + 4, y + 2, outlineOnly ? color : TEXT, false);
        return x + w;
    }

    private int drawChip(GuiGraphics g, Font font, int x, int y, String label, int color) {
        int w = font.width(label) + 9;
        g.fill(x, y, x + w, y + 10, dimColor(color, 0x90));
        g.fill(x + 1, y + 1, x + w - 1, y + 9, dimColor(color, 0x22));
        g.drawString(font, label, x + 4, y + 1, dimColor(color, 0xFF), false);
        return x + w + 4;
    }

    private static void drawRect(GuiGraphics g, int x, int y, int w, int h, int border, int fill) {
        g.fill(x, y, x + w, y + h, border);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }

    private static void renderScrollBar(GuiGraphics g, int x, int y, int w, int h, int scroll, int maxScroll) {
        g.fill(x, y, x + w, y + h, BORDER);
        int thumbH = Math.max(14, h * h / Math.max(h + maxScroll, 1));
        int thumbY = y + (int)((h - thumbH) * (scroll / (float) maxScroll));
        g.fill(x, thumbY, x + w, thumbY + thumbH, ACCENT);
    }

    private static int tierColor(PerkTier tier) {
        return tier.primaryColor | 0xFF000000;
    }

    private static int tierFill(PerkTier tier, int alpha) {
        return (alpha << 24) | (tier.primaryColor & 0x00FFFFFF);
    }

    private static int dimColor(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private static int countClientItem(Item item) {
        Player p = Minecraft.getInstance().player;
        if (p == null || item == Items.AIR) return 0;
        int count = 0;
        for (ItemStack stack : p.getInventory().items) {
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    private static void sendAction(PerkActionPacket packet) {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            packet.toBytes(buf);
            NetworkManager.sendToServer(NichirinPacketRegistry.PERK_ACTION_ID, buf);
        } catch (Exception ignored) {
        }
    }

    private static String trim(Font font, String text, int maxW) {
        if (text == null) return "";
        if (font.width(text) <= maxW) return text;
        return font.plainSubstrByWidth(text, Math.max(0, maxW - font.width("..."))) + "...";
    }

    private static String trimPlain(String text, int maxChars) {
        if (text == null) return "";
        if (text.length() <= maxChars) return text;
        return text.substring(0, Math.max(0, maxChars - 3)).stripTrailing() + "...";
    }

    private static List<String> wordWrap(Font font, String text, int maxW) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return lines;
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String test = current.length() == 0 ? word : current + " " + word;
            if (font.width(test) > maxW && current.length() > 0) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(test);
            }
        }
        if (current.length() > 0) lines.add(current.toString());
        return lines;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private static int approxW(String text) {
        return text.length() * 6;
    }

    private static String roman(int index) {
        return switch (index) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> String.valueOf(index);
        };
    }

    private static boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
