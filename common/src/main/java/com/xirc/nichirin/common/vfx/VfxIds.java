package com.xirc.nichirin.common.vfx;

import com.xirc.nichirin.BreathOfNichirin;
import net.minecraft.resources.ResourceLocation;

public final class VfxIds {
    public static final ResourceLocation WATER_SURFACE_SLASH =
            ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "water_surface_slash");
    public static final ResourceLocation WATER_SURFACE_SLASH_REVERSE = id("water_surface_slash_reverse");
    public static final ResourceLocation WATER_WHEEL = id("water_wheel");
    public static final ResourceLocation DROP_RIPPLE_THRUST = id("drop_ripple_thrust");
    public static final ResourceLocation FLOWING_DANCE = id("flowing_dance");
    public static final ResourceLocation STRIKING_TIDE = id("striking_tide");
    public static final ResourceLocation WATERFALL_BASIN = id("waterfall_basin");
    public static final ResourceLocation SPLASHING_WATER_FLOW = id("splashing_water_flow");
    public static final ResourceLocation WHIRLPOOL = id("whirlpool");
    public static final ResourceLocation BLESSED_RAIN = id("blessed_rain");
    public static final ResourceLocation BLESSED_RAIN_LEAP = id("blessed_rain_leap");
    public static final ResourceLocation CONSTANT_FLUX = id("constant_flux");
    public static final ResourceLocation DEAD_CALM = id("dead_calm");
    public static final ResourceLocation UNKNOWING_FIRE = id("unknowing_fire");
    public static final ResourceLocation RISING_SCORCHING_SUN = id("rising_scorching_sun");
    public static final ResourceLocation BLAZING_UNIVERSE = id("blazing_universe");
    public static final ResourceLocation BLAZING_UNIVERSE_IMPACT = id("blazing_universe_impact");
    public static final ResourceLocation BLOOMING_FLAME_UNDULATION = id("blooming_flame_undulation");
    public static final ResourceLocation FLAME_TIGER = id("flame_tiger");
    public static final ResourceLocation RENGOKU = id("rengoku");
    public static final ResourceLocation FLAME_POMMEL_SLASH = id("flame_pommel_slash");
    public static final ResourceLocation THUNDERCLAP_FLASH = id("thunderclap_flash");
    public static final ResourceLocation GODSPEED = id("godspeed");
    public static final ResourceLocation RICE_SPIRIT_SLASH = id("rice_spirit_slash");
    public static final ResourceLocation THUNDER_SWARM_SLASH = id("thunder_swarm_slash");
    public static final ResourceLocation DISTANT_THUNDER_CHARGE = id("distant_thunder_charge");
    public static final ResourceLocation HEAT_LIGHTNING_RISE = id("heat_lightning_rise");
    public static final ResourceLocation THUNDER_STRIKE_WARNING = id("thunder_strike_warning");
    public static final ResourceLocation THUNDER_STRIKE = id("thunder_strike");
    public static final ResourceLocation HONOIKAZUCHI_NO_KAMI = id("honoikazuchi_no_kami");
    public static final ResourceLocation HONOIKAZUCHI_IMPACT = id("honoikazuchi_impact");
    public static final ResourceLocation LOW_CLOUDS_DISTANT_HAZE = id("low_clouds_distant_haze");
    public static final ResourceLocation EIGHT_LAYERED_MIST = id("eight_layered_mist");
    public static final ResourceLocation SCATTERING_MIST_SPLASH = id("scattering_mist_splash");
    public static final ResourceLocation SHIFTING_FLOW_SLASH = id("shifting_flow_slash");
    public static final ResourceLocation SEA_OF_CLOUDS_AND_HAZE = id("sea_of_clouds_and_haze");
    public static final ResourceLocation LUNAR_DISPERSING_MIST = id("lunar_dispersing_mist");
    public static final ResourceLocation MIST_FINISHER = id("mist_finisher");
    public static final ResourceLocation OBSCURING_CLOUDS = id("obscuring_clouds");
    public static final ResourceLocation BEAST_PIERCE = id("beast_pierce");
    public static final ResourceLocation BEAST_X_SLICE = id("beast_x_slice");
    public static final ResourceLocation BEAST_EXPLOSIVE_RUSH = id("beast_explosive_rush");
    public static final ResourceLocation BEAST_DEVOUR = id("beast_devour");
    public static final ResourceLocation BEAST_RAPID_SLASH = id("beast_rapid_slash");
    public static final ResourceLocation BEAST_CRAZY_CUTTING = id("beast_crazy_cutting");
    public static final ResourceLocation BEAST_PALISADE_BITE = id("beast_palisade_bite");
    public static final ResourceLocation BEAST_SPATIAL_AWARENESS = id("beast_spatial_awareness");
    public static final ResourceLocation BEAST_BENDY_SLASH = id("beast_bendy_slash");
    public static final ResourceLocation BEAST_WHIRLING_FANGS = id("beast_whirling_fangs");
    public static final ResourceLocation BEAST_THROWING_STRIKE = id("beast_throwing_strike");
    public static final ResourceLocation SOUND_RESONDING_SLASHES = id("sound_resounding_slashes");
    public static final ResourceLocation SOUND_RHYTHMIC_STEP = id("sound_rhythmic_step");
    public static final ResourceLocation SOUND_ROAR = id("sound_roar");
    public static final ResourceLocation SOUND_STRING_PERFORMANCE = id("sound_string_performance");
    public static final ResourceLocation SOUND_TEMPO_BREAKER = id("sound_tempo_breaker");
    public static final ResourceLocation SOUND_IMPACT = id("sound_impact");
    public static final ResourceLocation INSECT_QUICK_STING = id("insect_quick_sting");
    public static final ResourceLocation INSECT_BEE_STING = id("insect_bee_sting");
    public static final ResourceLocation INSECT_BUTTERFLY_DANCE = id("insect_butterfly_dance");
    public static final ResourceLocation INSECT_BUTTERFLY_DASH = id("insect_butterfly_dash");
    public static final ResourceLocation INSECT_DRAGONFLY = id("insect_dragonfly");
    public static final ResourceLocation INSECT_CENTIPEDE = id("insect_centipede");
    public static final ResourceLocation INSECT_IMPACT = id("insect_impact");

    private VfxIds() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, path);
    }
}
