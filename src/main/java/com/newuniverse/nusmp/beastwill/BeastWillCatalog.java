package com.newuniverse.nusmp.beastwill;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Why: the set of wills the server loaded from {@code data/nusmp/beast_will} (3.45.0), swapped in one step at every data reload so a reader sees the old set or the new one, never a mixture. Read by the
 * manager, the drops and the commands; the loader ({@link BeastWillLoader}) is the only writer.
 */
public final class BeastWillCatalog {
    private BeastWillCatalog() {}

    private static volatile Map<String, BeastWillDef> defs = Map.of();

    public static void install(Map<String, BeastWillDef> loaded) { defs = Map.copyOf(loaded); }

    public static BeastWillDef get(String id) { return id == null ? null : defs.get(id); }

    public static Collection<BeastWillDef> all() { return defs.values(); }

    public static List<String> ids() { return defs.keySet().stream().sorted().toList(); }

    public static int size() { return defs.size(); }

    /** Every will a creature of this entity type id can give (before rank, age and luck are looked at). Usually none; cheap: a few string compares. */
    public static List<BeastWillDef> forEntity(String typeId) {
        List<BeastWillDef> out = new java.util.ArrayList<>(2);
        for (BeastWillDef d : defs.values()) if (d.fromEntity(typeId)) out.add(d);
        return out;
    }
}
