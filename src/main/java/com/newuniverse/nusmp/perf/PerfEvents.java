package com.newuniverse.nusmp.perf;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Why: the probe's wide view. One pair of listeners per hot server event: the first runs before every other listener of the event ({@link EventPriority#HIGHEST}) and takes the time, the last
 * ({@link EventPriority#LOWEST}) takes it again; the difference is everything every mod hung on that event cost, so {@code /multiverse perf report} shows at a glance whether the server tick, the
 * player tick, the level tick or the damage events are where the time goes, before the narrower sections of the individual handlers say which handler. Cost while the probe is off: two calls
 * of a lambda that reads one static flag, per event (a few per tick). Server thread only; the start time is kept in a plain static because one event of a kind is dispatched to completion before
 * the next one begins.
 */
final class PerfEvents {
    private PerfEvents() {}

    static final PerfProbe.Section SERVER_TICK = PerfProbe.section("s.event.server_tick");
    static final PerfProbe.Section PLAYER_TICK = PerfProbe.section("s.event.player_tick");
    static final PerfProbe.Section LEVEL_TICK = PerfProbe.section("s.event.level_tick");
    static final PerfProbe.Section INCOMING_DAMAGE = PerfProbe.section("s.event.incoming_damage");

    private static long tServer, tPlayer, tLevel, tDamage;

    static void register(IEventBus bus) {
        bus.addListener(EventPriority.HIGHEST, (ServerTickEvent.Post e) -> tServer = PerfProbe.begin());
        bus.addListener(EventPriority.LOWEST, (ServerTickEvent.Post e) -> { PerfProbe.end(SERVER_TICK, tServer); tServer = 0L; });
        bus.addListener(EventPriority.HIGHEST, (PlayerTickEvent.Post e) -> tPlayer = PerfProbe.begin());
        bus.addListener(EventPriority.LOWEST, (PlayerTickEvent.Post e) -> { PerfProbe.end(PLAYER_TICK, tPlayer); tPlayer = 0L; });
        bus.addListener(EventPriority.HIGHEST, (LevelTickEvent.Post e) -> tLevel = PerfProbe.begin());
        bus.addListener(EventPriority.LOWEST, (LevelTickEvent.Post e) -> { PerfProbe.end(LEVEL_TICK, tLevel); tLevel = 0L; });
        bus.addListener(EventPriority.HIGHEST, true, (LivingIncomingDamageEvent e) -> tDamage = PerfProbe.begin());
        bus.addListener(EventPriority.LOWEST, true, (LivingIncomingDamageEvent e) -> { PerfProbe.end(INCOMING_DAMAGE, tDamage); tDamage = 0L; });
    }
}
