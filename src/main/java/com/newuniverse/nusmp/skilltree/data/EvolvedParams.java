package com.newuniverse.nusmp.skilltree.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Why (3.26.0, the Evolution skills): a passive may carry an {@code evolved} block, a flat set of parameters (a number or an enum word per name, the same names and ranges as the passive's own kind) that
 * REPLACE the passive's table values once the player has evolved the skill, so "the Debt stores 25 % of your blows" becomes "stores 45 %" with no new node and no new kind. Numbers are single values (not per-level
 * tables: an evolved passive is at its top level), words are enums ({@code vfx}, {@code effect}). Immutable, no Minecraft type; empty for every node that has no block.
 */
public record EvolvedParams(Map<String, Double> nums, Map<String, String> strs) {
    public static final EvolvedParams NONE = new EvolvedParams(Map.of(), Map.of());

    public EvolvedParams {
        nums = nums == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(nums));
        strs = strs == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(strs));
    }

    public boolean isEmpty() { return nums.isEmpty() && strs.isEmpty(); }

    /** The evolved number of this name, or null when the block does not override it. */
    public Double num(String key) { return nums.get(key); }

    /** The evolved word of this name, or null. */
    public String str(String key) { return strs.get(key); }
}
