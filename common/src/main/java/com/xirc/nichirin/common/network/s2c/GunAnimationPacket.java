package com.xirc.nichirin.common.network.s2c;

import com.xirc.nichirin.client.animation.GenyaDBAnimator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class GunAnimationPacket {

    private final int playerId;
    private final String animName;

    public GunAnimationPacket(int playerId, String animName) {
        this.playerId = playerId;
        this.animName = animName;
    }

    public GunAnimationPacket(FriendlyByteBuf buf) {
        this.playerId = buf.readInt();
        this.animName = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(playerId);
        buf.writeUtf(animName);
    }

    @Environment(EnvType.CLIENT)
    public void handleClient() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity entity = mc.level.getEntity(playerId);
        if (!(entity instanceof Player player)) return;
        if (player.getMainHandItem().isEmpty()) return;
        // Publish the animation name, then bump the sequence so every gun animator instance picks it up
        // exactly once on its next render. Order matters for the volatile happens-before guarantee.
        GenyaDBAnimator.requestedAnim = animName;
        GenyaDBAnimator.requestSeq++;
    }
}
