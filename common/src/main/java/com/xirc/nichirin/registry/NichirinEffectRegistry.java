package com.xirc.nichirin.registry;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.effect.BlockingStatusEffect;
import com.xirc.nichirin.common.effect.ShockedStatusEffect;
import com.xirc.nichirin.common.effect.StunnedStatusEffect;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;

public interface NichirinEffectRegistry {

    DeferredRegister<MobEffect> EFFECT_REGISTRY = DeferredRegister.create(BreathOfNichirin.MOD_ID, Registries.MOB_EFFECT);

    // Register the Shocked effect
    RegistrySupplier<MobEffect> SHOCKED = EFFECT_REGISTRY.register("shocked", ShockedStatusEffect::new);
    RegistrySupplier<MobEffect> BLOCKING = EFFECT_REGISTRY.register("blocking", BlockingStatusEffect::new);
    RegistrySupplier<StunnedStatusEffect> STUNNED = EFFECT_REGISTRY.register("stunned", StunnedStatusEffect::new);

    static void init() {
        // Register the deferred register
        EFFECT_REGISTRY.register();
        BreathOfNichirin.LOGGER.info("Registered Nichirin status effects");
    }
}