package com.xirc.nichirin.client.gui.biggui;

import com.xirc.nichirin.client.data.ClientProgressionCache;
import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.data.ProgressionHelper;
import com.xirc.nichirin.registry.MovesetRegistry;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;

import static com.xirc.nichirin.common.data.ProgressionHelper.getUnlockRequirement;

/**
 * Breathing Styles selection panel inside the MOVESET section.
 *
 * Layout: 3-column grid so all 7 styles fit within the visible content area
 * without scrolling on typical screen sizes.
 */
public class BreathingStylesSection extends AbstractGuiPage {

    // All registered breathing styles in display order.
    static final String[] BREATHING_STYLES = {
            "water_breathing",
            "flame_breathing",
            "thunder_breathing",
            "insect_breathing",
            "sound_breathing",
            "mist_breathing",
            "beast_breathing",
    };

    private static final int TOP_MARGIN   = 20;
    private static final int COLS         = 3;
    private static final int BOX_WIDTH    = 130;
    private static final int BOX_HEIGHT   = 70;
    private static final int SPACING      = 15;

    // Render
    public void render(GuiGraphics graphics, Player player, int contentWidth, int contentHeight,
                       Font font, int mouseX, int mouseY) {

        int centerX    = (contentWidth - 20) / 2;
        int contentY   = TOP_MARGIN + 10;

        // Title
        Component title = Component.translatable("gui.nichirin.breathing_styles.title")
                .withStyle(s -> s.withBold(true));
        drawAccentTitle(graphics, font, title, centerX, contentY, COLOR_PALETTE.ACCENT.argb());
        contentY += 30;

        // Current style line (only when a style is active)
        String currentStyle = MovesetHelper.getBreathingMovesetId(player);
        if (currentStyle != null) {
            Component current = Component.translatable("gui.nichirin.breathing_styles.current",
                    Component.translatable("breathing_style." + currentStyle))
                    .withStyle(s -> s.withColor(COLOR_PALETTE.ACCENT.rgb()));
            graphics.drawString(font, current, 20, contentY, COLOR_PALETTE.ACCENT.rgb());
            contentY += 25;
        }

        // Instructions
        Component instructions = Component.translatable("gui.nichirin.breathing_styles.instructions");
        graphics.drawString(font, instructions, 20, contentY, COLOR_PALETTE.TEXT_DIM.rgb());
        contentY += 20;

        // Grid
        int gridY      = contentY + 10;
        int totalWidth = (BOX_WIDTH * COLS) + (SPACING * (COLS - 1));
        int startX     = centerX - totalWidth / 2;

        String hoveredLocked = null;

        for (int i = 0; i < BREATHING_STYLES.length; i++) {
            String styleId = BREATHING_STYLES[i];
            int row = i / COLS;
            int col = i % COLS;

            int x = startX + col * (BOX_WIDTH + SPACING);
            int y = gridY  + row * (BOX_HEIGHT + SPACING);

            if (mouseX >= x && mouseX <= x + BOX_WIDTH && mouseY >= y && mouseY <= y + BOX_HEIGHT
                    && !isStyleUnlocked(player, styleId)) {
                hoveredLocked = styleId;
            }

            renderBox(graphics, font, player, currentStyle, styleId, x, y);
        }

        // Tooltip for hovered-locked style
        if (hoveredLocked != null) {
            String req = getUnlockRequirement(hoveredLocked);
            Component tooltip = Component.literal(req)
                    .withStyle(s -> s.withColor(COLOR_PALETTE.ACCENT_LIGHT.rgb()).withBold(true));
            graphics.drawString(font, tooltip, centerX - font.width(tooltip) / 2, 50,
                    COLOR_PALETTE.ACCENT_LIGHT.rgb());
        }

        // "None" button
        int lastRow      = (BREATHING_STYLES.length - 1) / COLS;
        int noneY        = gridY + (lastRow + 1) * (BOX_HEIGHT + SPACING) + 20;
        int noneX        = centerX - 75;
        int noneW        = 150;
        int noneH        = 20;

        int noneBg     = currentStyle == null ? COLOR_PALETTE.PANEL_HOVER.argb() : COLOR_PALETTE.PANEL_MID.argb();
        int noneBorder = currentStyle == null ? COLOR_PALETTE.ACCENT.argb()      : COLOR_PALETTE.BORDER_HI.argb();

        graphics.fill(noneX - 1, noneY - 1, noneX + noneW + 1, noneY + noneH + 1, noneBorder);
        graphics.fill(noneX, noneY, noneX + noneW, noneY + noneH, noneBg);

        Component noneText  = Component.translatable("gui.nichirin.breathing_styles.none");
        int noneTextColor   = currentStyle == null ? COLOR_PALETTE.ACCENT.rgb() : COLOR_PALETTE.TEXT_DIM.rgb();
        graphics.drawString(font, noneText,
                noneX + (noneW - font.width(noneText)) / 2,
                noneY + 6, noneTextColor);
    }

