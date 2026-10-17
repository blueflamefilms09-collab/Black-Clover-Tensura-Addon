package com.newuniverse.nusmp.arc.sds;

import com.newuniverse.nusmp.entity.zogratis.MoveSpec;

import java.util.List;

/**
 * Why: Ban's move numbers in one pure place, as {@link MoveSpec}s the framework's scheduler can read without Minecraft, so a test can play a whole
 * fight through the real scheduler and prove what he does at each power and distance. {@code BanBoss} turns each row into a {@code Move} by adding its
 * telegraph and its code, found by id.
 *
 * <p>The shape of the fight: the fastest of the three, a man who fights close with a staff of four sections (the combo, the sweep, the extended
 * strike), steals at range (Snatch, then the Barrage that snatches from several), closes the gap like a fox (Fox Step), takes a heart (Hunter Fest),
 * focuses (Super Concentration) and, from the middle of the fight, calls the Fountain up out of the ground. His Fox's Hunt is the ultimate. His
 * immortality is not a move but a rule ({@link FountainOfYouth}); the Snatch has its own ledger ({@link SnatchLedger}).
 */
public final class BanTable {
    private BanTable() {}

    public static final String COMBO = "courechouse_combo", SWEEP = "courechouse_sweep", LASH = "courechouse_extend", SNATCH = "snatch", FOX_STEP = "fox_step",
            HUNTER_FEST = "hunter_fest", CONCENTRATION = "super_concentration", BARRAGE = "snatch_barrage", GEYSER = "fountain_geyser", HUNT = "foxs_hunt";

    /** Base damage of the moves (before the boss's damage law: power tuning, his own scale and what he has borrowed, the victim's EP, the balance law's caps). */
    public static final float COMBO_DAMAGE = 7f, SWEEP_DAMAGE = 10f, LASH_DAMAGE = 11f, SNATCH_DAMAGE = 5f, STEP_DAMAGE = 12f,
            HUNTER_DAMAGE = 18f, GEYSER_DAMAGE = 13f, HUNT_DAMAGE = 14f;

    /** One row of the table. The record's accessors are the {@link MoveSpec} methods. */
    public record Spec(String id, String label, int minPower, int windUpTicks, int cooldownTicks, double minRange, double maxRange,
                       int weight, boolean ultimate, boolean crowd, int recovery) implements MoveSpec {
        /** The mana an ordinary move costs below 60% power (the same rule as the framework's {@code Move}): 6 + a sixteenth of its cooldown, at most 32; 60 for an ultimate. */
        @Override public double manaCost() { return ultimate ? 60 : Math.min(32, 6 + cooldownTicks / 16.0); }
    }

    private static final List<Spec> SPECS = List.of(
            new Spec(COMBO, "Courechouse Combo", 20, 16, 60, 0, 4.8, 6, false, false, 8),
            new Spec(SWEEP, "Courechouse Sweep", 20, 16, 130, 0, 5.6, 3, false, true, 10),
            new Spec(LASH, "Courechouse: Extend", 20, 16, 100, 4, 12.5, 5, false, false, 8),
            new Spec(SNATCH, "Snatch", 20, 16, 190, 4, 18, 5, false, false, 8),
            new Spec(FOX_STEP, "Fox Step", 20, 16, 150, 6, 26, 4, false, false, 8),
            new Spec(HUNTER_FEST, "Hunter Fest", 40, 20, 280, 3, 15, 3, false, false, 12),
            new Spec(CONCENTRATION, "Super Concentration", 40, 20, 520, 0, 48, 1, false, false, 12),
            new Spec(BARRAGE, "Snatch Barrage", 60, 22, 330, 4, 22, 3, false, true, 12),
            new Spec(GEYSER, "Fountain Geyser", 80, 22, 260, 4, 32, 3, false, true, 12),
            new Spec(HUNT, "Fox's Hunt", 60, 58, 1300, 0, 30, 5, true, true, 60));

    /** The table, in the order of the boss's move list (the client reads move names by index; only ever append). */
    public static List<Spec> specs() { return SPECS; }

    public static Spec byId(String id) {
        for (Spec s : SPECS) if (s.id().equals(id)) return s;
        return null;
    }
}
