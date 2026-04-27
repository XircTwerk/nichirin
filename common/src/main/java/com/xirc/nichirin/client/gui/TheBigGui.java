package com.xirc.nichirin.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
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

    // Colors
    private static final int BACKGROUND_COLOR = 0xC0101010; // Dark gray with transparency
    private static final int ACTIVE_BUTTON_COLOR = 0xFF3F3F3F;

    // Fixed scale constants
    private static final double FIXED_GUI_SCALE = 2.0;

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
    /**
     * -- GETTER --
     *  Get scaled width for use by sections
     */
    // Scaled dimensions
    @Getter
    private int scaledWidth;
    /**
     * -- GETTER --
     *  Get scaled height for use by sections
     */
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

    /**
     * Calculate dimensions for fixed GUI scale of 2
     */
    private void calculateScaledDimensions() {
        Minecraft mc = Minecraft.getInstance();

        // Get the window's framebuffer dimensions
        int framebufferWidth = mc.getWindow().getWidth();
        int framebufferHeight = mc.getWindow().getHeight();

        // Calculate what the dimensions would be at GUI scale 2
        this.scaledWidth = (int) (framebufferWidth / FIXED_GUI_SCALE);
        this.scaledHeight = (int) (framebufferHeight / FIXED_GUI_SCALE);

        // Ensure minimum dimensions
        this.scaledWidth = Math.max(this.scaledWidth, 320);
        this.scaledHeight = Math.max(this.scaledHeight, 240);
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
        // Get the current GUI scale factor
        assert minecraft != null;
        double currentScale = minecraft.getWindow().getGuiScale();
        double scaleRatio = FIXED_GUI_SCALE / currentScale;

        // Apply our fixed scaling
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.scale((float) scaleRatio, (float) scaleRatio, 1.0f);

        // Adjust mouse coordinates for our scaling
        int adjustedMouseX = (int) (mouseX / scaleRatio);
        int adjustedMouseY = (int) (mouseY / scaleRatio);

        // Recalculate dimensions in case of window resize
        calculateScaledDimensions();

        // Draw dark background
        graphics.fill(0, 0, this.scaledWidth, this.scaledHeight, BACKGROUND_COLOR);

        // Draw content area background (slightly lighter) - 20 pixels higher
        int contentRight = this.scaledWidth - BUTTON_WIDTH - RIGHT_MARGIN - 10;
        graphics.fill(10, TOP_MARGIN - 20, contentRight, this.scaledHeight - 10, 0xB0202020);

        // Calculate content area dimensions
        int contentWidth = contentRight - 10;
        int contentHeight = this.scaledHeight - 10;

        // Render current section content
        switch (currentSection) {
            case HOME -> homeSection.render(graphics, adjustedMouseX, adjustedMouseY, player, contentWidth, contentHeight, this.font);
            case SKILLS -> skillsSection.render(graphics, player, this.font, contentWidth, contentHeight, adjustedMouseX, adjustedMouseY);
            case BESTIARY -> bestiarySection.render(graphics, player, this.font);
            case QUESTS -> questsSection.render(graphics, player, this.font);
            case REPUTATION -> reputationSection.render(graphics, player, this.font);
            case MOVESET -> movesetSection.render(graphics, player, this.font, contentWidth, contentHeight, adjustedMouseX, adjustedMouseY);
        }

        // Restore pose stack before rendering buttons (they handle their own scaling)
        poseStack.popPose();

        // Scale the buttons manually using pose stack
        poseStack.pushPose();
        poseStack.scale((float) scaleRatio, (float) scaleRatio, 1.0f);

        // Render buttons with scaled coordinates
        for (SectionButton button : sectionButtons) {
            button.render(graphics, adjustedMouseX, adjustedMouseY, partialTick);
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
        if (currentSection == GuiSection.SKILLS) {
            assert minecraft != null;
            double scaleRatio = FIXED_GUI_SCALE / minecraft.getWindow().getGuiScale();
            if (skillsSection.handleScroll(mouseX / scaleRatio, mouseY / scaleRatio, delta)) return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Adjust mouse coordinates for our fixed scaling
        assert minecraft != null;
        double currentScale = minecraft.getWindow().getGuiScale();
        double scaleRatio = FIXED_GUI_SCALE / currentScale;

        double adjustedMouseX = mouseX / scaleRatio;
        double adjustedMouseY = mouseY / scaleRatio;

        // Compute content dimensions (must match render())
        int contentRight = this.scaledWidth - BUTTON_WIDTH - RIGHT_MARGIN - 10;
        int clickContentWidth = contentRight - 10;
        int clickContentHeight = this.scaledHeight - 10;

        // Handle section-specific clicks
        boolean handled = switch (currentSection) {
            case HOME -> homeSection.handleClick(adjustedMouseX, adjustedMouseY, player);
            case SKILLS -> skillsSection.handleClick(adjustedMouseX, adjustedMouseY, player, clickContentWidth, clickContentHeight);
            case BESTIARY -> bestiarySection.handleClick(adjustedMouseX, adjustedMouseY, player);
            case QUESTS -> questsSection.handleClick(adjustedMouseX, adjustedMouseY, player);
            case REPUTATION -> reputationSection.handleClick(adjustedMouseX, adjustedMouseY, player);
            case MOVESET -> movesetSection.handleClick(adjustedMouseX, adjustedMouseY, player, clickContentWidth, clickContentHeight);
        };

        if (handled) {
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
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
            // Adjust mouse coordinates to match our fixed scaling
            Minecraft mc = Minecraft.getInstance();
            double currentScale = mc.getWindow().getGuiScale();
            double scaleRatio = FIXED_GUI_SCALE / currentScale;

            double adjustedMouseX = mouseX / scaleRatio;
            double adjustedMouseY = mouseY / scaleRatio;

            // Use adjusted coordinates for hit detection
            return super.mouseClicked(adjustedMouseX, adjustedMouseY, button);
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            // Also adjust mouse coordinates for hover detection
            Minecraft mc = Minecraft.getInstance();
            double currentScale = mc.getWindow().getGuiScale();
            double scaleRatio = FIXED_GUI_SCALE / currentScale;

            double adjustedMouseX = mouseX / scaleRatio;
            double adjustedMouseY = mouseY / scaleRatio;

            return super.isMouseOver(adjustedMouseX, adjustedMouseY);
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