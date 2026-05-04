package com.xirc.nichirin.common.entity.npc;

import com.xirc.nichirin.client.renderer.entity.dispatcher.WaterBreathingTrainerDispatcher;
import com.xirc.nichirin.common.attack.moveset.breathing.WaterBreathingMoveset;
import com.xirc.nichirin.common.entity.ai.WaterBreathingAttackGoal;
import com.xirc.nichirin.registry.NichirinItemRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class WaterBreathingTrainerEntity extends BaseBreathingTrainerEntity {

    private static final float MAX_HP = 100.0f;

    public final WaterBreathingTrainerDispatcher dispatcher;

    private UUID provokedByPlayer = null;

    private String consumedAttackAnim = "";
    private int animCooldownTicks = 0;
    private Boolean lastWasWalking = null;

    public WaterBreathingTrainerEntity(EntityType<? extends WaterBreathingTrainerEntity> type, Level level) {
        super(type, level, TrainerType.WATER);
        this.maxBreathGauge        = 200.0f;
        this.maxStamina            = 100.0f;
        this.breathRegenMultiplier = 2.0f;
        this.dispatcher            = new WaterBreathingTrainerDispatcher(this);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return BaseBreathingTrainerEntity.createAttributes()
                .add(Attributes.MAX_HEALTH,      MAX_HP)
                .add(Attributes.MOVEMENT_SPEED,  0.32)
                .add(Attributes.ATTACK_DAMAGE,   8.0)
                .add(Attributes.ARMOR,           4.0)
                .add(Attributes.FOLLOW_RANGE,    40.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new WaterBreathingAttackGoal(this, 1.2, true));
        goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 16.0f));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false, null));
        targetSelector.addGoal(3, new ProvokedPlayerTargetGoal(this));
    }

    @Override
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor level, @NotNull DifficultyInstance difficulty,
                                        @NotNull MobSpawnType spawnType, SpawnGroupData data, CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, data, tag);
        moveset = new WaterBreathingMoveset();
        setBreathGauge(maxBreathGauge);
        setStamina(maxStamina);
        return result;
    }

    @Override
    protected void equipArmor() {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(NichirinItemRegistry.UROKODAKI_KATANA.get()));
        setDropChance(EquipmentSlot.MAINHAND, 0.0f);
    }

    @Override
    protected void dropEquipment() {
        super.dropEquipment();
        if (getRandom().nextFloat() < 0.25f) spawnAtLocation(new ItemStack(NichirinItemRegistry.UROKODAKI_HEADPIECE.get()));
        if (getRandom().nextFloat() < 0.25f) spawnAtLocation(new ItemStack(NichirinItemRegistry.UROKODAKI_CAPE.get()));
        if (getRandom().nextFloat() < 0.25f) spawnAtLocation(new ItemStack(NichirinItemRegistry.UROKODAKI_LEGGINGS.get()));
        if (getRandom().nextFloat() < 0.25f) spawnAtLocation(new ItemStack(NichirinItemRegistry.UROKODAKI_BOOTS.get()));
        if (getRandom().nextFloat() < 0.75f) spawnAtLocation(new ItemStack(NichirinItemRegistry.UROKODAKI_KATANA.get()));
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (result && source.getEntity() instanceof Player attacker
                && getMode() == TrainerMode.PEACEFUL) {
            provokedByPlayer = attacker.getUUID();
        }
        return result;
    }

    public UUID getProvokedByPlayer() { return provokedByPlayer; }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            updateAnimations();
            return;
        }

        if (moveset == null) moveset = new WaterBreathingMoveset();
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

        boolean walking = getDeltaMovement().horizontalDistanceSqr() > 0.001;
        if (!Boolean.valueOf(walking).equals(lastWasWalking)) {
            if (walking) dispatcher.walk();
            else dispatcher.idle();
            lastWasWalking = walking;
        }
    }

    private int getAnimDurationTicks(String animName) {
        return switch (animName) {
            case "water_surface_slash"  -> 14;
            case "water_wheel"          -> 22;
            case "flowing_dance"        -> 20;
            case "striking_tide"        -> 17;
            case "drop_ripple_thrust"   -> 12;
            case "constant_flux"        -> 30;
            case "dash"                 -> 8;
            case "backstep"             -> 7;
            case "air_dodge"            -> 6;
            case "double_jump"          -> 10;
            default                     -> 20;
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
        if (moveset == null) moveset = new WaterBreathingMoveset();
        equipArmor();
    }

    private static class ProvokedPlayerTargetGoal extends NearestAttackableTargetGoal<Player> {
        private final WaterBreathingTrainerEntity trainer;

        ProvokedPlayerTargetGoal(WaterBreathingTrainerEntity trainer) {
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
