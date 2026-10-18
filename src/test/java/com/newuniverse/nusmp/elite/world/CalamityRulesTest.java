package com.newuniverse.nusmp.elite.world;

import com.google.gson.JsonParser;
import com.newuniverse.nusmp.elite.world.rules.CalamityDef;
import com.newuniverse.nusmp.elite.world.rules.CalamityDirector;
import com.newuniverse.nusmp.elite.world.rules.CalamityPhases;
import com.newuniverse.nusmp.elite.world.rules.MoveScheduler;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** The calamity data, the director's triggers, the phase rules and the move scheduler. */
class CalamityRulesTest {
    private static final String OW = "minecraft:overworld";
    private static final UUID P = UUID.nameUUIDFromBytes("p".getBytes(StandardCharsets.UTF_8));

    private static CalamityDef shipped(String name) throws Exception {
        try (var in = CalamityRulesTest.class.getResourceAsStream("/data/nusmp/elite/world/calamities/" + name + ".json")) {
            assertNotNull(in, name + " is shipped");
            CalamityDef d = CalamityDef.parse(JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject());
            assertNotNull(d, name + " parses");
            return d;
        }
    }

    private static CalamityDef def(String json) { return CalamityDef.parse(JsonParser.parseString(json).getAsJsonObject()); }

    private static CalamityDirector.Observation obs(double density, int rocks, boolean night, int moon, long time) {
        return new CalamityDirector.Observation(P, OW, density, rocks, night, moon, time);
    }

    // ---------------------------------------------------------------- data

    @Test
    void shippedCalamitiesParse() throws Exception {
        CalamityDef c = shipped("gravebound_colossus");
        assertEquals("nusmp:gravebound_colossus", c.entity());
        assertEquals(List.of(0.66, 0.33), c.phases());
        assertEquals(0x8B7B5A, c.color());
        assertTrue(c.hasTrigger(CalamityDef.Type.SUSTAINED_MAGICULE));
        assertTrue(c.hasTrigger(CalamityDef.Type.MOTHER_ROCKS));
        assertTrue(c.hasTrigger(CalamityDef.Type.RITUAL));
        assertTrue(c.hasTrigger(CalamityDef.Type.COMMAND));
        assertTrue(c.allowsDimension(OW));
        assertFalse(c.allowsDimension("nusmp:mega_dungeon"));
        CalamityDef l = shipped("lich_calamity");
        assertEquals("nusmp:lich_boss", l.entity());
        assertTrue(l.hasTrigger(CalamityDef.Type.NIGHT));
        assertEquals(4, l.triggers().get(0).moonPhase());
    }

    @Test
    void badFilesAreRejectedNotFatal() {
        assertNull(CalamityDef.parse(JsonParser.parseString("{}").getAsJsonObject()));
        assertNull(CalamityDef.parse(JsonParser.parseString("{\"id\":\"Bad Id\",\"entity\":\"a:b\"}").getAsJsonObject()));
        CalamityDef d = def("{\"id\":\"x\",\"entity\":\"a:b\",\"triggers\":[{\"type\":\"nonsense\"},{\"type\":\"command\"}],\"phases\":[0.3,0.7],\"color\":\"zzz\"}");
        assertEquals(1, d.triggers().size(), "an unknown trigger type is skipped");
        assertEquals(List.of(0.7, 0.3), d.phases(), "phases are sorted descending");
        assertEquals(0x8B7B5A, d.color(), "a bad colour falls back");
        assertTrue(d.enabled());
        assertEquals(1, d.weight());
    }

    // ---------------------------------------------------------------- director

