package com.newuniverse.nusmp.mana.method;

/**
 * 3.8.0: a rune writes the user's OWN element (as in the anime: a fire mage writes fire runes, not all four). Pure rules (no game classes).
 *
 * <p>The rule is decided by the grimoire the page belongs to (the page is a page OF that grimoire, on both sides, so a second grimoire of the same player decides for itself):
 * the grimoire's own magic, and the two parents written on it when it is a fused (hybrid) grimoire ("FLAME:WATER" on a book of another attribute). A book counts for every one of
 * those names that is a basic natural element: Flame, Water, Wind, Earth. The table is the same set as {@code TrueMagicRules.NATURAL} minus Lightning and Plant (they have no basic
 * rune page; the True Magic Rune page covers them). Every other magic (Time, Star, Magma, Ice, Storm, Sealing ...) writes NO elemental rune page; it keeps the non-elemental pages.
 *
 * <p>The four elemental pages are listed and castable only when {@code manaMethod.ownElementOnly} is on AND the grimoire owns the element, or when the setting is off (all four, as
 * in 3.7.0). Every other page has no element and is never filtered by this.
 */
public final class RuneOwnElement {
    private RuneOwnElement() {}

    /** The element of a basic rune page id, or null for any other page (the True Magic page and the mobility, body, brush and state pages are not elemental pages). */
    public static RuneElement pageElement(String pageId) {
        if (pageId == null) return null;
        return switch (pageId) {
            case "mana_method_fire" -> RuneElement.FIRE;
            case "mana_method_water" -> RuneElement.WATER;
            case "mana_method_wind" -> RuneElement.WIND;
            case "mana_method_earth" -> RuneElement.EARTH;
            default -> null;
        };
    }

    /** The basic element a MagicType NAME stands for (Flame, Water, Wind, Earth), else null. Lightning, Plant and every other magic give null here. */
    public static RuneElement basicOf(String magicName) {
        RuneElement e = RuneElement.ofMagic(magicName);
        return e == null || e == RuneElement.LIGHTNING || e == RuneElement.PLANT ? null : e;
    }

    /** Whether a grimoire of magic {@code magicName} with the fusion string {@code fusion} ("A:B" or empty/null) owns the basic element {@code e}. Allocation-free. */
    public static boolean owns(String magicName, String fusion, RuneElement e) {
        if (e == null) return false;
        if (basicOf(magicName) == e) return true;
        if (fusion == null || fusion.isEmpty()) return false;
        int cut = fusion.indexOf(':');
        if (cut <= 0 || cut >= fusion.length() - 1) return false;                   // not a pair: no parents
        return basicOfRegion(fusion, 0, cut) == e || basicOfRegion(fusion, cut + 1, fusion.length()) == e;
    }

    private static RuneElement basicOfRegion(String s, int from, int to) {
        int len = to - from;
        if (len == 5 && s.regionMatches(true, from, "FLAME", 0, 5)) return RuneElement.FIRE;
        if (len == 5 && s.regionMatches(true, from, "WATER", 0, 5)) return RuneElement.WATER;
        if (len == 4 && s.regionMatches(true, from, "WIND", 0, 4)) return RuneElement.WIND;
        if (len == 5 && s.regionMatches(true, from, "EARTH", 0, 5)) return RuneElement.EARTH;
        return null;
    }

    /** May this grimoire use the page: not elemental, or the setting is off, or the grimoire owns the page's element. */
    public static boolean allowed(boolean ownElementOnly, String pageId, String magicName, String fusion) {
        RuneElement page = pageElement(pageId);
        if (page == null || !ownElementOnly) return true;
        return owns(magicName, fusion, page);
    }

    /** The basic elements a grimoire owns, in the order of {@link RuneElement#BASIC} (for messages and tests). */
    public static java.util.List<RuneElement> ownedBy(String magicName, String fusion) {
        java.util.List<RuneElement> l = new java.util.ArrayList<>(2);
        for (RuneElement e : RuneElement.BASIC) if (owns(magicName, fusion, e)) l.add(e);
        return l;
    }
}
