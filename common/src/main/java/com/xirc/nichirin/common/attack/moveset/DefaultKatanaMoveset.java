package com.xirc.nichirin.common.attack.moveset;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.moves.*;
import com.xirc.nichirin.common.config.NichirinModConfig;
import com.xirc.nichirin.common.util.StaminaManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default katana moveset — used when the player holds a katana but has no breathing style.
 *
 * <p>Routes all attacks through {@link AbstractMoveset}'s unified animation and
 * {@link MoveExecutor} systems.</p>
 *
 * <ul>
 *   <li>Left-click — Slash / Slash2 combo</li>
 *   <li>Right-click — Double Slash</li>
 *   <li>Crouch + Right-click — Rising Slash</li>
 *   <li>Wheel 0 — Check</li>
 *   <li>Wheel 1 — Overhead</li>
 *   <li>Wheel 2 — Thrust</li>
 * </ul>
 */
public class DefaultKatanaMoveset extends AbstractMoveset {

    public static final DefaultKatanaMoveset INSTANCE = new DefaultKatanaMoveset();

    private static final int COMBO_WINDOW = 20;
    private static final float SPECIAL_STAMINA_COST = 15.0f;

    private static final Map<UUID, ComboState> comboStates = new ConcurrentHashMap<>();

    // Stat sources for the click attacks (slash combo, double slash, rising slash).
    // Edit these to tune the attacks — the values here override the attack-class defaults.
    private static final MoveConfiguration SLASH1_CONFIG = new MoveBuilder("slash", "Slash")
            .withTiming(0, 0, 7).withDamage(4.0f).withRange(2.5f).withKnockback(0.3f).withHitboxSize(2.0f).withHitStun(5).build();
    private static final MoveConfiguration SLASH2_CONFIG = new MoveBuilder("slash", "Slash")
            .withTiming(0, 0, 10).withDamage(5.0f).withRange(2.5f).withKnockback(0.5f).withHitboxSize(2.0f).withHitStun(5).build();
    private static final MoveConfiguration DOUBLE_SLASH_CONFIG = new MoveBuilder("double_slash", "Double Slash")
            .withTiming(20, 0, 16).withDamage(3.5f).withRange(2.8f).withKnockback(0.4f).withHitboxSize(2.0f).withHitStun(7).build();
    private static final MoveConfiguration RISING_SLASH_CONFIG = new MoveBuilder("rising_slash", "Rising Slash")
            .withTiming(25, 0, 10).withDamage(4.0f).withRange(2.5f).withKnockback(0.2f).withHitboxSize(2.0f).withHitStun(10).build();

    private static class ComboState {
        long lastAttackTime = 0;
        int comboCount = 0;
    }

    private DefaultKatanaMoveset() {
        super("default_katana", "Katana Arts", MovesetType.NEUTRAL, createBuilder());
    }

    private static MovesetBuilder createBuilder() {
        return new MovesetBuilder()
                .withMove(new MoveBuilder("check", "Check")
                        .withAnimation("nichirin:sword.check", 6)
                        .withDescription("Shoulder bash with the katana handle. Close-range stun.")
                        .withTiming(30, 0, 1)
                        .withStaminaCost(SPECIAL_STAMINA_COST)
                        .withDamage(2.0f)
                        .withRange(0.9f)
                        .withKnockback(2.2f)
                        .withHitboxSize(2.0f)
                        .withAction(entity -> {
                            if (!(entity instanceof Player p)) return;
                            KatanaCheckAttack attack = KatanaCheckAttack.createDefault();
                            attack.configure(INSTANCE.getMove(0));
                            MoveExecutor.executeAttack(p, attack, "default_katana", "check");
                        })
                )

                .withMove(new MoveBuilder("overhead", "Overhead")
                        .withAnimation("nichirin:sword.vertical", 12)
                        .withDescription("Heavy downward slash. Slams airborne targets.")
                        .withTiming(40, 4, 8)
                        .withStaminaCost(SPECIAL_STAMINA_COST)
                        .withDamage(10.0f)
                        .withRange(2.8f)
                        .withKnockback(1.0f)
                        .withHitboxSize(2.0f)
                        .withAction(entity -> {
                            if (!(entity instanceof Player p)) return;
                            KatanaOverheadAttack attack = KatanaOverheadAttack.createDefault();
                            attack.configure(INSTANCE.getMove(1));
                            MoveExecutor.executeAttack(p, attack, "default_katana", "overhead");
                        })
                )

                .withMove(new MoveBuilder("thrust", "Thrust")
                        .withAnimation("nichirin:sword.thrust", 14)
                        .withDescription("Forward dash attack. Great knockback.")
                        .withTiming(50, 3, 10)
                        .withStaminaCost(SPECIAL_STAMINA_COST)
                        .withDamage(8.0f)
                        .withRange(7.0f)
                        .withKnockback(1.2f)
                        .withHitboxSize(2.0f)
                        .withAction(entity -> {
                            if (!(entity instanceof Player p)) return;
                            KatanaThrustAttack attack = KatanaThrustAttack.createDefault();
                            attack.configure(INSTANCE.getMove(2));
                            MoveExecutor.executeAttack(p, attack, "default_katana", "thrust");
                        })
                );
    }

    @Override
    public int getMoveCount() {
        return 3;
    }

