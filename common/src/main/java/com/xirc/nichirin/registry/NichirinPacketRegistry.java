package com.xirc.nichirin.registry;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.client.data.ClientProgressionCache;
import com.xirc.nichirin.client.gui.trainer.TrainerDialogueClientHandler;
import com.xirc.nichirin.client.renderer.effects.AttackHitboxRenderer;
import com.xirc.nichirin.client.renderer.effects.ParrySparkHandler;
import com.xirc.nichirin.common.attack.moves.breathing.thunder.ThunderclapChargeManager;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.attack.moveset.CqcMoveset;
import com.xirc.nichirin.common.config.NichirinModConfig;
import com.xirc.nichirin.common.data.*;
import com.xirc.nichirin.common.item.katana.SimpleKatana;
import com.xirc.nichirin.common.item.gun.GenyaDB;
import com.xirc.nichirin.common.network.c2s.*;
import com.xirc.nichirin.common.network.s2c.*;
import net.minecraft.world.phys.Vec3;
import com.xirc.nichirin.common.network.util.CooldownDisplayPacket;
import com.xirc.nichirin.common.network.util.MovesetSyncPacket;
import com.xirc.nichirin.common.system.DemonComponent;
import com.xirc.nichirin.common.system.blocking.HandToHandBlock;
import com.xirc.nichirin.common.system.blocking.KatanaBlock;
import com.xirc.nichirin.common.system.sheathing.PlayerSheathData;
import com.xirc.nichirin.common.util.MultiplayerInputHandler;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.InteractionHand;
import io.netty.buffer.Unpooled;

import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.xirc.nichirin.common.util.NetworkBufferUtils.client;
import static com.xirc.nichirin.common.util.NetworkBufferUtils.server;
import static com.xirc.nichirin.common.util.NetworkBufferUtils.serverCopy;
import java.util.concurrent.ConcurrentHashMap;

public interface NichirinPacketRegistry {

