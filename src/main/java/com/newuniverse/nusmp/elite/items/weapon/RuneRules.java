package com.newuniverse.nusmp.elite.items.weapon;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Why: the rules of the Runic Swords' rune slots with no Minecraft in them, so they are tested. A Runic Sword has slots by tier (Renewed 1, Ancient 2, Mythical 3). A rune is an ENGRAVING (a Tensura
 * enchantment such as {@code tensura:severance}) that was put into a slot either from an enchanted book that carries it or from a rune item named in {@code data/nusmp/elite/items/runes.json};
 * a slotted rune stays on the sword as a real enchantment (so Tensura's own engraving rules apply) and is remembered in the {@code nusmp:runes} component so the sword knows how many slots are used.
 * The same engraving cannot be slotted twice. A rune whose enchantment is gone (a grindstone took it) is dropped from the list ({@link #reconcile}), which frees the slot. Each active rune adds a flat
 * attack damage ({@link #damagePerRune}), applied as an attribute modifier by the item.
 */
public final class RuneRules {
    private RuneRules() {}

    /** The result of trying to slot a rune. */
    public enum Slotting { OK, FULL, DUPLICATE, INVALID }

    /** The number of rune slots of a Runic Sword item id ({@code runic_sword_ancient}); 0 for anything else. */
    public static int slots(String weaponId) {
        if (weaponId == null) return 0;
        return switch (weaponId) {
            case "runic_sword_renewed" -> 1;
            case "runic_sword_ancient" -> 2;
            case "runic_sword_mythical" -> 3;
            default -> 0;
        };
    }

    /** Flat attack damage one active rune adds: the better the sword, the stronger its runes. */
    public static double damagePerRune(String weaponId) {
        if (weaponId == null) return 0;
        return switch (weaponId) {
            case "runic_sword_renewed" -> 1.0;
            case "runic_sword_ancient" -> 1.5;
            case "runic_sword_mythical" -> 2.0;
            default -> 0;
        };
    }

    /** A rune id must look like {@code namespace:path}. */
    public static boolean validRuneId(String id) { return id != null && id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"); }

    /** May this rune be slotted into a sword with these active runes and this many slots? */
    public static Slotting check(List<String> active, int slots, String rune) {
        if (!validRuneId(rune)) return Slotting.INVALID;
        if (active.contains(rune)) return Slotting.DUPLICATE;
        if (active.size() >= slots) return Slotting.FULL;
        return Slotting.OK;
    }

    /** The active runes after slotting {@code rune}; the list unchanged when {@link #check} refuses. */
    public static List<String> add(List<String> active, int slots, String rune) {
        List<String> out = new ArrayList<>(active);
        if (check(active, slots, rune) == Slotting.OK) out.add(rune);
        return out;
    }

    /** Drops the runes whose enchantment is no longer on the sword, and anything beyond the slot count. */
    public static List<String> reconcile(List<String> stored, int slots, Predicate<String> stillEnchanted) {
        List<String> out = new ArrayList<>();
        for (String r : stored) if (out.size() < slots && validRuneId(r) && !out.contains(r) && stillEnchanted.test(r)) out.add(r);
        return out;
    }

    /** The total flat damage of the active runes. */
    public static double bonusDamage(String weaponId, int activeRunes) { return damagePerRune(weaponId) * Math.max(0, activeRunes); }
}
