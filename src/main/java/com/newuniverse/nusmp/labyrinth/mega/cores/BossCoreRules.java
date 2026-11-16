package com.newuniverse.nusmp.labyrinth.mega.cores;

import java.util.Locale;

/**
 * Why: the numbers and choices of the Boss Cores system as PURE functions (no Minecraft type), so a unit test can pin them: which family of core a Decennial boss pays by its level, how many by its floor, how
 * the four-piece armour sets add up, and how the set bonuses of mixed sets are capped. The game side ({@link BossCoreAward}, {@link BossCoreEvents}) only reads these and applies the result.
 * <p>
 * The rule of the pay: the boss of level L stands on floor 10 L (Holy positive, Evil negative); the SIDE makes no difference, only |L|. The family is {@code |L| mod 4} (Aegis, Striker, Wellspring, Ascendant);
 * a Wellspring boss pays the resource {@code (|L| / 4) mod 4} (Magicule, Aura, Ki, Nihility), so every resource comes round once every 16 levels (160 floors). Nothing is paid above the first band.
 */
public final class BossCoreRules {
    private BossCoreRules() {}

    public enum Family { AEGIS, STRIKER, WELLSPRING, ASCENDANT }

    public enum Resource { MAGICULE, AURA, KI, NIHILITY }

    /** The four pieces of a set, head to feet. */
    public enum Piece {
        HELMET, CHESTPLATE, LEGGINGS, BOOTS;

        public String id() { return name().toLowerCase(Locale.ROOT); }
    }

    /** One set of armour (and the one core it is made of): seven in all, the Wellspring has one per resource. The order is fixed (it is the index of the piece counters). */
    public enum Kit {
        AEGIS(Family.AEGIS, null), STRIKER(Family.STRIKER, null),
        WELLSPRING_MAGICULE(Family.WELLSPRING, Resource.MAGICULE), WELLSPRING_AURA(Family.WELLSPRING, Resource.AURA),
        WELLSPRING_KI(Family.WELLSPRING, Resource.KI), WELLSPRING_NIHILITY(Family.WELLSPRING, Resource.NIHILITY),
        ASCENDANT(Family.ASCENDANT, null);

        private final Family family;
        private final Resource resource;

        Kit(Family family, Resource resource) {
            this.family = family;
            this.resource = resource;
        }

        public Family family() { return family; }

        /** The resource a Wellspring set restores, or null for the other families. */
        public Resource resource() { return resource; }

        public String id() { return name().toLowerCase(Locale.ROOT); }

        /** The item id of the core: {@code core_aegis}, {@code core_wellspring_ki}, ... */
        public String coreId() { return "core_" + id(); }

        /** The item id of one piece: {@code aegis_helmet}, {@code wellspring_aura_boots}, ... */
        public String pieceId(Piece p) { return id() + "_" + p.id(); }

        public static Kit of(Family f, Resource r) {
            if (f != Family.WELLSPRING) return f == Family.AEGIS ? AEGIS : f == Family.STRIKER ? STRIKER : ASCENDANT;
            switch (r == null ? Resource.MAGICULE : r) {
                case AURA: return WELLSPRING_AURA;
                case KI: return WELLSPRING_KI;
                case NIHILITY: return WELLSPRING_NIHILITY;
                default: return WELLSPRING_MAGICULE;
            }
        }
    }

    public static final int KITS = Kit.values().length;

