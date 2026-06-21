package com.xirc.nichirin.common.item;

import com.xirc.nichirin.common.config.NichirinModConfig;
import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.common.data.ProgressionHelper;
import com.xirc.nichirin.common.data.PlayerDataStorage;
import com.xirc.nichirin.common.system.DemonManager;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A vial of demonic blood, dropped from slain demons. Drinking it permanently turns the player
 * into a demon — but the first right-click is treated as a "confirm?" prompt rather than an
 * instant transformation, since this is a one-way trip the player can't undo without an admin
 * command (or, if the {@code removeDemonOnDeath} config is on, a single death).
 *
 * <p>The confirmation window is intentionally short and per-player rather than baked into NBT:
 * a player who picks the vial up, gets distracted, and right-clicks an hour later should be
 * re-prompted, not silently transformed.</p>
 */
public class DemonBloodVialItem extends Item {

    /** How long the first-click "are you sure" prompt stays active before resetting (ticks). */
    private static final long CONFIRM_WINDOW_TICKS = 200L; // 10 seconds
    private static final int DRINK_DURATION_TICKS = 32;

    /** Maps player UUID → game-time tick at which their confirmation window expires. */
    private static final ConcurrentHashMap<UUID, Long> PENDING_CONFIRM = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> CLIENT_PENDING_CONFIRM = new ConcurrentHashMap<>();

    public DemonBloodVialItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Already a demon — drinking a second vial is a no-op and shouldn't consume the item.
        if (MovesetHelper.hasDemonMoveset(player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("item.nichirin.demon_blood_vial.already_demon")
                                .withStyle(ChatFormatting.DARK_RED),
                        true);
            }
            return InteractionResultHolder.fail(stack);
        }

        long now = level.getGameTime();
        if (level.isClientSide) {
            Long deadline = CLIENT_PENDING_CONFIRM.get(player.getUUID());
            if (deadline == null || now > deadline) {
                CLIENT_PENDING_CONFIRM.put(player.getUUID(), now + CONFIRM_WINDOW_TICKS);
                return InteractionResultHolder.success(stack);
            }
            return ItemUtils.startUsingInstantly(level, player, hand);
        }

        if (!(player instanceof ServerPlayer)) {
            return InteractionResultHolder.fail(stack);
        }

        Long deadline = PENDING_CONFIRM.get(player.getUUID());

        if (deadline == null || now > deadline) {
            // First click (or stale prompt) — arm the confirmation window and warn the player.
            PENDING_CONFIRM.put(player.getUUID(), now + CONFIRM_WINDOW_TICKS);
            player.displayClientMessage(
                    Component.translatable("item.nichirin.demon_blood_vial.confirm_prompt")
                            .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                    false);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.7f, 0.6f);
            return InteractionResultHolder.consume(stack);
        }

        // Second click within the window starts the drinking animation.
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return stack;
        }

        CLIENT_PENDING_CONFIRM.remove(player.getUUID());
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return stack;
        }

        Long deadline = PENDING_CONFIRM.remove(player.getUUID());
        if (deadline == null || level.getGameTime() > deadline || MovesetHelper.hasDemonMoveset(player)) {
            return stack;
        }

        CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
        serverPlayer.awardStat(Stats.ITEM_USED.get(this));
        transformIntoDemon(serverPlayer);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return DRINK_DURATION_TICKS;
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

    /**
     * Runs the same path the {@code /nichirin become demon <player>} command uses, so the
     * vial-induced transformation is indistinguishable from an admin one and goes through all
     * the same data sync / persistence steps.
     */
    private void transformIntoDemon(ServerPlayer player) {
        if (!ProgressionHelper.isMovesetUnlocked(player, "default_demon")) {
            ProgressionHelper.unlockMoveset(player, "default_demon");
        }
        PlayerDataProvider.getData(player).getMovesetData().setDemonMovesetId("default_demon");
        DemonManager.setBloodPoints(player, NichirinModConfig.get().demon.maxBloodPoints);
        PlayerDataStorage.savePlayerData(player);
        PlayerDataProvider.forceSync(player.server);

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.6f, 1.4f);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.0f, 0.5f);
        player.displayClientMessage(
                Component.translatable("item.nichirin.demon_blood_vial.transformed")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                false);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.nichirin.demon_blood_vial.tooltip_line1")
                .withStyle(ChatFormatting.DARK_RED));
    }

    /** Drop the pending-confirm entry when the player logs out so it can't dangle forever. */
    public static void clearPending(UUID playerId) {
        PENDING_CONFIRM.remove(playerId);
        CLIENT_PENDING_CONFIRM.remove(playerId);
    }

    public static void clearAll() {
        PENDING_CONFIRM.clear();
        CLIENT_PENDING_CONFIRM.clear();
    }
}