package com.xirc.nichirin.client.afterimage;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class AfterimageRenderState {
    private static boolean rendering;

    private AfterimageRenderState() {
    }

    public static boolean isRendering() {
        return rendering;
    }

    // Public: CloneRingRenderer reuses this flag so its ghost dispatches get the same
    // nameplate/armor suppression the afterimage mixins apply.
    public static void setRendering(boolean value) {
        rendering = value;
    }
}
