package com.xirc.nichirin.common.event.system;

import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.common.system.movement.Dash;
import com.xirc.nichirin.common.system.movement.Dodge;
import com.xirc.nichirin.common.system.abilities.PlayerDoubleJump;
import com.xirc.nichirin.common.system.DemonManager;
import com.xirc.nichirin.common.system.sheathing.SheathingManager;
import com.xirc.nichirin.common.system.perks.PerkArchive;
import com.xirc.nichirin.common.system.perks.PerkManager;
import com.xirc.nichirin.common.system.perks.PerkData;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class PlayerTickHandler {

    private static final ResourceLocation STEADFAST_KB_ID = id("steadfast_kb");
    private static final ResourceLocation NIGHT_PROWLER_ID = id("night_prowler");
    private static final ResourceLocation LIGHTFOOT_ID = id("lightfoot");
    private static final ResourceLocation JUGGERNAUT_ID = id("juggernaut");

    public static void register() {
        TickEvent.PLAYER_POST.register(PlayerTickHandler::onPlayerTick);

        TickEvent.SERVER_POST.register(server -> {
            Dodge.tick();
            Dash.tickAllDashes();
        });

        PlayerEvent.PLAYER_QUIT.register(player -> {
            if (player instanceof ServerPlayer) {
                ServerPlayer sp = (ServerPlayer) player;
                removeAllPerkModifiers(sp);
                SheathingManager.cleanupPlayer(sp);
            }
        });

        // Sleep Deprived flaw — refuse bed interactions
        InteractionEvent.RIGHT_CLICK_BLOCK.register((player, hand, pos, face) -> {
            if (PerkArchive.ARCHIVED) return EventResult.pass();
            if (player.level().isClientSide) return EventResult.pass();
            var state = player.level().getBlockState(pos);
            if (!(state.getBlock() instanceof BedBlock)) {
                return EventResult.pass();
            }
            PerkData perkData = PlayerDataProvider.getData(player).getPerkData();
            if (perkData.hasFlaw("sleep_deprived")) {
                player.displayClientMessage(
                        Component.literal("You cannot sleep — your mind refuses rest.")
                                .withStyle(s -> s.withColor(0xFF5555)),
                        true);
                return EventResult.interruptFalse();
            }
            return EventResult.pass();
        });
    }

    private static void onPlayerTick(Player player) {
        PlayerDoubleJump.tickPlayer(player);

        if (!player.level().isClientSide) {
            SheathingManager.tick(player);
            Dodge.tickForPlayer(player);
            DemonManager.tickDemon(player);

            if (player instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer) player;
                removeAllPerkModifiers(serverPlayer);
                if (!PerkArchive.ARCHIVED) {
                    tickPerkEffects(serverPlayer);
                    tickFlawEffects(serverPlayer);
                }
            }
        }
    }

    /**
     * Per-tick effects driven by equipped flaws.
     */
    private static void tickFlawEffects(ServerPlayer player) {
        PerkData data = PlayerDataProvider.getData(player).getPerkData();

        // Cursed Eyes — permanent Blindness, no Night Vision benefit
        if (data.hasFlaw("cursed_eyes")) {
            if (player.hasEffect(MobEffects.NIGHT_VISION)) {
                player.removeEffect(MobEffects.NIGHT_VISION);
            }
            var blind = player.getEffect(MobEffects.BLINDNESS);
            if (blind == null || blind.getDuration() < 40) {
                player.addEffect(new MobEffectInstance(
                        MobEffects.BLINDNESS, Integer.MAX_VALUE, 0, false, false, false));
            }
        }

        // Daywalker — all stats halved at night
        if (data.hasFlaw("daywalker")) {
            long timeOfDay = player.level().getDayTime() % 24000L;
            boolean isNight = timeOfDay >= 13000L && timeOfDay <= 23000L;
            if (isNight) {
                if (!hasEffect(player, MobEffects.WEAKNESS, 1)) {
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1, false, false, false));
                }
                if (!hasEffect(player, MobEffects.MOVEMENT_SLOWDOWN, 1)) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, false, false));
                }
                if (!hasEffect(player, MobEffects.DIG_SLOWDOWN, 1)) {
                    player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 1, false, false, false));
                }
            }
        }

        // Hollow — hunger drains 3x faster, no saturation
        if (data.hasFlaw("hollow")) {
            player.causeFoodExhaustion(0.04f);
            if (player.getFoodData().getSaturationLevel() > 0f) {
                player.getFoodData().setSaturation(0f);
            }
        }

        // Forsaken — food provides no healing (clamp food just below regen threshold)
        if (data.hasFlaw("forsaken")) {
            if (player.getFoodData().getFoodLevel() > 17) {
                player.getFoodData().setFoodLevel(17);
            }
            if (player.getFoodData().getSaturationLevel() > 0f) {
                player.getFoodData().setSaturation(0f);
            }
        }
    }

    private static boolean hasEffect(ServerPlayer player, Holder<MobEffect> effect, int minAmplifier) {
        var inst = player.getEffect(effect);
        return inst != null && inst.getAmplifier() >= minAmplifier && inst.getDuration() > 20;
    }

    private static void tickPerkEffects(ServerPlayer player) {
        // Moonlit fury dawn reset
        PerkManager.tickMoonlitFury(player);

        applyOrRemoveModifier(
                player,
                Attributes.KNOCKBACK_RESISTANCE,
                STEADFAST_KB_ID,
                "Steadfast KB",
                PerkManager.getSteadfastKnockbackResistance(player),
                AttributeModifier.Operation.ADD_VALUE
        );

        float jugPenalty = PerkManager.getJuggernautSpeedPenalty(player);

        applyOrRemoveModifier(player, Attributes.MOVEMENT_SPEED, NIGHT_PROWLER_ID,
                "Night Prowler Speed", PerkManager.getNightProwlerSpeedBonus(player),
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        applyOrRemoveModifier(player, Attributes.MOVEMENT_SPEED, LIGHTFOOT_ID,
                "Lightfoot Speed", PerkManager.getLightfootSpeedBonus(player),
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        applyOrRemoveModifier(player, Attributes.MOVEMENT_SPEED, JUGGERNAUT_ID,
                "Juggernaut Penalty", jugPenalty,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        if (PerkManager.hasNightProwlerNightVision(player)) {
            if (!player.hasEffect(MobEffects.NIGHT_VISION)
                    || player.getEffect(MobEffects.NIGHT_VISION).getDuration() < 40) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, false, false));
            }
        }

        double range = PerkManager.getHuntersInstinctRange(player);
        if (range > 0 && player.tickCount % 20 == 0) { // check every second
            AABB box = player.getBoundingBox().inflate(range);
            List<LivingEntity> nearby = player.level().getEntitiesOfClass(
                    LivingEntity.class, box,
                    e -> e != player && (e instanceof Player) && MovesetHelper.hasDemonMoveset((Player) e));
            for (LivingEntity demon : nearby) {
                if (!demon.hasEffect(MobEffects.GLOWING) || demon.getEffect(MobEffects.GLOWING).getDuration() < 30) {
                    demon.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false));
                }
            }
        }
    }

    /**
     * Applies (or updates) a named attribute modifier, removing any existing one first.
     * Skips no-op (value == 0) cases by removing the modifier cleanly.
     */
    private static void applyOrRemoveModifier(ServerPlayer player,
                                               Holder<Attribute> attribute,
                                               ResourceLocation id, String name,
                                               float value,
                                               AttributeModifier.Operation op) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst == null) return;

        AttributeModifier existing = inst.getModifier(id);
        if (value == 0f) {
            if (existing != null) inst.removeModifier(id);
            return;
        }
        // Only update if the value changed
        if (existing != null && (float) existing.amount() == value) return;
        if (existing != null) inst.removeModifier(id);
        inst.addTransientModifier(new AttributeModifier(id, value, op));
    }

    private static void removeAllPerkModifiers(ServerPlayer player) {
        for (ResourceLocation id : new ResourceLocation[]{STEADFAST_KB_ID, NIGHT_PROWLER_ID, LIGHTFOOT_ID, JUGGERNAUT_ID}) {
            AttributeInstance kbInst = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (kbInst != null) kbInst.removeModifier(id);
            AttributeInstance spInst = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (spInst != null) spInst.removeModifier(id);
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("nichirin", path);
    }
}