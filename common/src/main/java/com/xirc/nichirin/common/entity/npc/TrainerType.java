package com.xirc.nichirin.common.entity.npc;

import com.xirc.nichirin.common.data.ProgressionHelper;
import com.xirc.nichirin.common.network.s2c.OpenTrainerDialoguePacket;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Immutable config for each breathing-style trainer NPC.
 *
 * <p>Held by {@link BaseBreathingTrainerEntity} instances and passed inside
 * {@link OpenTrainerDialoguePacket} so
 * the client dialogue screen can show the right name and dialogue.</p>
 */
public enum TrainerType {

    WATER(
        "Sakonji Urokodaki",
        "water_breathing",
        0x1E90FF,
        Items.LILY_OF_THE_VALLEY, 5,
        "Spar with me. The water will judge your resolve.",
        "You have proven yourself. Water Breathing — it is yours now.",
        "You fought well. But you are not yet ready."
    ),

    FLAME(
        "Kyojuro Rengoku",
        "flame_breathing",
        0xFF6B00,
        Items.BLAZE_POWDER, 5,
        "Let us see if your flame matches your conviction!",
        "Magnificent! Your flame burns true. Flame Breathing is yours!",
        "Your flame flickered. Train harder and return."
    ),

    THUNDER(
        "Jigoro Kuwajima",
        "thunder_breathing",
        0xFFD700,
        Items.GOLD_INGOT, 8,
        "Move like thunder. Let us begin.",
        "Ha! You have done it. Carry this thunder forward.",
        "Speed without timing is just noise. Return when your mind is sharper."
    ),

    INSECT(
        "Shinobu Kocho",
        "insect_breathing",
        0xAA55FF,
        Items.SPIDER_EYE, 10,
        "Still your mind. The insect sees everything.",
        "Impressive focus. Insect Breathing belongs to those who master patience.",
        "You let impatience win. Focus and return."
    ),

    SOUND(
        "Tengen Uzui",
        "sound_breathing",
        0xFF44BB,
        Items.GOLD_BLOCK, 2,
        "FLAMBOYANT! Let us make this fight worthy of the name!",
        "MAGNIFICENT! FLAMBOYANT! Sound Breathing — you have earned it!",
        "Not flashy enough! Come back with more flair!"
    );


    /** NPC display name shown in the dialogue screen header. */
    public final String npcName;

    /** Moveset ID passed to {@link ProgressionHelper}. */
    public final String movesetId;

    /** Hex RGB color used for server-side chat messages. */
    public final int styleColor;

    /** Item the player must carry before a duel is allowed. */
    public final Item prerequisiteItem;
    public final int  prerequisiteCount;

    /** Shown in chat when the duel starts. */
    public final String duelStartMsg;
    /** Shown to winner in chat. */
    public final String duelWinMsg;
    /** Shown to loser in chat. */
    public final String duelLoseMsg;


    TrainerType(String npcName, String movesetId, int styleColor,
                Item prerequisiteItem, int prerequisiteCount,
                String duelStartMsg, String duelWinMsg, String duelLoseMsg) {
        this.npcName            = npcName;
        this.movesetId          = movesetId;
        this.styleColor         = styleColor;
        this.prerequisiteItem   = prerequisiteItem;
        this.prerequisiteCount  = prerequisiteCount;
        this.duelStartMsg       = duelStartMsg;
        this.duelWinMsg         = duelWinMsg;
        this.duelLoseMsg        = duelLoseMsg;
    }
}