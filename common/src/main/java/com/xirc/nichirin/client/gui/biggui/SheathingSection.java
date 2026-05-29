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
    private static final int CARD_H = 124;
    private static final int BUTTON_H = 18;
    private static final int GAP = 12;

    public void render(GuiGraphics graphics, Player player, int width, int height, Font font, int mouseX, int mouseY) {
        PlayerSheathData data = SheathingManager.get(player);
        int headerY = 12;
        graphics.drawString(font, "Katana Sheathing", 16, headerY, COLOR_PALETTE.TEXT.rgb(), false);
        graphics.drawString(font, "Y stores the selected katana. Select the empty linked slot and release Y to quickdraw.", 16, headerY + 14, COLOR_PALETTE.TEXT_DIM.rgb(), false);

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
        String title = slot.getPosition().getDisplayName();
        graphics.drawString(font, title, x + 12, y + 10, COLOR_PALETTE.TEXT.rgb(), false);
        graphics.drawString(font, stateLabel(slot), x + w - 12 - font.width(stateLabel(slot)), y + 10, stateColor(slot), false);

        String storage = slot.hasStoredSword()
                ? uiTrimToWidth(font, slot.getStoredSword().getHoverName().getString(), w - 24)
                : "No katana stored";
        graphics.drawString(font, storage, x + 12, y + 28, slot.hasStoredSword() ? COLOR_PALETTE.ACCENT_LIGHT.rgb() : COLOR_PALETTE.TEXT_DIM.rgb(), false);
        graphics.drawString(font, slotMeta(slot), x + 12, y + 42, COLOR_PALETTE.TEXT_DIM.rgb(), false);
        graphics.drawString(font, "Quickdraw: " + slot.getTapAttack().getDisplayName(), x + 12, y + 56, COLOR_PALETTE.TEXT_DIM.rgb(), false);

        int bw = Math.max(74, (w - 36) / 3);
        drawButton(graphics, font, x + 12, y + 72, bw, yesNo(slot.isEnabled()), mouseX, mouseY);
        drawButton(graphics, font, x + 18 + bw, y + 72, bw, "Slot " + (slot.getLinkedHotbarSlot() + 1), mouseX, mouseY);
        drawButton(graphics, font, x + 24 + bw * 2, y + 72, bw, "Prio " + slot.getPriority(), mouseX, mouseY);

        drawButton(graphics, font, x + 12, y + 96, w - 24, "Quickdraw " + shortAttack(slot.getTapAttack()), mouseX, mouseY);
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
        if (uiHit(mouseX, mouseY, x + 12, y + 72, bw, BUTTON_H)) {
            slot.setEnabled(!slot.isEnabled());
            sync(slot);
            return true;
        }
        if (uiHit(mouseX, mouseY, x + 18 + bw, y + 72, bw, BUTTON_H)) {
            slot.setLinkedHotbarSlot(nextFreeHotbarSlot(player, slot));
            sync(slot);
            return true;
        }
        if (uiHit(mouseX, mouseY, x + 24 + bw * 2, y + 72, bw, BUTTON_H)) {
            slot.setPriority(slot.getPriority() >= 4 ? 1 : slot.getPriority() + 1);
            sync(slot);
            return true;
        }
        if (uiHit(mouseX, mouseY, x + 12, y + 96, w - 24, BUTTON_H)) {
            slot.setTapAttack(next(slot.getTapAttack()));
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
                slot.getLinkedHotbarSlot(), slot.getPriority(), slot.getTapAttack(), slot.getTapAttack(), slot.isVisible()));
    }

    private UnsheatheAttackType next(UnsheatheAttackType type) {
        UnsheatheAttackType[] values = UnsheatheAttackType.values();
        return values[(type.ordinal() + 1) % values.length];
    }

    private String yesNo(boolean value) {
        return value ? "Enabled" : "Disabled";
    }

    private int nextFreeHotbarSlot(Player player, SheathSlotData slot) {
        PlayerSheathData data = SheathingManager.get(player);
        int current = slot.getLinkedHotbarSlot();
        for (int step = 1; step <= 9; step++) {
            int candidate = (current + step) % 9;
            boolean used = false;
            for (SheathSlotData other : data.getSlots()) {
                if (other != slot && other.isEnabled() && other.getLinkedHotbarSlot() == candidate) {
                    used = true;
                    break;
                }
            }
            if (!used) return candidate;
        }
        return current;
    }

    private String slotMeta(SheathSlotData slot) {
        return "Hotbar " + (slot.getLinkedHotbarSlot() + 1)
                + "  Priority " + slot.getPriority()
                + "  " + (slot.isVisible() ? "Visible" : "Hidden");
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
            case DUAL_CROSS_SLASH -> "Dual";
        };
    }
}
