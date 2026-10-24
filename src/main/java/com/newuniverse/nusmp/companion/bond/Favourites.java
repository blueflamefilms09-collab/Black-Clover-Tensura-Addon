package com.newuniverse.nusmp.companion.bond;

import com.newuniverse.nusmp.companion.CompanionRules;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: what each companion loves, is glad of, and cannot stand, as a registry keyed by her bond key, so the gift code is the same for every companion and each one only states her tastes (one call, at setup). Three
 * lists: LOVES (a loved gift earns the most), ACCEPTS (things that are not food but she is glad to be given: they count as a plain gift), HATES (a hated gift costs rapport; it wins over the other lists and over food).
 * An item on no list is a plain gift when it is food and is refused (not taken) when it is not. A companion with no entry treats every food as a plain gift. Registered once at setup; read only afterwards.
 */
public final class Favourites {
    private Favourites() {}

    /** One companion's tastes. */
    public record Taste(Set<Item> loves, Set<Item> accepts, Set<Item> hates) {}

    private static final Map<String, Taste> TASTES = new ConcurrentHashMap<>();
    private static final Taste NONE = new Taste(Set.of(), Set.of(), Set.of());
    private static final int MAX_KEYS = 512;

    /**
     * Registers the tastes of a companion. An item on both a good list and the hated list is a mistake and refused (so a typo cannot make a gift both loved and hated). Registering the same key again replaces the
     * earlier tastes (a reload).
     */
    public static void register(String key, List<Item> loves, List<Item> accepts, List<Item> hates) {
        if (!CompanionData.validKey(key)) throw new IllegalArgumentException("a bond key is lower case letters, digits and underscores: " + key);
        Set<Item> l = new LinkedHashSet<>(loves == null ? List.of() : loves), a = new LinkedHashSet<>(accepts == null ? List.of() : accepts), h = new LinkedHashSet<>(hates == null ? List.of() : hates);
        for (Item hated : h) if (l.contains(hated) || a.contains(hated)) throw new IllegalArgumentException(key + " both likes and hates " + hated);
        if (TASTES.size() >= MAX_KEYS && !TASTES.containsKey(key)) throw new IllegalStateException("too many companions registered");
        TASTES.put(key, new Taste(Collections.unmodifiableSet(l), Collections.unmodifiableSet(a), Collections.unmodifiableSet(h)));
    }

    public static boolean has(String key) { return key != null && TASTES.containsKey(key); }

    /** The tastes of a companion (empty ones when she has none registered). */
    public static Taste of(String key) { return key == null ? NONE : TASTES.getOrDefault(key, NONE); }

    /** How the companion takes this stack. {@code food} is whether the item can be eaten. */
    public static CompanionRules.Gift classify(String key, ItemStack stack, boolean food) {
        Taste t = of(key);
        Item item = stack.getItem();
        return CompanionRules.classify(t.loves().contains(item), t.accepts().contains(item), t.hates().contains(item), food);
    }

    /** Forgets everything (tests only). */
    public static void clearForTests() { TASTES.clear(); }
}
