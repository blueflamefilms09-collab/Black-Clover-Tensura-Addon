package com.newuniverse.nusmp.relics.wave;

import java.util.ArrayList;
import java.util.List;

/**
 * Wave 13 of the Relics of Many Worlds: the pure rules (no Minecraft imports) behind the nine blades of this wave. Tabletop becomes Minecraft
 * here: a "long rest" is a Minecraft day (24000 ticks), a die roll is a fixed average, and every counter lives in a {@link Bag} that the game side
 * backs with the stack's custom data.
 */
public final class Wave13Rules {
    private Wave13Rules() {}

    public static final long DAY = 24000L;

    /** A long-valued key/value store; an absent key reads as 0. */
    public interface Bag {
        long get(String key);

        void put(String key, long value);
    }

    // ---------------------------------------------------------------- uses per day (the tier-gated acts keep their own count)
    public static int usesLeft(Bag b, String key, long now, int perDay) {
        long day = now / DAY;
        return b.get("ud_" + key) != day + 1 ? perDay : (int) Math.max(0, perDay - b.get("uc_" + key));
    }

    public static boolean useUp(Bag b, String key, long now, int perDay) {
        if (usesLeft(b, key, now, perDay) <= 0) return false;
        long day = now / DAY;
        if (b.get("ud_" + key) != day + 1) {
            b.put("ud_" + key, day + 1);
            b.put("uc_" + key, 0);
        }
        b.put("uc_" + key, b.get("uc_" + key) + 1);
        return true;
    }

    // ---------------------------------------------------------------- Dream Glass: ethereal form ignores worn armour
    private static double absorbFraction(double damage, double armor, double toughness) {
        double f = 2.0 + toughness / 4.0;
        double g = Math.min(20.0, Math.max(armor - damage / f, armor * 0.2));
        return g / 25.0;
    }

    /**
     * The damage to put into a hit (before armour) so that, once the target's worn armour has done its work, the hit lands as if only the natural
     * armour ({@code armorNatural}, the attribute's base) were there. At most 2.5 times the damage; barriers and absorption are not touched.
     */
    public static double bypassArmour(double amount, double armorFull, double armorNatural, double toughness) {
        if (amount <= 0 || armorFull <= armorNatural + 1.0e-6) return amount;
        double wanted = amount * (1.0 - absorbFraction(amount, armorNatural, toughness));
        double lo = amount, hi = amount * 2.5;
        if (hi * (1.0 - absorbFraction(hi, armorFull, toughness)) <= wanted) return hi;
        for (int i = 0; i < 40; i++) {                                           // the damage that survives the armour grows with the damage: bisect
            double mid = (lo + hi) / 2.0;
            if (mid * (1.0 - absorbFraction(mid, armorFull, toughness)) < wanted) lo = mid; else hi = mid;
        }
        return hi;
    }

    // ---------------------------------------------------------------- Axe of the Crimson Saint: the sacrifice
    public static final int STAGE_EVERYTHING = 5, STAGE_MAX_OFFER = 4;
    public static final double CURSE_HP = 2.0, OFFER_HP = 2.0, EVERYTHING_HP = 12.0, MIN_MAX_HP = 6.0;

    /** The sacrifice of this stage in hit points (0 nothing, 1..4 offers of one heart, 5 everything), without the curse itself. */
    public static double sacrificeHp(int stage) {
        if (stage >= STAGE_EVERYTHING) return EVERYTHING_HP;
        return Math.max(0, stage) * OFFER_HP;
    }

    /** The max-health modifier to apply (negative, or 0) so that the wielder keeps at least {@link #MIN_MAX_HP} counting the curse. */
    public static double sacrificeModifier(int stage, double baseMax) {
        double room = Math.max(0.0, baseMax - CURSE_HP - MIN_MAX_HP);
        return -Math.min(sacrificeHp(stage), room);
    }

    public static double stageDamage(int stage) {
        if (stage >= STAGE_EVERYTHING) return 9.0;
        return Math.max(0, stage) * 1.5;
    }

