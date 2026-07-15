package com.xirc.nichirin.common.util;

import net.minecraft.resources.ResourceLocation;

/**
 * Vanilla derives sign sprite / GUI texture locations with
 * {@code ResourceLocation.withDefaultNamespace("<prefix>/" + woodType.name() + "<suffix>")}, which
 * assumes an un-namespaced wood-type name. A modded wood type name like {@code "nichirin:wisteria"}
 * leaves a ':' in the path and throws on validation. This rebuilds such a string as a proper
 * {@code <namespace>:<prefix>/<name><suffix>} location; un-namespaced names fall through unchanged.
 */
public final class SignTexturePaths {

    private SignTexturePaths() {}

    public static ResourceLocation namespaced(String path) {
        int slash = path.lastIndexOf('/');
        String file = slash >= 0 ? path.substring(slash + 1) : path;
        int colon = file.indexOf(':');
        if (colon >= 0) {
            String namespace = file.substring(0, colon);
            String prefix = slash >= 0 ? path.substring(0, slash + 1) : "";
            return ResourceLocation.fromNamespaceAndPath(namespace, prefix + file.substring(colon + 1));
        }
        return ResourceLocation.withDefaultNamespace(path);
    }
}
