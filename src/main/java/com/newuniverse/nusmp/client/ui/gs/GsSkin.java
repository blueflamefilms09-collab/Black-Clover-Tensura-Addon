package com.newuniverse.nusmp.client.ui.gs;

/**
 * The two shipped skins of the Great Spirit theme: B = Parchment and Ink (warm, the look for everybody) and C = Slime Crest (dark violet,
 * for devil users). Direction A (Holo Grimoire) is a mock-up only and is not built. Pure (no Minecraft types), unit tested.
 */
public enum GsSkin {
    B, C;

    /** The client option {@code ui.greatSpirit.skin}: AUTO picks by the user, B and C force one skin. */
    public enum Choice {
        AUTO, B, C;

        /** Reads the config text (any case); anything unknown is AUTO. */
        public static Choice parse(String s) {
            if (s != null) for (Choice c : values()) if (c.name().equalsIgnoreCase(s.trim())) return c;
            return AUTO;
        }
    }

    /** AUTO: C for a devil user, B for everybody else. A forced choice wins. */
    public static GsSkin choose(Choice choice, boolean devilUser) {
        if (choice == Choice.B) return B;
        if (choice == Choice.C) return C;
        return devilUser ? C : B;
    }
}
