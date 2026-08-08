package com.xirc.nichirin.common.attack.moveset;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.ServerCooldownManager;
import com.xirc.nichirin.common.attack.component.AbstractDemonAttack;
import com.xirc.nichirin.common.attack.moves.cqc.*;
import com.xirc.nichirin.common.attack.moves.cqc.destructive.*;
import com.xirc.nichirin.common.attack.moves.demon.basic.*;
import com.xirc.nichirin.common.data.CqcMoveCatalog;
import com.xirc.nichirin.common.data.CqcPresetData;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.common.system.DemonManager;
import com.xirc.nichirin.common.util.StaminaManager;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Customizable close-quarters-combat moveset. The registered moveset is a template;
 * player-owned CQC preset data decides which move occupies each slot.
 */
public class CqcMoveset extends AbstractMoveset {

    public static final String ID = "cqc";
    private static final Map<UUID, SlashComboState> SLASH_STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, CqcFollowupState> FOLLOWUP_STATES = new ConcurrentHashMap<>();

    public static final MoveConfiguration JAB = new MoveBuilder("jab", "Jab")
            .withDescription("Short gut jab. Fast body-shot that stuns at close range.")
            .withTiming(10, 4, 3)
            .withDamage(2.0f)
            .withRange(1.15f)
            .withKnockback(0.08f)
            .withHitStun(12)
            .withHitboxSize(1.9f)
            .withStaminaCost(4.0f)
            .build();

    public static final MoveConfiguration CROSS = new MoveBuilder("cross", "Cross")
            .withDescription("Committed straight punch. Pulls you into boxing range.")
            .withTiming(16, 6, 8)
            .withDamage(2.4f)
            .withRange(1.6f)
            .withKnockback(0.25f)
            .withHitStun(12)
            .withHitboxSize(0.95f)
            .withStaminaCost(5.0f)
            .withDashSpeed(0.9f)
            .build();

    public static final MoveConfiguration LEFT_HOOK = new MoveBuilder("lefthook", "Left Hook")
            .withDescription("Short hook with a wider hitbox and stronger stagger.")
            .withTiming(20, 7, 7)
            .withDamage(2.8f)
            .withRange(1.55f)
            .withKnockback(0.45f)
            .withHitStun(15)
            .withHitboxSize(1.2f)
            .withStaminaCost(6.0f)
            .build();

    public static final MoveConfiguration EYE_POKE = new MoveBuilder("eye_poke", "Eye Poke")
            .withDescription("Low damage poke that briefly blinds and interrupts.")
            .withTiming(28, 2, 5)
            .withDamage(1.2f)
            .withRange(1.25f)
            .withKnockback(0.05f)
            .withHitStun(21)
            .withHitboxSize(0.75f)
            .withStaminaCost(5.0f)
            .build();

    public static final MoveConfiguration THROAT_CHOP = new MoveBuilder("throat_chop", "Throat Chop")
            .withDescription("Close interrupt that slows and locks down a target.")
            .withTiming(30, 3, 5)
            .withDamage(2.0f)
            .withRange(1.35f)
            .withKnockback(0.15f)
            .withHitStun(25)
            .withHitboxSize(0.85f)
            .withStaminaCost(7.0f)
            .build();

    public static final MoveConfiguration UPPERCUT = new MoveBuilder("uppercut", "Uppercut")
            .withDescription("Dashing launcher. Dashes 5 blocks and knocks targets upward.")
            .withTiming(38, 4, 8)
            .withDamage(3.4f)
            .withRange(1.65f)
            .withKnockback(0.25f)
            .withHitStun(19)
            .withHitboxSize(1.0f)
            .withStaminaCost(10.0f)
            .withDashSpeed(10.0f)
            .build();

    public static final MoveConfiguration SPINNING_HEEL_KICK = new MoveBuilder("spinning_heel_kick", "Spinning Heel Kick")
            .withDescription("Slow, wide, heavy kick that launches sideways.")
            .withTiming(56, 6, 11)
            .withDamage(5.2f)
            .withRange(2.25f)
            .withKnockback(1.0f)
            .withHitStun(17)
            .withHitboxSize(1.55f)
            .withStaminaCost(13.0f)
            .build();

