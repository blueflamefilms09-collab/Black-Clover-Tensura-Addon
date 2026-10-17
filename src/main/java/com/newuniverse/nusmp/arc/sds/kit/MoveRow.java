package com.newuniverse.nusmp.arc.sds.kit;

import com.newuniverse.nusmp.entity.zogratis.MoveSpec;

/**
 * Why: every Zogratis boss keeps its moves as a pure table first (the numbers: power, wind-up, cool-down, range, weight) and turns each row into a
 * {@code Move} by adding a telegraph and the code. A table has to be a {@link MoveSpec} so the scheduler can be played against it in a unit test
 * without a game, and the four Sins would otherwise each carry a copy of the same record. This is that one record. {@code free} is Merlin's Infinity (a move
 * that costs no mana at any power, which is the "endless magic uptime" of the character); {@code group} is a small tag a boss may use to sort its
 * moves (Diane's stances: 0 any, 1 giant, 2 human).
 */
public record MoveRow(String id, String label, int minPower, int windUpTicks, int cooldownTicks, double minRange, double maxRange, int weight,
                      boolean ultimate, boolean crowd, int recovery, boolean free, int group) implements MoveSpec {

    /** An ordinary move: pays mana below 60% power like the framework's default. */
    public static MoveRow of(String id, String label, int minPower, int windUp, int cooldown, double minRange, double maxRange, int weight, int recovery) {
        return new MoveRow(id, label, minPower, windUp, cooldown, minRange, maxRange, weight, false, false, recovery, false, 0);
    }

    public MoveRow crowdMove() { return new MoveRow(id, label, minPower, windUpTicks, cooldownTicks, minRange, maxRange, weight, ultimate, true, recovery, free, group); }

    public MoveRow ult() { return new MoveRow(id, label, minPower, windUpTicks, cooldownTicks, minRange, maxRange, weight, true, crowd, recovery, free, group); }

    /** The same move at no mana cost at any power. */
    public MoveRow noMana() { return new MoveRow(id, label, minPower, windUpTicks, cooldownTicks, minRange, maxRange, weight, ultimate, crowd, recovery, true, group); }

    public MoveRow inGroup(int g) { return new MoveRow(id, label, minPower, windUpTicks, cooldownTicks, minRange, maxRange, weight, ultimate, crowd, recovery, free, g); }

    /** The mana an ordinary move costs below 60% power (the framework's rule: 6 + a sixteenth of its cool-down, at most 32; 60 for an ultimate); 0 when {@code free}. */
    @Override public double manaCost() { return free ? 0 : ultimate ? 60 : Math.min(32, 6 + cooldownTicks / 16.0); }
}
