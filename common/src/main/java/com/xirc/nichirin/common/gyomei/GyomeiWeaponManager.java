package com.xirc.nichirin.common.gyomei;

import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.attack.moveset.ChainBallAxeMoveset;
import com.xirc.nichirin.common.gyomei.GyomeiAttackController.Attack;
import com.xirc.nichirin.registry.NichirinMovesetRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-authoritative owner of each player's Gyomei weapon simulation. The sim runs on the server, so
 * whether the flail or axe hits something is decided by their REAL simulated positions — "the physics IS
 * the weapon." Ticked per player from {@code PlayerTickHandler}.
 *
 * <p>Damage is velocity-based and comes from the ends' actual swept motion: the heavy flail deals blunt
 * damage with big knockback, the axe deals higher cutting damage with little knockback.</p>
 */
public final class GyomeiWeaponManager {

    private static final Map<UUID, GyomeiWeaponSimulation> SIMS = new HashMap<>();
    private static final Map<UUID, Map<UUID, Long>> HIT_COOLDOWN = new HashMap<>();
    private static final Map<UUID, GyomeiAttackController> ATTACKS = new HashMap<>();
    /** true = flail stance (grip the middle of the chain); false = axe stance. Toggled by sheathing. */
    private static final Map<UUID, Boolean> FLAIL_MODE = new HashMap<>();
    /** true while the player holds crouch+M2 in axe stance to reel the ball in. */
    private static final Map<UUID, Boolean> REELING = new HashMap<>();
    private static final double REEL_MIN_SCALE = 0.35;

    private static final double FLAIL_HIT_SPEED = 0.30; // min blocks/tick for the end to "connect"
    private static final double AXE_HIT_SPEED = 0.22;
    private static final int HIT_COOLDOWN_TICKS = 10;

    private GyomeiWeaponManager() {}

    public static boolean isActive(UUID id) { return SIMS.containsKey(id); }
    public static GyomeiWeaponSimulation sim(UUID id) { return SIMS.get(id); }

    /** Client-toggled (debug/dev). Spawns or removes the server weapon for a player. */
    public static void toggle(ServerPlayer player) {
        UUID id = player.getUUID();
        if (SIMS.remove(id) != null) {
            HIT_COOLDOWN.remove(id);
            return;
        }
        GyomeiWeaponSimulation sim = new GyomeiWeaponSimulation(handAnchor(player), player.getLookAngle());
        sim.setCollider((from, to, radius) ->
                player.level() instanceof ServerLevel sl ? GyomeiCollision.resolveSwept(sl, from, to, radius) : to);
        SIMS.put(id, sim);
    }

    public static void remove(UUID id) {
        SIMS.remove(id);
        HIT_COOLDOWN.remove(id);
        ATTACKS.remove(id);
        FLAIL_MODE.remove(id);
        REELING.remove(id);
    }

    private static double approach(double v, double target, double step) {
        if (v < target) return Math.min(target, v + step);
        if (v > target) return Math.max(target, v - step);
        return v;
    }

