package com.newuniverse.nusmp.labyrinth.mega.spawnfilter;

/**
 * Why: the whole "may this creature enter the Dungeon" decision as one pure function over plain facts, so the truth table is unit tested and the event glue
 * ({@link SpawnFilterEvents}) only gathers the facts. Two layers, in this order. The BAN (the {@code spawn.blacklist} list and the {@code nusmp:dungeon/banned}
 * tag) is absolute: it applies to every way a creature can arrive, a creature loaded from an old save included (that is how a Gorgon already standing in a
 * chunk is cleaned up), and only an operator's own {@code /summon} or spawn egg can bypass it, and only when the config says so (off by default: "never"
 * means never). The THEME palette ({@link FloorThemeMobFilter}) is softer: it applies only to a fresh spawn of a creature that has a faction, never to an
 * operator's, an owned, a named or a boss creature, and only while its switch is on.
 */
public final class SpawnFilterRules {
    private SpawnFilterRules() {}

    public enum Verdict { ALLOW, DENY_BANNED, DENY_THEME }

    /**
     * @param banned          the creature's type is on the blacklist or in the banned tag
     * @param adminSpawn      an operator asked for it (command or spawn egg)
     * @param adminBypassesBan the config lets an operator's creature through the ban
     * @param exemptMob       owned, named or a boss: never subject to the theme palette
     * @param freshSpawn      a new creature, not one loaded from a saved chunk
     * @param themeOn         the palette switch
     * @param palette         the palette of the floor the creature appears on (null = no floor)
     * @param faction         the creature's faction id, or null
     */
    public static Verdict judge(boolean banned, boolean adminSpawn, boolean adminBypassesBan, boolean exemptMob, boolean freshSpawn, boolean themeOn,
                                FloorThemeMobFilter.Palette palette, String faction) {
        if (banned) return adminSpawn && adminBypassesBan ? Verdict.ALLOW : Verdict.DENY_BANNED;
        if (!themeOn || adminSpawn || exemptMob || !freshSpawn) return Verdict.ALLOW;
        return FloorThemeMobFilter.allows(palette, faction) ? Verdict.ALLOW : Verdict.DENY_THEME;
    }
}
