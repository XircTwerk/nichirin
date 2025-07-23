package com.xirc.nichirin.client.util;

/**
 * Shared state manager to coordinate input blocking between different systems
 */
public class InputStateManager {

    private static boolean attackWheelOpen = false;

    /**
     * Set whether the attack wheel is open
     */
    public static void setAttackWheelOpen(boolean open) {
        attackWheelOpen = open;
        System.out.println("DEBUG: InputStateManager - Attack wheel open: " + open);
    }

    /**
     * Check if the attack wheel is open
     */
    public static boolean isAttackWheelOpen() {
        return attackWheelOpen;
    }

    /**
     * Check if normal katana attacks should be blocked
     */
    public static boolean shouldBlockKatanaAttacks() {
        return attackWheelOpen;
    }

    /**
     * Check if hotbar changes should be blocked
     */
    public static boolean shouldBlockHotbarChanges() {
        return attackWheelOpen;
    }
}