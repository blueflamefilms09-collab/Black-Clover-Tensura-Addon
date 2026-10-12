package com.newuniverse.nusmp.dungeon.wild;

/**
 * The hunger of one creature of the Living Dungeon (0-100, the scale of dungeon.DungeonCreature): it falls by one every
 * {@link WildRules#DECAY_TICKS} game ticks (a legendary creature burns three times slower), and at zero the creature starves a little every
 * {@link WildRules#STARVE_TICKS}. The clocks are not saved: a creature that was out of reach for a while (an unloaded chunk) starts again
 * from now instead of starving all at once. Pure.
 */
public final class Appetite {
    /** A gap longer than this many game ticks between two looks counts as "was away". */
    public static final long AWAY = 1000L;

    private int hunger;
    private long clock = Long.MIN_VALUE, starved = Long.MIN_VALUE;

    public Appetite(int hunger) { this.hunger = WildRules.clampHunger(hunger); }

    public int hunger() { return hunger; }

    public void set(int hunger) { this.hunger = WildRules.clampHunger(hunger); }

    public boolean hungry() { return WildRules.hungry(hunger); }

    public boolean full() { return WildRules.full(hunger); }

    /** It ate: hunger rises and the starving clock stops. */
    public void feed(int amount) {
        hunger = WildRules.clampHunger(hunger + Math.max(0, amount));
        if (hunger > 0) starved = Long.MIN_VALUE;
    }

    /** Moves to game time {@code now}. Returns true when a point of starvation damage is due. */
    public boolean advance(long now, boolean slow) {
        int period = slow ? WildRules.LEGEND_DECAY_TICKS : WildRules.DECAY_TICKS;
        if (clock == Long.MIN_VALUE || now < clock || now - clock > AWAY) {
            clock = now;
        } else {
            long steps = (now - clock) / period;
            if (steps > 0) {
                hunger = WildRules.clampHunger(hunger - (int) Math.min(steps, WildRules.HUNGER_MAX));
                clock += steps * period;
            }
        }
        if (hunger > 0) {
            starved = Long.MIN_VALUE;
            return false;
        }
        if (starved == Long.MIN_VALUE || now < starved || now - starved > AWAY) {
            starved = now;
            return false;
        }
        if (now - starved >= WildRules.STARVE_TICKS) {
            starved = now;
            return true;
        }
        return false;
    }
}
