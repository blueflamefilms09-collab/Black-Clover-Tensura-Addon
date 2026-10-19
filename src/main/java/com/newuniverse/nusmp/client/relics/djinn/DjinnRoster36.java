package com.newuniverse.nusmp.client.relics.djinn;

import com.newuniverse.nusmp.client.relics.djinn.DjinnLook.Ambient;
import com.newuniverse.nusmp.client.relics.djinn.DjinnLook.Anchor;
import com.newuniverse.nusmp.client.relics.djinn.DjinnLook.Style;
import com.newuniverse.nusmp.relics.djinn.Djinn;
import java.util.HashMap;
import java.util.Map;

/**
 * The look tables of the 24 Djinn added after Vassago (Bael to Murmur), keyed by the enum constant's NAME so that this class compiles and works whatever
 * the order or the Look of the enum entries is: {@link #styleOf} and {@link #ambientOf} answer for a Djinn's ordinal (the first twelve keep their rows in
 * {@link DjinnLook}); {@link #dustOf} and {@link #colours} describe the Djinn's dust, a particle in the colours of the Djinn (its glow fading to its
 * body colour, black and violet for a Dark Djinn) moving the way its affinity moves; {@link DjinnRoster36Fx} spawns it (this class has no Minecraft types, so it is unit tested). The art itself (meshes, skins, runes, busts, weapons) is read by name from
 * assets/nusmp/djinn and textures/entity/djinn like the first twelve; tools/relics/djinn36_build.py writes it and tools/relics/djinn36_java.py writes THIS FILE
 * from the same table (tools/relics/djinn36_roster.py).
 *
 * <p>The tail rule of the first twelve holds: a Djinn whose Look has no tail has no tail segments in its row (and one that has a tail has some), whatever
 * the table says, so a later change of a Look cannot break it.
 */
public final class DjinnRoster36 {
    private DjinnRoster36() {}

    /** How the dust of an affinity moves. */
    enum Motion { RISE, FALL, SWIRL, HEAVY, RING, TWINKLE, JITTER }

    /** The dust of one Djinn: its motion, its rate (particles a second at the Full Equip). */
    record Dust(Motion motion, int rate) {}

    private static final Map<String, Style> STYLES = new HashMap<>();
    private static final Map<String, Ambient> AMBIENT = new HashMap<>();
    private static final Map<String, Dust> DUST = new HashMap<>();

    private static void put(String name, Style style, Ambient ambient, Dust dust) {
        STYLES.put(name, style);
        AMBIENT.put(name, ambient);
        DUST.put(name, dust);
    }

