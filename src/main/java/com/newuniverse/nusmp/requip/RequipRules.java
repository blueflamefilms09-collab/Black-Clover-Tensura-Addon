package com.newuniverse.nusmp.requip;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: every number that decides whether Requip: The Knight is fair and consistent (who may learn it, which armours are open, what an armour adds, what it takes off a hit, what the swap takes
 * and how long it lasts) is arithmetic that must not be checked by hand in game. This class is that arithmetic with no Minecraft classes in it; {@code skill.ErzaRequipSkill} and
 * {@code skill.ErzaRequipFx} apply the results, {@code ArmourTableTest} pins them.
 */
public final class RequipRules {
    private RequipRules() {}

    // ------------------------------------------------------------ modes
    /** One mode per armour (the table's order), then Dismiss. */
    public static final int ARMOURS = Armour.values().length;
    public static final int DISMISS_MODE = ARMOURS;
    public static final int MODES = ARMOURS + 1;

    // ------------------------------------------------------------ the equip animation (the Djinn full equip, client.aura.EquipAnimation)
    /** Ticks an armour takes to shatter out when it is dismissed or swapped: EXACTLY the shatter window of the Djinn full equip, {@code EquipAnimation.SHATTER_WINDOW} (12 ticks to shatter plus 6 of stagger).
     *  Written as a number so this class stays free of client classes; {@code RequipAnimationTest} fails the build if the two ever differ. */
    public static final int SHATTER_WINDOW = 18;
    /** Ticks a fresh armour needs to be fully on: the last piece starts at 12 and forges for 8 ({@code EquipAnimation.FORGE_TICKS}). */
    public static final int FORGE_TICKS = 20;
    /** The old armour finishes shattering, then the new one starts to forge: the gap in ticks between the two (the swap is "a short animation"). */
    public static final int SWAP_GAP = SHATTER_WINDOW;
    /** How long a render aura is set for; the server refreshes it every {@link #AURA_REFRESH} ticks while the armour is on (a refresh keeps the pieces assembled). */
    public static final int AURA_TICKS = 1200;
    public static final int AURA_REFRESH = 400;
    /** Cooldown (ticks) of a swap, on the mode of the armour that was put on. */
    public static final int SWAP_COOLDOWN = 60;

    // ------------------------------------------------------------ who may learn it
    /** Max EP that is enough on its own for the book. */
    public static final double REQUIRED_EP = 100_000;
    /** Max EP that is enough together with one mastered Unique skill. */
    public static final double REQUIRED_EP_WITH_UNIQUE = 25_000;

    public static boolean eligible(double maxEp, int masteredUniqueSkills) {
        return maxEp >= REQUIRED_EP || (maxEp >= REQUIRED_EP_WITH_UNIQUE && masteredUniqueSkills > 0);
    }

    /** True when the armour can be put on: unlocked by mastery, or the wearer is an operator / in creative. */
    public static boolean unlocked(Armour a, double masteryFrac, boolean privileged) {
        return privileged || masteryFrac + 1e-9 >= a.unlock();
    }

    // ------------------------------------------------------------ costs
    public static final double EQUIP_FLOOR = 40, UPKEEP_FLOOR = 4, ATTACK_FLOOR = 30;

    /** The equip cost is a share of max Aura (else Magicule); swapping from another armour costs 60 percent of it (the old armour's magic is reused). */
    public static double equipShare(Armour a, boolean swapping) { return a.cost().equip() * (swapping ? 0.6 : 1.0); }

    // ------------------------------------------------------------ what an armour adds
    public enum Op { ADD_VALUE, ADD_MULTIPLIED_BASE }

    /** One attribute modifier: {@code attribute} is the vanilla attribute's path ({@code movement_speed}, {@code attack_damage}, ...). */
    public record Mod(String attribute, Op op, double amount) {
        public String modifierPath() { return "erza_requip_" + attribute; }
    }

    /** The attribute modifiers the armour puts on its wearer; mastery (0..1) strengthens the attack and defence ones by up to 25 percent. Zero entries are left out. */
    public static List<Mod> modifiers(Armour a, double masteryFrac) { return modifiers(a, masteryFrac, 1.0); }

