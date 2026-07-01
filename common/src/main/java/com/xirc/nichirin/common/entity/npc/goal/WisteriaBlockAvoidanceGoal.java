package com.xirc.nichirin.common.entity.npc.goal;

import com.xirc.nichirin.common.entity.npc.DemonNPCEntity;
import com.xirc.nichirin.common.util.WisteriaBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class WisteriaBlockAvoidanceGoal extends Goal {

    private static final int SEARCH_RADIUS = 8;
    private static final int VERTICAL_RADIUS = 5;
    private static final double AVOID_DISTANCE = 10.0D;

    private final DemonNPCEntity demon;
    private final double speedModifier;
    private Vec3 avoidTarget;

    public WisteriaBlockAvoidanceGoal(DemonNPCEntity demon, double speedModifier) {
        this.demon = demon;
        this.speedModifier = speedModifier;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        avoidTarget = findAvoidTarget();
        return avoidTarget != null;
    }

    @Override
    public boolean canContinueToUse() {
        return !demon.getNavigation().isDone() && isNearWisteria();
    }

    @Override
    public void start() {
        demon.setTarget(null);
        demon.getNavigation().moveTo(avoidTarget.x, avoidTarget.y, avoidTarget.z, speedModifier);
    }

    @Override
    public void tick() {
        demon.setTarget(null);
        if (demon.getNavigation().isDone()) {
            avoidTarget = findAvoidTarget();
            if (avoidTarget != null) {
                demon.getNavigation().moveTo(avoidTarget.x, avoidTarget.y, avoidTarget.z, speedModifier);
            }
        }
    }

    @Override
    public void stop() {
        avoidTarget = null;
    }

    private Vec3 findAvoidTarget() {
        Vec3 push = findPushVector();
        if (push == null) return null;

        Vec3 base = demon.position().add(push.normalize().scale(AVOID_DISTANCE));
        Level level = demon.level();
        for (int i = 0; i < 8; i++) {
            double x = base.x + demon.getRandom().nextIntBetweenInclusive(-3, 3);
            double z = base.z + demon.getRandom().nextIntBetweenInclusive(-3, 3);
            BlockPos pos = BlockPos.containing(x, demon.getY(), z);
            BlockPos surface = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos);
            if (!WisteriaBlocks.isWisteriaBlock(level.getBlockState(surface))
                    && !WisteriaBlocks.isWisteriaBlock(level.getBlockState(surface.below()))) {
                return Vec3.atBottomCenterOf(surface);
            }
        }
        return base;
    }

    private Vec3 findPushVector() {
        Level level = demon.level();
        BlockPos center = demon.blockPosition();
        Vec3 push = Vec3.ZERO;
        boolean found = false;

        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-SEARCH_RADIUS, -VERTICAL_RADIUS, -SEARCH_RADIUS),
                center.offset(SEARCH_RADIUS, VERTICAL_RADIUS, SEARCH_RADIUS))) {
            if (!WisteriaBlocks.isWisteriaBlock(level.getBlockState(pos))) continue;
            Vec3 away = demon.position().subtract(Vec3.atCenterOf(pos));
            double distanceSqr = Math.max(0.25D, away.lengthSqr());
            push = push.add(away.normalize().scale(1.0D / distanceSqr));
            found = true;
        }
        return found && push.lengthSqr() > 1.0E-4D ? push : null;
    }

    private boolean isNearWisteria() {
        return findPushVector() != null;
    }
}
