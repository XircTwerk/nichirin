package com.xirc.nichirin.client.gui.biggui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.xirc.nichirin.client.gui.MoveIcon;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.attack.moveset.CqcMoveset;
import com.xirc.nichirin.common.attack.moveset.DefaultKatanaMoveset;
import com.xirc.nichirin.common.attack.moveset.DualKatanaMoveset;
import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.registry.NichirinKeybindRegistry;
import com.xirc.nichirin.registry.NichirinMovesetRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Data section for moveset - shows move icons in a grid with detailed tooltips on hover
 */
public class MovesetDataSection extends AbstractGuiPage {

    private static final int TOP_MARGIN = 20;
    private static final int ICON_SIZE = 32;
    private static final int ICON_SPACING = 8;
    private static final int ICONS_PER_ROW = 6;
    private static final int SLOT_STEP = ICON_SIZE + 18 + ICON_SPACING;
    private static final int SECTION_SPACING = 26;

    public void render(GuiGraphics graphics, Player player, int contentWidth, int contentHeight, Font font, int mouseX, int mouseY) {
        int contentX = 20;
        int contentY = TOP_MARGIN + 10;
        int centerX = (contentWidth - 20) / 2;

        // Title
        Component title = Component.translatable("gui.nichirin.moveset.data.title").withStyle(style -> style.withBold(true));
        drawAccentTitle(graphics, font, title, centerX, contentY, COLOR_PALETTE.BREATH_CYAN.argb());
        contentY += 30;

        List<SelectedMoveset> selectedMovesets = getSelectedMovesets(player);
        if (selectedMovesets.isEmpty()) {
            Component noMoveset = Component.translatable("gui.nichirin.moveset.data.no_moveset");
            drawPopPanel(graphics, contentX, contentY, Math.max(160, font.width(noMoveset) + 28), 32, COLOR_PALETTE.BREATH_CYAN.argb());
            graphics.drawString(font, noMoveset, contentX + 14, contentY + 12, COLOR_PALETTE.TEXT_DIM.rgb());
            return;
        }

        // Instructions
        Component instructions = Component.literal("Hover over move icons to see detailed information");
        graphics.drawString(font, instructions, contentX, contentY, COLOR_PALETTE.GRAY.rgb());
        contentY += 25;

        for (SelectedMoveset selected : selectedMovesets) {
            AbstractMoveset moveset = resolveMoveset(selected.movesetId());
            if (moveset == null) {
                Component invalidMoveset = Component.literal("Invalid " + selected.label() + ": " + selected.movesetId());
                drawPopPanel(graphics, contentX, contentY, Math.max(174, font.width(invalidMoveset) + 28), 32, COLOR_PALETTE.DANGER.argb());
                graphics.drawString(font, invalidMoveset, contentX + 14, contentY + 12, COLOR_PALETTE.DANGER.rgb());
                contentY += 42;
                continue;
            }

            AbstractMoveset finalMoveset = moveset;
            int sectionY = contentY;
            int[] blockBottom = new int[1];
            Runnable renderBlock = () -> blockBottom[0] = renderMovesetBlock(graphics, font, selected, finalMoveset,
                    contentX, sectionY, centerX, mouseX, mouseY, contentWidth);
            if (moveset instanceof CqcMoveset) {
                CqcMoveset.withPlayer(player, renderBlock);
            } else {
                renderBlock.run();
            }
            contentY = blockBottom[0];
            if (moveset instanceof CqcMoveset) {
                contentY = renderStanceInfo(graphics, font, player, contentX, contentY + 8);
            }
            contentY += SECTION_SPACING;
        }
    }

