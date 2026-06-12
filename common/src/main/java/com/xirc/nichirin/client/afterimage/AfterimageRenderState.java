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

    static void setRendering(boolean value) {
        rendering = value;
    }
}
