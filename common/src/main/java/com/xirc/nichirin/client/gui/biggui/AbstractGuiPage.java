package com.xirc.nichirin.client.gui.biggui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared drawing base for Big GUI pages and nested tabs.
 *
 * <p>The concrete pages currently expose a few different render signatures,
 * so this class intentionally provides reusable palette, button, text, and
 * layout primitives without forcing a single render contract.</p>
 */
public abstract class AbstractGuiPage {

    protected static final int DEFAULT_PAD = 16;
    protected static final int DEFAULT_GAP = 14;
    protected static final int WORKSPACE_HEADER_H = 64;
    protected static final int WORKSPACE_FOOTER_H = 28;

    protected enum COLOR_PALETTE {
        BG(0x0D0D0D),
        PANEL_DEEP(0x111111),
        PANEL(0x171717),
        PANEL_DARK(0x121212),
        PANEL_MID(0x1D1D1D),
        PANEL_HOVER(0x252525),
        BORDER(0x303030),
        BORDER_HI(0x4A4A4A),
        TEXT(0xE7E2DA),
        TEXT_DIM(0x8B8780),
        TEXT_FAINT(0x56524E),
        ACCENT(0xE0B15B),
        ACCENT_LIGHT(0xFFE0A6),
        GREEN(0x76C47A),
        RED(0xD05C5C),
        DANGER(0xFF5555),
        DEMON_RED(0xFF3333),
        SLAYER_BLUE(0x5555FF),
        BREATH_CYAN(0x55FFFF),
        WHITE(0xFFFFFF),
        GOLD(0xFFD700),
        GRAY(0xAAAAAA),
        PLACEHOLDER(0x666666),
        MUTED(0x777777),
        DARK_GRAY(0x555555),
        DOTS(0x444444),
        BLACK(0x000000),
        TOOLTIP_FILL(0x202020, 0xE0),
        XP_BAR_BG(0x262320),
        HINT_BG(0x1F1F1F),
        HINT_TOP(0x343434),
        PILL_BG(0x1C1A16);

        private final int argb;

        COLOR_PALETTE(int rgb) {
            this(rgb, 0xFF);
        }

        COLOR_PALETTE(int rgb, int alpha) {
            this.argb = ((alpha & 0xFF) << 24) | (rgb & 0x00FFFFFF);
        }

        public int argb() {
            return argb;
        }

        public int rgb() {
            return argb & 0x00FFFFFF;
        }

        public int withAlpha(int alpha) {
            return ((alpha & 0xFF) << 24) | rgb();
        }
    }

    protected static class GuiButton {
        public final int x;
        public final int y;
        public final int width;
        public final int height;
        public final String label;
        public final int borderColor;
        public final int textColor;
        public final boolean enabled;

        public GuiButton(int x, int y, int width, int height, String label, int borderColor, int textColor, boolean enabled) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.label = label;
            this.borderColor = borderColor;
            this.textColor = textColor;
            this.enabled = enabled;
        }

