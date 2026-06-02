package com.xirc.nichirin.client.handler;

import com.xirc.nichirin.client.BreathOfNichirinClient;
import com.xirc.nichirin.client.gui.BreathingBarHUD;
import com.xirc.nichirin.client.shader.FlameBreathingAuraShader;
import com.xirc.nichirin.client.shader.NichirinShaderManager;
import com.xirc.nichirin.client.shader.WaterBreathingAuraShader;
import com.xirc.nichirin.common.data.MovesetData;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import dev.architectury.event.events.client.ClientTickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Drives the passive breathing-style aura shaders.
 *
 * Each tick it reads the local player's active breathing moveset ID, computes an
 * intensity from their synced breath level, then activates or deactivates the
 * matching NichirinPostProcessor so the GLSL uniforms stay current.
 *
 * Intensity is strongest when breath is depleted (high breathing cost) and fades
 * to a minimum glow when breath is full, so the aura reflects how hard the player
 * is pushing their technique.
 *
 * Registration: call BreathingAuraShaderHandler.register() from BreathOfNichirinClient.init().
 */
@Environment(EnvType.CLIENT)
public class BreathingAuraShaderHandler {

    private static FlameBreathingAuraShader flameShader;
    private static WaterBreathingAuraShader waterShader;

    /** Which style is currently active (null = none). */
    private static String activeStyleId = null;

    public static void register() {
        NichirinShaderManager manager = NichirinShaderManager.getInstance();

        flameShader = manager.getProcessor(FlameBreathingAuraShader.class);
        waterShader = manager.getProcessor(WaterBreathingAuraShader.class);

        if (flameShader == null) {
            flameShader = new FlameBreathingAuraShader();
            manager.register(flameShader);
        }
        if (waterShader == null) {
            waterShader = new WaterBreathingAuraShader();
            manager.register(waterShader);
        }

        ClientTickEvent.CLIENT_POST.register(BreathingAuraShaderHandler::tick);
    }

    private static void tick(Minecraft minecraft) {
        if (!BreathOfNichirinClient.isClientReady()) return;

        LocalPlayer player = minecraft.player;
        if (player == null) return;

        String styleId = null;
        try {
            MovesetData movesetData = PlayerDataProvider.getData(player).getMovesetData();
            if (movesetData != null && movesetData.hasBreathingMoveset()) {
                styleId = movesetData.getBreathingMovesetId();
            }
        } catch (Exception ignored) {
            // PlayerDataProvider may not be ready on the first few ticks
        }

        // BreathingBarHUD holds the client-synced breath value from SyncBreathPacket.
        // BreathingManager's data is server-side only and always returns 0 on the client.
        float breathPct = BreathingBarHUD.getBreathingPercentage();

        // Intensity is strongest when breath is depleted: base glow (0.3) plus up to
        // 0.7 more as breath drops, so the aura reflects the actual breathing cost.
        float intensity = styleId != null ? 0.3f + (1.0f - breathPct) * 0.7f : 0f;

        updateShaders(styleId, intensity);
    }

    private static void updateShaders(String styleId, float intensity) {
        boolean wantFlame = "flame_breathing".equals(styleId);
        boolean wantWater = "water_breathing".equals(styleId);

        if (wantFlame) {
            if (!flameShader.isActive()) flameShader.setActive(true);
            flameShader.setBreathIntensity(intensity);
        } else {
            if (flameShader.isActive()) flameShader.setActive(false);
        }

        if (wantWater) {
            if (!waterShader.isActive()) waterShader.setActive(true);
            waterShader.setBreathIntensity(intensity);
        } else {
            if (waterShader.isActive()) waterShader.setActive(false);
        }

        activeStyleId = styleId;
    }

    /** Expose shaders if other systems need direct access (e.g., resource reload). */
    public static FlameBreathingAuraShader getFlameShader() { return flameShader; }
    public static WaterBreathingAuraShader getWaterShader() { return waterShader; }
}