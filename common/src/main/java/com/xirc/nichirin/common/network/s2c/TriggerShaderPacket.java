package com.xirc.nichirin.common.network.s2c;

import com.xirc.nichirin.client.handler.ImpactFrameOverlay;
import com.xirc.nichirin.client.handler.MistBlurOverlay;
import com.xirc.nichirin.client.gui.CompassNeedleHUD;
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
    /** Impact shake only: separate flash strength so flash can be tuned apart from shake. -1 = coupled. */
    private final float flashMagnitude;

    public TriggerShaderPacket(String shaderEffectClass, boolean activate) {
        this(shaderEffectClass, activate, -1f);
    }

    public TriggerShaderPacket(String shaderEffectClass, boolean activate, float magnitude) {
        this(shaderEffectClass, activate, magnitude, -1f);
    }

    public TriggerShaderPacket(String shaderEffectClass, boolean activate, float magnitude, float flashMagnitude) {
        this.shaderEffectClass = shaderEffectClass;
        this.activate = activate;
        this.magnitude = magnitude;
        this.flashMagnitude = flashMagnitude;
    }

    public TriggerShaderPacket(FriendlyByteBuf buf) {
        this.shaderEffectClass = buf.readUtf();
        this.activate = buf.readBoolean();
        this.magnitude = buf.readFloat();
        this.flashMagnitude = buf.readFloat();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(shaderEffectClass);
        buf.writeBoolean(activate);
        buf.writeFloat(magnitude);
        buf.writeFloat(flashMagnitude);
    }

    public void handleClient() {
        try {
            if ("com.xirc.nichirin.client.shader.MistBlurShaderEffect".equals(shaderEffectClass)) {
                if (activate) {
                    float strength = magnitude >= 0f ? magnitude : 1.0f;
                    MistBlurOverlay.trigger(strength);
                } else {
                    MistBlurOverlay.setTargetIntensity(0.0f);
                }
                return;
            }

            if ("com.xirc.nichirin.client.shader.ImpactShakeShaderEffect".equals(shaderEffectClass)) {
                if (activate) {
                    if (flashMagnitude >= 0f) {
                        // Decoupled: gun uses a fainter flash than the shake (and below the legacy floors).
                        ImpactFrameOverlay.trigger(flashMagnitude, magnitude >= 0f ? magnitude : 1.0f);
                    } else {
                        ImpactFrameOverlay.trigger(magnitude >= 0f ? magnitude : 1.0f);
                    }
                }
                return;
            }

            if ("com.xirc.nichirin.client.shader.CompassNeedleShaderEffect".equals(shaderEffectClass)) {
                if (activate) {
                    CompassNeedleHUD.activate(magnitude >= 0.0f ? magnitude : 6.0f);
                } else {
                    CompassNeedleHUD.clear();
                }
                return;
            }

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
