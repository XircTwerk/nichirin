package com.xirc.nichirin.client.gui.biggui.skills;

import com.xirc.nichirin.client.data.ClientPerkCache;
import com.xirc.nichirin.client.gui.PerkIcon;
import com.xirc.nichirin.common.network.c2s.PerkActionPacket;
import com.xirc.nichirin.common.system.perks.*;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import net.minecraft.resources.ResourceLocation;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * Perks sub-tab — icon grid layout.
 *
 * <pre>
 *  EQUIPPED  n/slots                         [Sort ▾] [Perks: ON]
 *  [eq] [eq] [ ] [🔒] [🔒]
 *  FLAWS: [f1] [+]
 *  ─────────────────────────────────────────────────────────────────
 *  DISCOVERED  (n / total)              [Search...]
 *  [icon][icon][icon]...   (scrollable)
 *  ─────────────────────────────────────────────────────────────────
 *  UNDISCOVERED  (n remaining)
 *  [ ?? ][ ?? ]...          (dimmed, scrollable)
 *  ─────────────────────────────────────────────────────────────────
 *  ═ Selected perk detail + upgrade / equip buttons  (only when selected)
 * </pre>
 */
public class PerksTab {

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int PAD        = 8;
    private static final int ICON_SZ    = 36;
    private static final int ICON_GAP   = 5;
    private static final int EQ_SZ      = 44;
    private static final int EQ_GAP     = 6;
    private static final int SEC_H      = 14;
    private static final int DETAIL_H   = 82;  // bottom detail panel height when visible

    // ── Colours — BigGui dark palette ─────────────────────────────────────────
    private static final int C_BG           = 0xFF0E0E0E;
    private static final int C_PANEL        = 0xFF181818;
    private static final int C_PANEL2       = 0xFF1E1E1E;
    private static final int C_PANEL_LITE   = 0xFF262626;
    private static final int C_BORDER       = 0xFF303030;
    private static final int C_BORDER_MID   = 0xFF3A3A3A;
    private static final int C_BORDER_HI    = 0xFF505050;
    private static final int C_TEXT         = 0xFFDDDDDD;
    private static final int C_TEXT_DIM     = 0xFF777777;
    private static final int C_TEXT_FAINT   = 0xFF383838;
    private static final int C_ACCENT       = 0xFFAAAAAA;
    private static final int C_ACCENT_DIM   = 0xFF2A2A2A;
    private static final int C_GREEN        = 0xFF55AA55;
    private static final int C_RED          = 0xFFAA4444;
    private static final int C_GOLD         = 0xFFCCAA33;
    private static final int C_SLOT_EMPTY   = 0xFF111111;
    private static final int C_SLOT_EQ      = 0xFF202020;
    private static final int C_SLOT_LOCKED  = 0xFF0A0A0A;
    private static final int OV_HOVER       = 0x1FFFFFFF;
    private static final int OV_HOVER2      = 0x2EFFFFFF;
    private static final int OV_LOCKED      = 0xAA080808;

    // ── State ─────────────────────────────────────────────────────────────────
    private int      discScroll    = 0;
    private int      lockScroll    = 0;
    private SortMode sort          = SortMode.TIER;
    private boolean  searchActive  = false;
    private String   searchText    = "";
    private String   hoverPerkId   = null;
    private int      hoverMx, hoverMy;
    private boolean  mouseInDisc   = false;
    private boolean  mouseInLock   = false;
    private String   selectedId    = null;  // clicked perk → detail panel

    // Cached layout
    private int cachedCw;
    private int eqSectionBottom;
    private int discGridY, discGridH;
    private int lockGridY, lockGridH;

    enum SortMode {
        TIER("Tier"), NAME("Name"), CURSED("Cursed");
        final String label;
        SortMode(String l) { this.label = l; }
        SortMode next() { SortMode[] v = values(); return v[(ordinal() + 1) % v.length]; }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // RENDER
    // ═══════════════════════════════════════════════════════════════════════════

    // ── Equipped + Flaws ──────────────────────────────────────────────────────

