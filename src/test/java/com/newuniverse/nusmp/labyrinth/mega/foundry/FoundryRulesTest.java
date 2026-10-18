package com.newuniverse.nusmp.labyrinth.mega.foundry;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the pure rules of the Mob Foundry are the part that can be pinned without a game: the scaling (deeper is harder in EVERY stat, for every archetype, across every tier
 * boundary), the ability rules (cooldown, distance window, health condition, the situation each kind is for), the brain that picks the next ability (never during a cast, never a
 * passive, never on cooldown, once per roll interval), and the roster draw (the asked road and tier, a wished faction preferred, an empty tier falling back to a nearer one).
 */
public class FoundryRulesTest {
    private static FoundryLibrary library() throws Exception {
        String a = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/data/nusmp/mega/foundry/_abilities.json"));
        String b = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/data/nusmp/mega/foundry/_archetypes.json"));
        return new FoundryLibrary(FoundryLibrary.parseAbilities(a).rows(), FoundryLibrary.parseArchetypes(b).rows());
    }

    @Test
    public void scalingRisesWithTheTierInEveryStatForEveryArchetype() throws Exception {
        FoundryLibrary lib = library();
        assertEquals(10, lib.archetypes().size());
        for (FoundryArchetype a : lib.archetypes().values()) {
            double lastHealth = 0, lastDamage = 0, lastArmor = -1, lastSpeed = 0;
            for (int tier = 1; tier <= 5; tier++) {
                int first = (tier - 1) * 200 + 1, last = tier * 200;
                double h1 = FoundryScaling.health(a, tier, first), h2 = FoundryScaling.health(a, tier, last);
                double d1 = FoundryScaling.damage(a, tier, first), d2 = FoundryScaling.damage(a, tier, last);
                assertTrue(h2 >= h1, a.id() + " health falls inside tier " + tier);
                assertTrue(d2 >= d1, a.id() + " damage falls inside tier " + tier);
                assertTrue(h1 > lastHealth, a.id() + " tier " + tier + " is not tougher than the tier above it");
                assertTrue(d1 > lastDamage, a.id() + " tier " + tier + " does not hit harder than the tier above it");
                assertTrue(FoundryScaling.armor(a, tier) >= lastArmor, a.id() + " armour falls at tier " + tier);
                assertTrue(FoundryScaling.speed(a, tier) >= lastSpeed, a.id() + " speed falls at tier " + tier);
                lastHealth = h2;
                lastDamage = d2;
                lastArmor = FoundryScaling.armor(a, tier);
                lastSpeed = FoundryScaling.speed(a, tier);
            }
        }
    }

    @Test
    public void scalingStaysInsideItsCaps() throws Exception {
        for (FoundryArchetype a : library().archetypes().values()) {
            for (int floor : new int[]{1, 200, 201, 500, 1000, 5000, -1, -999, 0}) {
                int tier = FoundryScaling.tierOf(floor);
                assertTrue(tier >= 1 && tier <= 5);
                assertTrue(FoundryScaling.health(a, tier, floor) <= FoundryScaling.MAX_HEALTH);
                assertTrue(FoundryScaling.damage(a, tier, floor) <= FoundryScaling.MAX_DAMAGE);
                assertTrue(FoundryScaling.armor(a, tier) <= FoundryScaling.MAX_ARMOR);
                double sp = FoundryScaling.speed(a, tier);
                assertTrue(sp >= FoundryScaling.MIN_SPEED && sp <= FoundryScaling.MAX_SPEED);
            }
        }
    }

    @Test
    public void tierOfAFloorIsTheGuardiansRule() {
        assertEquals(1, FoundryScaling.tierOf(1));
        assertEquals(1, FoundryScaling.tierOf(200));
        assertEquals(2, FoundryScaling.tierOf(201));
        assertEquals(5, FoundryScaling.tierOf(801));
        assertEquals(5, FoundryScaling.tierOf(1000));
        assertEquals(5, FoundryScaling.tierOf(4000));
        assertEquals(3, FoundryScaling.tierOf(-401));
        assertEquals(1, FoundryScaling.tierOf(0));
        assertEquals(0.0, FoundryScaling.progress(1), 1e-9);
        assertEquals(1.0, FoundryScaling.progress(200), 1e-9);
        assertEquals(0.0, FoundryScaling.progress(201), 1e-9);
    }

    @Test
    public void theHitboxScaleFollowsTheModelHeightAndSize() {
        assertEquals(1.0, FoundryScaling.scaleAttribute(1.95, 1.0), 1e-9);
        assertTrue(FoundryScaling.scaleAttribute(3.1, 1.0) > FoundryScaling.scaleAttribute(2.0, 1.0));
        assertTrue(FoundryScaling.scaleAttribute(2.0, 1.4) > FoundryScaling.scaleAttribute(2.0, 1.0));
        assertTrue(FoundryScaling.scaleAttribute(0.1, 0.1) >= 0.4);
        assertTrue(FoundryScaling.scaleAttribute(99, 9) <= 3.0);
    }