    /** Renders a single breathing-style selection box. */
    private void renderBox(GuiGraphics graphics, Font font, Player player,
                           String currentStyle, String styleId, int x, int y) {
        boolean unlocked  = isStyleUnlocked(player, styleId);
        boolean selected  = styleId.equals(currentStyle);

        int bg     = unlocked ? (selected ? COLOR_PALETTE.PANEL_HOVER.argb() : COLOR_PALETTE.PANEL_MID.argb())
                              : COLOR_PALETTE.PANEL_DARK.argb();
        int border = unlocked ? (selected ? COLOR_PALETTE.ACCENT.argb() : COLOR_PALETTE.BORDER_HI.argb())
                              : COLOR_PALETTE.BORDER.argb();

        if (selected) {
            graphics.fill(x - 3, y - 3, x + BOX_WIDTH + 3, y + BOX_HEIGHT + 3, withAlpha(border, 0x24));
        }
        graphics.fill(x - 1, y - 1, x + BOX_WIDTH + 1, y + BOX_HEIGHT + 1, border);
        graphics.fill(x, y, x + BOX_WIDTH, y + BOX_HEIGHT, bg);
        if (unlocked) {
            graphics.fill(x, y, x + BOX_WIDTH, y + 2, withAlpha(border, selected ? 0xEE : 0x80));
            graphics.fill(x, y, x + 2, y + BOX_HEIGHT, withAlpha(border, selected ? 0xDD : 0x55));
        }

        Component name = Component.literal(formatStyleName(styleId));
        int nameColor  = unlocked ? COLOR_PALETTE.TEXT.rgb() : COLOR_PALETTE.TEXT_FAINT.rgb();
        graphics.drawString(font, name,
                x + (BOX_WIDTH - font.width(name)) / 2, y + 8, nameColor);

        if (!unlocked) {
            Component locked = Component.translatable("gui.nichirin.breathing_styles.locked_status")
                    .withStyle(s -> s.withColor(COLOR_PALETTE.TEXT_FAINT.rgb()));
            graphics.drawString(font, locked,
                    x + (BOX_WIDTH - font.width(locked)) / 2, y + 26,
                    COLOR_PALETTE.TEXT_FAINT.rgb());
        } else if (selected) {
            Component equipped = Component.translatable("gui.nichirin.breathing_styles.equipped")
                    .withStyle(s -> s.withColor(COLOR_PALETTE.ACCENT.rgb()));
            graphics.drawString(font, equipped,
                    x + (BOX_WIDTH - font.width(equipped)) / 2, y + 26,
                    COLOR_PALETTE.ACCENT.rgb());
        } else {
            Component click = Component.translatable("gui.nichirin.breathing_styles.click_to_select")
                    .withStyle(s -> s.withColor(COLOR_PALETTE.TEXT_DIM.rgb()));
            graphics.drawString(font, click,
                    x + (BOX_WIDTH - font.width(click)) / 2, y + 26,
                    COLOR_PALETTE.TEXT_DIM.rgb());
        }

        // Icon placeholder
        int iconBg = unlocked ? COLOR_PALETTE.PANEL_HOVER.argb() : COLOR_PALETTE.PANEL_MID.argb();
        int iconBorder = unlocked ? withAlpha(border, 0xAA) : COLOR_PALETTE.BORDER.argb();
        graphics.fill(x + BOX_WIDTH / 2 - 13, y + 43, x + BOX_WIDTH / 2 + 13, y + 65, iconBorder);
        graphics.fill(x + BOX_WIDTH / 2 - 12, y + 44, x + BOX_WIDTH / 2 + 12, y + 64, iconBg);
    }

