package com.xirc.nichirin.registry;

import com.xirc.nichirin.client.renderer.entity.ThunderBallRenderer;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public interface NichirinEntityRendererRegistry {

    record RendererData<T extends Entity>(RegistrySupplier<? extends EntityType<? extends T>> supplier, EntityRendererProvider<T> provider) {
        public void registerFabric() {
            EntityRendererRegistry.register(supplier, provider);
        }
    }

    RendererData<?>[] entries = {
            new RendererData<>(NichirinEntityRegistry.THUNDER_BALL, ThunderBallRenderer::new),
            // Add more entity renderers here as needed
    };

    static void registerEntityRenderers(Consumer<RendererData<?>> consumer) {
        for (RendererData<?> entry : entries) consumer.accept(entry);
    }

    static void init() {
        registerEntityRenderers(RendererData::registerFabric);
    }
}