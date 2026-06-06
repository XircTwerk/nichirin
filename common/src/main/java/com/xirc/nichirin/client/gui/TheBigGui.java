package com.xirc.nichirin.client.gui;

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

    // Scale applied to the entire GUI so it fills the window proportionally.
    // Stored as a field so mouse-event handlers can transform coords without recomputing.
    private float contentScale = 1.0f;


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
    // ObtainmentSection lives as a subtab inside MovesetSection now, not as a top-level section.

    public TheBigGui(Player player) {
        super(Component.translatable("gui.nichirin.main.title"));
        this.player = player;

        // Initialize player stats when GUI is created
        PlayerStats.initialize();
    }

    @Override
    protected void init() {
        super.init();

        // Compute contentScale FIRST so button positions use logical coordinates.
        var window = Minecraft.getInstance().getWindow();
        int physW = window.getWidth();
        int refScale = Math.max(1, physW / 960);
        double actualScale = window.getGuiScale();
        contentScale = (float) (refScale / actualScale);

        int logW = Math.round(this.width / contentScale);

        // Clear previous buttons
        sectionButtons.clear();

        int buttonX = logW - BUTTON_WIDTH - RIGHT_MARGIN;
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
        graphics.pose().pushPose();
        graphics.pose().scale(contentScale, contentScale, 1.0f);

        int logW = Math.round(this.width  / contentScale);
        int logH = Math.round(this.height / contentScale);
        int logMouseX = Math.round(mouseX / contentScale);
        int logMouseY = Math.round(mouseY / contentScale);

        graphics.fill(0, 0, logW, logH, BACKGROUND_COLOR);

        int contentRight  = logW - BUTTON_WIDTH - RIGHT_MARGIN - 10;
        graphics.fill(CONTENT_X, CONTENT_Y, contentRight, logH - BOTTOM_MARGIN, 0xFF1A1817);

        int contentWidth  = contentRight - CONTENT_X;
        int contentHeight = logH - BOTTOM_MARGIN - CONTENT_Y;
        int contentMouseX = logMouseX - CONTENT_X;
        int contentMouseY = logMouseY - CONTENT_Y;

        graphics.pose().pushPose();
        graphics.pose().translate(CONTENT_X, CONTENT_Y, 0);
        switch (currentSection) {
            case HOME       -> homeSection      .render(graphics, contentMouseX, contentMouseY, player, contentWidth, contentHeight, this.font);
            case SKILLS     -> skillsSection    .render(graphics, player, this.font, contentWidth, contentHeight, contentMouseX, contentMouseY);
            case BESTIARY   -> bestiarySection  .render(graphics, player, this.font, contentWidth, contentHeight, contentMouseX, contentMouseY);
            case QUESTS     -> questsSection    .render(graphics, player, this.font, contentWidth, contentHeight, contentMouseX, contentMouseY);
            case REPUTATION -> reputationSection.render(graphics, player, this.font, contentWidth, contentHeight, contentMouseX, contentMouseY);
            case MOVESET    -> movesetSection   .render(graphics, player, this.font, contentWidth, contentHeight, contentMouseX, contentMouseY);
        }
        graphics.pose().popPose();

        for (SectionButton button : sectionButtons) {
            button.render(graphics, logMouseX, logMouseY, partialTick);
        }

        graphics.pose().popPose();
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Don't pause the game
    }

    @Override
    public void removed() {
        super.removed();
    }

    @Override
    public void onClose() {
        super.onClose();
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
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        double lx = mouseX / contentScale - CONTENT_X;
        double ly = mouseY / contentScale - CONTENT_Y;
        if (currentSection == GuiSection.SKILLS) {
            if (skillsSection.handleScroll(lx, ly, verticalAmount)) return true;
        }
        return super.mouseScrolled(mouseX / contentScale, mouseY / contentScale, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double logMouseX = mouseX / contentScale;
        double logMouseY = mouseY / contentScale;
        double contentMouseX = logMouseX - CONTENT_X;
        double contentMouseY = logMouseY - CONTENT_Y;

        int logW = Math.round(this.width  / contentScale);
        int logH = Math.round(this.height / contentScale);
        int contentRight  = logW - BUTTON_WIDTH - RIGHT_MARGIN - 10;
        int clickContentW = contentRight - CONTENT_X;
        int clickContentH = logH - BOTTOM_MARGIN - CONTENT_Y;

        boolean handled = switch (currentSection) {
            case HOME       -> homeSection      .handleClick(contentMouseX, contentMouseY, player);
            case SKILLS     -> skillsSection    .handleClick(contentMouseX, contentMouseY, player, clickContentW, clickContentH);
            case BESTIARY   -> bestiarySection  .handleClick(contentMouseX, contentMouseY, player);
            case QUESTS     -> questsSection    .handleClick(contentMouseX, contentMouseY, player);
            case REPUTATION -> reputationSection.handleClick(contentMouseX, contentMouseY, player);
            case MOVESET    -> movesetSection   .handleClick(contentMouseX, contentMouseY, player, clickContentW, clickContentH);
        };

        if (handled) return true;
        return super.mouseClicked(logMouseX, logMouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        double logMouseX = mouseX / contentScale;
        double logMouseY = mouseY / contentScale;
        double contentMouseX = logMouseX - CONTENT_X;
        double contentMouseY = logMouseY - CONTENT_Y;

        int logW = Math.round(this.width  / contentScale);
        int logH = Math.round(this.height / contentScale);
        int contentRight  = logW - BUTTON_WIDTH - RIGHT_MARGIN - 10;
        int clickContentW = contentRight - CONTENT_X;
        int clickContentH = logH - BOTTOM_MARGIN - CONTENT_Y;

        if (currentSection == GuiSection.MOVESET
                && movesetSection.handleRelease(contentMouseX, contentMouseY, player, clickContentW, clickContentH)) {
            return true;
        }
        return super.mouseReleased(logMouseX, logMouseY, button);
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
