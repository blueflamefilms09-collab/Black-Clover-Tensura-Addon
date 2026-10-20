package com.newuniverse.nusmp.perf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: nothing here can be profiled by the author, so the owner needs to SEE which part of the mod is slow on their own machine. The probe samples {@code System.nanoTime} around named
 * sections of the biggest handlers and renderers and keeps, per section, the number of calls, the total time, the rolling average and the rolling maximum. It is OFF by default and costs
 * one read of a static flag per section when it is off ({@link #begin} returns 0 and {@link #end} returns at once on a 0). It is switched on by the config
 * ({@code probe.enabled} in nusmp-performance-common.toml) or by the commands {@code /multiverse perf on|off|report|reset} (server sections) and {@code /nusmp perf on|off|report|reset} (client
 * sections). Section names start with {@code s.} (server thread) or {@code c.} (client thread) so each command prints its own side.
 *
 * <pre>
 *   private static final PerfProbe.Section SEC = PerfProbe.section("s.relic.curios");
 *   long t0 = PerfProbe.begin();
 *   try { ... the work ... } finally { PerfProbe.end(SEC, t0); }
 * </pre>
 * Pure (no game classes); thread-safe (a section locks while it records, which only happens while the probe is on).
 */
public final class PerfProbe {
    private PerfProbe() {}

    private static volatile boolean enabled;
    private static volatile long since = System.nanoTime();
    private static final Map<String, Section> SECTIONS = new ConcurrentHashMap<>();

    /** Ring size of the rolling average: about the last five seconds of a section that runs once a tick. */
    public static final int WINDOW = 100;

    /** One measured place. */
    public static final class Section {
        private final String name;
        private final RollingStat stat = new RollingStat(WINDOW);

        Section(String name) { this.name = name; }

        public String name() { return name; }

        void add(long nanos) {
            synchronized (this) { stat.add(nanos); }
        }

        /** A consistent copy of the numbers. */
        public Row row() {
            synchronized (this) {
                return new Row(name, stat.count(), stat.totalNanos(), stat.avgNanos(), stat.rollingAvgNanos(), stat.maxNanos(), stat.rollingMaxNanos());
            }
        }

        void reset() {
            synchronized (this) { stat.reset(); }
        }
    }

    /** The numbers of one section at one moment. Times in nanoseconds. */
    public record Row(String name, long calls, long totalNanos, double avgNanos, double rollingAvgNanos, long maxNanos, long rollingMaxNanos) {
        /** Milliseconds of work per second of wall clock since the probe was last reset. */
        public double msPerSecond(double seconds) { return seconds <= 0 ? 0.0 : totalNanos / 1.0e6 / seconds; }
    }

    /** The section of this name (made on the first call, the same object after): keep it in a static final. */
    public static Section section(String name) { return SECTIONS.computeIfAbsent(name, Section::new); }

    public static boolean enabled() { return enabled; }

    public static void setEnabled(boolean on) {
        if (on && !enabled) reset();
        enabled = on;
    }

    /** Starts a measurement: the time, or 0 when the probe is off. */
    public static long begin() { return enabled ? System.nanoTime() : 0L; }

    /** Ends a measurement started by {@link #begin}; does nothing when that returned 0. */
    public static void end(Section s, long started) {
        if (started != 0L) s.add(System.nanoTime() - started);
    }

    /** Clears every section and restarts the clock the per-second figures are measured against. */
    public static void reset() {
        for (Section s : SECTIONS.values()) s.reset();
        since = System.nanoTime();
    }

    /** Seconds since the probe was switched on or last reset. */
    public static double seconds() { return (System.nanoTime() - since) / 1.0e9; }

    /** Every section with at least one call whose name starts with {@code prefix}, the most expensive (total time) first, at most {@code n}. */
    public static List<Row> top(int n, String prefix) {
        List<Row> rows = new ArrayList<>();
        for (Section s : SECTIONS.values()) {
            if (prefix != null && !s.name.startsWith(prefix)) continue;
            Row r = s.row();
            if (r.calls() > 0) rows.add(r);
        }
        rows.sort(Comparator.comparingLong(Row::totalNanos).reversed().thenComparing(Row::name));
        return rows.size() > n ? new ArrayList<>(rows.subList(0, n)) : rows;
    }

    /** How many sections exist (called at least once or not). */
    public static int sectionCount() { return SECTIONS.size(); }

    /** One text line per row: name, calls, average and rolling average and maximum in microseconds, and the ms of work per second. */
    public static List<String> report(int n, String prefix) {
        List<String> out = new ArrayList<>();
        double secs = seconds();
        out.add(String.format(Locale.ROOT, "perf probe %s, %.1f s of samples", enabled ? "ON" : "OFF", secs));
        List<Row> rows = top(n, prefix);
        if (rows.isEmpty()) {
            out.add(enabled ? "no section has run yet" : "no samples; switch it on with the perf command");
            return out;
        }
        int i = 1;
        for (Row r : rows) {
            out.add(String.format(Locale.ROOT, "%2d. %-34s %8d calls  avg %8.1f us  now %8.1f us  max %9.1f us  %6.2f ms/s",
                    i++, r.name(), r.calls(), r.avgNanos() / 1000.0, r.rollingAvgNanos() / 1000.0, r.maxNanos() / 1000.0, r.msPerSecond(secs)));
        }
        return out;
    }
}
