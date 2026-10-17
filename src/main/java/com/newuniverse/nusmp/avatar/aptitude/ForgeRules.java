package com.newuniverse.nusmp.avatar.aptitude;

/**
 * Why: the balance numbers of the Soul Forge (how many aptitudes, how heavy the taxes are, how the awakening works) live in a config file the server
 * owner edits, but the maths that uses them (the cost quote, the validator, the roll) is pure and unit tested. This record is the snapshot of those
 * numbers the pure code reads; {@code AvatarSoulForgeConfig.rules()} builds it from the config, the tests build it by hand. Every value is clamped here,
 * so a nonsensical config can never make the maths divide by zero or return a tax above the pool.
 *
 * @param maxAptitudes       the most aptitudes an Avatar can carry (hand picked plus rolled), 0..8
 * @param aptitudeTaxScale   multiplier on the tax percent of a hand-picked aptitude (1.0 = as written in the data)
 * @param skillTaxScale      multiplier on the EP tax of a chosen Unique / Ultimate skill
 * @param maxTotalTax        the most permanent tax (fraction of the creator's maximum magicule, all their Avatars together) a creation may leave them with
 * @param upfrontFactor      magicule paid at once = tax fraction * this * the creator's maximum magicule (0 = nothing up front)
 * @param awakeningBase      the 5 of "below 5 + total luck" (on a d100)
 * @param awakeningMaxChance the most chance, in percent, the awakening roll can have however lucky the creator is
 * @param awakeningTax       the permanent tax (fraction) of paying for a guaranteed awakening
 * @param awakeningExtraCores how many Genesis Cores the paid awakening costs on top of the one the Avatar needs
 * @param rollsMax           how many times one ritual may use the free Randomize button
 * @param luckStrength       how strongly luck shifts the roll towards the rarer classes (see {@link AptitudeRoll})
 * @param chosenSkillsSecure true: an Avatar that was given a Unique or Ultimate skill in the forge counts as holding one (no betrayal roll)
 */
public record ForgeRules(int maxAptitudes, double aptitudeTaxScale, double skillTaxScale, double maxTotalTax, double upfrontFactor, double awakeningBase,
                         double awakeningMaxChance, double awakeningTax, int awakeningExtraCores, int rollsMax, double luckStrength, boolean chosenSkillsSecure) {
    public static final ForgeRules DEFAULT = new ForgeRules(2, 1.0, 1.0, 0.60, 1.0, 5.0, 75.0, 0.05, 1, 3, 0.06, true);

    public ForgeRules {
        maxAptitudes = Math.max(0, Math.min(8, maxAptitudes));
        aptitudeTaxScale = clamp(aptitudeTaxScale, 0.0, 10.0);
        skillTaxScale = clamp(skillTaxScale, 0.0, 10.0);
        maxTotalTax = clamp(maxTotalTax, 0.0, 0.9);
        upfrontFactor = clamp(upfrontFactor, 0.0, 10.0);
        awakeningBase = clamp(awakeningBase, 0.0, 100.0);
        awakeningMaxChance = clamp(awakeningMaxChance, 0.0, 100.0);
        awakeningTax = clamp(awakeningTax, 0.0, 0.9);
        awakeningExtraCores = Math.max(0, Math.min(8, awakeningExtraCores));
        rollsMax = Math.max(0, Math.min(20, rollsMax));
        luckStrength = clamp(luckStrength, 0.0, 1.0);
    }

    private static double clamp(double v, double lo, double hi) { return Double.isNaN(v) ? lo : Math.max(lo, Math.min(hi, v)); }
}