    public static final MoveConfiguration KNEE = new MoveBuilder("knee", "Knee")
            .withDescription("Short, reliable knee with strong stun.")
            .withTiming(28, 3, 7)
            .withDamage(2.8f)
            .withRange(1.25f)
            .withKnockback(0.25f)
            .withHitStun(18)
            .withHitboxSize(0.85f)
            .withStaminaCost(7.0f)
            .build();

    public static final MoveConfiguration LOW_KICK = new MoveBuilder("low_kick", "Low Kick")
            .withDescription("Fast leg kick that slows grounded targets.")
            .withTiming(22, 2, 6)
            .withDamage(2.2f)
            .withRange(1.65f)
            .withKnockback(0.35f)
            .withHitStun(13)
            .withHitboxSize(1.0f)
            .withStaminaCost(5.0f)
            .build();

    public static final MoveConfiguration DOUBLE_PALM = new MoveBuilder("double_palm", "Double Palm")
            .withDescription("Two-handed shove that creates space.")
            .withTiming(32, 3, 7)
            .withDamage(3.0f)
            .withRange(1.45f)
            .withKnockback(1.15f)
            .withHitStun(14)
            .withHitboxSize(1.15f)
            .withStaminaCost(8.0f)
            .build();

    public static final MoveConfiguration SNAP_PUNCH = new MoveBuilder("snap_punch", "Snap Punch")
            .withDescription("Quick forward strike. Spawns a short-range shockwave when Destructive Death's toggle is on.")
            .withTiming(14, 1, 6)
            .withDamage(4.5f)
            .withRange(2.0f)
            .withKnockback(0.25f)
            .withHitStun(10)
            .withHitboxSize(1.3f)
            .withStaminaCost(5.0f)
            .build();

    public static final MoveConfiguration ANNIHILATION_TYPE = new MoveBuilder("annihilation_type", "Annihilation Type")
            .withDescription("Crouched-stance forward palm dash. Twin circular shockwaves on swing start.")
            .withTiming(34, 9, 12)
            .withDamage(7.0f)
            .withRange(2.4f)
            .withKnockback(1.2f)
            .withHitStun(18)
            .withHitboxSize(2.3f)
            .withStaminaCost(12.0f)
            .withDashSpeed(6.0f)
            .build();

    public static final MoveConfiguration CROWN_SPLITTER = new MoveBuilder("crown_splitter", "Crown Splitter")
            .withDescription("Rising reverse axe-kick. High burst damage with a vertical shockwave on connect.")
            .withTiming(22, 2, 8)
            .withDamage(6.0f)
            .withRange(1.9f)
            .withKnockback(0.55f)
            .withHitStun(16)
            .withHitboxSize(1.2f)
            .withStaminaCost(8.0f)
            .build();

    public static final MoveConfiguration FLYING_PLANET = new MoveBuilder("flying_planet_thousand_wheels", "Flying Planet Thousand Wheels")
            .withDescription("Upward kick that lifts both you and the target. Sets up aerial follow-ups.")
            .withTiming(28, 2, 9)
            .withDamage(5.0f)
            .withRange(2.2f)
            .withKnockback(0.2f)
            .withHitStun(12)
            .withHitboxSize(1.2f)
            .withStaminaCost(8.0f)
            .build();

    public static final MoveConfiguration EIGHT_LAYERED_DEMON_CORE = new MoveBuilder("eight_layered_demon_core", "Eight-Layered Demon Core")
            .withDescription("Eight stacked punches firing overlapping shockwave rings forward — one per punch.")
            // 8 punches × 3-tick interval = 21 active ticks needed; 24 gives a small buffer.
            .withTiming(20, 6, 24)
            .withDamage(3.0f)
            .withRange(2.4f)
            .withKnockback(0.4f)
            .withHitStun(8)
            .withHitboxSize(1.3f)
            .withStaminaCost(14.0f)
            .build();

    public static final MoveConfiguration DONUT = new MoveBuilder("donut", "Donut")
            .withAnimation("nichirin:donut", 6)
            .withDescription("Close-range arm-thrust. Single target, massive damage, impalement slow.")
            .withTiming(80, 12, 8)
            .withDamage(18.0f)
            .withRange(1.5f)
            .withKnockback(0.1f)
            .withHitStun(40)
            .withHyperArmor()
            .withHitboxSize(1.2f)
            .withStaminaCost(18.0f)
            .build();

