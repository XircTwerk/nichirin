package com.xirc.nichirin.common.attack.moveset;

import com.xirc.nichirin.common.gyomei.GyomeiAttackController.Attack;
import com.xirc.nichirin.common.gyomei.GyomeiWeaponManager;
import net.minecraft.server.level.ServerPlayer;

/**
 * Gyomei Himejima's ball-and-chain-axe as a proper moveset. Unlike the breathing/demon movesets, its
 * attacks are PHYSICS-driven: each move drives the weapon simulation's control point (see
 * {@link com.xirc.nichirin.common.gyomei.GyomeiAttackController}) and damage comes from the simulated
 * ends' velocity. The moveset layer exists so the weapon plugs into the same cooldown, HUD, animation,
 * GUI and progression systems as every other moveset.
 *
 * <p>Two stances, switched by sheathing with the weapon in hand:</p>
 * <ul>
 *   <li><b>Axe</b> — M1 {@code axe_hack}, M2 {@code flail_lob} (crouch+M2 held reels the ball in).</li>
 *   <li><b>Flail</b> (grip the middle of the chain) — M1 {@code reaping_sweep}, M2 {@code meteor_drop},
 *       crouch+M1 {@code rising_crescent}, crouch+M2 {@code twin_cyclone}.</li>
 * </ul>
 *
 * <p>Each move id doubles as its PlayerAnimator animation name (see
 * {@code assets/nichirin/player_animations/chain_ball_axe.json}). Input is routed here by
 * {@link GyomeiWeaponManager#triggerAttack} based on the active stance.</p>
 */
public class ChainBallAxeMoveset extends AbstractMoveset {

    public static final String ID = "chain_ball_axe";

    public ChainBallAxeMoveset() {
        super(ID, "Chain Ball Axe", MovesetType.NEUTRAL, createBuilder());
    }

    private static MovesetBuilder createBuilder() {
        return new MovesetBuilder()
                // Axe stance
                .withMove(physicsMove("axe_hack", "Axe Hack", Attack.AXE_HACK, 12, 4, 12, 8.0f,
                        "Axe stance — overhead chop with the axe head."))
                .withMove(physicsMove("flail_lob", "Flail Lob", Attack.LOB, 22, 3, 12, 6.0f,
                        "Axe stance — lob the spiked ball at where you aim; it swings back on the chain."))
                // Flail stance (grip the middle of the chain)
                .withMove(physicsMove("reaping_sweep", "Reaping Sweep", Attack.REAPING_SWEEP, 10, 3, 12, 6.0f,
                        "Flail stance — wide horizontal ball swing across the front."))
                .withMove(physicsMove("meteor_drop", "Meteor Drop", Attack.METEOR_DROP, 25, 5, 16, 9.0f,
                        "Flail stance — whirl the ball overhead and slam it straight down."))
                .withMove(physicsMove("rising_crescent", "Rising Crescent", Attack.RISING_CRESCENT, 18, 4, 14, 6.5f,
                        "Flail stance — swing the ball from low to high, launching the enemy upward."))
                .withMove(physicsMove("twin_cyclone", "Twin Cyclone", Attack.TWIN_CYCLONE, 45, 4, 30, 5.0f,
                        "Flail stance — spin the whole chain around you; ball and axe scythe a full circle."));
    }

    private static MoveConfiguration physicsMove(String id, String name, Attack attack, int cooldown,
                                                 int windup, int duration, float damage, String description) {
        return new MoveBuilder(id, name)
                .withTiming(cooldown, windup, duration)
                .withDamage(damage)
                .withDescription(description)
                .withAction(entity -> {
                    if (entity instanceof ServerPlayer player) {
                        GyomeiWeaponManager.fireMovesetAttack(player, attack, cooldown, name);
                    }
                })
                .build();
    }
}
