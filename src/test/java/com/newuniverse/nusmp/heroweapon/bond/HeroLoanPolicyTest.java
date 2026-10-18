package com.newuniverse.nusmp.heroweapon.bond;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.newuniverse.nusmp.heroweapon.bond.HeroLoanPolicy.Bond;
import com.newuniverse.nusmp.heroweapon.bond.HeroLoanPolicy.Owner;
import com.newuniverse.nusmp.heroweapon.bond.HeroLoanPolicy.Verdict;
import org.junit.jupiter.api.Test;

/**
 * Why: this table decides what happens to every hero weapon stack found in an inventory, and the abusive rows are the point of the owner's "permanently bound to
 * the user's body": a projection moved to another player dissolves, a projection kept after the weapon was put away dissolves, a bare stack is adopted only by a
 * holder who has the weapon or is free to take it. Every combination is listed, so adding a column later cannot silently change a row.
 */
class HeroLoanPolicyTest {
    private static Verdict v(boolean exempt, Owner o, Bond b, boolean summoned) { return HeroLoanPolicy.verdict(exempt, o, b, summoned); }

    @Test
    void creativeAndSpectatorPlayersKeepEverything() {
        for (Owner o : Owner.values())
            for (Bond b : Bond.values())
                for (boolean s : new boolean[]{false, true}) assertEquals(Verdict.KEEP, v(true, o, b, s), "exempt " + o + " " + b + " " + s);
    }

    @Test
    void someoneElsesProjectionAlwaysDissolves() {
        for (Bond b : Bond.values())
            for (boolean s : new boolean[]{false, true}) assertEquals(Verdict.DISSOLVE, v(false, Owner.OTHER, b, s), "other " + b + " " + s);
    }

    @Test
    void myProjectionIsKeptOnlyWhileMyBondSaysTheWeaponIsOut() {
        assertEquals(Verdict.KEEP, v(false, Owner.SELF, Bond.SAME, true));
        assertEquals(Verdict.DISSOLVE, v(false, Owner.SELF, Bond.SAME, false), "put away: a stray projection goes");
        assertEquals(Verdict.DISSOLVE, v(false, Owner.SELF, Bond.NONE, true), "the bond ended");
        assertEquals(Verdict.DISSOLVE, v(false, Owner.SELF, Bond.NONE, false));
        assertEquals(Verdict.DISSOLVE, v(false, Owner.SELF, Bond.OTHER, true), "my bond is another weapon now");
        assertEquals(Verdict.DISSOLVE, v(false, Owner.SELF, Bond.OTHER, false));
    }

    @Test
    void aBareStackIsAdoptedByAHolderWhoHasItOrIsFree() {
        for (boolean s : new boolean[]{false, true}) {
            assertEquals(Verdict.ADOPT, v(false, Owner.NONE, Bond.SAME, s));
            assertEquals(Verdict.ADOPT, v(false, Owner.NONE, Bond.NONE, s));
            assertEquals(Verdict.DISSOLVE, v(false, Owner.NONE, Bond.OTHER, s), "a Sword in the Shield hero's pocket is not adopted");
        }
    }
}
