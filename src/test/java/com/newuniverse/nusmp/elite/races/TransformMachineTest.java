package com.newuniverse.nusmp.elite.races;

import com.newuniverse.nusmp.elite.races.state.Coexistence;
import com.newuniverse.nusmp.elite.races.state.EntryCondition;
import com.newuniverse.nusmp.elite.races.state.EntryCondition.Reason;
import com.newuniverse.nusmp.elite.races.state.MoonRule;
import com.newuniverse.nusmp.elite.races.state.StateModifier;
import com.newuniverse.nusmp.elite.races.state.StateModifier.Op;
import com.newuniverse.nusmp.elite.races.state.TransformContext;
import com.newuniverse.nusmp.elite.races.state.TransformMachine;
import com.newuniverse.nusmp.elite.races.state.TransformSpec;
import com.newuniverse.nusmp.elite.races.state.TransformState;
import com.newuniverse.nusmp.elite.races.state.Zenkai;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the transformation rules (who may enter, from where, for how long, how long to rest, what ends a state) are the part of the races that is easy to get subtly wrong, so the state machine is driven here with a settable clock. */
class TransformMachineTest {
    static final class Ctx implements TransformContext {
        int tier = 3;
        double ep = 100_000;
        double hp = 1.0;
        boolean night, fullMoon, sky = true;
        double aura = 1.0;
        long now = 1000;
        final Map<String, Integer> items = new HashMap<>();

        @Override public int tier() { return tier; }
        @Override public double ep() { return ep; }
        @Override public double hpFraction() { return hp; }
        @Override public boolean night() { return night; }
        @Override public boolean fullMoon() { return fullMoon; }
        @Override public boolean skyVisible() { return sky; }
        @Override public int itemCount(String id) { return items.getOrDefault(id, 0); }
        @Override public double auraFraction() { return aura; }
        @Override public long now() { return now; }
    }

    private static EntryCondition entry(double ep, double hp, MoonRule moon, boolean sky, String item, int tier, boolean auto) {
        return new EntryCondition(ep, hp, moon, sky, item, item.isEmpty() ? 0 : 1, false, tier, auto);
    }

    private static TransformSpec spec(TransformState s, Set<TransformState> from, int duration, int cooldown, double drain, boolean dawn, EntryCondition... entries) {
        return new TransformSpec(s, from, 1, List.of(entries), duration, cooldown, drain, dawn, true, false, 0xFFFFFFFF,
                List.of(new StateModifier("attack_damage", 0.5, Op.MULT_BASE), new StateModifier("max_health", 10, Op.ADD)));
    }

    private static Map<TransformState, TransformSpec> specs() {
        Map<TransformState, TransformSpec> m = new EnumMap<>(TransformState.class);
        m.put(TransformState.BOOSTED, spec(TransformState.BOOSTED, Set.of(TransformState.BASE), 1200, 200, 0.01, false, EntryCondition.ALWAYS));
        m.put(TransformState.ASCENDED, spec(TransformState.ASCENDED, Set.of(TransformState.BASE, TransformState.BOOSTED), 2400, 6000, 0.01, false,
                entry(50_000, 0.3, MoonRule.ANY, false, "", 1, true), entry(500_000, 1.0, MoonRule.ANY, false, "", 1, false)));
        m.put(TransformState.GREAT_APE, spec(TransformState.GREAT_APE, Set.of(TransformState.BASE), 6000, 12000, 0.0, true,
                entry(0, 1.0, MoonRule.FULL_MOON, true, "", 1, true), entry(60_000, 1.0, MoonRule.ANY, false, "", 3, false)));
        m.put(TransformState.DIVINE, spec(TransformState.DIVINE, Set.of(TransformState.ASCENDED, TransformState.BOOSTED), 800, 24000, 0.02, false,
                entry(400_000, 1.0, MoonRule.ANY, false, "", 4, false)));
        return m;
    }

    @Test
    void aFreshMachineIsBaseWithNoRest() {
        TransformMachine m = new TransformMachine();
        assertEquals(TransformState.BASE, m.state());
        assertFalse(m.active());
        assertEquals(0, m.restLeft(TransformState.BOOSTED, 5000));
        assertEquals(0, m.remaining(5000));
    }

