package com.newuniverse.nusmp.entity.zogratis;

/**
 * The timing of one Spatial Mana Domination (pure, unit tested). The cube is drawn from age 0. For its first {@link #FORM_TICKS}
 * ticks it is only closing: nothing is sealed yet, so anyone caught inside the walls as they snap shut still has a moment to step
 * out. From then until the last tick the seal bites, and at age {@code total} the cube is gone.
 *
 * @param total  ticks from the first wall to the last
 * @param form   ticks of closing before the seal begins
 */
public record DomainClock(int total, int form) {
    public static final int FORM_TICKS = 16, MIN_SEALED = 20, MAX_TOTAL = 600;

    public DomainClock {
        form = Math.max(0, Math.min(form, MAX_TOTAL - MIN_SEALED));
        total = Math.max(form + MIN_SEALED, Math.min(MAX_TOTAL, total));
    }

    /** A clock of 'ticks' ticks with the usual closing time (at least {@link #MIN_SEALED} ticks of seal, at most {@link #MAX_TOTAL}). */
    public static DomainClock of(int ticks) { return new DomainClock(ticks, FORM_TICKS); }

    public boolean forming(int age) { return age >= 0 && age < form; }

    /** The seal bites from the end of the closing until the cube is gone. */
    public boolean sealing(int age) { return age >= form && age < total; }

    public boolean over(int age) { return age >= total; }

    /** Ticks of seal still to come at this age (0 before it starts counts the whole seal; 0 once over). */
    public int sealLeft(int age) { return age >= total ? 0 : age < form ? total - form : total - age; }

    /** Ticks until the seal begins (0 once it has). */
    public int untilSealing(int age) { return Math.max(0, form - age); }

    /** 0..1 through the whole life of the cube. */
    public double progress(int age) { return Math.max(0, Math.min(1, age / (double) total)); }
}
