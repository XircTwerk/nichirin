package com.xirc.nichirin.common.util;

import com.xirc.nichirin.common.attack.component.AbstractBreathingAttack;
import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.item.katana.Katana;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Simple input handler - blocks custom inputs when player is interacting with something
 */
public class InputHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(InputHandler.class);

    // Server-side data
    private static final Map<UUID, Katana> PLAYER_KATANAS = new HashMap<>();
    private static final Map<UUID, Long> BLOCKED_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> INTERACTION_BLOCKED_UNTIL = new HashMap<>();

    private static final long BLOCK_TICKS = 0;
    private static final long INTERACTION_BLOCK_TICKS = 0;

    // Packet IDs
    private static final ResourceLocation LEFT_CLICK_ID = ResourceLocation.fromNamespaceAndPath("nichirin", "katana_left");
    private static final ResourceLocation RIGHT_CLICK_ID = ResourceLocation.fromNamespaceAndPath("nichirin", "katana_right");
    private static final ResourceLocation RIGHT_CROUCH_ID = ResourceLocation.fromNamespaceAndPath("nichirin", "katana_right_crouch");
    private static final ResourceLocation FEEDBACK_ID = ResourceLocation.fromNamespaceAndPath("nichirin", "katana_feedback");

    public static void register() {
        registerServerPackets();
        registerServerEvents();
        registerInteractionEvents();
    }

    public static void registerClient() {
        if (Platform.getEnvironment() == Env.CLIENT) {
            try {
                Class<?> clientHandlerClass = Class.forName("com.xirc.nichirin.client.util.ClientInputHandler");
                clientHandlerClass.getMethod("registerClientEvents").invoke(null);
            } catch (Exception e) {
                LOGGER.error("Failed to register client katana events: {}", e.getMessage());
            }
        }
    }

    private static void registerInteractionEvents() {
        // No interaction event handling here - moved to MultiplayerInputHandler
        // This was causing the wrong execution order
    }

    private static boolean shouldHandleCustomInput(Player player) {
        ItemStack heldItem = player.getMainHandItem();

        // Check for katana with breathing moveset
        if (heldItem.getItem() instanceof Katana) {
            return MovesetHelper.hasBreathingMoveset(player);
        }

        // Check for demon moveset
        return MovesetHelper.hasDemonMoveset(player);
    }

    private static void registerServerPackets() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, LEFT_CLICK_ID, (buf, context) -> {
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                context.queue(() -> handleServerLeftClick(serverPlayer));
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, RIGHT_CLICK_ID, (buf, context) -> {
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                context.queue(() -> handleServerRightClick(serverPlayer, false));
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, RIGHT_CROUCH_ID, (buf, context) -> {
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                context.queue(() -> handleServerRightClick(serverPlayer, true));
            }
        });
    }

    private static void handleServerLeftClick(ServerPlayer player) {
        if (isServerBlocked(player)) {
            return;
        }

        ItemStack item = player.getMainHandItem();
        if (item.getItem() instanceof Katana katana) {
            Katana instance = getKatanaInstance(player, katana);
            instance.performAttack(player);
        }
    }

    private static void handleServerRightClick(ServerPlayer player, boolean crouch) {
        if (isServerBlocked(player)) {
            return;
        }

        // Right-click cancels any active breathing attack before starting a new one.
        if (AbstractBreathingAttack.cancelActiveAttack(player)) {
            return;
        }

        ItemStack item = player.getMainHandItem();
        if (item.getItem() instanceof Katana katana) {
            Katana instance = getKatanaInstance(player, katana);

            boolean originalCrouch = player.isShiftKeyDown();
            if (crouch != originalCrouch) {
                player.setShiftKeyDown(crouch);
            }

            String moveName = null;
            var moveset = MovesetHelper.getMoveset(player);
            if (moveset != null) {
                moveName = crouch ?
                        moveset.getCrouchRightClickMoveName() :
                        moveset.getRightClickMoveName();
            }

            instance.use(player.level(), player, InteractionHand.MAIN_HAND);
            sendFeedback(player, moveName, crouch);

            if (crouch != originalCrouch) {
                player.setShiftKeyDown(originalCrouch);
            }
        }
    }

    private static boolean isServerBlocked(Player player) {
        UUID uuid = player.getUUID();
        long currentTime = player.level().getGameTime();

        // Check regular blocking
        Long blockedUntil = BLOCKED_UNTIL.get(uuid);
        if (blockedUntil != null) {
            if (currentTime < blockedUntil) {
                return true;
            } else {
                BLOCKED_UNTIL.remove(uuid);
            }
        }

        // Check interaction blocking
        Long interactionBlockedUntil = INTERACTION_BLOCKED_UNTIL.get(uuid);
        if (interactionBlockedUntil != null) {
            if (currentTime < interactionBlockedUntil) {
                return true;
            } else {
                INTERACTION_BLOCKED_UNTIL.remove(uuid);
            }
        }

        // Check effect blocking
        return player.hasEffect(NichirinEffectRegistry.blocking());
    }

    private static void sendFeedback(ServerPlayer player, String moveName, boolean crouch) {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeBoolean(moveName != null);

            if (moveName != null) {
                buf.writeUtf(moveName);
                int cooldown = moveName.contains("Heat Lightning") ? 40 : 30;
                buf.writeInt(cooldown);
            } else {
                buf.writeBoolean(crouch);
            }

            NetworkManager.sendToPlayer(player, FEEDBACK_ID, NetworkBufferUtils.server(buf, player));
        } catch (Exception e) {
            // Ignore networking errors
        }
    }

    private static void registerServerEvents() {
        TickEvent.PLAYER_POST.register(player -> {
            if (!player.level().isClientSide) {
                tickPlayer(player);
            }
        });

        PlayerEvent.PLAYER_QUIT.register(player -> {
            if (!player.level().isClientSide) {
                cleanupPlayer(player);
            }
        });
    }

    private static void tickPlayer(Player player) {
        Katana katana = PLAYER_KATANAS.get(player.getUUID());
        if (katana != null) {
            ItemStack mainHand = player.getMainHandItem();
            if (mainHand.getItem() instanceof Katana) {
                katana.tick(player);
            } else {
                PLAYER_KATANAS.remove(player.getUUID());
            }
        }
    }

    private static Katana getKatanaInstance(Player player, Katana item) {
        UUID id = player.getUUID();
        Katana existing = PLAYER_KATANAS.get(id);

        if (existing == null || existing != item) {
            PLAYER_KATANAS.put(id, item);
            return item;
        }

        return existing;
    }

    public static void cleanupPlayer(Player player) {
        UUID id = player.getUUID();
        PLAYER_KATANAS.remove(id);
        BLOCKED_UNTIL.remove(id);
        INTERACTION_BLOCKED_UNTIL.remove(id);
    }

    public static void clearAll() {
        PLAYER_KATANAS.clear();
        BLOCKED_UNTIL.clear();
        INTERACTION_BLOCKED_UNTIL.clear();
    }

    public static void blockAfterBreathingMove(Player player) {
        if (!player.level().isClientSide) {
            long blockUntil = player.level().getGameTime() + BLOCK_TICKS;
            BLOCKED_UNTIL.put(player.getUUID(), blockUntil);
        }
    }
}