    @Test
    void sustainedMagiculeNeedsTheFullTimeAndDrainsWhenItBreaks() {
        CalamityDef d = def("{\"id\":\"a\",\"entity\":\"e:e\",\"cooldownMinutes\":0,\"triggers\":[{\"type\":\"sustained_magicule\",\"density\":0.9,\"seconds\":100}]}");
        CalamityDirector dir = new CalamityDirector();
        Random r = new Random(1);
        long t = 100000;
        for (int i = 0; i < 19; i++) assertTrue(dir.check(List.of(d), obs(0.95, 0, false, 0, t), 100, false, id -> -999999999L, r).isEmpty(), "tick " + i);
        assertEquals(1900, dir.timer(P, "a", 0));
        Optional<CalamityDirector.Decision> dec = dir.check(List.of(d), obs(0.95, 0, false, 0, t), 100, false, id -> -999999999L, r);
        assertTrue(dec.isPresent(), "100 seconds is 2000 ticks: the 20th check of 100 ticks reaches it");
        assertEquals("a", dec.get().calamityId());
        assertEquals(CalamityDef.Type.SUSTAINED_MAGICULE, dec.get().reason());
        // breaking the condition drains twice as fast
        dir.check(List.of(d), obs(0.10, 0, false, 0, t), 100, false, id -> -999999999L, r);
        assertEquals(2000 - 200, dir.timer(P, "a", 0));
        dir.reset(P);
        assertEquals(0, dir.timer(P, "a", 0));
    }

    @Test
    void busyOrCoolingDownOrWrongDimensionNeverWakes() {
        CalamityDef d = def("{\"id\":\"a\",\"entity\":\"e:e\",\"cooldownMinutes\":10,\"dimensions\":[\"minecraft:overworld\"],\"triggers\":[{\"type\":\"sustained_magicule\",\"density\":0.5,\"seconds\":1}]}");
        CalamityDirector dir = new CalamityDirector();
        Random r = new Random(2);
        long now = 1_000_000;
        assertTrue(dir.check(List.of(d), obs(1.0, 0, false, 0, now), 100, true, id -> -999999999L, r).isEmpty(), "busy");
        assertTrue(dir.check(List.of(d), obs(1.0, 0, false, 0, now), 100, false, id -> now - 100, r).isEmpty(), "woke 100 ticks ago, cooldown is 12000");
        assertTrue(dir.check(List.of(d), new CalamityDirector.Observation(P, "nusmp:mega_dungeon", 1.0, 0, false, 0, now), 100, false, id -> -999999999L, r).isEmpty(), "wrong dimension");
        assertTrue(dir.check(List.of(d), obs(1.0, 0, false, 0, now), 100, false, id -> -999999999L, r).isPresent(), "everything fine");
    }

    @Test
    void disabledCalamitiesAndRitualOnlyOnesNeverWakeByThemselves() {
        CalamityDef off = def("{\"id\":\"off\",\"entity\":\"e:e\",\"enabled\":false,\"cooldownMinutes\":0,\"triggers\":[{\"type\":\"sustained_magicule\",\"density\":0.1,\"seconds\":0}]}");
        CalamityDef ritual = def("{\"id\":\"rit\",\"entity\":\"e:e\",\"cooldownMinutes\":0,\"triggers\":[{\"type\":\"ritual\"},{\"type\":\"command\"}]}");
        CalamityDirector dir = new CalamityDirector();
        for (int i = 0; i < 50; i++) assertTrue(dir.check(List.of(off, ritual), obs(1.0, 9, true, 4, 1_000_000), 100, false, id -> -999999999L, new Random(i)).isEmpty());
    }

    @Test
    void motherRocksNeedTheCountTheDensityAndTheTime() {
        CalamityDef d = def("{\"id\":\"m\",\"entity\":\"e:e\",\"cooldownMinutes\":0,\"triggers\":[{\"type\":\"mother_rocks\",\"count\":3,\"density\":0.7,\"seconds\":10}]}");
        CalamityDirector dir = new CalamityDirector();
        Random r = new Random(3);
        for (int i = 0; i < 5; i++) assertTrue(dir.check(List.of(d), obs(0.9, 2, false, 0, 1_000_000), 100, false, id -> -999999999L, r).isEmpty(), "two rocks are not three");
        for (int i = 0; i < 5; i++) assertTrue(dir.check(List.of(d), obs(0.5, 3, false, 0, 1_000_000), 100, false, id -> -999999999L, r).isEmpty(), "too thin");
        boolean woke = false;
        for (int i = 0; i < 4; i++) woke |= dir.check(List.of(d), obs(0.9, 3, false, 0, 1_000_000), 100, false, id -> -999999999L, r).isPresent();
        assertTrue(woke, "three rocks in a dense chunk for long enough");
    }

