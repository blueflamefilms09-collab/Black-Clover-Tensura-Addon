package com.newuniverse.nusmp.elite.skills.evolution;

import java.util.Locale;

/**
 * Why: not only how much was forged but how well. The best quality the player ever forged ({@code EliteHooks.forgeBestQuality}, 0 none, 1 common ... 6 mythical) must reach
 * {@code min}, given as a number or a name (common, uncommon, rare, epic, legendary, mythical).
 */
public final class ForgeQuality implements EvolutionTrigger {
    private static final String[] NAMES = {"none", "common", "uncommon", "rare", "epic", "legendary", "mythical"};

    @Override public String id() { return "forge_quality"; }

    /** The numeric level of a parameter: a number, or a quality name (unknown names count as 1). */
    static int level(Params p) {
        if (!p.has("min")) return 1;
        String raw = p.str("min", "1").trim().toLowerCase(Locale.ROOT);
        for (int i = 0; i < NAMES.length; i++) if (NAMES[i].equals(raw)) return i;
        return Math.max(0, p.integer("min", 1));
    }

    @Override
    public boolean test(EvolutionContext ctx, Params p, String skillId) { return ctx.forgeQuality() >= level(p); }

    @Override
    public String describe(Params p, String skillId) {
        int lv = Math.max(0, Math.min(NAMES.length - 1, level(p)));
        return "forge something of " + NAMES[lv] + " quality";
    }
}