    public static final MoveConfiguration TEN_THOUSAND_LEAVES = new MoveBuilder("ten_thousand_leaves_flashing_willow", "Ten Thousand Leaves Flashing Willow")
            .withDescription("25s windup. Hyper-armoured ring-shaped AoE around you. 1.5s stun on hit.")
            .withTiming(500, 8, 14)
            .withDamage(12.0f)
            .withRange(5.0f)
            .withKnockback(0.55f)
            .withHitStun(30)
            .withHyperArmor()
            .withHitboxSize(5.0f)
            .withStaminaCost(20.0f)
            .build();

    public static final MoveConfiguration DEMON_GUT_PUNCH = new MoveBuilder("demon_gut_punch", "Gut Punch")
            .withDescription("Powerful close-range punch that stuns enemies.")
            .withTiming(15, 1, 7)
            .withDamage(6.0f)
            .withRange(2.0f)
            .withKnockback(0.1f)
            .withHitStun(15)
            .withHitboxSize(2.8f)
            .build();

    public static final MoveConfiguration DEMON_SLASH = new MoveBuilder("demon_slash", "Slash")
            .withDescription("Basic claw slash, press again for the finisher.")
            .withTiming(0, 0, 14)
            .withDamage(4.0f)
            .withRange(3.0f)
            .withKnockback(0f)
            .withHitStun(10)
            .withHitboxSize(2.0f)
            .build();

    private static final MoveConfiguration DEMON_SLASH_2 = new MoveBuilder("demon_slash_2", "Slash Finisher")
            .withDescription("Finishing claw slash after the initial slash.")
            .withTiming(5, 0, 18)
            .withDamage(6.0f)
            .withRange(3.0f)
            .withKnockback(0.5f)
            .withHitStun(5)
            .withHitboxSize(2.2f)
            .build();

    public static final MoveConfiguration HIGH_JUMP = new MoveBuilder("high_jump", "High Jump")
            .withAnimation("nichirin:demon_high_jump", 8)
            .withDescription("Launch upward and lift nearby enemies.")
            .withTiming(220, 0, 4)
            .build();

    public static final MoveConfiguration DEMON_STOMP = new MoveBuilder("demon_stomp", "Stomp")
            .withDescription("Airborne stomp that slams enemies down.")
            .withTiming(60, 0, 11)
            .withDamage(10.0f)
            .withRange(4.0f)
            .withKnockback(0.8f)
            .withHitStun(30)
            .withHitboxSize(3.0f)
            .build();

    public static final MoveConfiguration DEMON_KICK = new MoveBuilder("demon_kick", "Kick")
            .withDescription("Powerful front kick with high knockback.")
            .withTiming(60, 5, 11)
            .withDamage(6.0f)
            .withRange(2.5f)
            .withKnockback(1f)
            .withHitStun(25)
            .withHitboxSize(2.0f)
            .build();

    public static final MoveConfiguration DASHING_STRIKE = new MoveBuilder("dashing_strike", "Dashing Strike")
            .withAnimation("nichirin:demon_dash_strike", 10)
            .withDescription("Dash forward and deliver a devastating punch.")
            .withTiming(140, 8, 6)
            .withDamage(12.0f)
            .withDashSpeed(6.0f)
            .withRange(5.5f)
            .withKnockback(0.2f)
            .withHitStun(20)
            .withHitboxSize(2)
            .build();

    public static final MoveConfiguration DEMON_BITE = new MoveBuilder("demon_bite", "Bite")
            .withDescription("Bite attack that steals blood.")
            .withTiming(100, 5, 11)
            .withDamage(5.0f)
            .withRange(2.0f)
            .withKnockback(0.1f)
            .withHitStun(20)
            .withHitboxSize(2.0f)
            .build();

    public static final MoveConfiguration DEMON_GRAB = new MoveBuilder("demon_grab", "Throw")
            .withDescription("Grab and instantly throw the target forward.")
            .withTiming(80, 3, 8)
            .build();

    private static final Map<String, MoveConfiguration> CONFIGS = buildConfigMap();

    private static class SlashComboState {
        int currentStage = 0;
        long slash1GameTick = -1;
        static final long MIN_FOLLOWUP_TICKS = 8;
        static final long MAX_FOLLOWUP_TICKS = 40;

