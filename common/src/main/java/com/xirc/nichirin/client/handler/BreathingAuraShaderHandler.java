package com.xirc.nichirin.client.handler;

import com.xirc.nichirin.client.BreathOfNichirinClient;
import com.xirc.nichirin.client.shader.FlameBreathingAuraShader;
import com.xirc.nichirin.client.shader.NichirinShaderManager;
import com.xirc.nichirin.client.shader.WaterBreathingAuraShader;
import com.xirc.nichirin.common.data.MovesetData;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.common.util.BreathingManager;
import dev.architectury.event.events.client.ClientTickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Drives the passive breathing-style aura shaders.
 *
 * Each tick it:
 *   1. Reads the local player's active breathing moveset ID.
 *   2. Reads their current breath percentage via BreathingManager.
 *   3. Activates / deactivates the correct NichirinPostProcessor subclass.
 *   4. Pushes the intensity value so the GLSL uniforms update.
 *
 * Registration: call BreathingAuraHandler.register() from BreathOfNichirinClient.init().
 */
@Environment(EnvType.CLIENT)
public class BreathingAuraShaderHandler {

    private static FlameBreathingAuraShader flameShader;
    private static WaterBreathingAuraShader waterShader;

    /** Which style is currently active (null = none). */
    private static String activeStyleId = null;

    public static void register() {
        // Create shader instances and register them with the manager
        flameShader = new FlameBreathingAuraShader();
        waterShader = new WaterBreathingAuraShader();

        NichirinShaderManager manager = NichirinShaderManager.getInstance();
        manager.register(flameShader);
        manager.register(waterShader);

        // Tick every client frame
        ClientTickEvent.CLIENT_POST.register(BreathingAuraShaderHandler::tick);
    }

    private static void tick(Minecraft minecraft) {
        if (!BreathOfNichirinClient.isClientReady()) return;

        LocalPlayer player = minecraft.player;
        if (player == null) return;

        // --- Determine active breathing style ---
        String styleId = null;
        try {
            MovesetData movesetData = PlayerDataProvider.getData(player).getMovesetData();
            if (movesetData != null && movesetData.hasBreathingMoveset()) {
                styleId = movesetData.getBreathingMovesetId();
            }
        } catch (Exception ignored) {
            // PlayerDataProvider may not be ready on first few ticks
        }

        // --- Read breath percentage ---
        float breathPct = BreathingManager.getBreathingPercentage(player);

        // --- Route to the correct shader ---
        updateShaders(styleId, breathPct);
    }

    private static void updateShaders(String styleId, float breathPct) {
        // Determine which shader should be on
        boolean wantFlame = "flame_breathing".equals(styleId);
        boolean wantWater = "water_breathing".equals(styleId);

        // Flame
        if (wantFlame) {
            if (!flameShader.isActive()) flameShader.setActive(true);
            flameShader.setBreathIntensity(breathPct);
        } else {
            if (flameShader.isActive()) flameShader.setActive(false);
        }

        // Water
        if (wantWater) {
            if (!waterShader.isActive()) waterShader.setActive(true);
            waterShader.setBreathIntensity(breathPct);
        } else {
            if (waterShader.isActive()) waterShader.setActive(false);
        }

        activeStyleId = styleId;
    }

    /** Expose shaders if other systems need direct access (e.g., resource reload). */
    public static FlameBreathingAuraShader getFlameShader() { return flameShader; }
    public static WaterBreathingAuraShader getWaterShader() { return waterShader; }
}
