package com.newuniverse.nusmp.relics.wave;

import net.minecraft.world.entity.LivingEntity;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the Eye (a mind read), the Puzzle (a penalty game) and the Scale (a weighed heart) each leave a mark on a foe for a few seconds, and the one who marked it deals a little more to it
 * while the mark lasts. The table is server memory only, keyed by the foe's UUID with the marker's UUID inside (so several players can mark the same foe without touching each other), every
 * timer is in game time, it is bounded and it is cleared when the server stops.
 */
final class Wave101Marks {
    private Wave101Marks() {}

    private record Entry(UUID by, long until) {}

    private static final Map<UUID, EnumMap<Wave101Rules.Mark, Entry>> MARKS = new HashMap<>();
    static final int CAP = 512;

    /** Marks {@code foe} for {@code by} until the game time {@code until} (a later mark of the same kind replaces an earlier one). */
    static void put(LivingEntity foe, UUID by, Wave101Rules.Mark kind, long until) {
        if (foe == null || by == null || kind == null) return;
        if (MARKS.size() >= CAP) MARKS.clear();
        MARKS.computeIfAbsent(foe.getUUID(), k -> new EnumMap<>(Wave101Rules.Mark.class)).put(kind, new Entry(by, until));
    }

    /** The extra share of damage {@code by} deals to {@code foe} at game time {@code now} (0 when nothing of theirs marks it). */
    static double bonus(LivingEntity foe, UUID by, long now) {
        if (foe == null || by == null || MARKS.isEmpty()) return 0.0;
        EnumMap<Wave101Rules.Mark, Entry> m = MARKS.get(foe.getUUID());
        if (m == null) return 0.0;
        boolean read = live(m.get(Wave101Rules.Mark.READ), by, now), penalty = live(m.get(Wave101Rules.Mark.PENALTY), by, now), weighed = live(m.get(Wave101Rules.Mark.WEIGHED), by, now);
        return Wave101Rules.markBonus(read, penalty, weighed);
    }

    private static boolean live(Entry e, UUID by, long now) { return e != null && e.by().equals(by) && now < e.until(); }

    /** Drops the marks whose time is up. */
    static void purge(long now) {
        if (MARKS.isEmpty()) return;
        for (Iterator<Map.Entry<UUID, EnumMap<Wave101Rules.Mark, Entry>>> it = MARKS.entrySet().iterator(); it.hasNext(); ) {
            EnumMap<Wave101Rules.Mark, Entry> m = it.next().getValue();
            m.values().removeIf(e -> now >= e.until());
            if (m.isEmpty()) it.remove();
        }
    }

    static int size() { return MARKS.size(); }

    static void clear() { MARKS.clear(); }
}
