package com.newuniverse.nusmp.requip;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: Partial Armour is the piece-by-piece side of Equip Magic: the user wears only chosen parts of an Erza armour (for example only the gauntlets and greaves of Adamantine) and gets that share of its
 * stats. The Erza armour is a render-layer aura and must never enter an armour slot, so a partial armour is the same aura drawn with a subset of its pieces. A subset is an integer mask over eleven
 * piece families; it rides in the aura's style int next to the armour's ordinal (ordinal in the low byte, mask above it) so it is synced with the aura lease and a full set stays the plain ordinal the
 * old clients and tests know. Pure arithmetic, no Minecraft classes.
 */
public final class PartialArmourMask {
    private PartialArmourMask() {}

    // ------------------------------------------------------------ the families (one bit each)
    public static final int HELM = 1, GORGET = 1 << 1, CHEST = 1 << 2, BELT = 1 << 3, PAULDRONS = 1 << 4, VAMBRACES = 1 << 5, GAUNTLETS = 1 << 6, CUISSES = 1 << 7, GREAVES = 1 << 8, SABATONS = 1 << 9,
            SIGNATURE = 1 << 10;
    public static final int FAMILIES = 11;
    /** Every family: the whole armour. */
    public static final int FULL = (1 << FAMILIES) - 1;
    /** Below this share of an armour's value its special attacks and guard stance stay shut. */
    public static final double MIN_ATTACK_FRACTION = 0.25;

    private static final String[] NAMES = {"helm", "gorget", "chest", "belt", "pauldrons", "vambraces", "gauntlets", "cuisses", "greaves", "sabatons", "signature"};
    private static final String[] ENGLISH = {"Helm and ornaments", "Gorget", "Chest and back", "Belt and skirt", "Pauldrons", "Vambraces", "Gauntlets", "Cuisses", "Greaves", "Sabatons", "Wings, tail, cape"};

    public static String familyName(int bitIndex) { return NAMES[bitIndex]; }
    public static String familyEnglish(int bitIndex) { return ENGLISH[bitIndex]; }

    /** The family bit of a piece (by the name in {@link ArmourSets}); 0 for a name nobody owns. */
    public static int familyOf(String piece) {
        return switch (piece) {
            case "helm", "halo", "ribbon" -> HELM;
            case "gorget" -> GORGET;
            case "chest", "backplate" -> CHEST;
            case "belt", "skirt" -> BELT;
            case "pauldron_r", "pauldron_l" -> PAULDRONS;
            case "vambrace_r", "vambrace_l" -> VAMBRACES;
            case "gauntlet_r", "gauntlet_l" -> GAUNTLETS;
            case "cuisse_r", "cuisse_l" -> CUISSES;
            case "greave_r", "greave_l" -> GREAVES;
            case "sabaton_r", "sabaton_l" -> SABATONS;
            case "wing_r", "wing_l", "tail", "cape", "backdrop" -> SIGNATURE;
            default -> 0;
        };
    }

    /** What a piece is worth in armour (its share of the stats); the sum over a full set is about a hundred. */
    public static int valueOf(String piece) {
        return switch (piece) {
            case "helm" -> 10;
            case "halo", "ribbon" -> 2;
            case "gorget" -> 3;
            case "chest" -> 18;
            case "backplate" -> 8;
            case "belt" -> 2;
            case "skirt" -> 4;
            case "pauldron_r", "pauldron_l" -> 6;
            case "vambrace_r", "vambrace_l", "gauntlet_r", "gauntlet_l" -> 4;
            case "cuisse_r", "cuisse_l" -> 6;
            case "greave_r", "greave_l" -> 5;
            case "sabaton_r", "sabaton_l" -> 3;
            case "wing_r", "wing_l" -> 4;
            case "tail", "cape" -> 3;
            case "backdrop" -> 2;
            default -> 0;
        };
    }

    // ------------------------------------------------------------ masks
    /** A mask held to the eleven families; 0 (nothing chosen) means the whole armour. */
    public static int normalize(int mask) {
        int m = mask & FULL;
        return m == 0 ? FULL : m;
    }

