package com.xirc.nichirin.common.util;

import com.xirc.nichirin.client.gui.CooldownHUD;
import com.xirc.nichirin.common.attack.moves.thunder.ThunderClapFlashAttack;
import com.xirc.nichirin.common.data.BreathingStyleHelper;
import com.xirc.nichirin.common.item.katana.SimpleKatana;
import com.xirc.nichirin.common.system.blocking.KatanaBlock;
import com.xirc.nichirin.common.util.AnimationUtils;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
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
 * Enhanced katana input handler with debug logging
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

    public static void register() {
        // Always register server packets and shared events
        System.out.println("DEBUG: KatanaInputHandler.register() - registering server-side components");
        registerServerPackets();
        registerSharedEvents();
        System.out.println("DEBUG: KatanaInputHandler.register() - server-side registration complete");
    }

    // Client-specific registration
    public static void registerClient() {
        System.out.println("DEBUG: KatanaInputHandler.registerClient() called");

        if (Platform.getEnv() == EnvType.CLIENT) {
            System.out.println("DEBUG: Environment is CLIENT - proceeding with client registration");
            try {
                registerClientEvents();
                System.out.println("DEBUG: Client events registered successfully");

                registerClientPackets();
                System.out.println("DEBUG: Client packets registered successfully");

                System.out.println("DEBUG: KatanaInputHandler client registration COMPLETE");
            } catch (Exception e) {
                System.out.println("ERROR: Failed to register KatanaInputHandler client: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("DEBUG: Environment is NOT CLIENT - skipping client registration");
        }
    }

    private static void registerClientEvents() {
        System.out.println("DEBUG: Registering client events for KatanaInputHandler");

        // Left click air
        InteractionEvent.CLIENT_LEFT_CLICK_AIR.register((player, hand) -> {
            ItemStack item = player.getItemInHand(hand);
            System.out.println("DEBUG: CLIENT_LEFT_CLICK_AIR triggered - item=" +
                    (item.getItem() instanceof SimpleKatana ? "SimpleKatana" : item.getItem().getClass().getSimpleName()));

            if (!(item.getItem() instanceof SimpleKatana)) {
                System.out.println("DEBUG: Not a katana, ignoring left click air");
                return;
            }

            // ENHANCED BLOCKING CHECK
            if (isInputBlocked()) {
                System.out.println("DEBUG: Left click air BLOCKED by input handler");
                return;
            }

            System.out.println("DEBUG: Left click air ALLOWED - sending to server");
            sendLeftClick(player);
        });

        // Right click air
        InteractionEvent.CLIENT_RIGHT_CLICK_AIR.register((player, hand) -> {
            ItemStack item = player.getItemInHand(hand);
            System.out.println("DEBUG: CLIENT_RIGHT_CLICK_AIR triggered - item=" +
                    (item.getItem() instanceof SimpleKatana ? "SimpleKatana" : item.getItem().getClass().getSimpleName()));

            if (!(item.getItem() instanceof SimpleKatana)) {
                System.out.println("DEBUG: Not a katana, ignoring right click air");
                return;
            }

            // ENHANCED BLOCKING CHECK (also block right clicks if needed)
            if (isInputBlocked()) {
                System.out.println("DEBUG: Right click air BLOCKED by input handler");
                return;
            }

            System.out.println("DEBUG: Right click air ALLOWED - sending to server");
            sendRightClick(player);
        });

        System.out.println("DEBUG: Client events registration complete");
    }

    /**
     * ENHANCED: Comprehensive input blocking check - CLIENT ONLY
     */
    private static boolean isInputBlocked() {
        try {
            Minecraft mc = Minecraft.getInstance();

            // Check if player has blocking effect - BLOCK ALL INPUTS
            if (mc.player != null && mc.player.hasEffect(NichirinEffectRegistry.BLOCKING.get())) {
                System.out.println("DEBUG: Input BLOCKED - player has BLOCKING effect");
                return true;
            }

            // Check wheel state first (most important)
            try {
                if (com.xirc.nichirin.client.handler.AttackWheelHandler.shouldBlockAttackInputs()) {
                    System.out.println("DEBUG: Input BLOCKED - attack wheel is blocking");
                    return true;
                }
            } catch (Exception e) {
                System.out.println("WARNING: Could not check wheel blocking state: " + e.getMessage());
            }

            // Check multiplayer input handler
            try {
                if (MultiplayerInputHandler.shouldBlockInputsClient()) {
                    System.out.println("DEBUG: Input BLOCKED - multiplayer input handler blocking");
                    return true;
                }
            } catch (Exception e) {
                System.out.println("WARNING: Could not check multiplayer input blocking: " + e.getMessage());
            }

            System.out.println("DEBUG: Input NOT BLOCKED - all checks passed");
            return false;

        } catch (Exception e) {
            System.out.println("ERROR: Exception in isInputBlocked(): " + e.getMessage());
            return false;
        }
    }

    private static void sendLeftClick(Player player) {
        System.out.println("DEBUG: sendLeftClick() called for player " + player.getName().getString());

        // Client feedback
        try {
            if (player.getMainHandItem().getItem() instanceof SimpleKatana katana) {
                katana.displayClientCooldown(player);
                System.out.println("DEBUG: Displayed client cooldown");
            }
        } catch (Exception e) {
            System.out.println("WARNING: Could not display client cooldown: " + e.getMessage());
        }

        // Send to server
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            NetworkManager.sendToServer(LEFT_CLICK_ID, buf);
            System.out.println("DEBUG: LEFT_CLICK packet sent to server successfully");
        } catch (Exception e) {
            System.out.println("ERROR: Failed to send LEFT_CLICK packet: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void sendRightClick(Player player) {
        boolean crouch = player.isCrouching();
        ResourceLocation id = crouch ? RIGHT_CROUCH_ID : RIGHT_CLICK_ID;

        System.out.println("DEBUG: sendRightClick() called - crouch=" + crouch + ", packet=" + id);

        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            NetworkManager.sendToServer(id, buf);
            System.out.println("DEBUG: RIGHT_CLICK packet sent to server successfully");
        } catch (Exception e) {
            System.out.println("ERROR: Failed to send RIGHT_CLICK packet: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void registerServerPackets() {
        System.out.println("DEBUG: Registering server packets for KatanaInputHandler");

        // Left click
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, LEFT_CLICK_ID, (buf, context) -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player != null) {
                System.out.println("DEBUG: Received LEFT_CLICK packet from " + player.getName().getString());
                context.queue(() -> handleServerLeftClick(player));
            }
        });

        // Right click
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, RIGHT_CLICK_ID, (buf, context) -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player != null) {
                System.out.println("DEBUG: Received RIGHT_CLICK packet from " + player.getName().getString());
                context.queue(() -> handleServerRightClick(player, false));
            }
        });

        // Right click crouch
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, RIGHT_CROUCH_ID, (buf, context) -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player != null) {
                System.out.println("DEBUG: Received RIGHT_CROUCH packet from " + player.getName().getString());
                context.queue(() -> handleServerRightClick(player, true));
            }
        });

        System.out.println("DEBUG: Server packets registration complete");
    }

    private static void registerClientPackets() {
        System.out.println("DEBUG: Registering client packets for KatanaInputHandler");

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, FEEDBACK_ID, (buf, context) -> {
            boolean hasBreathingMove = buf.readBoolean();
            System.out.println("DEBUG: Received FEEDBACK packet - hasBreathingMove=" + hasBreathingMove);

            context.queue(() -> {
                try {
                    if (hasBreathingMove) {
                        String moveName = buf.readUtf();
                        int cooldown = buf.readInt();
                        System.out.println("DEBUG: Processing breathing move feedback - " + moveName + " cooldown=" + cooldown);

                        CooldownHUD.setCooldown(moveName, cooldown);

                        if (moveName.contains("Thunder Clap")) {
                            AnimationUtils.playAnimation(Minecraft.getInstance().player, "thunder_clap_flash");
                        } else if (moveName.contains("Heat Lightning")) {
                            AnimationUtils.playAnimation(Minecraft.getInstance().player, "heat_lightning");
                        }
                    } else {
                        boolean wasCrouching = buf.readBoolean();
                        System.out.println("DEBUG: Processing regular attack feedback - wasCrouching=" + wasCrouching);

                        if (wasCrouching) {
                            AnimationUtils.playAnimation(Minecraft.getInstance().player, "rising_slash");
                            CooldownHUD.setCooldown("Rising Slash", 25);
                        } else {
                            AnimationUtils.playAnimation(Minecraft.getInstance().player, "double_slash");
                            CooldownHUD.setCooldown("Double Slash", 20);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("ERROR: Failed to process feedback packet: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        });

        System.out.println("DEBUG: Client packets registration complete");
    }

    private static void handleServerLeftClick(ServerPlayer player) {
        System.out.println("DEBUG: handleServerLeftClick() for " + player.getName().getString());

        if (isServerBlocked(player)) {
            System.out.println("DEBUG: Server left click BLOCKED for player " + player.getName().getString());
            return;
        }

        ItemStack item = player.getMainHandItem();
        if (item.getItem() instanceof SimpleKatana katana) {
            System.out.println("DEBUG: Executing performAttack() for " + player.getName().getString());
            SimpleKatana instance = getKatanaInstance(player, katana);
            instance.performAttack(player);
            System.out.println("DEBUG: performAttack() completed for " + player.getName().getString());
        } else {
            System.out.println("DEBUG: Player " + player.getName().getString() + " not holding katana");
        }
    }

    private static void handleServerRightClick(ServerPlayer player, boolean crouch) {
        System.out.println("DEBUG: handleServerRightClick() for " + player.getName().getString() + " crouch=" + crouch);

        if (isServerBlocked(player)) {
            System.out.println("DEBUG: Server right click BLOCKED for player " + player.getName().getString());
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

            System.out.println("DEBUG: Server right click executed for " + player.getName().getString());
        } else {
            System.out.println("DEBUG: Player " + player.getName().getString() + " not holding katana for right click");
        }
    }

    private static boolean isServerBlocked(Player player) {
        Long blockedUntil = BLOCKED_UNTIL.get(player.getUUID());
        if (blockedUntil != null) {
            long currentTime = player.level().getGameTime();
            if (currentTime < blockedUntil) {
                System.out.println("DEBUG: Player " + player.getName().getString() + " server blocked until " + blockedUntil);
                return true;
            } else {
                BLOCKED_UNTIL.remove(player.getUUID());
            }
        }

        // Add blocking check - can't attack while blocking
        if (player.hasEffect(NichirinEffectRegistry.BLOCKING.get())) {
            System.out.println("DEBUG: Player " + player.getName().getString() + " server blocked due to BLOCKING effect");
            return true;
        }

        return false;
    }

    private static void sendFeedback(ServerPlayer player, String moveName, boolean crouch) {
        System.out.println("DEBUG: Sending feedback to " + player.getName().getString() + " - move=" + moveName);

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
            System.out.println("DEBUG: Feedback sent successfully");
        } catch (Exception e) {
            System.out.println("ERROR: Failed to send feedback: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void registerSharedEvents() {
        System.out.println("DEBUG: Registering shared events for KatanaInputHandler");

        // Entity attack blocking
        PlayerEvent.ATTACK_ENTITY.register((player, level, entity, hand, hitResult) -> {
            ItemStack item = player.getItemInHand(hand);
            if (!(item.getItem() instanceof SimpleKatana)) {
                return EventResult.pass();
            }

            if (level.isClientSide) {
                System.out.println("DEBUG: ATTACK_ENTITY triggered on client side");

                // ENHANCED BLOCKING CHECK for entity attacks too
                if (isInputBlocked()) {
                    System.out.println("DEBUG: Entity attack BLOCKED - wheel state or input handler");
                    return EventResult.interruptFalse();
                }

                System.out.println("DEBUG: Entity attack ALLOWED - sending left click");
                sendLeftClick(player);
            } else {
                System.out.println("DEBUG: ATTACK_ENTITY triggered on server side");
            }

            return EventResult.interruptFalse();
        });

        // Player ticking
        TickEvent.PLAYER_POST.register(player -> {
            if (!player.level().isClientSide) {
                tickPlayer(player);
            }
        });

        // Cleanup
        PlayerEvent.PLAYER_QUIT.register(player -> {
            if (!player.level().isClientSide) {
                cleanupPlayer(player);
            }
        });

        System.out.println("DEBUG: Shared events registration complete");
    }

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

    private static SimpleKatana getKatanaInstance(Player player, SimpleKatana item) {
        UUID id = player.getUUID();
        SimpleKatana existing = PLAYER_KATANAS.get(id);

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
    }

    /**
     * Block katana inputs after breathing move execution
     */
    public static void blockAfterBreathingMove(Player player) {
        if (!player.level().isClientSide) {
            long blockUntil = player.level().getGameTime() + BLOCK_TICKS;
            BLOCKED_UNTIL.put(player.getUUID(), blockUntil);
            System.out.println("DEBUG: Blocked inputs for player " + player.getName().getString() + " after breathing move until " + blockUntil);
        }
    }
}