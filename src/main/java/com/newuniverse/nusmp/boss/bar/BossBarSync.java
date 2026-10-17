package com.newuniverse.nusmp.boss.bar;

import com.newuniverse.nusmp.entity.zogratis.PowerCurve;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.NeoForge;
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
 * Server half of the animated boss bar: tells the players who can see a mod boss's bar how to draw it ({@link BossBarNet.Style}) and
 * when it is gone ({@link BossBarNet.Clear}). A boss calls {@link #update(ZogratisBoss, ServerBossEvent)} whenever it refreshes its
 * bar and {@link #remove} when it drops it; every Zogratis boss does so through {@code ZogratisBoss}. Players who start seeing the
 * bar later are caught by a per-tick check that compares the bar's watchers with the ones already told, so the order in which the
 * boss adds them does not matter. Other bosses with a {@code ServerBossEvent} can join through the generic {@link #update(Entity,
 * ServerBossEvent, String, int, int, int, boolean)}. Server thread only; nothing here changes the boss or its bar.
 */
public final class BossBarSync {
    private BossBarSync() {}

    private static final Map<UUID, Entry> BARS = new HashMap<>();

    private static final class Entry {
        final ServerBossEvent bar;
        final WeakReference<Entity> owner;
        BossBarNet.Style style;
        final Set<UUID> told = new HashSet<>();

        Entry(ServerBossEvent bar, Entity owner) {
            this.bar = bar;
            this.owner = new WeakReference<>(owner);
        }
    }

    /** Registers the per-tick watcher check and the cleanup at server stop (call once from the mod constructor). */
    public static void init() {
        NeoForge.EVENT_BUS.addListener(BossBarSync::onServerTick);
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> BARS.clear());
    }

    /** A Zogratis boss refreshed its bar: its devil, power and type decide the style. */
    public static void update(ZogratisBoss boss, ServerBossEvent bar) {
        if (boss == null || bar == null || boss.level().isClientSide) return;
        int power = boss.power();
        int phase = PowerCurve.stage(power);
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType()).getPath();
        BossBarThemes.Tint t = BossBarThemes.tint(id, boss.styleColor(), boss.styleAccent(), phase);        // 1.60.0: the boss's own palette (a devil's for the Zogratis family, the character's for the arcs)
        float glow = com.newuniverse.nusmp.boss.refill.PhaseRefill.glowStep(boss.refillGlow());      // 1.35.0: a refill lightens the tint (the bar morphs, no new packet)
        update(boss, bar, id, com.newuniverse.nusmp.boss.refill.PhaseRefill.lift(t.primary(), glow),
                com.newuniverse.nusmp.boss.refill.PhaseRefill.lift(t.secondary(), glow), phase, BossBarThemes.rage(power));
    }

    /** Any boss with a {@code ServerBossEvent}: sends the style to everyone who watches the bar now, and again whenever it changes. */
    public static void update(Entity owner, ServerBossEvent bar, String bossId, int primary, int secondary, int phase, boolean rage) {
        if (owner == null || bar == null || owner.level().isClientSide) return;
        BossBarNet.Style style = new BossBarNet.Style(bar.getId(), bossId, primary & 0xFFFFFF, secondary & 0xFFFFFF,
                Math.max(0, Math.min(BossBarThemes.MAX_PHASE, phase)), rage);
        Entry e = BARS.computeIfAbsent(bar.getId(), k -> new Entry(bar, owner));
        if (style.equals(e.style)) return;
        e.style = style;
        for (ServerPlayer p : bar.getPlayers()) {
            PacketDistributor.sendToPlayer(p, style);
            e.told.add(p.getUUID());
        }
    }

    /** The boss drops its bar: tells the watchers to forget the style. Call before {@code removeAllPlayers()}. */
    public static void remove(ServerBossEvent bar) {
        if (bar == null) return;
        Entry e = BARS.remove(bar.getId());
        if (e == null) return;
        BossBarNet.Clear clear = new BossBarNet.Clear(bar.getId());
        for (ServerPlayer p : bar.getPlayers()) PacketDistributor.sendToPlayer(p, clear);
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        if (BARS.isEmpty()) return;
        Iterator<Entry> it = BARS.values().iterator();
        while (it.hasNext()) {
            Entry e = it.next();
            Entity owner = e.owner.get();
            if (owner == null || owner.isRemoved()) {                  // gone without a remove() call (an unload): the client drops it when the bar goes
                it.remove();
                continue;
            }
            if (e.style == null) continue;
            int watchers = 0;
            for (ServerPlayer p : e.bar.getPlayers()) {
                watchers++;
                if (e.told.add(p.getUUID())) PacketDistributor.sendToPlayer(p, e.style);
            }
            if (e.told.size() > watchers) {                            // somebody left: forget them so that a return is told again
                Set<UUID> now = new HashSet<>();
                for (ServerPlayer p : e.bar.getPlayers()) now.add(p.getUUID());
                e.told.retainAll(now);
            }
        }
    }
}
