package com.newuniverse.nusmp.labyrinth.mega.enc;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the Dungeon Master has to HEAR the party. Vanilla already announces every footstep, door, broken block, shot arrow and blast as a game
 * event (that is how a sculk sensor listens), so the noise of a party is read from that stream instead of from a dozen separate hooks, plus
 * one direct hook for damage (the game event for damage is not reliably sent for every source). Everything is restricted to the Mega-Dungeon
 * dimension and costs a pointer comparison elsewhere. Each event is turned into a {@link NoiseKind} through a table that is built once per
 * event type (an identity map on the event holder, so no string is made per event), faded by the distance to every player of the dimension, and
 * added to that player's {@link NoiseMeter}; the party's reading is the maximum over its members. Footsteps, doors and the like count only when a
 * PLAYER made them: a zombie shuffling about is the dungeon being awake, not the party being loud. Loud, sudden events (a blast, a fight, a block
 * broken) are also written to a small ring ({@link BurstRing}) so a wandering boss can turn its head towards where they happened. No
 * allocation per event beyond what the game itself makes; a handler never throws into the game (it logs once instead).
 */
public final class NoiseEvents {
    private NoiseEvents() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    private static final Map<UUID, NoiseMeter> METERS = new HashMap<>();
    private static final IdentityHashMap<Holder<GameEvent>, NoiseKind> KINDS = new IdentityHashMap<>();
    private static boolean warned;
    /** 1.94.0: how many game events reached the handler in the Mega-Dungeon, and how many times a player's meter was fed (for {@code why}: 0 seen means the listener is not wired). */
    private static long seen, heard;

    /** Game events the noise handler has been given in the Mega-Dungeon since the server started. */
    static long eventsSeen() { return seen; }

    /** Times a player's noise meter was fed since the server started. */
    static long eventsHeard() { return heard; }

    /** The loud events of the last seconds, for the wandering bosses. */
    static final BurstRing BURSTS = new BurstRing(48);

    // ---------------------------------------------------------------- the handlers
    public static void onGameEvent(VanillaGameEvent e) {
        try {
            Level level = e.getLevel();
            if (level.isClientSide() || !(level instanceof ServerLevel sl) || !Labyrinth.inMega(sl)) return;
            seen++;
            handle(sl, e.getVanillaEvent(), e.getCause(), e.getEventPosition(), sl.players(), sl.getGameTime());
        } catch (Throwable t) {
            warn(t);
        }
    }

    public static void onDamage(LivingIncomingDamageEvent e) {
        try {
            LivingEntity victim = e.getEntity();
            Level level = victim.level();
            if (level.isClientSide() || !(level instanceof ServerLevel sl) || !Labyrinth.inMega(sl)) return;
            if (!(victim instanceof ServerPlayer) && !(e.getSource().getEntity() instanceof ServerPlayer)) return;
            emit(sl, sl.players(), victim.getX(), victim.getEyeY(), victim.getZ(), NoiseKind.HURT, sl.getGameTime());
        } catch (Throwable t) {
            warn(t);
        }
    }

    /**
     * What one game event does to the listeners' noise. Split from the event handler so the self-test can feed it the real event holders and
     * stand-in players. Returns the kind that was applied ({@link NoiseKind#SILENT} when the event is not a noise or was not the party's).
     */
    public static NoiseKind handle(ServerLevel level, Holder<GameEvent> holder, Entity cause, Vec3 at, Iterable<? extends ServerPlayer> listeners, long now) {
        NoiseKind kind = kindOf(holder);
        if (kind == NoiseKind.SILENT) return kind;
        if (kind == NoiseKind.STEP_WALK && cause != null && cause.isSprinting()) kind = NoiseKind.STEP_SPRINT;
        if (kind.playerOnly() && !(cause instanceof Player)) return NoiseKind.SILENT;
        emit(level, listeners, at.x, at.y, at.z, kind, now);
        return kind;
    }

    // ---------------------------------------------------------------- the field
    /**
     * Makes a noise at a point: every listener within earshot hears it, faded by the distance, and adds it to their meter; a loud one is also
     * remembered for the wandering bosses. {@code listeners} are the players of the level (the stand-ins of the self-tests are passed here too).
     */
    public static void emit(ServerLevel level, Iterable<? extends ServerPlayer> listeners, double x, double y, double z, NoiseKind kind, long now) {
        double amount = kind.amount() * EncConfig.get(EncConfig.NOISE_SCALE);
        if (!(amount > 0.0)) return;
        double decay = EncConfig.get(EncConfig.NOISE_DECAY);
        double earshot = kind.earshot();
        for (ServerPlayer p : listeners) {
            if (p.isSpectator() || !p.isAlive()) continue;
            double dx = p.getX() - x, dy = p.getY() - y, dz = p.getZ() - z;
            double f = NoiseMeter.falloff(Math.sqrt(dx * dx + dy * dy + dz * dz), earshot);
            if (f > 0.0) {
                METERS.computeIfAbsent(p.getUUID(), k -> new NoiseMeter()).add(amount * f, now, decay);
                heard++;
            }
        }
        if (kind.burst()) BURSTS.add(x, y, z, amount, now);
    }

    /** Makes a noise heard by the players of the level, now. */
    public static void emit(ServerLevel level, double x, double y, double z, NoiseKind kind) {
        emit(level, level.players(), x, y, z, kind, level.getGameTime());
    }

    /** A player's noise level now (0 when nothing was ever heard near them). Does not create a meter. */
    public static double reading(UUID player, long now) {
        NoiseMeter m = METERS.get(player);
        return m == null ? 0.0 : m.peek(now, EncConfig.get(EncConfig.NOISE_DECAY));
    }

    /** The kind of noise a game event is, from its registry name (cached per event). */
    public static NoiseKind kindOf(Holder<GameEvent> h) {
        NoiseKind k = KINDS.get(h);
        if (k != null) return k;
        String path = h.unwrapKey().map(key -> key.location().getPath()).orElse("");
        k = NoiseKind.byGameEvent(path, false);
        KINDS.put(h, k);
        return k;
    }

    static void forget(UUID id) {
        if (!METERS.isEmpty()) METERS.remove(id);
    }

    static void clear() {
        METERS.clear();
        BURSTS.clear();
    }

    private static void warn(Throwable t) {
        if (warned) return;
        warned = true;
        LOG.warn("[nusmp] The Dungeon Master could not read a noise event (further failures are not logged)", t);
    }
}
