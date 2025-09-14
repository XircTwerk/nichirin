package com.xirc.nichirin.common.util;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.ArrayList;

/**
 * Enhanced data class for configuring attack hitboxes with rotation support
 */
public record HitboxData(float size, Vec3 offset, HitboxShape shape, float duration, boolean rotateWithLook) {

    /**
     * Creates a basic cubic hitbox with 1 tick duration, rotates with look direction
     */
    public HitboxData(float size, Vec3 offset) {
        this(size, offset, HitboxShape.CUBE, 1.0f, true);
    }

    /**
     * Creates a basic cubic hitbox with offset components
     */
    public HitboxData(float size, double offsetX, double offsetY, double offsetZ) {
        this(size, new Vec3(offsetX, offsetY, offsetZ), HitboxShape.CUBE, 1.0f, true);
    }

    /**
     * Creates a basic cubic hitbox at origin
     */
    public HitboxData(float size) {
        this(size, Vec3.ZERO, HitboxShape.CUBE, 1.0f, true);
    }

    /**
     * Creates a hitbox with specified shape
     */
    public HitboxData(float size, HitboxShape shape) {
        this(size, Vec3.ZERO, shape, 1.0f, true);
    }

    /**
     * Creates a non-rotating hitbox (legacy compatibility)
     */
    public static HitboxData nonRotating(float size, HitboxShape shape) {
        return new HitboxData(size, Vec3.ZERO, shape, 1.0f, false);
    }

    /**
     * Generates the AABB for this hitbox at the given position
     * Legacy method - doesn't support rotation
     */
    public AABB createAABB(Vec3 center) {
        return createAABB(center, Vec3.ZERO, 0.0f);
    }

    /**
     * Generates the AABB for this hitbox at the given position with rotation support
     * @param center The center position of the hitbox
     * @param lookDirection The entity's look direction (normalized)
     * @param yaw The entity's yaw rotation in radians
     */
    public AABB createAABB(Vec3 center, Vec3 lookDirection, float yaw) {
        Vec3 finalOffset = offset;

        // Apply rotation to offset if enabled
        if (rotateWithLook && !offset.equals(Vec3.ZERO)) {
            finalOffset = rotateVectorAroundY(offset, yaw);
        }

        Vec3 finalCenter = center.add(finalOffset);

        if (!rotateWithLook) {
            // Use original axis-aligned logic
            return createAxisAlignedAABB(finalCenter);
        }

        // Create rotated hitbox
        return createRotatedAABB(finalCenter, lookDirection, yaw);
    }

    /**
     * Creates an axis-aligned bounding box (original behavior)
     */
    private AABB createAxisAlignedAABB(Vec3 center) {
        float halfSize = size / 2.0f;

        return switch (shape) {
            case CUBE -> new AABB(
                    center.x - halfSize,
                    center.y - halfSize,
                    center.z - halfSize,
                    center.x + halfSize,
                    center.y + halfSize,
                    center.z + halfSize
            );

            case WIDE -> new AABB(
                    center.x - halfSize * 1.5,
                    center.y - halfSize * 0.5,
                    center.z - halfSize,
                    center.x + halfSize * 1.5,
                    center.y + halfSize * 0.5,
                    center.z + halfSize
            );

            case TALL -> new AABB(
                    center.x - halfSize * 0.5,
                    center.y - halfSize * 1.5,
                    center.z - halfSize * 0.5,
                    center.x + halfSize * 0.5,
                    center.y + halfSize * 1.5,
                    center.z + halfSize * 0.5
            );

            case LONG -> new AABB(
                    center.x - halfSize * 0.5,
                    center.y - halfSize * 0.5,
                    center.z - halfSize * 1.5,
                    center.x + halfSize * 0.5,
                    center.y + halfSize * 0.5,
                    center.z + halfSize * 1.5
            );
        };
    }

