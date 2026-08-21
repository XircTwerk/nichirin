package com.xirc.nichirin.client;

import com.xirc.nichirin.client.gui.CooldownHUD;
import com.xirc.nichirin.client.gui.CompassNeedleHUD;
import com.xirc.nichirin.client.gyomei.ClientGyomeiWeaponManager;
import com.xirc.nichirin.client.gyomei.GyomeiDebugRenderer;
import com.xirc.nichirin.client.handler.MistBlurOverlay;
import com.xirc.nichirin.client.shader.NichirinShaderManager;
import com.xirc.nichirin.client.util.ClientInputHandler;
import com.xirc.nichirin.client.util.MenuTipPrefs;
import com.xirc.nichirin.common.system.DemonComponent;
import com.xirc.nichirin.registry.NichirinKeybindRegistry;
import dev.architectury.event.events.client.ClientCommandRegistrationEvent;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

@Environment(EnvType.CLIENT)
public class ClientEventHandler {

    // One-shot "press G to open the menu" tip. Shown once per game launch, a couple seconds after the
    // first world join so it lands after the vanilla join spam instead of being buried by it. The menu
    // is the core of the mod and easy to miss, so this points every new player straight at it.
    private static boolean joinTipShown = false;
    private static int joinTipCountdown = -1;

    public static void register() {
        ClientInputHandler.registerClientEvents();

        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(player -> {
            // Skip entirely if the player has dismissed the tip via its "Don't show this again" link.
            if (!joinTipShown && !MenuTipPrefs.isMenuTipHidden()) {
                joinTipCountdown = 40; // ~2s after join
            }
        });

        // Client-side command backing the tip's dismiss link (and a way to bring it back).
        ClientCommandRegistrationEvent.EVENT.register((dispatcher, ctx) -> dispatcher.register(
                ClientCommandRegistrationEvent.literal("nichirinmenutip")
                        .then(ClientCommandRegistrationEvent.literal("hide").executes(c -> {
                            MenuTipPrefs.setMenuTipHidden(true);
                            c.getSource().arch$sendSuccess(() -> Component.literal(
                                    "Breath of Nichirin menu tip hidden. Bring it back with /nichirinmenutip show.")
                                    .withStyle(ChatFormatting.GRAY), false);
                            return 1;
                        }))
                        .then(ClientCommandRegistrationEvent.literal("show").executes(c -> {
                            MenuTipPrefs.setMenuTipHidden(false);
                            joinTipShown = false; // let it fire again on the next world join this session too
                            c.getSource().arch$sendSuccess(() -> Component.literal(
                                    "Breath of Nichirin menu tip re-enabled — it'll show on your next join.")
                                    .withStyle(ChatFormatting.GRAY), false);
                            return 1;
                        }))));

        // Register HUD render event
        ClientGuiEvent.RENDER_HUD.register((graphics, partialTicks) -> {
            Minecraft minecraft = Minecraft.getInstance();

            if (minecraft.player == null || minecraft.options.hideGui) {
                return;
            }

            // Render cooldown HUD
            CooldownHUD.render(graphics, partialTicks.getGameTimeDeltaPartialTick(true));
            CompassNeedleHUD.render(graphics);
            GyomeiDebugRenderer.renderHud(graphics);

            // Note: Blood bar is now rendered via mixin - no manual rendering needed
        });

        // Handle player respawn - reset blood display
        ClientPlayerEvent.CLIENT_PLAYER_RESPAWN.register((oldPlayer, newPlayer) -> {
            // Reset client-side blood display when player respawns
            DemonComponent.onPlayerRespawn();
            clearScreenEffects();
        });

        // Clear lingering screen shaders/overlays the moment the player dies.
        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            if (minecraft.player != null && !minecraft.player.isAlive()) {
                clearScreenEffects();
            }
            ClientGyomeiWeaponManager.clientTick(minecraft);
            tickJoinTip(minecraft);
        });
    }

    /** Counts down the post-join delay and shows the one-time menu tip once the player is in-world. */
    private static void tickJoinTip(Minecraft minecraft) {
        if (joinTipCountdown < 0) return;
        if (minecraft.player == null || minecraft.level == null || minecraft.gui == null) return;
        if (joinTipCountdown-- > 0) return;
        joinTipCountdown = -1;
        joinTipShown = true;
        if (MenuTipPrefs.isMenuTipHidden()) return; // dismissed between join and firing

        Component key = NichirinKeybindRegistry.OPEN_GUI_KEY.getTranslatedKeyMessage();
        MutableComponent tip = Component.literal("⚔ Breath of Nichirin — press ")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal("[").withStyle(ChatFormatting.GRAY))
                .append(key.copy().withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                .append(Component.literal("] ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("to open your menu — breathing styles, moves, perks & more.")
                        .withStyle(ChatFormatting.GOLD));
        MutableComponent help = Component.literal("   ")
                .append(Component.literal("[/nichirin help]").withStyle(s -> s
                        .withColor(0x7EC8E3).withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nichirin help"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal("Run /nichirin help")))))
                .append(Component.literal("   ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal("[Don't show this again]").withStyle(s -> s
                        .withColor(0x888888).withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nichirinmenutip hide"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal("Stop showing this join tip")))));

        // Fire through the vanilla chat log (persists, scrollable) rather than the action bar.
        minecraft.gui.getChat().addMessage(tip);
        minecraft.gui.getChat().addMessage(help);
    }

    private static void clearScreenEffects() {
        NichirinShaderManager.getInstance().clearAll();
        MistBlurOverlay.clear();
        CompassNeedleHUD.clear();
    }
}
