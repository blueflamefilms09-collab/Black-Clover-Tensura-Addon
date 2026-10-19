package com.newuniverse.nusmp.labyrinth.mega.enc;

import java.util.List;
import java.util.Locale;

/**
 * Why: "why has nothing happened?" has about a dozen possible answers (the switch is off, the player is in creative, the party stands in a safe room, the sector
 * is still being built, the table is empty, the cooldown has not run out, a crowd of old monsters is still alive, the dice are too low, no place can be found ...)
 * and the owner cannot attach a debugger to a running game. {@code /multiverse mega dm why} walks the gates in the order the Director itself checks them and
 * prints each with the value it has; this class is the pure part: the line of a gate, the first gate that blocks, the odds of the dice and the time they
 * imply. Pure (the Director supplies the values), so the wording and the arithmetic are tested.
 */
public final class EncGates {
    private EncGates() {}

    /** PASS: the gate is open. BLOCK: the Director will not roll or spawn while this holds. WARN: open, but a likely reason for few encounters. INFO: a value, not a gate. */
    public enum Status { PASS, BLOCK, WARN, INFO }

    public record Gate(String name, String value, Status status) {
        public String line() {
            String mark = switch (status) {
                case PASS -> "[ok]   ";
                case BLOCK -> "[STOP] ";
                case WARN -> "[warn] ";
                case INFO -> "[info] ";
            };
            return mark + name + ": " + value;
        }
    }

    public static Gate pass(String name, String value) { return new Gate(name, value, Status.PASS); }

    public static Gate block(String name, String value) { return new Gate(name, value, Status.BLOCK); }

    public static Gate warn(String name, String value) { return new Gate(name, value, Status.WARN); }

    public static Gate info(String name, String value) { return new Gate(name, value, Status.INFO); }

    /** The first gate that blocks, or null when every gate is open. */
    public static Gate firstBlock(List<Gate> gates) {
        for (Gate g : gates) if (g.status() == Status.BLOCK) return g;
        return null;
    }

    /** The one-line verdict printed after the gates. */
    public static String verdict(List<Gate> gates) {
        Gate b = firstBlock(gates);
        if (b != null) return "Blocked at \"" + b.name() + "\": " + b.value();
        int warns = 0;
        for (Gate g : gates) if (g.status() == Status.WARN) warns++;
        return warns == 0 ? "Nothing blocks the Dungeon Master: it rolls for this party on its next pass." : "Nothing blocks the Dungeon Master, but " + warns + " gate(s) above are weak.";
    }

    /**
     * The dice as a gate: the chance that one pass is not quiet, how long the first encounter takes on average (with the tension bonus growing as it does in the
     * game), and a BLOCK when no roll can ever reach the difficulty class and a WARN when the first encounter is more than a minute and a half away on average.
     */
    public static Gate dice(EncounterRoll.Mods mods, int dc, int rollEveryTicks, int tensionEvery, int tensionCap) {
        double p = EncounterRoll.chance(mods, dc);
        double[] d = EncounterRoll.distribution(mods, dc);
        double passes = Tension.expectedPasses(mods.withTension(0), dc, tensionEvery, tensionCap, 400);
        double seconds = passes * Math.max(1, rollEveryTicks) / 20.0;
        String value = String.format(Locale.ROOT, "total %+d against DC %d: %.0f%% of passes bring trouble now (minor %.0f%%, standard %.0f%%, hard %.0f%%, ambush %.0f%%); with the tension of a quiet party the first one comes after about %.0f s",
                mods.total() - mods.tension(), dc, p * 100.0, d[Band.MINOR.ordinal()] * 100.0, d[Band.STANDARD.ordinal()] * 100.0, d[Band.HARD.ordinal()] * 100.0, d[Band.AMBUSH.ordinal()] * 100.0, seconds);
        if (p <= 0.0 && Tension.bonus(1_000_000, tensionEvery, tensionCap) <= 0) return block("dice", value + " (unreachable)");
        return seconds > 90.0 ? warn("dice", value) : pass("dice", value);
    }

    /** "12 s", "3 min 05 s": a duration in ticks for the lines of the report. */
    public static String duration(long ticks) {
        long s = Math.max(0L, ticks) / 20L;
        return s < 60 ? s + " s" : (s / 60) + " min " + String.format(Locale.ROOT, "%02d", s % 60) + " s";
    }
}
