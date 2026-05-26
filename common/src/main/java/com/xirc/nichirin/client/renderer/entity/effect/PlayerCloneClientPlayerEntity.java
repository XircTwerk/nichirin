package com.xirc.nichirin.client.renderer.entity.effect;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.xirc.nichirin.common.entity.effect.PlayerCloneEntity;
import dev.architectury.event.events.client.ClientTickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * A fake AbstractClientPlayer that wraps a PlayerCloneEntity.
 * Used so PlayerRenderer can render a clone with the correct skin, cape, and equipment.
 */
@Environment(EnvType.CLIENT)
public class PlayerCloneClientPlayerEntity extends AbstractClientPlayer {

    private static final Set<PlayerCloneClientPlayerEntity> entities =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static final GameProfile DUMMY_PROFILE =
            new GameProfile(UUID.nameUUIDFromBytes("nichirin$playerClone".getBytes()), null);

    private final PlayerCloneEntity clone;

    static {
        ClientTickEvent.CLIENT_LEVEL_POST.register(world ->
                entities.stream().filter(e -> !e.isRemoved()).forEach(PlayerCloneClientPlayerEntity::tick));
        ClientTickEvent.CLIENT_POST.register(client -> {
            if (client.level == null) entities.clear();
        });
    }

    public PlayerCloneClientPlayerEntity(PlayerCloneEntity clone) {
        super(Minecraft.getInstance().level, DUMMY_PROFILE);
        this.clone = clone;
        entities.add(this);
    }

    @Override
    public boolean isSpectator() { return false; }

    @Override
    public boolean isCreative() { return false; }

    @Override
    protected PlayerInfo getPlayerInfo() { return null; }

    @Override
    public boolean isSkinLoaded() {
        return CloneSkinTracker.getSkinFor(clone, MinecraftProfileTexture.Type.SKIN) != null;
    }

    @Override
    public ResourceLocation getSkinTextureLocation() {
        return CloneSkinTracker.getSkinFor(clone, MinecraftProfileTexture.Type.SKIN);
    }

    @Override
    public boolean isCapeLoaded() {
        return CloneSkinTracker.getSkinFor(clone, MinecraftProfileTexture.Type.CAPE) != null;
    }

    @Override
    public ResourceLocation getCloakTextureLocation() {
        return CloneSkinTracker.getSkinFor(clone, MinecraftProfileTexture.Type.CAPE);
    }

    @Override
    public boolean isElytraLoaded() {
        return CloneSkinTracker.getSkinFor(clone, MinecraftProfileTexture.Type.ELYTRA) != null;
    }

    @Override
    public ResourceLocation getElytraTextureLocation() {
        return CloneSkinTracker.getSkinFor(clone, MinecraftProfileTexture.Type.ELYTRA);
    }

    @Override
    public String getModelName() {
        return CloneSkinTracker.getModelFor(clone);
    }

    @Override
    public boolean shouldShowName() { return false; }

    @Override
    public void tick() {
        super.tick();
        tickCount++;
        setMainArm(clone.isLeftHanded() ? HumanoidArm.LEFT : HumanoidArm.RIGHT);
        entityData.set(Player.DATA_PLAYER_MODE_CUSTOMISATION, clone.getPartMask());
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setItemSlot(slot, clone.getItemBySlot(slot));
        }
    }

    /** Sync position/rotation/animation state from the clone entity before rendering. */
    public void updateData() {
        xo = clone.xo;
        yo = clone.yo;
        zo = clone.zo;
        setPosRaw(clone.getX(), clone.getY(), clone.getZ());
        yBodyRotO = clone.yBodyRotO;
        yBodyRot = clone.yBodyRot;
        yHeadRotO = clone.yHeadRotO;
        yHeadRot = clone.yHeadRot;
        hurtTime = clone.hurtTime;
        hurtDuration = clone.hurtDuration;
        swinging = clone.swinging;
        swingingArm = clone.swingingArm;
        oAttackAnim = clone.oAttackAnim;
        attackAnim = clone.attackAnim;
        swingTime = clone.swingTime;
        deathTime = clone.deathTime;
        dead = clone.isDeadOrDying();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) { return false; }
}