    @Test
    void nightNeedsNightMoonRocksAndTheRoll() {
        CalamityDef d = def("{\"id\":\"n\",\"entity\":\"e:e\",\"cooldownMinutes\":0,\"triggers\":[{\"type\":\"night\",\"density\":0.8,\"rocks\":1,\"moonPhase\":4,\"chance\":1.0}]}");
        CalamityDirector dir = new CalamityDirector();
        Random r = new Random(4);
        assertTrue(dir.check(List.of(d), obs(0.9, 1, false, 4, 1_000_000), 100, false, id -> -999999999L, r).isEmpty(), "daytime");
        assertTrue(dir.check(List.of(d), obs(0.9, 1, true, 3, 1_000_000), 100, false, id -> -999999999L, r).isEmpty(), "wrong moon");
        assertTrue(dir.check(List.of(d), obs(0.9, 0, true, 4, 1_000_000), 100, false, id -> -999999999L, r).isEmpty(), "no rock");
        assertTrue(dir.check(List.of(d), obs(0.5, 1, true, 4, 1_000_000), 100, false, id -> -999999999L, r).isEmpty(), "too thin");
        assertTrue(dir.check(List.of(d), obs(0.9, 1, true, 4, 1_000_000), 100, false, id -> -999999999L, r).isPresent());
        CalamityDef rare = def("{\"id\":\"n\",\"entity\":\"e:e\",\"cooldownMinutes\":0,\"triggers\":[{\"type\":\"night\",\"density\":0.8,\"rocks\":1,\"chance\":0.0}]}");
        assertTrue(dir.check(List.of(rare), obs(0.9, 1, true, 4, 1_000_000), 100, false, id -> -999999999L, r).isEmpty(), "chance 0 never");
    }

    @Test
    void theChoiceIsWeightedAndSeeded() {
        String a = "{\"id\":\"a\",\"entity\":\"e:e\",\"weight\":9,\"cooldownMinutes\":0,\"triggers\":[{\"type\":\"sustained_magicule\",\"density\":0.1,\"seconds\":0}]}";
        String b = "{\"id\":\"b\",\"entity\":\"e:e\",\"weight\":1,\"cooldownMinutes\":0,\"triggers\":[{\"type\":\"sustained_magicule\",\"density\":0.1,\"seconds\":0}]}";
        List<CalamityDef> defs = List.of(def(a), def(b));
        Map<String, Integer> count = new HashMap<>();
        Random r = new Random(7);
        CalamityDirector dir = new CalamityDirector();
        for (int i = 0; i < 1000; i++) {
            var dec = dir.check(defs, obs(1.0, 0, false, 0, 1_000_000), 100, false, id -> -999999999L, r);
            assertTrue(dec.isPresent());
            count.merge(dec.get().calamityId(), 1, Integer::sum);
        }
        assertTrue(count.get("a") > 800 && count.get("a") < 960, "about 90%: " + count);
        assertTrue(count.get("b") > 40, "the light one still happens: " + count);
    }

    // ---------------------------------------------------------------- phases

    @Test
    void phaseFollowsHealthAndThresholds() {
        List<Double> th = List.of(0.66, 0.33);
        assertEquals(0, CalamityPhases.phaseFor(1.0, th));
        assertEquals(0, CalamityPhases.phaseFor(0.67, th));
        assertEquals(1, CalamityPhases.phaseFor(0.66, th));
        assertEquals(1, CalamityPhases.phaseFor(0.5, th));
        assertEquals(2, CalamityPhases.phaseFor(0.33, th));
        assertEquals(2, CalamityPhases.phaseFor(0.01, th));
        assertEquals(2, CalamityPhases.last(th));
        assertEquals(0, CalamityPhases.phaseFor(0.1, List.of()), "a boss without thresholds has one phase");
    }

    @Test
    void aBlowCanNeverSkipAPhase() {
        List<Double> th = List.of(0.66, 0.33);
        assertEquals(100f, CalamityPhases.clampDamage(1000f, 1000f, 0, th, 100f), 1e-3);
        assertEquals(340f, CalamityPhases.clampDamage(1000f, 1000f, 0, th, 9999f), 1e-3, "stops on the 66% line");
        assertEquals(0f, CalamityPhases.clampDamage(660f, 1000f, 0, th, 50f), 1e-3, "already on the line: nothing until it changes phase");
        assertEquals(330f, CalamityPhases.clampDamage(660f, 1000f, 1, th, 9999f), 1e-3, "phase 1 stops on the 33% line");
        assertEquals(9999f, CalamityPhases.clampDamage(330f, 1000f, 2, th, 9999f), 1e-3, "the last phase is not cut");
        assertEquals(0f, CalamityPhases.clampDamage(500f, 1000f, 1, th, -4f), 1e-3);
        assertEquals(0f, CalamityPhases.clampDamage(500f, 0f, 1, th, 4f), 1e-3);
        assertEquals(-1.0, CalamityPhases.nextThreshold(2, th), 1e-9);
    }

