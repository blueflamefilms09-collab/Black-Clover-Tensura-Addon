package com.newuniverse.nusmp.arc.early;

/**
 * Why: Vetto is the owner's reference for "anti-magic against overwhelming physical stats": his Beast Magic wraps animal auras round his body that make him
 * faster, harder to hurt and a great deal stronger, and in the anime the blade of anti-magic that was driven into him weakened the magic that was
 * strengthening his body. The fight is that, as rules. He always wears an aura (bear: heavy and armoured; cheetah: fast; eagle: leaps; at the Gates the
 * mythical aura of beasts that never lived), each is a fixed set of factors, and the aura can be torn off: at once by an anti-magic blow, or by a burst of
 * damage (a share of his health inside three seconds). A torn Vetto is weaker than he is bare (his body is hurt, and he cannot put an aura back on for a
 * while). So a party with anti-magic has a clean answer, and a party without it has a harder, honest one: hit him all at once. This pure class is the state;
 * the game glue applies the factors to his speed, damage and armour.
 */
public final class BeastAuras {
    /** The auras. NONE is the bare Vetto; MYTHICAL is the Gates aura. */
    public enum Aura { NONE, BEAR, CHEETAH, EAGLE, MYTHICAL }

    /** What an aura does: a factor on his damage and on his speed, and the share of an ordinary blow his body shrugs off. */
    public record Stats(double damage, double speed, double mitigation) {}

    /** What a blow did to the aura. */
    public enum Hit { NONE, ANTI_MAGIC, OVERLOAD }

    /** Ticks torn after an anti-magic blow, after an overload, after his own Mana Release, and after a broken Mana Release. */
    public static final int TORN_ANTI_MAGIC = 160, TORN_OVERLOAD = 80, TORN_RELEASE = 100, TORN_BROKEN = 200;
    /** Share of his maximum health, dealt inside {@value #OVERLOAD_WINDOW} ticks, that overloads an aura (more for the mythical one). */
    public static final double OVERLOAD_SHARE = 0.08, OVERLOAD_SHARE_MYTHICAL = 0.12;
    public static final int OVERLOAD_WINDOW = 60;

    private static final Stats BARE_STATS = new Stats(1.0, 1.0, 0.0);
    private static final Stats BEAR_STATS = new Stats(1.25, 0.92, 0.30);
    private static final Stats CHEETAH_STATS = new Stats(0.95, 1.28, 0.10);
    private static final Stats EAGLE_STATS = new Stats(1.05, 1.10, 0.15);
    private static final Stats MYTHICAL_STATS = new Stats(1.35, 1.20, 0.25);
    private static final Stats TORN_STATS = new Stats(0.80, 0.85, 0.0);

    private Aura current = Aura.NONE;
    private final Window torn = new Window();
    private final BurstMeter overload = new BurstMeter(OVERLOAD_WINDOW);

    /** The factors of an aura worn whole. */
    public static Stats stats(Aura a) {
        return switch (a) {
            case BEAR -> BEAR_STATS;
            case CHEETAH -> CHEETAH_STATS;
            case EAGLE -> EAGLE_STATS;
            case MYTHICAL -> MYTHICAL_STATS;
            default -> BARE_STATS;
        };
    }

    /** The aura that follows another in the cycle (bear, cheetah, eagle, bear ...); from NONE or MYTHICAL it starts with the bear. */
    public static Aura next(Aura a) {
        return switch (a) {
            case BEAR -> Aura.CHEETAH;
            case CHEETAH -> Aura.EAGLE;
            default -> Aura.BEAR;
        };
    }

    public Aura current() { return current; }

    /** Puts an aura on. Does nothing while he is torn. True if it was put on. */
    public boolean wear(Aura a, long now) {
        if (torn.active(now)) return false;
        current = a;
        overload.clear();
        return true;
    }

    /** Is he torn at {@code now}? */
    public boolean torn(long now) { return torn.active(now); }

    public int tornLeft(long now) { return torn.left(now); }

    /** The factors he fights with now: the worn aura, or the torn ones while the wound is open. */
    public Stats now(long now) { return torn.active(now) ? TORN_STATS : stats(current); }

    /** Tears the aura off and opens the wound for {@code ticks} ticks. */
    public void strip(long now, int ticks) {
        current = Aura.NONE;
        torn.open(now, ticks);
        overload.clear();
    }

    /** Opens the wound without touching the aura (his own release of mana). */
    public void wound(long now, int ticks) { torn.open(now, ticks); }

    public static double overloadThreshold(double maxHealth, Aura a) {
        return maxHealth * (a == Aura.MYTHICAL ? OVERLOAD_SHARE_MYTHICAL : OVERLOAD_SHARE);
    }

    /** A blow on Vetto. With an aura on, anti-magic tears it off, and so does a burst that carries the last three seconds' damage over the threshold. */
    public Hit hit(long now, double damage, boolean antiMagic, double maxHealth) {
        if (damage <= 0 || current == Aura.NONE) return Hit.NONE;
        if (antiMagic) {
            strip(now, TORN_ANTI_MAGIC);
            return Hit.ANTI_MAGIC;
        }
        overload.record(now, damage);
        if (overload.total(now) < overloadThreshold(maxHealth, current)) return Hit.NONE;
        strip(now, TORN_OVERLOAD);
        return Hit.OVERLOAD;
    }

    /** A new fight: bare, healed. */
    public void reset() {
        current = Aura.NONE;
        torn.close();
        overload.clear();
    }
}
