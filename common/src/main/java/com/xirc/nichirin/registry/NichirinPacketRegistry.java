package com.xirc.nichirin.registry;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.network.c2s.*;
import com.xirc.nichirin.common.network.s2c.*;
import com.xirc.nichirin.common.network.util.MovesetSyncPacket;
import com.xirc.nichirin.common.system.blocking.KatanaBlock;
import com.xirc.nichirin.common.data.*;
import com.xirc.nichirin.common.util.MultiplayerInputHandler;
import com.xirc.nichirin.common.system.DemonComponent;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.InteractionHand;
import io.netty.buffer.Unpooled;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * UPDATED: Architectury networking with mixin-based demon blood bar support
 */
public interface NichirinPacketRegistry {

    // Packet IDs
    ResourceLocation DOUBLE_JUMP_ID = new ResourceLocation(BreathOfNichirin.MOD_ID, "double_jump");
    ResourceLocation BREATHING_MOVE_ID = new ResourceLocation(BreathOfNichirin.MOD_ID, "breathing_move");
    ResourceLocation BREATHING_EFFECT_ID = new ResourceLocation(BreathOfNichirin.MOD_ID, "breathing_effect");
    ResourceLocation SYNC_BREATH_ID = new ResourceLocation(BreathOfNichirin.MOD_ID, "sync_breath");
    ResourceLocation SYNC_STAMINA_ID = new ResourceLocation(BreathOfNichirin.MOD_ID, "sync_stamina");
    ResourceLocation SYNC_STANCE_ID = new ResourceLocation(BreathOfNichirin.MOD_ID, "sync_stance");
    ResourceLocation BLOCK_START_ID = new ResourceLocation(BreathOfNichirin.MOD_ID, "block_start");
    ResourceLocation BLOCK_STOP_ID = new ResourceLocation(BreathOfNichirin.MOD_ID, "block_stop");
    ResourceLocation PARRY_ID = new ResourceLocation(BreathOfNichirin.MOD_ID, "parry");
    ResourceLocation PLAYER_ANIMATION_ID = new ResourceLocation(BreathOfNichirin.MOD_ID, "player_animation");
    ResourceLocation MOVEMENT_INPUT_ID = new ResourceLocation(BreathOfNichirin.MOD_ID, "movement_input");
    ResourceLocation MOVEMENT_INPUT_SYNC_ID = new ResourceLocation(BreathOfNichirin.MOD_ID, "movement_input_sync");
    ResourceLocation SYNC_BREATHING_STYLE = new ResourceLocation(BreathOfNichirin.MOD_ID, "sync_breathing_style");
    ResourceLocation REQUEST_STYLE_CHANGE = new ResourceLocation(BreathOfNichirin.MOD_ID, "request_style_change");
    ResourceLocation COMBO_COUNTER_ID = new ResourceLocation(BreathOfNichirin.MOD_ID, "combo_counter");
    ResourceLocation HITBOX_PACKET_ID = new ResourceLocation(BreathOfNichirin.MOD_ID, "hitbox_data");
    ResourceLocation MOVE_HOTKEY_ID = new ResourceLocation(BreathOfNichirin.MOD_ID, "move_hotkey");
    ResourceLocation SYNC_PROGRESSION_ID = new ResourceLocation(BreathOfNichirin.MOD_ID, "sync_progression");
    ResourceLocation DEMON_MOVE_ID = new ResourceLocation(BreathOfNichirin.MOD_ID, "demon_move");
    ResourceLocation MOVESET_CONFIG_ID = new ResourceLocation(BreathOfNichirin.MOD_ID, "moveset_config_sync");
    ResourceLocation DEMON_SYNC_ID = new ResourceLocation(BreathOfNichirin.MOD_ID, "demon_sync");
    ResourceLocation DEMON_INPUT_ID = new ResourceLocation(BreathOfNichirin.MOD_ID, "demon_input");
    ResourceLocation ATTACK_WHEEL_STATE_ID = new ResourceLocation(BreathOfNichirin.MOD_ID, "attack_wheel_state");
    ResourceLocation KATANA_INPUT_ID = new ResourceLocation(BreathOfNichirin.MOD_ID, "katana_input");

    // Packet class mappings
    Map<Class<?>, ResourceLocation> PACKET_IDS = new HashMap<>();

    // Server-side state tracking for MultiplayerInputHandler
    Map<java.util.UUID, MultiplayerInputHandler.PlayerInputState> SERVER_PLAYER_STATES = new java.util.concurrent.ConcurrentHashMap<>();

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

