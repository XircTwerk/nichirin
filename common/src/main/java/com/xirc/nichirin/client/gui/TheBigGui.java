package com.xirc.nichirin.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.xirc.nichirin.common.data.BreathingStyleHelper;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Quaternionf;
import com.mojang.blaze3d.platform.Lighting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * THE BIG GUI - Main menu system for all mod features
 * Full-screen interface with vanilla+ styling
 */
public class TheBigGui extends Screen {

    // UI Constants
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 4;
    private static final int RIGHT_MARGIN = 10;
    private static final int TOP_MARGIN = 40;

    // Colors
    private static final int BACKGROUND_COLOR = 0xC0101010; // Dark gray with transparency
    private static final int ACTIVE_BUTTON_COLOR = 0xFF3F3F3F;

    // Current section
    private GuiSection currentSection = GuiSection.HOME;

    // Player reference
    private final Player player;

    // Section buttons
    private final List<SectionButton> sectionButtons = new ArrayList<>();

    // Content renderers
    private Consumer<GuiGraphics> currentContentRenderer;

    public TheBigGui(Player player) {
        super(Component.literal("Nichirin Menu"));
        this.player = player;
    }

    @Override
    protected void init() {
        super.init();

        // Clear previous buttons
        sectionButtons.clear();

        // Calculate button positions
        int buttonX = this.width - BUTTON_WIDTH - RIGHT_MARGIN;
        int buttonY = TOP_MARGIN;

        // Create section buttons
        for (GuiSection section : GuiSection.values()) {
            SectionButton button = new SectionButton(
                    buttonX,
                    buttonY,
                    BUTTON_WIDTH,
                    BUTTON_HEIGHT,
                    Component.literal(section.getDisplayName()),
                    section,
                    btn -> switchToSection(section)
            );

            this.addRenderableWidget(button);
            sectionButtons.add(button);

            buttonY += BUTTON_HEIGHT + BUTTON_SPACING;
        }

        // Initialize with home section
        switchToSection(GuiSection.HOME);
    }

