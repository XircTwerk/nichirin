package com.xirc.nichirin.common.attack.actions;

import com.xirc.nichirin.common.attack.component.IPhysicalAttacker;
import com.xirc.nichirin.common.util.AnimationUtils;
import net.minecraft.world.entity.player.Player;

/**
 * Action that plays a player animation when triggered
 */
public class PlayerAnimationAction {

    private final String animationName;
    private final boolean forcePlay;

    private PlayerAnimationAction(String animationName, boolean forcePlay) {
        this.animationName = animationName;
        this.forcePlay = forcePlay;
    }

    /**
     * Creates a new animation action
     */
    public static PlayerAnimationAction playAnim(String animationName) {
        return new PlayerAnimationAction(animationName, false);
    }

    /**
     * Forces this animation to play, interrupting any current animation
     */
    public PlayerAnimationAction forcePlay() {
        return new PlayerAnimationAction(animationName, true);
    }

    /**
     * Performs the animation action
     */
    public void perform(IPhysicalAttacker<?, ?> attacker) {
        Player player = attacker.getPlayer();

        if (player == null) {
            System.out.println("DEBUG: PlayerAnimationAction - player is null, skipping animation");
            return;
        }

        if (forcePlay || !AnimationUtils.isAnimationPlaying(player, animationName)) {
            System.out.println("DEBUG: PlayerAnimationAction - triggering animation: " + animationName);
            AnimationUtils.playAnimation(player, animationName);
        } else {
            System.out.println("DEBUG: PlayerAnimationAction - animation already playing: " + animationName);
        }
    }

    public String getAnimationName() {
        return animationName;
    }

    public boolean isForcePlay() {
        return forcePlay;
    }
}