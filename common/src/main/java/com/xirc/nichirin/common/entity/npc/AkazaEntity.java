package com.xirc.nichirin.common.entity.npc;

import com.xirc.nichirin.client.renderer.entity.dispatcher.AkazaDispatcher;
import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.moves.demon.destructive.IDestructiveDeathHost;
import com.xirc.nichirin.common.attack.moveset.demon.AkazaMoveset;
import com.xirc.nichirin.common.entity.npc.goal.WisteriaBlockAvoidanceGoal;
import com.xirc.nichirin.common.system.movement.EntityMovement;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import mod.azure.azurelib.common.util.MoveAnalysis;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Akaza — Upper Moon Three. Fights hand-to-hand with the Destructive Death CQC kit and zones with
 * forward shockwaves. Below 40% HP he enters Overdrive: his lines and eyes glow red, his shockwaves
 * turn red, he gains Speed/Strength, and he switches to a heavier overdrive moveset.
 *
 * <p>Uses the general NPC moveset-switching support on {@link DemonNPCEntity}: two movesets are
 * loaded ({@link AkazaMoveset#ID} and {@link AkazaMoveset#ID_OVERDRIVE}) and swapped on enrage.</p>
 */
public class AkazaEntity extends DemonNPCEntity implements IDestructiveDeathHost {

    private static final EntityDataAccessor<Boolean> OVERDRIVE =
            SynchedEntityData.defineId(AkazaEntity.class, EntityDataSerializers.BOOLEAN);

    /** Enrage threshold — enters Overdrive once health drops below this fraction of max. */
    private static final float OVERDRIVE_HP_FRACTION = 0.40f;

    public final AkazaDispatcher dispatcher;
    public final MoveAnalysis moveAnalysis;

    // Client-side animation state (mirrors TempleDemonEntity's approach)
    private String consumedAttackAnim = "";
    private int animCooldownTicks = 0;
    private Boolean lastWasWalking = null;

    // Server-side animation auto-clear countdown
    private int serverAnimTicksRemaining = 0;

    public AkazaEntity(EntityType<? extends DemonNPCEntity> entityType, Level level) {
        super(entityType, level);
        this.setDemonType("akaza");
        this.dispatcher = new AkazaDispatcher(this);
        this.moveAnalysis = new MoveAnalysis(this);

        // Two switchable phases: neutral kit (active) and the overdrive kit.
        this.addMoveset(new AkazaMoveset(false));
        this.addMoveset(new AkazaMoveset(true));

        this.maxBloodPoints = 20;
        this.maxBreathGauge = 200.0f;
        this.aggression = 1.0f;
        this.damageMultiplier = 1.25f;
        this.attackSpeedMultiplier = 1.15f;
        this.moveSpeedMultiplier = 1.15f;
        this.canRegenBlood = true;
        this.bloodRegenMultiplier = 1.5f;
        this.breathRegenMultiplier = 2.5f;
        this.setBloodPoints(maxBloodPoints);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OVERDRIVE, false);
    }

    @Override
    protected String getDefaultDemonType() {
        return "akaza";
    }

    // --- IDestructiveDeathHost: Akaza always zones; overdrive is his enrage phase ---

    @Override
    public boolean ddShockwaveEnabled() {
        return true;
    }

    @Override
    public boolean ddOverdriveActive() {
        return entityData.get(OVERDRIVE);
    }

    public boolean isOverdrive() {
        return entityData.get(OVERDRIVE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createDemonAttributes()
                .add(Attributes.MAX_HEALTH, 140.0)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.31)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new WisteriaBlockAvoidanceGoal(this, 1.4));
        this.goalSelector.addGoal(2, new AkazaAttackGoal(this, 1.25));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 40.0f));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canContinueToUse() {
                if (AkazaEntity.this.getTarget() != null && isDemonKin(AkazaEntity.this.getTarget())) {
                    AkazaEntity.this.setTarget(null);
                    return false;
                }
                return super.canContinueToUse();
            }
        });
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                entity -> entity instanceof Player p && !p.isCreative() && !p.isSpectator()));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                entity -> !isDemonKin(entity)));
    }

    private static boolean isDemonKin(Entity entity) {
        return entity instanceof DemonNPCEntity;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            moveAnalysis.update();
            updateAnimations();
        } else {
            // CQC attacks are ticked externally — drive our own here (tickAllAttacks only ticks players).
            MoveExecutor.tickAttacks(this);
            tickOverdrive();
        }
    }

    /**
     * Enrage into Overdrive at low HP, then keep the Overdrive buffs refreshed while it's active.
     */
    private void tickOverdrive() {
        boolean overdrive = entityData.get(OVERDRIVE);
        if (!overdrive) {
            if (getMaxHealth() > 0 && getHealth() < getMaxHealth() * OVERDRIVE_HP_FRACTION) {
                enterOverdrive();
            }
            return;
        }
        // Maintain the buffs (short refresh so they never lapse mid-fight).
        if (tickCount % 40 == 0) {
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, false, false, true));
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, false, false, true));
        }
    }

    private void enterOverdrive() {
        entityData.set(OVERDRIVE, true);
        switchMoveset(AkazaMoveset.ID_OVERDRIVE);
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, false, false, true));
        addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, false, false, true));
        level().playSound(null, getX(), getY(), getZ(),
                SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.4f, 0.7f);
    }

    private void updateAnimations() {
        String serverAnim = getCurrentAnimation();

        if (!serverAnim.isEmpty() && (!serverAnim.equals(consumedAttackAnim) || wasAnimationReset())) {
            dispatcher.playAnimation(serverAnim);
            consumedAttackAnim = serverAnim;
            animCooldownTicks = getMoveset() != null ? getMoveset().getAnimationDurationTicks(serverAnim, 16) : 16;
            lastWasWalking = false;
        }

        if (animCooldownTicks > 0) {
            animCooldownTicks--;
            return;
        }

        if (serverAnim.isEmpty()) {
            consumedAttackAnim = "";
        }

        boolean walking = moveAnalysis.isMovingHorizontally();
        if (!Boolean.valueOf(walking).equals(lastWasWalking)) {
            if (walking) {
                dispatcher.walk();
            } else {
                dispatcher.idle();
            }
            lastWasWalking = walking;
        }
    }

    @Override
    public void triggerMovesetAnimation(String animationName) {
        super.triggerMovesetAnimation(animationName);
        serverAnimTicksRemaining = (getMoveset() != null ? getMoveset().getAnimationDurationTicks(animationName, 16) : 16) + 4;
    }

    @Override
    protected void handleAnimationTick() {
        if (serverAnimTicksRemaining > 0) {
            serverAnimTicksRemaining--;
            if (serverAnimTicksRemaining == 0) {
                stopAnimation();
            }
        } else {
            stopAnimation();
        }
    }

    /**
     * Combat AI: Snap Punch and burst melee up close, forward-shockwave zoning at mid range, and
     * a dash palm (Annihilation Type) to close gaps. Attacks are fired straight from the moveset so
     * the CQC pipeline (and its shockwave spawning) runs exactly as it does for players.
     */
    public static class AkazaAttackGoal extends MeleeAttackGoal {

        private final AkazaEntity akaza;

        private int globalCd = 0;
        private int leftClickCd = 0;
        private int burstCd = 0;
        private int dashCd = 0;
        private int zoneCd = 0;
        private int signatureCd = 0;
        private int approachDashCd = 0;

        public AkazaAttackGoal(AkazaEntity akaza, double speedModifier) {
            super(akaza, speedModifier, true);
            this.akaza = akaza;
        }

        @Override
        public boolean canUse() {
            if (akaza.isInLethalSunlight()) return false;
            LivingEntity target = akaza.getTarget();
            return target != null && target.isAlive() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = akaza.getTarget();
            if (target == null || !target.isAlive() || akaza.isInLethalSunlight()) return false;
            return super.canContinueToUse();
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity target) {
            // No-op: attacks are driven from tick().
        }

        @Override
        public void tick() {
            super.tick();

            if (globalCd > 0) globalCd--;
            if (leftClickCd > 0) leftClickCd--;
            if (burstCd > 0) burstCd--;
            if (dashCd > 0) dashCd--;
            if (zoneCd > 0) zoneCd--;
            if (signatureCd > 0) signatureCd--;
            if (approachDashCd > 0) approachDashCd--;

            LivingEntity target = akaza.getTarget();
            if (target == null || !target.isAlive()) return;

            akaza.getLookControl().setLookAt(target, 30.0F, 30.0F);

            if (akaza.hasEffect(NichirinEffectRegistry.stunned())) {
                akaza.getNavigation().stop();
                return;
            }
            if (globalCd > 0 || akaza.getMoveset() == null) return;
            // Don't stack a new attack on top of one that's still winding up / active.
            if (MoveExecutor.hasActiveAttacks(akaza)) return;

            double distance = Math.sqrt(akaza.distanceToSqr(target));
            decideAttack(target, distance);
        }

        private void decideAttack(LivingEntity target, double distance) {
            boolean overdrive = akaza.isOverdrive();

            faceTarget(target);

            if (distance <= 3.2) {
                // In melee range — bread-and-butter poke plus bursts.
                if (overdrive && signatureCd == 0) { // Donut finisher
                    fireWheel(AkazaMoveset.MOVE_SIGNATURE);
                    signatureCd = 140;
                    return;
                }
                if (burstCd == 0) { // Crown Splitter
                    fireWheel(AkazaMoveset.MOVE_BURST);
                    burstCd = overdrive ? 30 : 45;
                    return;
                }
                if (leftClickCd == 0) { // Snap Punch
                    akaza.getMoveset().handleLeftClick(akaza);
                    leftClickCd = overdrive ? 8 : 12;
                    globalCd = 6;
                    return;
                }
            } else if (distance <= 7.0) {
                // Mid range — zone with forward shockwaves, or dash-palm in.
                if (zoneCd == 0) { // Explosive Flurry / Eight-Layered — travels forward
                    fireWheel(AkazaMoveset.MOVE_ZONE);
                    zoneCd = overdrive ? 55 : 70;
                    return;
                }
                if (dashCd == 0) { // Annihilation Type — dashing palm closes distance
                    fireWheel(AkazaMoveset.MOVE_DASH);
                    dashCd = 60;
                    return;
                }
                if (leftClickCd == 0) {
                    akaza.getMoveset().handleLeftClick(akaza);
                    leftClickCd = 12;
                    globalCd = 6;
                    return;
                }
            } else if (distance <= 16.0) {
                // Far — approach, but still zone occasionally so the target can't freely reset.
                if (zoneCd == 0 && distance <= 12.0) {
                    fireWheel(AkazaMoveset.MOVE_ZONE);
                    zoneCd = overdrive ? 55 : 80;
                    return;
                }
                if (approachDashCd == 0) {
                    Vec3 toTarget = target.position().subtract(akaza.position()).normalize();
                    EntityMovement.applyDash(akaza, toTarget);
                    approachDashCd = 22;
                    globalCd = 5;
                } else {
                    akaza.getNavigation().moveTo(target, 1.25);
                    globalCd = 3;
                }
            }
        }

        private void fireWheel(int index) {
            akaza.getMoveset().performMove(akaza, index);
            globalCd = 10;
        }

        private void faceTarget(LivingEntity target) {
            double dx = target.getX() - akaza.getX();
            double dz = target.getZ() - akaza.getZ();
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            akaza.setYRot(yaw);
            akaza.yRotO = yaw;
            akaza.setYBodyRot(yaw);
            akaza.setYHeadRot(yaw);
        }

        @Override
        protected int getAttackInterval() {
            return 20;
        }
    }
}
