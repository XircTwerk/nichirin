package com.xirc.nichirin.client.gui.trainer;

import com.xirc.nichirin.common.entity.npc.BaseBreathingTrainerEntity;
import com.xirc.nichirin.common.entity.npc.TrainerType;
import com.xirc.nichirin.common.network.c2s.TrainerActionPacket;
import com.xirc.nichirin.common.network.s2c.OpenTrainerDialoguePacket;
import com.xirc.nichirin.common.util.NetworkBufferUtils;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Dialogue screen for any breathing-style trainer.
 * Content adapts to the {@link TrainerType} and {@link OpenTrainerDialoguePacket.DialogueState}.
 */
public class TrainerDialogueScreen extends Screen {

    // Colors — BigGui dark palette
    private static final int COL_BG           = 0xE8101010;
    private static final int COL_BORDER       = 0xFF3A3A3A;
    private static final int COL_BORDER_INNER = 0xFF1E1E1E;
    private static final int COL_NAME         = 0xFFFFFFFF;
    private static final int COL_NAME_SHADOW  = 0xFF333333;
    private static final int COL_SEPARATOR    = 0xFF383838;
    private static final int COL_TEXT         = 0xFFCCCCCC;
    private static final int COL_BTN_IDLE     = 0xCC282828;
    private static final int COL_BTN_HOVER    = 0xCC3F3F3F;
    private static final int COL_BTN_BORDER   = 0xFF4A4A4A;
    private static final int COL_BTN_TEXT     = 0xFFEEEEEE;

    // State
    private final UUID trainerUUID;
    private final TrainerType trainerType;
    private final OpenTrainerDialoguePacket.DialogueState state;
    private final boolean hasBeatenTrainer;

    // Layout
    private int dialogX, dialogY, dialogW, dialogH;

    public TrainerDialogueScreen(UUID trainerUUID, TrainerType trainerType,
                                 OpenTrainerDialoguePacket.DialogueState state) {
        this(trainerUUID, trainerType, state, false);
    }

    public TrainerDialogueScreen(UUID trainerUUID, TrainerType trainerType,
                                 OpenTrainerDialoguePacket.DialogueState state,
                                 boolean hasBeatenTrainer) {
        super(Component.empty());
        this.trainerUUID      = trainerUUID;
        this.trainerType      = trainerType;
        this.state            = state;
        this.hasBeatenTrainer = hasBeatenTrainer;
    }

    @Override
    protected void init() {
        dialogW = Math.min(360, width - 40);
        dialogH = 220;
        dialogX = (width  - dialogW) / 2;
        dialogY = (height - dialogH) / 2;
        clearWidgets();
        addOptionButtons();
    }

    private void addOptionButtons() {
        List<OptionButton> options = buildOptions();
        int btnW  = dialogW - 32;
        int btnH  = 18;
        int gap   = 4;
        int startY = dialogY + 140;

        for (int i = 0; i < options.size(); i++) {
            OptionButton opt = options.get(i);
            addRenderableWidget(new DialogueButton(
                    dialogX + 16, startY + i * (btnH + gap), btnW, btnH,
                    opt.label, btn -> opt.action.run()));
        }
    }

    private List<OptionButton> buildOptions() {
        List<OptionButton> list = new ArrayList<>();
        switch (state) {
            case PREREQ_MET -> {
                list.add(new OptionButton("Spar",     () -> requestDuel(BaseBreathingTrainerEntity.DuelDifficulty.EASY)));
                list.add(new OptionButton("Farewell", this::onClose));
            }
            case STUDENT -> {
                if (hasBeatenTrainer) {
                    list.add(new OptionButton("Spar",           () -> requestDuel(BaseBreathingTrainerEntity.DuelDifficulty.EASY)));
                    list.add(new OptionButton("Fight (Medium)", () -> requestDuel(BaseBreathingTrainerEntity.DuelDifficulty.MEDIUM)));
                    list.add(new OptionButton("Fight (Hard)",   () -> requestDuel(BaseBreathingTrainerEntity.DuelDifficulty.HARD)));
                    list.add(new OptionButton("Farewell",       this::onClose));
                } else {
                    list.add(new OptionButton("Spar",     () -> requestDuel(BaseBreathingTrainerEntity.DuelDifficulty.EASY)));
                    list.add(new OptionButton("Farewell", this::onClose));
                }
            }
            case STRANGER, DUEL_COOLDOWN -> list.add(new OptionButton("Farewell", this::onClose));
        }
        return list;
    }

