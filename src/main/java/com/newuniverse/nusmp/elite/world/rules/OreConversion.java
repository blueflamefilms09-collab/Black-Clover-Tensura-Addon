package com.newuniverse.nusmp.elite.world.rules;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Why: in a chunk that has been dense with magicule for long, ordinary ores slowly turn into magic ore. This class holds the two pure halves of that: the data-driven MAP (which ore
 * becomes what, read from {@code data/nusmp/elite/world/ore_conversion.json}, a pack can add or replace entries) and the BUDGET (how many blocks a chunk may convert per interval and in
 * its whole life, so a rich chunk is never eaten whole). The game side only walks random positions, asks {@link #lookup} and {@link Budget#tryTake}, and swaps the block.
 *
 * <p>File format: {@code {"conversions":[{"from":["minecraft:iron_ore",...],"to":[{"block":"tensura:magic_ore","weight":3},...],"minDensity":0.7,"minY":-64,"maxY":320}]}}.
 * Targets that are not registered (Tensura absent or renamed) are skipped by the {@code exists} predicate of {@link #pick}; nothing here names a game class.
 */
public final class OreConversion {
    private OreConversion() {}

    public record Target(String block, int weight) {}

    public record Entry(List<String> from, List<Target> to, double minDensity, int minY, int maxY) {
        public boolean fits(double density, int y) { return density >= minDensity && y >= minY && y <= maxY; }
    }

    /** The parsed table: ore id to its entry. A later entry for the same ore replaces an earlier one (a datapack's file loads after the mod's). */
    public static final class Table {
        private final Map<String, Entry> byFrom = new HashMap<>();

        public Entry lookup(String fromId) { return fromId == null ? null : byFrom.get(fromId); }

        public int size() { return byFrom.size(); }

        public boolean isEmpty() { return byFrom.isEmpty(); }

        public java.util.Set<String> sources() { return Collections.unmodifiableSet(byFrom.keySet()); }

        /** Merges the entries of one file into this table (the later file wins per ore). Returns how many ore ids were set. */
        public int add(JsonObject root) {
            int n = 0;
            if (root == null || !root.has("conversions") || !root.get("conversions").isJsonArray()) return 0;
            for (JsonElement el : root.getAsJsonArray("conversions")) {
                if (!el.isJsonObject()) continue;
                Entry e = parseEntry(el.getAsJsonObject());
                if (e == null) continue;
                for (String f : e.from()) {
                    byFrom.put(f, e);
                    n++;
                }
            }
            return n;
        }
    }

    private static Entry parseEntry(JsonObject o) {
        List<String> from = new ArrayList<>();
        if (o.has("from") && o.get("from").isJsonArray()) for (JsonElement f : o.getAsJsonArray("from")) if (f.isJsonPrimitive()) from.add(f.getAsString());
        if (o.has("from") && o.get("from").isJsonPrimitive()) from.add(o.get("from").getAsString());
        List<Target> to = new ArrayList<>();
        if (o.has("to") && o.get("to").isJsonArray()) {
            JsonArray arr = o.getAsJsonArray("to");
            for (JsonElement t : arr) {
                if (t.isJsonPrimitive()) to.add(new Target(t.getAsString(), 1));
                else if (t.isJsonObject() && t.getAsJsonObject().has("block")) {
                    JsonObject to1 = t.getAsJsonObject();
                    int w = to1.has("weight") ? Math.max(1, to1.get("weight").getAsInt()) : 1;
                    to.add(new Target(to1.get("block").getAsString(), w));
                }
            }
        }
        if (from.isEmpty() || to.isEmpty()) return null;
        double min = o.has("minDensity") ? o.get("minDensity").getAsDouble() : 0.7;
        int minY = o.has("minY") ? o.get("minY").getAsInt() : Integer.MIN_VALUE;
        int maxY = o.has("maxY") ? o.get("maxY").getAsInt() : Integer.MAX_VALUE;
        return new Entry(List.copyOf(from), List.copyOf(to), min, minY, maxY);
    }

    /**
     * Picks the block an entry turns the ore into, by weight, among the targets that exist. {@code roll} is a uniform number in 0..1. Null when no target exists.
     */
    public static String pick(Entry e, double roll, Predicate<String> exists) {
        if (e == null) return null;
        int total = 0;
        for (Target t : e.to()) if (exists.test(t.block())) total += t.weight();
        if (total <= 0) return null;
        double r = Math.max(0.0, Math.min(0.999999, roll)) * total;
        int acc = 0;
        String last = null;
        for (Target t : e.to()) {
            if (!exists.test(t.block())) continue;
            acc += t.weight();
            last = t.block();
            if (r < acc) return t.block();
        }
        return last;
    }

    /**
     * The conversion budget of one chunk: {@code perInterval} blocks in each window of {@code intervalTicks}, and {@code lifetimeCap} blocks in all. The state is three longs, so the
     * chunk data can store it. Pure and monotone: asking never gives more than the limits.
     */
    public static final class Budget {
        public long windowStart;
        public int usedInWindow;
        public int total;

        public Budget() {}

        public Budget(long windowStart, int usedInWindow, int total) {
            this.windowStart = windowStart;
            this.usedInWindow = usedInWindow;
            this.total = total;
        }

        /** Takes one conversion if the limits allow it at game time {@code now}. */
        public boolean tryTake(long now, int intervalTicks, int perInterval, int lifetimeCap) {
            if (perInterval <= 0 || lifetimeCap <= 0 || total >= lifetimeCap) return false;
            if (now < windowStart || now - windowStart >= Math.max(1, intervalTicks)) {      // a new window (or the clock went back)
                windowStart = now;
                usedInWindow = 0;
            }
            if (usedInWindow >= perInterval) return false;
            usedInWindow++;
            total++;
            return true;
        }

        /** How many more this chunk may convert in its life. */
        public int lifetimeLeft(int lifetimeCap) { return Math.max(0, lifetimeCap - total); }
    }
}
