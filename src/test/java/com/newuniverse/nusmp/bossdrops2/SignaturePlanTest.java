package com.newuniverse.nusmp.bossdrops2;

import com.google.gson.JsonParser;
import com.newuniverse.nusmp.bossdrops2.OneOfOne.Outcome;
import com.newuniverse.nusmp.bossdrops2.SignaturePlan.Award;
import com.newuniverse.nusmp.bossdrops2.SignaturePlan.Kind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why (docs/boss_drops_worthwhile.md): the fighters, the one-of-one, the personal rolls and the piece cap of a kill, as plain data. Pure. */
class SignaturePlanTest {
    private static UUID id(int n) { return new UUID(0L, n); }

    private static SignatureTable.Table table(int extras, String relic, double echo) {
        StringBuilder rows = new StringBuilder();
        for (int i = 0; i < extras; i++) rows.append(i == 0 ? "" : ",").append("{\"item\":\"minecraft:diamond\",\"chance\":1.0,\"min\":2,\"max\":4}");
        String boon = "[{\"effect\":\"minecraft:strength\",\"seconds\":60,\"amplifier\":0}]";
        String json = "{\"boss\":\"x\",\"tier\":\"high\",\"signature\":{\"chance\":1.0,\"min\":1,\"max\":1,\"boon\":" + boon + "},\"extras\":[" + rows + "]"
                + (relic == null ? "" : ",\"one_of_one\":{\"relic\":\"" + relic + "\",\"echo_chance\":" + echo + ",\"echo_boon\":" + boon + "}") + "}";
        return SignatureTable.parse(JsonParser.parseString(json).getAsJsonObject());
    }

    private static long count(List<Award> a, Kind k) { return a.stream().filter(x -> x.kind() == k).count(); }

    // ---------------------------------------------------------------- the fighters
    @Test
    void theTopDamagerComesFirstAndTheKillerWinsATie() {
        Map<UUID, Double> d = new HashMap<>();
        d.put(id(1), 50.0);
        d.put(id(2), 90.0);
        d.put(id(3), 90.0);
        List<Fighter> f = SignaturePlan.select(d, List.of(), id(3), 1000.0);
        assertEquals(id(3), f.get(0).id(), "equal damage: the last blow is first");
        assertEquals(id(2), f.get(1).id());
        assertEquals(id(1), f.get(2).id());
    }

    @Test
    void someoneWhoBarelyTouchedTheBossIsNotAFighter() {
        Map<UUID, Double> d = new HashMap<>();
        d.put(id(1), 500.0);
        d.put(id(2), 5.0);
        List<Fighter> f = SignaturePlan.select(d, List.of(), id(1), 1000.0);
        assertEquals(1, f.size(), "5 of 1000 is under the 2 percent share");
    }

    @Test
    void theKillerIsKeptEvenWithoutDamageAndWithNoLedger() {
        List<Fighter> f = SignaturePlan.select(Map.of(), List.of(), id(7), 1000.0);
        assertEquals(1, f.size());
        assertEquals(id(7), f.get(0).id());
        assertTrue(f.get(0).killer());
        assertTrue(SignaturePlan.select(Map.of(), List.of(), null, 1000.0).isEmpty(), "no killer, no damage: nobody to pay");
    }

    @Test
    void presentFightersJoinWithZeroDamageBehindTheDamagers() {
        List<Fighter> f = SignaturePlan.select(Map.of(id(1), 100.0), List.of(id(2), id(1)), id(1), 1000.0);
        assertEquals(List.of(id(1), id(2)), f.stream().map(Fighter::id).toList());
        assertEquals(0.0, f.get(1).damage());
    }

    @Test
    void noMoreThanTheFighterCapAndTheKillerIsNeverCut() {
        Map<UUID, Double> d = new HashMap<>();
        for (int i = 1; i <= 12; i++) d.put(id(i), 100.0 + i);
        List<Fighter> f = SignaturePlan.select(d, List.of(), id(1), 1000.0);
        assertEquals(SignaturePlan.MAX_FIGHTERS, f.size());
        assertTrue(f.stream().anyMatch(x -> x.id().equals(id(1))), "the killer had the lowest damage and is still paid");
        assertEquals(id(12), f.get(0).id());
        assertEquals(new HashSet<>(f).size(), f.size());
    }

    @Test
    void aBrokenMaxHealthFallsBackToTheAbsoluteFloor() {
        assertEquals(1, SignaturePlan.select(Map.of(id(1), 2.0), List.of(), null, Double.NaN).size());
        assertEquals(0, SignaturePlan.select(Map.of(id(1), 0.5), List.of(), null, 0.0).size());
    }

