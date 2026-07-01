package com.xirc.nichirin.common.item;

import com.xirc.nichirin.common.config.NichirinModConfig;
import com.xirc.nichirin.common.entity.npc.BaseBreathingTrainerEntity;
import com.xirc.nichirin.common.system.DemonManager;
import com.xirc.nichirin.common.util.ItemStackData;
import com.xirc.nichirin.registry.NichirinStatRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Raw flesh torn from a slain mob. Only demons can devour it, and doing so restores blood points.
 * The dropped stack remembers which kind of mob it came from (its "tier", stored in custom item
 * data), so flesh from tougher mobs restores more blood and flesh from humans counts toward the
 * "humans eaten as demon" statistic.
 *
 * <p>Tiering is decided once, at drop time, by {@link #tierFor(LivingEntity)}.</p>
 */
public class BloodyFleshItem extends Item {

    /** Custom-data key holding the flesh tier (1-3). Absent/0 is treated as tier 1. */
    private static final String TIER_KEY = "FleshTier";

    public static final int TIER_COMMON = 1; // ordinary fleshy mobs
    public static final int TIER_TOUGH = 2;  // large / high-health mobs
    public static final int TIER_HUMAN = 3;  // trainers, villagers, players

    /** Mobs at or above this max health drop the tougher, blood-richer flesh. */
    private static final float TOUGH_HEALTH_THRESHOLD = 30.0f;
    private static final int EAT_DURATION_TICKS = 32;

    public BloodyFleshItem(Properties properties) {
        super(properties);
    }

    /**
     * Whether a mob has flesh worth dropping. Skeletons and other undead, plus constructs like
     * iron/snow golems, have no flesh and drop nothing.
     */
    public static boolean hasFlesh(LivingEntity entity) {
        if (entity.getType().is(EntityTypeTags.UNDEAD)) return false;
        return !(entity instanceof AbstractGolem);
    }

    /** Decides the flesh tier for the mob the flesh was torn from. */
    public static int tierFor(LivingEntity entity) {
        if (entity instanceof BaseBreathingTrainerEntity || entity instanceof AbstractVillager) {
            return TIER_HUMAN;
        }
        if (entity instanceof Player player) {
            // A demonized player is no longer human flesh; only un-demonized players count as human.
            return DemonManager.isDemon(player) ? TIER_TOUGH : TIER_HUMAN;
        }
        if (entity.getMaxHealth() >= TOUGH_HEALTH_THRESHOLD) {
            return TIER_TOUGH;
        }
        return TIER_COMMON;
    }

    /** Builds a Bloody Flesh drop carrying the source mob's tier. */
    public static ItemStack createDrop(LivingEntity entity) {
        ItemStack stack = new ItemStack(com.xirc.nichirin.registry.NichirinItemRegistry.BLOODY_FLESH.get());
        int tier = tierFor(entity);
        if (tier != TIER_COMMON) {
            ItemStackData.update(stack, tag -> tag.putInt(TIER_KEY, tier));
        }
        return stack;
    }

    private static int readTier(ItemStack stack) {
        int tier = ItemStackData.get(stack).getInt(TIER_KEY);
        return tier <= 0 ? TIER_COMMON : tier;
    }

    /** Blood points restored for a given tier. */
    private static int bloodForTier(int tier) {
        return tier >= TIER_TOUGH ? 3 : 2;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Only demons can stomach raw flesh for blood.
        if (!DemonManager.isDemon(player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("item.nichirin.bloody_flesh.not_demon")
                                .withStyle(ChatFormatting.DARK_RED),
                        true);
            }
            return InteractionResultHolder.fail(stack);
        }

        // Don't waste flesh when blood is already full.
        if (DemonManager.getBloodPoints(player) >= NichirinModConfig.get().demon.maxBloodPoints) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.literal("Blood is too full.")
                                .withStyle(style -> style.withColor(0x8B0000)),
                        true);
            }
            return InteractionResultHolder.fail(stack);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return stack;
        }
        if (!DemonManager.isDemon(player)) {
            return stack;
        }

        int tier = readTier(stack);
        int blood = bloodForTier(tier);
        DemonManager.addBloodPoints(player, blood);

        if (tier >= TIER_HUMAN) {
            player.awardStat(Stats.CUSTOM.get(NichirinStatRegistry.HUMANS_EATEN_AS_DEMON.get()));
            player.displayClientMessage(
                    Component.translatable("item.nichirin.bloody_flesh.devoured_human")
                            .withStyle(ChatFormatting.DARK_RED),
                    true);
        } else {
            player.displayClientMessage(
                    Component.literal("+" + blood + " Blood")
                            .withStyle(style -> style.withColor(0xAA0000)),
                    true);
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return EAT_DURATION_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public SoundEvent getEatingSound() {
        return SoundEvents.GENERIC_EAT;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.nichirin.bloody_flesh.tooltip")
                .withStyle(ChatFormatting.DARK_RED));
    }
}
