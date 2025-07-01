package com.xirc.nichirin.client.gui;

import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;

@Getter
public abstract class GuiBar {

    // Bar dimensions
    private static final int BAR_WIDTH = 42;
    private static final int BAR_HEIGHT = 6;

    // Colors
    private static final int BLACK_OUTLINE = 0x000000;
    private static final int GRAY_BACKGROUND = 0xFF404040;

    /**
     * -- GETTER --
     *  Gets the current value
     */
    // Current and maximum values
    protected float currentValue;
    /**
     * -- GETTER --
     *  Gets the maximum value
     */
    protected float maxValue;

    public GuiBar(float maxValue) {
        this.maxValue = maxValue;
        this.currentValue = maxValue;
    }

    /**
     * Updates the current value of the bar
     */
    public void updateValue(float current) {
        this.currentValue = Math.max(0, Math.min(current, maxValue));
    }

    /**
     * Gets the fill color for this specific bar type
     * Must be implemented by subclasses
     */
    protected abstract int getFillColor();

    /**
     * Renders the bar at the specified position
     */
    public void render(GuiGraphics graphics, int x, int y) {
        // Step 1: Draw black outline (full 42x6 rectangle)
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, BLACK_OUTLINE);

        // Step 2: Draw gray background inside the outline (40x4 rectangle, 1px inset on all sides)
        graphics.fill(x + 1, y + 1, x + BAR_WIDTH - 1, y + BAR_HEIGHT - 1, GRAY_BACKGROUND);

        // Step 3: Draw the colored fill based on current/max ratio
        if (maxValue > 0 && currentValue > 0) {
            float fillPercent = currentValue / maxValue;
            int innerWidth = BAR_WIDTH - 2; // 40 pixels (42 - 2 for 1px border on each side)
            int fillWidth = Math.max(1, (int)(innerWidth * fillPercent)); // Ensure at least 1px when not empty

            // Only draw if there's something to fill
            if (fillWidth > 0) {
                graphics.fill(x + 1, y + 1, x + 1 + fillWidth, y + BAR_HEIGHT - 1, getFillColor());
            }
        }
    }

    /**
     * Gets the fill percentage (0.0 to 1.0)
     */
    public float getFillPercentage() {
        return maxValue > 0 ? currentValue / maxValue : 0;
    }

    /**
     * Sets the maximum value
     */
    public void setMaxValue(float maxValue) {
        this.maxValue = Math.max(1, maxValue);
        this.currentValue = Math.min(this.currentValue, this.maxValue);
    }
}