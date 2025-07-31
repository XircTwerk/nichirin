package com.xirc.nichirin.common.util;

import com.xirc.nichirin.common.attack.moves.thunder.ThunderClapFlashAttack;
import com.xirc.nichirin.common.data.BreathingStyleHelper;
import com.xirc.nichirin.common.item.katana.SimpleKatana;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * FIXED: Server-safe katana input handler
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
        registerServerPackets();
        registerSharedEvents();

        System.out.println("DEBUG: KatanaInputHandler - registered server-side components");
    }

    // MOVED TO CLIENT INIT: Client-specific registration should be done in BreathOfNichirinClient
    public static void registerClient() {
        if (Platform.getEnv() == EnvType.CLIENT) {
            registerClientEvents();
            registerClientPackets();
            System.out.println("DEBUG: KatanaInputHandler - registered client-side components");
        }
    }

    private static void registerClientEvents() {
        // Left click air
        InteractionEvent.CLIENT_LEFT_CLICK_AIR.register((player, hand) -> {
            ItemStack item = player.getItemInHand(hand);
            if (!(item.getItem() instanceof SimpleKatana)) return;

            // ENHANCED BLOCKING CHECK
            if (isInputBlocked()) {
                System.out.println("DEBUG: Left click blocked - wheel state or input handler");
                return;
            }

            sendLeftClick(player);
        });

        // Right click air
        InteractionEvent.CLIENT_RIGHT_CLICK_AIR.register((player, hand) -> {
            ItemStack item = player.getItemInHand(hand);
            if (!(item.getItem() instanceof SimpleKatana)) return;

            // ENHANCED BLOCKING CHECK (also block right clicks if needed)
            if (isInputBlocked()) {
                System.out.println("DEBUG: Right click blocked - wheel state or input handler");
                return;
            }

            sendRightClick(player);
        });
    }

    /**
     * ENHANCED: Comprehensive input blocking check - CLIENT ONLY
     */
    private static boolean isInputBlocked() {
        // This method should only be called on client side
        try {
            // Import Minecraft class dynamically to avoid server loading issues
            Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
            Object player = minecraftClass.getField("player").get(minecraft);

            if (player != null) {
                // Check blocking effect using reflection to avoid client class loading on server
                java.lang.reflect.Method hasEffectMethod = player.getClass().getMethod("hasEffect", net.minecraft.world.effect.MobEffect.class);
                boolean hasBlockingEffect = (boolean) hasEffectMethod.invoke(player, NichirinEffectRegistry.BLOCKING.get());

                if (hasBlockingEffect) {
                    return true;
                }
            }
        } catch (Exception e) {
            System.out.println("WARNING: Could not check blocking effect: " + e.getMessage());
        }

        // Check wheel state first (most important)
        try {
            if (com.xirc.nichirin.client.handler.AttackWheelHandler.shouldBlockAttackInputs()) {
                return true;
            }
        } catch (Exception e) {
            System.out.println("WARNING: Could not check wheel blocking state: " + e.getMessage());
        }

        // Check multiplayer input handler
        try {
            if (MultiplayerInputHandler.shouldBlockInputsClient()) {
                return true;
            }
        } catch (Exception e) {
            System.out.println("WARNING: Could not check multiplayer input blocking: " + e.getMessage());
        }

        return false;
    }

    private static void sendLeftClick(Player player) {
        // Client feedback - use reflection to avoid server class loading
        try {
            if (player.getMainHandItem().getItem() instanceof SimpleKatana katana) {
                katana.displayClientCooldown(player);
            }
        } catch (Exception e) {
            System.out.println("WARNING: Could not display client cooldown: " + e.getMessage());
        }

        // Send to server
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        NetworkManager.sendToServer(LEFT_CLICK_ID, buf);

        System.out.println("DEBUG: Sent left click to server");
    }

    private static void sendRightClick(Player player) {
        boolean crouch = player.isCrouching();
        ResourceLocation id = crouch ? RIGHT_CROUCH_ID : RIGHT_CLICK_ID;

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        NetworkManager.sendToServer(id, buf);

        System.out.println("DEBUG: Sent right click to server (crouch: " + crouch + ")");
    }

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

    private static void registerClientPackets() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, FEEDBACK_ID, (buf, context) -> {
            boolean hasBreathingMove = buf.readBoolean();

            context.queue(() -> {
                try {
                    // Use reflection to avoid loading client classes on server
                    Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
                    Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
                    Object player = minecraftClass.getField("player").get(minecraft);

                    if (hasBreathingMove) {
                        String moveName = buf.readUtf();
                        int cooldown = buf.readInt();

                        // Use reflection for CooldownHUD
                        Class<?> cooldownHUDClass = Class.forName("com.xirc.nichirin.client.gui.CooldownHUD");
                        cooldownHUDClass.getMethod("setCooldown", String.class, int.class).invoke(null, moveName, cooldown);

                        // Use reflection for AnimationUtils
                        Class<?> animationUtilsClass = Class.forName("com.xirc.nichirin.common.util.AnimationUtils");
                        if (moveName.contains("Thunder Clap")) {
                            animationUtilsClass.getMethod("playAnimation", Object.class, String.class).invoke(null, player, "thunder_clap_flash");
                        } else if (moveName.contains("Heat Lightning")) {
                            animationUtilsClass.getMethod("playAnimation", Object.class, String.class).invoke(null, player, "heat_lightning");
                        }
                    } else {
                        boolean wasCrouching = buf.readBoolean();

                        Class<?> animationUtilsClass = Class.forName("com.xirc.nichirin.common.util.AnimationUtils");
                        Class<?> cooldownHUDClass = Class.forName("com.xirc.nichirin.client.gui.CooldownHUD");

                        if (wasCrouching) {
                            animationUtilsClass.getMethod("playAnimation", Object.class, String.class).invoke(null, player, "rising_slash");
                            cooldownHUDClass.getMethod("setCooldown", String.class, int.class).invoke(null, "Rising Slash", 25);
                        } else {
                            animationUtilsClass.getMethod("playAnimation", Object.class, String.class).invoke(null, player, "double_slash");
                            cooldownHUDClass.getMethod("setCooldown", String.class, int.class).invoke(null, "Double Slash", 20);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("WARNING: Could not handle client feedback: " + e.getMessage());
                }
            });
        });
    }

    private static void handleServerLeftClick(ServerPlayer player) {
        if (isServerBlocked(player)) {
            System.out.println("DEBUG: Server left click blocked for player " + player.getName().getString());
            return;
        }

        ItemStack item = player.getMainHandItem();
        if (item.getItem() instanceof SimpleKatana katana) {
            SimpleKatana instance = getKatanaInstance(player, katana);
            instance.performAttack(player);
            System.out.println("DEBUG: Server executed left click for player " + player.getName().getString());
        }
    }

    private static void handleServerRightClick(ServerPlayer player, boolean crouch) {
        if (isServerBlocked(player)) {
            System.out.println("DEBUG: Server right click blocked for player " + player.getName().getString());
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

            System.out.println("DEBUG: Server executed right click for player " + player.getName().getString() + " (crouch: " + crouch + ")");
        }
    }

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

    private static void sendFeedback(ServerPlayer player, String moveName, boolean crouch) {
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
    }

    private static void registerSharedEvents() {
        // Entity attack blocking
        PlayerEvent.ATTACK_ENTITY.register((player, level, entity, hand, hitResult) -> {
            ItemStack item = player.getItemInHand(hand);
            if (!(item.getItem() instanceof SimpleKatana)) {
                return EventResult.pass();
            }

            if (level.isClientSide) {
                // ENHANCED BLOCKING CHECK for entity attacks too
                if (isInputBlocked()) {
                    System.out.println("DEBUG: Entity attack blocked - wheel state or input handler");
                    return EventResult.interruptFalse();
                }
                sendLeftClick(player);
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
            System.out.println("DEBUG: Blocked inputs for player " + player.getName().getString() + " after breathing move");
        }
    }
}