package com.newuniverse.nusmp.labyrinth.mega.system;

/**
 * Why: the loaded quest catalog and stat table are read from the server thread (the roller, the reward, the attribute application), from commands and from the
 * self-tests, and replaced by the data reload (which runs on a reload thread's completion on the server executor). Two volatile references, each swapped in one
 * assignment by {@link SystemReload}, mean a reader always sees a complete catalog or a complete table and never a half-filled one, with no locking. Before the
 * first reload (a very early call) both are empty and every consumer treats empty as "nothing to roll" / "nothing to apply" instead of failing.
 */
public final class SystemData {
    private SystemData() {}

    private static volatile QuestCatalog quests = QuestCatalog.EMPTY;
    private static volatile StatTable stats = StatTable.EMPTY;

    public static QuestCatalog quests() { return quests; }

    public static StatTable stats() { return stats; }

    /** Installs freshly loaded data (the reload listener, tests). */
    public static void install(QuestCatalog q, StatTable s) {
        quests = q == null ? QuestCatalog.EMPTY : q;
        stats = s == null ? StatTable.EMPTY : s;
    }
}
