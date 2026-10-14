package com.newuniverse.nusmp.charter;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** The world's drop lock: the first claim wins and nothing in the public surface can undo it. */
class DropLedgerTest {
    private static final UUID A = new UUID(1, 1), B = new UUID(2, 2);

    @Test
    void firstWins() {
        DropLedger l = new DropLedger();
        assertFalse(l.hasDropped("dante"));
        assertTrue(l.markDropped("dante", A, "Ayla", 3));
        assertFalse(l.markDropped("dante", B, "Bram", 4), "two kills in one tick: the second finds the lock set");
        assertTrue(l.hasDropped("dante"));
        assertEquals(A, l.claim("dante").id());
        assertEquals("Ayla", l.claim("dante").name());
        assertEquals(3L, l.claim("dante").day());
        assertFalse(l.hasDropped("vanica"), "the other grimoires are untouched");
        assertTrue(l.markDropped("vanica", B, "Bram", 4));
        assertEquals(2, l.all().size());
    }

    @Test
    void aSavedClaimIsNeverOverwrittenByALoad() {
        DropLedger l = new DropLedger();
        l.markDropped("zenon", A, "Ayla", 1);
        l.restore("zenon", B, "Bram", 9);
        assertEquals(A, l.claim("zenon").id());
    }

    @Test
    void theViewIsReadOnly() {
        DropLedger l = new DropLedger();
        l.markDropped("lucius", A, "Ayla", 1);
        assertThrows(UnsupportedOperationException.class, () -> l.all().remove("lucius"));
        assertTrue(l.hasDropped("lucius"));
    }

    @Test
    void nothingPublicCanClearAClaim() {
        for (Method m : DropLedger.class.getDeclaredMethods()) {
            String n = m.getName().toLowerCase();
            if (n.contains("clear") || n.contains("remove") || n.contains("reset") || n.contains("unset")) assertFalse(Modifier.isPublic(m.getModifiers()), m.getName() + " must not be public");
        }
        // the admin door exists, and is package-private
        try {
            Method m = DropLedger.class.getDeclaredMethod("adminClear", String.class);
            assertFalse(Modifier.isPublic(m.getModifiers()));
            assertFalse(Modifier.isPrivate(m.getModifiers()));
        } catch (NoSuchMethodException e) {
            fail("the operator command needs adminClear");
        }
    }

    @Test
    void anAdminClearOpensTheLockAgain() {
        DropLedger l = new DropLedger();
        l.markDropped("goliath", A, "Ayla", 1);
        assertNull(l.adminClear("charybdis_spawn"));
        assertEquals(A, l.adminClear("goliath").id());
        assertFalse(l.hasDropped("goliath"));
        assertTrue(l.markDropped("goliath", B, "Bram", 2));
    }

    @Test
    void racingClaimsHaveExactlyOneWinnerAndTheViewIsASnapshot() throws Exception {
        DropLedger l = new DropLedger();
        java.util.concurrent.atomic.AtomicInteger wins = new java.util.concurrent.atomic.AtomicInteger();
        Thread[] ts = new Thread[8];
        for (int i = 0; i < ts.length; i++) {
            final int n = i;
            ts[i] = new Thread(() -> { if (l.markDropped("lucius", new UUID(n, n), "P" + n, n)) wins.incrementAndGet(); });
        }
        for (Thread t : ts) t.start();
        for (Thread t : ts) t.join();
        assertEquals(1, wins.get());
        java.util.Map<String, DropLedger.Claim> view = l.all();
        l.markDropped("dante", A, "Ayla", 1);
        assertFalse(view.containsKey("dante"), "an earlier view does not change under a reader");
        assertThrows(UnsupportedOperationException.class, () -> view.put("x", null));
    }
}
