package com.xirc.nichirin.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.system.DemonComponent;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {

    @Unique
    private static final ResourceLocation BLOOD_FULL = ResourceLocation.fromNamespaceAndPath("nichirin", "textures/gui/blood_full.png");
    @Unique
    private static final ResourceLocation BLOOD_HALF = ResourceLocation.fromNamespaceAndPath("nichirin", "textures/gui/blood_half.png");
    @Unique
    private static final ResourceLocation BLOOD_EMPTY = ResourceLocation.fromNamespaceAndPath("nichirin", "textures/gui/blood_empty.png");

    @Unique
    private static final int BLOOD_BAR_WIDTH = 9;
    @Unique
    private static final int BLOOD_BAR_HEIGHT = 9;

    /**
     * Replaces the hunger bar with the blood bar for demon players.
     */
    @Inject(method = "renderFood", at = @At("HEAD"), cancellable = true)
    private void nichirin$renderBloodInsteadOfFood(GuiGraphics graphics, Player player, int y, int right,
                                                   CallbackInfo ci) {
        if (!MovesetHelper.hasDemonMoveset(player) || player.isCreative() || player.isSpectator()) {
            return;
        }

        RenderSystem.enableBlend();
        for (int i = 0; i < 10; i++) {
            int x = right - i * 8 - BLOOD_BAR_WIDTH;
            ResourceLocation texture = nichirin$getBloodTextureForSegment(i);
            graphics.blit(texture, x, y, 0, 0, BLOOD_BAR_WIDTH, BLOOD_BAR_HEIGHT,
                    BLOOD_BAR_WIDTH, BLOOD_BAR_HEIGHT);
        }
        RenderSystem.disableBlend();
        ci.cancel();
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