package com.mojang.math;

import org.joml.Quaternionf;

@FunctionalInterface
public interface Axis {
    Axis XP = r -> new Quaternionf().rotationX(r), YP = r -> new Quaternionf().rotationY(r), ZP = r -> new Quaternionf().rotationZ(r);
    Quaternionf rotation(float rad);
    default Quaternionf rotationDegrees(float deg) { return rotation((float) Math.toRadians(deg)); }
}
