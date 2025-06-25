package com.xirc.nichirin.common.util;

import com.xirc.nichirin.common.attack.component.AbstractSimpleAttack;
import com.xirc.nichirin.common.attack.component.IPhysicalAttacker;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for building and managing attack chains
 */
@Getter
public class AttackChain<T extends AbstractSimpleAttack<T, A>, A extends IPhysicalAttacker<A, ?>> {

    private final List<ChainLink<T, A>> links = new ArrayList<>();

    /**
     * Adds a link to the chain
     */
    public AttackChain<T, A> addLink(T attack, ResourceLocation animation) {
        links.add(new ChainLink<>(attack, animation));
        return this;
    }

    /**
     * Builds the chain by setting up all followups
     */
    public T build() {
        if (links.isEmpty()) {
            throw new IllegalStateException("Cannot build empty chain");
        }

        // Set up followups
        for (int i = 0; i < links.size() - 1; i++) {
            ChainLink<T, A> current = links.get(i);
            ChainLink<T, A> next = links.get(i + 1);

            current.attack.withFollowup(next.attack, next.animation);
        }

        // Return the first attack in the chain
        return links.get(0).attack;
    }

    /**
     * Gets the total stamina cost of the full chain
     */
    public float getTotalStaminaCost() {
        return links.stream()
                .map(link -> link.attack.getStaminaCost())
                .reduce(0f, Float::sum);
    }

    /**
     * Gets the total duration of the full chain (if all attacks connect)
     */
    public int getTotalDuration() {
        return links.stream()
                .map(link -> link.attack.getTotalDuration())
                .reduce(0, Integer::sum);
    }

    /**
     * Represents a single link in an attack chain
     */
    @Getter
    private static class ChainLink<T extends AbstractSimpleAttack<T, A>, A extends IPhysicalAttacker<A, ?>> {
        private final T attack;
        private final ResourceLocation animation;

        public ChainLink(T attack, ResourceLocation animation) {
            this.attack = attack;
            this.animation = animation;
        }
    }
}