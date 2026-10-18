package com.newuniverse.nusmp.elite.records.rule;

import com.newuniverse.nusmp.elite.records.rule.Defs.SynergyDef;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Why: the synergy engine's matching step, pure and deterministic. A synergy applies while the player HOLDS (not only wears) every title it requires, and, if it names a tag, at least N held titles
 * carry that tag ({@code "*"} counts every held title; {@code rarity:<id>} tags are implicit on every title). The matcher is given the held set and a function from a title id to its tags, so it
 * needs neither the catalog nor the game, and it is tested on its own. The server calls it on its beat with the player's held titles and applies the matched synergies' bonuses.
 */
public final class SynergyMatcher {
    private SynergyMatcher() {}

    /** True when the synergy applies to this held set. */
    public static boolean matches(SynergyDef s, Set<String> held, Function<String, Set<String>> tagsOf) {
        int[] p = progress(s, held, tagsOf);
        return p[1] > 0 && p[0] >= p[1];
    }

    /** {have, need} counting each required title as one and the tag part as {@code min(have, tagCount)} of {@code tagCount}; for the progress line of the screen. */
    public static int[] progress(SynergyDef s, Set<String> held, Function<String, Set<String>> tagsOf) {
        int have = 0, need = 0;
        for (String r : s.requires()) {
            need++;
            if (held.contains(r)) have++;
        }
        if (!s.tag().isEmpty() && s.tagCount() > 0) {
            need += s.tagCount();
            int n = 0;
            for (String t : held) {
                if (s.tag().equals("*") || tagsOf.apply(t).contains(s.tag())) n++;
            }
            have += Math.min(n, s.tagCount());
        }
        return new int[]{have, need};
    }

    /** The synergies of {@code all} that apply, in the order given. */
    public static List<SynergyDef> match(Collection<SynergyDef> all, Set<String> held, Function<String, Set<String>> tagsOf) {
        List<SynergyDef> out = new ArrayList<>();
        if (held.isEmpty()) return out;
        for (SynergyDef s : all) if (matches(s, held, tagsOf)) out.add(s);
        return out;
    }

    /** The combined bonuses of matched synergies. */
    public static Bonuses combined(List<SynergyDef> matched) {
        List<Bonuses> parts = new ArrayList<>();
        for (SynergyDef s : matched) parts.add(s.bonuses());
        return Bonuses.merge(parts);
    }
}
