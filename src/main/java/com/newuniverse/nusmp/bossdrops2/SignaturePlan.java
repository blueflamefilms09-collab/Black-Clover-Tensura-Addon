package com.newuniverse.nusmp.bossdrops2;

import com.newuniverse.nusmp.bossdrops2.OneOfOne.Outcome;
import com.newuniverse.nusmp.bossdrops2.SignatureTable.Row;
import com.newuniverse.nusmp.bossdrops2.SignatureTable.Table;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.DoubleSupplier;

/**
 * Why: who is paid what for one boss kill, decided as plain data (pure, unit tested; the game layer only turns an {@link Award} into an item and hands it over).
 * <ol>
 *   <li>{@link #select}: the fighters. Everybody who dealt at least {@link #MIN_SHARE} of the boss's health (and at least {@link #MIN_ABS} points), plus the killer and the fight's known participants;
 *       ordered by damage (the killer wins a tie), at most {@link #MAX_FIGHTERS} of them, the killer always kept.</li>
 *   <li>{@link #plan}: the top fighter gets the one-of-one (or its echo when it exists already); EVERY fighter rolls their own signature piece and their own extra rows (personal rolls, so a group is not
 *       a single roll shared out); the other fighters get an echo with the table's echo chance. At most {@link #MAX_PIECES} pieces for the whole kill: the one-of-one, the echo of the top fighter and the
 *       signatures are reserved first, the extras are dealt round by round (everybody's first row, then everybody's second ...) in damage order until the cap.</li>
 * </ol>
 * Worst case of one kill: {@link #MAX_FIGHTERS} fighters, {@link #MAX_PIECES} pieces.
 */
public final class SignaturePlan {
    private SignaturePlan() {}

    public static final int MAX_FIGHTERS = 5, MAX_PIECES = 12;
    public static final double MIN_SHARE = 0.02, MIN_ABS = 1.0;

    public enum Kind { UNIQUE, ECHO, SIGNATURE, EXTRA }

    /** One stack to hand to one player; {@code item} is empty for UNIQUE / ECHO / SIGNATURE (the game layer knows those items). */
    public record Award(UUID player, Kind kind, String item, int count) {}

    /**
     * Picks and orders the fighters. {@code damage} is what the ledger saw (already limited to players who may be paid), {@code present} are fighters known by presence only (they count with 0 damage),
     * {@code killer} may be null; it is kept even without damage because a last blow is a fight. {@code maxHealth} is the boss's maximum health (0 or NaN: only {@link #MIN_ABS} applies).
     */
    public static List<Fighter> select(Map<UUID, Double> damage, Iterable<UUID> present, UUID killer, double maxHealth) {
        double floor = Math.max(MIN_ABS, maxHealth > 0.0 && !Double.isNaN(maxHealth) ? MIN_SHARE * maxHealth : 0.0);
        List<Fighter> out = new ArrayList<>();
        for (Map.Entry<UUID, Double> e : damage.entrySet()) {
            double d = e.getValue() == null ? 0.0 : e.getValue();
            boolean isKiller = e.getKey().equals(killer);
            if (d >= floor || isKiller) out.add(new Fighter(e.getKey(), d, isKiller));
        }
        if (killer != null && out.stream().noneMatch(f -> f.id().equals(killer))) out.add(new Fighter(killer, 0.0, true));
        if (present != null) {
            for (UUID p : present) {
                if (p != null && out.stream().noneMatch(f -> f.id().equals(p))) out.add(new Fighter(p, 0.0, false));
            }
        }
        out.sort(Comparator.comparingDouble(Fighter::damage).reversed().thenComparing(Fighter::killer, Comparator.reverseOrder()).thenComparing(Fighter::id));
        if (out.size() > MAX_FIGHTERS) {
            List<Fighter> cut = new ArrayList<>(out.subList(0, MAX_FIGHTERS));
            Fighter k = out.stream().filter(Fighter::killer).findFirst().orElse(null);
            if (k != null && !cut.contains(k)) cut.set(MAX_FIGHTERS - 1, k);
            out = cut;
        }
        return out;
    }

    /** The pieces of one kill, in the order they are handed over. {@code fighters} is the result of {@link #select} (top fighter first). */
    public static List<Award> plan(Table t, List<Fighter> fighters, Outcome outcome, DoubleSupplier rng) {
        List<Award> out = new ArrayList<>();
        if (t == null || fighters == null || fighters.isEmpty()) return out;
        UUID top = fighters.get(0).id();
        if (outcome == Outcome.ISSUED) out.add(new Award(top, Kind.UNIQUE, "", 1));
        else if (outcome == Outcome.EXISTS) out.add(new Award(top, Kind.ECHO, "", 1));
        for (Fighter f : fighters) {
            if (roll(t.signature().chance(), rng)) out.add(new Award(f.id(), Kind.SIGNATURE, "", count(t.signature().min(), t.signature().max(), rng)));
        }
        if (outcome != Outcome.NONE && t.hasUnique()) {
            for (int i = 1; i < fighters.size(); i++) {
                if (roll(t.unique().echoChance(), rng)) out.add(new Award(fighters.get(i).id(), Kind.ECHO, "", 1));
            }
        }
        if (out.size() > MAX_PIECES) return new ArrayList<>(out.subList(0, MAX_PIECES));
        List<List<Award>> perFighter = new ArrayList<>();
        for (Fighter f : fighters) {
            List<Award> mine = new ArrayList<>();
            for (Row r : t.extras()) if (roll(r.chance(), rng)) mine.add(new Award(f.id(), Kind.EXTRA, r.item(), count(r.min(), r.max(), rng)));
            perFighter.add(mine);
        }
        for (int round = 0; out.size() < MAX_PIECES; round++) {
            boolean any = false;
            for (List<Award> mine : perFighter) {
                if (round < mine.size() && out.size() < MAX_PIECES) {
                    out.add(mine.get(round));
                    any = true;
                }
            }
            if (!any) break;
        }
        return out;
    }

    private static boolean roll(double chance, DoubleSupplier rng) { return chance >= 1.0 || rng.getAsDouble() < chance; }

    private static int count(int min, int max, DoubleSupplier rng) {
        if (max <= min) return min;
        int n = min + (int) (rng.getAsDouble() * (max - min + 1));
        return Math.min(max, Math.max(min, n));
    }
}
