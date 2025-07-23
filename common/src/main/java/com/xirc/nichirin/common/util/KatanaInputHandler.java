package com.xirc.nichirin.common.util;

import com.xirc.nichirin.client.gui.CooldownHUD;
import com.xirc.nichirin.client.handler.AttackWheelHandler;
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
            // SIMPLE BLOCKING: Check AttackWheelHandler directly
            InteractionEvent.CLIENT_LEFT_CLICK_AIR.register((player, hand) -> {
                ItemStack heldItem = player.getItemInHand(hand);
                if (heldItem.getItem() instanceof SimpleKatana) {
                    // DIRECT CHECK - NO COMPLEX SYSTEMS
                    if (AttackWheelHandler.shouldBlockKatanaAttacks()) {
                        System.out.println("DEBUG: BLOCKED CLIENT_LEFT_CLICK_AIR - wheel is open");
                        return; // COMPLETE BLOCK
                    }
                    System.out.println("DEBUG: ALLOWING CLIENT_LEFT_CLICK_AIR - wheel is closed");
                    sendLeftClickToServer(player);
                }
            });

            InteractionEvent.CLIENT_RIGHT_CLICK_AIR.register((player, hand) -> {
                ItemStack heldItem = player.getItemInHand(hand);
                if (heldItem.getItem() instanceof SimpleKatana) {
                    if (AttackWheelHandler.shouldBlockKatanaAttacks()) {
                        System.out.println("DEBUG: BLOCKED CLIENT_RIGHT_CLICK_AIR - wheel is open");
                        return;
                    }
                    sendRightClickToServer(player);
                }
            });
        }

        // SERVER SIDE: Handle the packet
        registerServerPacketHandler();

        // For entity attacks - BLOCK ON BOTH SIDES
        PlayerEvent.ATTACK_ENTITY.register((player, level, entity, hand, hitResult) -> {
            ItemStack heldItem = player.getItemInHand(hand);

            if (heldItem.getItem() instanceof SimpleKatana) {
                // DIRECT CHECK ON BOTH CLIENT AND SERVER
                if (AttackWheelHandler.shouldBlockKatanaAttacks()) {
                    System.out.println("DEBUG: BLOCKED ATTACK_ENTITY - wheel is open - Side: " + (level.isClientSide ? "CLIENT" : "SERVER"));
                    return EventResult.interruptFalse(); // COMPLETE BLOCK
                }

                System.out.println("DEBUG: ALLOWING ATTACK_ENTITY - wheel is closed - Side: " + (level.isClientSide ? "CLIENT" : "SERVER"));
                if (!level.isClientSide) {
                    handleLeftClick(player);
                } else {
                    sendLeftClickToServer(player);
                }
                return EventResult.interruptFalse(); // Always prevent vanilla attack
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
                    simpleKatana.displayClientCooldown(minecraft.player);
                }
            }
        }

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        NetworkManager.sendToServer(LEFT_CLICK_PACKET, buf);
        System.out.println("DEBUG: Left click packet SENT to server");
    }

    /**
     * Send right click packet to server (CLIENT ONLY)
     */
    private static void sendRightClickToServer(Player player) {
        boolean isCrouching = player.isCrouching();
        ResourceLocation packetId = isCrouching ? RIGHT_CLICK_CROUCH_PACKET : RIGHT_CLICK_PACKET;

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        NetworkManager.sendToServer(packetId, buf);
    }

    /**
     * Register server packet handler
     */
    private static void registerServerPacketHandler() {
        // Left click handler - CHECK WHEEL STATE ON SERVER TOO
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, LEFT_CLICK_PACKET, (buf, context) -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player != null) {
                context.queue(() -> {
                    // DOUBLE CHECK ON SERVER SIDE TOO
                    if (AttackWheelHandler.shouldBlockKatanaAttacks()) {
                        System.out.println("DEBUG: BLOCKED server left click packet - wheel is open");
                        return;
                    }
                    System.out.println("DEBUG: PROCESSING server left click packet - wheel is closed");
                    handleLeftClick(player);
                });
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, RIGHT_CLICK_PACKET, (buf, context) -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player != null) {
                context.queue(() -> {
                    if (AttackWheelHandler.shouldBlockKatanaAttacks()) {
                        System.out.println("DEBUG: BLOCKED server right click packet - wheel is open");
                        return;
                    }
                    handleRightClick(player, false);
                });
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, RIGHT_CLICK_CROUCH_PACKET, (buf, context) -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player != null) {
                context.queue(() -> {
                    if (AttackWheelHandler.shouldBlockKatanaAttacks()) {
                        System.out.println("DEBUG: BLOCKED server right click crouch packet - wheel is open");
                        return;
                    }
                    handleRightClick(player, true);
                });
            }
        });
    }

    private static void handleLeftClick(Player player) {
        // TRIPLE CHECK - Even in the handle method itself
        if (AttackWheelHandler.shouldBlockKatanaAttacks()) {
            System.out.println("DEBUG: BLOCKED handleLeftClick - wheel is open");
            return;
        }

        // Make sure we're on server side
        if (player.level().isClientSide) {
            return;
        }

        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.getItem() instanceof SimpleKatana simpleKatana) {
            SimpleKatana katanaInstance = getSimpleKatanaForPlayer(player, simpleKatana);
            katanaInstance.performAttack(player);
            System.out.println("DEBUG: Katana attack EXECUTED on server");
        }
    }

    private static void handleRightClick(Player player, boolean isCrouching) {
        // Make sure we're on server side
        if (player.level().isClientSide) {
            return;
        }

        ItemStack heldItem = player.getMainHandItem();

        if (heldItem.getItem() instanceof SimpleKatana simpleKatana) {
            SimpleKatana katanaInstance = getSimpleKatanaForPlayer(player, simpleKatana);
            ItemStack itemStack = player.getMainHandItem();
            InteractionHand hand = InteractionHand.MAIN_HAND;

            boolean originalCrouchState = player.isShiftKeyDown();
            if (isCrouching != originalCrouchState) {
                player.setShiftKeyDown(isCrouching);
            }

            String moveUsed = null;
            var moveset = BreathingStyleHelper.getMoveset(player);
            if (moveset != null) {
                if (isCrouching) {
                    moveUsed = moveset.getCrouchRightClickMoveName();
                } else {
                    moveUsed = moveset.getRightClickMoveName();
                }
            }

            katanaInstance.use(player.level(), player, hand);

            if (player instanceof ServerPlayer serverPlayer) {
                sendMoveUsedFeedback(serverPlayer, moveUsed, isCrouching);
            }

            if (isCrouching != originalCrouchState) {
                player.setShiftKeyDown(originalCrouchState);
            }
        }
    }

    private static void sendMoveUsedFeedback(ServerPlayer player, String moveName, boolean isCrouching) {
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
            buf.writeBoolean(isCrouching);
        }

        NetworkManager.sendToPlayer(player, new ResourceLocation("nichirin", "move_feedback"), buf);
    }

    static {
        if (isClientSide()) {
            NetworkManager.registerReceiver(NetworkManager.Side.S2C, new ResourceLocation("nichirin", "move_feedback"), (buf, context) -> {
                boolean hasBreathingMove = buf.readBoolean();

                context.queue(() -> {
                    if (hasBreathingMove) {
                        String moveName = buf.readUtf();
                        int cooldown = buf.readInt();

                        CooldownHUD.setCooldown(moveName, cooldown);

                        if (moveName.contains("Thunder Clap")) {
                            AnimationUtils.playAnimation(Minecraft.getInstance().player, "thunder_clap_flash");
                        } else if (moveName.contains("Heat Lightning")) {
                            AnimationUtils.playAnimation(Minecraft.getInstance().player, "heat_lightning");
                        }
                    } else {
                        boolean wasCrouching = buf.readBoolean();

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

    private static void tickPlayer(Player player) {
        SimpleKatana katana = PLAYER_SIMPLE_KATANAS.get(player.getUUID());
        if (katana != null) {
            ItemStack mainHand = player.getMainHandItem();
            if (mainHand.getItem() instanceof SimpleKatana) {
                katana.tick(player);
            } else {
                PLAYER_SIMPLE_KATANAS.remove(player.getUUID());
            }
        }
    }

    private static SimpleKatana getSimpleKatanaForPlayer(Player player, SimpleKatana itemKatana) {
        UUID playerId = player.getUUID();
        SimpleKatana katana = PLAYER_SIMPLE_KATANAS.get(playerId);

        if (katana == null || katana != itemKatana) {
            PLAYER_SIMPLE_KATANAS.put(playerId, itemKatana);
            return itemKatana;
        }

        return katana;
    }

    public static void cleanupPlayer(Player player) {
        PLAYER_SIMPLE_KATANAS.remove(player.getUUID());
    }
}