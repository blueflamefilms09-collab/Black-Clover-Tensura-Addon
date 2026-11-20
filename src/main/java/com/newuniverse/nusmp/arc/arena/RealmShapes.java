package com.newuniverse.nusmp.arc.arena;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.realm.BossRealm;
import com.newuniverse.nusmp.arc.realm.BossRealmConfig;
import com.newuniverse.nusmp.arc.realm.BossRealmRules;

import java.util.List;

/**
 * Why: the boss realm's own arenas (the Isle of the Seven Seas for Sinbad, the Tournament Wastes for Goku) are kept in a registry of their own and NOT added to {@link ArenaShapes#all()}: that list
 * is the six story arenas (pinned by {@code ArenaShapesTest}: six, the ring last) and stays exactly as it was. {@link ArenaShapes#forCharacter} asks this class first; it answers a shape only for
 * a realm boss that has one, and only while {@code bossRealm.enabled} is on, so turning the setting off gives every character the arena it had before. The id table is read first, so a character
 * that is not one of them never touches the settings.
 */
public final class RealmShapes {
    private RealmShapes() {}

    public static final ArenaShape SEVEN_SEAS_ISLE = new SevenSeasIsleShape();
    public static final ArenaShape TOURNAMENT_GROUNDS = new TournamentGroundsShape();

    private static final List<ArenaShape> ALL = List.of(SEVEN_SEAS_ISLE, TOURNAMENT_GROUNDS);

    /** The realm arenas (not the story arenas), in a fixed order. */
    public static List<ArenaShape> all() { return ALL; }

    /** The realm arena with this id, or null. */
    public static ArenaShape byId(String id) {
        if (id == null) return null;
        for (ArenaShape s : ALL) if (s.id().equalsIgnoreCase(id.trim())) return s;
        return null;
    }

    /** The realm arena of a character, or null (the story arena's rule applies): it must be a realm boss with an arena of its own and the realm must be switched on. */
    public static ArenaShape forCharacter(ArcCharacter c) {
        if (c == null) return null;
        String id = BossRealmRules.shapeId(c.id());
        if (id == null || !BossRealmConfig.enabled() || !BossRealm.isRealmBoss(c)) return null;
        return byId(id);
    }
}
