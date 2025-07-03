package com.xirc.nichirin.registry;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.network.*;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import io.netty.buffer.Unpooled;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class NichirinPacketRegistry {

    // Packet registry maps
    private static final Map<ResourceLocation, Function<FriendlyByteBuf, Object>> PACKET_DECODERS = new HashMap<>();
    private static final Map<Class<?>, ResourceLocation> PACKET_IDS = new HashMap<>();
    private static final Map<ResourceLocation, BiConsumer<Object, ServerPlayer>> SERVER_HANDLERS = new HashMap<>();
    private static final Map<ResourceLocation, Runnable> CLIENT_HANDLERS = new HashMap<>();

    // Packet IDs
    public static final ResourceLocation DOUBLE_JUMP_ID = new ResourceLocation(BreathOfNichirin.MOD_ID, "double_jump");
    public static final ResourceLocation BREATHING_MOVE_ID = new ResourceLocation(BreathOfNichirin.MOD_ID, "breathing_move");
    public static final ResourceLocation BREATHING_EFFECT_ID = new ResourceLocation(BreathOfNichirin.MOD_ID, "breathing_effect");
    public static final ResourceLocation SYNC_BREATH_ID = new ResourceLocation(BreathOfNichirin.MOD_ID, "sync_breath");
    public static final ResourceLocation SYNC_STAMINA_ID = new ResourceLocation(BreathOfNichirin.MOD_ID, "sync_stamina");

    // Initialize packet registry
    public static void init() {
        // Register Client to Server packets
        registerC2S(DOUBLE_JUMP_ID, DoubleJumpPacket.class, DoubleJumpPacket::new,
                (packet, player) -> ((DoubleJumpPacket) packet).handle(player));

        registerC2S(BREATHING_MOVE_ID, BreathingMovePacket.class, BreathingMovePacket::new,
                (packet, player) -> ((BreathingMovePacket) packet).handle(player));

        // Register Server to Client packets
        registerS2C(BREATHING_EFFECT_ID, BreathingEffectPacket.class, BreathingEffectPacket::new,
                () -> {
                    // Handle on client side
                });

        registerS2C(SYNC_BREATH_ID, SyncBreathPacket.class, SyncBreathPacket::new,
                () -> {
                    // Handle on client side
                });

        // Register Stamina Sync packet
        registerS2C(SYNC_STAMINA_ID, StaminaSyncPacket.class, StaminaSyncPacket::new,
                () -> {
                    // Handled in the packet's handleClient method
                });

        // Register with Architectury NetworkManager
        registerArchitecturyNetworking();
    }

    // Register all packets with Architectury's NetworkManager
    private static void registerArchitecturyNetworking() {
        // Register C2S packets
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

        // Register S2C packets
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
    }

    // Register Client to Server packet
    private static <T> void registerC2S(ResourceLocation id, Class<T> packetClass,
                                        Function<FriendlyByteBuf, T> decoder,
                                        BiConsumer<Object, ServerPlayer> handler) {
        PACKET_DECODERS.put(id, buf -> decoder.apply(buf));
        PACKET_IDS.put(packetClass, id);
        SERVER_HANDLERS.put(id, handler);
    }

    // Register Server to Client packet
    private static <T> void registerS2C(ResourceLocation id, Class<T> packetClass,
                                        Function<FriendlyByteBuf, T> decoder,
                                        Runnable handler) {
        PACKET_DECODERS.put(id, buf -> decoder.apply(buf));
        PACKET_IDS.put(packetClass, id);
        CLIENT_HANDLERS.put(id, handler);
    }

    // Handle incoming server packets
    public static void handleServerPacket(ResourceLocation id, FriendlyByteBuf buf, ServerPlayer player) {
        Function<FriendlyByteBuf, Object> decoder = PACKET_DECODERS.get(id);
        BiConsumer<Object, ServerPlayer> handler = SERVER_HANDLERS.get(id);

        if (decoder != null && handler != null) {
            Object packet = decoder.apply(buf);
            handler.accept(packet, player);
        }
    }

    // Handle incoming client packets
    public static void handleClientPacket(ResourceLocation id, FriendlyByteBuf buf) {
        Function<FriendlyByteBuf, Object> decoder = PACKET_DECODERS.get(id);
        Runnable handler = CLIENT_HANDLERS.get(id);

        if (decoder != null) {
            Object packet = decoder.apply(buf); // Decode the packet

            // Handle special packets with their own client handlers
            if (packet instanceof StaminaSyncPacket staminaPacket) {
                staminaPacket.handleClient();
            } else if (packet instanceof SyncBreathPacket breathPacket) {
                breathPacket.handleClient();
            } else if (packet instanceof BreathingEffectPacket effectPacket) {
                effectPacket.handleClient();
            } else if (handler != null) {
                handler.run(); // Use the registered handler
            }
        }
    }

    // Utility methods for packet sending - NOW IMPLEMENTED WITH ARCHITECTURY
    public static void sendToPlayer(Object packet, ServerPlayer player) {
        ResourceLocation id = PACKET_IDS.get(packet.getClass());
        if (id != null) {
            FriendlyByteBuf buf = encodePacket(packet);
            NetworkManager.sendToPlayer(player, id, buf);
        }
    }

    public static void sendToServer(Object packet) {
        ResourceLocation id = PACKET_IDS.get(packet.getClass());
        if (id != null) {
            FriendlyByteBuf buf = encodePacket(packet);
            NetworkManager.sendToServer(id, buf);
        }
    }

    public static void sendToAll(Object packet, MinecraftServer server) {
        ResourceLocation id = PACKET_IDS.get(packet.getClass());
        if (id != null && server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                sendToPlayer(packet, player);
            }
        }
    }

    // Encode packet to buffer
    public static FriendlyByteBuf encodePacket(Object packet) {
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
        }

        return buf;
    }

    // Get packet ID
    public static ResourceLocation getPacketId(Object packet) {
        return PACKET_IDS.get(packet.getClass());
    }
}