package com.xirc.nichirin.common.item.katana;

import com.xirc.nichirin.common.attack.component.*;
import com.xirc.nichirin.common.attack.moves.BasicSlashAttack;
import com.xirc.nichirin.common.util.AnimationUtils;
import com.xirc.nichirin.common.util.enums.MoveInputType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.phys.Vec3;

/**
 * A simple katana with basic light attacks
 */
public class SimpleKatana extends AbstractKatanaItem {

    public SimpleKatana(Properties properties) {
        super(properties, createBuilder());
        System.out.println("DEBUG: SimpleKatana created with " + moves.size() + " moves");
    }

    private static KatanaBuilder createBuilder() {
        System.out.println("DEBUG: Creating katana builder");
        return new KatanaBuilder(Tiers.IRON) //matches the minecraft sword builder
                .withBaseAttackDamage(4.0f)
                .withAttackSpeed(-2.4f)
                .withEnchantability(14)
                .withDefaultAnimations(
                        new ResourceLocation("nichirin", "katana_idle")
                )
                // Basic light attack (left click)
                .withMove(MoveInputType.BASIC.getMoveClass(), new MoveBuilder(SimpleKatana::createLightSlash1)

                        .withDamage(4.0f)
                        .withHitbox(1.5f, new Vec3(0, 0, 1.0))
                        .withRange(2.5f)
                        .withTiming(20, 3, 16) // cooldown, windup, duration
                        .withKnockback(0.3f)
                        .withHitStun(15)
                        .withAnimation("light_slash_1") // Animation to play
                        .build()
                )
                // Second light attack for combo
                .withMove(MoveInputType.BASIC.getMoveClass(), new MoveBuilder(SimpleKatana::createLightSlash2)
                        .withDamage(5.0f)
                        .withHitbox(1.5f, new Vec3(0, 0, 1.0))
                        .withRange(2.5f)
                        .withTiming(15, 2, 18) // Faster startup for combo
                        .withKnockback(0.5f)
                        .withHitStun(20)
                        .withAnimation("light_slash_2")
                        .build()
                );
    }

    /**
     * Creates the first light slash attack wrapped as a breathing attack
     */
    private static <A extends IBreathingAttacker<A, ?>> AbstractBreathingAttack<?, A> createLightSlash1() {
        System.out.println("DEBUG: Creating light slash 1 attack");
        BasicSlashAttack<PlayerPhysicalAttacker> slashAttack = new BasicSlashAttack<PlayerPhysicalAttacker>()
                .withTiming(3, 13, 4)  // startup, active, recovery
                .withDamage(4.0f)
                .withRange(2.5f)
                .withKnockback(0.3f)
                .withHitbox(1.5f, new Vec3(0, 0, 1.0))
                .withHitStun(15)
                .withStaminaCost(10.0f)
                .withSounds(SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.PLAYER_ATTACK_STRONG);

        return new SimpleAttackBreathingWrapper<A>(slashAttack);
    }

    /**
     * Creates the second light slash attack for combos
     */
    private static <A extends IBreathingAttacker<A, ?>> AbstractBreathingAttack<?, A> createLightSlash2() {
        BasicSlashAttack<PlayerPhysicalAttacker> slashAttack = new BasicSlashAttack<PlayerPhysicalAttacker>()
                .withTiming(2, 14, 4)  // Faster startup
                .withDamage(5.0f)
                .withRange(2.5f)
                .withKnockback(0.5f)
                .withHitbox(1.5f, new Vec3(0, 0, 1.0))
                .withHitStun(20)
                .withStaminaCost(12.0f)
                .withSounds(SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.PLAYER_ATTACK_STRONG);

        return new SimpleAttackBreathingWrapper<A>(slashAttack);
    }

    @Override
    public void performMove(Player player, MoveInputType inputType, IBreathingAttacker<?, ?> attacker) {
        System.out.println("DEBUG: performMove called - inputType: " + inputType + ", player: " + player.getName().getString());

        MoveConfiguration config = getMoveConfig(inputType);
        if (config == null) {
            System.out.println("DEBUG: No config found for inputType: " + inputType);
            return;
        }

        if (config.breathingAttack == null) {
            System.out.println("DEBUG: No breathing attack in config");
            return;
        }

        if (config.breathingAttack.isActive()) {
            System.out.println("DEBUG: Attack is already active, skipping");
            return;
        }

        System.out.println("DEBUG: Starting attack with config");

        // Play the animation if configured
        if (config.animationId != null) {
            System.out.println("DEBUG: Playing animation: " + config.animationId);
            playAnimation(player, config.animationId, config.animationPriority);
        }

        // Start the breathing attack
        System.out.println("DEBUG: Starting breathing attack");
        config.breathingAttack.start(attacker);
        System.out.println("DEBUG: Attack started, isActive: " + config.breathingAttack.isActive());
    }

    @Override
    protected void playAnimation(Player player, ResourceLocation animationId, int priority) {
        System.out.println("DEBUG: playAnimation called - " + animationId + " with priority " + priority);
        AnimationUtils.playAnimation(player, animationId.getPath());
    }
}