        boolean isReadyForSlash2(long currentTick) {
            if (slash1GameTick < 0) return false;
            long elapsed = currentTick - slash1GameTick;
            return elapsed >= MIN_FOLLOWUP_TICKS && elapsed <= MAX_FOLLOWUP_TICKS;
        }

        void recordSlash1(long currentTick) {
            slash1GameTick = currentTick;
            currentStage = 1;
        }

        void reset() {
            currentStage = 0;
            slash1GameTick = -1;
        }
    }

    private static class CqcFollowupState {
        final String baseMoveId;
        final String followupMoveId;
        boolean queued;

        CqcFollowupState(String baseMoveId, String followupMoveId) {
            this.baseMoveId = baseMoveId;
            this.followupMoveId = followupMoveId;
        }
    }

    public CqcMoveset() {
        super(ID, "CQC", MovesetType.NEUTRAL, buildMoveset());
    }

    @Override
    public int getMoveCount() {
        return CqcPresetData.WHEEL_SLOT_COUNT;
    }

    @Override
    public MoveConfiguration getMove(int index) {
        CqcPresetData preset = currentPreset();
        String moveId = preset != null ? preset.getWheelMove(index) : defaultPreset().getWheelMove(index);
        return configurationFor(moveId);
    }

    @Override
    public MoveConfiguration getLeftClickConfiguration() {
        CqcPresetData preset = currentPreset();
        return configurationFor(preset != null ? preset.getLeftClickMove() : defaultPreset().getLeftClickMove());
    }

    @Override
    public MoveConfiguration getRightClickConfiguration() {
        CqcPresetData preset = currentPreset();
        return configurationFor(preset != null ? preset.getRightClickMove() : defaultPreset().getRightClickMove());
    }

    @Override
    public MoveConfiguration getCrouchRightClickConfiguration() {
        CqcPresetData preset = currentPreset();
        return configurationFor(preset != null ? preset.getCrouchRightClickMove() : defaultPreset().getCrouchRightClickMove());
    }

    @Override
    public boolean handleLeftClick(LivingEntity entity) {
        if (!canUseCqc(entity)) return false;
        executeConfigured(entity, getLeftClickConfiguration(), CqcInputSlot.LEFT_CLICK);
        return true;
    }

    @Override
    public boolean handleRightClick(LivingEntity entity, boolean isCrouching) {
        if (!canUseCqc(entity)) return false;
        executeConfigured(entity, isCrouching ? getCrouchRightClickConfiguration() : getRightClickConfiguration(),
                isCrouching ? CqcInputSlot.CROUCH_RIGHT_CLICK : CqcInputSlot.RIGHT_CLICK);
        return true;
    }

    @Override
    public void performMove(LivingEntity entity, int moveIndex) {
        if (!canUseCqc(entity)) return;
        executeConfigured(entity, getMove(moveIndex), CqcInputSlot.WHEEL);
    }

    @Override
    public String getLeftClickMoveName() {
        MoveConfiguration config = getLeftClickConfiguration();
        return config != null ? config.getDisplayName() : "Left Strike";
    }

    @Override
    public String getRightClickMoveName() {
        MoveConfiguration config = getRightClickConfiguration();
        return config != null ? config.getDisplayName() : "Right Strike";
    }

    @Override
    public String getCrouchRightClickMoveName() {
        MoveConfiguration config = getCrouchRightClickConfiguration();
        return config != null ? config.getDisplayName() : "Crouch Strike";
    }

    private boolean canUseCqc(LivingEntity entity) {
        return entity instanceof Player player
                && player.getMainHandItem().isEmpty()
                && (player.getOffhandItem().isEmpty() || player.getOffhandItem().has(DataComponents.FOOD));
    }

