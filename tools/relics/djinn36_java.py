#!/usr/bin/env python3
"""djinn36_java: writes client/relics/djinn/DjinnRoster36.java, the name-keyed motion, ambient-particle and dust-palette tables of the 24 new Djinn, from
djinn36_roster.py (the one source), so the Java and the art tools cannot drift.   python3 -B tools/relics/djinn36_java.py   [--check]"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import djinn36_roster as R                      # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
OUT = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "client", "relics", "djinn", "DjinnRoster36.java")

# how the dust of each affinity moves and how much of it there is a second
PROFILE = {"HEAT": ("RISE", 9), "WATER": ("FALL", 8), "WIND": ("SWIRL", 11), "LIFE": ("RISE", 7), "STRENGTH": ("HEAVY", 8), "SOUND": ("RING", 7), "LIGHT": ("TWINKLE", 9), "LIGHTNING": ("JITTER", 10)}

TEMPLATE = '''package com.newuniverse.nusmp.client.relics.djinn;

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
@@ROWS@@
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
'''


def row_text(name):
    flap, tails, hs, bs, bp, ws, tw = R.style_row(name)
    main, mr, second, sr, anchor = R.AMBIENT[name]
    motion, rate = PROFILE[R.row(name)[3]]
    ff = lambda v: ("%.2f" % v).rstrip("0").rstrip(".") + "f" if "." in ("%.2f" % v) else "%.1ff" % v
    return '        put("%s", new Style(%s, %d, %s, %s, %s, %s, %s),\n            new Ambient("%s", %d, "%s", %d, Anchor.%s), new Dust(Motion.%s, %d));' % (
        name, ff(flap), tails, ff(hs), ff(bs), ff(bp), ff(ws), ff(tw), main, mr, second, sr, anchor, motion, rate)


def text():
    return TEMPLATE.replace("@@ROWS@@", "\n".join(row_text(n) for n in R.NAMES))


def main(argv):
    t = text()
    if "--check" in argv:
        ok = os.path.exists(OUT) and open(OUT, encoding="utf-8").read() == t
        print("DjinnRoster36.java", "up to date" if ok else "OUT OF DATE")
        return 0 if ok else 1
    with open(OUT, "w", encoding="utf-8", newline="\n") as f:
        f.write(t)
    print("wrote", OUT)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
