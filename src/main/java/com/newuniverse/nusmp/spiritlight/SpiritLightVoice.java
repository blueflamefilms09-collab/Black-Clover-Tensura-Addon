package com.newuniverse.nusmp.spiritlight;

import com.newuniverse.nusmp.audio.voice.VoiceConfig;
import com.newuniverse.nusmp.audio.voice.VoiceLimiter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the seraph speaks (docs/magic/spirit_light.md), and a spoken line must never be spam on a public server. This is the server half of its voice, in the pattern of the mod's spoken voices
 * ({@code audio.voice}: the pure {@link VoiceLimiter} and the {@code voice.maxPerSecond} server option are the ones it uses): the server picks the line ({@link LightMoodRules#line}), and sends the tiny
 * {@link SpiritLightNet.Say} packet to the players within {@link #RANGE} blocks of the speaker ONLY, each at most one line every {@link #LISTENER_GAP} ticks (4 seconds), the same line to the same listener
 * at most once per its cue's cooldown, never more than {@code voice.maxPerSecond} lines a second to a listener, fake players skipped. One box search of players per spoken line (a line is behind a cue
 * cooldown of at least five seconds per owner). State is keyed by the listener's UUID and dropped on logout and server stop; no static "current player".
 */
public final class SpiritLightVoice {
    private SpiritLightVoice() {}

    /** Only players this close hear the seraph, and each at most one line every four seconds. */
    public static final double RANGE = 32.0;
    public static final long LISTENER_GAP = 80;

    private static final VoiceLimiter LIMITER = new VoiceLimiter();
    private static final Map<UUID, Long> LAST_HEARD = new HashMap<>();
    private static long lastPrune;

    /**
     * The speaker {@code owner}'s spirit says {@code line} (an id of {@link SpiritLightVoiceLines#AVAILABLE}) to the players around it; {@code lineCooldown} is the cooldown of this line for one listener.
     * Returns how many players were sent it.
     */
    public static int say(ServerPlayer owner, String line, long lineCooldown) {
        if (owner == null || !SpiritLightVoiceLines.has(line)) return 0;
        long now = owner.level().getGameTime();
        AABB box = owner.getBoundingBox().inflate(RANGE);
        List<ServerPlayer> near = owner.level().getEntitiesOfClass(ServerPlayer.class, box, p -> p.distanceToSqr(owner) <= RANGE * RANGE && !p.isSpectator());
        int sent = 0;
        for (ServerPlayer p : near) {
            if (p instanceof FakePlayer || p.connection == null) continue;
            Long last = LAST_HEARD.get(p.getUUID());
            if (last != null && now - last < LISTENER_GAP) continue;                                        // at most one line every four seconds per listener
            if (!LIMITER.tryAcquire(p.getUUID(), line, now, VoiceConfig.maxPerSecond(), lineCooldown)) continue;
            LAST_HEARD.put(p.getUUID(), now);
            PacketDistributor.sendToPlayer(p, new SpiritLightNet.Say(line, owner.getId()));
            sent++;
        }
        prune(now);
        return sent;
    }

    private static void prune(long now) {
        if (now - lastPrune < 1200) return;
        lastPrune = now;
        LIMITER.prune(now);
        Iterator<Map.Entry<UUID, Long>> it = LAST_HEARD.entrySet().iterator();
        while (it.hasNext()) if (now - it.next().getValue() > LISTENER_GAP) it.remove();
    }

    /** A player logged out: forget their cooldowns. */
    public static void forget(UUID player) {
        LIMITER.forget(player);
        LAST_HEARD.remove(player);
    }

    public static void clear() {
        LIMITER.clear();
        LAST_HEARD.clear();
    }

    /** For the self-test: how many listeners the limiter tracks. */
    public static int tracked() { return LIMITER.tracked() + LAST_HEARD.size(); }

}
