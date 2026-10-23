package com.newuniverse.nusmp.book.pages15;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the "15 spells for every grimoire" work is split over eight builders (docs/magic/page_counts.md section 8) and each book belongs to exactly ONE batch, so no two builders ever edit the same
 * file. This is that table (book registry path -> batch 1..8), pure and unit tested against the plan in the doc. {@link Pages15} asks only the batch that owns a book.
 */
public final class Pages15Plan {
    private Pages15Plan() {}

    public static final int BATCHES = 8;

    public static final String[] NAMES = {
            "Flame, Lightning, Star and Thread", "Water, Ice, Fluids and Art", "Wind, Air, Sound, Mind and Soul", "Earth, Stone and Metal",
            "Light, Dark and Curse", "Space, Time, Game and Mirror", "Nature and Beast", "Seal, Transform and Body"};

    private static final Map<String, Integer> OWNER;

    static {
        Map<String, Integer> m = new LinkedHashMap<>();
        // Batch 1: Flame, Lightning, Star and Thread
        m.put("book_explosion", 1); m.put("book_storm", 1); m.put("book_thread", 1);
        m.put("book_magma", 1); m.put("book_sword", 1); m.put("book_reinforcement", 1);
        m.put("book_fire", 1); m.put("book_star", 1); m.put("book_lightning", 1);
        m.put("book_demon_fire", 1);
        // Batch 2: Water, Ice, Fluids and Art
        m.put("book_creation", 2); m.put("book_mist", 2); m.put("book_painting", 2);
        m.put("book_snow", 2); m.put("book_ice", 2); m.put("book_water", 2);
        m.put("book_demon_water", 2); m.put("book_mucus", 2); m.put("book_mud", 2);
        m.put("book_food", 2); m.put("book_eye", 2); m.put("book_demon_ice", 2);
        m.put("book_bubble", 2); m.put("book_ice_wedge", 2); m.put("book_gel", 2);
        m.put("book_black_oil", 2); m.put("book_eyeball", 2);
        // Batch 3: Wind, Air, Sound, Mind and Soul
        m.put("book_dream", 3); m.put("book_illusion", 3); m.put("book_air", 3);
        m.put("book_smoke", 3); m.put("book_vortex", 3); m.put("book_wing", 3);
        m.put("book_wind", 3); m.put("book_sound", 3); m.put("book_memory", 3);
        m.put("book_permeation", 3); m.put("book_soul", 3); m.put("book_soul_corpse", 3);
        m.put("book_song", 3);
        // Batch 4: Earth, Stone and Metal
        m.put("book_mercury", 4); m.put("book_sand", 4); m.put("book_steel", 4);
        m.put("book_rock", 4); m.put("book_red_ochre", 4); m.put("book_stone", 4);
        m.put("book_mineral", 4); m.put("book_sandstone", 4); m.put("book_earth", 4);
        m.put("book_shakudo", 4); m.put("book_crystal", 4); m.put("book_glass", 4);
        m.put("book_iron", 4); m.put("book_copper", 4); m.put("book_corundum", 4);
        m.put("book_bronze", 4);
        // Batch 5: Light, Dark and Curse
        m.put("book_bone", 5); m.put("book_dark", 5); m.put("book_shadow", 5);
        m.put("book_ash", 5); m.put("book_blood", 5); m.put("book_light", 5);
        m.put("book_curse", 5); m.put("book_anti_magic", 5); m.put("forbidden_magic", 5);
        m.put("book_curse_warding", 5); m.put("book_demon_light", 5);
        // Batch 6: Space, Time, Game and Mirror
        m.put("book_compass", 6); m.put("book_copy", 6); m.put("book_dice", 6);
        m.put("book_slash", 6); m.put("book_gravity", 6); m.put("book_game", 6);
        m.put("book_spatial", 6); m.put("book_mirror", 6); m.put("book_time", 6);
        m.put("book_legion", 6); m.put("book_key", 6); m.put("book_kotodama", 6);
        // Batch 7: Nature and Beast
        m.put("book_world_tree", 7); m.put("book_poison", 7); m.put("book_cotton", 7);
        m.put("book_plant", 7); m.put("book_cherry_blossom", 7); m.put("book_briar", 7);
        m.put("book_tree", 7); m.put("book_vine", 7); m.put("book_poison_plant", 7);
        m.put("book_beast", 7); m.put("book_body", 7); m.put("book_fungus", 7);
        m.put("book_demon_beast", 7);
        // Batch 8: Seal, Transform and Body
        m.put("book_sealing", 8); m.put("book_skin", 8); m.put("book_tongue", 8);
        m.put("book_switching", 8); m.put("book_recombination", 8); m.put("book_transmutation", 8);
        m.put("book_butoh", 8); m.put("book_modification", 8); m.put("book_hair", 8);
        m.put("book_nail", 8); m.put("book_scale", 8); m.put("book_spike", 8);
        m.put("book_chain", 8); m.put("book_barrier", 8);
        OWNER = Collections.unmodifiableMap(m);
    }

    /** The batch (1..8) that owns this book, or 0 for a book that is not in the plan. */
    public static int batchOf(String bookPath) { return bookPath == null ? 0 : OWNER.getOrDefault(bookPath, 0); }

    /** Every book of the plan, in plan order. */
    public static List<String> allBooks() { return List.copyOf(OWNER.keySet()); }

    /** The books of one batch, in plan order. */
    public static List<String> booksOf(int batch) {
        List<String> out = new ArrayList<>();
        for (var e : OWNER.entrySet()) if (e.getValue() == batch) out.add(e.getKey());
        return out;
    }

    /** The name of a batch ("Flame, Lightning, Star and Thread"), or "" for a number that is not a batch. */
    public static String nameOf(int batch) { return batch >= 1 && batch <= BATCHES ? NAMES[batch - 1] : ""; }
}
