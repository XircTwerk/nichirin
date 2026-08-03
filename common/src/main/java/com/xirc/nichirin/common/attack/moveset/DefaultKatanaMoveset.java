package com.xirc.nichirin.common.attack.moveset;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.component.AbstractAttack;
import com.xirc.nichirin.common.attack.moves.katana.KatanaCheckAttack;
import com.xirc.nichirin.common.attack.moves.katana.KatanaDoubleSlashAttack;
import com.xirc.nichirin.common.attack.moves.katana.KatanaOverheadAttack;
import com.xirc.nichirin.common.attack.moves.katana.KatanaRisingSlashAttack;
import com.xirc.nichirin.common.attack.moves.katana.KatanaSlashAttack;
import com.xirc.nichirin.common.attack.moves.katana.KatanaSlashFollowupAttack;
import com.xirc.nichirin.common.attack.moves.katana.KatanaStabFollowupAttack;
import com.xirc.nichirin.common.attack.moves.katana.KatanaThrustAttack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Component-driven neutral moveset used while holding one katana. */
public final class DefaultKatanaMoveset extends AbstractMoveset {

    public static final String ID = "default_katana";
    private static final int LIGHT_FOLLOWUP_TIMEOUT_TICKS = 20;
    private static final float SPECIAL_STAMINA_COST = 15.0f;

    private static final MoveConfiguration M1_CONFIG = new MoveBuilder("slash", "Slash")
            .withAnimation("nichirin:sword.slash", 8)
            .withTiming(0, 5, 2)
            .withDamage(4.0f)
            .withRange(2.5f)
            .withKnockback(0.3f)
            .withHitboxSize(2.0f)
            .withHitStun(5)
            .withDescription("A quick diagonal cut that starts a three-hit katana sequence.")
            .asLeftClick()
            .withAttack(KatanaSlashAttack::new)
            .build();

    private static final MoveConfiguration M1_FOLLOWUP_CONFIG =
            new MoveBuilder("slash_followup", "Reverse Slash")
                    .withAnimation("nichirin:sword.slash_followup", 8)
                    .withTiming(0, 5, 2)
                    .withDamage(5.0f)
                    .withRange(2.5f)
                    .withKnockback(0.35f)
                    .withHitboxSize(2.0f)
                    .withHitStun(7)
                    .withDescription("Reverse the blade through the target to continue the light combo.")
                    .withAttack(KatanaSlashFollowupAttack::new)
                    .build();

    private static final MoveConfiguration M1_FINAL_CONFIG =
            new MoveBuilder("slash_followup_2", "Piercing Finish")
                    .withAnimation("nichirin:sword.slash_followup_2", 9)
                    .withTiming(0, 4, 4)
                    .withDamage(6.0f)
                    .withRange(3.2f)
                    .withKnockback(0.15f)
                    .withHitboxSize(1.6f)
                    .withHitStun(20)
                    .withArmor(2)
                    .withDescription("An armored finishing stab with heavy hit stun.")
                    .withAttack(KatanaStabFollowupAttack::new)
                    .build();

    private static final MoveConfiguration DOUBLE_SLASH_CONFIG =
            new MoveBuilder("double_slash", "Double Slash")
                    .withAnimation("nichirin:sword.doubleslash", 10)
                    .withTiming(20, 1, 9)
                    .withStaminaCost(SPECIAL_STAMINA_COST)
                    .withDamage(3.5f)
                    .withRange(2.8f)
                    .withKnockback(0.4f)
                    .withHitboxSize(2.0f)
                    .withHitStun(7)
                    .withDescription("Two diagonal cuts that cross into an X.")
                    .asRightClick()
                    .withAttack(KatanaDoubleSlashAttack::new)
                    .build();

    private static final MoveConfiguration RISING_SLASH_CONFIG =
            new MoveBuilder("rising_slash", "Rising Slash")
                    .withAnimation("nichirin:sword.vertical", 9)
                    .withTiming(25, 4, 3)
                    .withStaminaCost(SPECIAL_STAMINA_COST)
                    .withDamage(4.0f)
                    .withRange(2.5f)
                    .withKnockback(0.2f)
                    .withHitboxSize(2.0f)
                    .withHitStun(10)
                    .withDescription("A rising cut that launches the target.")
                    .asCrouchRightClick()
                    .withAttack(KatanaRisingSlashAttack::new)
                    .build();

    private static final MoveConfiguration CHECK_CONFIG = new MoveBuilder("check", "Check")
            .withAnimation("nichirin:sword.check", 6)
            .withTiming(30, 3, 5)
            .withStaminaCost(SPECIAL_STAMINA_COST)
            .withDamage(2.0f)
            .withRange(0.9f)
            .withKnockback(2.2f)
            .withHitboxSize(2.0f)
            .withHitStun(20)
            .withDescription("Shoulder-check with the katana handle for close-range stun.")
            .withAttack(KatanaCheckAttack::new)
            .build();

    private static final MoveConfiguration OVERHEAD_CONFIG = new MoveBuilder("overhead", "Overhead")
            .withAnimation("nichirin:sword.overhead", 12)
            .withTiming(40, 6, 4)
            .withStaminaCost(SPECIAL_STAMINA_COST)
            .withDamage(10.0f)
            .withRange(2.8f)
            .withKnockback(1.0f)
            .withHitboxSize(2.0f)
            .withHitStun(10)
            .withDescription("A heavy downward slash that drives airborne targets into the ground.")
            .withAttack(KatanaOverheadAttack::new)
            .build();

