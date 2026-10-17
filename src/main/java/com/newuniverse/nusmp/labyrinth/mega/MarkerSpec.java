package com.newuniverse.nusmp.labyrinth.mega;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Why: the pure half of reading a marker: a structure block's metadata string ({@code nusmp:seat;as=chair;facing=north}) split into a
 * {@link MarkerKind} and its arguments. Split from {@link Marker} (which carries a {@code BlockPos}) so the parser can be unit-tested
 * without Minecraft and so the Python checker ({@code tools/labyrinth/mega_check.py}) and the Java reader provably accept the same grammar:
 * {@code nusmp:<kind>[;key=value]*}, keys lower-case, no spaces required, a missing {@code =} gives the value {@code "true"}.
 */
public record MarkerSpec(MarkerKind kind, Map<String, String> args) {
    public MarkerSpec {
        args = Collections.unmodifiableMap(new LinkedHashMap<>(args));
    }

    /** Parses a metadata string; null when it is not a marker of ours (another namespace, an unknown kind, blank). */
    public static MarkerSpec parse(String metadata) {
        if (metadata == null) return null;
        String[] parts = metadata.trim().split(";");
        if (parts.length == 0 || !parts[0].trim().startsWith("nusmp:")) return null;
        MarkerKind kind = MarkerKind.byId(parts[0]);
        if (kind == null) return null;
        Map<String, String> args = new LinkedHashMap<>();
        for (int i = 1; i < parts.length; i++) {
            String p = parts[i].trim();
            if (p.isEmpty()) continue;
            int eq = p.indexOf('=');
            if (eq < 0) args.put(p.toLowerCase(java.util.Locale.ROOT), "true");
            else args.put(p.substring(0, eq).trim().toLowerCase(java.util.Locale.ROOT), p.substring(eq + 1).trim());
        }
        return new MarkerSpec(kind, args);
    }

    public String arg(String key, String def) { return args.getOrDefault(key, def); }

    public int intArg(String key, int def) {
        try { return Integer.parseInt(args.getOrDefault(key, Integer.toString(def)).trim()); } catch (NumberFormatException e) { return def; }
    }

    /** The metadata string this spec was (or would be) written as. */
    public String toMetadata() {
        StringBuilder b = new StringBuilder("nusmp:").append(kind.id());
        for (Map.Entry<String, String> e : args.entrySet()) b.append(';').append(e.getKey()).append('=').append(e.getValue());
        return b.toString();
    }
}
