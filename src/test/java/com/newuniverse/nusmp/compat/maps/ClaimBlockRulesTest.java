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
        for (String n : DimensionAudit.NAMES) assertTrue(ClaimBlockRules.blocked("nusmp:" + n, ClaimBlockRules.DEFAULT_PATTERNS), n);
        assertTrue(ClaimBlockRules.blocked("nusmp:some_future_dimension", ClaimBlockRules.DEFAULT_PATTERNS));
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
        assertTrue(ClaimBlockRules.blocked("nusmp:arc_arena", "nusmp:*arena"));
        assertTrue(ClaimBlockRules.blocked("nusmp:zogratis_arena", "nusmp:*arena"));
        assertFalse(ClaimBlockRules.blocked("nusmp:dream", "nusmp:*arena"));
        assertTrue(ClaimBlockRules.blocked("anything:at_all", "*"));
        assertTrue(ClaimBlockRules.blocked("a:b", "x:y, a:* ,z:q"));
    }

    @Test
    void emptyOrBadListsBlockNothingAndNeverThrow() {
        assertFalse(ClaimBlockRules.blocked("nusmp:mega_dungeon", ""));
        assertFalse(ClaimBlockRules.blocked("nusmp:mega_dungeon", null));
        assertFalse(ClaimBlockRules.blocked("nusmp:mega_dungeon", " , ,, "));
        assertFalse(ClaimBlockRules.blocked("nusmp:mega_dungeon", "nusmp:(mega"), "an entry with regex characters is skipped, not compiled");
        assertFalse(ClaimBlockRules.blocked(null, "nusmp:*"));
        assertFalse(ClaimBlockRules.blocked("", "nusmp:*"));
        assertEquals(1, ClaimBlockRules.parse("nusmp:*, (bad").size());
    }

    @Test
    void caseDoesNotMatter() {
        assertTrue(ClaimBlockRules.blocked("NUSMP:Mega_Dungeon", "nusmp:*"));
        assertTrue(ClaimBlockRules.blocked("nusmp:mega_dungeon", " NUSMP:* "));
    }
}