    /** "Crits on 16 or higher" is a 25% chance; true when the roll (0..1) lands in it. */
    public static boolean saintCrit(int stage, double roll) { return stage >= STAGE_EVERYTHING && roll < 0.25; }

    /** The stage the axe stands at today: it asks again each dawn, so a stage from another day is forgotten. */
    public static int stage(Bag b, long now) {
        return b.get("st_day") == now / DAY + 1 ? (int) Math.max(0, Math.min(STAGE_EVERYTHING, b.get("stage"))) : 0;
    }

    public static void setStage(Bag b, long now, int stage) {
        b.put("st_day", now / DAY + 1);
        b.put("stage", Math.max(0, Math.min(STAGE_EVERYTHING, stage)));
    }

    /** One more offer; after the fourth the next one is "Nodol" (back to nothing). Never touches "everything". */
    public static int nextOffer(int stage) { return stage >= STAGE_MAX_OFFER ? 0 : Math.max(0, stage) + 1; }

    /** True once per day: the axe has not asked its question today. */
    public static boolean askedToday(Bag b, long now) {
        long day = now / DAY + 1;
        if (b.get("asked") == day) return true;
        b.put("asked", day);
        return false;
    }

    // ---------------------------------------------------------------- Palladion: otherworldly foes
    private static final String[] OTHERWORLD = {"daemon", "demon", "devil", "spirit", "elemental", "fairy", "angel", "wraith", "ghost", "undine", "sylph",
            "salamander", "gnome_spirit", "dryad", "phantom", "lich", "vampire", "wight", "specter", "spectre", "imp", "fiend", "colossus"};
    private static final String[] VANILLA_OTHERWORLD = {"blaze", "ghast", "vex", "wither", "allay", "breeze", "phantom", "magma_cube", "enderman", "shulker"};

    /** Celestial, fiend, fey, elemental or undead: by the undead tag, or by the entity's registry path. */
    public static boolean otherworldly(String namespace, String path, boolean undeadTag) {
        if (undeadTag) return true;
        if (path == null) return false;
        if ("minecraft".equals(namespace)) {
            for (String s : VANILLA_OTHERWORLD) if (path.equals(s)) return true;
            return false;
        }
        for (String s : OTHERWORLD) if (path.contains(s)) return true;
        return false;
    }

    // ---------------------------------------------------------------- Dagger of Betrayal
    /** Looking into the victim's eyes: the victim faces the attacker (dot of its view vector and the way to the attacker). */
    public static boolean eyeContact(double dot) { return dot >= 0.5; }

    /** The Constitution save, DC 16, against a +3 or so: about even. */
    public static boolean saveHolds(double roll) { return roll < 0.5; }

    // ---------------------------------------------------------------- Humbling Longsword
    /** The foe gives up the fight: below 35% of its health after the blow, and not a boss-sized creature. */
    public static boolean surrenders(double healthAfter, double max) { return max > 0 && max <= 150.0 && healthAfter / max <= 0.35; }

    // ---------------------------------------------------------------- Eschaton: charges
    public static final int[] THRESHOLDS = {5, 15, 30, 50, 75};
    public static final long MAX_CHARGE_HUNDREDTHS = 10000L;

    /** 0 when under 5 charges, then 1..5 as each threshold is passed. */
    public static int tier(long chargeHundredths) {
        int t = 0;
        for (int i = 0; i < THRESHOLDS.length; i++) if (chargeHundredths >= THRESHOLDS[i] * 100L) t = i + 1;
        return t;
    }

    /** Charges (in hundredths) gained by spending this fraction of the maximum magicules: a 10% spell is worth about three charges. */
    public static long gain(double spentFraction) {
        if (spentFraction <= 0) return 0;
        return Math.min(800L, Math.round(spentFraction * 100.0 / 3.0 * 100.0));
    }

    /** The charges today (they are lost at dawn, as at a long rest). */
    public static long charge(Bag b, long now) { return b.get("chg_day") == now / DAY + 1 ? b.get("chg") : 0L; }

