package com.newuniverse.nusmp.entity.veldanava;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

import java.lang.reflect.Field;

/**
 * Why (3.30.0): vanilla clamps the MAX_HEALTH attribute to 1024, so a boss with five million health needs the cap lifted. This raises the upper bound of that one attribute
 * (nothing else: no entity gets more health unless a mod gives it some) the first time it is asked, on the client and the server alike (the client clamps the attribute it is
 * sent the same way, so both sides must agree). When it cannot (a restricted runtime), {@link #raised()} stays false and {@link VeldanavaStats#healthDivisor} scales the damage
 * instead, so the fight is the same size either way. One-time, no per-tick cost.
 */
public final class HealthCap {
    private HealthCap() {}

    private static boolean tried, raised;

    /** Lifts the cap to {@link VeldanavaStats#HEALTH_MAX}. Safe to call any number of times. */
    public static synchronized boolean raise() {
        if (tried) return raised;
        tried = true;
        try {
            Attribute a = Attributes.MAX_HEALTH.value();
            if (!(a instanceof RangedAttribute r)) return false;
            Field max = field(r.getClass());
            if (max == null) return false;
            max.setAccessible(true);
            if (max.getDouble(r) < VeldanavaStats.HEALTH_MAX) max.setDouble(r, VeldanavaStats.HEALTH_MAX);
            raised = r.sanitizeValue(VeldanavaStats.HEALTH_MAX) >= VeldanavaStats.HEALTH_MAX;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            raised = false;
        }
        return raised;
    }

    /** True once the cap was lifted. */
    public static boolean raised() { return raise(); }

    /** The upper bound field of a ranged attribute: by its name, or else the second of its two double fields (minimum, maximum). */
    private static Field field(Class<?> type) {
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            try { return c.getDeclaredField("maxValue"); } catch (NoSuchFieldException ignored) { /* try the next */ }
            Field first = null;
            for (Field f : c.getDeclaredFields()) {
                if (f.getType() != double.class || java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                if (first == null) first = f; else return f;
            }
        }
        return null;
    }
}
