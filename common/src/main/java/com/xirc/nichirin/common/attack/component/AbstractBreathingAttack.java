package com.xirc.nichirin.common.attack.component;

import com.xirc.nichirin.common.attack.moveset.AbstractMoveset.MoveConfiguration;
import com.xirc.nichirin.common.network.s2c.TriggerShaderPacket;
import com.xirc.nichirin.common.util.BreathingManager;
import com.xirc.nichirin.common.util.ComboIntegration;
import com.xirc.nichirin.common.util.NichirinArmorDamage;
import com.xirc.nichirin.common.util.NichirinDamageSources;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Base class for all breathing technique attacks.
 * Extends AbstractAttack for shared fields, hitbox geometry, interrupt logic, and ticking.
 * Adds breath cost management, a breathing-specific damage source, and impact-shake feedback.
 */
@Getter
@SuppressWarnings("rawtypes")
public abstract class AbstractBreathingAttack<T extends AbstractBreathingAttack, A extends IBreathingAttacker>
        extends AbstractAttack {

    protected float breathCost;
    private boolean breathConsumed = false;


    public void configure(MoveConfiguration config) {
        if (configured) return;
        configureCommon(config);
        if (config != null) this.breathCost = config.getBreathCostOrDefault(0f);
    }


    public void start(Player player, Level world) {
        if (!configured || duration <= 0) return;

        this.user = player;
        this.world = world;
        resetTickState();
        breathConsumed = false;

        if (breathCost > 0 && !BreathingManager.hasBreath(player, breathCost)) {
            player.displayClientMessage(
                    Component.literal("Not enough breath!")
                            .withStyle(s -> s.withColor(0xFF5555)), true);
            return;
        }

        if (breathCost > 0) {
            if (BreathingManager.consume(player, breathCost)) {
                breathConsumed = true;
            } else {
                player.displayClientMessage(
                        Component.literal("Failed to consume breath!")
                                .withStyle(s -> s.withColor(0xFF5555)), true);
                return;
            }
        }

        this.isActive = true;
        registerForTicking();

        try {
            onStart();
        } catch (Exception e) {
            e.printStackTrace();
            this.isActive = false;
            if (breathConsumed) { BreathingManager.restore(player, breathCost); breathConsumed = false; }
            return;
        }

        if (!isActive && breathConsumed) {
            BreathingManager.restore(player, breathCost);
            breathConsumed = false;
        }
    }

    /** NPC path; breath already consumed by caller. */
    public void start(LivingEntity entity, Level world) {
        if (entity instanceof Player player) { start(player, world); return; }
        if (!configured || duration <= 0) return;

        this.user = entity;
        this.world = world;
        resetTickState();
        breathConsumed = false;
        this.isActive = true;
        registerForTicking();

        try {
            onStart();
        } catch (Exception e) {
            e.printStackTrace();
            this.isActive = false;
        }
    }

    public void start(Player player) { start(player, player.level()); }

    public void start(A attacker) {
        Player player = attacker.getPlayer();
        start(player, player.level());
    }


    /** Legacy bridge; delegates to the no-arg tick() in the base. */
    public void tick(Player player) { tick(); }


    @Override
    protected void hitTarget(LivingEntity target) {
        if (world.isClientSide) return;
        if (getHitEntities().contains(target.getUUID())) return;

        DamageSource source = NichirinDamageSources.breathing(user);
        boolean damaged = NichirinArmorDamage.hurt(target, source, damage);

        if (damaged && user instanceof Player player) {
            ComboIntegration.handleSuccessfulHit(player, target, hitStun, damage);
        }

        applyHitStun(target);
        applyKnockback(target);

        getHitEntities().add(target.getUUID());
        setHitCount(getHitCount() + 1);

        if (damaged && user instanceof ServerPlayer sp) {
            float mag = Math.max(0.2f, Math.min(2.0f, (damage / 20.0f + hitStun / 20.0f) * 0.5f));
            NichirinPacketRegistry.sendToPlayer(
                    new TriggerShaderPacket("com.xirc.nichirin.client.shader.ImpactShakeShaderEffect", true, mag), sp);
        }
    }

    @Override
    protected void hitTargetNoImmunity(LivingEntity target) {
        if (world.isClientSide) return;
        target.invulnerableTime = 0;
        target.hurtTime = 0;

        DamageSource source = NichirinDamageSources.breathing(user);
        boolean damaged = NichirinArmorDamage.hurt(target, source, damage);

        if (damaged && user instanceof Player player) {
            ComboIntegration.handleSuccessfulHit(player, target, hitStun, damage);
        }

        // No-immunity variant always applies stunned (not slam)
        if (hitStun > 0) {
            target.invulnerableTime = hitStun;
            target.addEffect(new MobEffectInstance(
                    NichirinEffectRegistry.stunned(),
                    hitStun, 2, false, false, true));
        }
        applyKnockback(target);
        setHitCount(getHitCount() + 1);
    }


    @Override
    protected void registerForTicking() {
        if (user instanceof Player) super.registerForTicking();
    }


    protected void teleportSafe(Vec3 targetPos) {
        Vec3 eye = new Vec3(0, user.getBbHeight() / 2.0, 0);
        Vec3 from = user.position().add(eye);
        Vec3 to   = targetPos.add(eye);
        BlockHitResult hit = world.clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, user));
        Vec3 safePos;
        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3 dir = to.subtract(from);
            double len = dir.length();
            Vec3 norm = len > 0.001 ? dir.scale(1.0 / len) : Vec3.ZERO;
            safePos = hit.getLocation().subtract(norm.scale(0.15)).subtract(eye);
        } else {
            safePos = targetPos;
        }
        if (user instanceof ServerPlayer sp) sp.teleportTo(safePos.x, safePos.y, safePos.z);
        else user.absMoveTo(safePos.x, safePos.y, safePos.z, user.getYRot(), user.getXRot());
        user.setDeltaMovement(Vec3.ZERO);
    }

    protected void moveEntitySafe(LivingEntity entity, Vec3 targetPos) {
        Vec3 eye = new Vec3(0, entity.getBbHeight() / 2.0, 0);
        Vec3 from = entity.position().add(eye);
        Vec3 to   = targetPos.add(eye);
        BlockHitResult hit = world.clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        Vec3 safePos;
        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3 dir = to.subtract(from);
            double len = dir.length();
            Vec3 norm = len > 0.001 ? dir.scale(1.0 / len) : Vec3.ZERO;
            safePos = hit.getLocation().subtract(norm.scale(0.15)).subtract(eye);
        } else {
            safePos = targetPos;
        }
        entity.absMoveTo(safePos.x, safePos.y, safePos.z, entity.getYRot(), entity.getXRot());
        entity.setDeltaMovement(Vec3.ZERO);
        entity.hurtMarked = true;
    }


    public boolean wasBreathConsumed() { return breathConsumed; }


    /** @deprecated Use {@link AbstractAttack#tickAllActiveAttacks(MinecraftServer)} */
    public static void tickAllActiveAttacks(MinecraftServer server) {
        AbstractAttack.tickAllActiveAttacks(server);
    }

    public static void clearSelfTickingAttacks(Player player) {
        AbstractAttack.clearSelfTickingAttacks(player);
    }

    public static boolean cancelActiveAttack(Player player) {
        return AbstractAttack.cancelActiveAttack(player);
    }
}