    public static void addCharge(Bag b, long now, long hundredths) {
        long cur = charge(b, now);
        b.put("chg_day", now / DAY + 1);
        b.put("chg", Math.min(MAX_CHARGE_HUNDREDTHS, cur + Math.max(0, hundredths)));
    }

    public static void clearCharge(Bag b, long now) {
        b.put("chg_day", now / DAY + 1);
        b.put("chg", 0);
    }

    // ---------------------------------------------------------------- Contingency Plan
    public enum Trigger {
        LOW_HEALTH("health falls under 30%"), BURNING("you are set on fire"), DROWNING("you are running out of air"),
        FALLING("you fall from a great height"), SURROUNDED("three foes close in"), AILING("you are poisoned or withered");

        private final String when;

        Trigger(String when) { this.when = when; }

        public String when() { return when; }
    }

    /** What the dagger looks at, once every ten ticks. */
    public record Snapshot(double health, double max, boolean burning, int air, double fall, int foesNear, boolean ailing, boolean creative) {}

    public static boolean fires(Trigger t, Snapshot s) {
        if (s.creative()) return false;
        return switch (t) {
            case LOW_HEALTH -> s.max() > 0 && s.health() / s.max() < 0.30 && s.health() > 0;
            case BURNING -> s.burning();
            case DROWNING -> s.air() < 60;
            case FALLING -> s.fall() > 12.0;
            case SURROUNDED -> s.foesNear() >= 3;
            case AILING -> s.ailing();
        };
    }

    public static Trigger nextTrigger(int ordinal) {
        Trigger[] all = Trigger.values();
        return all[Math.floorMod(ordinal + 1, all.length)];
    }

    public static Trigger triggerAt(long ordinal) {
        Trigger[] all = Trigger.values();
        return all[(int) Math.floorMod(ordinal, (long) all.length)];
    }

    /** One stored effect: "id@ticks@amplifier". */
    public record Stored(String id, int ticks, int amp) {}

    public static final int MAX_STORED = 4, MAX_TICKS = 20 * 180, MAX_AMP = 3;

    public static String encode(List<Stored> list) {
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (Stored s : list) {
            if (s.id() == null || s.id().indexOf('@') >= 0 || s.id().indexOf(';') >= 0 || n >= MAX_STORED) continue;
            if (n > 0) sb.append(';');
            sb.append(s.id()).append('@').append(Math.max(1, Math.min(MAX_TICKS, s.ticks()))).append('@').append(Math.max(0, Math.min(MAX_AMP, s.amp())));
            n++;
        }
        return sb.toString();
    }

    public static List<Stored> decode(String text) {
        List<Stored> out = new ArrayList<>();
        if (text == null || text.isBlank()) return out;
        for (String part : text.split(";")) {
            String[] f = part.split("@");
            if (f.length != 3 || out.size() >= MAX_STORED) continue;
            try {
                out.add(new Stored(f[0], Math.max(1, Math.min(MAX_TICKS, Integer.parseInt(f[1]))), Math.max(0, Math.min(MAX_AMP, Integer.parseInt(f[2])))));
            } catch (NumberFormatException bad) {
                // a damaged entry is skipped
            }
        }
        return out;
    }

    /** "Once used, the dagger cannot be used this way again until the next dawn." */
    public static boolean spentToday(Bag b, long now) { return b.get("spent") == now / DAY + 1; }

    public static void markSpent(Bag b, long now) { b.put("spent", now / DAY + 1); }

    // ---------------------------------------------------------------- Time Bender Dadao: tiers unlock with time
    public static final long TIER2_TICKS = DAY, TIER3_TICKS = 3 * DAY;

    public static int timeTier(long wieldedTicks) {
        if (wieldedTicks >= TIER3_TICKS) return 3;
        return wieldedTicks >= TIER2_TICKS ? 2 : 1;
    }

    /** Ticks still to wait for the next tier, or 0 at the top. */
    public static long untilNextTier(long wieldedTicks) {
        if (wieldedTicks >= TIER3_TICKS) return 0;
        return (wieldedTicks >= TIER2_TICKS ? TIER3_TICKS : TIER2_TICKS) - wieldedTicks;
    }
}
