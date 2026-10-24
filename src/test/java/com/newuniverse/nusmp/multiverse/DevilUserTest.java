package com.newuniverse.nusmp.multiverse;

import com.newuniverse.nusmp.blackclover.GrimoireCover;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the Great Spirit menu shows the Slime Crest skin only for devil users; the server decides, from the rules the owner gave. */
class DevilUserTest {
    private static boolean is(GrimoireCover c, boolean bound, long union, int anti, Race r, Race fam) { return DevilUser.is(c, bound, union, anti, r, fam); }

    @Test
    void anOrdinaryPlayerIsNotADevilUser() {
        assertFalse(is(null, false, 0, 0, Race.HUMAN, Race.HUMAN));
        assertFalse(is(GrimoireCover.THREE_LEAF, false, 0, 0, Race.HUMAN, null));
        assertFalse(is(GrimoireCover.FOUR_LEAF, false, 0, 2, Race.ELF, Race.ELF));
        assertFalse(is(GrimoireCover.GOD_TIER, false, 0, 0, Race.HUMAN, Race.HUMAN), "the god-tier cover is tier 5 but not forbidden");
    }

    @Test
    void aForbiddenOrDevilBoundGrimoireMakesADevilUser() {
        for (GrimoireCover c : GrimoireCover.values()) assertTrue(is(c, false, 0, 0, Race.HUMAN, null) == c.isForbidden(), c.name());
        assertTrue(is(GrimoireCover.FIVE_LEAF, false, 0, 0, Race.HUMAN, null));
        assertTrue(is(GrimoireCover.BLACK_MAGIC, false, 0, 0, Race.HUMAN, null));
        assertTrue(is(GrimoireCover.THREE_LEAF, true, 0, 0, Race.HUMAN, null), "bound to a devil");
    }

    @Test
    void devilUnionActiveOrUsedMakesADevilUser() {
        assertTrue(is(GrimoireCover.THREE_LEAF, false, 12345L, 0, Race.HUMAN, null), "the grimoire remembers it was in Devil Union");
        assertTrue(is(null, false, 0, DevilUser.ANTI_DEVIL_UNION, Race.HUMAN, null), "Anti-Magic Devil Union mode");
        assertFalse(is(null, false, 0, 2, Race.HUMAN, null), "Black Form is not Devil Union");
    }

    @Test
    void aDevilOrDaemonRaceMakesADevilUser() {
        assertTrue(is(null, false, 0, 0, Race.DEVIL, null));
        assertTrue(is(null, false, 0, 0, Race.DAEMON, null));
        assertTrue(is(null, false, 0, 0, Race.HUMAN, Race.DAEMON));
        assertTrue(is(null, false, 0, 0, Race.HUMAN, Race.DEVIL));
        assertFalse(is(null, false, 0, 0, Race.VAMPIRE, Race.SLIME));
    }
}
