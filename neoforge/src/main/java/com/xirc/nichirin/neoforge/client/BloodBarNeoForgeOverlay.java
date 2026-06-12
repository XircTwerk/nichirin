package com.xirc.nichirin.neoforge.client;

import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.system.DemonComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * NeoForge-only blood bar.
 *
 * <p>On NeoForge the hunger bar is drawn by its own FOOD_LEVEL GUI layer rather than
 * vanilla {@code Gui.renderPlayerHealth}, so the common {@code GuiMixin} @Redirect never fires —
 * demons saw a normal food bar. Here we cancel the vanilla food overlay for demon players and
 * draw the blood bar in its place, mirroring the GuiMixin segment logic.</p>
 */
@EventBusSubscriber(modid = "nichirin", bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class BloodBarNeoForgeOverlay {

    private static final ResourceLocation BLOOD_FULL  = ResourceLocation.fromNamespaceAndPath("nichirin", "textures/gui/blood_full.png");
    private static final ResourceLocation BLOOD_HALF  = ResourceLocation.fromNamespaceAndPath("nichirin", "textures/gui/blood_half.png");
    private static final ResourceLocation BLOOD_EMPTY = ResourceLocation.fromNamespaceAndPath("nichirin", "textures/gui/blood_empty.png");

    private static final int ICON = 9;

    private BloodBarNeoForgeOverlay() {}

    @SubscribeEvent
    public static void onRenderFood(RenderGuiLayerEvent.Pre event) {
        if (!event.getName().equals(VanillaGuiLayers.FOOD_LEVEL)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        Player player = mc.player;
        if (player == null) return;
        if (player.isCreative() || player.isSpectator()) return;
        if (!MovesetHelper.hasDemonMoveset(player)) return;

        // Suppress the vanilla hunger bar and draw blood instead.
        event.setCanceled(true);

        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth  = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Same row/position as the vanilla hunger bar (right of centre).
        int right = screenWidth / 2 + 91;
        int y = screenHeight - 39;

        for (int i = 0; i < 10; i++) {
            int x = right - i * 8 - ICON;
            ResourceLocation tex = textureForSegment(i);
            graphics.blit(tex, x, y, 0, 0, ICON, ICON, ICON, ICON);
        }
    }

    /** Mirrors GuiMixin#nichirin$getBloodTextureForSegment. Segment 0 = rightmost = blood point 1. */
    private static ResourceLocation textureForSegment(int segmentIndex) {
        int fullBloodPoints = DemonComponent.getClientBloodPoints();
        int halfBloodPoints = Math.max(0, Math.min(DemonComponent.getClientHalfBloodPoints(), 1));
        double actualBlood = fullBloodPoints - (halfBloodPoints * 0.5);

        if (actualBlood <= 0) return BLOOD_EMPTY;

        double segmentBloodValue = segmentIndex + 1;
        if (actualBlood >= segmentBloodValue)        return BLOOD_FULL;
        if (actualBlood >= segmentBloodValue - 0.5)  return BLOOD_HALF;
        return BLOOD_EMPTY;
    }
}