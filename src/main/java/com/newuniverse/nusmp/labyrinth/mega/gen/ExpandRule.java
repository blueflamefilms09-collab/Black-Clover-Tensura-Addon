package com.newuniverse.nusmp.labyrinth.mega.gen;

import java.util.List;

/**
 * Why: {@code /multiverse mega expand [holy|abyssal]} is the admin's (and the CI smoke test's) one-word way to make a road of the dungeon grow, and "grow" means two
 * different things: with no sector on the road yet it creates the road's first sector, and otherwise it hangs the next sector off the newest sector of that road that
 * still has a free Way Onward (the command gives this rule the rows of ONE road). Which sector that is, and what to say when none can grow, is a small decision with
 * awkward corners (the newest sector is still being built, the newest one failed, the last one is a dead end that has no seal), so it is made here on plain numbers and
 * pinned by tests, and the command only carries it out. {@link #refusalForNext} is the same kind of rule for the facade's request: when may the sector behind a given
 * sector be built.
 */
public final class ExpandRule {
    private ExpandRule() {}

    /** What the rule needs to know about one sector. */
    public record Row(int id, boolean hasSeal, int nextId, boolean generating, boolean failed) {}

    /**
     * The decision: {@code first} means "create the road's first sector", otherwise {@code from} is the sector to hang the next one off; {@code refusal} is non-null when nothing
     * can grow (then the other two are meaningless).
     */
    public record Decision(boolean first, int from, String refusal) {
        static Decision bootstrap() { return new Decision(true, -1, null); }

        static Decision from(int id) { return new Decision(false, id, null); }

        static Decision refuse(String why) { return new Decision(false, -1, why); }
    }

    /**
     * Why a sector cannot have a next sector hung off its Way Onward right now, or null when it can (1.55.0 states it as a rule because the dungeon no longer builds
     * the next sector the moment the Gatekeeper opens: with {@code mega.expandAfterVault} it waits for the Crucible, so the request may arrive at ANY time after the
     * Guardian is dead). The only things that stop it are the sector's own condition (still being generated, failed, no Way Onward), a successor that already
     * exists, the limit on the size of the dungeon, and a road that already has a sector in the slot a new one would take; its progress (Guardian alive, Guardian dead,
     * Gatekeeper open, Crucible resolved) never does, which is what lets an admin grow the dungeon at will and the Crucible's resolution grow it on time, and what
     * makes a second request for the same sector refuse instead of building a second successor.
     *
     * @param from        the sector the new one would hang from
     * @param sectors     how many sectors exist
     * @param maxSectors  the configured limit
     * @param slotTaken   does the road of {@code from} already have a sector at the next chain index (a successor whose link was lost)?
     */
    public static String refusalForNext(Row from, int sectors, int maxSectors, boolean slotTaken) {
        if (from == null) return "no sector";
        if (from.nextId() != -1) return "sector " + from.id() + " already leads to sector " + from.nextId();
        if (from.generating()) return "sector " + from.id() + " is still generating";
        if (from.failed()) return "sector " + from.id() + " failed to generate (retry it with /multiverse mega regen " + from.id() + ")";
        if (!from.hasSeal()) return "sector " + from.id() + " has no Way Onward";
        if (slotTaken) return "the road of sector " + from.id() + " already has a sector after it (its link was lost: look at /multiverse mega status)";
        if (sectors >= maxSectors) return "the dungeon reached maxSectors (" + maxSectors + ")";
        return null;
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