    // Packet IDs
    ResourceLocation DOUBLE_JUMP_ID = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "double_jump");
    ResourceLocation BREATHING_MOVE_ID = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "breathing_move");
    ResourceLocation BREATHING_EFFECT_ID = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "breathing_effect");
    ResourceLocation SYNC_BREATH_ID = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "sync_breath");
    ResourceLocation SYNC_STAMINA_ID = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "sync_stamina");
    ResourceLocation SYNC_STANCE_ID = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "sync_stance");
    ResourceLocation BLOCK_START_ID = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "block_start");
    ResourceLocation BLOCK_STOP_ID = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "block_stop");
    ResourceLocation PARRY_ID = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "parry");
    ResourceLocation PLAYER_ANIMATION_ID = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "player_animation");
    ResourceLocation MOVEMENT_INPUT_ID = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "movement_input");
    ResourceLocation MOVEMENT_INPUT_SYNC_ID = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "movement_input_sync");
    ResourceLocation SYNC_BREATHING_STYLE = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "sync_breathing_style");
    ResourceLocation REQUEST_STYLE_CHANGE = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "request_style_change");
    ResourceLocation COMBO_COUNTER_ID = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "combo_counter");
    ResourceLocation HITBOX_PACKET_ID = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "hitbox_data");
    ResourceLocation MOVE_HOTKEY_ID = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "move_hotkey");
    ResourceLocation SYNC_PROGRESSION_ID = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "sync_progression");
    ResourceLocation DEMON_MOVE_ID = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "demon_move");
    ResourceLocation MOVESET_CONFIG_ID = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "moveset_config_sync");
    ResourceLocation DEMON_SYNC_ID = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "demon_sync");
    ResourceLocation DEMON_INPUT_ID = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "demon_input");
    ResourceLocation ATTACK_WHEEL_STATE_ID = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "attack_wheel_state");
    ResourceLocation KATANA_INPUT_ID = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "katana_input");
    ResourceLocation GUN_INPUT_ID = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "gun_input");
    ResourceLocation TRIGGER_SHADER_ID = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "trigger_shader");
    ResourceLocation PARRY_SPARK_ID        = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "parry_spark");
    ResourceLocation OPEN_CONFIG_SCREEN_ID = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "open_config_screen");
    ResourceLocation BLOOD_MOON_SYNC_ID    = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "blood_moon_sync");
    ResourceLocation PERK_SYNC_ID                  = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "perk_sync");
    ResourceLocation PERK_ACTION_ID                = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "perk_action");
    ResourceLocation OPEN_TRAINER_DIALOGUE_ID      = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "open_trainer_dialogue");
    ResourceLocation TRAINER_ACTION_ID             = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "trainer_action");
    ResourceLocation MIST_CLONES_ID                = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "mist_clones");
    ResourceLocation CLONE_RING_ID                 = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "clone_ring");
    ResourceLocation SHEATH_INPUT_ID               = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "sheath_input");
    ResourceLocation SHEATH_CONFIG_ID              = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "sheath_config");
    ResourceLocation SHEATH_SYNC_ID                = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "sheath_sync");
    ResourceLocation CQC_PRESET_UPDATE_ID          = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "cqc_preset_update");
    ResourceLocation CQC_STANCE_UPDATE_ID          = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "cqc_stance_update");
    ResourceLocation CQC_ACTIVE_PRESET_UPDATE_ID   = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "cqc_active_preset_update");
    ResourceLocation CQC_PRESET_RESET_ID           = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "cqc_preset_reset");
    ResourceLocation CQC_FOLLOWUP_UPDATE_ID        = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "cqc_followup_update");
    ResourceLocation CQC_PRESET_SYNC_ID            = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "cqc_preset_sync");
    // Shared cooldown HUD channel — sent by CooldownDisplayPacket, MoveExecutor and KatanaBlock,
    // received by CooldownDisplayPacket.registerClient() on the client.
    ResourceLocation COOLDOWN_DISPLAY_ID           = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "cooldown_display");
    ResourceLocation AURA_ADD_ID                   = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "aura_add");
    ResourceLocation AURA_REMOVE_ID                = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "aura_remove");
    ResourceLocation AURA_CLEAR_ID                 = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "aura_clear");
    ResourceLocation OUTLINE_ADD_ID                = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "outline_add");
    ResourceLocation OUTLINE_REMOVE_ID             = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "outline_remove");
    ResourceLocation OUTLINE_CLEAR_ID              = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "outline_clear");
    ResourceLocation AFTERIMAGE_ID                 = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "afterimage");
    ResourceLocation THUNDERCLAP_RELEASE_ID        = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "thunderclap_release");
    ResourceLocation DESTRUCTIVE_DEATH_STATE_ID    = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "destructive_death_state");
    ResourceLocation GUN_ANIMATION_ID              = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "gun_animation");

    // Packet class mappings
    Map<Class<?>, ResourceLocation> PACKET_IDS = new HashMap<>();

    // Server-side state tracking for MultiplayerInputHandler
    Map<UUID, MultiplayerInputHandler.PlayerInputState> SERVER_PLAYER_STATES = new ConcurrentHashMap<>();

    static void init() {
        // Map packet classes to IDs
        PACKET_IDS.put(DoubleJumpPacket.class, DOUBLE_JUMP_ID);
        PACKET_IDS.put(BreathingMovePacket.class, BREATHING_MOVE_ID);
        PACKET_IDS.put(DemonMovePacket.class, DEMON_MOVE_ID);
        PACKET_IDS.put(BreathingEffectPacket.class, BREATHING_EFFECT_ID);
        PACKET_IDS.put(SyncBreathPacket.class, SYNC_BREATH_ID);
        PACKET_IDS.put(StaminaSyncPacket.class, SYNC_STAMINA_ID);
        PACKET_IDS.put(StanceSyncPacket.class, SYNC_STANCE_ID);
        PACKET_IDS.put(PlayerAnimationPacket.class, PLAYER_ANIMATION_ID);
        PACKET_IDS.put(MovementInputPacket.class, MOVEMENT_INPUT_ID);
        PACKET_IDS.put(MovementInputSyncPacket.class, MOVEMENT_INPUT_SYNC_ID);
        PACKET_IDS.put(ComboCounterPacket.class, COMBO_COUNTER_ID);
        PACKET_IDS.put(MoveHotkeyPacket.class, MOVE_HOTKEY_ID);
        PACKET_IDS.put(DemonSyncPacket.class, DEMON_SYNC_ID);
        PACKET_IDS.put(TriggerShaderPacket.class, TRIGGER_SHADER_ID);
        PACKET_IDS.put(MistClonesPacket.class, MIST_CLONES_ID);
        PACKET_IDS.put(CloneRingPacket.class, CLONE_RING_ID);
        PACKET_IDS.put(AfterimagePacket.class, AFTERIMAGE_ID);
        PACKET_IDS.put(SheathInputPacket.class, SHEATH_INPUT_ID);
        PACKET_IDS.put(SheathConfigPacket.class, SHEATH_CONFIG_ID);
        PACKET_IDS.put(SheathSyncPacket.class, SHEATH_SYNC_ID);
        PACKET_IDS.put(DestructiveDeathStateSyncPacket.class, DESTRUCTIVE_DEATH_STATE_ID);

        registerPackets();
    }

    static void registerPackets() {
        try {
            registerC2SPackets();
            // S2C channels follow Architectury's documented contract: register the RECEIVER on
            // the client (which also registers the payload Type), and register just the payload
            // TYPE on the server. The server needs the Type registered to be able to SEND —
            // otherwise sendToPlayer builds a payload with a null CustomPacketPayload.Type and
            // throws "type is null" on dedicated servers. We must NOT register the client
            // receivers on the server: their bodies reference client-only classes.
            if (Platform.getEnvironment() == Env.CLIENT) {
                registerS2CPacketsWithFallback();
            } else {
                registerS2CTypesForServer();
            }
        } catch (Exception e) {
            throw new RuntimeException("Packet registration failed", e);
        }
    }

    /**
     * Server-side: register the payload TYPE for every S2C channel the server sends, so that
     * {@link NetworkManager#sendToPlayer} can build a valid payload. No receiver/handler is
     * registered here (the server never receives S2C), which keeps client-only classes off the
     * server. Keep this list in sync with the channels registered in
     * {@link #registerS2CPacketsWithFallback()} plus {@code cooldown_display}.
     */
    static void registerS2CTypesForServer() {
        ResourceLocation[] s2cIds = {
                BREATHING_EFFECT_ID, SYNC_BREATH_ID, SYNC_STAMINA_ID, SYNC_STANCE_ID,
                PLAYER_ANIMATION_ID, COMBO_COUNTER_ID, MOVESET_CONFIG_ID, SYNC_BREATHING_STYLE,
                SYNC_PROGRESSION_ID, DEMON_SYNC_ID, HITBOX_PACKET_ID, TRIGGER_SHADER_ID,
                PARRY_SPARK_ID, BLOOD_MOON_SYNC_ID, PERK_SYNC_ID, OPEN_TRAINER_DIALOGUE_ID,
                MIST_CLONES_ID, CLONE_RING_ID, SHEATH_SYNC_ID, OPEN_CONFIG_SCREEN_ID, CQC_PRESET_SYNC_ID, COOLDOWN_DISPLAY_ID,
                AURA_ADD_ID, AURA_REMOVE_ID, AURA_CLEAR_ID,
                OUTLINE_ADD_ID, OUTLINE_REMOVE_ID, OUTLINE_CLEAR_ID, AFTERIMAGE_ID,
                DESTRUCTIVE_DEATH_STATE_ID, GUN_ANIMATION_ID
        };
        for (ResourceLocation id : s2cIds) {
            try {
                NetworkManager.registerS2CPayloadType(id);
            } catch (Throwable ignored) {
                // Already registered or unsupported on this Architectury version; ignore.
            }
        }
    }

    static void registerC2SPackets() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, DOUBLE_JUMP_ID, (buf, context) -> {
            DoubleJumpPacket packet = new DoubleJumpPacket(buf);
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                context.queue(() -> packet.handle(serverPlayer));
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, BREATHING_MOVE_ID, (buf, context) -> {
            BreathingMovePacket packet = new BreathingMovePacket(buf);
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                context.queue(() -> packet.handle(serverPlayer));
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, DEMON_MOVE_ID, (buf, context) -> {
            DemonMovePacket packet = new DemonMovePacket(buf);
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                context.queue(() -> packet.handle(serverPlayer));
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, ATTACK_WHEEL_STATE_ID, (buf, context) -> {
            boolean wheelOpen = buf.readBoolean();
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                context.queue(() -> {
                    MultiplayerInputHandler.PlayerInputState state = getOrCreatePlayerState(serverPlayer);
                    state.attackWheelOpen = wheelOpen;

                    if (!wheelOpen) {
                        state.wheelCloseTime = serverPlayer.level().getGameTime();
                        state.inputBlocked = true;
                        state.blockUntilTime = serverPlayer.level().getGameTime() + 10;
                    }
                });
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, KATANA_INPUT_ID, (buf, context) -> {
            MultiplayerInputHandler.InputType inputType = buf.readEnum(MultiplayerInputHandler.InputType.class);
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                context.queue(() -> {
                    // The right-click special is block + left-click, so it must fire while guarding.
                    boolean isBlockSpecial = inputType == MultiplayerInputHandler.InputType.RIGHT_CLICK
                            || inputType == MultiplayerInputHandler.InputType.RIGHT_CLICK_CROUCH;
                    if (shouldBlockInputsServer(serverPlayer, !isBlockSpecial)) {
                        return;
                    }
                    executeKatanaInput(serverPlayer, inputType);
                });
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, GUN_INPUT_ID, (buf, context) -> {
            int barrels = buf.readInt();
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                context.queue(() -> {
                    if (shouldBlockInputsServer(serverPlayer)) {
                        return;
                    }
                    if (serverPlayer.getMainHandItem().getItem() instanceof GenyaDB gun) {
                        gun.performShoot(serverPlayer, barrels);
                    }
                });
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, DEMON_INPUT_ID, (buf, context) -> {
            String inputTypeName = buf.readUtf();
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                context.queue(() -> {
                    try {
                        MultiplayerInputHandler.InputType inputType = MultiplayerInputHandler.InputType.valueOf(inputTypeName);
                        boolean isBlockSpecial = inputType == MultiplayerInputHandler.InputType.RIGHT_CLICK
                                || inputType == MultiplayerInputHandler.InputType.RIGHT_CLICK_CROUCH;
                        if (shouldBlockInputsServer(serverPlayer, !isBlockSpecial)) {
                            return;
                        }
                        handleDemonInput(serverPlayer, inputType);
                    } catch (Exception e) {
                        // Invalid input type, ignore
                    }
                });
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, BLOCK_START_ID, (buf, context) -> {
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                context.queue(() -> {
                    if (!KatanaBlock.startBlocking(serverPlayer)) {
                        HandToHandBlock.startBlocking(serverPlayer);
                    }
                });
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, BLOCK_STOP_ID, (buf, context) -> {
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                context.queue(() -> {
                    KatanaBlock.stopBlocking(serverPlayer);
                    HandToHandBlock.stopBlocking(serverPlayer);
                });
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, PARRY_ID, (buf, context) -> {
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                context.queue(() -> {
                    if (!KatanaBlock.attemptParry(serverPlayer)) {
                        HandToHandBlock.attemptParry(serverPlayer);
                    }
                });
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, MOVEMENT_INPUT_ID, (buf, context) -> {
            MovementInputPacket packet = new MovementInputPacket(buf);
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                context.queue(() -> packet.handle(serverPlayer));
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, MOVEMENT_INPUT_SYNC_ID, (buf, context) -> {
            MovementInputSyncPacket packet = new MovementInputSyncPacket(buf);
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                context.queue(() -> packet.handle(serverPlayer));
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, REQUEST_STYLE_CHANGE, (buf, context) -> {
            String movesetId = buf.readBoolean() ? buf.readUtf() : null;
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                context.queue(() -> handleStyleChangeRequestFromOriginalPacket(serverPlayer, movesetId));
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, MOVE_HOTKEY_ID, (buf, context) -> {
            MoveHotkeyPacket packet = new MoveHotkeyPacket(buf);
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                context.queue(() -> packet.handle(context));
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, PERK_ACTION_ID, (buf, context) -> {
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, TRAINER_ACTION_ID, (buf, context) -> {
            TrainerActionPacket packet =
                    new TrainerActionPacket(buf);
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                context.queue(() -> packet.handle(serverPlayer));
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, SHEATH_INPUT_ID, (buf, context) -> {
            SheathInputPacket packet = new SheathInputPacket(buf);
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                context.queue(() -> packet.handle(serverPlayer));
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, SHEATH_CONFIG_ID, (buf, context) -> {
            SheathConfigPacket packet = new SheathConfigPacket(buf);
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                context.queue(() -> packet.handle(serverPlayer));
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, CQC_PRESET_UPDATE_ID, (buf, context) -> {
            String slotName = buf.readUtf();
            int wheelIndex = buf.readInt();
            String moveId = buf.readUtf();
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                context.queue(() -> handleCqcPresetUpdate(serverPlayer, slotName, wheelIndex, moveId));
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, CQC_STANCE_UPDATE_ID, (buf, context) -> {
            int stanceIndex = buf.readInt();
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                context.queue(() -> handleCqcStanceUpdate(serverPlayer, stanceIndex));
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, CQC_ACTIVE_PRESET_UPDATE_ID, (buf, context) -> {
            int presetIndex = buf.readInt();
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                context.queue(() -> handleCqcActivePresetUpdate(serverPlayer, presetIndex));
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, CQC_PRESET_RESET_ID, (buf, context) -> {
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                context.queue(() -> handleCqcPresetReset(serverPlayer));
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, CQC_FOLLOWUP_UPDATE_ID, (buf, context) -> {
            String baseMoveId = buf.readUtf();
            String followupMoveId = buf.readUtf();
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                context.queue(() -> handleCqcFollowupUpdate(serverPlayer, baseMoveId, followupMoveId));
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, THUNDERCLAP_RELEASE_ID, (buf, context) -> {
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                context.queue(() -> ThunderclapChargeManager.releaseCharge(serverPlayer));
            }
        });
    }

    static void registerS2CPacketsWithFallback() {
        try {
            // cooldown_display receiver (client only). The server registers the matching
            // payload Type in registerS2CTypesForServer() so it can send this channel.
            CooldownDisplayPacket.registerClient();

            NetworkManager.registerReceiver(NetworkManager.Side.S2C, BREATHING_EFFECT_ID, (buf, context) -> {
                BreathingEffectPacket packet = new BreathingEffectPacket(buf);
                context.queue(() -> packet.handleClient());
            });

            NetworkManager.registerReceiver(NetworkManager.Side.S2C, SYNC_BREATH_ID, (buf, context) -> {
                SyncBreathPacket packet = new SyncBreathPacket(buf);
                context.queue(() -> packet.handleClient());
            });

            NetworkManager.registerReceiver(NetworkManager.Side.S2C, SYNC_STAMINA_ID, (buf, context) -> {
                StaminaSyncPacket packet = new StaminaSyncPacket(buf);
                context.queue(() -> packet.handleClient());
            });

            NetworkManager.registerReceiver(NetworkManager.Side.S2C, SYNC_STANCE_ID, (buf, context) -> {
                StanceSyncPacket packet = new StanceSyncPacket(buf);
                context.queue(() -> packet.handleClient());
            });

            NetworkManager.registerReceiver(NetworkManager.Side.S2C, PLAYER_ANIMATION_ID, (buf, context) -> {
                PlayerAnimationPacket packet = new PlayerAnimationPacket(buf);
                context.queue(() -> packet.handleClient());
            });

            NetworkManager.registerReceiver(NetworkManager.Side.S2C, COMBO_COUNTER_ID, (buf, context) -> {
                ComboCounterPacket packet = new ComboCounterPacket(buf);
                context.queue(() -> packet.handleClient());
            });

            NetworkManager.registerReceiver(NetworkManager.Side.S2C, MOVESET_CONFIG_ID, (buf, context) -> {
                MovesetConfigSyncPacket packet = new MovesetConfigSyncPacket(buf);
                context.queue(() -> packet.handleClient());
            });

            NetworkManager.registerReceiver(NetworkManager.Side.S2C, SYNC_BREATHING_STYLE, (buf, context) -> {
                String movesetId = buf.readBoolean() ? buf.readUtf() : null;
                context.queue(() -> {
                    Player player = context.getPlayer();
                    if (player != null) {
                        MovesetData data = PlayerDataProvider.getMovesetData(player);

                        if (movesetId != null) {
                            AbstractMoveset moveset =
                                    NichirinMovesetRegistry.getMoveset(movesetId);

                            if (moveset != null) {
                                if (moveset.isBreathingMoveset()) {
                                    data.setBreathingMovesetId(movesetId);
                                } else if (moveset.isDemonMoveset()) {
                                    data.setDemonMovesetId(movesetId);
                                } else if (moveset.isNeutralMoveset()) {
                                    data.setFightingMovesetId(movesetId);
                                }
                            }
                        } else {
                            data.clearMovesets();
                        }
                    }
                });
            });

            NetworkManager.registerReceiver(NetworkManager.Side.S2C, SYNC_PROGRESSION_ID, (buf, context) -> {
                int count = buf.readInt();
                Set<String> unlockedStyles = new HashSet<>();

                for (int i = 0; i < count; i++) {
                    unlockedStyles.add(buf.readUtf());
                }

                context.queue(() -> {
                    ClientProgressionCache.setUnlockedStyles(unlockedStyles);
                });
            });

            NetworkManager.registerReceiver(NetworkManager.Side.S2C, DEMON_SYNC_ID, (buf, context) -> {
                int bloodPoints = buf.readInt();
                int halfBloodPoints = buf.readInt();
                boolean isDemon = buf.readBoolean();

                context.queue(() -> {
                    DemonComponent.updateBloodFromSync(bloodPoints, halfBloodPoints, isDemon);
                });
            });

            NetworkManager.registerReceiver(NetworkManager.Side.S2C, HITBOX_PACKET_ID, (buf, context) -> {
                int hitboxCount = buf.readInt();
                List<AABB> hitboxesToAdd = new ArrayList<>();
                long duration = 0;

                for (int i = 0; i < hitboxCount; i++) {
                    double minX = buf.readDouble();
                    double minY = buf.readDouble();
                    double minZ = buf.readDouble();
                    double maxX = buf.readDouble();
                    double maxY = buf.readDouble();
                    double maxZ = buf.readDouble();
                    duration = buf.readLong();

                    AABB hitbox = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
                    hitboxesToAdd.add(hitbox);
                }

                final long finalDuration = duration;
                context.queue(() -> {
                    for (AABB hitbox : hitboxesToAdd) {
                        AttackHitboxRenderer.addHitbox(hitbox, finalDuration, false);
                    }
                });
            });

            NetworkManager.registerReceiver(NetworkManager.Side.S2C, TRIGGER_SHADER_ID, (buf, context) -> {
                TriggerShaderPacket packet = new TriggerShaderPacket(buf);
                context.queue(() -> packet.handleClient());
            });

            NetworkManager.registerReceiver(NetworkManager.Side.S2C, PARRY_SPARK_ID, (buf, context) -> {
                double x = buf.readDouble();
                double y = buf.readDouble();
                double z = buf.readDouble();
                context.queue(() -> ParrySparkHandler.spawnSparks(x, y, z));
            });

            NetworkManager.registerReceiver(NetworkManager.Side.S2C, BLOOD_MOON_SYNC_ID, (buf, context) -> {
                BloodMoonSyncPacket packet =
                        new BloodMoonSyncPacket(buf);
                context.queue(() -> packet.handleClient());
            });

            NetworkManager.registerReceiver(NetworkManager.Side.S2C, PERK_SYNC_ID, (buf, context) -> {
            });

            NetworkManager.registerReceiver(NetworkManager.Side.S2C, OPEN_TRAINER_DIALOGUE_ID, (buf, context) -> {
                OpenTrainerDialoguePacket packet =
                        new OpenTrainerDialoguePacket(buf);
                context.queue(() -> TrainerDialogueClientHandler.open(packet));
            });

            NetworkManager.registerReceiver(NetworkManager.Side.S2C, MIST_CLONES_ID, (buf, context) -> {
                MistClonesPacket packet = new MistClonesPacket(buf);
                context.queue(() -> packet.handleClient());
            });

            NetworkManager.registerReceiver(NetworkManager.Side.S2C, CLONE_RING_ID, (buf, context) -> {
                CloneRingPacket packet = new CloneRingPacket(buf);
                context.queue(packet::handleClient);
            });

            NetworkManager.registerReceiver(NetworkManager.Side.S2C, AFTERIMAGE_ID, (buf, context) -> {
                AfterimagePacket packet = new AfterimagePacket(buf);
                context.queue(packet::handleClient);
            });

            NetworkManager.registerReceiver(NetworkManager.Side.S2C, DESTRUCTIVE_DEATH_STATE_ID, (buf, context) -> {
                DestructiveDeathStateSyncPacket packet = new DestructiveDeathStateSyncPacket(buf);
                context.queue(packet::handleClient);
            });

            NetworkManager.registerReceiver(NetworkManager.Side.S2C, GUN_ANIMATION_ID, (buf, context) -> {
                GunAnimationPacket packet = new GunAnimationPacket(buf);
                context.queue(packet::handleClient);
            });

            NetworkManager.registerReceiver(NetworkManager.Side.S2C, SHEATH_SYNC_ID, (buf, context) -> {
                SheathSyncPacket packet = new SheathSyncPacket(buf);
                context.queue(packet::handleClient);
            });

            NetworkManager.registerReceiver(NetworkManager.Side.S2C, CQC_PRESET_SYNC_ID, (buf, context) -> {
                int activePresetIndex = buf.readInt();
                CqcPresetData.Preset[] syncedPresets = new CqcPresetData.Preset[CqcPresetData.PRESET_COUNT];
                for (int presetIndex = 0; presetIndex < CqcPresetData.PRESET_COUNT; presetIndex++) {
                    String left = buf.readUtf();
                    String right = buf.readUtf();
                    String crouchRight = buf.readUtf();
                    String[] wheelMoves = new String[CqcPresetData.WHEEL_SLOT_COUNT];
                    for (int i = 0; i < CqcPresetData.WHEEL_SLOT_COUNT; i++) {
                        wheelMoves[i] = buf.readUtf();
                    }
                    int stanceIndex = buf.readInt();
                    syncedPresets[presetIndex] = new CqcPresetData.Preset("Preset " + (presetIndex + 1),
                            left, right, crouchRight, stanceIndex,
                            wheelMoves[0], wheelMoves[1], wheelMoves[2], wheelMoves[3], wheelMoves[4]);
                    int followupCount = buf.readInt();
                    for (int i = 0; i < followupCount; i++) {
                        syncedPresets[presetIndex].setFollowupMove(buf.readUtf(), buf.readUtf());
                    }
                }
                context.queue(() -> {
                    Player player = context.getPlayer();
                    if (player == null) return;
                    CqcPresetData preset = PlayerDataProvider.getData(player).getCqcPresetData();
                    for (int i = 0; i < CqcPresetData.PRESET_COUNT; i++) {
                        preset.setPresetFromNetwork(i, syncedPresets[i]);
                    }
                    preset.setActivePresetIndex(activePresetIndex);
                });
            });

            // Open the Cloth Config GUI on the client when requested by a command
            NetworkManager.registerReceiver(NetworkManager.Side.S2C, OPEN_CONFIG_SCREEN_ID, (buf, context) -> {
                context.queue(() -> {
                    Minecraft mc = Minecraft.getInstance();
                    mc.setScreen(AutoConfig
                            .getConfigScreen(NichirinModConfig.class, mc.screen)
                            .get());
                });
            });

        } catch (NoSuchMethodError e) {
            // Handle older Architectury versions
        } catch (Exception e) {
            // Log error but don't crash
        }
    }

    // Utility methods
    static MultiplayerInputHandler.PlayerInputState getOrCreatePlayerState(Player player) {
        return SERVER_PLAYER_STATES.computeIfAbsent(player.getUUID(), uuid -> new MultiplayerInputHandler.PlayerInputState());
    }

    static void clearInputBlock(Player player) {
        MultiplayerInputHandler.PlayerInputState state = SERVER_PLAYER_STATES.get(player.getUUID());
        if (state != null) {
            state.clearBlock();
        }
    }

    static boolean shouldBlockInputsServer(Player player) {
        return shouldBlockInputsServer(player, true);
    }

    /**
     * @param respectBlocking when false, the active block (guard) does NOT count as a reason to
     *                        suppress the input — used for the block + left-click special, which by
     *                        definition fires while guarding. Stun and the post-move input lock
     *                        still apply.
     */
    static boolean shouldBlockInputsServer(Player player, boolean respectBlocking) {
        if (player.level().isClientSide) return false;

        if (player.hasEffect(NichirinEffectRegistry.stunned())) {
            return true;
        }

        if (respectBlocking && player.hasEffect(NichirinEffectRegistry.blocking())) {
            return true;
        }

        MultiplayerInputHandler.PlayerInputState state = SERVER_PLAYER_STATES.get(player.getUUID());
        if (state == null) return false;

        long currentTime = player.level().getGameTime();
        return state.shouldBlockInput(currentTime);
    }

    static void executeKatanaInput(ServerPlayer player, MultiplayerInputHandler.InputType inputType) {
        var mainHand = player.getMainHandItem();
        if (!(mainHand.getItem() instanceof SimpleKatana katana)) {
            return;
        }

        switch (inputType) {
            case LEFT_CLICK -> katana.performAttack(player);
            case RIGHT_CLICK -> katana.performSpecial(player, false);
            case RIGHT_CLICK_CROUCH -> katana.performSpecial(player, true);
        }

        MultiplayerInputHandler.PlayerInputState state = getOrCreatePlayerState(player);
        state.inputBlocked = true;
        state.blockUntilTime = player.level().getGameTime();
    }

    static void handleDemonInput(ServerPlayer player, MultiplayerInputHandler.InputType inputType) {
        if (player.getMainHandItem().isEmpty() && MovesetHelper.hasFightingMoveset(player)) {
            AbstractMoveset fightingMoveset = MovesetHelper.getFightingMoveset(player);
            if (fightingMoveset != null && fightingMoveset.isNeutralMoveset()) {
                if (fightingMoveset instanceof CqcMoveset) {
                    CqcMoveset.withPlayer(player, () -> {
                        switch (inputType) {
                            case LEFT_CLICK -> fightingMoveset.handleLeftClick(player);
                            case RIGHT_CLICK -> fightingMoveset.handleRightClick(player, false);
                            case RIGHT_CLICK_CROUCH -> fightingMoveset.handleRightClick(player, true);
                        }
                    });
                } else {
                    switch (inputType) {
                        case LEFT_CLICK -> fightingMoveset.handleLeftClick(player);
                        case RIGHT_CLICK -> fightingMoveset.handleRightClick(player, false);
                        case RIGHT_CLICK_CROUCH -> fightingMoveset.handleRightClick(player, true);
                    }
                }
                return;
            }
        }

        if (inputType == MultiplayerInputHandler.InputType.LEFT_CLICK
                && !player.getMainHandItem().isEmpty()) {
            return;
        }

        if (player.hasEffect(NichirinEffectRegistry.stunned())) {
            return;
        }

        // The demon right-click special is performed via block + left-click, so it must fire while
        // guarding. Only suppress the demon LEFT_CLICK (M1) while blocking, not the block-special.
        boolean isDemonBlockSpecial = inputType == MultiplayerInputHandler.InputType.RIGHT_CLICK
                || inputType == MultiplayerInputHandler.InputType.RIGHT_CLICK_CROUCH;
        if (!isDemonBlockSpecial && player.hasEffect(NichirinEffectRegistry.blocking())) {
            return;
        }

        if (!MovesetHelper.hasDemonMoveset(player)) {
            return;
        }

        AbstractMoveset moveset = MovesetHelper.getDemonMoveset(player);
        if (moveset == null || !moveset.isDemonMoveset()) {
            return;
        }

        switch (inputType) {
            case LEFT_CLICK -> moveset.handleLeftClick(player);
            case RIGHT_CLICK -> moveset.handleRightClick(player, false);
            case RIGHT_CLICK_CROUCH -> moveset.handleRightClick(player, true);
        }
    }

    static void handleStyleChangeRequestFromOriginalPacket(ServerPlayer player, String movesetId) {
        try {
            if (movesetId != null && !NichirinMovesetRegistry.isRegistered(movesetId)) {
                player.sendSystemMessage(Component.literal(
                        "§cInvalid breathing style: " + movesetId
                ));
                return;
            }

            AbstractMoveset requestedMoveset = movesetId != null ? NichirinMovesetRegistry.getMoveset(movesetId) : null;
            if (requestedMoveset != null
                    && requestedMoveset.isNeutralMoveset()
                    && movesetId.equals(PlayerDataProvider.getMovesetData(player).getFightingMovesetId())) {
                PlayerDataProvider.clearFightingAndSync(player);
                player.displayClientMessage(
                        Component.literal("\u00A77Unequipped " + formatStyleName(movesetId) + "."), true);
                return;
            }

            boolean unlockedByDefault = requestedMoveset != null && requestedMoveset.isNeutralMoveset();
            if (movesetId != null && !unlockedByDefault && !ProgressionHelper.isStyleUnlocked(player, movesetId)) {
                String requirement = ProgressionHelper.getUnlockRequirement(movesetId);
                player.displayClientMessage(Component.literal(
                        "§cYou haven't unlocked this breathing style! §fRequirement: §e" + requirement), true);
                return;
            }

            PlayerDataProvider.updateAndSync(player, movesetId);

            if (movesetId != null) {
                String styleName = formatStyleName(movesetId);
                player.displayClientMessage(
                        Component.literal("§aSwitched to " + styleName + "."), true);
            } else {
                player.displayClientMessage(
                        Component.literal("§7Cleared breathing style."), true);
            }
        } catch (Exception e) {
            // Log error but don't crash
        }
    }

    static String formatStyleName(String styleId) {
        String[] parts = styleId.split("_");
        StringBuilder formatted = new StringBuilder();
        for (String part : parts) {
            if (formatted.length() > 0) formatted.append(" ");
            formatted.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
        }
        return formatted.toString();
    }

    // Packet sending methods
    /**
     * Broadcast a hitbox to all players tracking a given entity (works for NPCs too).
     * Falls back to sending to all online players if not on a ServerLevel.
     */
    /**
     * Broadcasts a parry spark effect at the parrying player's position to all online players.
     */
    static void sendParrySpark(LivingEntity parrier) {
        if (parrier.level().isClientSide) return;
        if (!(parrier.level() instanceof ServerLevel serverLevel)) return;
        try {
            double x = parrier.getX();
            double y = parrier.getY() + parrier.getBbHeight() * 0.6;
            double z = parrier.getZ();
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeDouble(x);
            buf.writeDouble(y);
            buf.writeDouble(z);
            for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
                NetworkManager.sendToPlayer(player, PARRY_SPARK_ID, serverCopy(buf, player));
            }
            buf.release();
        } catch (Exception e) {
            // ignore
        }
    }

    static void sendOpenConfigScreen(ServerPlayer player) {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            NetworkManager.sendToPlayer(player, OPEN_CONFIG_SCREEN_ID, server(buf, player));
        } catch (Exception e) {
            // ignore
        }
    }

    static void sendHitboxToTracking(LivingEntity entity, AABB hitbox, long durationMs) {
        if (entity.level().isClientSide) return;
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeInt(1);
            buf.writeDouble(hitbox.minX);
            buf.writeDouble(hitbox.minY);
            buf.writeDouble(hitbox.minZ);
            buf.writeDouble(hitbox.maxX);
            buf.writeDouble(hitbox.maxY);
            buf.writeDouble(hitbox.maxZ);
            buf.writeLong(durationMs);
            for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
                NetworkManager.sendToPlayer(player, HITBOX_PACKET_ID, serverCopy(buf, player));
            }
            buf.release();
        } catch (Exception e) {
            // ignore
        }
    }

    static void sendHitboxToClient(ServerPlayer player, AABB hitbox, long durationMs) {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeInt(1);
            buf.writeDouble(hitbox.minX);
            buf.writeDouble(hitbox.minY);
            buf.writeDouble(hitbox.minZ);
            buf.writeDouble(hitbox.maxX);
            buf.writeDouble(hitbox.maxY);
            buf.writeDouble(hitbox.maxZ);
            buf.writeLong(durationMs);
            NetworkManager.sendToPlayer(player, HITBOX_PACKET_ID, server(buf, player));
        } catch (Exception e) {
            // Handle error
        }
    }

    static void sendToPlayer(ServerPlayer player, String movesetId) {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeBoolean(movesetId != null);
            if (movesetId != null) {
                buf.writeUtf(movesetId);
            }
            NetworkManager.sendToPlayer(player, SYNC_BREATHING_STYLE, server(buf, player));
        } catch (Exception e) {
            // Handle error
        }
    }

    static void sendToTracking(ServerPlayer player, String movesetId) {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeBoolean(movesetId != null);
            if (movesetId != null) {
                buf.writeUtf(movesetId);
            }

            player.server.getPlayerList().getPlayers().stream()
                    .filter(p -> p.level() == player.level())
                    .forEach(p -> NetworkManager.sendToPlayer(p, SYNC_BREATHING_STYLE, serverCopy(buf, p)));
        } catch (Exception e) {
            // Handle error
        }
    }

    static void handleCqcPresetUpdate(ServerPlayer player, String slotName, int wheelIndex, String moveId) {
        CqcPresetData.Slot slot = CqcPresetData.Slot.fromWireName(slotName);
        boolean changed = PlayerDataProvider.getData(player).getCqcPresetData().setSlotForPlayer(player, slot, wheelIndex, moveId);
        if (!changed) {
            player.displayClientMessage(Component.literal("Invalid CQC move selection.")
                    .withStyle(style -> style.withColor(0xFF5555)), true);
            return;
        }
        PlayerDataStorage.savePlayerData(player);
        sendCqcPresetSync(player);
    }

    static void handleCqcStanceUpdate(ServerPlayer player, int stanceIndex) {
        boolean changed = PlayerDataProvider.getData(player).getCqcPresetData().setStanceIndex(stanceIndex);
        if (!changed) return;
        PlayerDataStorage.savePlayerData(player);
        sendCqcPresetSync(player);
    }

    static void handleCqcActivePresetUpdate(ServerPlayer player, int presetIndex) {
        boolean changed = PlayerDataProvider.getData(player).getCqcPresetData().setActivePresetIndex(presetIndex);
        if (!changed) return;
        PlayerDataStorage.savePlayerData(player);
        sendCqcPresetSync(player);
    }

    static void handleCqcPresetReset(ServerPlayer player) {
        PlayerDataProvider.getData(player).getCqcPresetData().resetActivePreset();
        PlayerDataStorage.savePlayerData(player);
        sendCqcPresetSync(player);
    }

    static void handleCqcFollowupUpdate(ServerPlayer player, String baseMoveId, String followupMoveId) {
        boolean changed = PlayerDataProvider.getData(player).getCqcPresetData().setFollowupForPlayer(player, baseMoveId, followupMoveId);
        if (!changed) {
            player.displayClientMessage(Component.literal("Invalid CQC followup selection.")
                    .withStyle(style -> style.withColor(0xFF5555)), true);
            return;
        }
        PlayerDataStorage.savePlayerData(player);
        sendCqcPresetSync(player);
    }

    static void sendCqcPresetSync(ServerPlayer player) {
        try {
            CqcPresetData preset = PlayerDataProvider.getData(player).getCqcPresetData();
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeInt(preset.getActivePresetIndex());
            for (int presetIndex = 0; presetIndex < CqcPresetData.PRESET_COUNT; presetIndex++) {
                CqcPresetData.Preset presetData = preset.getPresetCopy(presetIndex);
                buf.writeUtf(presetData.leftClickMove());
                buf.writeUtf(presetData.rightClickMove());
                buf.writeUtf(presetData.crouchRightClickMove());
                for (String moveId : presetData.wheelMovesCopy()) {
                    buf.writeUtf(moveId);
                }
                buf.writeInt(presetData.stanceIndex());
                Map<String, String> followups = presetData.followupsCopy();
                buf.writeInt(followups.size());
                for (Map.Entry<String, String> entry : followups.entrySet()) {
                    buf.writeUtf(entry.getKey());
                    buf.writeUtf(entry.getValue());
                }
            }
            NetworkManager.sendToPlayer(player, CQC_PRESET_SYNC_ID, server(buf, player));
        } catch (Exception e) {
            // Handle error
        }
    }

    static void requestCqcPresetUpdate(CqcPresetData.Slot slot, int wheelIndex, String moveId) {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeUtf(slot.name());
            buf.writeInt(wheelIndex);
            buf.writeUtf(moveId);
            NetworkManager.sendToServer(CQC_PRESET_UPDATE_ID, client(buf));
        } catch (Exception e) {
            // Handle error
        }
    }

    static void requestCqcStanceUpdate(int stanceIndex) {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeInt(stanceIndex);
            NetworkManager.sendToServer(CQC_STANCE_UPDATE_ID, client(buf));
        } catch (Exception e) {
            // Handle error
        }
    }

    static void requestCqcActivePresetUpdate(int presetIndex) {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeInt(presetIndex);
            NetworkManager.sendToServer(CQC_ACTIVE_PRESET_UPDATE_ID, client(buf));
        } catch (Exception e) {
            // Handle error
        }
    }

    static void requestCqcPresetReset() {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            NetworkManager.sendToServer(CQC_PRESET_RESET_ID, client(buf));
        } catch (Exception e) {
            // Handle error
        }
    }

    static void requestCqcFollowupUpdate(String baseMoveId, String followupMoveId) {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeUtf(baseMoveId);
            buf.writeUtf(followupMoveId);
            NetworkManager.sendToServer(CQC_FOLLOWUP_UPDATE_ID, client(buf));
        } catch (Exception e) {
            // Handle error
        }
    }

    /**
     * Request the server to set the player's active moveset. Works for both breathing styles
     * and demon arts — the server routes to the right field based on the moveset id.
     */
    static void requestMovesetChange(String movesetId) {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeBoolean(movesetId != null);
            if (movesetId != null) {
                buf.writeUtf(movesetId);
            }
            NetworkManager.sendToServer(REQUEST_STYLE_CHANGE, client(buf));
        } catch (Exception e) {
            // Handle error
        }
    }

    static void sendBlockStart() {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            NetworkManager.sendToServer(BLOCK_START_ID, client(buf));
        } catch (Exception e) {
            // Handle error
        }
    }

    static void sendBlockStop() {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            NetworkManager.sendToServer(BLOCK_STOP_ID, client(buf));
        } catch (Exception e) {
            // Handle error
        }
    }

    static void sendParry() {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            NetworkManager.sendToServer(PARRY_ID, client(buf));
        } catch (Exception e) {
            // Handle error
        }
    }

    static void sendToPlayer(Object packet, ServerPlayer player) {
        ResourceLocation id = PACKET_IDS.get(packet.getClass());
        if (id != null) {
            try {
                FriendlyByteBuf buf = encodePacket(packet);
                NetworkManager.sendToPlayer(player, id, server(buf, player));
            } catch (Exception e) {
                // Handle error
            }
        }
    }

    static void sendToServer(Object packet) {
        ResourceLocation id = PACKET_IDS.get(packet.getClass());
        if (id != null) {
            try {
                FriendlyByteBuf buf = encodePacket(packet);
                NetworkManager.sendToServer(id, client(buf));
            } catch (Exception e) {
                // Handle error
            }
        }
    }

    /**
     * Broadcasts a PlayerAnimationPacket only to players within 256 blocks of the animated player.
     * Avoids sending to the entire dimension like the old implementation did.
     */
    static void broadcastPlayerAnimation(ServerPlayer animatedPlayer, PlayerAnimationPacket packet) {
        ResourceLocation id = PACKET_IDS.get(packet.getClass());
        if (id == null) return;
        try {
            FriendlyByteBuf buf = encodePacket(packet);
            animatedPlayer.server.getPlayerList().getPlayers().stream()
                    .filter(p -> p.level() == animatedPlayer.level()
                            && p.distanceToSqr(animatedPlayer) <= 256.0 * 256.0)
                    .forEach(p -> {
                        try {
                            NetworkManager.sendToPlayer(p, id, serverCopy(buf, p));
                        } catch (Exception ignored) {}
                    });
            buf.release();
        } catch (Exception ignored) {}
    }

    static void sendToAll(Object packet, MinecraftServer server) {
        ResourceLocation id = PACKET_IDS.get(packet.getClass());
        if (id != null && server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                sendToPlayer(packet, player);
            }
        }
    }

    static void sendDemonSync(ServerPlayer player, int bloodPoints, int halfBloodPoints, boolean isDemon) {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeInt(bloodPoints);
            buf.writeInt(halfBloodPoints);
            buf.writeBoolean(isDemon);
            NetworkManager.sendToPlayer(player, DEMON_SYNC_ID, server(buf, player));
        } catch (Exception e) {
            // Handle error
        }
    }

    static FriendlyByteBuf encodePacket(Object packet) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        if (packet instanceof DoubleJumpPacket p) {
            p.toBytes(buf);
        } else if (packet instanceof BreathingMovePacket p) {
            p.toBytes(buf);
        } else if (packet instanceof BreathingEffectPacket p) {
            p.toBytes(buf);
        } else if (packet instanceof SyncBreathPacket p) {
            p.toBytes(buf);
        } else if (packet instanceof StaminaSyncPacket p) {
            p.toBytes(buf);
        } else if (packet instanceof StanceSyncPacket p) {
            p.toBytes(buf);
        } else if (packet instanceof PlayerAnimationPacket p) {
            p.toBytes(buf);
        } else if (packet instanceof MovementInputPacket p) {
            p.toBytes(buf);
        } else if (packet instanceof MovementInputSyncPacket p) {
            p.toBytes(buf);
        } else if (packet instanceof MoveHotkeyPacket p) {
            p.toBytes(buf);
        } else if (packet instanceof DemonMovePacket p) {
            p.toBytes(buf);
        } else if (packet instanceof MovesetConfigSyncPacket p) {
            p.toBytes(buf);
        } else if (packet instanceof DemonSyncPacket p) {
            p.toBytes(buf);
        } else if (packet instanceof TriggerShaderPacket p) {
            p.toBytes(buf);
        } else if (packet instanceof MistClonesPacket p) {
            p.toBytes(buf);
        } else if (packet instanceof CloneRingPacket p) {
            p.toBytes(buf);
        } else if (packet instanceof AfterimagePacket p) {
            p.toBytes(buf);
        } else if (packet instanceof SheathInputPacket p) {
            p.toBytes(buf);
        } else if (packet instanceof SheathConfigPacket p) {
            p.toBytes(buf);
        } else if (packet instanceof DestructiveDeathStateSyncPacket p) {
            p.toBytes(buf);
        }

        return buf;
    }

    static void sendMistClones(LivingEntity caster, Vec3 center, float radius, int lifetimeTicks) {
        if (caster.level().isClientSide) return;
        if (!(caster.level() instanceof ServerLevel serverLevel)) return;
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeInt(caster.getId());
            buf.writeDouble(center.x);
            buf.writeDouble(center.y);
            buf.writeDouble(center.z);
            buf.writeFloat(radius);
            buf.writeInt(lifetimeTicks);
            serverLevel.getServer().getPlayerList().getPlayers().stream()
                    .filter(p -> p.level() == caster.level()
                            && p.distanceToSqr(caster) <= 256.0 * 256.0)
                    .forEach(p -> NetworkManager.sendToPlayer(p, MIST_CLONES_ID, serverCopy(buf, p)));
            buf.release();
        } catch (Exception e) {
            // ignore
        }
    }

    static void sendCloneRing(LivingEntity caster, CloneRingPacket packet) {
        if (caster.level().isClientSide) return;
        if (!(caster.level() instanceof ServerLevel serverLevel)) return;
        try {
            FriendlyByteBuf buf = encodePacket(packet);
            serverLevel.getServer().getPlayerList().getPlayers().stream()
                    .filter(p -> p.level() == caster.level()
                            && p.distanceToSqr(caster) <= 256.0 * 256.0)
                    .forEach(p -> NetworkManager.sendToPlayer(p, CLONE_RING_ID, serverCopy(buf, p)));
            buf.release();
        } catch (Exception ignored) {
        }
    }

    static void sendAfterimageTrail(LivingEntity entity, Vec3 from, Vec3 to, int lifetimeTicks, int copies, float alpha) {
        if (entity.level().isClientSide) return;
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;
        try {
            ResourceLocation id = PACKET_IDS.get(AfterimagePacket.class);
            if (id == null) return;

            FriendlyByteBuf buf = encodePacket(new AfterimagePacket(entity.getId(), from, to, lifetimeTicks, copies, alpha));
            serverLevel.getServer().getPlayerList().getPlayers().stream()
                    .filter(p -> p.level() == entity.level()
                            && p.distanceToSqr(entity) <= 256.0 * 256.0)
                    .forEach(p -> NetworkManager.sendToPlayer(p, id, serverCopy(buf, p)));
            buf.release();
        } catch (Exception ignored) {
        }
    }

    static void sendGunAnimation(Player player, String animName) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (player.level().isClientSide) return;
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            new GunAnimationPacket(player.getId(), animName).toBytes(buf);
            NetworkManager.sendToPlayer(serverPlayer, GUN_ANIMATION_ID, server(buf, serverPlayer));
            buf.release();
        } catch (Exception e) {
            // ignore
        }
    }

    static void sendSheathSync(ServerPlayer player, PlayerSheathData data) {
        SheathSyncPacket packet = new SheathSyncPacket(player, data);
        ResourceLocation id = PACKET_IDS.get(SheathSyncPacket.class);
        if (id == null) return;
        try {
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
            packet.toBytes(buf);
            player.server.getPlayerList().getPlayers().stream()
                    .filter(p -> p.level() == player.level() && p.distanceToSqr(player) <= 256.0 * 256.0)
                    .forEach(p -> NetworkManager.sendToPlayer(p, id, serverCopy(buf, p)));
            buf.release();
        } catch (Exception ignored) {
        }
    }

    static void cleanupPlayer(Player player) {
        SERVER_PLAYER_STATES.remove(player.getUUID());
    }

    static void clearServerPlayerStates() {
        SERVER_PLAYER_STATES.clear();
    }
}
