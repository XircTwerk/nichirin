package com.xirc.nichirin.common.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Data class for configuring attack hitboxes
 */
@Getter
@AllArgsConstructor
@With
public class HitboxData {
    private final float size;
    private final Vec3 offset;
    private final HitboxShape shape;
    private final float duration; // How long the hitbox stays active

    /**
     * Creates a basic cubic hitbox
     */
    public HitboxData(float size, Vec3 offset) {
        this(size, offset, HitboxShape.CUBE, 1.0f);
    }

    /**
     * Creates a basic cubic hitbox with offset components
     */
    public HitboxData(float size, double offsetX, double offsetY, double offsetZ) {
        this(size, new Vec3(offsetX, offsetY, offsetZ));
    }

    /**
     * Generates the AABB for this hitbox at the given position
     */
    public AABB createAABB(Vec3 center) {
        Vec3 finalCenter = center.add(offset);

        switch (shape) {
            case CUBE:
                return new AABB(
                        finalCenter.x - size,
                        finalCenter.y - size,
                        finalCenter.z - size,
                        finalCenter.x + size,
                        finalCenter.y + size,
                        finalCenter.z + size
                );

            case WIDE:
                return new AABB(
                        finalCenter.x - size * 1.5,
                        finalCenter.y - size * 0.5,
                        finalCenter.z - size,
                        finalCenter.x + size * 1.5,
                        finalCenter.y + size * 0.5,
                        finalCenter.z + size
                );

            case TALL:
                return new AABB(
                        finalCenter.x - size * 0.5,
                        finalCenter.y - size * 1.5,
                        finalCenter.z - size * 0.5,
                        finalCenter.x + size * 0.5,
                        finalCenter.y + size * 1.5,
                        finalCenter.z + size * 0.5
                );

            case LONG:
                return new AABB(
                        finalCenter.x - size * 0.5,
                        finalCenter.y - size * 0.5,
                        finalCenter.z - size * 1.5,
                        finalCenter.x + size * 0.5,
                        finalCenter.y + size * 0.5,
                        finalCenter.z + size * 1.5
                );

            default:
                return createAABB(center); // Fallback to cube
        }
    }

    /**
     * Different hitbox shapes for various attack types
     */
    public enum HitboxShape {
        CUBE,    // Standard cubic hitbox
        WIDE,    // Horizontal slash
        TALL,    // Vertical slash
        LONG     // Thrust/stab
    }
}