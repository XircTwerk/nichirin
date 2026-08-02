package com.xirc.nichirin.client.renderer.item;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.AbstractClientPlayer;

/** Identifies the player whose hand layer is currently dispatching an item renderer. */
@Environment(EnvType.CLIENT)
public final class HeldItemRenderContext {
    private static final ThreadLocal<AbstractClientPlayer> PLAYER = new ThreadLocal<>();

    private HeldItemRenderContext() {}

    public static void begin(AbstractClientPlayer player) { PLAYER.set(player); }
    public static AbstractClientPlayer current() { return PLAYER.get(); }
    public static void end() { PLAYER.remove(); }
}
