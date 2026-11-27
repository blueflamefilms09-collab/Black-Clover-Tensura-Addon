package com.newuniverse.nusmp.lords;

/**
 * Why: every number of the Lords of the Mortal Sins in one pure place (no game types), so the forms, the mastery curve, the burden (the drawback) and the Greed hunger can be unit-tested ({@code LordRulesTest}) and cannot
 * drift apart. The four weapons are the Black Axe of Sloth, the Black Gunblade of Envy, the Black Spear of Vanity and the holy sword Seifort ("Send Forth"); Greed (the Black Sword) has its own package but shares the
 * mastery curve here. Each weapon is ONE in the world (like the Black Sword Greed), has three forms (the old item is form 0, two new forms are appended), a named drawback (the Burden) that is bounded and curable, a
 * named mastery track that opens the forms and softens the drawback, and a Lord's decree (ultimate). Entries of {@link Sin} are only ever appended (the ordinal is saved).
 */
public final class LordRules {
    private LordRules() {}

    /** The five sins that have a mastery. The first four are weapons of this package; GREED is the Black Sword (its mastery is the Half-Starved Mastery). */
    public enum Sin {
        SLOTH("sloth", "Black Axe of Sloth", "lord_of_slothful_weight", 0x7A3FD1,
                new String[] {"black_axe_of_sloth", "black_axe_of_sloth_titan", "black_axe_of_sloth_chains"},
                new String[] {"Black Axe of Sloth", "Titan's Cleaver", "Chained Anchor"}, new String[] {"Dead Weight", "Titan's Verdict", "Drowsing Chains"},
                "Abyssal Stillness", "Weight of Sloth", "Mastery of Stillness"),
        ENVY("envy", "Black Gunblade of Envy", "lord_of_green_eyed_envy", 0x2FBF5A,
                new String[] {"black_gunblade_of_envy", "black_gunblade_of_envy_rifle", "black_gunblade_of_envy_coil"},
                new String[] {"Black Gunblade of Envy", "Coveted Rifle", "Serpent Coil"}, new String[] {"Green-Eyed Shot", "Jealous Volley", "Coveting Coil"},
                "Covet All", "Hollow of Envy", "Mastery of Covetousness"),
        VANITY("vanity", "Black Spear of Vanity", "lord_of_empty_vanity", 0xD64FB0,
                new String[] {"black_spear_of_vanity", "black_spear_of_vanity_mirror", "black_spear_of_vanity_peacock"},
                new String[] {"Black Spear of Vanity", "Mirror Glaive", "Peacock Halberd"}, new String[] {"Vain Thrust", "Reflected Gaze", "Fan of Vanity"},
                "Hall of Mirrors", "Cracked Mirror", "Mastery of Reflection"),
        HOLY("holy", "Seifort Holy Sword", "seifort_holy_judgement", 0xE8B98A,
                new String[] {"seifort_holy_sword", "seifort_holy_sword_radiant", "seifort_holy_sword_sent"},
                new String[] {"Seifort Holy Sword", "Seifort, Radiant", "Seifort, Sent Forth"}, new String[] {"Grand Cross", "Pillar of Judgement", "Send Forth"},
                "Holy Judgement", "Penance", "Mastery of Devotion"),
        GREED("greed", "Black Sword Greed", "lord_of_gluttonous_greed", 0xB3122B,
                new String[] {"black_sword_greed"}, new String[] {"Black Sword Greed"}, new String[] {"Secret Technique"},
                "Lord of Gluttonous Greed", "Hunger of the Blade", "Half-Starved Mastery");

        public final String id, english, skillId, ultimate, drawback, mastery;
        public final int rgb;
        public final String[] items, formNames, arts;

        Sin(String id, String english, String skillId, int rgb, String[] items, String[] formNames, String[] arts, String ultimate, String drawback, String mastery) {
            this.id = id; this.english = english; this.skillId = skillId; this.rgb = rgb; this.items = items; this.formNames = formNames; this.arts = arts;
            this.ultimate = ultimate; this.drawback = drawback; this.mastery = mastery;
        }

        /** The four weapons of this package (Greed is the fifth, with its own rules). */
        public boolean isLord() { return this != GREED; }

        public int forms() { return items.length; }

        public String baseItem() { return items[0]; }

        /** The lang key of the drawback's plain-words explanation, lines 1..4: what it takes, how much, how it recovers, how to avoid it. */
        public String drawbackKey(int line) { return "lords.drawback." + id + "." + line; }

        public String masteryKey() { return "lords.mastery." + id; }

        public String drawbackNameKey() { return "lords.burden." + id; }

        public String formKey(int form) { return "lords.form." + id + "." + form; }

        public String artKey(int form) { return "lords.art." + id + "." + form; }

        public String ultimateKey() { return "lords.ultimate." + id; }

