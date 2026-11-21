package com.newuniverse.nusmp.arc.goku;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Why: Goku Omega's career is the WORLD's, not a player's and not a static: one small record saved with the overworld ({@code data/nusmp_goku_omega.dat}): his experience, the two clocks that
 * make autonomous training pure arithmetic (the game time of the last credit, and what the day's caps have already given), and a few counters. Every number in it is changed by
 * {@link GokuOmegaMath.Progress}, which is pure and tested; this class only loads, saves and finds it. One row, a few longs: no per-player data at all.
 * <p>Training costs nothing while he waits: {@link #timer} runs once an in-game hour (a modulus per tick, then arithmetic) and {@link #settle} runs at the start of a fight; neither touches an
 * entity, a chunk or a player.
 */
public final class GokuWorld extends SavedData {
    private static final String NAME = "nusmp_goku_omega";

    /** The ticks between two timer checks: one in-game hour. */
    public static final int TIMER_TICKS = (int) GokuOmegaMath.TICKS_PER_HOUR;

    public final GokuOmegaMath.Progress progress = new GokuOmegaMath.Progress();

    public static GokuWorld get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(GokuWorld::new, GokuWorld::load, null), NAME);
    }

    static GokuWorld load(CompoundTag t, HolderLookup.Provider provider) {
        GokuWorld w = new GokuWorld();
        GokuOmegaMath.Progress p = w.progress;
        p.xp = Math.max(0L, Math.min(GokuOmegaMath.xpCeiling(), t.contains("xp") ? t.getLong("xp") : p.xp));
        p.lastTrain = t.contains("lastTrain") ? t.getLong("lastTrain") : -1L;
        p.day = t.contains("day") ? t.getLong("day") : -1L;
        p.trainToday = Math.max(0L, t.getLong("trainToday"));
        p.fightToday = Math.max(0L, t.getLong("fightToday"));
        p.wins = Math.max(0, t.getInt("wins"));
        p.farewells = Math.max(0, t.getInt("farewells"));
        p.survived = Math.max(0, t.getInt("survived"));
        p.kills = Math.max(0, t.getInt("kills"));
        p.seenLevel = t.contains("seenLevel") ? Math.max(1, t.getInt("seenLevel")) : p.seenLevel;
        return w;
    }

    @Override
    public CompoundTag save(CompoundTag t, HolderLookup.Provider provider) {
        GokuOmegaMath.Progress p = progress;
        t.putLong("xp", p.xp);
        t.putLong("lastTrain", p.lastTrain);
        t.putLong("day", p.day);
        t.putLong("trainToday", p.trainToday);
        t.putLong("fightToday", p.fightToday);
        t.putInt("wins", p.wins);
        t.putInt("farewells", p.farewells);
        t.putInt("survived", p.survived);
        t.putInt("kills", p.kills);
        t.putInt("seenLevel", p.seenLevel);
        return t;
    }

    /** His level now (the setting's cap applied). */
    public int level() { return progress.level(GokuBossConfig.omegaLevelCap()); }

    /** The game time of the world (the overworld's), the one clock training counts in. */
    public static long now(MinecraftServer server) { return server.overworld().getGameTime(); }

    /**
     * Credits the training since the last credit, or only moves the clock when training is off or a fight is running. Returns the experience gained. Cheap: arithmetic on one record.
     */
    public long settle(MinecraftServer server, boolean fightRunning) {
        long now = now(server);
        long gained;
        if (fightRunning || !GokuBossConfig.omegaTraining()) {
            progress.rest(now);
            gained = 0L;
        } else {
            gained = progress.train(now, GokuBossConfig.get(GokuBossConfig.OMEGA_XP_PER_HOUR), GokuBossConfig.get(GokuBossConfig.OMEGA_TRAINING_DAILY));
        }
        setDirty();
        return gained;
    }

    /** The slow timer: once per in-game hour, from the server tick. */
    public static void timer(MinecraftServer server) {
        if (!GokuBossConfig.omega() || !GokuBossConfig.enabled()) return;
        boolean fight = false;
        for (GokuBoss b : GokuBoss.live()) if (b.fightActive()) { fight = true; break; }
        get(server).settle(server, fight);
    }
}
