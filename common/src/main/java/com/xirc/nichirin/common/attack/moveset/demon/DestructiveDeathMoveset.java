package com.xirc.nichirin.common.attack.moveset.demon;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.moves.demon.destructive.*;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.registry.NichirinMovesetRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
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
        return new MovesetBuilder()
                .withIdleAnimation("nichirin:demon_idle")
                .withSpeedMultiplier(1.10f)

                .withLeftClickMove(new MoveBuilder("snap_punch", "Snap Punch")
                        .withAnimation("nichirin:snap_punch", 10)
                        .withTiming(0, 1, 7)
                        .withDamage(4.5f)
                        .withRange(2.2f)
                        .withKnockback(0.25f)
                        .withHitStun(10)
                        .withHitboxSize(1.5f)
                        .withDescription("Quick forward strike. Sends a short-range shockwave when the toggle is on.")
                        .withAction(entity -> {
                            // SnapPunchAttack is a CQC move (extends AbstractCqcAttack); the BDA's
                            // M1 just re-runs it directly. MoveExecutor uses reflection to find a
                            // matching start(...) overload.
                            SnapPunchAttack attack = new SnapPunchAttack();
                            attack.configure(captureLeftClickConfigFor("destructive_death"));
                            MoveExecutor.executeAttack(entity, attack, "destructive_death", "snap_punch");
                        })
                )

                .withRightClickMove(new MoveBuilder("compass_needle", "Compass Needle")
                        .withAnimation("nichirin:compass_needle", 6)
                        .withTiming(0, 1, 130)
                        .withDamage(0f)
                        .withRange(20f)
                        .withDescription("Manifests the compass. Tracks nearby entities for 6 seconds and amplifies damage on them.")
                        .withAction(entity -> {
                            CompassNeedleAttack attack = new CompassNeedleAttack();
                            attack.configure(captureRightClickConfigFor("destructive_death"));
                            MoveExecutor.executeAttack(entity, attack, "destructive_death", "compass_needle");
                        })
                )

                .withCrouchRightClickMove(new MoveBuilder("compass_overdrive", "Compass Overdrive")
                        .withAnimation("nichirin:compass_needle", 6)
                        .withTiming(0, 1, 130)
                        .withDamage(0f)
                        .withRange(20f)
                        .withDescription("Compass Needle + Speed I + Strength I + red shockwaves. Arrow points to the closest entity.")
                        .withAction(entity -> {
                            CompassOverdriveAttack attack = new CompassOverdriveAttack();
                            attack.configure(captureCrouchRightClickConfigFor("destructive_death"));
                            MoveExecutor.executeAttack(entity, attack, "destructive_death", "compass_overdrive");
                        })
                )

                // Wheel 0 — Shockwave Toggle
                .withMove(new MoveBuilder("shockwave_toggle", "Shockwave Toggle")
                        .withAnimation("nichirin:snap_punch", 4)
                        .withTiming(0, 1, 3)
                        .withDescription("Toggle: future Snap Punches (and enhanced CQC attacks) spawn forward shockwaves.")
                        .withAction(entity -> {
                            ShockwaveToggleAttack attack = new ShockwaveToggleAttack();
                            attack.configure(captureWheelMoveFor("destructive_death", 0));
                            MoveExecutor.executeAttack(entity, attack, "destructive_death", "shockwave_toggle");
                        })
                )

                // Wheel 1 — Overdrive Toggle
                .withMove(new MoveBuilder("overdrive_toggle", "Overdrive")
                        .withAnimation("nichirin:snap_punch", 4)
                        .withTiming(0, 1, 3)
                        .withDescription("Toggle: Strength I + Speed I, and all shockwaves render red and hit harder.")
                        .withAction(entity -> {
                            OverdriveToggleAttack attack = new OverdriveToggleAttack();
                            attack.configure(captureWheelMoveFor("destructive_death", 1));
                            MoveExecutor.executeAttack(entity, attack, "destructive_death", "overdrive_toggle");
                        })
                )

                // Wheel 2 — Blue Silver Chaotic Afterglow (ultimate)
                .withMove(new MoveBuilder("blue_silver_chaotic_afterglow", "Blue Silver Chaotic Afterglow")
                        .withAnimation("nichirin:annihilation_type", 12)
                        .withTiming(0, 1, 12)
                        .withDamage(9.0f)
                        .withRange(20f)
                        .withHitStun(20)
                        .withDescription("Spawns 12 omni-directional shockwaves. Requires Compass Needle to be active.")
                        .withAction(entity -> {
                            BlueSilverChaoticAfterglowAttack attack = new BlueSilverChaoticAfterglowAttack();
                            attack.configure(captureWheelMoveFor("destructive_death", 2));
                            MoveExecutor.executeAttack(entity, attack, "destructive_death", "blue_silver_chaotic_afterglow");
                        })
                );
    }

    @Override
    public int getMoveCount() {
        return 3; // wheel slots 0..2
    }

    @Override
    public boolean handleLeftClick(LivingEntity entity) {
        return super.handleLeftClick(entity);
    }

    @Override
    public boolean handleRightClick(LivingEntity entity, boolean isCrouching) {
        return super.handleRightClick(entity, isCrouching);
    }

    @Override
    public void performMove(LivingEntity entity, int moveIndex) {
        super.performMove(entity, moveIndex);
    }

    @Override
    public int getRightClickMoveIndex(boolean isCrouching) {
        return -1;
    }

    @Override
    public String getLeftClickMoveName() { return "Snap Punch"; }
    @Override
    public String getRightClickMoveName() { return "Compass Needle"; }
    @Override
    public String getCrouchRightClickMoveName() { return "Compass Overdrive"; }

    @Override
    public void onMovePerformed(LivingEntity entity, int moveIndex, boolean isCrouching) {
    }

    public static void cleanupPlayer(Player player) {
        DestructiveDeathState.cleanup(player.getUUID());
        CompassNeedleTracker.clear(player.getUUID());
    }

    /** Lazy capture helpers — pulls the per-slot {@link MoveConfiguration} for this moveset. */
    private static MoveConfiguration captureLeftClickConfigFor(String id) {
        AbstractMoveset m = NichirinMovesetRegistry.getMoveset(id);
        return m != null ? m.getLeftClickConfiguration() : null;
    }
    private static MoveConfiguration captureRightClickConfigFor(String id) {
        AbstractMoveset m = NichirinMovesetRegistry.getMoveset(id);
        return m != null ? m.getRightClickConfiguration() : null;
    }
    private static MoveConfiguration captureCrouchRightClickConfigFor(String id) {
        AbstractMoveset m = NichirinMovesetRegistry.getMoveset(id);
        return m != null ? m.getCrouchRightClickConfiguration() : null;
    }
    private static MoveConfiguration captureWheelMoveFor(String id, int index) {
        AbstractMoveset m = NichirinMovesetRegistry.getMoveset(id);
        return m != null ? m.getMove(index) : null;
    }
}