        public static Sin byOrdinal(int i) { return i < 0 || i >= values().length ? SLOTH : values()[i]; }

        /** The weapon that owns this registry path (any form, old or new), or null. Greed's forms are named by {@code greed.GreedRules}. */
        public static Sin ofItem(String path) {
            if (path == null) return null;
            for (Sin s : values()) {
                if (!s.isLord()) continue;
                for (String i : s.items) if (i.equals(path)) return s;
            }
            return null;
        }

        /** The form number of a registry path in its weapon, or -1. */
        public int formOf(String path) {
            for (int i = 0; i < items.length; i++) if (items[i].equals(path)) return i;
            return -1;
        }
    }

    public static final int LORDS = 4;

    // ---------------------------------------------------------------- the mastery
    public static final int MAX_LEVEL = 10;

    /** The experience a track needs for level n (n = 1..10): 50 n^2, so the top costs 5000, about an hour and a half spent in the strained state. */
    public static int xpFor(int level) {
        int n = Math.max(0, Math.min(MAX_LEVEL, level));
        return 50 * n * n;
    }

    public static final int MAX_XP = 50 * MAX_LEVEL * MAX_LEVEL;

    public static int clampXp(long xp) { return (int) Math.max(0L, Math.min((long) MAX_XP, xp)); }

    /** The mastery level (0..10) of an experience total. */
    public static int level(int xp) {
        int l = 0;
        for (int n = 1; n <= MAX_LEVEL; n++) if (xp >= xpFor(n)) l = n;
        return l;
    }

    /** Progress to the next level in percent (0..99), 100 at the top. */
    public static int progress(int xp) {
        int l = level(xp);
        if (l >= MAX_LEVEL) return 100;
        int lo = xpFor(l), hi = xpFor(l + 1);
        return (int) Math.max(0, Math.min(99, (long) (xp - lo) * 100L / Math.max(1, hi - lo)));
    }

    /** Experience of one second spent in the strained state, and of a kill (more for a bigger victim, capped so a boss is a few minutes, not a level). */
    public static final int XP_PER_SECOND = 1;

    public static int xpForKill(double victimMaxHealth) {
        double h = Double.isNaN(victimMaxHealth) ? 0.0 : Math.max(0.0, Math.min(2000.0, victimMaxHealth));
        return (int) Math.max(3, Math.min(40, Math.round(3.0 + Math.sqrt(h) * 0.8)));
    }

    /** The forms open with the mastery: form 0 always, form 1 at level 3, form 2 at level 6; the Lord's decree (ultimate) at level 8. */
    public static final int FORM1_LEVEL = 3, FORM2_LEVEL = 6, ULTIMATE_LEVEL = 8;

    public static boolean formOpen(int form, int level) { return form <= 0 || (form == 1 && level >= FORM1_LEVEL) || (form >= 2 && level >= FORM2_LEVEL); }

    public static boolean ultimateOpen(int level) { return level >= ULTIMATE_LEVEL; }

    public static int formLevel(int form) { return form <= 0 ? 0 : form == 1 ? FORM1_LEVEL : FORM2_LEVEL; }

    /** The highest form the level opens (0..2). */
    public static int topForm(int level) { return level >= FORM2_LEVEL ? 2 : level >= FORM1_LEVEL ? 1 : 0; }

    // ---------------------------------------------------------------- the burden (the named drawback of a Lord weapon)
    public static final float BURDEN_MAX = 100.0f, STRAIN_AT = 50.0f, OVERLOAD_RESET = 60.0f;
    /** An overload lasts this many ticks (6 s): the weapon's arts are refused and the strong penalty applies; at its end the burden is {@link #OVERLOAD_RESET}. */
    public static final int OVERLOAD_TICKS = 120;
    /** The burden an art costs (right click of form 0, 1, 2) and the Lord's decree. */
    public static final float[] ART_BURDEN = {12.0f, 18.0f, 22.0f};
    public static final float ULTIMATE_BURDEN = 45.0f, HIT_BURDEN = 1.0f, KILL_RELIEF = 8.0f;
    /** The burden lost per second just by waiting; three times as fast while the weapon is not in hand. */
    public static final float DECAY_PER_SECOND = 1.5f, STOWED_FACTOR = 3.0f;

    /** Mastery softens the drawback: 4% less burden gained per level (40% at the top). */
    public static float gainFactor(int level) { return 1.0f - 0.04f * Math.max(0, Math.min(MAX_LEVEL, level)); }

    /** ... and 5% faster recovery per level (50% at the top). */
    public static float decayFactor(int level) { return 1.0f + 0.05f * Math.max(0, Math.min(MAX_LEVEL, level)); }

    public static float clampBurden(float b) { return Float.isNaN(b) ? 0.0f : Math.max(0.0f, Math.min(BURDEN_MAX, b)); }

