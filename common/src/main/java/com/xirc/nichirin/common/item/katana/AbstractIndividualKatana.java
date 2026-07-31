package com.xirc.nichirin.common.item.katana;

/** One independently usable half of a dual-katana set. */
public abstract class AbstractIndividualKatana extends Katana implements KatanaSet {

    protected final boolean isRightKatana;

    protected AbstractIndividualKatana(Properties properties, boolean isRightKatana) {
        super(properties);
        this.isRightKatana = isRightKatana;
    }
}
