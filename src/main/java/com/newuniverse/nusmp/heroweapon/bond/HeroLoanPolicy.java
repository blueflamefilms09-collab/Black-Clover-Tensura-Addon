package com.newuniverse.nusmp.heroweapon.bond;

/**
 * Why: a hero weapon stack found in a player's inventory can be one of four things (the player's own projection, someone else's, a stale one after the bond
 * ended, or a bare stack from the creative tab or a command), and the right answer for each is a small table that must not drift: keep the player's own while
 * the weapon is summoned, dissolve anything that is not theirs, adopt a bare stack when its holder has the weapon or is free to take it. Creative and spectator
 * players are outside the whole machine (the stack is a plain item for them). The table is pure so every row, including the ones that guard against abuse (a
 * projection moved to another player, a stack kept after the weapon was put away), is a unit test; {@code HeroLoan.vet} only collects the facts and obeys.
 */
public final class HeroLoanPolicy {
    private HeroLoanPolicy() {}

    /** Whose projection the stack says it is (its {@code nusmp:hero_owner} component) compared with the player holding it. */
    public enum Owner { NONE, SELF, OTHER }

    /** The holder's bond compared with the stack's weapon. */
    public enum Bond { NONE, SAME, OTHER }

    /** What to do with the stack. */
    public enum Verdict { KEEP, DISSOLVE, ADOPT }

    /**
     * The verdict for one stack. {@code exempt}: the holder is in creative or spectator mode. {@code summoned}: the holder's bond says the weapon is out.
     * An {@link Verdict#ADOPT} means "make this the holder's weapon": the stack itself is removed and the real projection is built (binding the weapon first
     * when the holder has no bond).
     */
    public static Verdict verdict(boolean exempt, Owner owner, Bond bond, boolean summoned) {
        if (exempt) return Verdict.KEEP;
        switch (owner) {
            case OTHER:
                return Verdict.DISSOLVE;
            case SELF:
                return bond == Bond.SAME && summoned ? Verdict.KEEP : Verdict.DISSOLVE;
            default:
                return bond == Bond.OTHER ? Verdict.DISSOLVE : Verdict.ADOPT;
        }
    }
}
