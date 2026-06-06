package com.xirc.nichirin.client.util;

import com.xirc.nichirin.client.handler.AttackWheelHandler;
import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.item.katana.SimpleKatana;
import com.xirc.nichirin.common.util.MultiplayerInputHandler;
import com.xirc.nichirin.common.util.NetworkBufferUtils;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * CLIENT-ONLY handler with proper vanilla interaction priority
 */
public class ClientInputHandler {

    private static final ResourceLocation LEFT_CLICK_ID = ResourceLocation.fromNamespaceAndPath("nichirin", "katana_left");
    private static final ResourceLocation RIGHT_CLICK_ID = ResourceLocation.fromNamespaceAndPath("nichirin", "katana_right");
    private static final ResourceLocation RIGHT_CROUCH_ID = ResourceLocation.fromNamespaceAndPath("nichirin", "katana_right_crouch");
    private static final ResourceLocation FEEDBACK_ID = ResourceLocation.fromNamespaceAndPath("nichirin", "katana_feedback");
    private static boolean registered;

    public static void registerClientEvents() {
        if (registered) return;
        registered = true;
        registerClientInteractions();
    }

    private static void registerClientInteractions() {

        // Left click air. While the block button (RMB) is held, a left click performs the special
        // move that used to be on right-click (crouch variant if sneaking) instead of the M1 attack.
        InteractionEvent.CLIENT_LEFT_CLICK_AIR.register((player, hand) -> {
            if (!canPerformAttacks(player, hand)) {
                return;
            }
            if (isBlockHeld(player)) {
                sendBlockSpecial(player);
                return;
            }
            if (isInputBlocked()) {
                return;
            }
            sendLeftClick(player);
        });

        // Right click air is now BLOCK (handled by the hold-poll in BlockingInputHandler), so it no
        // longer fires a special. Intentionally a no-op.
        InteractionEvent.CLIENT_RIGHT_CLICK_AIR.register((player, hand) -> {
        });

        // Entity attack (LEFT CLICK ON ENTITIES) — same block+left-click special routing.
        PlayerEvent.ATTACK_ENTITY.register((player, level, entity, hand, hitResult) -> {
            if (!canPerformAttacks(player, hand)) {
                return EventResult.pass();
            }

            if (level.isClientSide) {
                if (isBlockHeld(player)) {
                    sendBlockSpecial(player);
                    return EventResult.interruptFalse();
                }
                if (isInputBlocked()) {
                    return EventResult.interruptFalse();
                }
                sendLeftClick(player);
            }

            return EventResult.interruptFalse();
        });
    }

    /** True when the player is holding the block button (RMB) and is able to block/attack. */
    public static boolean isBlockHeld(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null || !mc.options.keyUse.isDown()) return false;
        if (player.getMainHandItem().getItem() instanceof SimpleKatana) return true;
        if (player.getOffhandItem().getItem() instanceof SimpleKatana) return true;
        return player.getMainHandItem().isEmpty()
                && (MovesetHelper.hasFightingMoveset(player) || MovesetHelper.hasDemonMoveset(player));
    }

    /** Send the old right-click special (crouch variant if sneaking) — used for block + left-click. */
    private static void sendBlockSpecial(Player player) {
        boolean crouch = isCrouchInputDown(player);
        try {
            if (player.getMainHandItem().getItem() instanceof SimpleKatana katana) {
                katana.displayClientRightClickFeedback(player, crouch);
            }
        } catch (Exception ignored) {
        }
        MultiplayerInputHandler.InputType inputType = crouch
                ? MultiplayerInputHandler.InputType.RIGHT_CLICK_CROUCH
                : MultiplayerInputHandler.InputType.RIGHT_CLICK;
        MultiplayerInputHandler.sendInput(inputType, player);
    }

    /**
     * Called after Minecraft has tried block, entity, and item use for both hands without
     * any vanilla or modded interaction claiming the right click.
     */
    public static void sendDemonRightClickFallback() {
        // Right-click is now the BLOCK button for katana, CQC and demon users alike. Their specials
        // moved to block + left-click (see CLIENT_LEFT_CLICK_AIR), so this right-click fallback is
        // intentionally disabled to avoid firing a special when the player means to guard.
    }

    private static boolean canPerformAttacks(Player player, InteractionHand hand) {
        ItemStack item = player.getItemInHand(hand);

        // Check if holding katana (for breathing users ONLY)
        if (item.getItem() instanceof SimpleKatana) {
            return true; // Katana holders can use breathing abilities
        }

        // Empty-hand fighting styles take priority over demon arts.
        return item.isEmpty() && (MovesetHelper.hasFightingMoveset(player) || MovesetHelper.hasDemonMoveset(player));
    }

    private static boolean canPerformRightClickAttacks(Player player, InteractionHand hand) {
        ItemStack item = player.getItemInHand(hand);
        if (item.getItem() instanceof SimpleKatana) {
            return true;
        }
        return hand == InteractionHand.MAIN_HAND && item.isEmpty() && MovesetHelper.hasFightingMoveset(player);
    }

    private static boolean isInputBlocked() {
        try {
            Minecraft mc = Minecraft.getInstance();

            if (mc.player != null && mc.player.hasEffect(NichirinEffectRegistry.blocking())) {
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
            // Show cooldown for katana users
            if (player.getMainHandItem().getItem() instanceof SimpleKatana katana) {
                katana.displayClientCooldown(player);
            }
        } catch (Exception e) {
        }

        try {
            // Use the unified input system that handles both katana and demon routing
            MultiplayerInputHandler.sendInput(MultiplayerInputHandler.InputType.LEFT_CLICK, player);
        } catch (Exception e) {
            // Fallback to direct packet for backwards compatibility
            try {
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                NetworkManager.sendToServer(LEFT_CLICK_ID, NetworkBufferUtils.client(buf));
            } catch (Exception fallbackException) {
            }
        }
    }

    private static void sendRightClick(Player player) {
        boolean crouch = isCrouchInputDown(player);

        try {
            // Show cooldown for katana users
            if (player.getMainHandItem().getItem() instanceof SimpleKatana katana) {
                katana.displayClientRightClickFeedback(player, crouch);
            }
        } catch (Exception e) {
        }

        try {
            // Use the unified input system that handles both katana and demon routing
            MultiplayerInputHandler.InputType inputType = crouch ?
                    MultiplayerInputHandler.InputType.RIGHT_CLICK_CROUCH :
                    MultiplayerInputHandler.InputType.RIGHT_CLICK;

            MultiplayerInputHandler.sendInput(inputType, player);
        } catch (Exception e) {
            // Fallback to direct packet for backwards compatibility
            try {
                ResourceLocation id = crouch ? RIGHT_CROUCH_ID : RIGHT_CLICK_ID;
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                NetworkManager.sendToServer(id, NetworkBufferUtils.client(buf));
            } catch (Exception fallbackException) {
            }
        }
    }

    private static boolean isCrouchInputDown(Player player) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.options != null && mc.options.keyShift.isDown()) {
                return true;
            }
        } catch (Exception ignored) {
        }
        return player.isShiftKeyDown() || player.isCrouching();
    }
}
