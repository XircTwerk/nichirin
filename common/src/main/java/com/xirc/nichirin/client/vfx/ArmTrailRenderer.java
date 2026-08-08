package com.xirc.nichirin.client.vfx;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Frame-sampled ribbons swept by a player's arms (used by Destructive Death). Same engine as
 * {@link BladeTrailRenderer}, keyed per player + strand. Each arm sweeps several strands (one per
 * lower-arm corner) so the trails wrap around the arm's volume instead of being a single flat sheet.
 */
@Environment(EnvType.CLIENT)
public final class ArmTrailRenderer {

    /** Strands per arm — ribbons spaced around the lower-arm perimeter so they envelope the whole box. */
    public static final int STRANDS_PER_ARM = 8;

    private static final Impl INSTANCE = new Impl();

    private ArmTrailRenderer() {}

    public static void capture(UUID player, int strand, Vec3 base, Vec3 tip,
                               BladeTrailProfiles.Profile profile) {
        INSTANCE.capture(new Key(player, strand), base, tip, profile);
    }

    public static boolean hasTrails() {
        return INSTANCE.hasTrails();
    }

    public static void render(PoseStack poseStack, Camera camera) {
        INSTANCE.render(poseStack, camera);
    }

    private record Key(UUID player, int strand) {}

    private static final class Impl extends AbstractRibbonTrailRenderer<Key> {
        @Override
        protected UUID keyOwner(Key key) {
            return key.player();
        }
    }
}