    // Click handling
    private long lastClickTime = 0;
    private static final long CLICK_COOLDOWN_MS = 500;

    public boolean handleClick(double mouseX, double mouseY, Player player, int contentWidth) {
        long now = System.currentTimeMillis();
        if (now - lastClickTime < CLICK_COOLDOWN_MS) return false;

        String currentStyle = MovesetHelper.getBreathingMovesetId(player);
        int    centerX      = (contentWidth - 20) / 2;
        int    totalWidth   = (BOX_WIDTH * COLS) + (SPACING * (COLS - 1));
        int    startX       = centerX - totalWidth / 2;

        int gridY = TOP_MARGIN + 10 + 30 + 20 + 10;
        if (currentStyle != null) gridY += 25;

        // Style boxes
        for (int i = 0; i < BREATHING_STYLES.length; i++) {
            String styleId = BREATHING_STYLES[i];
            int row = i / COLS;
            int col = i % COLS;

            int x = startX + col * (BOX_WIDTH + SPACING);
            int y = gridY  + row * (BOX_HEIGHT + SPACING);

            if (mouseX >= x && mouseX <= x + BOX_WIDTH && mouseY >= y && mouseY <= y + BOX_HEIGHT) {
                return handleStyleClick(player, styleId, currentStyle, now);
            }
        }

        // "None" button
        int lastRow = (BREATHING_STYLES.length - 1) / COLS;
        int noneY   = gridY + (lastRow + 1) * (BOX_HEIGHT + SPACING) + 20;
        int noneX   = centerX - 75;
        int noneW   = 150;
        int noneH   = 20;

        if (mouseX >= noneX && mouseX <= noneX + noneW && mouseY >= noneY && mouseY <= noneY + noneH) {
            NichirinPacketRegistry.requestStyleChange(null);
            playClick(1.0f);
            lastClickTime = now;
            return true;
        }

        return false;
    }

    private boolean handleStyleClick(Player player, String styleId, String currentStyle, long now) {
        if (!isStyleUnlocked(player, styleId)) {
            playClick(0.5f, 0.8f);
            lastClickTime = now;
            return true;
        }
        if (!styleId.equals(currentStyle)) {
            NichirinPacketRegistry.requestStyleChange(styleId);
            playClick(1.0f);
        }
        lastClickTime = now;
        return true;
    }

    // Helpers
    /** Computes the Y coordinate of the grid top row, matching render(). */
    static int gridTopY(String currentStyle) {
        int y = TOP_MARGIN + 10 + 30 + 20 + 10;
        if (currentStyle != null) y += 25;
        return y;
    }

    /** Number of rows the grid occupies (used by MovesetSection for subtab placement). */
    static int gridRows() {
        return (int) Math.ceil((double) BREATHING_STYLES.length / COLS);
    }

    /** Y coordinate of the bottom of the "None" button. */
    static int noneButtonBottomY(String currentStyle) {
        int gridY   = gridTopY(currentStyle);
        int lastRow = (BREATHING_STYLES.length - 1) / COLS;
        return gridY + (lastRow + 1) * (BOX_HEIGHT + SPACING) + 20 + 20; // +20 = button height
    }

    private boolean isStyleUnlocked(Player player, String styleId) {
        if (player.level().isClientSide()) {
            return ClientProgressionCache.isUnlocked(styleId);
        }
        if (!MovesetRegistry.isRegistered(styleId)) return false;
        return ProgressionHelper.isStyleUnlocked(player, styleId);
    }

    private String formatStyleName(String styleId) {
        String[] parts = styleId.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    private static void playClick(float volume) {
        playClick(volume, 1.0f);
    }

    private static void playClick(float volume, float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), volume, pitch));
    }
}
