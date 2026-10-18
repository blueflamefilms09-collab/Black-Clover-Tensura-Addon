package com.newuniverse.nusmp.heroweapon.forms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Why: the gem must take what a form asks for, no more than is useful, and never be hammered into a lag machine; these are the counting rules that decide it.
 */
class AbsorbRulesTest {
    private static final Map<String, Set<String>> TAGS = Map.of("minecraft:leaves", Set.of("minecraft:oak_leaves", "minecraft:birch_leaves"));
    private static final FormTree.TagMatcher MATCH = (tag, item) -> TAGS.getOrDefault(tag, Set.of()).contains(item);

    private static HeroForm form(String id, int tier, List<String> parents, HeroForm.Requirement... req) {
        return new HeroForm(HeroKind.SHIELD, id, id, "", tier, parents, List.of(req), 100, Map.of(), Map.of(), Map.of(), List.of(), "base", false, null, 0);
    }

    private static final List<HeroForm> FORMS = List.of(HeroForm.base(HeroKind.SHIELD),
            form("leaf", 1, List.of("base"), new HeroForm.Requirement("", "minecraft:leaves", 32), new HeroForm.Requirement("minecraft:glow_berries", "", 8)),
            form("leafier", 2, List.of("leaf"), new HeroForm.Requirement("", "minecraft:leaves", 64)));

    private static Map<String, Integer> got(Object... kv) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], (Integer) kv[i + 1]);
        return m;
    }

    @Test
    void theGemAcceptsWhatSomeFormAsksForByIdOrTag() {
        assertTrue(AbsorbRules.accepts("minecraft:glow_berries", FORMS, MATCH));
        assertTrue(AbsorbRules.accepts("minecraft:oak_leaves", FORMS, MATCH), "through the tag");
        assertFalse(AbsorbRules.accepts("minecraft:cobblestone", FORMS, MATCH));
        assertFalse(AbsorbRules.accepts("minecraft:oak_leaves", FORMS, FormTree.NO_TAGS), "no tag knowledge, no match");
        assertFalse(AbsorbRules.accepts("", FORMS, MATCH));
    }

    @Test
    void neededIsTheLargestShortfallAmongTheFormsStillToLearn() {
        assertEquals(64, AbsorbRules.needed("minecraft:oak_leaves", FORMS, Set.of("base"), got(), MATCH), "leafier asks for 64 and is bigger than leaf's 32");
        assertEquals(44, AbsorbRules.needed("minecraft:oak_leaves", FORMS, Set.of("base"), got("minecraft:birch_leaves", 20), MATCH), "20 already count");
        assertEquals(32, AbsorbRules.needed("minecraft:oak_leaves", FORMS, Set.of("base", "leaf"), got("minecraft:birch_leaves", 32), MATCH), "leaf is done, leafier still wants 32 more");
        assertEquals(0, AbsorbRules.needed("minecraft:oak_leaves", FORMS, Set.of("base", "leaf", "leafier"), got(), MATCH), "nothing left to learn from it");
        assertEquals(0, AbsorbRules.needed("minecraft:oak_leaves", FORMS, Set.of("base"), got("minecraft:birch_leaves", 64), MATCH));
        assertEquals(8, AbsorbRules.needed("minecraft:glow_berries", FORMS, Set.of("base"), got(), MATCH));
        assertEquals(5, AbsorbRules.needed("minecraft:glow_berries", FORMS, Set.of("base"), got("minecraft:glow_berries", 3), MATCH));
        assertEquals(0, AbsorbRules.needed("minecraft:cobblestone", FORMS, Set.of("base"), got(), MATCH));
    }

    @Test
    void takeFollowsTheThreeSwitches() {
        assertEquals(8, AbsorbRules.take(64, 8, true, false, false), "only what is needed");
        assertEquals(5, AbsorbRules.take(5, 8, true, false, false), "never more than the stack");
        assertEquals(0, AbsorbRules.take(64, 0, true, false, false), "nothing is needed");
        assertEquals(64, AbsorbRules.take(64, 8, true, true, false), "shift-click takes the whole stack");
        assertEquals(64, AbsorbRules.take(64, 0, false, false, false), "the cap switched off");
        assertEquals(64, AbsorbRules.take(64, 0, true, false, true), "absorbAnything cannot tell what is needed");
        assertEquals(0, AbsorbRules.take(0, 8, true, true, true), "an empty stack gives nothing");
    }

    @Test
    void theLimiterAllowsAFixedNumberPerTwentyTicks() {
        AbsorbRules.Limiter lim = new AbsorbRules.Limiter(3);
        assertTrue(lim.tryAcquire(100));
        assertTrue(lim.tryAcquire(100));
        assertTrue(lim.tryAcquire(105));
        assertFalse(lim.tryAcquire(110), "the fourth in one window");
        assertFalse(lim.tryAcquire(119));
        assertTrue(lim.tryAcquire(120), "a new window");
        assertTrue(lim.tryAcquire(121));
        assertTrue(lim.tryAcquire(122));
        assertFalse(lim.tryAcquire(123));
    }

    @Test
    void theLimiterSurvivesAClockThatGoesBackAndAZeroLimit() {
        AbsorbRules.Limiter lim = new AbsorbRules.Limiter(1);
        assertTrue(lim.tryAcquire(5000));
        assertFalse(lim.tryAcquire(5001));
        assertTrue(lim.tryAcquire(10), "the world was reloaded: the clock is lower");
        AbsorbRules.Limiter zero = new AbsorbRules.Limiter(0);
        assertTrue(zero.tryAcquire(0), "a limit below one is raised to one, never a lock-out");
        assertTrue(new AbsorbRules.Limiter(5).tryAcquire(0), "the first call of a new limiter is always allowed");
    }
}
