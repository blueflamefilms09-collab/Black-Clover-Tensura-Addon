package com.newuniverse.nusmp.elite.records.rule;

/**
 * Why: the counters the records system keeps itself (the game does not count them for it) and the one place that says how each event changes {@link RecordData}: a kill adds to the total, to its group
 * (hostile / passive / player / boss) and to the per-entity count; a hit raises the best single hit; a forged item counts once and raises the best quality; and so on. The game-side event handlers and
 * the public {@code RecordEvents} API only decide WHICH of these to call, so the arithmetic is unit-tested here without the game. Every method ignores nonsense input (negative, NaN, empty ids).
 */
public final class Tracking {
    private Tracking() {}

    /** Kill group flags of a victim. */
    public record Victim(String entityId, boolean hostile, boolean passive, boolean player, boolean boss) {}

    public static void kill(RecordData d, Victim v) {
        if (v == null || v.entityId() == null || v.entityId().isEmpty()) return;
        d.addCounter("kills_all", 1);
        if (v.hostile()) d.addCounter("kills_hostile", 1);
        if (v.passive()) d.addCounter("kills_passive", 1);
        if (v.player()) d.addCounter("kills_player", 1);
        if (v.boss()) d.addCounter("kills_boss", 1);
        d.mapInc("kills", v.entityId(), 1);
    }

    /** A blow dealt by the player; raises the best single hit. */
    public static void hit(RecordData d, double damage) { d.bestMax("single_hit", damage); }

    public static void death(RecordData d) { d.addCounter("deaths", 1); }

    /** One item forged at quality 1..6 (0 and below are ignored, above 6 is capped). */
    public static void forged(RecordData d, int quality) {
        if (quality < 1) return;
        int q = Math.min(6, quality);
        d.addCounter("forge_count", 1);
        d.mapInc("forge_quality", Integer.toString(q), 1);
        d.bestMax("forge_quality", q);
    }

    public static void calamityDamage(RecordData d, double amount) {
        if (amount > 0) d.addCounter("calamity_damage", amount);
    }

    public static void calamityKill(RecordData d, String calamityId) {
        d.addCounter("calamity_kills", 1);
        if (calamityId != null && !calamityId.isEmpty()) d.mapInc("calamity_kills", calamityId, 1);
    }

    public static void walpurgis(RecordData d) { d.addCounter("walpurgis", 1); }

    public static void warKill(RecordData d) { d.addCounter("war_kills", 1); }

    public static void warWin(RecordData d) { d.addCounter("war_wins", 1); }

    /** A biome the player stands in; true when it is new. */
    public static boolean biome(RecordData d, String id) { return d.setAdd("biomes", id); }

    /** A structure the player stands in; true when it is new. */
    public static boolean structure(RecordData d, String id) { return d.setAdd("structures", id); }
}
