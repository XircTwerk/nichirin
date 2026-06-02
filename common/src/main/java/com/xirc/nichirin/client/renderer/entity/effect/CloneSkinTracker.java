package com.xirc.nichirin.client.renderer.entity.effect;

import com.mojang.authlib.GameProfile;
import com.xirc.nichirin.common.entity.effect.PlayerCloneEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

@Environment(EnvType.CLIENT)
public class CloneSkinTracker {

    private static final Map<PlayerCloneEntity, PlayerSkin> skinCache = new WeakHashMap<>();
    private static final Map<PlayerCloneEntity, PlayerCloneClientPlayerEntity> playerCache = new WeakHashMap<>();
    private static final Set<PlayerCloneEntity> loading =
            Collections.newSetFromMap(new WeakHashMap<>());

    public static PlayerSkin getSkinFor(PlayerCloneEntity clone) {
        if (!skinCache.containsKey(clone)) {
            load(clone);
        }
        UUID uid = clone.getMasterUUID();
        return skinCache.getOrDefault(clone, DefaultPlayerSkin.get(uid != null ? uid : new UUID(0, 0)));
    }

    public static boolean isSlimFor(PlayerCloneEntity clone) {
        return getSkinFor(clone).model() == PlayerSkin.Model.SLIM;
    }

    public static PlayerCloneClientPlayerEntity toPlayer(PlayerCloneEntity clone) {
        if (clone.getGameProfile() == null) return null;
        PlayerCloneClientPlayerEntity clonePlayer =
                playerCache.computeIfAbsent(clone, PlayerCloneClientPlayerEntity::new);
        clonePlayer.updateData();
        return clonePlayer;
    }

    private static void load(PlayerCloneEntity clone) {
        GameProfile profile = clone.getGameProfile();
        if (profile == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            for (AbstractClientPlayer player : mc.level.players()) {
                if (player.getUUID().equals(profile.getId())) {
                    skinCache.put(clone, player.getSkin());
                    return;
                }
            }
        }

        synchronized (loading) {
            if (loading.contains(clone)) return;
            loading.add(clone);
        }

        mc.getSkinManager().getOrLoad(profile).thenAccept(skin -> {
            skinCache.put(clone, skin);
            synchronized (loading) {
                loading.remove(clone);
            }
        });
    }
}