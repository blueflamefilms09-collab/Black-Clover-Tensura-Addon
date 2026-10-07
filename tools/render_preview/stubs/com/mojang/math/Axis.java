package com.mojang.math;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Preview stub of Minecraft's Axis (the six axis rotations, in radians or degrees). */
@FunctionalInterface
public interface Axis {
    Axis XN = f -> new Quaternionf().rotationX(-f);
    Axis XP = f -> new Quaternionf().rotationX(f);
    Axis YN = f -> new Quaternionf().rotationY(-f);
    Axis YP = f -> new Quaternionf().rotationY(f);
    Axis ZN = f -> new Quaternionf().rotationZ(-f);
    Axis ZP = f -> new Quaternionf().rotationZ(f);

    static Axis of(Vector3f axis) { return f -> new Quaternionf().rotationAxis(f, axis); }

    Quaternionf rotation(float radians);

    default Quaternionf rotationDegrees(float degrees) { return this.rotation(degrees * ((float) Math.PI / 180F)); }
}