    private int renderEquippedSection(GuiGraphics g, Font font, PerkData data, int cw, int y, int mx, int my) {
        int slots  = data.getPerkSlots();
        boolean on = data.isPerksEnabled();
        Map<String, PerkTier> eqMap = data.getEquippedPerks();
        List<Map.Entry<String, PerkTier>> eList = new ArrayList<>(eqMap.entrySet());

        int flawsNeeded  = Math.max(0, data.equippedCount() - 3);
        int flawsHave    = data.equippedFlawCount();
        boolean hasFlaws = flawsNeeded > 0 || flawsHave > 0;
        int sectionH     = SEC_H + 4 + EQ_SZ + PAD + (hasFlaws ? EQ_SZ / 2 + 6 : 0);

        g.fill(0, y, cw, y + sectionH, C_PANEL);
        g.fill(0, y + sectionH, cw, y + sectionH + 1, C_BORDER_MID);
        g.fill(0, y + sectionH + 1, cw, y + sectionH + 2, C_BORDER);

        // Header row
        g.drawString(font, "EQUIPPED  " + eList.size() + " / " + slots, PAD + 2, y + 3, C_ACCENT, false);

        String sortLabel = "Sort: " + sort.label + " \u25be";
        int sortW  = font.width(sortLabel) + 10;
        int togW   = font.width("Perks: OFF") + 14;
        int togX   = cw - PAD - togW;
        int sortX  = togX - 4 - sortW;

        boolean sortHov = inRect(mx, my, sortX, y + 2, sortW, 11);
        drawPillBtn(g, font, sortX, y + 2, sortW, 11, sortLabel, C_BORDER_MID, sortHov ? C_TEXT : C_TEXT_DIM, sortHov);

        boolean togHov = inRect(mx, my, togX, y + 2, togW, 11);
        drawPillBtn(g, font, togX, y + 2, togW, 11, "Perks: " + (on ? "ON" : "OFF"),
                on ? C_GREEN : C_BORDER_MID, on ? 0xFFAAFFBB : C_TEXT_DIM, togHov);

        // Equipped slots
        int slotY = y + SEC_H + 4;
        for (int i = 0; i < 5; i++) {
            int sx = PAD + i * (EQ_SZ + EQ_GAP);
            if (i >= slots) {
                renderLockedEqSlot(g, font, sx, slotY, EQ_SZ, mx, my);
            } else if (i < eList.size()) {
                PerkDefinition def = NichirinPerkRegistry.getPerk(eList.get(i).getKey());
                renderFilledEqSlot(g, font, def, eList.get(i).getValue(), sx, slotY, EQ_SZ, mx, my);
            } else {
                renderEmptyEqSlot(g, font, sx, slotY, EQ_SZ, mx, my);
            }
        }

        // Flaws row
        if (hasFlaws) {
            int fy = slotY + EQ_SZ + 4;
            int fx = PAD;
            g.drawString(font, "FLAWS:", fx, fy + (EQ_SZ / 2 - font.lineHeight) / 2, C_RED, false);
            fx += font.width("FLAWS:") + 5;

            List<String> flawList = new ArrayList<>(data.getEquippedFlaws());
            int flawSz = EQ_SZ / 2;
            for (int i = 0; i < Math.max(flawsNeeded, flawsHave); i++) {
                boolean filled    = i < flawList.size();
                boolean required  = i < flawsNeeded;
                int borderCol = filled ? C_RED : (required ? 0xFFAA2222 : C_BORDER);

                g.fill(fx + 2, fy + 2, fx + flawSz + 2, fy + flawSz + 2, 0x35000000);
                g.fill(fx - 1, fy - 1, fx + flawSz + 1, fy + flawSz + 1, borderCol);
                g.fill(fx, fy, fx + flawSz, fy + flawSz, filled ? 0xFF1A0808 : C_SLOT_LOCKED);

                if (filled) {
                    FlawDefinition flaw = NichirinPerkRegistry.getFlaw(flawList.get(i));
                    String abbr = flaw != null ? flaw.name.substring(0, Math.min(2, flaw.name.length())).toUpperCase() : "?";
                    g.drawString(font, abbr, fx + (flawSz - font.width(abbr)) / 2, fy + (flawSz - font.lineHeight) / 2, C_RED, false);
                } else if (required) {
                    g.drawString(font, "!", fx + (flawSz - font.width("!")) / 2, fy + (flawSz - font.lineHeight) / 2, 0xFFAA2222, false);
                } else {
                    g.drawString(font, "+", fx + (flawSz - font.width("+")) / 2, fy + (flawSz - font.lineHeight) / 2, C_BORDER_HI, false);
                }
                fx += flawSz + 3;
            }

            if (flawsNeeded > flawsHave) {
                int missing = flawsNeeded - flawsHave;
                String warn = "Equip " + missing + " more flaw" + (missing > 1 ? "s" : "") + " to support extra perks";
                g.drawString(font, warn, fx + 6, fy + (flawSz - font.lineHeight) / 2, 0xFFAA4444, false);
            }
        }

        return y + sectionH;
    }

    private void renderFilledEqSlot(GuiGraphics g, Font font, PerkDefinition def, PerkTier tier,
                                    int x, int y, int sz, int mx, int my) {
        boolean hov = inRect(mx, my, x - 2, y - 2, sz + 4, sz + 4);
        int tc = tier.primaryColor | 0xFF000000;

        g.fill(x + 3, y + 3, x + sz + 3, y + sz + 3, 0x55000000);
        g.fill(x - 2, y - 2, x + sz + 2, y + sz + 2, (tc & 0x00FFFFFF) | 0x66000000);
        g.fill(x - 1, y - 1, x + sz + 1, y + sz + 1, tc);
        g.fill(x, y, x + sz, y + sz, C_SLOT_EQ);
        g.fill(x, y, x + sz, y + 3, tc);
        g.fill(x, y, x + sz / 2, y + 2, (tc & 0x00FFFFFF) | 0x55FFFFFF);

        if (hov) {
            g.fill(x, y + 3, x + sz, y + sz, OV_HOVER2);
            if (hoverPerkId == null && def != null) { hoverPerkId = def.id; hoverMx = mx; hoverMy = my; }
        }

        // Perk icon texture
        if (def != null) {
            renderPerkIcon(g, def.id, tier, x + (sz - 32) / 2, y + (sz - 32) / 2 + 1);
        }

        g.fill(x + sz - 6, y + sz - 5, x + sz - 1, y + sz - 1, tc);
    }

    private void renderEmptyEqSlot(GuiGraphics g, Font font, int x, int y, int sz, int mx, int my) {
        boolean hov = inRect(mx, my, x, y, sz, sz);
        g.fill(x + 2, y + 2, x + sz + 2, y + sz + 2, 0x30000000);
        g.fill(x - 1, y - 1, x + sz + 1, y + sz + 1, C_BORDER);
        g.fill(x, y, x + sz, y + sz, hov ? C_PANEL_LITE : C_SLOT_EMPTY);
        if (hov) g.fill(x, y, x + sz, y + sz, OV_HOVER);
        int cx = x + sz / 2, cy = y + sz / 2;
        g.fill(cx - 6, cy - 1, cx + 7, cy + 2, C_BORDER_HI);
        g.fill(cx - 1, cy - 6, cx + 2, cy + 7, C_BORDER_HI);
    }

