package com.newuniverse.nusmp.relics.djinn.weapon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The loan of a Djinn weapon, as a pure state machine (no Minecraft types, so WeaponLoanTest can throw random sequences of events at it): while a
 * Half or Full Equip lasts, the temporary weapon sits in one hotbar slot (the home slot, the one that was selected when it was summoned). Whatever
 * stood in that slot is displaced, never deleted: it goes to the first free slot of the 36 main slots (and is remembered, so the end of the loan can
 * put it back where it was), or, with no free slot, to the stash list, which the caller saves with the player and which gives its items back at the
 * end.
 *
 * <p>The one exception is the bearer's own vessel ({@link Ops#overlays}): when that stands in the home slot (a weapon that is also the Djinn's vessel)
 * the weapon overlays it in place. The vessel is not moved to another slot (a vessel that leaves the hands is no longer found, which ended the Equip);
 * it is kept in the loan state, in no slot at all, and returns to the home slot unchanged at the end.
 *
 * <p>The slots are a flat view the caller builds over the player's inventory: indices 0-8 the hotbar, 9-35 the rest of the main inventory
 * ({@link #MAIN} slots, the only ones an item is ever placed in), then any number of other places a weapon must not be (the offhand, the armour, the
 * cursor): {@link #ensure} and {@link #release} wipe a weapon from all of them. Nothing but the weapon token is ever removed, and the only items that
 * leave the state machine are the ones {@link #release} hands back as overflow, for the caller to drop at the player's feet.
 */
public final class WeaponLoan<T> {
    /** The slots the loan reads and writes. */
    public interface Slots<T> {
        int size();
        T get(int i);
        void set(int i, T t);
    }

    /** What the loan needs to know about an item. */
    public interface Ops<T> {
        T empty();
        boolean isEmpty(T t);
        boolean isWeapon(T t);
        boolean same(T a, T b);

        /**
         * Is this item one the weapon must OVERLAY instead of displace: the bearer's own vessel. A vessel is only found while it is held, worn or
         * carried in a Curios slot, so moving a vessel weapon out of the selected slot made the engine lose it and end the Equip ("your vessel left
         * you"). An overlaid item stays out of every slot (it lives in the loan's record) and goes back to the home slot, unchanged, when the loan ends.
         */
        default boolean overlays(T t) { return false; }
    }

    public static final int HOTBAR = 9, MAIN = 36;

    private final Ops<T> ops;
    private int home = -1, moved = -1;
    private T movedItem;
    private T overlaid;
    private final List<T> stash = new ArrayList<>();

    public WeaponLoan(Ops<T> ops) { this.ops = ops; }

    public boolean active() { return home >= 0; }

    public int home() { return home; }

    /** The free slot the displaced item went to, or -1. */
    public int moved() { return moved; }

    public T movedItem() { return movedItem; }

    /** The item the weapon overlays in the home slot (the bearer's vessel), or null. It is in no inventory slot while the loan lasts. */
    public T overlaid() { return overlaid; }

    public List<T> stash() { return Collections.unmodifiableList(stash); }

    /** Takes a saved state back (the caller read it from the player's record). A home outside the hotbar means "no loan". */
    public void load(int home, int moved, T movedItem, List<T> stash) { load(home, moved, movedItem, stash, null); }

    /**
     * The same with the overlaid item (the vessel the weapon sits over). An empty or weapon item is read as none. An overlaid item with no valid home
     * is still kept, so that {@link #release} can give it back (to the first free slot).
     */
    public void load(int home, int moved, T movedItem, List<T> stash, T overlaid) {
        this.home = home >= 0 && home < HOTBAR ? home : -1;
        this.moved = moved >= 0 && moved < MAIN ? moved : -1;
        this.movedItem = this.moved >= 0 ? movedItem : null;
        this.overlaid = overlaid != null && !ops.isEmpty(overlaid) && !ops.isWeapon(overlaid) ? overlaid : null;
        this.stash.clear();
        if (stash != null) for (T t : stash) if (!ops.isEmpty(t) && !ops.isWeapon(t)) this.stash.add(t);
    }

    // ---------------------------------------------------------------- summoning
    /**
     * Puts the weapon in the selected hotbar slot. A loan already running is only made whole again ({@link #ensure}).
     * {@code current} tells a weapon of the right kind from a stale one (another Djinn's, the other look).
     */
    public void summon(Slots<T> s, int selected, Supplier<T> weapon, Predicate<T> current) {
        if (active()) {
            ensure(s, weapon, current);
            return;
        }
        home = Math.max(0, Math.min(HOTBAR - 1, selected));
        moved = -1;
        movedItem = null;
        T stale = overlaid;                                                    // an overlaid item left by a record that lost its home: it is not dropped, it is displaced like any other
        overlaid = null;
        wipe(s);
        T occupant = s.get(home);
        if (!ops.isEmpty(occupant)) {
            if (ops.overlays(occupant)) overlaid = occupant;                  // the vessel stays in the hand's place: not moved, the weapon takes its slot
            else displace(s, occupant);
        }
        if (stale != null) displace(s, stale);
        s.set(home, weapon.get());
    }

    /**
     * Keeps the loan whole: exactly one current weapon, in the home slot, none anywhere else. A weapon that strayed is wiped; whatever the player put
     * in the home slot meanwhile is displaced like the original was; a missing weapon is made again.
     */
    public void ensure(Slots<T> s, Supplier<T> weapon, Predicate<T> current) {
        if (!active()) return;
        boolean ok = false;
        for (int i = 0; i < s.size(); i++) {
            T t = s.get(i);
            if (!ops.isWeapon(t)) continue;
            if (i == home && !ok && current.test(t)) ok = true;
            else s.set(i, ops.empty());
        }
        if (ok) return;
        T occupant = s.get(home);
        if (!ops.isEmpty(occupant)) {
            if (overlaid == null && ops.overlays(occupant)) overlaid = occupant;      // the vessel was put into the home slot meanwhile: it is overlaid too, not shoved out of the hand
            else displace(s, occupant);
        }
        s.set(home, weapon.get());
    }

    // ---------------------------------------------------------------- ending
    /**
     * Ends the loan: every weapon is wiped, the original item returns to the home slot (from the stash, else from where it was moved to), the rest of
     * the stash fills free slots. What does not fit is returned for the caller to drop. Safe to call with no loan (it still wipes stray weapons).
     */
    public List<T> release(Slots<T> s) {
        wipe(s);
        List<T> overflow = new ArrayList<>();
        if (home < 0 && stash.isEmpty() && overlaid == null) {
            reset();
            return overflow;
        }
        List<T> toPlace = new ArrayList<>(stash);
        T first = null;
        if (overlaid != null) first = overlaid;                               // the vessel the weapon overlaid goes back first, into its own slot
        else if (!toPlace.isEmpty()) first = toPlace.remove(0);
        else if (home >= 0 && moved >= 0 && movedItem != null) {
            T there = s.get(moved);
            if (!ops.isEmpty(there) && !ops.isWeapon(there) && ops.same(there, movedItem) && ops.isEmpty(s.get(home))) {
                s.set(home, there);
                s.set(moved, ops.empty());
            }
        }
        if (first != null) {
            if (home >= 0 && ops.isEmpty(s.get(home))) s.set(home, first);
            else place(s, first, overflow);
        }
        for (T t : toPlace) place(s, t, overflow);
        reset();
        return overflow;
    }

    // ---------------------------------------------------------------- inside
    private void wipe(Slots<T> s) {
        for (int i = 0; i < s.size(); i++) if (ops.isWeapon(s.get(i))) s.set(i, ops.empty());
    }

    private void reset() {
        home = -1;
        moved = -1;
        movedItem = null;
        overlaid = null;
        stash.clear();
    }

    private int firstFree(Slots<T> s, int exclude) {
        int n = Math.min(MAIN, s.size());
        for (int i = 0; i < n; i++) if (i != exclude && ops.isEmpty(s.get(i))) return i;
        return -1;
    }

    private void displace(Slots<T> s, T item) {
        int free = firstFree(s, home);
        if (free >= 0) {
            s.set(free, item);
            if (moved < 0) {
                moved = free;
                movedItem = item;
            }
        } else {
            stash.add(item);
        }
    }

    private void place(Slots<T> s, T item, List<T> overflow) {
        int free = firstFree(s, -1);
        if (free >= 0) s.set(free, item);
        else overflow.add(item);
    }
}
