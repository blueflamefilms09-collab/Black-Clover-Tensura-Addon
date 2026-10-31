package com.newuniverse.nusmp.arc.tier;

/**
 * Why: a light boss has "a few canon moves", and writing a move class per character per move would be hundreds of classes. A move here is plain data: which of a
 * handful of bounded shapes it takes ({@link Kind}), the numbers of that shape, an optional status on the foes it hits, and the name it shows. The game side
 * ({@code TierMoves}) turns a row into a framework {@code Move} once; every shape is built on the early bosses' tested acts (cone, circle, ring wave, bolt, charge,
 * marked circles, wisps), so the cost of any move is bounded the same way (at most 24 foes looked at, a bolt lives at most 40 ticks, at most 5 bolts or marks,
 * 4 wisps) and no move changes a block, spawns an entity or heals.
 *
 * <p>{@link Basis} says honestly where the name comes from, because the wiki could not be reached when the first batch was written: NAMED = the move's name as I
 * remember it from the series, THEME = something the character canonically has or does, named and shaped by me, FILLER = an invented attack so the fight has enough
 * moves. None of it is wiki-verified yet.
 *
 * @param size  per kind: reach of a cleave, radius of a slam / wave / marked circle / wisp burst, ticks of a rush, length of a beam, spread in degrees of a volley
 * @param count bolts of a volley, circles of a marked strike, wisps of a swarm (1 for the other kinds)
 * @param damage multiple of the boss's melee damage
 * @param knock blocks-per-impulse shove on a hit (0 = none)
 */
public record TierMove(String id, String label, Kind kind, int minPower, int windUp, int cooldown, double minRange, double maxRange, int weight,
                       double damage, double size, int count, Effect effect, int effectTicks, double knock, boolean magic, boolean ultimate, boolean crowd, Basis basis) {

    /** The shapes a move can take. */
    public enum Kind { CLEAVE, SLAM, WAVE, BOLT, VOLLEY, MARKED, RUSH, BEAM, SWARM }

    /** What a hit leaves on the foe. */
    public enum Effect { NONE, SLOW, WEAKEN, BLIND, FROST, BURN,
        /** 3.39.0 (appended): poison for the given ticks (vanilla poison stops at 1 health, so it never kills). */
        POISON,
        /** 3.39.0: disables a raised shield for the given ticks (the shield item's cooldown). */
        SHIELD_BREAK,
        /** 3.39.0: strips beneficial effects: 1 for an ordinary hit, every one in reach for a crowd (ultimate) move. The tick count is only a marker (it must be above 0). */
        STRIP }

    /** Where the move's name comes from (see the class comment). */
    public enum Basis { NAMED, THEME, FILLER }

    public static final int MAX_COUNT = 5, MAX_EFFECT_TICKS = 120;
    public static final double MAX_DAMAGE = 4.0, MAX_SIZE = 16.0, MAX_SPREAD = 90.0, MAX_KNOCK = 1.5;

    public static Builder of(String id, String label, Kind kind) { return new Builder(id, label, kind); }

    /** Fluent builder: ordinary defaults (power 20, wind-up 20, cooldown 100, range 0..48, weight 3, damage 1.0). */
    public static final class Builder {
        private final String id, label;
        private final Kind kind;
        private int minPower = 20, windUp = 20, cooldown = 100, weight = 3, count = 1, effectTicks;
        private double minRange, maxRange = 48, damage = 1.0, size = 4, knock;
        private Effect effect = Effect.NONE;
        private boolean magic, ultimate, crowd;
        private Basis basis = Basis.FILLER;

        private Builder(String id, String label, Kind kind) {
            this.id = id;
            this.label = label;
            this.kind = kind;
        }

        public Builder power(int p) { minPower = p; return this; }
        public Builder wind(int ticks) { windUp = ticks; return this; }
        public Builder cd(int ticks) { cooldown = ticks; return this; }
        public Builder range(double min, double max) { minRange = min; maxRange = max; return this; }
        public Builder weight(int w) { weight = w; return this; }
        public Builder dmg(double d) { damage = d; return this; }
        public Builder size(double s) { size = s; return this; }
        public Builder count(int n) { count = n; return this; }
        public Builder fx(Effect e, int ticks) { effect = e; effectTicks = ticks; return this; }
        public Builder knock(double k) { knock = k; return this; }
        public Builder magic() { magic = true; return this; }
        public Builder crowd() { crowd = true; return this; }
        /** The one signature move of the boss: from 60% power, a long wind-up, the boss is exposed afterwards. */
        public Builder ult() { ultimate = true; minPower = Math.max(minPower, 60); windUp = Math.max(windUp, 44); cooldown = Math.max(cooldown, 400); return this; }
        public Builder named() { basis = Basis.NAMED; return this; }
        public Builder theme() { basis = Basis.THEME; return this; }
        public Builder filler() { basis = Basis.FILLER; return this; }

        public TierMove build() {
            return new TierMove(id, label, kind, minPower, windUp, cooldown, minRange, maxRange, weight, damage, size, count, effect, effectTicks, knock, magic, ultimate, crowd, basis);
        }
    }
}
