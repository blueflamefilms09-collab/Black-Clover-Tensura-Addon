package com.mojang.logging;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Preview stub of Mojang's LogUtils: the logger keeps warn / error lines (formatted like slf4j) so the preview can report them as issues
 * ("geo file ... not found" is only a log line in the game, and then nothing is drawn).
 */
public final class LogUtils {
    private LogUtils() {}

    /** Preview only: [level, text] pairs logged since the last drain. */
    public static final List<String[]> MESSAGES = new ArrayList<>();

    public static Logger getLogger() { return new Capture(); }

    private static String fmt(String format, Object... args) {
        StringBuilder b = new StringBuilder();
        int a = 0, i = 0;
        while (i < format.length()) {
            int j = format.indexOf("{}", i);
            if (j < 0 || a >= args.length) { b.append(format.substring(i)); break; }
            b.append(format, i, j).append(args[a++]);
            i = j + 2;
        }
        return b.toString();
    }

    private static void add(String level, String format, Object... args) {
        Throwable t = args.length > 0 && args[args.length - 1] instanceof Throwable th ? th : null;
        MESSAGES.add(new String[]{level, fmt(format, args) + (t == null ? "" : " (" + t + ")")});
    }

    private static final class Capture implements Logger {
        public void trace(String m) {}
        public void trace(String f, Object a) {}
        public void trace(String f, Object a, Object b) {}
        public void trace(String f, Object... a) {}
        public void trace(String m, Throwable t) {}
        public void debug(String m) {}
        public void debug(String f, Object a) {}
        public void debug(String f, Object a, Object b) {}
        public void debug(String f, Object... a) {}
        public void debug(String m, Throwable t) {}
        public void info(String m) {}
        public void info(String f, Object a) {}
        public void info(String f, Object a, Object b) {}
        public void info(String f, Object... a) {}
        public void info(String m, Throwable t) {}
        public void warn(String m) { add("WARN", m); }
        public void warn(String f, Object a) { add("WARN", f, a); }
        public void warn(String f, Object a, Object b) { add("WARN", f, a, b); }
        public void warn(String f, Object... a) { add("WARN", f, a); }
        public void warn(String m, Throwable t) { add("WARN", m, t); }
        public void error(String m) { add("ERROR", m); }
        public void error(String f, Object a) { add("ERROR", f, a); }
        public void error(String f, Object a, Object b) { add("ERROR", f, a, b); }
        public void error(String f, Object... a) { add("ERROR", f, a); }
        public void error(String m, Throwable t) { add("ERROR", m, t); }
    }
}