    private void renderLockedEqSlot(GuiGraphics g, Font font, int x, int y, int sz, int mx, int my) {
        g.fill(x + 2, y + 2, x + sz + 2, y + sz + 2, 0x25000000);
        g.fill(x - 1, y - 1, x + sz + 1, y + sz + 1, C_BORDER);
        g.fill(x, y, x + sz, y + sz, C_SLOT_LOCKED);
        g.fill(x, y, x + sz, y + sz, OV_LOCKED);
        renderLockIcon(g, x + sz / 2 - 4, y + sz / 2 - 6, C_TEXT_FAINT);
        String earn = "EARN";
        g.drawString(font, earn, x + (sz - font.width(earn)) / 2, y + sz - font.lineHeight - 2, C_TEXT_FAINT, false);
    }

    // ── Section header ────────────────────────────────────────────────────────

    private void renderSectionHeader(GuiGraphics g, Font font, int cw, int y,
                                     String title, String badge, boolean showSearch,
                                     PerkData data, int mx, int my) {
        g.drawString(font, title, PAD + 2, y + 1, C_ACCENT, false);
        int afterX = PAD + 2 + font.width(title) + 5;
        if (badge != null) {
            g.drawString(font, badge, afterX, y + 1, C_TEXT_DIM, false);
            afterX += font.width(badge) + 5;
        }
        int lineEndX = cw - PAD;
        if (showSearch && data != null) {
            int sw = 95; int sx = cw - PAD - sw; int sy = y - 1;
            lineEndX = sx - 5;
            boolean shov = inRect(mx, my, sx, sy, sw, 11);
            int sbg = searchActive ? 0xFF182038 : (shov ? C_PANEL_LITE : C_PANEL2);
            g.fill(sx - 1, sy - 1, sx + sw + 1, sy + 12, C_BORDER_MID);
            g.fill(sx, sy, sx + sw, sy + 11, sbg);
            String sdisp = (searchText.isEmpty() && !searchActive) ? "Search..."
                    : searchText + (searchActive ? "\u258c" : "");
            g.drawString(font, sdisp, sx + 3, sy + 2, searchText.isEmpty() ? C_TEXT_DIM : C_TEXT, false);
        }
        int lineY = y + SEC_H / 2;
        g.fill(afterX, lineY, lineEndX, lineY + 1, C_BORDER);
        int fl = Math.min(30, lineEndX - afterX);
        for (int i = 0; i < fl; i++) {
            int a = (int)(0x44 * (i / (double) fl));
            g.fill(afterX + i, lineY, afterX + i + 1, lineY + 1, (a << 24) | (C_ACCENT & 0xFFFFFF));
        }
    }

    // ── Discovered grid ───────────────────────────────────────────────────────

    private void renderDiscGrid(GuiGraphics g, Font font, PerkData data,
                                List<PerkDefinition> all, int cols,
                                int gx, int gy, int gw, int gh, int mx, int my) {
        g.fill(gx, gy, gx + gw, gy + gh, C_PANEL2);
        g.fill(gx, gy, gx + gw, gy + 1, C_BORDER);
        g.fill(gx, gy + gh - 1, gx + gw, gy + gh, C_BORDER);

        String search = searchText.toLowerCase();
        List<PerkDefinition> perks = new ArrayList<>();
        for (PerkDefinition d : all) {
            if (!search.isEmpty()) {
                boolean m = d.name.toLowerCase().contains(search)
                        || d.description.toLowerCase().contains(search)
                        || Arrays.stream(d.tags).anyMatch(t -> t.displayName.toLowerCase().contains(search));
                if (!m) continue;
            }
            perks.add(d);
        }
        sortPerks(perks, data);

        int rowsVisible = Math.max(1, gh / (ICON_SZ + ICON_GAP));
        int maxScroll = Math.max(0, (perks.size() + cols - 1) / cols - rowsVisible);
        discScroll = Math.max(0, Math.min(discScroll, maxScroll));
        if (inRect(mx, my, gx, gy, gw, gh)) mouseInDisc = true;

        for (int i = discScroll * cols; i < Math.min(perks.size(), (discScroll + rowsVisible + 1) * cols); i++) {
            int col = i % cols, row = i / cols - discScroll;
            int ix = gx + col * (ICON_SZ + ICON_GAP), iy = gy + row * (ICON_SZ + ICON_GAP) + 1;
            if (iy + ICON_SZ > gy + gh) break;

            PerkDefinition def = perks.get(i);
            boolean equipped = data.isEquipped(def.id);
            PerkTier tier    = equipped ? data.getTier(def.id) : def.minTier;
            boolean sel      = def.id.equals(selectedId);
            boolean hov      = inRect(mx, my, ix, iy, ICON_SZ, ICON_SZ);
            renderDiscCell(g, font, def, tier, equipped, sel, ix, iy, ICON_SZ, ICON_SZ, hov);
            if (hov && hoverPerkId == null) { hoverPerkId = def.id; hoverMx = mx; hoverMy = my; }
        }

        if (maxScroll > 0) renderScrollBar(g, gx + gw - 3, gy + 1, 2, gh - 2, discScroll, maxScroll);
        if (perks.isEmpty()) {
            String msg = search.isEmpty() ? "No discovered perks yet." : "No matches.";
            g.drawString(font, msg, gx + (gw - font.width(msg)) / 2, gy + gh / 2 - 4, C_TEXT_DIM, false);
        }
    }

