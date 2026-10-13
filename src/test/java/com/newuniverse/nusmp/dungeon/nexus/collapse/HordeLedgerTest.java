package com.newuniverse.nusmp.dungeon.nexus.collapse;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** The books of the horde: every ceiling is held at the moment a creature is counted, whatever the walls hand over and whenever. */
class HordeLedgerTest {
    private static UUID id(long n) { return new UUID(0xABCDL, n); }

    @Test
    void aCreatureIsCountedOnceAndNeverPastAnExitsCeiling() {
        CollapseState s = new CollapseState();
        HordeLedger book = new HordeLedger(s);
        for (int i = 0; i < GuardPlan.PER_EXIT_ALIVE; i++) assertTrue(book.admit(id(i), 0), "creature " + i + " of exit 0");
        assertFalse(book.admit(id(100), 0), "an exit that holds six takes no more");
        assertTrue(book.admit(id(101), 1), "another exit is open");
        assertTrue(book.admit(id(102), GuardPlan.PLAYER), "a player's slot has no exit ceiling here (the plan holds it)");
        assertEquals(GuardPlan.PER_EXIT_ALIVE, book.at(0));
        assertEquals(1, book.at(1));
        assertEquals(1, book.at(GuardPlan.PLAYER));
        assertEquals(GuardPlan.PER_EXIT_ALIVE + 2, book.alive());
        assertEquals(GuardPlan.PER_EXIT_ALIVE + 2, s.spawned, "each admitted creature is one made");
        assertFalse(book.has(id(100)), "the refused one is not on the books");
    }

    @Test
    void theHordeNeverPassesTwentyFourAliveOrFiftySixMade() {
        CollapseState s = new CollapseState();
        HordeLedger book = new HordeLedger(s);
        int slot = 0, made = 0;
        for (int i = 0; i < 40; i++) {
            if (book.at(slot) >= GuardPlan.PER_EXIT_ALIVE) slot++;
            if (book.admit(id(i), slot)) made++;
        }
        assertEquals(GuardPlan.MAX_ALIVE, made, "twenty-four alive and no more");
        assertEquals(GuardPlan.MAX_ALIVE, book.alive());
        assertTrue(book.full());
        assertFalse(book.room(GuardPlan.PLAYER));
        // the made counter is a ceiling of its own: killed creatures still count as made
        CollapseState t = new CollapseState();
        t.spawned = GuardPlan.MAX_SPAWNED - 1;
        HordeLedger other = new HordeLedger(t);
        assertTrue(other.admit(id(1), 0), "the fifty-sixth");
        assertFalse(other.admit(id(2), 1), "the fifty-seventh is refused although nobody is alive");
        assertEquals(GuardPlan.MAX_SPAWNED, t.spawned);
        assertTrue(other.full());
    }

    @Test
    void aCreatureCountedAlreadyIsNotCountedTwice() {
        CollapseState s = new CollapseState();
        HordeLedger book = new HordeLedger(s);
        book.adopt(id(5));
        assertEquals(0, s.spawned, "found again, not made again");
        assertTrue(book.admit(id(5), 2), "the join handler found it first: now its slot is known");
        assertEquals(1, book.alive());
        assertEquals(1, book.at(2));
        assertEquals(0, s.spawned, "and it was not counted as made a second time");
        assertTrue(book.admit(id(5), 2));
        assertEquals(1, book.alive());
    }

    @Test
    void creaturesFoundAgainAfterARestartCountAliveButBelongToNoExit() {
        CollapseState s = new CollapseState();
        HordeLedger book = new HordeLedger(s);
        for (int i = 0; i < 10; i++) book.adopt(id(i));
        assertEquals(10, book.alive());
        assertEquals(0, s.spawned);
        assertEquals(10, book.at(HordeLedger.FOUND));
        assertArrayEquals(new int[]{0, 0, 0}, book.load(3), "they hold no exit");
        for (int i = 10; i < 60; i++) book.adopt(id(i));
        assertEquals(GuardPlan.MAX_ALIVE + 4, book.alive(), "a few more than the ceiling are taken back, no more");
        assertFalse(book.admit(id(1000), 0), "and nothing new while the horde is at the ceiling");
        book.adopt(id(3));
        assertEquals(GuardPlan.MAX_ALIVE + 4, book.alive(), "twice is once");
    }

