package com.xirc.nichirin.common.network.s2c;

import com.xirc.nichirin.common.system.sheathing.PlayerSheathData;
import com.xirc.nichirin.common.system.sheathing.SheathPosition;
import com.xirc.nichirin.common.system.sheathing.SheathSlotData;
import com.xirc.nichirin.common.system.sheathing.SheathState;
import com.xirc.nichirin.common.system.sheathing.SheathingManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class SheathSyncPacket {
    private final int entityId;
    private final SlotSync[] slots;

    public SheathSyncPacket(Player player, PlayerSheathData data) {
        this.entityId = player.getId();
        this.slots = new SlotSync[data.getSlots().length];
        SheathSlotData[] source = data.getSlots();
        for (int i = 0; i < source.length; i++) {
            this.slots[i] = new SlotSync(source[i]);
        }
    }

    public SheathSyncPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        int count = buf.readInt();
        this.slots = new SlotSync[count];
        for (int i = 0; i < count; i++) {
            slots[i] = new SlotSync(buf);
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(slots.length);
        for (SlotSync slot : slots) {
            slot.write(buf);
        }
    }

    public void handleClient() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        Entity entity = minecraft.level.getEntity(entityId);
        if (!(entity instanceof Player player)) return;
        for (SlotSync slot : slots) {
            SheathingManager.applyClientSync(player, slot.position, slot.enabled, slot.hotbarSlot,
                    slot.priority, slot.state, slot.cooldownTicks, slot.visible);
            SheathingManager.get(player).getSlot(slot.position).setStoredSword(slot.storedSword);
        }
    }

    private static class SlotSync {
        private final SheathPosition position;
        private final boolean enabled;
        private final int hotbarSlot;
        private final int priority;
        private final SheathState state;
        private final int cooldownTicks;
        private final boolean visible;
        private final ItemStack storedSword;

        private SlotSync(SheathSlotData slot) {
            this.position = slot.getPosition();
            this.enabled = slot.isEnabled();
            this.hotbarSlot = slot.getLinkedHotbarSlot();
            this.priority = slot.getPriority();
            this.state = slot.getState();
            this.cooldownTicks = slot.getCooldownTicks();
            this.visible = slot.isVisible();
            this.storedSword = slot.getStoredSword().copy();
        }

        private SlotSync(FriendlyByteBuf buf) {
            this.position = buf.readEnum(SheathPosition.class);
            this.enabled = buf.readBoolean();
            this.hotbarSlot = buf.readInt();
            this.priority = buf.readInt();
            this.state = buf.readEnum(SheathState.class);
            this.cooldownTicks = buf.readInt();
            this.visible = buf.readBoolean();
            this.storedSword = buf.readItem();
        }

        private void write(FriendlyByteBuf buf) {
            buf.writeEnum(position);
            buf.writeBoolean(enabled);
            buf.writeInt(hotbarSlot);
            buf.writeInt(priority);
            buf.writeEnum(state);
            buf.writeInt(cooldownTicks);
            buf.writeBoolean(visible);
            buf.writeItem(storedSword);
        }
    }
}