    public static boolean isFull(int mask) { return normalize(mask) == FULL; }

    public static boolean includes(int mask, String piece) {
        int f = familyOf(piece);
        return f != 0 && (normalize(mask) & f) != 0;
    }

    /** The pieces of an armour that the mask keeps, in the armour's own order. */
    public static List<ArmourSets.Piece> subset(Armour a, int mask) {
        List<ArmourSets.Piece> out = new ArrayList<>();
        for (ArmourSets.Piece p : ArmourSets.of(a)) if (includes(mask, p.name())) out.add(p);
        return out;
    }

    /** The share (0..1) of the armour's value that the kept pieces are: what a partial armour gives of its stats, resistances and upkeep. */
    public static double fraction(Armour a, int mask) {
        int total = 0, kept = 0;
        for (ArmourSets.Piece p : ArmourSets.of(a)) {
            int v = valueOf(p.name());
            total += v;
            if (includes(mask, p.name())) kept += v;
        }
        return total <= 0 ? 0.0 : Math.max(0.0, Math.min(1.0, kept / (double) total));
    }

    /** True when the armour is a winged one and the mask keeps its wings: only then does it fly. An armour without wings (flies by its own magic, or not at all) is not held back by the mask. */
    public static boolean flies(Armour a, int mask) {
        boolean wings = false;
        for (ArmourSets.Piece p : ArmourSets.of(a)) if (p.name().startsWith("wing_")) wings = true;
        return !wings || (normalize(mask) & SIGNATURE) != 0;
    }

    /** True when the mask keeps enough of the armour for its attacks and guard. */
    public static boolean canFight(Armour a, int mask) { return fraction(a, mask) + 1e-9 >= MIN_ATTACK_FRACTION; }

    public static int count(int mask) { return Integer.bitCount(normalize(mask)); }

    /** The mask with one family flipped. The result is never empty: flipping off the last family leaves it on. */
    public static int toggle(int mask, int bitIndex) {
        if (bitIndex < 0 || bitIndex >= FAMILIES) return normalize(mask);
        int m = normalize(mask) ^ (1 << bitIndex);
        return (m & FULL) == 0 ? normalize(mask) : m & FULL;
    }

    // ------------------------------------------------------------ the style int of the aura
    /** The style the aura is set with: the armour's ordinal, and for a partial set the mask above it. A full set is the plain ordinal, exactly as before the Equip Magic was added. */
    public static int encode(int armourOrdinal, int mask) {
        int m = normalize(mask);
        return m == FULL ? armourOrdinal & 0xFF : (armourOrdinal & 0xFF) | (m << 8);
    }

    public static int armourOf(int style) { return style & 0xFF; }

    /** The mask in a style: the whole set when none was added. */
    public static int maskOf(int style) { return normalize(style >>> 8); }

    // ------------------------------------------------------------ presets (what the Partial Armour mode cycles through)
    public record Preset(String id, String english, int mask) {}

    /** The first preset is the whole armour; the rest are the useful parts. Append only. */
    public static final List<Preset> PRESETS = List.of(
            new Preset("full", "Whole armour", FULL),
            new Preset("fists_and_boots", "Gauntlets and greaves", GAUNTLETS | GREAVES),
            new Preset("helm_chest", "Helm and chest", HELM | GORGET | CHEST),
            new Preset("arms", "Arms", PAULDRONS | VAMBRACES | GAUNTLETS),
            new Preset("legs", "Legs", CUISSES | GREAVES | SABATONS),
            new Preset("plate_only", "Plate without ornaments", FULL & ~(SIGNATURE | BELT)),
            new Preset("wings_chest", "Wings and chest", SIGNATURE | CHEST | GORGET));

    public static Preset preset(int index) { return PRESETS.get(Math.floorMod(index, PRESETS.size())); }

    /** The index of the preset with this mask, or -1 (a custom mask). */
    public static int presetIndexOf(int mask) {
        int m = normalize(mask);
        for (int i = 0; i < PRESETS.size(); i++) if (PRESETS.get(i).mask() == m) return i;
        return -1;
    }
}
