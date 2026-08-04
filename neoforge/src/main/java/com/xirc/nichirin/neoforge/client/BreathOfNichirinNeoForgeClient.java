package com.xirc.nichirin.neoforge.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.xirc.nichirin.client.BreathOfNichirinClient;
import com.xirc.nichirin.client.outline.OutlineShaderHolder;
import com.xirc.nichirin.client.vfx.VfxShaderHolder;
import com.xirc.nichirin.client.particle.*;
import com.xirc.nichirin.client.renderer.block.KatanaHolderBlockRenderer;
import com.xirc.nichirin.client.renderer.entity.animal.BoarEntityRenderer;
import com.xirc.nichirin.client.renderer.entity.attack.ThunderBallRenderer;
import com.xirc.nichirin.client.renderer.entity.npc.AkazaRenderer;
import com.xirc.nichirin.client.renderer.entity.npc.TempleDemonRenderer;
import com.xirc.nichirin.client.renderer.entity.npc.ThunderBreathingTrainerRenderer;
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
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import java.io.IOException;

@EventBusSubscriber(modid = "nichirin", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class BreathOfNichirinNeoForgeClient {
    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(NichirinParticleRegistry.THUNDER.get(), ThunderParticleProvider::new);
        event.registerSpriteSet(NichirinParticleRegistry.SHOCKWAVE.get(), ShockwaveParticleProvider::new);
        event.registerSpriteSet(NichirinParticleRegistry.SOUND.get(), SoundParticleProvider::new);
        event.registerSpriteSet(NichirinParticleRegistry.FLASH1.get(), Flash1ParticleProvider::new);
        event.registerSpriteSet(NichirinParticleRegistry.FLASH2.get(), Flash2ParticleProvider::new);
        event.registerSpriteSet(NichirinParticleRegistry.BLUE_FLASH1.get(), BlueFlash1ParticleProvider::new);
        event.registerSpriteSet(NichirinParticleRegistry.BLUE_FLASH2.get(), BlueFlash2ParticleProvider::new);
        event.registerSpriteSet(NichirinParticleRegistry.BLUE_SHOCKWAVE.get(), BlueShockwaveParticleProvider::new);
        event.registerSpriteSet(NichirinParticleRegistry.BUTTERFLY.get(), ButterflyParticleProvider::new);
        event.registerSpriteSet(NichirinParticleRegistry.BLOOD_SPLAT.get(), BloodSplatParticleProvider::new);
        event.registerSpriteSet(NichirinParticleRegistry.BREATHING_AURA_WISP.get(), BreathingAuraWispParticleProvider::new);
        event.registerSpriteSet(NichirinParticleRegistry.SLASH_IMPACT_SPARK.get(), SlashImpactSparkParticleProvider::new);
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
        // Cel-shader outline: displaces each vertex along its surface normal so body parts
        // expand outward together (rather than drifting apart from a uniform pose-stack scale).
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath("nichirin", "outline_cel"),
                        DefaultVertexFormat.NEW_ENTITY),
                OutlineShaderHolder::setShader);
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath("nichirin", "vfx_pixel"),
                        DefaultVertexFormat.POSITION_COLOR),
                VfxShaderHolder::setShader);
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        // Must happen here: NeoForge closes the keymapping registry before FMLClientSetupEvent
        NichirinKeybindRegistry.registerKeyMappings(event::register);
        NichirinKeybindRegistry.registerClientTickHandler();
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Register the "Config" button that appears in the NeoForge mod list
        try {
            ModLoadingContext.get().registerExtensionPoint(
                    IConfigScreenFactory.class,
                    () -> (mc, parent) -> AutoConfig.getConfigScreen(NichirinModConfig.class, parent).get()
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
            EntityRenderers.register(NichirinEntityRegistry.AKAZA.get(), AkazaRenderer::new);
            EntityRenderers.register(NichirinEntityRegistry.WATER_BREATHING_TRAINER.get(), WaterBreathingTrainerRenderer::new);
            EntityRenderers.register(NichirinEntityRegistry.THUNDER_BREATHING_TRAINER.get(), ThunderBreathingTrainerRenderer::new);
            EntityRenderers.register(NichirinEntityRegistry.THROWN_KATANA.get(), ThrownKatanaRenderer::new);

            // BentoBoxBlockRenderer not yet implemented
            BlockEntityRenderers.register(NichirinBlockEntityRegistry.KATANA_HOLDER_BLOCK_ENTITY.get(), KatanaHolderBlockRenderer::new);
            BlockEntityRenderers.register(NichirinBlockEntityRegistry.WISTERIA_SIGN.get(), SignRenderer::new);
            BlockEntityRenderers.register(NichirinBlockEntityRegistry.WISTERIA_HANGING_SIGN.get(), HangingSignRenderer::new);
        });
    }
}