    private void executeConfigured(LivingEntity entity, MoveConfiguration config, CqcInputSlot inputSlot) {
        if (entity.level().isClientSide()) return;
        if (queueFollowup(entity)) return;
        if (config == null) return;
        if (entity.hasEffect(NichirinEffectRegistry.stunned())) return;
        if (MoveExecutor.hasActiveAttacks(entity)) return;
        CqcMoveCatalog.Definition definition = CqcMoveCatalog.get(config.getMoveId());
        if (definition == null) return;
        if (definition.demonOnly() && !canUseDemonOnlyMove(entity)) {
            if (entity instanceof Player player) {
                player.displayClientMessage(Component.literal("Can only be used by demons.")
                        .withStyle(style -> style.withColor(0xFF5555)), true);
            }
            return;
        }
        if (isOnCooldown(entity, config)) {
            if (entity instanceof Player player) {
                int remaining = getRemainingCooldown(entity, config);
                player.displayClientMessage(Component.literal("Move on cooldown! " + String.format("%.1f", remaining / 20.0f) + "s remaining")
                        .withStyle(style -> style.withColor(0xFF5555)), true);
            }
            return;
        }
        if (entity instanceof Player player && config.hasStaminaCost() && !StaminaManager.consume(player, config.getStaminaCostOrDefault(0f))) {
            player.displayClientMessage(Component.literal("Not enough stamina!"), true);
            return;
        }
        if (executeDemonPhysical(entity, config, definition, inputSlot)) {
            startFollowupWindow(entity, config);
            return;
        }
        triggerAnimation(entity, definition.animationName());
        AbstractCqcAttack attack = createAttack(config.getMoveId());
        if (attack == null) return;
        attack.configure(config);
        if (usesOrthodoxDamageBonus(entity, inputSlot)) {
            attack.applyDamageMultiplier(1.2f);
        }
        MoveExecutor.executeAttackWithInfo(entity, attack, ID, config);
        setCooldown(entity, config);
        startFollowupWindow(entity, config);
    }

    private boolean executeDemonPhysical(LivingEntity entity, MoveConfiguration config, CqcMoveCatalog.Definition definition,
                                         CqcInputSlot inputSlot) {
        String moveId = CqcMoveCatalog.normalize(config.getMoveId());
        if (!isDemonPhysicalMove(moveId)) return false;
        if ("demon_slash".equals(moveId)) {
            executeSlashCombo(entity, usesOrthodoxDamageBonus(entity, inputSlot));
            return true;
        }

        triggerAnimation(entity, definition.animationName());
        Object attack = createDemonAttack(moveId);
        if (attack == null) return true;
        if (attack instanceof DemonGrabAttack grabAttack) {
            grabAttack.execute(entity);
            MoveExecutor.sendCooldownDisplay(entity instanceof Player player ? player : null, ID, config);
            setCooldown(entity, config);
            return true;
        }
        if (attack instanceof AbstractDemonAttack<?, ?> demonAttack) {
            demonAttack.configure(config);
            if (!definition.demonOnly()) {
                demonAttack.allowNonDemonUser();
            }
            if (canUseDemonOnlyMove(entity)) {
                demonAttack.applyStatMultiplier(AbstractCqcAttack.DEMON_CQC_STAT_MULTIPLIER);
            }
            if (usesOrthodoxDamageBonus(entity, inputSlot)) {
                demonAttack.applyDamageMultiplier(1.2f);
            }
        }
        MoveExecutor.executeAttackWithInfo(entity, attack, ID, config);
        if ("dashing_strike".equals(moveId) && entity instanceof Player player) {
            MoveExecutor.sendCooldownDisplay(player, ID, config);
        }
        setCooldown(entity, config);
        return true;
    }

    private boolean queueFollowup(LivingEntity entity) {
        CqcFollowupState state = FOLLOWUP_STATES.get(entity.getUUID());
        if (state == null || state.queued) return false;
        state.queued = true;
        if (entity instanceof Player player) {
            player.displayClientMessage(Component.literal("Followup queued!")
                    .withStyle(style -> style.withColor(0x55FF55)), true);
        }
        return true;
    }

