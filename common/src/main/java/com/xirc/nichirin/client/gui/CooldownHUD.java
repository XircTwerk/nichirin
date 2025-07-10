package com.xirc.nichirin.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class CooldownHUD {

    private static final Map<String, CooldownEntry> cooldowns = new HashMap<>();
    private static final int DISPLAY_X = 10; // From right edge
    private static final int DISPLAY_Y = 10; // From top edge
    private static final int ENTRY_HEIGHT = 20;
    private static final int MIN_ENTRY_WIDTH = 80;
    private static final int PADDING = 6; // Padding on each side
    private static final int TIME_SPACING = 10; // Space between name and time

    /**
     * Renders the cooldown display
     */
    public static void render(GuiGraphics graphics, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.font == null) return;

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int y = DISPLAY_Y;

        List<Map.Entry<String, CooldownEntry>> activeCooldowns = new ArrayList<>();

        // Update and collect active cooldowns
        long currentTime = System.currentTimeMillis();
        cooldowns.entrySet().removeIf(entry -> {
            CooldownEntry cooldown = entry.getValue();
            if (currentTime >= cooldown.endTime) {
                return true; // Remove expired cooldowns
            }
            activeCooldowns.add(entry);
            return false;
        });

        // Sort by remaining time
        activeCooldowns.sort((a, b) -> Long.compare(a.getValue().endTime, b.getValue().endTime));

        // Render each cooldown with dynamic width
        for (Map.Entry<String, CooldownEntry> entry : activeCooldowns) {
            // Calculate the width needed for this entry
            String name = entry.getKey();
            CooldownEntry cooldown = entry.getValue();
            long remaining = cooldown.endTime - currentTime;
            String timeText = String.format("%.1fs", remaining / 1000.0);

            // Calculate required width
            int nameWidth = minecraft.font.width(name);
            int timeWidth = minecraft.font.width(timeText);
            int totalWidth = PADDING + nameWidth + TIME_SPACING + timeWidth + PADDING;
            int entryWidth = Math.max(totalWidth, MIN_ENTRY_WIDTH);

            // Position from right edge
            int x = screenWidth - entryWidth - DISPLAY_X;

            renderCooldownEntry(graphics, x, y, entryWidth, entry.getKey(), entry.getValue(), currentTime);
            y += ENTRY_HEIGHT + 2;
        }
    }

    private static void renderCooldownEntry(GuiGraphics graphics, int x, int y, int width, String name, CooldownEntry cooldown, long currentTime) {
        long remaining = cooldown.endTime - currentTime;
        float progress = 1.0f - (float)remaining / (float)cooldown.duration;
        progress = Mth.clamp(progress, 0.0f, 1.0f);

        // Background
        graphics.fill(x, y, x + width, y + ENTRY_HEIGHT, 0x80000000);

        // Progress bar
        int progressWidth = (int)(width * progress);
        int color = interpolateColor(0xFF0000, 0x00FF00, progress); // Red to green
        graphics.fill(x, y, x + progressWidth, y + ENTRY_HEIGHT, color | 0x80000000);

        // Border
        graphics.renderOutline(x, y, width, ENTRY_HEIGHT, 0xFFFFFFFF);

        // Text - translated down by 2 pixels from the original y + 2
        String timeText = String.format("%.1fs", remaining / 1000.0);

        // Save the current matrix state
        graphics.pose().pushPose();

        // Scale down the text to 85% size (slightly larger than before)
        float scale = 0.85f;
        graphics.pose().scale(scale, scale, 1.0f);

        // Calculate scaled positions
        float scaledX = (x + PADDING) / scale;
        float scaledY = (y + 7) / scale; // Moved down from y + 6 to y + 7

        // Draw move name
        graphics.drawString(Minecraft.getInstance().font, name,
                (int)scaledX, (int)scaledY, 0xFFFFFFFF, true);

        // Draw time (right-aligned)
        int timeWidth = Minecraft.getInstance().font.width(timeText);
        float scaledRightX = (x + width - PADDING) / scale;
        graphics.drawString(Minecraft.getInstance().font, timeText,
                (int)(scaledRightX - timeWidth), (int)scaledY, 0xFFFFFFFF, true);

        // Restore the matrix state
        graphics.pose().popPose();
    }

    private static int interpolateColor(int startColor, int endColor, float progress) {
        int r1 = (startColor >> 16) & 0xFF;
        int g1 = (startColor >> 8) & 0xFF;
        int b1 = startColor & 0xFF;

        int r2 = (endColor >> 16) & 0xFF;
        int g2 = (endColor >> 8) & 0xFF;
        int b2 = endColor & 0xFF;

        int r = (int)(r1 + (r2 - r1) * progress);
        int g = (int)(g1 + (g2 - g1) * progress);
        int b = (int)(b1 + (b2 - b1) * progress);

        return (r << 16) | (g << 8) | b;
    }

    /**
     * Adds or updates a cooldown - prevents duplicate entries
     */
    public static void setCooldown(String name, int durationTicks) {
        // Don't set cooldown if duration is 0 or negative
        if (durationTicks <= 0) {
            cooldowns.remove(name); // Remove any existing cooldown
            return;
        }

        long durationMillis = durationTicks * 50L; // Convert ticks to milliseconds
        cooldowns.put(name, new CooldownEntry(System.currentTimeMillis() + durationMillis, durationMillis));
    }

    /**
     * Checks if a cooldown is active
     */
    public static boolean isOnCooldown(String name) {
        CooldownEntry entry = cooldowns.get(name);
        return entry != null && System.currentTimeMillis() < entry.endTime;
    }

    /**
     * Gets remaining cooldown time in ticks
     */
    public static int getRemainingCooldown(String name) {
        CooldownEntry entry = cooldowns.get(name);
        if (entry == null) return 0;

        long remaining = entry.endTime - System.currentTimeMillis();
        return remaining > 0 ? (int)(remaining / 50L) : 0;
    }

    private static class CooldownEntry {
        final long endTime;
        final long duration;

        CooldownEntry(long endTime, long duration) {
            this.endTime = endTime;
            this.duration = duration;
        }
    }
}