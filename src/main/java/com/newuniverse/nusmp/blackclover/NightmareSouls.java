package com.newuniverse.nusmp.blackclover;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;

/**
 * Reads a player's TR Nightmare soul type without a compile-time dependency.
 * Returns: the soul type name (e.g. "FLAME"), null if Nightmare hasn't assigned one yet,
 * or NO_NIGHTMARE if Nightmare isn't installed.
 */
public final class NightmareSouls {
    public static final String NO_NIGHTMARE = "NO_NIGHTMARE";
    private static Method getSoulType;
    private static boolean failed;

    private NightmareSouls() {}

    public static String soulTypeOf(LivingEntity entity) {
        if (failed || !ModList.get().isLoaded("trnightmare")) return NO_NIGHTMARE;
        try {
            if (getSoulType == null) {
                Class<?> c = Class.forName("com.github.hvnbael.trnightmare.capability.ISoulData");
                getSoulType = c.getMethod("getSoulType", LivingEntity.class);
            }
            Object type = getSoulType.invoke(null, entity);
            return type == null ? null : ((Enum<?>) type).name();
        } catch (Throwable t) {
            failed = true;
            return NO_NIGHTMARE;
        }
    }
}
