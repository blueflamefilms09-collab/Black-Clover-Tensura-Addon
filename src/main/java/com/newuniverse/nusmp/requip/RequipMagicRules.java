package com.newuniverse.nusmp.requip;

import java.util.List;

/**
 * Why: Requip: The Knight gave the Equip Magic its armours. This is the rest of the magic as Fairy Tail shows it: the pocket space (the Armoury), the instant swap (Loadouts), the weapon on call
 * (Requip: Blade, Barrage, Strike), the armour worn piece by piece (Partial Armour) and Erza's named finishers. The modes are appended after Dismiss, so no saved mode index moves. This class holds
 * the table (mode order, ids, what unlocks each mode, what it costs and how long it rests) and the passive rules (Instant-Change, Pocket Capacity, Weapon Affinity, Requip Reflex) with no Minecraft
 * classes; {@code skill.ErzaRequipMagic} runs them and {@code RequipMagicRulesTest} pins the order.
 */
public final class RequipMagicRules {
    private RequipMagicRules() {}

    /** The modes the skill had before the magic was added (the 14 armours and Dismiss): they keep their indices. */
    public static final int BASE_MODES = RequipRules.MODES;

    /**
     * The appended modes, in order (append only: the ordinal plus {@link #BASE_MODES} is the saved mode index).
     * {@code canon} is true when the name is the technique's name in the manga or anime; false names are descriptive or the owner's own (see docs/requip_magic.md).
     */
    public enum Mode {
        ARMOURY("armoury", "Armoury", 0.0, 0, 0.0, 0, 0.0, 0.0, false),
        LOADOUTS("loadouts", "Loadouts", 0.10, 0, 1.5, 0, 0.0, 0.0, false),
        BLADE("blade", "Requip: Blade", 0.05, 0, 0.5, 10, 0.0, 0.0, false),
        BARRAGE("barrage", "Requip: Barrage", 0.20, 50_000, 6.0, 240, 0.0, 40.0, false),
        STRIKE("strike", "Requip: Strike", 0.15, 0, 3.0, 100, 0.0, 22.0, false),
        PARTIAL("partial", "Partial Armour", 0.30, 0, 0.0, 20, 0.0, 0.0, false),
        CIRCLE_SWORD("circle_sword", "Circle Sword", 0.25, 50_000, 8.0, 280, 9.0, 26.0, true),
        BLUMENBLATT("blumenblatt", "Blumenblatt", 0.35, 75_000, 10.0, 340, 14.0, 9.0, true),
        TITANIAS_RAMPAGE("titanias_rampage", "Titania's Rampage", 0.45, 100_000, 9.0, 300, 15.0, 18.0, false),
        DANCE_OF_SWORDS("dance_of_swords", "Dance of the Swords", 0.55, 150_000, 10.0, 400, 6.0, 3.6, false),
        SEA_OF_SWORDS("sea_of_swords", "Sea of Swords", 0.70, 250_000, 14.0, 600, 16.0, 11.0, false),
        MOONLIGHT_CROSS("moonlight_cross", "Moonlight Cross", 0.60, 200_000, 9.0, 260, 24.0, 15.0, false),
        DIMENSION_CUT("dimension_cut", "Dimension Cut", 0.50, 100_000, 7.0, 160, 0.0, 6.0, false);

        private final String id, english;
        private final double unlock, minEp, cost, raw, range;
        private final int cooldown;
        private final boolean canon;

        Mode(String id, String english, double unlock, double minEp, double cost, int cooldown, double raw, double range, boolean canon) {
            this.id = id; this.english = english; this.unlock = unlock; this.minEp = minEp; this.cost = cost; this.cooldown = cooldown; this.raw = raw; this.range = range; this.canon = canon;
        }

        /** The mode id the skill reports ({@code erza.<id>}). */
        public String id() { return id; }
        public String english() { return english; }
        /** Mastery (0..1) the mode needs. */
        public double unlock() { return unlock; }
        /** Max EP the mode needs. */
        public double minEp() { return minEp; }
        /** Cost: a share of the max Aura (else Magicule) in percent. */
        public double cost() { return cost; }
        /** The mode's own cooldown in ticks (0 = none). */
        public int cooldown() { return cooldown; }
        /** Raw damage per hit of a finisher (0 for the modes that take their damage from the weapon). */
        public double raw() { return raw; }
        public double range() { return range; }
        public boolean canon() { return canon; }
        public int modeIndex() { return BASE_MODES + ordinal(); }
        public boolean isFinisher() { return ordinal() >= CIRCLE_SWORD.ordinal(); }
    }

    public static final int MODES = BASE_MODES + Mode.values().length;

    /** The magic mode of a mode index, or null for the armours and Dismiss (and for an index out of range). */
    public static Mode byModeIndex(int index) {
        int i = index - BASE_MODES;
        return i >= 0 && i < Mode.values().length ? Mode.values()[i] : null;
    }

    public static Mode byId(String id) {
        for (Mode m : Mode.values()) if (m.id.equals(id)) return m;
        return null;
    }

    /** True when the mode is open: mastery and max EP enough, or the user is an operator or in creative. */
    public static boolean unlocked(Mode m, double mastery, double maxEp, boolean privileged) {
        return privileged || (mastery + 1e-9 >= m.unlock && maxEp >= m.minEp);
    }

    /** The modes that are finishers (the names of Erza's techniques and the Dimension Cut). */
    public static List<Mode> finishers() {
        return java.util.Arrays.stream(Mode.values()).filter(Mode::isFinisher).toList();
    }

    // ------------------------------------------------------------ Requip Reflex
    /** Mastery at which the Requip Reflex passive wakes. */
    public static final double REFLEX_AT = 0.40;
    /** The default pause between two reflexes in seconds (the server config can change it). */
    public static final int REFLEX_SECONDS = 20;

    /** True when a hit may trigger the reflex: it is on, mastery is enough, the pause has run out. */
    public static boolean reflexReady(boolean enabled, double mastery, long now, long lastAt, int pauseTicks) {
        return enabled && mastery + 1e-9 >= REFLEX_AT && now - lastAt >= Math.max(0, pauseTicks);
    }

    // ------------------------------------------------------------ the weapon on call
    /** An item counts as a weapon when its own attack damage (the player's 1 included) reaches this. */
    public static final double WEAPON_MIN_DAMAGE = 3.0;

    public static boolean isWeapon(double attackDamage) { return attackDamage + 1e-9 >= WEAPON_MIN_DAMAGE; }

    /** Requip: Strike keeps the thrown weapon out of the hand for at most this many ticks before it comes home on its own. */
    public static final int STRIKE_MAX_TICKS = 80;

    /** How far Requip: Strike throws: 14 blocks, up to 24 with mastery. */
    public static double strikeRange(double mastery) { return 14.0 + 10.0 * Math.max(0.0, Math.min(1.0, mastery)); }
}
