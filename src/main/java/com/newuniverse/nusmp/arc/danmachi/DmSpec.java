package com.newuniverse.nusmp.arc.danmachi;

import com.newuniverse.nusmp.entity.zogratis.MoveSpec;

/**
 * Why: a boss's moves are mostly numbers (when it may use the move, how long the telegraph lasts, how big the shape on the floor is, how hard it hits),
 * and the same few shapes serve all four monsters, so a move is one row of data that the pure scheduler can read and the tests can check without a game.
 * This record is that row: the {@link MoveSpec} numbers the framework's scheduler needs, plus which {@link Shape} the move draws and its parameters. The
 * boss class turns a row into a {@code Move} by adding the telegraph and the strike that {@code DmShapes} runs for the shape; a {@link Shape#CUSTOM} row is
 * a move the boss class writes itself (the Howl, the summons, the mist, the ultimates).
 *
 * <p>What the parameters mean, by shape (a number the shape does not use is 0):
 * <ul>
 *   <li>{@code CONE}: {@code reach} blocks, {@code width} = half angle in degrees, {@code damage}, {@code knock}, {@code extra} 1 = also throws the foe up.</li>
 *   <li>{@code SPIN}: a circle round the body of radius {@code reach}, {@code damage}, {@code knock}.</li>
 *   <li>{@code WAVE}: a ring wave of {@code reach}, {@code damage}, {@code knock}, {@code count} waves, {@code extra} = ticks between waves. It can be jumped.</li>
 *   <li>{@code LANE}: {@code count} lanes side by side (at least 1), each {@code reach} = length and {@code width} wide, {@code damage}, {@code knock}, {@code extra} = ticks per block it takes to run (0 = at once).</li>
 *   <li>{@code MARKS}: {@code count} circles of radius {@code reach} on the ground, {@code damage}, {@code knock}, {@code extra} = ticks after the wind-up.</li>
 *   <li>{@code CHARGE}: {@code reach} = length, {@code width}, {@code damage}, {@code knock}, {@code extra} = blocks a tick.</li>
 *   <li>{@code LEAP}: the boss jumps onto the target's mark and a ring of {@code reach} blocks lands, {@code damage}, {@code knock}, {@code extra} = ticks in the air.</li>
 *   <li>{@code VOLLEY}: {@code count} projectiles fanned over {@code width} degrees, {@code reach} = range, {@code damage}, {@code extra} = blocks a tick.</li>
 *   <li>{@code SPOKES}: {@code count} lanes from the boss, {@code reach} = length, {@code width}, {@code damage}, {@code knock}.</li>
 *   <li>{@code BREATH}: a cone of {@code reach} and half angle {@code width}, {@code count} pulses of {@code damage}, swept over {@code extra} degrees.</li>
 * </ul>
 *
 * @param leveled true for a move only the leveled form uses (it needs a Level-Up step, see {@link DungeonLevel})
 */
