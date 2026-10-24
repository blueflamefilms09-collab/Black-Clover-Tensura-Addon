package com.newuniverse.nusmp.companion.talk;

import com.newuniverse.nusmp.companion.CompanionConfig;
import com.newuniverse.nusmp.companion.net.CompanionNet;
import com.newuniverse.nusmp.perf.PerfBudget;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: a speech bubble over any entity's head, for everyone tracking it: {@code Bubble.say(entity, component)}. The text is a {@link Component} (a lang key), so each player reads it in their own language and a
 * dedicated server needs no lang file. It is cheap by construction: one small packet, sent only to the players tracking the speaker, at most one bubble per speaker every {@link BubbleRules#MIN_GAP} ticks, at most
 * {@link BubbleRules#GLOBAL_PER_TICK} bubbles per tick on the whole server (a {@link PerfBudget}), a line that is absurdly long is refused (the client cuts what it draws to {@link BubbleRules#MAX_CHARS}), and the
 * per-speaker record (a map keyed by the speaker's UUID, never more than 512 entries) is cleaned when the speaker leaves the world and every few seconds. The server setting {@code companions.bubbles} turns it off.
 * Server thread only.
 */
public final class Bubble {
    private Bubble() {}

    private static final Map<UUID, Long> LAST = new HashMap<>();
    private static final PerfBudget BUDGET = new PerfBudget("companion-bubbles", BubbleRules.GLOBAL_PER_TICK);
    private static final int MAX_RECORDS = 512;

    /** Puts a bubble over the entity. Returns whether one was sent (false when throttled, switched off, or the entity is not in a server level). */
    public static boolean say(Entity speaker, Component line) {
        if (speaker == null || line == null || !(speaker.level() instanceof ServerLevel level) || !speaker.isAlive() || !CompanionConfig.bubbles()) return false;
        int chars = line.getString().length();
        if (chars == 0 || chars > BubbleRules.MAX_CHARS * 4) return false;
        long now = level.getGameTime();
        Long last = LAST.get(speaker.getUUID());
        if (last != null && !BubbleRules.mayFor(last, now)) return false;
        if (LAST.size() >= MAX_RECORDS) sweep(now);
        if (LAST.size() >= MAX_RECORDS) return false;
        if (BUDGET.take(now, 1) < 1) return false;
        LAST.put(speaker.getUUID(), now);
        PacketDistributor.sendToPlayersTrackingEntity(speaker, new CompanionNet.BubbleLine(speaker.getId(), line, BubbleRules.lifeTicks(Math.min(chars, BubbleRules.MAX_CHARS))));
        return true;
    }

    /** Drops the records that no longer throttle anything. */
    public static void sweep(long now) {
        LAST.values().removeIf(t -> now < t || now - t > BubbleRules.MIN_GAP);
    }

    /** The speaker left the world. */
    public static void forget(UUID speaker) { LAST.remove(speaker); }

    public static void clear() { LAST.clear(); }

    /** How many speakers are recorded (self-test). */
    public static int recorded() { return LAST.size(); }
}