    @Test
    public void everyArchetypeOfTheContractHasAStyleAndSaneNumbers() throws Exception {
        FoundryLibrary lib = library();
        for (String id : FoundryArchetype.CONTRACT) {
            FoundryArchetype a = lib.archetype(id);
            assertNotNull(a, id);
            assertNotNull(a.style(), id);
            if (a.style() == FoundryArchetype.Style.RANGED || a.style() == FoundryArchetype.Style.SUPPORT) assertTrue(a.far() >= a.near() && a.keep() > 0, id + " needs a distance band");
        }
    }

    // ---------------------------------------------------------------- abilities
    @Test
    public void theShippedLibraryParsesCleanAndHasTheWholeSet() throws Exception {
        String a = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/data/nusmp/mega/foundry/_abilities.json"));
        String b = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/data/nusmp/mega/foundry/_archetypes.json"));
        FoundryLibrary.Parsed<FoundryAbility> pa = FoundryLibrary.parseAbilities(a);
        FoundryLibrary.Parsed<FoundryArchetype> pb = FoundryLibrary.parseArchetypes(b);
        assertEquals(List.of(), pa.problems());
        assertEquals(List.of(), pb.problems());
        assertTrue(pa.rows().size() >= 40, "the library holds about forty abilities or more");
        for (FoundryAbility.Kind k : FoundryAbility.Kind.values()) {
            boolean used = false;
            for (FoundryAbility x : pa.rows().values()) used |= x.kind() == k;
            assertTrue(used, "no ability of the kind " + k);
        }
    }

    @Test
    public void aBadLibraryRowIsDroppedAndReported() {
        String json = "{\"abilities\":[{\"id\":\"ok\",\"kind\":\"dash\",\"cd\":50,\"min\":2,\"max\":8},{\"id\":\"Bad Id\",\"kind\":\"dash\"},{\"id\":\"nokind\",\"kind\":\"teleport_to_moon\"},"
                + "{\"id\":\"ok\",\"kind\":\"dash\",\"cd\":50},{\"id\":\"nocd\",\"kind\":\"slam\",\"cd\":0,\"max\":3},{\"id\":\"huge\",\"kind\":\"volley\",\"cd\":99999,\"n\":999,\"pow\":99,\"dur\":99999}]}";
        FoundryLibrary.Parsed<FoundryAbility> p = FoundryLibrary.parseAbilities(json);
        assertEquals(3, p.rows().size());                                   // ok, nocd (kept with a problem), huge (clamped)
        assertTrue(p.problems().size() >= 4, p.problems().toString());
        FoundryAbility huge = p.rows().get("huge");
        assertTrue(huge.cd() <= 6000 && huge.n() <= FoundryAbility.MAX_COUNT && huge.pow() <= FoundryAbility.MAX_POWER && huge.dur() <= FoundryAbility.MAX_DURATION);
        assertTrue(FoundryLibrary.parseAbilities("not json at all").problems().size() > 0);
    }

    private static FoundryAbility ab(FoundryAbility.Kind k, double min, double max, double hp, double r, int n) {
        return FoundryAbility.clamped("x", k, 100, min, max, 8, 1.0, 100, n, r, 0, hp, "road", "");
    }

    @Test
    public void anAbilityKnowsItsWindowCooldownAndHealthRule() {
        FoundryAbility dash = ab(FoundryAbility.Kind.DASH, 4, 12, 0, 0, 0);
        assertTrue(dash.needsTarget());
        assertFalse(dash.inWindow(3.9));
        assertTrue(dash.inWindow(4));
        assertTrue(dash.inWindow(12));
        assertFalse(dash.inWindow(12.1));
        assertTrue(dash.ready(100, 100));
        assertFalse(dash.ready(101, 100));
        FoundryAbility enrage = ab(FoundryAbility.Kind.ENRAGE, 0, 0, 0.5, 0, 0);
        assertFalse(enrage.needsTarget());
        assertTrue(enrage.hpOk(0.49));
        assertFalse(enrage.hpOk(0.5));
        assertEquals(6.0, FoundryAbility.clamped("d", FoundryAbility.Kind.SLAM, 10, 0, 3, 5, 1.5, 0, 0, 3, 0, 0, "road", "").damage(4.0), 1e-9);
        assertTrue(ab(FoundryAbility.Kind.BURST, 0, 0, 0, 3, 0).passive());
        assertTrue(FoundryAbility.clamped("w", FoundryAbility.Kind.SLAM, 10, 0, 3, 24, 1, 0, 0, 3, 0, 0, "road", "").big());
        assertTrue(new FoundryAbility("a", FoundryAbility.Kind.AURA, 10, 0, 0, 5, 1, 1, 0, 1, 0, 0, "dark", "").dark(false));
        assertFalse(new FoundryAbility("a", FoundryAbility.Kind.AURA, 10, 0, 0, 5, 1, 1, 0, 1, 0, 0, "light", "").dark(true));
        assertTrue(new FoundryAbility("a", FoundryAbility.Kind.AURA, 10, 0, 0, 5, 1, 1, 0, 1, 0, 0, "road", "").dark(true));
    }

