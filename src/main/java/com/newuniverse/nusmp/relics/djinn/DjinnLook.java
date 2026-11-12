package com.newuniverse.nusmp.relics.djinn;

import java.util.HashMap;
import java.util.Map;

/**
 * Why: until 3.75.0 every Djinn's magic was drawn with the same white-gold Rukh whatever its element, so Amon's fire and Vinea's water looked alike. This pure table (no Minecraft type, so
 * JUnit reads it) says what each of the eight kinds of Djinn magic looks like: the particle THEME its motes come from (embers, sparks, droplets, feathers, leaves, rock chips, notes, stars) and
 * the mesh effect of the mod's effect library for four moments of a spell: a CAST that travels (beams, cones, cross arms), a ZONE on the ground (the telegraph, the vortex), an IMPACT (novas,
 * quakes, the equip flare) and a STRIKE on a marked point (bolts, columns, rain). Shapes and themes are named as strings and checked against {@code VfxShape} and
 * {@code particle_themes.json} by {@code DjinnLookTest}. {@link #affinityOf} finds the element from a Djinn's own colour, which is what the old helpers were handed.
 */
public final class DjinnLook {
    private DjinnLook() {}

    public enum Moment { CAST, ZONE, IMPACT, STRIKE }

    private static final String[] THEME = new String[Affinity.values().length];
    private static final String[][] SHAPE = new String[Affinity.values().length][Moment.values().length];

    private static void row(Affinity a, String theme, String cast, String zone, String impact, String strike) {
        THEME[a.ordinal()] = theme;
        SHAPE[a.ordinal()] = new String[] {cast, zone, impact, strike};
    }

    static {
        row(Affinity.HEAT, "magic.FLAME", "FLAMEFX_CAST", "FLAMEFX_ZONE", "FLAMEFX_BURST", "FIRE_PILLAR");                    // a lance of flame, a burning field, a fireball, a pillar of fire
        row(Affinity.LIGHTNING, "magic.LIGHTNING", "LIGHTNINGFX_CAST", "LIGHTNINGFX_ZONE", "LIGHTNINGFX_BURST", "LIGHTNING_SPEAR"); // an arc, a crackling field, a thunderclap, a jagged bolt from the sky
        row(Affinity.WATER, "magic.WATER", "WATERFX_CAST", "WATERFX_ZONE", "WATERFX_BURST", "WATER_BURST");                    // a jet, a tide pool, a crash of water, a splash crown
        row(Affinity.WIND, "magic.WIND", "WIND_GALE", "WIND_TORNADO", "WIND_RING", "WIND_TORNADO");                            // streaks of gale, a tornado, gust rings, a small twister
        row(Affinity.LIFE, "magic.PLANT", "PLANT_FX1", "PLANT_FX2", "PLANT_FX3", "TREE_ROOTS");                                // a thrown growth, a living field, a bloom, roots breaking the ground
        row(Affinity.STRENGTH, "magic.EARTH", "EARTHFAM_CAST", "EARTHFAM_ZONE", "EARTHFAM_BURST", "EARTHFAM_BURST");           // thrown rock, a ground sigil with debris, a stone impact
        row(Affinity.SOUND, "magic.SOUND", "SOUND_FX1", "SOUND_FX2", "SOUND_FX3", "SONG_RING");                                // a sound wave, a resonating field, a boom, a ring of sound on the ground
        row(Affinity.LIGHT, "magic.LIGHT", "DIVINE_RAY", "DIVINE_SIGIL", "LIGHT_FLARE", "DIVINE_PILLAR");                      // a ray, a sigil of light, a flare, a pillar of light
    }

    /** The particle theme of an element ({@code data/nusmp/particle_themes.json}). */
    public static String theme(Affinity a) { return THEME[a.ordinal()]; }

    /** The name of the {@code VfxShape} an element uses at a moment. */
    public static String shape(Affinity a, Moment m) { return SHAPE[a.ordinal()][m.ordinal()]; }

    /** True when the element's STRIKE is drawn from the sky down to the mark (a bolt), not standing on the mark. */
    public static boolean fromSky(Affinity a) { return a == Affinity.LIGHTNING; }

    /** How long the mesh effect of a moment lives, in ticks. */
    public static int ticks(Moment m) {
        return switch (m) {
            case CAST -> 16;
            case ZONE -> 70;
            case IMPACT -> 26;
            case STRIKE -> 22;
        };
    }

    private static final Map<Integer, Affinity> BY_COLOUR = new HashMap<>();
    private static final Affinity[] AMBIGUOUS = new Affinity[0];

    static {
        Map<Integer, Affinity[]> seen = new HashMap<>();
        for (Djinn d : Djinn.values())
            for (int rgb : new int[] {d.color() & 0xFFFFFF, d.glow() & 0xFFFFFF}) {
                Affinity[] had = seen.get(rgb);
                if (had == null) seen.put(rgb, new Affinity[] {d.affinity()});
                else if (had != AMBIGUOUS && had[0] != d.affinity()) seen.put(rgb, AMBIGUOUS);
            }
        for (Map.Entry<Integer, Affinity[]> e : seen.entrySet()) if (e.getValue() != AMBIGUOUS) BY_COLOUR.put(e.getKey(), e.getValue()[0]);
    }

    /** The element of the Djinn that owns this colour (its body colour or its glow), or null when no Djinn has it or Djinns of different elements share it (plain white). */
    public static Affinity affinityOf(int rgb) { return BY_COLOUR.get(rgb & 0xFFFFFF); }
}