    @Override
    public boolean handleLeftClick(LivingEntity entity) {
        if (entity.level().isClientSide) return true;
        if (MoveExecutor.hasActiveAttacks(entity)) return true;

        ComboState combo = comboStates.computeIfAbsent(entity.getUUID(), k -> new ComboState());
        long now = entity.level().getGameTime();
        boolean isCombo = (now - combo.lastAttackTime) <= COMBO_WINDOW && combo.comboCount > 0;

        if (isCombo && combo.comboCount == 1) {
            KatanaSlashAttack attack = KatanaSlashAttack.createSlash2();
            attack.configure(SLASH2_CONFIG);
            combo.comboCount = 2;
            triggerAnimation(entity, "sword.slash");
            MoveExecutor.executeAttack(entity, attack, "default_katana", "slash");
        } else {
            KatanaSlashAttack attack = KatanaSlashAttack.createSlash1();
            attack.configure(SLASH1_CONFIG);
            combo.comboCount = 1;
            triggerAnimation(entity, "sword.slash");
            MoveExecutor.executeAttack(entity, attack, "default_katana", "slash");
        }

        combo.lastAttackTime = now;
        return true;
    }

    @Override
    public boolean handleRightClick(LivingEntity entity, boolean isCrouching) {
        if (entity.level().isClientSide) return true;
        if (MoveExecutor.hasActiveAttacks(entity)) return true;
        if (!(entity instanceof Player player)) return false;

        float cost = NichirinModConfig.get().stamina.heavyAttackStaminaCost;

        if (!StaminaManager.hasStamina(player, cost)) {
            player.displayClientMessage(
                    Component.literal("Not enough stamina for special attack!")
                            .withStyle(s -> s.withColor(0xFF5555)), true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.5f, 0.5f);
            return true;
        }

        if (!StaminaManager.consume(player, cost)) return true;

        if (isCrouching) {
            KatanaRisingSlashAttack attack = KatanaRisingSlashAttack.createDefault();
            attack.configure(RISING_SLASH_CONFIG);
            triggerAnimation(entity, "sword.vertical");
            MoveExecutor.executeAttack(player, attack, "default_katana", "rising_slash");
        } else {
            KatanaDoubleSlashAttack attack = KatanaDoubleSlashAttack.createDefault();
            attack.configure(DOUBLE_SLASH_CONFIG);
            triggerAnimation(entity, "sword.doubleslash");
            MoveExecutor.executeAttack(player, attack, "default_katana", "double_slash");
        }

        ComboState combo = comboStates.get(entity.getUUID());
        if (combo != null) {
            combo.comboCount = 0;
            combo.lastAttackTime = 0;
        }

        return true;
    }

    @Override
    public void performMove(LivingEntity entity, int moveIndex) {
        if (entity.level().isClientSide) return;
        if (MoveExecutor.hasActiveAttacks(entity)) return;
        if (!(entity instanceof Player player)) return;

        if (!StaminaManager.hasStamina(player, SPECIAL_STAMINA_COST)) {
            player.displayClientMessage(
                    Component.literal("Not enough stamina!")
                            .withStyle(s -> s.withColor(0xFF5555)), true);
            return;
        }

        MoveConfiguration config = getMove(moveIndex);
        if (config != null && config.startAction != null) {
            if (!StaminaManager.consume(player, SPECIAL_STAMINA_COST)) return;

            if (config.animationId != null) {
                triggerAnimation(entity, config.animationId.getPath());
            }

            // Executing a wheel move breaks the slash-combo chain — pressing M1 after this
            // starts a fresh Slash, not Slash2.
            ComboState combo = comboStates.get(entity.getUUID());
            if (combo != null) {
                combo.comboCount = 0;
                combo.lastAttackTime = 0;
            }

            config.startAction.accept(entity);
        }
    }

    public static void tick(Player player) {
        ComboState combo = comboStates.get(player.getUUID());
        if (combo == null) return;

        long now = player.level().getGameTime();
        if (now - combo.lastAttackTime > COMBO_WINDOW && combo.comboCount > 0) {
            combo.comboCount = 0;
        }

        if (now % 100 == 0) {
            comboStates.entrySet().removeIf(e -> {
                ComboState s = e.getValue();
                return s.comboCount == 0 && now - s.lastAttackTime > COMBO_WINDOW;
            });
        }
    }

    public static KatanaState getOrCreateState(Player player) {
        ComboState combo = comboStates.computeIfAbsent(player.getUUID(), k -> new ComboState());
        KatanaState state = new KatanaState();
        state.lastAttackTime = combo.lastAttackTime;
        state.comboCount = combo.comboCount;
        return state;
    }

    public static void cleanupPlayer(Player player) {
        comboStates.remove(player.getUUID());
    }

    public static void clearAll() {
        comboStates.clear();
    }

    public static void interruptPlayerAttack(Player player) {
        MoveExecutor.clearAttacks(player);
        comboStates.remove(player.getUUID());
    }

    @Override
    public void onMovePerformed(LivingEntity entity, int moveIndex, boolean isCrouching) {}

    @Override
    public int getRightClickMoveIndex(boolean isCrouching) {
        return isCrouching ? -2 : -1;
    }

    @Override
    public String getRightClickMoveName() {
        return "Double Slash";
    }

    @Override
    public String getCrouchRightClickMoveName() {
        return "Rising Slash";
    }

    /**
     * Kept for backward compatibility with client-side HUD code.
     */
    public static class KatanaState {
        public long lastAttackTime = 0;
        public int comboCount = 0;
    }
}