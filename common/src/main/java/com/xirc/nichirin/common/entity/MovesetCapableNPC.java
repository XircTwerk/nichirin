package com.xirc.nichirin.common.entity;

import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Set;
import java.util.UUID;

public interface MovesetCapableNPC {


    AbstractMoveset getMoveset();
    void setMoveset(AbstractMoveset moveset);

    UUID getEntityUUID();
    Level getEntityLevel();
    LivingEntity asLivingEntity();

    boolean canUseMove(int moveIndex);
    void performMovesetMove(int moveIndex);
    void triggerMovesetAnimation(String animationName);


    int getBloodPoints();
    void setBloodPoints(int bloodPoints);
    int getMaxBloodPoints();

    boolean canRegenBlood();
    float getBloodRegenMultiplier();


    float getBreathGauge();
    void setBreathGauge(float breath);
    float getMaxBreathGauge();
    float getBreathRegenMultiplier();


    float getStamina();
    void setStamina(float stamina);
    float getMaxStamina();
    float getStaminaRegenMultiplier();

    default boolean hasStamina(float amount) {
        return getStamina() >= amount;
    }

    default boolean consumeStamina(float amount) {
        float current = getStamina();
        if (current < amount) return false;
        setStamina(current - amount);
        return true;
    }


    boolean canDoubleJump();
    void markDoubleJumped();
    void resetDoubleJump();

    boolean isSprinting();
    void setSprinting(boolean sprinting);


    float getAggression();
    float getDamageMultiplier();
    float getAttackSpeedMultiplier();
    float getMoveSpeedMultiplier();


    Set<Integer> getBlacklistedMoves();

    default boolean isMoveBlacklisted(int moveIndex) {
        return getBlacklistedMoves().contains(moveIndex);
    }


    void tickMovesetSystems();
}
