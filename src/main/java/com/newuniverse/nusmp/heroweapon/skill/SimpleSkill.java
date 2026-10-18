package com.newuniverse.nusmp.heroweapon.skill;

/**
 * Why: about 150 techniques share one shape (an id, two texts, a cooldown, a cost, a body), and writing each as its own class would bury the part that matters, the
 * body, under boilerplate. A skill built with {@link #of} is exactly a {@link HeroSkill} whose {@link #cast} runs the lambda; {@link #windUp} and {@link #asUltimate}
 * return a changed copy. A weapon whose technique needs state (a channel, a mark table) writes its own class implementing {@link HeroSkill} instead.
 */
public final class SimpleSkill implements HeroSkill {
    /** The effect of a technique: true when it was used. */
    @FunctionalInterface
    public interface Body {
        boolean cast(HeroCtx c);
    }

    private final String id, name, desc;
    private final int cooldown, windUp;
    private final double cost;
    private final boolean ultimate;
    private final Body body;

    private SimpleSkill(String id, String name, String desc, int cooldown, double cost, int windUp, boolean ultimate, Body body) {
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.cooldown = cooldown;
        this.cost = cost;
        this.windUp = windUp;
        this.ultimate = ultimate;
        this.body = body;
    }

    /** A skill with no wind-up that is not an ultimate. {@code cooldownTicks} before the level scaling; {@code magoiShare} 0.02 is two percent of the maximum magicules. */
    public static SimpleSkill of(String id, String name, String desc, int cooldownTicks, double magoiShare, Body body) {
        return new SimpleSkill(id, name, desc, cooldownTicks, magoiShare, 0, false, body);
    }

    /** The same skill with a wind-up of {@code ticks} (the dispatcher waits that long, then casts). */
    public SimpleSkill windUp(int ticks) { return new SimpleSkill(id, name, desc, cooldown, cost, Math.max(0, ticks), ultimate, body); }

    /** The same skill marked as the strongest technique of its form ({@link #ultimate()} then answers true). */
    public SimpleSkill asUltimate() { return new SimpleSkill(id, name, desc, cooldown, cost, windUp, true, body); }

    @Override
    public String id() { return id; }

    @Override
    public String name() { return name; }

    @Override
    public String desc() { return desc; }

    @Override
    public int cooldownTicks(HeroCtx c) { return cooldown; }

    @Override
    public double magoiShare() { return cost; }

    @Override
    public int windUpTicks() { return windUp; }

    @Override
    public boolean ultimate() { return ultimate; }

    @Override
    public boolean cast(HeroCtx c) { return body.cast(c); }
}
