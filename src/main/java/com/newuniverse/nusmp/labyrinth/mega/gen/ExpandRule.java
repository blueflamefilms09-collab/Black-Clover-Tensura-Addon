package com.newuniverse.nusmp.labyrinth.mega.gen;

import java.util.List;

/**
 * Why: {@code /multiverse mega expand} is the admin's (and the CI smoke test's) one-word way to make the dungeon grow, and "grow" means two different things:
 * with no sector yet it creates sector 0, and otherwise it hangs the next sector off the newest sector that still has a free Way Onward. Which sector that is,
 * and what to say when none can grow, is a small decision with awkward corners (the newest sector is still being built, the newest one failed, the last one
 * is a dead end that has no seal), so it is made here on plain numbers and pinned by tests, and the command only carries it out.
 */
public final class ExpandRule {
    private ExpandRule() {}

    /** What the rule needs to know about one sector. */
    public record Row(int id, boolean hasSeal, int nextId, boolean generating, boolean failed) {}

    /**
     * The decision: {@code first} means "create sector 0", otherwise {@code from} is the sector to hang the next one off; {@code refusal} is non-null when nothing
     * can grow (then the other two are meaningless).
     */
    public record Decision(boolean first, int from, String refusal) {
        static Decision bootstrap() { return new Decision(true, -1, null); }

        static Decision from(int id) { return new Decision(false, id, null); }

        static Decision refuse(String why) { return new Decision(false, -1, why); }
    }

    public static Decision decide(List<Row> sectors) {
        if (sectors.isEmpty()) return Decision.bootstrap();
        Row best = null;
        Row generating = null, failed = null;
        for (Row r : sectors) {
            if (r.generating() && (generating == null || r.id() > generating.id())) generating = r;
            if (r.failed() && (failed == null || r.id() > failed.id())) failed = r;
            if (r.hasSeal() && r.nextId() < 0 && !r.generating() && !r.failed() && (best == null || r.id() > best.id())) best = r;
        }
        if (best != null) return Decision.from(best.id());
        if (generating != null) return Decision.refuse("sector " + generating.id() + " is still generating");
        if (failed != null) return Decision.refuse("sector " + failed.id() + " failed to generate, retry it with regen " + failed.id());
        return Decision.refuse("no sector has a free Way Onward");
    }
}
