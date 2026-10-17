package com.newuniverse.nusmp.arc.zagred;

/**
 * Why: the framework picks a move by a weighted draw over the moves that are allowed (power, range, cooldown, mana), which is right for a brawler but wrong
 * for a reader of the party: Zagred is supposed to ANSWER what the players do (a volley of arrows is met with Shatter, a burst of damage with Reverse, a
 * knot of players with Banish, the strongest caster with Seal, a flier with Fall). This pure class turns a few facts about the situation into a weight for
 * each move, and a draw over weights; the boss class gathers the facts from the world and asks. Keeping it pure means every "he answers X with Y" claim in
 * the docs is a unit test, and the owner can retune one number without reading the entity.
 */
public final class ZagredAi {
    private ZagredAi() {}

    /**
     * The situation.
     *
     * @param distance     blocks to his current target
     * @param foes         foes (players and their summons) within 32 blocks
     * @param close        foes within seven blocks of him
     * @param fliers       foes off the ground and not in water
     * @param shots        enemy projectiles about to reach him
     * @param casters      players of very high EP within range
     * @param burst        the share of his maximum health he lost in the last few seconds (0 to 1)
     * @param controlReady the control law lets him bind or silence now
     * @param stage        the framework's power step (0 to 4)
     * @param gates        the Gates are open (the devil form)
     * @param lastId       the id of the move he used last
     * @param pages        pages (lesser daemons) alive
     * @param pageCap      pages he may have at most
     * @param silenced     he cannot speak a Word
     * @param verdictOpen  a verdict is waiting for answers
     * @param ruleOpen     an Overwrite rule or its stones are in force
     */
    public record Read(double distance, int foes, int close, int fliers, int shots, int casters, double burst, boolean controlReady, int stage,
                       boolean gates, String lastId, int pages, int pageCap, boolean silenced, boolean verdictOpen, boolean ruleOpen) {}

    /** The weight of one move in this situation (0 = do not pick it). The row's own weight is multiplied by what the situation calls for. */
    public static double weight(ZagredTable.Spec s, Read r) {
        double w = s.weight();
        if (s.isWord() && (r.silenced() || r.verdictOpen())) return 0;           // a cut Word silences him; a verdict is answered before the next Word
        if (s.hardControl() && !r.controlReady()) return 0;                       // one binding or silencing Word per twelve seconds, whatever else is ready
        switch (s.id()) {
            case ZagredTable.CLAW -> {
                if (r.distance() < 4.5) w *= 2.2;
                else if (r.distance() > 7 && !r.gates()) w *= 0.4;                // the human form does not run at you; the devil form leaps
                if (r.gates() && r.distance() > 6) w *= 1.6;
            }
            case ZagredTable.HALT -> {
                if (r.foes() >= 2) w *= 1.4;
                if (r.close() >= 2) w *= 1.6;                                      // the band spares whoever stands next to him: it punishes a ring of ranged fighters
                if (r.distance() > 18) w *= 0.6;
            }
            case ZagredTable.FALL -> {
                if (r.fliers() > 0) w *= 3.0 + 0.5 * r.fliers();
                if (r.close() >= 2) w *= 1.2;
            }
            case ZagredTable.TRIDENT -> {
                if (r.distance() >= 8) w *= 1.4;
                if (r.foes() == 1) w *= 1.1;
            }
            case ZagredTable.REDACT -> {
                if (r.close() >= 2) w *= 1.4;
                if (r.fliers() > 0) w *= 0.7;
            }
            case ZagredTable.ARTS -> { if (r.distance() >= 8) w *= 1.3; }
            case ZagredTable.SWORDS -> { if (r.foes() >= 2) w *= 1.3; }
            case ZagredTable.SEAL -> w *= r.casters() > 0 ? 3.5 : 0.4;
            case ZagredTable.SHATTER -> w *= r.shots() >= 3 ? 10 : r.shots() == 2 ? 7 : r.shots() == 1 ? 4 : 0.15;
            case ZagredTable.BANISH -> w *= r.close() >= ZagredMath.crowdSize() ? 10 : r.close() == ZagredMath.crowdSize() - 1 ? 3 : 0.05;
            case ZagredTable.PAGES -> {
                if (r.pages() >= r.pageCap()) return 0;
                if (r.pages() == 0) w *= 2.0;
            }
            case ZagredTable.REVERSE -> w *= r.burst() >= 0.12 ? 6 : 0.25;
            case ZagredTable.VERDICT -> { if (r.foes() >= 2) w *= 1.4; }
            case ZagredTable.OVERWRITE -> { if (r.ruleOpen()) return 0; }
            default -> { }
        }
        if (r.ruleOpen() && (s.id().equals(ZagredTable.VERDICT))) return 0;       // the party is busy with the stones
        if (s.isWord() && s.id().equals(r.lastId())) w *= 0.3;                     // never the same Word twice in a row if anything else is on offer
        return Math.max(0, w);
    }

    /** The index chosen by {@code roll} (0 to 1) over {@code weights}, or -1 when every weight is zero. */
    public static int choose(double[] weights, double roll) {
        double total = 0;
        for (double w : weights) total += Math.max(0, w);
        if (total <= 0) return -1;
        double x = Math.max(0, Math.min(0.999999999, roll)) * total;
        int chosen = -1;
        for (int i = 0; i < weights.length; i++) {
            if (weights[i] <= 0) continue;
            chosen = i;
            x -= weights[i];
            if (x < 0) break;
        }
        return chosen;
    }
}
