package com.xirc.nichirin.registry;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.entity.animal.BoarEntity;
import com.xirc.nichirin.common.entity.projectile.FlashBombEntity;
import com.xirc.nichirin.common.entity.projectile.SmokeBombEntity;
import com.xirc.nichirin.common.entity.projectile.ThrownKatanaEntity;
import com.xirc.nichirin.common.entity.attack.ThunderBallEntity;
import com.xirc.nichirin.common.entity.npc.TempleDemonEntity;
import com.xirc.nichirin.common.entity.npc.ThunderBreathingTrainerEntity;
import com.xirc.nichirin.common.entity.npc.WaterBreathingTrainerEntity;
import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public interface NichirinEntityRegistry {
    DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BreathOfNichirin.MOD_ID, Registries.ENTITY_TYPE);

    RegistrySupplier<EntityType<ThunderBallEntity>> THUNDER_BALL =
            ENTITY_TYPES.register("thunder_ball", () -> EntityType.Builder.<ThunderBallEntity>of(
                            ThunderBallEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("thunder_ball"));

    RegistrySupplier<EntityType<SmokeBombEntity>> SMOKE_BOMB =
            ENTITY_TYPES.register("smoke_bomb", () -> EntityType.Builder.<SmokeBombEntity>of(
                            SmokeBombEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("smoke_bomb"));

    RegistrySupplier<EntityType<FlashBombEntity>> FLASH_BOMB =
            ENTITY_TYPES.register("flash_bomb", () -> EntityType.Builder.<FlashBombEntity>of(
                            FlashBombEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("flash_bomb"));

    RegistrySupplier<EntityType<BoarEntity>> BOAR =
            ENTITY_TYPES.register("boar", () -> EntityType.Builder.<BoarEntity>of(
                            BoarEntity::new, MobCategory.CREATURE)
                    .sized(1.4f, 0.9f)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .build("boar"));

    RegistrySupplier<EntityType<ThrownKatanaEntity>> THROWN_KATANA =
            ENTITY_TYPES.register("thrown_katana", () -> EntityType.Builder.<ThrownKatanaEntity>of(
                            ThrownKatanaEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("thrown_katana"));

    RegistrySupplier<EntityType<TempleDemonEntity>> TEMPLE_DEMON =
            ENTITY_TYPES.register("temple_demon", () -> EntityType.Builder.<TempleDemonEntity>of(
                            TempleDemonEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(16)
                    .updateInterval(2)
                    .build("temple_demon"));

    RegistrySupplier<EntityType<WaterBreathingTrainerEntity>> WATER_BREATHING_TRAINER =
            ENTITY_TYPES.register("water_breathing_trainer", () -> EntityType.Builder.<WaterBreathingTrainerEntity>of(
                            WaterBreathingTrainerEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .build("water_breathing_trainer"));

    RegistrySupplier<EntityType<ThunderBreathingTrainerEntity>> THUNDER_BREATHING_TRAINER =
            ENTITY_TYPES.register("thunder_breathing_trainer", () -> EntityType.Builder.<ThunderBreathingTrainerEntity>of(
                            ThunderBreathingTrainerEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .build("thunder_breathing_trainer"));

    static void registerAttributes() {
        EntityAttributeRegistry.register(BOAR, BoarEntity::createAttributes);
        EntityAttributeRegistry.register(TEMPLE_DEMON, TempleDemonEntity::createAttributes);
        EntityAttributeRegistry.register(WATER_BREATHING_TRAINER, WaterBreathingTrainerEntity::createAttributes);
        EntityAttributeRegistry.register(THUNDER_BREATHING_TRAINER, ThunderBreathingTrainerEntity::createAttributes);
    }

    static void init() {
        ENTITY_TYPES.register();
        registerAttributes();
    }
}