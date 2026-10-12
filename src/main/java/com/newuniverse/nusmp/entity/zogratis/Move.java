package com.newuniverse.nusmp.entity.zogratis;

import net.minecraft.world.entity.LivingEntity;

/**
 * One thing a Zogratis boss can do: its numbers (a {@link MoveSpec}, read by the {@link MoveScheduler}) and the code that runs
 * when the wind-up ends. Build them with {@link #builder} in the boss's {@code moves()}:
 * <pre>
 * Move.builder("rock_swords", "Compressed Rock Swords").minPower(20).windUp(24).cooldown(160).range(5, 32).weight(4)
 *     .run((boss, target) -> { ... });
 * </pre>
 * The run code executes on the server when the telegraph is over. {@code boss.tuning().damageMultiplier()} is already inside
 * {@link ZogratisBoss#bossDamage}; read {@code boss.power()} / {@code boss.tuning().castSlots()} to scale volleys.
 */
public final class Move implements MoveSpec {
    /** The move itself. The target is the boss's current target when the wind-up ends (never null). */
    @FunctionalInterface
    public interface MoveRun { void run(ZogratisBoss boss, LivingEntity target); }

    /** A move's own telegraph, called every third tick of the wind-up (replaces the default ring). */
    @FunctionalInterface
    public interface MoveTell { void tell(ZogratisBoss boss, LivingEntity target, int ticksLeft, int totalTicks); }

    /** Ticks after an ultimate during which the boss is exposed (takes extra damage) and starts nothing. */
    public static final int EXPOSED_FROM = 40;

    private final String id, label;
    private final int minPower, windUpTicks, cooldownTicks, weight, recoveryTicks;
    private final double minRange, maxRange, manaCost;
    private final boolean ultimate, crowd;
    private final MoveRun run;
    private final MoveTell tell;

    private Move(String id, String label, int minPower, int windUpTicks, int cooldownTicks, double minRange, double maxRange, int weight,
                 boolean ultimate, boolean crowd, double manaCost, int recoveryTicks, MoveRun run, MoveTell tell) {
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("a move needs an id");
        if (run == null) throw new IllegalArgumentException("move " + id + " has no code");
        this.id = id;
        this.label = label == null ? id : label;
        this.minPower = Math.max(0, minPower);
        this.windUpTicks = Math.max(0, windUpTicks);
        this.cooldownTicks = Math.max(0, cooldownTicks);
        this.minRange = Math.max(0, minRange);
        this.maxRange = Math.max(this.minRange, maxRange);
        this.weight = Math.max(1, weight);
        this.ultimate = ultimate;
        this.crowd = crowd;
        this.manaCost = manaCost >= 0 ? manaCost : ultimate ? 60 : Math.min(32, 6 + this.cooldownTicks / 16.0);
        this.recoveryTicks = recoveryTicks >= 0 ? recoveryTicks : ultimate ? 60 : 8;
        this.run = run;
        this.tell = tell;
    }

    /** All the numbers at once (mana cost, recovery and tell take their defaults). */
    public static Move of(String id, String label, int minPower, int windUpTicks, int cooldownTicks, double minRange, double maxRange,
                          int weight, boolean ultimate, MoveRun run) {
        return new Move(id, label, minPower, windUpTicks, cooldownTicks, minRange, maxRange, weight, ultimate, false, -1, -1, run, null);
    }

    public static Builder builder(String id, String label) { return new Builder(id, label); }

    @Override public String id() { return id; }
    @Override public String label() { return label; }
    @Override public int minPower() { return minPower; }
    @Override public int windUpTicks() { return windUpTicks; }
    @Override public int cooldownTicks() { return cooldownTicks; }
    @Override public double minRange() { return minRange; }
    @Override public double maxRange() { return maxRange; }
    @Override public int weight() { return weight; }
    @Override public boolean ultimate() { return ultimate; }
    @Override public double manaCost() { return manaCost; }
    @Override public boolean crowd() { return crowd; }

    /** Ticks after the move during which the boss starts nothing else (60 after an ultimate: it is exposed, 8 otherwise). */
    public int recoveryTicks() { return recoveryTicks; }
    public MoveRun run() { return run; }
    /** The move's own telegraph, or null for the default ring. */
    public MoveTell tell() { return tell; }

    @Override public String toString() { return "Move[" + id + "]"; }

    /** Defaults: power 20, wind-up 20, cooldown 100, range 0..48, weight 3, an ordinary move. */
    public static final class Builder {
        private final String id, label;
        private int minPower = 20, windUp = 20, cooldown = 100, weight = 3, recovery = -1;
        private double minRange = 0, maxRange = 48, mana = -1;
        private boolean ultimate, crowd;
        private MoveTell tell;

        private Builder(String id, String label) { this.id = id; this.label = label; }

        public Builder minPower(int percent) { this.minPower = percent; return this; }
        public Builder windUp(int ticks) { this.windUp = ticks; return this; }
        public Builder cooldown(int ticks) { this.cooldown = ticks; return this; }
        public Builder range(double min, double max) { this.minRange = min; this.maxRange = max; return this; }
        public Builder weight(int w) { this.weight = w; return this; }
        /** An ultimate: spaced by the power-tuned gap, never twice in a row, 60 ticks of exposure after it. */
        public Builder ultimate() { this.ultimate = true; return this; }
        /** Preferred against two or more foes. */
        public Builder crowd() { this.crowd = true; return this; }
        /** Mana it costs below 60% power (default: from its cooldown, 60 for an ultimate). */
        public Builder mana(double cost) { this.mana = cost; return this; }
        /** Ticks after it during which the boss does nothing else; 40 or more also exposes it to extra damage. */
        public Builder recovery(int ticks) { this.recovery = ticks; return this; }
        public Builder tell(MoveTell t) { this.tell = t; return this; }

        /** Finishes the move with the code that runs when the wind-up ends. */
        public Move run(MoveRun code) {
            return new Move(id, label, minPower, windUp, cooldown, minRange, maxRange, weight, ultimate, crowd, mana, recovery, code, tell);
        }
    }
}
