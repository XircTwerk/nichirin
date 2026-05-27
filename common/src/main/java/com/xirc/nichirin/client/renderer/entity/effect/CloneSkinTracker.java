package com.xirc.nichirin.client.renderer.entity.effect;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.xirc.nichirin.common.entity.effect.PlayerCloneEntity;
import com.xirc.nichirin.mixin.client.PlayerModelAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
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
    private static final Map<PlayerCloneEntity, Boolean> slimCache = new WeakHashMap<>();
    private static final Map<PlayerCloneEntity, PlayerCloneClientPlayerEntity> playerCache = new WeakHashMap<>();
    private static final Set<PlayerCloneEntity> loading =
            Collections.newSetFromMap(new WeakHashMap<>());

    public static ResourceLocation getSkinFor(PlayerCloneEntity clone, MinecraftProfileTexture.Type type) {
        // Keep retrying until we have a real skin entry (UUID may not be synced yet on first frames)
        Map<MinecraftProfileTexture.Type, ResourceLocation> cached = skinCache.get(clone);
        if (cached == null || !cached.containsKey(MinecraftProfileTexture.Type.SKIN)) {
            load(clone);
        }
        ResourceLocation skin = skinCache.getOrDefault(clone, Collections.emptyMap()).get(type);
        if (skin == null && type == MinecraftProfileTexture.Type.SKIN) {
            UUID uid = clone.getMasterUUID();
            return DefaultPlayerSkin.getDefaultSkin(uid != null ? uid : new UUID(0, 0));
        }
        return skin;
    }

    public static boolean isSlimFor(PlayerCloneEntity clone) {
        Map<MinecraftProfileTexture.Type, ResourceLocation> cached = skinCache.get(clone);
        if (cached == null || !cached.containsKey(MinecraftProfileTexture.Type.SKIN)) {
            load(clone);
        }
        return slimCache.getOrDefault(clone, false);
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

        // Fast path: check all players currently in the level — their skins are already loaded.
        // This covers both the local player (immediate) and other players in multiplayer (synchronous).
        if (mc.level != null) {
            for (AbstractClientPlayer p : mc.level.players()) {
                if (p.getUUID().equals(profile.getId())) {
                    Map<MinecraftProfileTexture.Type, ResourceLocation> map =
                            skinCache.computeIfAbsent(clone, c -> new HashMap<>());
                    map.put(MinecraftProfileTexture.Type.SKIN, p.getSkinTextureLocation());
                    ResourceLocation cape = p.getCloakTextureLocation();
                    if (cape != null) map.put(MinecraftProfileTexture.Type.CAPE, cape);

                    // Use PlayerModelAccessor on the actual player renderer model — same approach as armor
                    var renderer = mc.getEntityRenderDispatcher().getRenderer(p);
                    if (renderer instanceof PlayerRenderer pr) {
                        PlayerModel<?> playerModel = pr.getModel();
                        slimCache.put(clone, ((PlayerModelAccessor) playerModel).isSlim());
                    } else {
                        slimCache.put(clone, false);
                    }
                    return;
                }
            }
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
                slimCache.put(clone, "slim".equals(model));
            }
        }, true);
    }
}
