package com.newuniverse.nusmp.charset;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the data of one character's grimoire (docs/magic/character_spells.md): which pages of the shared element book are HIS, in the order he got them, and the rule for each. Immutable and pure;
 * {@link CharacterSetLoader} builds it from the character's JSON and checks it.
 *
 * @param canon    the CanonBook id ("yuno")
 * @param magic    the MagicType name of that character's book ("WIND")
 * @param book     the registry path of the book skill ("book_wind")
 * @param sources  source id -> where it came from (so every entry can say which wiki it follows)
 * @param spells   the character's spells in the order he got them
 * @param planned  spells that are canon for him but have no page yet (documentation for the content builders; the engine ignores them)
 * @param notes    free text
 */
public record CharacterSet(String canon, String magic, String book, Map<String, String> sources, List<Spell> spells, List<Planned> planned, String notes) {
    public CharacterSet {
        sources = Map.copyOf(new LinkedHashMap<>(sources));
        spells = List.copyOf(spells);
        planned = List.copyOf(planned);
    }

    /** One spell of the character: a page of the book plus when it opens. */
    public record Spell(String page, String name, String ja, String origin, boolean exclusive, String source, SpellRule rule, String note) {}

    /** A canon spell with no page yet. */
    public record Planned(String name, String ja, String origin, String source, String note) {}

    public Spell spell(String pageId) {
        for (Spell s : spells) if (s.page().equals(pageId)) return s;
        return null;
    }

    public boolean has(String pageId) { return spell(pageId) != null; }
}
