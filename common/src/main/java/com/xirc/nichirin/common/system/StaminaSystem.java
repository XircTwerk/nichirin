package com.xirc.nichirin.common.system;

import com.xirc.nichirin.common.util.enums.Gauge;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * Manages stamina for physical attacks and movement abilities
 */
@Getter
@Setter
public class StaminaSystem {

    private final Player player;
    private float stamina;
    private float maxStamina;
    private float regenRate;
    private int regenDelay; // Ticks before regen starts
    private int timeSinceUse;

    public StaminaSystem(Player player, float maxStamina, float regenRate, int regenDelay) {
        this.player = player;
        this.maxStamina = maxStamina;
        this.stamina = maxStamina;
        this.regenRate = regenRate;
        this.regenDelay = regenDelay;
        this.timeSinceUse = regenDelay;
    }

    /**
     * Ticks the stamina system, handling regeneration
     */
    public void tick() {
        timeSinceUse++;

        // Regenerate stamina after delay
        if (timeSinceUse >= regenDelay && stamina < maxStamina) {
            stamina = Math.min(maxStamina, stamina + regenRate);
        }
    }

    /**
     * Attempts to consume stamina
     * @return true if successful, false if insufficient stamina
     */
    public boolean consume(float amount) {
        if (stamina >= amount) {
            stamina -= amount;
            timeSinceUse = 0;
            return true;
        }
        return false;
    }

    /**
     * Checks if player has enough stamina
     */
    public boolean hasStamina(float amount) {
        return stamina >= amount;
    }

    /**
     * Gets stamina as a percentage (0-1)
     */
    public float getStaminaPercentage() {
        return stamina / maxStamina;
    }

    /**
     * Instantly restores stamina
     */
    public void restore(float amount) {
        stamina = Math.min(maxStamina, stamina + amount);
    }

    /**
     * Fully restores stamina
     */
    public void restoreFull() {
        stamina = maxStamina;
    }

    /**
     * Saves stamina data to NBT
     */
    public void save(CompoundTag tag) {
        tag.putFloat("Stamina", stamina);
        tag.putFloat("MaxStamina", maxStamina);
        tag.putFloat("RegenRate", regenRate);
        tag.putInt("RegenDelay", regenDelay);
        tag.putInt("TimeSinceUse", timeSinceUse);
    }

    /**
     * Loads stamina data from NBT
     */
    public void load(CompoundTag tag) {
        stamina = tag.getFloat("Stamina");
        maxStamina = tag.getFloat("MaxStamina");
        regenRate = tag.getFloat("RegenRate");
        regenDelay = tag.getInt("RegenDelay");
        timeSinceUse = tag.getInt("TimeSinceUse");
    }
}