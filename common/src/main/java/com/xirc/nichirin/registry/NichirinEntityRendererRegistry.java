package com.xirc.nichirin.registry;

import com.xirc.nichirin.client.renderer.BentoBoxBlockRenderer;
import com.xirc.nichirin.client.renderer.KatanaHolderBlockRenderer;
import com.xirc.nichirin.client.renderer.entity.FlashBombRenderer;
import com.xirc.nichirin.client.renderer.entity.SmokeBombRenderer;
import com.xirc.nichirin.client.renderer.entity.ThunderBallRenderer;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public interface NichirinEntityRendererRegistry {
    Logger LOGGER = LoggerFactory.getLogger(NichirinEntityRendererRegistry.class);

    record RendererData<T extends Entity>(RegistrySupplier<? extends EntityType<? extends T>> supplier, EntityRendererProvider<T> provider) {
        public void registerFabric() {
            try {
                if (supplier != null && supplier.isPresent()) {
                    EntityRendererRegistry.register(supplier, provider);
                } else {
                    LOGGER.warn("Entity supplier is null or not present for renderer registration");
                }
            } catch (Exception e) {
                LOGGER.error("Failed to register entity renderer: {}", e.getMessage());
            }
        }
    }

    record BlockEntityRendererData<T extends BlockEntity>(RegistrySupplier<? extends BlockEntityType<? extends T>> supplier, BlockEntityRendererProvider<T> provider) {
        public void registerFabric() {
            try {
                if (supplier != null && supplier.isPresent()) {
                    BlockEntityRendererRegistry.register(supplier.get(), provider);
                    LOGGER.info("Successfully registered block entity renderer for: {}", supplier.getId());
                } else {
                    LOGGER.warn("Block entity supplier is null or not present: {}", supplier != null ? supplier.getId() : "null");
                }
            } catch (Exception e) {
                LOGGER.error("Failed to register block entity renderer for {}: {}",
                        supplier != null ? supplier.getId() : "unknown", e.getMessage());
            }
        }
    }

    RendererData<?>[] entries = {
            new RendererData<>(NichirinEntityRegistry.THUNDER_BALL, ThunderBallRenderer::new),
            new RendererData<>(NichirinEntityRegistry.SMOKE_BOMB, SmokeBombRenderer::new),
            new RendererData<>(NichirinEntityRegistry.FLASH_BOMB, FlashBombRenderer::new)
    };

    BlockEntityRendererData<?>[] blockEntityEntries = {
            new BlockEntityRendererData<>(NichirinBlockEntityRegistry.BENTO_BOX_BLOCK_ENTITY, BentoBoxBlockRenderer::new),
            new BlockEntityRendererData<>(NichirinBlockEntityRegistry.KATANA_HOLDER_BLOCK_ENTITY, KatanaHolderBlockRenderer::new)
    };

    static void registerEntityRenderers(Consumer<RendererData<?>> consumer) {
        for (RendererData<?> entry : entries) {
            try {
                consumer.accept(entry);
            } catch (Exception e) {
                LOGGER.error("Failed to register entity renderer: {}", e.getMessage());
            }
        }
    }

    static void registerBlockEntityRenderers(Consumer<BlockEntityRendererData<?>> consumer) {
        for (BlockEntityRendererData<?> entry : blockEntityEntries) {
            try {
                consumer.accept(entry);
            } catch (Exception e) {
                LOGGER.error("Failed to register block entity renderer: {}", e.getMessage());
            }
        }
    }

    static void init() {
        LOGGER.info("Initializing NichirinEntityRendererRegistry...");

        // Register entity renderers first
        try {
            registerEntityRenderers(RendererData::registerFabric);
            LOGGER.info("Entity renderers registered successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to register entity renderers: {}", e.getMessage());
        }

        // Register block entity renderers second
        try {
            registerBlockEntityRenderers(BlockEntityRendererData::registerFabric);
            LOGGER.info("Block entity renderers registered successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to register block entity renderers: {}", e.getMessage());
        }
    }
}