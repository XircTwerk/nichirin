package com.xirc.nichirin.common.network.s2c;

import com.xirc.nichirin.client.shader.NichirinShaderManager;
import net.minecraft.network.FriendlyByteBuf;

/**
 * S2C packet to trigger shader effects on the client
 */
public class TriggerShaderPacket {

    private final String shaderEffectClass;
    private final boolean activate;

    public TriggerShaderPacket(String shaderEffectClass, boolean activate) {
        this.shaderEffectClass = shaderEffectClass;
        this.activate = activate;
    }

    public TriggerShaderPacket(FriendlyByteBuf buf) {
        this.shaderEffectClass = buf.readUtf();
        this.activate = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(shaderEffectClass);
        buf.writeBoolean(activate);
    }

    public void handleClient() {
        try {
            // Get the shader effect class
            @SuppressWarnings("unchecked")
            Class<? extends com.xirc.nichirin.client.shader.NichirinPostProcessor> clazz =
                    (Class<? extends com.xirc.nichirin.client.shader.NichirinPostProcessor>) Class.forName(shaderEffectClass);

            // Get the shader from the manager
            var shader = NichirinShaderManager.getInstance().getProcessor(clazz);

            if (shader != null) {
                if (activate) {
                    System.out.println("CLIENT: Activating shader: " + shaderEffectClass);
                    // Use reflection to call trigger() method
                    var triggerMethod = shader.getClass().getMethod("trigger");
                    triggerMethod.invoke(shader);
                } else {
                    System.out.println("CLIENT: Deactivating shader: " + shaderEffectClass);
                    shader.setActive(false);
                }
            } else {
                System.out.println("ERROR: Shader not found: " + shaderEffectClass);
            }
        } catch (Exception e) {
            System.err.println("ERROR: Failed to trigger shader: " + e.getMessage());
            e.printStackTrace();
        }
    }
}