    /** Shows the active CQC blocking stance and what it actually does in combat. */
    private int renderStanceInfo(GuiGraphics graphics, Font font, Player player, int contentX, int contentY) {
        int stance = PlayerDataProvider.getData(player).getCqcPresetData().getStanceIndex();
        String name = switch (stance) {
            case 0 -> "High Guard";
            case 1 -> "Orthodox";
            case 2 -> "Cross Guard";
            case 3 -> "Shoulder Roll";
            default -> "Unknown";
        };
        String[] effects = switch (stance) {
            case 0 -> new String[] {
                    "The only stance that can parry attacks.",
                    "Slightly weaker damage resistance while blocking."};
            case 1 -> new String[] {
                    "+20% damage on left-click and right-click CQC moves.",
                    "No blocking perks — pure offense."};
            case 2 -> new String[] {
                    "Guard drains 33% slower while blocking.",
                    "Cannot parry."};
            case 3 -> new String[] {
                    "Guard drains 50% slower and blocks resist more damage.",
                    "Knocks attackers away when you block their hit. Cannot parry."};
            default -> new String[] {"No stance data."};
        };

        graphics.drawString(font, Component.literal("Blocking Stance: " + name)
                .withStyle(s -> s.withBold(true)), contentX, contentY, COLOR_PALETTE.ACCENT.rgb());
        contentY += 12;
        for (String line : effects) {
            graphics.drawString(font, line, contentX + 8, contentY, COLOR_PALETTE.TEXT_DIM.rgb());
            contentY += 10;
        }
        return contentY;
    }

    private int renderMovesetBlock(GuiGraphics graphics, Font font, SelectedMoveset selected, AbstractMoveset moveset,
                                   int contentX, int contentY, int centerX, int mouseX, int mouseY, int contentWidth) {
        Component styleLabel = Component.literal(selected.label() + ": ")
                .append(getMovesetDisplayName(selected.movesetId(), moveset));
        graphics.fill(contentX - 5, contentY - 2, contentX - 3, contentY + font.lineHeight + 2, selected.accentColor());
        graphics.drawString(font, styleLabel, contentX, contentY, selected.accentRgb());
        contentY += 18;

        int gridStartX = centerX - ((ICONS_PER_ROW * (ICON_SIZE + ICON_SPACING) - ICON_SPACING) / 2);
        int gridStartY = contentY;

        // Track which icon is being hovered for tooltip
        int hoveredMoveIndex = -1;
        boolean hoveredIsLeftClick = false;
        boolean hoveredIsRightClick = false;
        boolean hoveredIsCrouchRightClick = false;

        // Render move icons in grid
        int currentX = gridStartX;
        int currentY = gridStartY;
        int iconsInCurrentRow = 0;

        // Left click icon
        if (isIconHovered(mouseX, mouseY, currentX, currentY)) {
            hoveredIsLeftClick = true;
        }
        ResourceLocation leftClickIcon = MoveIcon.getIcon(selected.movesetId(), "left_click");
        renderMoveIcon(graphics, currentX, currentY, leftClickIcon, COLOR_PALETTE.ACCENT.argb());
        renderSlotCaption(graphics, font, currentX, currentY, "LMB", String.valueOf(moveset.getLeftClickMoveIndex()));

        currentX += ICON_SIZE + ICON_SPACING;
        iconsInCurrentRow++;

        // Right click icon
        if (isIconHovered(mouseX, mouseY, currentX, currentY)) {
            hoveredIsRightClick = true;
        }
        ResourceLocation rightClickIcon = MoveIcon.getIcon(selected.movesetId(), "right_click");
        renderMoveIcon(graphics, currentX, currentY, rightClickIcon, COLOR_PALETTE.SLAYER_BLUE.argb());
        renderSlotCaption(graphics, font, currentX, currentY, "RMB", String.valueOf(moveset.getRightClickMoveIndex(false)));

        currentX += ICON_SIZE + ICON_SPACING;
        iconsInCurrentRow++;

        // Crouch right click icon (second)
        if (isIconHovered(mouseX, mouseY, currentX, currentY)) {
            hoveredIsCrouchRightClick = true;
        }
        ResourceLocation crouchRightClickIcon = MoveIcon.getIcon(selected.movesetId(), "crouch_right_click");
        renderMoveIcon(graphics, currentX, currentY, crouchRightClickIcon, COLOR_PALETTE.GREEN.argb());
        renderSlotCaption(graphics, font, currentX, currentY, "C+RMB", String.valueOf(moveset.getRightClickMoveIndex(true)));

        currentX += ICON_SIZE + ICON_SPACING;
        iconsInCurrentRow++;

        // Moveset-specific moves
        for (int i = 0; i < moveset.getMoveCount(); i++) {
            AbstractMoveset.MoveConfiguration move = moveset.getMove(i);
            if (move == null) continue;

            // Check for new row
            if (iconsInCurrentRow >= ICONS_PER_ROW) {
                currentX = gridStartX;
                currentY += SLOT_STEP;
                iconsInCurrentRow = 0;
            }

            // Check if this icon is being hovered
            if (isIconHovered(mouseX, mouseY, currentX, currentY)) {
                hoveredMoveIndex = i;
            }

            ResourceLocation moveIcon = MoveIcon.getIcon(selected.movesetId(), move.getMoveId());
            renderMoveIcon(graphics, currentX, currentY, moveIcon, COLOR_PALETTE.BORDER_HI.argb());
            renderSlotCaption(graphics, font, currentX, currentY, getMoveHotkeyLabel(i), String.valueOf(i));

            currentX += ICON_SIZE + ICON_SPACING;
            iconsInCurrentRow++;
        }

        // Render tooltip if hovering over an icon
        if (hoveredIsLeftClick) {
            renderLeftClickTooltip(graphics, font, mouseX, mouseY, moveset, contentWidth);
        } else if (hoveredIsRightClick) {
            renderRightClickTooltip(graphics, font, mouseX, mouseY, moveset, contentWidth);
        } else if (hoveredIsCrouchRightClick) {
            renderCrouchRightClickTooltip(graphics, font, mouseX, mouseY, moveset, contentWidth);
        } else if (hoveredMoveIndex >= 0) {
            AbstractMoveset.MoveConfiguration move = moveset.getMove(hoveredMoveIndex);
            if (move != null) {
                renderMoveTooltip(graphics, font, mouseX, mouseY, move, hoveredMoveIndex, contentWidth);
            }
        }

        int rows = Math.max(1, (int) Math.ceil((moveset.getMoveCount() + 3) / (double) ICONS_PER_ROW));
        return gridStartY + rows * SLOT_STEP - ICON_SPACING;
    }

