package com.xirc.nichirin.common.attack.moveset.demon;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.moves.demon.destructive.*;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.registry.NichirinMovesetRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Akaza's Blood Demon Art moveset — Destructive Death.
 *
 * <p>Bound to the demon-moveset slot, so it takes over input when CQC is not equipped (the
 * existing demon-input router in {@code NichirinPacketRegistry} already handles that fall-through).
 * Wiring:</p>
 * <ul>
 *   <li>M1 → Snap Punch (basic forward jab; shockwave on hit when toggle is on)</li>
 *   <li>M2 → Compass Needle (6s tracker that buffs damage on tracked targets)</li>
 *   <li>Crouch M2 → Compass Overdrive (Compass + Speed I + Strength I + red shockwaves)</li>
 *   <li>Wheel 0 → Shockwave Toggle</li>
 *   <li>Wheel 1 → Overdrive Toggle</li>
 *   <li>Wheel 2 → Blue Silver Chaotic Afterglow (ultimate, requires active Compass Needle)</li>
 * </ul>
 */
public class DestructiveDeathMoveset extends AbstractMoveset {

    public DestructiveDeathMoveset() {
        super("destructive_death", "Destructive Death", MovesetType.DEMON, createBuilder());
    }

    private static MovesetBuilder createBuilder() {
        // BDA only owns the wheel — basic strikes are handled by CQC. M1/M2/CrM2 are not used:
        // RMB is the block button in this codebase, so right-click never reaches a BDA action.
        return new MovesetBuilder()
                .withIdleAnimation("nichirin:demon_idle")
                .withSpeedMultiplier(1.10f)

                // Wheel 0 — Shockwave Toggle
                .withMove(new MoveBuilder("shockwave_toggle", "Shockwave Toggle")
                        .withAnimation("nichirin:snap_punch", 4)
                        .withTiming(0, 1, 3)
                        .withDescription("Toggle: future CQC attacks spawn forward shockwaves.")
                        .withAction(entity -> {
                            ShockwaveToggleAttack attack = new ShockwaveToggleAttack();
                            attack.configure(captureWheelMoveFor("destructive_death", 0));
                            MoveExecutor.executeAttack(entity, attack, "destructive_death", "shockwave_toggle");
                        })
                )

                // Wheel 1 — Compass Needle (crouch while activating to enter Compass Overdrive)
                .withMove(new MoveBuilder("compass_needle", "Compass Needle")
                        .withAnimation("nichirin:compass_needle", 6)
                        .withTiming(0, 1, 4)
                        .withRange(20f)
                        .withDescription("Tracks nearby entities for 6 seconds and amplifies damage on them. Crouch while activating to enter Compass Overdrive (Speed I + Strength I + red shockwaves).")
                        .withAction(entity -> {
                            boolean crouching = entity instanceof Player p && p.isCrouching();
                            CompassNeedleAttack attack = crouching
                                    ? new CompassOverdriveAttack()
                                    : new CompassNeedleAttack();
                            attack.configure(captureWheelMoveFor("destructive_death", 1));
                            MoveExecutor.executeAttack(entity, attack, "destructive_death",
                                    crouching ? "compass_overdrive" : "compass_needle");
                        })
                )

                // Wheel 2 — Overdrive Toggle
                .withMove(new MoveBuilder("overdrive_toggle", "Overdrive")
                        .withAnimation("nichirin:snap_punch", 4)
                        .withTiming(0, 1, 3)
                        .withDescription("Toggle: Strength I + Speed I, and all shockwaves render red and hit harder.")
                        .withAction(entity -> {
                            OverdriveToggleAttack attack = new OverdriveToggleAttack();
                            attack.configure(captureWheelMoveFor("destructive_death", 2));
                            MoveExecutor.executeAttack(entity, attack, "destructive_death", "overdrive_toggle");
                        })
                )

                // Wheel 3 — Blue Silver Chaotic Afterglow (ultimate, 10s channel)
                .withMove(new MoveBuilder("blue_silver_chaotic_afterglow", "Blue Silver Chaotic Afterglow")
                        .withAnimation("nichirin:snap_punch", 12)
                        // Duration must match BlueSilverChaoticAfterglowAttack.TOTAL_TICKS.
                        .withTiming(800, 5, BlueSilverChaoticAfterglowAttack.TOTAL_TICKS)
                        .withDamage(9.0f)
                        .withRange(20f)
                        .withHitStun(40)
                        .withHyperArmor()
                        .withDescription("10s channel: twelve clones manifest around you and batter a Compass-tracked target with a storm of thin shockwaves, ending in a unison blow. Requires Compass Needle + Overdrive.")
                        .withAction(entity -> {
                            // Gate BEFORE executing so a failed requirement doesn't burn the cooldown.
                            if (!BlueSilverChaoticAfterglowAttack.canCast(entity)) return;
                            BlueSilverChaoticAfterglowAttack attack = new BlueSilverChaoticAfterglowAttack();
                            attack.configure(captureWheelMoveFor("destructive_death", 3));
                            MoveExecutor.executeAttack(entity, attack, "destructive_death", "blue_silver_chaotic_afterglow");
                        })
                );
    }

    @Override
    public int getMoveCount() {
        return 4; // wheel slots 0..3
    }

    @Override
    public int getRightClickMoveIndex(boolean isCrouching) {
        return -1;
    }

    @Override
    public String getLeftClickMoveName() { return "None"; }
    @Override
    public String getRightClickMoveName() { return "None"; }
    @Override
    public String getCrouchRightClickMoveName() { return "None"; }

    @Override
    public void onMovePerformed(LivingEntity entity, int moveIndex, boolean isCrouching) {
    }

    public static void cleanupPlayer(Player player) {
        DestructiveDeathState.cleanup(player.getUUID());
        CompassNeedleTracker.clear(player.getUUID());
        // Online (redemption): broadcast the aura removal so it actually disappears. Offline
        // (logout): just drop the cache — the despawned entity stops rendering it anyway.
        if (player instanceof ServerPlayer sp) {
            DestructiveDeathPlayerAura.remove(sp);
        } else {
            DestructiveDeathPlayerAura.clear(player.getUUID());
        }
    }

    /** Lazy capture helper — pulls the per-slot {@link MoveConfiguration} for this moveset. */
    private static MoveConfiguration captureWheelMoveFor(String id, int index) {
        AbstractMoveset m = NichirinMovesetRegistry.getMoveset(id);
        return m != null ? m.getMove(index) : null;
    }
}
