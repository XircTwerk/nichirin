package com.xirc.nichirin.common.attack.moveset;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.moves.*;
import com.xirc.nichirin.common.config.NichirinModConfig;
import com.xirc.nichirin.common.network.s2c.PlayerAnimationPacket;
import com.xirc.nichirin.common.util.ComboIntegration;
import com.xirc.nichirin.common.util.StaminaManager;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default katana moveset — used when the player holds a katana but has no breathing style.
 *
 * <p>Owns all attack state and logic so {@link com.xirc.nichirin.common.item.katana.SimpleKatana}
 * is reduced to a thin delegation layer.</p>
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

    /** Shared instance — client-side display + server-side attack routing. */
    public static final DefaultKatanaMoveset INSTANCE = new DefaultKatanaMoveset();

    private static final int  COMBO_WINDOW            = 20;   // ticks between hits to chain combo
    private static final float SPECIAL_STAMINA_COST   = 15.0f;

    // Per-player attack state (server-side)
    private static final Map<UUID, KatanaState> playerStates = new ConcurrentHashMap<>();

    // =========================================================================
    //  Constructor / builder
    // =========================================================================

    private DefaultKatanaMoveset() {
        super("default_katana", "Katana Arts", MovesetType.BREATHING, createBuilder());
    }

    private static MovesetBuilder createBuilder() {
        return new MovesetBuilder()
                // ── Left-click: slash combo ──────────────────────────────────
                .withLeftClickMove(new MoveBuilder("slash", "Slash")
                        .withDescription("Light slash — combo for a heavier follow-up.")
                        .withAction(entity -> { if (entity instanceof Player p) performLeftClick(p); })
                )

                // ── Right-click: double slash ────────────────────────────────
                .withRightClickMove(new MoveBuilder("double_slash", "Double Slash")
                        .withDescription("Powerful double slash. Costs stamina.")
                        .withAction(entity -> { if (entity instanceof Player p) performRightClick(p, false); })
                )

                // ── Crouch + right-click: rising slash ───────────────────────
                .withCrouchRightClickMove(new MoveBuilder("rising_slash", "Rising Slash")
                        .withDescription("Launches the target upward. Costs stamina.")
                        .withAction(entity -> { if (entity instanceof Player p) performRightClick(p, true); })
                )

                // ── Wheel move 0: Check ──────────────────────────────────────
                .withMove(new MoveBuilder("check", "Check")
                        .withDescription("Quick pommel strike. Stuns the target.")
                        .withTiming(0, 1, 4)
                        .withCooldown(30)
                        .withStaminaCost(SPECIAL_STAMINA_COST)
                        .withDamage(2.0f)
                        .withRange(2.0f)
                        .withKnockback(2.2f)
                        .withHitboxSize(1.2f)
                        .withAction(entity -> { if (entity instanceof Player p) performWheelMove(p, 0); })
                )

                // ── Wheel move 1: Overhead ───────────────────────────────────
                .withMove(new MoveBuilder("overhead", "Overhead")
                        .withDescription("Heavy downward slash. Slams airborne targets.")
                        .withTiming(4, 8, 10)
                        .withCooldown(40)
                        .withStaminaCost(SPECIAL_STAMINA_COST)
                        .withDamage(10.0f)
                        .withRange(2.8f)
                        .withKnockback(1.0f)
                        .withHitboxSize(1.5f)
                        .withAction(entity -> { if (entity instanceof Player p) performWheelMove(p, 1); })
                )

                // ── Wheel move 2: Thrust ─────────────────────────────────────
                .withMove(new MoveBuilder("thrust", "Thrust")
                        .withDescription("Forward dash attack. Great knockback.")
                        .withTiming(3, 10, 8)
                        .withCooldown(50)
                        .withStaminaCost(SPECIAL_STAMINA_COST)
                        .withDamage(8.0f)
                        .withRange(7.0f)
                        .withKnockback(1.2f)
                        .withHitboxSize(1.2f)
                        .withAction(entity -> { if (entity instanceof Player p) performWheelMove(p, 2); })
                );
    }

    // =========================================================================
    //  State management
    // =========================================================================

    public static KatanaState getOrCreateState(Player player) {
        return playerStates.computeIfAbsent(player.getUUID(), u -> new KatanaState());
    }

    /** Called every tick from {@code SimpleKatana.inventoryTick} while the katana is selected. */
    public static void tick(Player player) {
        KatanaState state = playerStates.get(player.getUUID());
        if (state == null) return;

        if (state.currentSlash        != null && state.currentSlash.isActive())       state.currentSlash.tick(player);
        if (state.currentDoubleSlash  != null && state.currentDoubleSlash.isActive()) state.currentDoubleSlash.tick(player);
        if (state.currentRisingSlash  != null && state.currentRisingSlash.isActive()) state.currentRisingSlash.tick(player);
        if (state.currentCheck        != null && state.currentCheck.isActive())        state.currentCheck.tick(player);
        if (state.currentOverhead     != null && state.currentOverhead.isActive())     state.currentOverhead.tick(player);
        if (state.currentThrust       != null && state.currentThrust.isActive())       state.currentThrust.tick(player);

        // Reset combo if the window expired
        long now = player.level().getGameTime();
        if (now - state.lastAttackTime > COMBO_WINDOW && state.comboCount > 0) {
            state.comboCount = 0;
        }

        // Periodically remove inactive state entries (keeps the map tidy)
        if (now % 100 == 0) {
            playerStates.entrySet().removeIf(e -> {
                KatanaState s = e.getValue();
                return (s.currentSlash       == null || !s.currentSlash.isActive())
                    && (s.currentDoubleSlash == null || !s.currentDoubleSlash.isActive())
                    && (s.currentRisingSlash == null || !s.currentRisingSlash.isActive())
                    && (s.currentCheck       == null || !s.currentCheck.isActive())
                    && (s.currentOverhead    == null || !s.currentOverhead.isActive())
                    && (s.currentThrust      == null || !s.currentThrust.isActive())
                    && s.comboCount == 0;
            });
        }
    }

    // =========================================================================
    //  Attack entry points (called by SimpleKatana)
    // =========================================================================

    /**
     * Left-click handler — slash combo.
     * Guard checks (stun, blocking, canPerformAttack) are done by the caller.
     */
    public static void performLeftClick(Player player) {
        if (player.level().isClientSide) return;

        KatanaState state = getOrCreateState(player);
        if (isAnyAttackActive(state)) return;

        long now = player.level().getGameTime();
        boolean isCombo = (now - state.lastAttackTime) <= COMBO_WINDOW && state.comboCount > 0;

        if (!isCombo && now < state.slash1CooldownUntil) return;
        if (isCombo  && now < state.slash2CooldownUntil) return;

        List<LivingEntity> targets = findTargetsInRange(player, 2.5f);

        if (isCombo && state.comboCount == 1) {
            state.currentSlash = createLightSlash2();
            state.currentSlash.start(player);
            state.comboCount = 2;
            state.slash2CooldownUntil = now + state.currentSlash.getCooldown();
            if (player instanceof ServerPlayer sp)
                NichirinPacketRegistry.broadcastPlayerAnimation(sp, new PlayerAnimationPacket(sp.getId(), "sword.slash"));
            for (LivingEntity t : targets) ComboIntegration.handleKatanaHit(player, t, 5, 5.0f);
        } else {
            state.currentSlash = createLightSlash1();
            state.currentSlash.start(player);
            state.comboCount = 1;
            state.slash1CooldownUntil = now + state.currentSlash.getCooldown();
            if (player instanceof ServerPlayer sp)
                NichirinPacketRegistry.broadcastPlayerAnimation(sp, new PlayerAnimationPacket(sp.getId(), "sword.slash"));
            for (LivingEntity t : targets) ComboIntegration.handleKatanaHit(player, t, 5, 4.0f);
        }

        state.lastAttackTime = now;
    }

    /**
     * Right-click handler — double slash (normal) or rising slash (crouching).
     * Guard checks (stun, blocking, canPerformAttack) are done by the caller.
     */
    public static void performRightClick(Player player, boolean isCrouching) {
        if (player.level().isClientSide) return;

        KatanaState state = getOrCreateState(player);
        if (isAnyAttackActive(state)) return;

        long now = player.level().getGameTime();
        float cost = NichirinModConfig.get().breathing.heavyAttackStaminaCost;

        if (!StaminaManager.hasStamina(player, cost)) {
            player.displayClientMessage(
                    Component.literal("Not enough stamina for special attack!")
                             .withStyle(s -> s.withColor(0xFF5555)), true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.5f, 0.5f);
            return;
        }

        if (isCrouching) {
            if (now < state.risingSlashCooldownUntil) return;
            if (!StaminaManager.consume(player, cost)) return;

            state.currentRisingSlash = createRisingSlashAttack();
            state.currentRisingSlash.start(player);
            state.risingSlashCooldownUntil = now + state.currentRisingSlash.getCooldown();
            if (player instanceof ServerPlayer sp)
                NichirinPacketRegistry.broadcastPlayerAnimation(sp, new PlayerAnimationPacket(sp.getId(), "sword.vertical"));
            for (LivingEntity t : findTargetsInRange(player, 2.5f))
                ComboIntegration.handleKatanaHit(player, t, 10, 4.0f);

        } else {
            if (now < state.doubleSlashCooldownUntil) return;
            if (!StaminaManager.consume(player, cost)) return;

            state.currentDoubleSlash = createDoubleSlashAttack();
            state.currentDoubleSlash.start(player);
            state.doubleSlashCooldownUntil = now + state.currentDoubleSlash.getCooldown();
            if (player instanceof ServerPlayer sp)
                NichirinPacketRegistry.broadcastPlayerAnimation(sp, new PlayerAnimationPacket(sp.getId(), "sword.doubleslash"));
            for (LivingEntity t : findTargetsInRange(player, 2.8f))
                ComboIntegration.handleKatanaHit(player, t, 7, 3.5f);
        }

        state.comboCount = 0;
        state.lastAttackTime = 0;
    }

    /**
     * Wheel move handler — Check (0), Overhead (1), Thrust (2).
     * Guard checks (stun, blocking) are done by the caller.
     */
    public static void performWheelMove(Player player, int moveIndex) {
        if (player.level().isClientSide) return;

        KatanaState state = getOrCreateState(player);
        if (isAnyAttackActive(state)) return;

        long now = player.level().getGameTime();
        if (!StaminaManager.hasStamina(player, SPECIAL_STAMINA_COST)) {
            player.displayClientMessage(
                    Component.literal("Not enough stamina!")
                             .withStyle(s -> s.withColor(0xFF5555)), true);
            return;
        }

        switch (moveIndex) {
            case 0 -> { // Check
                if (now < state.checkCooldownUntil) return;
                if (!StaminaManager.consume(player, SPECIAL_STAMINA_COST)) return;
                state.currentCheck = KatanaCheckAttack.createDefault();
                state.currentCheck.start(player);
                int checkCd = state.currentCheck.getCooldown();
                state.checkCooldownUntil = now + checkCd;
                MoveExecutor.sendCooldownDisplay(player, "Check", checkCd);
                if (player instanceof ServerPlayer sp)
                    NichirinPacketRegistry.broadcastPlayerAnimation(sp, new PlayerAnimationPacket(sp.getId(), "sword.slash"));
            }
            case 1 -> { // Overhead
                if (now < state.overheadCooldownUntil) return;
                if (!StaminaManager.consume(player, SPECIAL_STAMINA_COST)) return;
                state.currentOverhead = KatanaOverheadAttack.createDefault();
                state.currentOverhead.start(player);
                int overheadCd = state.currentOverhead.getCooldown();
                state.overheadCooldownUntil = now + overheadCd;
                MoveExecutor.sendCooldownDisplay(player, "Overhead", overheadCd);
                if (player instanceof ServerPlayer sp)
                    NichirinPacketRegistry.broadcastPlayerAnimation(sp, new PlayerAnimationPacket(sp.getId(), "sword.vertical"));
            }
            case 2 -> { // Thrust
                if (now < state.thrustCooldownUntil) return;
                if (!StaminaManager.consume(player, SPECIAL_STAMINA_COST)) return;
                state.currentThrust = KatanaThrustAttack.createDefault();
                state.currentThrust.start(player);
                int thrustCd = state.currentThrust.getCooldown();
                state.thrustCooldownUntil = now + thrustCd;
                MoveExecutor.sendCooldownDisplay(player, "Thrust", thrustCd);
                if (player instanceof ServerPlayer sp)
                    NichirinPacketRegistry.broadcastPlayerAnimation(sp, new PlayerAnimationPacket(sp.getId(), "sword.doubleslash"));
            }
        }
    }

    // =========================================================================
    //  Private helpers
    // =========================================================================

    private static boolean isAnyAttackActive(KatanaState s) {
        return (s.currentSlash       != null && s.currentSlash.isActive())
            || (s.currentDoubleSlash != null && s.currentDoubleSlash.isActive())
            || (s.currentRisingSlash != null && s.currentRisingSlash.isActive())
            || (s.currentCheck       != null && s.currentCheck.isActive())
            || (s.currentOverhead    != null && s.currentOverhead.isActive())
            || (s.currentThrust      != null && s.currentThrust.isActive());
    }

    private static List<LivingEntity> findTargetsInRange(Player player, float range) {
        AABB box = new AABB(
                player.getX() - range, player.getY() - range, player.getZ() - range,
                player.getX() + range, player.getY() + range, player.getZ() + range);
        return player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && !e.isSpectator());
    }

    // ── Attack factories ──────────────────────────────────────────────────────

    private static SimpleSlashAttack createLightSlash1() {
        return new SimpleSlashAttack.Builder()
                .withTiming(0, 7, 2).withCooldown(0).withDamage(4.0f).withRange(2.5f)
                .withKnockback(0.3f).withHitbox(1.5f, new net.minecraft.world.phys.Vec3(0, 0, 1.0))
                .withHitStun(5).withSounds(SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.PLAYER_ATTACK_STRONG)
                .build();
    }

    private static SimpleSlashAttack createLightSlash2() {
        return new SimpleSlashAttack.Builder()
                .withTiming(0, 10, 3).withCooldown(0).withDamage(5.0f).withRange(2.5f)
                .withKnockback(0.5f).withHitbox(1.5f, new net.minecraft.world.phys.Vec3(0, 0, 1.0))
                .withHitStun(5).withSounds(SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.PLAYER_ATTACK_STRONG)
                .build();
    }

    private static DoubleSlashAttack createDoubleSlashAttack() {
        return new DoubleSlashAttack.Builder()
                .withTiming(0, 16, 6).withCooldown(20).withDamage(3.5f).withRange(2.8f)
                .withKnockback(0.4f).withHitbox(1.6f, new net.minecraft.world.phys.Vec3(0, 0, 1.0))
                .withHitStun(7).withSlashDelay(2)
                .withSounds(SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.PLAYER_ATTACK_STRONG)
                .build();
    }

    private static RisingSlashAttack createRisingSlashAttack() {
        return new RisingSlashAttack.Builder()
                .withTiming(0, 10, 8).withCooldown(25).withDamage(4.0f).withRange(2.5f)
                .withLaunchPower(0.8f).withKnockback(0.2f)
                .withHitbox(1.5f, new net.minecraft.world.phys.Vec3(0, 0.5, 1.0))
                .withHitStun(10).withSounds(SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.PLAYER_ATTACK_CRIT)
                .build();
    }

    // =========================================================================
    //  Per-player state
    // =========================================================================

    public static class KatanaState {
        public long lastAttackTime = 0;
        public int  comboCount     = 0;

        public SimpleSlashAttack    currentSlash       = null;
        public DoubleSlashAttack    currentDoubleSlash = null;
        public RisingSlashAttack    currentRisingSlash = null;
        public KatanaCheckAttack    currentCheck       = null;
        public KatanaOverheadAttack currentOverhead    = null;
        public KatanaThrustAttack   currentThrust      = null;

        public long slash1CooldownUntil      = 0;
        public long slash2CooldownUntil      = 0;
        public long doubleSlashCooldownUntil = 0;
        public long risingSlashCooldownUntil = 0;
        public long checkCooldownUntil       = 0;
        public long overheadCooldownUntil    = 0;
        public long thrustCooldownUntil      = 0;
    }
}
