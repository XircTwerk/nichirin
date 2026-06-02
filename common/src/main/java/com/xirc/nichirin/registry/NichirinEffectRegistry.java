package com.xirc.nichirin.registry;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.effect.*;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;

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

    static Holder<MobEffect> shocked() {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(SHOCKED.get());
    }

    static Holder<MobEffect> blocking() {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(BLOCKING.get());
    }

    static Holder<MobEffect> stunned() {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(STUNNED.get());
    }

    static Holder<MobEffect> burning() {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(BURNING.get());
    }

    static Holder<MobEffect> venom() {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(VENOM.get());
    }

    static Holder<MobEffect> disoriented() {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(DISORIENTED.get());
    }

    static Holder<MobEffect> blurry() {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(BLURRY.get());
    }

    static void init() {
        // Register the deferred register
        EFFECT_REGISTRY.register();
    }
}