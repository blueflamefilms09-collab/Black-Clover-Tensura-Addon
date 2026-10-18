package com.newuniverse.nusmp.heroweapon.weapon.sword;

/**
 * Why: the Sword remembers a few things about its wielder between one call and the next: the run of blows (the {@link HitLedger}), the foes marked by Gold
 * Rebellion, when the carapace last turned a blow aside, when Plunder last took, the floating swords that circle the wielder, and the sword's mark on the last
 * target of the fan. They are gathered in one plain object per wielder (no Minecraft types, so the parts that have rules are tested alone) which the runtime keeps in a
 * map and drops when the wielder leaves or the server stops. Everything is bounded.
 */
public final class SwordState {
    public final HitLedger ledger = new HitLedger();
    public final MarkBook marks = new MarkBook();
    /** Game time before which the carapace may not turn another blow aside. */
    public long carapaceReadyAt;
    /** Game time before which Plunder may not take again. */
    public long plunderReadyAt;
    /** Whether the Float Sword's blades circle the wielder now, and until when (a second cast replaces the first). */
    public long floatUntil;
    /** A counter that numbers the casts of a wielder, so a delayed part of an old cast can tell it has been replaced. */
    public int castNumber;
    /** The last Tempo level shown to the wielder, so a new level is told once. */
    public int shownTempo;

    public void reset() {
        ledger.reset();
        marks.clear();
        carapaceReadyAt = 0;
        plunderReadyAt = 0;
        floatUntil = 0;
        shownTempo = 0;
    }
}
