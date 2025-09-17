package com.xirc.nichirin.client.util;

import com.xirc.nichirin.client.gui.CooldownHUD;
import com.xirc.nichirin.client.handler.AttackWheelHandler;
import com.xirc.nichirin.common.item.katana.SimpleKatana;
import com.xirc.nichirin.common.util.MultiplayerInputHandler;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * CLIENT-ONLY katana handler - contains all client-specific code
 */
public class KatanaClientHandler {

    private static final ResourceLocation LEFT_CLICK_ID = new ResourceLocation("nichirin", "katana_left");
    private static final ResourceLocation RIGHT_CLICK_ID = new ResourceLocation("nichirin", "katana_right");
    private static final ResourceLocation RIGHT_CROUCH_ID = new ResourceLocation("nichirin", "katana_right_crouch");
    private static final ResourceLocation FEEDBACK_ID = new ResourceLocation("nichirin", "katana_feedback");

    public static void registerClientEvents() {
        registerClientInteractions();
    }

    private static void registerClientInteractions() {
        // Left click air
        InteractionEvent.CLIENT_LEFT_CLICK_AIR.register((player, hand) -> {
            ItemStack item = player.getItemInHand(hand);
            if (!(item.getItem() instanceof SimpleKatana)) return;

            if (isInputBlocked()) {
                return;
            }

            sendLeftClick(player);
        });

        // Right click air
        InteractionEvent.CLIENT_RIGHT_CLICK_AIR.register((player, hand) -> {
            ItemStack item = player.getItemInHand(hand);
            if (!(item.getItem() instanceof SimpleKatana)) return;

            if (isInputBlocked()) {
                return;
            }

            sendRightClick(player);
        });

        // Entity attack blocking
        PlayerEvent.ATTACK_ENTITY.register((player, level, entity, hand, hitResult) -> {
            ItemStack item = player.getItemInHand(hand);
            if (!(item.getItem() instanceof SimpleKatana)) {
                return EventResult.pass();
            }

            if (level.isClientSide) {
                if (isInputBlocked()) {
                    return EventResult.interruptFalse();
                }

                sendLeftClick(player);
            }

            return EventResult.interruptFalse();
        });
    }

    private static boolean isInputBlocked() {
        try {
            Minecraft mc = Minecraft.getInstance();

            if (mc.player != null && mc.player.hasEffect(NichirinEffectRegistry.BLOCKING.get())) {
                return true;
            }

            try {
                if (AttackWheelHandler.shouldBlockAttackInputs()) {
                    return true;
                }
            } catch (Exception e) {
                // Ignore
            }

            try {
                if (MultiplayerInputHandler.shouldBlockInputsClient()) {
                    return true;
                }
            } catch (Exception e) {
                // Ignore
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static void sendLeftClick(Player player) {
        try {
            if (player.getMainHandItem().getItem() instanceof SimpleKatana katana) {
                katana.displayClientCooldown(player);
            }
        } catch (Exception e) {
            // Ignore
        }

        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            NetworkManager.sendToServer(LEFT_CLICK_ID, buf);
        } catch (Exception e) {
            // Ignore
        }
    }

    private static void sendRightClick(Player player) {
        boolean crouch = player.isCrouching();
        ResourceLocation id = crouch ? RIGHT_CROUCH_ID : RIGHT_CLICK_ID;

        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            NetworkManager.sendToServer(id, buf);
        } catch (Exception e) {
            // Ignore
        }
    }
}