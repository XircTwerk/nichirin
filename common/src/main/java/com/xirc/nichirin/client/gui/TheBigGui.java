package com.xirc.nichirin.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import com.xirc.nichirin.BreathOfNichirin;

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
            button.setActive(button.getSection() != section);
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

        // Player name
        graphics.drawString(this.font,
                Component.literal("Player: " + player.getName().getString()),
                contentX, contentY, 0xFFFFFF);
        contentY += 20;

        // Slayer rank
        graphics.drawString(this.font,
                Component.literal("Slayer Rank: Mizunoto"), // Placeholder
                contentX, contentY, 0xFFFFFF);
        contentY += 20;

        // Breathing style
        String breathingStyle = "None"; // TODO: Get from player data
        graphics.drawString(this.font,
                Component.literal("Breathing Style: " + breathingStyle),
                contentX, contentY, 0xFFFFFF);
        contentY += 20;

        // Demon kill count
        graphics.drawString(this.font,
                Component.literal("Demons Slain: 0"), // Placeholder
                contentX, contentY, 0xFFFFFF);

        // TODO: Add 3D player model render
    }

    private void renderBreathingStylesContent(GuiGraphics graphics) {
        int contentX = 20;
        int contentY = TOP_MARGIN + 10;

        graphics.drawString(this.font,
                Component.literal("Available Breathing Styles:"),
                contentX, contentY, 0xFFFFFF);
        contentY += 20;

        // TODO: List breathing styles with unlock status
        graphics.drawString(this.font,
                Component.literal("• Water Breathing - Unlocked"),
                contentX, contentY, 0x55FF55);
        contentY += 15;

        graphics.drawString(this.font,
                Component.literal("• Thunder Breathing - Locked"),
                contentX, contentY, 0x555555);
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
    private static class SectionButton extends Button {
        private final GuiSection section;

        public SectionButton(int x, int y, int width, int height, Component text, GuiSection section, OnPress onPress) {
            super(x, y, width, height, text, onPress, DEFAULT_NARRATION);
            this.section = section;
        }

        public GuiSection getSection() {
            return section;
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

        public String getDisplayName() {
            return displayName;
        }
    }
}