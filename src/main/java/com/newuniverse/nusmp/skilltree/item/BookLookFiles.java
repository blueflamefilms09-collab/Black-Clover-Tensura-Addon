package com.newuniverse.nusmp.skilltree.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Why: the 3.26.0 look is 400 files that no compiler reads (55 body models, 55 icon models, 55 wrappers, 55 painted icons with their tint layers, 11 covers with their relief layers, 5 trims, the
 * animated mythic edge). A file missing from the jar is a purple-and-black checker in a player's hand, and nothing in the build notices. This pure class lists every resource path the look needs
 * for a franchise list, so one unit test (against the source tree) and one server self-test {@code skillbook.look.files} (against the packed class path) ask the same question. No Minecraft type.
 */
public final class BookLookFiles {
    private BookLookFiles() {}

    public static final String ITEM_MODELS = "assets/nusmp/models/item/";
    public static final String TEX_V2 = "assets/nusmp/textures/item/skill_book_v2/";
    public static final String TEX_ICONS = "assets/nusmp/textures/item/skill_book_icons/";
    /** The franchises that have a 1.82.0 cover (the first ten of the table); every later one only has the new look. */
    public static final int LEGACY_FRANCHISES = 10;

    /** Every resource path (relative to the class path root) the look needs for these franchises, in a stable order. */
    public static List<String> requiredPaths(List<String> franchises) {
        List<String> out = new ArrayList<>();
        out.add(ITEM_MODELS + "skill_book.json");
        out.add(ITEM_MODELS + "skill_book_v2.json");
        out.add(ITEM_MODELS + "skill_book_st_generic.json");
        for (String r : BookLook.RARITY_IDS) {
            out.add(TEX_V2 + "trim_" + r + ".png");
        }
        out.add(TEX_V2 + "glow_mythic.png");
        out.add(TEX_V2 + "glow_mythic.png.mcmeta");
        for (int i = 0; i < franchises.size(); i++) {
            String f = franchises.get(i);
            out.add(TEX_V2 + f + ".png");
            out.add(TEX_V2 + f + "_fx.png");
            for (String r : BookLook.RARITY_IDS) {
                out.add(ITEM_MODELS + modelName(f, r) + ".json");
                out.add(ITEM_MODELS + "skill_book_icon_" + f + "_" + r + ".json");
                out.add(ITEM_MODELS + "skill_book_v2st_" + f + "_" + r + ".json");
                out.add(TEX_ICONS + f + "_" + r + ".png");
                out.add(TEX_ICONS + f + "_" + r + "_tint.png");
            }
            if (i < LEGACY_FRANCHISES) {
                out.add(ITEM_MODELS + "skill_book_3d_" + f + ".json");
                out.add(ITEM_MODELS + "skill_book_st_" + f + ".json");
            }
        }
        return out;
    }

    /** The body model's file name (without .json) of a franchise and rarity. */
    public static String modelName(String franchise, String rarity) { return "skill_book_v2_" + franchise + "_" + rarity; }

    /** The required paths that {@code exists} does not know: empty when the look is complete. */
    public static List<String> missing(List<String> franchises, Predicate<String> exists) {
        List<String> bad = new ArrayList<>();
        for (String p : requiredPaths(franchises)) if (!exists.test(p)) bad.add(p);
        return bad;
    }
}
