package com.newuniverse.nusmp.ultimate;

import com.newuniverse.nusmp.blackclover.Kingdom;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the charge pose of an Ultimate depends on who casts it. Each kingdom of the Black Clover world has its own wind-up, hold and release clip (assets/nusmp/animations/player/cast.animation.json, written by
 * tools/gen_ultimate_anims.py); a caster without a kingdom, and a server that switched {@code ultimate.kingdomAnimations} off, keep the old single looping Mana Zone pose and the Signature release. Pure (no game
 * class), unit tested; {@link UltimateManager} asks it once per cast, on the server, and sends the chosen clip names with the EXISTING cast-animation payload (no packet of its own, rule 9).
 *
 * <p>The three phases: {@code start} (the wind-up pose, {@link #START_TICKS} ticks, begins with the cast), {@code loop} (the long hold, sent once when the start is over) and {@code release} (sent at the release,
 * on the first pulse). The start clip is authored as a looping clip so that the client blends the loop out of it (CastAnimClient only blends from a looping clip); it is 1 second long and the loop replaces it at
 * tick 10, so it never wraps. A phase that is {@code null} is skipped (the default has no start clip: the loop begins at once, exactly as before).
 */
public final class UltimateAnim {
    private UltimateAnim() {}

    /** Ticks of the wind-up pose before the long hold begins. */
    public static final int START_TICKS = 10;

    /** The three clip names of one caster ({@code start} may be null). */
    public record Clips(String start, String loop, String release) {
        /** The clip that begins with the cast. */
        public String first() { return start != null ? start : loop; }

        /** Is the long hold due now (the first tick after the start pose is over)? Never for a style without a start clip. */
        public boolean loopDue(int age) { return start != null && age == START_TICKS; }
    }

    /** The old behaviour: the looping Mana Zone pose all the way, the Signature release. (The names are those of anim.CastAnim.MANA_ZONE / SIGNATURE.) */
    public static final Clips DEFAULT = new Clips(null, "cast_mana_zone", "cast_signature");

    /** The clip id prefix of every clip this class names. */
    public static final String PREFIX = "ult_";

    /** The kingdoms that have their own animation, in declaration order of {@link Kingdom}. */
    public static List<Kingdom> styled() {
        List<Kingdom> out = new ArrayList<>();
        for (Kingdom k : Kingdom.values()) if (forKingdom(k) != DEFAULT) out.add(k);
        return out;
    }

    /** The clips of a kingdom's users; {@link #DEFAULT} for no kingdom. */
    public static Clips forKingdom(Kingdom k) {
        if (k == null) return DEFAULT;
        return switch (k) {
            case CLOVER -> of("clover");
            case DIAMOND -> of("diamond");
            case SPADE -> of("spade");
            case HEART -> of("heart");
            default -> DEFAULT;                                         // a kingdom added later keeps the old pose until its clips exist (UltimateAnimTest fails until then)
        };
    }

    private static Clips of(String id) { return new Clips(PREFIX + id + "_start", PREFIX + id + "_loop", PREFIX + id + "_release"); }

    /** The clips for a cast: the kingdom's own when the server setting is on, else the old single loop. */
    public static Clips choose(boolean kingdomAnimations, Kingdom k) { return kingdomAnimations ? forKingdom(k) : DEFAULT; }

    /** The caster's kingdom: the one they hail from (the Tower Wizard's heritage), else the kingdom of the grimoire cover that accepted them, else none. */
    public static Kingdom resolveKingdom(Kingdom heritage, Kingdom cover) { return heritage != null ? heritage : cover; }

    /** Every clip name this class can hand out, for the tests and the docs (the default's two old clips are not listed). */
    public static List<String> ids() {
        List<String> out = new ArrayList<>();
        for (Kingdom k : styled()) {
            Clips c = forKingdom(k);
            out.add(c.start());
            out.add(c.loop());
            out.add(c.release());
        }
        return out;
    }
}
