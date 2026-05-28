package com.xirc.nichirin.client.gui.biggui;

import com.xirc.nichirin.common.network.c2s.SheathConfigPacket;
import com.xirc.nichirin.common.system.sheathing.PlayerSheathData;
import com.xirc.nichirin.common.system.sheathing.SheathSlotData;
import com.xirc.nichirin.common.system.sheathing.SheathState;
import com.xirc.nichirin.common.system.sheathing.SheathingManager;
import com.xirc.nichirin.common.system.sheathing.UnsheatheAttackType;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

public class SheathingSection extends AbstractGuiPage {
    private static final int CARD_H = 112;
    private static final int BUTTON_H = 18;
    private static final int GAP = 12;

    public void render(GuiGraphics graphics, Player player, int width, int height, Font font, int mouseX, int mouseY) {
        PlayerSheathData data = SheathingManager.get(player);
        int headerY = 12;
        graphics.drawString(font, "Katana Sheathing", 16, headerY, COLOR_PALETTE.TEXT.rgb(), false);
        graphics.drawString(font, "Y sheaths or quickdraws. Shift + Y quick-sheathes. Stored katanas leave the hotbar.", 16, headerY + 14, COLOR_PALETTE.TEXT_DIM.rgb(), false);

        int cols = width >= 620 ? 2 : 1;
        int cardW = cols == 2 ? (width - 32 - GAP) / 2 : width - 32;
        int startY = 46;
        SheathSlotData[] slots = data.getSlots();
        for (int i = 0; i < slots.length; i++) {
            int col = i % cols;
            int row = i / cols;
            int x = 16 + col * (cardW + GAP);
            int y = startY + row * (CARD_H + GAP);
            renderSlotCard(graphics, font, slots[i], x, y, cardW, mouseX, mouseY);
        }
    }

    private void renderSlotCard(GuiGraphics graphics, Font font, SheathSlotData slot, int x, int y, int w, int mouseX, int mouseY) {
        int accent = switch (slot.getPosition()) {
            case LEFT_HIP -> 0xFFE0B15B;
            case RIGHT_HIP -> 0xFF76C47A;
            case BACK -> 0xFF55FFFF;
            case BACK_2 -> 0xFFD05C5C;
        };
        drawPopPanel(graphics, x, y, w, CARD_H, accent);
        graphics.drawString(font, slot.getPosition().getDisplayName(), x + 12, y + 10, COLOR_PALETTE.TEXT.rgb(), false);
        graphics.drawString(font, stateLabel(slot), x + w - 12 - font.width(stateLabel(slot)), y + 10, stateColor(slot), false);

        String storage = slot.hasStoredSword()
                ? uiTrimToWidth(font, slot.getStoredSword().getHoverName().getString(), w - 24)
                : "No katana stored";
        graphics.drawString(font, storage, x + 12, y + 28, slot.hasStoredSword() ? COLOR_PALETTE.ACCENT_LIGHT.rgb() : COLOR_PALETTE.TEXT_DIM.rgb(), false);
        graphics.drawString(font, "Hotbar " + (slot.getLinkedHotbarSlot() + 1) + "  Priority " + slot.getPriority(), x + 12, y + 42, COLOR_PALETTE.TEXT_DIM.rgb(), false);

        int bw = Math.max(74, (w - 36) / 3);
        drawButton(graphics, font, x + 12, y + 62, bw, "Enabled " + yesNo(slot.isEnabled()), mouseX, mouseY);
        drawButton(graphics, font, x + 18 + bw, y + 62, bw, "Hotbar " + (slot.getLinkedHotbarSlot() + 1), mouseX, mouseY);
        drawButton(graphics, font, x + 24 + bw * 2, y + 62, bw, "Priority " + slot.getPriority(), mouseX, mouseY);

        int lowerW = (w - 30) / 2;
        drawButton(graphics, font, x + 12, y + 86, lowerW, "Tap: " + shortAttack(slot.getTapAttack()), mouseX, mouseY);
        drawButton(graphics, font, x + 18 + lowerW, y + 86, lowerW, "Hold: " + shortAttack(slot.getHoldAttack()), mouseX, mouseY);
    }

    public boolean handleClick(double mouseX, double mouseY, Player player, int width) {
        PlayerSheathData data = SheathingManager.get(player);
        int cols = width >= 620 ? 2 : 1;
        int cardW = cols == 2 ? (width - 32 - GAP) / 2 : width - 32;
        int startY = 46;
        SheathSlotData[] slots = data.getSlots();
        for (int i = 0; i < slots.length; i++) {
            int col = i % cols;
            int row = i / cols;
            int x = 16 + col * (cardW + GAP);
            int y = startY + row * (CARD_H + GAP);
            if (handleSlotClick(mouseX, mouseY, slots[i], x, y, cardW, player)) return true;
        }
        return false;
    }

