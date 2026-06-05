package com.xirc.nichirin.common.attack.moveset;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.moves.cqc.*;
import com.xirc.nichirin.common.data.CqcMoveCatalog;
import com.xirc.nichirin.common.data.CqcPresetData;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.common.util.StaminaManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Customizable close-quarters-combat moveset. The registered moveset is a template;
 * player-owned CQC preset data decides which move occupies each slot.
 */
public class CqcMoveset extends AbstractMoveset {

    public static final String ID = "cqc";
    private static final CqcPresetData DEFAULT_PRESET = new CqcPresetData();
    private static final Map<UUID, Map<String, Long>> COOLDOWNS = new ConcurrentHashMap<>();

    public static final MoveConfiguration JAB = new MoveBuilder("jab", "Jab")
            .withAnimation("nichirin:jab", 6)
            .withDescription("Short gut jab. Fast body-shot that stuns at close range.")
            .withTiming(10, 1, 3)
            .withDamage(4.25f)
            .withRange(1.15f)
            .withKnockback(0.08f)
            .withHitStun(9)
            .withHitboxSize(1.9f)
            .withStaminaCost(4.0f)
            .build();

    public static final MoveConfiguration CROSS = new MoveBuilder("cross", "Cross")
            .withAnimation("nichirin:cross", 6)
            .withDescription("Committed straight punch. Pulls you into boxing range.")
            .withTiming(16, 2, 8)
            .withDamage(3.0f)
            .withRange(1.6f)
            .withKnockback(0.25f)
            .withHitStun(12)
            .withHitboxSize(0.95f)
            .withStaminaCost(5.0f)
            .withDashSpeed(0.9f)
            .build();

    public static final MoveConfiguration LEFT_HOOK = new MoveBuilder("lefthook", "Left Hook")
            .withAnimation("nichirin:lefthook", 6)
            .withDescription("Short hook with a wider hitbox and stronger stagger.")
            .withTiming(20, 3, 7)
            .withDamage(3.5f)
            .withRange(1.55f)
            .withKnockback(0.45f)
            .withHitStun(15)
            .withHitboxSize(1.2f)
            .withStaminaCost(6.0f)
            .build();

    public static final MoveConfiguration ROUNDHOUSE_FAST = new MoveBuilder("roundhouse_fast", "Roundhouse Fast")
            .withAnimation("nichirin:roundhouse_fast", 6)
            .withDescription("Fast sweeping kick with extra reach.")
            .withTiming(24, 3, 8)
            .withDamage(3.5f)
            .withRange(2.0f)
            .withKnockback(0.65f)
            .withHitStun(13)
            .withHitboxSize(1.35f)
            .withStaminaCost(7.0f)
            .build();

    public static final MoveConfiguration EYE_POKE = new MoveBuilder("eye_poke", "Eye Poke")
            .withAnimation("nichirin:eye_poke", 6)
            .withDescription("Low damage poke that briefly blinds and interrupts.")
            .withTiming(28, 2, 5)
            .withDamage(1.5f)
            .withRange(1.25f)
            .withKnockback(0.05f)
            .withHitStun(21)
            .withHitboxSize(0.75f)
            .withStaminaCost(5.0f)
            .build();

    public static final MoveConfiguration THROAT_CHOP = new MoveBuilder("throat_chop", "Throat Chop")
            .withAnimation("nichirin:throat_chop", 6)
            .withDescription("Close interrupt that slows and locks down a target.")
            .withTiming(30, 3, 5)
            .withDamage(2.5f)
            .withRange(1.35f)
            .withKnockback(0.15f)
            .withHitStun(25)
            .withHitboxSize(0.85f)
            .withStaminaCost(7.0f)
            .build();

    public static final MoveConfiguration HEADKICK = new MoveBuilder("headkick", "Headkick")
            .withAnimation("nichirin:headkick", 6)
            .withDescription("Heavy high kick that heavily staggers targets.")
            .withTiming(46, 5, 9)
            .withDamage(5.5f)
            .withRange(2.05f)
            .withKnockback(0.75f)
            .withHitStun(17)
            .withHitboxSize(1.25f)
            .withStaminaCost(10.0f)
            .build();

