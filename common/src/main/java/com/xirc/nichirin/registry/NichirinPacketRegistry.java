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

/**
 * FIXED: Remove double registration, keep it simple
 */
public class NichirinPacketRegistry {

    // Packet IDs
    public static final ResourceLocation DOUBLE_JUMP_ID = new ResourceLocation(BreathOfNichirin.MOD_ID, "double_jump");
    public static final ResourceLocation BREATHING_MOVE_ID = new ResourceLocation(BreathOfNichirin.MOD_ID, "breathing_move");
    public static final ResourceLocation BREATHING_EFFECT_ID = new ResourceLocation(BreathOfNichirin.MOD_ID, "breathing_effect");
    public static final ResourceLocation SYNC_BREATH_ID = new ResourceLocation(BreathOfNichirin.MOD_ID, "sync_breath");
    public static final ResourceLocation SYNC_STAMINA_ID = new ResourceLocation(BreathOfNichirin.MOD_ID, "sync_stamina");

    // Packet class mappings
    private static final Map<Class<?>, ResourceLocation> PACKET_IDS = new HashMap<>();

    /**
     * FIXED: Single registration point - no double registration
     */
    public static void init() {
        // Map packet classes to IDs
        PACKET_IDS.put(DoubleJumpPacket.class, DOUBLE_JUMP_ID);
        PACKET_IDS.put(BreathingMovePacket.class, BREATHING_MOVE_ID);
        PACKET_IDS.put(BreathingEffectPacket.class, BREATHING_EFFECT_ID);
        PACKET_IDS.put(SyncBreathPacket.class, SYNC_BREATH_ID);
        PACKET_IDS.put(StaminaSyncPacket.class, SYNC_STAMINA_ID);

        // Register with Architectury - ONCE
        registerPackets();
    }

    private static void registerPackets() {
        // C2S packets
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

        // S2C packets
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

    // Simple packet sending
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

    // Simple packet encoding
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
}