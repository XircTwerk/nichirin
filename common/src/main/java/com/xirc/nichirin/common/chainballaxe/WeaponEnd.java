package com.xirc.nichirin.common.chainballaxe;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * A physical endpoint of the weapon (the axe or the flail). It is a Verlet particle like a chain node,
 * but also carries an orientation so the rendered model can face believably, and it participates in the
 * chain's distance constraints as a heavy point.
 */
public abstract class WeaponEnd extends PhysicsPoint {

    public final EndType type;
    public final Quaternionf rotation = new Quaternionf();
    public final Quaternionf previousRotation = new Quaternionf();

    protected WeaponEnd(Vec3 position, double inverseMass, EndType type) {
        super(position, inverseMass);
        this.type = type;
    }
}
