package com.newuniverse.nusmp.core.magic.merge;

import java.util.List;

/**
 * One merged magic: the data that is saved in the world and synced to every client. Everything else (profile, spells, colours,
 * models) is derived from it, so two clients with the same definition always draw the same thing.
 *
 * @param id      "merged:flame+water" ({@link MergedKeys#idOf}); unique per parent set
 * @param name    display name ("Flame-Sea Magic", or the one the owner gave)
 * @param parents canonical parent enum names (alphabetical, 2..4); the first is the dominant parent
 * @param slot    which of the 64 registered merged skills ({@code nusmp:merged_00 .. merged_63}) casts it
 * @param custom  true if the owner gave the name
 */
public record MergedDef(String id, String name, List<String> parents, int slot, boolean custom) {
    public MergedDef {
        parents = List.copyOf(parents);
    }

    /** The dominant parent (the first in canonical order). */
    public String dominant() { return parents.get(0); }

    public int size() { return parents.size(); }

    public int key() { return MergedKeys.hash(id); }
}
