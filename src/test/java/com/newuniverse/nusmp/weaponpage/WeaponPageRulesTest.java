package com.newuniverse.nusmp.weaponpage;

import com.newuniverse.nusmp.weaponpage.WeaponPageData.Bar;
import com.newuniverse.nusmp.weaponpage.WeaponPageData.Entry;
import com.newuniverse.nusmp.weaponpage.WeaponPageData.Opt;
import com.newuniverse.nusmp.weaponpage.WeaponPageData.View;
import com.newuniverse.nusmp.weaponpage.WeaponPageRules.Verdict;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: everything the server refuses before it touches the game (the weapon, the action, the argument, the gaps) and the checksum that lets it skip a sync are pure, so a bad packet can be thrown at them without a
 * game: no weapon, a wrong action for a weapon, a form past the end, an injected or oversized id, a negative or huge number, a clock set back. Pure: no game types, no files.
 */
class WeaponPageRulesTest {
    @Test
    void anOpenCarriesNoWeaponAndAnOptionalChecksum() {
        assertEquals(Verdict.OK, WeaponPageRules.check("", WeaponPageRules.ACT_OPEN, ""));
        assertEquals(Verdict.OK, WeaponPageRules.check("", WeaponPageRules.ACT_OPEN, "-2147483648"));
        assertEquals(Verdict.OK, WeaponPageRules.check("", WeaponPageRules.ACT_OPEN, "123456"));
        assertEquals(Verdict.BAD_WEAPON, WeaponPageRules.check("hero", WeaponPageRules.ACT_OPEN, ""));
        assertEquals(Verdict.BAD_WEAPON, WeaponPageRules.check(null, WeaponPageRules.ACT_OPEN, ""));
        for (String bad : new String[] {"abc", "-", "1.5", " 1", "99999999999", "2147483648", "1e5"}) assertEquals(Verdict.BAD_ARG, WeaponPageRules.check("", WeaponPageRules.ACT_OPEN, bad), bad);
    }

    @Test
    void everyActionBelongsToItsWeapons() {
        for (String w : WeaponPageRules.WEAPONS) {
            boolean hero = w.equals("hero"), greed = w.equals("greed");
            assertEquals(hero ? Verdict.OK : Verdict.BAD_ACTION, WeaponPageRules.check(w, WeaponPageRules.ACT_LEARN, "base"), w + " learn");
            assertEquals(hero ? Verdict.OK : Verdict.BAD_ACTION, WeaponPageRules.check(w, WeaponPageRules.ACT_FEED, "minecraft:leather"), w + " feed");
            assertEquals(greed ? Verdict.OK : Verdict.BAD_ACTION, WeaponPageRules.check(w, WeaponPageRules.ACT_HALF, ""), w + " half");
            assertEquals(Verdict.OK, WeaponPageRules.check(w, WeaponPageRules.ACT_FORM, hero ? "base" : "0"), w + " form");
        }
        assertEquals(Verdict.BAD_ARG, WeaponPageRules.check("greed", WeaponPageRules.ACT_HALF, "1"));
    }

    @Test
    void numberedFormsStopAtTheEndOfTheWeapon() {
        for (String lord : new String[] {"sloth", "envy", "vanity", "holy"}) {
            assertEquals(Verdict.OK, WeaponPageRules.check(lord, WeaponPageRules.ACT_FORM, "2"));
            assertEquals(Verdict.BAD_ARG, WeaponPageRules.check(lord, WeaponPageRules.ACT_FORM, "3"));
        }
        assertEquals(Verdict.OK, WeaponPageRules.check("greed", WeaponPageRules.ACT_FORM, "7"));
        assertEquals(Verdict.BAD_ARG, WeaponPageRules.check("greed", WeaponPageRules.ACT_FORM, "8"));
        for (String bad : new String[] {"", "-1", "+1", "01x", "007", "1 ", "٣", "1.0"}) assertEquals(Verdict.BAD_ARG, WeaponPageRules.check("greed", WeaponPageRules.ACT_FORM, bad), "'" + bad + "'");
    }

    @Test
    void idsAreShapedLikeIdsAndNothingElse() {
        for (String good : new String[] {"base", "small_shield", "form2", "a"}) assertTrue(WeaponPageRules.isFormId(good), good);
        for (String bad : new String[] {"", "Base", "a b", "a:b", "../x", "a-b", "a".repeat(49), "é"}) assertFalse(WeaponPageRules.isFormId(bad), bad);
        for (String good : new String[] {"minecraft:leather", "anime_multiverse:heart_of_words", "tensura:a/b", "mod-x:a.b"}) assertTrue(WeaponPageRules.isItemId(good), good);
        for (String bad : new String[] {"", "leather", ":leather", "minecraft:", "a:b:c", "Minecraft:leather", "a:b c", "a/b:c", "../x", "a:" + "x".repeat(100)}) assertFalse(WeaponPageRules.isItemId(bad), bad);
        assertEquals(Verdict.BAD_WEAPON, WeaponPageRules.check("nobody", WeaponPageRules.ACT_FORM, "0"));
        assertEquals(Verdict.BAD_WEAPON, WeaponPageRules.check("", WeaponPageRules.ACT_FORM, "0"));
        assertEquals(Verdict.BAD_ACTION, WeaponPageRules.check("hero", 99, ""));
        assertEquals(Verdict.BAD_ACTION, WeaponPageRules.check("hero", -1, ""));
        assertEquals(Verdict.BAD_ARG, WeaponPageRules.check("hero", WeaponPageRules.ACT_FORM, null));
        assertEquals(Verdict.BAD_ARG, WeaponPageRules.check("hero", WeaponPageRules.ACT_FORM, "a".repeat(WeaponPageRules.ARG + 1)));
    }

