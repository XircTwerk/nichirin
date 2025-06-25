package com.xirc.nichirin.common.attack.component;

import com.mojang.serialization.Codec;

public interface BreathingMoveType<M extends AbstractBreathingAttack<? extends M, ?>> {
    Codec<M> getCodec();
}