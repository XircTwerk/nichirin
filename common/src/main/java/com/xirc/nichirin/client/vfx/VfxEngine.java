package com.xirc.nichirin.client.vfx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xirc.nichirin.client.vfx.effect.WaterSurfaceSlashEffect;
import com.xirc.nichirin.client.vfx.effect.WaterTechniqueEffect;
import com.xirc.nichirin.client.vfx.effect.FlameTechniqueEffect;
import com.xirc.nichirin.common.vfx.VfxIds;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public final class VfxEngine {
    private static final double MAX_RENDER_DISTANCE_SQR = 160.0 * 160.0;
    private static final int MAX_ACTIVE_EFFECTS = 256;
    private static final Map<ResourceLocation, Supplier<? extends VfxEffect>> EFFECTS = new HashMap<>();
    private static final List<VfxInstance> ACTIVE = new ArrayList<>();

    private VfxEngine() {}

    public static void init() {
        register(VfxIds.WATER_SURFACE_SLASH, WaterSurfaceSlashEffect::new);
        register(VfxIds.WATER_WHEEL, () -> new WaterTechniqueEffect(WaterTechniqueEffect.Style.WHEEL));
        register(VfxIds.DROP_RIPPLE_THRUST, () -> new WaterTechniqueEffect(WaterTechniqueEffect.Style.RIPPLE_THRUST));
        register(VfxIds.FLOWING_DANCE, () -> new WaterTechniqueEffect(WaterTechniqueEffect.Style.FLOWING_DANCE));
        register(VfxIds.STRIKING_TIDE, () -> new WaterTechniqueEffect(WaterTechniqueEffect.Style.STRIKING_TIDE));
        register(VfxIds.WATERFALL_BASIN, () -> new WaterTechniqueEffect(WaterTechniqueEffect.Style.WATERFALL));
        register(VfxIds.SPLASHING_WATER_FLOW, () -> new WaterTechniqueEffect(WaterTechniqueEffect.Style.SPLASHING_FLOW));
        register(VfxIds.WHIRLPOOL, () -> new WaterTechniqueEffect(WaterTechniqueEffect.Style.WHIRLPOOL));
        register(VfxIds.BLESSED_RAIN, () -> new WaterTechniqueEffect(WaterTechniqueEffect.Style.BLESSED_RAIN));
        register(VfxIds.CONSTANT_FLUX, () -> new WaterTechniqueEffect(WaterTechniqueEffect.Style.CONSTANT_FLUX));
        register(VfxIds.DEAD_CALM, () -> new WaterTechniqueEffect(WaterTechniqueEffect.Style.DEAD_CALM));
        register(VfxIds.UNKNOWING_FIRE, () -> new FlameTechniqueEffect(FlameTechniqueEffect.Style.UNKNOWING_FIRE));
        register(VfxIds.RISING_SCORCHING_SUN, () -> new FlameTechniqueEffect(FlameTechniqueEffect.Style.RISING_SUN));
        register(VfxIds.BLAZING_UNIVERSE, () -> new FlameTechniqueEffect(FlameTechniqueEffect.Style.BLAZING_UNIVERSE));
        register(VfxIds.BLOOMING_FLAME_UNDULATION, () -> new FlameTechniqueEffect(FlameTechniqueEffect.Style.BLOOMING));
        register(VfxIds.FLAME_TIGER, () -> new FlameTechniqueEffect(FlameTechniqueEffect.Style.FLAME_TIGER));
        register(VfxIds.RENGOKU, () -> new FlameTechniqueEffect(FlameTechniqueEffect.Style.RENGOKU));
        register(VfxIds.FLAME_POMMEL_SLASH, () -> new FlameTechniqueEffect(FlameTechniqueEffect.Style.POMMEL_SLASH));
    }

    public static void register(ResourceLocation id, Supplier<? extends VfxEffect> factory) {
        EFFECTS.put(id, factory);
    }

    public static void spawn(ResourceLocation id, Vec3 origin, Vec3 direction, float scale, long seed, int entityId) {
        Supplier<? extends VfxEffect> factory = EFFECTS.get(id);
        Minecraft minecraft = Minecraft.getInstance();
        if (factory == null || minecraft.level == null) return;
        if (ACTIVE.size() >= MAX_ACTIVE_EFFECTS) ACTIVE.remove(0);
        ACTIVE.add(new VfxInstance(id, factory.get(), origin, direction, scale, seed, entityId));
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            ACTIVE.clear();
            return;
        }
        ACTIVE.forEach(instance -> {
            VfxAccentParticles.tick(instance);
            instance.tick();
        });
        ACTIVE.removeIf(VfxInstance::isFinished);
    }

    public static void render(PoseStack poseStack, Camera camera, float partialTick) {
        if (ACTIVE.isEmpty()) return;
        Vec3 cameraPosition = camera.getPosition();
        for (VfxInstance instance : List.copyOf(ACTIVE)) {
            if (instance.origin().distanceToSqr(cameraPosition) <= MAX_RENDER_DISTANCE_SQR) {
                instance.effect().render(instance, poseStack, camera, partialTick);
            }
        }
    }

    public static boolean hasActiveEffects() {
        return !ACTIVE.isEmpty();
    }
}
