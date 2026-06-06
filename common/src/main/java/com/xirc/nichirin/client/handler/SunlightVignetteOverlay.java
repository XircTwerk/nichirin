package com.xirc.nichirin.client.handler;

import com.mojang.blaze3d.systems.RenderSystem;
import com.xirc.nichirin.common.data.MovesetHelper;
import dev.architectury.event.events.client.ClientGuiEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public final class SunlightVignetteOverlay {

    private static final int CHECK_RADIUS = 5;
    private static final int BANDS = 8;
    private static final int SUNLIGHT_RGB = 0xFFD84A;

    private SunlightVignetteOverlay() {
    }

    public static void register() {
        ClientGuiEvent.RENDER_HUD.register((graphics, partialTicks) -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null || minecraft.player == null || minecraft.options.hideGui) return;
            if (!MovesetHelper.hasDemonMoveset(minecraft.player)) return;

            float intensity = sunlightIntensity(minecraft);
            if (intensity <= 0.01F) return;

            int width = minecraft.getWindow().getGuiScaledWidth();
            int height = minecraft.getWindow().getGuiScaledHeight();
            int maxThickness = Math.max(18, Math.min(width, height) / 5);

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            for (int i = 0; i < BANDS; i++) {
                float band = 1.0F - (i / (float) BANDS);
                int alpha = Math.round(72.0F * intensity * band * band);
                if (alpha <= 0) continue;

                int color = (alpha << 24) | SUNLIGHT_RGB;
                int outer = i * maxThickness / BANDS;
                int inner = (i + 1) * maxThickness / BANDS;

                graphics.fill(outer, outer, width - outer, inner, color);
                graphics.fill(outer, height - inner, width - outer, height - outer, color);
                graphics.fill(outer, inner, inner, height - inner, color);
                graphics.fill(width - inner, inner, width - outer, height - inner, color);
            }
            RenderSystem.disableBlend();
        });
    }

    private static float sunlightIntensity(Minecraft minecraft) {
        if (!minecraft.level.isDay() || minecraft.level.isRaining() || minecraft.level.isThundering()) {
            return 0.0F;
        }

        BlockPos origin = minecraft.player.blockPosition();
        if (minecraft.level.canSeeSky(origin)) {
            return 1.0F;
        }

        float strongest = 0.0F;
        for (int dx = -CHECK_RADIUS; dx <= CHECK_RADIUS; dx++) {
            for (int dz = -CHECK_RADIUS; dz <= CHECK_RADIUS; dz++) {
                if (dx == 0 && dz == 0) continue;
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > CHECK_RADIUS) continue;

                BlockPos sample = origin.offset(dx, 0, dz);
                if (minecraft.level.canSeeSky(sample)) {
                    float proximity = 1.0F - (float) (distance / CHECK_RADIUS);
                    strongest = Math.max(strongest, proximity * 0.75F);
                }
            }
        }

        return strongest;
    }
}
