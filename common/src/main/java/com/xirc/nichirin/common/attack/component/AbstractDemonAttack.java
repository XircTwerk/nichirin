package com.xirc.nichirin.common.attack.component;

import com.xirc.nichirin.common.attack.moveset.AbstractMoveset.MoveConfiguration;
import com.xirc.nichirin.common.entity.npc.TempleDemonEntity;
import com.xirc.nichirin.common.util.ComboIntegration;
import com.xirc.nichirin.common.util.NichirinArmorDamage;
import com.xirc.nichirin.common.util.NichirinDamageSources;
import com.xirc.nichirin.common.system.DemonManager;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for all demon art attacks.
 * Extends AbstractAttack for shared fields, hitbox geometry, interrupt logic, and ticking.
 * Adds blood-gain mechanics and demon-specific damage source.
 */
@Getter
@SuppressWarnings("rawtypes")
public abstract class AbstractDemonAttack<T extends AbstractDemonAttack, A extends IDemonAttacker>
        extends AbstractAttack {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDemonAttack.class);

    // Blood restoration (3 hits = 1 blood)
    protected int hitsForBlood = 3;
    protected int maxBloodPerAttack = 3;
    protected int bloodOnKill = 3;

    // State
    protected float statMultiplier = 1.0f;
    protected boolean allowNonDemonUser = false;

    // Per-attack blood tracking
    private int hitCountForBlood = 0;
    private int bloodGainedThisAttack = 0;


    public void configure(MoveConfiguration config) {
        if (configured) return;
        configureCommon(config); // sets all common fields including teleport movement
    }


    public void applyStatMultiplier(float multiplier) {
        if (multiplier == 1.0f) return;
        this.statMultiplier *= multiplier;
        this.damage         *= multiplier;
        this.range          *= multiplier;
        this.knockback      *= multiplier;
        this.hitboxSize     *= multiplier;
        if (teleportDistance != null) teleportDistance *= multiplier;
        if (dashSpeed != null)        dashSpeed        *= multiplier;
    }

    public void applyDamageMultiplier(float multiplier) {
        if (multiplier == 1.0f) return;
        this.damage *= multiplier;
    }

    public void allowNonDemonUser() { this.allowNonDemonUser = true; }


    public void start(LivingEntity user, Level world) {
        if (!configured) {
            LOGGER.warn("Demon attack {} not configured before start()", getClass().getSimpleName());
            return;
        }
        if (duration <= 0) {
            LOGGER.warn("Demon attack {} has invalid duration: {}", getClass().getSimpleName(), duration);
            return;
        }

        this.user  = user;
        this.world = world;
        resetTickState();
        hitCountForBlood       = 0;
        bloodGainedThisAttack  = 0;

        this.isActive = true;
        registerForTicking();

        try {
            onStart();
        } catch (Exception e) {
            LOGGER.error("Error in demon attack {} onStart()", getClass().getSimpleName(), e);
            this.isActive = false;
        }
    }

    public void start(Player player, Level world) { start((LivingEntity) player, world); }
    public void start(Player player)              { start((LivingEntity) player, player.level()); }
    public void start(A attacker)                 { Player p = attacker.getPlayer(); start((LivingEntity) p, p.level()); }


    @Override
    protected boolean shouldContinueTick() {
        return allowNonDemonUser || !(user instanceof Player p) || DemonManager.isDemon(p);
    }


    @Override
    protected void hitTarget(LivingEntity target) {
        if (world.isClientSide) return;
        if (getHitEntities().contains(target.getUUID())) return;

        DamageSource source = damageSourceFor(user);
        boolean damaged = NichirinArmorDamage.hurt(target, source, damage);

        if (damaged) {
            if (user instanceof Player player)
                ComboIntegration.handleSuccessfulHit(player, target, hitStun, damage);
            handleBloodGain(target);
        }

        applyHitStun(target);
        applyKnockback(target);

        getHitEntities().add(target.getUUID());
        setHitCount(getHitCount() + 1);
    }

    @Override
    protected void hitTargetNoImmunity(LivingEntity target) {
        if (world.isClientSide) return;
        target.invulnerableTime = 0;
        target.hurtTime = 0;

        DamageSource source = damageSourceFor(user);
        boolean damaged = NichirinArmorDamage.hurt(target, source, damage);

        if (damaged) {
            if (user instanceof Player player)
                ComboIntegration.handleSuccessfulHit(player, target, hitStun, damage);
            handleBloodGain(target);
        }

        applyHitStun(target);
        applyKnockback(target);
        setHitCount(getHitCount() + 1);
    }


    private void handleBloodGain(LivingEntity target) {
        if (!(user instanceof Player player) || !DemonManager.isDemon(player)) return;

        boolean targetDied = !target.isAlive() || target.getHealth() <= 0;
        if (targetDied) {
            if (bloodOnKill > 0) {
                DemonManager.addBloodPoints(player, bloodOnKill);
                if (player instanceof ServerPlayer sp)
                    sp.displayClientMessage(
                            Component.literal("§c+" + bloodOnKill + " Blood (Kill)")
                                    .withStyle(s -> s.withColor(0xDC143C)), true);
            }
        } else if (bloodGainedThisAttack < maxBloodPerAttack) {
            hitCountForBlood++;
            if (hitCountForBlood >= hitsForBlood) {
                int gain = Math.min(1, maxBloodPerAttack - bloodGainedThisAttack);
                if (gain > 0) {
                    DemonManager.addBloodPoints(player, gain);
                    bloodGainedThisAttack += gain;
                    hitCountForBlood = 0;
                    if (player instanceof ServerPlayer sp)
                        sp.displayClientMessage(
                                Component.literal("§c+" + gain + " Blood")
                                        .withStyle(s -> s.withColor(0xDC143C)), true);
                }
            }
        }
    }

    private DamageSource damageSourceFor(LivingEntity attacker) {
        if (allowNonDemonUser)              return NichirinDamageSources.cqc(attacker);
        if (attacker instanceof TempleDemonEntity) return NichirinDamageSources.templeDemon(attacker);
        return NichirinDamageSources.demon(attacker);
    }


    public void setHitsForBlood(int v)       { this.hitsForBlood       = Math.max(1, v); }
    public void setMaxBloodPerAttack(int v)  { this.maxBloodPerAttack  = Math.max(0, v); }
    public void setBloodOnKill(int v)        { this.bloodOnKill        = Math.max(0, v); }


    /** @deprecated Use {@link AbstractAttack#tickAllActiveAttacks(MinecraftServer)} */
    public static void tickAllActiveAttacks(MinecraftServer server) {
        AbstractAttack.tickAllActiveAttacks(server);
    }

    public static void clearSelfTickingAttacks(LivingEntity entity) {
        AbstractAttack.clearSelfTickingAttacks(entity);
    }

    public static void clearSelfTickingAttacks(Player player) {
        AbstractAttack.clearSelfTickingAttacks(player);
    }
}
