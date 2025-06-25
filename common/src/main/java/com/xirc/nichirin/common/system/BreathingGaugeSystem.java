package com.xirc.nichirin.common.system;

import com.xirc.nichirin.common.util.enums.Gauge;
import com.xirc.nichirin.common.util.enums.BreathingStyle;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * Manages breathing gauge for breathing techniques
 */
@Getter
@Setter
public class BreathingGaugeSystem {

    private final Player player;
    private float breathGauge;
    private float maxBreathGauge;
    private float regenRate;
    private int concentrationLevel; // Affects regen and max gauge
    private BreathingStyle breathingStyle;
    private boolean isConcentrating;
    private int timeSinceUse;

    public BreathingGaugeSystem(Player player, float maxBreathGauge, float regenRate) {
        this.player = player;
        this.maxBreathGauge = maxBreathGauge;
        this.breathGauge = maxBreathGauge;
        this.regenRate = regenRate;
        this.concentrationLevel = 0;
        this.breathingStyle = BreathingStyle.WATER; // Default
        this.isConcentrating = false;
        this.timeSinceUse = 0;
    }

    /**
     * Ticks the breathing system
     */
    public void tick() {
        timeSinceUse++;

        // Regenerate breath gauge
        if (breathGauge < maxBreathGauge) {
            float actualRegenRate = regenRate;

            // Concentration breathing increases regen
            if (isConcentrating) {
                actualRegenRate *= 2.0f;
            }

            // Higher concentration level = faster regen
            actualRegenRate *= (1.0f + concentrationLevel * 0.1f);

            breathGauge = Math.min(maxBreathGauge, breathGauge + actualRegenRate);
        }
    }

    /**
     * Attempts to consume breath gauge
     * @return true if successful, false if insufficient breath
     */
    public boolean consume(float amount) {
        // Breathing style may modify consumption
        float actualCost = breathingStyle.modifyBreathCost(amount, concentrationLevel);

        if (breathGauge >= actualCost) {
            breathGauge -= actualCost;
            timeSinceUse = 0;
            return true;
        }
        return false;
    }

    /**
     * Checks if player has enough breath
     */
    public boolean hasBreath(float amount) {
        float actualCost = breathingStyle.modifyBreathCost(amount, concentrationLevel);
        return breathGauge >= actualCost;
    }

    /**
     * Gets breath gauge as a percentage (0-1)
     */
    public float getBreathPercentage() {
        return breathGauge / maxBreathGauge;
    }

    /**
     * Toggles concentration breathing
     */
    public void toggleConcentration() {
        isConcentrating = !isConcentrating;
    }

    /**
     * Increases concentration level (through training/progression)
     */
    public void increaseConcentrationLevel() {
        concentrationLevel++;
        // Increase max breath gauge with concentration level
        maxBreathGauge = 100.0f + (concentrationLevel * 20.0f);
    }

    /**
     * Instantly restores breath gauge
     */
    public void restore(float amount) {
        breathGauge = Math.min(maxBreathGauge, breathGauge + amount);
    }

    /**
     * Fully restores breath gauge
     */
    public void restoreFull() {
        breathGauge = maxBreathGauge;
    }

    /**
     * Saves breathing data to NBT
     */
    public void save(CompoundTag tag) {
        tag.putFloat("BreathGauge", breathGauge);
        tag.putFloat("MaxBreathGauge", maxBreathGauge);
        tag.putFloat("BreathRegenRate", regenRate);
        tag.putInt("ConcentrationLevel", concentrationLevel);
        tag.putString("BreathingStyle", breathingStyle.name());
        tag.putBoolean("IsConcentrating", isConcentrating);
        tag.putInt("TimeSinceBreathUse", timeSinceUse);
    }

    /**
     * Loads breathing data from NBT
     */
    public void load(CompoundTag tag) {
        breathGauge = tag.getFloat("BreathGauge");
        maxBreathGauge = tag.getFloat("MaxBreathGauge");
        regenRate = tag.getFloat("BreathRegenRate");
        concentrationLevel = tag.getInt("ConcentrationLevel");
        breathingStyle = BreathingStyle.valueOf(tag.getString("BreathingStyle"));
        isConcentrating = tag.getBoolean("IsConcentrating");
        timeSinceUse = tag.getInt("TimeSinceBreathUse");
    }
}