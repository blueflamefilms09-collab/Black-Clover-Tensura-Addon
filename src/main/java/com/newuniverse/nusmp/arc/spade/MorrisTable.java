package com.newuniverse.nusmp.arc.spade;

import com.newuniverse.nusmp.entity.zogratis.MoveSpec;

import java.util.List;

/**
 * Why: every number of Morris Libardirt's moves in one place, as {@link MoveSpec}s the scheduler can read without Minecraft, so the shape of the fight (what
 * is available at which power and distance, how much telegraph each move gives, how the ultimates are paced) is pinned by tests and tuned in one file.
 * {@code MorrisBoss} turns each row into a framework {@code Move} by adding its telegraph and its code, looked up by id. The order is the order of the
 * boss's move list and must never change (the client reads move names by index); a new move is appended.
 * <p>
 * The shape of the fight: a scholar who keeps his distance and takes the party apart. Needles and the Structure Shock are his plain answers at range and
 * point blank; Clawed Tendrils and the Specimen Mark are the dissection (a grab, and a thread that must be cut); Spell Rewrite and Re-Structure are the
 * Modification Magic turned on magic and on his own body; Test Subjects are the followers he made of devil-touched men; from 60% the gravity he borrows
 * from Lucifero's contract comes in (the Gravity Pin, Borrowed Weight), the Dissection Theatre is his ultimate and, once the Gates are open and Lucifero
 * is surfacing through him, The Second Gate.
 */
public final class MorrisTable {
    private MorrisTable() {}

    public static final String NEEDLES = "needles", STRUCTURE_SHOCK = "structure_shock", TENDRILS = "tendrils", SPECIMEN = "specimen",
            SPELL_REWRITE = "spell_rewrite", TEST_SUBJECTS = "test_subjects", RESTRUCTURE = "restructure", PIN_DOWN = "pin_down",
            BORROWED_WEIGHT = "borrowed_weight", DISSECTION_THEATRE = "dissection_theatre", SECOND_GATE = "second_gate";

    /** Base damage of the moves (before the boss's damage law: power tuning, victim EP, the balance law's caps). */
    public static final float NEEDLE = 5.5f, SHOCK = 13f, TENDRIL = 9f, SPECIMEN_DRAIN = 2.5f, DISSECT = 28f, REWRITE_NEEDLE = 5f, PIN_TICK = 5f,
            THEATRE_WALL = 12f, THEATRE_SQUEEZE = 30f, GATE_WAVE = 14f;

    /** One row of the table. The record's accessors are the {@link MoveSpec} methods. */
    public record Spec(String id, String label, int minPower, int windUpTicks, int cooldownTicks, double minRange, double maxRange,
                       int weight, boolean ultimate, boolean crowd, int recovery) implements MoveSpec {
        /** The mana an ordinary move costs below 60% power (the same rule as the framework's Move): 6 + a sixteenth of its cooldown, at most 32; 60 for an ultimate. */
        @Override public double manaCost() { return ultimate ? 60 : Math.min(32, 6 + cooldownTicks / 16.0); }
    }

    private static final List<Spec> SPECS = List.of(
            new Spec(NEEDLES, "Dissection Needles", 20, 18, 90, 5, 30, 6, false, false, 8),
            new Spec(STRUCTURE_SHOCK, "Structure Shock", 20, 14, 110, 0, 5.5, 6, false, false, 10),
            new Spec(TENDRILS, "Clawed Tendrils", 20, 24, 200, 3, 20, 5, false, true, 12),
            new Spec(SPECIMEN, "Specimen Mark", 20, 26, 360, 4, 30, 3, false, false, 10),
            new Spec(SPELL_REWRITE, "Spell Rewrite", 20, 18, 380, 0, 48, 2, false, false, 8),
            new Spec(TEST_SUBJECTS, "Test Subjects", 20, 30, 700, 0, 48, 2, false, false, 10),
            new Spec(RESTRUCTURE, "Re-Structure", 40, 24, 520, 0, 48, 2, false, false, 12),
            new Spec(PIN_DOWN, "Gravity Pin", 60, 26, 240, 3, 18, 4, false, true, 12),
            new Spec(BORROWED_WEIGHT, "Borrowed Weight", 80, 28, 300, 4, 32, 3, false, true, 12),
            new Spec(DISSECTION_THEATRE, "Dissection Theatre", 60, 60, 1500, 0, 40, 5, true, false, 60),
            new Spec(SECOND_GATE, "The Second Gate", 100, 66, 1500, 0, 40, 5, true, false, 60));

    /** The table, in the order of the boss's move list (the client reads move names by index). */
    public static List<Spec> specs() { return SPECS; }

    public static Spec byId(String id) {
        for (Spec s : SPECS) if (s.id().equals(id)) return s;
        return null;
    }
}
