package com.newuniverse.nusmp.arc;

import com.newuniverse.nusmp.labyrinth.mega.Polarity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Why: one place that answers "who are the characters of the arcs" for every system (the floor-boss picker, the story unlock, the NPC entity, the
 * Charter table, the advancement generator, the commands), instead of each system keeping its own list. Characters register themselves from
 * {@code ArcCharacters.bootstrap()} (the one list the lead maintains), in a fixed order, so the order of the roster is the order of the story and of the
 * saved ids. Registering the same instance twice is harmless (a reload); a DIFFERENT character under an id already taken is a bug and throws at once, at
 * boot, instead of a silent overwrite.
 */
public final class ArcRoster {
    private ArcRoster() {}

    private static final Map<String, ArcCharacter> BY_ID = new LinkedHashMap<>();

    /** Adds a character. Returns it, so a class can write {@code CHARACTER = ArcRoster.register(...)}. */
    public static synchronized ArcCharacter register(ArcCharacter c) {
        ArcCharacter old = BY_ID.get(c.id());
        if (old != null && !old.equals(c)) throw new IllegalStateException("two different characters registered under the id " + c.id());
        BY_ID.put(c.id(), c);
        return c;
    }

    /** Forgets every character (tests only). */
    public static synchronized void clearForTests() { BY_ID.clear(); }

    public static synchronized List<ArcCharacter> all() { return Collections.unmodifiableList(new ArrayList<>(BY_ID.values())); }

    public static synchronized ArcCharacter byId(String id) { return id == null ? null : BY_ID.get(id.toLowerCase(Locale.ROOT)); }

    public static List<ArcCharacter> bosses() { return filter(ArcRole.BOSS, null); }

    public static List<ArcCharacter> npcs() { return filter(ArcRole.NPC, null); }

    public static List<ArcCharacter> ofSaga(ArcSaga saga) { return filter(null, saga); }

    private static synchronized List<ArcCharacter> filter(ArcRole role, ArcSaga saga) {
        List<ArcCharacter> out = new ArrayList<>();
        for (ArcCharacter c : BY_ID.values()) if ((role == null || c.role() == role) && (saga == null || c.saga() == saga)) out.add(c);
        return out;
    }

    /** The bosses that may stand as the floor boss of this absolute floor on this road, in roster order. */
    public static List<ArcCharacter> floorBosses(int floor, Polarity road) {
        List<ArcCharacter> out = new ArrayList<>();
        for (ArcCharacter c : bosses()) if (c.standsOn(floor, road)) out.add(c);
        return out;
    }

    /** The characters whose defeat unlocks {@code c} (looked up; ids that are not registered are skipped). */
    public static List<ArcCharacter> prerequisitesOf(ArcCharacter c) {
        List<ArcCharacter> out = new ArrayList<>();
        for (String id : c.prerequisites()) { ArcCharacter p = byId(id); if (p != null) out.add(p); }
        return out;
    }
}
