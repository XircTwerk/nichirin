package com.xirc.nichirin.common.item;

import com.xirc.nichirin.common.entity.npc.TempleDemonEntity;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.SpawnEggItem;

/**
 * A spawn egg item that lazily resolves its entity type via a supplier,
 * avoiding registration-order issues on Forge where item RegisterEvent
 * fires before entity type RegisterEvent.
 */
public class LazySpawnEggItem extends SpawnEggItem {
    private final RegistrySupplier<EntityType<TempleDemonEntity>> entityTypeSupplier;

    public LazySpawnEggItem(RegistrySupplier<EntityType<TempleDemonEntity>> entityTypeSupplier, int primaryColor, int secondaryColor, Properties properties) {
        super(null, primaryColor, secondaryColor, properties);
        this.entityTypeSupplier = entityTypeSupplier;
    }

    @Override
    public FeatureFlagSet requiredFeatures() {
        return FeatureFlags.VANILLA_SET;
    }

    @Override
    public EntityType<?> getType(CompoundTag nbt) {
        if (nbt != null && nbt.contains("EntityTag", 10)) {
            CompoundTag entityTag = nbt.getCompound("EntityTag");
            if (entityTag.contains("id", 8)) {
                return EntityType.byString(entityTag.getString("id")).orElse(entityTypeSupplier.get());
            }
        }
        return entityTypeSupplier.get();
    }
}
