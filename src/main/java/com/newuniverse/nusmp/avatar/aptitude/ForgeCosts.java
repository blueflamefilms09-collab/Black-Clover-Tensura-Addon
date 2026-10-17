package com.newuniverse.nusmp.avatar.aptitude;

import java.util.List;

/**
 * Why: the price of an Avatar's gifts is the heart of the Soul Forge ("manually picking a rare aptitude inflicts a massive, permanent tax; the free gamble
 * costs nothing"), and the server must compute it from its OWN data, never from a number the client sent. This is that one formula, pure, so the server
 * validator, the unit tests and the screen's live cost panel all use exactly the same arithmetic.
 *
 * <ul>
 *   <li><b>Permanent tax</b> (a fraction of the creator's maximum magicule, applied through the existing Avatar tax machinery): the sum of
 *       {@code taxPercent / 100 * aptitudeTaxScale} over the HAND-PICKED aptitudes (a rolled one is free), plus {@code epTaxPercent / 100 * skillTaxScale}
 *       for each chosen skill, plus the awakening tax when the creator pays for a guaranteed awakening. Never more than 0.9.</li>
 *   <li><b>Magicule paid at once</b>: that same fraction times {@code upfrontFactor} times the creator's maximum magicule. So a creator pays the share of
 *       their pool they give up for good once more, now, out of what they hold: they must be filled up to bind something heavy.</li>
 *   <li><b>Genesis Cores</b>: one for the Avatar, plus the extra ones of a paid awakening.</li>
 * </ul>
 */
public final class ForgeCosts {
    private ForgeCosts() {}

    /** The bill. {@code permanentTax} and the three parts are fractions (0.15 = 15% of the creator's maximum magicule). */
    public record Quote(double aptitudeTax, double skillTax, double awakeningTax, double permanentTax, double upfrontMagicule, int coresNeeded) {
        /** True when the Avatar costs the creator anything beyond its Genesis Core. */
        public boolean taxed() { return permanentTax > 1.0e-12; }
    }

    public static final double TAX_LIMIT = 0.9;

    /**
     * @param handPicked the aptitudes picked by hand (null entries are ignored; the rolled aptitude is NOT in this list)
     * @param unique     the chosen Unique skill or null
     * @param ultimate   the chosen Ultimate skill or null
     */
    public static Quote quote(List<Aptitude> handPicked, ForgeSkill unique, ForgeSkill ultimate, boolean paidAwakening, ForgeRules rules, double maxMagicule) {
        double apt = 0.0;
        if (handPicked != null) for (Aptitude a : handPicked) if (a != null) apt += a.taxPercent() / 100.0 * rules.aptitudeTaxScale();
        double skill = 0.0;
        if (unique != null) skill += unique.epTaxPercent() / 100.0 * rules.skillTaxScale();
        if (ultimate != null) skill += ultimate.epTaxPercent() / 100.0 * rules.skillTaxScale();
        double awake = paidAwakening ? rules.awakeningTax() : 0.0;
        double total = Math.min(TAX_LIMIT, apt + skill + awake);
        double upfront = total * rules.upfrontFactor() * Math.max(0.0, maxMagicule);
        int cores = 1 + (paidAwakening ? rules.awakeningExtraCores() : 0);
        return new Quote(apt, skill, awake, total, upfront, cores);
    }

    /** The chance (0..1) of the free Instant Skill Awakening at spawn: a d100 below {@code base + luck}, capped. */
    public static double awakeningChance(ForgeRules rules, double luck) {
        double threshold = rules.awakeningBase() + (Double.isNaN(luck) ? 0.0 : luck);
        double successes = Math.ceil(threshold) - 1.0;                     // the values 1..100 that are strictly below the threshold
        double percent = Math.max(0.0, Math.min(rules.awakeningMaxChance(), Math.min(100.0, successes)));
        return percent / 100.0;
    }

    /** The d100 itself: {@code roll} is 1..100, true when it is strictly below {@code base + luck} (and the cap lets it). */
    public static boolean awakens(ForgeRules rules, double luck, int roll) {
        if (roll < 1 || roll > 100) return false;
        return roll <= Math.round(awakeningChance(rules, luck) * 100.0);
    }
}
