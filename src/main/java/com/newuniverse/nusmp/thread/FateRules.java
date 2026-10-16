package com.newuniverse.nusmp.thread;

/**
 * Why: Rouge's whole job is one decision, made inside a damage event: "does this blow need its fate rewritten, and may she afford
 * it?". A wrong answer either lets a friend die (too strict) or burns the caster's mana on harmless scratches (too loose), so the
 * decision is a pure function of plain numbers that unit tests can pin down, and the event glue ({@link RougeFateEvents}) only
 * gathers the numbers and acts on the verdict. Nothing here touches Minecraft classes.
 * <p>
 * The rule: a blow is rewritten when it would kill (its armour-reduced size reaches health plus absorption) or when it is a big hit
 * (at least {@code bigHitFraction} of the victim's maximum health). It is never rewritten when the damage type cannot be fate
 * (the void, /kill, anything that bypasses invulnerability, a stopped-time hit). The same attacker is skipped for a few ticks after
 * a save (one swing can arrive as several events). A boss blow is allowed but costs double and each victim gets a cooldown against
 * boss saves, so Rouge cannot be used to walk through a boss fight untouched.
 */
public final class FateRules {
    private FateRules() {}

    /** What to do with one incoming blow. */
    public enum Verdict {
        /** Not Rouge's business: let the blow land normally. */
        IGNORE,
        /** Same attacker as a save a moment ago: cancel again at no cost. */
        REPEAT_FREE,
        /** A boss blow while this victim's boss cooldown runs: the blow lands. */
        BOSS_COOLDOWN,
        /** Rewrite the fate: cancel the blow and charge the caster. */
        SAVE
    }

    /** The settings the rules read (from {@link ThreadConfig}). */
    public record Params(double bigHitFraction, double manaFlat, double manaPercent, int bossCooldownTicks, int repeatTicks) {
        public static Params defaults() { return new Params(0.35, 150, 20, 160, 10); }
    }

    /** Everything known about one blow. 'stoppedTime' / 'unsavableType' are decided by the glue from the damage source. */
    public record Hit(float amount, boolean ignoresArmor, double armor, double toughness, float health, float absorption, float maxHealth,
                      boolean unsavableType, boolean fromBoss, boolean sameAttackerRecently, long now, long bossCooldownUntil) {}

    /** Vanilla's armour formula (CombatRules.getDamageAfterAbsorb) on plain numbers; enchantments and effects are ignored on purpose. */
    public static float afterArmour(float amount, double armor, double toughness) {
        if (amount <= 0f) return 0f;
        double f = 2.0 + toughness / 4.0;
        double g = Math.min(20.0, Math.max(armor * 0.2, armor - amount / f));
        return (float) (amount * (1.0 - g / 25.0));
    }

    /** The size Rouge reasons about: the raw amount, or its armour-reduced estimate. */
    public static float estimate(Hit h) {
        return h.ignoresArmor() ? h.amount() : afterArmour(h.amount(), h.armor(), h.toughness());
    }

    /** Would this blow take the last of the victim's health and absorption? */
    public static boolean lethal(float estimate, float health, float absorption) {
        return estimate > 0f && estimate >= health + Math.max(0f, absorption);
    }

    /** Is it at least 'fraction' of the victim's maximum health? */
    public static boolean big(float estimate, float maxHealth, double fraction) {
        return maxHealth > 0f && estimate >= maxHealth * fraction;
    }

    public static Verdict judge(Hit h, Params p) {
        if (h.unsavableType() || h.amount() <= 0f) return Verdict.IGNORE;
        if (h.sameAttackerRecently()) return Verdict.REPEAT_FREE;
        float est = estimate(h);
        if (!lethal(est, h.health(), h.absorption()) && !big(est, h.maxHealth(), p.bigHitFraction())) return Verdict.IGNORE;
        if (h.fromBoss() && h.now() < h.bossCooldownUntil()) return Verdict.BOSS_COOLDOWN;
        return Verdict.SAVE;
    }

    /** The mana one rewrite costs: a flat part plus a percentage of the caster's maximum, doubled against a boss. Never negative. */
    public static double cost(double maxMana, Params p, boolean boss) {
        double c = Math.max(0.0, p.manaFlat()) + Math.max(0.0, maxMana) * Math.max(0.0, p.manaPercent()) / 100.0;
        return boss ? c * 2.0 : c;
    }

    /** She saves only if the whole price can be paid. */
    public static boolean canPay(double have, double cost) { return have >= cost; }

    /** True while the mark left by a save still covers this attacker. */
    public static boolean sameAttacker(String markedAttacker, long markedUntil, String attacker, long now) {
        return attacker != null && attacker.equals(markedAttacker) && now < markedUntil;
    }

    /** When the boss cooldown that a boss save starts ends. */
    public static long bossCooldownEnd(long now, Params p) { return now + p.bossCooldownTicks(); }
}
