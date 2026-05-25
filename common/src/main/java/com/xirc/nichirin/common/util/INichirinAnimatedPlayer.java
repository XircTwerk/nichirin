package com.xirc.nichirin.common.util;

import com.xirc.nichirin.client.animation.NichirinPlayerModifierLayer;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;

public interface INichirinAnimatedPlayer {
    ModifierLayer<IAnimation> nichirin_getAnimLayer();
}
