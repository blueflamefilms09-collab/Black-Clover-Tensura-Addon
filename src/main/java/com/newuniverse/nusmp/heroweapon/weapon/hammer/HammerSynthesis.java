package com.newuniverse.nusmp.heroweapon.weapon.hammer;

import java.util.Collection;

/**
 * Why: the canon power-up method of the Vassal Hammer is Weapon Form Synthesis: the hammer copies the most characteristic special effect of one weapon form onto another (from
 * the fandom wiki's search summary; the page is thin on how it plays). Here that becomes a rule the player can feel: every form has ONE signature effect (a passive), and the top
 * form, which carries the passive {@code synthesis}, keeps the signature effect of every form the wielder has learned, not only its own. A lower form keeps only its own. This
 * class is the table of signatures and the rule, pure, so it can be tested without the game.
 */
public final class HammerSynthesis {
    private HammerSynthesis() {}

    /** The signature passive of a form, or null for a form that has none. */
    public static String signature(String formId) {
        if (formId == null) return null;
        return switch (formId) {
            case "base" -> HammerNumbers.HEAVY_BLOWS;
            case "stone_hammer" -> HammerNumbers.RUBBLE;
            case "anvil_hammer" -> HammerNumbers.ANVIL_STANCE;
            case "synthesis_hammer" -> HammerNumbers.CRUSHING_WEIGHT;
            default -> null;
        };
    }

    /**
     * True when {@code passive} is in force: it is one of the active form's own, or the active form synthesises and the signature of some learned form is that passive.
     * {@code learnedForms} are the ids of the forms the bond has learned (the base form is always among them).
     */
    public static boolean effective(String passive, Collection<String> formPassives, Collection<String> learnedForms) {
        if (formPassives.contains(passive)) return true;
        if (!formPassives.contains(HammerNumbers.SYNTHESIS)) return false;
        for (String form : learnedForms) if (passive.equals(signature(form))) return true;
        return false;
    }
}
