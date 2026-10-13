package com.newuniverse.nusmp.relics.wave;

/**
 * Wave 1: the pure text and name tables (no Minecraft imports): the Book of Mispells' seven spells and what they truly are, the lines
 * "Dad" answers with, and the dye cycle the Rainbow Rapier recolours blocks with.
 */
public final class Wave1Words {
    private Wave1Words() {}

    /** One page of the Book of Mispells: the spell it claims to be, what it truly is, and the line shown when it goes off. */
    public record Misspell(String shown, String truth, String reveal) {}

    public static final Misspell[] MISSPELLS = {
            new Misspell("Message", "Massage", "It was not Message at all: it was Massage. A pleasant backrub."),
            new Misspell("Mage Armour", "Mage Armoir", "It was Mage Armoir: a fancy wardrobe is simply there now."),
            new Misspell("Hold Person", "Fold Person", "It was Fold Person: they bend over backwards."),
            new Misspell("Speak with Dead", "Speak with Dad", "It was Speak with Dad."),
            new Misspell("Ice Storm", "Ick Storm", "It was Ick Storm: gross sludge rains down."),
            new Misspell("Teleport", "Telepoot", "It was Telepoot."),
            new Misspell("Wish", "Wash", "It was Wash: a wet sponge scrubs you vigorously."),
    };

    /** What Dad says over the line (the card's "for 1 minute you can communicate aloud with your father"). */
    public static final String[] DAD = {
            "Dad: Wear a jacket, it is colder than it looks.",
            "Dad: Did you eat? You look thin.",
            "Dad: Measure twice, cut once. Mine twice, dig once.",
            "Dad: Hi, hungry, I'm Dad. ...Was that the wrong plane for a joke?",
            "Dad: Never go into a cave without a torch, and a second torch, and a spare torch.",
            "Dad: I am proud of you. Now put some boots on.",
            "Dad: Back in my day we crafted by hand. Uphill. Both ways.",
            "Dad: Call your mother. She worries.",
    };

    public static String pick(String[] lines, long seed) {
        return lines[(int) Math.floorMod(seed * 2654435761L, (long) lines.length)];
    }

    // ---------------------------------------------------------------- the dye cycle
    /** Vanilla dye order. */
    public static final String[] COLOURS = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};

    /** Block families that come in all sixteen dyes and may be recoloured by touch (no containers, no beds, no banners). */
    public static final String[] FAMILIES = {"wool", "carpet", "concrete", "concrete_powder", "terracotta", "glazed_terracotta",
            "stained_glass", "stained_glass_pane"};

    /**
     * The block path one dye further round the vanilla dye order ("lime_wool" -> "pink_wool", "black_wool" -> "white_wool"), or null
     * if the path is not a dyeable family. The undyed terracotta, glass and glass pane enter the cycle at white.
     */
    public static String nextDye(String path) {
        if (path == null) return null;
        switch (path) {
            case "terracotta": return "white_terracotta";
            case "glass": return "white_stained_glass";
            case "glass_pane": return "white_stained_glass_pane";
            default: break;
        }
        for (int i = 0; i < COLOURS.length; i++) {
            String prefix = COLOURS[i] + "_";
            if (!path.startsWith(prefix)) continue;
            String family = path.substring(prefix.length());
            for (String f : FAMILIES) {
                if (f.equals(family)) return COLOURS[(i + 1) % COLOURS.length] + "_" + family;
            }
        }
        return null;
    }
}
