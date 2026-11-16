package com.newuniverse.nusmp.stones;

/**
 * Why: the six stones are the existing Wave 30 relics; this enum only names them for the slot code (relic id, colour, act keys) so no string is repeated in the rules.
 * Pure data, no Minecraft classes. Append only; the order is the order of the Gauntlet's knuckles.
 */
public enum InfinityStone {
    SPACE("space", "the_space_stone_or_the_tesseract", 0x3A8BFF),
    MIND("mind", "the_mind_stone", 0xFFD93A),
    POWER("power", "the_power_stone", 0xA04AF0),
    TIME("time", "the_time_stone_or_the_eye_of_agamotto", 0x3AE07A),
    REALITY("reality", "the_reality_stone", 0xE03A3A),
    SOUL("soul", "the_soul_stone", 0xFF8A2A);

    private final String id, relicId;
    private final int rgb;

    InfinityStone(String id, String relicId, int rgb) {
        this.id = id;
        this.relicId = relicId;
        this.rgb = rgb;
    }

    public String id() { return id; }

    public String relicId() { return relicId; }

    public int rgb() { return rgb; }

    /** The key of the first (1) or second (2) slot act this mod adds to the stone's relic. */
    public String actKey(int n) { return "inf_" + id + "_" + n; }

    public static InfinityStone byRelicId(String relicId) {
        if (relicId == null) return null;
        for (InfinityStone s : values()) if (s.relicId.equals(relicId)) return s;
        return null;
    }

    public static InfinityStone byId(String id) {
        if (id == null) return null;
        for (InfinityStone s : values()) if (s.id.equalsIgnoreCase(id)) return s;
        return null;
    }

    /** Is this act key one of the slot acts (they only work in the infinity_stone slot)? */
    public static boolean isSlotAct(String actKey) { return actKey != null && actKey.startsWith("inf_"); }

    public static InfinityStone ofActKey(String actKey) {
        if (!isSlotAct(actKey)) return null;
        for (InfinityStone s : values()) if (actKey.startsWith("inf_" + s.id + "_")) return s;
        return null;
    }
}
