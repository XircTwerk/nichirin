package com.xirc.nichirin.common.vfx;

import com.xirc.nichirin.BreathOfNichirin;
import net.minecraft.resources.ResourceLocation;

public final class VfxIds {
    public static final ResourceLocation WATER_SURFACE_SLASH =
            ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "water_surface_slash");
    public static final ResourceLocation WATER_WHEEL = id("water_wheel");
    public static final ResourceLocation DROP_RIPPLE_THRUST = id("drop_ripple_thrust");
    public static final ResourceLocation FLOWING_DANCE = id("flowing_dance");
    public static final ResourceLocation STRIKING_TIDE = id("striking_tide");
    public static final ResourceLocation WATERFALL_BASIN = id("waterfall_basin");
    public static final ResourceLocation SPLASHING_WATER_FLOW = id("splashing_water_flow");
    public static final ResourceLocation WHIRLPOOL = id("whirlpool");
    public static final ResourceLocation BLESSED_RAIN = id("blessed_rain");
    public static final ResourceLocation CONSTANT_FLUX = id("constant_flux");
    public static final ResourceLocation DEAD_CALM = id("dead_calm");
    public static final ResourceLocation UNKNOWING_FIRE = id("unknowing_fire");
    public static final ResourceLocation RISING_SCORCHING_SUN = id("rising_scorching_sun");
    public static final ResourceLocation BLAZING_UNIVERSE = id("blazing_universe");
    public static final ResourceLocation BLOOMING_FLAME_UNDULATION = id("blooming_flame_undulation");
    public static final ResourceLocation FLAME_TIGER = id("flame_tiger");
    public static final ResourceLocation RENGOKU = id("rengoku");
    public static final ResourceLocation FLAME_POMMEL_SLASH = id("flame_pommel_slash");

    private VfxIds() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, path);
    }
}
