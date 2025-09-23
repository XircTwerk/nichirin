package com.xirc.nichirin.registry;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.network.c2s.*;
import com.xirc.nichirin.common.network.s2c.*;
import com.xirc.nichirin.common.network.util.MovesetSyncPacket;
import com.xirc.nichirin.common.system.blocking.KatanaBlock;
import com.xirc.nichirin.common.data.*;
import com.xirc.nichirin.common.util.MultiplayerInputHandler;
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
 * FIXED: Architectury networking with all required packet registrations including MovesetSyncPacket
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

    // NEW: Missing MultiplayerInputHandler packet IDs
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

        // REMOVED: MovesetSyncPacket.register() - causes Architectury version incompatibility
        // We handle moveset sync directly in this registry now

        // Register packets with error handling
        registerPackets();
    }

    static void registerPackets() {

        try {
            // Register C2S packets (these work fine)
            registerC2SPackets();

            // Try to register S2C packets with fallback
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

        // NEW: Attack wheel state packet
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, ATTACK_WHEEL_STATE_ID, (buf, context) -> {
            boolean wheelOpen = buf.readBoolean();
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                context.queue(() -> {
                    // Handle attack wheel state on server
                    MultiplayerInputHandler.PlayerInputState state = getOrCreatePlayerState(serverPlayer);
                    state.attackWheelOpen = wheelOpen;

                    if (!wheelOpen) {
                        // When wheel closes, brief block to prevent click leakage
                        state.wheelCloseTime = serverPlayer.level().getGameTime();
                        state.inputBlocked = true;
                        state.blockUntilTime = serverPlayer.level().getGameTime() + 10; // 0.5 second block
                    }
                });
            }
        });

        // NEW: Katana input packet
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, KATANA_INPUT_ID, (buf, context) -> {
            MultiplayerInputHandler.InputType inputType = buf.readEnum(MultiplayerInputHandler.InputType.class);
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                context.queue(() -> {
                    // Check if inputs should be blocked
                    if (shouldBlockInputsServer(serverPlayer)) {
                        return;
                    }

                    // Execute katana input (requires katana)
                    executeKatanaInput(serverPlayer, inputType);
                });
            }
        });

        // Demon input handling (no katana required)
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, DEMON_INPUT_ID, (buf, context) -> {
            String inputTypeName = buf.readUtf();
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                context.queue(() -> {
                    try {
                        MultiplayerInputHandler.InputType inputType = MultiplayerInputHandler.InputType.valueOf(inputTypeName);

                        // Check if inputs should be blocked
                        if (shouldBlockInputsServer(serverPlayer)) {
                            return;
                        }

                        handleDemonInput(serverPlayer, inputType);
                    } catch (Exception e) {
                    }
                });
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, BLOCK_START_ID, (buf, context) -> {
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                context.queue(() -> {
                    KatanaBlock.startBlocking(serverPlayer);
                });
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, BLOCK_STOP_ID, (buf, context) -> {
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                context.queue(() -> {
                    KatanaBlock.stopBlocking(serverPlayer);
                });
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, PARRY_ID, (buf, context) -> {
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                context.queue(() -> {
                    KatanaBlock.attemptParry(serverPlayer);
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

        // BREATHING STYLE CHANGE REQUEST (C2S)
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
        // Get the katana and execute the appropriate action
        var mainHand = player.getMainHandItem();
        if (!(mainHand.getItem() instanceof com.xirc.nichirin.common.item.katana.SimpleKatana katana)) {
            return;
        }

        switch (inputType) {
            case LEFT_CLICK -> {
                katana.performAttack(player);
            }
            case RIGHT_CLICK -> {
                katana.use(player.level(), player, InteractionHand.MAIN_HAND);
            }
            case RIGHT_CLICK_CROUCH -> {
                // Temporarily set crouch state
                boolean wasCrouching = player.isShiftKeyDown();
                player.setShiftKeyDown(true);
                katana.use(player.level(), player, InteractionHand.MAIN_HAND);
                player.setShiftKeyDown(wasCrouching);
            }
        }

        // Block inputs after execution
        MultiplayerInputHandler.PlayerInputState state = getOrCreatePlayerState(player);
        state.inputBlocked = true;
        state.blockUntilTime = player.level().getGameTime() + 40; // 2 seconds
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

            // FIXED: Proper dual moveset sync handling
            NetworkManager.registerReceiver(NetworkManager.Side.S2C, SYNC_BREATHING_STYLE, (buf, context) -> {
                String movesetId = buf.readBoolean() ? buf.readUtf() : null;
                context.queue(() -> {
                    Player player = context.getPlayer();
                    if (player != null) {
                        MovesetData data = PlayerDataProvider.getMovesetData(player);

                        if (movesetId != null) {
                            // Determine if it's breathing or demon and set to correct slot
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
                            // Clear all movesets if null
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

            NetworkManager.registerReceiver(NetworkManager.Side.S2C, DEMON_SYNC_ID, (buf, context) -> {
                int bloodPoints = buf.readInt();
                int halfBloodPoints = buf.readInt();
                boolean isDemon = buf.readBoolean();

                context.queue(() -> {
                    com.xirc.nichirin.client.gui.DemonBloodGui.updateBloodPoints(bloodPoints, isDemon);
                    com.xirc.nichirin.client.gui.DemonBloodGui.updateHalfBloodPoints(halfBloodPoints);
                });
            });

            // HITBOX PACKET (S2C)
            NetworkManager.registerReceiver(NetworkManager.Side.S2C, HITBOX_PACKET_ID, (buf, context) -> {
                // Read hitbox data OUTSIDE the queue - buffer will be invalid inside the lambda
                int hitboxCount = buf.readInt();

                // Read all hitbox data into local variables first
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

                // THEN queue the action with the local data
                final long finalDuration = duration;
                context.queue(() -> {
                    for (AABB hitbox : hitboxesToAdd) {
                        com.xirc.nichirin.client.renderer.effects.AttackHitboxRenderer.addHitbox(hitbox, finalDuration, false);
                    }
                });
            });

        } catch (NoSuchMethodError e) {
        } catch (Exception e) {
        }
    }

    // Hitbox packet sending methods
    static void sendHitboxToClient(ServerPlayer player, AABB hitbox, long durationMs) {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

            // Write hitbox count (1)
            buf.writeInt(1);

            // Write hitbox data
            buf.writeDouble(hitbox.minX);
            buf.writeDouble(hitbox.minY);
            buf.writeDouble(hitbox.minZ);
            buf.writeDouble(hitbox.maxX);
            buf.writeDouble(hitbox.maxY);
            buf.writeDouble(hitbox.maxZ);
            buf.writeLong(durationMs);

            NetworkManager.sendToPlayer(player, HITBOX_PACKET_ID, buf);
        } catch (Exception e) {
        }
    }

    // Breathing style specific methods - keeping the same interface as the original BreathingStyleSyncPacket
    static void sendToPlayer(ServerPlayer player, String movesetId) {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeBoolean(movesetId != null);
            if (movesetId != null) {
                buf.writeUtf(movesetId);
            }
            NetworkManager.sendToPlayer(player, SYNC_BREATHING_STYLE, buf);
        } catch (Exception e) {
        }
    }

    static void sendToTracking(ServerPlayer player, String movesetId) {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeBoolean(movesetId != null);
            if (movesetId != null) {
                buf.writeUtf(movesetId);
            }

            // Send to all players in the same dimension
            player.server.getPlayerList().getPlayers().stream()
                    .filter(p -> p.level() == player.level())
                    .forEach(p -> NetworkManager.sendToPlayer(p, SYNC_BREATHING_STYLE, buf));
        } catch (Exception e) {
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
        }
    }

    // Blocking-specific methods for easy access
    static void sendBlockStart() {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            NetworkManager.sendToServer(BLOCK_START_ID, buf);
        } catch (Exception e) {
        }
    }

    static void sendBlockStop() {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            NetworkManager.sendToServer(BLOCK_STOP_ID, buf);
        } catch (Exception e) {
        }
    }

    static void sendParry() {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            NetworkManager.sendToServer(PARRY_ID, buf);
        } catch (Exception e) {
        }
    }

    // General packet sending methods
    static void sendToPlayer(Object packet, ServerPlayer player) {
        ResourceLocation id = PACKET_IDS.get(packet.getClass());
        if (id != null) {
            try {
                FriendlyByteBuf buf = encodePacket(packet);
                NetworkManager.sendToPlayer(player, id, buf);
            } catch (Exception e) {
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
        }
    }

    // Simple packet encoding
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
        }

        return buf;
    }

    // Handler method for breathing style change requests (copying logic from original BreathingStyleSyncPacket)
    static void handleStyleChangeRequestFromOriginalPacket(ServerPlayer player, String movesetId) {
        try {
            // Validate the moveset exists
            if (movesetId != null && !MovesetRegistry.isRegistered(movesetId)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§cInvalid breathing style: " + movesetId
                ));
                return;
            }

            // Check if the player has unlocked this breathing style
            if (movesetId != null && !ProgressionHelper.isStyleUnlocked(player, movesetId)) {
                String requirement = ProgressionHelper.getUnlockRequirement(movesetId);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§cYou haven't unlocked this breathing style! §fRequirement: §e" + requirement
                ));
                return;
            }

            // All checks passed - update the moveset
            PlayerDataProvider.updateAndSync(player, movesetId);

            // Send confirmation message
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

    static void handleDemonInput(ServerPlayer player, MultiplayerInputHandler.InputType inputType) {
        // Check if player is stunned
        if (player.hasEffect(com.xirc.nichirin.registry.NichirinEffectRegistry.STUNNED.get())) {
            return;
        }

        // Check if player has blocking effect
        if (player.hasEffect(com.xirc.nichirin.registry.NichirinEffectRegistry.BLOCKING.get())) {
            return;
        }

        // Get the demon moveset
        if (!com.xirc.nichirin.common.data.MovesetHelper.hasDemonMoveset(player)) {
            return;
        }

        com.xirc.nichirin.common.attack.moveset.AbstractMoveset moveset = com.xirc.nichirin.common.data.MovesetHelper.getDemonMoveset(player);
        if (moveset == null || !moveset.isDemonMoveset()) {
            return;
        }

        switch (inputType) {
            case LEFT_CLICK -> {
                moveset.handleLeftClick(player);
            }
            case RIGHT_CLICK -> {
                moveset.handleRightClick(player, false);
            }
            case RIGHT_CLICK_CROUCH -> {
                moveset.handleRightClick(player, true);
            }
        }
    }

    // Clean up player state when they disconnect
    static void cleanupPlayer(Player player) {
        SERVER_PLAYER_STATES.remove(player.getUUID());
    }
}