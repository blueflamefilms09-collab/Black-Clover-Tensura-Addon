package com.newuniverse.nusmp.shishou;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: everything about Shishou that is NOT saved and belongs to one player: who is holding him right now, the cooldown table, the open windows (barrier, sense, stealth, released), the clones, the
 * anti-theft clocks, the speech limits, the key and Appraisal gaps. It is all keyed by the player's UUID (never a static "current player"), single threaded (the server thread), small (one wielder at most, plus
 * any thief for ten seconds), and dropped at logout, death, dimension change, respawn and server stop by {@link #forget} / {@link #clearAll}. The saved truth is {@link ShishouData}.
 */
public final class ShishouLive {
    private ShishouLive() {}

    public static final ShishouTheft THEFT = new ShishouTheft();
    public static final ShishouCooldowns COOLDOWNS = new ShishouCooldowns();
    public static final ShishouWindows WINDOWS = new ShishouWindows();
    public static final ShishouClones CLONES = new ShishouClones();
    public static final ShishouSpeechGate GATE = new ShishouSpeechGate();

    /** The wielder -> the game tick of the last beat in which they held the sword (a link lasts two beats). */
    static final Map<UUID, Long> LINKED = new HashMap<>();
    static final Map<UUID, Long> KEY_GAP = new HashMap<>(), APPRAISE_GAP = new HashMap<>(), CHARGE_TICK = new HashMap<>(), FEED_GAP = new HashMap<>();
    static final Map<UUID, Float> CHARGE = new HashMap<>();
    static final Map<UUID, Integer> SPEECH_COUNTER = new HashMap<>();
    /** Spatial Blade: the wielder's next blow is a full-charge swing (set by the left click, consumed by the hit). */
    static final Map<UUID, Long> BLADE_READY = new HashMap<>();
    /** The released Divine Sword and the Black Flame bonus windows live in WINDOWS; these are the wielder's own flags. */
    static final Map<UUID, Long> LOW_MANA_TOLD = new HashMap<>();

    public static boolean isLinked(UUID who, long now) {
        Long t = LINKED.get(who);
        return t != null && now - t <= 2L * ShishouRules.THEFT_CHECK + 2L;
    }

    public static boolean anyLinked() { return !LINKED.isEmpty(); }

    /** A player left, died, changed dimension or respawned: every transient thing of theirs goes. */
    public static void forget(UUID who) {
        THEFT.forget(who);
        COOLDOWNS.forget(who);
        WINDOWS.forget(who);
        CLONES.forget(who);
        GATE.forget(who);
        LINKED.remove(who);
        KEY_GAP.remove(who);
        APPRAISE_GAP.remove(who);
        FEED_GAP.remove(who);
        CHARGE_TICK.remove(who);
        CHARGE.remove(who);
        SPEECH_COUNTER.remove(who);
        BLADE_READY.remove(who);
        LOW_MANA_TOLD.remove(who);
    }

    public static void clearAll() {
        THEFT.clear();
        COOLDOWNS.clear();
        WINDOWS.clear();
        CLONES.clear();
        GATE.clear();
        LINKED.clear();
        KEY_GAP.clear();
        APPRAISE_GAP.clear();
        FEED_GAP.clear();
        CHARGE_TICK.clear();
        CHARGE.clear();
        SPEECH_COUNTER.clear();
        BLADE_READY.clear();
        LOW_MANA_TOLD.clear();
    }

    /** A gap table: true (and recorded) when {@code now} is at least {@code gap} ticks after the last accepted ask. */
    public static boolean gap(Map<UUID, Long> table, UUID who, long now, long gap) {
        Long t = table.get(who);
        if (t != null && now - t < gap) return false;
        table.put(who, now);
        return true;
    }

    public static int liveCount() { return LINKED.size() + THEFT.tracked() + WINDOWS.tracked() + CLONES.count() + COOLDOWNS.tracked(); }
}
