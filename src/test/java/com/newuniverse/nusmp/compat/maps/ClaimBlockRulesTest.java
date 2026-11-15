package com.newuniverse.nusmp.compat.maps;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: which dimensions FTB Chunks claims are refused in is decided by a config list with wildcards, and the default must cover every dimension of the mod and nothing else (a claim in the
 * Overworld must stay possible). The reflection that hooks FTB Chunks cannot run without it, but this decision can be pinned exactly.
 */
class ClaimBlockRulesTest {
    @Test
    void theDefaultBlocksEveryDimensionOfTheMod() {
        for (String n : DimensionAudit.NAMES) assertTrue(ClaimBlockRules.blocked("anime_multiverse:" + n, ClaimBlockRules.DEFAULT_PATTERNS), n);
        assertTrue(ClaimBlockRules.blocked("anime_multiverse:some_future_dimension", ClaimBlockRules.DEFAULT_PATTERNS));
    }

    @Test
    void otherDimensionsStayClaimable() {
        for (String d : new String[]{"minecraft:overworld", "minecraft:the_nether", "minecraft:the_end", "tensura:hell", "othernusmp:x", "nusmpx:y", "xnusmp:mega"})
            assertFalse(ClaimBlockRules.blocked(d, ClaimBlockRules.DEFAULT_PATTERNS), d);
    }

    @Test
    void exactIdsAndWildcardsInTheMiddle() {
        assertTrue(ClaimBlockRules.blocked("minecraft:the_nether", "minecraft:the_nether"));
        assertFalse(ClaimBlockRules.blocked("minecraft:the_end", "minecraft:the_nether"));
        assertTrue(ClaimBlockRules.blocked("anime_multiverse:arc_arena", "anime_multiverse:*arena"));
        assertTrue(ClaimBlockRules.blocked("anime_multiverse:zogratis_arena", "anime_multiverse:*arena"));
        assertFalse(ClaimBlockRules.blocked("anime_multiverse:dream", "anime_multiverse:*arena"));
        assertTrue(ClaimBlockRules.blocked("anything:at_all", "*"));
        assertTrue(ClaimBlockRules.blocked("a:b", "x:y, a:* ,z:q"));
    }

    @Test
    void emptyOrBadListsBlockNothingAndNeverThrow() {
        assertFalse(ClaimBlockRules.blocked("anime_multiverse:mega_dungeon", ""));
        assertFalse(ClaimBlockRules.blocked("anime_multiverse:mega_dungeon", null));
        assertFalse(ClaimBlockRules.blocked("anime_multiverse:mega_dungeon", " , ,, "));
        assertFalse(ClaimBlockRules.blocked("anime_multiverse:mega_dungeon", "anime_multiverse:(mega"), "an entry with regex characters is skipped, not compiled");
        assertFalse(ClaimBlockRules.blocked(null, "anime_multiverse:*"));
        assertFalse(ClaimBlockRules.blocked("", "anime_multiverse:*"));
        assertEquals(1, ClaimBlockRules.parse("anime_multiverse:*, (bad").size());
    }

    @Test
    void caseDoesNotMatter() {
        assertTrue(ClaimBlockRules.blocked("NUSMP:Mega_Dungeon", "anime_multiverse:*"));
        assertTrue(ClaimBlockRules.blocked("anime_multiverse:mega_dungeon", " NUSMP:* "));
    }
}