    /** Is this the item id of one of the seven cores or twenty-eight pieces? */
    public static boolean isBossCoreId(String path) {
        if (path == null) return false;
        for (Kit k : Kit.values()) {
            if (path.equals(k.coreId())) return true;
            for (Piece p : Piece.values()) if (path.equals(k.pieceId(p))) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- what a boss pays
    private static int mag(int level) { return level == Integer.MIN_VALUE ? Integer.MAX_VALUE : Math.abs(level); }

    /** The family a boss of this level pays (the side does not matter). */
    public static Family familyOfLevel(int level) { return Family.values()[mag(level) % 4]; }

    /** The resource a Wellspring boss of this level pays. */
    public static Resource resourceOfLevel(int level) { return Resource.values()[(mag(level) / 4) % 4]; }

    /** The core (as a set) a boss of this level pays. */
    public static Kit kitOfLevel(int level) { return Kit.of(familyOfLevel(level), resourceOfLevel(level)); }

    /**
     * How many cores a boss on this floor pays: none below {@code minFloor}, else the count of the band the floor is in (bands start at {@code minFloor}, {@code band2}, {@code band3}, {@code band4}; the
     * floor's sign is ignored). The counts are clamped to 0..64.
     */
    public static int count(int floor, int minFloor, int band2, int band3, int band4, int c1, int c2, int c3, int c4) {
        int f = mag(floor);
        if (f < minFloor) return 0;
        int n = f >= band4 ? c4 : f >= band3 ? c3 : f >= band2 ? c2 : c1;
        return Math.max(0, Math.min(64, n));
    }

    // ---------------------------------------------------------------- the sets
    /** 0 for fewer than two pieces of one set, 1 for two or three, 2 for the whole set. */
    public static int tierOfPieces(int pieces) { return pieces >= 4 ? 2 : pieces >= 2 ? 1 : 0; }

    /**
     * The set-bonus tier each kit really gets: its own tier, but the tiers of all kits together never pass {@code cap} (so mixing two half sets cannot out-do one whole set unless the owner raises the cap).
     * The bigger tier is served first, ties by kit order; a kit that cannot get its whole tier gets what is left.
     */
    public static int[] cappedTiers(int[] pieces, int cap) {
        int[] out = new int[KITS];
        int left = Math.max(0, cap);
        for (int t = 2; t >= 1; t--) {
            for (int i = 0; i < KITS && i < pieces.length; i++) {
                if (tierOfPieces(pieces[i]) != t || left <= 0) continue;
                out[i] = Math.min(t, left);
                left -= out[i];
            }
        }
        return out;
    }

    /** The factor a tier gives to a per-second or per-kill number: 1, {@code m2}, {@code m4}. */
    public static double tierMult(int tier, double m2, double m4) { return tier >= 2 ? m4 : tier == 1 ? m2 : 1.0; }

    /** Damage reduction in percent of the Aegis: per piece plus the set bonus of its tier, never above {@code cap} and never below 0. */
    public static double resistPercent(int pieces, int tier, double per, double bonus2, double bonus4, double cap) {
        double v = per * Math.max(0, pieces) + (tier >= 2 ? bonus4 : tier == 1 ? bonus2 : 0.0);
        return Math.max(0.0, Math.min(cap, v));
    }

    /** Percent of a pool a Wellspring gives back in one second (per piece, times the tier factor), capped. */
    public static double regenPercent(int pieces, int tier, double per, double m2, double m4, double cap) {
        return Math.max(0.0, Math.min(cap, per * Math.max(0, pieces) * tierMult(tier, m2, m4)));
    }

    /** The extra EP an Ascendant kill adds: a share of the victim's EP (per piece, times the tier factor), never above a share of the wearer's own base EP. */
    public static double epBonus(double victimEp, int pieces, int tier, double sharePct, double m2, double m4, double baseEp, double capPct) {
        if (victimEp <= 0.0 || pieces <= 0) return 0.0;
        double raw = victimEp * sharePct * pieces * tierMult(tier, m2, m4) / 100.0;
        return Math.max(0.0, Math.min(raw, Math.max(0.0, baseEp) * capPct / 100.0));
    }

    /** A cooldown after one second of Nihility: it falls by {@code pct} percent, at least by one, never below 0 (units are the skill's own, so the rule has no unit). */
    public static int reduceCooldown(int cooldown, double pct) {
        if (cooldown <= 0) return 0;
        int cut = Math.max(1, (int) Math.floor(cooldown * Math.max(0.0, pct) / 100.0));
        return Math.max(0, cooldown - cut);
    }

    /** The second damage type of the Striker: this share (percent) of a hit. */
    public static float extraShare(float amount, double pct) { return amount <= 0f || pct <= 0.0 ? 0f : (float) (amount * pct / 100.0); }

    /** Is this a damage type of another mod (its registry namespace is not {@code minecraft})? */
    public static boolean foreignNamespace(String namespace) { return namespace != null && !namespace.isEmpty() && !"minecraft".equals(namespace); }
}
