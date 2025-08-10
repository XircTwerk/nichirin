package com.xirc.nichirin.client.renderer.shaders;

import com.xirc.nichirin.registry.NichirinEffectRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

/**
 * Renders the Musical Score overlay effect when the effect is active
 * Creates a bone-colored overlay with musical staff lines
 */
@Environment(EnvType.CLIENT)
public class MusicalScoreOverlay {

    private static final int BONE_COLOR = 0xF5DEB3; // Bone color from effect
    private static final int STAFF_LINE_COLOR = 0x8B4513; // Saddle brown for staff lines

    /**
     * Render the Musical Score overlay if the player has the effect
     */
    public static void render(GuiGraphics graphics, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null) return;

        // Check if player has Musical Score effect
        if (!player.hasEffect(NichirinEffectRegistry.MUSICAL_SCORE.get())) {
            return;
        }

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        long gameTime = player.level().getGameTime();
        float effectTime = gameTime + partialTicks;

        // Render bone-colored overlay
        renderBoneOverlay(graphics, screenWidth, screenHeight, effectTime);

        // Render musical staff lines
        renderStaffLines(graphics, screenWidth, screenHeight, effectTime);

        // Render floating musical notes
        renderMusicalNotes(graphics, screenWidth, screenHeight, effectTime);
    }

    /**
     * Render the bone-colored background overlay
     */
    private static void renderBoneOverlay(GuiGraphics graphics, int width, int height, float time) {
        // Create subtle pulsing alpha effect (much more gentle)
        float pulse = 0.25f + 0.05f * Mth.sin(time * 0.05f); // Slower, subtler pulse
        int alpha = (int)(pulse * 255) << 24;
        int color = alpha | (BONE_COLOR & 0xFFFFFF);

        // Fill entire screen with translucent bone color
        graphics.fill(0, 0, width, height, color);
    }

    /**
     * Render musical staff lines across the screen
     */
    private static void renderStaffLines(GuiGraphics graphics, int width, int height, float time) {
        // Draw 5 horizontal staff lines
        int staffSpacing = height / 8; // Space between staff lines
        int lineThickness = 2;

        for (int i = 0; i < 5; i++) {
            int y = height / 4 + i * (staffSpacing / 5);

            // Add slight wave effect to make lines feel alive
            for (int x = 0; x < width; x += 4) {
                int waveY = y + (int)(2 * Mth.sin((time * 0.02f) + (x * 0.01f)));

                // Draw line segments
                graphics.fill(x, waveY, x + 3, waveY + lineThickness,
                        0x80000000 | (STAFF_LINE_COLOR & 0xFFFFFF));
            }
        }
    }

    /**
     * Render floating musical notes
     */
    private static void renderMusicalNotes(GuiGraphics graphics, int width, int height, float time) {
        // Draw 6 floating musical notes with smoother movement
        for (int i = 0; i < 6; i++) {
            float phase = time * 0.02f + i * 1.047f; // Slower movement

            // Calculate floating position with smoother motion
            float x = (width * 0.2f) + (i * width * 0.12f) + 20 * Mth.sin(phase); // Reduced movement range
            float y = (height * 0.3f) + 30 * Mth.cos(phase + i); // Reduced movement range

            // Smoother scale and fade notes
            float scale = 0.9f + 0.1f * Mth.sin(time * 0.03f + i); // Subtle scaling
            int alpha = (int)(160 + 30 * Mth.sin(time * 0.04f + i)); // Less alpha variation

            renderMusicalNote(graphics, (int)x, (int)y, scale, alpha);
        }
    }

    /**
     * Render a single musical note symbol
     */
    private static void renderMusicalNote(GuiGraphics graphics, int x, int y, float scale, int alpha) {
        int color = (alpha << 24) | 0x2F4F4F; // Dark slate gray
        int size = (int)(12 * scale);

        // Draw note head (filled circle)
        graphics.fill(x, y, x + size, y + (size / 2), color);

        // Draw note stem (vertical line)
        graphics.fill(x + size - 2, y - size, x + size, y + (size / 2), color);

        // Draw note flag (curved line at top)
        for (int i = 0; i < 3; i++) {
            graphics.fill(x + size, y - size + i * 2, x + size + 6, y - size + i * 2 + 1, color);
        }
    }
}