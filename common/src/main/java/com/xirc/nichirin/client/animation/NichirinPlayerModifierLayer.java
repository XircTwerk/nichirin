package com.xirc.nichirin.client.animation;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.util.Vec3f;
import org.jetbrains.annotations.NotNull;

public class NichirinPlayerModifierLayer<T extends IAnimation> extends ModifierLayer<T> implements IAnimation {

    private static final FirstPersonConfiguration FIRST_PERSON_CONFIG = new FirstPersonConfiguration(true, true, true, true);

    @Override
    public @NotNull Vec3f get3DTransform(@NotNull String modelName, @NotNull TransformType type, float tickDelta, @NotNull Vec3f value0) {
        return super.get3DTransform(modelName, type, tickDelta, value0);
    }

    @Override
    public void setupAnim(float tickDelta) {
        super.setupAnim(tickDelta);
    }

    @Override
    public @NotNull FirstPersonMode getFirstPersonMode(float tickDelta) {
        return FirstPersonMode.THIRD_PERSON_MODEL;
    }

    @Override
    public @NotNull FirstPersonConfiguration getFirstPersonConfiguration(float tickDelta) {
        return FIRST_PERSON_CONFIG;
    }
}
