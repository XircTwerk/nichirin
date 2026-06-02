package com.xirc.nichirin.common.item.scroll;

import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.common.event.BreathOfNichirinEventHandler;
import com.xirc.nichirin.common.system.perks.FlawDefinition;
import com.xirc.nichirin.common.system.perks.NichirinPerkRegistry;
import com.xirc.nichirin.common.system.perks.PerkArchive;
import com.xirc.nichirin.common.system.perks.PerkData;
import com.xirc.nichirin.common.system.perks.PerkDefinition;
import com.xirc.nichirin.common.system.perks.PerkManager;
import com.xirc.nichirin.common.system.perks.PerkTier;
import com.xirc.nichirin.common.util.ItemStackData;
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
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Cursed scroll that grants a perk at {@link PerkTier#LEGENDARY} and binds a flaw.
 */
public class CursedScrollItem extends Item {

    private static final String TAG_PERK_ID = "PerkId";

    public CursedScrollItem(Properties properties) {
        super(properties.stacksTo(4));
    }

    public static ItemStack forPerk(Item scrollItem, String perkId) {
        ItemStack stack = new ItemStack(scrollItem);
        ItemStackData.update(stack, tag -> tag.putString(TAG_PERK_ID, perkId));
        return stack;
    }

    @Nullable
    public static String getPerkId(ItemStack stack) {
        if (!ItemStackData.has(stack, TAG_PERK_ID)) return null;
        String id = ItemStackData.get(stack).getString(TAG_PERK_ID);
        return id.isEmpty() ? null : id;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.pass(stack);
        if (PerkArchive.ARCHIVED) return InteractionResultHolder.pass(stack);

        PerkData data = PlayerDataProvider.getData(serverPlayer).getPerkData();
        String perkId = getPerkId(stack);
        if (perkId == null) {
            List<PerkDefinition> candidates = NichirinPerkRegistry.allPerks().stream()
                    .filter(def -> !def.cursed)
                    .collect(Collectors.toList());
            if (candidates.isEmpty()) {
                player.sendSystemMessage(Component.literal("This scroll is blank.").withStyle(ChatFormatting.GRAY));
                return InteractionResultHolder.fail(stack);
            }
            PerkDefinition chosen = candidates.get(level.getRandom().nextInt(candidates.size()));
            return bindCursedPerk(level, serverPlayer, stack, chosen, data);
        }

        PerkDefinition def = NichirinPerkRegistry.getPerk(perkId);
        if (def == null) {
            player.sendSystemMessage(Component.literal("This scroll is corrupted.").withStyle(ChatFormatting.DARK_RED));
            return InteractionResultHolder.fail(stack);
        }

        return bindCursedPerk(level, serverPlayer, stack, def, data);
    }

    private InteractionResultHolder<ItemStack> bindCursedPerk(Level level, ServerPlayer player, ItemStack stack,
                                                              PerkDefinition def, PerkData data) {
        String flawId = def.linkedFlawId != null ? def.linkedFlawId : chooseRandomFlaw(data, level);
        boolean equippedNewFlaw = false;

        if (flawId != null) {
            FlawDefinition flaw = NichirinPerkRegistry.getFlaw(flawId);
            if (flaw != null && !data.hasFlaw(flawId)) {
                PerkManager.Result flawResult = PerkManager.tryEquipFlaw(player, flawId);
                if (!flawResult.success) {
                    player.sendSystemMessage(Component.literal("Could not bind the curse's flaw: " + flawResult.message)
                            .withStyle(ChatFormatting.DARK_RED));
                    return InteractionResultHolder.fail(stack);
                }
                equippedNewFlaw = true;
            }
        }

        PerkManager.discover(player, def.id);
        PerkManager.Result equipResult = PerkManager.tryEquip(player, def.id, PerkTier.LEGENDARY);
        if (!equipResult.success) {
            player.sendSystemMessage(Component.literal("Could not equip cursed perk: " + equipResult.message)
                    .withStyle(ChatFormatting.RED));
            if (equippedNewFlaw && flawId != null) {
                data.unequipFlaw(flawId);
                PerkManager.cleanupFlawEffects(player, flawId);
            }
            BreathOfNichirinEventHandler.syncPerksToPlayer(player);
            return InteractionResultHolder.fail(stack);
        }

        BreathOfNichirinEventHandler.syncPerksToPlayer(player);
        level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.4f, 1.5f);

        player.sendSystemMessage(Component.literal("The curse takes hold! You have been bound to: ")
                .withStyle(ChatFormatting.DARK_RED)
                .append(Component.literal(def.name).withStyle(ChatFormatting.RED)));

        if (flawId != null) {
            FlawDefinition flaw = NichirinPerkRegistry.getFlaw(flawId);
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

    @Nullable
    private static String chooseRandomFlaw(PerkData data, Level level) {
        List<FlawDefinition> available = new ArrayList<>();
        for (FlawDefinition flaw : NichirinPerkRegistry.allFlaws()) {
            if (!data.hasFlaw(flaw.id)) {
                available.add(flaw);
            }
        }
        if (available.isEmpty()) {
            return null;
        }
        return available.get(level.getRandom().nextInt(available.size())).id;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (PerkArchive.ARCHIVED) return;
        String perkId = getPerkId(stack);
        if (perkId == null) {
            tooltip.add(Component.literal("Blank cursed scroll").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("Binds a random legendary perk and a flaw.").withStyle(ChatFormatting.DARK_RED));
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
        if (PerkArchive.ARCHIVED) return super.getName(stack);
        if (perkId != null) {
            PerkDefinition def = NichirinPerkRegistry.getPerk(perkId);
            if (def != null) {
                return Component.literal("Cursed Scroll: " + def.name);
            }
        }
        return super.getName(stack);
    }
}