package com.newuniverse.nusmp.relics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** The pure check the catalog runs over the waves' defs (kept apart so it can be tested without the game). */
public final class RelicChecks {
    private RelicChecks() {}

    /** The checked defs and what was wrong with the ones that were not taken as they were. */
    public record Checked(List<RelicDef> defs, List<String> problems) {}

    /**
     * Pure check of a list of defs in the order given: a repeated id is dropped, a vessel below LEGEND loses the flag, an id in {@code taken} (items
     * that exist already) is dropped unless the def adopts that item.
     */
    public static Checked check(List<RelicDef> raw, Set<String> taken) {
        List<RelicDef> ok = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (RelicDef d : raw) {
            if (d == null) { problems.add("a wave handed over a null def"); continue; }
            if (!seen.add(d.id())) { problems.add("duplicate relic id " + d.id() + " (the later one is dropped)"); continue; }
            if (!d.isAdopted() && taken.contains(d.id())) { problems.add("relic id " + d.id() + " is already an item of the mod (it must adopt it, or take another id)"); continue; }
            RelicDef use = d;
            if (d.isVessel() && !d.grade().djinnCapable()) {
                problems.add("relic " + d.id() + " is a vessel but its grade " + d.grade() + " is below LEGEND (flag dropped)");
                use = d.dropVessel();
            }
            Set<String> keys = new HashSet<>();
            for (Act a : use.acts()) if (!keys.add(a.key())) problems.add("relic " + use.id() + " has two acts with the key " + a.key());
            if (use.slot() == RelicSlot.CONSUMABLE && use.acts().isEmpty()) problems.add("consumable " + use.id() + " has no act");
            ok.add(use);
        }
        return new Checked(ok, problems);
    }
}