    // ---------------------------------------------------------------- the pieces
    @Test
    void theFirstKillIssuesTheUniqueToTheTopFighterOnly() {
        List<Fighter> f = List.of(new Fighter(id(1), 90, false), new Fighter(id(2), 60, true));
        List<Award> a = SignaturePlan.plan(table(2, "relic_a", 0.0), f, Outcome.ISSUED, () -> 0.5);
        assertEquals(1, count(a, Kind.UNIQUE));
        assertEquals(id(1), a.stream().filter(x -> x.kind() == Kind.UNIQUE).findFirst().orElseThrow().player());
        assertEquals(0, count(a, Kind.ECHO), "echo chance 0: nobody else gets an echo");
    }

    @Test
    void aSecondKillPaysTheTopFighterAnEchoAndNoUnique() {
        List<Fighter> f = List.of(new Fighter(id(1), 90, true), new Fighter(id(2), 60, false));
        List<Award> a = SignaturePlan.plan(table(0, "relic_a", 0.0), f, Outcome.EXISTS, () -> 0.99);
        assertEquals(0, count(a, Kind.UNIQUE));
        assertEquals(1, count(a, Kind.ECHO));
        assertEquals(id(1), a.stream().filter(x -> x.kind() == Kind.ECHO).findFirst().orElseThrow().player());
    }

    @Test
    void otherFightersRollTheEchoChanceThemselves() {
        List<Fighter> f = List.of(new Fighter(id(1), 90, true), new Fighter(id(2), 60, false), new Fighter(id(3), 30, false));
        List<Award> win = SignaturePlan.plan(table(0, "relic_a", 0.2), f, Outcome.ISSUED, () -> 0.0);
        assertEquals(2, count(win, Kind.ECHO), "dice 0.0 is under 0.2: both other fighters get an echo");
        List<Award> miss = SignaturePlan.plan(table(0, "relic_a", 0.2), f, Outcome.ISSUED, () -> 0.9);
        assertEquals(0, count(miss, Kind.ECHO));
    }

    @Test
    void noOneOfOneMeansNoUniqueNoEchoEvenIfTheOutcomeSaysSo() {
        List<Fighter> f = List.of(new Fighter(id(1), 90, true));
        List<Award> a = SignaturePlan.plan(table(1, null, 0.0), f, Outcome.NONE, () -> 0.0);
        assertEquals(0, count(a, Kind.UNIQUE) + count(a, Kind.ECHO));
        assertEquals(1, count(a, Kind.SIGNATURE));
        assertEquals(1, count(a, Kind.EXTRA));
    }

    @Test
    void everyFighterRollsTheirOwnSignatureAndExtras() {
        List<Fighter> f = List.of(new Fighter(id(1), 90, true), new Fighter(id(2), 60, false), new Fighter(id(3), 30, false));
        List<Award> a = SignaturePlan.plan(table(1, null, 0.0), f, Outcome.NONE, () -> 0.0);
        for (int i = 1; i <= 3; i++) {
            final int n = i;
            assertEquals(1, a.stream().filter(x -> x.player().equals(id(n)) && x.kind() == Kind.SIGNATURE).count());
            assertEquals(1, a.stream().filter(x -> x.player().equals(id(n)) && x.kind() == Kind.EXTRA).count());
        }
    }

    @Test
    void aFailedChanceRollPaysNothingForThatRow() {
        SignatureTable.Table t = SignatureTable.parse(JsonParser.parseString("{\"boss\":\"x\",\"tier\":\"mid\",\"signature\":{\"chance\":0.3,\"boon\":[{\"effect\":\"minecraft:speed\",\"seconds\":60}]},"
                + "\"extras\":[{\"item\":\"minecraft:diamond\",\"chance\":0.3}]}").getAsJsonObject());
        List<Award> a = SignaturePlan.plan(t, List.of(new Fighter(id(1), 10, true)), Outcome.NONE, () -> 0.9);
        assertTrue(a.isEmpty());
    }

    @Test
    void countsStayInsideTheirRange() {
        List<Fighter> f = List.of(new Fighter(id(1), 90, true));
        Random r = new Random(7);
        for (int i = 0; i < 2000; i++) {
            for (Award x : SignaturePlan.plan(table(1, null, 0.0), f, Outcome.NONE, r::nextDouble)) {
                if (x.kind() == Kind.EXTRA) assertTrue(x.count() >= 2 && x.count() <= 4, "count " + x.count());
            }
        }
        for (double edge : new double[]{0.0, 0.999999999, 1.0 - 1e-16}) {
            for (Award x : SignaturePlan.plan(table(1, null, 0.0), f, Outcome.NONE, () -> edge)) if (x.kind() == Kind.EXTRA) assertTrue(x.count() >= 2 && x.count() <= 4);
        }
    }

