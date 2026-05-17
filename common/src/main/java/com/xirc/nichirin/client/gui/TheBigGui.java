package com.xirc.nichirin.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xirc.nichirin.client.gui.NichirinPalette;
import com.xirc.nichirin.client.gui.biggui.*;
import com.xirc.nichirin.registry.NichirinKeybindRegistry;
import com.xirc.nichirin.common.util.PlayerStats;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * THE BIG GUI - Main menu system for all mod features
 * Full-screen interface with vanilla+ styling, unlock system, and translatable text
 * FIXED SCALE: Always renders at GUI scale 2 regardless of user's GUI scale setting
 */
public class TheBigGui extends Screen {

    // UI Constants
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 4;
    private static final int RIGHT_MARGIN = 10;
    private static final int TOP_MARGIN = 40;
    private static final int CONTENT_X = 10;
    private static final int CONTENT_Y = TOP_MARGIN - 20;
    private static final int BOTTOM_MARGIN = 10;

    // Colors
    private static final int BACKGROUND_COLOR    = NichirinPalette.BG_DARK;
    private static final int ACTIVE_BUTTON_COLOR = NichirinPalette.BG_BOX_ACTIVE;

    // Current section
    private GuiSection currentSection = GuiSection.HOME;

    // Player reference
    private final Player player;

    // Section buttons
    private final List<SectionButton> sectionButtons = new ArrayList<>();

    // Section instances
    private final HomeSection homeSection = new HomeSection();
    private final SkillsSection skillsSection = new SkillsSection();
    private final BestiarySection bestiarySection = new BestiarySection();
    private final QuestsSection questsSection = new QuestsSection();
    private final ReputationSection reputationSection = new ReputationSection();
    private final MovesetSection movesetSection = new MovesetSection();
    @Getter
    private int scaledWidth;
    @Getter
    private int scaledHeight;

    public TheBigGui(Player player) {
        super(Component.translatable("gui.nichirin.main.title"));
        this.player = player;

        // Initialize player stats when GUI is created
        PlayerStats.initialize();
    }

