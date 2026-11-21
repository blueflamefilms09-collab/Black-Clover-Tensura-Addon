package com.newuniverse.nusmp.dragonballs.voice;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Why: speech recognition is the one expensive thing of the voice feature, so it is boxed in. ONE daemon worker thread does all of it; the voice thread only cuts the stream into utterances ({@link Utterance})
 * and queues finished ones. Bounds: at most {@link Utterance#MAX_QUEUED} waiting utterances per player, at most {@link #QUEUE_CAP} in all, an utterance older than {@link #STALE_MS} when its turn comes is
 * dropped; every drop is counted. A recogniser that fails to load leaves the engine unavailable and everything inert. Cost on the voice thread: an array copy per frame of a listened-to player.
 */
public final class BoundedEngine implements SpeechEngine {
    public static final int QUEUE_CAP = 8, MAX_PLAYERS = 64;
    public static final long STALE_MS = 15_000L;
    private static final Logger LOG = LogUtils.getLogger();

    private record Job(UUID id, short[] pcm, long atMs) {}

    private final Map<UUID, Utterance> utterances = new ConcurrentHashMap<>();
    private final Map<UUID, AtomicInteger> pending = new ConcurrentHashMap<>();
    private final BlockingQueue<Job> queue = new ArrayBlockingQueue<>(QUEUE_CAP);
    private final AtomicLong dropped = new AtomicLong();
    private final Sink sink;
    private final Thread worker;
    private volatile boolean ready, closed;
    private volatile String grammar = "";
    private int errors;

    /** {@code loader} runs on the worker thread (loading a model takes seconds) and returns null when no recogniser can be had. */
    public BoundedEngine(Supplier<Recogniser> loader, Sink sink) {
        this.sink = sink;
        this.worker = new Thread(() -> run(loader), "nusmp-dbvoice-recogniser");
        worker.setDaemon(true);
        worker.setPriority(Thread.NORM_PRIORITY - 1);
        worker.start();
    }

    @Override public boolean available() { return ready && !closed; }

    @Override public long dropped() { return dropped.get(); }

    @Override public void setGrammar(String json) { grammar = json == null ? "" : json; }

    @Override
    public void feed(UUID id, short[] pcm, float rms) {
        if (!ready || closed) return;
        if (utterances.size() >= MAX_PLAYERS) utterances.clear();                    // bounded even if forget() is never called
        Utterance u = utterances.computeIfAbsent(id, k -> new Utterance());
        short[] audio = null;
        synchronized (u) {
            if (u.add(pcm, rms) != Utterance.Action.NONE) audio = u.take();
        }
        if (audio != null) submit(id, audio);
    }

    @Override
    public void endTransmission(UUID id) {
        Utterance u = utterances.get(id);
        if (u == null) return;
        short[] audio = null;
        synchronized (u) {
            if (u.flush() != Utterance.Action.NONE) audio = u.take();
        }
        if (audio != null) submit(id, audio);
    }

    @Override public void forget(UUID id) { utterances.remove(id); }

    private void submit(UUID id, short[] audio) {
        AtomicInteger c = pending.computeIfAbsent(id, k -> new AtomicInteger());
        if (!Utterance.admit(c.get())) { dropped.incrementAndGet(); return; }
        c.incrementAndGet();
        if (!queue.offer(new Job(id, audio, System.currentTimeMillis()))) {
            c.decrementAndGet();
            dropped.incrementAndGet();
        }
    }

    private void run(Supplier<Recogniser> loader) {
        Recogniser rec = null;
        try {
            rec = loader.get();
            if (rec == null) return;
            String applied = "";
            ready = !closed;
            while (!closed) {
                Job j = queue.take();
                try {
                    String g = grammar;
                    if (!g.equals(applied)) { rec.grammar(g); applied = g; }
                    if (System.currentTimeMillis() - j.atMs() > STALE_MS) { dropped.incrementAndGet(); continue; }
                    String text = rec.recognise(j.pcm());
                    if (text != null && !text.isBlank()) sink.heard(j.id(), text);
                } catch (RuntimeException | LinkageError e) {
                    if (errors++ < 3) LOG.warn("[dbvoice] the recogniser failed on one utterance: {}", e.toString());
                } finally {
                    AtomicInteger c = pending.get(j.id());
                    if (c != null) c.decrementAndGet();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("[dbvoice] the recogniser stopped: {}", e.toString());
        } finally {
            ready = false;
            if (rec != null) {
                try { rec.close(); } catch (RuntimeException | LinkageError e) { LOG.debug("[dbvoice] closing the recogniser failed: {}", e.toString()); }
            }
        }
    }

    @Override
    public void close() {
        closed = true;
        ready = false;
        queue.clear();
        utterances.clear();
        worker.interrupt();
    }
}