        registerPackets();
    }

    static void registerPackets() {
        try {
            registerC2SPackets();
            registerS2CPacketsWithFallback();
        } catch (Exception e) {
            throw new RuntimeException("Packet registration failed", e);
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
                    if (shouldBlockInputsServer(serverPlayer)) {
                        return;
                    }
                    executeKatanaInput(serverPlayer, inputType);
                });
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, DEMON_INPUT_ID, (buf, context) -> {
            String inputTypeName = buf.readUtf();
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                context.queue(() -> {
                    try {
                        MultiplayerInputHandler.InputType inputType = MultiplayerInputHandler.InputType.valueOf(inputTypeName);
                        if (shouldBlockInputsServer(serverPlayer)) {
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
                context.queue(() -> KatanaBlock.startBlocking(serverPlayer));
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, BLOCK_STOP_ID, (buf, context) -> {
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                context.queue(() -> KatanaBlock.stopBlocking(serverPlayer));
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, PARRY_ID, (buf, context) -> {
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                context.queue(() -> KatanaBlock.attemptParry(serverPlayer));
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
    }

    static void registerS2CPacketsWithFallback() {
        try {
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
                            com.xirc.nichirin.common.attack.moveset.AbstractMoveset moveset =
                                    com.xirc.nichirin.registry.MovesetRegistry.getMoveset(movesetId);

                            if (moveset != null) {
                                if (moveset.isBreathingMoveset()) {
                                    data.setBreathingMovesetId(movesetId);
                                } else if (moveset.isDemonMoveset()) {
                                    data.setDemonMovesetId(movesetId);
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
                    com.xirc.nichirin.client.data.ClientProgressionCache.setUnlockedStyles(unlockedStyles);
                });
            });

            // UPDATED: Use DemonComponent instead of DemonBloodGui
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
                java.util.List<AABB> hitboxesToAdd = new java.util.ArrayList<>();
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
                        com.xirc.nichirin.client.renderer.effects.AttackHitboxRenderer.addHitbox(hitbox, finalDuration, false);
                    }
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

    static boolean shouldBlockInputsServer(Player player) {
        if (player.level().isClientSide) return false;

        if (player.hasEffect(com.xirc.nichirin.registry.NichirinEffectRegistry.STUNNED.get())) {
            return true;
        }

        if (player.hasEffect(com.xirc.nichirin.registry.NichirinEffectRegistry.BLOCKING.get())) {
            return true;
        }

        MultiplayerInputHandler.PlayerInputState state = SERVER_PLAYER_STATES.get(player.getUUID());
        if (state == null) return false;

        long currentTime = player.level().getGameTime();
        return state.shouldBlockInput(currentTime);
    }

    static void executeKatanaInput(ServerPlayer player, MultiplayerInputHandler.InputType inputType) {
        var mainHand = player.getMainHandItem();
        if (!(mainHand.getItem() instanceof com.xirc.nichirin.common.item.katana.SimpleKatana katana)) {
            return;
        }

        switch (inputType) {
            case LEFT_CLICK -> katana.performAttack(player);
            case RIGHT_CLICK -> katana.use(player.level(), player, InteractionHand.MAIN_HAND);
            case RIGHT_CLICK_CROUCH -> {
                boolean wasCrouching = player.isShiftKeyDown();
                player.setShiftKeyDown(true);
                katana.use(player.level(), player, InteractionHand.MAIN_HAND);
                player.setShiftKeyDown(wasCrouching);
            }
        }

        MultiplayerInputHandler.PlayerInputState state = getOrCreatePlayerState(player);
        state.inputBlocked = true;
        state.blockUntilTime = player.level().getGameTime() + 40;
    }

    static void handleDemonInput(ServerPlayer player, MultiplayerInputHandler.InputType inputType) {
        if (player.hasEffect(com.xirc.nichirin.registry.NichirinEffectRegistry.STUNNED.get())) {
            return;
        }

        if (player.hasEffect(com.xirc.nichirin.registry.NichirinEffectRegistry.BLOCKING.get())) {
            return;
        }

        if (!com.xirc.nichirin.common.data.MovesetHelper.hasDemonMoveset(player)) {
            return;
        }

        com.xirc.nichirin.common.attack.moveset.AbstractMoveset moveset = com.xirc.nichirin.common.data.MovesetHelper.getDemonMoveset(player);
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
            if (movesetId != null && !MovesetRegistry.isRegistered(movesetId)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§cInvalid breathing style: " + movesetId
                ));
                return;
            }

            if (movesetId != null && !ProgressionHelper.isStyleUnlocked(player, movesetId)) {
                String requirement = ProgressionHelper.getUnlockRequirement(movesetId);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§cYou haven't unlocked this breathing style! §fRequirement: §e" + requirement
                ));
                return;
            }

            PlayerDataProvider.updateAndSync(player, movesetId);

            if (movesetId != null) {
                String styleName = formatStyleName(movesetId);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§aSwitched to " + styleName + "."
                ));
            } else {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§7Cleared breathing style."
                ));
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
            NetworkManager.sendToPlayer(player, HITBOX_PACKET_ID, buf);
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
            NetworkManager.sendToPlayer(player, SYNC_BREATHING_STYLE, buf);
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
                    .forEach(p -> NetworkManager.sendToPlayer(p, SYNC_BREATHING_STYLE, buf));
        } catch (Exception e) {
            // Handle error
        }
    }

    static void requestStyleChange(String movesetId) {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeBoolean(movesetId != null);
            if (movesetId != null) {
                buf.writeUtf(movesetId);
            }
            NetworkManager.sendToServer(REQUEST_STYLE_CHANGE, buf);
        } catch (Exception e) {
            // Handle error
        }
    }

    static void sendBlockStart() {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            NetworkManager.sendToServer(BLOCK_START_ID, buf);
        } catch (Exception e) {
            // Handle error
        }
    }

    static void sendBlockStop() {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            NetworkManager.sendToServer(BLOCK_STOP_ID, buf);
        } catch (Exception e) {
            // Handle error
        }
    }

    static void sendParry() {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            NetworkManager.sendToServer(PARRY_ID, buf);
        } catch (Exception e) {
            // Handle error
        }
    }

    static void sendToPlayer(Object packet, ServerPlayer player) {
        ResourceLocation id = PACKET_IDS.get(packet.getClass());
        if (id != null) {
            try {
                FriendlyByteBuf buf = encodePacket(packet);
                NetworkManager.sendToPlayer(player, id, buf);
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
                NetworkManager.sendToServer(id, buf);
            } catch (Exception e) {
                // Handle error
            }
        }
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
            NetworkManager.sendToPlayer(player, DEMON_SYNC_ID, buf);
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
        }

        return buf;
    }

    static void cleanupPlayer(Player player) {
        SERVER_PLAYER_STATES.remove(player.getUUID());
    }
}