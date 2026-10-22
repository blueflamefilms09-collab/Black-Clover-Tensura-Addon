package com.newuniverse.nusmp.labyrinth.props;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the rare armour stand of the dungeon ({@link ArmourStandRarity}) is worth finding only if what it wears belongs to the floor it stands on: the same armour the dungeon's loot gives
 * at that depth. The loot system cuts depth into five bands ({@code labyrinth/loot/LootBand}: floors 0-9, 10-29, 30-99, 100-299, 300 and deeper) and has two armour sets of its own, Drake-Scale
 * (from band 1) and Shadowsteel (from band 2). So the stand wears iron on the first ten floors, Drake-Scale from floor 10 on, Shadowsteel from floor 30 on, and the pieces are rolled gear of a
 * rarity that rises with the band (uncommon, uncommon, rare, epic, legendary), made by the loot system's own roll. Two to four pieces, always the chest piece, so the stand is never a bare
 * pole. Pure: the choice is a function of the band and a seed; {@code ArmourStandGate} turns the ids into stacks.
 */
public final class RareStandSet {
    private RareStandSet() {}

    /** One piece: the slot name ({@code head}, {@code chest}, {@code legs}, {@code feet}), the item id (namespace included) and the loot rarity id ({@code ""} for a plain vanilla item). */
    public record Piece(String slot, String item, String rarity) {}

    private static final String[] SLOTS = {"head", "chest", "legs", "feet"};

    /** The loot band of a floor (the same cut points as {@code LootBand.of}; the sign is ignored). */
    public static int band(int depth) {
        int d = Math.abs(depth);
        if (d < 10) return 0;
        if (d < 30) return 1;
        if (d < 100) return 2;
        if (d < 300) return 3;
        return 4;
    }

    /** The armour set of a band: {@code iron}, {@code drakescale} or {@code shadowsteel}. */
    public static String setOf(int band) {
        if (band <= 0) return "iron";
        if (band == 1) return "drakescale";
        return "shadowsteel";
    }

    /** The rarity id the pieces of a band are rolled with (empty for the vanilla iron of band 0). */
    public static String rarityOf(int band) {
        return switch (Math.max(0, Math.min(4, band))) {
            case 0 -> "";
            case 1 -> "uncommon";
            case 2 -> "rare";
            case 3 -> "epic";
            default -> "legendary";
        };
    }

    /** The item id of one slot of a set. */
    public static String itemOf(String set, String slot) {
        return switch (set) {
            case "iron" -> "minecraft:iron_" + switch (slot) {
                case "head" -> "helmet";
                case "chest" -> "chestplate";
                case "legs" -> "leggings";
                default -> "boots";
            };
            case "drakescale" -> "nusmp:drakescale_" + switch (slot) {
                case "head" -> "helm";
                case "chest" -> "vest";
                case "legs" -> "greaves";
                default -> "boots";
            };
            default -> "nusmp:shadowsteel_" + switch (slot) {
                case "head" -> "helm";
                case "chest" -> "cuirass";
                case "legs" -> "greaves";
                default -> "sabatons";
            };
        };
    }

    /** How many pieces (2, 3 or 4) the stand wears for this seed: 40, 40 and 20 percent. */
    public static int count(long seed) {
        int r = (int) Long.remainderUnsigned(ArmourStandRarity.mix(seed ^ 0x7F4A7C15L) >>> 1, 10);
        return r < 4 ? 2 : r < 8 ? 3 : 4;
    }

    /** The pieces the stand of a floor wears (never empty, always with the chest piece, at most one per slot), for the floor's depth and a seed. */
    public static List<Piece> pieces(int depth, long seed) {
        int band = band(depth);
        String set = setOf(band), rarity = rarityOf(band);
        int n = count(seed);
        boolean[] on = new boolean[SLOTS.length];
        on[1] = true;
        long z = seed ^ 0x51ED270B0B1AL;
        int have = 1;
        for (int guard = 0; have < n && guard < 32; guard++) {
            z = ArmourStandRarity.mix(z);
            int i = (int) Long.remainderUnsigned(z >>> 1, SLOTS.length);
            if (!on[i]) {
                on[i] = true;
                have++;
            }
        }
        List<Piece> out = new ArrayList<>();
        for (int i = 0; i < SLOTS.length; i++) if (on[i]) out.add(new Piece(SLOTS[i], itemOf(set, SLOTS[i]), rarity));
        return out;
    }
}
