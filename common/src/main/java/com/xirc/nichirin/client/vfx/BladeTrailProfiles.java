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
            maxSamples = Math.max(1, Math.min(120, maxSamples));
            heightMultiplier = Math.max(0.15f, Math.min(6.0f, heightMultiplier));
            opacity = Math.max(0.05f, Math.min(1.0f, opacity));
            if (theme == null) theme = Theme.WHITE;
        }
    }

    public static final Profile DEFAULT = new Profile(165L, 14, 1.0f, 0.88f, Theme.WHITE);
    private static final Map<String, Profile> ATTACK_PROFILES = new HashMap<>();

    static {
        Profile dualLight = new Profile(340L, 32, 1.35f, 0.92f, Theme.WHITE);
        Profile dualHeavy = new Profile(420L, 36, 1.55f, 0.94f, Theme.WHITE);
        // Check strikes with the pommel; retaining blade samples here reads like a liquid trail.
        register("sword.check", new Profile(55L, 1, 0.15f, 0.05f, Theme.WHITE));
        register("sword.slash", new Profile(340L, 32, 1.35f, 0.92f, Theme.WHITE));
        register("sword.slash_followup", new Profile(340L, 32, 1.35f, 0.92f, Theme.WHITE));
        register("sword.slash_followup_2", new Profile(380L, 34, 1.45f, 0.93f, Theme.WHITE));
        register("sword.doubleslash", new Profile(380L, 34, 1.45f, 0.93f, Theme.WHITE));
        register("sword.vertical", new Profile(1260L, 108, 4.65f, 0.94f, Theme.WHITE));
        register("sword.overhead", new Profile(1260L, 108, 4.65f, 0.94f, Theme.WHITE));
        register("sword.thrust", new Profile(1380L, 114, 4.05f, 0.92f, Theme.WHITE));
        register("sword.dual_m1", dualLight);
        register("sword.dual_m1_followup", dualLight);
        register("sword.dual_xslash", new Profile(420L, 36, 1.50f, 0.94f, Theme.WHITE));
        register("sword.dualcrouchheavy", new Profile(480L, 38, 1.70f, 0.95f, Theme.WHITE));
        register("sword.dual_combo", new Profile(420L, 38, 1.50f, 0.94f, Theme.WHITE));
        register("sword.dual_slam", dualHeavy);
        register("sword.dual_thrust", new Profile(540L, 40, 1.65f, 0.95f, Theme.WHITE));
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
