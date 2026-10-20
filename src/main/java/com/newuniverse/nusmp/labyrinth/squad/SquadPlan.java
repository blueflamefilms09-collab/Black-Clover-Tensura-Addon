package com.newuniverse.nusmp.labyrinth.squad;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SplittableRandom;

/**
 * Why: "5-to-8 man squads with a tank, damage, a flanker and a healer" is a DECISION before it is any monsters, and the decision is plain arithmetic on a few numbers
 * (how deep the floor is, how big the party, how hard the encounter rolled, what kind of monsters live there), so it is made here with no Minecraft type in sight and
 * tested over every depth from 1 to 1000. The size grows with the encounter band, the depth and the party (never below 5 or above 8: a squad needs room for its roles
 * and a room cannot hold more); the composition always has one or two tanks, one or two supports and at least one ranged damage dealer and one flanker once the
 * squad has room for them; an optional leader appears in harder or deeper fights. The faction (or, for a monster with none, the theme of the floor) tilts the mix:
 * ogres bring tanks, goblins bring flankers and archers, cultists bring healers, spirits bring casters. Everything comes from a seed, so the same encounter with the
 * same seed is the same squad (a self-test, a command and a replay all rely on it), and {@link #roles} comes out in formation order, front line first.
 */
public final class SquadPlan {
    private SquadPlan() {}

    public static final int MIN_SIZE = 5, MAX_SIZE = 8;
    /** The encounter bands of the Dungeon Master as numbers: a nuisance, a fair fight, a hard fight, an ambush. */
    public static final int MINOR = 0, STANDARD = 1, HARD = 2, AMBUSH = 3;

    /** How a kind of monster tilts the mix of damage roles and how many tanks and healers it brings (positive = more, negative = fewer). */
    public record Flavour(int tank, int ranged, int flank, int support, int melee) {
        public static final Flavour NEUTRAL = new Flavour(0, 0, 0, 0, 0);
    }

    private static final Map<String, Flavour> FACTIONS = Map.ofEntries(
            Map.entry("undead", new Flavour(0, 1, 0, 1, 0)),
            Map.entry("goblin", new Flavour(-1, 1, 1, 0, 0)),
            Map.entry("ogre", new Flavour(2, -1, -1, -1, 1)),
            Map.entry("beast", new Flavour(0, -1, 2, -1, 1)),
            Map.entry("slime", new Flavour(2, 0, -1, 0, 0)),
            Map.entry("construct", new Flavour(2, 1, -1, -1, 0)),
            Map.entry("cultist", new Flavour(0, 1, 0, 2, 0)),
            Map.entry("demon", new Flavour(0, 0, 1, 0, 1)),
            Map.entry("elemental", new Flavour(0, 2, 0, 1, -1)),
            Map.entry("aquatic", new Flavour(1, 0, 0, 0, 1)),
            Map.entry("insect", new Flavour(-1, 0, 2, -1, 1)));

    /** The faction a floor theme stands for when the monsters have none of their own. */
    private static final Map<String, String> THEMES = Map.ofEntries(
            Map.entry("necropolis", "undead"), Map.entry("haunted_manor", "undead"), Map.entry("cursed_archive", "cultist"), Map.entry("holy_church", "cultist"),
            Map.entry("celestial_court", "cultist"), Map.entry("infernal_foundry", "demon"), Map.entry("abyss", "demon"), Map.entry("blood", "demon"),
            Map.entry("flame", "elemental"), Map.entry("frost", "elemental"), Map.entry("gravity", "elemental"), Map.entry("astral", "elemental"),
            Map.entry("crystal_geode", "elemental"), Map.entry("drowned_crypt", "aquatic"), Map.entry("dwarven_forgehold", "construct"),
            Map.entry("fungal_underdark", "insect"), Map.entry("feywild_grove", "beast"), Map.entry("mountain_monastery", "ogre"));

    /** The flavour of a faction id, else of the faction the theme stands for, else neutral. */
    public static Flavour flavour(String faction, String theme) {
        Flavour f = faction == null ? null : FACTIONS.get(faction.trim().toLowerCase(Locale.ROOT));
        if (f != null) return f;
        String stand = theme == null ? null : THEMES.get(theme.trim().toLowerCase(Locale.ROOT));
        f = stand == null ? null : FACTIONS.get(stand);
        return f == null ? Flavour.NEUTRAL : f;
    }

