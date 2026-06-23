package com.xirc.nichirin.registry;

import com.xirc.nichirin.BreathOfNichirin;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public interface NichirinSoundRegistry {
    DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BreathOfNichirin.MOD_ID, Registries.SOUND_EVENT);

    // Katana sounds
    RegistrySupplier<SoundEvent> BASIC_SLASH_1 = registerSound("basicslash1");
    RegistrySupplier<SoundEvent> BASIC_SLASH_2 = registerSound("basicslash2");

    // Combat sounds
    RegistrySupplier<SoundEvent> PARRY_CLASH = registerSound("parry_clash");
    RegistrySupplier<SoundEvent> PARRY_CLASH_2 = registerSound("parry_clash_2");
    RegistrySupplier<SoundEvent> SLASH_WHOOSH_1 = registerSound("slash_whoosh_1");
    RegistrySupplier<SoundEvent> SLASH_WHOOSH_2 = registerSound("slash_whoosh_2");
    RegistrySupplier<SoundEvent> BLOCK_CLANG = registerSound("block_clang");

    // Demon sounds
    RegistrySupplier<SoundEvent> BITE_CRUNCH = registerSound("bite_crunch");
    RegistrySupplier<SoundEvent> STOMP_LAND = registerSound("stomp_land");

    // Gun (Genya DB) sounds
    RegistrySupplier<SoundEvent> GENYA_SINGLESHOT = registerSound("genya_db_singleshot");
    RegistrySupplier<SoundEvent> GENYA_DOUBLESHOT = registerSound("genya_db_doubleshot");
    RegistrySupplier<SoundEvent> GENYA_RELOAD = registerSound("genya_db_reload");

    // Entity sounds
    RegistrySupplier<SoundEvent> THUNDER_BALL = registerSound("thunderball");

    RegistrySupplier<SoundEvent> THUNDERCLAP_FLASH = registerSound("thunderclap_flash");


    static RegistrySupplier<SoundEvent> registerSound(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, name)
        ));
    }

    static void init() {
        SOUND_EVENTS.register();
    }
}