package com.xirc.nichirin.common.util;

import com.xirc.nichirin.common.attack.moves.thunder.ThunderClapFlashAttack;
import com.xirc.nichirin.common.data.BreathingStyleHelper;
import com.xirc.nichirin.common.item.katana.SimpleKatana;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.api.EnvType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Enhanced katana input handler with FIXED client/server separation
 */
public class KatanaInputHandler {

    // Server-side katana instances
    private static final Map<UUID, SimpleKatana> PLAYER_KATANAS = new HashMap<>();

    // Server-side blocking system
    private static final Map<UUID, Long> BLOCKED_UNTIL = new HashMap<>();
    private static final long BLOCK_TICKS = 25; // 1.25 seconds

    // Packet IDs
    private static final ResourceLocation LEFT_CLICK_ID = new ResourceLocation("nichirin", "katana_left");
    private static final ResourceLocation RIGHT_CLICK_ID = new ResourceLocation("nichirin", "katana_right");
    private static final ResourceLocation RIGHT_CROUCH_ID = new ResourceLocation("nichirin", "katana_right_crouch");
    private static final ResourceLocation FEEDBACK_ID = new ResourceLocation("nichirin", "katana_feedback");

    /**
     * FIXED: Only register server-side stuff here - NO CLIENT CODE
     */
    public static void register() {
        // Only register server packets and shared events
        registerServerPackets();
        registerServerEvents();
    }

    /**
     * CLIENT-ONLY: Call this from client initialization only
     */
    public static void registerClient() {
        if (Platform.getEnv() == EnvType.CLIENT) {
            try {
                // Use reflection to avoid loading client classes on server
                Class<?> clientHandlerClass = Class.forName("com.xirc.nichirin.client.util.KatanaClientHandler");
                clientHandlerClass.getMethod("registerClientEvents").invoke(null);
            } catch (Exception e) {
                System.err.println("Failed to register client katana events: " + e.getMessage());
            }
        }
    }

    /**
     * CLIENT-ONLY METHODS REMOVED - moved to separate client handler
     * This eliminates client imports that cause server crashes
     */

    /**
     * SERVER-ONLY: Register server packet handlers
     */
    private static void registerServerPackets() {
        // Left click
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, LEFT_CLICK_ID, (buf, context) -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player != null) {
                context.queue(() -> handleServerLeftClick(player));
            }
        });

        // Right click
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, RIGHT_CLICK_ID, (buf, context) -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player != null) {
                context.queue(() -> handleServerRightClick(player, false));
            }
        });

        // Right click crouch
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, RIGHT_CROUCH_ID, (buf, context) -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player != null) {
                context.queue(() -> handleServerRightClick(player, true));
            }
        });
    }

    /**
     * CLIENT PACKET HANDLERS REMOVED - moved to separate client handler
     */

    /**
     * SERVER-ONLY: Handle left click on server
     */
    private static void handleServerLeftClick(ServerPlayer player) {
        if (isServerBlocked(player)) {
            return;
        }

        ItemStack item = player.getMainHandItem();
        if (item.getItem() instanceof SimpleKatana katana) {
            SimpleKatana instance = getKatanaInstance(player, katana);
            instance.performAttack(player);
        }
    }

    /**
     * SERVER-ONLY: Handle right click on server
     */
    private static void handleServerRightClick(ServerPlayer player, boolean crouch) {
        if (isServerBlocked(player)) {
            return;
        }

        ItemStack item = player.getMainHandItem();
        if (item.getItem() instanceof SimpleKatana katana) {
            SimpleKatana instance = getKatanaInstance(player, katana);

            // Set crouch state temporarily
            boolean originalCrouch = player.isShiftKeyDown();
            if (crouch != originalCrouch) {
                player.setShiftKeyDown(crouch);
            }

            // Get move name for feedback
            String moveName = null;
            var moveset = BreathingStyleHelper.getMoveset(player);
            if (moveset != null) {
                if (crouch) {
                    moveName = moveset.getCrouchRightClickMoveName();
                    if (moveName != null && moveName.contains("Thunder Clap")) {
                        ThunderClapFlashAttack.setCrouchDash(player, true);
                    }
                } else {
                    moveName = moveset.getRightClickMoveName();
                }
            }

            // Execute
            instance.use(player.level(), player, InteractionHand.MAIN_HAND);

            // Send feedback
            sendFeedback(player, moveName, crouch);

            // Restore crouch state
            if (crouch != originalCrouch) {
                player.setShiftKeyDown(originalCrouch);
            }
        }
    }

    /**
     * SERVER-ONLY: Check if player inputs are blocked
     */
    private static boolean isServerBlocked(Player player) {
        Long blockedUntil = BLOCKED_UNTIL.get(player.getUUID());
        if (blockedUntil != null) {
            long currentTime = player.level().getGameTime();
            if (currentTime < blockedUntil) {
                return true;
            } else {
                BLOCKED_UNTIL.remove(player.getUUID());
            }
        }

        // Add blocking check - can't attack while blocking
        if (player.hasEffect(NichirinEffectRegistry.BLOCKING.get())) {
            return true;
        }

        return false;
    }

    /**
     * SERVER-ONLY: Send feedback to client
     */
    private static void sendFeedback(ServerPlayer player, String moveName, boolean crouch) {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeBoolean(moveName != null);

            if (moveName != null) {
                buf.writeUtf(moveName);

                int cooldown = 30;
                if (moveName.contains("Thunder Clap")) {
                    cooldown = 30;
                } else if (moveName.contains("Heat Lightning")) {
                    cooldown = 40;
                }
                buf.writeInt(cooldown);
            } else {
                buf.writeBoolean(crouch);
            }

            NetworkManager.sendToPlayer(player, FEEDBACK_ID, buf);
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * SERVER-ONLY: Register server events (no client code)
     */
    private static void registerServerEvents() {
        // Player ticking - SERVER ONLY
        TickEvent.PLAYER_POST.register(player -> {
            if (!player.level().isClientSide) {
                tickPlayer(player);
            }
        });

        // Cleanup - SERVER ONLY
        PlayerEvent.PLAYER_QUIT.register(player -> {
            if (!player.level().isClientSide) {
                cleanupPlayer(player);
            }
        });
    }

    /**
     * SERVER-ONLY: Tick player katana
     */
    private static void tickPlayer(Player player) {
        SimpleKatana katana = PLAYER_KATANAS.get(player.getUUID());
        if (katana != null) {
            ItemStack mainHand = player.getMainHandItem();
            if (mainHand.getItem() instanceof SimpleKatana) {
                katana.tick(player);
            } else {
                PLAYER_KATANAS.remove(player.getUUID());
            }
        }
    }

    /**
     * SERVER-ONLY: Get or create katana instance
     */
    private static SimpleKatana getKatanaInstance(Player player, SimpleKatana item) {
        UUID id = player.getUUID();
        SimpleKatana existing = PLAYER_KATANAS.get(id);

        if (existing == null || existing != item) {
            PLAYER_KATANAS.put(id, item);
            return item;
        }

        return existing;
    }

    /**
     * SERVER-ONLY: Cleanup player data
     */
    public static void cleanupPlayer(Player player) {
        UUID id = player.getUUID();
        PLAYER_KATANAS.remove(id);
        BLOCKED_UNTIL.remove(id);
    }

    /**
     * SERVER-ONLY: Block katana inputs after breathing move execution
     */
    public static void blockAfterBreathingMove(Player player) {
        if (!player.level().isClientSide) {
            long blockUntil = player.level().getGameTime() + BLOCK_TICKS;
            BLOCKED_UNTIL.put(player.getUUID(), blockUntil);
        }
    }
}