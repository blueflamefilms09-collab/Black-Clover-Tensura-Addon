package com.newuniverse.nusmp.arc.lines;

import com.newuniverse.nusmp.audio.voice.Voice;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Why: the one place where the Goku and the Sinbad boss speak with their own lines (docs/boss_redesign_goku_sinbad.md, "Voice"). SERVER only: the boss's chat text always goes out (the subtitle of a line without a
 * recording), and when the line has a voice file ({@link BossLines#isRegistered}) it is also sent as a sound to the players within {@link #RANGE} blocks, which shows the same words as its subtitle.
 * Every call passes {@link BossLineGate} (a rest per group, a gap between two lines, a bounded memory per boss), so a boss never floods the chat or the network. Cost: one gate lookup per call and,
 * for a registered line, one packet per player within {@link #RANGE} blocks, at most one line per {@link BossLineGate#MIN_GAP} ticks per boss (priority lines excepted: the fight start, a transformation, the end).
 */
public final class BossSpeech {
    private BossSpeech() {}

    /** Players farther than this do not hear a line (the voice option's range clamps it further). */
    public static final double RANGE = 48.0;

    private static final BossLineGate GATE = new BossLineGate();
    /** Per boss: the players seen dead in this fight, so a death is said once. At most 32 each, 16 bosses. */
    private static final Map<UUID, Set<UUID>> DEAD = new HashMap<>();

    /** The boss says a line of this group. Returns true when something was said. */
    public static boolean say(ZogratisBoss b, String boss, String group) {
        String id = take(b, boss, group);
        if (id == null) return false;
        b.say(BossLines.text(id));
        send(b, id);
        return true;
    }

    /** The words of a line of this group for a hook that returns text (the opening and the death line of the framework), sending the voice; {@code fallback} when the gate or the group says no. */
    public static String text(ZogratisBoss b, String boss, String group, String fallback) {
        String id = take(b, boss, group);
        if (id == null) return fallback;
        send(b, id);
        return BossLines.text(id);
    }

    /** The boss starts to cast {@code moveId}: its call-out, if it has one. */
    public static void callout(ZogratisBoss b, String boss, String moveId) {
        String group = BossLines.calloutGroup(boss, moveId);
        if (group != null) say(b, boss, group);
    }

    /** Looks for the players of the fight who have died since the last call; says the boss's line for the first one. Call about once a second. */
    public static void watchDeaths(ZogratisBoss b, String boss) {
        if (!(b.level() instanceof ServerLevel sl) || b.fighters().isEmpty()) return;
        Set<UUID> seen = DEAD.computeIfAbsent(b.getUUID(), k -> new HashSet<>());
        boolean fresh = false;
        for (UUID id : b.fighters()) {
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(id);
            if (p == null) continue;
            if (p.isDeadOrDying()) {
                if (seen.size() < 32 && seen.add(id)) fresh = true;
            } else {
                seen.remove(id);
            }
        }
        if (DEAD.size() > BossLineGate.MAX_BOSSES) DEAD.clear();
        if (fresh) say(b, boss, "player_death");
    }

    /** True when a player of the fight is dead or dying right now (the boss won the fight). */
    public static boolean anyFighterDown(ZogratisBoss b) {
        if (!(b.level() instanceof ServerLevel sl)) return false;
        for (UUID id : b.fighters()) {
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(id);
            if (p != null && p.isDeadOrDying()) return true;
        }
        return false;
    }

    /** Drops what is remembered of a boss (it is removed, or its fight ended). */
    public static void forget(UUID boss) {
        GATE.forget(boss);
        DEAD.remove(boss);
    }

    public static void clear() {
        GATE.clear();
        DEAD.clear();
    }

    private static String take(ZogratisBoss b, String boss, String group) {
        if (b == null || b.level().isClientSide || !(b.level() instanceof ServerLevel) || !BossLines.hasGroup(boss, group)) return null;
        if (!GATE.allow(b.getUUID(), group, b.tickCount)) return null;
        return BossLines.pick(boss, group, GATE.next(b.getUUID(), group));
    }

    private static void send(ZogratisBoss b, String id) {
        if (BossLines.isRegistered(id) && b.level() instanceof ServerLevel sl) Voice.sayNear(sl, b.position(), id, RANGE);
    }
}
