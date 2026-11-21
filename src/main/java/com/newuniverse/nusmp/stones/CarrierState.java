package com.newuniverse.nusmp.stones;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the few things Phase B remembers between ticks (a thing lifted by telekinesis, an open Mind Link, the last mastery award, the socket mask of what a player carries, the circuit boost of the act
 * that is running), in one place, every table keyed by the player's UUID and emptied on logout, death and dimension change so nothing grows with the age of the server. Server thread only (no locking).
 * Size: one lift and one link per player, at most {@link StoneConfig#maxLifts()} lifts and {@link StoneConfig#maxLinks()} links in all, one small entry per carrier owner.
 */
final class CarrierState {
    private CarrierState() {}

    /** A thing a player holds in the air: its UUID, the dimension, and the game tick it is thrown at the latest. */
    record Lift(UUID target, String dim, long until) {}

    /** An open Mind Link: the allies (at most four), the dimension, when it ends and when the next pulse is due. */
    record Link(List<UUID> allies, String dim, long until, long nextPulse) {}

    private static final Map<UUID, Lift> LIFTS = new HashMap<>();
    private static final Map<UUID, Link> LINKS = new HashMap<>();
    private static final Map<UUID, Long> MASTERY = new HashMap<>();
    private static final Map<UUID, Integer> MASKS = new HashMap<>();

    /** The circuit boost of the act that is running through a carrier (set around its body by {@link CarrierActs}); 1.0 otherwise. */
    private static double boost = 1.0;

    static double boost() { return boost; }

    static void setBoost(double b) { boost = b <= 0 ? 1.0 : Math.min(b, 2.0); }

    // ---------------------------------------------------------------- lifts
    static boolean openLift(UUID caster, Lift l, int cap) {
        if (!LIFTS.containsKey(caster) && !CarrierRules.liftRoom(LIFTS.size(), cap)) return false;
        LIFTS.put(caster, l);
        return true;
    }

    static Lift lift(UUID caster) { return LIFTS.get(caster); }

    static void closeLift(UUID caster) { LIFTS.remove(caster); }

    static Map<UUID, Lift> lifts() { return LIFTS; }

    // ---------------------------------------------------------------- links
    static boolean openLink(UUID caster, Link l, int cap) {
        if (!LINKS.containsKey(caster) && LINKS.size() >= cap) return false;
        LINKS.put(caster, l);
        return true;
    }

    /** Replaces an open link in place (the next pulse); never adds one. */
    static void replaceLink(UUID caster, Link l) { if (LINKS.containsKey(caster)) LINKS.put(caster, l); }

    static Link link(UUID caster) { return LINKS.get(caster); }

    static void closeLink(UUID caster) { LINKS.remove(caster); }

    static Map<UUID, Link> links() { return LINKS; }

    // ---------------------------------------------------------------- mastery bonus
    static long lastMastery(UUID id) { return MASTERY.getOrDefault(id, 0L); }

    static void masteryAwarded(UUID id, long now) { MASTERY.put(id, now); }

    // ---------------------------------------------------------------- what a player carries
    static int mask(UUID id) { return MASKS.getOrDefault(id, 0); }

    static void setMask(UUID id, int mask) {
        if (mask == 0) MASKS.remove(id);
        else MASKS.put(id, mask);
    }

    // ---------------------------------------------------------------- cleanup
    /** Death, dimension change: what was in motion ends (a lifted thing falls, a link closes). */
    static void forgetMotion(UUID id) {
        LIFTS.remove(id);
        LINKS.remove(id);
    }

    /** Logout. */
    static void forgetAll(UUID id) {
        forgetMotion(id);
        MASTERY.remove(id);
        MASKS.remove(id);
    }

    static void clear() {
        LIFTS.clear();
        LINKS.clear();
        MASTERY.clear();
        MASKS.clear();
        boost = 1.0;
    }

    static List<UUID> liftOwners() { return new ArrayList<>(LIFTS.keySet()); }

    static List<UUID> linkOwners() { return new ArrayList<>(LINKS.keySet()); }
}