    @Test
    void theGapsHoldAndANegativeClockNeverLocksForEver() {
        assertTrue(WeaponPageRules.ready(100, -1, 10), "never asked");
        assertFalse(WeaponPageRules.ready(105, 100, WeaponPageRules.GAP_ACT_TICKS));
        assertTrue(WeaponPageRules.ready(110, 100, WeaponPageRules.GAP_ACT_TICKS));
        assertFalse(WeaponPageRules.ready(110, 100, WeaponPageRules.GAP_OPEN_TICKS));
        assertTrue(WeaponPageRules.ready(120, 100, WeaponPageRules.GAP_OPEN_TICKS));
        assertTrue(WeaponPageRules.ready(5, 100000, WeaponPageRules.GAP_OPEN_TICKS), "the world clock was set back");
        assertTrue(WeaponPageRules.gap(WeaponPageRules.ACT_OPEN) > WeaponPageRules.gap(WeaponPageRules.ACT_FORM), "an open builds the whole page, so it waits longer");
    }

    @Test
    void tierProgressFollowsTheSwordsThresholds() {
        double[] t = {0, 2_500, 10_000, 35_000, 100_000, 300_000, 750_000, 2_000_000};
        assertEquals(0, WeaponPageRules.tierPct(0, t));
        assertEquals(0, WeaponPageRules.tierPct(-5, t));
        assertEquals(50, WeaponPageRules.tierPct(1_250, t));
        assertEquals(0, WeaponPageRules.tierPct(2_500, t));
        assertEquals(99, WeaponPageRules.tierPct(9_999, t));
        assertEquals(100, WeaponPageRules.tierPct(2_000_000, t));
        assertEquals(100, WeaponPageRules.tierPct(1e12, t));
        assertEquals(0, WeaponPageRules.tierPct(Double.NaN, t));
    }

    private static View view(int level, int pct, int held) {
        Entry e = new Entry("hero", "Vassal Sword", "God Grade #1", level, 100, pct, "Base", List.of(new Bar("anime_multiverse.weaponpage.bar.level", "", level, 100, pct)),
                List.of(new Opt(WeaponPageRules.ACT_FORM, "base", "Base", WeaponPageRules.ST_CURRENT, ""), new Opt(WeaponPageRules.ACT_LEARN, "big", "Big", WeaponPageRules.ST_READY, "learn")));
        return new View(held, List.of(e));
    }

    @Test
    void theChecksumFollowsEverythingThePageShows() {
        View base = view(5, 40, 0);
        assertEquals(base.sum(), view(5, 40, 0).sum(), "equal views, equal sums");
        assertNotEquals(base.sum(), view(6, 40, 0).sum());
        assertNotEquals(base.sum(), view(5, 41, 0).sum());
        assertNotEquals(base.sum(), view(5, 40, -1).sum());
        Entry e = base.entries().get(0);
        List<Opt> swapped = new ArrayList<>(e.opts());
        swapped.set(1, new Opt(WeaponPageRules.ACT_LEARN, "big", "Big", WeaponPageRules.ST_LOCKED, "learn"));
        assertNotEquals(base.sum(), new View(0, List.of(new Entry(e.id(), e.name(), e.grade(), e.level(), e.max(), e.pct(), e.form(), e.bars(), swapped))).sum());
        assertNotEquals(View.EMPTY.sum(), base.sum());
    }

    @Test
    void aViewMustObeyTheBoundsOrThePacketIsRefused() {
        assertTrue(view(5, 40, 0).bounded());
        assertTrue(View.EMPTY.bounded());
        assertFalse(new View(1, view(5, 40, 0).entries()).bounded(), "held past the list");
        assertFalse(new View(-2, view(5, 40, 0).entries()).bounded());
        Entry e = view(5, 40, 0).entries().get(0);
        List<Opt> many = new ArrayList<>();
        for (int i = 0; i <= WeaponPageRules.MAX_OPTIONS; i++) many.add(new Opt(WeaponPageRules.ACT_FORM, "f" + i, "F", WeaponPageRules.ST_READY, ""));
        assertFalse(new View(0, List.of(new Entry(e.id(), e.name(), e.grade(), e.level(), e.max(), e.pct(), e.form(), e.bars(), many))).bounded(), "too many options");
        assertFalse(new View(0, List.of(new Entry("nobody", e.name(), e.grade(), e.level(), e.max(), e.pct(), e.form(), e.bars(), e.opts()))).bounded(), "unknown weapon id");
        assertFalse(new View(0, List.of(new Entry(e.id(), "x".repeat(WeaponPageRules.TEXT + 1), e.grade(), e.level(), e.max(), e.pct(), e.form(), e.bars(), e.opts()))).bounded(), "a long name");
        List<Opt> badState = List.of(new Opt(WeaponPageRules.ACT_FORM, "a", "A", 7, ""));
        assertFalse(new View(0, List.of(new Entry(e.id(), e.name(), e.grade(), e.level(), e.max(), e.pct(), e.form(), e.bars(), badState))).bounded(), "a state that does not exist");
    }

    @Test
    void theServerOnlyDoesWhatThePageOffers() {
        View v = view(5, 40, 0);
        assertEquals("Base", v.option("hero", WeaponPageRules.ACT_FORM, "base").label());
        assertNull(v.option("hero", WeaponPageRules.ACT_FORM, "nothing"));
        assertNull(v.option("hero", WeaponPageRules.ACT_LEARN, "base"), "same id, other action");
        assertNull(v.option("greed", WeaponPageRules.ACT_FORM, "0"), "a weapon the player does not own");
        assertEquals(WeaponPageRules.ST_READY, v.option("hero", WeaponPageRules.ACT_LEARN, "big").state());
    }
}
