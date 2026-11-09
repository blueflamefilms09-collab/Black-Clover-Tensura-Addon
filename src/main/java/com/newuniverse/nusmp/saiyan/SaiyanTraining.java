package com.newuniverse.nusmp.saiyan;

import java.util.List;
import java.util.Locale;

/**
 * Why: the training and technique-mastery numbers of the Saiyan system (3.65.0, Stage 2), as plain arithmetic with no Minecraft type so they are unit tested. Two things live here. TRAINING: four
 * categories of points (physical, Ki, martial, divine) that real fighting earns under a per-day cap, which the Goku awakening and the forms ask for. MASTERY: how many uses make a universal Saiyan technique
 * "mastered" (the Goku awakening asks for five of them). Nothing here reads the game; {@link SaiyanState} stores the numbers and {@link SaiyanManager} feeds them.
 */
public final class SaiyanTraining {
    private SaiyanTraining() {}

    /** The four kinds of training. The id of each is a save key: never renamed. */
    public enum Cat {
        PHYSICAL, KI, MARTIAL, DIVINE;

        public String id() { return name().toLowerCase(Locale.ROOT); }

        public static Cat byId(String id) {
            if (id != null) for (Cat c : values()) if (c.id().equals(id)) return c;
            return null;
        }
    }

    /** Most points of each category one game day can give (a floor against idle and weak-mob farming; the divine one is a boss-kill count). */
    public static double dailyCap(Cat c) {
        return switch (c) {
            case PHYSICAL -> 60.0;
            case KI -> 60.0;
            case MARTIAL -> 60.0;
            case DIVINE -> 3.0;
        };
    }

    /** What one hit on a worthy foe teaches (before the difficulty weight): hits are small, kills and boss kills are bigger. */
    public static double perHit(Cat c) {
        return switch (c) {
            case PHYSICAL -> 0.25;
            case KI -> 0.30;
            case MARTIAL -> 0.40;
            case DIVINE -> 0.0;
        };
    }

    /** Points still allowed today: the cap minus what was gained. */
    public static double room(Cat c, double gainedToday, double scale) { return Math.max(0.0, dailyCap(c) * Math.max(0.0, scale) - Math.max(0.0, gainedToday)); }

    /** The universal techniques every Saiyan can learn, in the order of the mastery list. The id of each is a save key: never renamed. */
    public static final List<String> UNIVERSAL = List.of("blast", "barrage", "flight", "sense", "suppress");
    /** The Goku techniques (counted for their own mastery, not for the "five universal" requirement). */
    public static final List<String> GOKU = List.of("kame", "combo");

    /** Uses (hits for an attack, seconds for flight and suppression, scans for the senses) that master a technique. */
    public static int masteryNeed(String tech) {
        return switch (tech == null ? "" : tech) {
            case "blast" -> 200;
            case "barrage" -> 100;
            case "flight" -> 3600;
            case "sense" -> 100;
            case "suppress" -> 600;
            case "kame" -> 120;
            case "combo" -> 200;
            default -> 100;
        };
    }

    /** 0..1: how far a technique is mastered. */
    public static double mastery01(String tech, int uses) {
        int need = masteryNeed(tech);
        return need <= 0 ? 1.0 : Math.max(0.0, Math.min(1.0, uses / (double) need));
    }

    /** True when the technique is fully mastered. */
    public static boolean mastered(String tech, int uses) { return uses >= masteryNeed(tech); }
}
