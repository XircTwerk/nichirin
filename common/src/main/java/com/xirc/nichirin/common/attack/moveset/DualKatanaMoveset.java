package com.xirc.nichirin.common.attack.moveset;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.component.AbstractAttack;
import com.xirc.nichirin.common.attack.moves.dual.DualKatanaComboAttack;
import com.xirc.nichirin.common.attack.moves.dual.DualKatanaCrouchHeavyAttack;
import com.xirc.nichirin.common.attack.moves.dual.DualKatanaM1Attack;
import com.xirc.nichirin.common.attack.moves.dual.DualKatanaM1FollowupAttack;
import com.xirc.nichirin.common.attack.moves.dual.DualKatanaSlamAttack;
import com.xirc.nichirin.common.attack.moves.dual.DualKatanaThrustAttack;
import com.xirc.nichirin.common.attack.moves.dual.DualKatanaXSlashAttack;
import com.xirc.nichirin.common.item.katana.Katana;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Neutral, component-driven moveset used while a player holds a katana in each hand. */
public final class DualKatanaMoveset extends AbstractMoveset {

    public static final String ID = "dual_katana";
    private static final int LIGHT_FOLLOWUP_TIMEOUT_TICKS = 20;
    private static final float SPECIAL_STAMINA_COST = 20.0f;

    private static final MoveConfiguration M1_CONFIG = new MoveBuilder("alternating_slash", "Alternating Slash")
            .withAnimation("nichirin:sword.dual_m1", 8)
            .withTiming(0, 1, 7)
            .withDamage(4.0f)
            .withRange(2.5f)
            .withKnockback(0.3f)
            .withHitboxSize(2.0f)
            .withHitStun(5)
            .withDescription("A fast right-hand cut that flows into a mirrored offhand followup.")
            .asLeftClick()
            .withAttack(DualKatanaM1Attack::new)
            .build();

    private static final MoveConfiguration M1_FOLLOWUP_CONFIG =
            new MoveBuilder("alternating_slash_followup", "Alternating Slash Followup")
                    .withAnimation("nichirin:sword.dual_m1_followup", 8)
                    .withTiming(0, 1, 7)
                    .withDamage(5.0f)
                    .withRange(2.5f)
                    .withKnockback(0.5f)
                    .withHitboxSize(2.0f)
                    .withHitStun(5)
                    .withDescription("The offhand blade reverses the opening cut to finish the light combo.")
                    .withAttack(DualKatanaM1FollowupAttack::new)
                    .build();

    private static final MoveConfiguration X_SLASH_CONFIG = new MoveBuilder("x_slash", "X Slash")
            .withAnimation("nichirin:sword.dual_xslash", 20)
            .withTiming(0, 5, 15)
            .withStaminaCost(SPECIAL_STAMINA_COST)
            .withDamage(3.5f)
            .withRange(2.8f)
            .withKnockback(0.4f)
            .withHitboxSize(2.0f)
            .withHitStun(7)
            .withDescription("Cross both blades through the target in a synchronized two-hit X.")
            .asRightClick()
            .withAttack(DualKatanaXSlashAttack::new)
            .build();

    private static final MoveConfiguration CROUCH_HEAVY_CONFIG =
            new MoveBuilder("crouching_heavy", "Crouching Heavy")
                    .withAnimation("nichirin:sword.dualcrouchheavy", 50)
                    .withTiming(0, 5, 45)
                    .withStaminaCost(SPECIAL_STAMINA_COST)
                    .withDamage(6.0f)
                    .withRange(2.5f)
                    .withKnockback(0.2f)
                    .withHitboxSize(2.0f)
                    .withHitStun(10)
                    .withDescription("Commit both blades to a heavy rising cut that launches the target.")
                    .asCrouchRightClick()
                    .withAttack(DualKatanaCrouchHeavyAttack::new)
                    .build();

    private static final MoveConfiguration COMBO_CONFIG = new MoveBuilder("combo", "Combo")
            .withAnimation("nichirin:sword.dual_combo", 40)
            .withTiming(40, 0, 40)
            .withStaminaCost(SPECIAL_STAMINA_COST)
            .withDamage(2.0f)
            .withRange(0.9f)
            .withKnockback(0)
            .withHitboxSize(2.0f)
            .withHitStun(20)
            .withDescription("Four close-range cuts. The final crossing strike deals double damage.")
            .withAttack(DualKatanaComboAttack::new)
            .build();

