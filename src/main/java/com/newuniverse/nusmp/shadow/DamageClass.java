package com.newuniverse.nusmp.shadow;

import java.util.Locale;

/**
 * Why: Unite Equus takes 40 % off PHYSICAL damage only, and the game has no single "this is physical" flag: vanilla uses tags and Tensura's
 * magic uses its own damage types. The decision is made here from plain facts (the damage type's id and two vanilla tags), so it is testable
 * and so a new Tensura type is handled by one list of words instead of code scattered in an event handler.
 *
 * <p>Physical = not bypassing invulnerability (void, kill), not bypassing armour (magic, starvation, falling, drowning), and the type id
 * does not name a magical, elemental, curse or energy family.
 */
public final class DamageClass {
    private DamageClass() {}

    /** Words in a damage type id that mark it as magic, elements, curses or energy (Tensura's ids look like {@code darkness_elemental}). */
    private static final String[] MAGICAL = {"magic", "elemental", "curse", "fear", "holy", "poison", "insanity", "soul", "mind", "drain", "breath",
            "ray", "lightning", "flame", "fire", "burn", "heat", "petrif", "disintegr", "gravity", "dimension", "megiddo", "death", "devour", "corros",
            "infection", "sonic", "wither", "freez", "frost", "ice_", "_ice", "sacrific", "wish", "bless", "tornado", "paraly"};

    /**
     * @param typePath         the damage type's path ({@code mob_attack}, {@code arrow}, {@code magic_generic} ...)
     * @param bypassesArmor    the type is in the vanilla tag {@code bypasses_armor}
     * @param bypassesInvuln   the type is in the vanilla tag {@code bypasses_invulnerability}
     */
    public static boolean isPhysical(String typePath, boolean bypassesArmor, boolean bypassesInvuln) {
        if (bypassesArmor || bypassesInvuln) return false;
        if (typePath == null) return true;
        String p = typePath.toLowerCase(Locale.ROOT);
        for (String w : MAGICAL) if (p.contains(w)) return false;
        return true;
    }
}