    @Test
    void whatIsAliveOrOnItsWayToAnExitCountsAgainstIt() {
        CollapseState s = new CollapseState();
        HordeLedger book = new HordeLedger(s);
        book.admit(id(1), 1);
        book.admit(id(2), 1);
        book.admit(id(3), 0);
        book.addRing(new HordeLedger.Ring(1, 5, 5, 5, 100, 2, 0));
        book.addRing(new HordeLedger.Ring(GuardPlan.PLAYER, 5, 5, 5, 100, 2, 0));
        book.queue(new HordeLedger.Job(1, 0, 0, 0, 3, 0));
        book.queue(new HordeLedger.Job(2, 0, 0, 0, 4, 0));
        book.queue(new HordeLedger.Job(GuardPlan.PLAYER, 0, 0, 0, 2, 0));
        book.queue(new HordeLedger.Job(9, 0, 0, 0, 7, 0));
        HordeLedger.Batch batch = new HordeLedger.Batch(1, 500);
        batch.left = 2;
        book.addBatch(batch);
        HordeLedger.Batch stale = new HordeLedger.Batch(0, 500);
        stale.left = -3;
        book.addBatch(stale);
        assertArrayEquals(new int[]{1, 2 + 1 + 3 + 2, 4}, book.load(3), "exit 1: two alive, a ring, a job of three, a batch of two; exit 9 is not an exit of three");
        assertEquals(2 + (3 + 4 + 2 + 7) + 2, book.coming(), "two rings, jobs of 3, 4, 2 and 7, a batch of 2 (a batch that went negative counts none)");
        assertEquals(1 + 2, book.comingPlayer(), "the ring and the job for the player");
        assertEquals(0, book.load(0).length);
        assertEquals(0, book.load(-4).length);
    }

    @Test
    void batchesAreGivenUpWhenTheirTimeIsPast() {
        HordeLedger book = new HordeLedger(new CollapseState());
        HordeLedger.Batch a = new HordeLedger.Batch(0, 100), b = new HordeLedger.Batch(1, 200);
        a.left = 2;
        b.left = 3;
        book.addBatch(a);
        book.addBatch(b);
        assertEquals(5, book.coming());
        book.expire(100);
        assertEquals(5, book.coming(), "a batch is kept up to and including its last tick");
        book.expire(101);
        assertEquals(3, book.coming());
        book.expire(201);
        assertEquals(0, book.coming());
    }

    @Test
    void theSharesOfAWaveComeOutOneAtATimeInOrder() {
        HordeLedger book = new HordeLedger(new CollapseState());
        assertFalse(book.hasJobs());
        assertNull(book.nextJob());
        book.queue(new HordeLedger.Job(0, 1, 2, 3, 2, 4));
        book.queue(new HordeLedger.Job(1, 4, 5, 6, 3, 4));
        assertTrue(book.hasJobs());
        assertEquals(new HordeLedger.Job(0, 1, 2, 3, 2, 4), book.nextJob());
        assertEquals(5 - 2, book.coming(), "what was taken is no longer waiting");
        assertEquals(new HordeLedger.Job(1, 4, 5, 6, 3, 4), book.nextJob());
        assertNull(book.nextJob());
    }

    @Test
    void ringsAreKeptAndGivenBackAsACopy() {
        HordeLedger book = new HordeLedger(new CollapseState());
        HordeLedger.Ring r = new HordeLedger.Ring(0, 1, 2, 3, 50, 2, 0);
        book.addRing(r);
        List<HordeLedger.Ring> copy = book.rings();
        assertEquals(List.of(r), copy);
        copy.clear();
        assertEquals(1, book.rings().size(), "a copy: emptying it changes nothing");
        book.removeRing(r);
        assertTrue(book.rings().isEmpty());
        assertEquals(0, book.coming());
    }

    @Test
    void thoseWhoAreGoneAreForgottenAndTheEndClearsEverything() {
        CollapseState s = new CollapseState();
        HordeLedger book = new HordeLedger(s);
        for (int i = 0; i < 6; i++) book.admit(id(i), i % 3);
        book.retain(u -> u.getLeastSignificantBits() % 2 == 0);
        assertEquals(3, book.alive());
        assertEquals(6, s.spawned, "forgetting the dead does not unmake them: the made counter only goes up");
        book.queue(new HordeLedger.Job(0, 0, 0, 0, 2, 0));
        book.addRing(new HordeLedger.Ring(0, 0, 0, 0, 0, 2, 0));
        book.closeBirths();
        assertEquals(0, book.coming(), "nothing more is born");
        assertEquals(3, book.alive(), "but those alive are still on the books until they are removed");
        book.clear();
        assertEquals(0, book.alive());
        assertEquals(List.of(), book.ids());
    }

    @Test
    void theIdsAreACopy() {
        HordeLedger book = new HordeLedger(new CollapseState());
        book.admit(id(1), 0);
        List<UUID> ids = book.ids();
        book.retain(u -> false);
        assertEquals(1, ids.size(), "the list was taken before");
        assertEquals(0, book.alive());
    }

