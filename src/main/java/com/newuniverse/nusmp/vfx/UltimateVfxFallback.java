package com.newuniverse.nusmp.vfx;

import java.util.EnumMap;
import java.util.Map;

/**
 * 3.29.0: the ten Evolution Ultimates (3.28.0) each have their own signature effect ({@code ULT_*} shapes). Before 3.29.0 they played an existing, generic shape. This pure table remembers which one, so the CLIENT setting
 * {@code visual.uniqueUltimates} (default on) can turn the unique look off and play the old shape again. Pure: no Minecraft classes, safe on a dedicated server; only the client reads the switch.
 */
public final class UltimateVfxFallback {
    private static final Map<VfxShape, VfxShape> OLD = new EnumMap<>(VfxShape.class);
    private static volatile boolean unique = true;

    static {
        OLD.put(VfxShape.ULT_IRON_DRAGON, VfxShape.SHADOW_POOL);
        OLD.put(VfxShape.ULT_SATAN_CORE, VfxShape.DEVIL_CIRCLE);
        OLD.put(VfxShape.ULT_ALL_CRUSH, VfxShape.ASH_FORMATION);
        OLD.put(VfxShape.ULT_GREAT_FLOOD, VfxShape.WATER_BURST);
        OLD.put(VfxShape.ULT_MONARCH_SHADOWS, VfxShape.SPIRIT_AURA);
        OLD.put(VfxShape.ULT_CHAIRMAN_STAND, VfxShape.SPIRIT_AURA);
        OLD.put(VfxShape.ULT_DEVOURING_SHADOW, VfxShape.SHADOW_POOL);
        OLD.put(VfxShape.ULT_FINAL_DOSE, VfxShape.POISON_BREATH);
        OLD.put(VfxShape.ULT_DISASTER_FLOODS, VfxShape.WATER_BURST);
        OLD.put(VfxShape.ULT_THOUSAND_ELF, VfxShape.ELF_CIRCLE);
    }

    private UltimateVfxFallback() {}

    public static boolean uniqueEnabled() { return unique; }

    public static void setUniqueEnabled(boolean on) { unique = on; }

    /** The shape the client should draw for {@code shape}: itself, or (switch off) the generic shape the Ultimate played before 3.29.0. */
    public static VfxShape resolve(VfxShape shape) {
        if (unique) return shape;
        VfxShape old = OLD.get(shape);
        return old != null ? old : shape;
    }

    /** The old shape of a unique Ultimate shape, or null when {@code shape} is not one of the ten. */
    public static VfxShape oldShapeOf(VfxShape shape) { return OLD.get(shape); }
}