    private void startFollowupWindow(LivingEntity entity, MoveConfiguration config) {
        if (!(entity instanceof Player player)) return;
        String baseMoveId = CqcMoveCatalog.normalize(config.getMoveId());
        if (!CqcPresetData.canHaveFollowup(baseMoveId)) return;
        String followupMoveId = PlayerDataProvider.getData(player).getCqcPresetData().getFollowupMove(baseMoveId);
        if (CqcMoveCatalog.normalize(followupMoveId).isEmpty()) return;
        MoveConfiguration followupConfig = configurationFor(followupMoveId);
        if (followupConfig == null) return;

        CqcFollowupState state = new CqcFollowupState(baseMoveId, CqcMoveCatalog.normalize(followupMoveId));
        FOLLOWUP_STATES.put(entity.getUUID(), state);
        int delayTicks = Math.max(1, config.getWindupOrDefault(0) + config.getDurationOrDefault(0));
        MinecraftServer server = entity.getServer();
        CompletableFuture.delayedExecutor(delayTicks * 50L, TimeUnit.MILLISECONDS).execute(() -> {
            if (server != null) {
                server.execute(() -> executeQueuedFollowup(entity, state));
            } else {
                executeQueuedFollowup(entity, state);
            }
        });
    }

    private void executeQueuedFollowup(LivingEntity entity, CqcFollowupState state) {
        CqcFollowupState current = FOLLOWUP_STATES.get(entity.getUUID());
        if (current != state) return;
        FOLLOWUP_STATES.remove(entity.getUUID());
        if (!state.queued || !entity.isAlive() || !canUseCqc(entity)) return;
        MoveConfiguration followupConfig = configurationFor(state.followupMoveId);
        if (followupConfig == null) return;
        executeFollowupConfigured(entity, followupConfig);
    }

    private void executeFollowupConfigured(LivingEntity entity, MoveConfiguration config) {
        if (entity.level().isClientSide()) return;
        if (entity.hasEffect(NichirinEffectRegistry.stunned())) {
            entity.removeEffect(NichirinEffectRegistry.stunned());
        }
        if (MoveExecutor.hasActiveAttacks(entity)) return;
        CqcMoveCatalog.Definition definition = CqcMoveCatalog.get(config.getMoveId());
        if (definition == null) return;
        if (definition.demonOnly() && !canUseDemonOnlyMove(entity)) return;
        if (isOnCooldown(entity, config)) return;
        if (entity instanceof Player player && config.hasStaminaCost() && !StaminaManager.consume(player, config.getStaminaCostOrDefault(0f))) return;
        if (executeDemonPhysical(entity, config, definition, CqcInputSlot.FOLLOWUP)) return;
        triggerAnimation(entity, definition.animationName());
        AbstractCqcAttack attack = createAttack(config.getMoveId());
        if (attack == null) return;
        attack.configure(config);
        MoveExecutor.executeAttackWithInfo(entity, attack, ID, config);
        setCooldown(entity, config);
    }

    private void executeSlashCombo(LivingEntity entity, boolean damageBonus) {
        UUID entityUUID = entity.getUUID();
        long currentTick = entity.level().getGameTime();
        SlashComboState comboState = SLASH_STATES.computeIfAbsent(entityUUID, k -> new SlashComboState());

        if (comboState.currentStage == 1) {
            if (comboState.isReadyForSlash2(currentTick)) {
                executeSlashStage(entity, 1, comboState, currentTick, damageBonus);
                return;
            } else if (currentTick - comboState.slash1GameTick <= SlashComboState.MAX_FOLLOWUP_TICKS) {
                return;
            }
            comboState.reset();
        }

        executeSlashStage(entity, 0, comboState, currentTick, damageBonus);
    }

    private void executeSlashStage(LivingEntity entity, int stage, SlashComboState comboState, long currentTick, boolean damageBonus) {
        MoveConfiguration slashConfig = stage == 0 ? DEMON_SLASH : DEMON_SLASH_2;
        String animationName = stage == 0 ? "demon_slash" : "demon_slash_2";

        triggerAnimation(entity, animationName);
        DemonSlashAttack slashAttack = new DemonSlashAttack();
        slashAttack.setSlashStage(stage + 1);
        slashAttack.configure(slashConfig);
        if (canUseDemonOnlyMove(entity)) {
            slashAttack.applyStatMultiplier(AbstractCqcAttack.DEMON_CQC_STAT_MULTIPLIER);
        }
        if (damageBonus) {
            slashAttack.applyDamageMultiplier(1.2f);
        }
        MoveExecutor.executeAttackWithInfo(entity, slashAttack, ID, slashConfig);

        if (stage == 0) {
            comboState.recordSlash1(currentTick);
        } else {
            comboState.reset();
            setCooldown(entity, DEMON_SLASH);
        }
    }