    /**
     * Creates a rotated bounding box by calculating all corner points
     */
    private AABB createRotatedAABB(Vec3 center, Vec3 lookDirection, float yaw) {
        // Get the base dimensions for this shape
        Vec3 dimensions = getShapeDimensions();

        // Create the 8 corner points of the hitbox in local space
        List<Vec3> corners = createLocalCorners(dimensions);

        // Rotate each corner and find the bounding box
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE, maxZ = Double.MIN_VALUE;

        for (Vec3 corner : corners) {
            // Rotate the corner point around Y axis
            Vec3 rotatedCorner = rotateVectorAroundY(corner, yaw);

            // Translate to world position
            Vec3 worldCorner = center.add(rotatedCorner);

            // Update bounding box
            minX = Math.min(minX, worldCorner.x);
            minY = Math.min(minY, worldCorner.y);
            minZ = Math.min(minZ, worldCorner.z);
            maxX = Math.max(maxX, worldCorner.x);
            maxY = Math.max(maxY, worldCorner.y);
            maxZ = Math.max(maxZ, worldCorner.z);
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Get the dimensions for each shape type
     */
    private Vec3 getShapeDimensions() {
        float halfSize = size / 2.0f;

        return switch (shape) {
            case CUBE -> new Vec3(halfSize, halfSize, halfSize);
            case WIDE -> new Vec3(halfSize * 1.5, halfSize * 0.5, halfSize);
            case TALL -> new Vec3(halfSize * 0.5, halfSize * 1.5, halfSize * 0.5);
            case LONG -> new Vec3(halfSize * 0.5, halfSize * 0.5, halfSize * 1.5);
        };
    }

    /**
     * Create the 8 corner points of a box in local space
     */
    private List<Vec3> createLocalCorners(Vec3 dimensions) {
        List<Vec3> corners = new ArrayList<>();

        // All 8 combinations of +/- for each dimension
        for (int x = -1; x <= 1; x += 2) {
            for (int y = -1; y <= 1; y += 2) {
                for (int z = -1; z <= 1; z += 2) {
                    corners.add(new Vec3(
                            dimensions.x * x,
                            dimensions.y * y,
                            dimensions.z * z
                    ));
                }
            }
        }

        return corners;
    }

    /**
     * Rotate a vector around the Y axis by the given angle in radians
     */
    private Vec3 rotateVectorAroundY(Vec3 vector, double angleRadians) {
        double cos = Math.cos(angleRadians);
        double sin = Math.sin(angleRadians);

        double newX = vector.x * cos - vector.z * sin;
        double newZ = vector.x * sin + vector.z * cos;

        return new Vec3(newX, vector.y, newZ);
    }

    /**
     * Convenience method to create AABB from an entity's position and look direction
     */
    public AABB createAABBFromEntity(Entity entity) {
        Vec3 center = entity.position().add(0, entity.getBbHeight() / 2, 0);
        Vec3 lookDirection = entity.getLookAngle();
        float yaw = (float) Math.toRadians(entity.getYRot());

        return createAABB(center, lookDirection, yaw);
    }

    /**
     * Create a new HitboxData with modified size
     */
    public HitboxData withSize(float newSize) {
        return new HitboxData(newSize, offset, shape, duration, rotateWithLook);
    }

    /**
     * Create a new HitboxData with modified offset
     */
    public HitboxData withOffset(Vec3 newOffset) {
        return new HitboxData(size, newOffset, shape, duration, rotateWithLook);
    }

    /**
     * Create a new HitboxData with modified shape
     */
    public HitboxData withShape(HitboxShape newShape) {
        return new HitboxData(size, offset, newShape, duration, rotateWithLook);
    }

    /**
     * Create a new HitboxData with modified duration
     */
    public HitboxData withDuration(float newDuration) {
        return new HitboxData(size, offset, shape, newDuration, rotateWithLook);
    }

    /**
     * Create a new HitboxData with rotation enabled/disabled
     */
    public HitboxData withRotation(boolean shouldRotate) {
        return new HitboxData(size, offset, shape, duration, shouldRotate);
    }

    /**
     * Different hitbox shapes for various attack types
     */
    public enum HitboxShape {
        CUBE,    // Standard cubic hitbox
        WIDE,    // Horizontal slash (rotates to follow player direction)
        TALL,    // Vertical slash (rotates to follow player direction)
        LONG     // Thrust/stab (rotates to follow player direction)
    }
}