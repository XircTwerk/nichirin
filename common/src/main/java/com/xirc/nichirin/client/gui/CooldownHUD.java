package com.xirc.nichirin.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.data.MovesetData;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.common.util.enums.BreathingStyle;
import com.xirc.nichirin.registry.NichirinMovesetRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class CooldownHUD {

    private static final Map<String, CooldownEntry> cooldowns = new HashMap<>();
    private static final List<Map.Entry<String, CooldownEntry>> ACTIVE_COOLDOWNS = new ArrayList<>();
    private static final Comparator<Map.Entry<String, CooldownEntry>> REMAINING_ASCENDING =
            Comparator.comparingLong(entry -> entry.getValue().endTime);

    private static final int DISPLAY_X = 10;
    private static final int DISPLAY_Y = 10;
    private static final int TILE_SIZE = 48;
    private static final float TILE_SCALE = 0.6f;
    private static final int LAYOUT_TILE_SIZE = Math.round(TILE_SIZE * TILE_SCALE);
    private static final int TILE_GAP = 6;
    private static final int ICON_SIZE = TILE_SIZE;
    private static final int ICON_TEXTURE_SIZE = 32;
    private static final int RING_SEGMENTS = 32;
    private static final int AURA_SEGMENTS = 40;
    private static final int READY_PULSE_MS = 150;

    private static final int DEFAULT_COLOR = 0xFFDDDDDD;

    public static void render(GuiGraphics graphics, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.font == null || (minecraft.screen != null && !(minecraft.screen instanceof ChatScreen))
                || minecraft.options.hideGui || minecraft.getDebugOverlay().showDebugScreen() || !minecraft.player.isAlive()) {
            return;
        }

        long now = System.currentTimeMillis();
        collectActiveCooldowns(now);
        if (ACTIVE_COOLDOWNS.isEmpty()) {
            return;
        }

        ACTIVE_COOLDOWNS.sort(REMAINING_ASCENDING);

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int x = screenWidth - LAYOUT_TILE_SIZE - DISPLAY_X;
        int y = DISPLAY_Y;

        for (Map.Entry<String, CooldownEntry> entry : ACTIVE_COOLDOWNS) {
            renderCooldownTile(graphics, minecraft.font, x, y, entry.getValue(), now, partialTicks);
            y += LAYOUT_TILE_SIZE + TILE_GAP;
        }
    }

    private static void collectActiveCooldowns(long now) {
        ACTIVE_COOLDOWNS.clear();
        Iterator<Map.Entry<String, CooldownEntry>> iterator = cooldowns.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, CooldownEntry> entry = iterator.next();
            CooldownEntry cooldown = entry.getValue();
            if (now > cooldown.endTime + READY_PULSE_MS) {
                iterator.remove();
            } else {
                ACTIVE_COOLDOWNS.add(entry);
            }
        }
    }

    private static void renderCooldownTile(GuiGraphics graphics, Font font, int x, int y, CooldownEntry cooldown,
                                           long now, float partialTicks) {
        long remaining = Math.max(0L, cooldown.endTime - now);
        float progress = cooldown.duration > 0L
                ? 1.0f - (float) remaining / (float) cooldown.duration
                : 1.0f;
        progress = Mth.clamp(progress, 0.0f, 1.0f);

        float pulseProgress = now > cooldown.endTime
                ? Mth.clamp((now - cooldown.endTime + partialTicks) / READY_PULSE_MS, 0.0f, 1.0f)
                : 0.0f;

        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0f);
        graphics.pose().scale(TILE_SCALE, TILE_SCALE, 1.0f);

        renderAura(graphics, cooldown.elementColorARGB, progress, pulseProgress);
        renderTileChrome(graphics);
        renderIcon(graphics, cooldown.icon, progress);
        renderRing(graphics, cooldown.elementColorARGB, progress, pulseProgress);
        renderSeconds(graphics, font, remaining, progress);

        graphics.pose().popPose();
    }

    private static void renderTileChrome(GuiGraphics graphics) {
        graphics.fill(0, 0, TILE_SIZE, TILE_SIZE, 0xFF14181D);
        graphics.fill(0, 0, TILE_SIZE, 1, 0xFF353A42);
        graphics.fill(0, 0, 1, TILE_SIZE, 0xFF353A42);
        graphics.fill(0, TILE_SIZE - 1, TILE_SIZE, TILE_SIZE, 0xFF0B0D10);
        graphics.fill(TILE_SIZE - 1, 0, TILE_SIZE, TILE_SIZE, 0xFF0B0D10);
    }

    private static void renderIcon(GuiGraphics graphics, ResourceLocation icon, float progress) {
        float dimBrightness = 0.55f;
        float colorAlpha = progress;
        int iconX = (TILE_SIZE - ICON_SIZE) / 2;
        int iconY = (TILE_SIZE - ICON_SIZE) / 2;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.setShaderColor(dimBrightness, dimBrightness, dimBrightness, 1.0f);
        graphics.blit(icon, iconX, iconY, ICON_SIZE, ICON_SIZE, 0.0f, 0.0f,
                ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE);

        if (colorAlpha > 0.01f) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, colorAlpha);
            graphics.blit(icon, iconX, iconY, ICON_SIZE, ICON_SIZE, 0.0f, 0.0f,
                    ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE);
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private static void renderSeconds(GuiGraphics graphics, Font font, long remaining, float progress) {
        if (progress > 0.97f || remaining <= 0L) {
            return;
        }

        String seconds = Integer.toString((int) Math.ceil(remaining / 1000.0));
        int textX = TILE_SIZE + 2 - font.width(seconds);
        int textY = TILE_SIZE - font.lineHeight + 2;

        graphics.drawString(font, seconds, textX - 1, textY, 0xFF000000, false);
        graphics.drawString(font, seconds, textX + 1, textY, 0xFF000000, false);
        graphics.drawString(font, seconds, textX, textY - 1, 0xFF000000, false);
        graphics.drawString(font, seconds, textX, textY + 1, 0xFF000000, false);
        graphics.drawString(font, seconds, textX, textY, 0xFFFFFFFF, false);
    }

    private static void renderAura(GuiGraphics graphics, int argb, float progress, float pulseProgress) {
        float alpha = progress * (0.42f + pulseProgress * 0.35f);
        if (alpha <= 0.01f) {
            return;
        }

        float radius = 36.0f + pulseProgress * 10.0f;
        float innerRadius = 12.0f;
        float center = TILE_SIZE / 2.0f;

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        drawGradientDisc(graphics.pose().last().pose(), center, center, innerRadius, radius, argb, alpha);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static void renderRing(GuiGraphics graphics, int argb, float progress, float pulseProgress) {
        Matrix4f matrix = graphics.pose().last().pose();
        float center = TILE_SIZE / 2.0f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        drawArc(matrix, center, center, 21.0f, 1.2f, 1.0f, 0x0FFFFFFF);

        if (progress > 0.0f) {
            float glowAlpha = progress * 0.24f * (1.0f - pulseProgress * 0.5f);
            drawArc(matrix, center, center, 23.0f + progress * 4.0f, 4.0f + progress * 2.0f, progress, withAlpha(argb, glowAlpha));
            drawArc(matrix, center, center, 21.0f, 1.6f, progress, argb);
        }

        if (pulseProgress > 0.0f && pulseProgress < 1.0f) {
            float pulseAlpha = (1.0f - pulseProgress) * 0.45f;
            drawArc(matrix, center, center, 21.0f + pulseProgress * 8.0f, 1.6f, 1.0f, withAlpha(argb, pulseAlpha));
        }

        RenderSystem.disableBlend();
    }

    private static void drawGradientDisc(Matrix4f matrix, float centerX, float centerY, float innerRadius,
                                         float outerRadius, int argb, float alpha) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        int innerColor = withAlpha(argb, alpha);
        int outerColor = withAlpha(argb, 0.0f);

        for (int i = 0; i < AURA_SEGMENTS; i++) {
            float angle1 = (Mth.TWO_PI * i) / AURA_SEGMENTS;
            float angle2 = (Mth.TWO_PI * (i + 1)) / AURA_SEGMENTS;

            float x1Inner = centerX + innerRadius * Mth.cos(angle1);
            float y1Inner = centerY + innerRadius * Mth.sin(angle1);
            float x2Inner = centerX + innerRadius * Mth.cos(angle2);
            float y2Inner = centerY + innerRadius * Mth.sin(angle2);
            float x1Outer = centerX + outerRadius * Mth.cos(angle1);
            float y1Outer = centerY + outerRadius * Mth.sin(angle1);
            float x2Outer = centerX + outerRadius * Mth.cos(angle2);
            float y2Outer = centerY + outerRadius * Mth.sin(angle2);

            vertex(buffer, matrix, x1Inner, y1Inner, innerColor);
            vertex(buffer, matrix, x1Outer, y1Outer, outerColor);
            vertex(buffer, matrix, x2Outer, y2Outer, outerColor);

            vertex(buffer, matrix, x1Inner, y1Inner, innerColor);
            vertex(buffer, matrix, x2Outer, y2Outer, outerColor);
            vertex(buffer, matrix, x2Inner, y2Inner, innerColor);
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void drawArc(Matrix4f matrix, float centerX, float centerY, float radius, float stroke,
                                float progress, int argb) {
        if (((argb >>> 24) & 0xFF) == 0) {
            return;
        }

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        int segments = Math.max(1, Mth.ceil(RING_SEGMENTS * progress));
        float start = -Mth.HALF_PI;
        float sweep = Mth.TWO_PI * progress;
        float innerRadius = radius - stroke * 0.5f;
        float outerRadius = radius + stroke * 0.5f;

        for (int i = 0; i < segments; i++) {
            float angle1 = start + sweep * i / segments;
            float angle2 = start + sweep * (i + 1) / segments;

            float x1Inner = centerX + innerRadius * Mth.cos(angle1);
            float y1Inner = centerY + innerRadius * Mth.sin(angle1);
            float x2Inner = centerX + innerRadius * Mth.cos(angle2);
            float y2Inner = centerY + innerRadius * Mth.sin(angle2);
            float x1Outer = centerX + outerRadius * Mth.cos(angle1);
            float y1Outer = centerY + outerRadius * Mth.sin(angle1);
            float x2Outer = centerX + outerRadius * Mth.cos(angle2);
            float y2Outer = centerY + outerRadius * Mth.sin(angle2);

            vertex(buffer, matrix, x1Inner, y1Inner, argb);
            vertex(buffer, matrix, x1Outer, y1Outer, argb);
            vertex(buffer, matrix, x2Outer, y2Outer, argb);

            vertex(buffer, matrix, x1Inner, y1Inner, argb);
            vertex(buffer, matrix, x2Outer, y2Outer, argb);
            vertex(buffer, matrix, x2Inner, y2Inner, argb);
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, int argb) {
        buffer.addVertex(matrix, x, y, 0.0f)
                .setColor((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF)
                ;
    }

    private static int withAlpha(int argb, float alpha) {
        int a = Mth.clamp((int) (alpha * 255.0f), 0, 255);
        return (argb & 0x00FFFFFF) | (a << 24);
    }

    public static void setCooldown(String name, int durationTicks) {
        Visuals visuals = resolveVisuals(name);
        setCooldown(name, durationTicks, visuals.icon, visuals.elementColorARGB);
    }

    public static void setCooldown(String name, int durationTicks, ResourceLocation icon, int elementColorARGB) {
        if (durationTicks <= 0) {
            cooldowns.remove(name);
            return;
        }

        long durationMillis = durationTicks * 50L;
        cooldowns.put(name, new CooldownEntry(
                System.currentTimeMillis() + durationMillis,
                durationMillis,
                resolveIcon(icon),
                elementColorARGB));
    }

    public static boolean isOnCooldown(String name) {
        CooldownEntry entry = cooldowns.get(name);
        return entry != null && System.currentTimeMillis() < entry.endTime;
    }

    public static int getRemainingCooldown(String name) {
        CooldownEntry entry = cooldowns.get(name);
        if (entry == null) return 0;

        long remaining = entry.endTime - System.currentTimeMillis();
        return remaining > 0 ? (int) (remaining / 50L) : 0;
    }

    public static void clearAllCooldowns() {
        cooldowns.clear();
        ACTIVE_COOLDOWNS.clear();
    }

    public static void removeCooldown(String name) {
        cooldowns.remove(name);
    }

    public static int getElementColor(String movesetId) {
        BreathingStyle style = BreathingStyle.fromMovesetId(movesetId);
        return style != null ? 0xFF000000 | style.getColor() : DEFAULT_COLOR;
    }

    private static Visuals resolveVisuals(String moveDisplayName) {
        try {
            var player = Minecraft.getInstance().player;
            if (player == null) {
                return new Visuals(MoveIcon.getDefaultIcon(), DEFAULT_COLOR);
            }

            MovesetData movesetData = PlayerDataProvider.getMovesetData(player);
            String breathingId = movesetData.getBreathingMovesetId();
            if (breathingId != null) {
                return new Visuals(resolveMoveIcon(breathingId, moveDisplayName), getElementColor(breathingId));
            }

            String demonId = movesetData.getDemonMovesetId();
            if (demonId != null) {
                return new Visuals(resolveMoveIcon(demonId, moveDisplayName), getElementColor(demonId));
            }

            String fightingId = movesetData.getFightingMovesetId();
            if (fightingId != null) {
                return new Visuals(resolveMoveIcon(fightingId, moveDisplayName), getElementColor(fightingId));
            }
        } catch (Exception ignored) {
        }

        return new Visuals(MoveIcon.getDefaultIcon(), DEFAULT_COLOR);
    }

    private static ResourceLocation resolveMoveIcon(String movesetId, String moveDisplayName) {
        AbstractMoveset moveset = NichirinMovesetRegistry.getMoveset(movesetId);
        if (moveset != null) {
            AbstractMoveset.MoveConfiguration config = findMoveByDisplayName(moveset, moveDisplayName);
            if (config != null) {
                return MoveIcon.getIconFromConfig(movesetId, config.getMoveId());
            }
        }

        return MoveIcon.getIconFormatted(movesetId, moveDisplayName);
    }

    private static AbstractMoveset.MoveConfiguration findMoveByDisplayName(AbstractMoveset moveset, String moveDisplayName) {
        for (int i = 0; i < moveset.getMoveCount(); i++) {
            AbstractMoveset.MoveConfiguration config = moveset.getMove(i);
            if (config != null && moveDisplayName.equals(config.getDisplayName())) {
                return config;
            }
        }

        AbstractMoveset.MoveConfiguration leftClick = moveset.getLeftClickConfiguration();
        if (leftClick != null && moveDisplayName.equals(leftClick.getDisplayName())) {
            return leftClick;
        }

        AbstractMoveset.MoveConfiguration rightClick = moveset.getRightClickConfiguration();
        if (rightClick != null && moveDisplayName.equals(rightClick.getDisplayName())) {
            return rightClick;
        }

        AbstractMoveset.MoveConfiguration crouchRightClick = moveset.getCrouchRightClickConfiguration();
        if (crouchRightClick != null && moveDisplayName.equals(crouchRightClick.getDisplayName())) {
            return crouchRightClick;
        }

        return null;
    }

    private static ResourceLocation resolveIcon(ResourceLocation icon) {
        ResourceLocation resolved = icon != null ? icon : MoveIcon.getDefaultIcon();
        try {
            return Minecraft.getInstance().getResourceManager().getResource(resolved).isPresent()
                    ? resolved
                    : MoveIcon.getDefaultIcon();
        } catch (Exception ignored) {
            return MoveIcon.getDefaultIcon();
        }
    }

    private record Visuals(ResourceLocation icon, int elementColorARGB) {
    }

    private static class CooldownEntry {
        final long endTime;
        final long duration;
        final ResourceLocation icon;
        final int elementColorARGB;

        CooldownEntry(long endTime, long duration, ResourceLocation icon, int elementColorARGB) {
            this.endTime = endTime;
            this.duration = duration;
            this.icon = icon;
            this.elementColorARGB = elementColorARGB;
        }
    }
}
