package com.newuniverse.nusmp.heroweapon.weapon.pierce;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the owner's balance budget (design.md section 5) is a handful of numbers (raw damage 3..12 for an ordinary technique, 10..20 for a heavy one, at most
 * 32 for an ultimate; cooldown at least 60 ticks for damage, 100 for utility, 600 for an ultimate; cost 1 to 3% of the magicules, 5 to 8% for an ultimate; at
 * most 12 targets), and the easiest way to break it is to edit one number later and not notice. Every technique of the Legendary Spear and Bow is therefore
 * ONE row of a table (id, texts, kind, budget class, cooldown, cost, the most raw damage one target can take from one cast, the most targets), the skill
 * objects are built from the rows, and {@link #problems} checks a row against the budget so a unit test and the in-game self-test both fail on a row that
 * breaks it. No game type here, so it is tested with plain JUnit.
 *
 * @param raw     the most raw damage ONE target can take from one cast, all hits of the cast added up (0 for a technique that does no damage)
 * @param targets the most targets one cast can hit (0 for none)
 */
public record SkillRow(String id, String name, String desc, Kind kind, Budget budget, int cooldown, double cost, double raw, int targets) {

    /** What the technique is for: it decides the shortest cooldown the budget allows. */
    public enum Kind {
        DAMAGE(60), UTILITY(100), ULTIMATE(600);

        public final int minCooldown;

        Kind(int minCooldown) { this.minCooldown = minCooldown; }
    }

    /** How hard one cast may hit one target (raw, before the level factor of 1.0 to 2.2). */
    public enum Budget {
        ORDINARY(12.0), HEAVY(20.0), ULTIMATE(32.0);

        public final double maxRaw;

        Budget(double maxRaw) { this.maxRaw = maxRaw; }
    }

    /** The most targets any technique may hit. */
    public static final int MAX_TARGETS = 12;

    /**
     * The ticks after a blow in which the game ignores a second blow on the same creature that is no stronger (the vanilla hurt immunity). Two hits of one
     * cast on one foe that come sooner than this are not two hits: the second does nothing. Waves, pulses and a second arrow are therefore spaced by at least this.
     */
    public static final int HURT_IMMUNITY = 10;

    /** The things wrong with this row against the budget; empty when it is fine. */
    public List<String> problems() {
        List<String> bad = new ArrayList<>();
        if (id == null || !id.matches("[a-z]+\\.[a-z_]+")) bad.add("bad id '" + id + "'");
        if (name == null || name.isBlank()) bad.add(id + ": no name");
        if (desc == null || desc.isBlank() || desc.length() > 420) bad.add(id + ": the description is empty or longer than 420 characters");
        if (cooldown < kind.minCooldown) bad.add(id + ": cooldown " + cooldown + " is below the " + kind.minCooldown + " ticks a " + kind + " technique needs");
        if (kind == Kind.ULTIMATE) {
            if (cost < 0.05 - 1e-9 || cost > 0.08 + 1e-9) bad.add(id + ": an ultimate costs 5 to 8%, this one " + cost * 100 + "%");
            if (budget != Budget.ULTIMATE) bad.add(id + ": an ultimate has the ULTIMATE damage budget");
        } else {
            if (cost < 0.01 - 1e-9 || cost > 0.03 + 1e-9) bad.add(id + ": an ordinary technique costs 1 to 3%, this one " + cost * 100 + "%");
            if (budget == Budget.ULTIMATE) bad.add(id + ": only an ultimate has the ULTIMATE damage budget");
        }
        if (raw < 0.0 || raw > budget.maxRaw + 1e-9) bad.add(id + ": raw damage " + raw + " is above the " + budget + " budget of " + budget.maxRaw);
        if (targets < 0 || targets > MAX_TARGETS) bad.add(id + ": " + targets + " targets (at most " + MAX_TARGETS + ")");
        if (raw > 0.0 && targets < 1) bad.add(id + ": it does damage but hits no target");
        return bad;
    }
}