    private List<SelectedMoveset> getSelectedMovesets(Player player) {
        List<SelectedMoveset> selected = new ArrayList<>();
        String breathingId = MovesetHelper.getBreathingMovesetId(player);
        if (DualKatanaMoveset.isDualWielding(player)) {
            selected.add(new SelectedMoveset("Dual Katana", DualKatanaMoveset.INSTANCE.getMovesetId(),
                    COLOR_PALETTE.SLAYER_BLUE.argb(), COLOR_PALETTE.SLAYER_BLUE.rgb()));
        } else if (breathingId != null) {
            selected.add(new SelectedMoveset("Breathing Style", breathingId,
                    COLOR_PALETTE.BREATH_CYAN.argb(), COLOR_PALETTE.BREATH_CYAN.rgb()));
        } else {
            selected.add(new SelectedMoveset("Default Katana", DefaultKatanaMoveset.INSTANCE.getMovesetId(),
                    COLOR_PALETTE.SLAYER_BLUE.argb(), COLOR_PALETTE.SLAYER_BLUE.rgb()));
        }

        String fightingId = MovesetHelper.getFightingMovesetId(player);
        if (fightingId != null) {
            selected.add(new SelectedMoveset("Fighting Style", fightingId,
                    COLOR_PALETTE.ACCENT.argb(), COLOR_PALETTE.ACCENT.rgb()));
        }

        String demonId = MovesetHelper.getDemonMovesetId(player);
        if (demonId != null) {
            selected.add(new SelectedMoveset("Demon Art", demonId,
                    COLOR_PALETTE.DANGER.argb(), COLOR_PALETTE.DANGER.rgb()));
        }
        return selected;
    }

    private AbstractMoveset resolveMoveset(String movesetId) {
        if (DefaultKatanaMoveset.INSTANCE.getMovesetId().equals(movesetId)) {
            return DefaultKatanaMoveset.INSTANCE;
        }
        if (DualKatanaMoveset.INSTANCE.getMovesetId().equals(movesetId)) {
            return DualKatanaMoveset.INSTANCE;
        }
        return NichirinMovesetRegistry.getMoveset(movesetId);
    }

    private Component getMovesetDisplayName(String movesetId, AbstractMoveset moveset) {
        if (DefaultKatanaMoveset.INSTANCE.getMovesetId().equals(movesetId)
                || DualKatanaMoveset.INSTANCE.getMovesetId().equals(movesetId)) {
            return Component.literal(moveset.getDisplayName());
        }
        return Component.translatable(getTranslationKey(movesetId));
    }

