package com.newuniverse.nusmp.grimoire;

/**
 * Why: a grimoire that belongs to a named character (Rades Spirito's Soul Corpse grimoire, Yami Sukehiro's Dark grimoire) is that character's magic, so the Unique Skill it teaches reads
 * "Rades Spirito's Soul Corpse Magic" and not only "Soul Corpse Magic". The rule is a handful of string decisions that are pure (no Minecraft types), so they live here and a unit test pins
 * them for every {@link CanonBook}: who the owner is, how the possessive is written, what a book with no character is called (the plain magic name, as before). The game side
 * ({@code book.GrimoireSkillInstance}, the grimoire tooltip, the chant line) only calls these helpers; the character id is read from the grimoire item's data or the skill instance's tag
 * (both server-written data), never from client state, so the same text comes out on a dedicated server and on every client.
 */
public final class CanonNaming {
    private CanonNaming() {}

    /** The tag key (item data and skill-instance tag) that holds the {@link CanonBook#id()} of the bound character. */
    public static final String TAG = "Canon";

    /** The name of a character with the possessive ending: "Yami Sukehiro's", "Mars'" (a name that already ends in s keeps a bare apostrophe). */
    public static String possessive(String owner) {
        if (owner == null || owner.isBlank()) return "";
        String o = owner.strip();
        return o.endsWith("s") || o.endsWith("S") ? o + "'" : o + "'s";
    }

    /** The owner's display name of a canon id ("rades" gives "Rades Spirito"), or null for an unknown, blank or null id (a grimoire without a character). */
    public static String ownerOf(String canonId) {
        CanonBook b = CanonBook.byId(canonId);
        return b == null ? null : b.owner;
    }

    /** "Rades Spirito's Soul Corpse Magic": the character in front of the magic's name; the plain name when there is no character. */
    public static String named(String canonId, String magicName) {
        String name = magicName == null ? "" : magicName;
        String owner = ownerOf(canonId);
        return owner == null ? name : possessive(owner) + " " + name;
    }

    /** The same from an owner's display name already known (null or blank gives the plain name). */
    public static String namedFor(String owner, String magicName) {
        String name = magicName == null ? "" : magicName;
        return owner == null || owner.isBlank() ? name : possessive(owner) + " " + name;
    }

    /**
     * The character id a skill instance of {@code instMagic} must carry, given the grimoire in the player's slot ({@code slotMagic}, {@code slotCanon}; blank when the slot holds none of
     * the player's own books): the slot's character when the instance is the book of the slot's magic, else "" (a second book the player holds belongs to no character).
     */
    public static String wanted(String slotMagic, String slotCanon, String instMagic) {
        if (slotMagic == null || slotMagic.isBlank() || instMagic == null || !slotMagic.equals(instMagic)) return "";
        return known(slotCanon) ? slotCanon : "";
    }

    /** True when the id names one of the characters of {@link CanonBook}. */
    public static boolean known(String canonId) { return CanonBook.byId(canonId) != null; }
}
