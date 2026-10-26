package com.newuniverse.nusmp.entity.veldanava.skills;

import com.newuniverse.nusmp.avatar.TensuraBridge;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: Israfil's Rift Walk lets the owner phase out of reality and walk through solid blocks. The only way a player's body can be made to pass through blocks, on the server and on their own client,
 * is the spectator game mode, so a walk puts the owner in spectator for a short, fixed time and puts the old game mode back afterwards. That swap is dangerous if it is ever left half done, so this class
 * owns it completely: one {@link Session} per walking player, a hard time limit, the Magicule paid second by second, and an ending on every way out (time, no Magicule, the key let go, death, logout,
 * the server stopping, and a login after a crash - the old game mode is kept on the player's data, key {@link #K_PREV}, until it is given back).
 *
 * <p>Bounds: the map holds only walking players (a few at most, each for at most {@link IsrafilRules#RIFT_SECONDS_MASTERED} seconds); the server tick returns on its first line when it is empty and
 * otherwise does one comparison per walker, plus one Magicule charge per walker per second; ending costs at most 17 collision checks. No dimension of the Labyrinth (the dungeon's stone is magic).
 */
public final class RiftWalk {
    private RiftWalk() {}

    static final String K_PREV = "nusmp_rift_prev";

    /** One walk. */
    static final class Session {
        final long endsAt;
        long nextCostAt;
        final Vec3 start;
        final double costPerSecond;
        final boolean free;

        Session(long endsAt, long nextCostAt, Vec3 start, double costPerSecond, boolean free) {
            this.endsAt = endsAt;
            this.nextCostAt = nextCostAt;
            this.start = start;
            this.costPerSecond = costPerSecond;
            this.free = free;
        }
    }

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    public static int count() { return SESSIONS.size(); }

    public static boolean walking(UUID id) { return SESSIONS.containsKey(id); }

    /**
     * Starts a walk for {@code seconds}. The first second is already paid by the caller. Returns the reason in words when it cannot start, or null when it started.
     */
    public static String begin(ServerPlayer p, int seconds, double costPerSecond) {
        if (SESSIONS.containsKey(p.getUUID())) return "You are already walking the rift.";
        if (p.isCreative() || p.isSpectator()) return "You need no rift: you already pass through.";
        if (p.isPassenger()) return "You cannot walk the rift while riding.";
        if (Labyrinth.in(p.level())) return "The Dungeon's stone is magic: the rift cannot part it.";
        long now = p.level().getGameTime();
        CompoundTag d = p.getPersistentData();
        d.putString(K_PREV, p.gameMode.getGameModeForPlayer().getName());
        SESSIONS.put(p.getUUID(), new Session(now + Math.max(1, seconds) * 20L, now + 20L, p.position(), costPerSecond, false));
        p.setGameMode(GameType.SPECTATOR);
        TrialFx.rift(p.serverLevel(), p);
        return null;
    }

    /** Server tick: ends walks that are over, charges the Magicule of each second. */
    public static void onServerTick(MinecraftServer server) {
        if (SESSIONS.isEmpty()) return;
        long now = server.overworld().getGameTime();
        for (UUID id : new ArrayList<>(SESSIONS.keySet())) {
            Session s = SESSIONS.get(id);
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (s == null) continue;
            if (p == null || !p.isAlive()) { SESSIONS.remove(id); continue; }                     // gone: the next login puts the game mode back
            if (now >= s.endsAt || !JudgmentConfig.enabled()) { end(p, false, "The rift closes."); continue; }
            if (now >= s.nextCostAt) {
                s.nextCostAt += 20L;
                if (s.costPerSecond > 0.0 && !p.isCreative() && !TensuraBridge.spendMagicule(p, s.costPerSecond)) end(p, false, "The rift closes: no magicules left.");
                else TrialFx.rift(p.serverLevel(), p);
            }
        }
    }

    /** Ends a walk (the key was let go, death, logout): the game mode comes back and the body is put on a free spot. No-op when the player is not walking. */
    public static void end(ServerPlayer p, boolean force, String message) {
        Session s = SESSIONS.remove(p.getUUID());
        CompoundTag d = p.getPersistentData();
        if (!d.contains(K_PREV)) return;
        GameType back = GameType.byName(d.getString(K_PREV), GameType.SURVIVAL);
        d.remove(K_PREV);
        ServerLevel l = p.serverLevel();
        if (!l.noCollision(p, p.getBoundingBox())) {
            Vec3 spot = stand(p, s == null ? null : s.start);
            if (spot != null) p.teleportTo(spot.x, spot.y, spot.z);
        }
        p.setGameMode(back);
        p.fallDistance = 0.0f;
        if (back != GameType.CREATIVE && back != GameType.SPECTATOR) p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, false, false, false));
        if (message != null) p.displayClientMessage(Component.literal(message).withStyle(ChatFormatting.AQUA), true);
    }

    /** A free place near a body that is stuck in blocks: straight up, straight down (eight blocks each), then where the walk began. */
    static Vec3 stand(ServerPlayer p, Vec3 start) {
        ServerLevel l = p.serverLevel();
        AABB box = p.getBoundingBox();
        for (int dy = 1; dy <= 8; dy++) if (l.noCollision(p, box.move(0.0, dy, 0.0))) return p.position().add(0.0, dy, 0.0);
        for (int dy = 1; dy <= 8; dy++) if (l.noCollision(p, box.move(0.0, -dy, 0.0))) return p.position().add(0.0, -dy, 0.0);
        return start;
    }

    /** A login after a crash or a kick while walking: the game mode comes back, out of the wall. */
    public static void restoreStale(ServerPlayer p) {
        if (!p.getPersistentData().contains(K_PREV) || SESSIONS.containsKey(p.getUUID())) return;
        end(p, true, null);
    }

    /** The server is stopping: everybody who walks is put back at once (their data is saved after this). */
    public static void endAll(MinecraftServer server) {
        for (UUID id : new ArrayList<>(SESSIONS.keySet())) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) end(p, true, null);
        }
        SESSIONS.clear();
    }

    /** Tests only: forgets every walk without touching a player. */
    static void clearAll() { SESSIONS.clear(); }
}
