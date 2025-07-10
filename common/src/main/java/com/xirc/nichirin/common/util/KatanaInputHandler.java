package com.xirc.nichirin.common.util;

import com.xirc.nichirin.client.gui.CooldownHUD;
import com.xirc.nichirin.common.attack.moves.thunder.ThunderClapFlashAttack;
import com.xirc.nichirin.common.data.BreathingStyleHelper;
import com.xirc.nichirin.common.item.katana.SimpleKatana;
import com.xirc.nichirin.common.util.AnimationUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.client.Minecraft;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.event.EventResult;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles input for katana attacks
 */
public class KatanaInputHandler {

    // Store SimpleKatana instances per player for tracking
    private static final Map<UUID, SimpleKatana> PLAYER_SIMPLE_KATANAS = new HashMap<>();

    // Network packet IDs
    private static final ResourceLocation LEFT_CLICK_PACKET = new ResourceLocation("nichirin", "left_click");
    private static final ResourceLocation RIGHT_CLICK_PACKET = new ResourceLocation("nichirin", "right_click");
    private static final ResourceLocation RIGHT_CLICK_CROUCH_PACKET = new ResourceLocation("nichirin", "right_click_crouch");

    public static void register() {

        // CLIENT SIDE: Detect left clicks and send to server
        if (isClientSide()) {
            // For left click in air (client only)
            InteractionEvent.CLIENT_LEFT_CLICK_AIR.register((player, hand) -> {
                ItemStack heldItem = player.getItemInHand(hand);
                if (heldItem.getItem() instanceof SimpleKatana) {
                    sendLeftClickToServer(player);
                }
            });

            // For right click with item (client only)
            InteractionEvent.CLIENT_RIGHT_CLICK_AIR.register((player, hand) -> {
                ItemStack heldItem = player.getItemInHand(hand);
                if (heldItem.getItem() instanceof SimpleKatana) {
                    sendRightClickToServer(player);
                }
            });
        }

        // SERVER SIDE: Handle the packet
        registerServerPacketHandler();

        // For entity attacks - this runs on both sides
        PlayerEvent.ATTACK_ENTITY.register((player, level, entity, hand, hitResult) -> {
            ItemStack heldItem = player.getItemInHand(hand);

            // If it's a katana, handle our custom attack
            if (heldItem.getItem() instanceof SimpleKatana) {
                if (!level.isClientSide) {
                    // Server side - perform the attack
                    handleLeftClick(player);
                } else {
                    // Client side - send packet to server
                    sendLeftClickToServer(player);
                }
                return EventResult.interruptFalse(); // Prevent vanilla attack
            }

            return EventResult.pass();
        });

        // Register player tick event to update katanas (runs on both sides)
        TickEvent.PLAYER_POST.register(player -> {
            if (!player.level().isClientSide) {
                tickPlayer(player);
            }
        });

        // Clean up when player leaves
        PlayerEvent.PLAYER_QUIT.register(player -> {
            if (!player.level().isClientSide) {
                cleanupPlayer(player);
            }
        });
    }

