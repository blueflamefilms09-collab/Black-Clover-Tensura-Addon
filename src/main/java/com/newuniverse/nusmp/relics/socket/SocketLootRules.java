package com.newuniverse.nusmp.relics.socket;

/**
 * The low weights at which Cores turn up, as plain numbers (pure, tested). Chests: a small chance per rolled structure chest, higher in the vaults,
 * bastions, ancient cities and trial chambers; bosses: the mod's registered bosses each drop one now and then. A Spirit Core of a common element is
 * three times likelier than light or dark; a Devil Core is picked evenly among the nine and only the bosses and the deepest chests give them.
 */
public final class SocketLootRules {
    private SocketLootRules() {}

    public static final float CHEST_SPIRIT = 0.012f, CHEST_DEEP_SPIRIT = 0.03f, CHEST_DEEP_DEVIL = 0.008f, BOSS_SPIRIT = 0.10f, BOSS_DEVIL = 0.06f;

    /** The six Spirits' weights in catalog order (fire, water, wind, earth, light, dark). */
    private static final int[] SPIRIT_WEIGHTS = {3, 3, 3, 3, 1, 1};

    /** Is this chest table path one of the deep ones (the same words as the relic loot modifier's top tier)? */
    public static boolean deep(String path) {
        return path != null && (path.contains("ancient_city") || path.contains("bastion") || path.contains("trial") || path.contains("vault"));
    }

    /** Does the path name a structure chest that may hold a Core at all? */
    public static boolean chest(String path) {
        return path != null && path.contains("chests/") && (deep(path) || path.contains("stronghold") || path.contains("dungeon"));
    }

    /** The spirit id for a roll {@code r} in [0, 1). */
    public static String pickSpirit(double r) {
        int total = 0;
        for (int w : SPIRIT_WEIGHTS) total += w;
        double x = Math.max(0.0, Math.min(0.999999, r)) * total;
        java.util.List<SocketDef> all = SocketCatalog.spirits();
        for (int i = 0; i < all.size(); i++) {
            x -= SPIRIT_WEIGHTS[Math.min(i, SPIRIT_WEIGHTS.length - 1)];
            if (x < 0) return all.get(i).id();
        }
        return all.get(all.size() - 1).id();
    }

    /** The devil id for a roll {@code r} in [0, 1): even among the devils. */
    public static String pickDevil(double r) {
        java.util.List<SocketDef> all = SocketCatalog.devils();
        int i = (int) Math.floor(Math.max(0.0, Math.min(0.999999, r)) * all.size());
        return all.get(Math.min(i, all.size() - 1)).id();
    }
}