    // ---------------------------------------------------------------- a whole collapse on the books
    @Test
    void aWholeCollapseOnTheBooksNeverPassesAnyCeilingWhateverTheWallsDoAndWhenever() {
        for (long seed = 0; seed < 300; seed++) {
            Random r = new Random(seed);
            CollapseState s = new CollapseState();
            int exits = r.nextInt(7);
            for (int i = 0; i < exits; i++) s.exits.add(new int[]{i * 10, 0, 0});
            HordeLedger book = new HordeLedger(s);
            List<long[]> comingOut = new ArrayList<>();                   // {tick it comes out at, slot}: wall births that will be handed over
            List<HordeLedger.Batch> batches = new ArrayList<>();
            int next = 0, wave = 0;
            for (long now = 0; now < 3600; now++) {
                if (wave < GuardPlan.WAVES.length && now == GuardPlan.WAVES[wave].at()) {
                    boolean player = r.nextInt(6) != 0;
                    int coming = book.coming();
                    int[] plan = GuardPlan.assign(wave, exits, book.load(exits), player, r.nextInt(3) + book.comingPlayer(), book.alive() + coming, s.spawned + coming);
                    int[] perExit = new int[exits];
                    int perPlayer = 0;
                    for (int slot : plan) {
                        if (slot == GuardPlan.PLAYER) perPlayer++;
                        else perExit[slot]++;
                    }
                    for (int e = 0; e < exits; e++) if (perExit[e] > 0) book.queue(new HordeLedger.Job(e, 0, 0, 0, perExit[e], wave));
                    if (perPlayer > 0) book.queue(new HordeLedger.Job(GuardPlan.PLAYER, 0, 0, 0, perPlayer, wave));
                    assertTrue(book.alive() + book.coming() <= Math.max(GuardPlan.MAX_ALIVE, book.alive()), "planned within MAX_ALIVE: seed " + seed + " wave " + wave);
                    assertTrue(s.spawned + book.coming() <= Math.max(GuardPlan.MAX_SPAWNED, s.spawned), "planned within MAX_SPAWNED: seed " + seed + " wave " + wave);
                    wave++;
                }
                HordeLedger.Job job = book.nextJob();                       // one share a tick, as the walls are asked
                if (job != null) {
                    int got = r.nextInt(job.k() + 1);                        // the walls find places for some
                    HordeLedger.Batch b = new HordeLedger.Batch(job.slot(), now + GuardPlan.TELEGRAPH + 100);
                    b.left = got;
                    if (got > 0) {
                        book.addBatch(b);
                        batches.add(b);
                        for (int k = 0; k < got; k++) comingOut.add(new long[]{now + GuardPlan.TELEGRAPH + r.nextInt(30), job.slot(), batches.size() - 1});
                    }
                    for (int k = got; k < job.k(); k++) book.addRing(new HordeLedger.Ring(job.slot(), 0, 0, 0, now + GuardPlan.TELEGRAPH, 2, 0));
                }
                for (int i = comingOut.size() - 1; i >= 0; i--) {            // wall births that come out now: most do, some never do
                    long[] c = comingOut.get(i);
                    if (c[0] > now) continue;
                    comingOut.remove(i);
                    if (r.nextInt(10) == 0) continue;                         // the player stood there: nothing is born
                    batches.get((int) c[2]).left--;
                    book.admit(id(next++), (int) c[1]);
                }
                for (HordeLedger.Ring ring : book.rings()) {                 // rings that burn down
                    if (ring.due() > now) continue;
                    book.removeRing(ring);
                    if (r.nextInt(4) != 0) book.admit(id(next++), ring.slot());
                }
                book.expire(now);
                if (r.nextInt(12) == 0 && book.alive() > 0) {                // the players kill one
                    List<UUID> all = book.ids();
                    UUID dead = all.get(r.nextInt(all.size()));
                    book.retain(u -> !u.equals(dead));
                }
                assertTrue(book.alive() <= GuardPlan.MAX_ALIVE, "alive " + book.alive() + " at " + now + " seed " + seed);
                assertTrue(s.spawned <= GuardPlan.MAX_SPAWNED, "made " + s.spawned + " at " + now + " seed " + seed);
                for (int e = 0; e < exits; e++) assertTrue(book.at(e) <= GuardPlan.PER_EXIT_ALIVE, "exit " + e + " holds " + book.at(e) + " at " + now + " seed " + seed);
                assertTrue(book.coming() >= 0);
            }
            assertEquals(GuardPlan.WAVES.length, wave, "every wave was planned");
            assertEquals(0, book.coming(), "by the end nothing is still on its way: the books are square, seed " + seed);
        }
    }
}