    /** The same for a partial armour (Equip Magic): every amount is the share {@code fraction} (0..1) of the whole armour's. A whole armour is the plain overload. */
    public static List<Mod> modifiers(Armour a, double masteryFrac, double fraction) {
        double k = 1.0 + 0.25 * Math.max(0.0, Math.min(1.0, masteryFrac));
        double f = Double.isNaN(fraction) ? 1.0 : Math.max(0.0, Math.min(1.0, fraction));
        Armour.Stats s = a.stats();
        List<Mod> l = new ArrayList<>();
        add(l, "movement_speed", Op.ADD_MULTIPLIED_BASE, s.speed() * f);
        add(l, "attack_damage", Op.ADD_VALUE, s.attack() * k * f);
        add(l, "armor", Op.ADD_VALUE, s.armor() * k * f);
        add(l, "armor_toughness", Op.ADD_VALUE, s.toughness() * k * f);
        add(l, "attack_speed", Op.ADD_MULTIPLIED_BASE, s.attackSpeed() * f);
        add(l, "knockback_resistance", Op.ADD_VALUE, s.knockbackResistance() * f);
        return l;
    }

    private static void add(List<Mod> l, String attr, Op op, double amount) {
        if (Math.abs(amount) > 1e-9) l.add(new Mod(attr, op, amount));
    }

    /** Every attribute path any armour may set (so the skill can clear all of them on a swap, whatever was worn). */
    public static final List<String> ATTRIBUTES = List.of("movement_speed", "attack_damage", "armor", "armor_toughness", "attack_speed", "knockback_resistance");

    // ------------------------------------------------------------ what an armour takes off a hit
    /** The most any armour (with its guard stance) can take off a hit. */
    public static final double MAX_REDUCTION = 0.90;
    /** While Reflect Stance / Kreuz Ward is up, every class is reduced by this much more. */
    public static final double GUARD_BONUS = 0.30;

    /** The damage left of {@code amount} after the armour's resistance to {@code c} (and the guard stance's bonus). */
    public static float afterResist(Armour a, Armour.DamageClass c, float amount, boolean guard) { return afterResist(a, c, amount, guard, 1.0); }

    /** The same for a partial armour: the resistance is the share {@code fraction} of the whole armour's; the guard stance's bonus needs at least a quarter of the armour on. */
    public static float afterResist(Armour a, Armour.DamageClass c, float amount, boolean guard, double fraction) {
        double f = Double.isNaN(fraction) ? 1.0 : Math.max(0.0, Math.min(1.0, fraction));
        double r = Math.min(MAX_REDUCTION, a.resist().of(c) * f + (guard && f >= 0.25 ? GUARD_BONUS : 0.0));
        return (float) Math.max(0.0, amount * (1.0 - r));
    }

    /** The share of a melee hit that the Adamantine plate throws back at the attacker: passively 25 percent, 70 percent in Reflect Stance. Other armours reflect nothing. */
    public static double reflect(Armour a, boolean guard) {
        if (a != Armour.ADAMANTINE) return 0.0;
        return guard ? 0.70 : 0.25;
    }

    /** The guard stance (Kreuz Ward, Reflect Stance) lasts this long, in ticks. */
    public static int guardTicks(Armour a) { return a == Armour.ADAMANTINE ? 120 : 160; }

    // ------------------------------------------------------------ the special attacks
    /** Hits that a VOLLEY of {@code swords} swords deals to {@code targets} targets: the swords are shared out, the first targets get the spare ones. Returns the swords per target. */
    public static int[] shareSwords(int swords, int targets) {
        if (targets <= 0 || swords <= 0) return new int[0];
        int[] out = new int[targets];
        for (int i = 0; i < swords; i++) out[i % targets]++;
        return out;
    }

    /** A target count for a volley: as many foes as there are, at most {@code max}, never more than the swords. */
    public static int volleyTargets(int foes, int swords, int max) { return Math.max(0, Math.min(Math.min(foes, swords), max)); }

    /** What a self-damaging attack takes from the wearer: a share of the current health, never leaving less than 1 HP. */
    public static float selfDamage(float health, double share) {
        if (share <= 0 || health <= 1f) return 0f;
        return (float) Math.min(health - 1f, health * share);
    }

    /** Extra damage factor of Sea Empress's Whirlpool and Tidal Wave while the wearer stands in water (the armour's underwater bonus). */
    public static double waterBonus(Armour a, boolean inWater) { return a == Armour.SEA_EMPRESS && inWater ? 1.4 : 1.0; }

    /** Nakagami's bonus against casters: the share of extra damage per hundred thousand of the target's max EP, capped at +60 percent. */
    public static double antiCasterBonus(double targetMaxEp) { return Math.min(0.60, Math.max(0.0, targetMaxEp) / 100_000.0 * 0.15); }

    /** The weapon requipped with the armour is marked with this custom-data flag so it is removed with the armour. */
    public static final String WEAPON_FLAG = "ErzaRequip";
}