    /**
     * Check if mouse is hovering over an icon
     */
    private boolean isIconHovered(int mouseX, int mouseY, int iconX, int iconY) {
        return mouseX >= iconX && mouseX <= iconX + ICON_SIZE &&
                mouseY >= iconY && mouseY <= iconY + ICON_SIZE;
    }

    /**
     * Render a move icon using your MoveIcon system
     */
    private void renderMoveIcon(GuiGraphics graphics, int x, int y, ResourceLocation iconTexture, int borderColor) {
        graphics.fill(x - 2, y - 2, x + ICON_SIZE + 2, y + ICON_SIZE + 2, withAlpha(borderColor, 0x26));
        // Draw border
        graphics.fill(x - 1, y - 1, x + ICON_SIZE + 1, y + ICON_SIZE + 1, borderColor);
        graphics.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, COLOR_PALETTE.PANEL_MID.argb());
        graphics.fill(x, y, x + ICON_SIZE, y + 2, withAlpha(borderColor, 0xCC));

        // Draw the actual icon texture — enable blend before blit for transparency support
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(iconTexture, x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        RenderSystem.disableBlend();
    }

    private void renderSlotCaption(GuiGraphics graphics, Font font, int x, int y, String primary, String secondary) {
        graphics.drawCenteredString(font, trimToWidth(font, primary, ICON_SIZE + 12),
                x + ICON_SIZE / 2, y + ICON_SIZE + 3, COLOR_PALETTE.TEXT.rgb());
        graphics.drawCenteredString(font, trimToWidth(font, secondary, ICON_SIZE + 18),
                x + ICON_SIZE / 2, y + ICON_SIZE + 12, COLOR_PALETTE.TEXT_DIM.rgb());
    }

    /**
     * Render tooltip for left click.
     */
    private void renderLeftClickTooltip(GuiGraphics graphics, Font font, int mouseX, int mouseY, AbstractMoveset moveset, int contentWidth) {
        List<String> tooltipLines = new ArrayList<>();

        String moveName = moveset.getLeftClickMoveName();
        tooltipLines.add("Left Click - " + moveName);
        tooltipLines.add("Input: Left Click");
        tooltipLines.add("Index: " + moveset.getLeftClickMoveIndex());
        tooltipLines.add("");

        String description = moveset.getLeftClickDescription();
        if (description != null && !description.isEmpty()) {
            tooltipLines.add(description);
            tooltipLines.add("");
        }

        AbstractMoveset.MoveConfiguration leftClickConfig = moveset.getLeftClickConfiguration();
        if (leftClickConfig != null) {
            addConfigTooltipLines(tooltipLines, leftClickConfig);
        } else {
            tooltipLines.add("Basic moveset ability");
            tooltipLines.add("Stats vary by moveset");
        }

        renderTooltip(graphics, font, mouseX, mouseY, tooltipLines.toArray(new String[0]), contentWidth);
    }

    /**
     * Render tooltip for right click - NOW PROPERLY DISPLAYS STATS
     */
    private void renderRightClickTooltip(GuiGraphics graphics, Font font, int mouseX, int mouseY, AbstractMoveset moveset, int contentWidth) {
        List<String> tooltipLines = new ArrayList<>();

        String moveName = moveset.getRightClickMoveName();
        tooltipLines.add("Right Click - " + moveName);
        tooltipLines.add("Input: Right Click");
        tooltipLines.add("Index: " + moveset.getRightClickMoveIndex(false));
        tooltipLines.add("");

        // Add description if available
        String description = moveset.getRightClickDescription();
        if (description != null && !description.isEmpty()) {
            tooltipLines.add(description);
            tooltipLines.add("");
        }

        // Get right click stats if the moveset exposes them
        AbstractMoveset.MoveConfiguration rightClickConfig = moveset.getRightClickConfiguration();
        if (rightClickConfig != null) {
            // Actually add the config data to tooltip lines
            addConfigTooltipLines(tooltipLines, rightClickConfig);
        } else {
            tooltipLines.add("Special moveset ability");
            tooltipLines.add("Stats vary by breathing style");
            tooltipLines.add("");
            tooltipLines.add("Use this move to capture configuration data");
        }

        renderTooltip(graphics, font, mouseX, mouseY, tooltipLines.toArray(new String[0]), contentWidth);
    }