    private boolean handleSlotClick(double mouseX, double mouseY, SheathSlotData slot, int x, int y, int w, Player player) {
        int bw = Math.max(74, (w - 36) / 3);
        if (uiHit(mouseX, mouseY, x + 12, y + 62, bw, BUTTON_H)) {
            slot.setEnabled(!slot.isEnabled());
            sync(slot);
            return true;
        }
        if (uiHit(mouseX, mouseY, x + 18 + bw, y + 62, bw, BUTTON_H)) {
            slot.setLinkedHotbarSlot((slot.getLinkedHotbarSlot() + 1) % 9);
            sync(slot);
            return true;
        }
        if (uiHit(mouseX, mouseY, x + 24 + bw * 2, y + 62, bw, BUTTON_H)) {
            slot.setPriority(slot.getPriority() >= 4 ? 1 : slot.getPriority() + 1);
            sync(slot);
            return true;
        }
        int lowerW = (w - 30) / 2;
        if (uiHit(mouseX, mouseY, x + 12, y + 86, lowerW, BUTTON_H)) {
            slot.setTapAttack(next(slot.getTapAttack()));
            sync(slot);
            return true;
        }
        if (uiHit(mouseX, mouseY, x + 18 + lowerW, y + 86, lowerW, BUTTON_H)) {
            slot.setHoldAttack(next(slot.getHoldAttack()));
            sync(slot);
            return true;
        }
        return false;
    }

    private void drawButton(GuiGraphics graphics, Font font, int x, int y, int w, String label, int mouseX, int mouseY) {
        boolean hovered = uiHit(mouseX, mouseY, x, y, w, BUTTON_H);
        drawBorderedRect(graphics, x, y, w, BUTTON_H,
                hovered ? COLOR_PALETTE.BORDER_HI.argb() : COLOR_PALETTE.BORDER.argb(),
                hovered ? COLOR_PALETTE.PANEL_HOVER.argb() : COLOR_PALETTE.PANEL_DARK.argb());
        graphics.drawString(font, uiTrimToWidth(font, label, w - 8), x + 4, y + 5, COLOR_PALETTE.TEXT.rgb(), false);
    }

    private void sync(SheathSlotData slot) {
        NichirinPacketRegistry.sendToServer(new SheathConfigPacket(slot.getPosition(), slot.isEnabled(),
                slot.getLinkedHotbarSlot(), slot.getPriority(), slot.getTapAttack(), slot.getHoldAttack(), slot.isVisible()));
    }

    private UnsheatheAttackType next(UnsheatheAttackType type) {
        UnsheatheAttackType[] values = UnsheatheAttackType.values();
        return values[(type.ordinal() + 1) % values.length];
    }

    private String yesNo(boolean value) {
        return value ? "ON" : "OFF";
    }

    private String stateLabel(SheathSlotData slot) {
        if (slot.hasStoredSword() && slot.getState() == SheathState.SHEATHED) return "STORED";
        return slot.getState().name();
    }

    private int stateColor(SheathSlotData slot) {
        return switch (slot.getState()) {
            case SHEATHED -> COLOR_PALETTE.GREEN.rgb();
            case DRAWN -> COLOR_PALETTE.TEXT_DIM.rgb();
            case SHEATHING, UNSHEATHING -> COLOR_PALETTE.ACCENT_LIGHT.rgb();
            case COOLDOWN -> COLOR_PALETTE.RED.rgb();
            case EMPTY -> COLOR_PALETTE.TEXT_FAINT.rgb();
        };
    }

    private String shortAttack(UnsheatheAttackType type) {
        return switch (type) {
            case QUICKDRAW_SLASH -> "Quickdraw";
            case REVERSE_DRAW_SLASH -> "Reverse";
            case OVERHEAD_BACKDRAW -> "Overhead";
            case DIAGONAL_BACKDRAW -> "Diagonal";
            case SPRINTING_DRAW_DASH -> "Sprint";
            case CROUCHING_LOW_DRAW -> "Low";
            case AERIAL_DRAW_SLASH -> "Aerial";
            case CHARGED_DRAW_SLASH -> "Charged";
            case DUAL_CROSS_SLASH -> "Dual";
        };
    }
}
