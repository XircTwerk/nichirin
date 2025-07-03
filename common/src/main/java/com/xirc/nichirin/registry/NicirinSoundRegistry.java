package com.xirc.nichirin.registry;

import com.xirc.nichirin.BreathOfNichirin;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public interface NicirinSoundRegistry {
    DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BreathOfNichirin.MOD_ID, Registries.SOUND_EVENT);

    // Katana sounds
    RegistrySupplier<SoundEvent> BASIC_SLASH_1 = registerSound("basicslash1");
    RegistrySupplier<SoundEvent> BASIC_SLASH_2 = registerSound("basicslash2");

    static RegistrySupplier<SoundEvent> registerSound(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(
                new ResourceLocation(BreathOfNichirin.MOD_ID, name)
        ));
    }

    static void init() {
        SOUND_EVENTS.register();
    }
}