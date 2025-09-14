package com.xirc.nichirin.registry;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.worldgen.trees.wysteria.WysteriaRootDecorator;
import com.xirc.nichirin.common.worldgen.trees.wysteria.WysteriaHangingLeavesDecorator;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;

public interface NichirinTreeDecoratorTypes {
    DeferredRegister<TreeDecoratorType<?>> TREE_DECORATORS = DeferredRegister.create(BreathOfNichirin.MOD_ID, Registries.TREE_DECORATOR_TYPE);

    RegistrySupplier<TreeDecoratorType<WysteriaRootDecorator>> WYSTERIA_ROOT_DECORATOR = TREE_DECORATORS.register("wisteria_root_decorator",
            () -> new TreeDecoratorType<>(WysteriaRootDecorator.CODEC));

    RegistrySupplier<TreeDecoratorType<WysteriaHangingLeavesDecorator>> WYSTERIA_HANGING_LEAVES_DECORATOR = TREE_DECORATORS.register("wisteria_hanging_leaves_decorator",
            () -> new TreeDecoratorType<>(WysteriaHangingLeavesDecorator.CODEC));

    static void register() {
        TREE_DECORATORS.register();
    }
}