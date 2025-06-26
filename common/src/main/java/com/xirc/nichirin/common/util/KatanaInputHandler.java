package com.xirc.nichirin.common.util;

import com.xirc.nichirin.common.attack.component.IBreathingAttacker;
import com.xirc.nichirin.common.attack.component.BreathingMoveMap;
import com.xirc.nichirin.common.item.katana.AbstractKatanaItem;
import com.xirc.nichirin.common.item.katana.SimpleKatana;
import com.xirc.nichirin.common.util.enums.MoveInputType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.event.EventResult;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles input for katana attacks
 */
public class KatanaInputHandler {

    // Store temporary attackers for players
    private static final Map<UUID, TestBreathingAttacker> PLAYER_ATTACKERS = new HashMap<>();
    // Store SimpleKatana instances per player for tracking
    private static final Map<UUID, SimpleKatana> PLAYER_SIMPLE_KATANAS = new HashMap<>();

    // Network packet ID for left click
    private static final ResourceLocation LEFT_CLICK_PACKET = new ResourceLocation("nichirin", "left_click");

    public static void register() {
        System.out.println("DEBUG: Registering katana input handlers");

        // CLIENT SIDE: Detect left clicks and send to server
        if (isClientSide()) {
            // For left click in air (client only)
            InteractionEvent.CLIENT_LEFT_CLICK_AIR.register((player, hand) -> {
                System.out.println("DEBUG: Client left click air detected");
                sendLeftClickToServer();
            });
        }

        // SERVER SIDE: Handle the packet
        registerServerPacketHandler();

        // For entity attacks - this runs on both sides
        PlayerEvent.ATTACK_ENTITY.register((player, level, entity, hand, hitResult) -> {
            System.out.println("DEBUG: Attack entity detected on " + (level.isClientSide ? "CLIENT" : "SERVER"));
            ItemStack heldItem = player.getItemInHand(hand);

            // If it's a katana, handle our custom attack
            if (heldItem.getItem() instanceof SimpleKatana || heldItem.getItem() instanceof AbstractKatanaItem) {
                if (!level.isClientSide) {
                    // Server side - perform the attack
                    handleLeftClick(player);
                } else {
                    // Client side - send packet to server
                    sendLeftClickToServer();
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
    private static boolean isClientSide() {
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
    private static void sendLeftClickToServer() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        // No data needed, just the packet itself
        NetworkManager.sendToServer(LEFT_CLICK_PACKET, buf);
    }

    /**
     * Register server packet handler
     */
    private static void registerServerPacketHandler() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, LEFT_CLICK_PACKET, (buf, context) -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player != null) {
                // Schedule on main thread
                context.queue(() -> {
                    System.out.println("DEBUG: Server received left click packet from " + player.getName().getString());
                    handleLeftClick(player);
                });
            }
        });
    }

    private static void handleLeftClick(Player player) {
        // Make sure we're on server side
        if (player.level().isClientSide) {
            System.out.println("DEBUG: handleLeftClick called on client side, ignoring");
            return;
        }

        ItemStack heldItem = player.getMainHandItem();
        System.out.println("DEBUG: Server handling left click - Held item: " + heldItem.getItem().getClass().getSimpleName());

        // Handle SimpleKatana
        if (heldItem.getItem() instanceof SimpleKatana simpleKatana) {
            System.out.println("DEBUG: Found SimpleKatana in main hand, triggering attack on server");

            // Get or create instance tracker for this player
            SimpleKatana katanaInstance = getSimpleKatanaForPlayer(player, simpleKatana);
            katanaInstance.performAttack(player);

        }
        // Handle AbstractKatanaItem (breathing system)
        else if (heldItem.getItem() instanceof AbstractKatanaItem katana) {
            System.out.println("DEBUG: Found AbstractKatanaItem in main hand, triggering breathing attack");

            // Create or get breathing attacker for this player
            TestBreathingAttacker attacker = getBreathingAttacker(player);

            if (attacker != null) {
                System.out.println("DEBUG: Got breathing attacker, performing move");
                katana.performMove(player, MoveInputType.BASIC, attacker);
            } else {
                System.out.println("DEBUG: No breathing attacker found for player");
            }
        }
    }

    /**
     * Tick all katanas for the player (SERVER ONLY)
     */
    private static void tickPlayer(Player player) {
        // Tick breathing attacker if exists
        TestBreathingAttacker attacker = PLAYER_ATTACKERS.get(player.getUUID());
        if (attacker != null) {
            attacker.tick();
        }

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
     * Gets or creates a breathing attacker for the given player
     */
    private static TestBreathingAttacker getBreathingAttacker(Player player) {
        System.out.println("DEBUG: Getting breathing attacker for player");

        UUID playerId = player.getUUID();
        TestBreathingAttacker attacker = PLAYER_ATTACKERS.get(playerId);

        if (attacker == null) {
            System.out.println("DEBUG: Creating new breathing attacker for player");
            attacker = new TestBreathingAttacker(player);
            PLAYER_ATTACKERS.put(playerId, attacker);
        }

        return attacker;
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
     * Cleans up attacker when player leaves
     */
    public static void cleanupPlayer(Player player) {
        PLAYER_ATTACKERS.remove(player.getUUID());
        PLAYER_SIMPLE_KATANAS.remove(player.getUUID());
    }

    /**
     * Simple test state enum
     */
    public enum SimpleState {
        IDLE,
        ATTACKING,
        COOLDOWN
    }

    /**
     * Concrete implementation of IBreathingAttacker for testing
     * Note: The generic parameters are self-referential: TestBreathingAttacker extends IBreathingAttacker<TestBreathingAttacker, SimpleState>
     */
    public static class TestBreathingAttacker implements IBreathingAttacker<TestBreathingAttacker, SimpleState> {
        private final Player player;
        private SimpleState state = SimpleState.IDLE;
        private float breathLevel = 100.0f;
        private final BreathingMoveMap<TestBreathingAttacker, SimpleState> moveMap;

        public TestBreathingAttacker(Player player) {
            this.player = player;
            this.moveMap = new BreathingMoveMap<>();
        }

        @Override
        public Player getPlayer() {
            return player;
        }

        @Override
        public SimpleState getState() {
            return state;
        }

        @Override
        public void setState(SimpleState newState) {
            System.out.println("DEBUG: Changing state from " + this.state + " to " + newState);
            this.state = newState;
        }

        @Override
        public BreathingMoveMap<TestBreathingAttacker, SimpleState> getMoveMap() {
            return moveMap;
        }

        @Override
        public boolean canUseBreathing() {
            return player != null && player.isAlive() && breathLevel > 0 && state != SimpleState.COOLDOWN;
        }

        @Override
        public float getBreathLevel() {
            return breathLevel;
        }

        @Override
        public boolean consumeBreath(float amount) {
            if (breathLevel >= amount) {
                breathLevel -= amount;
                System.out.println("DEBUG: Consumed " + amount + " breath, remaining: " + breathLevel);
                return true;
            }
            System.out.println("DEBUG: Not enough breath to consume " + amount + ", current: " + breathLevel);
            return false;
        }

        /**
         * Regenerates breath over time
         */
        public void tick() {
            if (breathLevel < 100.0f) {
                breathLevel = Math.min(100.0f, breathLevel + 0.5f); // Regenerate 0.5 per tick
            }
        }
    }
}