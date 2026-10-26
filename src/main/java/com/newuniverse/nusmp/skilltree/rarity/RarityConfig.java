package com.newuniverse.nusmp.skilltree.rarity;

/**
 * Why: the server settings of the rarity tiers are read from a TOML file, and a config value that is not loaded yet, hand-edited to nonsense, or read on the wrong thread must never reach the drop rules as it
 * is. This is the plain-values snapshot of the {@code [rarity]} section ({@code rarityTiers}, {@code guardianBookChance}, {@code rarityBaseChance}, {@code hideBlockedFranchises},
 * {@code blockedFranchises}) that the loot glue builds ONCE per drop from the config and hands to the pure rules, so the rules and their tests need no config class and no Minecraft type. The two
 * chances are clamped to 0..1 (NaN becomes 0) and a null blocked list becomes empty. {@link #effectiveBlocked} is the setting the franchise blacklist works with: the list when
 * {@code hideBlockedFranchises} is on, an empty string (nothing blocked) when it is off.
 *
 * @param tiers          true = the rarity-weighted drops; false = the old pick and the old 0.004 / 0.30 numbers, exactly as in 3.25
 * @param guardianChance the chance that a decennial Guardian drops its themed book (1.0 = guaranteed)
 * @param baseChance     the base chance per kill that an ordinary Abyssal monster drops a book (0.001 = 0.1%), before Luck and Looting
 * @param hideBlocked    true = the blacklist is on
 * @param blocked        the comma separated franchise keys of the blacklist
 */
public record RarityConfig(boolean tiers, double guardianChance, double baseChance, boolean hideBlocked, String blocked) {
    public RarityConfig {
        guardianChance = RarityRules.unit(guardianChance);
        baseChance = RarityRules.unit(baseChance);
        blocked = blocked == null ? "" : blocked;
    }

    /** The numbers of the contract: tiers on, guaranteed Guardian book, 0.1% per monster, the blacklist on with Tensura, Black Clover and Magi. */
    public static final RarityConfig DEFAULT = new RarityConfig(true, 1.0, 0.001, true, BlockedFranchises.DEFAULT);

    /** The same with the rarity tiers off: the drops of 3.25 (the blacklist keeps its own setting). */
    public RarityConfig withTiers(boolean on) { return new RarityConfig(on, guardianChance, baseChance, hideBlocked, blocked); }

    /** The same with another blacklist setting. */
    public RarityConfig withBlocked(boolean hide, String list) { return new RarityConfig(tiers, guardianChance, baseChance, hide, list); }

    /** The blacklist in force: the list when {@code hideBlockedFranchises} is on, "" (nothing blocked) when it is off. */
    public String effectiveBlocked() { return hideBlocked ? blocked : ""; }
}
