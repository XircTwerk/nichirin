package com.xirc.nichirin.common.attack.component;

import com.xirc.nichirin.common.util.enums.AttackType;
import com.xirc.nichirin.common.util.enums.MoveClass;
import lombok.Getter;
import lombok.NonNull;
import net.minecraft.world.entity.player.Player;

/**
 * Adapter classes to make different attack types work with IKatanaAttack
 */
public class AttackAdapter {

    /**
     * Adapter for AbstractSimpleAttack
     */
    public static class SimpleAttackAdapter<T extends AbstractSimpleAttack<T, A>, A extends IPhysicalAttacker<A, ?>>
            implements IKatanaAttack {

        private final AbstractSimpleAttack<T, A> attack;
        @Getter
        private final A attackerInstance;

        public SimpleAttackAdapter(AbstractSimpleAttack<T, A> attack, A attackerInstance) {
            this.attack = attack;
            this.attackerInstance = attackerInstance;
        }

        @Override
        public AttackType getAttackType() {
            return AttackType.SIMPLE_ATTACK;
        }

        @Override
        public void start(Player player, Object attacker) {
            if (attacker instanceof IPhysicalAttacker) {
                @SuppressWarnings("unchecked")
                A typedAttacker = (A) attacker;
                attack.start(typedAttacker);
            }
        }
        @Override
        public void tick(Player player, Object attacker) {
            if (attacker instanceof IPhysicalAttacker) {
                attack.tick((Player) attacker);
            }
        }

        @Override
        public boolean isActive() {
            return attack.isActive();
        }

        @Override
        public boolean canStart(Player player, Object attacker) {
            if (attacker instanceof IPhysicalAttacker) {
                return attack.canStart((A) attacker);
            }
            return false;
        }

        @Override
        public void onRegister(MoveClass moveClass) {
            attack.onRegister(moveClass);
        }

        @Override
        public float getDamage() {
            return attack.getDamage();
        }

        @Override
        public float getRange() {
            return attack.getRange();
        }

        @Override
        public float getKnockback() {
            return attack.getKnockback();
        }

        public @NonNull AbstractSimpleAttack <T, A> getAttack() {
            return attack;
        }

    }

    /**
     * Adapter for AbstractBreathingAttack
     */
    @Getter
    public static class BreathingAttackAdapter<T extends AbstractBreathingAttack<T, A>, A extends IBreathingAttacker<A, ?>>
            implements IKatanaAttack {

        private final AbstractBreathingAttack<T, A> attack;

        public BreathingAttackAdapter(AbstractBreathingAttack<T, A> attack) {
            this.attack = attack;
        }

        @Override
        public AttackType getAttackType() {
            return AttackType.BREATHING_ATTACK;
        }

        @Override
        public void start(Player player, Object attacker) {
            if (attacker instanceof IBreathingAttacker) {
                attack.start((A) attacker);
            }
        }

        @Override
        public void tick(Player player, Object attacker) {
            if (attacker instanceof IBreathingAttacker) {
            }
        }

        @Override
        public boolean isActive() {
            return false;
        }

        @Override
        public boolean canStart(Player player, Object attacker) {
            // Implement based on your AbstractBreathingAttack logic
            return attacker instanceof IBreathingAttacker;
        }

        @Override
        public void onRegister(MoveClass moveClass) {
            attack.onRegister(moveClass);
        }

        @Override
        public float getDamage() {
            // Return damage from attack configuration
            return 0f;
        }

        @Override
        public float getRange() {
            // Return range from attack configuration
            return 0f;
        }

        @Override
        public float getKnockback() {
            // Return knockback from attack configuration
            return 0f;
        }

    }
}