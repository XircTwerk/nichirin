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
    private static final int DEMON_BORDER = 0xFFFF3333;
    private static final int PRESET_W = 88;
    private static final int PRESET_H = 22;
    private static final int STANCE_W = 116;
    private static final int STANCE_H = 22;
    private static final String[] STANCE_NAMES = {
            "High Guard",
            "Orthodox",
            "Cross Guard",
            "Shoulder Roll"
    };

    private CqcPresetData.Slot selectedSlot = CqcPresetData.Slot.WHEEL;
    private int selectedWheelIndex = 0;
    private long lastClickTime;
    private String draggingMoveId;

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
        y += 32;

        CqcPresetData preset = PlayerDataProvider.getData(player).getCqcPresetData();
        graphics.drawString(font, "Preset", 24, y + 7, COLOR_PALETTE.TEXT.rgb());
        int presetX = 78;
        for (int i = 0; i < CqcPresetData.PRESET_COUNT; i++) {
            renderPresetButton(graphics, font, presetX + i * (PRESET_W + 6), y, i,
                    preset.getActivePresetIndex(), preset.getPresetName(i), mouseX, mouseY);
        }
        y += 32;

        graphics.drawString(font, "Blocking Stance", 24, y + 6, COLOR_PALETTE.TEXT.rgb());
        int stanceX = 140;
        for (int i = 0; i < STANCE_NAMES.length; i++) {
            renderStanceButton(graphics, font, stanceX + i * (STANCE_W + 6), y, i, preset.getStanceIndex(), mouseX, mouseY);
        }
        y += 36;

        CqcMoveCatalog.Definition hoveredDefinition = null;
        int leftX = 24;
        int rightX = leftX + SLOT_W + 34;
        int sectionY = y;

        graphics.drawString(font, "Preset Slots", leftX, sectionY, COLOR_PALETTE.TEXT.rgb());
        sectionY += 16;

        renderSlot(graphics, font, leftX, sectionY, "Left Click", preset.getLeftClickMove(),
                CqcPresetData.Slot.LEFT_CLICK, -1, mouseX, mouseY);
        hoveredDefinition = hoveredDefinition(hoveredDefinition, leftX, sectionY, SLOT_W, SLOT_H, preset.getLeftClickMove(), mouseX, mouseY);
        sectionY += SLOT_H + GAP;
        renderSlot(graphics, font, leftX, sectionY, "Right Click", preset.getRightClickMove(),
                CqcPresetData.Slot.RIGHT_CLICK, -1, mouseX, mouseY);
        hoveredDefinition = hoveredDefinition(hoveredDefinition, leftX, sectionY, SLOT_W, SLOT_H, preset.getRightClickMove(), mouseX, mouseY);
        sectionY += SLOT_H + GAP;
        renderSlot(graphics, font, leftX, sectionY, "Crouch Right", preset.getCrouchRightClickMove(),
                CqcPresetData.Slot.CROUCH_RIGHT_CLICK, -1, mouseX, mouseY);
        hoveredDefinition = hoveredDefinition(hoveredDefinition, leftX, sectionY, SLOT_W, SLOT_H, preset.getCrouchRightClickMove(), mouseX, mouseY);
        sectionY += SLOT_H + GAP + 8;

        for (int i = 0; i < CqcPresetData.WHEEL_SLOT_COUNT; i++) {
            renderSlot(graphics, font, leftX, sectionY, "Move " + (i + 1), preset.getWheelMove(i),
                    CqcPresetData.Slot.WHEEL, i, mouseX, mouseY);
            hoveredDefinition = hoveredDefinition(hoveredDefinition, leftX, sectionY, SLOT_W, SLOT_H, preset.getWheelMove(i), mouseX, mouseY);
            sectionY += SLOT_H + GAP;
        }
        String selectedMoveId = selectedMoveId(preset);
        String followupMoveId = preset.getFollowupMove(selectedMoveId);
        renderFollowupSlot(graphics, font, leftX, sectionY, selectedMoveId, followupMoveId, mouseX, mouseY);
        hoveredDefinition = hoveredDefinition(hoveredDefinition, leftX, sectionY, SLOT_W, SLOT_H, followupMoveId, mouseX, mouseY);

        graphics.drawString(font, "Move Catalog", rightX, y, COLOR_PALETTE.TEXT.rgb());
        int moveX = rightX;
        int moveY = y + 16;
        int col = 0;
        int maxCols = Math.max(1, (contentWidth - rightX - 24) / (MOVE_W + GAP));
        for (CqcMoveCatalog.Definition definition : CqcMoveCatalog.all()) {
            renderCatalogMove(graphics, font, moveX + col * (MOVE_W + GAP), moveY,
                    definition, mouseX, mouseY);
            int currentX = moveX + col * (MOVE_W + GAP);
            if (mouseX >= currentX && mouseX <= currentX + MOVE_W && mouseY >= moveY && mouseY <= moveY + MOVE_H) {
                hoveredDefinition = definition;
            }
            col++;
            if (col >= maxCols) {
                col = 0;
                moveY += MOVE_H + GAP;
            }
        }

        renderBottomTooltip(graphics, font, hoveredDefinition, contentWidth, contentHeight);
        if (draggingMoveId != null) {
            renderMoveIcon(graphics, draggingMoveId, mouseX - ICON_SIZE / 2, mouseY - ICON_SIZE / 2, ICON_SIZE);
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

    private void renderFollowupSlot(GuiGraphics graphics, Font font, int x, int y, String baseMoveId, String followupMoveId,
                                    int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + SLOT_W && mouseY >= y && mouseY <= y + SLOT_H;
        boolean canHaveFollowup = CqcPresetData.canHaveFollowup(baseMoveId);
        int border = hovered && canHaveFollowup ? COLOR_PALETTE.ACCENT.argb() : COLOR_PALETTE.BORDER.argb();
        drawBorderedRect(graphics, x, y, SLOT_W, SLOT_H, border, hovered ? COLOR_PALETTE.PANEL_HOVER.argb() : COLOR_PALETTE.PANEL_MID.argb());
        renderMoveIcon(graphics, followupMoveId, x + 4, y + 4, ICON_SIZE);
        graphics.drawString(font, "Followup", x + 30, y + 4, COLOR_PALETTE.TEXT_DIM.rgb());
        CqcMoveCatalog.Definition definition = CqcMoveCatalog.get(followupMoveId);
        String moveName = !canHaveFollowup ? "Unavailable" : definition != null ? definition.displayName() : "None";
        graphics.drawString(font, fitText(font, moveName, SLOT_W - 38), x + 30, y + 15,
                canHaveFollowup ? COLOR_PALETTE.TEXT.rgb() : COLOR_PALETTE.TEXT_DIM.rgb());
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

    private void renderPresetButton(GuiGraphics graphics, Font font, int x, int y, int presetIndex,
                                    int activePresetIndex, String presetName, int mouseX, int mouseY) {
        boolean selected = presetIndex == activePresetIndex;
        boolean hovered = mouseX >= x && mouseX <= x + PRESET_W && mouseY >= y && mouseY <= y + PRESET_H;
        int border = selected ? COLOR_PALETTE.ACCENT.argb() : hovered ? COLOR_PALETTE.BORDER_HI.argb() : COLOR_PALETTE.BORDER.argb();
        int bg = selected ? COLOR_PALETTE.PILL_BG.argb() : hovered ? COLOR_PALETTE.PANEL_HOVER.argb() : COLOR_PALETTE.PANEL_MID.argb();
        drawBorderedRect(graphics, x, y, PRESET_W, PRESET_H, border, bg);
        String label = fitText(font, presetName, PRESET_W - 12);
        graphics.drawString(font, label, x + (PRESET_W - font.width(label)) / 2, y + 7,
                selected ? COLOR_PALETTE.ACCENT_LIGHT.rgb() : COLOR_PALETTE.TEXT.rgb());
    }

    private void renderStanceButton(GuiGraphics graphics, Font font, int x, int y, int stanceIndex,
                                    int selectedStance, int mouseX, int mouseY) {
        boolean selected = stanceIndex == selectedStance;
        boolean hovered = mouseX >= x && mouseX <= x + STANCE_W && mouseY >= y && mouseY <= y + STANCE_H;
        int border = selected ? COLOR_PALETTE.ACCENT.argb() : hovered ? COLOR_PALETTE.BORDER_HI.argb() : COLOR_PALETTE.BORDER.argb();
        int bg = selected ? COLOR_PALETTE.PILL_BG.argb() : hovered ? COLOR_PALETTE.PANEL_HOVER.argb() : COLOR_PALETTE.PANEL_MID.argb();
        drawBorderedRect(graphics, x, y, STANCE_W, STANCE_H, border, bg);
        String label = fitText(font, STANCE_NAMES[stanceIndex], STANCE_W - 12);
        graphics.drawString(font, label, x + (STANCE_W - font.width(label)) / 2, y + 7,
                selected ? COLOR_PALETTE.ACCENT_LIGHT.rgb() : COLOR_PALETTE.TEXT.rgb());
    }

    private void renderMoveIcon(GuiGraphics graphics, String moveId, int x, int y, int size) {
        ResourceLocation icon = MoveIcon.getIcon("cqc", moveId);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, icon);
        graphics.blit(icon, x, y, 0, 0, size, size, size, size);
        RenderSystem.disableBlend();
        CqcMoveCatalog.Definition definition = CqcMoveCatalog.get(moveId);
        if (definition != null && definition.demonOnly()) {
            graphics.fill(x - 1, y - 1, x + size + 1, y, DEMON_BORDER);
            graphics.fill(x - 1, y + size, x + size + 1, y + size + 1, DEMON_BORDER);
            graphics.fill(x - 1, y - 1, x, y + size + 1, DEMON_BORDER);
            graphics.fill(x + size, y - 1, x + size + 1, y + size + 1, DEMON_BORDER);
        }
    }

    private CqcMoveCatalog.Definition hoveredDefinition(CqcMoveCatalog.Definition current, int x, int y, int w, int h,
                                                        String moveId, int mouseX, int mouseY) {
        if (current != null) return current;
        if (mouseX < x || mouseX > x + w || mouseY < y || mouseY > y + h) return null;
        return CqcMoveCatalog.get(moveId);
    }

    private void renderBottomTooltip(GuiGraphics graphics, Font font, CqcMoveCatalog.Definition definition,
                                     int contentWidth, int contentHeight) {
        if (definition == null) return;
        int x = 24;
        int y = Math.max(8, contentHeight - 54);
        int w = contentWidth - 48;
        int h = 42;
        drawBorderedRect(graphics, x, y, w, h, COLOR_PALETTE.BORDER_HI.argb(), COLOR_PALETTE.PANEL_MID.argb());
        renderMoveIcon(graphics, definition.id(), x + 8, y + 11, ICON_SIZE);
        graphics.drawString(font, definition.displayName(), x + 36, y + 8, COLOR_PALETTE.TEXT.rgb());
        graphics.drawString(font, fitText(font, definition.description(), w - 48), x + 36, y + 20, COLOR_PALETTE.TEXT_DIM.rgb());
        if (definition.demonOnly()) {
            graphics.drawString(font, "Can only be used by demons", x + 36, y + 31, DEMON_BORDER);
        }
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

        int presetRowY = y + 32;
        int presetX = 78;
        for (int i = 0; i < CqcPresetData.PRESET_COUNT; i++) {
            int x = presetX + i * (PRESET_W + 6);
            if (mouseX >= x && mouseX <= x + PRESET_W && mouseY >= presetRowY && mouseY <= presetRowY + PRESET_H) {
                NichirinPacketRegistry.requestCqcActivePresetUpdate(i);
                playClick();
                lastClickTime = now;
                return true;
            }
        }

        int stanceRowY = y + 64;
        int stanceX = 140;
        for (int i = 0; i < STANCE_NAMES.length; i++) {
            int x = stanceX + i * (STANCE_W + 6);
            if (mouseX >= x && mouseX <= x + STANCE_W && mouseY >= stanceRowY && mouseY <= stanceRowY + STANCE_H) {
                NichirinPacketRegistry.requestCqcStanceUpdate(i);
                playClick();
                lastClickTime = now;
                return true;
            }
        }

        int leftX = 24;
        int rightX = leftX + SLOT_W + 34;
        int sectionY = layoutY();

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

        int moveY = layoutY();
        int col = 0;
        int maxCols = Math.max(1, (contentWidth - rightX - 24) / (MOVE_W + GAP));
        for (CqcMoveCatalog.Definition definition : CqcMoveCatalog.all()) {
            int x = rightX + col * (MOVE_W + GAP);
            if (mouseX >= x && mouseX <= x + MOVE_W && mouseY >= moveY && mouseY <= moveY + MOVE_H) {
                draggingMoveId = definition.id();
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

    public boolean handleRelease(double mouseX, double mouseY, Player player, int contentWidth) {
        if (draggingMoveId == null) return false;
        TargetSlot target = slotAt(mouseX, mouseY);
        CqcPresetData preset = PlayerDataProvider.getData(player).getCqcPresetData();
        if (target != null && target.followup) {
            String baseMoveId = selectedMoveId(preset);
            if (!CqcPresetData.canHaveFollowup(baseMoveId)) {
                player.displayClientMessage(Component.literal("This move cannot have a followup.")
                        .withStyle(style -> style.withColor(DEMON_BORDER)), true);
                draggingMoveId = null;
                playClick();
                return true;
            }
            if (!CqcPresetData.canAssign(player, draggingMoveId)) {
                player.displayClientMessage(Component.literal("Can only be used by demons.")
                        .withStyle(style -> style.withColor(DEMON_BORDER)), true);
                draggingMoveId = null;
                playClick();
                return true;
            }
            NichirinPacketRegistry.requestCqcFollowupUpdate(baseMoveId, draggingMoveId);
            draggingMoveId = null;
            playClick();
            return true;
        }
        CqcPresetData.Slot slot = target != null ? target.slot : selectedSlot;
        int wheelIndex = target != null ? target.wheelIndex : selectedWheelIndex;
        if (!CqcPresetData.canAssign(player, draggingMoveId)) {
            player.displayClientMessage(Component.literal("Can only be used by demons.")
                    .withStyle(style -> style.withColor(DEMON_BORDER)), true);
            draggingMoveId = null;
            playClick();
            return true;
        }
        NichirinPacketRegistry.requestCqcPresetUpdate(slot, wheelIndex, draggingMoveId);
        if (target != null) {
            selectedSlot = target.slot;
            selectedWheelIndex = target.wheelIndex;
        }
        draggingMoveId = null;
        playClick();
        return true;
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

    private TargetSlot slotAt(double mouseX, double mouseY) {
        int leftX = 24;
        int sectionY = layoutY();
        if (inside(mouseX, mouseY, leftX, sectionY, SLOT_W, SLOT_H)) return new TargetSlot(CqcPresetData.Slot.LEFT_CLICK, -1, false);
        sectionY += SLOT_H + GAP;
        if (inside(mouseX, mouseY, leftX, sectionY, SLOT_W, SLOT_H)) return new TargetSlot(CqcPresetData.Slot.RIGHT_CLICK, -1, false);
        sectionY += SLOT_H + GAP;
        if (inside(mouseX, mouseY, leftX, sectionY, SLOT_W, SLOT_H)) return new TargetSlot(CqcPresetData.Slot.CROUCH_RIGHT_CLICK, -1, false);
        sectionY += SLOT_H + GAP + 8;
        for (int i = 0; i < CqcPresetData.WHEEL_SLOT_COUNT; i++) {
            if (inside(mouseX, mouseY, leftX, sectionY, SLOT_W, SLOT_H)) return new TargetSlot(CqcPresetData.Slot.WHEEL, i, false);
            sectionY += SLOT_H + GAP;
        }
        if (inside(mouseX, mouseY, leftX, sectionY, SLOT_W, SLOT_H)) return new TargetSlot(CqcPresetData.Slot.WHEEL, -1, true);
        return null;
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private int layoutY() {
        return TOP_MARGIN + 8 + 28 + 32 + 32 + 36 + 16;
    }

    private String selectedMoveId(CqcPresetData preset) {
        return switch (selectedSlot) {
            case LEFT_CLICK -> preset.getLeftClickMove();
            case RIGHT_CLICK -> preset.getRightClickMove();
            case CROUCH_RIGHT_CLICK -> preset.getCrouchRightClickMove();
            case WHEEL -> preset.getWheelMove(selectedWheelIndex);
        };
    }

    private record TargetSlot(CqcPresetData.Slot slot, int wheelIndex, boolean followup) {}

    private static void playClick() {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f));
    }
}
