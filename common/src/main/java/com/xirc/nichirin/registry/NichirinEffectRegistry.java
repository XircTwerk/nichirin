package com.xirc.nichirin.registry;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.effect.*;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Mob;

public interface NichirinEffectRegistry {

    DeferredRegister<MobEffect> EFFECT_REGISTRY = DeferredRegister.create(BreathOfNichirin.MOD_ID, Registries.MOB_EFFECT);

    // Register the Shocked effect
    RegistrySupplier<MobEffect> SHOCKED = EFFECT_REGISTRY.register("shocked", ShockedStatusEffect::new);
    RegistrySupplier<MobEffect> BLOCKING = EFFECT_REGISTRY.register("blocking", BlockingStatusEffect::new);
    RegistrySupplier<MobEffect> STUNNED = EFFECT_REGISTRY.register("stunned", StunnedStatusEffect::new);
    RegistrySupplier<MobEffect> BURNING = EFFECT_REGISTRY.register("burning", BurningStatusEffect::new);
    RegistrySupplier<MobEffect> VENOM = EFFECT_REGISTRY.register("venom", VenomStatusEffect::new);
    RegistrySupplier<MobEffect> DISORIENTED = EFFECT_REGISTRY.register("disoriented", DisorientedStatusEffect::new);
    RegistrySupplier<MobEffect> BLURRY = EFFECT_REGISTRY.register("blurry", BlurryStatusEffect::new);

    static void init() {
        // Register the deferred register
        EFFECT_REGISTRY.register();
        BreathOfNichirin.LOGGER.info("Registered Nichirin status effects");
    }
}