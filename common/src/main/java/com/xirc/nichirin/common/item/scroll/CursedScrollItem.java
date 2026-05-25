package com.xirc.nichirin.common.item.scroll;

import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.common.system.perks.*;
import net.minecraft.ChatFormatting;
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

/**
 * Cursed scroll that instantly grants a {@link PerkTier#LEGENDARY} perk along with
 * its linked flaw.
 *
 * <p>The perk ID is stored in NBT under {@code "PerkId"}, same as {@link PerkScrollItem}.
 * The flaw is determined from {@link PerkDefinition#linkedFlawId}.</p>
 *
 * <p>Reading the scroll:</p>
 * <ol>
 *   <li>Discovers and equips the cursed perk at LEGENDARY tier (if a slot is available).</li>
 *   <li>Discovers and equips the linked flaw (if the flaw system is enabled and a flaw slot is open).</li>
 *   <li>If no perk slot is available the scroll is NOT consumed — the player must free a slot first.</li>
 * </ol>
 */
public class CursedScrollItem extends Item {

    private static final String TAG_PERK_ID = "PerkId";

    public CursedScrollItem(Properties properties) {
        super(properties.stacksTo(4));
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

        String perkId = getPerkId(stack);
        if (perkId == null) {
            player.sendSystemMessage(Component.literal("This scroll is blank.").withStyle(ChatFormatting.GRAY));
            return InteractionResultHolder.fail(stack);
        }

        PerkDefinition def = NichirinPerkRegistry.getPerk(perkId);
        if (def == null || !def.cursed) {
            player.sendSystemMessage(Component.literal("This scroll is corrupted.").withStyle(ChatFormatting.DARK_RED));
            return InteractionResultHolder.fail(stack);
        }

        // Must equip the linked flaw first (makes a slot available), unless already wearing it
        PerkData data = PlayerDataProvider.getData(serverPlayer).getPerkData();

        // Equip the flaw if needed
        if (def.linkedFlawId != null) {
            FlawDefinition flaw = NichirinPerkRegistry.getFlaw(def.linkedFlawId);
            if (flaw != null && !data.hasFlaw(def.linkedFlawId)) {
                PerkManager.Result flawResult = PerkManager.tryEquipFlaw(serverPlayer, def.linkedFlawId);
                if (!flawResult.success) {
                    player.sendSystemMessage(Component.literal("Could not bind the curse's flaw: " + flawResult.message)
                            .withStyle(ChatFormatting.DARK_RED));
                    // Continue anyway — the perk is cursed, not impossible
                }
            }
        }

        // Discover + equip the perk
        PerkManager.discover(serverPlayer, perkId);
        PerkManager.Result equipResult = PerkManager.tryEquip(serverPlayer, perkId, PerkTier.LEGENDARY);
        if (!equipResult.success) {
            player.sendSystemMessage(Component.literal("Could not equip cursed perk: " + equipResult.message)
                    .withStyle(ChatFormatting.RED));
            // Scroll not consumed
            return InteractionResultHolder.fail(stack);
        }

        level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.4f, 1.5f);

        player.sendSystemMessage(Component.literal("The curse takes hold! You have been bound to: ")
                .withStyle(ChatFormatting.DARK_RED)
                .append(Component.literal(def.name).withStyle(ChatFormatting.RED)));

        if (def.linkedFlawId != null) {
            FlawDefinition flaw = NichirinPerkRegistry.getFlaw(def.linkedFlawId);
            if (flaw != null) {
                player.sendSystemMessage(Component.literal("Flaw inflicted: ")
                        .withStyle(ChatFormatting.DARK_GRAY)
                        .append(Component.literal(flaw.name).withStyle(ChatFormatting.GRAY)));
            }
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.consume(stack);
    }


    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        String perkId = getPerkId(stack);
        if (perkId == null) {
            tooltip.add(Component.literal("Blank scroll").withStyle(ChatFormatting.GRAY));
            return;
        }
        PerkDefinition def = NichirinPerkRegistry.getPerk(perkId);
        if (def == null) {
            tooltip.add(Component.literal("Unknown cursed perk").withStyle(ChatFormatting.DARK_RED));
            return;
        }
        tooltip.add(Component.literal("Cursed Perk: ").withStyle(ChatFormatting.DARK_RED)
                .append(Component.literal(def.name).withStyle(ChatFormatting.RED)));
        tooltip.add(Component.literal(def.description).withStyle(ChatFormatting.DARK_GRAY));
        if (def.linkedFlawId != null) {
            FlawDefinition flaw = NichirinPerkRegistry.getFlaw(def.linkedFlawId);
            if (flaw != null) {
                tooltip.add(Component.literal("Inflicts: ").withStyle(ChatFormatting.DARK_PURPLE)
                        .append(Component.literal(flaw.name).withStyle(ChatFormatting.DARK_RED)));
            }
        }
        tooltip.add(Component.literal("WARNING: Reading this scroll binds a curse upon you.").withStyle(ChatFormatting.RED));
    }

    @Override
    public Component getName(ItemStack stack) {
        String perkId = getPerkId(stack);
        if (perkId != null) {
            PerkDefinition def = NichirinPerkRegistry.getPerk(perkId);
            if (def != null) {
                return Component.literal("Cursed Scroll: " + def.name);
            }
        }
        return super.getName(stack);
    }
}
