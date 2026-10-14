package com.newuniverse.nusmp.relics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * The later relic waves (Wave8 and up, plus WaveBoss) found by name: a wave class is optional, so a missing one is skipped and a broken one is logged
 * and survived, as for the first eight waves in {@link RelicCatalog}. Each wave class has {@code public static void defs(Consumer<RelicDef>)} and
 * {@code public static void hooks()}.
 */
final class WaveRegistry {
    private WaveRegistry() {}

    private static final Logger LOG = LoggerFactory.getLogger("nusmp");
    private static final String PKG = "com.newuniverse.nusmp.relics.wave.";
    static final int FIRST = 8, LAST = 40;

    static void defs(Consumer<RelicDef> out) {
        call("WaveBoss", "defs", out);
        for (int n = FIRST; n <= LAST; n++) call("Wave" + n, "defs", out);
    }

    static void hooks() {
        call("WaveBoss", "hooks", null);
        for (int n = FIRST; n <= LAST; n++) call("Wave" + n, "hooks", null);
    }

    private static void call(String cls, String method, Consumer<RelicDef> out) {
        Class<?> c;
        try {
            c = Class.forName(PKG + cls);
        } catch (ClassNotFoundException absent) {
            return;
        } catch (LinkageError broken) {
            LOG.error("[nusmp] relics: {} could not be loaded", cls, broken);
            return;
        }
        try {
            Method m = out == null ? c.getMethod(method) : c.getMethod(method, Consumer.class);
            if (out == null) m.invoke(null); else m.invoke(null, out);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failed) {
            LOG.error("[nusmp] relics: {}.{} failed; its relics may be missing", cls, method, failed);
        }
    }
}
