package com.newuniverse.nusmp.dungeon;

import com.newuniverse.nusmp.dungeon.MawShapes.Kind;

/**
 * The words the Dungeon says to the server about its portals and its food web (the owner's wordings, docs/living_dungeon.md section 2).
 * Pure, so the exact text is checked in tests. DungeonMaw.broadcast adds the config gate.
 */
public final class MawText {
    private MawText() {}

    /** Shown when the biome has no readable name. */
    public static final String UNKNOWN_BIOME = "Uncharted";

    /** "dark_forest" becomes "Dark Forest"; any separator ('_', '/', '.', '-') ends a word; a name with no letters is {@link #UNKNOWN_BIOME}. */
    public static String biomeName(String path) {
        if (path == null) return UNKNOWN_BIOME;
        StringBuilder out = new StringBuilder();
        boolean startOfWord = true;
        for (int i = 0; i < path.length(); i++) {
            char ch = path.charAt(i);
            if (Character.isLetterOrDigit(ch)) {
                out.append(startOfWord ? Character.toUpperCase(ch) : Character.toLowerCase(ch));
                startOfWord = false;
            } else if (!startOfWord) {
                out.append(' ');
                startOfWord = true;
            }
        }
        String name = out.toString().trim();
        return name.isEmpty() ? UNKNOWN_BIOME : name;
    }

    /** A Maw has opened: its manifestation ("Tower", "Ruin", "Gate"; "Maw" for the plain mouth), the biome and where (the caller rounds x and z). */
    public static String opened(Kind kind, String biomePath, int x, int z) {
        return "[Server] A spatial distortion has materialized. A new Dungeon " + kind.word + " has breached the " + biomeName(biomePath)
                + " biome near (" + x + ", " + z + ")...";
    }

    /** The predators of a stretch of the Dungeon starved. {@code tierLabel} is the label of its tier ("Volcanic Vents", "The Variable Core"). */
    public static String famine(String tierLabel) {
        return "[Server] A localized famine in " + the(tierLabel) + " has caused a mass predator die-off.";
    }

    /** "the Volcanic Vents" from "Volcanic Vents" and "the Variable Core" from "The Variable Core". */
    public static String the(String label) {
        if (label == null || label.isBlank()) return "the Dungeon";
        String l = label.trim();
        return l.regionMatches(true, 0, "The ", 0, 4) ? "the " + l.substring(4) : "the " + l;
    }
}
