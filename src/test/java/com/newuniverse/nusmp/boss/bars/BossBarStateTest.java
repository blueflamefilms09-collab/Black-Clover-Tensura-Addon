package com.newuniverse.nusmp.boss.bars;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the state packet is the contract between the server's hub and the client's bar, and the diff is what keeps it small. The byte layout is pinned by a round trip through plain data
 * streams (the game's {@code FriendlyByteBuf} sits behind the same two small interfaces, and the self-test {@code boss.bars} repeats the round trip with the real codec), the clamps by feeding
 * the constructor nonsense, and the diff by the cases that must send (a phase change, a new lair action, a move that started) and the ones that must not (a countdown that merely ran down).
 */
class BossBarStateTest {
    private static final UUID BAR = UUID.fromString("00000000-0000-0000-0000-00000000b0b0");

    private static BossBarState sample() {
        return new BossBarState(BAR, 77, "Quillothe the Hollow Crown", BarStyleIds.FALLEN_KING, "Fallen King of the Shattered Hall", "The Hollow Court", 2, 3, List.of(66, 33), 35,
                BossBarState.INVULNERABLE | BossBarState.ENRAGED, "judgment_rings", 90, 100, 4, "Radiant Nova", 45, 3);
    }

    private static byte[] write(BossBarState s) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream d = new DataOutputStream(bos);
        s.write(new BossBarState.Out() {
            @Override public void writeByte(int v) { io(() -> d.writeByte(v)); }
            @Override public void writeShort(int v) { io(() -> d.writeShort(v)); }
            @Override public void writeInt(int v) { io(() -> d.writeInt(v)); }
            @Override public void writeLong(long v) { io(() -> d.writeLong(v)); }
            @Override public void writeUtf(String t, int max) { assertTrue(t.length() <= max, "text longer than its limit: " + t); io(() -> d.writeUTF(t)); }
        });
        return bos.toByteArray();
    }

    private static BossBarState read(byte[] bytes) {
        DataInputStream d = new DataInputStream(new ByteArrayInputStream(bytes));
        return BossBarState.read(new BossBarState.In() {
            @Override public int readByte() { return ioGet(d::readByte); }
            @Override public int readShort() { return ioGet(d::readShort); }
            @Override public int readInt() { return ioGet(d::readInt); }
            @Override public long readLong() { return ioGet(d::readLong); }
            @Override public String readUtf(int max) { return ioGet(d::readUTF); }
        });
    }

    interface IoRun { void run() throws IOException; }

    interface IoGet<T> { T get() throws IOException; }

    private static void io(IoRun r) {
        try { r.run(); } catch (IOException e) { throw new UncheckedIOException(e); }
    }

    private static <T> T ioGet(IoGet<T> g) {
        try { return g.get(); } catch (IOException e) { throw new UncheckedIOException(e); }
    }

    @Test
    void theStateSurvivesAByteRoundTrip() {
        BossBarState s = sample();
        assertEquals(s, read(write(s)));
        BossBarState quiet = new BossBarState(BAR, -1, "", BarStyleIds.HOLY, "", "", 1, 1, List.of(), 0, 0, "", -1, 0, -1, "", -1, -1);
        assertEquals(quiet, read(write(quiet)));
        assertEquals(-1, read(write(quiet)).castPercent());
        assertEquals(-1, read(write(quiet)).adds());
        assertEquals(-1, read(write(quiet)).fighters());
        assertFalse(read(write(quiet)).lairPending());
    }

    @Test
    void theConstructorClampsNonsense() {
        BossBarState s = new BossBarState(BAR, 5, null, null, null, null, 99, 0, List.of(0, 150, 60, 50, 40, 30, 20, 10, 5), 400, 0, null, 5, -9, 2, null, 250, 999);
        assertEquals(1, s.phaseCount());
        assertEquals(1, s.phase());
        assertEquals(BossBarState.MAX_THRESHOLDS, s.thresholds().size(), "at most six notches, none at 0 or 100");
        assertEquals(List.of(60, 50, 40, 30, 20, 10), s.thresholds());
        assertEquals(100, s.shield());
        assertEquals("", s.lairId());
        assertEquals(-1, s.lairTicks(), "no lair id, no countdown");
        assertEquals(0, s.lairTotal());
        assertEquals(100, s.castPercent());
        assertEquals(120, s.fighters());
        assertFalse(s.casting(), "no name, no cast bar");
    }

    @Test
    void aPhaseOrANewMoveIsAChangeAndACountdownIsNot() {
        BossBarState a = sample();
        assertTrue(BossBarState.differs(null, 0, a, 0));
        assertFalse(BossBarState.differs(a, 100, a, 100), "the same state sampled again");
        BossBarState later = new BossBarState(BAR, 77, a.name(), a.style(), a.title(), a.phaseName(), 2, 3, a.thresholds(), 35, a.flags(), "judgment_rings", 86, 100, 4, "Radiant Nova", 46, 3);
        assertFalse(BossBarState.differs(a, 100, later, 104), "the lair countdown ran down by the 4 ticks that passed, the wind-up moved one percent");
        BossBarState phase3 = new BossBarState(BAR, 77, a.name(), a.style(), a.title(), a.phaseName(), 3, 3, a.thresholds(), 35, a.flags(), "judgment_rings", 86, 100, 4, "Radiant Nova", 46, 3);
        assertTrue(BossBarState.differs(a, 100, phase3, 104), "a new phase");
        BossBarState moved = new BossBarState(BAR, 77, a.name(), a.style(), a.title(), a.phaseName(), 2, 3, a.thresholds(), 35, a.flags(), "judgment_rings", 86, 100, 4, "Radiant Nova", 51, 3);
        assertTrue(BossBarState.differs(a, 100, moved, 104), "the wind-up moved five percent");
        BossBarState other = new BossBarState(BAR, 77, a.name(), a.style(), a.title(), a.phaseName(), 2, 3, a.thresholds(), 35, a.flags(), "falling_stars", 86, 100, 4, "Radiant Nova", 46, 3);
        assertTrue(BossBarState.differs(a, 100, other, 104), "another lair action");
        BossBarState retimed = new BossBarState(BAR, 77, a.name(), a.style(), a.title(), a.phaseName(), 2, 3, a.thresholds(), 35, a.flags(), "judgment_rings", 40, 100, 4, "Radiant Nova", 46, 3);
        assertTrue(BossBarState.differs(a, 100, retimed, 104), "the lair action was pushed back or forward");
        BossBarState dead = new BossBarState(BAR, 77, a.name(), a.style(), a.title(), a.phaseName(), 2, 3, a.thresholds(), 35, a.flags() | BossBarState.DEAD, "judgment_rings", 86, 100, 4, "Radiant Nova", 46, 3);
        assertTrue(BossBarState.differs(a, 100, dead, 104), "the boss died");
        BossBarState wound = new BossBarState(BAR, 77, a.name(), a.style(), a.title(), a.phaseName(), 2, 3, a.thresholds(), 35, a.flags(), "judgment_rings", 86, 100, 4, "", -1, 3);
        assertTrue(BossBarState.differs(a, 100, wound, 104), "the wind-up ended");
    }

    @Test
    void theFlagsReadBack() {
        BossBarState s = sample();
        assertTrue(s.invulnerable());
        assertTrue(s.enraged());
        assertFalse(s.healing());
        assertFalse(s.dead());
        assertTrue(s.casting());
        assertTrue(s.lairPending());
    }
}
