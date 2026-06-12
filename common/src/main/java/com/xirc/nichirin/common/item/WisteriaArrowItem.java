package com.xirc.nichirin.common.item;

import com.xirc.nichirin.registry.NichirinEffectRegistry;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class WisteriaArrowItem extends ArrowItem {

    public WisteriaArrowItem(Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack ammo, LivingEntity shooter, ItemStack weapon) {
        Arrow arrow = new Arrow(level, shooter, ammo.copyWithCount(1), weapon);
        arrow.addEffect(new MobEffectInstance(NichirinEffectRegistry.wisteriasGrace(), 160, 0));
        return arrow;
    }
}
