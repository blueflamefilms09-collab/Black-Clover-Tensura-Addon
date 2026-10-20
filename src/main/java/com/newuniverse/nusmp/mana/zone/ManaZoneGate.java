package com.newuniverse.nusmp.mana.zone;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Who may open a Mana Zone: a pure, tested decision (no Minecraft types). The Mana Zone is the top of the ladder after mana skin and
 * reinforcement, so it is a LATE-GAME technique. Six requirements, all of which must hold:
 * <ol>
 *   <li><b>EP</b>: Tensura existence points (default 2,000,000; BalanceLaw.epScale is x9.2 there and tops out at 10M);</li>
 *   <li><b>Rank</b>: Magic Knight step 14 (Senior Magic Knight, 1st Class), 140 net stars at 10 stars per class; Grand Magic Knight and
 *       the Wizard King count too;</li>
 *   <li><b>Mana Skin</b>: the player's own grimoire fully mastered (Stage 0), or Reinforcement Magic (the mana skin school) at 60%;</li>
 *   <li><b>Bosses</b>: 15 boss kills (the quest counter {@code boss.defeated});</li>
 *   <li><b>Depth</b>: the deepest dungeon floor reached on either road, 40 (the hub's top shop tier is 25);</li>
 *   <li><b>The attunement</b>: a long ritual, 1,800 seconds (30 minutes) spent inside a Grand Magic Zone with the mana skin held.</li>
 * </ol>
 * The Wizard King is open by right of the crown (the Crown of Mana is the original forced Mana Zone). The numbers come from
 * {@link ManaZoneRules}; the {@link #FLOOR_EP} and {@link #FLOOR_STEP} floors hold whatever a datapack or a config multiplier says.
 */
public final class ManaZoneGate {
    private ManaZoneGate() {}

    /** No file and no config value can lower the gate under these. */
    public static final double FLOOR_EP = 1_000_000.0;
    public static final int FLOOR_STEP = 12, FLOOR_BOSS_KILLS = 5, FLOOR_FLOOR = 10, FLOOR_ATTUNE_SECONDS = 300;
    /** The server config multiplier is clamped to this range: it may make the gate stricter, or a little easier, but never early. */
    public static final double MIN_STRICTNESS = 0.5, MAX_STRICTNESS = 4.0;

    public enum Req { EP, RANK, MANA_SKIN, BOSSES, DEPTH, ATTUNEMENT }

    /** What the player has now. Mastery values are 0..1 (0 when the grimoire or the Reinforcement book is missing). */
    public record Progress(double ep, int knightStep, double grimoireMastery, double reinforcementMastery, int bossKills, int deepestFloor,
                           int attuneSeconds, boolean wizardKing) {
        public static final Progress NONE = new Progress(0, 0, 0, 0, 0, 0, 0, false);
    }

    /** One line of the checklist: what is needed, what the player has, and whether that is enough. */
    public record Check(Req req, boolean met, String have, String need) {}

    public record Verdict(boolean open, List<Check> checks) {
        public List<Check> unmet() {
            List<Check> out = new ArrayList<>();
            for (Check c : checks) if (!c.met) out.add(c);
            return out;
        }

        /** One short line for the action bar: "EP 12,000 of 2,000,000; ...". */
        public String summary() {
            StringBuilder sb = new StringBuilder();
            for (Check c : unmet()) {
                if (sb.length() > 0) sb.append("; ");
                sb.append(label(c.req)).append(' ').append(c.have).append(" of ").append(c.need);
            }
            return sb.toString();
        }
    }

    public static String label(Req r) {
        return switch (r) {
            case EP -> "EP";
            case RANK -> "rank";
            case MANA_SKIN -> "mana skin";
            case BOSSES -> "bosses";
            case DEPTH -> "depth";
            case ATTUNEMENT -> "attunement";
        };
    }

    // ---------------------------------------------------------------- the effective numbers
    public static double clampStrictness(double s) { return Double.isNaN(s) ? 1.0 : Math.max(MIN_STRICTNESS, Math.min(MAX_STRICTNESS, s)); }

    public static double needEp(ManaZoneRules.Gate g, double strictness) { return Math.max(FLOOR_EP, g.minEp() * clampStrictness(strictness)); }

    public static int needStep(ManaZoneRules.Gate g, double strictness) {
        return Math.max(FLOOR_STEP, Math.min(16, g.minKnightStep()));      // the rank is a ladder, not a quantity: the multiplier does not move it
    }

    public static int needBosses(ManaZoneRules.Gate g, double strictness) { return Math.max(FLOOR_BOSS_KILLS, (int) Math.ceil(g.minBossKills() * clampStrictness(strictness))); }

    public static int needFloor(ManaZoneRules.Gate g, double strictness) { return Math.max(FLOOR_FLOOR, (int) Math.ceil(g.minDeepestFloor() * clampStrictness(strictness))); }

    public static int needAttune(ManaZoneRules.Gate g, double strictness) { return Math.max(FLOOR_ATTUNE_SECONDS, (int) Math.ceil(g.attuneSeconds() * clampStrictness(strictness))); }

    // ---------------------------------------------------------------- the decision
    public static Verdict evaluate(ManaZoneRules.Gate g, double strictness, Progress p) {
        List<Check> out = new ArrayList<>(6);
        double ep = Math.max(0, p.ep());
        out.add(new Check(Req.EP, ep >= needEp(g, strictness), number(ep), number(needEp(g, strictness))));
        out.add(new Check(Req.RANK, p.knightStep() >= needStep(g, strictness), rank(p.knightStep()), rank(needStep(g, strictness))));
        boolean skin = p.grimoireMastery() >= g.grimoireMastery() - 1e-9 || p.reinforcementMastery() >= g.reinforcementMastery() - 1e-9;
        out.add(new Check(Req.MANA_SKIN, skin, percent(Math.max(p.grimoireMastery(), p.reinforcementMastery())),
                percent(g.grimoireMastery()) + " of your grimoire or " + percent(g.reinforcementMastery()) + " of Reinforcement"));
        out.add(new Check(Req.BOSSES, p.bossKills() >= needBosses(g, strictness), String.valueOf(p.bossKills()), String.valueOf(needBosses(g, strictness))));
        out.add(new Check(Req.DEPTH, p.deepestFloor() >= needFloor(g, strictness), String.valueOf(p.deepestFloor()), String.valueOf(needFloor(g, strictness))));
        out.add(new Check(Req.ATTUNEMENT, p.attuneSeconds() >= needAttune(g, strictness), minutes(p.attuneSeconds()), minutes(needAttune(g, strictness))));
        boolean all = true;
        for (Check c : out) all &= c.met;
        return new Verdict(all || p.wizardKing(), List.copyOf(out));
    }

    /** True when the player may open a Mana Zone. */
    public static boolean passes(ManaZoneRules.Gate g, double strictness, Progress p) { return evaluate(g, strictness, p).open(); }

    // ---------------------------------------------------------------- attunement (the ritual)
    /**
     * Seconds of attunement a player earns for one second spent here. A named or dungeon-made Grand Magic Zone region counts in full for a
     * player who holds the mana skin (half their mana, as the Ultime-Volcano trail trains it); a naturally dense biome (density 1.8, the
     * deep sea, the Nether, the End) counts a quarter; anywhere else nothing. Dense mana alone is not enough: the player must also be strong
     * enough to stand in it (harm below one half), or the pressure teaches nothing.
     */
    public static double attuneGain(boolean inRegion, double density, boolean holdsManaSkin, double harm) {
        if (!holdsManaSkin || harm >= 0.5) return 0.0;
        if (inRegion) return 1.0;
        return density >= 1.8 ? 0.25 : 0.0;
    }

    // ---------------------------------------------------------------- text
    static String number(double v) { return String.format(Locale.ROOT, "%,d", Math.round(v)); }

    static String percent(double v) { return Math.round(v * 100) + "%"; }

    static String minutes(int seconds) { return (seconds / 60) + " min " + (seconds % 60) + " s"; }

    /** Plain name of a Magic Knight step (0 = Junior 5th Class ... 14 = Senior 1st Class, 15 Grand Magic Knight, 16 Wizard King). */
    public static String rank(int step) {
        if (step >= 16) return "Wizard King";
        if (step >= 15) return "Grand Magic Knight";
        String[] ranks = {"Junior", "Intermediate", "Senior"};
        int s = Math.max(0, step), cls = 5 - (s % 5);
        return ranks[Math.min(2, s / 5)] + " " + cls + (cls == 1 ? "st" : cls == 2 ? "nd" : cls == 3 ? "rd" : "th") + " Class";
    }
}
