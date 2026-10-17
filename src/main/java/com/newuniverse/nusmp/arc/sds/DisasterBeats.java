package com.newuniverse.nusmp.arc.sds;

/**
 * Why: the timeline of a Disaster field in one place, so it can be checked: the field is a row of beats; at the start of each beat the gardens of that beat appear (the
 * telegraph), {@code warn} ticks later the field pulses and strikes everyone outside a garden, and the next beat begins when the period is over. The player's whole
 * answer is made inside {@code warn}, so it has a floor (twelve ticks is the least any telegraph of this fight gives) and must be shorter than the period (the gardens
 * of one beat are gone before the next show). This pure class answers "what happens at this age" for the zone step of the glue.
 */
public final class DisasterBeats {
    private DisasterBeats() {}

    /** A field's rhythm: how many beats, the ticks of one beat, the ticks from the show of the gardens to the pulse. */
    public record Spec(int beats, int period, int warn) {
        public boolean valid() { return beats >= 1 && warn >= 12 && warn < period; }

        /** The ticks the whole field lasts. */
        public int total() { return beats * period; }
    }

    private static boolean inside(int age, Spec s) { return age >= 0 && age < s.total(); }

    /** The beat the age falls in (0-based), or -1 when the field is over. */
    public static int beatOf(int age, Spec s) { return inside(age, s) ? age / s.period() : -1; }

    /** Do the gardens of a new beat appear at this age? */
    public static boolean showsAt(int age, Spec s) { return inside(age, s) && age % s.period() == 0; }

    /** Does the field pulse at this age? */
    public static boolean pulsesAt(int age, Spec s) { return inside(age, s) && age % s.period() == s.warn(); }

    /** The ticks left until the pulse of the current beat (0 once it has pulsed, or when the field is over): what the closing ring of the telegraph shows. */
    public static int warnLeft(int age, Spec s) {
        if (!inside(age, s)) return 0;
        int into = age % s.period();
        return Math.max(0, s.warn() - into);
    }

    /** The pulses a whole field makes. */
    public static int pulses(Spec s) { return Math.max(0, s.beats()); }
}
