package com.newuniverse.nusmp.mana.method;

import com.newuniverse.nusmp.mana.method.RuneAccess.Cast;
import com.newuniverse.nusmp.mana.method.RuneAccess.Stamps;
import com.newuniverse.nusmp.perf.PerfProbe;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 3.8.0, {@code manaMethod.access = TIMED}: the "Rune State" page ({@code mana_method_state}) switches the runes ON for {@code timedSeconds}, then {@code timedCooldownSeconds} must
 * pass before it can be cast again. The rules are {@link RuneAccess}; this class is the server side: who is ON, the messages, the stamps kept on the player and the clean-up.
 *
 * <ul>
 *   <li><b>Per player, by UUID.</b> {@link #ACTIVE} maps a UUID to the tick its ON state ends (one entry per player who is ON, none otherwise). The two stamps (ends at, cooldown ends at)
 *       are also kept in the player's PERSISTED data as WORLD game-time ticks, so logging out and in again, dying or leaving the server can never reset the cooldown.</li>
 *   <li><b>Ends early</b> on logout, death, dimension change and server stop, like the other Mana Method sessions: the ON state is gone and the cooldown starts at that moment (nothing is
 *       gained by leaving, and the remaining ON time is lost). A state that was left ON in the file (a crash) is ended at the next login the same way.</li>
 *   <li><b>Idle cost</b>: one {@code isEmpty()} check per tick; with a player ON one pass over the (few) ON players once a second, no world access.</li>
 *   <li><b>Network</b>: one tiny clientbound packet to the player themselves when the state starts and when it ends (at most 2 per 18 minutes with the defaults), none per tick; it only
 *       tells the client's menu whether to list the pages ({@link RuneStateGate}).</li>
 * </ul>
 */
public final class RuneState {
    private RuneState() {}

    private static final PerfProbe.Section PROBE = PerfProbe.section("s.mana.state");
    private static final String KEY = "nusmp_mana_method_state";
    private static final Map<UUID, Long> ACTIVE = new ConcurrentHashMap<>();
    private static final Set<UUID> WARNED = ConcurrentHashMap.newKeySet();

    // ---------------------------------------------------------------- the stamps (persisted on the player)
    private static CompoundTag data(Player p) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(Player.PERSISTED_NBT_TAG, 10)) pd.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        CompoundTag root = pd.getCompound(Player.PERSISTED_NBT_TAG);
        if (!root.contains(KEY, 10)) root.put(KEY, new CompoundTag());
        return root.getCompound(KEY);
    }

    static Stamps read(ServerPlayer p) {
        CompoundTag d = data(p);
        return new Stamps(d.getLong("OnUntil"), d.getLong("CooldownUntil"));
    }

    static void write(ServerPlayer p, Stamps s) {
        CompoundTag d = data(p);
        d.putLong("OnUntil", s.onUntil());
        d.putLong("CooldownUntil", s.cooldownUntil());
    }

    /** The WORLD clock (the overworld's game time), the one the stamps use on every dimension. */
    static long clock(ServerPlayer p) { return p.getServer().overworld().getGameTime(); }

    // ---------------------------------------------------------------- the question the grimoire asks
    /** Whether this entity's rune state is ON: the server's record for a player, what the server last told this client otherwise. */
    public static boolean on(LivingEntity e) {
        if (e instanceof ServerPlayer p) {
            if (ACTIVE.isEmpty()) return false;
            Long until = ACTIVE.get(p.getUUID());
            return until != null && clock(p) < until;
        }
        return RuneStateGate.on();
    }

    public static boolean isActive(UUID id) { return ACTIVE.containsKey(id); }

    public static int activeCount() { return ACTIVE.size(); }

    // ---------------------------------------------------------------- the page
    /** The cast of the Rune State page. True when the state started. A refusal costs nothing (the page is free) and tells the player why. */
    public static boolean cast(ServerPlayer p) {
        return castAt(p, clock(p), ManaMethodConfig.timedSeconds(), ManaMethodConfig.timedCooldownSeconds());
    }

    /** As {@link #cast} with the clock and the two durations given (the self-test advances time by hand). */
    static boolean castAt(ServerPlayer p, long now, int durationSeconds, int cooldownSeconds) {
        Stamps s = read(p);
        if (RuneAccess.on(s, now) && !ACTIVE.containsKey(p.getUUID())) s = RuneAccess.endNow(s, now, cooldownSeconds);   // a state the file says is ON but the server does not hold: it is over
        Cast c = RuneAccess.cast(s, now, durationSeconds, cooldownSeconds);
        switch (c.result()) {
            case STARTED -> {
                write(p, c.stamps());
                ACTIVE.put(p.getUUID(), c.stamps().onUntil());
                WARNED.remove(p.getUUID());
                tell(p, Component.translatableWithFallback("nusmp.mana_method.state.start", "Rune State: ON for %s. The runes answer your call.", RuneAccess.clock(c.stamps().onUntil() - now)).withStyle(ChatFormatting.LIGHT_PURPLE));
                sendState(p, true, (int) Math.min(RuneAccess.MAX_SECONDS, (c.stamps().onUntil() - now + 19) / 20));
                return true;
            }
            case ALREADY_ON -> tell(p, Component.translatableWithFallback("nusmp.mana_method.state.already", "Rune State: already ON (%s left).", RuneAccess.clock(RuneAccess.onLeft(s, now))).withStyle(ChatFormatting.GRAY));
            default -> tell(p, Component.translatableWithFallback("nusmp.mana_method.state.cooling", "Rune State: the runes have not settled yet (%s left).", RuneAccess.clock(RuneAccess.cooldownLeft(s, now))).withStyle(ChatFormatting.RED));
        }
        return false;
    }

    // ---------------------------------------------------------------- the sweep (once a second, only while someone is ON)
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) return;
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % RuneAccess.SWEEP_TICKS != 0) return;
        long t0 = PerfProbe.begin();
        try {
            sweep(server, server.overworld().getGameTime());
        } finally {
            PerfProbe.end(PROBE, t0);
        }
    }

    /** One pass at tick {@code now}: the 30 second warning and the natural end (public for the self-test, which advances time by hand). */
    public static void sweep(MinecraftServer server, long now) {
        sweepWith(id -> server.getPlayerList().getPlayer(id), now);
    }

    /** As {@link #sweep(MinecraftServer, long)} with the way a player is found given (the self-test's stand-in is not in the player list). */
    static void sweepWith(Function<UUID, ServerPlayer> lookup, long now) {
        for (Iterator<Map.Entry<UUID, Long>> it = ACTIVE.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Long> en = it.next();
            ServerPlayer p = lookup.apply(en.getKey());
            if (p == null) { it.remove(); WARNED.remove(en.getKey()); continue; }       // gone without a logout event: nothing to keep
            Stamps s = read(p);
            if (now >= en.getValue()) {                                                  // the natural end: the cooldown was stamped at the start
                it.remove();
                WARNED.remove(en.getKey());
                tell(p, Component.translatableWithFallback("nusmp.mana_method.state.end", "Rune State: OFF. It can be called again in %s.", RuneAccess.clock(RuneAccess.cooldownLeft(s, now))).withStyle(ChatFormatting.GRAY));
                sendState(p, false, 0);
            } else if (RuneAccess.warnDue(new Stamps(en.getValue(), s.cooldownUntil()), now, WARNED.contains(en.getKey()))) {
                WARNED.add(en.getKey());
                tell(p, Component.translatableWithFallback("nusmp.mana_method.state.warn", "Rune State: the runes fade in %s.", RuneAccess.clock(en.getValue() - now)).withStyle(ChatFormatting.YELLOW));
            }
        }
    }

    // ---------------------------------------------------------------- ending early
    /** Ends the ON state of a player NOW (the cooldown starts now). Returns whether there was one. */
    public static boolean endEarly(ServerPlayer p, boolean tellPlayer) {
        Long until = ACTIVE.remove(p.getUUID());
        WARNED.remove(p.getUUID());
        if (until == null) return false;
        long now = clock(p);
        Stamps ended = RuneAccess.endNow(new Stamps(until, read(p).cooldownUntil()), now, ManaMethodConfig.timedCooldownSeconds());
        write(p, ended);
        if (tellPlayer) {
            tell(p, Component.translatableWithFallback("nusmp.mana_method.state.cut", "Rune State: the runes fade early. It can be called again in %s.", RuneAccess.clock(RuneAccess.cooldownLeft(ended, now))).withStyle(ChatFormatting.GRAY));
            sendState(p, false, 0);
        }
        return true;
    }

    public static void onDeath(LivingDeathEvent e) {
        if (!ACTIVE.isEmpty() && e.getEntity() instanceof ServerPlayer p) { endEarly(p, false); sendState(p, false, 0); }
    }

    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (!ACTIVE.isEmpty() && e.getEntity() instanceof ServerPlayer p) endEarly(p, true);
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!ACTIVE.isEmpty() && e.getEntity() instanceof ServerPlayer p) endEarly(p, false);
    }

    /** A state still ON in the file (the server stopped without a logout, a crash) is ended at the next login: the cooldown starts there, the ON time is not kept. */
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        Stamps s = read(p);
        long now = clock(p);
        if (RuneAccess.on(s, now)) write(p, RuneAccess.endNow(s, now, ManaMethodConfig.timedCooldownSeconds()));
    }

    public static void onServerStopping(MinecraftServer server) {
        for (UUID id : ACTIVE.keySet().toArray(new UUID[0])) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) endEarly(p, false);
        }
        clearAll();
    }

    public static void clearAll() {
        ACTIVE.clear();
        WARNED.clear();
    }

    // ---------------------------------------------------------------- messages and the packet
    private static void tell(ServerPlayer p, Component c) { p.displayClientMessage(c, true); }

    private static void sendState(ServerPlayer p, boolean on, int secondsLeft) {
        try { PacketDistributor.sendToPlayer(p, new RuneNet.StatePayload(on, secondsLeft)); } catch (RuntimeException noConnection) { /* a stand-in player (CI self-test) has no connection */ }
    }
}
