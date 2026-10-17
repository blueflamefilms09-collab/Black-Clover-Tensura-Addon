package com.newuniverse.nusmp.labyrinth.mega;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Rotation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Why: a {@link MarkerSpec} that has been placed in the world: its kind, its world position and its arguments, with every DIRECTION argument
 * ({@code facing}) already turned by the rotation of the piece it came from (the template says "north"; the piece was turned 90 degrees, so
 * the marker says "east"). Saved in the sector so the encounter director, the rest system and the expansion code never have to re-read a
 * template to find a spawn cell or a seat. Immutable.
 */
public record Marker(MarkerKind kind, BlockPos pos, Map<String, String> args) {
    public Marker {
        args = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(args));
    }

    /** The marker for a parsed spec at a world position of a piece turned by {@code rotation}. */
    public static Marker of(MarkerSpec spec, BlockPos pos, Rotation rotation) {
        Map<String, String> a = new LinkedHashMap<>(spec.args());
        String f = a.get("facing");
        if (f != null) {
            Direction d = Direction.byName(f);
            if (d != null && d.getAxis().isHorizontal()) a.put("facing", rotation.rotate(d).getName());
        }
        return new Marker(spec.kind(), pos.immutable(), a);
    }

    public String arg(String key, String def) { return args.getOrDefault(key, def); }

    public int intArg(String key, int def) {
        try { return Integer.parseInt(args.getOrDefault(key, Integer.toString(def)).trim()); } catch (NumberFormatException e) { return def; }
    }

    /** The {@code facing} argument as a horizontal direction, or null. */
    public Direction facing() {
        String f = args.get("facing");
        Direction d = f == null ? null : Direction.byName(f);
        return d != null && d.getAxis().isHorizontal() ? d : null;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("k", kind.id());
        t.putLong("p", pos.asLong());
        if (!args.isEmpty()) {
            CompoundTag a = new CompoundTag();
            for (Map.Entry<String, String> e : args.entrySet()) a.putString(e.getKey(), e.getValue());
            t.put("a", a);
        }
        return t;
    }

    /** Reads a saved marker; null when its kind is no longer known (an old world keeps loading). */
    public static Marker load(CompoundTag t) {
        MarkerKind k = MarkerKind.byId(t.getString("k"));
        if (k == null) return null;
        Map<String, String> a = new LinkedHashMap<>();
        if (t.contains("a", 10)) {
            CompoundTag c = t.getCompound("a");
            for (String key : c.getAllKeys()) a.put(key, c.getString(key));
        }
        return new Marker(k, BlockPos.of(t.getLong("p")), a);
    }
}
