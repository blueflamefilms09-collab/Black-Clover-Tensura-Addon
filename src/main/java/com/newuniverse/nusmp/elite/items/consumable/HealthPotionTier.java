package com.newuniverse.nusmp.elite.items.consumable;

/**
 * Why: the three Health Potion sizes as pure numbers (no Minecraft types), so the balance can be pinned by a test and read by the item, the tooltip and the lang fragment from one place. Designed here
 * (the original addon's numbers are not available): a quick heal first, then a regeneration tail, bigger sizes also a little absorption, and a longer drink time so the large one is a commitment, not
 * a reflex.
 *
 * <ul>
 *   <li><b>Small</b>: heals 6 health (3 hearts) at once and Regeneration I for 5 s (2 more): 8 in all. Quick to drink (1 s). Stacks to 16.</li>
 *   <li><b>Medium</b>: 12 (6 hearts) and Regeneration I for 10 s (4 more): 16 in all. 1.2 s. Stacks to 16.</li>
 *   <li><b>Large</b>: 20 (10 hearts) and Regeneration II for 10 s (8 more): 28 in all, and 4 hearts of Absorption for 30 s. 1.6 s. Stacks to 8.</li>
 * </ul>
 * Regeneration I heals 1 health every 50 ticks, II every 25 ticks (vanilla), which is what {@link #regenHeal()} counts.
 */
public enum HealthPotionTier {
    SMALL("small", 6f, 0, 100, 0f, 0, 20, 16),
    MEDIUM("medium", 12f, 0, 200, 0f, 0, 24, 16),
    LARGE("large", 20f, 1, 200, 8f, 600, 32, 8);

    private final String id;
    private final float heal;
    private final int regenAmp, regenTicks;
    private final float absorb;
    private final int absorbTicks, useTicks, stack;

    HealthPotionTier(String id, float heal, int regenAmp, int regenTicks, float absorb, int absorbTicks, int useTicks, int stack) {
        this.id = id;
        this.heal = heal;
        this.regenAmp = regenAmp;
        this.regenTicks = regenTicks;
        this.absorb = absorb;
        this.absorbTicks = absorbTicks;
        this.useTicks = useTicks;
        this.stack = stack;
    }

    /** {@code small}, {@code medium}, {@code large}. */
    public String id() { return id; }

    /** {@code health_potion_small} ... */
    public String itemId() { return "health_potion_" + id; }

    /** Health restored at once. */
    public float heal() { return heal; }

    /** The Regeneration amplifier (0 = level I). */
    public int regenAmp() { return regenAmp; }

    /** The Regeneration duration in ticks. */
    public int regenTicks() { return regenTicks; }

    /** Absorption health granted (0 for none), and for how long. */
    public float absorb() { return absorb; }

    public int absorbTicks() { return absorbTicks; }

    /** Ticks it takes to drink. */
    public int useTicks() { return useTicks; }

    /** The stack size. */
    public int stack() { return stack; }

    /** The health the regeneration tail heals over its whole duration (vanilla: 1 HP every 50 >> amp ticks). */
    public float regenHeal() { return (float) Math.floor(regenTicks / (double) (50 >> regenAmp)); }

    /** Everything one potion heals, the at-once part plus the tail. */
    public float totalHeal() { return heal + regenHeal(); }

    public static HealthPotionTier byId(String id) {
        for (HealthPotionTier t : values()) if (t.id.equals(id)) return t;
        return null;
    }
}
