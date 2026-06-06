package com.xirc.nichirin.common.attack.moveset.demon;

import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Player-facing default demon moveset.
 *
 * Physical demon attacks live in CQC now; demon movesets are reserved for blood demon art content.
 */
public class DefaultDemonMoveset extends AbstractMoveset {

    public DefaultDemonMoveset() {
        super("default_demon", "Demon Arts", MovesetType.DEMON, createBuilder());
    }

    private static MovesetBuilder createBuilder() {
        return new MovesetBuilder()
                .withIdleAnimation("nichirin:demon_idle")
                .withSpeedMultiplier(1.05f);
    }

    @Override
    public boolean handleLeftClick(LivingEntity entity) {
        return false;
    }

    @Override
    public boolean handleRightClick(LivingEntity entity, boolean isCrouching) {
        return false;
    }

    @Override
    public void performMove(LivingEntity entity, int moveIndex) {
    }

    @Override
    public String getLeftClickMoveName() {
        return "None";
    }

    @Override
    public String getRightClickMoveName() {
        return "None";
    }

    @Override
    public String getCrouchRightClickMoveName() {
        return "None";
    }

    public static void tickEntity(LivingEntity entity) {
    }

    public static void tickPlayer(Player player) {
    }

    public static void resetCooldowns(LivingEntity entity) {
    }

    public static void cleanupEntity(LivingEntity entity) {
    }

    public static void cleanupPlayer(Player player) {
    }
}
