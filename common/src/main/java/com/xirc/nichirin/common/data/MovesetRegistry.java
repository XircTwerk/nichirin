package com.xirc.nichirin.common.data;

import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.attack.moveset.ThunderBreathingMoveset;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Registry for all available breathing style movesets
 */
public class MovesetRegistry {

    private static final Map<String, AbstractMoveset> MOVESETS = new HashMap<>();
    private static final Map<String, MovesetFactory> FACTORIES = new HashMap<>();

    /**
     * Function that creates a moveset instance
     */
    @FunctionalInterface
    public interface MovesetFactory {
        AbstractMoveset create();
    }

    /**
     * Registers a moveset factory
     * This allows lazy instantiation of movesets
     */
    public static void registerMoveset(String id, MovesetFactory factory) {
        FACTORIES.put(id, factory);
    }

    /**
     * Registers a moveset instance directly
     */
    public static void registerMoveset(AbstractMoveset moveset) {
        MOVESETS.put(moveset.getMovesetId(), moveset);
    }

    /**
     * Gets a moveset by ID, creating it if necessary
     */
    @Nullable
    public static AbstractMoveset getMoveset(String id) {
        // Check if already instantiated
        AbstractMoveset moveset = MOVESETS.get(id);
        if (moveset != null) {
            return moveset;
        }

        // Try to create from factory
        MovesetFactory factory = FACTORIES.get(id);
        if (factory != null) {
            moveset = factory.create();
            MOVESETS.put(id, moveset);
            return moveset;
        }

        return null;
    }

    /**
     * Gets all registered moveset IDs
     */
    public static Set<String> getAllMovesetIds() {
        Set<String> ids = new HashSet<>();
        ids.addAll(MOVESETS.keySet());
        ids.addAll(FACTORIES.keySet());
        return ids;
    }

    /**
     * Gets all instantiated movesets
     */
    public static Collection<AbstractMoveset> getAllMovesets() {
        // Instantiate any factories that haven't been created yet
        for (Map.Entry<String, MovesetFactory> entry : FACTORIES.entrySet()) {
            if (!MOVESETS.containsKey(entry.getKey())) {
                MOVESETS.put(entry.getKey(), entry.getValue().create());
            }
        }
        return MOVESETS.values();
    }

    /**
     * Checks if a moveset is registered
     */
    public static boolean isRegistered(String id) {
        return MOVESETS.containsKey(id) || FACTORIES.containsKey(id);
    }

    /**
     * Clears all registered movesets
     * Mainly for testing or reloading
     */
    public static void clear() {
        MOVESETS.clear();
        FACTORIES.clear();
    }

    /**
     * Gets a random moveset (useful for testing)
     */
    @Nullable
    public static AbstractMoveset getRandomMoveset() {
        List<AbstractMoveset> movesets = new ArrayList<>(getAllMovesets());
        if (movesets.isEmpty()) {
            return null;
        }
        return movesets.get(new Random().nextInt(movesets.size()));
    }

    /**
     * Initialize default movesets
     * Call this during mod initialization
     */
    public static void init() {
        // Register Thunder Breathing moveset
        registerMoveset("thunder_breathing", ThunderBreathingMoveset::new);

        // Future movesets can be added here:
        // registerMoveset("water_breathing", WaterBreathingMoveset::new);
        // registerMoveset("flame_breathing", FlameBreathingMoveset::new);
    }
}