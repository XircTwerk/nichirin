package com.xirc.nichirin.client.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Client-only visual config for Breath of Nichirin.
 *
 * <p>Saved to {@code .minecraft/config/nichirin_client.toml} on each player's
 * own machine. Changes here only affect YOUR game — not other players.</p>
 */
@Environment(EnvType.CLIENT)
@Config(name = "nichirin_client")
public class NichirinClientConfig implements ConfigData {

    @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
    public VisualConfig visual = new VisualConfig();

    public static class VisualConfig {

        @ConfigEntry.Gui.Tooltip
        public boolean enableBreathingAuraParticles = true;

        @ConfigEntry.Gui.Tooltip
        public boolean enableHitParticles = true;

        @ConfigEntry.Gui.Tooltip
        public boolean enableParrySparks = true;

        @ConfigEntry.Gui.Tooltip
        public boolean enableAfterimages = true;
    }

    /**
     * Returns the live client config instance. Safe to call any time on the client.
     */
    public static NichirinClientConfig get() {
        try {
            return AutoConfig.getConfigHolder(NichirinClientConfig.class).getConfig();
        } catch (Exception e) {
            return new NichirinClientConfig();
        }
    }

    /**
     * Register with AutoConfig. Call once during client init.
     */
    public static void register() {
        try {
            AutoConfig.register(NichirinClientConfig.class,
                    NichirinClientConfigSerializer::new);
        } catch (Exception e) {
            // cloth-config not installed; defaults will be used
        }
    }
}