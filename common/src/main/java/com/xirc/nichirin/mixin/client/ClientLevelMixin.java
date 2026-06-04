package com.xirc.nichirin.mixin.client;

import com.xirc.nichirin.registry.NichirinBiomeRegistry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {
    private static final int WISTERIA_DAY_SKY = 0xB9A7FF;
    private static final int WISTERIA_DAY_SKY_ACCENT = 0xA9D8FF;
    private static final int WISTERIA_NIGHT_SKY = 0x1A082D;

    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void nichirin$wisteriaSkyColor(Vec3 cameraPos, float partialTick, CallbackInfoReturnable<Vec3> cir) {
        ClientLevel level = (ClientLevel) (Object) this;
        if (!level.getBiome(BlockPos.containing(cameraPos)).is(NichirinBiomeRegistry.WISTERIA_GROVE)) {
            return;
        }

        float dayAmount = getDayAmount(level.getTimeOfDay(partialTick));
        int dayColor = blendRgb(WISTERIA_DAY_SKY, WISTERIA_DAY_SKY_ACCENT, 0.35F);
        cir.setReturnValue(Vec3.fromRGB24(blendRgb(WISTERIA_NIGHT_SKY, dayColor, dayAmount)));
    }

    private static float getDayAmount(float timeOfDay) {
        float brightness = (float) Math.cos(timeOfDay * Math.PI * 2.0F);
        return clamp((brightness * 2.0F) + 0.5F);
    }

    private static int blendRgb(int from, int to, float amount) {
        amount = clamp(amount);
        int r = Math.round(channel(from, 16) + (channel(to, 16) - channel(from, 16)) * amount);
        int g = Math.round(channel(from, 8) + (channel(to, 8) - channel(from, 8)) * amount);
        int b = Math.round(channel(from, 0) + (channel(to, 0) - channel(from, 0)) * amount);
        return (r << 16) | (g << 8) | b;
    }

    private static int channel(int color, int shift) {
        return (color >> shift) & 0xFF;
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
