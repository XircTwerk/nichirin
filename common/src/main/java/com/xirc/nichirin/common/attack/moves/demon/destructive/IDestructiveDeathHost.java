package com.xirc.nichirin.common.attack.moves.demon.destructive;

/**
 * Implemented by non-player entities (the Akaza NPC) that carry Destructive Death's shockwave /
 * overdrive state on themselves.
 *
 * <p>The player BDA keeps this state in {@link DestructiveDeathState} keyed by player UUID and syncs
 * it with a packet. An NPC instead stores it on the entity (synched entity data), so
 * {@link IDestructiveDeathCQC} and {@link DestructiveDeathCqcHook} can drive the exact same
 * zoning/overdrive behavior from a {@code LivingEntity} without a {@code ServerPlayer}.</p>
 */
public interface IDestructiveDeathHost {

    /** Whether destructive CQC moves / successful CQC hits should spawn forward shockwaves. */
    boolean ddShockwaveEnabled();

    /** Whether Overdrive is active — red shockwaves (and, on the Akaza NPC, red glow). */
    boolean ddOverdriveActive();
}
