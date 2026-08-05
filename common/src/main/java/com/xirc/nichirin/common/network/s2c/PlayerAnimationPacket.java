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
    private final float animSpeed;

    public PlayerAnimationPacket(int playerId, String animationName) {
        this(playerId, animationName, 1.0f);
    }

    public PlayerAnimationPacket(int playerId, String animationName, float animSpeed) {
        this.playerId = playerId;
        this.animationName = animationName;
        this.animSpeed = animSpeed;
    }

    public PlayerAnimationPacket(FriendlyByteBuf buf) {
        this.playerId = buf.readInt();
        this.animationName = buf.readUtf();
        this.animSpeed = buf.readFloat();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(playerId);
        buf.writeUtf(animationName);
        buf.writeFloat(animSpeed);
    }

    public String getAnimationName() {
        return animationName;
    }

    @Environment(EnvType.CLIENT)
    public void handleClient() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Entity entity = mc.level.getEntity(playerId);
            if (entity instanceof Player player) {
                NichirinAnimations.playAnimation(player, animationName, animSpeed);
            }
        }
    }
}
