package com.newuniverse.nusmp.boss.bars;

import com.newuniverse.nusmp.boss.bar.BossBarNet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Why: the server half of the custom boss bar. A boss that owns a {@link ServerBossEvent} calls {@link #track} (one line in its tick, where it already refreshes the bar); from then on the hub
 * asks the boss's {@link BossBarSource} for a {@link BossBarState} at most FIVE times a second, sends it to everyone who watches the bar only when it differs from the last one sent
 * ({@link BossBarState#differs}), tells a player who starts watching straight away, sends a final DEAD state when the boss dies (so the client can play the death animation after the vanilla
 * bar is gone) and a {@link BossBarNet.Clear} to everyone told when the boss is removed. The vanilla boss event still carries the name and the health; this is only the extra. The hub does
 * nothing at all while no bar is tracked. Server thread only.
 */
public final class BossBarHub {
    private BossBarHub() {}

    /** The sampling period: every fourth tick = five times a second. */
    public static final int PERIOD = 4;

    private static final class Entry {
        final ServerBossEvent bar;
        final WeakReference<LivingEntity> owner;
        final BossBarSource source;
        BossBarState last;
        long lastTick, nextAt;
        long healUntil;
        float lastHp = 1f;
        boolean dead;
        final Set<UUID> told = new HashSet<>();

        Entry(ServerBossEvent bar, LivingEntity owner, long now) {
            this.bar = bar;
            this.owner = new WeakReference<>(owner);
            this.source = BossBarSources.of(owner);
            this.nextAt = now + Math.floorMod(bar.getId().hashCode(), PERIOD);        // bars are spread over the four ticks of the period
            this.lastHp = owner.getMaxHealth() <= 0f ? 1f : owner.getHealth() / owner.getMaxHealth();
        }
    }

    private static final Map<UUID, Entry> BARS = new HashMap<>();
    private static boolean inited;

    /** Registers the tick, the death listener, the commands and the self-test (called once from {@code BossBarSync.init}). */
    public static void init() {
        if (inited) return;
        inited = true;
        NeoForge.EVENT_BUS.addListener(BossBarHub::onServerTick);
        NeoForge.EVENT_BUS.addListener(BossBarHub::onDeath);
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> BARS.clear());
        NeoForge.EVENT_BUS.addListener(BossBarCommands::register);
        BossBarSelfTests.register();
    }

    /** A boss with a {@code ServerBossEvent}: start (or keep) telling its watchers how to draw the custom bar. Cheap: a map lookup once registered. */
    public static void track(LivingEntity boss, ServerBossEvent bar) {
        if (boss == null || bar == null || boss.level().isClientSide) return;
        if (BARS.get(bar.getId()) == null) {
            long now = boss.level().getServer() == null ? 0L : boss.level().getServer().getTickCount();
            BARS.put(bar.getId(), new Entry(bar, boss, now));
        }
    }

    /** The boss let go of its bar (the caller tells the watchers itself). */
    public static void untrack(ServerBossEvent bar) {
        if (bar != null) BARS.remove(bar.getId());
    }

    public static int tracked() { return BARS.size(); }

    private static final com.newuniverse.nusmp.perf.PerfProbe.Section PROBE = com.newuniverse.nusmp.perf.PerfProbe.section("s.boss.bars_hub");

    private static void onServerTick(ServerTickEvent.Post event) {
        if (BARS.isEmpty()) return;
        long t0 = com.newuniverse.nusmp.perf.PerfProbe.begin();
        try {
            tick(event.getServer());
        } finally {
            com.newuniverse.nusmp.perf.PerfProbe.end(PROBE, t0);
        }
    }

    private static void tick(MinecraftServer server) {
        long now = server.getTickCount();
        Iterator<Entry> it = BARS.values().iterator();
        while (it.hasNext()) {
            Entry e = it.next();
            LivingEntity boss = e.owner.get();
            if (boss == null || boss.isRemoved()) {                                   // gone: whoever was told forgets the bar (a dying bar keeps its animation on the client)
                clearTold(server, e);
                it.remove();
                continue;
            }
            int watchers = 0;
            boolean fresh = false;
            for (ServerPlayer p : e.bar.getPlayers()) {
                watchers++;
                if (!e.told.contains(p.getUUID())) fresh = true;
            }
            if (watchers == 0) {
                e.told.clear();
                continue;
            }
            if (now >= e.nextAt || (fresh && e.last == null)) {
                e.nextAt = now + PERIOD;
                BossBarState next = snapshot(e, boss, now);
                if (BossBarState.differs(e.last, e.lastTick, next, now)) {
                    if (e.last == null) voice(boss, next, "intro");                                  // 1.110.0: spoken lines (audio/voice), the family follows the bar's style
                    else if (next.phase() > e.last.phase()) voice(boss, next, "phase2");
                    e.last = next;
                    e.lastTick = now;
                    sendAll(e, next);
                    continue;
                }
            }
            if (fresh && e.last != null) {
                BossBarStatePayload pay = new BossBarStatePayload(withLairNow(e, now));
                for (ServerPlayer p : e.bar.getPlayers()) if (e.told.add(p.getUUID())) PacketDistributor.sendToPlayer(p, pay);
            }
            if (e.told.size() > watchers) {                                             // somebody left: forget them so that a return is told again
                Set<UUID> now2 = new HashSet<>();
                for (ServerPlayer p : e.bar.getPlayers()) now2.add(p.getUUID());
                e.told.retainAll(now2);
            }
        }
    }

    /** The last state with its lair countdown moved on to now (a late watcher must not see the countdown of the moment it was sampled). */
    private static BossBarState withLairNow(Entry e, long now) {
        BossBarState s = e.last;
        if (!s.lairPending()) return s;
        int left = (int) Math.max(0, s.lairTicks() - (now - e.lastTick));
        return new BossBarState(s.bar(), s.entityId(), s.name(), s.style(), s.title(), s.phaseName(), s.phase(), s.phaseCount(), s.thresholds(), s.shield(), s.flags(), s.lairId(), left,
                s.lairTotal(), s.adds(), s.castName(), s.castPercent(), s.fighters());
    }

    private static void sendAll(Entry e, BossBarState s) {
        BossBarStatePayload pay = new BossBarStatePayload(s);
        for (ServerPlayer p : e.bar.getPlayers()) {
            PacketDistributor.sendToPlayer(p, pay);
            e.told.add(p.getUUID());
        }
    }

    private static void clearTold(MinecraftServer server, Entry e) {
        if (e.told.isEmpty()) return;
        BossBarNet.Clear clear = new BossBarNet.Clear(e.bar.getId());
        for (UUID id : e.told) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) PacketDistributor.sendToPlayer(p, clear);
        }
    }

    private static void onDeath(LivingDeathEvent event) {
        if (BARS.isEmpty() || event.getEntity().level().isClientSide) return;
        LivingEntity dying = event.getEntity();
        for (Entry e : BARS.values()) {
            if (e.owner.get() != dying) continue;
            e.dead = true;
            MinecraftServer server = dying.level().getServer();
            long now = server == null ? 0L : server.getTickCount();
            BossBarState s = snapshot(e, dying, now);
            voice(dying, s, "death");                                                             // 1.110.0: spoken line
            e.last = s;
            e.lastTick = now;
            sendAll(e, s);
        }
    }

    /** The boss speaks to the players around it: the voice family comes from the style id of its bar (the same resolution the bar uses). */
    private static void voice(LivingEntity boss, BossBarState state, String event) {
        if (boss.level() instanceof net.minecraft.server.level.ServerLevel sl && state != null) {
            com.newuniverse.nusmp.audio.voice.Voice.boss(sl, boss.position(), state.style(), event);
        }
    }

    // ================================================================ the sample
    static BossBarState snapshot(Entry e, LivingEntity boss, long now) {
        BossBarSource s = e.source;
        float max = boss.getMaxHealth();
        float hp = max <= 0f ? 0f : boss.getHealth() / max;
        if (hp > e.lastHp + 0.004f) e.healUntil = now + 20;
        e.lastHp = hp;
        int flags = (s.invulnerable() ? BossBarState.INVULNERABLE : 0) | (s.enraged() ? BossBarState.ENRAGED : 0) | (now < e.healUntil ? BossBarState.HEALING : 0)
                | (e.dead || !boss.isAlive() ? BossBarState.DEAD : 0);
        String style = s.styleId();
        if (!BarStyleIds.known(style)) style = BarStyleIds.WARDEN;
        String lair = s.lairId();
        int lairTicks = s.lairTicks();
        if (lair.isEmpty() || lairTicks < 0 || lairTicks > BossBarState.LAIR_WARNING_TICKS + 40) {
            lair = "";
            lairTicks = -1;
        }
        float cp = s.castProgress();
        String cast = s.castName();
        int castPct = cast.isEmpty() || cp < 0f ? -1 : Math.round(Math.max(0f, Math.min(1f, cp)) * 100f);
        return new BossBarState(e.bar.getId(), boss.getId(), s.name(), style, s.title(), s.phaseName(), s.phase(), s.phaseCount(), s.phaseThresholds(), Math.round(s.shield() * 100f), flags,
                lair, lairTicks, BossBarState.LAIR_WARNING_TICKS, s.adds(), cast, castPct, s.fighters());
    }

    /** For the self-test: the state a source would send for an entity right now. */
    public static BossBarState sample(LivingEntity boss, ServerBossEvent bar, long now) {
        return snapshot(new Entry(bar, boss, now), boss, now);
    }
}
