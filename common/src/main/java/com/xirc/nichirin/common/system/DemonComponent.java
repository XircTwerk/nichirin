package com.xirc.nichirin.common.system;

import com.xirc.nichirin.common.event.system.DemonFoodHandler;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Component for handling demon blood data and syncing
 */
public class DemonComponent {

    private static int clientBloodPoints = 10;
    private static int clientHalfBloodPoints = 0;

    /**
     * Gets blood points for display (client-side)
     */
    public static int getClientBloodPoints() {
        return clientBloodPoints;
    }

    /**
     * Gets half blood points for display (client-side)
     */
    public static int getClientHalfBloodPoints() {
        return clientHalfBloodPoints;
    }

    /**
     * Sets blood points from server sync (client-side)
     */
    public static void setClientBloodPoints(int bloodPoints) {
        clientBloodPoints = Math.max(0, Math.min(bloodPoints, 10));
    }

    /**
     * Sets half blood points from server sync (client-side)
     */
    public static void setClientHalfBloodPoints(int halfBloodPoints) {
        clientHalfBloodPoints = Math.max(0, Math.min(halfBloodPoints, 1));
    }

    /**
     * Updates both blood values from server sync (client-side)
     */
    public static void updateBloodFromSync(int bloodPoints, int halfBloodPoints, boolean isDemon) {
        if (isDemon) {
            setClientBloodPoints(bloodPoints);
            setClientHalfBloodPoints(halfBloodPoints);
        }
    }

    /**
     * Called when player dies/respawns to reset blood to full
     */
    public static void onPlayerRespawn() {
        clientBloodPoints = 10;
        clientHalfBloodPoints = 0;
    }

    /**
     * Syncs demon data to a player
     */
    public static void sync(ServerPlayer player) {
        if (DemonManager.isDemon(player)) {
            int bloodPoints = DemonManager.getBloodPoints(player);
            int halfBloodPoints = DemonFoodHandler.getHalfBloodPoints(player);
            NichirinPacketRegistry.sendDemonSync(player, bloodPoints, halfBloodPoints, true);
        }
    }

    /**
     * Saves demon data to NBT
     */
    public static CompoundTag save(Player player) {
        CompoundTag tag = new CompoundTag();
        if (DemonManager.isDemon(player)) {
            tag.putInt("BloodPoints", DemonManager.getBloodPoints(player));
            tag.putInt("HalfBloodPoints", DemonFoodHandler.getHalfBloodPoints(player));
            tag.putBoolean("IsDemon", true);
        }
        return tag;
    }

    /**
     * Loads demon data from NBT
     */
    public static void load(Player player, CompoundTag tag) {
        if (tag.getBoolean("IsDemon")) {
            int bloodPoints = tag.getInt("BloodPoints");
            int halfBloodPoints = tag.getInt("HalfBloodPoints");

            DemonManager.setBloodPointsDirectly(player, bloodPoints);
            DemonFoodHandler.setHalfBloodPointsDirectly(player, halfBloodPoints);

            if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                sync(serverPlayer);
            }
        }
    }
}