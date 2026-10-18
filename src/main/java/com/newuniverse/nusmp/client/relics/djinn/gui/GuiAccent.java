package com.newuniverse.nusmp.client.relics.djinn.gui;

import com.newuniverse.nusmp.relics.djinn.Affinity;

import java.util.Map;

/**
 * The accent: the one big shape that stands behind a Djinn's emblem and is its signature. The twelve Djinn of the first roster each have their own
 * (hand tuned, {@link #TUNED}); any other Djinn gets one chosen from the pool of its Affinity by a hash of its id, so two new Djinn of the same magic
 * usually look different and the choice never changes between runs.
 */
public enum GuiAccent {
    /** A slow ring of dots with four heavy beads, turning. */
    BLOOD_RING,
    /** A half sun of rays above the head, breathing. */
    SUN_ARCH,
    /** Two ribbons of water weaving behind the shoulders. */
    WATER_RIBBONS,
    /** Two tilted rings spinning against each other. */
    CYCLONE_RINGS,
    /** A crown of leaves round the head. */
    VINE_CROWN,
    /** Six shards of crystal circling the head. */
    SHARD_CROWN,
    /** Arcs of sound opening to either side, like a tuning fork's field. */
    SOUND_FORK,
    /** A mane of flame streaming up and back from the head. */
    EMBER_MANE,
    /** A fan of dawn rays, half circle. */
    DAWN_FAN,
    /** Jagged arcs that flicker round the medallion. */
    STORM_ARCS,
    /** A full moon behind the head, still. */
    MOON,
    /** A thin crescent, tilted, breathing. */
    REAPER_MOON;

    /** Hand tuned traits of one Djinn: its accent, how fast it moves, the corner ornament (0..3), the rune glyph set (0..2) and the turning direction. */
    public record Tune(GuiAccent accent, float tempo, int corner, int rune, int spin) {}

    private static final Map<String, Tune> TUNED = Map.ofEntries(
            Map.entry("baal", new Tune(BLOOD_RING, 0.9f, 0, 0, 1)),
            Map.entry("amon", new Tune(SUN_ARCH, 1.2f, 3, 1, 1)),
            Map.entry("vinea", new Tune(WATER_RIBBONS, 0.8f, 1, 2, -1)),
            Map.entry("focalor", new Tune(CYCLONE_RINGS, 1.3f, 2, 1, -1)),
            Map.entry("zagan", new Tune(VINE_CROWN, 0.7f, 1, 2, 1)),
            Map.entry("zepar", new Tune(SHARD_CROWN, 0.6f, 2, 0, 1)),
            Map.entry("furfur", new Tune(SOUND_FORK, 0.9f, 1, 1, -1)),
            Map.entry("marchosias", new Tune(EMBER_MANE, 1.2f, 3, 0, -1)),
            Map.entry("sitri", new Tune(DAWN_FAN, 0.8f, 0, 2, 1)),
            Map.entry("bifron", new Tune(STORM_ARCS, 1.4f, 3, 1, -1)),
            Map.entry("paimon", new Tune(MOON, 0.8f, 0, 2, 1)),
            Map.entry("vassago", new Tune(REAPER_MOON, 0.7f, 2, 0, -1)));

    /** The accents a Djinn of an Affinity may get when it has no hand tuned row, two or three per magic. */
    public static GuiAccent[] pool(Affinity a) {
        if (a == null) return new GuiAccent[]{MOON, CYCLONE_RINGS};
        return switch (a) {
            case HEAT -> new GuiAccent[]{SUN_ARCH, EMBER_MANE};
            case LIGHTNING -> new GuiAccent[]{STORM_ARCS, BLOOD_RING};
            case WATER -> new GuiAccent[]{WATER_RIBBONS, REAPER_MOON};
            case WIND -> new GuiAccent[]{CYCLONE_RINGS, MOON};
            case LIFE -> new GuiAccent[]{VINE_CROWN, DAWN_FAN};
            case STRENGTH -> new GuiAccent[]{SHARD_CROWN, BLOOD_RING};
            case SOUND -> new GuiAccent[]{SOUND_FORK, CYCLONE_RINGS};
            case LIGHT -> new GuiAccent[]{DAWN_FAN, MOON, SUN_ARCH};
            default -> new GuiAccent[]{MOON, CYCLONE_RINGS};
        };
    }

    /** True for a Djinn whose traits were tuned by hand. */
    public static boolean isTuned(String id) { return id != null && TUNED.containsKey(id); }

    /** The traits of a Djinn: the hand tuned row if there is one, else derived from its id and Affinity (stable). */
    public static Tune tuneOf(String id, Affinity a) {
        Tune t = id == null ? null : TUNED.get(id);
        if (t != null) return t;
        int h = GuiAnim.hash(id == null ? 0 : id.hashCode());
        GuiAccent[] pool = pool(a);
        GuiAccent acc = pool[(h >>> 3) % pool.length];
        float tempo = 0.75f + ((h >>> 9) & 7) * 0.08f;                       // 0.75 .. 1.31
        return new Tune(acc, tempo, (h >>> 12) & 3, ((h >>> 14) & 255) % 3, ((h >>> 22) & 1) == 0 ? 1 : -1);
    }
}