    public static final MoveConfiguration SPINNING_BACKFIST = new MoveBuilder("spinning_backfist", "Spinning Backfist")
            .withAnimation("nichirin:spinning_backfist", 6)
            .withDescription("Spinning strike with a wide arc and solid knockback.")
            .withTiming(42, 4, 9)
            .withDamage(4.75f)
            .withRange(1.8f)
            .withKnockback(0.75f)
            .withHitStun(15)
            .withHitboxSize(1.4f)
            .withStaminaCost(9.0f)
            .build();

    public static final MoveConfiguration OVERHAND_RIGHT = new MoveBuilder("overhand_right", "Overhand Right")
            .withAnimation("nichirin:overhand_right", 6)
            .withDescription("Heavy downward punch that punishes airborne targets.")
            .withTiming(34, 4, 7)
            .withDamage(4.25f)
            .withRange(1.55f)
            .withKnockback(0.45f)
            .withHitStun(14)
            .withHitboxSize(1.0f)
            .withStaminaCost(8.0f)
            .build();

    public static final MoveConfiguration UPPERCUT = new MoveBuilder("uppercut", "Uppercut")
            .withAnimation("nichirin:uppercut", 6)
            .withDescription("Dashing launcher. Dashes 5 blocks and knocks targets upward.")
            .withTiming(38, 4, 8)
            .withDamage(4.25f)
            .withRange(1.65f)
            .withKnockback(0.25f)
            .withHitStun(19)
            .withHitboxSize(1.0f)
            .withStaminaCost(10.0f)
            .withDashSpeed(10.0f)
            .build();

    public static final MoveConfiguration KNEE_STRIKE = new MoveBuilder("knee_strike", "Knee Strike")
            .withAnimation("nichirin:knee_strike", 6)
            .withDescription("Close knee that pulls targets into clinch range.")
            .withTiming(34, 4, 8)
            .withDamage(4.0f)
            .withRange(1.3f)
            .withKnockback(0.1f)
            .withHitStun(19)
            .withHitboxSize(0.9f)
            .withStaminaCost(8.0f)
            .build();

    public static final MoveConfiguration ELBOW_STRIKE = new MoveBuilder("elbow_strike", "Elbow Strike")
            .withAnimation("nichirin:elbow_strike", 6)
            .withDescription("Compact elbow with fast armor-break style impact.")
            .withTiming(24, 2, 6)
            .withDamage(3.25f)
            .withRange(1.25f)
            .withKnockback(0.35f)
            .withHitStun(15)
            .withHitboxSize(0.85f)
            .withStaminaCost(6.0f)
            .build();

    public static final MoveConfiguration SPINNING_HEEL_KICK = new MoveBuilder("spinning_heel_kick", "Spinning Heel Kick")
            .withAnimation("nichirin:spinning_heel_kick", 6)
            .withDescription("Slow, wide, heavy kick that launches sideways.")
            .withTiming(56, 6, 11)
            .withDamage(6.5f)
            .withRange(2.25f)
            .withKnockback(1.0f)
            .withHitStun(17)
            .withHitboxSize(1.55f)
            .withStaminaCost(13.0f)
            .build();

    public static final MoveConfiguration KNEE = new MoveBuilder("knee", "Knee")
            .withAnimation("nichirin:knee", 6)
            .withDescription("Short, reliable knee with strong stun.")
            .withTiming(28, 3, 7)
            .withDamage(3.5f)
            .withRange(1.25f)
            .withKnockback(0.25f)
            .withHitStun(18)
            .withHitboxSize(0.85f)
            .withStaminaCost(7.0f)
            .build();

    public static final MoveConfiguration AXE_KICK = new MoveBuilder("axe_kick", "Axe Kick")
            .withAnimation("nichirin:axe_kick", 6)
            .withDescription("Heavy vertical kick that slams airborne targets down.")
            .withTiming(48, 5, 9)
            .withDamage(5.75f)
            .withRange(1.75f)
            .withKnockback(0.3f)
            .withHitStun(15)
            .withHitboxSize(1.15f)
            .withStaminaCost(11.0f)
            .build();

    public static final MoveConfiguration LOW_KICK = new MoveBuilder("low_kick", "Low Kick")
            .withAnimation("nichirin:low_kick", 6)
            .withDescription("Fast leg kick that slows grounded targets.")
            .withTiming(22, 2, 6)
            .withDamage(2.75f)
            .withRange(1.65f)
            .withKnockback(0.35f)
            .withHitStun(13)
            .withHitboxSize(1.0f)
            .withStaminaCost(5.0f)
            .build();

