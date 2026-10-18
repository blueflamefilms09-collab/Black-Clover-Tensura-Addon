package com.newuniverse.nusmp.skill.rules;

/**
 * Why: the four DanMachi god skills (Hestia, Freya, Hephaestus, Loki) are single-press divine powers whose strength is only "base or
 * mastered". Their radii, durations, heal shares and blink distances are collected here so the balance can be read and tested in one
 * place. Costs and cooldowns are in ticks / magicules before the pack-wide DanMachi multipliers.
 */
public final class GodRules {
    private GodRules() {}

    /** Hestia, Goddess of the Hearth: heal and bless. */
    public static final class Hestia {
        private Hestia() {}
        public static final double COST = 1500;
        public static final int COOLDOWN = 1200;
        public static double radius(Tier t) { return t == Tier.BASE ? 10 : 16; }
        public static double healShare(Tier t) { return t == Tier.BASE ? 0.35 : 0.6; }
        public static int regenAmp(Tier t) { return t == Tier.BASE ? 1 : 2; }
        public static int regenTicks() { return 200; }
        public static int absorbAmp(Tier t) { return t == Tier.BASE ? 1 : 3; }
        public static int absorbTicks() { return 1200; }
        /** A mastered Hearth also washes harmful effects away and wards off fire. */
        public static boolean cleanses(Tier t) { return t != Tier.BASE; }
        public static int fireWardTicks(Tier t) { return t == Tier.BASE ? 0 : 1200; }
    }

    /** Freya, Goddess of Beauty: Charm. */
    public static final class Freya {
        private Freya() {}
        public static final double COST = 1200;
        public static final int COOLDOWN = 900;
        public static double radius(Tier t) { return t == Tier.BASE ? 9 : 14; }
        /** Ticks the charmed are dazed (slow, weak). */
        public static int dazeTicks(Tier t) { return t == Tier.BASE ? 120 : 200; }
        /** Ticks charmed monsters will not pick the caster as a target again. */
        public static int charmTicks(Tier t) { return t == Tier.BASE ? 200 : 400; }
        public static int slowAmp() { return 3; }
        public static int weakAmp(Tier t) { return t == Tier.BASE ? 1 : 2; }
        /** Charmed monsters keep the lock for the caster only while still within this distance. */
        public static double keepDistance() { return 32.0; }
    }

    /** Hephaestus, Goddess of the Forge: repair. */
    public static final class Hephaestus {
        private Hephaestus() {}
        public static final double COST = 2000;
        public static final int COOLDOWN = 2400;
        /** Share of a worn-out item's lost durability restored (1.0 = fully repaired). */
        public static double repairShare(Tier t) { return 1.0; }
        public static boolean armor(Tier t) { return t != Tier.BASE; }
        public static boolean offhand(Tier t) { return t != Tier.BASE; }
        /** Durability points restored for an item with {@code damage} lost out of {@code max}. */
        public static int restored(int damage, int max, Tier t) {
            if (damage <= 0 || max <= 0) return 0;
            return Math.min(damage, (int) Math.ceil(damage * repairShare(t)));
        }
    }

    /** Loki, the Trickster: vanish and blink. */
    public static final class Loki {
        private Loki() {}
        public static final double COST = 800;
        public static final int COOLDOWN = 400;
        public static double blinkRange(Tier t) { return t == Tier.BASE ? 10 : 16; }
        public static int invisibleTicks(Tier t) { return t == Tier.BASE ? 100 : 200; }
        public static double minBlink() { return 2.0; }
        /** Monsters within this radius forget the caster as they vanish. */
        public static double forgetRadius(Tier t) { return t == Tier.BASE ? 12 : 20; }
    }
}