    /**
     * Switches to a different section
     */
    private void switchToSection(GuiSection section) {
        this.currentSection = section;

        // Update button states
        for (SectionButton button : sectionButtons) {
            button.active = (button.getSection() != section);
        }

        // Set the appropriate content renderer
        this.currentContentRenderer = switch (section) {
            case HOME -> this::renderHomeContent;
            case BREATHING_STYLES -> this::renderBreathingStylesContent;
            case SKILLS -> this::renderSkillsContent;
            case BESTIARY -> this::renderBestiaryContent;
            case PERKS -> this::renderPerksContent;
            case QUESTS -> this::renderQuestsContent;
            case REPUTATION -> this::renderReputationContent;
            case COSMETICS -> this::renderCosmeticsContent;
            case MOVESET -> this::renderMovesetContent;
            case CONFIG -> this::renderConfigContent;
        };
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Draw dark background
        graphics.fill(0, 0, this.width, this.height, BACKGROUND_COLOR);

        // Draw section title
        Component title = Component.literal(currentSection.getDisplayName());
        int titleX = (this.width - this.font.width(title)) / 2;
        graphics.drawString(this.font, title, titleX, 10, 0xFFFFFF);

        // Draw content area background (slightly lighter)
        int contentRight = this.width - BUTTON_WIDTH - RIGHT_MARGIN - 10;
        graphics.fill(10, TOP_MARGIN, contentRight, this.height - 10, 0xB0202020);

        // Render current section content
        if (currentContentRenderer != null) {
            currentContentRenderer.accept(graphics);
        }

        // Render buttons and other widgets
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Don't pause the game
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Close on ESC or the keybind key
        if (keyCode == 256) { // ESC key
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // Content rendering methods

    private void renderHomeContent(GuiGraphics graphics) {
        int contentX = 20;
        int contentY = TOP_MARGIN + 10;
        int centerX = (this.width - BUTTON_WIDTH - RIGHT_MARGIN - 20) / 2;

        // === 3D Player Model ===
        // Calculate position for player model (upper center)
        int modelX = centerX;
        int modelY = contentY + 60;
        int modelSize = 60;

        // Draw background for model area
        graphics.fill(modelX - modelSize - 5, modelY - modelSize - 5,
                modelX + modelSize + 5, modelY + modelSize + 5, 0xFF2A2A2A);
        graphics.fill(modelX - modelSize - 3, modelY - modelSize - 3,
                modelX + modelSize + 3, modelY + modelSize + 3, 0xFF1A1A1A);

        // Render 3D player model
        renderPlayerModel(graphics, modelX, modelY, modelSize, player);

        // === Player Stats Section ===
        int statsY = modelY + modelSize + 20;
        int statLineHeight = 16;

        // Title
        Component statsTitle = Component.literal("PLAYER STATS").withStyle(style -> style.withBold(true));
        graphics.drawString(this.font, statsTitle,
                centerX - this.font.width(statsTitle) / 2, statsY, 0xFFFFFF);
        statsY += 25;

        // Player name with decorative line
        drawStatLine(graphics, contentX, statsY, "Name", player.getName().getString(), 0xFFD700);
        statsY += statLineHeight;

        // Slayer rank
        String slayerRank = "Coming Soon"; // Not implemented yet
        drawStatLine(graphics, contentX, statsY, "Slayer Rank", slayerRank, 0x5555FF);
        statsY += statLineHeight;

        // Breathing style
        String breathingStyle = BreathingStyleHelper.getMovesetId(player);
        if (breathingStyle == null) breathingStyle = "None";
        drawStatLine(graphics, contentX, statsY, "Breathing Style", formatBreathingStyle(breathingStyle), 0x55FFFF);
        statsY += statLineHeight;

        // Combat stats separator
        statsY += 10;
        graphics.drawString(this.font, "─────────────────────", contentX, statsY, 0x555555);
        statsY += 15;

        // Demon kill count
        String demonKills = "Coming Soon"; // Not implemented yet
        drawStatLine(graphics, contentX, statsY, "Demons Slain", demonKills, 0xFF5555);
        statsY += statLineHeight;

        // Best combo
        String bestCombo = "Coming Soon"; // Not implemented yet
        drawStatLine(graphics, contentX, statsY, "Best Combo", bestCombo, 0xFFAA00);
        statsY += statLineHeight;

        // Total playtime
        long playtime = player.level().getGameTime() / 20; // Convert ticks to seconds
        String playtimeStr = formatPlaytime(playtime);
        drawStatLine(graphics, contentX, statsY, "Playtime", playtimeStr, 0xAAAAAA);
        statsY += statLineHeight;

        // Level/Experience
        int level = player.experienceLevel;
        drawStatLine(graphics, contentX, statsY, "Level", String.valueOf(level), 0x55FF55);

        // Instructions at bottom
        int bottomY = this.height - 30;
        Component instructions = Component.literal("Press ESC to close • Click sections on the right to navigate")
                .withStyle(style -> style.withColor(0x777777).withItalic(true));
        graphics.drawString(this.font, instructions,
                (this.width - BUTTON_WIDTH - RIGHT_MARGIN - this.font.width(instructions)) / 2,
                bottomY, 0x777777);
    }

    /**
     * Renders a stat line with label and value
     */
    private void drawStatLine(GuiGraphics graphics, int x, int y, String label, String value, int valueColor) {
        // Draw label
        graphics.drawString(this.font, label + ":", x, y, 0xAAAAAA);

        // Draw value (right-aligned within content area)
        int valueX = this.width - BUTTON_WIDTH - RIGHT_MARGIN - 30 - this.font.width(value);
        graphics.drawString(this.font, value, valueX, y, valueColor);

        // Draw connecting dots
        String dots = ".".repeat(Math.max(1, (valueX - x - this.font.width(label + ": ") - 5) / 4));
        graphics.drawString(this.font, dots, x + this.font.width(label + ": "), y, 0x444444);
    }

    /**
     * Formats breathing style name
     */
    private String formatBreathingStyle(String style) {
        if (style.equals("None")) return style;
        // Convert snake_case to Title Case
        String[] parts = style.split("_");
        StringBuilder formatted = new StringBuilder();
        for (String part : parts) {
            if (formatted.length() > 0) formatted.append(" ");
            formatted.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
        }
        return formatted.toString();
    }

    /**
     * Formats playtime into readable format
     */
    private String formatPlaytime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }

    /**
     * Renders the 3D player model
     */
    private void renderPlayerModel(GuiGraphics graphics, int x, int y, int size, Player player) {
        // Calculate rotation based on time
        float rotation = (System.currentTimeMillis() / 50L % 360L) * 0.017453292F; // Convert to radians

        // Render player using the inventory render method
        graphics.pose().pushPose();
        graphics.pose().translate(x, y + 55, 50.0F); // Added +55 to move player down
        graphics.pose().scale((float)size, (float)size, (float)size);

        // Apply rotation
        Quaternionf quaternion = Axis.ZP.rotationDegrees(180.0F);
        Quaternionf quaternion2 = Axis.XP.rotationDegrees(-20.0F);
        Quaternionf quaternion3 = Axis.YP.rotation(rotation); // Rotation animation

        quaternion.mul(quaternion2);
        quaternion.mul(quaternion3);

        graphics.pose().mulPose(quaternion);

        // Set up lighting
        Lighting.setupForEntityInInventory();

        // Prepare entity for rendering
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        entityrenderdispatcher.setRenderShadow(false);

        // Render the player
        MultiBufferSource.BufferSource bufferSource = graphics.bufferSource();
        RenderSystem.runAsFancy(() -> {
            entityrenderdispatcher.render(player, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F,
                    graphics.pose(), bufferSource, 15728880);
        });

        bufferSource.endBatch();
        entityrenderdispatcher.setRenderShadow(true);

        graphics.pose().popPose();
        Lighting.setupFor3DItems();
    }

    private void renderBreathingStylesContent(GuiGraphics graphics) {
        int contentX = 20;
        int contentY = TOP_MARGIN + 10;
        int centerX = (this.width - BUTTON_WIDTH - RIGHT_MARGIN - 20) / 2;

        // Title
        Component title = Component.literal("BREATHING STYLES").withStyle(style -> style.withBold(true));
        graphics.drawString(this.font, title,
                centerX - this.font.width(title) / 2, contentY, 0xFFFFFF);
        contentY += 30;

        // Current style
        String currentStyle = BreathingStyleHelper.getMovesetId(player);
        if (currentStyle != null) {
            Component current = Component.literal("Current: " + formatBreathingStyle(currentStyle))
                    .withStyle(style -> style.withColor(0x55FFFF));
            graphics.drawString(this.font, current, contentX, contentY, 0x55FFFF);
            contentY += 25;
        }

        // Instructions
        graphics.drawString(this.font, "Click to select a breathing style:", contentX, contentY, 0xAAAAAA);
        contentY += 20;

        // Style grid - Only Thunder Breathing for now
        int gridX = contentX + 10;
        int gridY = contentY + 10;
        int boxWidth = 150;
        int boxHeight = 80;

        // Only Thunder Breathing
        String styleName = "thunder_breathing";
        boolean isUnlocked = true;
        boolean isSelected = styleName.equals(currentStyle);

        // Center the single box
        int x = centerX - boxWidth / 2;
        int y = gridY;

        // Draw box
        int bgColor = isSelected ? 0xFF3A3A3A : 0xFF2A2A2A;
        int borderColor = isSelected ? 0xFF55FFFF : 0xFF4A4A4A;

        // Border
        graphics.fill(x - 1, y - 1, x + boxWidth + 1, y + boxHeight + 1, borderColor);
        // Background
        graphics.fill(x, y, x + boxWidth, y + boxHeight, bgColor);

        // Style name
        String displayName = "Thunder Breathing";
        graphics.drawString(this.font, displayName,
                x + (boxWidth - this.font.width(displayName)) / 2,
                y + 10, 0xFFFFFF);

        // Status
        if (isSelected) {
            Component equipped = Component.literal("EQUIPPED").withStyle(style -> style.withColor(0x55FFFF));
            graphics.drawString(this.font, equipped,
                    x + (boxWidth - this.font.width(equipped)) / 2,
                    y + 30, 0x55FFFF);
        } else {
            Component clickToSelect = Component.literal("Click to Select").withStyle(style -> style.withColor(0xAAAAAA));
            graphics.drawString(this.font, clickToSelect,
                    x + (boxWidth - this.font.width(clickToSelect)) / 2,
                    y + 30, 0xAAAAAA);
        }

        // Icon placeholder (thunder icon)
        graphics.fill(x + boxWidth/2 - 16, y + 45, x + boxWidth/2 + 16, y + 77, 0xFF3A3A3A);

        // Coming soon text for other styles
        Component comingSoon = Component.literal("More breathing styles coming soon!")
                .withStyle(style -> style.withColor(0x777777).withItalic(true));
        graphics.drawString(this.font, comingSoon,
                centerX - this.font.width(comingSoon) / 2,
                y + boxHeight + 20, 0x777777);
    }

    private void renderSkillsContent(GuiGraphics graphics) {
        graphics.drawString(this.font,
                Component.literal("Skills - Coming Soon"),
                20, TOP_MARGIN + 10, 0xFFFFFF);
    }

    private void renderBestiaryContent(GuiGraphics graphics) {
        graphics.drawString(this.font,
                Component.literal("Bestiary - Coming Soon"),
                20, TOP_MARGIN + 10, 0xFFFFFF);
    }

    private void renderPerksContent(GuiGraphics graphics) {
        graphics.drawString(this.font,
                Component.literal("Perks - Coming Soon"),
                20, TOP_MARGIN + 10, 0xFFFFFF);
    }

    private void renderQuestsContent(GuiGraphics graphics) {
        graphics.drawString(this.font,
                Component.literal("Quests - Coming Soon"),
                20, TOP_MARGIN + 10, 0xFFFFFF);
    }

    private void renderReputationContent(GuiGraphics graphics) {
        int contentX = 20;
        int contentY = TOP_MARGIN + 10;

        graphics.drawString(this.font,
                Component.literal("Reputation System"),
                contentX, contentY, 0xFFFFFF);
        contentY += 20;

        // Slayer reputation
        graphics.drawString(this.font,
                Component.literal("Slayer Reputation: 0"),
                contentX, contentY, 0x5555FF);
        contentY += 15;

        // Demon reputation
        graphics.drawString(this.font,
                Component.literal("Demon Reputation: 0"),
                contentX, contentY, 0xFF5555);
    }

    private void renderCosmeticsContent(GuiGraphics graphics) {
        graphics.drawString(this.font,
                Component.literal("Cosmetics - Coming Soon"),
                20, TOP_MARGIN + 10, 0xFFFFFF);
    }

    private void renderMovesetContent(GuiGraphics graphics) {
        int contentX = 20;
        int contentY = TOP_MARGIN + 10;

        graphics.drawString(this.font,
                Component.literal("Current Moveset"),
                contentX, contentY, 0xFFFFFF);
        contentY += 20;

        // TODO: Display current moveset with frame data
        graphics.drawString(this.font,
                Component.literal("Select a breathing style to view moves"),
                contentX, contentY, 0xAAAAAA);
    }

    private void renderConfigContent(GuiGraphics graphics) {
        graphics.drawString(this.font,
                Component.literal("Configuration - Coming Soon"),
                20, TOP_MARGIN + 10, 0xFFFFFF);
    }

    /**
     * Custom button for sections
     */
    @Getter
    private static class SectionButton extends Button {
        private final GuiSection section;

        public SectionButton(int x, int y, int width, int height, Component text, GuiSection section, OnPress onPress) {
            super(x, y, width, height, text, onPress, DEFAULT_NARRATION);
            this.section = section;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            // Custom rendering to match vanilla style
            if (!this.active) {
                // Highlight active section
                graphics.fill(this.getX() - 2, this.getY() - 2,
                        this.getX() + this.width + 2, this.getY() + this.height + 2,
                        ACTIVE_BUTTON_COLOR);
            }
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
        }
    }

    /**
     * Enum for all GUI sections
     */
    @Getter
    public enum GuiSection {
        HOME("Home"),
        BREATHING_STYLES("Breathing Styles"),
        SKILLS("Skills"),
        BESTIARY("Bestiary"),
        PERKS("Perks"),
        QUESTS("Quests"),
        REPUTATION("Reputation"),
        COSMETICS("Cosmetics"),
        MOVESET("Moveset"),
        CONFIG("Config");

        private final String displayName;

        GuiSection(String displayName) {
            this.displayName = displayName;
        }

    }
}