    // Rendering

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderTransparentBackground(g);

        // Outer border
        g.fill(dialogX - 2, dialogY - 2, dialogX + dialogW + 2, dialogY + dialogH + 2, COL_BORDER);
        // Inner border
        g.fill(dialogX - 1, dialogY - 1, dialogX + dialogW + 1, dialogY + dialogH + 1, COL_BORDER_INNER);
        // Background
        g.fill(dialogX, dialogY, dialogX + dialogW, dialogY + dialogH, COL_BG);

        // Top accent strip (trainer style color)
        int accent = trainerType.styleColor | 0xFF000000;
        g.fill(dialogX, dialogY, dialogX + dialogW, dialogY + 3, accent);
        g.fill(dialogX, dialogY, dialogX + dialogW / 2, dialogY + 2,
                (accent & 0x00FFFFFF) | 0x55FFFFFF);

        // NPC name
        int nx = dialogX + 16, ny = dialogY + 12;
        g.drawString(font, trainerType.npcName, nx + 1, ny + 1, COL_NAME_SHADOW, false);
        g.drawString(font, trainerType.npcName, nx, ny, COL_NAME, false);

        // Separator
        int sepY = ny + font.lineHeight + 4;
        g.fill(dialogX + 12, sepY, dialogX + dialogW - 12, sepY + 1, COL_SEPARATOR);

        // Dialogue lines
        String[] lines = getDialogueLines();
        int textY = sepY + 8;
        for (String line : lines) {
            for (String wrapped : wordWrap(line, dialogW - 32)) {
                g.drawString(font, wrapped, dialogX + 16, textY, COL_TEXT, false);
                textY += font.lineHeight;
            }
            textY += 3;
        }

        // Prereq hint if applicable
        if (state == OpenTrainerDialoguePacket.DialogueState.STRANGER) {
            String hint = "Need: " + trainerType.prerequisiteCount + "x "
                    + trainerType.prerequisiteItem.getDefaultInstance().getHoverName().getString();
            g.drawString(font, hint, dialogX + 16, textY + 2, 0xFFAAAA44, false);
        }