public record DmSpec(String id, String label, int minPower, int windUpTicks, int cooldownTicks, double minRange, double maxRange, int weight,
                     boolean ultimate, boolean crowd, int recovery, boolean leveled, Shape shape,
                     double reach, double width, double damage, int count, double knock, double extra) implements MoveSpec {

    /** The shapes a move can draw on the floor. */
    public enum Shape { CONE, SPIN, WAVE, LANE, MARKS, CHARGE, LEAP, VOLLEY, SPOKES, BREATH, CUSTOM }

    public DmSpec {
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("a move needs an id");
        if (label == null || label.isBlank()) throw new IllegalArgumentException("move " + id + " needs a label");
        if (shape == null) throw new IllegalArgumentException("move " + id + " needs a shape");
    }

    /** Mana below 60% power: the framework's own rule ({@code Move}): 6 plus a sixteenth of the cooldown, at most 32; 60 for an ultimate. */
    @Override public double manaCost() { return ultimate ? 60 : Math.min(32, 6 + cooldownTicks / 16.0); }

    /** How many marks, projectiles or spokes the move makes when the boss's volley size is {@code slots} (1 at the opening, 4 at the Gates). */
    public int scaledCount(int slots) {
        int more = Math.max(1, slots) - 1;
        return switch (shape) {
            case MARKS -> Math.min(8, count + more);
            case VOLLEY -> Math.min(12, count + 2 * more);
            case SPOKES -> Math.min(9, count + more);
            default -> count;
        };
    }

    /** Starts a row. */
    public static Builder move(String id, String label) { return new Builder(id, label); }

    /** The numbers every move has; a shape method finishes the row. Defaults: power 20, wind-up 20, cooldown 100, range 0 to 48, weight 3, recovery 10. */
    public static final class Builder {
        private final String id, label;
        private int minPower = 20, wind = 20, cooldown = 100, weight = 3, recovery = 10;
        private double minRange = 0, maxRange = 48;
        private boolean ultimate, crowd, leveled;

        private Builder(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public Builder power(int v) { minPower = v; return this; }
        public Builder wind(int v) { wind = v; return this; }
        public Builder cd(int v) { cooldown = v; return this; }
        public Builder range(double min, double max) { minRange = min; maxRange = max; return this; }
        public Builder weight(int v) { weight = v; return this; }
        public Builder recovery(int v) { recovery = v; return this; }
        /** An ultimate: spaced by the power-tuned gap, never twice in a row, exposed for 60 ticks after it. */
        public Builder ultimate() { ultimate = true; recovery = 60; return this; }
        public Builder crowd() { crowd = true; return this; }
        /** A move of the leveled form only. */
        public Builder leveled() { leveled = true; return this; }

        private DmSpec done(DmSpec.Shape shape, double reach, double width, double damage, int count, double knock, double extra) {
            return new DmSpec(id, label, minPower, wind, cooldown, minRange, maxRange, weight, ultimate, crowd, recovery, leveled, shape, reach, width, damage, count, knock, extra);
        }

        public DmSpec cone(double reach, double halfAngleDeg, double damage, double knock, boolean lift) { return done(Shape.CONE, reach, halfAngleDeg, damage, 0, knock, lift ? 1 : 0); }
        public DmSpec spin(double reach, double damage, double knock) { return done(Shape.SPIN, reach, 0, damage, 0, knock, 0); }
        public DmSpec wave(double reach, double damage, double knock, int waves, int gapTicks) { return done(Shape.WAVE, reach, 0, damage, waves, knock, gapTicks); }
        public DmSpec lane(int lanes, double length, double width, double damage, double knock, double ticksPerBlock) { return done(Shape.LANE, length, width, damage, Math.max(1, lanes), knock, ticksPerBlock); }
        public DmSpec marks(int count, double radius, double damage, double knock, int delayTicks) { return done(Shape.MARKS, radius, 0, damage, count, knock, delayTicks); }
        public DmSpec charge(double length, double width, double damage, double knock, double blocksPerTick) { return done(Shape.CHARGE, length, width, damage, 0, knock, blocksPerTick); }
        public DmSpec leap(double ringReach, double damage, double knock, int airTicks) { return done(Shape.LEAP, ringReach, 0, damage, 0, knock, airTicks); }
        public DmSpec volley(int count, double spreadDeg, double damage, double range, double blocksPerTick) { return done(Shape.VOLLEY, range, spreadDeg, damage, count, 0, blocksPerTick); }
        public DmSpec spokes(int count, double length, double width, double damage, double knock) { return done(Shape.SPOKES, length, width, damage, count, knock, 0); }
        public DmSpec breath(double reach, double halfAngleDeg, int pulses, double damagePerPulse, double sweepDeg) { return done(Shape.BREATH, reach, halfAngleDeg, damagePerPulse, pulses, 0, sweepDeg); }
        /** A move the boss class writes itself; {@code damage} and {@code reach} are for its own use (and for the tests' sanity checks). */
        public DmSpec custom(double reach, double damage) { return done(Shape.CUSTOM, reach, 0, damage, 0, 0, 0); }
    }
}
