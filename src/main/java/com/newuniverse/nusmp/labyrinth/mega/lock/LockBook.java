package com.newuniverse.nusmp.labyrinth.mega.lock;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.Instance;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.MechanicsDirector;
import com.newuniverse.nusmp.labyrinth.mega.system.SystemSync;
import com.newuniverse.nusmp.labyrinth.mega.system.ToastKind;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Why: this is the engine of the owner's second request, "remove the random being locked in a room or add a way to get out". Everything that can close on a player registers
 * here as a {@link Lockable} (a trial chamber keeps itself in the book while it is sealed; the book finds the sealing mechanics by itself, once a second, from the mechanics
 * Director), and once a second the book asks the pure rules ({@link EscapeRules#decide}) what to do with each: stay closed, show the release rune, or OPEN. It tells the
 * prisoners when the seal closes (with the time until the rune) and when it opens (with the reason), places and removes the rune ({@link LockRunes}), and answers the rune click
 * and the command {@code /dungeon release}. A lock that is not kept for three seconds (its chunk went away, its block entity was removed) is dropped and its rune is swept.
 * The clock is a parameter ({@link #tick(ServerLevel, long)}), so the self-test drives a whole seal from closing to opening without waiting ten real minutes. With
 * {@code lock.enabled = false} the book does nothing and the dungeon is as it was before. Server thread only.
 */
public final class LockBook {
    private LockBook() {}

    private static final Logger LOG = LogUtils.getLogger();
    /** A lock that has not been kept for this many ticks is gone. */
    static final int STALE_TICKS = 60;

    /** What the book remembers about one lock. */
    public static final class Entry {
        public final Lockable lock;
        long lastKept;
        boolean toldSealed, toldRune, runeReady;
        long firstSeen;
        EscapeRules.Facts facts;
        EscapeRules.Decision decision = new EscapeRules.Decision(EscapeRules.Action.HOLD, EscapeRules.Why.NONE);

        Entry(Lockable lock, long now) {
            this.lock = lock;
            this.lastKept = now;
            this.firstSeen = now;
        }

        public boolean runeReady() { return runeReady; }

        public EscapeRules.Facts facts() { return facts; }

        public EscapeRules.Decision decision() { return decision; }
    }

    private static final Map<String, Entry> LIVE = new LinkedHashMap<>();
    private static final Map<String, MechLock> MECH = new HashMap<>();
    private static final Map<UUID, Long> LAST_USE = new HashMap<>();

    // ================================================================ registering a lock
    /** The owner of a lock calls this every time it sees its lock closed (the trial chamber on every one of its own ticks). Cheap. */
    public static void keep(Lockable lock, long now) {
        if (lock == null || !LockConfig.enabled()) return;
        Entry e = LIVE.get(lock.lockKey());
        if (e == null) LIVE.put(lock.lockKey(), new Entry(lock, now));
        else {
            e.lastKept = now;
            if (e.lock != lock) LIVE.put(lock.lockKey(), new Entry(lock, now));          // a block entity that was reloaded is a new object with the same key
        }
    }

    /** The owner calls this when the lock opened by its own rules (the wave is dead, the puzzle solved): the entry and the rune go. */
    public static void drop(ServerLevel level, String key) {
        LIVE.remove(key);
        MECH.remove(key.startsWith("mech:") ? key.substring(5) : key);
        if (level != null) LockRunes.remove(level, key);
    }

    public static boolean isLive(String key) { return LIVE.containsKey(key); }

    public static int liveCount() { return LIVE.size(); }

    public static Set<String> liveKeys() { return new HashSet<>(LIVE.keySet()); }

    public static Entry entry(String key) { return LIVE.get(key); }

    // ================================================================ the clock
    public static void onServerTick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 3) return;
        ServerLevel level = MegaDungeon.level(server);
        if (level == null) return;
        try {
            tick(level, level.getGameTime());
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] The escape rules failed: {}", e.toString());
        }
    }

    /** One look at every lock, at time {@code now}. Public for the self-test, which passes its own clock. */
    public static void tick(ServerLevel level, long now) {
        if (!LockConfig.enabled()) {
            if (!LIVE.isEmpty() || LockRunes.count() > 0) clear(level);
            return;
        }
        if (LockConfig.get(LockConfig.MECHANIC_RULES)) syncMechanics(level, now);
        for (Entry e : new ArrayList<>(LIVE.values())) {
            if (e.lock.lockLevel() != level) continue;
            if (now - e.lastKept > STALE_TICKS) {
                LIVE.remove(e.lock.lockKey());
                LockRunes.remove(level, e.lock.lockKey());
                continue;
            }
            try {
                evaluate(level, e, now);
            } catch (RuntimeException ex) {
                LOG.warn("[nusmp] The escape rules failed for {}: {}", e.lock.lockKey(), ex.toString());
            }
        }
        if (now % 100 < 20) LockRunes.sweep(level, LIVE.keySet());
    }

    private static void syncMechanics(ServerLevel level, long now) {
        Set<String> seen = new HashSet<>();
        for (Instance in : MechanicsDirector.active()) {
            if (in.level != level || !(in.sealsPlayers() || in.holdsObstacles())) continue;
            MechLock ml = MECH.computeIfAbsent(in.key, k -> new MechLock(in, now));
            if (ml.instance() != in) {
                ml = new MechLock(in, now);
                MECH.put(in.key, ml);
            }
            seen.add(in.key);
            keep(ml, now);
        }
        for (String k : new ArrayList<>(MECH.keySet())) {
            if (seen.contains(k)) continue;
            MECH.remove(k);
            LIVE.remove("mech:" + k);
            LockRunes.remove(level, "mech:" + k);
        }
    }

    // ================================================================ one lock
    private static void evaluate(ServerLevel level, Entry e, long now) {
        Lockable l = e.lock;
        List<ServerPlayer> prisoners = l.prisoners();
        EscapeRules.Facts f = l.facts(now);
        EscapeRules.Config cfg = l.config(LockConfig.escape());
        EscapeRules.Decision d = EscapeRules.decide(f, cfg);
        boolean soft = l.soft();
        if (soft && d.action() == EscapeRules.Action.SHOW_RUNE) d = new EscapeRules.Decision(EscapeRules.Action.HOLD, EscapeRules.Why.NONE);      // an obstacle is not a seal: no rune
        e.facts = f;
        e.decision = d;
        boolean boss = l.lockKind() == EscapeRules.Kind.BOSS;

        if (!soft && !e.toldSealed && !prisoners.isEmpty()) {
            e.toldSealed = true;
            long secs = EscapeRules.secondsUntilRune(f, cfg);
            Component msg = boss ? LockMsg.text(LockText.SEALED_BOSS) : LockMsg.text(LockText.SEALED, secs);
            for (ServerPlayer p : prisoners) {
                LockMsg.tell(p, msg.copy().withStyle(ChatFormatting.AQUA));
                if (!boss) SystemSync.toast(p, ToastKind.INFO, LockText.TOAST_SEALED, Long.toString(secs));
            }
        }
        if (d.release()) {
            open(level, e, null, d.why(), prisoners);
            return;
        }
        e.runeReady = d.rune();
        if (d.rune() && LockConfig.get(LockConfig.RUNES)) {
            LockRunes.ensure(level, l.lockKey(), l.runePos(), boss);
            if (!e.toldRune && !prisoners.isEmpty()) {
                e.toldRune = true;
                for (ServerPlayer p : prisoners) {
                    if (!boss) LockMsg.tell(p, LockMsg.text(LockText.RUNE_SHOWN).withStyle(ChatFormatting.GOLD));
                    SystemSync.toast(p, ToastKind.REWARD, LockText.TOAST_RUNE);
                }
            }
        }
    }

    private static void open(ServerLevel level, Entry e, ServerPlayer by, EscapeRules.Why why, List<ServerPlayer> prisoners) {
        Lockable l = e.lock;
        LIVE.remove(l.lockKey());
        LockRunes.remove(level, l.lockKey());
        List<ServerPlayer> told = new ArrayList<>(prisoners);
        try {
            l.open(by, why);
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] Opening {} failed: {}", l.lockKey(), ex.toString());
        }
        Component msg = whyText(why, by);
        for (ServerPlayer p : told) {
            LockMsg.tell(p, msg.copy().withStyle(ChatFormatting.GREEN));
            SystemSync.toast(p, ToastKind.SUCCESS, LockText.TOAST_OPEN);
        }
        LOG.info("[nusmp] Lock {} opened ({}){}", l.lockKey(), why, by == null ? "" : " by " + by.getGameProfile().getName());
    }

    private static Component whyText(EscapeRules.Why why, ServerPlayer by) {
        return switch (why) {
            case ABSENT -> LockMsg.text(LockText.OPEN_ABSENT);
            case BOSS_DEAD -> LockMsg.text(LockText.OPEN_BOSS_DEAD);
            case MAX_TIME -> LockMsg.text(LockText.OPEN_MAX_TIME);
            case UNSOLVABLE -> LockMsg.text(LockText.OPEN_UNSOLVABLE);
            case RESCUE -> LockMsg.text(LockText.OPEN_RESCUE);
            case IDLE, TIMEOUT, BOSS_FIGHT -> by == null ? LockMsg.text(LockText.OPEN_ADMIN) : LockMsg.text(LockText.OPEN_RUNE, by.getDisplayName());
            case NONE -> by == null ? LockMsg.text(LockText.OPEN_ADMIN) : LockMsg.text(LockText.OPEN_COMMAND, by.getDisplayName());
        };
    }

    // ================================================================ the player's side
    /** The locks whose prisoners include this player. */
    public static List<Entry> around(ServerPlayer p) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : LIVE.values()) {
            if (e.lock.lockLevel() != p.level() || e.lock.soft()) continue;
            for (ServerPlayer q : e.lock.prisoners()) if (q == p || q.getUUID().equals(p.getUUID())) {
                out.add(e);
                break;
            }
        }
        return out;
    }

    /** Is this player inside a closed lock? (Then {@code /dungeon unstuck} is free and has no cooldown.) */
    public static boolean isSealed(ServerPlayer p) { return !around(p).isEmpty(); }

    /** A click on a rune (or the command with the key of the lock the player stands in). */
    public static void useRune(ServerPlayer p, String key) {
        long now = p.serverLevel().getGameTime();
        Long last = LAST_USE.get(p.getUUID());
        if (last != null && now - last < 5) return;                                   // one click arrives as an interact and an interact-at: count it once
        LAST_USE.put(p.getUUID(), now);
        Entry e = LIVE.get(key);
        ServerLevel level = p.serverLevel();
        if (e == null) {
            LockRunes.remove(level, key);                                              // a rune whose lock is gone
            LockMsg.tell(p, LockMsg.text(LockText.RELEASE_NONE).withStyle(ChatFormatting.GRAY));
            return;
        }
        release(level, e, p, EscapeRules.Why.IDLE, true);
    }

    /** {@code /dungeon release}: opens the lock the player stands in, if its rune is ready. Returns the number of locks opened. */
    public static int releaseFor(ServerPlayer p) {
        List<Entry> mine = around(p);
        if (mine.isEmpty()) {
            LockMsg.tell(p, LockMsg.text(LockText.RELEASE_NONE).withStyle(ChatFormatting.GRAY));
            return 0;
        }
        int n = 0;
        for (Entry e : mine) if (release(p.serverLevel(), e, p, EscapeRules.Why.NONE, true)) n++;
        return n;
    }

    private static boolean release(ServerLevel level, Entry e, ServerPlayer by, EscapeRules.Why why, boolean tellWhenNotReady) {
        if (!e.runeReady) {
            if (tellWhenNotReady) {
                long cfgRune = e.facts == null ? 0 : EscapeRules.secondsUntilRune(e.facts, e.lock.config(LockConfig.escape()));
                long cfgOpen = e.facts == null ? 0 : EscapeRules.secondsUntilRelease(e.facts, e.lock.config(LockConfig.escape()));
                LockMsg.tell(by, LockMsg.text(LockText.RELEASE_WAIT, cfgRune, cfgOpen).withStyle(ChatFormatting.YELLOW));
            }
            return false;
        }
        open(level, e, by, why, e.lock.prisoners());
        return true;
    }

    /** An operator opens every lock around a player (or every lock when {@code p} is null), whatever the timers say. Returns how many. */
    public static int forceOpen(ServerLevel level, ServerPlayer p) {
        int n = 0;
        for (Entry e : new ArrayList<>(LIVE.values())) {
            if (e.lock.lockLevel() != level) continue;
            if (p != null) {
                boolean mine = false;
                for (ServerPlayer q : e.lock.prisoners()) if (q.getUUID().equals(p.getUUID())) mine = true;
                if (!mine) continue;
            }
            open(level, e, null, EscapeRules.Why.NONE, e.lock.prisoners());
            n++;
        }
        return n;
    }

    /** The status lines for a player: free, or what holds them and for how long. */
    public static List<Component> statusFor(ServerPlayer p) {
        List<Component> out = new ArrayList<>();
        List<Entry> mine = around(p);
        if (mine.isEmpty()) {
            out.add(LockMsg.text(LockText.STATUS_FREE).withStyle(ChatFormatting.GRAY));
            return out;
        }
        long now = p.serverLevel().getGameTime();
        for (Entry e : mine) {
            EscapeRules.Facts f = e.facts != null ? e.facts : e.lock.facts(now);
            EscapeRules.Config cfg = e.lock.config(LockConfig.escape());
            out.add(LockMsg.text(LockText.STATUS_SEALED, e.lock.lockName(), f.sealedTicks() / 20L, EscapeRules.secondsUntilRune(f, cfg), EscapeRules.secondsUntilRelease(f, cfg))
                    .withStyle(ChatFormatting.YELLOW));
            if (e.runeReady) out.add(LockMsg.text(LockText.STATUS_RUNE_READY).withStyle(ChatFormatting.GOLD));
        }
        return out;
    }

    /** The key of the first lock a player is inside, or null. */
    public static String keyAround(ServerPlayer p) {
        List<Entry> mine = around(p);
        return mine.isEmpty() ? null : mine.get(0).lock.lockKey();
    }

    // ================================================================ lifecycle
    public static void forget(UUID id) { LAST_USE.remove(id); }

    public static void clear(ServerLevel level) {
        if (level != null) for (String k : new ArrayList<>(LIVE.keySet())) LockRunes.remove(level, k);
        LIVE.clear();
        MECH.clear();
        LockRunes.clear();
    }

    public static void clearAll() {
        LIVE.clear();
        MECH.clear();
        LAST_USE.clear();
        LockRunes.clear();
    }
}
