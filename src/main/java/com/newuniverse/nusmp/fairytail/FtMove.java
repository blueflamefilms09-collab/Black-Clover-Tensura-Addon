package com.newuniverse.nusmp.fairytail;

/**
 * Why: one spell of a Fairy Tail skill is a row of numbers, not a class. The row says what shape it has ({@link FtKind}), what it costs (a
 * share of the caster's maximum Aura, with a floor; Aura first and magicules when the Aura is empty, through the mod's energy bridge), how
 * long it recovers, how hard it hits before the balance law scales it by the caster's EP, how far and wide it reaches, how much mastery
 * of the skill opens it and, for forms and buffs, how long it lasts.
 *
 * @param id        stable key (lang key and mode id): never renamed once shipped
 * @param name      English fallback name
 * @param unlockPct mastery of the skill (0 to 100) needed before the mode can be selected
 * @param costFrac  share of max Aura (0.05 = 5 percent)
 * @param cooldown  ticks
 * @param damage    base damage (the balance law multiplies it by the caster's EP scale)
 * @param range     blocks (lane length, reach, dash distance, aim distance)
 * @param radius    blocks (lane half width, burst radius)
 * @param ticks     duration of a buff, form or lock
 * @param count     pulses, lances, blows or amplifier, by shape
 * @param form      index into {@link FtForms} when the move is a form (or leaves a short armour), else -1
 */
public record FtMove(String id, String name, FtKind kind, int unlockPct, double costFrac, int cooldown, double damage, double range,
                     double radius, int ticks, int count, int form) {

    /** Floor of the Aura cost (a spell is never free for a big pool). */
    public static final double COST_FLOOR = 12.0;

    public static FtMove of(String id, String name, FtKind kind, int unlockPct, double costFrac, int cooldown, double damage, double range,
                            double radius, int ticks, int count) {
        return new FtMove(id, name, kind, unlockPct, costFrac, cooldown, damage, range, radius, ticks, count, -1);
    }

    public FtMove withForm(int formIndex) {
        return new FtMove(id, name, kind, unlockPct, costFrac, cooldown, damage, range, radius, ticks, count, formIndex);
    }

    /** True for the shapes that deal damage (the damage column means something). */
    public boolean hurts() {
        return switch (kind) {
            case BREATH, CONE, DASH, RING, TARGET_AREA, RAIN, BARRAGE, COMBO, LIFT, BOMB, DISPEL, LOCK -> true;
            default -> false;
        };
    }
}