    /** The burden after an art or a hit. */
    public static float gain(float burden, float cost, int level) { return clampBurden(burden + Math.max(0.0f, cost) * gainFactor(level)); }

    /** The burden after one second of waiting. */
    public static float decay(float burden, boolean held, int level) { return clampBurden(burden - DECAY_PER_SECOND * decayFactor(level) * (held ? 1.0f : STOWED_FACTOR)); }

    /** The burden after a kill (the weapon is fed). */
    public static float relief(float burden) { return clampBurden(burden - KILL_RELIEF); }

    public static boolean strained(float burden) { return burden >= STRAIN_AT; }

    public static boolean overloaded(float burden) { return burden >= BURDEN_MAX - 0.001f; }

    // ---------------------------------------------------------------- the Lord's decree (ultimate) and the arts
    public static final int ULTIMATE_COOLDOWN_TICKS = 2400, ART_MIN_GAP_TICKS = 20, FORM_SWITCH_GAP_TICKS = 40, MAX_TARGETS = 12, MAX_ART_TARGETS = 8;
    public static final long INACTIVITY_MS = 14L * 24L * 60L * 60L * 1000L;

    /** The item cooldown (ticks) of a form's art, by sin (SLOTH, ENVY, VANITY, HOLY) and form (0 is the old technique's own cooldown, listed for the tooltip only). */
    private static final int[][] ART_COOLDOWN = {{160, 140, 180}, {60, 70, 90}, {80, 100, 120}, {100, 120, 140}};

    public static int artCooldown(Sin s, int form) {
        if (!s.isLord()) return 60;
        return ART_COOLDOWN[s.ordinal()][Math.max(0, Math.min(2, form))];
    }

    /** The burden an art of a form costs: Penance is eased by smiting the undead (half), and nothing else changes it. */
    public static float artBurden(Sin s, int form, int undeadHit) {
        float c = ART_BURDEN[Math.max(0, Math.min(2, form))];
        return s == Sin.HOLY && undeadHit > 0 ? c * 0.5f : c;
    }

    /** A cooldown check that survives a clock set back: ready when now is past the stamp, or the stamp is further away than the cooldown itself. */
    public static boolean ready(long now, long stamp, int cooldown) { return now >= stamp || stamp - now > cooldown; }

    // ---------------------------------------------------------------- Greed: the Hunger of the Blade and the Half-Starved Mastery
    /** Below this satiation the blade is Ravenous (it drinks magicule and heals you less); at 0 it is Dormant (its devoured bonuses sleep until it is fed). It never damages the wielder and never touches the food bar. */
    public static final float RAVENOUS_BELOW = 20.0f;
    /** The magicule the ravenous blade drinks per second, in percent of the wielder's current magicule (bounded: 1.5% of what is there, so it can never empty a pool at once). */
    public static final double RAVENOUS_DRAIN_PERCENT = 1.5;

    /** The share of healing a ravenous wielder still receives: half, and Half-Starved Mastery gives back 4% per level (90% at the top). */
    public static float ravenousHeal(int halfLevel) { return 0.5f + 0.04f * Math.max(0, Math.min(MAX_LEVEL, halfLevel)); }

    /** The hunger multiplier of Half-Starvation: 2.5 times as fast, mastery trims 0.1 per level (1.5 at the top). */
    public static double halfHunger(int halfLevel) { return 2.5 - 0.1 * Math.max(0, Math.min(MAX_LEVEL, halfLevel)); }

    /** The stat multiplier of Half-Starvation: 1.5, mastery adds 1% per level (1.6 at the top). */
    public static double halfStats(int halfLevel) { return 1.5 + 0.01 * Math.max(0, Math.min(MAX_LEVEL, halfLevel)); }

    /** May Half-Starvation be switched now? Switching off is always allowed; switching on needs the skill's mastery (percent of its maximum) to reach {@code needPercent}; 0 = no gate. */
    public static boolean halfMayToggle(boolean currentlyOn, int masteryPercent, int needPercent) { return currentlyOn || needPercent <= 0 || masteryPercent >= needPercent; }

    /** The skill's mastery in percent of its maximum (0 for a bad maximum). */
    public static int masteryPercent(double mastery, double max) { return max <= 0.0 ? 0 : (int) Math.max(0, Math.min(100, Math.floor(mastery * 100.0 / max))); }

    /** The share of the devoured bonuses the blade lets the wielder use: all when fed, 70% while ravenous, none while dormant. */
    public static double bonusShare(float satiation) { return satiation <= 0.0f ? 0.0 : satiation < RAVENOUS_BELOW ? 0.7 : 1.0; }

    public static boolean ravenous(float satiation) { return satiation < RAVENOUS_BELOW; }

    public static boolean dormant(float satiation) { return satiation <= 0.0f; }
}
