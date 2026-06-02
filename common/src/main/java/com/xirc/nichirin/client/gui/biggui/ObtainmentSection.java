package com.xirc.nichirin.client.gui.biggui;

import com.xirc.nichirin.common.data.MovesetProgression;
import com.xirc.nichirin.common.data.ProgressionHelper;
import com.xirc.nichirin.common.event.unlock.FlameBreathingUnlockHandler;
import com.xirc.nichirin.registry.NichirinItemRegistry;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Obtainment subtab — shows how to unlock each moveset and which ones the player has.
 * Renders inside the Moveset workspace's body region, so it draws NO chrome of its own.
 *
 * <p>Behavior:</p>
 * <ul>
 *   <li>Sorted unlocked-first within each section so the player sees their kit at a glance.</li>
 *   <li>Clicking an unlocked card requests the server to equip that style.</li>
 *   <li>Locked entries show a live progress hint when one is available (e.g. flame burn timer).</li>
 *   <li>Secret styles in the lang file but without an unlock handler appear as "???" placeholders.</li>
 * </ul>
 */
public class ObtainmentSection extends AbstractGuiPage {

    /** A moveset entry. {@code translationKey} resolves the display name via the language file. */
    private record Entry(String id, String translationKey, int accentColor) {}

    private static final List<Entry> BREATHING_STYLES = List.of(
            new Entry("water_breathing",   "breathing_style.water_breathing",   0xFF55AAFF),
            new Entry("flame_breathing",   "breathing_style.flame_breathing",   0xFFFF5500),
            new Entry("thunder_breathing", "breathing_style.thunder_breathing", 0xFFFFFF55),
            new Entry("sound_breathing",   "breathing_style.sound_breathing",   0xFF00CED1),
            new Entry("insect_breathing",  "breathing_style.insect_breathing",  0xFF9932CC),
            new Entry("beast_breathing",   "breathing_style.beast_breathing",   0xFF8B4513),
            new Entry("mist_breathing",    "breathing_style.mist_breathing",    0xFFB0C4DE)
    );

    private static final int CARD_H = 50;
    private static final int GAP = 8;
    private static final int DEMON_CARD_H = 36;
    private static final long CLICK_COOLDOWN_MS = 500;

    /** Last hit-test layout snapshot, recorded each render so click handling can find cards. */
    private final List<CardRect> hitRects = new ArrayList<>();
    private long lastClickTime = 0;

    /** Geometry of a rendered breathing-style card, used to translate clicks back to an entry id. */
    private record CardRect(int x, int y, int w, int h, String movesetId, boolean unlocked) {
        boolean contains(double mx, double my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
    }

    public void render(GuiGraphics graphics, Player player, Font font,
                       int contentWidth, int contentHeight, int mouseX, int mouseY) {
        hitRects.clear();

        int x = DEFAULT_PAD;
        int y = DEFAULT_PAD;
        int rowW = contentWidth - DEFAULT_PAD * 2;
        int bottomLimit = contentHeight - DEFAULT_PAD;

        // Sort unlocked-first within breathing styles; preserve insertion order within each group.
        List<Entry> orderedBreathing = new ArrayList<>(BREATHING_STYLES);
        orderedBreathing.sort(Comparator.comparing(
                (Entry e) -> ProgressionHelper.isStyleUnlocked(player, e.id()) ? 0 : 1));

        y = drawHeader(graphics, font,
                Component.translatable("gui.nichirin.obtainment.header.breathing_styles").getString(),
                x, y, COLOR_PALETTE.BREATH_CYAN.rgb());
        for (Entry e : orderedBreathing) {
            if (y + CARD_H > bottomLimit) return;
            renderEntry(graphics, font, player, e, x, y, rowW);
            y += CARD_H + GAP;
        }

        // ── Demon arts ──
        y += GAP;
        if (y + font.lineHeight + 10 + DEMON_CARD_H > bottomLimit) return;
        y = drawHeader(graphics, font,
                Component.translatable("gui.nichirin.obtainment.header.demon_arts").getString(),
                x, y, COLOR_PALETTE.DEMON_RED.rgb());
        String comingSoon = Component.translatable("gui.nichirin.obtainment.demon_coming_soon").getString();
        drawInfoCard(graphics, font, x, y, rowW, DEMON_CARD_H,
                comingSoon, "", COLOR_PALETTE.DEMON_RED.argb());
    }

