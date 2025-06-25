package com.xirc.nichirin.common.util.enums;

import com.xirc.nichirin.common.attack.component.IBreathingAttacker;
import net.minecraft.world.entity.LivingEntity;

public enum BreathingStyle {
    WATER("Water Breathing", 0x1E90FF) {
        @Override
        public float modifyBreathCost(float baseCost, int concentrationLevel) {
            // Water breathing is efficient, reduces cost
            return baseCost * (0.9f - concentrationLevel * 0.02f);
        }
    },
    FLAME("Flame Breathing", 0xFF4500) {
        @Override
        public float modifyBreathCost(float baseCost, int concentrationLevel) {
            // Flame breathing is intensive, slightly higher cost
            return baseCost * (1.1f - concentrationLevel * 0.01f);
        }
    },
    THUNDER("Thunder Breathing", 0xFFD700) {
        @Override
        public float modifyBreathCost(float baseCost, int concentrationLevel) {
            // Thunder breathing is burst-heavy
            return baseCost * (1.15f - concentrationLevel * 0.015f);
        }
    },
    WIND("Wind Breathing", 0x98FB98) {
        @Override
        public float modifyBreathCost(float baseCost, int concentrationLevel) {
            // Wind breathing is balanced
            return baseCost * (1.0f - concentrationLevel * 0.015f);
        }
    },
    STONE("Stone Breathing", 0x808080) {
        @Override
        public float modifyBreathCost(float baseCost, int concentrationLevel) {
            // Stone breathing is steady and efficient
            return baseCost * (0.85f - concentrationLevel * 0.02f);
        }
    },
    LOVE("Love Breathing", 0xFF69B4) {
        @Override
        public float modifyBreathCost(float baseCost, int concentrationLevel) {
            // Love breathing varies with emotion
            return baseCost * (1.05f - concentrationLevel * 0.015f);
        }
    },
    MIST("Mist Breathing", 0xE0E0E0) {
        @Override
        public float modifyBreathCost(float baseCost, int concentrationLevel) {
            // Mist breathing is deceptive, low cost
            return baseCost * (0.8f - concentrationLevel * 0.025f);
        }
    },
    INSECT("Insect Breathing", 0x9370DB) {
        @Override
        public float modifyBreathCost(float baseCost, int concentrationLevel) {
            // Insect breathing is precise
            return baseCost * (0.95f - concentrationLevel * 0.02f);
        }
    },
    SERPENT("Serpent Breathing", 0x8B008B) {
        @Override
        public float modifyBreathCost(float baseCost, int concentrationLevel) {
            // Serpent breathing flows continuously
            return baseCost * (0.9f - concentrationLevel * 0.018f);
        }
    },
    SOUND("Sound Breathing", 0x4B0082) {
        @Override
        public float modifyBreathCost(float baseCost, int concentrationLevel) {
            // Sound breathing resonates
            return baseCost * (1.0f - concentrationLevel * 0.02f);
        }
    },
    MOON("Moon Breathing", 0x191970) {
        @Override
        public float modifyBreathCost(float baseCost, int concentrationLevel) {
            // Moon breathing is powerful but costly
            return baseCost * (1.2f - concentrationLevel * 0.01f);
        }
    },
    SUN("Sun Breathing", 0xFFA500) {
        @Override
        public float modifyBreathCost(float baseCost, int concentrationLevel) {
            // Sun breathing is the most efficient
            return baseCost * (0.7f - concentrationLevel * 0.03f);
        }
    };

    private final String displayName;
    private final int color;

    BreathingStyle(String displayName, int color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getColor() {
        return color;
    }

    public float modifyDamage(float baseDamage, IBreathingAttacker<?, ?> attacker, LivingEntity target) {
        // Override for specific breathing style damage modifiers
        return baseDamage;
    }

    /**
     * Modifies the breath cost based on breathing style and concentration level
     * @param baseCost The base breath cost
     * @param concentrationLevel The user's concentration level
     * @return The modified breath cost
     */
    public abstract float modifyBreathCost(float baseCost, int concentrationLevel);
}