package com.newuniverse.nusmp.core.magic.profile;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SafeRunnerTest {
    /** Fresh state per test (the test classes are also run by a plain runner that shares one instance). */
    private static final class Fx {
        final List<String> log = new ArrayList<>();
        final FaultLedger ledger = new FaultLedger(100, 1200, log::add);
        final SafeRunner runner = new SafeRunner(ledger);
    }

    @Test
    void aCastThatWorksPassesItsResultThrough() {
        Fx f = new Fx();
        List<String> log = f.log; FaultLedger ledger = f.ledger; SafeRunner runner = f.runner;
        assertTrue(runner.run("FLAME", 10, "cast", () -> true, () -> fail("no fallback")));
        assertFalse(runner.run("FLAME", 10, "cast", () -> false, () -> fail("no fallback")), "a cast that returns false stays false (no cooldown)");
        assertEquals(0, ledger.count("FLAME"));
        assertTrue(log.isEmpty());
    }

    @Test
    void aThrowingCastIsCaughtFallsBackAndIsLoggedOnce() {
        Fx f = new Fx();
        List<String> log = f.log; FaultLedger ledger = f.ledger; SafeRunner runner = f.runner;
        AtomicInteger fallbacks = new AtomicInteger();
        for (int i = 0; i < 5; i++)
            assertTrue(runner.run("NEWMAGIC", 100L * i, "cast", () -> { throw new IllegalStateException("boom"); }, fallbacks::incrementAndGet),
                    "the cast counts as spent so the player is not stuck");
        assertEquals(5, fallbacks.get(), "the generic effect plays every time");
        assertEquals(5, ledger.count("newmagic"), "ids are canonical");
        assertEquals(1, log.size(), "one log line per magic id, however many faults");
        assertTrue(log.get(0).contains("NEWMAGIC") && log.get(0).contains("boom"));
    }

    @Test
    void theCooldownDoublesAndIsCapped() {
        Fx f = new Fx();
        List<String> log = f.log; FaultLedger ledger = f.ledger; SafeRunner runner = f.runner;
        assertEquals(100, ledger.record("A", 0, "x", new RuntimeException()));
        assertEquals(200, ledger.record("A", 0, "x", new RuntimeException()));
        assertEquals(400, ledger.record("A", 0, "x", new RuntimeException()));
        for (int i = 0; i < 20; i++) ledger.record("A", 0, "x", new RuntimeException());
        assertEquals(1200, ledger.record("A", 0, "x", new RuntimeException()), "never above the cap");
        assertTrue(ledger.blocked("A", 1199));
        assertFalse(ledger.blocked("A", 1200));
        assertEquals(1000, ledger.ticksLeft("A", 200));
        assertEquals(0, ledger.ticksLeft("B", 0));
    }

    @Test
    void blockedAndClear() {
        Fx f = new Fx();
        List<String> log = f.log; FaultLedger ledger = f.ledger; SafeRunner runner = f.runner;
        runner.run("X", 50, "cast", () -> { throw new RuntimeException(); }, null);
        assertTrue(ledger.blocked("X", 100));
        assertFalse(ledger.blocked("X", 150));
        assertFalse(ledger.blocked("Y", 100));
        ledger.clear("X");
        assertFalse(ledger.blocked("X", 100));
        assertEquals(0, ledger.count("X"));
        assertTrue(ledger.faulted().isEmpty());
    }

    @Test
    void aBrokenFallbackIsCaughtToo() {
        Fx f = new Fx();
        List<String> log = f.log; FaultLedger ledger = f.ledger; SafeRunner runner = f.runner;
        assertTrue(runner.run("Z", 0, "cast", () -> { throw new RuntimeException("first"); }, () -> { throw new RuntimeException("second"); }));
        assertEquals(2, ledger.count("Z"), "the fallback fault is counted");
    }

    @Test
    void linkageErrorsAreCaughtButVmErrorsAreNot() {
        Fx f = new Fx();
        List<String> log = f.log; FaultLedger ledger = f.ledger; SafeRunner runner = f.runner;
        assertTrue(runner.run("L", 0, "cast", () -> { throw new NoClassDefFoundError("gone"); }, null));
        assertTrue(runner.run("S", 0, "cast", () -> { throw new StackOverflowError(); }, null));
        try {
            runner.run("O", 0, "cast", () -> { throw new OutOfMemoryError(); }, null);
            fail("an out-of-memory error must not be swallowed");
        } catch (OutOfMemoryError expected) { /* good */ }
    }

    @Test
    void guardReportsAFaultWithoutThrowing() {
        Fx f = new Fx();
        List<String> log = f.log; FaultLedger ledger = f.ledger; SafeRunner runner = f.runner;
        assertTrue(runner.guard("tick", 0, "tick", () -> {}));
        assertFalse(runner.guard("tick", 0, "tick", () -> { throw new NullPointerException(); }));
        assertFalse(runner.guard("tick", 0, "tick", () -> { throw new NullPointerException(); }));
        assertEquals(1, log.size());
        assertEquals(2, ledger.count("tick"));
    }

    @Test
    void nullAndBlankIdsAreHandled() {
        Fx f = new Fx();
        List<String> log = f.log; FaultLedger ledger = f.ledger; SafeRunner runner = f.runner;
        assertTrue(runner.run(null, 0, "cast", () -> { throw new RuntimeException(); }, null));
        assertTrue(runner.run("", 0, "cast", () -> { throw new RuntimeException(); }, null));
        assertEquals(2, ledger.count(""), "both land on the empty id");
    }
}
