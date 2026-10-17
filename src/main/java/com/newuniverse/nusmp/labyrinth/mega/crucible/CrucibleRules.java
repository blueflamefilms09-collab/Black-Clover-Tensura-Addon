package com.newuniverse.nusmp.labyrinth.mega.crucible;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the Crucible of Choice is a balance rule before it is a block entity: "exactly one relic per region, the rest forfeit, however many players
 * click and whatever happens in between". A rule that strict is only believable when it is a handful of pure functions with a test for every
 * transition, not a tangle of if-statements inside a click handler, so everything the Crucible DECIDES lives here (no Minecraft types, no
 * Gson; the sector's saved {@code Sector.Crucible.Phase} is mirrored by {@link Phase} because the real enum sits in a class that needs the game):
 * which phase follows which, how many relics the vault offers, the lowest grade it may offer, which clicks are legal and what each one
 * costs the other pedestals, which catastrophe a pick draws, and how hard the Cursed Spirit and the flood are at a given threat. The game glue
 * ({@code CrucibleHook}, {@code CrucibleVault}, {@code CatastropheRunner}) reads the saved state, asks one of these functions and applies the
 * answer, so the interesting part of the one-pick guarantee, "a second click in the same tick finds Phase CHOSEN", is the single line in
 * {@link #take} that says so and is pinned by {@code CrucibleRulesTest}.
 */
public final class CrucibleRules {
    private CrucibleRules() {}

    /** Mirrors {@code Sector.Crucible.Phase} by name (the glue converts through {@link #of}). */
    public enum Phase {
        /** Nobody entered the vault yet: nothing is rolled, no relic is chosen. */
        UNOPENED,
        /** The offer is drawn and shown on the pedestals; one may be taken. */
        ROLLED,
        /** One was taken: the others are gone, the catastrophe runs. */
        CHOSEN,
        /** The vault is done (the catastrophe ended, the vault was empty, or a restart ended it). */
        RESOLVED;

        /** The phase with this name, UNOPENED for anything unknown (an old save must still load). */
        public static Phase of(String name) {
            if (name != null) for (Phase p : values()) if (p.name().equals(name)) return p;
            return UNOPENED;
        }
    }

    /** Most relics a Crucible ever offers. */
    public static final int MAX_OFFER = 3;
    /** The highest grade ordinal ({@code Grade.GENESIS}). */
    public static final int GRADE_MAX = 6;
    /** The vault box: a player this close to the vault centre is "in the vault" (horizontal half-size and vertical half-size, blocks). */
    public static final int BOX_HORIZONTAL = 14, BOX_VERTICAL = 8;

    // ---------------------------------------------------------------- the offer
    /** How many relics to put out: the configured count ({@code offerCount}, 1 to 3, 3 by default) but never more than the vault has pedestals. */
    public static int offerCount(int configured, int pedestals) {
        return Math.max(0, Math.min(Math.min(MAX_OFFER, Math.max(1, configured)), pedestals));
    }

    /** The offer is drawn only for a vault nobody has opened. */
    public static boolean shouldRoll(Phase phase) { return phase == Phase.UNOPENED; }

    /** Is the offset from the vault centre inside the vault box? */
    public static boolean inVaultBox(int dx, int dy, int dz) {
        return Math.abs(dx) <= BOX_HORIZONTAL && Math.abs(dz) <= BOX_HORIZONTAL && Math.abs(dy) <= BOX_VERTICAL;
    }

    // ---------------------------------------------------------------- the grades
    /**
     * The relic grades by lower-case name, lowest to highest: the order of {@code relics.Grade} (a test reads that file and compares, so the two cannot drift
     * apart). The Crucible never rolls by sector depth: every vault draws from the configured minimum grade up to GENESIS, weighted by the grade's own loot
     * weight (so Genesis stays very rare), which is why only a minimum is needed here.
     */
    public static final List<String> GRADES = List.of("normal", "special", "rare", "unique", "legend", "god", "genesis");
    /** The ordinal of UNIQUE: the lowest grade a vault offers unless the owner changes {@code minGrade}. */
    public static final int GRADE_UNIQUE = 3;

    /**
     * The ordinal (0 to 6) of a grade named in the config: any case, with or without a trailing "grade" ("Legend Grade"), and {@code mythical}, the owner's name
     * for GOD. -1 when the text is not a grade (the config validator uses this to refuse it).
     */
    public static int gradeOrdinal(String name) {
        if (name == null) return -1;
        String s = name.trim().toLowerCase(java.util.Locale.ROOT);
        if (s.endsWith(" grade")) s = s.substring(0, s.length() - 6).trim();
        if (s.equals("mythical")) s = "god";
        return GRADES.indexOf(s);
    }

    /** The lowest grade the vault offers: the configured one, or UNIQUE when the value is missing or is not a grade. */
    public static int minGrade(String configured) {
        int g = gradeOrdinal(configured);
        return g < 0 ? GRADE_UNIQUE : g;
    }

    // ---------------------------------------------------------------- the pick
    /** What a click on a pedestal means. */
    public enum Verdict {
        /** Legal: this relic is taken and the others vanish. */
        PICK,
        /** The offer was not drawn yet (UNOPENED). */
        NOT_OFFERED,
        /** A relic was already taken (CHOSEN) or the vault is done (RESOLVED): the strict one-per-region rule. */
        TOO_LATE,
        /** The pedestal's relic is already gone. */
        EMPTY_SLOT,
        /** There is no such slot in the offer. */
        UNKNOWN_SLOT
    }

    /** The verdict, the clicked slot, the relic it holds (empty unless PICK) and the OTHER slots whose relics vanish with the pick. */
    public record Pick(Verdict verdict, int slot, String relicId, List<Integer> crumble) {}

    /**
     * A click on slot {@code slot}. Legal only in ROLLED, and exactly once: the caller writes CHOSEN FIRST on a PICK, so the next call, a second
     * player in the same tick or a double click, meets {@link Verdict#TOO_LATE}. The slots listed in {@code crumble} are every other slot that still
     * holds a relic.
     */
    public static Pick take(Phase phase, int slot, List<String> offered) {
        if (phase == Phase.UNOPENED) return new Pick(Verdict.NOT_OFFERED, slot, "", List.of());
        if (phase == Phase.CHOSEN || phase == Phase.RESOLVED) return new Pick(Verdict.TOO_LATE, slot, "", List.of());
        if (offered == null || slot < 0 || slot >= offered.size()) return new Pick(Verdict.UNKNOWN_SLOT, slot, "", List.of());
        String id = offered.get(slot);
        if (id == null || id.isEmpty()) return new Pick(Verdict.EMPTY_SLOT, slot, "", List.of());
        List<Integer> others = new ArrayList<>();
        for (int i = 0; i < offered.size(); i++) if (i != slot && offered.get(i) != null && !offered.get(i).isEmpty()) others.add(i);
        return new Pick(Verdict.PICK, slot, id, Collections.unmodifiableList(others));
    }

    // ---------------------------------------------------------------- the phases
    /** After the offer was drawn: ROLLED, or RESOLVED when no relic could be offered (the world has none left of that depth). */
    public static Phase afterRoll(int offeredCount) { return offeredCount > 0 ? Phase.ROLLED : Phase.RESOLVED; }

    /** After a legal pick. */
    public static Phase afterPick() { return Phase.CHOSEN; }

    /** After the catastrophe ended (or was cut short): only CHOSEN becomes RESOLVED. */
    public static Phase afterCatastrophe(Phase phase) { return phase == Phase.CHOSEN ? Phase.RESOLVED : phase; }

    /** A server restart in the middle of a catastrophe simply ends it, so nobody is trapped by a countdown that no longer runs. */
    public static Phase afterRestart(Phase phase) { return afterCatastrophe(phase); }

    // ---------------------------------------------------------------- the catastrophe
    public static final String COLLAPSE = "collapse", FLOOD = "flood", SPIRIT = "spirit";
    public static final List<String> KINDS = List.of(COLLAPSE, FLOOD, SPIRIT);

    /** splitmix64's finaliser: a bijection with good avalanche, enough for a one-shot draw. */
    static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** The seed of the draw: the sector's seed and the chooser, mixed so that either alone changes it. */
    public static long drawSeed(long sectorSeed, UUID chooser) {
        long c = chooser == null ? 0L : chooser.getMostSignificantBits() * 31L + chooser.getLeastSignificantBits();
        return mix(sectorSeed * 0x9E3779B97F4A7C15L + mix(c ^ 0x632BE59BD9B4E019L));
    }

    /**
     * The catastrophe a pick draws: a weighted choice among collapse, flood and spirit with the theme's weights, deterministic from the sector's seed
     * and the chooser (the same pick in the same sector always draws the same, so a test can pin it and a restart cannot re-roll it). A kind with
     * weight 0 is never drawn; {@code spiritAvailable} false (no roster entity exists in this game) takes the spirit out of the draw; when nothing is
     * drawable the answer is {@link #COLLAPSE}, the one catastrophe that needs no entity.
     */
    public static String chooseCatastrophe(Map<String, Integer> weights, long sectorSeed, UUID chooser, boolean spiritAvailable) {
        long[] w = new long[KINDS.size()];
        long total = 0;
        for (int i = 0; i < w.length; i++) {
            Integer v = weights == null ? null : weights.get(KINDS.get(i));
            w[i] = v == null ? 0 : Math.max(0, v);
            if (KINDS.get(i).equals(SPIRIT) && !spiritAvailable) w[i] = 0;
            total += w[i];
        }
        if (total <= 0) return COLLAPSE;
        long r = Math.floorMod(drawSeed(sectorSeed, chooser), total);
        for (int i = 0; i < w.length; i++) {
            if (r < w[i]) return KINDS.get(i);
            r -= w[i];
        }
        return COLLAPSE;                                                           // unreachable: the weights sum to total
    }

    // ---------------------------------------------------------------- how hard
    /** The toxic flood's effect level at this threat: {@code round((threat - 1) * perThreat)}, 0 to 4. */
    public static int floodAmplifier(int threat, double perThreat) {
        return (int) Math.max(0, Math.min(4, Math.round(Math.max(0, threat - 1) * perThreat)));
    }

    /** Damage of one collapse pulse: the base plus a share per threat, never more than 35 % of the victim's maximum health (a pulse is a warning, not an execution). */
    public static float collapsePulseDamage(int threat, double base, double perThreat, float victimMaxHealth) {
        double d = base + perThreat * Math.max(0, threat - 1);
        return (float) Math.max(0.0, Math.min(d, 0.35 * Math.max(1f, victimMaxHealth)));
    }

    /** The Cursed Spirit's maximum-health factor: its roster entry's scale times {@code 1 + perThreat * (threat - 1)}, kept within 0.1 to 100. */
    public static double spiritHealthMultiplier(int threat, double healthScale, double perThreat) {
        double m = (healthScale <= 0 ? 1.0 : healthScale) * (1.0 + Math.max(0.0, perThreat) * Math.max(0, threat - 1));
        return Math.max(0.1, Math.min(100.0, m));
    }

    /** The Cursed Spirit's attack-damage factor: {@code 1 + perThreat * (threat - 1)}, 1 to 20. */
    public static double spiritDamageMultiplier(int threat, double perThreat) {
        return Math.max(1.0, Math.min(20.0, 1.0 + Math.max(0.0, perThreat) * Math.max(0, threat - 1)));
    }
}
