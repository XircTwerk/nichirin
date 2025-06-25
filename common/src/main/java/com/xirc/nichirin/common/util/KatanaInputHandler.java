package com.xirc.nichirin.common.util;

import com.xirc.nichirin.common.attack.component.IBreathingAttacker;
import com.xirc.nichirin.common.attack.component.BreathingMoveMap;
import com.xirc.nichirin.common.item.katana.AbstractKatanaItem;
import com.xirc.nichirin.common.util.enums.MoveInputType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.InteractionEvent;
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

            if (heldItem.getItem() instanceof AbstractKatanaItem) {
                handleLeftClick(player);
                return CompoundEventResult.interruptDefault(heldItem);
            }

            return CompoundEventResult.pass();
        });

        // For entity attacks - this event expects EventResult
        PlayerEvent.ATTACK_ENTITY.register((player, level, entity, hand, hitResult) -> {
            System.out.println("DEBUG: Attack entity detected");
            handleLeftClick(player);
            return EventResult.pass();
        });
    }

    private static void handleLeftClick(Player player) {
        ItemStack heldItem = player.getMainHandItem();
        System.out.println("DEBUG: Held item: " + heldItem.getItem().getClass().getSimpleName());

        if (heldItem.getItem() instanceof AbstractKatanaItem katana) {
            System.out.println("DEBUG: Found katana in main hand, triggering basic attack");

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
     * Cleans up attacker when player leaves
     */
    public static void cleanupPlayer(Player player) {
        PLAYER_ATTACKERS.remove(player.getUUID());
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