package com.newuniverse.nusmp.audio.voice;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

/**
 * Why: the single facade every emitter calls to make the game speak: {@code Voice.say(player, "system_floor_enter")} next to the existing message, {@code Voice.sayNear(level, pos, id)}
 * for a boss, an NPC or a Djinn that speaks to the players around it. Everything is decided here on the SERVER and nothing else is needed at the call site: an unknown id is ignored, a fake
 * player is skipped, the per-player limits ({@link VoiceLimiter}: a cooldown per line, a gap between lines, the {@code voice.maxPerSecond} server option) are applied, only players near
 * the speaker are sent the packet ({@code voice.range}, clamped), and the packet is the tiny {@link VoiceNet.Say}. The message text that goes with a line is never touched, so a player who
 * switches the voices off (the client option) or whose line was held back by a limit still reads everything. State is keyed by player UUID; there is no static "current player".
 */
public final class Voice {
    private Voice() {}

    private static final VoiceLimiter LIMITER = new VoiceLimiter();
    private static long lastPrune;

    /** Says a line to one player. Returns true when the packet was sent. Never throws. */
    public static boolean say(ServerPlayer player, String id) { return say(player, id, -1L); }

    /** Same with an own cooldown for this line in ticks (a negative number means the {@code voice.lineCooldownSeconds} option). */
    public static boolean say(ServerPlayer player, String id, long cooldownTicks) {
        try {
            if (player == null || !VoiceCatalog.has(id) || player instanceof FakePlayer || player.connection == null) return false;
            MinecraftServer server = player.getServer();
            long now = server == null ? 0L : server.getTickCount();
            if (!LIMITER.tryAcquire(player.getUUID(), id, now, VoiceConfig.maxPerSecond(), cooldownTicks >= 0 ? cooldownTicks : VoiceConfig.lineCooldownTicks())) return false;
            PacketDistributor.sendToPlayer(player, new VoiceNet.Say(id));
            prune(now);
            return true;
        } catch (RuntimeException noConnection) {
            return false;
        }
    }

    /** Says a line to every player within {@code radius} blocks of a point (clamped to the {@code voice.range} option). Returns how many were sent the line. */
    public static int sayNear(ServerLevel level, Vec3 at, String id, double radius) {
        if (level == null || at == null || !VoiceCatalog.has(id)) return 0;
        double r = Math.max(1.0, Math.min(radius, VoiceConfig.range()));
        double r2 = r * r;
        int sent = 0;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || p.distanceToSqr(at) > r2) continue;
            if (say(p, id)) sent++;
        }
        return sent;
    }

    /** Same with the configured range. */
    public static int sayNear(ServerLevel level, Vec3 at, String id) { return sayNear(level, at, id, VoiceConfig.range()); }

    /** The player's Djinn speaks ({@code event}: summon or attack) to the players around the wearer; the name may be the enum name or the display name. */
    public static int djinn(ServerPlayer wearer, String djinnName, String event) {
        if (wearer == null) return 0;
        String id = VoicePicker.djinn(djinnName, event);
        return id == null ? 0 : sayNear(wearer.serverLevel(), wearer.position(), id, 32.0);
    }

    /** A boss of the given bar style speaks ({@code intro}, {@code phase2}, {@code death}) to the players around it. */
    public static int boss(ServerLevel level, Vec3 at, String barStyleId, String event) {
        String id = VoicePicker.boss(barStyleId, event);
        return id == null ? 0 : sayNear(level, at, id);
    }

    /** An NPC speaks to the player who talks to it. */
    public static boolean npc(ServerPlayer player, VoicePicker.NpcAct act, boolean merchant) {
        String id = VoicePicker.npc(act, merchant);
        return id != null && say(player, id);
    }

    /** A player logged out: forget their cooldowns. */
    public static void forget(UUID player) { LIMITER.forget(player); }

    /** The server stopped: forget everything. */
    public static void clear() { LIMITER.clear(); }

    /** For the self-test: how many players the limiter tracks. */
    public static int tracked() { return LIMITER.tracked(); }

    private static void prune(long now) {
        if (now - lastPrune < 1200) return;
        lastPrune = now;
        LIMITER.prune(now);
    }
}