        public boolean contains(double mouseX, double mouseY) {
            return uiHit(mouseX, mouseY, x, y, width, height);
        }
    }

    protected void fillPageBackground(GuiGraphics graphics, int width, int height) {
        graphics.fill(0, 0, width, height, COLOR_PALETTE.BG.argb());
    }

    protected void drawWorkspaceChrome(GuiGraphics graphics, Font font, int width, int height,
                                       String title, String subtitle, String rightStatus, String footerLeft, String footerRight) {
        fillPageBackground(graphics, width, height);
        graphics.fill(0, 0, width, WORKSPACE_HEADER_H, COLOR_PALETTE.PANEL.argb());
        graphics.fill(0, WORKSPACE_HEADER_H - 1, width, WORKSPACE_HEADER_H, COLOR_PALETTE.BORDER.argb());
        graphics.drawString(font, title, DEFAULT_PAD, 11, COLOR_PALETTE.TEXT.rgb(), false);
        if (subtitle != null && !subtitle.isEmpty()) {
            graphics.drawString(font, subtitle, DEFAULT_PAD, 27, COLOR_PALETTE.TEXT_DIM.rgb(), false);
        }
        if (rightStatus != null && !rightStatus.isEmpty()) {
            graphics.drawString(font, rightStatus, width - DEFAULT_PAD - font.width(rightStatus), 22, COLOR_PALETTE.TEXT_DIM.rgb(), false);
        }

        int footerY = height - WORKSPACE_FOOTER_H;
        graphics.fill(0, footerY, width, height, COLOR_PALETTE.PANEL.argb());
        graphics.fill(0, footerY, width, footerY + 1, COLOR_PALETTE.BORDER.argb());
        if (footerLeft != null && !footerLeft.isEmpty()) {
            graphics.drawString(font, footerLeft, DEFAULT_PAD, footerY + 10, COLOR_PALETTE.TEXT_DIM.rgb(), false);
        }
        if (footerRight != null && !footerRight.isEmpty()) {
            graphics.drawString(font, footerRight, width - DEFAULT_PAD - font.width(footerRight), footerY + 10, COLOR_PALETTE.TEXT_DIM.rgb(), false);
        }
    }

    protected int workspaceBodyY() {
        return WORKSPACE_HEADER_H;
    }

    protected int workspaceBodyHeight(int pageHeight) {
        return Math.max(0, pageHeight - WORKSPACE_HEADER_H - WORKSPACE_FOOTER_H);
    }

    protected void drawInfoCard(GuiGraphics graphics, Font font, int x, int y, int width, int height,
                                String title, String body, int accentColor) {
        drawBorderedRect(graphics, x, y, width, height, COLOR_PALETTE.BORDER.argb(), COLOR_PALETTE.PANEL_MID.argb());
        graphics.fill(x, y, x + 2, y + height, accentColor);
        graphics.drawString(font, title, x + 12, y + 10, COLOR_PALETTE.TEXT.rgb(), false);
        int textY = y + 26;
        for (String line : uiWordWrap(font, body, width - 24)) {
            if (textY + font.lineHeight > y + height - 8) break;
            graphics.drawString(font, line, x + 12, textY, COLOR_PALETTE.TEXT_DIM.rgb(), false);
            textY += font.lineHeight + 2;
        }
    }

    protected void drawBorderedRect(GuiGraphics graphics, int x, int y, int width, int height, int borderColor, int fillColor) {
        graphics.fill(x, y, x + width, y + height, borderColor);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, fillColor);
    }

    protected void drawPopPanel(GuiGraphics graphics, int x, int y, int width, int height, int accentColor) {
        graphics.fill(x - 2, y - 2, x + width + 2, y + height + 2, withAlpha(accentColor, 0x22));
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, COLOR_PALETTE.BORDER.argb());
        graphics.fill(x, y, x + width, y + height, COLOR_PALETTE.PANEL_MID.argb());
        graphics.fill(x, y, x + width, y + 2, accentColor);
        graphics.fill(x, y, x + 2, y + height, withAlpha(accentColor, 0xCC));
    }

    protected void drawAccentTitle(GuiGraphics graphics, Font font, Component title, int centerX, int y, int accentColor) {
        int titleX = centerX - font.width(title) / 2;
        graphics.drawString(font, title, titleX, y, COLOR_PALETTE.TEXT.rgb());
        int underlineW = Math.min(82, Math.max(28, font.width(title) + 10));
        int underlineX = centerX - underlineW / 2;
        graphics.fill(underlineX, y + font.lineHeight + 4, underlineX + underlineW, y + font.lineHeight + 6, accentColor);
        graphics.fill(underlineX - 8, y + font.lineHeight + 5, underlineX, y + font.lineHeight + 6, withAlpha(accentColor, 0x45));
        graphics.fill(underlineX + underlineW, y + font.lineHeight + 5, underlineX + underlineW + 8, y + font.lineHeight + 6, withAlpha(accentColor, 0x45));
    }

    protected void drawHorizontalDivider(GuiGraphics graphics, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 1, COLOR_PALETTE.BORDER.argb());
    }

    protected void drawVerticalDivider(GuiGraphics graphics, int x, int y, int height) {
        graphics.fill(x, y, x + 1, y + height, COLOR_PALETTE.BORDER.argb());
    }

    protected void drawButton(GuiGraphics graphics, Font font, GuiButton button, double mouseX, double mouseY) {
        boolean hovered = button.enabled && button.contains(mouseX, mouseY);
        int border = button.enabled ? button.borderColor : COLOR_PALETTE.BORDER.argb();
        int fill = hovered ? COLOR_PALETTE.PANEL_HOVER.argb() : COLOR_PALETTE.PANEL_MID.argb();
        drawBorderedRect(graphics, button.x, button.y, button.width, button.height, border, fill);
        int text = button.enabled ? button.textColor : COLOR_PALETTE.TEXT_FAINT.rgb();
        graphics.drawString(font, button.label,
                button.x + (button.width - font.width(button.label)) / 2,
                button.y + (button.height - font.lineHeight) / 2,
                text,
                false);
    }

    protected void drawPill(GuiGraphics graphics, Font font, int x, int y, int width, int height,
                            String label, int borderColor, int textColor, boolean hovered) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, borderColor);
        graphics.fill(x, y, x + width, y + height, hovered ? COLOR_PALETTE.PANEL_HOVER.argb() : COLOR_PALETTE.PANEL_MID.argb());
        graphics.drawString(font, label,
                x + (width - font.width(label)) / 2,
                y + (height - font.lineHeight) / 2,
                textColor,
                false);
    }

    protected void drawCenteredString(GuiGraphics graphics, Font font, String text, int centerX, int y, int color) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
    }

    protected void drawEmptyState(GuiGraphics graphics, Font font, int width, int height, String text) {
        drawCenteredString(graphics, font, text, width / 2, height / 2, COLOR_PALETTE.TEXT_DIM.rgb());
    }

    protected void drawComingSoon(GuiGraphics graphics, Font font, Component label, int x, int y) {
        int width = Math.max(134, font.width(label) + 26);
        int height = 30;
        drawPopPanel(graphics, x, y, width, height, COLOR_PALETTE.ACCENT.argb());
        graphics.drawString(font, label, x + 13, y + 11, COLOR_PALETTE.TEXT.rgb(), false);
    }

    protected static int withAlpha(int color, int alpha) {
        return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
    }

    protected static String uiTrimToWidth(Font font, String text, int maxWidth) {
        if (text == null) return "";
        if (font.width(text) <= maxWidth) return text;
        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width("..."))) + "...";
    }

    protected static List<String> uiWordWrap(Font font, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return lines;
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String test = current.length() == 0 ? word : current + " " + word;
            if (font.width(test) > maxWidth && current.length() > 0) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(test);
            }
        }
        if (current.length() > 0) lines.add(current.toString());
        return lines;
    }

    protected static boolean uiHit(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
