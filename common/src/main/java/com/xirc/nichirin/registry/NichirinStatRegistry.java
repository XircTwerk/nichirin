package com.xirc.nichirin.registry;

import com.xirc.nichirin.BreathOfNichirin;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;

/**
 * Registers custom vanilla statistics so they appear in the in-game Statistics screen.
 */
public interface NichirinStatRegistry {

    DeferredRegister<ResourceLocation> STAT_REGISTRY =
            DeferredRegister.create(BreathOfNichirin.MOD_ID, Registries.CUSTOM_STAT);

    /** Counts how many human-type mobs the player has devoured as a demon (via Bloody Flesh). */
    RegistrySupplier<ResourceLocation> HUMANS_EATEN_AS_DEMON =
            STAT_REGISTRY.register("humans_eaten_as_demon",
                    () -> BreathOfNichirin.id("humans_eaten_as_demon"));

    static void init() {
        // Attach the listener BEFORE registering so we don't miss the registration callback.
        // Creating the Stat (with its formatter) is what makes it appear in the vanilla
        // Statistics screen at 0, rather than only after the first award.
        HUMANS_EATEN_AS_DEMON.listen(id -> Stats.CUSTOM.get(id, StatFormatter.DEFAULT));
        STAT_REGISTRY.register();
    }
}