        super.render(g, mx, my, pt);
    }

    private String[] getDialogueLines() {
        return switch (trainerType) {
            case WATER -> switch (state) {
                case STRANGER -> new String[]{
                    "You have potential, but your breathing is undisciplined.",
                    "Before I take you seriously, prove your resolve.",
                    "Bring me 5 wisteria blossoms. Then we will talk."
                };
                case PREREQ_MET -> new String[]{
                    "You have brought the blossoms. Good.",
                    "Your resolve is clear to me now.",
                    "Step forward and show me what you are made of."
                };
                case STUDENT -> new String[]{
                    "You carry the water within you now.",
                    "Each technique flows from the same stillness.",
                    "Want to sharpen your form? I am here."
                };
                case DUEL_COOLDOWN -> new String[]{
                    "Even water needs time to settle after turbulence.",
                    "Rest, young one. Return when you are ready."
                };
            };
            case FLAME -> switch (state) {
                case STRANGER -> new String[]{
                    "Flame Breathing demands passion and strength!",
                    "Bring me 5 blaze powders from the Nether.",
                    "If you can reach the Nether, you are ready to face me."
                };
                case PREREQ_MET -> new String[]{
                    "You've ventured into the Nether and returned. Good.",
                    "I can see the fire in your eyes.",
                    "Let us see how bright your flame truly burns!"
                };
                case STUDENT -> new String[]{
                    "Your flame burns with conviction — magnificent!",
                    "Come back whenever you wish to keep the flame sharp."
                };
                case DUEL_COOLDOWN -> new String[]{
                    "Even the greatest flames need rest to burn brighter.",
                    "Come back soon. The training continues!"
                };
            };
            case THUNDER -> switch (state) {
                case STRANGER -> new String[]{
                    "Thunder Breathing requires lightning-fast precision.",
                    "Prove you have the foundation. Bring me 8 gold ingots.",
                    "Gold is forged under immense pressure — like a true warrior."
                };
                case PREREQ_MET -> new String[]{
                    "Gold ingots. You understand value and effort.",
                    "Now let us see if you can move like thunder itself.",
                    "Prepare yourself."
                };
                case STUDENT -> new String[]{
                    "You have claimed the thunder. Do not let it fade.",
                    "Return if you wish to hone your strikes further."
                };
                case DUEL_COOLDOWN -> new String[]{
                    "Even thunder has silence between its strikes.",
                    "Rest. Return when you are sharp again."
                };
            };
            case INSECT -> switch (state) {
                case STRANGER -> new String[]{
                    "Insect Breathing is born from patience and precision.",
                    "Prove your patience. Bring me 10 spider eyes.",
                    "The spider sees all with patience. Can you?"
                };
                case PREREQ_MET -> new String[]{
                    "Ten spider eyes. You did not rush.",
                    "That patience is exactly what Insect Breathing requires.",
                    "Let us begin — slowly, deliberately."
                };
                case STUDENT -> new String[]{
                    "You have mastered the sting of the insect.",
                    "Come back. There is always more precision to cultivate."
                };
                case DUEL_COOLDOWN -> new String[]{
                    "Rest. Even insects are still before they strike.",
                    "Return when your focus has returned."
                };
            };
            case SOUND -> switch (state) {
                case STRANGER -> new String[]{
                    "Sound Breathing is FLAMBOYANT! Loud! Brilliant!",
                    "Show me you understand what that means.",
                    "Bring me 2 blocks of gold. Go on — be extravagant!"
                };
                case PREREQ_MET -> new String[]{
                    "TWO blocks of gold! Now THAT is flamboyant!",
                    "I like you already. Let's make this a spectacular fight!"
                };
                case STUDENT -> new String[]{
                    "Sound Breathing lives in you now — magnificently!",
                    "Return any time. Every spar should be a performance!"
                };
                case DUEL_COOLDOWN -> new String[]{
                    "Even the most flamboyant performers need an intermission.",
                    "Come back soon. The encore awaits!"
                };
            };
        };
    }

    // Actions

    private void requestDuel(BaseBreathingTrainerEntity.DuelDifficulty difficulty) {
        sendAction(TrainerActionPacket.Action.REQUEST_DUEL, difficulty);
        onClose();
    }

    private void sendAction(TrainerActionPacket.Action action, BaseBreathingTrainerEntity.DuelDifficulty difficulty) {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            new TrainerActionPacket(trainerUUID, action, difficulty).toBytes(buf);
            NetworkManager.sendToServer(NichirinPacketRegistry.TRAINER_ACTION_ID, NetworkBufferUtils.client(buf));
        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // Helpers

    private List<String> wordWrap(String text, int maxW) {
        List<String> result = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder cur = new StringBuilder();
        for (String w : words) {
            String test = cur.length() == 0 ? w : cur + " " + w;
            if (font.width(test) > maxW && cur.length() > 0) {
                result.add(cur.toString());
                cur = new StringBuilder(w);
            } else {
                cur = new StringBuilder(test);
            }
        }
        if (cur.length() > 0) result.add(cur.toString());
        return result;
    }

    private record OptionButton(String label, Runnable action) {}

    private class DialogueButton extends Button {
        DialogueButton(int x, int y, int w, int h, String label, OnPress press) {
            super(x, y, w, h, Component.literal(label), press, DEFAULT_NARRATION);
        }

        @Override
        public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            boolean hov = isHovered();
            g.fill(getX() - 1, getY() - 1, getX() + getWidth() + 1, getY() + getHeight() + 1, COL_BTN_BORDER);
            g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), hov ? COL_BTN_HOVER : COL_BTN_IDLE);
            g.drawString(font, (hov ? "> " : "  ") + getMessage().getString(),
                    getX() + 6, getY() + (getHeight() - font.lineHeight) / 2,
                    hov ? 0xFFFFFFFF : COL_BTN_TEXT, false);
        }
    }
}