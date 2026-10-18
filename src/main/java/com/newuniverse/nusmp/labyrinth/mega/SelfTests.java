package com.newuniverse.nusmp.labyrinth.mega;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: nothing here can be played by the author, so each system of the Mega-Dungeon registers a SELF-TEST that the CI server smoke test
 * (tools/ci/server_smoke.py) runs in the real game, with a {@link FakePlayer} standing in for a person: rest at a marked seat, the Dungeon Master's
 * roll, a turf war, the Guardian and the Mana Core opening the sector Gatekeeper, the Crucible's single pick. A test is a function that reads the
 * generated sector 0 (the smoke script creates it first with {@code /multiverse mega expand}), drives the system through its OWN public entry
 * points (the same ones the event handlers call, advancing time by calling the tick methods in a loop), asserts what must hold, and returns a
 * one-line result. The registry catches every exception, so a broken system reports FAIL with the reason instead of stopping the run, and the
 * command {@code /multiverse mega selftest [key]} prints one line per test in a fixed format the script parses. Tests may change the world (they
 * run on a throw-away CI world); they must not depend on each other's side effects except through {@link MegaData}.
 */
public final class SelfTests {
    private SelfTests() {}

    /** The outcome of one test: pass or fail, with a short human detail (never multi-line). */
    public record Result(boolean ok, String detail) {
        public static Result ok(String detail) { return new Result(true, detail); }

        public static Result fail(String detail) { return new Result(false, detail); }
    }

    /** One self-test. Throw freely: {@link #run} turns an exception into a failed result. */
    @FunctionalInterface
    public interface Test {
        Result run(MinecraftServer server) throws Exception;
    }

    private static final Map<String, Test> TESTS = new LinkedHashMap<>();

    /** Registers a test under a key such as {@code safe.rest}, from a registry's {@code init} (once; a repeated key replaces the old test). */
    public static synchronized void register(String key, Test test) {
        if (key != null && test != null) TESTS.put(key, test);
    }

    public static synchronized List<String> keys() { return new ArrayList<>(TESTS.keySet()); }

    /** Runs one test; never throws. An unknown key is a failure. */
    public static Result run(String key, MinecraftServer server) {
        Test t;
        synchronized (SelfTests.class) { t = TESTS.get(key); }
        if (t == null) return Result.fail("no such self-test");
        try {
            Result r = t.run(server);
            return r == null ? Result.fail("the test returned nothing") : r;
        } catch (Throwable e) {
            StackTraceElement[] st = e.getStackTrace();
            String where = st.length > 0 ? " at " + st[0].getClassName().substring(st[0].getClassName().lastIndexOf('.') + 1) + "." + st[0].getMethodName() + ":" + st[0].getLineNumber() : "";
            return Result.fail(e.getClass().getSimpleName() + ": " + e.getMessage() + where);
        }
    }

    /** The Mega-Dungeon's level, or null (then a test should FAIL with "no mega dimension"). */
    public static ServerLevel mega(MinecraftServer server) { return MegaDungeon.level(server); }

    /**
     * A fake player (a real {@code ServerPlayer} with no connection, survival, at the given position) with a STABLE profile per name, in the given
     * level. It is the stand-in for a person in a self-test. It cannot change dimension (no connection): create it IN the level the test needs.
     */
    public static FakePlayer player(ServerLevel level, String name, double x, double y, double z) {
        UUID id = UUID.nameUUIDFromBytes(("nusmp-selftest:" + name).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        FakePlayer p = FakePlayerFactory.get(level, new GameProfile(id, name));
        p.setPos(x, y, z);
        p.setHealth(p.getMaxHealth());
        return p;
    }

    /**
     * 1.62.0: like {@link #player} but a {@link StandIn}: it answers {@code position()}, {@code blockPosition()} and the bounding box with the place it was put at, where a plain
     * {@code FakePlayer} answers with the origin. Use it when the test asks something about the neighbourhood of the stand-in (a technique's circle, a shop's reach, a rewind).
     */
    public static FakePlayer standIn(ServerLevel level, String name, double x, double y, double z) {
        UUID id = UUID.nameUUIDFromBytes(("nusmp-selftest:" + name).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StandIn p = StandIn.get(level, new GameProfile(id, name));
        p.setPos(x, y, z);
        p.setHealth(p.getMaxHealth());
        return p;
    }
}
