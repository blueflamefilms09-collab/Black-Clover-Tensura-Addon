package com.newuniverse.nusmp.arc.zagred;

import com.newuniverse.nusmp.entity.zogratis.MoveSpec;

import java.util.List;

/**
 * Why: the numbers of Zagred's fourteen moves in one pure table the framework's move scheduler can read without Minecraft (the same shape as
 * {@code DanteTable}): which power unlocks each, how long its telegraph lasts, how often it can come, at what distance it makes sense, and which Word it
 * is. {@code ZagredBoss} turns each row into a {@code Move} by adding the telegraph and the code, found by id, and the tests play the table through the
 * scheduler to prove the shape of the fight: what he does at each act, that no distance leaves him with nothing to do, that the ultimate is rare.
 *
 * <p>The shape of the fight: a Word-caster who keeps his distance in the human form (mid range, the Words), a brawler with wings in the devil form (the
 * claw becomes a leap), a few moves that answer what the party does (Shatter against volleys, Reverse against bursts, Banish against a crowd, Seal against
 * the strongest caster, Fall against fliers), the Verdict that puts a word of its own over several players, and one ultimate, Overwrite, from 80%.
 * Every move is telegraphed for at least sixteen ticks (twelve at the Gates, after the framework's 0.75 wind-up factor) and the ultimate for over three
 * seconds. Never reorder: the client reads the casting move by index.
 */
public final class ZagredTable {
    private ZagredTable() {}

    public static final String CLAW = "claw", HALT = "halt", FALL = "fall", TRIDENT = "trident", REDACT = "redact", ARTS = "borrowed_art",
            SWORDS = "swords", SEAL = "seal", SHATTER = "shatter", BANISH = "banish", PAGES = "call_pages", REVERSE = "reverse", VERDICT = "verdict",
            OVERWRITE = "overwrite";

    /** What a move is, for the AI and the telegraph. */
    public enum Kind { MELEE, WORD, SUMMON, ARTS }

    /** One row. The record's accessors are the {@link MoveSpec} methods. {@code word} is null for the moves that are not a spoken Word. */
    public record Spec(String id, String label, Kind kind, ZagredWord word, int minPower, int windUpTicks, int cooldownTicks, double minRange,
                       double maxRange, int weight, boolean ultimate, boolean crowd, int recovery) implements MoveSpec {
        /** The mana a move costs below 60% power: the framework's rule (6 + a sixteenth of its cooldown, at most 32; 60 for an ultimate). */
        @Override public double manaCost() { return ultimate ? 60 : Math.min(32, 6 + cooldownTicks / 16.0); }

        /** Is it a spoken Word (silence stops it)? */
        public boolean isWord() { return kind == Kind.WORD; }

        /** Does it bind or silence (so it needs the control law's permission)? */
        public boolean hardControl() { return word != null && word.hardControl(); }
    }

    private static final List<Spec> SPECS = List.of(
            new Spec(CLAW, "Inked Claw", Kind.MELEE, null, 20, 16, 70, 0, 9, 6, false, false, 10),
            new Spec(HALT, "Halt", Kind.WORD, ZagredWord.HALT, 20, 24, 300, 0, 24, 5, false, true, 12),
            new Spec(FALL, "Fall", Kind.WORD, ZagredWord.FALL, 20, 22, 320, 0, 22, 4, false, true, 12),
            new Spec(TRIDENT, "Otherworld Trident", Kind.WORD, ZagredWord.TRIDENT, 20, 20, 150, 5, 34, 5, false, false, 8),
            new Spec(REDACT, "Redact", Kind.WORD, ZagredWord.REDACT, 20, 28, 220, 3, 30, 4, false, false, 12),
            new Spec(ARTS, "Borrowed Art", Kind.ARTS, null, 20, 18, 260, 6, 30, 2, false, false, 8),
            new Spec(SWORDS, "Swords", Kind.WORD, ZagredWord.SWORDS, 40, 26, 260, 5, 32, 4, false, false, 10),
            new Spec(SEAL, "Seal", Kind.WORD, ZagredWord.SEAL, 40, 24, 360, 6, 32, 3, false, false, 10),
            new Spec(SHATTER, "Shatter", Kind.WORD, ZagredWord.SHATTER, 40, 16, 380, 0, 40, 2, false, false, 6),
            new Spec(BANISH, "Banish", Kind.WORD, ZagredWord.BANISH, 40, 26, 420, 0, 10, 3, false, true, 12),
            new Spec(PAGES, "Call the Pages", Kind.SUMMON, null, 40, 30, 700, 0, 48, 2, false, false, 10),
            new Spec(REVERSE, "Reverse", Kind.WORD, ZagredWord.REVERSE, 60, 18, 520, 0, 14, 2, false, false, 8),
            new Spec(VERDICT, "Verdict", Kind.WORD, ZagredWord.VERDICT, 60, 24, 560, 0, 36, 4, false, true, 10),
            new Spec(OVERWRITE, "Overwrite", Kind.WORD, ZagredWord.OVERWRITE, 80, 72, 1500, 0, 48, 5, true, false, 60));

    /** The table, in the order of the boss's move list. */
    public static List<Spec> specs() { return SPECS; }

    /** The row with this id, or null. */
    public static Spec byId(String id) {
        for (Spec s : SPECS) if (s.id().equals(id)) return s;
        return null;
    }
}