    private void renderDiscCell(GuiGraphics g, Font font, PerkDefinition def, PerkTier tier,
                                boolean equipped, boolean selected,
                                int x, int y, int w, int h, boolean hov) {
        int tc = tier.primaryColor | 0xFF000000;

        g.fill(x + 2, y + 2, x + w + 2, y + h + 2, 0x35000000);

        if (selected) {
            g.fill(x - 2, y - 2, x + w + 2, y + h + 2, (C_GOLD & 0x00FFFFFF) | 0x99000000);
            g.fill(x - 1, y - 1, x + w + 1, y + h + 1, C_GOLD);
        } else if (equipped) {
            g.fill(x - 2, y - 2, x + w + 2, y + h + 2, (tc & 0x00FFFFFF) | 0x55000000);
            g.fill(x - 1, y - 1, x + w + 1, y + h + 1, tc);
        } else {
            g.fill(x - 1, y - 1, x + w + 1, y + h + 1, C_BORDER_MID);
        }

        g.fill(x, y, x + w, y + h, equipped ? C_SLOT_EQ : C_PANEL);
        g.fill(x, y, x + w, y + 3, tc);
        g.fill(x, y, x + w / 2, y + 2, (tc & 0x00FFFFFF) | 0x44FFFFFF);

        if (hov) g.fill(x, y + 3, x + w, y + h, OV_HOVER2);
        if (equipped) g.fill(x, y + 3, x + w, y + h, 0x1299CCFF);
        if (selected) g.fill(x, y + 3, x + w, y + h, 0x20FFCC44);

        // Perk icon texture
        renderPerkIcon(g, def.id, tier, x + (w - 32) / 2, y + (h - 32) / 2 + 1);

        // Tier dot
        int dotCol = equipped ? tc : ((tc & 0x00FFFFFF) | 0xAA000000);
        g.fill(x + w - 5, y + h - 4, x + w - 1, y + h, dotCol);

        if (def.cursed) g.fill(x + 1, y + h - 4, x + 5, y + h, 0xFFAA1111);
    }

    // ── Locked grid ───────────────────────────────────────────────────────────

    private void renderLockGrid(GuiGraphics g, Font font, PerkData data,
                                List<PerkDefinition> perks, int cols,
                                int gx, int gy, int gw, int gh, int mx, int my) {
        g.fill(gx, gy, gx + gw, gy + gh, C_PANEL);
        g.fill(gx, gy, gx + gw, gy + 1, C_BORDER);

        int rowsVisible = Math.max(1, gh / (ICON_SZ + ICON_GAP));
        int maxScroll = Math.max(0, (perks.size() + cols - 1) / cols - rowsVisible);
        lockScroll = Math.max(0, Math.min(lockScroll, maxScroll));
        if (inRect(mx, my, gx, gy, gw, gh)) mouseInLock = true;

        for (int i = lockScroll * cols; i < Math.min(perks.size(), (lockScroll + rowsVisible + 1) * cols); i++) {
            int col = i % cols, row = i / cols - lockScroll;
            int ix = gx + col * (ICON_SZ + ICON_GAP), iy = gy + row * (ICON_SZ + ICON_GAP) + 1;
            if (iy + ICON_SZ > gy + gh) break;
            boolean hov = inRect(mx, my, ix, iy, ICON_SZ, ICON_SZ);
            renderLockCell(g, perks.get(i), ix, iy, ICON_SZ, ICON_SZ, hov);
            if (hov && hoverPerkId == null) { hoverPerkId = perks.get(i).id; hoverMx = mx; hoverMy = my; }
        }

        if (maxScroll > 0) renderScrollBar(g, gx + gw - 3, gy + 1, 2, gh - 2, lockScroll, maxScroll);
        if (perks.isEmpty()) {
            String msg = "All perks discovered!";
            g.drawString(font, msg, gx + (gw - font.width(msg)) / 2, gy + gh / 2 - 4, C_GREEN, false);
        }
    }

    private void renderLockCell(GuiGraphics g, PerkDefinition def, int x, int y, int w, int h, boolean hov) {
        g.fill(x + 2, y + 2, x + w + 2, y + h + 2, 0x25000000);
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, C_BORDER);
        g.fill(x, y, x + w, y + h, C_SLOT_LOCKED);
        g.fill(x, y, x + w, y + 3, C_BORDER);
        g.fill(x, y, x + w, y + h, OV_LOCKED);
        if (hov) g.fill(x, y, x + w, y + h, 0x18FFFFFF);
        renderLockIcon(g, x + w / 2 - 4, y + h / 2 - 7, C_TEXT_FAINT);
        if (def.cursed) g.fill(x + 1, y + h - 4, x + 5, y + h, 0xFF881111);
    }

    // ── Detail panel (bottom strip) ───────────────────────────────────────────

