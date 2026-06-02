package com.xirc.nichirin.client.handler;

import com.xirc.nichirin.client.BreathOfNichirinClient;
import com.xirc.nichirin.client.config.NichirinClientConfig;
import com.xirc.nichirin.client.particle.BreathingAuraWispParticleProvider;
import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.registry.NichirinParticleRegistry;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.util.RandomSource;

public class BreathingAuraWispHandler {

    // Track last game time to avoid spawning many particles when gameTime is frozen (paused SP)
    private static long lastSpawnGameTime = -20L;

    public static void register() {
        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            if (minecraft.level == null || minecraft.player == null) return;
            if (minecraft.player.isSpectator() || !minecraft.player.isAlive()) return;

            // Spawn once every 40 game ticks (2 seconds). Particles follow the player face while alive.
            long gameTime = minecraft.level.getGameTime();
            if (gameTime - lastSpawnGameTime < 40) return;
            lastSpawnGameTime = gameTime;

            if (!NichirinClientConfig.get().visual.enableBreathingAuraParticles) return;
            if (MovesetHelper.getBreathingMoveset(minecraft.player) == null) return;

            String styleId = PlayerDataProvider.getMovesetData(minecraft.player).getBreathingMovesetId();
            float[] color = BreathOfNichirinClient.getBreathingStyleColor(styleId);

            RandomSource rand = minecraft.level.random;

            // Horizontal forward and right vectors derived from player yaw
            float yaw = minecraft.player.getYRot() * ((float) Math.PI / 180f);
            // forward: (-sin, 0, cos) — right: (-cos, 0, -sin) in Minecraft's coord system
            double fwdX = -Math.sin(yaw);
            double fwdZ =  Math.cos(yaw);
            double rightX = -Math.cos(yaw);
            double rightZ = -Math.sin(yaw);

            // Place particles at the nose/mouth — forward from center and slightly below eyes
            double forward = 0.28;
            double spread  = 0.10 + rand.nextDouble() * 0.03;
            double py = minecraft.player.getEyeY() - 0.10 + (rand.nextDouble() - 0.5) * 0.06;
            double baseX = minecraft.player.getX() + fwdX * forward;
            double baseZ = minecraft.player.getZ() + fwdZ * forward;

            // Outward lateral drift: bias each nostril particle away from center
            double driftMag = 0.004;

            // Right nostril — drifts right, unmirrored
            BreathingAuraWispParticleProvider.pendingLateralX = rightX * driftMag;
            BreathingAuraWispParticleProvider.pendingLateralZ = rightZ * driftMag;
            BreathingAuraWispParticleProvider.pendingMirrored = false;
            BreathingAuraWispParticleProvider.pendingOffsetX = rightX * spread;
            BreathingAuraWispParticleProvider.pendingOffsetZ = rightZ * spread;
            minecraft.level.addParticle(NichirinParticleRegistry.BREATHING_AURA_WISP.get(),
                    baseX + rightX * spread, py, baseZ + rightZ * spread,
                    color[0], color[1], color[2]);

            // Left nostril — drifts left, horizontally mirrored
            BreathingAuraWispParticleProvider.pendingLateralX = -rightX * driftMag;
            BreathingAuraWispParticleProvider.pendingLateralZ = -rightZ * driftMag;
            BreathingAuraWispParticleProvider.pendingMirrored = true;
            BreathingAuraWispParticleProvider.pendingOffsetX = -rightX * spread;
            BreathingAuraWispParticleProvider.pendingOffsetZ = -rightZ * spread;
            minecraft.level.addParticle(NichirinParticleRegistry.BREATHING_AURA_WISP.get(),
                    baseX - rightX * spread, py, baseZ - rightZ * spread,
                    color[0], color[1], color[2]);
        });
    }
}