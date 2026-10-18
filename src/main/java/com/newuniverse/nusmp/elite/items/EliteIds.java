package com.newuniverse.nusmp.elite.items;

import com.newuniverse.nusmp.elite.items.crate.CrateTier;
import com.newuniverse.nusmp.elite.items.plush.PlushKinds;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: every item id of the elite/items subsystem in one pure place (no Minecraft types), so the asset-parity tests (an icon, a model and a lang key for each), the in-game self-test (each is registered),
 * the crate tables' check (a reward item that names an item of ours exists) and the creative tab read the same list the registry is built from.
 */
public final class EliteIds {
    private EliteIds() {}

    /** The six weapons. */
    public static final List<String> WEAPONS = List.of("astral_edge", "void_edge", "fenrir_sword", "runic_sword_renewed", "runic_sword_ancient", "runic_sword_mythical");

    /** Hood (helmet slot), cloak (chest), leggings, boots. */
    public static final List<String> ARMOR = List.of("nightingale_hood", "nightingale_cloak", "nightingale_leggings", "nightingale_boots");

    /** The consumables and plain items. */
    public static final List<String> CONSUMABLES = List.of("potential_catalyst", "health_potion_small", "health_potion_medium", "health_potion_large", "chronicle_book", "walpurgis_orb", "walpurgis_seal");

    /** Plain items that are NOT weapons or armour and have a flat 16x16 icon (everything but the 3D book, which has its own model). */
    public static final List<String> FLAT_ICON_ITEMS = List.of("potential_catalyst", "health_potion_small", "health_potion_medium", "health_potion_large", "chronicle_book", "walpurgis_orb", "walpurgis_seal");

    /** Crate block ids (each also an item). */
    public static List<String> crates() {
        List<String> out = new ArrayList<>();
        for (CrateTier t : CrateTier.values()) out.add(t.crateId());
        return out;
    }

    /** Key item ids. */
    public static List<String> keys() {
        List<String> out = new ArrayList<>();
        for (CrateTier t : CrateTier.values()) out.add(t.keyId());
        return out;
    }

    /** Plush block ids (each also an item): {@code plush_allay} ... */
    public static List<String> plushies() {
        List<String> out = new ArrayList<>();
        for (PlushKinds.Kind k : PlushKinds.ALL) out.add(k.blockId());
        return out;
    }

    /** Every id the subsystem registers as an item (blocks have a block item of the same id). */
    public static Set<String> allItems() {
        Set<String> out = new LinkedHashSet<>();
        out.addAll(WEAPONS);
        out.addAll(ARMOR);
        out.addAll(CONSUMABLES);
        out.addAll(crates());
        out.addAll(keys());
        out.addAll(plushies());
        return out;
    }
}
