package com.xirc.nichirin.common.gyomei;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Resolves a Verlet point (treated as a small sphere) out of block collision by minimum-translation
 * push-out. Because Verlet keeps velocity implicit in {@code previousPosition}, simply pushing the point
 * to the surface makes it slide/drag along terrain with almost no bounce — exactly the flail/chain feel
 * the spec asks for.
 */
public final class GyomeiCollision {

    private GyomeiCollision() {}

    /**
     * Swept push-out: walks from {@code from} to {@code to} in steps no larger than the sphere radius,
     * resolving at each and stopping at the first surface hit. This is what keeps a fast-falling flail or
     * whipped chain from tunnelling clean through a floor in one tick (which plain destination-only
     * resolution can't catch — once the centre passes a block's midline the push-out shoves it out the
     * far side, so it "noclips through the ground").
     */
    public static Vec3 resolveSwept(Level level, Vec3 from, Vec3 to, double radius) {
        Vec3 delta = to.subtract(from);
        double dist = delta.length();
        double step = Math.max(0.15, radius * 0.75);
        if (dist <= step) return resolve(level, to, radius);

        int steps = (int) Math.ceil(dist / step);
        Vec3 inc = delta.scale(1.0 / steps);
        Vec3 pos = from;
        for (int i = 0; i < steps; i++) {
            Vec3 next = pos.add(inc);
            Vec3 resolved = resolve(level, next, radius);
            if (resolved.distanceToSqr(next) > 1.0e-10) {
                return resolved; // first contact — settle here instead of advancing through the block
            }
            pos = next;
        }
        return resolve(level, to, radius);
    }

    public static Vec3 resolve(Level level, Vec3 pos, double radius) {
        double px = pos.x, py = pos.y, pz = pos.z;

        // A couple of passes so wedged corners settle instead of jittering on one axis.
        for (int pass = 0; pass < 3; pass++) {
            AABB box = new AABB(px - radius, py - radius, pz - radius, px + radius, py + radius, pz + radius);
            boolean moved = false;

            for (VoxelShape shape : level.getBlockCollisions(null, box)) {
                for (AABB block : shape.toAabbs()) {
                    if (!box.intersects(block)) continue;

                    // Signed minimum-translation distance out of this block on each axis.
                    double pushXpos = block.maxX - (px - radius); // move +x until clear
                    double pushXneg = (px + radius) - block.minX; // move -x until clear
                    double pushYpos = block.maxY - (py - radius);
                    double pushYneg = (py + radius) - block.minY;
                    double pushZpos = block.maxZ - (pz - radius);
                    double pushZneg = (pz + radius) - block.minZ;

                    double mtvX = pushXpos < pushXneg ? pushXpos : -pushXneg;
                    double mtvY = pushYpos < pushYneg ? pushYpos : -pushYneg;
                    double mtvZ = pushZpos < pushZneg ? pushZpos : -pushZneg;

                    double ax = Math.abs(mtvX), ay = Math.abs(mtvY), az = Math.abs(mtvZ);
                    if (ax <= ay && ax <= az) {
                        px += mtvX;
                    } else if (ay <= az) {
                        py += mtvY;
                    } else {
                        pz += mtvZ;
                    }
                    box = new AABB(px - radius, py - radius, pz - radius, px + radius, py + radius, pz + radius);
                    moved = true;
                }
            }
            if (!moved) break;
        }
        return new Vec3(px, py, pz);
    }
}
