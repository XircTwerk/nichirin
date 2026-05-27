package com.xirc.nichirin.common.item.scroll;

import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.common.event.BreathOfNichirinEventHandler;
import com.xirc.nichirin.common.system.perks.NichirinPerkRegistry;
import com.xirc.nichirin.common.system.perks.PerkArchive;
import com.xirc.nichirin.common.system.perks.PerkData;
import com.xirc.nichirin.common.system.perks.PerkDefinition;
import com.xirc.nichirin.common.system.perks.PerkManager;
import net.minecraft.ChatFormatting;
import net.minecraft.util.RandomSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Consumable scroll that grants the player discovery of a specific perk.
 *
 * <p>The perk ID is stored in the stack's NBT under {@code "PerkId"}.
 * Scrolls are typically found in temple loot tables or crafted.</p>
 *
 * <p>Right-clicking reads the scroll, granting discovery. Already-discovered
 * perks still consume the scroll (it's a one-time use item), but shows a
 * different message.</p>
 */
public class PerkScrollItem extends Item {

    private static final String TAG_PERK_ID = "PerkId";

    public PerkScrollItem(Properties properties) {
        super(properties.stacksTo(16));
    }


    public static ItemStack forPerk(Item scrollItem, String perkId) {
        ItemStack stack = new ItemStack(scrollItem);
        stack.getOrCreateTag().putString(TAG_PERK_ID, perkId);
        return stack;
    }

    @Nullable
    public static String getPerkId(ItemStack stack) {
        if (!stack.hasTag()) return null;
        String id = stack.getTag().getString(TAG_PERK_ID);
        return id.isEmpty() ? null : id;
    }


    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.pass(stack);
        if (PerkArchive.ARCHIVED) return InteractionResultHolder.pass(stack);

        String perkId = getPerkId(stack);
        if (perkId == null) {
            // Blank scroll — discover a random undiscovered perk
            PerkData data = PlayerDataProvider.getData(serverPlayer).getPerkData();
            List<PerkDefinition> undiscovered = NichirinPerkRegistry.allPerks().stream()
                    .filter(def -> !def.cursed && !data.hasDiscovered(def.id))
                    .collect(Collectors.toList());
            if (undiscovered.isEmpty()) {
                player.sendSystemMessage(Component.literal("You already know all available perks.").withStyle(ChatFormatting.GRAY));
                return InteractionResultHolder.fail(stack);
            }
            RandomSource rand = level.getRandom();
            PerkDefinition chosen = undiscovered.get(rand.nextInt(undiscovered.size()));
            PerkManager.discover(serverPlayer, chosen.id);
            BreathOfNichirinEventHandler.syncPerksToPlayer(serverPlayer);
            level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8f, 1.2f);
            player.sendSystemMessage(Component.literal("The scroll reveals: ")
                    .withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(chosen.name).withStyle(ChatFormatting.YELLOW)));
            if (!player.getAbilities().instabuild) stack.shrink(1);
            return InteractionResultHolder.consume(stack);
        }

        PerkDefinition def = NichirinPerkRegistry.getPerk(perkId);
        if (def == null) {
            player.sendSystemMessage(Component.literal("This scroll refers to an unknown perk.").withStyle(ChatFormatting.RED));
            return InteractionResultHolder.fail(stack);
        }

        boolean newlyDiscovered = PerkManager.discover(serverPlayer, perkId);
        BreathOfNichirinEventHandler.syncPerksToPlayer(serverPlayer);

        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8f, 1.2f);

        if (newlyDiscovered) {
            player.sendSystemMessage(Component.literal("You have learned: ")
                    .withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(def.name).withStyle(ChatFormatting.YELLOW)));
        } else {
            player.sendSystemMessage(Component.literal("You already know: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(def.name).withStyle(ChatFormatting.WHITE)));
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.consume(stack);
    }


    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (PerkArchive.ARCHIVED) return;
        String perkId = getPerkId(stack);
        if (perkId == null) {
            tooltip.add(Component.literal("Reveals a random undiscovered perk.").withStyle(ChatFormatting.GOLD));
            return;
        }
        PerkDefinition def = NichirinPerkRegistry.getPerk(perkId);
        if (def == null) {
            tooltip.add(Component.literal("Unknown perk").withStyle(ChatFormatting.RED));
            return;
        }
        tooltip.add(Component.literal("Perk: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(def.name).withStyle(ChatFormatting.YELLOW)));
        tooltip.add(Component.literal(def.description).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Right-click to learn this perk.").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public Component getName(ItemStack stack) {
        String perkId = getPerkId(stack);
        if (PerkArchive.ARCHIVED) return super.getName(stack);
        if (perkId != null) {
            PerkDefinition def = NichirinPerkRegistry.getPerk(perkId);
            if (def != null) {
                return Component.literal("Perk Scroll: " + def.name);
            }
        }
        return super.getName(stack);
    }
}
