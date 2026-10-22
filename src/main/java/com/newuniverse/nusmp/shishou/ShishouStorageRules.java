package com.newuniverse.nusmp.shishou;

import java.util.List;
import java.util.Locale;

/**
 * Why: Stomach (Ibukuro) is the sword's internal storage. It stores anything EXCEPT living things (the novel's rule), the sword itself, and anything that carries an inventory of its own (another backpack,
 * a shulker box, a bundle, a bag of holding), so a storage can never contain itself or hide an unbounded tree of containers. The decision is pure: the game passes the facts it reads from the stack (the
 * item id, whether it holds a container or bundle component, whether it carries an entity, whether it is a spawn egg or a mob bucket).
 */
public final class ShishouStorageRules {
    private ShishouStorageRules() {}

    public enum Refusal { NONE, SELF, CONTAINER, BUNDLE, LIVING, EMPTY }

    private static final List<String> CONTAINER_WORDS = List.of("shulker", "backpack", "satchel", "bag_of_holding", "bundle", "stomach", "portable_chest", "ender_chest");

    /**
     * Why a stack is refused, or {@link Refusal#NONE}.
     *
     * @param itemId       the item's registry id
     * @param hasContainer the stack has a container (inventory) component
     * @param hasBundle    the stack has bundle contents
     * @param livingThing  a spawn egg, a mob bucket, a stack with entity data: a living thing
     */
    public static Refusal refusal(String itemId, boolean empty, boolean hasContainer, boolean hasBundle, boolean livingThing) {
        if (empty) return Refusal.EMPTY;
        String id = itemId == null ? "" : itemId.toLowerCase(Locale.ROOT);
        if (id.equals("nusmp:" + ShishouRules.ID) || id.endsWith(":" + ShishouRules.ID)) return Refusal.SELF;
        if (livingThing) return Refusal.LIVING;
        if (hasBundle || id.endsWith("bundle")) return Refusal.BUNDLE;
        if (hasContainer) return Refusal.CONTAINER;
        for (String w : CONTAINER_WORDS) if (id.contains(w)) return Refusal.CONTAINER;
        return Refusal.NONE;
    }

    public static boolean accepts(String itemId, boolean empty, boolean hasContainer, boolean hasBundle, boolean livingThing) {
        return refusal(itemId, empty, hasContainer, hasBundle, livingThing) == Refusal.NONE;
    }

    /** The rows the storage shows (3..6), from the config. */
    public static int rows(int configured) { return Math.max(3, Math.min(6, configured)); }
}