    @Test
    void anUnconditionalStateIsEnteredAndEndsAfterItsDurationThenRests() {
        TransformMachine m = new TransformMachine();
        Ctx c = new Ctx();
        var sp = specs();
        assertTrue(m.enter(sp.get(TransformState.BOOSTED), null, c).ok());
        assertEquals(TransformState.BOOSTED, m.state());
        assertEquals(1200, m.remaining(c.now));
        c.now += 1199;
        assertEquals(TransformMachine.Event.NONE, m.tick(sp, c));
        c.now += 1;
        assertEquals(TransformMachine.Event.EXPIRED, m.tick(sp, c));
        assertEquals(TransformState.BASE, m.state());
        assertEquals(200, m.restLeft(TransformState.BOOSTED, c.now));
        TransformMachine.Result again = m.check(sp.get(TransformState.BOOSTED), c);
        assertFalse(again.ok());
        assertEquals(Reason.COOLDOWN, again.reason());
        c.now += 200;
        assertTrue(m.check(sp.get(TransformState.BOOSTED), c).ok());
    }

    @Test
    void theEpThresholdTheHealthAndTheTierAreEnforced() {
        TransformMachine m = new TransformMachine();
        Ctx c = new Ctx();
        var sp = specs();
        c.ep = 10_000;
        c.hp = 0.2;
        assertEquals(Reason.EP, m.check(sp.get(TransformState.ASCENDED), c).reason());   // rage, but not strong enough
        c.ep = 60_000;
        c.hp = 0.9;
        assertEquals(Reason.HEALTH, m.check(sp.get(TransformState.ASCENDED), c).reason()); // strong, but not in a rage
        c.hp = 0.25;
        assertTrue(m.check(sp.get(TransformState.ASCENDED), c).ok());
        c.hp = 1.0;
        c.ep = 600_000;                                                                    // the second entry: pure power, no rage needed
        assertTrue(m.check(sp.get(TransformState.ASCENDED), c).ok());
        c.tier = 3;
        m.forceEnter(sp.get(TransformState.BOOSTED), null, c.now);                         // divine opens from boosted ...
        assertEquals(Reason.TIER, m.check(sp.get(TransformState.DIVINE), c).reason());    // ... but only at tier 4
    }

    @Test
    void theApeNeedsAFullMoonUnderTheSkyUnlessTheRaceCommandsIt() {
        TransformMachine m = new TransformMachine();
        Ctx c = new Ctx();
        var sp = specs();
        c.tier = 1;
        c.ep = 0;
        assertEquals(Reason.MOON, m.check(sp.get(TransformState.GREAT_APE), c).reason());
        c.night = true;
        assertEquals(Reason.MOON, m.check(sp.get(TransformState.GREAT_APE), c).reason()); // night, but not a full moon
        c.fullMoon = true;
        c.sky = false;
        assertEquals(Reason.SKY, m.check(sp.get(TransformState.GREAT_APE), c).reason());  // a full moon behind a roof
        c.sky = true;
        assertTrue(m.check(sp.get(TransformState.GREAT_APE), c).ok());
        // a High Saiyan with enough EP needs neither
        c.night = false;
        c.fullMoon = false;
        c.tier = 3;
        c.ep = 70_000;
        assertTrue(m.check(sp.get(TransformState.GREAT_APE), c).ok());
    }

    @Test
    void anItemEntryNeedsTheItemAndNamesTheEntryThatLetItThrough() {
        TransformMachine m = new TransformMachine();
        Ctx c = new Ctx();
        c.tier = 1;
        c.ep = 0;
        EntryCondition moon = entry(0, 1.0, MoonRule.FULL_MOON, true, "", 1, true);
        EntryCondition powerBall = new EntryCondition(0, 1.0, MoonRule.ANY, true, "minecraft:sea_lantern", 1, true, 1, false);
        TransformSpec ape = spec(TransformState.GREAT_APE, Set.of(TransformState.BASE), 6000, 12000, 0.0, true, moon, powerBall);
        TransformMachine.Result none = m.check(ape, c);
        assertFalse(none.ok());
        assertEquals(Reason.MOON, none.reason());              // the first entry's reason is the one reported
        c.items.put("minecraft:sea_lantern", 1);
        TransformMachine.Result lit = m.check(ape, c);
        assertTrue(lit.ok());
        assertSame(powerBall, lit.entry());                    // so the runtime knows to take the lantern
        assertTrue(lit.entry().consume());
        c.sky = false;                                         // a Power Ball needs the open sky too
        assertFalse(m.check(ape, c).ok());
        c.sky = true;
        c.night = true;
        c.fullMoon = true;                                     // a real full moon is tried first, so the lantern is not wasted
        assertSame(moon, m.check(ape, c).entry());
        assertFalse(m.check(ape, c).entry().consume());
        // the automatic trigger never uses the item entry
        assertNull(new TransformMachine().autoTrigger(Map.of(TransformState.GREAT_APE, spec(TransformState.GREAT_APE, Set.of(TransformState.BASE), 6000, 12000, 0.0, true, powerBall)), c));
    }