    /** One squad: its seed, its headcount and its roles in formation order (leader, tanks, melee, flankers, ranged, supports). */
    public record Plan(long seed, int size, int band, int depth, int party, List<SquadRole> roles) {
        public Plan {
            roles = List.copyOf(roles);
        }

        public int count(SquadRole r) {
            int n = 0;
            for (SquadRole x : roles) if (x == r) n++;
            return n;
        }

        public boolean hasLeader() { return count(SquadRole.LEADER) > 0; }

        /** "6: 2 tank, 1 melee, 1 ranged, 1 support, 1 leader" for the System line and the log. */
        public String describe() {
            StringBuilder b = new StringBuilder().append(size).append(':');
            boolean first = true;
            for (SquadRole r : SquadRole.values()) {
                int n = count(r);
                if (n == 0) continue;
                b.append(first ? " " : ", ").append(n).append(' ').append(r.id());
                first = false;
            }
            return b.toString();
        }
    }

    // ---------------------------------------------------------------- the size
    /**
     * The headcount: the band's base (a nuisance and a fair fight five, a hard fight six, an ambush seven), plus one from floor 10 on and one more from floor 100 on,
     * plus one for every two players beyond the first, give or take one by the seed, held to {@link #MIN_SIZE} to {@link #MAX_SIZE}. The depth is the distance from the
     * Hub (a negative floor is as deep as its positive twin); 0 (unknown) counts as the first floor.
     */
    public static int sizeFor(long seed, int depth, int party, int band) {
        int d = Math.max(1, Math.abs(depth));
        int b = Math.max(MINOR, Math.min(AMBUSH, band));
        int size = 5 + (b >= HARD ? 1 : 0) + (b >= AMBUSH ? 1 : 0);
        if (d >= 10) size++;
        if (d >= 100) size++;
        size += Math.max(0, party - 1) / 2;
        SplittableRandom r = new SplittableRandom(seed);
        int roll = r.nextInt(8);
        if (roll == 0) size--;
        else if (roll >= 6) size++;
        return Math.max(MIN_SIZE, Math.min(MAX_SIZE, size));
    }

    // ---------------------------------------------------------------- the composition
    /** The squad for these inputs: always {@code size} roles, {@link #sizeFor} of them, in formation order. */
    public static Plan plan(long seed, int depth, int party, int band, String faction, String theme) {
        int n = sizeFor(seed, depth, party, band);
        Flavour fl = flavour(faction, theme);
        SplittableRandom r = new SplittableRandom(seed ^ 0x9E3779B97F4A7C15L);
        int d = Math.max(1, Math.abs(depth));
        int b = Math.max(MINOR, Math.min(AMBUSH, band));
        boolean leader = n >= 6 && (b >= HARD || d >= 30 || r.nextInt(4) == 0);
        int tanks = 1;
        if (n >= 7 || (n == 6 && fl.tank() >= 2)) tanks = 2;
        if (fl.tank() <= -1 && n <= 7) tanks = 1;
        int supports = 1;
        if ((n == 8 && fl.support() >= 1) || (n >= 7 && fl.support() >= 2)) supports = 2;
        int rest = n - tanks - supports - (leader ? 1 : 0);
        int[] dps = damage(rest, fl, r);                      // melee, ranged, flanker
        List<SquadRole> roles = new ArrayList<>(n);
        if (leader) roles.add(SquadRole.LEADER);
        for (int i = 0; i < tanks; i++) roles.add(SquadRole.TANK);
        for (int i = 0; i < dps[0]; i++) roles.add(SquadRole.DPS_MELEE);
        for (int i = 0; i < dps[2]; i++) roles.add(SquadRole.FLANKER);
        for (int i = 0; i < dps[1]; i++) roles.add(SquadRole.DPS_RANGED);
        for (int i = 0; i < supports; i++) roles.add(SquadRole.SUPPORT);
        return new Plan(seed, n, b, d, party, roles);
    }

