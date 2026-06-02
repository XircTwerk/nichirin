package com.xirc.nichirin.client;

import com.xirc.nichirin.client.particle.*;
import com.xirc.nichirin.client.util.fabric.ItemPropertiesHelperImpl;
import com.xirc.nichirin.registry.NichirinKeybindRegistry;
import com.xirc.nichirin.registry.NichirinParticleRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;

public class BreathOfNichirinFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        registerParticles();

        // Register item properties first (client-only)
        ItemPropertiesHelperImpl.registerBentoBoxProperty();

        // Register keybinds before init (Fabric's onInitializeClient is the correct timing)
        NichirinKeybindRegistry.register();

        // Initialize all client-side systems
        BreathOfNichirinClient.init();
    }

    private static void registerParticles() {
        ParticleFactoryRegistry registry = ParticleFactoryRegistry.getInstance();
        registry.register(NichirinParticleRegistry.THUNDER.get(), ThunderParticleProvider::new);
        registry.register(NichirinParticleRegistry.SHOCKWAVE.get(), ShockwaveParticleProvider::new);
        registry.register(NichirinParticleRegistry.SOUND.get(), SoundParticleProvider::new);
        registry.register(NichirinParticleRegistry.FLASH1.get(), Flash1ParticleProvider::new);
        registry.register(NichirinParticleRegistry.FLASH2.get(), Flash2ParticleProvider::new);
        registry.register(NichirinParticleRegistry.BLUE_FLASH1.get(), BlueFlash1ParticleProvider::new);
        registry.register(NichirinParticleRegistry.BLUE_FLASH2.get(), BlueFlash2ParticleProvider::new);
        registry.register(NichirinParticleRegistry.BLUE_SHOCKWAVE.get(), BlueShockwaveParticleProvider::new);
        registry.register(NichirinParticleRegistry.BUTTERFLY.get(), ButterflyParticleProvider::new);
        registry.register(NichirinParticleRegistry.BLOOD_SPLAT.get(), BloodSplatParticleProvider::new);
        registry.register(NichirinParticleRegistry.BREATHING_AURA_WISP.get(), BreathingAuraWispParticleProvider::new);
        registry.register(NichirinParticleRegistry.SLASH_IMPACT_SPARK.get(), SlashImpactSparkParticleProvider::new);
    }
}