    @Test
    public void eachKindOnlyFitsTheSituationItIsFor() {
        FoundryAbility.Situation hurt = new FoundryAbility.Situation(-1, 0.5, false, 0, 1.0, 0, false, false, false, false, false, false);
        FoundryAbility.Situation fight = new FoundryAbility.Situation(5, 1.0, true, 2, 1.0, 0, false, false, false, false, false, false);
        FoundryAbility.Situation woundedAlly = new FoundryAbility.Situation(5, 1.0, true, 2, 0.4, 0, false, false, false, false, false, false);
        FoundryAbility heal = ab(FoundryAbility.Kind.HEAL, 0, 0, 0, 6, 1);
        assertTrue(heal.fits(hurt), "a hurt mob heals even with no target");
        assertFalse(heal.fits(fight), "nobody is hurt");
        assertTrue(heal.fits(woundedAlly), "an ally is hurt");
        FoundryAbility summon = ab(FoundryAbility.Kind.SUMMON, 0, 24, 0, 3, 2);
        assertTrue(summon.fits(fight));
        assertFalse(summon.fits(new FoundryAbility.Situation(5, 1.0, true, 2, 1.0, 4, false, false, false, false, false, false)), "enough minions already");
        assertFalse(summon.fits(FoundryAbility.Situation.of(-1, 1.0)), "no target, no summon");
        FoundryAbility aura = ab(FoundryAbility.Kind.AURA, 0, 0, 0, 4, 0);
        assertTrue(aura.fits(FoundryAbility.Situation.of(5, 1.0)));
        assertFalse(aura.fits(FoundryAbility.Situation.of(20, 1.0)), "too far for an aura");
        assertFalse(aura.fits(new FoundryAbility.Situation(5, 1.0, true, 0, 1.0, 0, false, false, true, false, false, false)), "already burning");
        FoundryAbility guard = ab(FoundryAbility.Kind.GUARD, 0, 0, 0, 0, 0);
        assertTrue(guard.fits(FoundryAbility.Situation.of(6, 1.0)));
        assertFalse(guard.fits(new FoundryAbility.Situation(6, 1.0, true, 0, 1.0, 0, true, false, false, false, false, false)), "already guarded");
        FoundryAbility rally = ab(FoundryAbility.Kind.RALLY, 0, 0, 0, 12, 0);
        assertTrue(rally.fits(fight));
        assertFalse(rally.fits(FoundryAbility.Situation.of(5, 1.0)), "a rally needs allies");
        assertFalse(ab(FoundryAbility.Kind.BURST, 0, 0, 0, 3, 0).fits(fight), "a death burst is never cast");
        assertFalse(ab(FoundryAbility.Kind.DASH, 4, 12, 0, 0, 0).fits(FoundryAbility.Situation.of(2, 1.0)), "too close to dash");
        assertTrue(ab(FoundryAbility.Kind.DASH, 4, 12, 0, 0, 0).fits(FoundryAbility.Situation.of(8, 1.0)));
    }

    @Test
    public void theBrainPicksReadyFittingAbilitiesOncePerInterval() {
        FoundryAbility dash = FoundryAbility.clamped("dash", FoundryAbility.Kind.DASH, 100, 4, 12, 6, 1, 8, 0, 0, 0, 0, "road", "");
        FoundryAbility slam = FoundryAbility.clamped("slam", FoundryAbility.Kind.SLAM, 200, 0, 5, 14, 1, 0, 0, 4, 0, 0, "road", "");
        FoundryAbility burst = FoundryAbility.clamped("burst", FoundryAbility.Kind.BURST, 0, 0, 0, 0, 1, 0, 0, 4, 0, 0, "road", "");
        FoundryBrain b = new FoundryBrain(List.of(dash, slam, burst), 20);
        Random r = new Random(5);
        FoundryAbility.Situation far = FoundryAbility.Situation.of(8, 1.0), near = FoundryAbility.Situation.of(3, 1.0);
        assertTrue(b.due(0));
        assertEquals(-1, b.choose(0, true, far, r), "never during a cast");
        int first = b.choose(1, false, far, r);
        assertEquals(0, first, "only the dash fits at distance 8");
        assertFalse(b.due(5), "one roll per interval");
        assertEquals(-1, b.choose(5, false, far, r), "a second roll inside the interval answers nothing");
        b.used(0, 1);
        assertFalse(b.ready(0, 50));
        assertTrue(b.ready(0, 101));
        assertEquals(-1, b.choose(30, false, far, r), "the dash is cooling and nothing else fits at 8");
        assertEquals(1, b.choose(60, false, near, r), "at 3 only the slam fits");
        assertEquals(2, b.passive());
        for (long t = 100; t < 3000; t += 25) assertTrue(b.choose(t, false, near, new Random(t)) != 2, "the passive is never chosen");
        b.reset(0);
        assertTrue(b.ready(0, 0) && b.ready(1, 0));
    }

