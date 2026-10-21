package com.newuniverse.nusmp.client.relics.worn.lib;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

/**
 * The data check behind "a relic worn in a vanilla armour slot is always on the body": for a relic of the HEAD, CHEST or FEET slot it resolves, through the real catalogs, the worn
 * models it would wear (the visual upgrade's {@code up} models first, as {@link WornLibrary} does, then its own entry, then its slot and look, then the default look of its slot) and
 * checks that at least one of them loads AND hangs on a body part of that armour slot (a helmet on the head, boots on the legs, a chest piece on the body or the arms). The painted
 * layer under it always exists ({@code ArmorSlotPlan}), so a relic that fails this check is still drawn as its painted armour, but its model never reaches the body: the failure this
 * test exists to catch. Pure: no Minecraft types; the self-test {@code visual.armor_slot_relics} and the JUnit test both run it.
 */
public final class ArmorSlotCoverage {
    private ArmorSlotCoverage() {}

    /** The route a relic is drawn by. */
    public enum Route { UPGRADED_MODEL, LIBRARY_MODEL, NONE }

    /** The answer for one relic. {@code detail} names the pieces (or the reason there is no model). */
    public record Resolution(Route route, String detail) {
        public boolean ok() { return route != Route.NONE; }
    }

    /** The slots a relic is worn in as a vanilla armour piece. */
    public static boolean isArmourSlot(String slot) {
        if (slot == null) return false;
        return switch (slot.toUpperCase(Locale.ROOT)) {
            case "HEAD", "CHEST", "FEET" -> true;
            default -> false;
        };
    }

    /** The body parts a model of this armour slot may hang on. */
    public static Set<WornAnchor> anchorsOf(String slot) {
        return switch (slot.toUpperCase(Locale.ROOT)) {
            case "HEAD" -> EnumSet.of(WornAnchor.HEAD);
            case "CHEST" -> EnumSet.of(WornAnchor.BODY, WornAnchor.RIGHT_ARM, WornAnchor.LEFT_ARM);
            case "FEET" -> EnumSet.of(WornAnchor.RIGHT_LEG, WornAnchor.LEFT_LEG);
            default -> EnumSet.noneOf(WornAnchor.class);
        };
    }

    /**
     * @param anchorOf the body part the model file of a name hangs on, or null when the file is missing or broken
     * @param upgrade  the catalog of the visual upgrade (may be {@link UpgradeCatalog#EMPTY})
     */
    public static Resolution resolve(String id, String slot, String look, WornCatalog cat, UpgradeCatalog upgrade, Function<String, WornAnchor> anchorOf) {
        Set<WornAnchor> want = anchorsOf(slot);
        if (upgrade != null && upgrade.has(id)) {
            String why = check(upgrade.up(id), want, anchorOf);
            if (why == null) return new Resolution(Route.UPGRADED_MODEL, "upgraded: " + names(upgrade.up(id)));
            // an upgraded model that does not stand on the slot falls back to the older look in the library, so this is reported only when the older look fails too
            Resolution older = resolve(id, slot, look, cat, null, anchorOf);
            return older.ok() ? older : new Resolution(Route.NONE, "upgraded look: " + why + "; older look: " + older.detail());
        }
        List<WornCatalog.Piece> pieces = cat.piecesFor(id, slot, look);
        if (pieces.isEmpty()) return new Resolution(Route.NONE, "no catalog entry for " + slot + "/" + look + " and no default look for the slot");
        String why = check(pieces, want, anchorOf);
        return why == null ? new Resolution(Route.LIBRARY_MODEL, names(pieces)) : new Resolution(Route.NONE, why);
    }

    private static String names(List<WornCatalog.Piece> pieces) {
        List<String> out = new ArrayList<>();
        for (WornCatalog.Piece p : pieces) out.add(p.model());
        return String.join(" ", out);
    }

    /** Null when at least one piece loads and stands on a wanted body part; else the reason. */
    private static String check(List<WornCatalog.Piece> pieces, Set<WornAnchor> want, Function<String, WornAnchor> anchorOf) {
        List<String> notes = new ArrayList<>();
        for (WornCatalog.Piece p : pieces) {
            if (WornCatalog.NONE.equals(p.model())) continue;
            WornAnchor file = anchorOf.apply(p.model());
            if (file == null) {
                notes.add("model file " + p.model() + " is missing or broken");
                continue;
            }
            WornAnchor at = p.anchor() != null ? p.anchor() : file;
            if (want.contains(at)) return null;
            notes.add(p.model() + " hangs on " + at.key + ", a " + want + " piece needs one of " + want);
        }
        return notes.isEmpty() ? "no piece draws anything" : String.join("; ", notes);
    }
}
