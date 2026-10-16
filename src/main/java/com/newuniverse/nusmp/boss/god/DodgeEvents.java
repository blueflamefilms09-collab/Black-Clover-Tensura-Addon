package com.newuniverse.nusmp.boss.god;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Why: the Time Stasis Traps of the god-tier Lucius punish dodge spam, and the dodges of this mod live in different systems (Rouge's
 * fate rewrite, the Butoh evasion and dash, the Ebb Step page, a cancelled hit of Lucius's own side). Rather than make the boss know
 * each of them, every one of them may call this one tiny public method when a player dodges: {@code DodgeEvents.record(player)}. It is
 * cheap and does nothing at all unless a god-tier fight is on (the gate), counts at most one dodge per player per tick (so a system that
 * reports twice for one dodge, or a cancelled hit that two systems both report, counts once), and tells the single listener when a
 * player's meter ({@link DodgeSpamMeter}: three in six seconds) fires.
 */
public final class DodgeEvents {
    private DodgeEvents() {}

    private static final Map<UUID, DodgeSpamMeter> METERS = new HashMap<>();
    private static final Map<UUID, Long> LAST_TICK = new HashMap<>();
    private static BooleanSupplier gate = () -> false;
    private static Consumer<ServerPlayer> listener;

    /** Who listens (the Lucius handler sets it once) and when it is worth counting (a god-tier fight is on). */
    public static void install(BooleanSupplier fightOn, Consumer<ServerPlayer> onSpam) {
        gate = fightOn;
        listener = onSpam;
    }

    /** A player dodged: a save, an evasion, a dash, a blow cancelled by one of their systems. Safe to call from anywhere on the server thread. */
    public static void record(ServerPlayer p) {
        if (p == null || p.level().isClientSide || listener == null || !gate.getAsBoolean()) return;
        long now = p.level().getGameTime();
        Long last = LAST_TICK.put(p.getUUID(), now);
        if (last != null && last == now) return;
        if (METERS.size() > 256) METERS.clear();
        if (METERS.computeIfAbsent(p.getUUID(), k -> new DodgeSpamMeter()).record(now)) listener.accept(p);
    }

    public static void forget(UUID id) {
        METERS.remove(id);
        LAST_TICK.remove(id);
    }

    public static void clear() {
        METERS.clear();
        LAST_TICK.clear();
    }
}