    /** The ball-and-chain is two-handed — it may not sit in the offhand. Bump it out if it ends up there. */
    private static void evictFromOffhand(ServerPlayer player) {
        ItemStack off = player.getOffhandItem();
        if (off.isEmpty() || !(off.getItem() instanceof com.xirc.nichirin.common.item.gyomei.GyomeiWeapon)) return;
        if (player.getMainHandItem().isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, off.copy());
            player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        } else {
            player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            if (!player.getInventory().add(off)) {
                player.drop(off, false);
            }
        }
    }

    public static boolean isFlailMode(java.util.UUID id) { return FLAIL_MODE.getOrDefault(id, false); }

    /** Client → server: keep the stance (sheathe toggle) and reel-hold state in sync for the server sim. */
    public static void setState(ServerPlayer player, boolean flailMode, boolean reeling) {
        FLAIL_MODE.put(player.getUUID(), flailMode);
        REELING.put(player.getUUID(), reeling);
    }

    /** Resolve an input slot (0=M1, 1=M2, 2=crouch-M2, 3=crouch-M1) to an attack for the current stance. */
    public static Attack resolveAttack(int slot, boolean flailMode) {
        if (!flailMode) {
            return switch (slot) {
                case 0 -> Attack.AXE_HACK;
                case 1 -> Attack.LOB;
                default -> Attack.NONE; // crouch+M2 in axe stance is the reel-in hold, not a strike
            };
        }
        return switch (slot) {
            case 0 -> Attack.REAPING_SWEEP;
            case 1 -> Attack.METEOR_DROP;
            case 2 -> Attack.TWIN_CYCLONE;
            case 3 -> Attack.RISING_CRESCENT;
            default -> Attack.NONE;
        };
    }

    /** The ChainBallAxe move id backing each attack (also the PlayerAnimator animation name). */
    public static String moveIdFor(Attack attack) {
        return switch (attack) {
            case AXE_HACK -> "axe_hack";
            case LOB -> "flail_lob";
            case REAPING_SWEEP -> "reaping_sweep";
            case METEOR_DROP -> "meteor_drop";
            case RISING_CRESCENT -> "rising_crescent";
            case TWIN_CYCLONE -> "twin_cyclone";
            default -> "";
        };
    }

    /**
     * Server entry for an input slot. Resolves the attack for the player's stance and runs it THROUGH the
     * ChainBallAxe moveset, so cooldown gating, the HUD, and the PlayerAnimator animation all go through
     * the same unified path every other moveset uses. The move's own action calls {@link #fireMovesetAttack}
     * to drive the physics.
     */
    public static void triggerAttack(ServerPlayer player, int slot) {
        if (!SIMS.containsKey(player.getUUID())) return;
        Attack atk = resolveAttack(slot, isFlailMode(player.getUUID()));
        if (atk == Attack.NONE) return;
        AbstractMoveset moveset = NichirinMovesetRegistry.getMoveset(ChainBallAxeMoveset.ID);
        if (moveset == null) return;
        String moveId = moveIdFor(atk);
        for (int i = 0; i < moveset.getMoveCount(); i++) {
            AbstractMoveset.MoveConfiguration cfg = moveset.getMove(i);
            if (cfg != null && cfg.getMoveId().equals(moveId)) {
                moveset.performMove(player, i);
                return;
            }
        }
    }

    /**
     * Drives the physics attack and stamps the cooldown/HUD. Called from the ChainBallAxe move actions —
     * the moveset has already played the animation and gated the cooldown check.
     */
    public static void fireMovesetAttack(ServerPlayer player, Attack attack, int cooldown, String displayName) {
        ATTACKS.computeIfAbsent(player.getUUID(), k -> new GyomeiAttackController()).trigger(attack);
        com.xirc.nichirin.common.attack.ServerCooldownManager.set(player, moveIdFor(attack), cooldown);
        com.xirc.nichirin.common.attack.MoveExecutor.sendCooldownDisplay(player, displayName, cooldown);
    }

    public static void tick(ServerPlayer player) {
        evictFromOffhand(player);
        // Holding the item IS the activation — authoritative, no toggle needed.
        boolean holding = player.getMainHandItem().getItem() instanceof com.xirc.nichirin.common.item.gyomei.GyomeiWeapon;
        GyomeiWeaponSimulation sim = SIMS.get(player.getUUID());
        if (!holding) {
            if (sim != null) remove(player.getUUID());
            return;
        }
        if (sim == null) {
            sim = new GyomeiWeaponSimulation(handAnchor(player), player.getLookAngle());
            sim.setCollider((from, to, radius) ->
                    player.level() instanceof ServerLevel sl ? GyomeiCollision.resolveSwept(sl, from, to, radius) : to);
            SIMS.put(player.getUUID(), sim);
        }

        Vec3 hand = handAnchor(player);
        // Reel the ball toward the hand while crouch+M2 is held in the axe stance.
        boolean reeling = !isFlailMode(player.getUUID()) && REELING.getOrDefault(player.getUUID(), false);
        sim.lengthScale = approach(sim.lengthScale, reeling ? REEL_MIN_SCALE : 1.0, 0.06);

        GyomeiAttackController atk = ATTACKS.get(player.getUUID());
        // An active attack drives the grip along its curve; otherwise hold the axe (axe stance) or the
        // middle of the chain (flail stance, after sheathing).
        if (atk != null && atk.driveGrip(sim, hand, player.getLookAngle())) {
            sim.gripMode = GripMode.CHAIN;
        } else if (isFlailMode(player.getUUID())) {
            sim.gripMode = GripMode.CHAIN;
            sim.setGrip(sim.pointCount() / 2, hand);
        } else {
            sim.gripMode = GripMode.AXE;
            sim.setGrip(sim.axeIndex(), hand);
        }
        sim.step();

        // Damage from the ends' actual swept motion.
        hitWithEnd(player, sim.flail, sim.flail.radius, FLAIL_HIT_SPEED, 6.0f, 1.1f);
        hitWithEnd(player, sim.axe, sim.axe.radius, AXE_HIT_SPEED, 8.0f, 0.35f);
    }

    private static void hitWithEnd(ServerPlayer player, WeaponEnd end, double radius,
                                   double minSpeed, float baseDamage, float knockback) {
        if (end.speed() < minSpeed) return;
        Vec3 a = end.previousPosition, b = end.position;
        double pad = radius + 0.6;
        AABB search = new AABB(
                Math.min(a.x, b.x) - pad, Math.min(a.y, b.y) - pad, Math.min(a.z, b.z) - pad,
                Math.max(a.x, b.x) + pad, Math.max(a.y, b.y) + pad, Math.max(a.z, b.z) + pad);

        Map<UUID, Long> cooldown = HIT_COOLDOWN.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
        long now = player.level().getGameTime();

        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, search,
                e -> e != player && e.isAlive() && !e.isSpectator())) {
            double hitRadius = radius + target.getBbWidth() * 0.5;
            if (segmentDistanceSqr(a, b, targetCenter(target)) > hitRadius * hitRadius) continue;

            Long last = cooldown.get(target.getUUID());
            if (last != null && now - last < HIT_COOLDOWN_TICKS) continue;
            cooldown.put(target.getUUID(), now);

            float damage = (float) (baseDamage * Math.min(2.0, 0.6 + end.speed()));
            target.hurt(player.damageSources().playerAttack(player), damage);
            Vec3 dir = end.velocity().lengthSqr() > 1.0e-6 ? end.velocity().normalize() : b.subtract(a).normalize();
            target.knockback(knockback, -dir.x, -dir.z);
        }
    }

    private static Vec3 targetCenter(LivingEntity e) {
        return e.position().add(0, e.getBbHeight() * 0.5, 0);
    }

    /** Squared distance from a point to the segment a-b (swept collision). */
    private static double segmentDistanceSqr(Vec3 a, Vec3 b, Vec3 point) {
        Vec3 ab = b.subtract(a);
        double len2 = ab.lengthSqr();
        double t = len2 < 1.0e-9 ? 0.0 : Math.max(0.0, Math.min(1.0, point.subtract(a).dot(ab) / len2));
        return a.add(ab.scale(t)).distanceToSqr(point);
    }

    /**
     * Where the axe's chain-socket hangs in world space. Uses the player's BODY yaw (not head/look) and
     * ignores pitch entirely, so freely looking around does NOT whip the chain — it hangs off the body and
     * only swings when the player actually turns or moves. Shared by the client (render) and server
     * (damage) sims so the two stay in agreement.
     */
    public static Vec3 handAnchor(Player player) {
        double yaw = Math.toRadians(player.yBodyRot);
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        boolean rightHanded = player.getMainArm() == HumanoidArm.RIGHT;
        // Held near the hand but dropped toward the axe's pommel (the chain leaves the bottom of the
        // handle, which hangs below and forward of the grip), off the weapon-arm side of the body.
        return new Vec3(player.getX(), player.getEyeY() - 0.9, player.getZ())
                .add(forward.scale(0.45))
                .add(right.scale(rightHanded ? 0.4 : -0.4));
    }
}
