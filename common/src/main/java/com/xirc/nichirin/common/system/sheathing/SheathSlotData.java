package com.xirc.nichirin.common.system.sheathing;

import net.minecraft.world.item.ItemStack;

public class SheathSlotData {
    private final SheathPosition position;
    private boolean enabled = true;
    private int linkedHotbarSlot;
    private int priority;
    private SheathState state = SheathState.EMPTY;
    private UnsheatheAttackType tapAttack;
    private int transitionTicks;
    private int cooldownTicks;
    private boolean visible = true;
    private ItemStack storedSword = ItemStack.EMPTY;
    /**
     * True when the stored sword came from the player's offhand (the second blade of a dual
     * wield). On unsheathe it's restored to the offhand instead of the linked hotbar slot.
     */
    private boolean storedFromOffhand = false;

    public SheathSlotData(SheathPosition position, int linkedHotbarSlot, int priority, UnsheatheAttackType tapAttack) {
        this.position = position;
        this.linkedHotbarSlot = linkedHotbarSlot;
        this.priority = priority;
        this.tapAttack = tapAttack;
    }

    public SheathPosition getPosition() { return position; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getLinkedHotbarSlot() { return linkedHotbarSlot; }
    public void setLinkedHotbarSlot(int linkedHotbarSlot) { this.linkedHotbarSlot = Math.max(0, Math.min(8, linkedHotbarSlot)); }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = Math.max(1, priority); }
    public SheathState getState() { return state; }
    public void setState(SheathState state) { this.state = state; }
    public UnsheatheAttackType getTapAttack() { return tapAttack; }
    public void setTapAttack(UnsheatheAttackType tapAttack) { this.tapAttack = tapAttack; }
    public int getTransitionTicks() { return transitionTicks; }
    public void setTransitionTicks(int transitionTicks) { this.transitionTicks = Math.max(0, transitionTicks); }
    public int getCooldownTicks() { return cooldownTicks; }
    public void setCooldownTicks(int cooldownTicks) { this.cooldownTicks = Math.max(0, cooldownTicks); }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public ItemStack getStoredSword() { return storedSword; }
    public void setStoredSword(ItemStack storedSword) { this.storedSword = storedSword == null ? ItemStack.EMPTY : storedSword; }
    public boolean hasStoredSword() { return !storedSword.isEmpty(); }

    public boolean isStoredFromOffhand() { return storedFromOffhand; }
    public void setStoredFromOffhand(boolean storedFromOffhand) { this.storedFromOffhand = storedFromOffhand; }
}