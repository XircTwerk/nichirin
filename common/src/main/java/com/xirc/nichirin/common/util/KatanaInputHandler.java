package com.xirc.nichirin.common.util;

import com.xirc.nichirin.common.attack.component.IBreathingAttacker;
import com.xirc.nichirin.common.attack.component.BreathingMoveMap;
import com.xirc.nichirin.common.item.katana.AbstractKatanaItem;
import com.xirc.nichirin.common.item.katana.SimpleKatana;
import com.xirc.nichirin.common.util.enums.MoveInputType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.event.EventResult;
import dev.architectury.event.CompoundEventResult;

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

    public static void register() {
        System.out.println("DEBUG: Registering katana input handlers");

        // For left click in air - this event expects void return (no return statement)
        InteractionEvent.CLIENT_LEFT_CLICK_AIR.register((player, hand) -> {
            System.out.println("DEBUG: Client left click air detected");
            handleLeftClick(player);
            // Don't return anything - this event expects void
        });

        // For right click item - this event expects CompoundEventResult<ItemStack>
        InteractionEvent.RIGHT_CLICK_ITEM.register((player, hand) -> {
            System.out.println("DEBUG: Right click item detected (testing)");
            ItemStack heldItem = player.getItemInHand(hand);

            if (heldItem.getItem() instanceof AbstractKatanaItem || heldItem.getItem() instanceof SimpleKatana) {
                handleLeftClick(player);
                return CompoundEventResult.interruptDefault(heldItem);
            }

            return CompoundEventResult.pass();
        });

        // For entity attacks - this event expects EventResult
        PlayerEvent.ATTACK_ENTITY.register((player, level, entity, hand, hitResult) -> {
            System.out.println("DEBUG: Attack entity detected");
            ItemStack heldItem = player.getItemInHand(hand);

            // If it's a katana, handle our custom attack and prevent vanilla attack
            if (heldItem.getItem() instanceof SimpleKatana || heldItem.getItem() instanceof AbstractKatanaItem) {
                handleLeftClick(player);
                return EventResult.interruptFalse(); // Prevent vanilla attack
            }

            return EventResult.pass();
        });

        // Register player tick event to update katanas
        TickEvent.PLAYER_POST.register(player -> {
            tickPlayer(player);
        });

        // Clean up when player leaves
        PlayerEvent.PLAYER_QUIT.register(player -> {
            cleanupPlayer(player);
        });
    }

    private static void handleLeftClick(Player player) {
        ItemStack heldItem = player.getMainHandItem();
        System.out.println("DEBUG: Held item: " + heldItem.getItem().getClass().getSimpleName());

        // Handle SimpleKatana
        if (heldItem.getItem() instanceof SimpleKatana simpleKatana) {
            System.out.println("DEBUG: Found SimpleKatana in main hand, triggering attack");

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
     * Tick all katanas for the player
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