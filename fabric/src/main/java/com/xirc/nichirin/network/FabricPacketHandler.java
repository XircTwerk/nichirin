package com.xirc.nichirin.network;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.item.katana.Katana;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class FabricPacketHandler {

    public static void registerServerPackets() {
        PayloadTypeRegistry.playC2S().register(KatanaAttackPayload.TYPE, KatanaAttackPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(KatanaAttackPayload.TYPE, (payload, context) ->
                handleKatanaAttack(context.player()));
    }

    public static void sendKatanaAttackPacket() {
        ClientPlayNetworking.send(new KatanaAttackPayload());
    }

    private static void handleKatanaAttack(ServerPlayer player) {
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);

        // Verify player is still holding a katana
        if (mainHand.getItem() instanceof Katana katana) {
            // Call the performAttack method
            katana.performAttack(player);
        }
    }

    public record KatanaAttackPayload() implements CustomPacketPayload {
        public static final Type<KatanaAttackPayload> TYPE = new Type<>(BreathOfNichirin.id("katana_attack"));
        public static final StreamCodec<RegistryFriendlyByteBuf, KatanaAttackPayload> STREAM_CODEC =
                StreamCodec.unit(new KatanaAttackPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}