    /**
     * Render tooltip for crouch right click - NOW PROPERLY DISPLAYS STATS
     */
    private void renderCrouchRightClickTooltip(GuiGraphics graphics, Font font, int mouseX, int mouseY, AbstractMoveset moveset, int contentWidth) {
        List<String> tooltipLines = new ArrayList<>();

        String moveName = moveset.getCrouchRightClickMoveName();
        tooltipLines.add("Crouch + Right Click - " + moveName);
        tooltipLines.add("Input: Crouch + Right Click");
        tooltipLines.add("Index: " + moveset.getRightClickMoveIndex(true));
        tooltipLines.add("");

        // Add description if available
        String description = moveset.getCrouchRightClickDescription();
        if (description != null && !description.isEmpty()) {
            tooltipLines.add(description);
            tooltipLines.add("");
        }

        // Get crouch right click stats if the moveset exposes them
        AbstractMoveset.MoveConfiguration crouchRightClickConfig = moveset.getCrouchRightClickConfiguration();
        if (crouchRightClickConfig != null) {
            // Actually add the config data to tooltip lines
            addConfigTooltipLines(tooltipLines, crouchRightClickConfig);
        } else {
            tooltipLines.add("Special crouch ability");
            tooltipLines.add("Stats vary by breathing style");
            tooltipLines.add("");
            tooltipLines.add("Use this move to capture configuration data");
        }

        renderTooltip(graphics, font, mouseX, mouseY, tooltipLines.toArray(new String[0]), contentWidth);
    }

    /**
     * Render detailed tooltip for a specific move - ONLY CONFIGURED VALUES
     */
    private void renderMoveTooltip(GuiGraphics graphics, Font font, int mouseX, int mouseY,
                                   AbstractMoveset.MoveConfiguration move, int moveIndex, int contentWidth) {
        List<String> tooltipLines = new ArrayList<>();

        // Move name and number
        tooltipLines.add("Move " + (moveIndex + 1) + ": " + move.getDisplayName());
        tooltipLines.add("Input: Attack Wheel Slot " + (moveIndex + 1) + " / Move Hotkey " + (moveIndex + 1));
        tooltipLines.add("Index: " + moveIndex);
        tooltipLines.add("Keybind: " + getMoveHotkeyFullLabel(moveIndex));
        tooltipLines.add("");

        // Add description if available
        String description = move.getDescription();
        if (description != null && !description.isEmpty()) {
            tooltipLines.add(description);
            tooltipLines.add("");
        }

        addConfigTooltipLines(tooltipLines, move);
        renderTooltip(graphics, font, mouseX, mouseY, tooltipLines.toArray(new String[0]), contentWidth);
    }

    /**
     * Add configuration data to tooltip lines - only configured values
     */
    private void addConfigTooltipLines(List<String> tooltipLines, AbstractMoveset.MoveConfiguration config) {
        boolean hasStats = false;

        // Combat stats - only show if configured
        if (config.hasDamage()) {
            tooltipLines.add("Damage: " + String.format("%.1f", config.getDamage()));
            hasStats = true;
        }
        if (config.hasRange()) {
            tooltipLines.add("Range: " + String.format("%.1f blocks", config.getRange()));
            hasStats = true;
        }
        if (config.hasKnockback()) {
            tooltipLines.add("Knockback: " + String.format("%.1f", config.getKnockback()));
            hasStats = true;
        }

        // Timing - only show if configured
        if (config.hasCooldown()) {
            tooltipLines.add("Cooldown: " + (config.getCooldown() / 20) + "s");
            hasStats = true;
        }
        if (config.hasWindup()) {
            tooltipLines.add("Windup: " + String.format("%.1fs", config.getWindup() / 20.0f));
            hasStats = true;
        }
        if (config.hasDuration()) {
            tooltipLines.add("Duration: " + String.format("%.1fs", config.getDuration() / 20.0f));
            hasStats = true;
        }

        // Resource costs - only show if configured
        if (config.hasBreathCost()) {
            tooltipLines.add("Breath Cost: " + String.format("%.0f", config.getBreathCost()));
            hasStats = true;
        }
        if (config.hasStaminaCost()) {
            tooltipLines.add("Stamina Cost: " + String.format("%.0f", config.getStaminaCost()));
            hasStats = true;
        }

        // Additional effects - only show if configured
        if (config.hasHitStun()) {
            tooltipLines.add("Hit Stun: " + String.format("%.1fs", config.getHitStun() / 20.0f));
            hasStats = true;
        }
        if (config.hasHitboxSize()) {
            tooltipLines.add("Hitbox Size: " + String.format("%.1f", config.getHitboxSize()));
            hasStats = true;
        }
        if (config.hasTeleportDistance()) {
            tooltipLines.add("Teleport Distance: " + String.format("%.1f blocks", config.getTeleportDistance()));
            hasStats = true;
        }
        if (config.hasDashSpeed()) {
            tooltipLines.add("Dash Speed: " + String.format("%.1f blocks", config.getDashSpeed()));
            hasStats = true;
        }

        // Add separator if we added stats
        if (hasStats) {
            tooltipLines.add(""); // Empty line for spacing
        }
    }

