package com.newuniverse.nusmp.perf;

/** Preview stub: the probe does nothing offline. */
public final class PerfProbe {
    public static final class Section {}
    public static Section section(String name) { return new Section(); }
    public static long begin() { return 0L; }
    public static void end(Section s, long started) {}
}