    @Test
    void theApeEndsWithTheNight() {
        TransformMachine m = new TransformMachine();
        Ctx c = new Ctx();
        var sp = specs();
        c.night = true;
        c.fullMoon = true;
        assertTrue(m.enter(sp.get(TransformState.GREAT_APE), null, c).ok());
        c.now += 100;
        assertEquals(TransformMachine.Event.NONE, m.tick(sp, c));
        c.night = false;
        assertEquals(TransformMachine.Event.DAWN, m.tick(sp, c));
        assertEquals(TransformState.BASE, m.state());
        assertTrue(m.restLeft(TransformState.GREAT_APE, c.now) > 0);
    }

    @Test
    void aStateThatCostsAuraEndsWhenTheKiRunsOutAndCannotStartEmpty() {
        TransformMachine m = new TransformMachine();
        Ctx c = new Ctx();
        var sp = specs();
        c.aura = 0.0;
        assertEquals(Reason.AURA, m.check(sp.get(TransformState.BOOSTED), c).reason());
        c.aura = 0.5;
        assertTrue(m.enter(sp.get(TransformState.BOOSTED), null, c).ok());
        c.aura = 0.0;
        assertEquals(TransformMachine.Event.DRAINED, m.tick(sp, c));
        assertEquals(TransformState.BASE, m.state());
        // a state with no upkeep (the ape) does not care
        Ctx night = new Ctx();
        night.night = true;
        night.fullMoon = true;
        night.aura = 0.0;
        assertTrue(new TransformMachine().check(sp.get(TransformState.GREAT_APE), night).ok());
    }

    @Test
    void aStateIsEnteredOnlyFromTheStatesItListsAndLeavingOneStartsItsRest() {
        TransformMachine m = new TransformMachine();
        Ctx c = new Ctx();
        c.tier = 4;
        c.ep = 900_000;
        var sp = specs();
        assertEquals(Reason.FROM, m.check(sp.get(TransformState.DIVINE), c).reason());   // divine only from ascended or boosted
        assertTrue(m.enter(sp.get(TransformState.BOOSTED), null, c).ok());
        assertEquals(Reason.ALREADY, m.check(sp.get(TransformState.BOOSTED), c).reason());
        assertEquals(Reason.FROM, m.check(sp.get(TransformState.GREAT_APE), c).reason()); // the ape only from base
        c.now += 50;
        assertTrue(m.enter(sp.get(TransformState.ASCENDED), sp.get(TransformState.BOOSTED), c).ok());
        assertEquals(TransformState.ASCENDED, m.state());
        assertEquals(200, m.restLeft(TransformState.BOOSTED, c.now));                     // switching away from boosted starts its rest
        assertTrue(m.enter(sp.get(TransformState.DIVINE), sp.get(TransformState.ASCENDED), c).ok());
        assertEquals(TransformState.DIVINE, m.state());
    }

