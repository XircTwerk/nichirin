package com.xirc.nichirin.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.xirc.nichirin.common.data.BreathingStyleHelper;
import com.xirc.nichirin.common.data.BreathingStyleProgression;
import com.xirc.nichirin.common.data.ProgressionHelper;
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

    // Content renderers
    private Consumer<GuiGraphics> currentContentRenderer;

    public TheBigGui(Player player) {
        super(Component.translatable("gui.nichirin.main.title"));
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
        Component title = Component.translatable(currentSection.getTranslationKey());
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
        // Close on ESC
        if (keyCode == 256) { // ESC key
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Handle breathing style selection clicks
        if (currentSection == GuiSection.BREATHING_STYLES) {
            String currentStyle = BreathingStyleHelper.getMovesetId(player);

            // Calculate click area for Thunder Breathing box
            int centerX = (this.width - BUTTON_WIDTH - RIGHT_MARGIN - 20) / 2;
            int boxWidth = 150;
            int boxHeight = 80;
            int x = centerX - boxWidth / 2;
            int y = TOP_MARGIN + 10 + 30 + 25 + 20 + 10;

            // Check if click is within Thunder Breathing box
            if (mouseX >= x && mouseX <= x + boxWidth && mouseY >= y && mouseY <= y + boxHeight) {
                String styleName = "thunder_breathing";

                // Check if the style is unlocked
                if (!ProgressionHelper.isStyleUnlocked(player, styleName)) {
                    // Style is locked - just play error sound, no message
                    Minecraft.getInstance().getSoundManager().play(
                            net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.5F, 0.8F
                            )
                    );
                    return true;
                }

                // Style is unlocked - only set if not already selected
                if (!styleName.equals(currentStyle)) {
                    // Set the breathing style for the player
                    BreathingStyleHelper.setMovesetId(player, styleName);

                    // Play success sound
                    Minecraft.getInstance().getSoundManager().play(
                            net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.0F
                            )
                    );

                    // Don't call this.init() - just update the current section to refresh the display
                    switchToSection(GuiSection.BREATHING_STYLES);
                }
                return true;
            }

            // Check for "None" button click
            int noneButtonX = centerX - 75;
            int noneButtonY = y + boxHeight + 30;
            int noneButtonWidth = 150;
            int noneButtonHeight = 20;

            if (mouseX >= noneButtonX && mouseX <= noneButtonX + noneButtonWidth &&
                    mouseY >= noneButtonY && mouseY <= noneButtonY + noneButtonHeight) {

                // Toggle between None and current style
                if (currentStyle != null) {
                    // Clear breathing style (but preserve unlock)
                    BreathingStyleHelper.setMovesetId(player, null);
                } else {
                    // Find any unlocked style and set it
                    if (ProgressionHelper.isStyleUnlocked(player, "thunder_breathing")) {
                        BreathingStyleHelper.setMovesetId(player, "thunder_breathing");
                    }
                }

                // Play click sound
                Minecraft.getInstance().getSoundManager().play(
                        net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.0F
                        )
                );

                // Don't call this.init() - just update the current section to refresh the display
                switchToSection(GuiSection.BREATHING_STYLES);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    // Content rendering methods

    private void renderHomeContent(GuiGraphics graphics) {
        int contentX = 20;
        int contentY = TOP_MARGIN + 10;
        int centerX = (this.width - BUTTON_WIDTH - RIGHT_MARGIN - 20) / 2;

        // === 3D Player Model ===
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
        BreathingStyleProgression progression = ProgressionHelper.getProgression(player);

        // Title
        Component statsTitle = Component.translatable("gui.nichirin.home.player_stats").withStyle(style -> style.withBold(true));
        graphics.drawString(this.font, statsTitle,
                centerX - this.font.width(statsTitle) / 2, statsY, 0xFFFFFF);
        statsY += 25;

        // Player name
        drawStatLine(graphics, contentX, statsY, Component.translatable("gui.nichirin.home.name"), player.getName().getString(), 0xFFD700);
        statsY += statLineHeight;

        // Slayer rank
        String slayerRank = progression.getSlayerRankName() + " (Rank " + progression.getSlayerRank() + ")";
        drawStatLine(graphics, contentX, statsY, Component.translatable("gui.nichirin.home.slayer_rank"), slayerRank, 0x5555FF);
        statsY += statLineHeight;

        // Breathing style
        String breathingStyle = BreathingStyleHelper.getMovesetId(player);
        String breathingStyleDisplay = breathingStyle != null ? Component.translatable("breathing_style." + breathingStyle).getString() : Component.translatable("gui.nichirin.home.none").getString();
        drawStatLine(graphics, contentX, statsY, Component.translatable("gui.nichirin.home.breathing_style"), breathingStyleDisplay, 0x55FFFF);
        statsY += statLineHeight;

        // Combat stats separator
        statsY += 10;
        graphics.drawString(this.font, "─────────────────────", contentX, statsY, 0x555555);
        statsY += 15;

        // Demon kill count
        String demonKills = String.valueOf(progression.getDemonsSlain());
        drawStatLine(graphics, contentX, statsY, Component.translatable("gui.nichirin.home.demons_slain"), demonKills, 0xFF5555);
        statsY += statLineHeight;

        // Total damage dealt
        String totalDamage = String.valueOf(progression.getTotalDamageDealt());
        drawStatLine(graphics, contentX, statsY, Component.translatable("gui.nichirin.home.total_damage"), totalDamage, 0xFFAA00);
        statsY += statLineHeight;

        // Breathing experience
        String breathExp = String.valueOf(progression.getBreathingExperience());
        drawStatLine(graphics, contentX, statsY, Component.translatable("gui.nichirin.home.breathing_exp"), breathExp, 0x55FF55);
        statsY += statLineHeight;

        // Total playtime
        long playtime = player.level().getGameTime() / 20;
        String playtimeStr = formatPlaytime(playtime);
        drawStatLine(graphics, contentX, statsY, Component.translatable("gui.nichirin.home.playtime"), playtimeStr, 0xAAAAAA);
        statsY += statLineHeight;

        // Level/Experience
        int level = player.experienceLevel;
        drawStatLine(graphics, contentX, statsY, Component.translatable("gui.nichirin.home.level"), String.valueOf(level), 0x55FF55);

        // Instructions at bottom
        int bottomY = this.height - 30;
        Component instructions = Component.translatable("gui.nichirin.home.instructions")
                .withStyle(style -> style.withColor(0x777777).withItalic(true));
        graphics.drawString(this.font, instructions,
                (this.width - BUTTON_WIDTH - RIGHT_MARGIN - this.font.width(instructions)) / 2,
                bottomY, 0x777777);
    }

    private void renderBreathingStylesContent(GuiGraphics graphics) {
        int contentX = 20;
        int contentY = TOP_MARGIN + 10;
        int centerX = (this.width - BUTTON_WIDTH - RIGHT_MARGIN - 20) / 2;

        // Title
        Component title = Component.translatable("gui.nichirin.breathing_styles.title").withStyle(style -> style.withBold(true));
        graphics.drawString(this.font, title,
                centerX - this.font.width(title) / 2, contentY, 0xFFFFFF);
        contentY += 30;

        // Current style
        String currentStyle = BreathingStyleHelper.getMovesetId(player);
        if (currentStyle != null) {
            Component current = Component.translatable("gui.nichirin.breathing_styles.current",
                            Component.translatable("breathing_style." + currentStyle))
                    .withStyle(style -> style.withColor(0x55FFFF));
            graphics.drawString(this.font, current, contentX, contentY, 0x55FFFF);
            contentY += 25;
        }

        // Instructions
        Component instructions = Component.translatable("gui.nichirin.breathing_styles.instructions");
        graphics.drawString(this.font, instructions, contentX, contentY, 0xAAAAAA);
        contentY += 20;

        // Style grid - Only Thunder Breathing for now
        int gridY = contentY + 10;
        int boxWidth = 150;
        int boxHeight = 80;

        // Thunder Breathing
        String styleName = "thunder_breathing";
        boolean isUnlocked = ProgressionHelper.isStyleUnlocked(player, styleName);
        boolean isSelected = styleName.equals(currentStyle);

        // Center the single box
        int x = centerX - boxWidth / 2;
        int y = gridY;

        // Draw box with different colors based on unlock status
        int bgColor;
        int borderColor;

        if (!isUnlocked) {
            bgColor = 0xFF1A1A1A; // Darker for locked
            borderColor = 0xFF666666; // Gray border for locked
        } else if (isSelected) {
            bgColor = 0xFF3A3A3A;
            borderColor = 0xFF55FFFF; // Cyan for selected
        } else {
            bgColor = 0xFF2A2A2A;
            borderColor = 0xFF4A4A4A; // Normal border
        }

        // Border
        graphics.fill(x - 1, y - 1, x + boxWidth + 1, y + boxHeight + 1, borderColor);
        // Background
        graphics.fill(x, y, x + boxWidth, y + boxHeight, bgColor);

        // Style name
        Component displayName = Component.translatable("breathing_style.thunder_breathing");
        int nameColor = isUnlocked ? 0xFFFFFF : 0x888888;
        graphics.drawString(this.font, displayName,
                x + (boxWidth - this.font.width(displayName)) / 2,
                y + 10, nameColor);

        // Status
        if (!isUnlocked) {
            Component locked = Component.translatable("gui.nichirin.breathing_styles.locked_status").withStyle(style -> style.withColor(0xFF5555));
            graphics.drawString(this.font, locked,
                    x + (boxWidth - this.font.width(locked)) / 2,
                    y + 30, 0xFF5555);
        } else if (isSelected) {
            Component equipped = Component.translatable("gui.nichirin.breathing_styles.equipped").withStyle(style -> style.withColor(0x55FFFF));
            graphics.drawString(this.font, equipped,
                    x + (boxWidth - this.font.width(equipped)) / 2,
                    y + 30, 0x55FFFF);
        } else {
            Component clickToSelect = Component.translatable("gui.nichirin.breathing_styles.click_to_select").withStyle(style -> style.withColor(0xAAAAAA));
            graphics.drawString(this.font, clickToSelect,
                    x + (boxWidth - this.font.width(clickToSelect)) / 2,
                    y + 30, 0xAAAAAA);
        }

        // Icon placeholder (thunder icon)
        int iconColor = isUnlocked ? 0xFF3A3A3A : 0xFF2A2A2A;
        graphics.fill(x + boxWidth/2 - 16, y + 50, x + boxWidth/2 + 16, y + 75, iconColor);

        // Show unlock requirements if locked
        if (!isUnlocked) {
            int reqY = y + boxHeight + 15;
            Component reqTitle = Component.translatable("gui.nichirin.breathing_styles.unlock_requirements").withStyle(style -> style.withBold(true));
            graphics.drawString(this.font, reqTitle,
                    centerX - this.font.width(reqTitle) / 2, reqY, 0xFFFFFF);
            reqY += 15;

            String requirement = ProgressionHelper.getUnlockRequirement(styleName);
            graphics.drawString(this.font, requirement,
                    centerX - this.font.width(requirement) / 2, reqY, 0xFFAA00);
            reqY += 20;
        }

        // "None" button
        int noneButtonY = y + boxHeight + (isUnlocked ? 15 : 55);
        int noneButtonX = centerX - 75;
        int noneButtonWidth = 150;
        int noneButtonHeight = 20;

        // None button background
        int noneButtonBg = (currentStyle == null) ? 0xFF3A3A3A : 0xFF2A2A2A;
        int noneButtonBorder = (currentStyle == null) ? 0xFF55FFFF : 0xFF4A4A4A;

        graphics.fill(noneButtonX - 1, noneButtonY - 1,
                noneButtonX + noneButtonWidth + 1, noneButtonY + noneButtonHeight + 1, noneButtonBorder);
        graphics.fill(noneButtonX, noneButtonY,
                noneButtonX + noneButtonWidth, noneButtonY + noneButtonHeight, noneButtonBg);

        Component noneText = Component.translatable("gui.nichirin.breathing_styles.none");
        int noneTextColor = (currentStyle == null) ? 0x55FFFF : 0xAAAAAA;
        graphics.drawString(this.font, noneText,
                noneButtonX + (noneButtonWidth - this.font.width(noneText)) / 2,
                noneButtonY + 6, noneTextColor);

        // Coming soon text
        Component comingSoon = Component.translatable("gui.nichirin.breathing_styles.coming_soon")
                .withStyle(style -> style.withColor(0x777777).withItalic(true));
        graphics.drawString(this.font, comingSoon,
                centerX - this.font.width(comingSoon) / 2,
                this.height - 60, 0x777777);
    }

    /**
     * Renders a stat line with label and value
     */
    private void drawStatLine(GuiGraphics graphics, int x, int y, Component label, String value, int valueColor) {
        // Draw label
        String labelText = label.getString() + ":";
        graphics.drawString(this.font, labelText, x, y, 0xAAAAAA);

        // Draw value (right-aligned within content area)
        int valueX = this.width - BUTTON_WIDTH - RIGHT_MARGIN - 30 - this.font.width(value);
        graphics.drawString(this.font, value, valueX, y, valueColor);

        // Draw connecting dots
        String dots = ".".repeat(Math.max(1, (valueX - x - this.font.width(labelText) - 5) / 4));
        graphics.drawString(this.font, dots, x + this.font.width(labelText), y, 0x444444);
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
        float rotation = (System.currentTimeMillis() / 50L % 360L) * 0.017453292F;

        graphics.pose().pushPose();
        graphics.pose().translate(x, y + 55, 50.0F);
        graphics.pose().scale((float)size, (float)size, (float)size);

        Quaternionf quaternion = Axis.ZP.rotationDegrees(180.0F);
        Quaternionf quaternion2 = Axis.XP.rotationDegrees(-20.0F);
        Quaternionf quaternion3 = Axis.YP.rotation(rotation);

        quaternion.mul(quaternion2);
        quaternion.mul(quaternion3);

        graphics.pose().mulPose(quaternion);

        Lighting.setupForEntityInInventory();

        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        entityrenderdispatcher.setRenderShadow(false);

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

    private void renderSkillsContent(GuiGraphics graphics) {
        Component comingSoon = Component.translatable("gui.nichirin.coming_soon");
        graphics.drawString(this.font, comingSoon, 20, TOP_MARGIN + 10, 0xFFFFFF);
    }

    private void renderBestiaryContent(GuiGraphics graphics) {
        Component comingSoon = Component.translatable("gui.nichirin.coming_soon");
        graphics.drawString(this.font, comingSoon, 20, TOP_MARGIN + 10, 0xFFFFFF);
    }

    private void renderPerksContent(GuiGraphics graphics) {
        Component comingSoon = Component.translatable("gui.nichirin.coming_soon");
        graphics.drawString(this.font, comingSoon, 20, TOP_MARGIN + 10, 0xFFFFFF);
    }

    private void renderQuestsContent(GuiGraphics graphics) {
        Component comingSoon = Component.translatable("gui.nichirin.coming_soon");
        graphics.drawString(this.font, comingSoon, 20, TOP_MARGIN + 10, 0xFFFFFF);
    }

    private void renderReputationContent(GuiGraphics graphics) {
        Component comingSoon = Component.translatable("gui.nichirin.coming_soon");
        graphics.drawString(this.font, comingSoon, 20, TOP_MARGIN + 10, 0xFFFFFF);
    }

    private void renderCosmeticsContent(GuiGraphics graphics) {
        Component comingSoon = Component.translatable("gui.nichirin.coming_soon");
        graphics.drawString(this.font, comingSoon, 20, TOP_MARGIN + 10, 0xFFFFFF);
    }

    private void renderMovesetContent(GuiGraphics graphics) {
        int contentX = 20;
        int contentY = TOP_MARGIN + 10;

        Component title = Component.translatable("gui.nichirin.moveset.title");
        graphics.drawString(this.font, title, contentX, contentY, 0xFFFFFF);
        contentY += 20;

        // Show current breathing style moveset
        String currentStyle = BreathingStyleHelper.getMovesetId(player);
        if (currentStyle != null) {
            Component styleLabel = Component.translatable("gui.nichirin.moveset.current_style",
                    Component.translatable("breathing_style." + currentStyle));
            graphics.drawString(this.font, styleLabel, contentX, contentY, 0x55FFFF);
            contentY += 15;

            Component moveDetails = Component.translatable("gui.nichirin.moveset.move_details_coming_soon");
            graphics.drawString(this.font, moveDetails, contentX, contentY, 0xAAAAAA);
        } else {
            Component selectStyle = Component.translatable("gui.nichirin.moveset.select_style");
            graphics.drawString(this.font, selectStyle, contentX, contentY, 0xAAAAAA);
        }
    }

    private void renderConfigContent(GuiGraphics graphics) {
        Component comingSoon = Component.translatable("gui.nichirin.coming_soon");
        graphics.drawString(this.font, comingSoon, 20, TOP_MARGIN + 10, 0xFFFFFF);
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