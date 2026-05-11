package com.xirc.nichirin.client.handler;

import com.xirc.nichirin.client.BreathOfNichirinClient;
import com.xirc.nichirin.client.config.NichirinClientConfig;
import com.xirc.nichirin.client.particle.BreathingAuraWispParticleProvider;
import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.registry.NichirinParticleRegistry;
import dev.architectury.event.events.client.ClientTickEvent;

public class BreathingAuraWispHandler {

    // Track last game time to avoid spawning many particles when gameTime is frozen (paused SP)
    private static long lastSpawnGameTime = -20L;

    public static void register() {
        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            if (minecraft.level == null || minecraft.player == null) return;
            if (minecraft.player.isSpectator() || !minecraft.player.isAlive()) return;

            // Spawn once every 20 game ticks. Use a tracked variable instead of % 20 so
            // a frozen gameTime (singleplayer pause) doesn't fire particles on every frame.
            long gameTime = minecraft.level.getGameTime();
            if (gameTime - lastSpawnGameTime < 20) return;
            lastSpawnGameTime = gameTime;

            if (!NichirinClientConfig.get().visual.enableBreathingAuraParticles) return;
            if (MovesetHelper.getBreathingMoveset(minecraft.player) == null) return;

            String styleId = PlayerDataProvider.getMovesetData(minecraft.player).getBreathingMovesetId();
            float[] color = BreathOfNichirinClient.getBreathingStyleColor(styleId);

            net.minecraft.util.RandomSource rand = minecraft.level.random;

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
            double py = minecraft.player.getEyeY() - 0.22 + (rand.nextDouble() - 0.5) * 0.06;
            double baseX = minecraft.player.getX() + fwdX * forward;
            double baseZ = minecraft.player.getZ() + fwdZ * forward;

            // Outward lateral drift: bias each nostril particle away from center
            double driftMag = 0.004;

            // Left nostril — drifts in the +right direction
            BreathingAuraWispParticleProvider.pendingLateralX = rightX * driftMag;
            BreathingAuraWispParticleProvider.pendingLateralZ = rightZ * driftMag;
            minecraft.level.addParticle(NichirinParticleRegistry.BREATHING_AURA_WISP.get(),
                    baseX + rightX * spread, py, baseZ + rightZ * spread,
                    color[0], color[1], color[2]);

            // Right nostril — drifts in the -right direction
            BreathingAuraWispParticleProvider.pendingLateralX = -rightX * driftMag;
            BreathingAuraWispParticleProvider.pendingLateralZ = -rightZ * driftMag;
            minecraft.level.addParticle(NichirinParticleRegistry.BREATHING_AURA_WISP.get(),
                    baseX - rightX * spread, py, baseZ - rightZ * spread,
                    color[0], color[1], color[2]);
        });
    }
}
