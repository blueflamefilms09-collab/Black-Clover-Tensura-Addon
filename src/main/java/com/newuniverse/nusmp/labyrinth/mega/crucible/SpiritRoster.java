package com.newuniverse.nusmp.labyrinth.mega.crucible;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Why: which creature a Cursed Spirit is must be DATA ({@code data/nusmp/mega/cursed_spirits.json}), the way the Peak Altar's boss roster is,
 * because the best candidates are other mods' monsters (the Tensura daemons, hounds and wraiths) whose ids and strength the owner will want to
 * tune or extend without a mod update, and because an id that no loaded mod registers must simply be skipped rather than crash the catastrophe.
 * This class is the pure half: it parses the file's tree (maps, lists, strings, numbers: the same tree {@code ThemeDef.parse} reads, so no JSON
 * library is needed to test it), keeps only usable entries, and draws one by weight from a seed (the sector's seed mixed with the chooser, so the
 * same pick always summons the same spirit but two vaults summon different ones). A draw skips entries the caller's predicate rejects (the
 * entity type does not exist in this game) and returns null when nothing is drawable, in which case the catastrophe falls back to the collapse.
 */
public final class SpiritRoster {
    private SpiritRoster() {}

    /** One roster line: the entity id, its weight, the name the spirit wears and the factor applied to its maximum health before the threat scaling. */
    public record Entry(String entity, int weight, String name, double healthScale) {}

    /** The parsed roster and the warnings found while reading it. */
    public record Parsed(List<Entry> entries, List<String> warnings) {}

    /** The name a spirit wears when its roster line gives none. */
    public static final String DEFAULT_NAME = "Cursed Spirit";

    /** Reads {@code {"entries": [{"entity": "...", "weight": 3, "name": "Cursed Spirit", "healthScale": 1.0}]}}; never throws. */
    public static Parsed parse(Object tree) {
        List<Entry> out = new ArrayList<>();
        List<String> warn = new ArrayList<>();
        if (!(tree instanceof Map<?, ?> root) || !(root.get("entries") instanceof List<?> list)) {
            warn.add("the roster must be an object with an 'entries' list");
            return new Parsed(out, warn);
        }
        int i = 0;
        for (Object o : list) {
            i++;
            if (!(o instanceof Map<?, ?> m)) { warn.add("entry " + i + " is not an object"); continue; }
            String id = m.get("entity") instanceof String s ? s.trim().toLowerCase(Locale.ROOT) : "";
            if (id.isEmpty()) { warn.add("entry " + i + " has no entity id"); continue; }
            if (!id.contains(":")) id = "minecraft:" + id;
            int weight = m.get("weight") instanceof Number n ? n.intValue() : 1;
            if (weight <= 0) { warn.add("entry " + i + " (" + id + ") has weight " + weight + ": it is never drawn"); continue; }
            String name = m.get("name") instanceof String s && !s.isBlank() ? s.trim() : DEFAULT_NAME;
            double scale = m.get("healthScale") instanceof Number n ? n.doubleValue() : 1.0;
            if (!(scale > 0.0) || Double.isInfinite(scale)) { warn.add("entry " + i + " (" + id + ") has healthScale " + scale + ": using 1.0"); scale = 1.0; }
            out.add(new Entry(id, Math.min(weight, 1000), name, Math.min(scale, 20.0)));
        }
        return new Parsed(out, warn);
    }

    /** splitmix64's finaliser. */
    static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** One weighted draw among the entries {@code known} accepts; null when none is drawable. */
    public static Entry pick(List<Entry> roster, long seed, Predicate<String> known) {
        long total = 0;
        for (Entry e : roster) if (e != null && e.weight() > 0 && known.test(e.entity())) total += e.weight();
        if (total <= 0) return null;
        long r = Math.floorMod(mix(seed), total);
        for (Entry e : roster) {
            if (e == null || e.weight() <= 0 || !known.test(e.entity())) continue;
            if (r < e.weight()) return e;
            r -= e.weight();
        }
        return null;                                                               // unreachable: the weights sum to total
    }
}
