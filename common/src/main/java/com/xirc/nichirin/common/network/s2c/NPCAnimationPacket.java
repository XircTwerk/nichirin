package com.xirc.nichirin.common.network.s2c;

import com.xirc.nichirin.client.renderer.entity.npc.NPCAnimationManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class NPCAnimationPacket {

    private final int entityId;
    private final String animationName;
    private final boolean stopAnimation;

    public NPCAnimationPacket(int entityId, String animationName) {
        this.entityId = entityId;
        this.animationName = animationName;
        this.stopAnimation = false;
    }

    public NPCAnimationPacket(int entityId, String animationName, boolean stopAnimation) {
        this.entityId = entityId;
        this.animationName = animationName;
        this.stopAnimation = stopAnimation;
    }

    public NPCAnimationPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.animationName = buf.readUtf();
        this.stopAnimation = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeUtf(animationName);
        buf.writeBoolean(stopAnimation);
    }

    @Environment(EnvType.CLIENT)
    public void handleClient() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Entity entity = mc.level.getEntity(entityId);
            if (entity != null) {

                if (!(entity instanceof LivingEntity)) {
                    return;
                }

                LivingEntity livingEntity = (LivingEntity) entity;

                // Initialize if needed
                if (NPCAnimationManager.getAnimationStack(entity.getId()) == null) {
                    NPCAnimationManager.initializeNPCAnimation(livingEntity);
                }

                if (stopAnimation) {
                    NPCAnimationManager.stopAnimation(livingEntity);
                } else {
                    NPCAnimationManager.playAnimation(livingEntity, animationName);
                }
            }
        }
    }

    public static NPCAnimationPacket playAnimation(int entityId, String animationName) {
        return new NPCAnimationPacket(entityId, animationName, false);
    }

    public static NPCAnimationPacket stopAnimation(int entityId) {
        return new NPCAnimationPacket(entityId, "", true);
    }

    public int getEntityId() {
        return entityId;
    }

    public String getAnimationName() {
        return animationName;
    }

    public boolean isStopAnimation() {
        return stopAnimation;
    }
}