    @Test
    void aKillNeverPaysMoreThanTheCapEvenWithEveryRowAndEveryEchoRolled() {
        List<Fighter> f = new ArrayList<>();
        for (int i = 1; i <= SignaturePlan.MAX_FIGHTERS; i++) f.add(new Fighter(id(i), 100 - i, i == 1));
        List<Award> a = SignaturePlan.plan(table(SignatureTable.MAX_EXTRAS, "relic_a", 1.0), f, Outcome.ISSUED, () -> 0.0);
        assertEquals(SignaturePlan.MAX_PIECES, a.size());
        assertEquals(1, count(a, Kind.UNIQUE), "the unique is reserved before everything else");
        assertEquals(SignaturePlan.MAX_FIGHTERS, count(a, Kind.SIGNATURE), "every fighter keeps their signature");
    }

    @Test
    void extrasAreDealtRoundByRoundInDamageOrder() {
        List<Fighter> f = List.of(new Fighter(id(1), 90, true), new Fighter(id(2), 60, false), new Fighter(id(3), 30, false));
        List<Award> a = SignaturePlan.plan(table(SignatureTable.MAX_EXTRAS, "relic_a", 1.0), f, Outcome.ISSUED, () -> 0.0);
        assertEquals(SignaturePlan.MAX_PIECES, a.size());
        List<Award> extras = a.stream().filter(x -> x.kind() == Kind.EXTRA).toList();
        assertEquals(id(1), extras.get(0).player());
        assertEquals(id(2), extras.get(1).player());
        assertEquals(id(3), extras.get(2).player());
        assertEquals(id(1), extras.get(3).player(), "the second round starts with the top fighter again");
    }

    @Test
    void randomKillsAlwaysStayUnderTheCapAndGiveTheUniqueOnce() {
        Random r = new Random(11);
        for (int n = 0; n < 3000; n++) {
            int people = 1 + r.nextInt(8);
            Map<UUID, Double> d = new HashMap<>();
            for (int i = 1; i <= people; i++) d.put(id(i), r.nextDouble() * 500);
            List<Fighter> f = SignaturePlan.select(d, List.of(), id(1 + r.nextInt(people)), 800.0);
            if (f.isEmpty()) continue;
            Outcome o = Outcome.values()[r.nextInt(3)];
            List<Award> a = SignaturePlan.plan(table(r.nextInt(SignatureTable.MAX_EXTRAS + 1), "relic_a", r.nextDouble()), f, o, r::nextDouble);
            assertTrue(a.size() <= SignaturePlan.MAX_PIECES);
            assertTrue(count(a, Kind.UNIQUE) <= 1);
            assertEquals(o == Outcome.ISSUED ? 1 : 0, count(a, Kind.UNIQUE));
            for (Award x : a) assertTrue(f.stream().anyMatch(q -> q.id().equals(x.player())), "only fighters are paid");
        }
    }

    @Test
    void nothingIsPaidWithoutFightersOrATable() {
        assertTrue(SignaturePlan.plan(table(1, null, 0), List.of(), Outcome.NONE, () -> 0.0).isEmpty());
        assertTrue(SignaturePlan.plan(null, List.of(new Fighter(id(1), 1, true)), Outcome.NONE, () -> 0.0).isEmpty());
    }

    // ---------------------------------------------------------------- the one-of-one rule
    @Test
    void theOneOfOneIsIssuedOnceAndThenExists() {
        AtomicInteger asked = new AtomicInteger();
        boolean[] live = {false};
        java.util.function.BooleanSupplier issue = () -> {
            asked.incrementAndGet();
            if (live[0]) return false;
            live[0] = true;
            return true;
        };
        assertEquals(Outcome.ISSUED, OneOfOne.resolve(true, issue));
        assertEquals(Outcome.EXISTS, OneOfOne.resolve(true, issue));
        live[0] = false;                                                              // released back into the pool (lost, destroyed)
        assertEquals(Outcome.ISSUED, OneOfOne.resolve(true, issue));
        assertEquals(3, asked.get());
    }

    @Test
    void theLedgerIsNotAskedWhenThereIsNothingToGive() {
        AtomicInteger asked = new AtomicInteger();
        assertEquals(Outcome.NONE, OneOfOne.resolve(false, () -> {
            asked.incrementAndGet();
            return true;
        }));
        assertEquals(Outcome.NONE, OneOfOne.resolve(true, null));
        assertEquals(0, asked.get());
    }

    @Test
    void theLedgerIsAskedExactlyOnceOnAnIssuedKill() {
        AtomicInteger asked = new AtomicInteger();
        OneOfOne.resolve(true, () -> {
            asked.incrementAndGet();
            return true;
        });
        assertEquals(1, asked.get());
        assertFalse(Outcome.valueOf("NONE") == Outcome.ISSUED);
    }
}
