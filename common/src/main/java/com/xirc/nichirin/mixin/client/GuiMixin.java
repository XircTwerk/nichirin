package com.xirc.nichirin.mixin.client;

import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.system.DemonComponent;
import com.xirc.nichirin.common.event.DemonFoodHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Gui.class)
public class GuiMixin {

    @Unique
    private static final ResourceLocation BLOOD_FULL = new ResourceLocation("nichirin", "textures/gui/blood_full.png");
    @Unique
    private static final ResourceLocation BLOOD_HALF = new ResourceLocation("nichirin", "textures/gui/blood_half.png");
    @Unique
    private static final ResourceLocation BLOOD_EMPTY = new ResourceLocation("nichirin", "textures/gui/blood_empty.png");

    @Unique
    private static final int BLOOD_BAR_WIDTH = 9;
    @Unique
    private static final int BLOOD_BAR_HEIGHT = 9;

    /**
     * Intercepts hunger bar rendering and replaces with blood bar for demons
     * Uses coordinate-based segment detection to maintain the original segment logic
     */
    @Redirect(method = "renderPlayerHealth",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"))
    private void nichirin$replaceHungerWithBlood(GuiGraphics graphics, ResourceLocation texture,
                                                 int x, int y, int u, int v, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        // Check if this is a hunger bar blit for a demon player
        boolean isDemon = player != null && MovesetHelper.hasDemonMoveset(player);
        boolean isHungerBarBlit = v == 27; // Food icons are at v=27 in GUI_ICONS_LOCATION
        boolean isCreativeOrSpectator = player != null && (player.isCreative() || player.isSpectator());

        if (isDemon && isHungerBarBlit && !isCreativeOrSpectator) {
            // Calculate which segment this is based on x coordinate
            int segmentIndex = nichirin$calculateSegmentIndex(x, minecraft.getWindow().getGuiScaledWidth());

            if (segmentIndex >= 0) {
                // Get the appropriate blood texture for this segment
                ResourceLocation bloodTexture = nichirin$getBloodTextureForSegment(segmentIndex);

                // Render blood icon instead of hunger icon
                graphics.blit(bloodTexture, x, y, 0, 0, width, height,
                        BLOOD_BAR_WIDTH, BLOOD_BAR_HEIGHT);
                return;
            }
        }

        // Render normal GUI element (not demon hunger bar)
        graphics.blit(texture, x, y, u, v, width, height);
    }

    /**
     * Calculates which blood segment is being rendered based on x coordinate
     * Mirrors the positioning logic from the original DemonBloodGui
     */
    @Unique
    private int nichirin$calculateSegmentIndex(int x, int screenWidth) {
        // Calculate hunger bar position (same as original code)
        int hungerBarLeft = screenWidth / 2 + 91;

        // Each segment is 8 pixels apart, starting from the right
        // Segment 0 = rightmost, segment 9 = leftmost
        for (int i = 0; i < 10; i++) {
            int segmentX = hungerBarLeft - i * 8 - 9;
            if (x == segmentX) {
                return i;
            }
        }

        return -1; // Not a blood segment
    }

    /**
     * Determines the correct blood texture for a segment
     * Replicates the exact logic from DemonBloodGui.getTextureForSegment()
     */
    @Unique
    private ResourceLocation nichirin$getBloodTextureForSegment(int segmentIndex) {
        // Get current blood state from the component system
        int fullBloodPoints = DemonComponent.getClientBloodPoints();
        int halfBloodPoints = DemonComponent.getClientHalfBloodPoints();

        // Clamp half blood points to valid range (0 or 1)
        halfBloodPoints = Math.max(0, Math.min(halfBloodPoints, 1));

        // Calculate actual blood remaining (half-blood represents damage taken)
        double actualBlood = fullBloodPoints - (halfBloodPoints * 0.5);

        // Handle edge case: if actualBlood is 0 or negative, everything should be empty
        if (actualBlood <= 0) {
            return BLOOD_EMPTY;
        }

        // Segments render right to left: index 0 = rightmost = blood point 1
        // index 9 = leftmost = blood point 10
        double segmentBloodValue = segmentIndex + 1;

        if (actualBlood >= segmentBloodValue) {
            // This segment should be completely full
            return BLOOD_FULL;
        } else if (actualBlood >= segmentBloodValue - 0.5) {
            // This segment should be half full
            return BLOOD_HALF;
        } else {
            // This segment should be empty
            return BLOOD_EMPTY;
        }
    }
}