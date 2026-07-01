package com.xirc.nichirin.common.util;

import com.xirc.nichirin.registry.NichirinBlockRegistry;
import net.minecraft.world.level.block.state.BlockState;

public final class WisteriaBlocks {

    private WisteriaBlocks() {
    }

    public static boolean isWisteriaBlock(BlockState state) {
        return state.is(NichirinBlockRegistry.WISTERIA_LOG.get())
                || state.is(NichirinBlockRegistry.STRIPPED_WISTERIA_LOG.get())
                || state.is(NichirinBlockRegistry.WISTERIA_WOOD.get())
                || state.is(NichirinBlockRegistry.STRIPPED_WISTERIA_WOOD.get())
                || state.is(NichirinBlockRegistry.WISTERIA_PLANKS.get())
                || state.is(NichirinBlockRegistry.WISTERIA_LEAVES.get())
                || state.is(NichirinBlockRegistry.WISTERIA_GLOW_LICHEN.get())
                || state.is(NichirinBlockRegistry.WISTERIA_GLOW_BERRIES.get())
                || state.is(NichirinBlockRegistry.WISTERIA_LANTERN.get())
                || state.is(NichirinBlockRegistry.WISTERIA_STAIRS.get())
                || state.is(NichirinBlockRegistry.WISTERIA_SLAB.get())
                || state.is(NichirinBlockRegistry.WISTERIA_FENCE.get())
                || state.is(NichirinBlockRegistry.WISTERIA_FENCE_GATE.get())
                || state.is(NichirinBlockRegistry.WISTERIA_DOOR.get())
                || state.is(NichirinBlockRegistry.WISTERIA_TRAPDOOR.get())
                || state.is(NichirinBlockRegistry.WISTERIA_PRESSURE_PLATE.get())
                || state.is(NichirinBlockRegistry.WISTERIA_BUTTON.get())
                || state.is(NichirinBlockRegistry.WISTERIA_SAPLING.get());
    }

    public static boolean isWisteriaLightSource(BlockState state) {
        return state.is(NichirinBlockRegistry.WISTERIA_LEAVES.get())
                || state.is(NichirinBlockRegistry.WISTERIA_GLOW_LICHEN.get())
                || state.is(NichirinBlockRegistry.WISTERIA_GLOW_BERRIES.get())
                || state.is(NichirinBlockRegistry.WISTERIA_LANTERN.get());
    }

    public static boolean isWisteriaShaderLightSource(BlockState state) {
        return state.is(NichirinBlockRegistry.WISTERIA_LEAVES.get())
                || state.is(NichirinBlockRegistry.WISTERIA_LANTERN.get());
    }
}
