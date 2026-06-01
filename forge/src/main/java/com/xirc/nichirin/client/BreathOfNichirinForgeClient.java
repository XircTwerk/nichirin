package com.xirc.nichirin.client;

import com.xirc.nichirin.client.renderer.block.KatanaHolderBlockRenderer;
import com.xirc.nichirin.client.renderer.entity.animal.BoarEntityRenderer;
import com.xirc.nichirin.client.renderer.entity.attack.ThunderBallRenderer;
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

@Mod.EventBusSubscriber(modid = "nichirin", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class BreathOfNichirinForgeClient {
    @SubscribeEvent
    public static void onConstructMod(FMLConstructModEvent event) {
        // Register particles VERY early
        BreathOfNichirinClient.registerParticles();
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        // Must happen here — Forge closes the keymapping registry before FMLClientSetupEvent
        NichirinKeybindRegistry.registerKeyMappings(event::register);
        NichirinKeybindRegistry.registerClientTickHandler();
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
            EntityRenderers.register(NichirinEntityRegistry.THUNDER_BREATHING_TRAINER.get(), ThunderBreathingTrainerRenderer::new);
            EntityRenderers.register(NichirinEntityRegistry.THROWN_KATANA.get(), ThrownKatanaRenderer::new);

            // BentoBoxBlockRenderer not yet implemented
            BlockEntityRenderers.register(NichirinBlockEntityRegistry.KATANA_HOLDER_BLOCK_ENTITY.get(), KatanaHolderBlockRenderer::new);
        });
    }
}
