package com.xirc.nichirin.client.gui;

import com.xirc.nichirin.client.gui.biggui.*;
import com.xirc.nichirin.common.util.PlayerStats;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * THE BIG GUI - Main menu system for all mod features
 * Full-screen interface with vanilla+ styling, unlock system, and translatable text
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

    // Section instances
    private final HomeSection homeSection = new HomeSection();
    private final BreathingStylesSection breathingStylesSection = new BreathingStylesSection();
    private final SkillsSection skillsSection = new SkillsSection();
    private final BestiarySection bestiarySection = new BestiarySection();
    private final PerksSection perksSection = new PerksSection();
    private final QuestsSection questsSection = new QuestsSection();
    private final ReputationSection reputationSection = new ReputationSection();
    private final CosmeticsSection cosmeticsSection = new CosmeticsSection();
    private final MovesetSection movesetSection = new MovesetSection();
    private final ConfigSection configSection = new ConfigSection();

    public TheBigGui(Player player) {
        super(Component.translatable("gui.nichirin.main.title"));
        this.player = player;

        // Initialize player stats when GUI is created
        PlayerStats.initialize();
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
                    Component.translatable(section.getTranslationKey()),
                    section,
                    btn -> switchToSection(section)
            );

            this.addRenderableWidget(button);
            sectionButtons.add(button);

            buttonY += BUTTON_HEIGHT + BUTTON_SPACING;
        }

        // Initialize with home section
        switchToSection(GuiSection.HOME);

        // Request stats again when GUI is initialized (in case of screen resize, etc.)
        PlayerStats.initialize();
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

        // If switching to home section, refresh stats
        if (section == GuiSection.HOME) {
            PlayerStats.requestStatsFromServer();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Draw dark background
        graphics.fill(0, 0, this.width, this.height, BACKGROUND_COLOR);

        // Draw content area background (slightly lighter) - 20 pixels higher
        int contentRight = this.width - BUTTON_WIDTH - RIGHT_MARGIN - 10;
        graphics.fill(10, TOP_MARGIN - 20, contentRight, this.height - 10, 0xB0202020);

        // Calculate content area dimensions
        int contentWidth = contentRight - 10;
        int contentHeight = this.height - 10;

        // Render current section content
        switch (currentSection) {
            case HOME -> homeSection.render(graphics, mouseX, mouseY, player, contentWidth, contentHeight, this.font);
            case BREATHING_STYLES -> breathingStylesSection.render(graphics, player, contentWidth, contentHeight, this.font);
            case SKILLS -> skillsSection.render(graphics, player, this.font);
            case BESTIARY -> bestiarySection.render(graphics, player, this.font);
            case PERKS -> perksSection.render(graphics, player, this.font);
            case QUESTS -> questsSection.render(graphics, player, this.font);
            case REPUTATION -> reputationSection.render(graphics, player, this.font);
            case COSMETICS -> cosmeticsSection.render(graphics, player, this.font);
            case MOVESET -> movesetSection.render(graphics, player, this.font);
            case CONFIG -> configSection.render(graphics, player, this.font);
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
        // Close on ESC
        if (keyCode == 256) { // ESC key
            this.onClose();
            return true;
        }

        // Add F5 key to refresh stats (useful for debugging)
        if (keyCode == 294) { // F5 key
            PlayerStats.requestStatsFromServer();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Handle section-specific clicks
        boolean handled = switch (currentSection) {
            case BREATHING_STYLES -> {
                int contentWidth = this.width - BUTTON_WIDTH - RIGHT_MARGIN - 20;
                yield breathingStylesSection.handleClick(mouseX, mouseY, player, contentWidth);
            }
            case HOME -> homeSection.handleClick(mouseX, mouseY, player);
            case SKILLS -> skillsSection.handleClick(mouseX, mouseY, player);
            case BESTIARY -> bestiarySection.handleClick(mouseX, mouseY, player);
            case PERKS -> perksSection.handleClick(mouseX, mouseY, player);
            case QUESTS -> questsSection.handleClick(mouseX, mouseY, player);
            case REPUTATION -> reputationSection.handleClick(mouseX, mouseY, player);
            case COSMETICS -> cosmeticsSection.handleClick(mouseX, mouseY, player);
            case MOVESET -> movesetSection.handleClick(mouseX, mouseY, player);
            case CONFIG -> configSection.handleClick(mouseX, mouseY, player);
        };

        if (handled) {
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
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
        HOME("gui.nichirin.section.home"),
        BREATHING_STYLES("gui.nichirin.section.breathing_styles"),
        SKILLS("gui.nichirin.section.skills"),
        BESTIARY("gui.nichirin.section.bestiary"),
        PERKS("gui.nichirin.section.perks"),
        QUESTS("gui.nichirin.section.quests"),
        REPUTATION("gui.nichirin.section.reputation"),
        COSMETICS("gui.nichirin.section.cosmetics"),
        MOVESET("gui.nichirin.section.moveset"),
        CONFIG("gui.nichirin.section.config");

        private final String translationKey;

        GuiSection(String translationKey) {
            this.translationKey = translationKey;
        }
    }
}