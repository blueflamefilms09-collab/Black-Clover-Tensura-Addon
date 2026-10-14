package com.newuniverse.nusmp.client.gui.bossbar;

import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.UUID;

/**
 * Reads the client's boss bars out of vanilla's {@code BossHealthOverlay}, whose map of them ({@code events}, id to {@code LerpingBossEvent}) is
 * private to its package. There is no access transformer in this mod: the field is found by reflection once (by its Mojang name, else by the
 * only map whose type mentions {@code LerpingBossEvent}) and cached. Every failure is caught and logged once; the overlay then leaves vanilla alone.
 */
final class VanillaBossBars {
    private VanillaBossBars() {}

    private static final Logger LOGGER = LogUtils.getLogger();
    private static Field field;
    private static boolean looked, loggedFailure;

    /** The live map of the client's boss bars, or null when it cannot be read (the caller then draws nothing and cancels nothing). */
    @SuppressWarnings("unchecked")
    static Map<UUID, LerpingBossEvent> events(BossHealthOverlay overlay) {
        if (overlay == null) return null;
        if (!looked) {
            looked = true;
            field = find();
            if (field == null) fail("no map of LerpingBossEvent found in BossHealthOverlay", null);
        }
        if (field == null) return null;
        try {
            Object v = field.get(overlay);
            return v instanceof Map<?, ?> m ? (Map<UUID, LerpingBossEvent>) m : null;
        } catch (Throwable t) {
            field = null;
            fail("reading BossHealthOverlay.events failed", t);
            return null;
        }
    }

    /** True once reading has failed for good. */
    static boolean unavailable() {
        return looked && field == null;
    }

    private static Field find() {
        try {
            Field f = null;
            try {
                f = BossHealthOverlay.class.getDeclaredField("events");
            } catch (NoSuchFieldException ignored) {
                for (Field c : BossHealthOverlay.class.getDeclaredFields()) {
                    if (!Modifier.isStatic(c.getModifiers()) && Map.class.isAssignableFrom(c.getType())
                            && c.getGenericType().getTypeName().contains("LerpingBossEvent")) {
                        f = c;
                        break;
                    }
                }
            }
            if (f == null || !Map.class.isAssignableFrom(f.getType())) return null;
            f.setAccessible(true);
            return f;
        } catch (Throwable t) {
            fail("looking up BossHealthOverlay.events failed", t);
            return null;
        }
    }

    static void fail(String what, Throwable t) {
        if (loggedFailure) return;
        loggedFailure = true;
        if (t == null) LOGGER.warn("Animated boss bar disabled: {}. The vanilla boss bar is used.", what);
        else LOGGER.warn("Animated boss bar disabled: {}. The vanilla boss bar is used.", what, t);
    }
}
