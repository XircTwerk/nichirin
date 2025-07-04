package com.xirc.nichirin.fabric.network;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.item.katana.SimpleKatana;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class FabricPacketHandler {

    public static final ResourceLocation KATANA_ATTACK_PACKET = BreathOfNichirin.id("katana_attack");

    public static void registerServerPackets() {
        // Register server-side packet receiver
        ServerPlayNetworking.registerGlobalReceiver(KATANA_ATTACK_PACKET, (server, player, handler, buf, responseSender) -> {
            // Execute on server thread
            server.execute(() -> {
                handleKatanaAttack(player);
            });
        });
    }

    public static void sendKatanaAttackPacket() {
        // Send empty packet from client to server
        FriendlyByteBuf buf = PacketByteBufs.create();
        ClientPlayNetworking.send(KATANA_ATTACK_PACKET, buf);
    }

    private static void handleKatanaAttack(ServerPlayer player) {
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);

        // Verify player is still holding a katana
        if (mainHand.getItem() instanceof SimpleKatana katana) {
            // Call the performAttack method
            katana.performAttack(player);
        }
    }
}