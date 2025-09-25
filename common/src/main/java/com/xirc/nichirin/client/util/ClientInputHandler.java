package com.xirc.nichirin.client.util;

import com.xirc.nichirin.client.handler.AttackWheelHandler;
import com.xirc.nichirin.common.data.MovesetHelper;
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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * CLIENT-ONLY handler with proper vanilla interaction priority
 */
public class ClientInputHandler {

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
            if (isInputBlocked()) {
                return;
            }

            if (canPerformAttacks(player, hand)) {
                sendLeftClick(player);
            }
        });

        // Right click air
        InteractionEvent.CLIENT_RIGHT_CLICK_AIR.register((player, hand) -> {
            if (isInputBlocked()) {
                return;
            }

            if (canPerformAttacks(player, hand)) {
                sendRightClick(player);
            }
        });

        // Entity interaction - let vanilla happen first, only use custom if vanilla doesn't handle it
        InteractionEvent.INTERACT_ENTITY.register((player, entity, hand) -> {
            if (isInputBlocked()) {
                return EventResult.pass();
            }

            // For demons not holding katanas - check if vanilla would handle this interaction
            if (canPerformDemonAttacks(player, hand)) {
                // Test if vanilla would handle this interaction
                ItemStack heldItem = player.getItemInHand(hand);

                // Check common vanilla interactions that should take priority
                if (wouldVanillaHandleEntityInteraction(entity, heldItem)) {
                    return EventResult.pass(); // Let vanilla handle it
                }

                // Vanilla won't handle it, use custom demon attack
                sendRightClick(player);
                return EventResult.interruptTrue(); // Block vanilla to prevent double processing
            }

            return EventResult.pass(); // Allow normal entity interaction
        });

        // Entity attack blocking (LEFT CLICK ON ENTITIES)
        PlayerEvent.ATTACK_ENTITY.register((player, level, entity, hand, hitResult) -> {
            if (!canPerformAttacks(player, hand)) {
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

    /**
     * Check if vanilla would handle this entity interaction
     */
    private static boolean wouldVanillaHandleEntityInteraction(net.minecraft.world.entity.Entity entity, ItemStack heldItem) {
        // Check for animal feeding
        if (entity instanceof net.minecraft.world.entity.animal.Animal animal) {
            if (animal.isFood(heldItem)) {
                return true; // Vanilla will handle feeding
            }
            if (!animal.isBaby() && animal.canFallInLove()) {
                return true; // Vanilla might handle breeding
            }
        }

        // Check for other common vanilla interactions
        if (entity instanceof net.minecraft.world.entity.npc.Villager) {
            return true; // Trading
        }

        if (entity instanceof net.minecraft.world.entity.decoration.ItemFrame) {
            return true; // Item frame interactions
        }

        if (entity instanceof net.minecraft.world.entity.decoration.ArmorStand) {
            return true; // Armor stand interactions
        }

        // Check for lead usage
        if (heldItem.is(net.minecraft.world.item.Items.LEAD) &&
                entity instanceof net.minecraft.world.entity.Mob) {
            return true;
        }

        // Check for name tag usage
        if (heldItem.is(net.minecraft.world.item.Items.NAME_TAG) &&
                heldItem.hasCustomHoverName()) {
            return true;
        }

        return false; // No vanilla interaction expected
    }

    private static boolean canPerformAttacks(Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack item = player.getItemInHand(hand);

        // Check if holding katana (for breathing users ONLY)
        if (item.getItem() instanceof SimpleKatana) {
            return true; // Katana holders can use breathing abilities
        }

        // NOT holding katana - check if has demon moveset
        return MovesetHelper.hasDemonMoveset(player);
    }

    /**
     * Check if player can perform demon attacks specifically (no katana held)
     */
    private static boolean canPerformDemonAttacks(Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack item = player.getItemInHand(hand);
        boolean holdingKatana = item.getItem() instanceof SimpleKatana;
        boolean hasDemon = MovesetHelper.hasDemonMoveset(player);

        // Must NOT be holding katana AND must have demon moveset
        return !holdingKatana && hasDemon;
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
                NetworkManager.sendToServer(LEFT_CLICK_ID, buf);
            } catch (Exception fallbackException) {
            }
        }
    }

    private static void sendRightClick(Player player) {
        boolean crouch = player.isCrouching();

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
                NetworkManager.sendToServer(id, buf);
            } catch (Exception fallbackException) {
            }
        }
    }
}