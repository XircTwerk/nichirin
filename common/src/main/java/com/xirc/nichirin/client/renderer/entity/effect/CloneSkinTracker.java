package com.xirc.nichirin.client.renderer.entity.effect;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.xirc.nichirin.common.entity.effect.PlayerCloneEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

@Environment(EnvType.CLIENT)
public class CloneSkinTracker {

    private static final Map<PlayerCloneEntity, Map<MinecraftProfileTexture.Type, ResourceLocation>> skinCache =
            new WeakHashMap<>();
    private static final Map<PlayerCloneEntity, String> modelCache = new WeakHashMap<>();
    private static final Map<PlayerCloneEntity, PlayerCloneClientPlayerEntity> playerCache = new WeakHashMap<>();
    private static final Set<PlayerCloneEntity> loading =
            Collections.newSetFromMap(new WeakHashMap<>());

    public static ResourceLocation getSkinFor(PlayerCloneEntity clone, MinecraftProfileTexture.Type type) {
        if (!skinCache.containsKey(clone)) load(clone);
        ResourceLocation skin = skinCache.getOrDefault(clone, Collections.emptyMap()).get(type);
        if (skin == null && type == MinecraftProfileTexture.Type.SKIN) {
            UUID uid = clone.getMasterUUID();
            return DefaultPlayerSkin.getDefaultSkin(uid != null ? uid : new UUID(0, 0));
        }
        return skin;
    }

    public static String getModelFor(PlayerCloneEntity clone) {
        if (!skinCache.containsKey(clone)) load(clone);
        return modelCache.getOrDefault(clone,
                clone.getMasterUUID() == null ? "default" : DefaultPlayerSkin.getSkinModelName(clone.getMasterUUID()));
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

        // In dev / offline mode the skin manager can't reach Mojang.
        // If the clone belongs to the local player, grab their already-loaded skin directly.
        if (mc.player != null && mc.player.getUUID().equals(profile.getId())) {
            Map<MinecraftProfileTexture.Type, ResourceLocation> map =
                    skinCache.computeIfAbsent(clone, c -> new HashMap<>());
            map.put(MinecraftProfileTexture.Type.SKIN, mc.player.getSkinTextureLocation());
            modelCache.put(clone, mc.player.getModelName());
            ResourceLocation cape = mc.player.getCloakTextureLocation();
            if (cape != null) map.put(MinecraftProfileTexture.Type.CAPE, cape);
            return;
        }

        synchronized (loading) {
            if (loading.contains(clone)) return;
            loading.add(clone);
        }

        mc.getSkinManager().registerSkins(profile, (type, id, texture) -> {
            skinCache.computeIfAbsent(clone, c -> new HashMap<>()).put(type, id);
            synchronized (loading) { loading.remove(clone); }
            if (type == MinecraftProfileTexture.Type.SKIN) {
                String model = texture.getMetadata("model");
                if (model != null) modelCache.put(clone, model);
            }
        }, true);
    }
}