    private void renderDetailPanel(GuiGraphics g, Font font, PerkData data, String id, int cw, int ch, int mx, int my) {
        PerkDefinition def = NichirinPerkRegistry.getPerk(id);
        if (def == null) return;

        boolean equipped = data.isEquipped(id);
        PerkTier tier    = equipped ? data.getTier(id) : def.minTier;
        int tc           = tier.primaryColor | 0xFF000000;

        int panelY = ch - DETAIL_H;
        // Separator glow
        g.fill(0, panelY - 2, cw, panelY - 1, (tc & 0x00FFFFFF) | 0x88000000);
        g.fill(0, panelY - 1, cw, panelY,     C_BORDER_MID);
        // Panel bg
        g.fill(0, panelY, cw, ch, C_PANEL);
        // Top tier strip
        g.fill(0, panelY, cw, panelY + 2, tc);
        g.fill(0, panelY, cw / 2, panelY + 1, (tc & 0x00FFFFFF) | 0x55FFFFFF);

        int lx = PAD + 2, ly = panelY + 5;

        // Name + tier badge
        g.drawString(font, def.name, lx + 1, ly + 1, 0x44000000, false);
        g.drawString(font, def.name, lx, ly, 0xFFFFFFFF, false);
        String tierBadge = "\u2605 " + tier.displayName.toUpperCase();
        g.drawString(font, tierBadge, lx + font.width(def.name) + 6, ly, tc, false);
        ly += font.lineHeight + 3;

        // Description (one line, truncated)
        String desc = def.getDescriptionForTier(tier);
        if (desc != null) {
            int maxDescW = cw - PAD * 2 - 200; // leave room for buttons
            if (font.width(desc) > maxDescW) desc = font.plainSubstrByWidth(desc, maxDescW - 6) + "...";
            g.drawString(font, desc, lx, ly, C_TEXT, false);
            ly += font.lineHeight + 3;
        }

        // Upgrade cost
        PerkUpgradeCost cost = def.getUpgradeCost(tier);
        if (equipped && cost != null) {
            PerkTier next = tier.next();
            String nextName = next != null ? next.displayName.toUpperCase() : "MAX";
            g.drawString(font, "UPGRADE \u2192 " + nextName + ":", lx, ly, C_GOLD, false);
            ly += font.lineHeight + 2;

            Player localPlayer = Minecraft.getInstance().player;
            int playerXp = localPlayer != null ? localPlayer.experienceLevel : 0;
            boolean hasXp = playerXp >= cost.xpLevels;
            String xpLine = (hasXp ? "\u2713 " : "\u2717 ") + cost.xpLevels + " XP  (have: " + playerXp + ")";
            g.drawString(font, xpLine, lx + 2, ly, hasXp ? C_GREEN : C_RED, false);
            ly += font.lineHeight + 2;

            for (ItemStack req : cost.requiredItems) {
                int have = countClientItem(req.getItem());
                boolean ok = have >= req.getCount();
                String line = (ok ? "\u2713 " : "\u2717 ") + req.getCount() + "x " + req.getHoverName().getString() + "  (have: " + have + ")";
                if (font.width(line) + lx > cw / 2) break; // don't overflow
                g.drawString(font, line, lx + 2, ly, ok ? C_GREEN : C_RED, false);
                ly += font.lineHeight + 2;
            }
        } else if (equipped) {
            g.drawString(font, "Max tier reached.", lx, ly, C_TEXT_DIM, false);
        } else {
            // Show unlock hint abbreviated
            g.drawString(font, "How to get: " + def.unlockHint, lx, ly, 0xFFAA9966, false);
        }

        // Buttons on the right side of the panel
        int btnX = cw - PAD - 80;
        int btnY = panelY + 8;

        if (equipped) {
            // Unequip button
            boolean unHov = inRect(mx, my, btnX, btnY, 78, 14);
            drawPillBtn(g, font, btnX, btnY, 78, 14, "Unequip", C_RED, 0xFFFFCCCC, unHov);
            btnY += 20;

            // Upgrade button (if upgradeable)
            if (cost != null) {
                boolean upHov = inRect(mx, my, btnX, btnY, 78, 14);
                drawPillBtn(g, font, btnX, btnY, 78, 14, "Upgrade \u2191", C_GOLD, 0xFFFFFFCC, upHov);
            }
        } else {
            boolean eqHov = inRect(mx, my, btnX, btnY, 78, 14);
            drawPillBtn(g, font, btnX, btnY, 78, 14, "Equip", C_GREEN, 0xFFCCFFCC, eqHov);
        }

        // Tier progression dots
        int dotSz = 8, dotGap = 5;
        int totalDots = PerkTier.values().length;
        int barW = totalDots * dotSz + (totalDots - 1) * dotGap;
        int barX = btnX - barW - 8;
        int barY = panelY + 8;
        g.fill(barX, barY + dotSz / 2, barX + barW, barY + dotSz / 2 + 1, C_BORDER);
        for (int i = 0; i < totalDots; i++) {
            PerkTier t = PerkTier.fromLevel(i);
            int dx = barX + i * (dotSz + dotGap);
            boolean hasTier = def.hasTier(t);
            boolean active  = t.level == tier.level;
            int dotCol = !hasTier ? C_BORDER : active ? (t.primaryColor | 0xFF000000) : C_BORDER_MID;
            g.fill(dx - 1, barY - 1, dx + dotSz + 1, barY + dotSz + 1, hasTier ? (t.primaryColor | 0xFF000000) : C_BORDER);
            g.fill(dx, barY, dx + dotSz, barY + dotSz, dotCol);
            String abbr = t.displayName.substring(0, 1);
            g.drawString(font, abbr, dx + (dotSz - font.width(abbr)) / 2, barY + dotSz + 2, hasTier ? (t.primaryColor | 0xFF000000) : C_TEXT_FAINT, false);
        }
    }

    // ── Perk icon ─────────────────────────────────────────────────────────────

    private static void renderPerkIcon(GuiGraphics g, String perkId, PerkTier tier, int x, int y) {
        ResourceLocation tex = PerkIcon.get(perkId, tier);
        g.blit(tex, x, y, 0, 0, 32, 32, 32, 32);
    }

