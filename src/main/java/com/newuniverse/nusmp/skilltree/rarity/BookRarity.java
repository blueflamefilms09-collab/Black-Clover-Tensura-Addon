package com.newuniverse.nusmp.skilltree.rarity;

import java.util.Locale;

/**
 * Why: 3.26.0 gives every Skill Book a rarity (the owner's Gemini spec: Common, Uncommon, Rare, Epic, Mythic), and the rarity is read in four places that must never disagree: the drop rules (which
 * tier of the dungeon may hand out which rarity), the item (name colour, tooltip line, glint), the unlock show (the particle colour) and the model predicate of the client (the trim of the cover).
 * So the five values, their saved ids, their colours and the glint rule live in this one small enum that has no Minecraft type: the client table parser ({@code BookLook}) and the pure
 * rules ({@link RarityRules}) use it as freely as the item class does, and a unit test pins every number of the contract. The order IS the rank (never reorder, only append): the rank is the
 * factor of the Luck scaling, the index of the per-rarity weight tables and the value of the model predicate {@code nusmp:rarity} ({@code (rank + 1) / 6}). The ids are the lower-case strings
 * saved in the data component {@code nusmp:skill_book_rarity} and written in the shipped table {@code assets/nusmp/skill_books.json}; an unknown, blank or missing id reads as {@link #COMMON}, so an
 * old table (no field) and a typo in a data pack both give a plain book instead of an error.
 */
public enum BookRarity {
    COMMON("common", "Common", 0xFFFFFF, false),
    UNCOMMON("uncommon", "Uncommon", 0x55FF55, false),
    RARE("rare", "Rare", 0x55AAFF, false),
    EPIC("epic", "Epic", 0xC855FF, true),
    MYTHIC("mythic", "Mythic", 0xFFAA2B, true);

    private static final BookRarity[] ALL = values();

    private final String id;
    private final String label;
    private final int color;
    private final boolean glint;

    BookRarity(String id, String label, int color, boolean glint) {
        this.id = id;
        this.label = label;
        this.color = color;
        this.glint = glint;
    }

    /** The saved id ({@code epic}). */
    public String id() { return id; }

    /** The English name ({@code Epic}); the tooltip uses it as the fallback of the key {@code rarity.nusmp.<id>}. */
    public String label() { return label; }

    /** The rank, 0 for common to 4 for mythic. */
    public int rank() { return ordinal(); }

    /** The name colour as 0xRRGGBB. */
    public int color() { return color; }

    /** The colour of the unlock burst: the same as the name. */
    public int particleColor() { return color; }

    /** Does a book of this rarity glint like an enchanted item (epic and mythic only)? */
    public boolean glint() { return glint; }

    /** The value of the item property {@code nusmp:rarity}: {@code (rank + 1) / 6}, so 0 stays free for "unknown". */
    public float modelValue() { return (ordinal() + 1) / 6f; }

    /** How many rarities there are. */
    public static int count() { return ALL.length; }

    /** The rarity of a rank; below 0 is common, above 4 is mythic. */
    public static BookRarity fromRank(int rank) { return ALL[Math.max(0, Math.min(ALL.length - 1, rank))]; }

    /** The rarity with this saved id (case-insensitive), or null for null, blank and unknown ids. */
    public static BookRarity byId(String id) {
        if (id == null) return null;
        String s = id.trim();
        if (s.isEmpty()) return null;
        for (BookRarity r : ALL) if (r.id.equalsIgnoreCase(s)) return r;
        return null;
    }

    /** The rarity with this saved id, or {@link #COMMON} for null, blank and unknown ids. */
    public static BookRarity fromId(String id) {
        BookRarity r = byId(id);
        return r == null ? COMMON : r;
    }

    /** The rarity of a model-property value written by {@link #modelValue}, or null when it matches none (0 means "unknown"). */
    public static BookRarity fromModelValue(float value) {
        int rank = Math.round(value * 6f) - 1;
        return rank < 0 || rank >= ALL.length ? null : ALL[rank];
    }

    /** The lang key of the display name: {@code rarity.nusmp.epic}. */
    public String langKey() { return "rarity.nusmp." + id.toLowerCase(Locale.ROOT); }
}
