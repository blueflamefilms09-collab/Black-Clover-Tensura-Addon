package com.newuniverse.nusmp.song;

/**
 * Why: Song Magic is played by choosing a mode (one page each) and then singing; this table is the single source of what each mode
 * does in numbers (cost, strength, durations) so the spell code only applies them and the tests can pin them down. Ids are stored on
 * the skill tag as {@link #id}; 0 is "no mode" and is never reassigned.
 */
public enum SongMode {
    NONE(0, "none", "Silence", 0.0),
    HEAL(1, "song_mode_heal", "Healing Hymn", 0.8),
    LULLABY(2, "song_mode_lullaby", "Lullaby", 1.0),
    BARRIER(3, "song_mode_barrier", "Barrier Chorus", 1.2),
    REQUIEM(4, "song_mode_requiem", "Requiem", 1.5);

    public final int id;
    public final String pageId, label;
    /** Percent of max magicule one pulse costs. */
    public final double costPercent;

    SongMode(int id, String pageId, String label, double costPercent) {
        this.id = id; this.pageId = pageId; this.label = label; this.costPercent = costPercent;
    }

    public static SongMode byId(int id) {
        for (SongMode m : values()) if (m.id == id) return m;
        return NONE;
    }

    /** Skill level 1..5 from the book's mastery fraction (0..1). */
    public static int skillLevel(double masteryFrac) {
        return 1 + (int) Math.min(4, Math.max(0, Math.floor(masteryFrac * 5.0)));
    }

    /** Healing Hymn: fraction of max health an ally gets per pulse: 2% per level, scaled by the voice strength. */
    public static float healFraction(int level, float strength) { return 0.02f * level * strength; }

    /** Lullaby: base sleep in ticks before the control law clamps it: up to 3 seconds at full voice. */
    public static int sleepTicks(float strength) { return Math.round(60f * strength); }

    /** Barrier Chorus: Absorption amplifier (0 = 2 hearts) from the skill level. */
    public static int absorptionAmplifier(int level) { return level >= 4 ? 1 : 0; }

    /** Barrier Chorus: Absorption lasts 6 seconds. */
    public static final int ABSORPTION_TICKS = 120;

    /** Barrier Chorus: how far enemies are thrown, in blocks, at full strength (3). */
    public static double knockDistance(float strength) { return 3.0 * strength; }

    /** Requiem: raw damage before the book's multipliers, weak on purpose. */
    public static float requiemDamage(int level, float strength) { return (1.0f + 0.5f * level) * strength; }

    /** Magicule cost of one pulse in percent of max magicule. */
    public double pulseCostPercent(boolean crescendo) { return costPercent * (crescendo ? 3.0 : 1.0); }
}
