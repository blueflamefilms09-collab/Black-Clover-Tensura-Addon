package com.newuniverse.nusmp.heroweapon.weapon.strike;

import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.heroweapon.skill.SimpleSkill;

import java.util.function.ToDoubleFunction;

/**
 * Why: {@link SimpleSkill} fits a technique whose numbers never change, but two of these weapons let a technique's cooldown depend on the wielder (the Claw's skill mastery makes
 * a well-used technique recover faster, the Whip's top form recovers its movement faster), and the dispatcher asks the skill for its cooldown with the wielder's context. A
 * {@code StrikeSkill} is a {@link HeroSkill} built from a {@link SkillSpec} (so the numbers live in the pure table the budget test judges), a name, a text and a body, with an
 * optional cooldown factor that can never take the cooldown under the budget's floor for its kind. The wind-up is not used: the techniques that need one show their tell at the
 * moment of the cast and strike a few ticks later themselves ({@link Strike#later}), so the tell is visible from the first tick.
 */
public final class StrikeSkill implements HeroSkill {
    private final SkillSpec spec;
    private final String name, desc;
    private final SimpleSkill.Body body;
    private final ToDoubleFunction<HeroCtx> cooldownFactor;

    private StrikeSkill(SkillSpec spec, String name, String desc, SimpleSkill.Body body, ToDoubleFunction<HeroCtx> cooldownFactor) {
        this.spec = spec;
        this.name = name;
        this.desc = desc;
        this.body = body;
        this.cooldownFactor = cooldownFactor;
    }

    /** A technique with the numbers of {@code spec}. */
    public static StrikeSkill of(SkillSpec spec, String name, String desc, SimpleSkill.Body body) { return new StrikeSkill(spec, name, desc, body, null); }

    /** The same technique whose cooldown is multiplied by {@code factor(ctx)} (clamped to 0.5 to 1.5, and never below the floor of its kind). */
    public StrikeSkill cooldownFactor(ToDoubleFunction<HeroCtx> factor) { return new StrikeSkill(spec, name, desc, body, factor); }

    public SkillSpec spec() { return spec; }

    @Override
    public String id() { return spec.id(); }

    @Override
    public String name() { return name; }

    @Override
    public String desc() { return desc; }

    @Override
    public int cooldownTicks(HeroCtx c) {
        if (cooldownFactor == null || c == null) return spec.cooldown();
        double f;
        try {
            f = cooldownFactor.applyAsDouble(c);
        } catch (RuntimeException broken) {
            f = 1.0;
        }
        f = Math.max(0.5, Math.min(1.5, f));
        return (int) Math.max(Budget.cooldownFloor(spec.kind()), Math.round(spec.cooldown() * f));
    }

    @Override
    public double magoiShare() { return spec.cost(); }

    @Override
    public boolean ultimate() { return spec.ultimate(); }

    @Override
    public boolean cast(HeroCtx c) { return body.cast(c); }
}