    private boolean isDemonPhysicalMove(String moveId) {
        return switch (moveId) {
            case "demon_gut_punch", "demon_slash", "high_jump", "demon_stomp", "demon_kick",
                    "dashing_strike", "demon_bite", "demon_grab" -> true;
            default -> false;
        };
    }

    public static boolean isDemonOnlyMove(String moveId) {
        return switch (CqcMoveCatalog.normalize(moveId)) {
            case "demon_slash", "high_jump", "demon_bite" -> true;
            default -> false;
        };
    }

    /**
     * True when the CQC move belongs to the Destructive Death set — these get the light-blue
     * border in the CQC GUI and extra enhancement when the user has Destructive Death equipped.
     */
    public static boolean isDestructiveDeathMove(String moveId) {
        return switch (CqcMoveCatalog.normalize(moveId)) {
            case "snap_punch", "annihilation_type", "crown_splitter",
                 "flying_planet_thousand_wheels", "eight_layered_demon_core",
                 "ten_thousand_leaves_flashing_willow", "donut" -> true;
            default -> false;
        };
    }

    private boolean canUseDemonOnlyMove(LivingEntity entity) {
        return entity instanceof Player player && DemonManager.isDemon(player);
    }

    private boolean usesOrthodoxDamageBonus(LivingEntity entity, CqcInputSlot inputSlot) {
        if (!(entity instanceof Player player)) return false;
        if (inputSlot != CqcInputSlot.LEFT_CLICK && inputSlot != CqcInputSlot.RIGHT_CLICK) return false;
        return PlayerDataProvider.getData(player).getCqcPresetData().getStanceIndex() == 1;
    }

    private Object createDemonAttack(String moveId) {
        return switch (moveId) {
            case "demon_gut_punch" -> new DemonGutPunchAttack();
            case "high_jump" -> new DemonHighJumpAttack();
            case "demon_stomp" -> new DemonStompAttack();
            case "demon_kick" -> new DemonKickAttack();
            case "dashing_strike" -> new DemonDashStrikeAttack();
            case "demon_bite" -> new DemonBiteAttack();
            case "demon_grab" -> new DemonGrabAttack();
            default -> null;
        };
    }

    private AbstractCqcAttack createAttack(String moveId) {
        return switch (CqcMoveCatalog.normalize(moveId)) {
            case "jab" -> new CqcJabAttack();
            case "cross" -> new CqcCrossAttack();
            case "lefthook" -> new CqcLeftHookAttack();
            case "eye_poke" -> new CqcEyePokeAttack();
            case "throat_chop" -> new CqcThroatChopAttack();
            case "uppercut" -> new CqcUppercutAttack();
            case "spinning_heel_kick" -> new CqcSpinningHeelKickAttack();
            case "knee" -> new CqcKneeAttack();
            case "low_kick" -> new CqcLowKickAttack();
            case "double_palm" -> new CqcDoublePalmAttack();
            case "snap_punch" -> new CqcSnapPunchAttack();
            case "annihilation_type" -> new CqcAnnihilationTypeAttack();
            case "crown_splitter" -> new CqcCrownSplitterAttack();
            case "flying_planet_thousand_wheels" -> new CqcFlyingPlanetThousandWheelsAttack();
            case "eight_layered_demon_core" -> new CqcEightLayeredDemonCoreAttack();
            case "ten_thousand_leaves_flashing_willow" -> new CqcTenThousandLeavesFlashingWillowAttack();
            case "donut" -> new CqcDonutAttack();
            default -> null;
        };
    }

    @Override
    public int getAnimationDurationTicks(String animationName, int fallback) {
        CqcMoveCatalog.Definition definition = findDefinitionByAnimation(animationName);
        if (definition == null) return fallback;
        MoveConfiguration config = configurationFor(definition.id());
        if (config == null) return fallback;
        return config.getWindupOrDefault(0) + config.getDurationOrDefault(0);
    }

    public static MoveConfiguration configurationFor(String moveId) {
        return CONFIGS.get(CqcMoveCatalog.normalize(moveId));
    }

    public static Map<String, MoveConfiguration> configurations() {
        return CONFIGS;
    }

    public static String animationNameFor(MoveConfiguration config) {
        if (config == null) return "";
        return config.getAnimationId() != null
                ? config.getAnimationId().getPath()
                : config.getMoveId();
    }

