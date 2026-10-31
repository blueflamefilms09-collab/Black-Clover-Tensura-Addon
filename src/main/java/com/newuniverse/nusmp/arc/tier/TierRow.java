package com.newuniverse.nusmp.arc.tier;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcSaga;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: one light boss of the owner's tier list in one immutable value: the framework's {@link ArcCharacter} (what every system of the arcs reads), its tier letter,
 * its moves and its size. The roster tables ({@code TierRosterF}, {@code TierRosterD}, ...) are lists of these, so adding a character is adding one block of data,
 * and the boss class, the registration, the tests and the skin generator all read the same rows. Pure (no game types besides the record the arcs already share).
 *
 * @param character the arcs' record (stats from the letter, floors inside the letter's band, saga {@link ArcSaga#TIER_LIST})
 * @param letter    the tier of the owner's list
 * @param moves     its moves, in the order the scheduler considers them
 * @param height    its height in blocks (the one body model is scaled to it; the hitbox follows)
 */
public record TierRow(ArcCharacter character, TierLetter letter, List<TierMove> moves, float height, List<TierTraitSpec> traits) {
    public TierRow {
        moves = List.copyOf(moves);
        traits = traits == null ? List.of() : List.copyOf(traits);
    }

    /** The row without traits (3.33.0 shape, kept for old callers and tests). */
    public TierRow(ArcCharacter character, TierLetter letter, List<TierMove> moves, float height) {
        this(character, letter, moves, height, List.of());
    }

    /** 3.39.0: does the row carry this trait? */
    public boolean has(TierTrait t) {
        for (TierTraitSpec s : traits) if (s.trait() == t) return true;
        return false;
    }

    /** 3.39.0: the move id a trait hangs on (null if the row has no such trait or it stands alone). */
    public String moveOf(TierTrait t) {
        for (TierTraitSpec s : traits) if (s.trait() == t) return s.moveId();
        return null;
    }

    /** 3.39.0: points of the trait budget spent. */
    public int traitPoints() {
        int n = 0;
        for (TierTraitSpec s : traits) n += s.trait().price();
        return n;
    }

    public String id() { return character.id(); }

    /** The hitbox width for a body of this height. */
    public float width() { return Math.max(0.6f, height * 0.34f); }

    public static Builder of(TierLetter letter, String id, String name, String source) { return new Builder(letter, id, name, source); }

    public static final class Builder {
        private final TierLetter letter;
        private final ArcCharacter.Builder c;
        private int floorMin, floorMax;
        private float height = 1.9f;
        private double hp = 1.0, atk = 1.0;
        private final List<TierMove> moves = new ArrayList<>();
        private final List<TierTraitSpec> traits = new ArrayList<>();

        private Builder(TierLetter letter, String id, String name, String source) {
            this.letter = letter;
            this.floorMin = letter.floorMin();
            this.floorMax = letter.floorMax();
            this.c = ArcCharacter.boss(id, name).source(source).saga(ArcSaga.TIER_LIST).tier(letter.buffTier());
        }

        public Builder title(String t) { c.title(t); return this; }
        public Builder colors(int main, int rim) { c.colors(main, rim); return this; }
        public Builder floors(int min, int max) { floorMin = min; floorMax = max; return this; }
        public Builder height(float h) { height = h; return this; }
        /** Multiples of the tier's health and damage for a character that is a little below or above the others of its tier. */
        public Builder scale(double health, double attack) { hp = health; atk = attack; return this; }
        public Builder lines(String opening, String gates, String death) { c.lines(opening, gates, death); return this; }
        public Builder move(TierMove m) { moves.add(m); return this; }
        public Builder move(TierMove.Builder m) { moves.add(m.build()); return this; }
        /** 3.39.0: a standalone trait (slow field, armour, soul marks, fusion, sight, climb, poison, steam, system lock, shield break). */
        public Builder trait(TierTrait t) { traits.add(TierTraitSpec.of(t)); return this; }
        /** 3.39.0: a trait that hangs on one move of this row (balance, mirror, pull, karma, echo, untouchable). */
        public Builder trait(TierTrait t, String moveId) { traits.add(TierTraitSpec.on(t, moveId)); return this; }

        public TierRow build() {
            c.stats(Math.round(letter.health() * hp), Math.round(letter.attack() * atk * 10) / 10.0, letter.speed(), letter.armor()).floors(null, floorMin, floorMax);
            return new TierRow(c.build(), letter, moves, height, traits);
        }
    }
}
