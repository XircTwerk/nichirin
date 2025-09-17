package com.xirc.nichirin.common.network.s2c;

import com.xirc.nichirin.client.animation.NichirinAnimations;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class PlayerAnimationPacket {

    private final int playerId;
    private final String animationName;

    public PlayerAnimationPacket(int playerId, String animationName) {
        this.playerId = playerId;
        this.animationName = animationName;
    }

    public PlayerAnimationPacket(FriendlyByteBuf buf) {
        this.playerId = buf.readInt();
        this.animationName = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(playerId);
        buf.writeUtf(animationName);
    }

    @Environment(EnvType.CLIENT)
    public void handleClient() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Entity entity = mc.level.getEntity(playerId);
            if (entity instanceof Player player) {
                NichirinAnimations.playAnimation(player, animationName);
            }
        }
    }
}