package com.xirc.nichirin.client;

import com.xirc.nichirin.client.particle.*;
import com.xirc.nichirin.client.renderer.entity.*;
import com.xirc.nichirin.client.renderer.*;
import com.xirc.nichirin.client.util.ItemPropertiesHelper;
import com.xirc.nichirin.registry.NichirinBlockEntityRegistry;
import com.xirc.nichirin.registry.NichirinEntityRegistry;
import com.xirc.nichirin.registry.NichirinParticleRegistry;
import dev.architectury.registry.client.particle.ParticleProviderRegistry;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;

@Mod.EventBusSubscriber(modid = "nichirin", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class BreathOfNichirinForgeClient {

    @SubscribeEvent
    public static void onConstructMod(FMLConstructModEvent event) {
        // Register particles VERY early
        registerParticles();
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Initialize client systems (without particle registration)
            BreathOfNichirinClient.init();
            ItemPropertiesHelper.registerBentoBoxProperty();

            EntityRenderers.register(NichirinEntityRegistry.THUNDER_BALL.get(), ThunderBallRenderer::new);
            EntityRenderers.register(NichirinEntityRegistry.FLASH_BOMB.get(), FlashBombRenderer::new);
            EntityRenderers.register(NichirinEntityRegistry.SMOKE_BOMB.get(), SmokeBombRenderer::new);
            EntityRenderers.register(NichirinEntityRegistry.BOAR.get(), BoarEntityRenderer::new);

            BlockEntityRenderers.register(NichirinBlockEntityRegistry.BENTO_BOX_BLOCK_ENTITY.get(), BentoBoxBlockRenderer::new);
            BlockEntityRenderers.register(NichirinBlockEntityRegistry.KATANA_HOLDER_BLOCK_ENTITY.get(), KatanaHolderBlockRenderer::new);
        });
    }

    private static void registerParticles() {
        try {
            ParticleProviderRegistry.register(NichirinParticleRegistry.THUNDER, ThunderParticleProvider::new);
            ParticleProviderRegistry.register(NichirinParticleRegistry.SHOCKWAVE, ShockwaveParticleProvider::new);
            ParticleProviderRegistry.register(NichirinParticleRegistry.SOUND, SoundParticleProvider::new);
            ParticleProviderRegistry.register(NichirinParticleRegistry.FLASH1, Flash1ParticleProvider::new);
            ParticleProviderRegistry.register(NichirinParticleRegistry.FLASH2, Flash2ParticleProvider::new);
            ParticleProviderRegistry.register(NichirinParticleRegistry.BLUE_FLASH1, BlueFlash1ParticleProvider::new);
            ParticleProviderRegistry.register(NichirinParticleRegistry.BLUE_FLASH2, BlueFlash2ParticleProvider::new);
            ParticleProviderRegistry.register(NichirinParticleRegistry.BLUE_SHOCKWAVE, BlueShockwaveParticleProvider::new);
            ParticleProviderRegistry.register(NichirinParticleRegistry.BUTTERFLY, ButterflyParticleProvider::new);
        } catch (Exception e) {
            // Log but don't crash
            System.err.println("Failed to register particles: " + e.getMessage());
        }
    }
}