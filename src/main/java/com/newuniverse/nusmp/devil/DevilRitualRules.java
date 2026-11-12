package com.newuniverse.nusmp.devil;

/**
 * Why: whether a devil's ritual may begin, whether it goes on, and what it costs are decisions of the server that must be testable without one, so they are pure functions here (3.76.0). Each
 * world asks for something else ({@link DevilWorld#need}): a relic, despair, blood and paper, an offering and a name, a roof, the lowest point, a book and ether. The numbers are in one place so
 * the menu text, the checks and the tests cannot disagree. No Minecraft type.
 */
public final class DevilRitualRules {
    private DevilRitualRules() {}

    /** How far from the place the ritual began the caller may stand (blocks): leaving the circle breaks it. */
    public static final double CIRCLE = 3.5;
    /** Ticks an offer of a contract stays open. */
    public static final int OFFER_TICKS = 600;
    /** Ticks between two rituals of one player (a failed or refused one too). */
    public static final int COOLDOWN = 1200;

    public static final int OFFERING_FLESH = 32, BERSERK_LEVELS = 30;
    public static final double BUTLER_HEALTH = 0.5, BERSERK_HEALTH = 0.25, EXORCIST_WILL = 1.0 / 3.0, TENSURA_NAMING = 0.5, ZEREF_ETHER = 1.0 / 3.0;
    /** Blue Exorcist: the drop of blood (health points paid when the ritual begins) and what the demon does when the will breaks. */
    public static final float EXORCIST_BLOOD = 4f, EXORCIST_BACKLASH = 6f;
    /** Black Clover: the devil strikes at these ticks, each for this share of the caller's maximum health (never the last heart). */
    public static final int[] BINDING_BLOWS = {60, 95, 130, 165};
    public static final double BINDING_BLOW_SHARE = 0.15;

    /** What the server knows about the caller when the ritual is asked for. */
    public record Facts(boolean enabled, boolean hasBond, boolean busy, long sinceLast, double healthShare, float health, boolean relic, boolean paper, boolean book, int flesh,
                        int levels, double magiculeShare, boolean openSky) {}

    public enum Denial {
        NONE(""),
        DISABLED("The devils of other worlds do not answer on this server."),
        BOND("You already hold a bond. A mage holds one: a Spirit, a Devil or a Djinn."),
        BUSY("A ritual is already running, or a devil is waiting for your answer."),
        COOLDOWN("The air has not settled from the last ritual. Wait a minute."),
        NO_RELIC("You carry no relic tied to a devil (an Infernal Contract)."),
        NOT_DESPAIR("A demon answers only despair: your health must be at half or lower."),
        NO_PAPER("You need a sheet of paper to draw the circle on."),
        NO_BLOOD("You are too weak to give the drop of blood (more than three hearts are needed)."),
        NO_OFFERING("The offering is too small: 32 rotten flesh."),
        NO_MAGICULES("You do not have the magicules it asks for."),
        OPEN_SKY("The Hall of Hell has a roof: this cannot be done under the open sky."),
        NOT_LOWEST("The Behelit sleeps. It wakes only at your lowest point: a quarter of your health or less."),
        NO_LEVELS("You have too little to sacrifice: 30 experience levels."),
        NO_BOOK("You need a book to open.");

        public final String text;

        Denial(String text) { this.text = text; }
    }

    /** May this caller begin the ritual of this world? The first thing that is wrong is told. */
    public static Denial check(DevilWorld w, Facts f) {
        if (!f.enabled()) return Denial.DISABLED;
        if (f.hasBond()) return Denial.BOND;
        if (f.busy()) return Denial.BUSY;
        if (f.sinceLast() >= 0 && f.sinceLast() < COOLDOWN) return Denial.COOLDOWN;
        return switch (w) {
            case BLACK_CLOVER -> f.relic() ? Denial.NONE : Denial.NO_RELIC;
            case CHAINSAW_MAN -> Denial.NONE;
            case BLACK_BUTLER -> f.healthShare() <= BUTLER_HEALTH ? Denial.NONE : Denial.NOT_DESPAIR;
            case BLUE_EXORCIST -> !f.paper() ? Denial.NO_PAPER : f.health() <= EXORCIST_BLOOD + 2f ? Denial.NO_BLOOD : Denial.NONE;
            case TENSURA -> f.flesh() < OFFERING_FLESH ? Denial.NO_OFFERING : f.magiculeShare() < TENSURA_NAMING ? Denial.NO_MAGICULES : Denial.NONE;
            case DORORO -> f.openSky() ? Denial.OPEN_SKY : Denial.NONE;
            case BERSERK -> f.healthShare() > BERSERK_HEALTH ? Denial.NOT_LOWEST : f.levels() < BERSERK_LEVELS ? Denial.NO_LEVELS : Denial.NONE;
            case FAIRY_TAIL -> !f.book() ? Denial.NO_BOOK : f.magiculeShare() < ZEREF_ETHER ? Denial.NO_MAGICULES : Denial.NONE;
        };
    }

    /** Why a running ritual breaks this tick, or null: the caller left the circle, or (Blue Exorcist) the will broke. */
    public static String broken(DevilWorld w, double distance, double healthShare) {
        if (distance > CIRCLE) return "You left the circle. The ritual breaks.";
        if (w == DevilWorld.BLUE_EXORCIST && healthShare < EXORCIST_WILL) return "Your will breaks. The demon turns on you.";
        return null;
    }

    /** Black Clover: what one blow of the devil takes: a share of the maximum health, but never the last heart. */
    public static float bindingBlow(float health, float maxHealth) {
        float want = (float) (Math.max(0f, maxHealth) * BINDING_BLOW_SHARE);
        return Math.max(0f, Math.min(want, health - 2f));
    }

    /** True at the ticks the Black Clover devil strikes. */
    public static boolean blowAt(int tick) {
        for (int b : BINDING_BLOWS) if (b == tick) return true;
        return false;
    }

    /** Is an offer made at {@code at} still open at {@code now}? */
    public static boolean offerOpen(long at, long now) { return at > 0 && now >= at && now - at <= OFFER_TICKS; }
}
