package com.newuniverse.nusmp.labyrinth.mega.system;

import java.util.Locale;

/**
 * Why: the brief's own example quest is "Clear 5 rooms WITHOUT TAKING MAGIC DAMAGE", and the game has no single flag for "this was magic": vanilla has the
 * damage types {@code magic} and {@code indirect_magic}, and Tensura's spells have damage types of their own whose ids name the family
 * ({@code darkness_elemental}, {@code holy_magic} ...). The decision is made from the damage type's id alone, so it can be unit tested without a game, and a
 * datapack can widen it with the damage-type tag {@code nusmp:mega_system_magic} (checked by the game side before this). The word list is the family the
 * mod's Anti-Magic rules already treat as magic (so the System and the rest of the mod agree on what "magic damage" is): a quest that says "no magic damage"
 * fails on anything the mod itself would call magic.
 */
public final class MagicDamage {
    private MagicDamage() {}

    /** Words that mark a Tensura damage type as magic (the family the Anti-Magic code uses). */
    private static final String[] TENSURA_WORDS = {"magic", "elemental", "holy", "curse", "spatial", "space", "dark", "light"};

    /** Is the damage type {@code namespace:path} magic? Vanilla only by its two magic types; any namespace by the word "magic"; Tensura by its family words. */
    public static boolean isMagicType(String namespace, String path) {
        if (path == null || path.isEmpty()) return false;
        String ns = namespace == null ? "" : namespace.toLowerCase(Locale.ROOT);
        String p = path.toLowerCase(Locale.ROOT);
        if (p.contains("magic")) return true;
        if (ns.equals("tensura")) {
            for (String w : TENSURA_WORDS) if (p.contains(w)) return true;
        }
        return false;
    }

    /** The same from a full id such as {@code minecraft:indirect_magic} (no colon = the vanilla namespace). */
    public static boolean isMagicId(String id) {
        if (id == null) return false;
        int c = id.indexOf(':');
        return c < 0 ? isMagicType("minecraft", id) : isMagicType(id.substring(0, c), id.substring(c + 1));
    }
}
