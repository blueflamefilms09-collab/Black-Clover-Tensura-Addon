package com.newuniverse.nusmp.star;

/**
 * Black Neverland, the manga version (docs/vfx_remake/star_ingame.md, section "Black Neverland, canon check"). The pure rules, with no Minecraft class so tests can run them anywhere.
 *
 * <p>What canon says (sources and gaps are listed in the doc): Black Neverland is a Union Magic of Yuno Grinberryall, his Star Magic joined with the Wind Spirit Sylph's magic and with Anti-Magic (he
 * wields Asta's Demon-Dweller sword, ch. 381 to 383). It gathers the surrounding mana into six anti-magic stars that form a sphere; the magic of the opponents inside is cancelled (it was the answer to
 * Lucius's Permeation Magic), and the anti-magic is "starting to fade" by ch. 383. It is a short-range, supplementary spell. The manga gives no numbers, so size, length and price below are this mod's own.
 *
 * <p>How the mod uses it. The Grinberryall heir mark stays the base gate (owner decision) and is always required. The canon extras are two SERVER FLAGS, both required while {@code star.neverlandCanonGate}
 * is true (default): {@code sylph_bond} (the caster is bonded to the wind spirit Sylph) and {@code anti_magic_source} (the caster holds a Demon-Dweller-family sword or is the Anti-Magic Lord). With the
 * switch false the extras are not required and the page works as in phase 1 (a dome that weakens foes, ends their bolts and strengthens allies); the ANTI-MAGIC part of the dome (silence, jammed skills,
 * shattered barriers, the Aura price) then applies only to a caster who really has the anti-magic source.
 */
public final class StarNeverlandRules {
    private StarNeverlandRules() {}

    public static final String CONFIG_KEY = "neverlandCanonGate";
    public static final boolean DEFAULT_CANON_GATE = true;

    /** The server flags this page reads (the names the doc and the lang text use). */
    public static final String FLAG_SYLPH = "sylph_bond", FLAG_ANTI_MAGIC = "anti_magic_source";

    /** The dome: 8 blocks wide, 100 ticks in pulses of 20, six anti-magic stars on the rim (the look draws exactly six), the anti-magic fading after 80 ticks (the last pulse is a plain dome). */
    public static final double RADIUS = 8.0;
    public static final int TICKS = 100, STEP = 20, FADE_AFTER = 80, RIM_STARS = 6;
    /** Foe seconds of silence a pulse gives (ticks); a pulse jams a foe's skills for 3 seconds only on every second pulse, as the Nihility zone does. */
    public static final int SILENCE_TICKS = 30, JAM_SECONDS = 3;
    public static final float JAM_MISFIRE = 0.1f;
    /** Aura price of the anti-magic dome: 8 percent of the max Aura, at least 30 (between Black Hurricane's 6 percent and Black Asta's 12; the sword's Meteorite stays the dearest). */
    public static final double AURA_FRAC = 0.08, AURA_FLOOR = 30.0;

    public static final String NOT_HEIR = "Only a Grinberryall heir can cast this (a Spade Star grimoire at full mastery, after the Spade Kingdom Raid).";
    public static final String NEEDS_SYLPH = "Black Neverland is a Union Magic with the wind spirit Sylph: you need her bond.";
    public static final String NEEDS_ANTI_MAGIC = "Black Neverland needs anti-magic: hold a Demon-Dweller sword (or be the Anti-Magic Lord).";
    public static final String NO_AURA = "Not enough aura to raise Black Neverland.";

    /** What a cast does: {@code problem} null = it may start; {@code antiMagic} = the dome cancels magic (and costs Aura). */
    public record Plan(String problem, boolean antiMagic) {
        public boolean ok() { return problem == null; }
    }

    /**
     * The decision. The heir mark is always needed. With the canon gate on, the Sylph bond and the anti-magic source are needed too and the dome is the anti-magic one. With the gate off they are not
     * needed, and the dome is the anti-magic one only if the caster has the source anyway.
     */
    public static Plan plan(boolean heir, boolean sylphBond, boolean antiMagicSource, boolean canonGate) {
        if (!heir) return new Plan(NOT_HEIR, false);
        if (canonGate) {
            if (!sylphBond) return new Plan(NEEDS_SYLPH, false);
            if (!antiMagicSource) return new Plan(NEEDS_ANTI_MAGIC, false);
            return new Plan(null, true);
        }
        return new Plan(null, antiMagicSource);
    }

    /** Does the dome still cancel magic at this age (ticks since it rose)? The anti-magic fades after {@link #FADE_AFTER} ticks, as in ch. 383. */
    public static boolean cancelsAt(int age) { return age < FADE_AFTER; }

    /** Does this pulse also jam the foes' skills (every second pulse, like the Nihility zone)? */
    public static boolean jamsAt(int age) { return (age / STEP) % 2 == 0; }

    /** How many pulses a dome has (ages 0, 20, ..., TICKS - STEP). */
    public static int pulses() { return TICKS / STEP; }

    /** How many of those still cancel magic. */
    public static int cancellingPulses() {
        int n = 0;
        for (int age = 0; age < TICKS; age += STEP) if (cancelsAt(age)) n++;
        return n;
    }
}
