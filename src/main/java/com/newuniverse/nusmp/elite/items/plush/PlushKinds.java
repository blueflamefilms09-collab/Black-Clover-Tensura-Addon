package com.newuniverse.nusmp.elite.items.plush;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Why: the list of mob plushies is the single source of the plushies' ids. The registry registers one block and one item per entry from it, the creative tab, the collection counter and the tests iterate
 * it, and the generator ({@code tools/elite/gen_plushies.py}) holds the SAME ids with their look (the parity test reads the generated files and fails when an id here has no model, no texture or no
 * lang key, or the generator has one this list lacks). Pure (no Minecraft types). New plushies are only ever APPENDED at the end: the position is the plushie's number in the collection and the
 * creative-tab order (the owner's rule 2: old things stay, new ones are added).
 *
 * <p>{@code size} (0 tiny, 1 small, 2 medium, 3 large, 4 huge) says how low the squeak is and how tall the hitbox: a Bat squeaks high and fits a hand, a Wither squeaks low.
 */
public final class PlushKinds {
    private PlushKinds() {}

    /** One plushie: its id (the item is {@code nusmp:plush_<id>}), its English name and its size class. */
    public record Kind(String id, String name, int size) {
        public String blockId() { return "plush_" + id; }

        /** The squeak's base pitch: small plushies squeak high, huge ones low. */
        public float pitch() { return switch (size) { case 0 -> 1.75f; case 1 -> 1.5f; case 2 -> 1.25f; case 3 -> 1.0f; default -> 0.8f; }; }
    }

    private static Kind k(String name, int size) { return new Kind(name.toLowerCase(Locale.ROOT).replace(' ', '_'), name, size); }

    /** The plushies in collection order (appended to only). */
    public static final List<Kind> ALL = List.of(
            k("Allay", 1), k("Armadillo", 1), k("Axolotl", 1), k("Bat", 0), k("Bee", 0), k("Blaze", 2), k("Camel", 3), k("Cat", 1),
            k("Chicken", 1), k("Cow", 2), k("Creeper", 2), k("Dolphin", 2), k("Donkey", 2), k("Drowned", 2), k("Elder Guardian", 4), k("Ender Dragon", 4),
            k("Enderman", 3), k("Evoker", 2), k("Fox", 1), k("Frog", 1), k("Ghast", 4), k("Glow Squid", 2), k("Goat", 2), k("Guardian", 2),
            k("Hoglin", 3), k("Horse", 3), k("Iron Golem", 4), k("Llama", 3), k("Magma Cube", 2), k("Mooshroom", 2), k("Ocelot", 1), k("Panda", 2),
            k("Parrot", 0), k("Phantom", 2), k("Pig", 1), k("Piglin", 2), k("Polar Bear", 3), k("Rabbit", 0), k("Sheep", 2), k("Shulker", 1),
            k("Skeleton", 2), k("Slime", 2), k("Sniffer", 3), k("Spider", 1), k("Squid", 2), k("Strider", 2), k("Turtle", 1), k("Vex", 0),
            k("Villager", 2), k("Warden", 4), k("Witch", 2), k("Wither", 4), k("Wolf", 1), k("Zombie", 2));

    /** The kind with this id ({@code ender_dragon}), or null. */
    public static Kind byId(String id) {
        if (id == null) return null;
        for (Kind k : ALL) if (k.id().equals(id)) return k;
        return null;
    }

    /** The position of a kind in the collection (0-based), or -1. */
    public static int indexOf(String id) {
        for (int i = 0; i < ALL.size(); i++) if (ALL.get(i).id().equals(id)) return i;
        return -1;
    }

    /** Every id, in order. */
    public static List<String> ids() {
        List<String> out = new ArrayList<>();
        for (Kind k : ALL) out.add(k.id());
        return out;
    }

    /** True when no id is repeated and every id is a clean snake_case word (a registry path). */
    public static boolean wellFormed() {
        Set<String> seen = new HashSet<>();
        for (Kind k : ALL) if (!seen.add(k.id()) || !k.id().matches("[a-z][a-z0-9_]*")) return false;
        return true;
    }
}