    @Test
    void eachPhaseTightensTheBoss() {
        CalamityPhases.Tuning a = CalamityPhases.tuning(0, 3), b = CalamityPhases.tuning(1, 3), c = CalamityPhases.tuning(2, 3);
        assertEquals(1.0, a.cooldownMult(), 1e-9);
        assertTrue(b.cooldownMult() < a.cooldownMult() && c.cooldownMult() < b.cooldownMult());
        assertTrue(c.cooldownMult() >= 0.55 - 1e-9, "never under 55%");
        assertTrue(c.damageMult() <= 1.6 + 1e-9);
        assertTrue(c.damageMult() > b.damageMult() && b.damageMult() > a.damageMult());
        assertTrue(c.addCap() > a.addCap());
        assertEquals(1.0, CalamityPhases.tuning(0, 1).cooldownMult(), 1e-9, "a one phase boss does not divide by zero");
    }

    // ---------------------------------------------------------------- scheduler

    private static final List<MoveScheduler.Move> MOVES = List.of(
            new MoveScheduler.Move("swing", 0, 20, 10, 40, 0, 6, 5),
            new MoveScheduler.Move("slam", 0, 30, 12, 100, 0, 10, 3),
            new MoveScheduler.Move("raise", 1, 40, 12, 400, 0, 40, 2),
            new MoveScheduler.Move("rend", 2, 40, 14, 200, 3, 12, 2));

    @Test
    void movesAreFilteredByPhaseRangeAndCooldown() {
        Map<String, Long> ready = new HashMap<>();
        assertEquals(2, MoveScheduler.candidates(MOVES, 0, 4, ready, 0).size(), "swing and slam");
        assertEquals(1, MoveScheduler.candidates(MOVES, 0, 8, ready, 0).size(), "swing out of range");
        assertEquals(3, MoveScheduler.candidates(MOVES, 1, 4, ready, 0).size());
        assertEquals(4, MoveScheduler.candidates(MOVES, 2, 4, ready, 0).size());
        assertEquals(3, MoveScheduler.candidates(MOVES, 2, 1, ready, 0).size(), "rend needs 3+ blocks");
        ready.put("swing", 100L);
        assertEquals(1, MoveScheduler.candidates(MOVES, 0, 4, ready, 50).size(), "swing is cooling down");
        assertEquals(2, MoveScheduler.candidates(MOVES, 0, 4, ready, 100).size());
    }

    @Test
    void nothingToUseGivesNullAndTheLastMoveIsSkippedWhenPossible() {
        Random r = new Random(5);
        assertNull(MoveScheduler.choose(MOVES, 0, 50, new HashMap<>(), 0, null, r));
        for (int i = 0; i < 100; i++) assertNotEquals("swing", MoveScheduler.choose(MOVES, 0, 4, new HashMap<>(), 0, "swing", r).id());
        Map<String, Long> only = new HashMap<>();
        only.put("slam", 999L);
        assertEquals("swing", MoveScheduler.choose(MOVES, 0, 4, only, 0, "swing", r).id(), "if the last move is the only one it is still used");
    }

    @Test
    void cooldownScalesWithThePhaseButNotBelowTen() {
        MoveScheduler.Move m = MOVES.get(1);
        assertEquals(100, MoveScheduler.scaledCooldown(m, CalamityPhases.tuning(0, 3)));
        assertTrue(MoveScheduler.scaledCooldown(m, CalamityPhases.tuning(2, 3)) < 100);
        assertEquals(10, MoveScheduler.scaledCooldown(new MoveScheduler.Move("q", 0, 1, 1, 1, 0, 1, 1), CalamityPhases.tuning(2, 3)));
    }
}