    /**
     * Check if we're on client side (careful with side-specific code)
     */
    public static boolean isClientSide() {
        try {
            // This will only work on client
            Class.forName("net.minecraft.client.Minecraft");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Send left click packet to server (CLIENT ONLY)
     */
    private static void sendLeftClickToServer(Player player) {
        // Handle client-side visual feedback
        if (isClientSide()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                ItemStack heldItem = minecraft.player.getMainHandItem();
                if (heldItem.getItem() instanceof SimpleKatana simpleKatana) {
                    // Use existing displayClientCooldown method
                    simpleKatana.displayClientCooldown(minecraft.player);
                }
            }
        }

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        // No data needed, just the packet itself
        NetworkManager.sendToServer(LEFT_CLICK_PACKET, buf);
    }

    /**
     * Send right click packet to server (CLIENT ONLY)
     */
    private static void sendRightClickToServer(Player player) {
        boolean isCrouching = player.isCrouching();
        ResourceLocation packetId = isCrouching ? RIGHT_CLICK_CROUCH_PACKET : RIGHT_CLICK_PACKET;

        // DON'T display any client feedback here - let the server decide what happens

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        NetworkManager.sendToServer(packetId, buf);
    }

    /**
     * Register server packet handler
     */
    private static void registerServerPacketHandler() {
        // Left click handler
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, LEFT_CLICK_PACKET, (buf, context) -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player != null) {
                // Schedule on main thread
                context.queue(() -> {
                    handleLeftClick(player);
                });
            }
        });

        // Right click handler (normal)
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, RIGHT_CLICK_PACKET, (buf, context) -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player != null) {
                context.queue(() -> {
                    handleRightClick(player, false);
                });
            }
        });

        // Right click handler (crouching)
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, RIGHT_CLICK_CROUCH_PACKET, (buf, context) -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player != null) {
                context.queue(() -> {
                    handleRightClick(player, true);
                });
            }
        });
    }

    private static void handleLeftClick(Player player) {
        // Make sure we're on server side
        if (player.level().isClientSide) {
            return;
        }

        ItemStack heldItem = player.getMainHandItem();

        // Handle SimpleKatana
        if (heldItem.getItem() instanceof SimpleKatana simpleKatana) {

            // Get or create instance tracker for this player
            SimpleKatana katanaInstance = getSimpleKatanaForPlayer(player, simpleKatana);
            // Use existing performAttack method
            katanaInstance.performAttack(player);
        }
    }

    private static void handleRightClick(Player player, boolean isCrouching) {
        // Make sure we're on server side
        if (player.level().isClientSide) {
            return;
        }

        ItemStack heldItem = player.getMainHandItem();

        // Handle SimpleKatana
        if (heldItem.getItem() instanceof SimpleKatana simpleKatana) {

            // Get or create instance tracker for this player
            SimpleKatana katanaInstance = getSimpleKatanaForPlayer(player, simpleKatana);

            // Create a fake ItemStack and InteractionHand to simulate the use() call
            ItemStack itemStack = player.getMainHandItem();
            InteractionHand hand = InteractionHand.MAIN_HAND;

            // Temporarily set crouch state if needed for the use() method
            boolean originalCrouchState = player.isShiftKeyDown();
            if (isCrouching != originalCrouchState) {
                player.setShiftKeyDown(isCrouching);
            }

            // First check if the player has a breathing style that will handle this
            String moveUsed = null;
            var moveset = BreathingStyleHelper.getMoveset(player);
            if (moveset != null) {
                // Store which move will be used before calling use()
                if (isCrouching) {
                    moveUsed = moveset.getCrouchRightClickMoveName();
                } else {
                    moveUsed = moveset.getRightClickMoveName();
                }
            }

            // Call the existing use() method which handles both breathing moves and default moves
            katanaInstance.use(player.level(), player, hand);

            // Send feedback to client about which move was used
            if (player instanceof ServerPlayer serverPlayer) {
                sendMoveUsedFeedback(serverPlayer, moveUsed, isCrouching);
            }

            // Restore original crouch state
            if (isCrouching != originalCrouchState) {
                player.setShiftKeyDown(originalCrouchState);
            }
        }
    }

    /**
     * Send feedback to client about which move was actually used
     */
    private static void sendMoveUsedFeedback(ServerPlayer player, String moveName, boolean isCrouching) {
        // Create a packet to tell the client which move was used
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBoolean(moveName != null); // Has breathing style move
        if (moveName != null) {
            buf.writeUtf(moveName);

            // Get cooldown from the move if it's Thunder Breathing
            int cooldown = 30; // Default cooldown
            if (moveName.contains("Thunder Clap")) {
                cooldown = 30; // Thunder Clap and Flash cooldown
            } else if (moveName.contains("Heat Lightning")) {
                cooldown = 40; // Heat Lightning cooldown
            }
            // Add more moves as needed

            buf.writeInt(cooldown);
        } else {
            buf.writeBoolean(isCrouching); // For default moves
        }

        NetworkManager.sendToPlayer(player, new ResourceLocation("nichirin", "move_feedback"), buf);
    }

    /**
     * Register client-side feedback handler
     */
    static {
        if (isClientSide()) {
            NetworkManager.registerReceiver(NetworkManager.Side.S2C, new ResourceLocation("nichirin", "move_feedback"), (buf, context) -> {
                boolean hasBreathingMove = buf.readBoolean();

                context.queue(() -> {
                    if (hasBreathingMove) {
                        String moveName = buf.readUtf();
                        int cooldown = buf.readInt();

                        // Display the actual move that was used
                        CooldownHUD.setCooldown(moveName, cooldown);

                        // Also play appropriate animation if needed
                        if (moveName.contains("Thunder Clap")) {
                            AnimationUtils.playAnimation(Minecraft.getInstance().player, "thunder_clap_flash");
                        } else if (moveName.contains("Heat Lightning")) {
                            AnimationUtils.playAnimation(Minecraft.getInstance().player, "heat_lightning");
                        }
                        // Add more animation mappings as needed
                    } else {
                        boolean wasCrouching = buf.readBoolean();

                        // Default moves
                        if (wasCrouching) {
                            AnimationUtils.playAnimation(Minecraft.getInstance().player, "rising_slash");
                            CooldownHUD.setCooldown("Rising Slash", 25);
                        } else {
                            AnimationUtils.playAnimation(Minecraft.getInstance().player, "double_slash");
                            CooldownHUD.setCooldown("Double Slash", 20);
                        }
                    }
                });
            });
        }
    }

    /**
     * Tick all katanas for the player (SERVER ONLY)
     */
    private static void tickPlayer(Player player) {
        // Tick SimpleKatana if player has one
        SimpleKatana katana = PLAYER_SIMPLE_KATANAS.get(player.getUUID());
        if (katana != null) {
            // Check if player still has the katana
            ItemStack mainHand = player.getMainHandItem();
            if (mainHand.getItem() instanceof SimpleKatana) {
                katana.tick(player);
            } else {
                // Player no longer holding katana, remove from map
                PLAYER_SIMPLE_KATANAS.remove(player.getUUID());
            }
        }
    }

    /**
     * Gets or stores a SimpleKatana instance for tracking per-player state
     */
    private static SimpleKatana getSimpleKatanaForPlayer(Player player, SimpleKatana itemKatana) {
        UUID playerId = player.getUUID();
        SimpleKatana katana = PLAYER_SIMPLE_KATANAS.get(playerId);

        // If no katana tracked or it's a different one, use the item's instance
        if (katana == null || katana != itemKatana) {
            PLAYER_SIMPLE_KATANAS.put(playerId, itemKatana);
            return itemKatana;
        }

        return katana;
    }

    /**
     * Cleans up when player leaves
     */
    public static void cleanupPlayer(Player player) {
        PLAYER_SIMPLE_KATANAS.remove(player.getUUID());
    }
}