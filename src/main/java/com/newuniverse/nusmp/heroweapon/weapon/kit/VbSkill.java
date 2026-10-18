package com.newuniverse.nusmp.heroweapon.weapon.kit;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Why: every technique of the Staff, the Gauntlet, the Projectile and the Carriage is a row of the weapon's {@code Numbers} table (cooldown, cost, ultimate or
 * not) plus a body, and the live skill must be built FROM that row so that what the dispatcher sees is exactly what the budget test judged. The contract's
 * {@code SimpleSkill} fixes the cooldown at construction, but the Staff hastes its cooldowns and the Gauntlet's blossom step shortens two of them, so the cooldown
 * here may be computed from the live context ({@link Cooldown}); it is always at least the floor of the technique's kind. The skill never starts its own
 * cooldown and never pays its own cost: the dispatcher does both. A body that throws is caught here (the rule is that a skill never throws out), counted, logged
 * once per skill with its stack, and answered with a refusal (nothing paid, no cooldown); the self-test reads the counter so that a fault still fails the test.
 */
public final class VbSkill implements HeroSkill {
    /** The effect of a technique: true when it was used. */
    @FunctionalInterface
    public interface Body {
        boolean cast(HeroCtx c);
    }

    /** A cooldown that depends on the wielder's state (haste, a passive). */
    @FunctionalInterface
    public interface Cooldown {
        int ticks(HeroCtx c, Budget.Spec spec);
    }

    private static final AtomicLong FAULTS = new AtomicLong();
    private static volatile String lastFault = "";
    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();

    /** How many skill bodies have thrown since the game started (the self-tests compare it before and after a cast). */
    public static long faults() { return FAULTS.get(); }

    /** The id and the exception of the latest fault, for the self-test's message. */
    public static String lastFault() { return lastFault; }

    /** Forgets which faults were already logged (called when a server stops, so the next session logs its own first fault again). */
    public static void forgetLogged() {
        LOGGED.clear();
        lastFault = "";
    }

    private final Budget.Spec spec;
    private final String name, desc;
    private final Body body;
    private final Cooldown cooldown;

    private VbSkill(Budget.Spec spec, String name, String desc, Body body, Cooldown cooldown) {
        this.spec = spec;
        this.name = name;
        this.desc = desc;
        this.body = body;
        this.cooldown = cooldown;
    }

    /** A skill whose id, cooldown, cost and ultimate flag are the Spec's. */
    public static VbSkill of(Budget.Spec spec, String name, String desc, Body body) { return new VbSkill(spec, name, desc, body, null); }

    /** The same skill with a cooldown computed from the live context; the result is never below the budget floor of the skill's kind. */
    public VbSkill withCooldown(Cooldown rule) { return new VbSkill(spec, name, desc, body, rule); }

    public Budget.Spec spec() { return spec; }

    @Override
    public String id() { return spec.id(); }

    @Override
    public String name() { return name; }

    @Override
    public String desc() { return desc; }

    @Override
    public int cooldownTicks(HeroCtx c) {
        if (cooldown == null) return spec.cooldown();
        return Math.max(Budget.floor(spec.kind()), cooldown.ticks(c, spec));
    }

    @Override
    public double magoiShare() { return spec.cost(); }

    @Override
    public boolean ultimate() { return spec.ultimate(); }

    @Override
    public boolean cast(HeroCtx c) {
        try {
            return body.cast(c);
        } catch (RuntimeException | LinkageError e) {
            FAULTS.incrementAndGet();
            lastFault = spec.id() + " threw " + e;
            if (LOGGED.add(spec.id())) LogUtils.getLogger().error("[nusmp] hero skill {} failed and was refused", spec.id(), e);
            return false;
        }
    }
}
