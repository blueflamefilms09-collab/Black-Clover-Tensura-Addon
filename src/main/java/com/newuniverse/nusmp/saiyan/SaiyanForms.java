package com.newuniverse.nusmp.saiyan;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Why: the transformation roster of the Saiyan system (3.65.0, Stage 4) as pure data, so the engine, the commands, the config, the tests and the CI self-test all read one table and a new form is a new
 * row, not new code. The roster is the owner's chart "All Saiyan Forms of Goku" WITHOUT the two ape forms (owner: "don't do the ape's"): Base, Kaio-ken, Super Saiyan, Super Saiyan Full Power, Super
 * Saiyan 2, Super Saiyan 3, False Super Saiyan, Super Saiyan 4, Super Full Power Super Saiyan 4, Super Saiyan 4 Limit Breaker, Super Saiyan God, Super Saiyan Blue, Perfected Super Saiyan Blue, Super
 * Saiyan Blue Kaio-Ken, Ultra Instinct Sign, Perfected Ultra Instinct (the spec's Mastered Ultra Instinct) and True Ultra Instinct. The numbers start from the spec's table (damage, defence, Ki drain per
 * second) with the drain read as a share of the maximum Ki; they are starting balance, every one of them can be set by the server. No Minecraft type: the unlock rules ({@link #missing}), the mastery
 * efficiency and the Kaio-ken strain are plain arithmetic.
 */
public final class SaiyanForms {
    private SaiyanForms() {}

    public enum Branch { BASE, KAIO, SUPER, PRIMAL, GOD, INSTINCT }

    /** The look of a form's aura (the glue maps it to particles; none of this is a model). */
    public enum Aura { NONE, RED, GOLD, ELECTRIC, BLUE, DIVINE, SILVER }

    /** The id of the form a Saiyan is in when in none. */
    public static final String BASE = "base";

    /**
     * One form. {@code parent}/{@code parent2}: forms that must be mastered to the share {@code parentMastery} first. {@code minBp}: Battle Power. {@code breakthroughs}: Zenkai received (the emotional
     * breakthrough). {@code flag}: a special unlock an operator grants or a ritual earns (null = none). {@code trainCat}/{@code trainNeed}: training points of one kind (null = none). {@code goku}: only a
     * Saiyan who has awakened the Goku Ultimate may use it. {@code kiCost}: percent of the maximum Ki to enter; {@code drain}: percent of the maximum Ki per second; {@code staminaDrain}: stamina points
     * per second. {@code damage}/{@code defense}: bonus fractions; {@code speed}/{@code attackSpeed}: movement and attack-speed fractions (negative = slower). {@code cooldown}: ticks before another form.
     * {@code masterySeconds}: seconds of real fighting that master it.
     */
    public record Form(String id, Branch branch, boolean goku, String parent, double parentMastery, String parent2, double minBp, int breakthroughs, String flag,
                       SaiyanTraining.Cat trainCat, double trainNeed, double kiCost, double drain, double staminaDrain, double damage, double defense, double speed, double attackSpeed,
                       int cooldown, int masterySeconds, Aura aura, int rgb) {
        public String key() { return "nusmp.saiyan.form." + id; }
    }

    private static Form f(String id, Branch br, boolean goku, String parent, double pm, String parent2, double minBp, int bt, String flag, SaiyanTraining.Cat tc, double tn,
                          double ki, double drain, double st, double dmg, double def, double spd, double aspd, int cd, int mastery, Aura aura, int rgb) {
        return new Form(id, br, goku, parent, pm, parent2, minBp, bt, flag, tc, tn, ki, drain, st, dmg, def, spd, aspd, cd, mastery, aura, rgb);
    }

    private static final SaiyanTraining.Cat PHY = SaiyanTraining.Cat.PHYSICAL, KI = SaiyanTraining.Cat.KI, MAR = SaiyanTraining.Cat.MARTIAL, DIV = SaiyanTraining.Cat.DIVINE;

    public static final Form KAIOKEN = f("kaioken", Branch.KAIO, false, null, 0, null, 3_000, 0, null, MAR, 40, 3, 0.8, 4, 0.40, 0.00, 0.20, 0.10, 200, 900, Aura.RED, 0xFFE03020);
    public static final Form FALSE_SUPER_SAIYAN = f("false_super_saiyan", Branch.SUPER, false, null, 0, null, 12_000, 0, null, KI, 40, 4, 0.8, 3, 0.55, 0.10, 0.05, 0.00, 300, 600, Aura.GOLD, 0xFFE0B040);
    public static final Form SUPER_SAIYAN = f("super_saiyan", Branch.SUPER, false, null, 0, null, 20_000, 1, null, PHY, 60, 5, 0.4, 2, 0.75, 0.35, 0.15, 0.05, 300, 1200, Aura.GOLD, 0xFFFFD020);
    public static final Form SUPER_SAIYAN_FULL_POWER = f("super_saiyan_full_power", Branch.SUPER, false, "super_saiyan", 0.6, null, 40_000, 1, null, null, 0, 5, 0.2, 1, 0.85, 0.40, 0.15, 0.05, 200, 1200, Aura.GOLD, 0xFFFFE060);
    public static final Form SUPER_SAIYAN_2 = f("super_saiyan_2", Branch.SUPER, false, "super_saiyan", 0.5, null, 80_000, 2, null, KI, 120, 7, 0.7, 3, 1.10, 0.45, 0.22, 0.10, 400, 1800, Aura.ELECTRIC, 0xFFFFE840);
    public static final Form SUPER_SAIYAN_3 = f("super_saiyan_3", Branch.SUPER, false, "super_saiyan_2", 0.5, null, 400_000, 3, null, KI, 250, 10, 1.6, 5, 1.60, 0.55, -0.10, -0.15, 600, 2400, Aura.ELECTRIC, 0xFFFFC820);
    public static final Form SUPER_SAIYAN_4 = f("super_saiyan_4", Branch.PRIMAL, true, "super_saiyan_2", 0.7, null, 1_000_000, 4, null, PHY, 400, 10, 1.0, 4, 1.80, 0.70, 0.20, 0.10, 600, 3000, Aura.RED, 0xFFC02020);
    public static final Form SUPER_FULL_POWER_SUPER_SAIYAN_4 = f("super_full_power_super_saiyan_4", Branch.PRIMAL, true, "super_saiyan_4", 0.6, null, 3_000_000, 5, null, null, 0, 12, 1.2, 4, 2.10, 0.78, 0.22, 0.12, 600, 3600, Aura.RED, 0xFFE03030);
    public static final Form SUPER_SAIYAN_4_LIMIT_BREAKER = f("super_saiyan_4_limit_breaker", Branch.PRIMAL, true, "super_full_power_super_saiyan_4", 0.8, null, 8_000_000, 6, "limit_breaker", null, 0, 25, 2.0, 8, 2.50, 0.92, 0.25, 0.15, 1200, 4800, Aura.DIVINE, 0xFFFF4080);
    public static final Form SUPER_SAIYAN_GOD = f("super_saiyan_god", Branch.GOD, true, null, 0, null, 1_000_000, 4, null, DIV, 20, 12, 0.7, 3, 1.90, 0.80, 0.18, 0.10, 600, 3000, Aura.DIVINE, 0xFFFF3030);
    public static final Form SUPER_SAIYAN_BLUE = f("super_saiyan_blue", Branch.GOD, true, "super_saiyan_god", 0.6, null, 2_000_000, 5, null, DIV, 40, 15, 1.2, 4, 2.30, 0.90, 0.24, 0.12, 600, 3600, Aura.BLUE, 0xFF40B0FF);
    public static final Form PERFECTED_SUPER_SAIYAN_BLUE = f("perfected_super_saiyan_blue", Branch.GOD, true, "super_saiyan_blue", 0.8, null, 6_000_000, 6, null, null, 0, 15, 0.9, 3, 2.50, 0.95, 0.26, 0.14, 600, 4200, Aura.BLUE, 0xFF80D0FF);
    public static final Form SUPER_SAIYAN_BLUE_KAIOKEN = f("super_saiyan_blue_kaioken", Branch.GOD, true, "super_saiyan_blue", 0.6, "kaioken", 4_000_000, 5, null, MAR, 400, 25, 2.8, 12, 3.00, 0.90, 0.30, 0.18, 1200, 4800, Aura.BLUE, 0xFFFF4060);
    public static final Form ULTRA_INSTINCT_SIGN = f("ultra_instinct_sign", Branch.INSTINCT, true, "super_saiyan_blue", 0.5, null, 5_000_000, 6, null, MAR, 300, 18, 1.0, 4, 1.50, 0.50, 0.20, 0.20, 600, 4800, Aura.SILVER, 0xFFB0C8FF);
    public static final Form PERFECTED_ULTRA_INSTINCT = f("perfected_ultra_instinct", Branch.INSTINCT, true, "ultra_instinct_sign", 1.0, null, 8_000_000, 7, null, DIV, 60, 25, 2.0, 5, 2.40, 1.00, 0.25, 0.25, 900, 6000, Aura.SILVER, 0xFFE8F0FF);
    public static final Form TRUE_ULTRA_INSTINCT = f("true_ultra_instinct", Branch.INSTINCT, true, "perfected_ultra_instinct", 1.0, null, 10_000_000, 8, null, MAR, 800, 30, 2.5, 6, 2.60, 1.05, 0.28, 0.28, 1200, 7200, Aura.SILVER, 0xFFFFFFFF);

    /** Every form that can be entered (Base is the absence of a form). The id of each is a save key: never renamed; new forms are appended. */
    public static final List<Form> ALL = List.of(KAIOKEN, FALSE_SUPER_SAIYAN, SUPER_SAIYAN, SUPER_SAIYAN_FULL_POWER, SUPER_SAIYAN_2, SUPER_SAIYAN_3, SUPER_SAIYAN_4,
            SUPER_FULL_POWER_SUPER_SAIYAN_4, SUPER_SAIYAN_4_LIMIT_BREAKER, SUPER_SAIYAN_GOD, SUPER_SAIYAN_BLUE, PERFECTED_SUPER_SAIYAN_BLUE, SUPER_SAIYAN_BLUE_KAIOKEN,
            ULTRA_INSTINCT_SIGN, PERFECTED_ULTRA_INSTINCT, TRUE_ULTRA_INSTINCT);

    /** The most forms a save may hold unlocked (the bound of the stored set). */
    public static final int MAX_UNLOCKED = 32;

    public static Form byId(String id) {
        if (id != null) for (Form x : ALL) if (x.id().equals(id)) return x;
        return null;
    }

    /** The index of a form in {@link #ALL} (+1; 0 = Base) for the packet. */
    public static int code(String id) {
        for (int i = 0; i < ALL.size(); i++) if (ALL.get(i).id().equals(id)) return i + 1;
        return 0;
    }

    public static Form ofCode(int code) { return code >= 1 && code <= ALL.size() ? ALL.get(code - 1) : null; }

    // ------------------------------------------------------------------ the unlock rules
    /** What a Saiyan has done, as the rules read it. {@code mastery} is 0..1 per form id. */
    public record Progress(double bp, int breakthroughs, boolean goku, Set<String> flags, Map<String, Double> training, Map<String, Double> mastery) {
        public double train(SaiyanTraining.Cat c) { return training.getOrDefault(c.id(), 0.0); }
        public double mastered(String id) { return id == null ? 1.0 : mastery.getOrDefault(id, 0.0); }
    }

    /** What is still missing for this form, in words (empty = the form can be unlocked). {@code minBp} is the server's Battle Power requirement for it. */
    public static List<String> missing(Form f, double minBp, Progress p) {
        List<String> out = new ArrayList<>();
        if (f.goku() && !p.goku()) out.add("the Son Goku Ultimate Skill");
        if (p.bp() < minBp) out.add("Battle Power " + Math.round(minBp));
        if (p.breakthroughs() < f.breakthroughs()) out.add(f.breakthroughs() + " Zenkai breakthroughs (you have " + p.breakthroughs() + ")");
        if (f.trainCat() != null && p.train(f.trainCat()) < f.trainNeed()) out.add(Math.round(f.trainNeed()) + " " + f.trainCat().id() + " training");
        if (f.parent() != null && p.mastered(f.parent()) < f.parentMastery()) out.add(pct(f.parentMastery()) + "% mastery of " + f.parent());
        if (f.parent2() != null && p.mastered(f.parent2()) < f.parentMastery()) out.add(pct(f.parentMastery()) + "% mastery of " + f.parent2());
        if (f.flag() != null && !p.flags().contains(f.flag())) out.add("the " + f.flag() + " unlock");
        return out;
    }

    public static boolean meets(Form f, double minBp, Progress p) { return missing(f, minBp, p).isEmpty(); }

    private static long pct(double v) { return Math.round(v * 100.0); }

    // ------------------------------------------------------------------ mastery
    /** How strongly a form works at this mastery: 85% when new, 100% when mastered. */
    public static double effect(double mastery) { return 0.85 + 0.15 * clamp01(mastery); }

    /** The share of the Ki drain left at this mastery: a mastered form costs 60% of a new one. */
    public static double drainFactor(double mastery) { return 1.0 - 0.4 * clamp01(mastery); }

    /** Mastery gained from {@code seconds} of real fighting. */
    public static double masteryGain(double seconds, int masterySeconds) { return masterySeconds <= 0 ? 1.0 : Math.max(0.0, seconds) / masterySeconds; }

    private static double clamp01(double v) { return Double.isNaN(v) ? 0.0 : Math.max(0.0, Math.min(1.0, v)); }

    // ------------------------------------------------------------------ Kaio-ken: stages and strain
    /** The highest Kaio-ken stage a Saiyan can enter: x2 at once, x3 from a third mastery, x4 from two thirds. */
    public static int kaioMaxStage(double mastery) { return 1 + (mastery >= 0.34 ? 1 : 0) + (mastery >= 0.67 ? 1 : 0); }

    public static double kaioDamageMul(int stage) { return new double[] {1.0, 1.5, 2.0}[clampStage(stage) - 1]; }

    public static double kaioDrainMul(int stage) { return new double[] {1.0, 1.6, 2.4}[clampStage(stage) - 1]; }

    /** Strain points a second (0..100 fills the bar): a hundred seconds at x2, about twenty-five at x4. */
    public static double kaioStrainRate(int stage) { return new double[] {1.0, 2.2, 4.0}[clampStage(stage) - 1]; }

    private static int clampStage(int s) { return Math.max(1, Math.min(3, s)); }

    /** The strain after {@code seconds}: it rises while Kaio-ken is on and falls (two points a second) while it is off. */
    public static double strainAfter(double strain, boolean active, int stage, double seconds) {
        double v = active ? strain + kaioStrainRate(stage) * seconds : strain - 2.0 * seconds;
        return Math.max(0.0, Math.min(100.0, v));
    }

    /** The share of the maximum stamina the strain takes away (up to 40% at full strain). */
    public static double strainPenalty(double strain) { return 0.4 * Math.max(0.0, Math.min(100.0, strain)) / 100.0; }

    // ------------------------------------------------------------------ Ultra Instinct: automatic dodges with limits
    /** A form's automatic dodge: the chance, the ticks before the next, the stamina it costs, how many fit in a window, the window, and the bonus of the counter it opens (0 = no counter window). */
    public record Dodge(double chance, int cooldown, double stamina, int perWindow, int window, double counter, double flowPerStack) {}

    /** Ultra Instinct Sign has three awakenings (level 1..3 from the mastery): more chance and, from the second, a counter window. */
    public static int uiLevel(double mastery) { return 1 + (mastery >= 1.0 / 3.0 ? 1 : 0) + (mastery >= 2.0 / 3.0 ? 1 : 0); }

    /** The automatic dodge of a form at this mastery, or null for a form that has none (the racial Warrior's Reflex still applies). */
    public static Dodge dodge(String id, double mastery) {
        if (id == null) return null;
        return switch (id) {
            case "ultra_instinct_sign" -> {
                int lv = uiLevel(mastery);
                yield new Dodge(0.10 + 0.10 * lv, 40, 10.0, 3, 400, lv >= 2 ? 0.25 + 0.05 * lv : 0.0, 0.0);
            }
            case "perfected_ultra_instinct" -> new Dodge(0.60, 15, 8.0, 5, 600, 0.50, 0.0);
            case "true_ultra_instinct" -> new Dodge(0.65, 10, 6.0, 6, 600, 0.60, 0.04);
            default -> null;
        };
    }

    /** The registry as a map id -> form (the self-test and the commands read it). */
    public static Map<String, Form> byIdMap() {
        Map<String, Form> m = new LinkedHashMap<>();
        for (Form x : ALL) m.put(x.id(), x);
        return m;
    }

    /** Lower-case id for a command argument. */
    public static String norm(String s) { return s == null ? "" : s.toLowerCase(Locale.ROOT).trim().replace(' ', '_').replace('-', '_'); }
}
