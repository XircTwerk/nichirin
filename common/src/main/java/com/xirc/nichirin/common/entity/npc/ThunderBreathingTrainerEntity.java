package com.xirc.nichirin.common.entity.npc;

import com.xirc.nichirin.client.renderer.entity.dispatcher.ThunderBreathingTrainerDispatcher;
import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.moveset.breathing.ThunderBreathingMoveset;
import com.xirc.nichirin.common.entity.ai.ThunderBreathingAttackGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ThunderBreathingTrainerEntity extends BaseBreathingTrainerEntity {

    private static final float MAX_HP = 120.0f;

    public final ThunderBreathingTrainerDispatcher dispatcher;

    private UUID provokedByPlayer = null;
    private boolean waitingForFirstBlow = false;

    private String consumedAttackAnim = "";
    private int animCooldownTicks = 0;
    private Boolean lastWasWalking = null;
    private double prevAnimX = Double.NaN;
    private double prevAnimZ = Double.NaN;

    public ThunderBreathingTrainerEntity(EntityType<? extends ThunderBreathingTrainerEntity> type, Level level) {
        super(type, level, TrainerType.THUNDER);
        this.maxBreathGauge        = 200.0f;
        this.maxStamina            = 100.0f;
        this.breathRegenMultiplier = 2.5f;
        this.dispatcher            = new ThunderBreathingTrainerDispatcher(this);
    }

    @Override
    public float getScale() {
        return 0.8f;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return BaseBreathingTrainerEntity.createAttributes()
                .add(Attributes.MAX_HEALTH,      MAX_HP)
                .add(Attributes.MOVEMENT_SPEED,  0.38)
                .add(Attributes.ATTACK_DAMAGE,   9.0)
                .add(Attributes.ARMOR,           2.0)
                .add(Attributes.FOLLOW_RANGE,    40.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new ThunderBreathingAttackGoal(this, 1.4, true));
        goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 16.0f));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override public boolean canUse() {
                TrainerMode m = getMode();
                return (m == TrainerMode.DUELING || m == TrainerMode.SELF_DEFENSE) && super.canUse();
            }
        });
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false, null));
        targetSelector.addGoal(3, new ProvokedPlayerTargetGoal(this));
    }

    @Override
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor level, @NotNull DifficultyInstance difficulty,
                                        @NotNull MobSpawnType spawnType, SpawnGroupData data, CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, data, tag);
        moveset = new ThunderBreathingMoveset();
        setBreathGauge(maxBreathGauge);
        setStamina(maxStamina);
        return result;
    }

    @Override
    protected void equipArmor() {
        // Katana and armor are baked into the model.
    }

    @Override
    protected void dropEquipment() {
        // No drops.
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (result && source.getEntity() instanceof Player attacker) {
            if (getMode() == TrainerMode.PEACEFUL) {
                provokedByPlayer = attacker.getUUID();
            } else if (getMode() == TrainerMode.DUELING && waitingForFirstBlow
                    && getTarget() != null && attacker.getUUID().equals(getTarget().getUUID())) {
                waitingForFirstBlow = false;
            }
        }
        return result;
    }

    public UUID getProvokedByPlayer() { return provokedByPlayer; }

    public boolean isWaitingForFirstBlow() { return waitingForFirstBlow; }

    @Override
    public void startDuel(net.minecraft.server.level.ServerPlayer challenger, DuelDifficulty difficulty) {
        waitingForFirstBlow = true;
        super.startDuel(challenger, difficulty);
    }

    @Override
    protected net.minecraft.world.BossEvent.BossBarColor getBossBarColor() {
        return net.minecraft.world.BossEvent.BossBarColor.YELLOW;
    }

    @Override
    protected void endDuel(boolean playerWon) {
        provokedByPlayer = null;
        waitingForFirstBlow = false;
        super.endDuel(playerWon);
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            updateAnimations();
            return;
        }

        if (moveset == null) moveset = new ThunderBreathingMoveset();
        MoveExecutor.tickAttacks(this);
    }

    private void updateAnimations() {
        String serverAnim = getCurrentAnimation();

        if (!serverAnim.isEmpty() && (!serverAnim.equals(consumedAttackAnim) || wasAnimationReset())) {
            dispatcher.playAnimation(serverAnim);
            consumedAttackAnim = serverAnim;
            animCooldownTicks = getAnimDurationTicks(serverAnim);
            lastWasWalking = null;
        }

        if (animCooldownTicks > 0) {
            animCooldownTicks--;
            return;
        }

        if (serverAnim.isEmpty()) {
            consumedAttackAnim = "";
        }

        boolean walking;
        if (Double.isNaN(prevAnimX)) {
            walking = false;
        } else {
            double dx = getX() - prevAnimX;
            double dz = getZ() - prevAnimZ;
            walking = dx * dx + dz * dz > 0.0004;
        }
        prevAnimX = getX();
        prevAnimZ = getZ();

        if (!Boolean.valueOf(walking).equals(lastWasWalking)) {
            if (walking) dispatcher.walk();
            else dispatcher.idle();
            lastWasWalking = walking;
        }
    }

    private int getAnimDurationTicks(String animName) {
        return switch (animName) {
            case "thunderclap_flash"    -> 4;
            case "rice_spirit"          -> 12;
            case "thunder_swarm"        -> 18;
            case "distant_thunder"      -> 11;
            case "heat_lightning"       -> 14;
            case "rumble_flash"         -> 13;
            case "honoikazuchi_no_kami" -> 30;
            case "dash"                 -> 6;
            case "backstep"             -> 7;
            case "air_dodge"            -> 6;
            case "double_jump"          -> 10;
            default                     -> 16;
        };
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (provokedByPlayer != null) tag.putUUID("ProvokedBy", provokedByPlayer);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        provokedByPlayer = tag.hasUUID("ProvokedBy") ? tag.getUUID("ProvokedBy") : null;
        if (moveset == null) moveset = new ThunderBreathingMoveset();
        equipArmor();
    }

    private static class ProvokedPlayerTargetGoal extends NearestAttackableTargetGoal<Player> {
        private final ThunderBreathingTrainerEntity trainer;

        ProvokedPlayerTargetGoal(ThunderBreathingTrainerEntity trainer) {
            super(trainer, Player.class, 10, true, false, null);
            this.trainer = trainer;
        }

        @Override
        public boolean canUse() {
            if (trainer.provokedByPlayer == null) return false;
            Player target = trainer.level().getPlayerByUUID(trainer.provokedByPlayer);
            if (target == null || !target.isAlive()) {
                trainer.provokedByPlayer = null;
                return false;
            }
            trainer.setTarget(target);
            return true;
        }
    }
}