    static {
        put("BAEL", new Style(0.9f, 0, 0f, 0f, 0.02f, 0.75f, 1f),
            new Ambient("mote", 9, "spark", 3, Anchor.ABOVE), new Dust(Motion.HEAVY, 8));
        put("AGARES", new Style(0.7f, 6, 0f, 0f, 0f, 0.8f, 1.4f),
            new Ambient("bubble", 8, "ash", 3, Anchor.FEET), new Dust(Motion.FALL, 8));
        put("VALEFOR", new Style(1.4f, 4, 1.8f, 0f, 0.07f, 0.95f, 1.3f),
            new Ambient("crit", 9, "spark", 4, Anchor.WINGS), new Dust(Motion.JITTER, 10));
        put("BARBATOS", new Style(0.8f, 5, 1f, 0f, 0.03f, 0.8f, 1.1f),
            new Ambient("petal", 7, "feather", 3, Anchor.WINGS), new Dust(Motion.RISE, 7));
        put("GUSION", new Style(0.6f, 0, 1.6f, 0.4f, 0.02f, 0.85f, 1f),
            new Ambient("drip", 7, "mote2", 4, Anchor.ABOVE), new Dust(Motion.FALL, 8));
        put("VEPAR", new Style(0.65f, 4, 0f, 1f, 0f, 0.8f, 1.5f),
            new Ambient("bubble", 11, "snow", 3, Anchor.WINGS), new Dust(Motion.FALL, 8));
        put("SHAX", new Style(1.2f, 2, 0.5f, 0f, 0.04f, 0.85f, 0.5f),
            new Ambient("feather", 9, "enchant", 5, Anchor.ABOVE), new Dust(Motion.RING, 7));
        put("BUER", new Style(0.9f, 0, 0.7f, 1.6f, 0f, 0.8f, 1f),
            new Ambient("spore", 8, "petal", 4, Anchor.BODY), new Dust(Motion.RISE, 7));
        put("ANDREALPHUS", new Style(1f, 5, 0.9f, 0f, 0.09f, 0.78f, 1f),
            new Ambient("mote2", 10, "enchant", 5, Anchor.WINGS), new Dust(Motion.TWINKLE, 9));
        put("KIMARIS", new Style(1.3f, 6, 0f, 0f, 0.05f, 0.7f, 1.2f),
            new Ambient("smoke", 9, "flame", 4, Anchor.FEET), new Dust(Motion.HEAVY, 8));
        put("AMDUSIAS", new Style(1.1f, 3, 1.3f, 0f, 0.08f, 0.9f, 0.9f),
            new Ambient("note", 4, "mote", 7, Anchor.ABOVE), new Dust(Motion.RING, 7));
        put("BELIAL", new Style(0.5f, 3, 0.4f, 0.3f, 0.06f, 0.72f, 0.4f),
            new Ambient("ash", 8, "smoke", 5, Anchor.ABOVE), new Dust(Motion.RISE, 9));
        put("DECARABIA", new Style(0.75f, 3, 2.2f, 0.5f, 0f, 0.8f, 0.7f),
            new Ambient("drip", 5, "shard", 5, Anchor.FEET), new Dust(Motion.TWINKLE, 9));
        put("SEERE", new Style(1.9f, 4, 2.6f, -1.8f, 0f, 0.85f, 1.7f),
            new Ambient("wind", 12, "snow", 3, Anchor.FEET), new Dust(Motion.SWIRL, 11));
        put("DANTALION", new Style(0.9f, 0, 1.4f, 0f, 0.06f, 0.88f, 1f),
            new Ambient("enchant", 11, "ink", 3, Anchor.ABOVE), new Dust(Motion.TWINKLE, 9));
        put("ANDROMALIUS", new Style(0.8f, 5, 0.6f, 0f, 0.1f, 0.8f, 0.9f),
            new Ambient("ink", 7, "crit", 6, Anchor.BODY), new Dust(Motion.JITTER, 10));
        put("GLASYA_LABOLAS", new Style(1.5f, 5, 0f, 0f, 0f, 1f, 1.6f),
            new Ambient("ember", 10, "ash", 5, Anchor.BODY), new Dust(Motion.HEAVY, 8));
        put("HAAGENTI", new Style(0.6f, 4, 0f, 0.9f, 0f, 0.75f, 0.6f),
            new Ambient("flame", 6, "smoke", 5, Anchor.HAND), new Dust(Motion.HEAVY, 8));
        put("CROCELL", new Style(1.1f, 0, 2.8f, 0.6f, 0.01f, 0.85f, 1f),
            new Ambient("snow", 12, "shard", 3, Anchor.WINGS), new Dust(Motion.FALL, 8));
        put("FURCAS", new Style(0.55f, 0, 0f, 0f, 0f, 0.72f, 1f),
            new Ambient("snow", 7, "wind", 5, Anchor.BODY), new Dust(Motion.SWIRL, 11));
        put("BALAM", new Style(1.2f, 5, 1.1f, 0f, 0.11f, 0.82f, 1.3f),
            new Ambient("flame", 8, "mote", 4, Anchor.ABOVE), new Dust(Motion.RISE, 9));
        put("ALLOCES", new Style(1.6f, 4, 1.7f, 1.4f, 0.04f, 0.76f, 1.1f),
            new Ambient("ember", 9, "spark", 4, Anchor.WINGS), new Dust(Motion.RISE, 9));
        put("CAMIO", new Style(1.7f, 3, 0f, 0.7f, 0f, 0.9f, 1f),
            new Ambient("feather", 6, "note", 3, Anchor.WINGS), new Dust(Motion.RING, 7));
        put("MURMUR", new Style(0.85f, 3, 0.8f, 0f, 0.12f, 0.82f, 0.7f),
            new Ambient("spark", 6, "note", 3, Anchor.HAND), new Dust(Motion.RING, 7));
    }

    /** True for one of the 24. */
    public static boolean knows(String name) { return name != null && STYLES.containsKey(name); }

    /** The motion row of the Djinn with this ordinal, or null if it is not one of the 24. */
    static Style styleOf(int ordinal) {
        Djinn d = Djinn.byOrdinal(ordinal);
        Style s = d == null ? null : STYLES.get(d.name());
        if (s == null) return null;
        boolean tail = d.look().tail() != Djinn.Look.Tail.NONE;
        if (tail == s.tailSegs() > 0) return s;
        return new Style(s.flap(), tail ? Math.max(2, s.tailSegs()) : 0, s.haloSpin(), s.backSpin(), s.backPulse(), s.weaponScale(), s.tailWave());
    }

    /** The ambient particle recipe of the Djinn with this ordinal, or null if it is not one of the 24. */
    static Ambient ambientOf(int ordinal) {
        Djinn d = Djinn.byOrdinal(ordinal);
        return d == null ? null : AMBIENT.get(d.name());
    }

    /** The dust of the Djinn with this name, or null. */
    static Dust dustOf(String name) { return DUST.get(name); }

    /** The ambient recipe of the Djinn with this name, or null. */
    static Ambient ambientOfName(String name) { return AMBIENT.get(name); }

    /** The two colours of the dust: the Djinn's glow fading into its body colour; a Dark Djinn's saturated colour fading into black violet. */
    static int[] colours(Djinn d, boolean dark) {
        return dark ? new int[]{DjinnLook.darkGlow(d.color(), d.glow()), 0x120818} : new int[]{d.glow() & 0xFFFFFF, d.color() & 0xFFFFFF};
    }
}
