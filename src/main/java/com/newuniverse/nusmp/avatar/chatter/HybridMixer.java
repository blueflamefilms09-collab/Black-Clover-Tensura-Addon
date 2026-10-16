package com.newuniverse.nusmp.avatar.chatter;

import com.newuniverse.nusmp.avatar.AvatarPersonality;
import com.newuniverse.nusmp.avatar.AvatarPersonalityTraits;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Why: an Avatar with two personalities must sound like BOTH, not like a coin flip. Per line the mixer draws 50% from the primary pool, 30% from the
 * secondary pool and 20% a COMBINED line: the opening sentence of the personality that "leads" (tsundere insult, coward's stutter, lazy sigh, ...)
 * glued to the closing sentence of the one that "tails" (heroic shout, chunibyo flourish, the skill name, ...), e.g. tsundere + heroic gives
 * "I'm only saving you because it's the right thing to do, idiot! Light of justice, strike!". Pure and seeded so the tests can pin it.
 */
public final class HybridMixer {
    private HybridMixer() {}

    public enum Source { PRIMARY, SECONDARY, COMBINED }

    /** Probability bounds of the draw: below 0.5 primary, below 0.8 secondary, else combined. */
    public static final double PRIMARY_SHARE = 0.5, SECONDARY_SHARE = 0.3;
    /** A glued line longer than this falls back to the lead line alone (the chat line limit is 160 characters). */
    public static final int MAX_COMBINED = 150;

    private static final Pattern SENTENCE_END = Pattern.compile("(?<=[.!?…])\\s+");

    public static Source chooseSource(Rng rng) {
        double d = rng.nextDouble();
        return d < PRIMARY_SHARE ? Source.PRIMARY : d < PRIMARY_SHARE + SECONDARY_SHARE ? Source.SECONDARY : Source.COMBINED;
    }

    /** One line for a single personality. Null when the pool is empty. */
    public static String pickOne(Rng rng, List<String> pool) {
        return pool == null || pool.isEmpty() ? null : pool.get(rng.nextInt(pool.size()));
    }

    /**
     * One line for a hybrid Avatar from the two pools. An empty pool is skipped (the other one speaks); both empty gives null.
     * {@code combined} is false when the caller does not want glued lines (e.g. a one-word stoic).
     */
    public static String pick(Rng rng, AvatarPersonality primary, List<String> poolA, AvatarPersonality secondary, List<String> poolB) {
        boolean a = poolA != null && !poolA.isEmpty(), b = poolB != null && !poolB.isEmpty();
        if (!a && !b) return null;
        if (!b) return pickOne(rng, poolA);
        if (!a) return pickOne(rng, poolB);
        Source s = chooseSource(rng);
        if (s == Source.PRIMARY) return pickOne(rng, poolA);
        if (s == Source.SECONDARY) return pickOne(rng, poolB);
        return combine(primary, pickOne(rng, poolA), secondary, pickOne(rng, poolB));
    }

    /** Which personality's fragment goes first. A "front loaded" parent leads; when both or neither are, the primary leads. */
    static boolean primaryLeads(AvatarPersonality primary, AvatarPersonality secondary) {
        boolean p = AvatarPersonalityTraits.of(primary).frontLoaded(), s = AvatarPersonalityTraits.of(secondary).frontLoaded();
        return p || !s;
    }

    /**
     * Glues two lines: the FIRST sentence of the leading parent's line, then the LAST sentence of the other parent's line. Placeholders pass through
     * untouched (they are filled afterwards). A one-sentence line contributes itself whole. Too long a result falls back to the leading line.
     */
    public static String combine(AvatarPersonality primary, String lineA, AvatarPersonality secondary, String lineB) {
        if (lineA == null || lineA.isBlank()) return lineB;
        if (lineB == null || lineB.isBlank()) return lineA;
        boolean aLeads = primaryLeads(primary, secondary);
        String lead = aLeads ? first(lineA) : first(lineB);
        String tail = aLeads ? last(lineB) : last(lineA);
        if (lead.equals(tail)) return lead;
        String glued = lead + " " + tail;
        return glued.length() <= MAX_COMBINED ? glued : (aLeads ? lineA : lineB);
    }

    static String first(String line) {
        String[] parts = SENTENCE_END.split(line.strip());
        return parts[0];
    }

    static String last(String line) {
        String[] parts = SENTENCE_END.split(line.strip());
        return parts[parts.length - 1];
    }
}