    @Override
    protected void init() {
        super.init();

        // Calculate our fixed scale dimensions
        calculateScaledDimensions();

        // Clear previous buttons
        sectionButtons.clear();

        // Calculate button positions using scaled dimensions
        int buttonX = this.scaledWidth - BUTTON_WIDTH - RIGHT_MARGIN;
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

    private float getScaleFactor() {
        return 2.0f / (float) Minecraft.getInstance().getWindow().getGuiScale();
    }

    private void calculateScaledDimensions() {
        var window = Minecraft.getInstance().getWindow();
        // Always treat the GUI as if it were at scale 2 — virtual size = pixel_size / 2
        this.scaledWidth  = Math.max(window.getWidth()  / 2, 320);
        this.scaledHeight = Math.max(window.getHeight() / 2, 240);
    }

    /** Converts a raw GUI-scaled mouse coordinate to the scale-2 coordinate space. */
    private int toScale2(double coord) {
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        return (int)(coord * guiScale / 2.0);
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
        calculateScaledDimensions();

        float scaleFactor = getScaleFactor();
        // Convert raw GUI-scaled mouse to scale-2 space
        int adjMouseX = toScale2(mouseX);
        int adjMouseY = toScale2(mouseY);

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.scale(scaleFactor, scaleFactor, 1.0f);

        graphics.fill(0, 0, this.scaledWidth, this.scaledHeight, BACKGROUND_COLOR);

        int contentRight = this.scaledWidth - BUTTON_WIDTH - RIGHT_MARGIN - 10;
        graphics.fill(CONTENT_X, CONTENT_Y, contentRight, this.scaledHeight - BOTTOM_MARGIN, 0xFF1A1817);

        int contentWidth = contentRight - CONTENT_X;
        int contentHeight = this.scaledHeight - BOTTOM_MARGIN - CONTENT_Y;

        poseStack.pushPose();
        poseStack.translate(CONTENT_X, CONTENT_Y, 0);
        int contentMouseX = adjMouseX - CONTENT_X;
        int contentMouseY = adjMouseY - CONTENT_Y;
        switch (currentSection) {
            case HOME -> homeSection.render(graphics, contentMouseX, contentMouseY, player, contentWidth, contentHeight, this.font);
            case SKILLS -> skillsSection.render(graphics, player, this.font, contentWidth, contentHeight, contentMouseX, contentMouseY);
            case BESTIARY -> bestiarySection.render(graphics, player, this.font, contentWidth, contentHeight, contentMouseX, contentMouseY);
            case QUESTS -> questsSection.render(graphics, player, this.font, contentWidth, contentHeight, contentMouseX, contentMouseY);
            case REPUTATION -> reputationSection.render(graphics, player, this.font, contentWidth, contentHeight, contentMouseX, contentMouseY);
            case MOVESET -> movesetSection.render(graphics, player, this.font, contentWidth, contentHeight, contentMouseX, contentMouseY);
        }
        poseStack.popPose();

        for (SectionButton button : sectionButtons) {
            button.render(graphics, adjMouseX, adjMouseY, partialTick);
        }

        poseStack.popPose();
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

        // Close on the Open GUI key
        if (NichirinKeybindRegistry.OPEN_GUI_KEY.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }

        // Add F5 key to refresh stats (useful for debugging)
        if (keyCode == 294) { // F5 key
            PlayerStats.requestStatsFromServer();
            return true;
        }

        // Forward to skills section for search bar
        if (currentSection == GuiSection.SKILLS) {
            if (skillsSection.handleKeyTyped((char) 0, keyCode)) return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (currentSection == GuiSection.SKILLS) {
            if (skillsSection.handleKeyTyped(c, 0)) return true;
        }
        return super.charTyped(c, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        double adjX = toScale2(mouseX);
        double adjY = toScale2(mouseY);
        if (currentSection == GuiSection.SKILLS) {
            if (skillsSection.handleScroll(adjX - CONTENT_X, adjY - CONTENT_Y, delta)) return true;
        }
        return super.mouseScrolled(adjX, adjY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double adjX = toScale2(mouseX);
        double adjY = toScale2(mouseY);

        int contentRight = this.scaledWidth - BUTTON_WIDTH - RIGHT_MARGIN - 10;
        int clickContentWidth = contentRight - CONTENT_X;
        int clickContentHeight = this.scaledHeight - BOTTOM_MARGIN - CONTENT_Y;
        double contentMouseX = adjX - CONTENT_X;
        double contentMouseY = adjY - CONTENT_Y;

        // Handle section-specific clicks
        boolean handled = switch (currentSection) {
            case HOME -> homeSection.handleClick(contentMouseX, contentMouseY, player);
            case SKILLS -> skillsSection.handleClick(contentMouseX, contentMouseY, player, clickContentWidth, clickContentHeight);
            case BESTIARY -> bestiarySection.handleClick(contentMouseX, contentMouseY, player);
            case QUESTS -> questsSection.handleClick(contentMouseX, contentMouseY, player);
            case REPUTATION -> reputationSection.handleClick(contentMouseX, contentMouseY, player);
            case MOVESET -> movesetSection.handleClick(contentMouseX, contentMouseY, player, clickContentWidth, clickContentHeight);
        };

        if (handled) {
            return true;
        }

        // Pass scale-2 coords to widgets (buttons are placed at scale-2 positions)
        return super.mouseClicked(adjX, adjY, button);
    }

    @Getter
    private static class SectionButton extends Button {
        private final GuiSection section;

        public SectionButton(int x, int y, int width, int height, Component text, GuiSection section, OnPress onPress) {
            super(x, y, width, height, text, onPress, DEFAULT_NARRATION);
            this.section = section;
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            // Custom rendering to match vanilla style
            if (!this.active) {
                // Highlight active section
                graphics.fill(this.getX() - 2, this.getY() - 2,
                        this.getX() + this.width + 2, this.getY() + this.height + 2,
                        ACTIVE_BUTTON_COLOR);
            }
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return super.isMouseOver(mouseX, mouseY);
        }
    }

    /**
     * Enum for all GUI sections
     */
    @Getter
    public enum GuiSection {
        HOME("gui.nichirin.section.home"),
        SKILLS("gui.nichirin.section.skills"),
        BESTIARY("gui.nichirin.section.bestiary"),
        QUESTS("gui.nichirin.section.quests"),
        REPUTATION("gui.nichirin.section.reputation"),
        MOVESET("gui.nichirin.section.moveset");

        private final String translationKey;

        GuiSection(String translationKey) {
            this.translationKey = translationKey;
        }
    }
}
