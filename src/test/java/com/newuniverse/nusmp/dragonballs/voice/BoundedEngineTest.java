package com.newuniverse.nusmp.dragonballs.voice;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class BoundedEngineTest {
    private static final short[] LOUD = new short[320];

    static {
        Arrays.fill(LOUD, (short) 8000);
    }

    private static void speak(SpeechEngine e, UUID id) {
        for (int i = 0; i < 12; i++) e.feed(id, LOUD, 0.25f);
        e.endTransmission(id);
    }

    private static void waitReady(SpeechEngine e) {
        long until = System.currentTimeMillis() + 3000;
        while (!e.available() && System.currentTimeMillis() < until) Thread.onSpinWait();
        assertTrue(e.available());
    }

    @Test
    void anUtteranceBecomesOneText() throws Exception {
        CountDownLatch got = new CountDownLatch(1);
        UUID id = UUID.randomUUID();
        BoundedEngine e = new BoundedEngine(() -> pcm -> "hello", (p, t) -> { assertEquals(id, p); assertEquals("hello", t); got.countDown(); });
        try {
            waitReady(e);
            speak(e, id);
            assertTrue(got.await(3, TimeUnit.SECONDS));
        } finally { e.close(); }
    }

    @Test
    void aMissingRecogniserLeavesTheEngineInert() throws Exception {
        BoundedEngine e = new BoundedEngine(() -> null, (p, t) -> fail("no text can come"));
        try {
            Thread.sleep(200);
            assertFalse(e.available());
            e.feed(UUID.randomUUID(), LOUD, 0.25f);                  // must not throw
            e.endTransmission(UUID.randomUUID());
            e.forget(UUID.randomUUID());
        } finally { e.close(); }
    }

    @Test
    void aFullQueueDropsAndCounts() throws Exception {
        CountDownLatch release = new CountDownLatch(1), started = new CountDownLatch(1);
        AtomicInteger heard = new AtomicInteger();
        BoundedEngine e = new BoundedEngine(() -> pcm -> {
            started.countDown();
            try { release.await(5, TimeUnit.SECONDS); } catch (InterruptedException x) { Thread.currentThread().interrupt(); }
            return "x";
        }, (p, t) -> heard.incrementAndGet());
        try {
            waitReady(e);
            UUID a = UUID.randomUUID();
            speak(e, a);                                           // the worker takes this one and blocks
            assertTrue(started.await(3, TimeUnit.SECONDS));
            for (int i = 0; i < 10; i++) speak(e, a);             // one player: at most 4 may wait (one runs, so 3 more)
            assertTrue(e.dropped() >= 6, "dropped " + e.dropped());
            for (int i = 0; i < 20; i++) speak(e, UUID.randomUUID());   // many players: the queue cap (8) holds
            assertTrue(e.dropped() >= 6 + 20 - BoundedEngine.QUEUE_CAP, "dropped " + e.dropped());
        } finally {
            release.countDown();
            e.close();
        }
    }

    @Test
    void aFailingRecogniserDoesNotKillTheWorker() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch got = new CountDownLatch(1);
        BoundedEngine e = new BoundedEngine(() -> pcm -> {
            if (calls.getAndIncrement() == 0) throw new IllegalStateException("boom");
            return "ok";
        }, (p, t) -> got.countDown());
        try {
            waitReady(e);
            UUID id = UUID.randomUUID();
            speak(e, id);
            speak(e, id);
            assertTrue(got.await(3, TimeUnit.SECONDS));
        } finally { e.close(); }
    }

    @Test
    void voskResultTextIsReadWithoutUnk() {
        StringBuilder b = new StringBuilder();
        VoskEngine.append(b, "{\"text\" : \"come [unk] forth\"}");
        VoskEngine.append(b, "{\"text\" : \"\"}");
        VoskEngine.append(b, "{\"text\" : \"shenron\"}");
        assertEquals("come forth shenron", b.toString());
    }
}
