package com.newuniverse.nusmp.labyrinth.mega.gear;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Why: "magic resistance" must reduce SPELLS and nothing else (not a sword, an arrow, a fall, fire or poison), and the mod's spells come in through
 * damage-type ids: vanilla {@code magic} and {@code indirect_magic}, and Tensura's types, whose paths name their element or art
 * ({@code fire_elemental}, {@code magic_generic}, {@code holy_damage}, {@code flame_breath}, {@code blood_ray} ...). The mod already has three slightly
 * different yes/no tests for this, none of them with a tag of its own, so this is the one the gear uses: a pure rule over the id (so it is unit tested
 * against the real list of Tensura's damage types without a server) plus an owner-editable list for anything it does not know. The rule is deliberately
 * NARROW: a Tensura type counts when one of the words of its path (split at {@code _}, {@code /}, {@code .} and {@code -}) is an element or a spell word, so {@code magicule_poison},
 * {@code fear}, {@code insanity}, {@code soul_scatter} (status, mental and spiritual damage, which Tensura answers with its own engravings) and the
 * physical skills ({@code bullet}, {@code kunai}, {@code aura_slash}, {@code severer_blade}) are NOT magic here, and {@code thorns}, which this mod uses
 * as a neutral damage source for its own scripted hits, is not either. An unknown id is not magic: a missing entry costs a little protection, a wrong one
 * would shield the player from things a spell never was. Pure.
 */
public final class MagicDamage {
    private MagicDamage() {}

    /** The vanilla damage types that are spells (the witch's and the evoker's, the instant-damage potions, the dragon's breath). */
    private static final Set<String> VANILLA = Set.of("magic", "indirect_magic", "dragon_breath");

    /** The words of a Tensura damage-type path that mark a spell: an element, an art, a ray or a breath. {@code magicule} is a different word and does not match {@code magic}. */
    private static final Set<String> TENSURA_WORDS = Set.of("elemental", "magic", "holy", "curse", "megiddo", "breath", "ray", "gravity", "flame", "lightning");

    /** Tensura types that are spells but have no marker word in their path. */
    private static final Set<String> TENSURA_EXACT = Set.of("water_blade", "heat_wave");

    /** Is the damage type {@code namespace:path} a spell by the built-in rule? */
    public static boolean isMagicType(String namespace, String path) {
        if (namespace == null || path == null) return false;
        String ns = namespace.toLowerCase(Locale.ROOT), p = path.toLowerCase(Locale.ROOT);
        if (ns.equals("minecraft")) return VANILLA.contains(p);
        if (!ns.equals("tensura")) return false;
        if (TENSURA_EXACT.contains(p)) return true;
        for (String word : p.split("[_/.\\-]")) if (TENSURA_WORDS.contains(word)) return true;
        return false;
    }

    /**
     * The owner's extra list, from one config string: damage-type ids or {@code namespace:*} wildcards separated by commas or whitespace
     * ({@code "mymod:arcane_bolt, othermod:*"}). Blank pieces are ignored; the result is lower case. An empty string gives an empty list.
     */
    public static List<String> parseExtra(String csv) {
        List<String> out = new ArrayList<>();
        if (csv == null) return out;
        for (String piece : csv.split("[,\\s]+")) {
            String s = piece.trim().toLowerCase(Locale.ROOT);
            if (!s.isEmpty() && s.indexOf(':') > 0) out.add(s);
        }
        return out;
    }

    /** Does the damage type match one of the extra entries (an exact id, or {@code namespace:*} for the whole namespace)? */
    public static boolean matchesExtra(String namespace, String path, List<String> extra) {
        if (extra.isEmpty() || namespace == null || path == null) return false;
        String ns = namespace.toLowerCase(Locale.ROOT), full = ns + ":" + path.toLowerCase(Locale.ROOT);
        for (String e : extra) {
            if (e.equals(full)) return true;
            if (e.endsWith(":*") && e.substring(0, e.length() - 2).equals(ns)) return true;
        }
        return false;
    }
}
