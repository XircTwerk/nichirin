package com.xirc.nichirin.registry;

import com.xirc.nichirin.client.renderer.block.KatanaHolderBlockRenderer;
import com.xirc.nichirin.client.renderer.entity.effect.PlayerCloneRenderer;
import com.xirc.nichirin.client.renderer.entity.animal.BoarEntityRenderer;
import com.xirc.nichirin.client.renderer.entity.attack.ThunderBallRenderer;
import com.xirc.nichirin.client.renderer.entity.npc.TempleDemonRenderer;
import com.xirc.nichirin.client.renderer.entity.npc.ThunderBreathingTrainerRenderer;
import com.xirc.nichirin.client.renderer.entity.npc.WaterBreathingTrainerRenderer;
import com.xirc.nichirin.client.renderer.entity.projectile.FlashBombRenderer;
import com.xirc.nichirin.client.renderer.entity.projectile.SmokeBombRenderer;
import com.xirc.nichirin.client.renderer.entity.projectile.ThrownKatanaRenderer;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public interface NichirinEntityRendererRegistry {

    record RendererData<T extends Entity>(RegistrySupplier<? extends EntityType<? extends T>> supplier, EntityRendererProvider<T> provider) {
        public void registerFabric() {
            EntityRendererRegistry.register(supplier, provider);
        }
    }

    record BlockEntityRendererData<T extends BlockEntity>(RegistrySupplier<? extends BlockEntityType<? extends T>> supplier, BlockEntityRendererProvider<T> provider) {
        public void registerFabric() {
            BlockEntityRendererRegistry.register(supplier.get(), provider);
        }
    }

    RendererData<?>[] entries = {
            new RendererData<>(NichirinEntityRegistry.THUNDER_BALL, ThunderBallRenderer::new),
            new RendererData<>(NichirinEntityRegistry.FLASH_BOMB, FlashBombRenderer::new),
            new RendererData<>(NichirinEntityRegistry.SMOKE_BOMB, SmokeBombRenderer::new),
            new RendererData<>(NichirinEntityRegistry.BOAR, BoarEntityRenderer::new),
            new RendererData<>(NichirinEntityRegistry.TEMPLE_DEMON, TempleDemonRenderer::new),
            new RendererData<>(NichirinEntityRegistry.WATER_BREATHING_TRAINER, WaterBreathingTrainerRenderer::new),
            new RendererData<>(NichirinEntityRegistry.THUNDER_BREATHING_TRAINER, ThunderBreathingTrainerRenderer::new),
            new RendererData<>(NichirinEntityRegistry.THROWN_KATANA, ThrownKatanaRenderer::new),
            new RendererData<>(NichirinEntityRegistry.PLAYER_CLONE, PlayerCloneRenderer::new)
    };

    BlockEntityRendererData<?>[] blockEntityEntries = {
            new BlockEntityRendererData<>(NichirinBlockEntityRegistry.KATANA_HOLDER_BLOCK_ENTITY, KatanaHolderBlockRenderer::new)
    };


    static void registerEntityRenderers(Consumer<RendererData<?>> consumer) {
        for (RendererData<?> entry : entries) consumer.accept(entry);
    }

    static void registerBlockEntityRenderers(Consumer<BlockEntityRendererData<?>> consumer) {
        for (BlockEntityRendererData<?> entry : blockEntityEntries) consumer.accept(entry);
    }

    static void init() {
        registerEntityRenderers(RendererData::registerFabric);
        registerBlockEntityRenderers(BlockEntityRendererData::registerFabric);
    }
}