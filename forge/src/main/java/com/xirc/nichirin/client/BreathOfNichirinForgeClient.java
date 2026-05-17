package com.xirc.nichirin.client;

import com.xirc.nichirin.client.particle.*;
import com.xirc.nichirin.client.renderer.block.KatanaHolderBlockRenderer;
import com.xirc.nichirin.client.renderer.entity.animal.BoarEntityRenderer;
import com.xirc.nichirin.client.renderer.entity.attack.ThunderBallRenderer;
import com.xirc.nichirin.client.renderer.entity.npc.TempleDemonRenderer;
import com.xirc.nichirin.client.renderer.entity.npc.WaterBreathingTrainerRenderer;
import com.xirc.nichirin.client.renderer.entity.projectile.FlashBombRenderer;
import com.xirc.nichirin.client.renderer.entity.projectile.SmokeBombRenderer;
import com.xirc.nichirin.client.renderer.entity.projectile.ThrownKatanaRenderer;
import com.xirc.nichirin.client.util.ItemPropertiesHelper;
import com.xirc.nichirin.common.config.NichirinModConfig;
import com.xirc.nichirin.registry.NichirinBlockEntityRegistry;
import com.xirc.nichirin.registry.NichirinEntityRegistry;
import com.xirc.nichirin.registry.NichirinKeybindRegistry;
import com.xirc.nichirin.registry.NichirinParticleRegistry;
import dev.architectury.registry.client.particle.ParticleProviderRegistry;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod.EventBusSubscriber(modid = "nichirin", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class BreathOfNichirinForgeClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(BreathOfNichirinForgeClient.class);

    @SubscribeEvent
    public static void onConstructMod(FMLConstructModEvent event) {
        // Register particles VERY early
        registerParticles();
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        // Must happen here — Forge closes the keymapping registry before FMLClientSetupEvent
        NichirinKeybindRegistry.register();
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Register the "Config" button that appears in the Forge mod list
        try {
            ModLoadingContext.get().registerExtensionPoint(
                    ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory(
                            (mc, parent) -> AutoConfig.getConfigScreen(NichirinModConfig.class, parent).get()
                    )
            );
        } catch (Exception e) {
            // cloth-config not present — skip silently
        }

        event.enqueueWork(() -> {
            // Initialize client systems (without particle registration)
            BreathOfNichirinClient.init();
            ItemPropertiesHelper.registerBentoBoxProperty();

            EntityRenderers.register(NichirinEntityRegistry.THUNDER_BALL.get(), ThunderBallRenderer::new);
            EntityRenderers.register(NichirinEntityRegistry.FLASH_BOMB.get(), FlashBombRenderer::new);
            EntityRenderers.register(NichirinEntityRegistry.SMOKE_BOMB.get(), SmokeBombRenderer::new);
            EntityRenderers.register(NichirinEntityRegistry.BOAR.get(), BoarEntityRenderer::new);
            EntityRenderers.register(NichirinEntityRegistry.TEMPLE_DEMON.get(), TempleDemonRenderer::new);
            EntityRenderers.register(NichirinEntityRegistry.WATER_BREATHING_TRAINER.get(), WaterBreathingTrainerRenderer::new);
            EntityRenderers.register(NichirinEntityRegistry.THROWN_KATANA.get(), ThrownKatanaRenderer::new);

            // BentoBoxBlockRenderer not yet implemented
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
            ParticleProviderRegistry.register(NichirinParticleRegistry.BLOOD_SPLAT, BloodSplatParticleProvider::new);
            ParticleProviderRegistry.register(NichirinParticleRegistry.BREATHING_AURA_WISP, BreathingAuraWispParticleProvider::new);
            ParticleProviderRegistry.register(NichirinParticleRegistry.SLASH_IMPACT_SPARK, SlashImpactSparkParticleProvider::new);
        } catch (Exception e) {
            // Log but don't crash
            LOGGER.error("Failed to register particles: {}", e.getMessage());
        }
    }
}