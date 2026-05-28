package com.xirc.nichirin.common.system.sheathing;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class PlayerSheathData {
    private final SheathSlotData[] slots = new SheathSlotData[] {
            new SheathSlotData(SheathPosition.LEFT_HIP, 0, 1, UnsheatheAttackType.QUICKDRAW_SLASH),
            new SheathSlotData(SheathPosition.RIGHT_HIP, 1, 2, UnsheatheAttackType.REVERSE_DRAW_SLASH),
            new SheathSlotData(SheathPosition.BACK, 2, 3, UnsheatheAttackType.OVERHEAD_BACKDRAW),
            new SheathSlotData(SheathPosition.BACK_2, 3, 4, UnsheatheAttackType.DIAGONAL_BACKDRAW)
    };

    private SheathPosition chargingSlot;
    private int globalCooldownTicks;

    public List<SheathSlotData> getSlotsByPriority() {
        return Arrays.stream(slots)
                .sorted(Comparator.comparingInt(SheathSlotData::getPriority))
                .toList();
    }

    public SheathSlotData[] getSlots() {
        return slots;
    }

    public SheathSlotData getSlot(SheathPosition position) {
        return slots[position.ordinal()];
    }

    public SheathPosition getChargingSlot() {
        return chargingSlot;
    }

    public void setChargingSlot(SheathPosition chargingSlot) {
        this.chargingSlot = chargingSlot;
    }

    public int getGlobalCooldownTicks() {
        return globalCooldownTicks;
    }

    public void setGlobalCooldownTicks(int globalCooldownTicks) {
        this.globalCooldownTicks = Math.max(0, globalCooldownTicks);
    }
}