    /**
     * Shares {@code rest} damage places among melee, ranged and flanker by the flavour's weights (largest remainder, ties broken by the seed), then guarantees what the
     * brief wants: with three places or more one of each; with two a ranged and one melee-kind; with one the heavier of melee and ranged.
     */
    static int[] damage(int rest, Flavour fl, SplittableRandom r) {
        double[] w = {Math.max(0, 2 + fl.melee()), Math.max(0, 1 + fl.ranged()), Math.max(0, 1 + fl.flank())};
        double sum = w[0] + w[1] + w[2];
        if (sum <= 0.0) {
            w = new double[]{1, 1, 1};
            sum = 3.0;
        }
        int[] out = new int[3];
        double[] frac = new double[3];
        int used = 0;
        for (int i = 0; i < 3; i++) {
            double q = rest * w[i] / sum;
            out[i] = (int) Math.floor(q);
            frac[i] = q - out[i] + r.nextDouble() * 1e-6;       // the seed breaks exact ties
            used += out[i];
        }
        while (used < rest) {
            int best = 0;
            for (int i = 1; i < 3; i++) if (frac[i] > frac[best]) best = i;
            out[best]++;
            frac[best] = -1.0;
            used++;
        }
        if (rest >= 3) {
            for (int i = 0; i < 3; i++) {
                if (out[i] > 0) continue;
                int from = 0;
                for (int j = 1; j < 3; j++) if (out[j] > out[from]) from = j;
                out[from]--;
                out[i]++;
            }
        } else if (rest == 2) {
            if (out[1] == 0) {
                int from = out[0] >= out[2] ? 0 : 2;
                out[from]--;
                out[1]++;
            }
            if (out[0] == 0 && out[2] == 0) {
                out[1]--;
                out[0]++;
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- a squad that came out smaller
    /**
     * The roles of a plan cut down to {@code n} members, for the day fewer places were found than monsters were planned: the least essential go first (a second melee, the
     * leader, a second flanker, a second shooter, a second healer, a second tank), and the one tank, one healer, one shooter and one flanker are the last to go. The order of the
     * rest is kept.
     */
    public static List<SquadRole> trim(List<SquadRole> roles, int n) {
        List<SquadRole> out = new ArrayList<>(roles);
        SquadRole[] extras = {SquadRole.DPS_MELEE, SquadRole.LEADER, SquadRole.FLANKER, SquadRole.DPS_RANGED, SquadRole.SUPPORT, SquadRole.TANK};
        for (SquadRole r : extras) {
            int keep = r == SquadRole.LEADER ? 0 : 1;
            while (out.size() > n && count(out, r) > keep) out.remove(lastIndex(out, r));
        }
        SquadRole[] essentials = {SquadRole.DPS_MELEE, SquadRole.FLANKER, SquadRole.DPS_RANGED, SquadRole.SUPPORT, SquadRole.TANK};
        for (SquadRole r : essentials) {
            while (out.size() > n && count(out, r) > 0) out.remove(lastIndex(out, r));
        }
        return out;
    }

    private static int count(List<SquadRole> roles, SquadRole r) {
        int n = 0;
        for (SquadRole x : roles) if (x == r) n++;
        return n;
    }

    private static int lastIndex(List<SquadRole> roles, SquadRole r) {
        for (int i = roles.size() - 1; i >= 0; i--) if (roles.get(i) == r) return i;
        return -1;
    }

    // ---------------------------------------------------------------- fitting an encounter to a squad
    /**
     * The per-kind counts of an encounter changed to a total of {@code target}: too many and the LAST kind loses one at a time (the first kind never goes below one,
     * it is what the encounter is about); too few and the kinds that have members take one more in rotation, the first kind first. A kind with no members stays empty.
     */
    public static int[] fit(int[] counts, int target) {
        int[] c = Arrays.copyOf(counts, counts.length);
        int total = 0;
        int first = -1;
        for (int i = 0; i < c.length; i++) {
            total += c[i];
            if (first < 0 && c[i] > 0) first = i;
        }
        if (first < 0) return c;
        while (total > target) {
            int k = -1;
            for (int i = c.length - 1; i >= 0; i--) {
                int floor = i == first ? 1 : 0;
                if (c[i] > floor) {
                    k = i;
                    break;
                }
            }
            if (k < 0) break;
            c[k]--;
            total--;
        }
        int i = 0;
        int guard = 0;
        while (total < target && guard++ < 1000) {
            int k = i++ % c.length;
            if (c[k] > 0) {
                c[k]++;
                total++;
            }
        }
        return c;
    }
}
