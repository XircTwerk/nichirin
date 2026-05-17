package com.xirc.nichirin.registry;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.attack.moveset.breathing.*;
import com.xirc.nichirin.common.attack.moveset.demon.DefaultDemonMoveset;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public interface NichirinMovesetRegistry {

    Map<String, AbstractMoveset> MOVESETS = new HashMap<>();
    Map<String, MovesetFactory> FACTORIES = new HashMap<>();
    Map<ResourceLocation, MoveInfo> GLOBAL_MOVES = new HashMap<>();

    @FunctionalInterface
    interface MovesetFactory {
        AbstractMoveset create();
    }

    static void registerMoveset(String id, MovesetFactory factory) {
        FACTORIES.put(id, factory);
    }

    static void registerMoveset(AbstractMoveset moveset) {
        MOVESETS.put(moveset.getMovesetId(), moveset);
        indexMoves(moveset);
    }

    private static void indexMoves(AbstractMoveset moveset) {
        for (int i = 0; i < moveset.getMoveCount(); i++) {
            AbstractMoveset.MoveConfiguration config = moveset.getMove(i);
            if (config != null) {
                ResourceLocation moveId = new ResourceLocation(BreathOfNichirin.MOD_ID,
                        moveset.getMovesetId() + "/" + config.getMoveId());
                GLOBAL_MOVES.put(moveId, new MoveInfo(
                        moveset.getMovesetId(), config.getMoveId(),
                        config.getDisplayName(), i, moveset));
            }
        }
    }

    @Nullable
    static AbstractMoveset getMoveset(String id) {
        AbstractMoveset moveset = MOVESETS.get(id);
        if (moveset != null) return moveset;

        MovesetFactory factory = FACTORIES.get(id);
        if (factory != null) {
            moveset = factory.create();
            MOVESETS.put(id, moveset);
            indexMoves(moveset);
            return moveset;
        }
        return null;
    }

    static Set<String> getAllMovesetIds() {
        Set<String> ids = new HashSet<>();
        ids.addAll(MOVESETS.keySet());
        ids.addAll(FACTORIES.keySet());
        return ids;
    }

    static Collection<AbstractMoveset> getAllMovesets() {
        for (Map.Entry<String, MovesetFactory> entry : FACTORIES.entrySet()) {
            if (!MOVESETS.containsKey(entry.getKey())) {
                AbstractMoveset moveset = entry.getValue().create();
                MOVESETS.put(entry.getKey(), moveset);
                indexMoves(moveset);
            }
        }
        return MOVESETS.values();
    }

    static boolean isRegistered(String id) {
        return MOVESETS.containsKey(id) || FACTORIES.containsKey(id);
    }

    static void clear() {
        MOVESETS.clear();
        FACTORIES.clear();
        GLOBAL_MOVES.clear();
    }

    @Nullable
    static AbstractMoveset getRandomMoveset() {
        List<AbstractMoveset> movesets = new ArrayList<>(getAllMovesets());
        if (movesets.isEmpty()) return null;
        return movesets.get(new Random().nextInt(movesets.size()));
    }

    @Nullable
    static MoveInfo getMove(ResourceLocation id) {
        return GLOBAL_MOVES.get(id);
    }

    @Nullable
    static MoveInfo getMove(String movesetId, String moveId) {
        return getMove(new ResourceLocation(BreathOfNichirin.MOD_ID, movesetId + "/" + moveId));
    }

    static List<MoveInfo> getMovesForMoveset(String movesetId) {
        List<MoveInfo> moves = new ArrayList<>();
        for (MoveInfo info : GLOBAL_MOVES.values()) {
            if (info.movesetId.equals(movesetId)) moves.add(info);
        }
        return moves;
    }

    static Map<ResourceLocation, MoveInfo> getAllMoves() {
        return new HashMap<>(GLOBAL_MOVES);
    }

    static int getTotalMoveCount() {
        return GLOBAL_MOVES.size();
    }

    static void init() {
        GLOBAL_MOVES.clear();
        MOVESETS.clear();
        FACTORIES.clear();

        registerMoveset("thunder_breathing", ThunderBreathingMoveset::new);
        registerMoveset("flame_breathing", FlameBreathingMoveset::new);
        registerMoveset("insect_breathing", InsectBreathingMoveset::new);
        registerMoveset("sound_breathing", SoundBreathingMoveset::new);
        registerMoveset("water_breathing", WaterBreathingMoveset::new);
        registerMoveset("beast_breathing", BeastBreathingMoveset::new);
        registerMoveset("mist_breathing", MistBreathingMoveset::new);
        registerMoveset("default_demon", DefaultDemonMoveset::new);

    }

    class MoveInfo {
        public final String movesetId;
        public final String moveId;
        public final String displayName;
        public final int index;
        public final AbstractMoveset moveset;

        MoveInfo(String movesetId, String moveId, String displayName, int index, AbstractMoveset moveset) {
            this.movesetId = movesetId;
            this.moveId = moveId;
            this.displayName = displayName;
            this.index = index;
            this.moveset = moveset;
        }

        public void execute(Player player) {
            moveset.performMove(player, index);
        }

        @Override
        public String toString() {
            return movesetId + ":" + moveId + " (" + displayName + ")";
        }
    }
}
