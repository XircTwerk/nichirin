package com.xirc.nichirin.common.registry;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.network.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Simple packet registry that works across platforms
 * This is a basic implementation that you can extend with platform-specific networking
 */
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
                    // Handle on client side - you'll need to implement this based on your packet structure
                });

        registerS2C(SYNC_BREATH_ID, SyncBreathPacket.class, SyncBreathPacket::new,
                () -> {
                    // Handle on client side - you'll need to implement this based on your packet structure
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

        if (decoder != null && handler != null) {
            decoder.apply(buf); // Decode the packet
            handler.run(); // Handle it
        }
    }

    // Utility methods for packet sending (to be implemented with platform-specific code)
    public static void sendToPlayer(Object packet, ServerPlayer player) {
        ResourceLocation id = PACKET_IDS.get(packet.getClass());
        if (id != null) {
            // TODO: Implement platform-specific packet sending
            BreathOfNichirin.LOGGER.info("Sending packet {} to player {}", id, player.getName().getString());
        }
    }

    public static void sendToServer(Object packet) {
        ResourceLocation id = PACKET_IDS.get(packet.getClass());
        if (id != null) {
            // TODO: Implement platform-specific packet sending
            BreathOfNichirin.LOGGER.info("Sending packet {} to server", id);
        }
    }

    public static void sendToAll(Object packet) {
        ResourceLocation id = PACKET_IDS.get(packet.getClass());
        if (id != null) {
            // TODO: Implement platform-specific packet sending
            BreathOfNichirin.LOGGER.info("Broadcasting packet {}", id);
        }
    }

    // Encode packet to buffer
    public static FriendlyByteBuf encodePacket(Object packet) {
        FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());

        if (packet instanceof DoubleJumpPacket p) {
            p.toBytes(buf);
        } else if (packet instanceof BreathingMovePacket p) {
            p.toBytes(buf);
        } else if (packet instanceof BreathingEffectPacket p) {
            p.toBytes(buf);
        } else if (packet instanceof SyncBreathPacket p) {
            p.toBytes(buf);
        }

        return buf;
    }

    // Get packet ID
    public static ResourceLocation getPacketId(Object packet) {
        return PACKET_IDS.get(packet.getClass());
    }
}