package com.xirc.nichirin.client.vfx;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Frame-sampled ribbons swept by the complete animated Blade bone. */
@Environment(EnvType.CLIENT)
public final class BladeTrailRenderer {

    private static final Impl INSTANCE = new Impl();

    private BladeTrailRenderer() {}

    public static void capture(UUID player, ItemDisplayContext context, Vec3 base, Vec3 tip,
                               BladeTrailProfiles.Profile profile) {
        if (!isHandContext(context)) return;
        INSTANCE.capture(new Key(player, context), base, tip, profile);
    }

    public static boolean hasTrails() {
        return INSTANCE.hasTrails();
    }

    public static void render(PoseStack poseStack, Camera camera) {
        INSTANCE.render(poseStack, camera);
    }

    private static boolean isHandContext(ItemDisplayContext context) {
        return context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }

    private record Key(UUID player, ItemDisplayContext context) {}

    private static final class Impl extends AbstractRibbonTrailRenderer<Key> {
        @Override
        protected UUID keyOwner(Key key) {
            return key.player();
        }
    }
}
