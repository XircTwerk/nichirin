package com.xirc.nichirin.common.network;

import com.xirc.nichirin.common.util.AnimationUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class PlayerAnimationPacket {
    private final int playerId;
    private final String animationName;
    private final AnimationUtils.EasingType easing;

    public PlayerAnimationPacket(int playerId, String animationName) {
        this(playerId, animationName, AnimationUtils.EasingType.EASE_OUT_CUBIC);
    }

    public PlayerAnimationPacket(int playerId, String animationName, AnimationUtils.EasingType easing) {
        this.playerId = playerId;
        this.animationName = animationName;
        this.easing = easing;
    }

    public PlayerAnimationPacket(FriendlyByteBuf buf) {
        this.playerId = buf.readInt();
        this.animationName = buf.readUtf();
        this.easing = buf.readEnum(AnimationUtils.EasingType.class);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(playerId);
        buf.writeUtf(animationName);
        buf.writeEnum(easing);
    }

    @Environment(EnvType.CLIENT)
    public void handleClient() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Entity entity = mc.level.getEntity(playerId);
            if (entity instanceof Player player) {
                AnimationUtils.playAnimation(player, animationName, easing);
            }
        }
    }
}