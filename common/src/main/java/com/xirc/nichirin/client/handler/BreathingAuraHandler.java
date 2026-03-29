package com.xirc.nichirin.client.handler;

import com.xirc.nichirin.client.BreathOfNichirinClient;
import com.xirc.nichirin.client.config.NichirinClientConfig;
import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.registry.NichirinParticleRegistry;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;

public class BreathingAuraHandler {

    public static void register() {
        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            if (minecraft.level == null) return;
            if (minecraft.level.getGameTime() % 20 != 0) return;
            if (minecraft.player == null || !minecraft.player.isAlive() || minecraft.player.isSpectator()) return;

            if (!NichirinClientConfig.get().visual.enableBreathingAuraParticles) return;
            if (MovesetHelper.getBreathingMoveset(minecraft.player) == null) return;

            String styleId = PlayerDataProvider.getMovesetData(minecraft.player).getBreathingMovesetId();
            float[] color = BreathOfNichirinClient.getBreathingStyleColor(styleId);

            net.minecraft.util.RandomSource rand = minecraft.level.random;

            // Player facing direction — used to mirror left/right
            float yaw = minecraft.player.getYRot() * ((float) Math.PI / 180f);
            double rightX =  Math.cos(yaw);
            double rightZ = -Math.sin(yaw);

            // Lateral spread — wide enough to visually separate left/right wisps
            double spread = 0.15 + rand.nextDouble() * 0.04;
            double py = minecraft.player.getEyeY() - 0.18 + (rand.nextDouble() - 0.5) * 0.08;

            // Left wisp
            double lx = minecraft.player.getX() - rightX * spread;
            double lz = minecraft.player.getZ() - rightZ * spread;
            minecraft.level.addParticle(NichirinParticleRegistry.BREATHING_AURA_WISP.get(),
                    lx, py, lz, color[0], color[1], color[2]);

            // Right wisp (mirrored)
            double rx = minecraft.player.getX() + rightX * spread;
            double rz = minecraft.player.getZ() + rightZ * spread;
            minecraft.level.addParticle(NichirinParticleRegistry.BREATHING_AURA_WISP.get(),
                    rx, py, rz, color[0], color[1], color[2]);
        });
    }
}
