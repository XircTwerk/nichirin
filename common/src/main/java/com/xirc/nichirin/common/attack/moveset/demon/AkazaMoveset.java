package com.xirc.nichirin.common.attack.moveset.demon;

import com.xirc.nichirin.common.attack.moves.cqc.destructive.CqcAnnihilationTypeAttack;
import com.xirc.nichirin.common.attack.moves.cqc.destructive.CqcCrownSplitterAttack;
import com.xirc.nichirin.common.attack.moves.cqc.destructive.CqcDonutAttack;
import com.xirc.nichirin.common.attack.moves.cqc.destructive.CqcEightLayeredDemonCoreAttack;
import com.xirc.nichirin.common.attack.moves.cqc.destructive.CqcExplosiveFlurryAttack;
import com.xirc.nichirin.common.attack.moves.cqc.destructive.CqcSnapPunchAttack;
import com.xirc.nichirin.common.attack.moves.demon.destructive.BlueSilverChaoticAfterglowAttack;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import net.minecraft.world.entity.LivingEntity;

/**
 * Akaza's fighting kit — built entirely from the dedicated Destructive Death CQC attacks.
 *
 * <p>These attacks are {@code AbstractCqcAttack}s (LivingEntity-based, and already stat-boosted for
 * demon NPCs), so the NPC just fires them through the normal moveset pipeline. Because CQC attacks
 * are ticked externally, the Akaza entity drives them via {@code MoveExecutor.tickAttacks(this)}.</p>
 *
 * <p>Zoning comes for free: the Destructive Death CQC attacks spawn forward shockwaves when their
 * user reports {@code ddShockwaveEnabled()} — Akaza always does — and turn those shockwaves red once
 * he enters Overdrive. Snap Punch is his bread-and-butter poke (left click).</p>
 *
 * <p>Two phases are registered as separate switchable movesets ({@link #ID} and
 * {@link #ID_OVERDRIVE}); the Akaza entity switches to the overdrive kit when it enrages.</p>
 */
public class AkazaMoveset extends AbstractMoveset {

    public static final String ID = "akaza";
    public static final String ID_OVERDRIVE = "akaza_overdrive";

    // Wheel indices shared by both phases so the AI can reason about intent regardless of phase:
    //   0 = burst melee, 1 = dash palm, 2 = zoning (forward shockwaves), 3 = signature
    public static final int MOVE_BURST = 0;
    public static final int MOVE_DASH = 1;
    public static final int MOVE_ZONE = 2;
    public static final int MOVE_SIGNATURE = 3;
    public static final int MOVE_FINAL = 4;

    public AkazaMoveset() {
        this(false);
    }

    public AkazaMoveset(boolean overdrive) {
        super(overdrive ? ID_OVERDRIVE : ID,
                overdrive ? "Destructive Death (Overdrive)" : "Destructive Death",
                MovesetType.DEMON,
                createBuilder(overdrive));
    }

    private static MovesetBuilder createBuilder(boolean overdrive) {
        float speed = overdrive ? 1.30f : 1.12f;

        MovesetBuilder builder = new MovesetBuilder()
                .withIdleAnimation("nichirin:demon_idle")
                .withSpeedMultiplier(speed)

                // Left click — Snap Punch: fast poke, launches a short forward shockwave.
                .withMove(new MoveBuilder("snap_punch", "Snap Punch")
                        .withAnimation("snap_punch", 8)
                        .withTiming(10, 2, 8)
                        .withDescription("Quick forward strike that fires a short shockwave.")
                        .withAttack(CqcSnapPunchAttack::new)
                        .asLeftClick())

                // 0 — Crown Splitter: rising burst with a vertical shockwave.
                .withMove(new MoveBuilder("crown_splitter", "Crown Splitter")
                        .withAnimation("crown_splitter", 8)
                        .withTiming(70, 2, 10)
                        .withDescription("Rising reverse axe-kick with a vertical shockwave.")
                        .withAttack(CqcCrownSplitterAttack::new))

                // 1 — Annihilation Type: crouched forward palm dash with twin shockwaves.
                .withMove(new MoveBuilder("annihilation_type", "Annihilation Type")
                        .withAnimation("annihilation_type", 8)
                        .withTiming(90, 4, 16)
                        .withDescription("Forward palm dash with twin circular shockwaves.")
                        .withAttack(CqcAnnihilationTypeAttack::new));

        if (overdrive) {
            // 2 — Eight-Layered Demon Core: overlapping shockwave rings — heavy zoning.
            builder.withMove(new MoveBuilder("eight_layered_demon_core", "Eight-Layered Demon Core")
                            .withAnimation("eight_layered_demon_core", 8)
                            .withTiming(160, 6, 24)
                            .withDescription("Eight stacked punches, each firing a shockwave ring.")
                            .withAttack(CqcEightLayeredDemonCoreAttack::new))
                    // 3 — Donut: point-blank finisher, massive single-target damage.
                    .withMove(new MoveBuilder("donut", "Donut")
                            .withAnimation("annihilation_type", 8)
                            .withTiming(200, 6, 14)
                            .withDescription("Point-blank arm-thrust with huge single-target damage.")
                            .withAttack(CqcDonutAttack::new));
        } else {
            // 2 — Explosive Flurry: three piercing forward shockwaves — his neutral zoning tool.
            builder.withMove(new MoveBuilder("explosive_flurry", "Explosive Flurry")
                            .withAnimation("explosive_flurry", 8)
                            .withTiming(110, 3, 10)
                            .withDescription("Flurry of kicks, each laying a piercing shockwave.")
                            .withAttack(CqcExplosiveFlurryAttack::new))
                    // 3 — Crown Splitter again as the neutral signature slot (strong burst).
                    .withMove(new MoveBuilder("crown_splitter_alt", "Crown Splitter")
                            .withAnimation("crown_splitter", 8)
                            .withTiming(70, 2, 10)
                            .withDescription("Rising reverse axe-kick with a vertical shockwave.")
                            .withAttack(CqcCrownSplitterAttack::new));
        }

        return builder.withMove(new MoveBuilder("blue_silver_chaotic_afterglow", "Blue Silver Chaotic Afterglow")
                .withAnimation("snap_punch", 12)
                .withTiming(0, 0, BlueSilverChaoticAfterglowAttack.TOTAL_TICKS)
                .withDamage(5.0f)
                .withRange(30.0f)
                .withHitStun(40)
                .withHyperArmor()
                .withDescription("Akaza's final omni-directional shockwave barrage.")
                .withAttack(BlueSilverChaoticAfterglowAttack::new));
    }

    @Override
    public int getMoveCount() {
        return 5; // left click + combat slots 0..3 + scripted final attack
    }

    @Override
    public int getRightClickMoveIndex(boolean isCrouching) {
        return -1; // no right-click move — RMB isn't used by the NPC
    }

    @Override
    public String getLeftClickMoveName() { return "Snap Punch"; }
    @Override
    public String getRightClickMoveName() { return "None"; }
    @Override
    public String getCrouchRightClickMoveName() { return "None"; }

    @Override
    public void onMovePerformed(LivingEntity entity, int moveIndex, boolean isCrouching) {
    }
}
