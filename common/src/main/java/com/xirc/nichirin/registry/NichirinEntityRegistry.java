package com.xirc.nichirin.registry;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.entity.SmokeBombEntity;
import com.xirc.nichirin.common.entity.ThunderBallEntity;
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

    static void init() {
        ENTITY_TYPES.register();
    }
}