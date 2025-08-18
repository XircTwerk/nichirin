package com.xirc.nichirin.common.advancement;

import com.google.gson.JsonObject;
import com.xirc.nichirin.BreathOfNichirin;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Custom trigger for unlocking Sound Breathing
 */
public class SoundBreathingTrigger extends SimpleCriterionTrigger<SoundBreathingTrigger.TriggerInstance> {

    public static final SoundBreathingTrigger INSTANCE = new SoundBreathingTrigger();
    private static final ResourceLocation ID = BreathOfNichirin.id("sound_breathing_unlock");

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

        public static TriggerInstance soundBreathingUnlock() {
            return new TriggerInstance(ContextAwarePredicate.ANY);
        }

        @Override
        public @NotNull JsonObject serializeToJson(SerializationContext context) {
            return super.serializeToJson(context);
        }
    }
}