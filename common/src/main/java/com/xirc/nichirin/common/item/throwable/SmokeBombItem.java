package com.xirc.nichirin.common.item.throwable;

import com.xirc.nichirin.common.entity.SmokeBombEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SmokeBombItem extends Item {

    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();
    private static final int COOLDOWN_TICKS = 40; // 2 seconds at 20 TPS

    public SmokeBombItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            // Check cooldown
            long currentTime = level.getGameTime();
            Long lastUse = COOLDOWNS.get(player.getUUID());

            if (lastUse != null && currentTime - lastUse < COOLDOWN_TICKS) {
                int remainingTicks = (int)(COOLDOWN_TICKS - (currentTime - lastUse));
                player.displayClientMessage(
                        Component.literal("Smoke bomb on cooldown! (" + (remainingTicks / 20.0f) + "s)")
                                .withStyle(style -> style.withColor(0xFFAA00)),
                        true
                );
                return InteractionResultHolder.fail(itemStack);
            }

            // Set cooldown
            COOLDOWNS.put(player.getUUID(), currentTime);

            // Create and throw smoke bomb entity
            SmokeBombEntity smokeBomb = new SmokeBombEntity(level, player);
            smokeBomb.setItem(itemStack);
            smokeBomb.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            level.addFreshEntity(smokeBomb);
        }

        // Play throw sound
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

        // Update stats and consume item
        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) {
            itemStack.shrink(1);
        }

        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }

    /**
     * Clean up cooldowns when player disconnects
     */
    public static void cleanupPlayer(Player player) {
        COOLDOWNS.remove(player.getUUID());
    }
}