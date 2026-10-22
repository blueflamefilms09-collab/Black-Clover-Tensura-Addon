package com.newuniverse.nusmp.charset;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: a character did not have every spell from the first episode (Yuno's Spirit of Zephyr came after his Spirit Dive). An acquisition rule says WHEN a page of that character's grimoire opens.
 * The rule set is deliberately small, pure and the same for every character: from the start; a mastery level; another page cast N times; another page open; a spirit trust level; a named stage
 * flag (Saint Stage, Mana Zone, a spirit bond, the Anti-Magic Lord); and all-of / any-of combinations. Rank and race gates are NOT here: they stay the page gates of
 * {@code multiverse.MasteryPages}.
 */
public sealed interface SpellRule {
    /** The stage flags the game answers (see {@code CharacterSets.flag}). A rule naming any other flag is rejected by the loader. */
    List<String> FLAGS = List.of("spirit_bond", "saint_stage", "mana_zone", "anti_magic_lord");

    /** The page ids that are not part of a family list but that a rule may count (the Spirit pages every element book has). */
    List<String> UNIVERSAL_PAGES = List.of("spirit_dive", "spirit_channeling", "devil_union");

    boolean test(RuleFacts f);

    /** A short plain description for docs and tooling ("spirit_dive cast 2 times"). */
    String describe();

    /** Adds every page id this rule looks at (for dependency checks and for deciding which casts are counted). */
    void pages(Set<String> out);

    default Set<String> pages() { Set<String> s = new LinkedHashSet<>(); pages(s); return s; }

    record Start() implements SpellRule {
        public boolean test(RuleFacts f) { return true; }
        public String describe() { return "from the start"; }
        public void pages(Set<String> out) {}
    }

    record Mastery(double min) implements SpellRule {
        public boolean test(RuleFacts f) { return f.mastery() + 1e-9 >= min; }
        public String describe() { return "mastery " + Math.round(min * 100) + "%"; }
        public void pages(Set<String> out) {}
    }

    record PageUses(String page, int count) implements SpellRule {
        public boolean test(RuleFacts f) { return f.uses(page) >= count; }
        public String describe() { return page + " cast " + count + (count == 1 ? " time" : " times"); }
        public void pages(Set<String> out) { out.add(page); }
    }

    record PageOpen(String page) implements SpellRule {
        public boolean test(RuleFacts f) { return f.pageOpen(page); }
        public String describe() { return page + " open"; }
        public void pages(Set<String> out) { out.add(page); }
    }

    record Trust(int min) implements SpellRule {
        public boolean test(RuleFacts f) { return f.trust() >= min; }
        public String describe() { return "spirit trust " + min; }
        public void pages(Set<String> out) {}
    }

    record Flag(String name) implements SpellRule {
        public boolean test(RuleFacts f) { return f.flag(name); }
        public String describe() { return name.replace('_', ' '); }
        public void pages(Set<String> out) {}
    }

    record All(List<SpellRule> of) implements SpellRule {
        public All { of = List.copyOf(of); }
        public boolean test(RuleFacts f) { for (SpellRule r : of) if (!r.test(f)) return false; return true; }
        public String describe() { return join(of, " and "); }
        public void pages(Set<String> out) { for (SpellRule r : of) r.pages(out); }
    }

    record Any(List<SpellRule> of) implements SpellRule {
        public Any { of = List.copyOf(of); }
        public boolean test(RuleFacts f) { for (SpellRule r : of) if (r.test(f)) return true; return false; }
        public String describe() { return "(" + join(of, " or ") + ")"; }
        public void pages(Set<String> out) { for (SpellRule r : of) r.pages(out); }
    }

    private static String join(List<SpellRule> l, String sep) {
        List<String> parts = new ArrayList<>();
        for (SpellRule r : l) parts.add(r.describe());
        return String.join(sep, parts);
    }
}
