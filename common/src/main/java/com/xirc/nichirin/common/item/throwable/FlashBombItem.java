package com.xirc.nichirin.common.item.throwable;

import com.xirc.nichirin.common.entity.FlashBombEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class FlashBombItem extends Item {

    public FlashBombItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            FlashBombEntity flashBomb = new FlashBombEntity(level, player);
            flashBomb.setItem(itemStack);

            // Throw straight up with low upward velocity (about 2 blocks high)
            flashBomb.setDeltaMovement(0, 0.4, 0); // Lower throw height

            level.addFreshEntity(flashBomb);
        }

        // Play throw sound
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 1F, 0.4F);

        // Update stats and consume item
        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) {
            itemStack.shrink(1);
        }

        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }
}