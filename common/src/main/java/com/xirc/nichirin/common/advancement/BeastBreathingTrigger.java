package com.xirc.nichirin.common.advancement;

import com.google.gson.JsonObject;
import com.xirc.nichirin.BreathOfNichirin;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Custom trigger for unlocking Beast Breathing
 */
public class BeastBreathingTrigger extends SimpleCriterionTrigger<BeastBreathingTrigger.TriggerInstance> {

    public static final BeastBreathingTrigger INSTANCE = new BeastBreathingTrigger();
    private static final ResourceLocation ID = BreathOfNichirin.id("beast_breathing_unlock");

    @Override
    public @NotNull ResourceLocation getId() {
        return ID;
    }

    @Override
    protected @NotNull TriggerInstance createInstance(JsonObject json, ContextAwarePredicate predicate, DeserializationContext context) {
        return new TriggerInstance(predicate);
    }

    public void trigger(ServerPlayer player) {
        this.trigger(player, triggerInstance -> true);
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {

        public TriggerInstance(ContextAwarePredicate player) {
            super(ID, player);
        }

        public static TriggerInstance beastBreathingUnlock() {
            return new TriggerInstance(ContextAwarePredicate.ANY);
        }

        @Override
        public @NotNull JsonObject serializeToJson(SerializationContext context) {
            return super.serializeToJson(context);
        }
    }
}
