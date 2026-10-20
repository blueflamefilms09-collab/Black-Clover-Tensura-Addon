package com.newuniverse.nusmp.requip;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: Instant Requip is the swap of Equip Magic: up to six saved loadouts (four armour pieces, an offhand item and the nine hotbar slots) and one press to change between them. The swap must be
 * all or nothing and must never lose or double a stack, so the decision is made here, as pure arithmetic over copies of the three places a stack can be (the worn set, the rest of the inventory, the
 * armoury); the game only writes the finished plan back when {@link Status} says it may. A loadout is a TEMPLATE of what each slot should hold, never a store of items: the items themselves stay
 * where they are (the armoury or the inventory) until a swap moves them, so a loadout can never hand out a copy.
 * <p>
 * The rules: for every slot whose template is not empty, the slot is left alone when it already holds a match; otherwise the best match is found (the armoury first, then the rest of the inventory,
 * then another worn slot that is not needed where it is) and moved in, and the stack that was in the slot goes back to the ARMOURY (the cell the incoming stack left, else a free cell). A slot whose
 * template is empty is not touched. A missing item is skipped and counted. If the old set does not fit in the armoury nothing at all changes ({@link Status#ARMOURY_FULL}).
 */
public final class LoadoutRules {
    private LoadoutRules() {}

    /** Four armour slots (head, chest, legs, feet), the offhand, nine hotbar slots. */
    public static final int ARMOUR_SLOTS = 4, OFFHAND = 4, HOTBAR_FIRST = 5, HOTBAR = 9, SLOTS = HOTBAR_FIRST + HOTBAR, MAX_LOADOUTS = 6;
    /** The swap cooldown: one second; Instant-Change shortens it to a few ticks (just enough to stop a held key from swapping every tick). */
    public static final int COOLDOWN_TICKS = 20, INSTANT_COOLDOWN_TICKS = 4;
    /** Mastery at which the Instant-Change passive switches on. */
    public static final double INSTANT_CHANGE_AT = 0.75;

    public static int cooldown(double mastery, boolean privileged) {
        return mastery + 1e-9 >= INSTANT_CHANGE_AT || privileged ? INSTANT_COOLDOWN_TICKS : COOLDOWN_TICKS;
    }

    public enum Status {
        /** Every slot of the template is as it should be now. */
        OK,
        /** Some of the items were found and moved in, some were missing. */
        PARTIAL,
        /** Nothing changed because the worn set already matches the template. */
        UNCHANGED,
        /** Nothing changed because no item of the template exists any more. */
        NOTHING_FOUND,
        /** Nothing changed because the old set does not fit in the armoury. */
        ARMOURY_FULL,
        /** The template has nothing in it. */
        EMPTY_LOADOUT,
        /** The arithmetic did not balance (a bug, never expected): nothing changed. */
        FAILED
    }

    /** How well a candidate stack fits a template: 0 not at all, 1 the same item and identity (a worn weapon, damage ignored), 2 exactly. */
    public interface Matcher<S> { int score(S template, S candidate); }

    /** The result: the three lists as they are to be written back (the inputs are never changed), and the counts for the message. */
    public record Plan<S>(Status status, List<S> worn, List<S> rest, List<S> armoury, int moved, int missing) {
        public boolean applies() { return status == Status.OK || status == Status.PARTIAL; }
    }

    private enum Where { ARMOURY, REST, WORN }

    /**
     * @param template the loadout, {@link #SLOTS} entries (empty = leave the slot alone)
     * @param worn     the worn set now, {@link #SLOTS} entries in the same order
     * @param rest     the inventory outside the worn set (the main inventory)
     * @param armoury  the armoury cells (all of them, {@link ArmouryRules#MAX_SLOTS})
     * @param capacity how many armoury cells take new stacks
     */
    public static <S> Plan<S> plan(List<S> template, List<S> worn, List<S> rest, List<S> armoury, int capacity, ArmouryRules.Ops<S> ops, Matcher<S> matcher) {
        List<S> w = new ArrayList<>(worn), r = new ArrayList<>(rest), a = new ArrayList<>(armoury);
        if (template.size() < SLOTS || w.size() < SLOTS) return new Plan<>(Status.FAILED, worn, rest, armoury, 0, 0);
        boolean any = false;
        for (int i = 0; i < SLOTS; i++) if (!ops.isEmpty(template.get(i))) any = true;
        if (!any) return new Plan<>(Status.EMPTY_LOADOUT, worn, rest, armoury, 0, 0);
        long before = ArmouryRules.total(w, ops) + ArmouryRules.total(r, ops) + ArmouryRules.total(a, ops);

        int moved = 0, missing = 0;
        boolean[] keep = new boolean[SLOTS];
        for (int i = 0; i < SLOTS; i++) keep[i] = !ops.isEmpty(template.get(i)) && matcher.score(template.get(i), w.get(i)) > 0 && !ops.isEmpty(w.get(i));
        for (int i = 0; i < SLOTS; i++) {
            S want = template.get(i);
            if (ops.isEmpty(want) || keep[i]) continue;
            Where bestWhere = null;
            int bestIndex = -1, bestScore = 0;
            // the armoury first, then the rest of the inventory, then another worn slot that is not needed where it is; the best score wins, the earlier source breaks a tie
            for (int j = 0; j < a.size(); j++) {
                int sc = ops.isEmpty(a.get(j)) ? 0 : matcher.score(want, a.get(j));
                if (sc > bestScore) { bestScore = sc; bestWhere = Where.ARMOURY; bestIndex = j; }
            }
            for (int j = 0; j < r.size(); j++) {
                int sc = ops.isEmpty(r.get(j)) ? 0 : matcher.score(want, r.get(j));
                if (sc > bestScore) { bestScore = sc; bestWhere = Where.REST; bestIndex = j; }
            }
            for (int j = 0; j < SLOTS; j++) {
                if (j == i || keep[j] || ops.isEmpty(w.get(j))) continue;
                int sc = matcher.score(want, w.get(j));
                if (sc > bestScore) { bestScore = sc; bestWhere = Where.WORN; bestIndex = j; }
            }
            if (bestWhere == null) { missing++; continue; }
            S incoming;
            switch (bestWhere) {
                case ARMOURY -> { incoming = a.get(bestIndex); a.set(bestIndex, ops.empty()); }
                case REST -> { incoming = r.get(bestIndex); r.set(bestIndex, ops.empty()); }
                default -> { incoming = w.get(bestIndex); w.set(bestIndex, ops.empty()); }
            }
            S outgoing = w.get(i);
            w.set(i, incoming);
            keep[i] = true;                                            // what stands in a finished slot is not taken again for another one
            moved++;
            if (ops.isEmpty(outgoing)) continue;
            // the old stack goes back to the armoury: the cell the incoming one left, else any free cell below the capacity
            if (bestWhere == Where.ARMOURY) { a.set(bestIndex, outgoing); continue; }
            if (!ops.isEmpty(ArmouryRules.insert(a, capacity, outgoing, ops))) return new Plan<>(Status.ARMOURY_FULL, worn, rest, armoury, 0, missing);
        }
        if (moved == 0) return new Plan<>(missing > 0 ? Status.NOTHING_FOUND : Status.UNCHANGED, worn, rest, armoury, 0, missing);
        long after = ArmouryRules.total(w, ops) + ArmouryRules.total(r, ops) + ArmouryRules.total(a, ops);
        if (after != before) return new Plan<>(Status.FAILED, worn, rest, armoury, 0, missing);
        return new Plan<>(missing > 0 ? Status.PARTIAL : Status.OK, w, r, a, moved, missing);
    }

    /** The next saved loadout after {@code current} (or the previous one), wrapping round; {@code current} may be -1. Returns -1 when none is saved. */
    public static int cycle(boolean[] saved, int current, boolean reverse) {
        int n = saved.length;
        if (n == 0) return -1;
        int from = current < 0 && reverse ? 0 : current;
        for (int step = 1; step <= n; step++) {
            int i = reverse ? Math.floorMod(from - step, n) : Math.floorMod(from + step, n);
            if (saved[i]) return i;
        }
        return -1;
    }

    /** True when a slot number is a worn slot index that can take a template (every one of the {@link #SLOTS}). */
    public static boolean validSlot(int s) { return s >= 0 && s < SLOTS; }

    /** True when the template slot is one of the four armour slots (index 0 head, 1 chest, 2 legs, 3 feet in this order). */
    public static boolean isArmourSlot(int s) { return s >= 0 && s < ARMOUR_SLOTS; }
}
