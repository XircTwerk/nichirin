package com.xirc.nichirin.client.aura;

import com.xirc.nichirin.common.aura.AuraInstance;

import java.util.Random;
import java.util.UUID;

/** Immutable, deterministic per-aura shape data reused by every rendered frame. */
final class AuraWispLayout {
    final Wisp[] wisps;
    final Fragment[] fragments;

    private AuraWispLayout(Wisp[] wisps, Fragment[] fragments) {
        this.wisps = wisps;
        this.fragments = fragments;
    }

    static AuraWispLayout create(UUID entityId, AuraInstance aura) {
        long seed = entityId.getMostSignificantBits() ^ entityId.getLeastSignificantBits()
                ^ aura.id().getMostSignificantBits() ^ Long.rotateLeft(aura.id().getLeastSignificantBits(), 23);
        Random random = new Random(seed);
        int requested = Math.max(1, AuraConfig.wispCount);
        Wisp[] wisps = new Wisp[requested];
        int configuredTotal = Math.max(1, AuraConfig.outerWispCount
                + AuraConfig.middleWispCount + AuraConfig.innerWispCount);
        int outerEnd = Math.min(requested, Math.round(
                requested * Math.max(0, AuraConfig.outerWispCount) / (float) configuredTotal));
        int middleEnd = Math.min(requested, outerEnd + Math.round(
                requested * Math.max(0, AuraConfig.middleWispCount) / (float) configuredTotal));

        for (int i = 0; i < wisps.length; i++) {
            Layer layer = i < outerEnd ? Layer.OUTER : i < middleEnd ? Layer.MIDDLE : Layer.INNER;
            float layerHeight = switch (layer) {
                case OUTER -> lerp(random, 0.78f, 1.0f);
                case MIDDLE -> lerp(random, 0.58f, 0.88f);
                case INNER -> lerp(random, 0.32f, 0.62f);
            };
            float layerWidth = switch (layer) {
                case OUTER -> lerp(random, 0.72f, 1.0f);
                case MIDDLE -> lerp(random, 0.52f, 0.82f);
                case INNER -> lerp(random, 0.32f, 0.60f);
            };
            float layerRadius = switch (layer) {
                case OUTER -> lerp(random, 0.86f, 1.0f);
                case MIDDLE -> lerp(random, 0.68f, 0.90f);
                case INNER -> lerp(random, 0.48f, 0.72f);
            };
            float alpha = switch (layer) {
                case OUTER -> lerp(random, 0.24f, 0.36f);
                case MIDDLE -> lerp(random, 0.38f, 0.56f);
                case INNER -> lerp(random, 0.48f, 0.68f);
            };
            wisps[i] = new Wisp(
                    layer,
                    (i % 2 == 0 ? 0.0f : (float) Math.PI) + lerp(random, -0.34f, 0.34f),
                    lerp(random, AuraConfig.minimumRadius, AuraConfig.maximumRadius) * layerRadius,
                    lerp(random, AuraConfig.minimumHeight, AuraConfig.maximumHeight) * layerHeight,
                    lerp(random, AuraConfig.minimumWidth, AuraConfig.maximumWidth) * layerWidth,
                    lerp(random, 0.22f, layer == Layer.INNER ? 0.70f : 0.52f),
                    random.nextFloat(),
                    lerp(random, 0.72f, 1.38f),
                    lerp(random, -1.0f, 1.0f),
                    lerp(random, 0.55f, 1.35f),
                    alpha,
                    random.nextInt(3),
                    random.nextInt(Math.max(2, AuraConfig.verticalSegmentCount - 2)) + 1,
                    random.nextFloat() < 0.36f,
                    random.nextInt(5));
        }

        Fragment[] fragments = new Fragment[Math.max(0, AuraConfig.fragmentCount)];
        for (int i = 0; i < fragments.length; i++) {
            fragments[i] = new Fragment(
                    (float) (random.nextDouble() * Math.PI * 2.0),
                    lerp(random, AuraConfig.minimumRadius * 0.6f, AuraConfig.maximumRadius * 1.12f),
                    lerp(random, 0.10f, 0.94f),
                    random.nextFloat(),
                    lerp(random, AuraConfig.fragmentMinimumLifetime, AuraConfig.fragmentMaximumLifetime),
                    lerp(random, AuraConfig.fragmentMinimumSize, AuraConfig.fragmentMaximumSize),
                    lerp(random, -1.25f, 1.25f),
                    lerp(random, 0.08f, 0.32f),
                    lerp(random, 0.10f, 0.28f),
                    random.nextInt(5));
        }
        return new AuraWispLayout(wisps, fragments);
    }

    private static float lerp(Random random, float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    enum Layer { OUTER, MIDDLE, INNER }

    record Wisp(Layer layer, float angle, float radius, float height, float width,
                float startY, float phase, float speed, float bendDirection, float bendFrequency,
                float alpha, int facingMode, int gapSegment, boolean crossed, int topShape) {}

    record Fragment(float angle, float radius, float startY, float phase, float lifetime,
                    float size, float rotation, float outwardSpeed, float riseSpeed, int shape) {}
}
