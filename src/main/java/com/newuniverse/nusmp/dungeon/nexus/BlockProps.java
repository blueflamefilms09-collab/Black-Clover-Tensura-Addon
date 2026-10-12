package com.newuniverse.nusmp.dungeon.nexus;

import java.util.ArrayList;
import java.util.List;

/**
 * Block properties as the plan writes them: "key=value,key=value". Pure string work, shared by the planner (which turns a facing as a
 * frame mirrors or swaps it) and the glue (which sets the properties on a block state).
 */
public final class BlockProps {
    private BlockProps() {}

    /** The horizontal facings in the order 0 north, 1 east, 2 south, 3 west (the order {@link NexusOps#face} uses). */
    public static final String[] FACINGS = {"north", "east", "south", "west"};

    /** The key/value pairs, in order; empty and malformed parts are skipped. */
    public static List<String[]> parse(String props) {
        List<String[]> out = new ArrayList<>();
        if (props == null || props.isEmpty()) return out;
        for (String part : props.split(",")) {
            int eq = part.indexOf('=');
            if (eq <= 0 || eq == part.length() - 1) continue;
            out.add(new String[]{part.substring(0, eq).trim(), part.substring(eq + 1).trim()});
        }
        return out;
    }

    /** The index of a horizontal facing name (0 north to 3 west), or -1. */
    public static int facingIndex(String name) {
        for (int i = 0; i < FACINGS.length; i++) if (FACINGS[i].equals(name)) return i;
        return -1;
    }

    /** The properties with a horizontal "facing" turned by the frame ({@code faceMap[old index] = new index}) and an "axis" of x or z swapped if swapAxes. */
    public static String remap(String props, int[] faceMap, boolean swapAxes) {
        if (props == null || props.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String[] kv : parse(props)) {
            String v = kv[1];
            if (kv[0].equals("facing")) {
                int f = facingIndex(v);
                if (f >= 0) v = FACINGS[faceMap[f]];
            } else if (swapAxes && kv[0].equals("axis")) {
                v = v.equals("x") ? "z" : v.equals("z") ? "x" : v;
            }
            if (sb.length() > 0) sb.append(',');
            sb.append(kv[0]).append('=').append(v);
        }
        return sb.toString();
    }
}
