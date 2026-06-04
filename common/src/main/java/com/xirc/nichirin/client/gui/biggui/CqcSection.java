package com.xirc.nichirin.client.gui.biggui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.xirc.nichirin.client.gui.MoveIcon;
import com.xirc.nichirin.common.data.CqcMoveCatalog;
import com.xirc.nichirin.common.data.CqcPresetData;
import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;

/**
 * CQC customization tab. All moves are unlocked by default; the server validates assignments.
 */
public class CqcSection extends AbstractGuiPage {

    private static final int TOP_MARGIN = 18;
    private static final int SLOT_W = 170;
    private static final int SLOT_H = 28;
    private static final int MOVE_W = 160;
    private static final int MOVE_H = 28;
    private static final int ICON_SIZE = 20;
    private static final int GAP = 8;

    private CqcPresetData.Slot selectedSlot = CqcPresetData.Slot.WHEEL;
    private int selectedWheelIndex = 0;
    private long lastClickTime;

    public void render(GuiGraphics graphics, Player player, int contentWidth, int contentHeight,
                       Font font, int mouseX, int mouseY) {
        int centerX = (contentWidth - 20) / 2;
        int y = TOP_MARGIN + 8;

        Component title = Component.literal("CQC").withStyle(s -> s.withBold(true));
        drawAccentTitle(graphics, font, title, centerX, y, COLOR_PALETTE.ACCENT.argb());
        y += 28;

        boolean equipped = "cqc".equals(MovesetHelper.getFightingMovesetId(player));
        GuiButton equipButton = new GuiButton(centerX - 80, y, 160, 22,
                equipped ? "Unequip CQC" : "Equip CQC",
                equipped ? COLOR_PALETTE.GREEN.argb() : COLOR_PALETTE.ACCENT.argb(),
                equipped ? COLOR_PALETTE.GREEN.rgb() : COLOR_PALETTE.ACCENT_LIGHT.rgb(),
                true);
        drawButton(graphics, font, equipButton, mouseX, mouseY);
        y += 34;

        CqcPresetData preset = PlayerDataProvider.getData(player).getCqcPresetData();
        int leftX = 24;
        int rightX = leftX + SLOT_W + 34;
        int sectionY = y;

        graphics.drawString(font, "Preset Slots", leftX, sectionY, COLOR_PALETTE.TEXT.rgb());
        sectionY += 16;

        renderSlot(graphics, font, leftX, sectionY, "Left Click", preset.getLeftClickMove(),
                CqcPresetData.Slot.LEFT_CLICK, -1, mouseX, mouseY);
        sectionY += SLOT_H + GAP;
        renderSlot(graphics, font, leftX, sectionY, "Right Click", preset.getRightClickMove(),
                CqcPresetData.Slot.RIGHT_CLICK, -1, mouseX, mouseY);
        sectionY += SLOT_H + GAP;
        renderSlot(graphics, font, leftX, sectionY, "Crouch Right", preset.getCrouchRightClickMove(),
                CqcPresetData.Slot.CROUCH_RIGHT_CLICK, -1, mouseX, mouseY);
        sectionY += SLOT_H + GAP + 8;

        for (int i = 0; i < CqcPresetData.WHEEL_SLOT_COUNT; i++) {
            renderSlot(graphics, font, leftX, sectionY, "Move " + (i + 1), preset.getWheelMove(i),
                    CqcPresetData.Slot.WHEEL, i, mouseX, mouseY);
            sectionY += SLOT_H + GAP;
        }

        graphics.drawString(font, "Move Catalog", rightX, y, COLOR_PALETTE.TEXT.rgb());
        int moveX = rightX;
        int moveY = y + 16;
        int col = 0;
        int maxCols = Math.max(1, (contentWidth - rightX - 24) / (MOVE_W + GAP));
        for (CqcMoveCatalog.Definition definition : CqcMoveCatalog.all()) {
            renderCatalogMove(graphics, font, moveX + col * (MOVE_W + GAP), moveY,
                    definition, mouseX, mouseY);
            col++;
            if (col >= maxCols) {
                col = 0;
                moveY += MOVE_H + GAP;
            }
        }
    }

    private void renderSlot(GuiGraphics graphics, Font font, int x, int y, String label, String moveId,
                            CqcPresetData.Slot slot, int wheelIndex, int mouseX, int mouseY) {
        boolean selected = selectedSlot == slot && (slot != CqcPresetData.Slot.WHEEL || selectedWheelIndex == wheelIndex);
        boolean hovered = mouseX >= x && mouseX <= x + SLOT_W && mouseY >= y && mouseY <= y + SLOT_H;
        int border = selected ? COLOR_PALETTE.ACCENT.argb() : hovered ? COLOR_PALETTE.BORDER_HI.argb() : COLOR_PALETTE.BORDER.argb();
        drawBorderedRect(graphics, x, y, SLOT_W, SLOT_H, border, hovered ? COLOR_PALETTE.PANEL_HOVER.argb() : COLOR_PALETTE.PANEL_MID.argb());
        renderMoveIcon(graphics, moveId, x + 4, y + 4, ICON_SIZE);
        graphics.drawString(font, label, x + 30, y + 4, COLOR_PALETTE.TEXT_DIM.rgb());
        CqcMoveCatalog.Definition definition = CqcMoveCatalog.get(moveId);
        String moveName = definition != null ? definition.displayName() : "None";
        graphics.drawString(font, fitText(font, moveName, SLOT_W - 38), x + 30, y + 15,
                selected ? COLOR_PALETTE.ACCENT_LIGHT.rgb() : COLOR_PALETTE.TEXT.rgb());
    }