    private static final MoveConfiguration SLAM_CONFIG = new MoveBuilder("slam", "Slam")
            .withAnimation("nichirin:sword.dual_slam", 15)
            .withTiming(40, 6, 9)
            .withStaminaCost(SPECIAL_STAMINA_COST)
            .withDamage(10.0f)
            .withRange(2.8f)
            .withKnockback(1.0f)
            .withHitboxSize(2.0f)
            .withHitStun(10)
            .withDescription("Bring both blades down together, driving airborne targets into the ground.")
            .withAttack(DualKatanaSlamAttack::new)
            .build();

    private static final MoveConfiguration THRUST_CONFIG = new MoveBuilder("thrust", "Thrust")
            .withAnimation("nichirin:sword.dual_thrust", 27)
            .withTiming(100, 10, 17)
            .withStaminaCost(SPECIAL_STAMINA_COST)
            .withDamage(8.0f)
            .withRange(10.0f)
            .withKnockback(1.2f)
            .withHitboxSize(2.0f)
            .withHitStun(25)
            .withDashSpeed(4.2f)
            .withHyperArmor()
            .withDescription("Drive both points forward in a rushing pierce through every target in the path.")
            .withAttack(DualKatanaThrustAttack::new)
            .build();

    public static final DualKatanaMoveset INSTANCE = new DualKatanaMoveset();
    private static final Map<UUID, ComboState> comboStates = new ConcurrentHashMap<>();

    private static final class ComboState {
        long lastAttackTime;
        int comboCount;
    }

    public DualKatanaMoveset() {
        super(ID, "Dual Katana Arts", MovesetType.NEUTRAL, createBuilder());
    }

    private static MovesetBuilder createBuilder() {
        return new MovesetBuilder()
                .withMove(M1_CONFIG)
                .withMove(X_SLASH_CONFIG)
                .withMove(CROUCH_HEAVY_CONFIG)
                .withMove(COMBO_CONFIG)
                .withMove(SLAM_CONFIG)
                .withMove(THRUST_CONFIG);
    }

    public static boolean isDualWielding(LivingEntity entity) {
        return entity instanceof Player player
                && player.getMainHandItem().getItem() instanceof Katana
                && player.getOffhandItem().getItem() instanceof Katana;
    }

    public static AbstractMoveset neutralMovesetFor(Player player) {
        return isDualWielding(player) ? INSTANCE : DefaultKatanaMoveset.INSTANCE;
    }

    @Override
    public boolean canPerformMoves(LivingEntity entity) {
        return super.canPerformMoves(entity) && requireDualKatanas(entity);
    }

    @Override
    protected boolean canPerformLeftClick(LivingEntity entity) {
        return super.canPerformMoves(entity) && requireDualKatanas(entity);
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
        boolean followup = now - combo.lastAttackTime <= LIGHT_FOLLOWUP_TIMEOUT_TICKS && combo.comboCount == 1;
        MoveConfiguration config = followup ? M1_FOLLOWUP_CONFIG : M1_CONFIG;
        if (!hasResourcesForMove(entity, config)) return true;

        triggerAnimation(entity, config.getAnimationId().getPath());
        MoveExecutor.executeFactoryAttack(entity, config.getAttackFactory().get(), ID, config);
        combo.comboCount = followup ? 2 : 1;
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

    private static void resetCombo(LivingEntity entity) {
        ComboState combo = comboStates.get(entity.getUUID());
        if (combo != null) {
            combo.comboCount = 0;
            combo.lastAttackTime = 0;
        }
    }

    private static boolean hasActiveAttack(LivingEntity entity) {
        return MoveExecutor.hasActiveAttacks(entity) || AbstractAttack.hasActiveAttack(entity);
    }

    public static void tick(Player player) {
        ComboState combo = comboStates.get(player.getUUID());
        if (combo == null) return;
        long now = player.level().getGameTime();
        if (now - combo.lastAttackTime > LIGHT_FOLLOWUP_TIMEOUT_TICKS && combo.comboCount > 0) combo.comboCount = 0;
        if (now % 100 == 0) {
            comboStates.entrySet().removeIf(entry -> entry.getValue().comboCount == 0
                    && now - entry.getValue().lastAttackTime > LIGHT_FOLLOWUP_TIMEOUT_TICKS);
        }
    }

    public static int getComboCount(Player player) {
        ComboState combo = comboStates.get(player.getUUID());
        if (combo == null || player.level().getGameTime() - combo.lastAttackTime > LIGHT_FOLLOWUP_TIMEOUT_TICKS) return 0;
        return combo.comboCount;
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
}
