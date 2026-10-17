package com.newuniverse.nusmp.arc.midnight;

import java.util.Locale;
import java.util.Set;

/**
 * Why: the Phoenix Feathers Robe is one of Fana's own defences, a mantle of flame that closes over her. A defence that nothing goes through is a wall, and a
 * wall is not a fight, so the robe has an honest weakness that the story itself suggests for a fire elf: her flames shrug off fire and most blows, but water
 * and ice go through it whole. The classification works on the NAME of the damage type (the Tensura and Black Clover spells of this mod all carry their
 * element in their damage type id: {@code fire_elemental}, {@code water_elemental}, ...), split into words so that "device" is not mistaken for "ice". Pure, so the
 * weakness is proven by a test and can be retuned in one place.
 */
public final class PhoenixRobe {
    private PhoenixRobe() {}

    public enum Element { FIRE, COOL, OTHER }

    /** How long the robe lasts, and how close an attacker must stand to be scorched by it. */
    public static final int DURATION = 80;
    public static final double THORNS_RADIUS = 4.0;
    public static final int THORNS_SECONDS = 3;

    private static final Set<String> COOL_WORDS = Set.of("water", "ice", "icy", "freeze", "freezing", "frost", "frozen", "cold", "snow", "blizzard", "chill", "sea", "tide", "rain");
    private static final Set<String> FIRE_WORDS = Set.of("fire", "flame", "flames", "burn", "burning", "lava", "magma", "heat", "hellfire", "inferno", "explosion", "fireball", "blaze", "ember");

    /** The element of a damage type id such as {@code tensura:fire_elemental} or {@code minecraft:on_fire}. A cool word wins over a fire word ("steam"). */
    public static Element classify(String damageTypeId) {
        if (damageTypeId == null) return Element.OTHER;
        String path = damageTypeId.toLowerCase(Locale.ROOT);
        int colon = path.indexOf(':');
        if (colon >= 0) path = path.substring(colon + 1);
        boolean fire = false;
        for (String w : path.split("[^a-z]+")) {
            if (COOL_WORDS.contains(w)) return Element.COOL;
            if (FIRE_WORDS.contains(w)) fire = true;
        }
        return fire ? Element.FIRE : Element.OTHER;
    }

    /** The share of a blow that goes through the robe while it is up: 40% of fire, 55% of anything else, all of water and ice. */
    public static double factor(Element e) {
        return switch (e) {
            case FIRE -> 0.40;
            case OTHER -> 0.55;
            case COOL -> 1.0;
        };
    }
}