    private int drawHeader(GuiGraphics graphics, Font font, String label, int x, int y, int color) {
        graphics.drawString(font, label, x, y, color, false);
        drawHorizontalDivider(graphics, x, y + font.lineHeight + 3, 220);
        return y + font.lineHeight + 10;
    }

    private void renderEntry(GuiGraphics graphics, Font font, Player player, Entry e, int x, int y, int width) {
        boolean unlocked = ProgressionHelper.isStyleUnlocked(player, e.id());
        int accent = unlocked ? e.accentColor() : COLOR_PALETTE.MUTED.argb();
        drawPopPanel(graphics, x, y, width, CARD_H, accent);

        // Display name (translatable)
        String displayName = Component.translatable(e.translationKey()).getString();
        graphics.drawString(font, displayName, x + 12, y + 10,
                unlocked ? COLOR_PALETTE.TEXT.rgb() : COLOR_PALETTE.TEXT_DIM.rgb(), false);

        // Status pill on the right (translatable)
        String status = Component.translatable(unlocked
                ? "gui.nichirin.obtainment.unlocked"
                : "gui.nichirin.obtainment.locked").getString();
        int statusColor = unlocked ? COLOR_PALETTE.GREEN.rgb() : COLOR_PALETTE.DANGER.rgb();
        int statusW = font.width(status);
        graphics.drawString(font, status, x + width - 12 - statusW, y + 10, statusColor, false);

        // Body line: live progress hint if we have one, otherwise the requirement text.
        String requirement = ProgressionHelper.getUnlockRequirement(e.id());
        String progressHint = unlocked ? null : getProgressHint(player, e.id());
        String body = progressHint != null ? progressHint : requirement;
        int bodyColor = progressHint != null ? COLOR_PALETTE.ACCENT.rgb() : COLOR_PALETTE.TEXT_DIM.rgb();
        graphics.drawString(font, uiTrimToWidth(font, body, width - 24), x + 12, y + 28, bodyColor, false);

        hitRects.add(new CardRect(x, y, width, CARD_H, e.id(), unlocked));
    }

    /**
     * Returns a one-line progress string for in-progress unlocks, or null if there's nothing to show.
     * Only handlers that actually track progress contribute here — most unlocks are atomic conditions.
     */
    private String getProgressHint(Player player, String styleId) {
        return switch (styleId) {
            case "flame_breathing" -> {
                int progress = FlameBreathingUnlockHandler.getBurnProgress(player.getUUID());
                int required = FlameBreathingUnlockHandler.getRequiredBurnTicks();
                if (progress <= 0) yield null;
                yield "On fire: " + (progress / 20) + " / " + (required / 20) + "s";
            }
            case "beast_breathing" -> {
                for (ItemStack stack : player.getInventory().items) {
                    if (stack.getItem() == NichirinItemRegistry.BOAR_HEAD.get()) {
                        yield "Boar head in inventory — equip it to unlock";
                    }
                }
                yield null;
            }
            default -> null;
        };
    }

    public boolean handleClick(double mouseX, double mouseY, Player player) {
        long now = System.currentTimeMillis();
        if (now - lastClickTime < CLICK_COOLDOWN_MS) return false;

        for (CardRect rect : hitRects) {
            if (rect.contains(mouseX, mouseY)) {
                if (!rect.unlocked()) return false; // Locked cards do nothing.
                // Quick-equip: tell the server to switch the player's active breathing style.
                NichirinPacketRegistry.requestMovesetChange(rect.movesetId());
                playClick();
                lastClickTime = now;
                return true;
            }
        }
        return false;
    }

    private void playClick() {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f));
    }
}