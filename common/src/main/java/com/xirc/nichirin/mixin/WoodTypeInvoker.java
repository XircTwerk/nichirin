package com.xirc.nichirin.mixin;

import net.minecraft.world.level.block.state.properties.WoodType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * {@link WoodType#register(WoodType)} is private, but a custom wood type must be in
 * {@link WoodType#values()} so vanilla builds its sign model layers, renderer models and
 * sign materials for it. This invoker exposes the registration call.
 */
@Mixin(WoodType.class)
public interface WoodTypeInvoker {

    @Invoker("register")
    static WoodType nichirin$register(WoodType woodType) {
        throw new AssertionError();
    }
}
