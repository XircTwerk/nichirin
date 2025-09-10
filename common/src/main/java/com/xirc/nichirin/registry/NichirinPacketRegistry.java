package com.xirc.nichirin.registry;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.network.*;
import com.xirc.nichirin.common.system.blocking.KatanaBlock;
import com.xirc.nichirin.common.data.*;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import io.netty.buffer.Unpooled;

import java.util.HashMap;
import java.util.Map;

/**
 * FIXED: Architectury networking with version compatibility and fallback
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

    // Packet class mappings
    Map<Class<?>, ResourceLocation> PACKET_IDS = new HashMap<>();

    static void init() {
        BreathOfNichirin.LOGGER.info("Initializing Architectury packet registry...");

        // Map packet classes to IDs
        PACKET_IDS.put(DoubleJumpPacket.class, DOUBLE_JUMP_ID);
        PACKET_IDS.put(BreathingMovePacket.class, BREATHING_MOVE_ID);
        PACKET_IDS.put(BreathingEffectPacket.class, BREATHING_EFFECT_ID);
        PACKET_IDS.put(SyncBreathPacket.class, SYNC_BREATH_ID);
        PACKET_IDS.put(StaminaSyncPacket.class, SYNC_STAMINA_ID);
        PACKET_IDS.put(StanceSyncPacket.class, SYNC_STANCE_ID);
        PACKET_IDS.put(PlayerAnimationPacket.class, PLAYER_ANIMATION_ID);
        PACKET_IDS.put(MovementInputPacket.class, MOVEMENT_INPUT_ID);
        PACKET_IDS.put(MovementInputSyncPacket.class, MOVEMENT_INPUT_SYNC_ID);
        PACKET_IDS.put(ComboCounterPacket.class, COMBO_COUNTER_ID);

        // Register packets with error handling
        registerPackets();
    }

    private static void registerPackets() {
        BreathOfNichirin.LOGGER.info("Registering packets with Architectury...");

        try {
            // Register C2S packets (these work fine)
            registerC2SPackets();

            // Try to register S2C packets with fallback
            registerS2CPacketsWithFallback();

            BreathOfNichirin.LOGGER.info("Successfully registered all packets");

        } catch (Exception e) {
            BreathOfNichirin.LOGGER.error("Failed to register packets: {}", e.getMessage(), e);
            throw new RuntimeException("Packet registration failed", e);
        }
    }

    private static void registerC2SPackets() {
        BreathOfNichirin.LOGGER.info("Registering C2S packets...");

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

        // CRITICAL: Blocking packets
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, BLOCK_START_ID, (buf, context) -> {
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                context.queue(() -> {
                    KatanaBlock.startBlocking(serverPlayer);
                    BreathOfNichirin.LOGGER.debug("Started blocking for {}", serverPlayer.getName().getString());
                });
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, BLOCK_STOP_ID, (buf, context) -> {
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                context.queue(() -> {
                    KatanaBlock.stopBlocking(serverPlayer);
                    BreathOfNichirin.LOGGER.debug("Stopped blocking for {}", serverPlayer.getName().getString());
                });
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, PARRY_ID, (buf, context) -> {
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                context.queue(() -> {
                    KatanaBlock.attemptParry(serverPlayer);
                    BreathOfNichirin.LOGGER.debug("Parry attempt by {}", serverPlayer.getName().getString());
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

        BreathOfNichirin.LOGGER.info("C2S packets registered successfully");
    }

    private static void registerS2CPacketsWithFallback() {
        BreathOfNichirin.LOGGER.info("Attempting to register S2C packets...");

        try {
            // Try the standard Architectury way first
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

            // FIXED: Add the missing SYNC_BREATHING_STYLE S2C packet!
            NetworkManager.registerReceiver(NetworkManager.Side.S2C, SYNC_BREATHING_STYLE, (buf, context) -> {
                String movesetId = buf.readBoolean() ? buf.readUtf() : null;
                context.queue(() -> {
                    Player player = context.getPlayer();
                    if (player != null) {
                        PlayerDataProvider.getBreathingStyleData(player).setMovesetId(movesetId);
                        BreathOfNichirin.LOGGER.debug("Client received breathing style sync: {}", movesetId);
                    }
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
                    System.out.println("DEBUG: Processing " + hitboxesToAdd.size() + " hitboxes on client");

                    for (AABB hitbox : hitboxesToAdd) {
                        com.xirc.nichirin.client.renderer.effects.AttackHitboxRenderer.addHitbox(hitbox, finalDuration, false);
                        System.out.println("DEBUG: Added hitbox to renderer: " + hitbox);
                    }
                });
            });

            BreathOfNichirin.LOGGER.info("S2C packets registered successfully");

        } catch (NoSuchMethodError e) {
            BreathOfNichirin.LOGGER.warn("S2C packet registration failed due to Architectury version incompatibility: {}", e.getMessage());
            BreathOfNichirin.LOGGER.warn("S2C packets disabled - some sync features may not work properly");
            BreathOfNichirin.LOGGER.warn("Consider downgrading to architectury_api_version = 9.1.12 for full compatibility");
        } catch (Exception e) {
            BreathOfNichirin.LOGGER.error("Unexpected error during S2C packet registration: {}", e.getMessage(), e);
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
            System.out.println("DEBUG: Sent hitbox packet to client: " + hitbox);
        } catch (Exception e) {
            BreathOfNichirin.LOGGER.error("Failed to send hitbox packet: {}", e.getMessage());
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
            BreathOfNichirin.LOGGER.debug("Sent breathing style sync to {}: {}", player.getName().getString(), movesetId);
        } catch (Exception e) {
            BreathOfNichirin.LOGGER.error("Failed to send breathing style sync: {}", e.getMessage());
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
            BreathOfNichirin.LOGGER.error("Failed to send breathing style sync to tracking: {}", e.getMessage());
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
            BreathOfNichirin.LOGGER.debug("Requested style change: {}", movesetId);
        } catch (Exception e) {
            BreathOfNichirin.LOGGER.error("Failed to request style change: {}", e.getMessage());
        }
    }

    // Blocking-specific methods for easy access
    static void sendBlockStart() {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            NetworkManager.sendToServer(BLOCK_START_ID, buf);
            BreathOfNichirin.LOGGER.debug("Sent block start packet");
        } catch (Exception e) {
            BreathOfNichirin.LOGGER.error("Failed to send block start packet: {}", e.getMessage());
        }
    }

    static void sendBlockStop() {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            NetworkManager.sendToServer(BLOCK_STOP_ID, buf);
            BreathOfNichirin.LOGGER.debug("Sent block stop packet");
        } catch (Exception e) {
            BreathOfNichirin.LOGGER.error("Failed to send block stop packet: {}", e.getMessage());
        }
    }

    static void sendParry() {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            NetworkManager.sendToServer(PARRY_ID, buf);
            BreathOfNichirin.LOGGER.debug("Sent parry packet");
        } catch (Exception e) {
            BreathOfNichirin.LOGGER.error("Failed to send parry packet: {}", e.getMessage());
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
                BreathOfNichirin.LOGGER.error("Failed to send packet {} to player {}: {}",
                        packet.getClass().getSimpleName(), player.getName().getString(), e.getMessage());
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
                BreathOfNichirin.LOGGER.error("Failed to send packet {} to server: {}",
                        packet.getClass().getSimpleName(), e.getMessage());
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
        }

        return buf;
    }

    // Handler method for breathing style change requests (copying logic from original BreathingStyleSyncPacket)
    private static void handleStyleChangeRequestFromOriginalPacket(ServerPlayer player, String movesetId) {
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
            BreathOfNichirin.LOGGER.error("Failed to handle style change request: {}", e.getMessage());
        }
    }

    private static String formatStyleName(String styleId) {
        String[] parts = styleId.split("_");
        StringBuilder formatted = new StringBuilder();
        for (String part : parts) {
            if (formatted.length() > 0) formatted.append(" ");
            formatted.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
        }
        return formatted.toString();
    }
}