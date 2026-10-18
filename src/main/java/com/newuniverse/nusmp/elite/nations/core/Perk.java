package com.newuniverse.nusmp.elite.nations.core;

/**
 * Why: one perk a nation level grants (data: {@code data/nusmp/elite/nations/perks.json}). {@code type} is one of the constants below, {@code arg} an optional parameter (the attribute id of an
 * "attribute" perk), {@code value} the amount.
 */
public record Perk(String type, String arg, double value) {
    /** A member attribute buff near the capital: {@code arg} = attribute id (minecraft:generic.max_health), {@code value} = amount added. */
    public static final String ATTRIBUTE = "attribute";
    /** Percent off shop prices for members (read by shops through {@code NationApi.priceModifierPercent}). */
    public static final String SHOP_DISCOUNT = "shop_discount";
    /** Extra copper coins on the treasury cap. */
    public static final String TREASURY_CAP = "treasury_cap";
    /** Extra member slots. */
    public static final String MEMBER_CAP = "member_cap";
    /** Extra guards around the capital (spawner tracker). */
    public static final String GUARD_COUNT = "guard_count";
    /** Percent more nation XP and coins from completed contracts. */
    public static final String CONTRACT_BONUS = "contract_bonus";
    /** Extra open contracts on the board. */
    public static final String BOARD_SLOTS = "board_slots";

    public boolean is(String t) { return type.equals(t); }
}
