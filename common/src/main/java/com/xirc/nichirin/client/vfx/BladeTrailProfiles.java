package com.xirc.nichirin.client.vfx;

import com.xirc.nichirin.client.animation.NichirinAnimations;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.AbstractClientPlayer;

import java.util.HashMap;
import java.util.Map;

/** Code-configurable blade-trail properties selected by the active player animation. */
@Environment(EnvType.CLIENT)
public final class BladeTrailProfiles {
    public enum Theme {
        WHITE(0xDDE6E8, 0xFFFFFF, 0xFFFFFF),
        WATER(0x004E86, 0x34D1FD, 0xEEFBFD),
        FLAME(0x692F0F, 0xFC5520, 0xFFF245),
        THUNDER(0x181817, 0xF2CF1D, 0xFFFFF4),
        MIST(0x274E54, 0x4E9DA9, 0xBDE9F0),
        BEAST(0x171D22, 0x6CC1CF, 0xE7F0ED);

        private final int shadow;
        private final int body;
        private final int highlight;

        Theme(int shadow, int body, int highlight) {
            this.shadow = shadow;
            this.body = body;
            this.highlight = highlight;
        }

        int shadow() { return shadow; }
        int body() { return body; }
        int highlight() { return highlight; }
    }

    public record Profile(long lifetimeMillis, int maxSamples, float heightMultiplier,
                          float opacity, Theme theme) {
        public Profile {
            lifetimeMillis = Math.max(55L, lifetimeMillis);
            maxSamples = Math.max(2, Math.min(40, maxSamples));
            heightMultiplier = Math.max(0.15f, Math.min(4.0f, heightMultiplier));
            opacity = Math.max(0.05f, Math.min(1.0f, opacity));
            if (theme == null) theme = Theme.WHITE;
        }
    }

    public static final Profile DEFAULT = new Profile(165L, 14, 1.0f, 0.88f, Theme.WHITE);
    private static final Map<String, Profile> ATTACK_PROFILES = new HashMap<>();

    static {
        register("water_wheel", new Profile(380L, 32, 2.15f, 0.96f, Theme.WATER));
    }

    private BladeTrailProfiles() {}

    public static void register(String animationName, Profile profile) {
        if (animationName == null || animationName.isBlank() || profile == null) return;
        ATTACK_PROFILES.put(animationName.toLowerCase(), profile);
    }

    public static Profile forPlayer(AbstractClientPlayer player) {
        String animation = NichirinAnimations.getActiveAnimationName(player);
        return animation == null ? DEFAULT : ATTACK_PROFILES.getOrDefault(animation, DEFAULT);
    }
}