    @Test
    void aRaceThatLosesTheStateEndsItWithoutARest() {
        TransformMachine m = new TransformMachine();
        Ctx c = new Ctx();
        var sp = specs();
        assertTrue(m.enter(sp.get(TransformState.BOOSTED), null, c).ok());
        sp.remove(TransformState.BOOSTED);                   // the player became a race that has no boosted state
        assertEquals(TransformMachine.Event.REMOVED, m.tick(sp, c));
        assertEquals(TransformState.BASE, m.state());
        assertEquals(0, m.restLeft(TransformState.BOOSTED, c.now));
        // a drop in tier does the same
        var sp2 = specs();
        TransformMachine n = new TransformMachine();
        c.tier = 4;
        c.ep = 900_000;
        assertTrue(n.enter(sp2.get(TransformState.BOOSTED), null, c).ok());
        assertTrue(n.enter(sp2.get(TransformState.DIVINE), sp2.get(TransformState.BOOSTED), c).ok());
        c.tier = 2;
        sp2.put(TransformState.DIVINE, new TransformSpec(TransformState.DIVINE, Set.of(TransformState.BOOSTED), 4, List.of(EntryCondition.ALWAYS), 800, 100, 0, false, false, false, 0, List.of()));
        assertEquals(TransformMachine.Event.REMOVED, n.tick(sp2, c));
    }

    @Test
    void autoTriggersFireOnlyForTheEntriesMarkedAuto() {
        TransformMachine m = new TransformMachine();
        Ctx c = new Ctx();
        var sp = specs();
        c.ep = 600_000;                                      // the at-will ascension entry holds ...
        c.hp = 1.0;
        assertNull(m.autoTrigger(sp, c));                    // ... but it is not an auto entry, so nothing starts by itself
        c.hp = 0.2;
        assertEquals(TransformState.ASCENDED, m.autoTrigger(sp, c));    // the rage entry is auto
        c.night = true;
        c.fullMoon = true;
        assertEquals(TransformState.GREAT_APE, m.autoTrigger(sp, c));   // the moon does not wait
        m.forceEnter(sp.get(TransformState.BOOSTED), null, c.now);
        assertNull(m.autoTrigger(sp, c));                    // never while a state is up
    }

    @Test
    void theHotkeyPicksTheBestAndCyclesInOrder() {
        TransformMachine m = new TransformMachine();
        Ctx c = new Ctx();
        c.tier = 4;
        c.ep = 900_000;
        var sp = specs();
        assertEquals(TransformState.BOOSTED, m.cycleTarget(sp, c));
        assertEquals(TransformState.ASCENDED, m.best(sp, c), "from base the divine form is closed (it needs ascended or boosted first)");
        m.forceEnter(sp.get(TransformState.BOOSTED), null, c.now);
        assertEquals(TransformState.DIVINE, m.best(sp, c));                 // from boosted the strongest allowed is divine
        assertEquals(TransformState.ASCENDED, m.cycleTarget(sp, c));
        m.forceEnter(sp.get(TransformState.ASCENDED), sp.get(TransformState.BOOSTED), c.now);
        assertEquals(TransformState.DIVINE, m.cycleTarget(sp, c));
        m.forceEnter(sp.get(TransformState.DIVINE), sp.get(TransformState.ASCENDED), c.now);
        assertNull(m.cycleTarget(sp, c));                                   // the cycle wrapped: back to base
        // the ape is never chosen by 'best'
        TransformMachine fresh = new TransformMachine();
        Ctx night = new Ctx();
        night.night = true;
        night.fullMoon = true;
        night.tier = 1;
        night.ep = 0;
        var only = new EnumMap<TransformState, TransformSpec>(TransformState.class);
        only.put(TransformState.GREAT_APE, specs().get(TransformState.GREAT_APE));
        assertNull(fresh.best(only, night));
    }

    @Test
    void theStateSurvivesASaveAndACorruptSaveIsHarmless() {
        TransformMachine m = new TransformMachine();
        Ctx c = new Ctx();
        var sp = specs();
        m.forceEnter(sp.get(TransformState.BOOSTED), null, c.now);
        m.forceEnter(sp.get(TransformState.ASCENDED), sp.get(TransformState.BOOSTED), c.now + 10);
        TransformMachine back = TransformMachine.decode(m.encode());
        assertEquals(m.state(), back.state());
        assertEquals(m.endsAt(), back.endsAt());
        assertEquals(m.restLeft(TransformState.BOOSTED, c.now), back.restLeft(TransformState.BOOSTED, c.now));
        // an unlimited state round-trips as unlimited
        TransformSpec forever = new TransformSpec(TransformState.BOOSTED, Set.of(TransformState.BASE), 1, List.of(), 0, 0, 0, false, false, false, 0, List.of());
        TransformMachine u = new TransformMachine();
        u.forceEnter(forever, null, 5);
        assertEquals(Long.MAX_VALUE, TransformMachine.decode(u.encode()).endsAt());
        for (String junk : new String[]{null, "", "x", "boosted;a;b;", "boosted;1;2;boosted=zz", ";;;", "base;1;2;nonsense"}) {
            TransformMachine j = TransformMachine.decode(junk);
            assertNotNull(j, "decode(" + junk + ")");
            assertTrue(j.state() == TransformState.BASE || junk.startsWith("boosted"));
        }
    }

