package com.newuniverse.nusmp.shishou;

import java.util.Locale;

/**
 * Why: the Appraisal readout (a small AR panel on the targeted mob: health, a mana-ish number, a level-ish number, the skill it would give, its weak elements) is built from a few numbers the server
 * already knows, by pure formulas, so the reply is one tiny packet and the numbers are tested. The "level" is an estimate (the novel's appraisal also gives estimates for what it cannot see).
 */
public final class ShishouAppraisal {
    private ShishouAppraisal() {}

    public static final int W_FIRE = 1, W_ICE = 2, W_LIGHTNING = 4, W_POISON = 8, W_DARK = 16, W_HOLY = 32;

    /** The level-ish number of a mob: half its health, twice its attack, plus its armour, at least 1, at most 999. */
    public static int levelish(double maxHealth, double attack, double armor) {
        double v = maxHealth * 0.5 + attack * 2.0 + armor;
        return (int) Math.max(1, Math.min(999, Math.round(v)));
    }

    /** The mana-ish number: what its magic stone would be worth in experience (the same numbers Skill Eater uses). */
    public static int manaish(double maxHealth) { return (int) Math.min(9999L, ShishouPredation.killExp(maxHealth, false)); }

    /** The weak elements of a mob by its entity type id: a short table of the obvious ones. */
    public static int weaknesses(String typeId) {
        if (typeId == null) return 0;
        String p = typeId.toLowerCase(Locale.ROOT);
        int c = p.indexOf(':');
        if (c >= 0) p = p.substring(c + 1);
        int m = 0;
        if (has(p, "blaze", "magma", "fire", "flame", "ifrit", "lava")) m |= W_ICE;
        if (has(p, "stray", "snow", "frost", "ice", "blizzard", "polar", "drowned")) m |= W_FIRE | W_LIGHTNING;
        if (has(p, "skeleton", "zombie", "husk", "phantom", "wraith", "ghost", "undead", "wither", "vex")) m |= W_HOLY | W_FIRE;
        if (has(p, "spider", "silverfish", "endermite")) m |= W_FIRE;
        if (has(p, "enderman")) m |= W_ICE;
        if (has(p, "shadow", "demon", "dark")) m |= W_HOLY;
        if (has(p, "holy", "angel")) m |= W_DARK;
        if (has(p, "slime", "gel")) m |= W_FIRE;
        return m;
    }

    private static boolean has(String p, String... words) {
        for (String w : words) if (p.contains(w)) return true;
        return false;
    }

    /** The lang key of one weakness bit. */
    public static String weaknessKey(int bit) {
        return switch (bit) {
            case W_FIRE -> "shishou.element.fire";
            case W_ICE -> "shishou.element.ice";
            case W_LIGHTNING -> "shishou.element.lightning";
            case W_POISON -> "shishou.element.poison";
            case W_DARK -> "shishou.element.dark";
            case W_HOLY -> "shishou.element.holy";
            default -> "shishou.element.none";
        };
    }
}