    private void renderCatalogMove(GuiGraphics graphics, Font font, int x, int y,
                                   CqcMoveCatalog.Definition definition, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + MOVE_W && mouseY >= y && mouseY <= y + MOVE_H;
        drawBorderedRect(graphics, x, y, MOVE_W, MOVE_H,
                hovered ? COLOR_PALETTE.ACCENT.argb() : COLOR_PALETTE.BORDER.argb(),
                hovered ? COLOR_PALETTE.PANEL_HOVER.argb() : COLOR_PALETTE.PANEL_MID.argb());
        renderMoveIcon(graphics, definition.id(), x + 4, y + 4, ICON_SIZE);
        graphics.drawString(font, fitText(font, definition.displayName(), MOVE_W - 34), x + 30, y + 9,
                hovered ? COLOR_PALETTE.ACCENT_LIGHT.rgb() : COLOR_PALETTE.TEXT.rgb());
    }

    private void renderMoveIcon(GuiGraphics graphics, String moveId, int x, int y, int size) {
        ResourceLocation icon = MoveIcon.getIcon("cqc", moveId);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, icon);
        graphics.blit(icon, x, y, 0, 0, size, size, size, size);
        RenderSystem.disableBlend();
    }

    private String fitText(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String ellipsis = "...";
        int end = text.length();
        while (end > 0 && font.width(text.substring(0, end) + ellipsis) > maxWidth) {
            end--;
        }
        return end <= 0 ? ellipsis : text.substring(0, end) + ellipsis;
    }

    public boolean handleClick(double mouseX, double mouseY, Player player, int contentWidth) {
        long now = System.currentTimeMillis();
        if (now - lastClickTime < 150) return false;

        int centerX = (contentWidth - 20) / 2;
        int y = TOP_MARGIN + 8 + 28;
        if (mouseX >= centerX - 80 && mouseX <= centerX + 80 && mouseY >= y && mouseY <= y + 22) {
            NichirinPacketRegistry.requestMovesetChange("cqc");
            playClick();
            lastClickTime = now;
            return true;
        }

        int leftX = 24;
        int rightX = leftX + SLOT_W + 34;
        int sectionY = TOP_MARGIN + 8 + 28 + 34 + 16;

        if (selectSlot(mouseX, mouseY, leftX, sectionY, CqcPresetData.Slot.LEFT_CLICK, -1)) return clicked(now);
        sectionY += SLOT_H + GAP;
        if (selectSlot(mouseX, mouseY, leftX, sectionY, CqcPresetData.Slot.RIGHT_CLICK, -1)) return clicked(now);
        sectionY += SLOT_H + GAP;
        if (selectSlot(mouseX, mouseY, leftX, sectionY, CqcPresetData.Slot.CROUCH_RIGHT_CLICK, -1)) return clicked(now);
        sectionY += SLOT_H + GAP + 8;
        for (int i = 0; i < CqcPresetData.WHEEL_SLOT_COUNT; i++) {
            if (selectSlot(mouseX, mouseY, leftX, sectionY, CqcPresetData.Slot.WHEEL, i)) return clicked(now);
            sectionY += SLOT_H + GAP;
        }

        int moveY = TOP_MARGIN + 8 + 28 + 34 + 16;
        int col = 0;
        int maxCols = Math.max(1, (contentWidth - rightX - 24) / (MOVE_W + GAP));
        for (CqcMoveCatalog.Definition definition : CqcMoveCatalog.all()) {
            int x = rightX + col * (MOVE_W + GAP);
            if (mouseX >= x && mouseX <= x + MOVE_W && mouseY >= moveY && mouseY <= moveY + MOVE_H) {
                NichirinPacketRegistry.requestCqcPresetUpdate(selectedSlot, selectedWheelIndex, definition.id());
                playClick();
                lastClickTime = now;
                return true;
            }
            col++;
            if (col >= maxCols) {
                col = 0;
                moveY += MOVE_H + GAP;
            }
        }

        return false;
    }

    private boolean selectSlot(double mouseX, double mouseY, int x, int y, CqcPresetData.Slot slot, int wheelIndex) {
        if (mouseX < x || mouseX > x + SLOT_W || mouseY < y || mouseY > y + SLOT_H) return false;
        selectedSlot = slot;
        selectedWheelIndex = wheelIndex;
        playClick();
        return true;
    }

    private boolean clicked(long now) {
        lastClickTime = now;
        return true;
    }

    private static void playClick() {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f));
    }
}