    // ── Tooltip ───────────────────────────────────────────────────────────────

    private void renderTooltip(GuiGraphics g, Font font, PerkData data, String id,
                               int cw, int ch, int mx, int my) {
        PerkDefinition def = NichirinPerkRegistry.getPerk(id);
        if (def == null) return;

        boolean disc     = data.hasDiscovered(id);
        boolean equipped = data.isEquipped(id);
        PerkTier tier    = equipped ? data.getTier(id) : def.minTier;
        int tc           = tier.primaryColor | 0xFF000000;
        int tw           = 200;

        record L(String tag, String text) {}
        List<L> lines = new ArrayList<>();

        if (disc) {
            lines.add(new L("name", def.name));
            lines.add(new L("tier", "\u2605 " + tier.displayName.toUpperCase()));
            if (def.tags.length > 0) {
                StringBuilder sb = new StringBuilder();
                for (PerkTag t : def.tags) { if (sb.length() > 0) sb.append("  \u00b7  "); sb.append(t.displayName); }
                lines.add(new L("tags", sb.toString()));
            }
            lines.add(new L("sep", ""));
            String desc = def.getDescriptionForTier(tier);
            if (desc != null) for (String l : wordWrap(font, desc, tw - 14)) lines.add(new L("desc", l));
        } else {
            lines.add(new L("locked", "??? Undiscovered Perk"));
            lines.add(new L("sep", ""));
        }

        if (!def.unlockHint.isEmpty()) {
            lines.add(new L("head", "HOW TO UNLOCK"));
            for (String l : wordWrap(font, def.unlockHint, tw - 18)) lines.add(new L("hint", l));
        }
        if (disc && !def.locationHint.isEmpty()) {
            lines.add(new L("head", "WHERE TO FIND"));
            for (String l : wordWrap(font, def.locationHint, tw - 18)) lines.add(new L("loc", l));
        }

        if (disc) {
            lines.add(new L("sep", ""));
            lines.add(new L("act", equipped ? "Click: Unequip  |  Click again for details" : "Click: Select  |  Click equipped to remove"));
        }

        int th = 8;
        for (L l : lines) th += l.tag().equals("sep") ? 5 : (font.lineHeight + 1);

        int tx = mx + 14, ty = my - th / 2;
        if (tx + tw > cw - 2) tx = mx - tw - 6;
        if (ty < 2) ty = 2;
        if (ty + th > ch - DETAIL_H - 4) ty = ch - DETAIL_H - 4 - th - 2;

        g.pose().pushPose();
        g.pose().translate(0, 0, 400); // always render on top of icons

        g.fill(tx + 4, ty + 4, tx + tw + 4, ty + th + 4, 0x70000000);
        g.fill(tx - 2, ty - 2, tx + tw + 2, ty + th + 2, (tc & 0x00FFFFFF) | 0x88000000);
        g.fill(tx - 1, ty - 1, tx + tw + 1, ty + th + 1, C_BORDER_MID);
        g.fill(tx, ty, tx + tw, ty + th, C_PANEL);
        g.fill(tx, ty, tx + tw, ty + 3, tc);
        g.fill(tx, ty, tx + tw / 2, ty + 2, (tc & 0x00FFFFFF) | 0x55FFFFFF);

        int lx = tx + 6, ly = ty + 6;
        for (L line : lines) {
            switch (line.tag()) {
                case "sep"    -> { g.fill(lx, ly + 2, tx + tw - 6, ly + 3, C_BORDER_MID); ly += 5; }
                case "name"   -> { g.drawString(font, line.text(), lx + 1, ly + 1, 0x44000000, false); g.drawString(font, line.text(), lx, ly, 0xFFFFFFFF, false); ly += font.lineHeight + 1; }
                case "tier"   -> { g.drawString(font, line.text(), lx, ly, tc, false); ly += font.lineHeight + 1; }
                case "tags"   -> { g.drawString(font, line.text(), lx, ly, C_TEXT_DIM, false); ly += font.lineHeight + 1; }
                case "desc"   -> { g.drawString(font, line.text(), lx + 2, ly, C_TEXT, false); ly += font.lineHeight + 1; }
                case "head"   -> { g.drawString(font, line.text(), lx, ly, C_ACCENT, false); ly += font.lineHeight + 1; }
                case "hint"   -> { g.drawString(font, line.text(), lx + 2, ly, 0xFFAA9966, false); ly += font.lineHeight + 1; }
                case "loc"    -> { g.drawString(font, line.text(), lx + 2, ly, 0xFF7799AA, false); ly += font.lineHeight + 1; }
                case "locked" -> { g.drawString(font, line.text(), lx, ly, C_TEXT_DIM, false); ly += font.lineHeight + 1; }
                case "act"    -> { g.drawString(font, "\u25b6  " + line.text(), lx, ly, equipped ? C_RED : C_GREEN, false); ly += font.lineHeight + 1; }
            }
        }

        g.pose().popPose();
    }

    // ── Scrollbar ─────────────────────────────────────────────────────────────

    private void renderScrollBar(GuiGraphics g, int x, int y, int w, int h, int scroll, int maxScroll) {
        g.fill(x, y, x + w, y + h, C_BORDER);
        int thumbH = Math.max(8, h / Math.max(2, maxScroll + 2));
        int thumbY = y + (int)((h - thumbH) * ((float) scroll / maxScroll));
        g.fill(x, thumbY, x + w, thumbY + thumbH, C_ACCENT_DIM);
        g.fill(x, thumbY, x + w, thumbY + 1, C_ACCENT);
    }

