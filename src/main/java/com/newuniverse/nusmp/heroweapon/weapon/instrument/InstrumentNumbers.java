package com.newuniverse.nusmp.heroweapon.weapon.instrument;

import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Budget.Kind;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Budget.Row;

import java.util.List;
import java.util.Set;

/**
 * Why: every number the Vassal Musical Instrument plays with lives in this one pure table, so the balance budget of design.md section 5 can be pinned by a test
 * and a later edit that breaks it fails the build. The logic builds its skills from the {@link Row}s (so the table and the game cannot differ) and reads every other
 * constant from here, together with the few rules that turn the harmony of a phrase into a number (how wild a note flies, how long a song lasts, how strong a buff is).
 * Raw damage is before the level factor (1.0 to 2.2) and the balance law. Design, not canon: the figures are the author's.
 */
public final class InstrumentNumbers {
    private InstrumentNumbers() {}

    // ------------------------------------------------------------------ the budget rows (one per skill)
    public static final Row MUSIC_STRIKE = Row.damage("instrument.music_strike", Kind.ORDINARY, 8, 8, 1, 60, 0.01);
    public static final Row DEMONS_MELODY = Row.damage("instrument.demons_melody", Kind.ORDINARY, 9, 9, 8, 100, 0.02);
    public static final Row STUN_BEAT = Row.damage("instrument.stun_beat", Kind.ORDINARY, 6, 6, 6, 200, 0.02);
    public static final Row MARCH = Row.utility("instrument.march", 240, 0.02).reaching(8);
    public static final Row WAR_SONG = Row.utility("instrument.war_song", 300, 0.02).reaching(8);
    public static final Row LULLABY = Row.utility("instrument.lullaby", 260, 0.02).reaching(8);
    public static final Row DIRGE = Row.utility("instrument.dirge", 260, 0.02).reaching(8);
    public static final Row GLUTTON = Row.utility("instrument.glutton_god_tango", 400, 0.02).withHeal(0.10).reaching(6);
    public static final Row ZERO_NOTE = Row.utility("instrument.zero_note", 360, 0.01).reaching(8);
    public static final Row CRESCENDO = Row.damage("instrument.crescendo", Kind.ULTIMATE, 4, 24, 10, 700, 0.06).withHeal(0.15);

    public static final List<Row> ALL = List.of(MUSIC_STRIKE, DEMONS_MELODY, STUN_BEAT, MARCH, WAR_SONG, LULLABY, DIRGE, GLUTTON, ZERO_NOTE, CRESCENDO);

    /** The ids of the ultimates (one per form at most). */
    public static final Set<String> ULTIMATES = Set.of(CRESCENDO.id());

    // ------------------------------------------------------------------ Music Strike
    public static final double STRIKE_BASE = 6.0, STRIKE_PER_HARMONY = 0.5, STRIKE_RANGE = 24.0, STRIKE_WIDTH = 0.35;
    /** How many degrees a note may stray at harmony 0; each harmony takes {@code SPREAD_PER_HARMONY} off. */
    public static final double SPREAD_BASE = 6.0, SPREAD_PER_HARMONY = 2.0;

    // ------------------------------------------------------------------ Demon's Melody
    public static final int MELODY_NOTES = 7, MELODY_PER_TARGET = 3;
    public static final double MELODY_NOTE_RAW = 3.0, MELODY_RANGE = 14.0, MELODY_WIDTH = 0.4, MELODY_ANGLE_BASE = 20.0, MELODY_ANGLE_PER_HARMONY = 3.0, MELODY_ANGLE_MIN = 8.0;

    // ------------------------------------------------------------------ Stun Beat
    public static final double STUN_RANGE = 18.0, STUN_RADIUS = 3.5;
    public static final int STUN_FLIGHT = 10, STUN_CONTROL_TICKS = 40, STUN_BLIND_TICKS = 20;

    // ------------------------------------------------------------------ the songs
    /** A song pulses every {@code PULSE_EVERY} ticks, lasts its base time plus a tenth per harmony, and reaches its radius. */
    public static final int PULSE_EVERY = 20, EFFECT_TICKS = 60, MARCH_TICKS = 200, WAR_TICKS = 160;
    public static final double SONG_RADIUS = 10.0, CHOIR_RADIUS = 1.25;
    /** The one-shot songs. */
    public static final double LULLABY_RADIUS = 9.0, DIRGE_RADIUS = 9.0;
    public static final int LULLABY_TICKS = 120, DIRGE_TICKS = 120, DIRGE_EXTRA_TICKS = 40;
    /** From this harmony the songs are stronger (Speed II, Resistance with the war song, a longer dirge). */
    public static final int STRONG_HARMONY = 3;

    // ------------------------------------------------------------------ Glutton God Tango and Zero Note
    public static final double GLUTTON_RADIUS = 8.0, GLUTTON_HEAL_SHARE = 0.10, ZERO_RADIUS = 8.0;
    public static final int GLUTTON_FOOD = 4, ZERO_CLEANSE = 2;
    public static final float GLUTTON_SATURATION = 0.8f;

    // ------------------------------------------------------------------ Crescendo
    public static final int CRESCENDO_MIN_HARMONY = 3, CRESCENDO_TELL = 20, CRESCENDO_PULSES = 6, CRESCENDO_EVERY = 12;
    public static final double CRESCENDO_RADIUS = 10.0, CRESCENDO_HEAL_SHARE = 0.025;

    // ------------------------------------------------------------------ small pure rules
    /** How far (degrees) a note may stray: 6 degrees with no harmony, nothing from harmony 3; a wielder with perfect pitch never strays. */
    public static double spreadDeg(int harmony, boolean perfectPitch) {
        if (perfectPitch) return 0.0;
        return Math.max(0.0, SPREAD_BASE - SPREAD_PER_HARMONY * clamp(harmony));
    }

    /** Raw damage of a Music Strike: six, half a point more per harmony (eight at four). */
    public static double strikeRaw(int harmony) {
        return STRIKE_BASE + STRIKE_PER_HARMONY * clamp(harmony);
    }

    /** The half-angle (degrees) of the Demon's Melody fan: wide with no harmony, tighter as it builds, never under 8. */
    public static double melodyAngle(int harmony, boolean perfectPitch) {
        double a = Math.max(MELODY_ANGLE_MIN, MELODY_ANGLE_BASE - MELODY_ANGLE_PER_HARMONY * clamp(harmony));
        return perfectPitch ? Math.min(a, 12.0) : a;
    }

    /** How long a song lasts: its base time plus a tenth of it per harmony. */
    public static int songTicks(int base, int harmony) {
        return base + base * clamp(harmony) / 10;
    }

    /** The Speed amplifier of the March: Speed I, Speed II from harmony 3. */
    public static int marchAmp(int harmony) {
        return harmony >= STRONG_HARMONY ? 1 : 0;
    }

    /** The damage one target takes from the Melody: the notes that hit it (at most three) at three each. */
    public static double melodyDamage(int notesThatHit) {
        return MELODY_NOTE_RAW * Math.max(0, Math.min(MELODY_PER_TARGET, notesThatHit));
    }

    private static int clamp(int harmony) {
        return Math.max(0, Math.min(Tempo.MAX, harmony));
    }
}