    // ---------------------------------------------------------------- the draw
    private static FoundryDef def(String id, com.newuniverse.nusmp.labyrinth.mega.Polarity road, int tier, String faction) {
        return new FoundryDef(id, id, road, tier, faction, "brute", List.of("dash", "ground_slam"), 1.0, 2.0, "test", 0xFFFFFF);
    }

    @Test
    public void theDrawGivesTheAskedRoadAndTierAndPrefersTheWishedFaction() {
        com.newuniverse.nusmp.labyrinth.mega.Polarity H = com.newuniverse.nusmp.labyrinth.mega.Polarity.HOLY, A = com.newuniverse.nusmp.labyrinth.mega.Polarity.ABYSSAL;
        List<FoundryDef> defs = new java.util.ArrayList<>();
        for (int t = 1; t <= 5; t++) {
            defs.add(def("h" + t + "u", H, t, "undead"));
            defs.add(def("h" + t + "b", H, t, "beast"));
            defs.add(def("a" + t + "d", A, t, "demon"));
        }
        FoundryRoster r = new FoundryRoster(defs);
        Random rnd = new Random(3);
        int undead = 0, beast = 0;
        for (int i = 0; i < 1000; i++) {
            FoundryDef d = r.pick(H, 3, "undead", null, rnd);
            assertEquals(H, d.road());
            assertEquals(3, d.tier());
            if (d.faction().equals("undead")) undead++;
            else beast++;
        }
        assertTrue(undead > beast * 2.5, "the wished faction is about four times likelier: " + undead + " vs " + beast);
        for (int i = 0; i < 200; i++) {
            FoundryDef d = r.pick(A, 5, null, null, rnd);
            assertEquals(A, d.road());
            assertEquals(5, d.tier());
        }
        // a theme that shuts a faction out
        for (int i = 0; i < 200; i++) assertEquals("beast", r.pick(H, 2, null, f -> f.equals("undead") ? 0.0 : 1.0, rnd).faction());
        // a theme that shuts everything out is ignored rather than spawning nothing
        assertNotNull(r.pick(H, 2, null, f -> 0.0, rnd));
        // an empty tier falls back to the nearest below, then above
        FoundryRoster sparse = new FoundryRoster(List.of(def("only", H, 2, "undead")));
        assertEquals("only", sparse.pick(H, 5, null, null, rnd).id());
        assertEquals("only", sparse.pick(H, 1, null, null, rnd).id());
        assertEquals(null, sparse.pick(A, 1, null, null, rnd));
        assertEquals(1, sparse.count(H));
        assertEquals(0, sparse.count(A));
        assertEquals(List.of(), FoundryRoster.EMPTY.of(H, 1));
    }

    @Test
    public void minionsAreLowerTierMobsOfTheSameRoad() {
        com.newuniverse.nusmp.labyrinth.mega.Polarity H = com.newuniverse.nusmp.labyrinth.mega.Polarity.HOLY;
        FoundryRoster r = new FoundryRoster(List.of(def("a1", H, 1, "undead"), def("b2", H, 2, "undead"), def("c3", H, 3, "undead"), def("d1", H, 1, "beast")));
        assertEquals(2, r.minions(H, 2, "undead").size());
        assertEquals(3, r.minions(H, 2, null).size());
        assertEquals(1, r.minions(H, 1, "beast").size());
    }

    @Test
    public void aFoundryKeyRoundTripsAndNothingElseIsOne() {
        FoundryDef d = def("tunnel_kobold", com.newuniverse.nusmp.labyrinth.mega.Polarity.HOLY, 1, "beast");
        assertEquals("nusmp:foundry_mob#tunnel_kobold", d.entityKey());
        assertEquals("tunnel_kobold", FoundryDef.idOfKey(d.entityKey()));
        assertEquals(null, FoundryDef.idOfKey("minecraft:zombie"));
        assertEquals(null, FoundryDef.idOfKey("nusmp:foundry_mob#Bad Id"));
        assertEquals(null, FoundryDef.idOfKey(null));
    }
}
