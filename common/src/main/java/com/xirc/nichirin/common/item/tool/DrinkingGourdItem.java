package com.xirc.nichirin.common.item.tool;

import com.xirc.nichirin.common.util.StaminaManager;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DrinkingGourdItem extends Item {
    private static final int MAX_DRINKS = 8;
    private static final String DRINKS_TAG = "Drinks";
    private static final String LIQUID_TYPE_TAG = "LiquidType";
    private static final int DRINK_DURATION = 32;
    private static final float WATER_STAMINA_RESTORE = 20.0f;

    public DrinkingGourdItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int drinks = getDrinks(stack);

        // Try to fill from water if not full
        if (drinks < MAX_DRINKS) {
            BlockHitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);

            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = hitResult.getBlockPos();
                BlockState state = level.getBlockState(pos);

                // Check if it's a water source block without modifying it
                if (state.is(Blocks.WATER) && state.getFluidState().isSource() && state.getFluidState().getType() == Fluids.WATER) {
                    if (!level.isClientSide) {
                        addDrink(stack, LiquidType.WATER);
                        level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
                }
            }
        }

        // If has drinks, start drinking
        if (drinks > 0) {
            return ItemUtils.startUsingInstantly(level, player, hand);
        }

        return InteractionResultHolder.fail(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
            serverPlayer.awardStat(Stats.ITEM_USED.get(this));
        }

        if (!level.isClientSide) {
            LiquidType liquidType = getLiquidType(stack);

            if (liquidType == LiquidType.MILK) {
                entity.removeAllEffects();
            } else if (liquidType == LiquidType.WATER && entity instanceof Player player) {
                // Restore stamina when drinking water
                StaminaManager.restore(player, WATER_STAMINA_RESTORE);
            }

            decrementDrinks(stack);
        }

        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return DRINK_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public SoundEvent getDrinkingSound() {
        return SoundEvents.GENERIC_DRINK;
    }

    @Override
    public SoundEvent getEatingSound() {
        return SoundEvents.GENERIC_DRINK;
    }

    // Liquid management methods
    public static int getDrinks(ItemStack stack) {
        if (!stack.hasTag()) {
            return 0;
        }
        return stack.getTag().getInt(DRINKS_TAG);
    }

    public static LiquidType getLiquidType(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(LIQUID_TYPE_TAG)) {
            return LiquidType.EMPTY;
        }
        String type = stack.getTag().getString(LIQUID_TYPE_TAG);
        try {
            return LiquidType.valueOf(type);
        } catch (IllegalArgumentException e) {
            return LiquidType.EMPTY;
        }
    }

    public static void setLiquid(ItemStack stack, LiquidType type, int amount) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(LIQUID_TYPE_TAG, type.name());
        tag.putInt(DRINKS_TAG, Math.min(amount, MAX_DRINKS));
    }

    public static void addDrink(ItemStack stack, LiquidType type) {
        int currentDrinks = getDrinks(stack);
        LiquidType currentType = getLiquidType(stack);

        // If different type and not empty, clear and start fresh
        if (currentType != LiquidType.EMPTY && currentType != type) {
            setLiquid(stack, type, 1);
            return;
        }

        // If empty or same type, add one drink (up to max)
        if (currentDrinks < MAX_DRINKS) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putString(LIQUID_TYPE_TAG, type.name());
            tag.putInt(DRINKS_TAG, currentDrinks + 1);
        }
    }

    private static void decrementDrinks(ItemStack stack) {
        int drinks = getDrinks(stack);
        if (drinks > 0) {
            drinks--;
            if (drinks == 0) {
                // Clear liquid type when empty
                CompoundTag tag = stack.getOrCreateTag();
                tag.remove(LIQUID_TYPE_TAG);
                tag.putInt(DRINKS_TAG, 0);
            } else {
                stack.getOrCreateTag().putInt(DRINKS_TAG, drinks);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        int drinks = getDrinks(stack);
        LiquidType liquidType = getLiquidType(stack);

        String liquidName = liquidType == LiquidType.EMPTY ? "Empty" :
                liquidType == LiquidType.WATER ? "Water" : "Milk";

        tooltip.add(Component.literal("§b" + liquidName + ": §f" + drinks + "/" + MAX_DRINKS));

        if (drinks > 0) {
            tooltip.add(Component.literal("§7Right-click to drink"));
            if (liquidType == LiquidType.WATER) {
                tooltip.add(Component.literal("§7Restores " + (int)WATER_STAMINA_RESTORE + " stamina"));
            }
        } else {
            tooltip.add(Component.literal("§7Fill from water or milk"));
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getDrinks(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int drinks = getDrinks(stack);
        return Math.round(13.0F * drinks / MAX_DRINKS);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        LiquidType type = getLiquidType(stack);
        return type == LiquidType.MILK ? 0xFFFFFF : 0x3F76E4;
    }

    public enum LiquidType {
        EMPTY,
        WATER,
        MILK
    }
}