    private static CqcPresetData defaultPreset() {
        return DefaultPresetHolder.INSTANCE;
    }

    private static MovesetBuilder buildMoveset() {
        // CQC has no static click moves — left/right/crouch slots are resolved per-player from the
        // active preset (see getLeftClickConfiguration etc.), so only wheel moves are registered here.
        MovesetBuilder builder = new MovesetBuilder();
        for (MoveConfiguration config : CONFIGS.values()) {
            builder.withMove(config);
        }
        return builder;
    }

    private static Map<String, MoveConfiguration> buildConfigMap() {
        Map<String, MoveConfiguration> configs = new LinkedHashMap<>();
        register(configs, JAB);
        register(configs, CROSS);
        register(configs, LEFT_HOOK);
        register(configs, EYE_POKE);
        register(configs, THROAT_CHOP);
        register(configs, UPPERCUT);
        register(configs, SPINNING_HEEL_KICK);
        register(configs, KNEE);
        register(configs, LOW_KICK);
        register(configs, DOUBLE_PALM);
        register(configs, SNAP_PUNCH);
        register(configs, ANNIHILATION_TYPE);
        register(configs, CROWN_SPLITTER);
        register(configs, FLYING_PLANET);
        register(configs, EIGHT_LAYERED_DEMON_CORE);
        register(configs, TEN_THOUSAND_LEAVES);
        register(configs, DONUT);
        register(configs, DEMON_GUT_PUNCH);
        register(configs, DEMON_SLASH);
        register(configs, HIGH_JUMP);
        register(configs, DEMON_STOMP);
        register(configs, DEMON_KICK);
        register(configs, DASHING_STRIKE);
        register(configs, DEMON_BITE);
        register(configs, DEMON_GRAB);
        return Collections.unmodifiableMap(configs);
    }

    private static void register(Map<String, MoveConfiguration> configs, MoveConfiguration config) {
        configs.put(CqcMoveCatalog.normalize(config.getMoveId()), config);
    }

    private static CqcMoveCatalog.Definition findDefinitionByAnimation(String animationName) {
        if (animationName == null || animationName.isEmpty()) return null;
        for (CqcMoveCatalog.Definition definition : CqcMoveCatalog.all()) {
            if (definition.animationName().equals(animationName) || definition.id().equals(animationName)) {
                return definition;
            }
        }
        return null;
    }

    // CQC now shares the single server-authoritative cooldown store (ServerCooldownManager) with every
    // other moveset, keyed by move id — no separate CQC cooldown map.
    private static boolean isOnCooldown(LivingEntity entity, MoveConfiguration config) {
        return ServerCooldownManager.isOnCooldown(entity, config.getMoveId());
    }

    private static int getRemainingCooldown(LivingEntity entity, MoveConfiguration config) {
        return ServerCooldownManager.getRemaining(entity, config.getMoveId());
    }

    private static void setCooldown(LivingEntity entity, MoveConfiguration config) {
        ServerCooldownManager.set(entity, config.getMoveId(), config.getCooldownOrDefault(0));
    }

    public static void resetCooldowns(Player player) {
        ServerCooldownManager.clear(player.getUUID());
        SLASH_STATES.remove(player.getUUID());
        FOLLOWUP_STATES.remove(player.getUUID());
    }

    private CqcPresetData currentPreset() {
        Player player = CurrentPlayerHolder.PLAYER.get();
        if (player == null) return null;
        return PlayerDataProvider.getData(player).getCqcPresetData();
    }

    public static void withPlayer(Player player, Runnable action) {
        CurrentPlayerHolder.PLAYER.set(player);
        try {
            action.run();
        } finally {
            CurrentPlayerHolder.PLAYER.remove();
        }
    }

    private static final class CurrentPlayerHolder {
        private static final ThreadLocal<Player> PLAYER = new ThreadLocal<>();
    }

    private static final class DefaultPresetHolder {
        private static final CqcPresetData INSTANCE = new CqcPresetData();
    }

    private enum CqcInputSlot {
        LEFT_CLICK,
        RIGHT_CLICK,
        CROUCH_RIGHT_CLICK,
        WHEEL,
        FOLLOWUP
    }
}
