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
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Akaza — Upper Moon Three. Fights hand-to-hand with the Destructive Death CQC kit and zones with
 * forward shockwaves. Below 40% HP he enters Overdrive: his lines and eyes glow red, his shockwaves
 * turn red, he gains Speed/Strength, and he switches to a heavier overdrive moveset.
 *
 * <p>Uses the general NPC moveset-switching support on {@link DemonNPCEntity}: two movesets are
 * loaded ({@link AkazaMoveset#ID} and {@link AkazaMoveset#ID_OVERDRIVE}) and swapped on enrage.</p>
 */
public class AkazaEntity extends UpperMoonDemonEntity implements IDestructiveDeathHost {

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

    // --- UpperMoonDemonEntity: state + Akaza's voice ---

    @Override
    public boolean isEnraged() {
        return isOverdrive();
    }

    @Override
    protected String titleText() {
        return isOverdrive() ? "Akaza 《Overdrive》" : "Akaza";
    }

    @Override
    protected int nameColor() {
        return isOverdrive() ? 0xFF3020 : 0x4A90E2; // blue normally, hot red in Overdrive
    }

    @Override
    protected String recruitLine(String playerName) {
        return "You're strong... Become a demon, " + playerName + "!";
    }

    @Override
    protected String reengageLine() {
        return "So you're back for my head, aren't you.";
    }

    @Override
    protected String[] engageBarks() {
        return new String[]{
                "Show me your strength!",
                "Don't disappoint me.",
                "A worthy fight at last.",
                "Come — let me see what you can do."
        };
    }

    @Override
    protected String[] overdriveBarks() {
        return new String[]{
                "Now THIS is a fight!",
                "Yes... get stronger!",
                "Don't hold back now!"
        };
    }

    @Override
    protected String[] fleeBarks() {
        return new String[]{
                "Running only makes you weaker!",
                "Cowardice disgusts me.",
                "Face me and grow stronger!"
        };
    }

    @Override
    protected String[] idleBarks() {
        return new String[]{
                "Still human, I see. Pitiful.",
                "Have you grown at all?",
                "When you crave strength, come find me."
        };
    }

    @Override
    protected String drinkPromptLine() {
        return "Good. Now drink my blood — become strong, and never wither.";
    }

    @Override
    protected String spareFarewellLine(String playerName) {
        return "Grow strong, " + playerName + ". I will come for you again... do not disappoint me.";
    }

    @Override
    protected String standoffKillLine() {
        return "Weak. You were never worth it.";
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
        this.goalSelector.addGoal(1, new StandoffGoal(this));
        this.goalSelector.addGoal(1, new WisteriaBlockAvoidanceGoal(this, 1.4));
        this.goalSelector.addGoal(2, new AkazaCombatGoal(this));
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
        // Won't initiate on players he's neutral to (spared, or fellow demons). HurtByTargetGoal
        // still lets him retaliate — that's how a spared player re-starts the fight.
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                entity -> entity instanceof Player p && !isNeutralTo(p)));
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
        } else if (!isInStandoff()) {
            // CQC attacks are ticked externally — drive our own here (tickAllAttacks only ticks players).
            // Frozen entirely during a mercy standoff.
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
        applyNameTag();      // restyle the tag red for the enraged phase
        barkOverdrive();
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
     * Akaza's combat brain — built to be the most aggressive, highest-fight-IQ NPC in the mod.
     *
     * <p>Fully owns movement and looking (it never yields to stroll/look goals while a target
     * lives), so Akaza is <em>always</em> doing something purposeful: charging, dash-closing,
     * air-chasing, footsie-strafing, or attacking. Decisions read the fight — anti-airing elevated
     * targets, whiff-punishing committed swings with a dashing palm, chaining pressure inside a
     * combo window, and finishing low targets in Overdrive. Cooldowns are short and Overdrive makes
     * everything faster.</p>
     */
    public static class AkazaCombatGoal extends Goal {

        private final AkazaEntity akaza;

        /** Ideal strike spacing — he tries to live right here and punish anything at this range. */
        private static final double POCKET = 2.2;

        private int globalCd;
        private int pokeCd, burstCd, dashPalmCd, zoneCd, signatureCd;
        private int gapDashCd, leapCd, pathCd;
        private int comboWindow;      // brief window after an attack where he chains faster
        private int strafeDir = 1;    // +1 / -1 — which way he's currently circling
        private int strafeFlipCd = 0; // ticks until he reverses the circle direction

        public AkazaCombatGoal(AkazaEntity akaza) {
            this.akaza = akaza;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (akaza.isInLethalSunlight() || akaza.isInStandoff()) return false;
            LivingEntity target = akaza.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            akaza.onEngage(akaza.getTarget()); // engage bark (once per target)
        }

        @Override
        public void stop() {
            akaza.getNavigation().stop();
            akaza.setSprinting(false);
        }

        @Override
        public void tick() {
            // The standoff owns his movement/behavior entirely — never strafe or attack during it.
            if (akaza.isInStandoff()) {
                akaza.getNavigation().stop();
                return;
            }

            LivingEntity target = akaza.getTarget();
            if (target == null || !target.isAlive()) return;

            tickCooldowns();

            akaza.getLookControl().setLookAt(target, 45.0F, 45.0F);
            faceTarget(target);

            if (akaza.hasEffect(NichirinEffectRegistry.stunned())) {
                akaza.getNavigation().stop();
                akaza.setSprinting(false);
                return;
            }

            double distance = Math.sqrt(akaza.distanceToSqr(target));
            boolean overdrive = akaza.isOverdrive();
            boolean attacking = MoveExecutor.hasActiveAttacks(akaza);

            // Taunt fleeing targets (base rate-limits the actual bark).
            if (distance > 10.0 && isTargetFleeing(target)) {
                akaza.onTargetFleeing();
            }

            // Movement runs every tick so he's never standing around — even mid-cooldown.
            handleMovement(target, distance, overdrive, attacking);

            // Don't stack a new attack over an active one; the CQC move drives its own motion.
            if (attacking || globalCd > 0 || akaza.getMoveset() == null) return;

            chooseAttack(target, distance, overdrive);
        }

        private void tickCooldowns() {
            if (globalCd > 0) globalCd--;
            if (pokeCd > 0) pokeCd--;
            if (burstCd > 0) burstCd--;
            if (dashPalmCd > 0) dashPalmCd--;
            if (zoneCd > 0) zoneCd--;
            if (signatureCd > 0) signatureCd--;
            if (gapDashCd > 0) gapDashCd--;
            if (leapCd > 0) leapCd--;
            if (pathCd > 0) pathCd--;
            if (comboWindow > 0) comboWindow--;
        }

        /**
         * Chase to close a real gap, then <em>circle</em> the target in fighting range — always
         * facing them (see {@link #faceTarget}). The orbit uses the movement controller's strafe so
         * it's smooth, not the twitchy dash spam from before.
         */
        private void handleMovement(LivingEntity target, double distance, boolean overdrive, boolean attacking) {
            if (attacking) return; // let the CQC dash play out

            Vec3 flat = new Vec3(target.getX() - akaza.getX(), 0, target.getZ() - akaza.getZ());
            Vec3 dir = flat.lengthSqr() > 1.0e-4 ? flat.normalize() : akaza.getLookAngle();

            if (distance > 5.0) {
                // Charge in. Recompute the path on a short timer (avoids per-tick path thrash).
                akaza.setSprinting(distance > 4.0);
                if (pathCd == 0 || akaza.getNavigation().isDone()) {
                    akaza.getNavigation().moveTo(target, overdrive ? 1.55 : 1.4);
                    pathCd = 5;
                }
                // Blink-dash only to eat a genuinely large gap.
                if (gapDashCd == 0 && distance > 6.5) {
                    EntityMovement.applyDash(akaza, dir);
                    gapDashCd = overdrive ? 24 : 36;
                }
                // Leap at clearly airborne / elevated targets so height never buys a reset.
                if (leapCd == 0 && akaza.onGround() && target.getY() > akaza.getY() + 2.0 && distance < 8.0) {
                    akaza.setDeltaMovement(dir.x * 0.3, 0.8, dir.z * 0.3);
                    akaza.hasImpulse = true;
                    leapCd = 50;
                }
            } else {
                // Fighting range — circle the target left/right, always facing them.
                akaza.setSprinting(false);
                akaza.getNavigation().stop();
                strafeOrbit(distance, overdrive);
            }
        }

        /** Smoothly circle the target, holding roughly the pocket range and reversing periodically. */
        private void strafeOrbit(double distance, boolean overdrive) {
            if (strafeFlipCd <= 0) {
                strafeDir = -strafeDir;
                strafeFlipCd = 40 + akaza.getRandom().nextInt(40);
            } else {
                strafeFlipCd--;
            }
            // Forward bias keeps him in range; lateral component is the circling.
            float forward = distance > POCKET + 0.5 ? 0.65f : (distance < POCKET - 0.3 ? -0.5f : 0.05f);
            float strafe = strafeDir * (overdrive ? 1.0f : 0.85f);
            akaza.getMoveControl().strafe(forward, strafe);
        }

        /** Situational attack selection — the "fight IQ". */
        private void chooseAttack(LivingEntity target, double distance, boolean overdrive) {
            boolean targetAbove = target.getY() > akaza.getY() + 1.6;
            boolean targetAirborne = !target.onGround();
            boolean targetSwinging = target.swinging || target.attackAnim > 0.01f;
            boolean targetLowHp = target.getHealth() < target.getMaxHealth() * 0.35f;
            // Only throw the shockwave volley when it can actually land (target not strafing across it).
            boolean canZone = !isTargetStrafingHard(target);

            // Anti-air: rising Crown Splitter punishes elevated / jumping targets.
            if ((targetAbove || (targetAirborne && distance <= 3.5)) && distance <= 4.0 && burstCd == 0) {
                fireBurst(overdrive);
                return;
            }

            // Whiff punish / gap close: they committed to a swing at range → dash palm straight in.
            if (targetSwinging && distance > 2.4 && distance <= 8.0 && dashPalmCd == 0) {
                fireDashPalm(overdrive);
                return;
            }

            if (distance <= 2.6) {
                // Point-blank pressure. Overdrive finisher on a low or already-pressured target.
                if (overdrive && signatureCd == 0 && (targetLowHp || comboWindow > 0)) {
                    fireSignature(overdrive);
                    return;
                }
                if (burstCd == 0 && (comboWindow > 0 || akaza.getRandom().nextFloat() < 0.5f)) {
                    fireBurst(overdrive);
                    return;
                }
                if (pokeCd == 0) {
                    firePoke(overdrive);
                    return;
                }
                if (burstCd == 0) {
                    fireBurst(overdrive);
                    return;
                }
            } else if (distance <= 5.5) {
                // Just outside melee — dash palm to close (movement + hit), else poke or zone.
                if (dashPalmCd == 0) {
                    fireDashPalm(overdrive);
                    return;
                }
                if (pokeCd == 0) {
                    firePoke(overdrive);
                    return;
                }
                if (zoneCd == 0 && canZone) {
                    fireZone(overdrive);
                    return;
                }
            } else if (distance <= 11.0) {
                // Mid — cover the approach with forward shockwaves when they'll land, else blitz in.
                if (zoneCd == 0 && canZone) {
                    fireZone(overdrive);
                    return;
                }
                if (dashPalmCd == 0) {
                    fireDashPalm(overdrive);
                    return;
                }
            } else {
                // Far — long zoning pressure when it lands; otherwise movement closes the gap.
                if (zoneCd == 0 && canZone) {
                    fireZone(overdrive);
                }
            }
        }

        private void firePoke(boolean overdrive) {
            akaza.getMoveset().handleLeftClick(akaza);
            pokeCd = overdrive ? 5 : 7;
            globalCd = overdrive ? 4 : 6;
            comboWindow = 24;
        }

        private void fireBurst(boolean overdrive) {
            akaza.getMoveset().performMove(akaza, AkazaMoveset.MOVE_BURST);
            burstCd = overdrive ? 16 : 24;
            globalCd = overdrive ? 6 : 9;
            comboWindow = 24;
        }

        private void fireDashPalm(boolean overdrive) {
            akaza.getMoveset().performMove(akaza, AkazaMoveset.MOVE_DASH);
            dashPalmCd = overdrive ? 22 : 34;
            globalCd = 8;
            comboWindow = 20;
        }

        private void fireZone(boolean overdrive) {
            akaza.getMoveset().performMove(akaza, AkazaMoveset.MOVE_ZONE);
            zoneCd = overdrive ? 34 : 50;
            globalCd = 8;
        }

        private void fireSignature(boolean overdrive) {
            akaza.getMoveset().performMove(akaza, AkazaMoveset.MOVE_SIGNATURE);
            signatureCd = overdrive ? 55 : 26;
            globalCd = 10;
            comboWindow = 20;
        }

        private void faceTarget(LivingEntity target) {
            double dx = target.getX() - akaza.getX();
            double dz = target.getZ() - akaza.getZ();
            // Aim pitch at the target too, so forward shockwaves track its height (accuracy).
            double dy = (target.getY() + target.getBbHeight() * 0.4) - (akaza.getY() + akaza.getEyeHeight());
            double horiz = Math.sqrt(dx * dx + dz * dz);
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float pitch = (float) (-Math.toDegrees(Math.atan2(dy, Math.max(0.01, horiz))));
            akaza.setYRot(yaw);
            akaza.yRotO = yaw;
            akaza.setYBodyRot(yaw);
            akaza.setYHeadRot(yaw);
            akaza.setXRot(pitch);
            akaza.xRotO = pitch;
        }

        private boolean isTargetFleeing(LivingEntity target) {
            Vec3 toTarget = target.position().subtract(akaza.position());
            Vec3 vel = target.getDeltaMovement();
            if (vel.lengthSqr() < 0.01) return false;
            return toTarget.normalize().dot(vel.normalize()) > 0.4; // moving away from Akaza
        }

        /**
         * True if the target is moving fast <em>across</em> Akaza's line of fire (strafing). Straight
         * shockwaves can't hit that, so he shouldn't waste the zoning volley on it.
         */
        private boolean isTargetStrafingHard(LivingEntity target) {
            Vec3 toT = new Vec3(target.getX() - akaza.getX(), 0, target.getZ() - akaza.getZ());
            if (toT.lengthSqr() < 1.0e-4) return false;
            toT = toT.normalize();
            Vec3 vel = new Vec3(target.getDeltaMovement().x, 0, target.getDeltaMovement().z);
            Vec3 perp = vel.subtract(toT.scale(vel.dot(toT))); // component across the aim line
            return perp.length() > 0.06;
        }
    }
}