    @Test
    void kaiokenScalesOnlyTheMultipliersOfAKiStateOnTheStatsKaiokenAlsoTouches() {
        StateModifier atk = new StateModifier("attack_damage", 0.4, Op.MULT_BASE);
        StateModifier spd = new StateModifier("movement_speed", 0.2, Op.MULT_TOTAL);
        StateModifier flatAtk = new StateModifier("attack_damage", 3, Op.ADD);
        StateModifier hp = new StateModifier("max_health", 10, Op.ADD);
        StateModifier scale = new StateModifier("scale", 1.0, Op.MULT_BASE);
        TransformSpec ki = new TransformSpec(TransformState.BOOSTED, Set.of(TransformState.BASE), 1, List.of(), 100, 0, 0, false, true, false, 0, List.of(atk, spd, flatAtk, hp, scale));
        TransformSpec notKi = new TransformSpec(TransformState.DIVINE, Set.of(TransformState.BASE), 1, List.of(), 100, 0, 0, false, false, false, 0, List.of(atk, spd));
        assertEquals(ki.modifiers(), Coexistence.effective(ki, false, 0.5));           // alone: full strength
        List<StateModifier> both = Coexistence.effective(ki, true, 0.5);
        assertEquals(0.2, both.get(0).amount(), 1e-9);                                 // attack multiplier halved
        assertEquals(0.1, both.get(1).amount(), 1e-9);                                 // speed multiplier halved
        assertEquals(3.0, both.get(2).amount(), 1e-9);                                 // flat attack untouched
        assertEquals(10.0, both.get(3).amount(), 1e-9);
        assertEquals(1.0, both.get(4).amount(), 1e-9);                                 // scale untouched
        assertEquals(notKi.modifiers(), Coexistence.effective(notKi, true, 0.5));      // a non-ki state ignores Kaioken
        assertEquals(0.0, Coexistence.effective(ki, true, -3).get(0).amount(), 1e-9);  // the factor is clamped to 0..1
        assertEquals(0.4, Coexistence.effective(ki, true, 9).get(0).amount(), 1e-9);
    }

    @Test
    void zenkaiGrowsOnlyFromSurvivedNearDeathAtARateAndUpToTheTierCap() {
        Zenkai.State s = Zenkai.State.NONE;
        assertSame(s, Zenkai.onSurvivedHit(s, 0.9, 100000, 1));          // a scratch
        assertSame(s, Zenkai.onSurvivedHit(s, 0.0, 100000, 1));          // dead: no gain
        Zenkai.State one = Zenkai.onSurvivedHit(s, 0.2, 100000, 1);
        assertEquals(1, one.stacks());
        assertSame(one, Zenkai.onSurvivedHit(one, 0.1, 100000 + Zenkai.COOLDOWN_TICKS - 1, 1));   // too soon
        Zenkai.State two = Zenkai.onSurvivedHit(one, 0.1, 100000 + Zenkai.COOLDOWN_TICKS, 1);
        assertEquals(2, two.stacks());
        Zenkai.State at = two;
        long t = 200000;
        for (int i = 0; i < 10; i++) { at = Zenkai.onSurvivedHit(at, 0.1, t, 1); t += Zenkai.COOLDOWN_TICKS; }
        assertEquals(Zenkai.cap(1), at.stacks());                         // tier 1 holds three
        assertEquals(Zenkai.cap(4), 12);
        assertEquals(3, Zenkai.clampTo(new Zenkai.State(9, 0), 1).stacks());
        assertEquals(0.09, Zenkai.attackBonus(3), 1e-9);
        assertEquals(6.0, Zenkai.healthBonus(3), 1e-9);
        assertEquals(0.0, Zenkai.attackBonus(-4), 1e-9);
    }
}
