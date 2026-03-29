package com.xirc.nichirin.client.handler;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class BloodMoonClientState {

    private static boolean active = false;

    public static boolean isActive() {
        return active;
    }

    public static void setActive(boolean value) {
        active = value;
    }
}
