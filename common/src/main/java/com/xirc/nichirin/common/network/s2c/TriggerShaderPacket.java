package com.xirc.nichirin.common.network.s2c;

import com.xirc.nichirin.client.shader.NichirinPostProcessor;
import com.xirc.nichirin.client.shader.NichirinShaderManager;
import net.minecraft.network.FriendlyByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * S2C packet to trigger shader effects on the client
 */
public class TriggerShaderPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(TriggerShaderPacket.class);

    private final String shaderEffectClass;
    private final boolean activate;
    /** For impact shake: precomputed magnitude (damage/stun formula). -1 = use default trigger(). */
    private final float magnitude;

    public TriggerShaderPacket(String shaderEffectClass, boolean activate) {
        this(shaderEffectClass, activate, -1f);
    }

    public TriggerShaderPacket(String shaderEffectClass, boolean activate, float magnitude) {
        this.shaderEffectClass = shaderEffectClass;
        this.activate = activate;
        this.magnitude = magnitude;
    }

    public TriggerShaderPacket(FriendlyByteBuf buf) {
        this.shaderEffectClass = buf.readUtf();
        this.activate = buf.readBoolean();
        this.magnitude = buf.readFloat();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(shaderEffectClass);
        buf.writeBoolean(activate);
        buf.writeFloat(magnitude);
    }

    public void handleClient() {
        try {
            // Get the shader effect class
            @SuppressWarnings("unchecked")
            Class<? extends NichirinPostProcessor> clazz =
                    (Class<? extends NichirinPostProcessor>) Class.forName(shaderEffectClass);

            // Get the shader from the manager
            var shader = NichirinShaderManager.getInstance().getProcessor(clazz);

            if (shader != null) {
                if (activate) {
                    LOGGER.debug("Activating shader: {} magnitude={}", shaderEffectClass, magnitude);
                    if (magnitude >= 0f) {
                        // Magnitude-based trigger (e.g. impact shake with damage/stun scaling)
                        try {
                            var triggerMethod = shader.getClass().getMethod("trigger", float.class);
                            triggerMethod.invoke(shader, magnitude);
                        } catch (NoSuchMethodException e) {
                            var triggerMethod = shader.getClass().getMethod("trigger");
                            triggerMethod.invoke(shader);
                        }
                    } else {
                        var triggerMethod = shader.getClass().getMethod("trigger");
                        triggerMethod.invoke(shader);
                    }
                } else {
                    LOGGER.debug("Deactivating shader: {}", shaderEffectClass);
                    shader.setActive(false);
                }
            } else {
                LOGGER.error("Shader not found: {}", shaderEffectClass);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to trigger shader: {}", e.getMessage(), e);
        }
    }
}