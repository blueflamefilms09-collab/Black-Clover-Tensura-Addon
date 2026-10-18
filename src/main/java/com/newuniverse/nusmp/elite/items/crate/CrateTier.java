package com.newuniverse.nusmp.elite.items.crate;

import java.util.Locale;

/**
 * Why: the three crate tiers are the one place that names the ids a crate block, its key and its reward table share (the block is {@code <id>_crate}, the key {@code <id>_crate_key},
 * the table {@code data/nusmp/elite/items/crates/<id>.json}), so the registry, the loot modifier, the recipes, the lang fragment and the parity tests cannot disagree. No Minecraft types,
 * so the id rules are tested without a game. New tiers are only ever appended at the end (save and datapack compatibility, the owner's rule 2).
 */
public enum CrateTier {
    /** A plain oak crate with iron bands. */
    COMMON("common", 0xC9A36B),
    /** Dark wood, silver bands and a blue gem. */
    RARE("rare", 0x5AA9E6),
    /** Black wood, gold bands and a glowing violet gem. */
    ELITE("elite", 0xB05CF0);

    private final String id;
    private final int color;

    CrateTier(String id, int color) {
        this.id = id;
        this.color = color;
    }

    /** {@code common}, {@code rare}, {@code elite}. */
    public String id() { return id; }

    /** {@code common_crate}: the block and its item. */
    public String crateId() { return id + "_crate"; }

    /** {@code common_crate_key}: the key item. */
    public String keyId() { return id + "_crate_key"; }

    /** The tier's colour (0xRRGGBB) for tooltips, particles and the opening light. */
    public int color() { return color; }

    /** The tier of an id ({@code rare}); null when unknown. */
    public static CrateTier byId(String id) {
        if (id == null) return null;
        String s = id.toLowerCase(Locale.ROOT);
        for (CrateTier t : values()) if (t.id.equals(s)) return t;
        return null;
    }
}