    private static final MoveConfiguration THRUST_CONFIG = new MoveBuilder("thrust", "Thrust")
            .withAnimation("nichirin:sword.thrust", 14)
            .withTiming(50, 10, 8)
            .withStaminaCost(SPECIAL_STAMINA_COST)
            .withDamage(8.0f)
            .withRange(7.0f)
            .withKnockback(1.2f)
            .withHitboxSize(2.0f)
            .withHitStun(15)
            .withDashSpeed(3.0f)
            .withDescription("A delayed forward dash that pierces every target in its path.")
            .withAttack(KatanaThrustAttack::new)
            .build();

    public static final DefaultKatanaMoveset INSTANCE = new DefaultKatanaMoveset();
    private static final Map<UUID, ComboState> comboStates = new ConcurrentHashMap<>();

    private static final class ComboState {
        long lastAttackTime;
        int comboCount;
    }

    private DefaultKatanaMoveset() {
        super(ID, "Katana Arts", MovesetType.NEUTRAL, createBuilder());
    }

    private static MovesetBuilder createBuilder() {
        return new MovesetBuilder()
                .withMove(M1_CONFIG)
                .withMove(DOUBLE_SLASH_CONFIG)
                .withMove(RISING_SLASH_CONFIG)
                .withMove(CHECK_CONFIG)
                .withMove(OVERHEAD_CONFIG)
                .withMove(THRUST_CONFIG);
    }

    @Override
    public boolean canPerformMoves(LivingEntity entity) {
        return super.canPerformMoves(entity) && requireSingleKatana(entity);
    }

    @Override
    protected boolean canPerformLeftClick(LivingEntity entity) {
        return super.canPerformMoves(entity) && requireSingleKatana(entity);
    }

    @Override
    protected boolean shouldAutoStunClickMoves() {
        return false;
    }

    @Override
    public boolean handleLeftClick(LivingEntity entity) {
        if (entity.level().isClientSide) return true;
        if (!canPerformLeftClick(entity) || hasActiveAttack(entity)) return true;

        ComboState combo = comboStates.computeIfAbsent(entity.getUUID(), ignored -> new ComboState());
        long now = entity.level().getGameTime();
        boolean inSequence = now - combo.lastAttackTime <= LIGHT_FOLLOWUP_TIMEOUT_TICKS;
        int stage = inSequence ? combo.comboCount : 0;
        MoveConfiguration config = switch (stage) {
            case 1 -> M1_FOLLOWUP_CONFIG;
            case 2 -> M1_FINAL_CONFIG;
            default -> M1_CONFIG;
        };
        if (!hasResourcesForMove(entity, config)) return true;

        triggerAnimation(entity, config.getAnimationId().getPath());
        MoveExecutor.executeFactoryAttack(entity, config.getAttackFactory().get(), ID, config);
        combo.comboCount = stage >= 2 ? 0 : stage + 1;
        combo.lastAttackTime = now;
        return true;
    }

    @Override
    public boolean handleRightClick(LivingEntity entity, boolean isCrouching) {
        if (hasActiveAttack(entity)) return true;
        resetCombo(entity);
        return super.handleRightClick(entity, isCrouching);
    }

    @Override
    public void performMove(LivingEntity entity, int moveIndex) {
        if (hasActiveAttack(entity)) return;
        resetCombo(entity);
        super.performMove(entity, moveIndex);
    }

    private static boolean hasActiveAttack(LivingEntity entity) {
        return MoveExecutor.hasActiveAttacks(entity) || AbstractAttack.hasActiveAttack(entity);
    }

    private static void resetCombo(LivingEntity entity) {
        ComboState combo = comboStates.get(entity.getUUID());
        if (combo != null) {
            combo.comboCount = 0;
            combo.lastAttackTime = 0;
        }
    }

    public static void tick(Player player) {
        ComboState combo = comboStates.get(player.getUUID());
        if (combo == null) return;
        long now = player.level().getGameTime();
        if (now - combo.lastAttackTime > LIGHT_FOLLOWUP_TIMEOUT_TICKS && combo.comboCount > 0) {
            combo.comboCount = 0;
        }
        if (now % 100 == 0) {
            comboStates.entrySet().removeIf(entry -> entry.getValue().comboCount == 0
                    && now - entry.getValue().lastAttackTime > LIGHT_FOLLOWUP_TIMEOUT_TICKS);
        }
    }

    public static KatanaState getOrCreateState(Player player) {
        ComboState combo = comboStates.computeIfAbsent(player.getUUID(), ignored -> new ComboState());
        KatanaState state = new KatanaState();
        state.lastAttackTime = combo.lastAttackTime;
        state.comboCount = combo.comboCount;
        return state;
    }

    public static void cleanupPlayer(Player player) { comboStates.remove(player.getUUID()); }
    public static void clearAll() { comboStates.clear(); }

    public static void interruptPlayerAttack(Player player) {
        MoveExecutor.clearAttacks(player);
        AbstractAttack.clearSelfTickingAttacks(player);
        comboStates.remove(player.getUUID());
    }

    @Override public void onMovePerformed(LivingEntity entity, int moveIndex, boolean isCrouching) {}
    @Override public int getRightClickMoveIndex(boolean isCrouching) { return isCrouching ? -2 : -1; }
    @Override public MoveConfiguration getLeftClickConfiguration() { return M1_CONFIG; }
    @Override public MoveConfiguration getRightClickConfiguration() { return DOUBLE_SLASH_CONFIG; }
    @Override public MoveConfiguration getCrouchRightClickConfiguration() { return RISING_SLASH_CONFIG; }
    @Override public String getLeftClickMoveName() { return M1_CONFIG.getDisplayName(); }
    @Override public String getRightClickMoveName() { return DOUBLE_SLASH_CONFIG.getDisplayName(); }
    @Override public String getCrouchRightClickMoveName() { return RISING_SLASH_CONFIG.getDisplayName(); }

    public static class KatanaState {
        public long lastAttackTime;
        public int comboCount;
    }
}
