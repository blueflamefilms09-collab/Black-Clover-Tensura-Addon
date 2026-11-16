package com.newuniverse.nusmp.bossdrops;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.regex.Pattern;

/**
 * Why: the drops of a boss are DATA ({@code data/anime_multiverse/boss_drops/<boss_id>.json}), so an operator or a data pack changes them without code. This is the pure half (no Minecraft type, unit
 * tested): it reads one file into a {@link Table}, refuses a bad one with a message that names the row, and rolls a table against a random source. Bounds: at most {@link #MAX_ROWS} rows, a count of
 * at most {@link #MAX_COUNT} a row, and at most {@code max_stacks} stacks a kill ({@link #HARD_MAX_STACKS} at the very most), so a file can never make a kill pay an unbounded list.
 * <pre>
 * { "boss": "meliodas", "max_stacks": 6,
 *   "drops": [ { "item": "anime_multiverse:trophy_meliodas", "chance": 1.0, "min": 1, "max": 2, "first_kill_only": false } ] }
 * </pre>
 * {@code killer_only} is always true (the player of the last blow gets the items, see {@link BossDropEvents}); it is accepted in a file and must not be false.
 */
public final class BossDropTable {
    private BossDropTable() {}

    public static final int MAX_ROWS = 12, MAX_COUNT = 64, DEFAULT_MAX_STACKS = 6, HARD_MAX_STACKS = 16;
    private static final Pattern ITEM_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    public record Row(String item, double chance, int min, int max, boolean firstKillOnly) {}

    public record Stack(String item, int count) {}

    public record Table(String boss, int maxStacks, List<Row> rows) {}

    /** Reads one file; {@code IllegalArgumentException} (with the reason) when it is not valid. */
    public static Table parse(JsonObject o) {
        if (o == null || !o.has("boss") || !o.get("boss").isJsonPrimitive()) throw new IllegalArgumentException("missing 'boss'");
        String boss = o.get("boss").getAsString();
        if (boss.isBlank() || !boss.matches("[a-z0-9_]+")) throw new IllegalArgumentException("bad boss id '" + boss + "'");
        int maxStacks = o.has("max_stacks") ? o.get("max_stacks").getAsInt() : DEFAULT_MAX_STACKS;
        if (maxStacks < 1 || maxStacks > HARD_MAX_STACKS) throw new IllegalArgumentException(boss + ": max_stacks must be 1.." + HARD_MAX_STACKS);
        if (!o.has("drops") || !o.get("drops").isJsonArray()) throw new IllegalArgumentException(boss + ": missing 'drops'");
        JsonArray arr = o.getAsJsonArray("drops");
        if (arr.isEmpty() || arr.size() > MAX_ROWS) throw new IllegalArgumentException(boss + ": 1.." + MAX_ROWS + " drops");
        List<Row> rows = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            JsonElement e = arr.get(i);
            String at = boss + " drop " + i;
            if (!e.isJsonObject()) throw new IllegalArgumentException(at + ": not an object");
            JsonObject r = e.getAsJsonObject();
            String item = r.has("item") ? r.get("item").getAsString() : "";
            if (!ITEM_ID.matcher(item).matches()) throw new IllegalArgumentException(at + ": bad item id '" + item + "'");
            double chance = r.has("chance") ? r.get("chance").getAsDouble() : 1.0;
            if (!(chance > 0.0 && chance <= 1.0)) throw new IllegalArgumentException(at + ": chance must be in (0, 1]");
            int min = r.has("min") ? r.get("min").getAsInt() : 1, max = r.has("max") ? r.get("max").getAsInt() : min;
            if (min < 1 || max < min || max > MAX_COUNT) throw new IllegalArgumentException(at + ": need 1 <= min <= max <= " + MAX_COUNT);
            if (r.has("killer_only") && !r.get("killer_only").getAsBoolean()) throw new IllegalArgumentException(at + ": killer_only cannot be false");
            rows.add(new Row(item, chance, min, max, r.has("first_kill_only") && r.get("first_kill_only").getAsBoolean()));
        }
        return new Table(boss, maxStacks, List.copyOf(rows));
    }

    /** The most item pieces (stacks given or dropped) one kill can create, whatever the file says. */
    public static final int MAX_PIECES = 32;

    /** A count cut into stacks of at most {@code stackSize}, never more than {@code room} pieces (so a kill can never spawn an unbounded list). */
    public static List<Integer> split(int count, int stackSize, int room) {
        List<Integer> out = new ArrayList<>();
        int size = Math.max(1, stackSize);
        for (int left = Math.max(0, count); left > 0 && out.size() < room; left -= size) out.add(Math.min(left, size));
        return out;
    }

    /**
     * What one kill pays: each row in file order, a row whose chance (times {@code chanceScale}, never above 1) comes up adds a stack of min..max, a first-kill-only row only when {@code firstKill};
     * the list stops at the table's {@code maxStacks}. Pure: the random source is a parameter.
     */
    public static List<Stack> roll(Table t, boolean firstKill, double chanceScale, DoubleSupplier rng) {
        List<Stack> out = new ArrayList<>();
        for (Row r : t.rows()) {
            if (out.size() >= t.maxStacks()) break;
            if (r.firstKillOnly() && !firstKill) continue;
            if (rng.getAsDouble() >= Math.min(1.0, r.chance() * Math.max(0.0, chanceScale))) continue;
            int n = r.min() + (int) Math.floor(rng.getAsDouble() * (r.max() - r.min() + 1));
            out.add(new Stack(r.item(), Math.min(r.max(), n)));
        }
        return out;
    }
}
