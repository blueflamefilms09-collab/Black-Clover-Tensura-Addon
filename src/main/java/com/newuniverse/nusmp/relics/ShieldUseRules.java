package com.newuniverse.nusmp.relics;

/**
 * 1.110.0: what a right-click on a shield-class relic does (a pure decision; {@link RelicWeaponItem#use} acts on it, ShieldUseRulesTest pins it).
 * <p>
 * A shield relic used to block only when it had no act of its own and was attuned, so the Hero's Shield and the Calamity Wave Aegis (acts) and every unattuned shield
 * answered a click with a technique or a refusal and never raised. Now it is a shield first, like vanilla's: the plain click raises it, in either hand. What the
 * old click did is still there on the sneaking click, and on the Use Worn Relic key.
 */
public final class ShieldUseRules {
    private ShieldUseRules() {}

    public enum Use {
        /** Raise the shield (start using it). */
        BLOCK,
        /** Raise the shield and tell the player the relic is not attuned yet (the hint the refused click used to give). */
        BLOCK_HINT,
        /** The old relic click: the attunement ritual or the act (RelicEngine#useHeld). */
        RELIC_CLICK
    }

    public static Use decide(boolean sneaking, boolean hasActs, boolean needsAttunement, boolean attuned) {
        boolean unattuned = needsAttunement && !attuned;
        if (!sneaking) return unattuned ? Use.BLOCK_HINT : Use.BLOCK;
        if (unattuned) return Use.RELIC_CLICK;                                      // sneak + hold = the attunement ritual, as before
        return hasActs ? Use.RELIC_CLICK : Use.BLOCK;                               // sneaking with an act: the act; no act: just a shield (it blocks while sneaking, like vanilla's)
    }

    /** While the shield is held up without sneaking, the attunement ritual's clock does not run and its "broken" message is not sent: it is just a block. */
    public static boolean ritualRuns(boolean sneaking) { return sneaking; }
}