    private void renderLockIcon(GuiGraphics g, int x, int y, int col) {
        g.fill(x + 1, y,     x + 5, y + 1, col);
        g.fill(x,     y + 1, x + 1, y + 5, col);
        g.fill(x + 5, y + 1, x + 6, y + 5, col);
        g.fill(x - 1, y + 4, x + 7, y + 10, col);
        g.fill(x + 2, y + 6, x + 4, y + 9, C_SLOT_LOCKED);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // INPUT
    // ═══════════════════════════════════════════════════════════════════════════

    public boolean handleClick(double mx, double my, Player player) {
        PerkData data = ClientPerkCache.get();
        int cw = cachedCw;

        // Sort button
        String sortLabel = "Sort: " + sort.label + " \u25be";
        int togW  = approxW("Perks: OFF") + 14;
        int togX  = cw - 8 - togW;
        int sortW = approxW(sortLabel) + 10;
        int sortX = togX - 4 - sortW;
        if (inRect(mx, my, sortX, 8 + 2, sortW, 11)) { sort = sort.next(); return true; }

        // Toggle perks
        if (inRect(mx, my, togX, 8 + 2, togW, 11)) {
            sendAction(new PerkActionPacket(PerkActionPacket.Action.TOGGLE_ALL, ""));
            return true;
        }

        // Equipped slot unequip
        if (eqSectionBottom > 0 && my > 8 + SEC_H + 4 && my < eqSectionBottom) {
            List<Map.Entry<String, PerkTier>> eList = new ArrayList<>(data.getEquippedPerks().entrySet());
            for (int i = 0; i < Math.min(eList.size(), data.getPerkSlots()); i++) {
                int sx = 8 + i * (EQ_SZ + EQ_GAP);
                if (inRect(mx, my, sx, 8 + SEC_H + 4, EQ_SZ, EQ_SZ)) {
                    sendAction(new PerkActionPacket(PerkActionPacket.Action.UNEQUIP, eList.get(i).getKey()));
                    if (selectedId != null && selectedId.equals(eList.get(i).getKey())) selectedId = null;
                    return true;
                }
            }
        }

        // Search bar
        if (discGridY > 0) {
            int sw = 95, sx = cw - 8 - sw, sy = discGridY - SEC_H - 3;
            if (inRect(mx, my, sx, sy, sw, 11)) { searchActive = !searchActive; return true; }
            else if (my < discGridY) searchActive = false;
        }

        // Discovered grid click → select, equip, or unequip
        if (discGridY > 0 && my >= discGridY && my < discGridY + discGridH) {
            int cols = Math.max(2, (cw - 16 + ICON_GAP) / (ICON_SZ + ICON_GAP));
            List<PerkDefinition> perks = getDiscovered(data);
            String search = searchText.toLowerCase();
            if (!search.isEmpty()) perks.removeIf(d ->
                    !d.name.toLowerCase().contains(search) &&
                    !d.description.toLowerCase().contains(search) &&
                    Arrays.stream(d.tags).noneMatch(t -> t.displayName.toLowerCase().contains(search)));
            sortPerks(perks, data);
            int rv = Math.max(1, discGridH / (ICON_SZ + ICON_GAP));
            for (int i = discScroll * cols; i < Math.min(perks.size(), (discScroll + rv + 1) * cols); i++) {
                int col = i % cols, row = i / cols - discScroll;
                int ix = 8 + col * (ICON_SZ + ICON_GAP), iy = discGridY + row * (ICON_SZ + ICON_GAP) + 1;
                if (iy + ICON_SZ > discGridY + discGridH) break;
                if (inRect(mx, my, ix, iy, ICON_SZ, ICON_SZ)) {
                    PerkDefinition def = perks.get(i);
                    selectedId = def.id.equals(selectedId) ? null : def.id;
                    return true;
                }
            }
        }

        // Detail panel buttons
        if (selectedId != null) {
            PerkDefinition def = NichirinPerkRegistry.getPerk(selectedId);
            if (def != null && data.hasDiscovered(selectedId)) {
                boolean equipped = data.isEquipped(selectedId);
                PerkTier tier    = equipped ? data.getTier(selectedId) : def.minTier;
                int btnX = cw - 8 - 80;
                int panelY = (cachedCh > 0 ? cachedCh : 250) - DETAIL_H;
                if (equipped) {
                    if (inRect(mx, my, btnX, panelY + 8, 78, 14)) {
                        sendAction(new PerkActionPacket(PerkActionPacket.Action.UNEQUIP, selectedId));
                        return true;
                    }
                    PerkUpgradeCost cost = def.getUpgradeCost(tier);
                    if (cost != null && inRect(mx, my, btnX, panelY + 28, 78, 14)) {
                        sendAction(new PerkActionPacket(PerkActionPacket.Action.UPGRADE, selectedId));
                        return true;
                    }
                } else {
                    if (inRect(mx, my, btnX, panelY + 8, 78, 14)) {
                        sendAction(new PerkActionPacket(PerkActionPacket.Action.EQUIP, selectedId, def.minTier.level));
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private int cachedCh = 0;

    public void render(GuiGraphics g, Player player, Font font, int cw, int ch, int mx, int my) {
        cachedCh = ch;
        renderInternal(g, player, font, cw, ch, mx, my);
    }

    // redirect so the field-setting render above works with the inner render logic
    private void renderInternal(GuiGraphics g, Player player, Font font, int cw, int ch, int mx, int my) {
        PerkData data = ClientPerkCache.get();
        cachedCw = cw;
        hoverPerkId = null;
        mouseInDisc = false;
        mouseInLock = false;

        g.fill(0, 0, cw, ch, C_BG);
        for (int i = 0; i < 24; i++) {
            int a = (int)(0x12 * (1.0 - i / 24.0));
            g.fill(0, i, cw, i + 1, (a << 24) | 0x303030);
        }

        int cols = Math.max(2, (cw - PAD * 2 + ICON_GAP) / (ICON_SZ + ICON_GAP));
        boolean showDetail = selectedId != null && data.hasDiscovered(selectedId);
        int usableCh = showDetail ? ch - DETAIL_H - 4 : ch;

        int y = PAD;
        y = renderEquippedSection(g, font, data, cw, y, mx, my);
        eqSectionBottom = y;
        y += PAD;

        List<PerkDefinition> disc = getDiscovered(data);
        int remaining  = usableCh - y - PAD;
        int discRowCnt = Math.max(1, (disc.size() + cols - 1) / cols);
        discGridH = Math.min(remaining / 2, discRowCnt * (ICON_SZ + ICON_GAP) + 2);
        discGridH = Math.max(ICON_SZ + 6, discGridH);

        renderSectionHeader(g, font, cw, y, "DISCOVERED",
                disc.size() + " / " + NichirinPerkRegistry.allPerks().size(), true, data, mx, my);
        discGridY = y + SEC_H + 2;
        renderDiscGrid(g, font, data, disc, cols, PAD, discGridY, cw - PAD * 2, discGridH, mx, my);
        y = discGridY + discGridH + PAD;

        List<PerkDefinition> locked = getLocked(data);
        lockGridH = Math.max(ICON_SZ + 6, usableCh - y - SEC_H - PAD - 2);
        renderSectionHeader(g, font, cw, y, "UNDISCOVERED", locked.size() + " remaining", false, null, mx, my);
        lockGridY = y + SEC_H + 2;
        renderLockGrid(g, font, data, locked, cols, PAD, lockGridY, cw - PAD * 2, lockGridH, mx, my);

        if (showDetail) renderDetailPanel(g, font, data, selectedId, cw, ch, mx, my);
        if (hoverPerkId != null) renderTooltip(g, font, data, hoverPerkId, cw, ch, hoverMx, hoverMy);
    }

    public boolean handleKeyTyped(char c, int keyCode) {
        if (!searchActive) return false;
        if (keyCode == 259) { if (!searchText.isEmpty()) searchText = searchText.substring(0, searchText.length() - 1); }
        else if (keyCode == 256) { searchActive = false; }
        else if (c >= 32 && c < 127 && searchText.length() < 22) searchText += c;
        discScroll = 0;
        return true;
    }

    public boolean handleScroll(double delta) {
        if (mouseInLock) lockScroll = (int) Math.max(0, lockScroll - delta);
        else discScroll = (int) Math.max(0, discScroll - delta);
        return true;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private List<PerkDefinition> getDiscovered(PerkData data) {
        List<PerkDefinition> r = new ArrayList<>();
        for (PerkDefinition d : NichirinPerkRegistry.allPerks()) if (data.hasDiscovered(d.id)) r.add(d);
        return r;
    }

    private List<PerkDefinition> getLocked(PerkData data) {
        List<PerkDefinition> r = new ArrayList<>();
        for (PerkDefinition d : NichirinPerkRegistry.allPerks()) if (!data.hasDiscovered(d.id)) r.add(d);
        return r;
    }

    private void sortPerks(List<PerkDefinition> list, PerkData data) {
        switch (sort) {
            case NAME   -> list.sort(Comparator.comparing(d -> d.name));
            case CURSED -> list.sort((a, b) -> Boolean.compare(b.cursed, a.cursed));
            case TIER   -> list.sort((a, b) -> {
                boolean ae = data.isEquipped(a.id), be = data.isEquipped(b.id);
                if (ae != be) return ae ? -1 : 1;
                int at = (ae ? data.getTier(a.id) : a.minTier).level;
                int bt = (be ? data.getTier(b.id) : b.minTier).level;
                return bt - at;
            });
        }
    }

    private static void sendAction(PerkActionPacket packet) {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            packet.toBytes(buf);
            NetworkManager.sendToServer(NichirinPacketRegistry.PERK_ACTION_ID, buf);
        } catch (Exception ignored) {}
    }

    private static int countClientItem(net.minecraft.world.item.Item item) {
        Player p = Minecraft.getInstance().player;
        if (p == null) return 0;
        int n = 0;
        for (ItemStack s : p.getInventory().items) if (s.is(item)) n += s.getCount();
        return n;
    }

    private static int approxW(String s) { return s.length() * 6; }

    private static void drawPillBtn(GuiGraphics g, Font font, int x, int y, int w, int h,
                                    String label, int borderCol, int textCol, boolean hov) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, borderCol);
        g.fill(x, y, x + w, y + h, hov ? C_PANEL_LITE : C_PANEL2);
        if (hov) g.fill(x, y, x + w, y + h, OV_HOVER);
        g.drawString(font, label, x + (w - font.width(label)) / 2, y + (h - font.lineHeight) / 2, textCol, false);
    }

    private static List<String> wordWrap(Font font, String text, int maxW) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return lines;
        String[] words = text.split(" ");
        StringBuilder cur = new StringBuilder();
        for (String w : words) {
            String test = cur.length() == 0 ? w : cur + " " + w;
            if (font.width(test) > maxW && cur.length() > 0) { lines.add(cur.toString()); cur = new StringBuilder(w); }
            else cur = new StringBuilder(test);
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }

    private static boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
