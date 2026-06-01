package com.xirc.nichirin.client;

import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.system.DemonComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge-only blood bar.
 *
 * <p>On Forge the hunger bar is drawn by {@code ForgeGui}'s own FOOD_LEVEL overlay rather than
 * vanilla {@code Gui.renderPlayerHealth}, so the common {@code GuiMixin} @Redirect never fires —
 * demons saw a normal food bar. Here we cancel the vanilla food overlay for demon players and
 * draw the blood bar in its place, mirroring the GuiMixin segment logic.</p>
 */
@Mod.EventBusSubscriber(modid = "nichirin", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class BloodBarForgeOverlay {

    private static final ResourceLocation BLOOD_FULL  = new ResourceLocation("nichirin", "textures/gui/blood_full.png");
    private static final ResourceLocation BLOOD_HALF  = new ResourceLocation("nichirin", "textures/gui/blood_half.png");
    private static final ResourceLocation BLOOD_EMPTY = new ResourceLocation("nichirin", "textures/gui/blood_empty.png");

    private static final int ICON = 9;

    private BloodBarForgeOverlay() {}

    @SubscribeEvent
    public static void onRenderFood(RenderGuiOverlayEvent.Pre event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.FOOD_LEVEL.id())) return;

        Minecraft mc = Minecraft.getInstance();
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
