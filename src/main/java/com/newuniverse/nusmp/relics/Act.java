package com.newuniverse.nusmp.relics;

/**
 * An active ability of a relic (right-click or the worn-relic key). Built fluently; every call returns a changed copy.
 * <ul>
 *   <li>{@link #cooldown}: ticks before it can be used again (per player, survives relog).</li>
 *   <li>{@link #mana}: percent of the user's maximum magicules, like the grimoire spells; paid in magicules first, then aura, as the Kotodama words are (refused when both together cannot pay).</li>
 *   <li>{@link #epPercent}: percent of the user's maximum magicules AND maximum aura (existence points); refused if it would leave either under 10%.</li>
 *   <li>{@link #usesPerDay}: the D&amp;D "per long rest" turned into uses per Minecraft day (24000 ticks), counted on the stack.</li>
 *   <li>{@link #needsSneak}, {@link #hold} (hold the use button/key this many ticks), {@link #toggle} (on and off).</li>
 * </ul>
 */
public final class Act {
    private final String key, displayName;
    private final ActBody body;
    private int cooldown, mana, perDay, hold;
    private double ep;
    private boolean sneak, toggle;

    private Act(String key, String displayName, ActBody body) {
        this.key = key;
        this.displayName = displayName;
        this.body = body;
    }

    private Act(Act o) {
        this(o.key, o.displayName, o.body);
        cooldown = o.cooldown;
        mana = o.mana;
        perDay = o.perDay;
        hold = o.hold;
        ep = o.ep;
        sneak = o.sneak;
        toggle = o.toggle;
    }

    /** {@code key} is a short stable id inside the relic ("ensnaring_ivy"); it names the cooldown and the use counter. */
    public static Act of(String key, String displayName, ActBody body) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("an act needs a key");
        return new Act(key, displayName == null ? key : displayName, body == null ? ctx -> { } : body);
    }

    public Act cooldown(int ticks) { Act a = new Act(this); a.cooldown = Math.max(0, ticks); return a; }
    public Act mana(int percentOfMaxMagicules) { Act a = new Act(this); a.mana = Math.max(0, Math.min(100, percentOfMaxMagicules)); return a; }
    public Act epPercent(double percent) { Act a = new Act(this); a.ep = Math.max(0, Math.min(90, percent)); return a; }
    public Act usesPerDay(int n) { Act a = new Act(this); a.perDay = Math.max(0, n); return a; }
    public Act needsSneak() { Act a = new Act(this); a.sneak = true; return a; }
    public Act hold(int ticks) { Act a = new Act(this); a.hold = Math.max(0, ticks); return a; }
    public Act toggle() { Act a = new Act(this); a.toggle = true; return a; }

    public String key() { return key; }
    public String displayName() { return displayName; }
    public ActBody body() { return body; }
    public int cooldownTicks() { return cooldown; }
    public int manaPercent() { return mana; }
    public double epPercentCost() { return ep; }
    public int usesPerDay() { return perDay; }
    public boolean sneakRequired() { return sneak; }
    public int holdTicks() { return hold; }
    public boolean toggles() { return toggle; }
}