    /**
     * Render a tooltip box with multiple lines
     */
    private void renderTooltip(GuiGraphics graphics, Font font, int mouseX, int mouseY, String[] lines, int contentWidth) {
        if (lines.length == 0) return;

        // Calculate tooltip size
        int maxWidth = 0;
        for (String line : lines) {
            maxWidth = Math.max(maxWidth, font.width(line));
        }

        int tooltipWidth = maxWidth + 8;
        int tooltipHeight = lines.length * font.lineHeight + 6;

        // Position tooltip (avoid going off screen)
        int tooltipX = mouseX + 10;
        int tooltipY = mouseY - 10;

        if (tooltipX + tooltipWidth > contentWidth - 20) {
            tooltipX = mouseX - tooltipWidth - 10;
        }
        tooltipX = Math.max(5, tooltipX); // Prevent going off the left edge
        if (tooltipY < 10) {
            tooltipY = mouseY + 20;
        }

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);
        try {
            // Draw tooltip background
            graphics.fill(tooltipX - 1, tooltipY - 1, tooltipX + tooltipWidth + 1, tooltipY + tooltipHeight + 1, COLOR_PALETTE.BLACK.argb());
            graphics.fill(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight, COLOR_PALETTE.TOOLTIP_FILL.argb());

            // Draw tooltip text
            int textY = tooltipY + 3;
            for (String line : lines) {
                if (!line.isEmpty()) {
                    graphics.drawString(font, line, tooltipX + 4, textY, COLOR_PALETTE.WHITE.rgb());
                }
                textY += font.lineHeight;
            }
        } finally {
            graphics.pose().popPose();
        }
    }

    /**
     * Get appropriate translation key for moveset
     */
    private String getTranslationKey(String movesetId) {
        if (movesetId.equals("cqc")) {
            return "fighting_style." + movesetId;
        } else if (movesetId.equals("default_demon") || movesetId.contains("demon")) {
            return "demon_art." + movesetId;
        } else {
            return "breathing_style." + movesetId;
        }
    }

    private String getMoveHotkeyLabel(int moveIndex) {
        KeyMapping keyMapping = NichirinKeybindRegistry.getMoveHotkey(moveIndex);
        if (keyMapping == null) return "Unbound";
        String key = keyMapping.getTranslatedKeyMessage().getString();
        if (key == null || key.isBlank()) return "Unbound";
        if ("Not Bound".equalsIgnoreCase(key) || "Unknown".equalsIgnoreCase(key)) return "Unbound";
        return key;
    }

    private String getMoveHotkeyFullLabel(int moveIndex) {
        return "Move " + (moveIndex + 1) + " (" + getMoveHotkeyLabel(moveIndex) + ")";
    }

    private String trimToWidth(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String ellipsis = "...";
        int end = text.length();
        while (end > 0 && font.width(text.substring(0, end) + ellipsis) > maxWidth) {
            end--;
        }
        return end <= 0 ? ellipsis : text.substring(0, end) + ellipsis;
    }

    public boolean handleClick(double mouseX, double mouseY, Player player, int contentWidth) {
        // No click handling needed for data section
        return false;
    }

    private record SelectedMoveset(String label, String movesetId, int accentColor, int accentRgb) {}
}
