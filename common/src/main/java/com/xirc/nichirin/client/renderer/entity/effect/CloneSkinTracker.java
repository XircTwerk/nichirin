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
        // The live in-world player's resolved skin is authoritative — check it EVERY frame and
        // keep the cache fresh from it. Checking live before the cache is what fixes the "most
        // clones render as Alex" bug: loadAsync resolves an incomplete profile (UUID + name, no
        // texture properties) to a default slim/Alex skin and caches it permanently; a cache-first
        // lookup would then shadow the real skin forever. Since BSCA's clones belong to the caster
        // standing right there, the live skin is always available and the poisoned cache never wins.
        PlayerSkin live = livePlayerSkin(clone);
        if (live != null) {
            skinCache.put(clone, live);
            return live;
        }

        PlayerSkin cached = skinCache.get(clone);
        if (cached != null) return cached;

        // Master genuinely out of render range: resolve the profile async (kicked once). Until it
        // returns, show the UUID-derived default rather than caching it permanently.
        loadAsync(clone);
        UUID uid = clone.getMasterUUID();
        return DefaultPlayerSkin.get(uid != null ? uid : new UUID(0, 0));
    }

    private static PlayerSkin livePlayerSkin(PlayerCloneEntity clone) {
        UUID uid = clone.getMasterUUID();
        if (uid == null) return null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        for (AbstractClientPlayer player : mc.level.players()) {
            if (player.getUUID().equals(uid)) {
                return player.getSkin();
            }
        }
        return null;
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

    private static void loadAsync(PlayerCloneEntity clone) {
        GameProfile profile = clone.getGameProfile();
        if (profile == null) return;

        synchronized (loading) {
            if (loading.contains(clone)) return;
            loading.add(clone);
        }

        Minecraft.getInstance().getSkinManager().getOrLoad(profile).thenAccept(skin -> {
            if (skin != null) skinCache.put(clone, skin);
            synchronized (loading) {
                loading.remove(clone);
            }
        });
    }
}