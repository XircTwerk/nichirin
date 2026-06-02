package com.xirc.nichirin.common.network.c2s;

import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.common.system.perks.*;
import com.xirc.nichirin.common.network.s2c.PerkSyncPacket;
import com.xirc.nichirin.common.util.NetworkBufferUtils;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;

/**
 * C2S — player requests a perk-related action from the GUI.
 *
 * <p>All validation and state mutation happens server-side via {@link PerkManager}.
 * A {@link PerkSyncPacket} is sent back to the client after any successful action.</p>
 */
public class PerkActionPacket {

    public enum Action {
        EQUIP,
        UNEQUIP,
        UPGRADE,
        EQUIP_FLAW,
        UNEQUIP_FLAW,
        SAVE_PRESET,
        LOAD_PRESET,
        DELETE_PRESET,
        TOGGLE_ALL
    }

    private final Action action;
    /** Perk or flaw ID for equip/unequip/upgrade. Preset name for preset actions. */
    private final String id;
    /** Tier level; only relevant for EQUIP. */
    private final int tierLevel;

    public PerkActionPacket(Action action, String id, int tierLevel) {
        this.action = action;
        this.id = id;
        this.tierLevel = tierLevel;
    }

    /** Convenience constructor for actions that don't need a tier. */
    public PerkActionPacket(Action action, String id) {
        this(action, id, 0);
    }

    public PerkActionPacket(FriendlyByteBuf buf) {
        this.action = buf.readEnum(Action.class);
        this.id = buf.readUtf();
        this.tierLevel = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeUtf(id);
        buf.writeInt(tierLevel);
    }

    public void handle(ServerPlayer player) {
        PerkData data = PlayerDataProvider.getData(player).getPerkData();
        PerkManager.Result result = null;

        switch (action) {
            case EQUIP -> {
                PerkTier tier = PerkTier.fromLevel(tierLevel);
                result = PerkManager.tryEquip(player, id, tier);
            }
            case UNEQUIP -> result = PerkManager.tryUnequip(player, id);
            case UPGRADE -> result = PerkManager.tryUpgrade(player, id);
            case EQUIP_FLAW -> result = PerkManager.tryEquipFlaw(player, id);
            case UNEQUIP_FLAW -> result = PerkManager.tryUnequipFlaw(player, id);
            case SAVE_PRESET -> {
                PerkPreset snapshot = data.snapshotAsPreset(id);
                data.savePreset(snapshot);
                result = PerkManager.Result.ok();
            }
            case LOAD_PRESET -> {
                for (PerkPreset p : data.getPresets()) {
                    if (p.name.equals(id)) {
                        Set<String> previousFlaws = new HashSet<>(data.getEquippedFlaws());
                        data.applyPreset(p);
                        PerkManager.cleanupRemovedFlaws(player, previousFlaws, data.getEquippedFlaws());
                        result = PerkManager.Result.ok();
                        break;
                    }
                }
                if (result == null) result = PerkManager.Result.fail("Preset not found: " + id);
            }
            case DELETE_PRESET -> {
                data.deletePreset(id);
                result = PerkManager.Result.ok();
            }
            case TOGGLE_ALL -> {
                data.setPerksEnabled(!data.isPerksEnabled());
                result = PerkManager.Result.ok();
            }
        }

        if (result != null && !result.success) {
            player.displayClientMessage(
                    Component.literal(result.message).withStyle(ChatFormatting.RED),
                    true
            );
            return;
        }

        // Sync updated state back to client
        try {
            FriendlyByteBuf syncBuf = new FriendlyByteBuf(Unpooled.buffer());
            new PerkSyncPacket(data).toBytes(syncBuf);
            NetworkManager.sendToPlayer(player, NichirinPacketRegistry.PERK_SYNC_ID, NetworkBufferUtils.server(syncBuf, player));
        } catch (Exception e) {
            // ignore send errors
        }
    }
}