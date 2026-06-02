package com.xirc.nichirin.common.system.perks;

/**
 * Central kill switch for the archived perk system.
 *
 * <p>The classes and item IDs remain registered so old worlds and saved player
 * data can still load, but the system should not surface in gameplay.</p>
 */
public final class PerkArchive {
    public static final boolean ARCHIVED = true;

    private PerkArchive() {
    }
}