package com.xirc.nichirin.client.vfx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xirc.nichirin.client.vfx.effect.WaterSurfaceSlashEffect;
import com.xirc.nichirin.client.vfx.effect.WaterTechniqueEffect;
import com.xirc.nichirin.client.vfx.effect.FlameTechniqueEffect;
import com.xirc.nichirin.client.vfx.effect.ThunderTechniqueEffect;
import com.xirc.nichirin.client.vfx.effect.MistTechniqueEffect;
import com.xirc.nichirin.client.vfx.effect.BeastTechniqueEffect;
import com.xirc.nichirin.client.vfx.effect.SoundTechniqueEffect;
import com.xirc.nichirin.client.vfx.effect.InsectTechniqueEffect;
import com.xirc.nichirin.client.vfx.effect.KatanaTechniqueEffect;
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
        register(VfxIds.KATANA_CHECK, () -> new KatanaTechniqueEffect(KatanaTechniqueEffect.Style.CHECK));
        register(VfxIds.KATANA_PIERCING_FINISH,
                () -> new KatanaTechniqueEffect(KatanaTechniqueEffect.Style.PIERCING_FINISH));
        register(VfxIds.WATER_SURFACE_SLASH, WaterSurfaceSlashEffect::new);
        register(VfxIds.WATER_SURFACE_SLASH_REVERSE, () -> new WaterSurfaceSlashEffect(true));
        register(VfxIds.WATER_WHEEL, () -> new WaterTechniqueEffect(WaterTechniqueEffect.Style.WHEEL));
        register(VfxIds.DROP_RIPPLE_THRUST, () -> new WaterTechniqueEffect(WaterTechniqueEffect.Style.RIPPLE_THRUST));
        register(VfxIds.FLOWING_DANCE, () -> new WaterTechniqueEffect(WaterTechniqueEffect.Style.FLOWING_DANCE));
        register(VfxIds.STRIKING_TIDE, () -> new WaterTechniqueEffect(WaterTechniqueEffect.Style.STRIKING_TIDE));
        register(VfxIds.WATERFALL_BASIN, () -> new WaterTechniqueEffect(WaterTechniqueEffect.Style.WATERFALL));
        register(VfxIds.SPLASHING_WATER_FLOW, () -> new WaterTechniqueEffect(WaterTechniqueEffect.Style.SPLASHING_FLOW));
        register(VfxIds.WHIRLPOOL, () -> new WaterTechniqueEffect(WaterTechniqueEffect.Style.WHIRLPOOL));
        register(VfxIds.BLESSED_RAIN, () -> new WaterTechniqueEffect(WaterTechniqueEffect.Style.BLESSED_RAIN));
        register(VfxIds.BLESSED_RAIN_LEAP, () -> new WaterTechniqueEffect(WaterTechniqueEffect.Style.BLESSED_RAIN_LEAP));
        register(VfxIds.CONSTANT_FLUX, () -> new WaterTechniqueEffect(WaterTechniqueEffect.Style.CONSTANT_FLUX));
        register(VfxIds.DEAD_CALM, () -> new WaterTechniqueEffect(WaterTechniqueEffect.Style.DEAD_CALM));
        register(VfxIds.UNKNOWING_FIRE, () -> new FlameTechniqueEffect(FlameTechniqueEffect.Style.UNKNOWING_FIRE));
        register(VfxIds.RISING_SCORCHING_SUN, () -> new FlameTechniqueEffect(FlameTechniqueEffect.Style.RISING_SUN));
        register(VfxIds.BLAZING_UNIVERSE, () -> new FlameTechniqueEffect(FlameTechniqueEffect.Style.BLAZING_UNIVERSE));
        register(VfxIds.BLAZING_UNIVERSE_IMPACT,
                () -> new FlameTechniqueEffect(FlameTechniqueEffect.Style.BLAZING_UNIVERSE_IMPACT));
        register(VfxIds.BLOOMING_FLAME_UNDULATION, () -> new FlameTechniqueEffect(FlameTechniqueEffect.Style.BLOOMING));
        register(VfxIds.FLAME_TIGER, () -> new FlameTechniqueEffect(FlameTechniqueEffect.Style.FLAME_TIGER));
        register(VfxIds.RENGOKU, () -> new FlameTechniqueEffect(FlameTechniqueEffect.Style.RENGOKU));
        register(VfxIds.FLAME_POMMEL_SLASH, () -> new FlameTechniqueEffect(FlameTechniqueEffect.Style.POMMEL_SLASH));
        register(VfxIds.THUNDERCLAP_FLASH, () -> new ThunderTechniqueEffect(ThunderTechniqueEffect.Style.DASH));
        register(VfxIds.GODSPEED, () -> new ThunderTechniqueEffect(ThunderTechniqueEffect.Style.GODSPEED));
        register(VfxIds.RICE_SPIRIT_SLASH, () -> new ThunderTechniqueEffect(ThunderTechniqueEffect.Style.RICE_SLASH));
        register(VfxIds.THUNDER_SWARM_SLASH, () -> new ThunderTechniqueEffect(ThunderTechniqueEffect.Style.SWARM_SLASH));
        register(VfxIds.DISTANT_THUNDER_CHARGE, () -> new ThunderTechniqueEffect(ThunderTechniqueEffect.Style.CHARGE));
        register(VfxIds.HEAT_LIGHTNING_RISE, () -> new ThunderTechniqueEffect(ThunderTechniqueEffect.Style.HEAT_RISE));
        register(VfxIds.THUNDER_STRIKE_WARNING, () -> new ThunderTechniqueEffect(ThunderTechniqueEffect.Style.WARNING));
        register(VfxIds.THUNDER_STRIKE, () -> new ThunderTechniqueEffect(ThunderTechniqueEffect.Style.STRIKE));
        register(VfxIds.HONOIKAZUCHI_NO_KAMI, () -> new ThunderTechniqueEffect(ThunderTechniqueEffect.Style.DRAGON));
        register(VfxIds.HONOIKAZUCHI_IMPACT, () -> new ThunderTechniqueEffect(ThunderTechniqueEffect.Style.IMPACT));
        register(VfxIds.LOW_CLOUDS_DISTANT_HAZE, () -> new MistTechniqueEffect(MistTechniqueEffect.Style.THRUST));
        register(VfxIds.EIGHT_LAYERED_MIST, () -> new MistTechniqueEffect(MistTechniqueEffect.Style.EIGHT_LAYER));
        register(VfxIds.SCATTERING_MIST_SPLASH, () -> new MistTechniqueEffect(MistTechniqueEffect.Style.CIRCULAR));
        register(VfxIds.SHIFTING_FLOW_SLASH, () -> new MistTechniqueEffect(MistTechniqueEffect.Style.SHIFTING_FLOW));
        register(VfxIds.SEA_OF_CLOUDS_AND_HAZE, () -> new MistTechniqueEffect(MistTechniqueEffect.Style.SEA_OF_HAZE));
        register(VfxIds.LUNAR_DISPERSING_MIST, () -> new MistTechniqueEffect(MistTechniqueEffect.Style.LUNAR));
        register(VfxIds.MIST_FINISHER, () -> new MistTechniqueEffect(MistTechniqueEffect.Style.FINISHER));
        register(VfxIds.OBSCURING_CLOUDS, () -> new MistTechniqueEffect(MistTechniqueEffect.Style.OBSCURING));
        register(VfxIds.BEAST_PIERCE, () -> new BeastTechniqueEffect(BeastTechniqueEffect.Style.PIERCE));
        register(VfxIds.BEAST_X_SLICE, () -> new BeastTechniqueEffect(BeastTechniqueEffect.Style.X_SLICE));
        register(VfxIds.BEAST_EXPLOSIVE_RUSH, () -> new BeastTechniqueEffect(BeastTechniqueEffect.Style.RUSH));
        register(VfxIds.BEAST_DEVOUR, () -> new BeastTechniqueEffect(BeastTechniqueEffect.Style.DEVOUR));
        register(VfxIds.BEAST_RAPID_SLASH, () -> new BeastTechniqueEffect(BeastTechniqueEffect.Style.RAPID));
        register(VfxIds.BEAST_CRAZY_CUTTING, () -> new BeastTechniqueEffect(BeastTechniqueEffect.Style.CRAZY));
        register(VfxIds.BEAST_PALISADE_BITE, () -> new BeastTechniqueEffect(BeastTechniqueEffect.Style.PALISADE));
        register(VfxIds.BEAST_SPATIAL_AWARENESS, () -> new BeastTechniqueEffect(BeastTechniqueEffect.Style.AWARENESS));
        register(VfxIds.BEAST_BENDY_SLASH, () -> new BeastTechniqueEffect(BeastTechniqueEffect.Style.BENDY));
        register(VfxIds.BEAST_WHIRLING_FANGS, () -> new BeastTechniqueEffect(BeastTechniqueEffect.Style.WHIRL));
        register(VfxIds.BEAST_THROWING_STRIKE, () -> new BeastTechniqueEffect(BeastTechniqueEffect.Style.THROW));
        register(VfxIds.SOUND_RESONDING_SLASHES, () -> new SoundTechniqueEffect(SoundTechniqueEffect.Style.RESONDING));
        register(VfxIds.SOUND_RHYTHMIC_STEP, () -> new SoundTechniqueEffect(SoundTechniqueEffect.Style.RHYTHMIC));
        register(VfxIds.SOUND_ROAR, () -> new SoundTechniqueEffect(SoundTechniqueEffect.Style.ROAR));
        register(VfxIds.SOUND_STRING_PERFORMANCE, () -> new SoundTechniqueEffect(SoundTechniqueEffect.Style.STRINGS));
        register(VfxIds.SOUND_TEMPO_BREAKER, () -> new SoundTechniqueEffect(SoundTechniqueEffect.Style.TEMPO));
        register(VfxIds.SOUND_IMPACT, () -> new SoundTechniqueEffect(SoundTechniqueEffect.Style.IMPACT));
        register(VfxIds.INSECT_QUICK_STING, () -> new InsectTechniqueEffect(InsectTechniqueEffect.Style.QUICK_STING));
        register(VfxIds.INSECT_BEE_STING, () -> new InsectTechniqueEffect(InsectTechniqueEffect.Style.BEE_STING));
        register(VfxIds.INSECT_BUTTERFLY_DANCE, () -> new InsectTechniqueEffect(InsectTechniqueEffect.Style.BUTTERFLY));
        register(VfxIds.INSECT_BUTTERFLY_DASH, () -> new InsectTechniqueEffect(InsectTechniqueEffect.Style.BUTTERFLY_DASH));
        register(VfxIds.INSECT_DRAGONFLY, () -> new InsectTechniqueEffect(InsectTechniqueEffect.Style.DRAGONFLY));
        register(VfxIds.INSECT_CENTIPEDE, () -> new InsectTechniqueEffect(InsectTechniqueEffect.Style.CENTIPEDE));
        register(VfxIds.INSECT_IMPACT, () -> new InsectTechniqueEffect(InsectTechniqueEffect.Style.IMPACT));
    }

    public static void register(ResourceLocation id, Supplier<? extends VfxEffect> factory) {
        EFFECTS.put(id, factory);
    }

    public static void spawn(ResourceLocation id, Vec3 origin, Vec3 direction, float scale, long seed,
                             int attachmentEntityId, int ownerEntityId) {
        Supplier<? extends VfxEffect> factory = EFFECTS.get(id);
        Minecraft minecraft = Minecraft.getInstance();
        if (factory == null || minecraft.level == null) return;
        if (ACTIVE.size() >= MAX_ACTIVE_EFFECTS) ACTIVE.remove(0);
        ACTIVE.add(new VfxInstance(id, factory.get(), origin, direction, scale, seed,
                attachmentEntityId, ownerEntityId));
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
        Minecraft minecraft = Minecraft.getInstance();
        Vec3 cameraPosition = camera.getPosition();
        for (VfxInstance instance : List.copyOf(ACTIVE)) {
            if (instance.origin().distanceToSqr(cameraPosition) <= MAX_RENDER_DISTANCE_SQR) {
                boolean ownFirstPerson = minecraft.player != null
                        && minecraft.options.getCameraType().isFirstPerson()
                        && instance.ownerEntityId() == minecraft.player.getId();
                VfxPixelRender.setRenderContext(1.0f, ownFirstPerson);
                try {
                    instance.effect().render(instance, poseStack, camera, partialTick);
                } finally {
                    VfxPixelRender.clearRenderContext();
                }
            }
        }
    }

    public static boolean hasActiveEffects() {
        return !ACTIVE.isEmpty();
    }
}
