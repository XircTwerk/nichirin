package com.xirc.nichirin.common.util;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Data class for configuring attack hitboxes
 */
public record HitboxData(float size, Vec3 offset, HitboxShape shape, float duration) {

    /**
     * Creates a basic cubic hitbox with 1 tick duration
     */
    public HitboxData(float size, Vec3 offset) {
        this(size, offset, HitboxShape.CUBE, 1.0f);
    }

    /**
     * Creates a basic cubic hitbox with offset components
     */
    public HitboxData(float size, double offsetX, double offsetY, double offsetZ) {
        this(size, new Vec3(offsetX, offsetY, offsetZ), HitboxShape.CUBE, 1.0f);
    }

    /**
     * Creates a basic cubic hitbox at origin
     */
    public HitboxData(float size) {
        this(size, Vec3.ZERO, HitboxShape.CUBE, 1.0f);
    }

    /**
     * Creates a hitbox with specified shape
     */
    public HitboxData(float size, HitboxShape shape) {
        this(size, Vec3.ZERO, shape, 1.0f);
    }

    /**
     * Generates the AABB for this hitbox at the given position
     */
    public AABB createAABB(Vec3 center) {
        Vec3 finalCenter = center.add(offset);
        float halfSize = size / 2.0f;

        return switch (shape) {
            case CUBE -> new AABB(
                    finalCenter.x - halfSize,
                    finalCenter.y - halfSize,
                    finalCenter.z - halfSize,
                    finalCenter.x + halfSize,
                    finalCenter.y + halfSize,
                    finalCenter.z + halfSize
            );

            case WIDE -> new AABB(
                    finalCenter.x - halfSize * 1.5,
                    finalCenter.y - halfSize * 0.5,
                    finalCenter.z - halfSize,
                    finalCenter.x + halfSize * 1.5,
                    finalCenter.y + halfSize * 0.5,
                    finalCenter.z + halfSize
            );

            case TALL -> new AABB(
                    finalCenter.x - halfSize * 0.5,
                    finalCenter.y - halfSize * 1.5,
                    finalCenter.z - halfSize * 0.5,
                    finalCenter.x + halfSize * 0.5,
                    finalCenter.y + halfSize * 1.5,
                    finalCenter.z + halfSize * 0.5
            );

            case LONG -> new AABB(
                    finalCenter.x - halfSize * 0.5,
                    finalCenter.y - halfSize * 0.5,
                    finalCenter.z - halfSize * 1.5,
                    finalCenter.x + halfSize * 0.5,
                    finalCenter.y + halfSize * 0.5,
                    finalCenter.z + halfSize * 1.5
            );
        };
    }

    /**
     * Create a new HitboxData with modified size
     */
    public HitboxData withSize(float newSize) {
        return new HitboxData(newSize, offset, shape, duration);
    }

    /**
     * Create a new HitboxData with modified offset
     */
    public HitboxData withOffset(Vec3 newOffset) {
        return new HitboxData(size, newOffset, shape, duration);
    }

    /**
     * Create a new HitboxData with modified shape
     */
    public HitboxData withShape(HitboxShape newShape) {
        return new HitboxData(size, offset, newShape, duration);
    }

    /**
     * Create a new HitboxData with modified duration
     */
    public HitboxData withDuration(float newDuration) {
        return new HitboxData(size, offset, shape, newDuration);
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