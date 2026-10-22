package com.newuniverse.nusmp.shishou;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The 2.1.0 update of Shishou, pure and source level: the fast hunter (scan bounds, charge curve, strike cooldown), the stronger start and steeper climb (the before / after numbers, the migration of 2.0.0
 * saves), the Tensura Ultimate skill that replaced the three keys (the routing of the modes, the grant and revoke rule), feeding from the inventory (what is food, the cap per click, the owner rule), the
 * Akashic GUI geometry and the source rules that go with them (no key registration left, the hunt's scan is the one bounded box query, every text has its lang entry).
 */
class ShishouUpdate21Test {
    private static final Path MAIN = Path.of("src/main/java/com/newuniverse/nusmp");

    private static String read(String rel) throws IOException { return Files.readString(MAIN.resolve(rel)); }

    private static String code(String s) { return s.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", ""); }

    // ================================================================ A. the hunt
    @Test
    void theScanIsBoundedInRateAndInWork() {
        for (int gap = ShishouHunt.SCAN_TICKS_MIN; gap <= ShishouHunt.SCAN_TICKS_MAX; gap++)
            for (int id = 0; id < 6; id++) assertTrue(ShishouHunt.scanGap(gap, id) >= ShishouHunt.SCAN_TICKS_MIN && ShishouHunt.scanGap(gap, id) <= ShishouHunt.SCAN_TICKS_MAX + 2);
        assertEquals(6, ShishouHunt.SCAN_TICKS, "the default looks every six ticks (2.0.0: about ten, and it only saw four blocks up and down)");
        assertTrue(ShishouHunt.scansPerSecond(ShishouHunt.scanGap(ShishouHunt.SCAN_TICKS_MIN, 0)) <= 10.0, "at most ten looks a second even at the fastest setting");
        assertTrue(ShishouHunt.examinedPerSecond(ShishouHunt.SCAN_TICKS, ShishouHunt.MAX_EXAMINED) <= 220, "at most about two hundred creatures examined a second by default");
        assertEquals(ShishouHunt.RADIUS_MAX, ShishouHunt.scanRadius(false, 5000, 16), 1e-9, "never above the maximum radius");
        assertEquals(ShishouHunt.RADIUS_MIN, ShishouHunt.scanRadius(false, 1, 16), 1e-9, "never below the minimum radius");
        assertEquals(ShishouHunt.WILD_RADIUS, ShishouHunt.scanRadius(false, ShishouHunt.WILD_RADIUS, 16), 1e-9);
        assertEquals(24 + ShishouHunt.LEASH_SLACK, ShishouHunt.scanRadius(true, 99, 24), 1e-9, "a thrown sword looks as far as its leash plus the slack");
        assertTrue(ShishouHunt.WILD_RADIUS > ShishouRules.TARGET_RANGE, "the wild radius is larger than the 2.0.0 range");
        assertTrue(ShishouHunt.scanHeight(1000) <= ShishouHunt.VERTICAL_MAX, "the cube's height is capped");
        assertTrue(ShishouHunt.scanHeight(40) > 4, "he now sees far above and below (2.0.0: four blocks)");
    }

    @Test
    void thePreferredTargetIsTheNearestThreat() {
        assertTrue(ShishouHunt.score(10, 20, false) < ShishouHunt.score(14, 20, false), "nearer first");
        assertTrue(ShishouHunt.score(14, 200, false) < ShishouHunt.score(10, 20, false) + 8.0 + 1e-9, "a big one is worth up to eight blocks of distance");
        assertTrue(ShishouHunt.score(20, 20, true) < ShishouHunt.score(10, 20, false), "one that is hurting a player comes before a quiet one nearer");
        assertTrue(ShishouHunt.score(0, 1e9, true) <= 0.0 && ShishouHunt.score(-5, -5, false) >= 0.0, "no NaN, no overflow");
    }

    @Test
    void theChargeAcceleratesQuicklyAndSlowsOnArrival() {
        for (int level : new int[]{1, 10, 50, 100}) {
            double top = ShishouHunt.topSpeed(level, 100);
            assertTrue(top >= 1.05 && top <= ShishouHunt.CHARGE_HARD_CAP, "top speed of level " + level + ": " + top);
            assertEquals(top, ShishouHunt.chargeSpeed(10, top), 1e-9, "full speed within ten ticks");
            assertTrue(ShishouHunt.chargeSpeed(0, top) < top, "it starts slower");
            double prev = 0;
            for (int t = 0; t < 12; t++) {
                double v = ShishouHunt.chargeSpeed(t, top);
                assertTrue(v >= prev - 1e-12, "monotone");
                prev = v;
            }
        }
        assertTrue(ShishouHunt.topSpeed(10, 100) * 20 > 20, "supersonic feel: more than 20 blocks a second");
        assertTrue(ShishouHunt.topSpeed(100, 300) <= ShishouHunt.CHARGE_HARD_CAP, "even the config's 300 percent is capped, so a strike is never tunnelled through");
        assertEquals(1.0, ShishouHunt.arrive(1.0, 10), 1e-9);
        assertTrue(ShishouHunt.arrive(1.0, 0.5) < 0.6 && ShishouHunt.arrive(1.0, 0.0) >= ShishouHunt.ARRIVE_MIN - 1e-9, "slows near the target but keeps going");
        double v = 0.0;
        for (int i = 0; i < 10; i++) v = ShishouHunt.blend(v, 1.0);
        assertTrue(v > 0.98 && v <= 1.0, "the velocity follows the wish within about ten ticks: " + v);
    }

    @Test
    void theStrikeCooldownShrinksWithTheLevelAndABlockedChargeFreesItself() {
        assertTrue(ShishouHunt.strikeCooldown(10) < ShishouRules.STRIKE_COOLDOWN, "faster than 2.0.0 at the start level");
        assertTrue(ShishouHunt.strikeCooldown(100) <= ShishouHunt.strikeCooldown(10) && ShishouHunt.strikeCooldown(100) >= 6, "level scaled, never below six ticks");
        for (int l = 2; l <= 100; l++) assertTrue(ShishouHunt.strikeCooldown(l) <= ShishouHunt.strikeCooldown(l - 1));
        assertEquals(0, ShishouHunt.unstick(0));
        assertEquals(0, ShishouHunt.unstick(ShishouHunt.STUCK_NUDGE - 1));
        assertEquals(1, ShishouHunt.unstick(ShishouHunt.STUCK_NUDGE), "slides up and aside first");
        assertEquals(2, ShishouHunt.unstick(ShishouHunt.STUCK_GIVE_UP), "then gives the prey up for a while");
        assertFalse(ShishouHunt.progressed(0.0));
        assertTrue(ShishouHunt.progressed(0.5));
        assertEquals(0.0, ShishouHunt.patrolLeg(5), "no patrol toward a player who is near");
        assertEquals(0.0, ShishouHunt.patrolLeg(500), "none toward one who is far away");
        assertTrue(ShishouHunt.patrolLeg(100) > 0, "a patrol flies toward a player in the band");
    }

    @Test
    void theDormantRuleIsLevelScaledSoAFastHunterDoesNotRestInMinutes() {
        assertEquals(60 + 2 * 10, ShishouRules.dormantKillLimit(ShishouRules.DORMANT_KILLS, 10));
        assertTrue(ShishouRules.dormantKillLimit(ShishouRules.DORMANT_KILLS, 100) > ShishouRules.dormantKillLimit(ShishouRules.DORMANT_KILLS, 10));
        assertTrue(ShishouRules.dormantKillLimit(ShishouRules.DORMANT_KILLS, 10) > 3 * ShishouRules.LEGACY_DORMANT_KILLS, "much more than 2.0.0's 25");
        assertTrue(ShishouRules.WILD_MANA / ShishouRules.WILD_STRIKE_MANA >= 4 * (ShishouRules.LEGACY_WILD_MANA / ShishouRules.LEGACY_WILD_STRIKE_MANA) - 1e-9, "the wild budget allows far more strikes");
        assertTrue(ShishouRules.shouldGoDormant(80, 0L, 400.0, ShishouRules.dormantKillLimit(60, 10), 7));
        assertFalse(ShishouRules.shouldGoDormant(40, 0L, 400.0, ShishouRules.dormantKillLimit(60, 10), 7));
    }

    // ================================================================ C. strength and the migration
    @Test
    void theStartIsALowMagisteelSwordAndTheClimbPassesGenesisGrade() {
        assertEquals(8.5, ShishouRules.heldAttack(ShishouRules.START_LEVEL), 1e-9, "the start: the assumed low magisteel sword (item damage 8.5)");
        assertTrue(ShishouRules.heldAttack(ShishouRules.START_LEVEL) >= ShishouRules.legacyHeldAttack(ShishouRules.START_LEVEL), "a 2.0.0 level-10 sword never gets weaker in the hand");
        assertEquals(ShishouRules.heldAttack(1), ShishouRules.heldAttack(ShishouRules.START_LEVEL), 1e-9, "below the start level it is the same");
        double prev = 0;
        for (int l = 1; l <= 100; l++) {
            double a = ShishouRules.heldAttack(l);
            assertTrue(a >= prev - 1e-9, "monotone at " + l);
            prev = a;
            assertTrue(a <= ShishouRules.START_ATTACK + ShishouRules.ATTACK_BONUS_CAP + 1e-9, "capped at " + l);
        }
        assertTrue(ShishouRules.heldAttack(50) > ShishouRules.legacyHeldAttack(50) * 1.8, "level 50 hits much harder than 2.0.0");
        int pass = 0;
        for (int l = 1; l <= 100; l++) if (ShishouRules.heldAttack(l) > 40.0) { pass = l; break; }
        assertTrue(pass > 0 && pass >= 45 && pass <= 65, "Genesis-grade weapon damage (the 40 point tier cap of docs/relic_tiers_and_scaling.md) is passed in the high levels: level " + pass);
        assertEquals(78.5, ShishouRules.heldAttack(100), 1e-9);
        assertTrue(ShishouRules.heldAttack(60, 200) > ShishouRules.heldAttack(60, 100) && ShishouRules.heldAttack(60, 0) == ShishouRules.START_ATTACK, "the server's scale only moves the growth, never the start");
        // the strike follows the same curve
        for (int l : new int[]{10, 30, 50, 100}) assertEquals(Math.min(ShishouRules.STRIKE_CAP, ShishouRules.heldAttack(l)), ShishouRules.strikeDamage(l, 0, 0, ShishouRules.STRIKE_CAP), 1e-9);
        for (int l = 24; l <= 100; l++) assertTrue(ShishouRules.strikeDamage(l, 0, 0, 1000) >= ShishouRules.legacyStrikeDamage(l, 0, 0), "from level 24 on the strike is above 2.0.0 at " + l);
        for (int l = 10; l < 24; l++) assertTrue(ShishouRules.strikeDamage(l, 0, 0, 1000) >= ShishouRules.legacyStrikeDamage(l, 0, 0) - 2.6, "the start is lower than 2.0.0 by at most 2.6 at " + l + " (the owner's low magisteel start)");
    }

    @Test
    void manaExperienceAndKillsAreRaisedAndStillBounded() {
        assertTrue(ShishouRules.maxMana(10, 0) > 3 * ShishouRules.LEGACY_MANA_BASE);
        assertEquals(1850.0, ShishouRules.maxMana(100, 20), 1e-9);
        assertTrue(ShishouRules.regenPerTick(10) * 20 >= 3.5 && ShishouRules.regenPerTick(100) * 20 <= 13.5);
        assertTrue(ShishouPredation.killExp(20, false) > 3 + 0.8 * 20, "more experience per kill");
        assertTrue(ShishouPredation.killExp(1e9, true) <= ShishouPredation.MAX_EXP_PER_KILL);
        assertEquals(8, ShishouPredation.stoneBase("tensura:magic_stone"));
        assertEquals(90, ShishouPredation.stoneBase("tensura:high_quality_magic_crystal"));
        assertTrue(ShishouLevels.capTotal() < 100_000L && ShishouLevels.capTotal() > 50_000L, "the cap is still a long climb: " + ShishouLevels.capTotal());
        assertEquals(100, ShishouLevels.MAX_LEVEL, "the cap stays 100");
        assertTrue(ShishouRules.heldAttack(30) > 20, "the table of the docs: level 30");
    }

    @Test
    void anOldSaveIsRaisedAndNeverLowered() {
        for (int oldLevel : new int[]{1, 5, 9, 10, 20, 50, 99, 100}) {
            Map<String, Long> legacy = new HashMap<>();
            long exp = ShishouLevels.legacyTotalFor(oldLevel);
            legacy.put("exp", exp);
            legacy.put("mana", Double.doubleToLongBits(30.0));
            legacy.put("learned", 1L << ShishouSkill.FIRE_MAGIC.bit());
            legacy.put("kills", 17L);
            legacy.put("wkills", 9L);
            legacy.put("wmana", Double.doubleToLongBits(40.0));
            legacy.put("msteps", 3L);
            ShishouRecord r = ShishouRecord.fromMap(legacy);
            assertTrue(r.level() >= Math.max(ShishouRules.START_LEVEL, oldLevel), "a 2.0.0 level " + oldLevel + " save loads as level " + r.level());
            assertTrue(r.totalExp() >= exp || oldLevel == 100 || r.totalExp() == ShishouLevels.capTotal(), "experience is never lowered");
            assertEquals(17L, r.kills());
            assertEquals(9, r.wildKills());
            assertTrue(r.has(ShishouSkill.FIRE_MAGIC), "the eaten skills stay");
            assertEquals(3, r.manaSteps());
            assertEquals(40.0 * ShishouRules.WILD_MANA / ShishouRules.LEGACY_WILD_MANA, r.wildMana(), 1e-9, "the wild budget keeps its share");
            assertTrue(r.mana(0L) >= 30.0, "the mana is never lowered: " + r.mana(0L));
            assertEquals(ShishouLevels.CURVE_VERSION, r.toMap().get("ver").intValue(), "the new version is written, so the migration runs once");
            ShishouRecord again = ShishouRecord.fromMap(r.toMap());
            assertEquals(r.totalExp(), again.totalExp(), "a second load does not migrate again");
            assertEquals(r.wildMana(), again.wildMana(), 1e-9);
        }
        ShishouRecord fresh = new ShishouRecord();
        assertEquals(ShishouRules.START_LEVEL, fresh.level());
        assertFalse(fresh.raiseTo(ShishouRules.START_LEVEL, 0L), "raiseTo never lowers and does nothing at the floor");
        assertTrue(fresh.raiseTo(30, 0L) && fresh.level() == 30);
        assertFalse(fresh.raiseTo(20, 0L), "never lowers");
        assertEquals(30, fresh.level());
    }

    // ================================================================ B. the Tensura skill: modes and grant
    @Test
    void everyCastableAbilityIsReachableThroughExactlyOneMode() {
        List<ShishouSkill> routed = ShishouModes.routed();
        assertEquals(new HashSet<>(routed).size(), routed.size(), "an ability is in one mode only");
        for (ShishouSkill s : ShishouSkill.values()) {
            if (s.castable()) assertTrue(routed.contains(s), s + " is cast, so a mode must hold it");
            if (s == ShishouSkill.TELEKINESIS) assertEquals(ShishouModes.Mode.TELEKINESIS, ShishouModes.modeOf(s));
            else if (!s.castable()) assertNull(ShishouModes.modeOf(s), s + " is passive: no mode");
        }
        assertEquals(7, ShishouModes.COUNT, "a bounded number of modes");
        assertEquals(ShishouModes.Mode.MAGIC, ShishouModes.modeOf(ShishouSkill.HEALING_MAGIC));
        for (ShishouModes.Mode m : ShishouModes.Mode.values()) assertEquals("shishou.mode." + m.id(), m.langKey());
        assertNull(ShishouModes.byIndex(-1));
        assertNull(ShishouModes.byIndex(ShishouModes.COUNT));
        assertEquals(0, ShishouModes.next(ShishouModes.COUNT - 1, false));
        assertEquals(ShishouModes.COUNT - 1, ShishouModes.next(0, true));
    }

    @Test
    void theKeyPressRoutesByModeAndSneaking() {
        assertEquals(ShishouModes.Action.THROW_RECALL, ShishouModes.action(ShishouModes.Mode.TELEKINESIS, false, 1));
        assertEquals(ShishouModes.Action.THROW_RECALL, ShishouModes.action(ShishouModes.Mode.TELEKINESIS, true, 1));
        assertEquals(ShishouModes.Action.STATUS, ShishouModes.action(ShishouModes.Mode.COUNSEL, false, 0));
        assertEquals(ShishouModes.Action.CAST, ShishouModes.action(ShishouModes.Mode.STORAGE, false, 1));
        assertEquals(ShishouModes.Action.OPEN_STORAGE, ShishouModes.action(ShishouModes.Mode.STORAGE, true, 1), "sneak opens Stomach like the old shift + click");
        assertEquals(ShishouModes.Action.CAST, ShishouModes.action(ShishouModes.Mode.DEFENCE, false, 3));
        assertEquals(ShishouModes.Action.CYCLE, ShishouModes.action(ShishouModes.Mode.DEFENCE, true, 3), "sneak picks the next ability of a mode");
        assertEquals(ShishouModes.Action.CAST, ShishouModes.action(ShishouModes.Mode.DEFENCE, true, 1), "nothing to cycle with one ability: it casts");
    }

    @Test
    void theModePicksFollowTheLevelAndTheAbsorbedSkills() {
        assertTrue(ShishouModes.available(ShishouModes.Mode.OFFENCE, 10, 0L).isEmpty(), "tier three is closed at level 10");
        assertEquals(List.of(ShishouSkill.BLACK_LIGHTNING, ShishouSkill.DIVINE_SWORD_RELEASE, ShishouSkill.DOPPELGANGER), ShishouModes.available(ShishouModes.Mode.OFFENCE, 60, 0L));
        assertEquals(List.of(ShishouSkill.MULTILAYER_BARRIER), ShishouModes.available(ShishouModes.Mode.DEFENCE, 10, 0L), "Multilayer Barrier opens at level 10");
        long fire = 1L << ShishouSkill.FIRE_MAGIC.bit(), heal = 1L << ShishouSkill.HEALING_MAGIC.bit();
        assertEquals(List.of(ShishouSkill.FIRE_MAGIC, ShishouSkill.HEALING_MAGIC), ShishouModes.available(ShishouModes.Mode.MAGIC, 10, fire | heal));
        assertEquals(ShishouSkill.FIRE_MAGIC, ShishouModes.current(ShishouModes.Mode.MAGIC, -1, 10, fire | heal));
        assertEquals(ShishouSkill.HEALING_MAGIC, ShishouModes.current(ShishouModes.Mode.MAGIC, ShishouSkill.HEALING_MAGIC.bit(), 10, fire | heal));
        assertEquals(ShishouSkill.FIRE_MAGIC, ShishouModes.current(ShishouModes.Mode.MAGIC, ShishouSkill.WIND_MAGIC.bit(), 10, fire | heal), "a remembered pick that is not open any more falls back");
        assertEquals(ShishouSkill.HEALING_MAGIC, ShishouModes.cycle(ShishouModes.Mode.MAGIC, ShishouSkill.FIRE_MAGIC, 10, fire | heal));
        assertEquals(ShishouSkill.FIRE_MAGIC, ShishouModes.cycle(ShishouModes.Mode.MAGIC, ShishouSkill.HEALING_MAGIC, 10, fire | heal), "the cycle wraps");
        assertNull(ShishouModes.cycle(ShishouModes.Mode.MAGIC, null, 10, 0L));
        assertNull(ShishouModes.current(ShishouModes.Mode.COUNSEL, -1, 100, -1L));
    }

    @Test
    void theSkillBelongsToTheWielderOnly() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        assertTrue(ShishouGrant.shouldHave(true, true, a, a));
        assertFalse(ShishouGrant.shouldHave(true, true, a, b), "a stranger never has it");
        assertFalse(ShishouGrant.shouldHave(true, true, null, a), "no wielder: nobody has it");
        assertFalse(ShishouGrant.shouldHave(false, true, a, a), "Shishou switched off");
        assertFalse(ShishouGrant.shouldHave(true, false, a, a), "the grant switched off");
        assertEquals(ShishouGrant.Verdict.GRANT, ShishouGrant.verdict(true, false));
        assertEquals(ShishouGrant.Verdict.REVOKE, ShishouGrant.verdict(false, true));
        assertEquals(ShishouGrant.Verdict.KEEP, ShishouGrant.verdict(true, true));
        assertEquals(ShishouGrant.Verdict.KEEP, ShishouGrant.verdict(false, false));
        int due = 0;
        for (long t = 0; t < ShishouGrant.CHECK_PERIOD; t++) if (ShishouGrant.due(t, 37)) due++;
        assertEquals(1, due, "one check per period per player");
    }

    // ================================================================ D. feeding
    private static ShishouFeed.Item item(String id, int n, int rarity) { return new ShishouFeed.Item(id, n, rarity, false, false, false, false, false); }

    @Test
    void whatIsFoodIsPureAndBounded() {
        assertEquals(ShishouFeed.Kind.STONE, ShishouFeed.classify(item("tensura:magic_stone", 5, 0)));
        assertEquals(ShishouFeed.Kind.STONE, ShishouFeed.classify(item("tensura:high_quality_magic_crystal", 1, 1)));
        assertEquals(ShishouFeed.Kind.DROP, ShishouFeed.classify(item("minecraft:rotten_flesh", 5, 0)));
        assertEquals(ShishouFeed.Kind.DROP, ShishouFeed.classify(item("minecraft:blaze_rod", 5, 0)));
        assertEquals(ShishouFeed.Kind.DROP, ShishouFeed.classify(item("tensura:ogre_horn", 1, 0)), "another mod's horn, core, fang ... by its name");
        assertEquals(ShishouFeed.Kind.NONE, ShishouFeed.classify(item("minecraft:diamond", 5, 0)), "a gem is not food");
        assertEquals(ShishouFeed.Kind.NONE, ShishouFeed.classify(item("minecraft:iron_sword", 1, 0)));
        assertEquals(ShishouFeed.Kind.NONE, ShishouFeed.classify(item("minecraft:stone", 64, 0)));
        assertEquals(ShishouFeed.Kind.NONE, ShishouFeed.classify(new ShishouFeed.Item("minecraft:bone", 1, 0, false, true, false, false, false)), "a tool flag overrides");
        assertEquals(ShishouFeed.Kind.NONE, ShishouFeed.classify(new ShishouFeed.Item("minecraft:bone", 1, 0, false, false, true, false, false)), "armour");
        assertEquals(ShishouFeed.Kind.NONE, ShishouFeed.classify(new ShishouFeed.Item("minecraft:bone", 1, 0, false, false, false, true, false)), "a block");
        assertEquals(ShishouFeed.Kind.NONE, ShishouFeed.classify(new ShishouFeed.Item("minecraft:bone", 1, 0, false, false, false, false, true)), "a container");
        assertEquals(ShishouFeed.Kind.NONE, ShishouFeed.classify(item("nusmp:shishou", 1, 3)), "never himself");
        assertEquals(ShishouFeed.Kind.NONE, ShishouFeed.classify(item("minecraft:zombie_spawn_egg", 1, 0)));
        assertEquals(ShishouFeed.Kind.NONE, ShishouFeed.classify(item("minecraft:bone", 0, 0)));
        assertEquals(ShishouFeed.Kind.NONE, ShishouFeed.classify(null));
        assertEquals(ShishouFeed.Kind.VALUABLE, ShishouFeed.classify(item("minecraft:bone", 1, 2)), "a rare thing is refused");
        assertEquals(ShishouFeed.Kind.VALUABLE, ShishouFeed.classify(new ShishouFeed.Item("minecraft:bone", 1, 0, true, false, false, false, false)), "an enchanted thing is refused");
    }

    @Test
    void oneClickEatsAtMostTheCapAndTheValueFollowsTheTable() {
        assertEquals(1, ShishouFeed.take(64, false, 64), "a right click eats one");
        assertEquals(64, ShishouFeed.take(64, true, 64));
        assertEquals(10, ShishouFeed.take(10, true, 64), "never more than the stack");
        assertEquals(16, ShishouFeed.take(64, true, 16), "the configured cap");
        assertEquals(64, ShishouFeed.take(64, true, 9999), "never above a full stack");
        assertEquals(0, ShishouFeed.take(0, true, 64));
        assertEquals(ShishouPredation.stoneExp("tensura:magic_stone", 5, ShishouFeed.STONE_TIER), ShishouFeed.value(item("tensura:magic_stone", 5, 0), 5));
        assertEquals(ShishouPredation.dropExp(3, 0), ShishouFeed.value(item("minecraft:bone", 3, 0), 3));
        assertEquals(0L, ShishouFeed.value(item("minecraft:diamond", 3, 0), 3));
        assertTrue(ShishouFeed.value(item("tensura:high_quality_magic_crystal", 64, 0), 64) <= ShishouPredation.MAX_EXP_PER_ABSORB, "capped per click");
        assertTrue(ShishouFeed.value(item("tensura:magic_stone", 1, 0), 1) > ShishouFeed.value(item("minecraft:bone", 1, 0), 1), "a stone is worth more than a bone");
    }

    @Test
    void theVerdictOrderKeepsStrangersOutAndFullSwordsFed() {
        ShishouFeed.Item bone = item("minecraft:bone", 4, 0);
        assertEquals(ShishouFeed.Verdict.OK, ShishouFeed.check(bone, true, true, true, false));
        assertEquals(ShishouFeed.Verdict.NOT_OWNER, ShishouFeed.check(bone, false, true, true, false), "non-owners get nothing");
        assertEquals(ShishouFeed.Verdict.DISABLED, ShishouFeed.check(bone, true, false, true, false));
        assertEquals(ShishouFeed.Verdict.TOO_FAST, ShishouFeed.check(bone, true, true, false, false));
        assertEquals(ShishouFeed.Verdict.FULL_LEVEL, ShishouFeed.check(bone, true, true, true, true), "a sword at the cap keeps the stack");
        assertEquals(ShishouFeed.Verdict.VALUABLE, ShishouFeed.check(item("minecraft:bone", 1, 3), true, true, true, false));
        assertEquals(ShishouFeed.Verdict.NOT_FOOD, ShishouFeed.check(item("minecraft:dirt", 1, 0), true, true, true, false));
        assertEquals(ShishouFeed.Verdict.EMPTY, ShishouFeed.check(item("minecraft:bone", 0, 0), true, true, true, false));
        assertTrue(ShishouFeed.takesOver(bone, true));
        assertFalse(ShishouFeed.takesOver(bone, false), "the click of a non-wielder stays vanilla, on the client and the server alike");
        assertFalse(ShishouFeed.takesOver(item("minecraft:dirt", 1, 0), true));
    }

    @Test
    void skillEaterCanStealFromDropsByTheirSourceMonster() {
        assertEquals("minecraft:blaze", ShishouFeed.sourceId("minecraft:blaze_rod"));
        assertEquals("minecraft:enderman", ShishouFeed.sourceId("minecraft:ender_pearl"));
        assertEquals("minecraft:guardian", ShishouFeed.sourceId("minecraft:prismarine_shard"));
        assertEquals("", ShishouFeed.sourceId("tensura:magic_stone"), "a stone has no species, so it steals nothing");
        assertEquals(ShishouSkill.FIRE_MAGIC, ShishouStealTable.lookup(ShishouFeed.sourceId("minecraft:blaze_rod"), false).skill());
        assertEquals(ShishouSkill.SPATIAL_MOTION_SHORT, ShishouStealTable.lookup(ShishouFeed.sourceId("minecraft:ender_pearl"), false).skill());
        assertEquals(ShishouSkill.WATER_ICE_MAGIC, ShishouStealTable.lookup(ShishouFeed.sourceId("minecraft:prismarine_crystals"), false).skill());
        assertEquals(ShishouSkill.FIRE_MAGIC, ShishouStealTable.lookup(ShishouFeed.sourceId("tensura:flame_wolf_fang"), false).skill(), "another mod's drop by the words of its name");
        ShishouStealTable.Row blaze = ShishouStealTable.lookup("minecraft:blaze", false);
        assertEquals(0.0, ShishouFeed.stealChance(blaze, 100, 0), 1e-12);
        assertEquals(0.0, ShishouFeed.stealChance(blaze, 0, 64), 1e-12, "the server's steal switch");
        double one = ShishouFeed.stealChance(blaze, 100, 1), many = ShishouFeed.stealChance(blaze, 100, 64);
        assertEquals(0.2, one, 1e-9, "half of the table's 40 percent");
        assertTrue(many > one && many < 1.0 && many <= 1.0 - Math.pow(0.8, ShishouFeed.STEAL_ROLLS_MAX) + 1e-9, "at most four rolls however big the stack: " + many);
        assertEquals(ShishouFeed.STEAL_ROLLS_MAX, ShishouFeed.stealRolls(64));
        assertEquals(1, ShishouFeed.stealRolls(1));
    }

    // ================================================================ E. the GUI geometry
    @Test
    void theAkashicGuiGeometryHolds() throws IOException {
        ShishouGuiLayout.Region[] regions = {ShishouGuiLayout.STAR, ShishouGuiLayout.EMBLEM, ShishouGuiLayout.MOTE, ShishouGuiLayout.CORNER_TL, ShishouGuiLayout.CORNER_TR, ShishouGuiLayout.CORNER_BL,
                ShishouGuiLayout.CORNER_BR, ShishouGuiLayout.PRISM, ShishouGuiLayout.SLOTROW, ShishouGuiLayout.GAUGE};
        for (int i = 0; i < regions.length; i++) {
            ShishouGuiLayout.Region r = regions[i];
            assertTrue(r.u() >= 0 && r.v() >= 0 && r.u() + r.w() <= ShishouGuiLayout.ATLAS && r.v() + r.h() <= ShishouGuiLayout.ATLAS, "region " + i + " is inside the atlas");
            for (int j = i + 1; j < regions.length; j++) {
                ShishouGuiLayout.Region q = regions[j];
                boolean apart = r.u() + r.w() <= q.u() || q.u() + q.w() <= r.u() || r.v() + r.h() <= q.v() || q.v() + q.h() <= r.v();
                assertTrue(apart, "regions " + i + " and " + j + " overlap");
            }
        }
        assertEquals(168, ShishouGuiLayout.stomachHeight(3));
        assertEquals(222, ShishouGuiLayout.stomachHeight(6));
        assertEquals(18, ShishouGuiLayout.stomachHeight(4) - ShishouGuiLayout.stomachHeight(3), "a row is 18 pixels");
        assertTrue(ShishouGuiLayout.GAUGE_X + ShishouGuiLayout.GAUGE_W <= ShishouGuiLayout.STOMACH_WIDTH - 6, "the gauges are inside the panel");
        assertTrue(ShishouGuiLayout.MANA_GAUGE_Y + ShishouGuiLayout.GAUGE_H < ShishouGuiLayout.FIRST_SLOT_Y - 2, "the gauges are inside the title row");
        assertTrue(ShishouGuiLayout.EMBLEM_Y + ShishouGuiLayout.EMBLEM_SIZE <= ShishouGuiLayout.FIRST_SLOT_Y - 1);
        assertTrue(ShishouGuiLayout.LEVEL_X > ShishouGuiLayout.TITLE_X + 35 && ShishouGuiLayout.LEVEL_X + 28 < ShishouGuiLayout.GAUGE_X, "title, level and gauges do not overlap");
        assertTrue(ShishouGuiLayout.maxStarBlits(ShishouGuiLayout.STOMACH_WIDTH, ShishouGuiLayout.stomachHeight(6)) <= 20, "the star field is at most twenty blits a panel");
        assertTrue(ShishouGuiLayout.maxStarBlits(ShishouGuiLayout.HUD_W, 140) <= 20, "the HUD star field is bounded too");
        assertTrue(ShishouGuiLayout.MOTES_PANEL <= 16 && ShishouGuiLayout.MOTES_HUD <= ShishouGuiLayout.MOTES_PANEL);
        for (int i = 0; i < ShishouGuiLayout.MOTES_PANEL; i++)
            for (float t = 0; t < 50_000; t += 997) {
                float x = ShishouGuiLayout.moteX(i, t, 176), y = ShishouGuiLayout.moteY(i, t, 168), a = ShishouGuiLayout.moteAlpha(i, t);
                assertTrue(x >= 0 && x < 176 && y >= 0 && y < 168, "mote " + i + " stays in the panel");
                assertTrue(a >= 0.1f && a <= 0.65f, "mote brightness is gentle");
            }
        assertEquals(0, ShishouGuiLayout.gaugePixels(-1f, 50));
        assertEquals(50, ShishouGuiLayout.gaugePixels(7f, 50));
        assertEquals(25, ShishouGuiLayout.gaugePixels(0.5f, 50));
        // the atlas file and the skill icon exist and the regions are not empty
        Path atlas = Path.of("src/main/resources/assets/nusmp/textures/gui/shishou/akashic.png");
        assertTrue(Files.isRegularFile(atlas), "the atlas exists (python3 tools/shishou/gen_shishou_gui.py)");
        BufferedImage img = javax.imageio.ImageIO.read(atlas.toFile());
        assertEquals(ShishouGuiLayout.ATLAS, img.getWidth());
        assertEquals(ShishouGuiLayout.ATLAS, img.getHeight());
        for (ShishouGuiLayout.Region r : regions) {
            long opaque = 0;
            for (int y = r.v(); y < r.v() + r.h(); y++) for (int x = r.u(); x < r.u() + r.w(); x++) if ((img.getRGB(x, y) >>> 24) > 16) opaque++;
            assertTrue(opaque > 0, "a region of the atlas is empty: " + r);
        }
        BufferedImage icon = javax.imageio.ImageIO.read(Path.of("src/main/resources/assets/nusmp/textures/skill/shishou.png").toFile());
        assertEquals(32, icon.getWidth());
        assertEquals(32, icon.getHeight());
        long solid = 0;
        for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) if ((icon.getRGB(x, y) >>> 24) > 200) solid++;
        assertTrue(solid > 32 * 32 * 0.8, "the skill icon fills its frame");
    }

    // ================================================================ source rules
    @Test
    void noKeyOfShishouIsRegisteredAnyMore() throws IOException {
        String client = code(read("client/shishou/ShishouClient.java"));
        assertFalse(client.contains("KeyMapping") || client.contains("RegisterKeyMappingsEvent") || client.contains("InputConstants"), "the client registers no key for Shishou");
        try (Stream<Path> w = Files.walk(MAIN)) {
            for (Path p : (Iterable<Path>) w.filter(x -> x.toString().endsWith(".java"))::iterator) {
                String c = code(Files.readString(p));
                assertFalse(c.contains("\"key.nusmp.shishou"), p + " still registers a Shishou key");
            }
        }
        String net = code(read("shishou/ShishouNet.java"));
        assertFalse(net.contains("ACT_THROW_RECALL") || net.contains("ShishouNet.Key"), "no key action is left");
        String skill = code(read("skill/ShishouUltimateSkill.java"));
        assertTrue(skill.contains("SkillType.ULTIMATE") && skill.contains("getModes") && skill.contains("onPressed"), "it is an Ultimate with modes");
        assertTrue(code(read("skill/NUSkills.java")).contains("SKILLS.register(\"shishou\""), "registered as nusmp:shishou");
    }

    @Test
    void theHuntScanIsTheOneBoundedBoxQuery() throws IOException {
        String goals = code(read("shishou/ShishouGoals.java"));
        assertFalse(goals.contains("NearestAttackableTargetGoal"), "no vanilla nearest-target scan any more");
        assertEquals(1, count(goals, "getEntitiesOfClass\\("), "exactly one box query in the goals");
        String scan = goals.substring(goals.indexOf("static LivingEntity scan("));
        scan = scan.substring(0, scan.indexOf("\n    }\n"));
        assertTrue(scan.contains("huntExamined()") && scan.contains("break"), "the number of creatures examined is capped");
        assertTrue(scan.contains("isValidTarget") && !scan.contains("getChunk") && !scan.contains("sort"), "valid targets only, no chunk load, no sort");
        assertTrue(goals.contains("huntNextScan") && goals.contains("scanGap("), "a scan is gated by a timestamp (not by every tick)");
        assertTrue(goals.contains("getNearestPlayer") && count(goals, "getNearestPlayer\\(") == 1, "the patrol is one nearest-player lookup a second");
        String ent = code(read("shishou/ShishouEntity.java"));
        assertTrue(ent.contains("ShishouHunt.strikeCooldown") && ent.contains("ShishouHunt.chargeSpeed"), "the entity uses the level-scaled numbers");
        String tg = code(read("shishou/ShishouTargets.java"));
        assertTrue(tg.contains("t instanceof Player") && tg.contains("AbstractVillager") && tg.contains("OwnableEntity") && tg.contains("isAlliedTo"), "never players, villagers, pets or allies");
    }

    @Test
    void theNewConfigAndTheNewStateAreServerSideAndCleaned() throws IOException {
        String cfg = code(read("shishou/ShishouConfig.java"));
        for (String k : new String[]{"\"shishouFeed\"", "\"grantUltimateSkill\"", "\"startLevel\"", "\"huntRadius\"", "\"huntScanTicks\"", "\"huntMaxExamined\"", "\"chargeSpeedPercent\"", "\"damageScalePercent\"", "\"expPercent\"", "\"feedPerClick\"", "\"feedGapTicks\"", "\"dormantAfterKills\""})
            assertTrue(cfg.contains(k), "config key " + k);
        assertFalse(cfg.contains("Type.CLIENT"), "all server settings");
        String live = code(read("shishou/ShishouLive.java"));
        assertTrue(live.contains("FEED_GAP"), "the feeding gap is per player");
        assertTrue(method(live, "void forget").contains("FEED_GAP.remove") && method(live, "void clearAll").contains("FEED_GAP.clear"), "and it is forgotten at logout, death, dimension change and server stop");
        String ev = code(read("shishou/ShishouEvents.java"));
        assertTrue(ev.contains("PlayerLoggedInEvent") && ev.contains("PlayerTickEvent.Post") && ev.contains("ShishouGrant.due"), "the skill follows the sword at login and every few seconds");
        String feed = code(read("shishou/ShishouFeedService.java"));
        assertTrue(feed.contains("FEED_GAP") && feed.contains("isClientSide") && feed.contains("cap.isOwner") && feed.contains("food.shrink(n)"), "server side, rate limited, owner only, shrunk once");
        assertEquals(1, count(feed, "food\\.shrink\\("), "items are taken exactly once (never duplicated)");
        String item = code(read("shishou/ShishouItem.java"));
        assertTrue(item.contains("overrideStackedOnOther") && item.contains("overrideOtherStackedOnMe"), "both inventory clicks of the Djinn Seal pattern");
        String screen = code(read("client/shishou/ShishouStomachScreen.java"));
        assertFalse(screen.contains("renderBackground("), "the blur guard: no background by hand");
        assertTrue(code(read("client/shishou/ShishouClient.java")).contains("ScreenEvent.Opening"), "the vanilla chest screen is swapped on the client only");
    }

    @Test
    void everyNewTextHasItsLangEntryAndNoModelIsNamed() throws IOException {
        JsonObject a = JsonParser.parseString(Files.readString(Path.of("tools/lang/shishou.json"))).getAsJsonObject(), b = JsonParser.parseString(Files.readString(Path.of("tools/lang/shishou_skill.json"))).getAsJsonObject();
        Set<String> keys = new HashSet<>(a.keySet());
        keys.addAll(b.keySet());
        Pattern p = Pattern.compile("(?:translatableWithFallback|SkillMsg\\.mode|SkillMsg\\.t)\\(\\s*\"((?:shishou|nusmp\\.skill)[a-z_.]*[a-z_])\"");
        List<String> missing = new ArrayList<>();
        for (String f : new String[]{"shishou/ShishouGrantService.java", "shishou/ShishouFeedService.java", "client/shishou/ShishouStomachScreen.java", "skill/ShishouUltimateSkill.java"}) {
            Matcher m = p.matcher(read(f));
            while (m.find()) {
                String k = m.group(1);
                if (f.endsWith("ShishouUltimateSkill.java") && !k.startsWith("nusmp.skill")) k = "nusmp.skill.mode." + k;
                if (!keys.contains(k)) missing.add(f + ": " + k);
            }
        }
        for (ShishouModes.Mode m : ShishouModes.Mode.values()) {
            assertTrue(keys.contains(m.langKey()), "lang: " + m.langKey());
            assertTrue(keys.contains("nusmp.skill.mode.shishou." + m.id()), "lang: the skill system's mode name of " + m.id());
        }
        assertTrue(keys.contains("nusmp.skill.shishou") && keys.contains("nusmp.skill.shishou.description"), "the skill's name and description");
        assertTrue(keys.contains("nusmp.gui_scale.gui.shishou_stomach") && keys.contains("shishou.tooltip.keys") && keys.contains("shishou.tooltip.feed"));
        assertFalse(a.get("shishou.tooltip.keys").getAsString().toLowerCase().contains("controls >"), "the tooltip no longer points at a key in Controls");
        assertTrue(missing.isEmpty(), "texts without a lang entry: " + missing);
        for (String k : b.keySet()) {
            String t = b.get(k).getAsString().toLowerCase();
            assertFalse(t.contains("claude") || t.contains("anthropic") || t.contains("assistant"), k + " names no model or assistant");
        }
    }

    private static int count(String s, String regex) {
        Matcher m = Pattern.compile(regex).matcher(s);
        int n = 0;
        while (m.find()) n++;
        return n;
    }

    /** The body of the first method whose signature contains {@code head} (from the opening brace to the matching one). */
    private static String method(String src, String head) {
        int at = src.indexOf(head);
        assertTrue(at >= 0, head);
        int open = src.indexOf('{', at), depth = 0, i = open;
        for (; i < src.length(); i++) {
            char c = src.charAt(i);
            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) break;
        }
        return src.substring(open, Math.min(i + 1, src.length()));
    }
}