    public static final MoveConfiguration SUPERMAN_PUNCH = new MoveBuilder("superman_punch", "Superman Punch")
            .withAnimation("nichirin:superman_punch", 6)
            .withDescription("Leaping punch with reach and forward burst.")
            .withTiming(44, 4, 9)
            .withDamage(4.5f)
            .withRange(2.1f)
            .withKnockback(0.65f)
            .withHitStun(15)
            .withHitboxSize(1.05f)
            .withStaminaCost(10.0f)
            .withDashSpeed(2.4f)
            .build();

    public static final MoveConfiguration DOUBLE_PALM = new MoveBuilder("double_palm", "Double Palm")
            .withAnimation("nichirin:double_palm", 6)
            .withDescription("Two-handed shove that creates space.")
            .withTiming(32, 3, 7)
            .withDamage(3.75f)
            .withRange(1.45f)
            .withKnockback(1.15f)
            .withHitStun(14)
            .withHitboxSize(1.15f)
            .withStaminaCost(8.0f)
            .build();

    public static final MoveConfiguration BACKHAND_SLAP = new MoveBuilder("backhand_slap", "Backhand Slap")
            .withAnimation("nichirin:backhand_slap", 6)
            .withDescription("Fast backhand counter with sideways displacement.")
            .withTiming(24, 2, 6)
            .withDamage(2.75f)
            .withRange(1.4f)
            .withKnockback(0.5f)
            .withHitStun(14)
            .withHitboxSize(0.95f)
            .withStaminaCost(5.0f)
            .build();

