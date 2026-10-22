package com.newuniverse.nusmp.relics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Why: every path that falls back to drawing something plainer than the best look (no model for a relic, the client's own Curios read missed a relic the server knows, a renderer that is
 * not a player renderer drew a player, a slot the Curios inventory does not have) used to be silent, so "my relic is not shown" could not be traced to a cause. Each such path now says its
 * cause once per (cause, relic), in the log ({@code [nusmp] worn relic: ...}) and in the list {@code /multiverse relic debug} prints. The hot path costs one enum-indexed set lookup and
 * allocates nothing once a pair was seen (the message is only built the first time); at most {@link #MAX} distinct pairs are kept, so a flood of different ones cannot grow it. Thread
 * safe, no Minecraft type (the log line goes to a sink that {@code RelicSystem.registerGame} sets to the game's logger).
 */
public final class WornOnce {
    private WornOnce() {}

    /** The most distinct (cause, relic) pairs kept. */
    public static final int MAX = 96;

    /** Why a worn relic is drawn in a plainer way than its best look, or not through the usual path. */
    public enum Cause {
        NO_MODEL("has no worn model in the library: drawn as plain boxes or a floating charm instead"),
        SYNCED_ONLY("was not found by this client's own read of the Curios inventory, only in the list the server sent: drawn from the server's list"),
        MODELS_OFF("is drawn as plain boxes because relicfx.models is off"),
        SHOW_OFF("is not drawn because the player switched all worn relics off (relicfx.showWorn)"),
        RENDERER_NOT_PLAYER_MODEL("is on a player drawn by a renderer whose model is not a player model: the humanoid worn layer was attached to it"),
        RENDERER_NOT_HUMANOID("is on a player drawn by a renderer whose model is not a humanoid model: worn relics cannot be attached to that renderer"),
        LAYER_LATE("is on a player drawn by a renderer that never got the worn layer from the layer event: the layer was attached at the first frame"),
        SLOT_MISSING("sits in a Curios slot type this player's inventory does not have (the slot is not given to the player)"),
        EQUIP_FAILED("could not be put on by right-click");

        public final String text;

        Cause(String text) { this.text = text; }
    }

    private static final Map<Cause, Set<String>> SEEN = new EnumMap<>(Cause.class);
    private static final Map<String, String> MESSAGES = Collections.synchronizedMap(new LinkedHashMap<>());

    static {
        for (Cause c : Cause.values()) SEEN.put(c, ConcurrentHashMap.newKeySet());
    }

    /** True the first time this (cause, relic) pair is seen and room is left; the message is kept for the debug report. */
    public static boolean first(Cause cause, String who) {
        Set<String> set = SEEN.get(cause);
        if (set.contains(who)) return false;                                         // the hot path: no allocation
        synchronized (MESSAGES) {
            if (MESSAGES.size() >= MAX) return false;
            if (!set.add(who)) return false;
            MESSAGES.put(cause.name() + ":" + who, who + " " + cause.text);
            return true;
        }
    }

    private static volatile Consumer<String> sink = line -> { };

    /** Where the log lines go (the game's logger; set once at startup). */
    public static void setSink(Consumer<String> to) { sink = to == null ? line -> { } : to; }

    /** Logs the cause once for this relic (or wearer). Returns true when it was logged now. */
    public static boolean log(Cause cause, String who) {
        if (!first(cause, who)) return false;
        sink.accept(who + " " + cause.text);
        return true;
    }

    /** The messages of the causes seen so far, in the order they were first seen. */
    public static List<String> causes() {
        synchronized (MESSAGES) { return new ArrayList<>(MESSAGES.values()); }
    }

    /** How many distinct pairs were seen. */
    public static int count() { return MESSAGES.size(); }

    /** Forgets everything (leaving a world, a test). */
    public static void clear() {
        synchronized (MESSAGES) {
            MESSAGES.clear();
            for (Set<String> s : SEEN.values()) s.clear();
        }
    }
}
