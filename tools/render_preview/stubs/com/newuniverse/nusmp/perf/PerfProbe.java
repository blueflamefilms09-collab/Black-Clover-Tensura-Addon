package com.newuniverse.nusmp.perf;

/** Preview stub of perf.PerfProbe: no timing. */
public final class PerfProbe {
    private PerfProbe() {}
    public static final class Section { }
    public static Section section(String name) { return new Section(); }
    public static long begin() { return 0L; }
    public static void end(Section s, long t0) { }
}