    private static final Map<String, MoveConfiguration> CONFIGS = buildConfigMap();

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
        String moveId = preset != null ? preset.getWheelMove(index) : DEFAULT_PRESET.getWheelMove(index);
        return configurationFor(moveId);
    }

    @Override
    public MoveConfiguration getLeftClickConfiguration() {
        CqcPresetData preset = currentPreset();
        return configurationFor(preset != null ? preset.getLeftClickMove() : DEFAULT_PRESET.getLeftClickMove());
    }

    @Override
    public MoveConfiguration getRightClickConfiguration() {
        CqcPresetData preset = currentPreset();
        return configurationFor(preset != null ? preset.getRightClickMove() : DEFAULT_PRESET.getRightClickMove());
    }

    @Override
    public MoveConfiguration getCrouchRightClickConfiguration() {
        CqcPresetData preset = currentPreset();
        return configurationFor(preset != null ? preset.getCrouchRightClickMove() : DEFAULT_PRESET.getCrouchRightClickMove());
    }

    @Override
    public boolean handleLeftClick(LivingEntity entity) {
        if (!canUseCqc(entity)) return false;
        executeConfigured(entity, getLeftClickConfiguration());
        return true;
    }

    @Override
    public boolean handleRightClick(LivingEntity entity, boolean isCrouching) {
        if (!canUseCqc(entity)) return false;
        executeConfigured(entity, isCrouching ? getCrouchRightClickConfiguration() : getRightClickConfiguration());
        return true;
    }

    @Override
    public void performMove(LivingEntity entity, int moveIndex) {
        if (!canUseCqc(entity)) return;
        executeConfigured(entity, getMove(moveIndex));
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
        return entity instanceof Player player && player.getMainHandItem().isEmpty();
    }

    private void executeConfigured(LivingEntity entity, MoveConfiguration config) {
        if (config == null) return;
        if (entity.level().isClientSide()) return;
        if (entity.hasEffect(com.xirc.nichirin.registry.NichirinEffectRegistry.stunned())) return;
        if (MoveExecutor.hasActiveAttacks(entity)) return;
        if (isOnCooldown(entity, config)) {
            if (entity instanceof Player player) {
                int remaining = getRemainingCooldown(entity, config);
                player.displayClientMessage(Component.literal("Move on cooldown! " + String.format("%.1f", remaining / 20.0f) + "s remaining"), true);
            }
            return;
        }
        if (entity instanceof Player player && config.hasStaminaCost() && !StaminaManager.consume(player, config.getStaminaCostOrDefault(0f))) {
            player.displayClientMessage(Component.literal("Not enough stamina!"), true);
            return;
        }
        CqcMoveCatalog.Definition definition = CqcMoveCatalog.get(config.getMoveId());
        if (definition == null) return;
        triggerAnimation(entity, definition.animationName());
        AbstractCqcAttack attack = createAttack(config.getMoveId());
        if (attack == null) return;
        attack.configure(config);
        MoveExecutor.executeAttackWithInfo(entity, attack, config.getDisplayName(), config.getCooldownOrDefault(0));
        setCooldown(entity, config);
    }

    private AbstractCqcAttack createAttack(String moveId) {
        return switch (CqcMoveCatalog.normalize(moveId)) {
            case "jab" -> new CqcJabAttack();
            case "cross" -> new CqcCrossAttack();
            case "lefthook" -> new CqcLeftHookAttack();
            case "roundhouse_fast" -> new CqcRoundhouseFastAttack();
            case "eye_poke" -> new CqcEyePokeAttack();
            case "throat_chop" -> new CqcThroatChopAttack();
            case "headkick" -> new CqcHeadkickAttack();
            case "spinning_backfist" -> new CqcSpinningBackfistAttack();
            case "overhand_right" -> new CqcOverhandRightAttack();
            case "uppercut" -> new CqcUppercutAttack();
            case "knee_strike" -> new CqcKneeStrikeAttack();
            case "elbow_strike" -> new CqcElbowStrikeAttack();
            case "spinning_heel_kick" -> new CqcSpinningHeelKickAttack();
            case "knee" -> new CqcKneeAttack();
            case "axe_kick" -> new CqcAxeKickAttack();
            case "low_kick" -> new CqcLowKickAttack();
            case "superman_punch" -> new CqcSupermanPunchAttack();
            case "double_palm" -> new CqcDoublePalmAttack();
            case "backhand_slap" -> new CqcBackhandSlapAttack();
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

    private static MoveConfiguration configurationFor(String moveId) {
        return CONFIGS.get(CqcMoveCatalog.normalize(moveId));
    }

    public static Map<String, MoveConfiguration> configurations() {
        return CONFIGS;
    }

    private static MovesetBuilder buildMoveset() {
        MovesetBuilder builder = new MovesetBuilder()
                .withLeftClickMove(JAB)
                .withRightClickMove(CROSS)
                .withCrouchRightClickMove(LOW_KICK);

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
        register(configs, ROUNDHOUSE_FAST);
        register(configs, EYE_POKE);
        register(configs, THROAT_CHOP);
        register(configs, HEADKICK);
        register(configs, SPINNING_BACKFIST);
        register(configs, OVERHAND_RIGHT);
        register(configs, UPPERCUT);
        register(configs, KNEE_STRIKE);
        register(configs, ELBOW_STRIKE);
        register(configs, SPINNING_HEEL_KICK);
        register(configs, KNEE);
        register(configs, AXE_KICK);
        register(configs, LOW_KICK);
        register(configs, SUPERMAN_PUNCH);
        register(configs, DOUBLE_PALM);
        register(configs, BACKHAND_SLAP);
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

    private static boolean isOnCooldown(LivingEntity entity, MoveConfiguration config) {
        return getRemainingCooldown(entity, config) > 0;
    }

    private static int getRemainingCooldown(LivingEntity entity, MoveConfiguration config) {
        Map<String, Long> playerCooldowns = COOLDOWNS.get(entity.getUUID());
        if (playerCooldowns == null) return 0;
        long currentTime = entity.level().getGameTime();
        long endTime = playerCooldowns.getOrDefault(config.getMoveId(), 0L);
        int remaining = Math.max(0, (int) (endTime - currentTime));
        if (remaining == 0 && endTime > 0) {
            playerCooldowns.remove(config.getMoveId());
            if (playerCooldowns.isEmpty()) {
                COOLDOWNS.remove(entity.getUUID());
            }
        }
        return remaining;
    }

    private static void setCooldown(LivingEntity entity, MoveConfiguration config) {
        int cooldown = config.getCooldownOrDefault(0);
        if (cooldown <= 0) return;
        COOLDOWNS.computeIfAbsent(entity.getUUID(), id -> new HashMap<>())
                .put(config.getMoveId(), entity.level().getGameTime() + cooldown);
    }

    public static void resetCooldowns(Player player) {
        COOLDOWNS.remove(player.getUUID());
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
}
