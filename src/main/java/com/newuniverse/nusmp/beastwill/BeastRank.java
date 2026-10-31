package com.newuniverse.nusmp.beastwill;

/**
 * Why: the rank of a mana beast, the letter the Beast Will system reads (3.45.0). Tensura gives its monsters Existence Points but no public rank, so the rank is worked out from the beast's EP
 * against the thresholds in {@link BeastWillRules} (a server setting), and a Beast Will names the lowest rank it comes from. The name is saved, never the ordinal; new ranks only go at the end.
 */
public enum BeastRank {
    C, B, A, S, SS;

    public static BeastRank byName(String n) {
        if (n != null) for (BeastRank r : values()) if (r.name().equalsIgnoreCase(n.trim())) return r;
        return null;
    }

    public boolean atLeast(BeastRank other) { return other == null || ordinal() >= other.ordinal(); }
}
