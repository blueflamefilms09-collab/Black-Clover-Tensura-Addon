package com.newuniverse.nusmp.compat.maps;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Why: FTB Chunks lets players claim chunks, and a claimed chunk keeps other players from using blocks and entities in it. Inside this mod's dimensions (the Hub above
 * all, which is not instanced) that would lock altars and NPCs for everybody else. FTB Chunks' own switch is the server setting {@code claim_dimension_blacklist}; this
 * mod cannot (and does not) write another mod's config, so this class only READS it as plain text and reports whether the mod's dimensions are covered, so the server log
 * and {@code /multiverse compat maps} can tell an op what to add. Every failure is a quiet "unknown". PURE except for {@link #find}, which reads files.
 *
 * <p>Certainty: the key name {@code claim_dimension_blacklist} and the SNBT list shape are from memory of FTB Chunks' config; the file name {@code ftbchunks-world.snbt}
 * and its folders are the usual FTB layout ({@code <world>/serverconfig}, {@code <game>/config/ftbchunks}, {@code <game>/defaultconfigs}), not verified. A
 * {@code claim_dimension_whitelist} is honoured if present (an assumption). Wildcard entries such as {@code nusmp:*} are understood by THIS check; whether FTB Chunks
 * itself understands them is not known, so exact ids are what the warning recommends.
 */
public final class FtbChunksCheck {
    private FtbChunksCheck() {}

    public static final String FILE = "ftbchunks-world.snbt";
    public static final String BLACKLIST_KEY = "claim_dimension_blacklist", WHITELIST_KEY = "claim_dimension_whitelist";

    public enum Status {
        /** Every dimension asked about is blocked from claims (listed in the blacklist, or left out of a non-empty whitelist). */
        COVERED,
        /** The file was read and at least one dimension can still be claimed. */
        OPEN,
        /** No file found or not readable: nothing can be said. */
        UNKNOWN
    }

    public record Result(Status status, List<String> uncovered, String source) {}

    public static Result unknown() { return new Result(Status.UNKNOWN, List.of(), ""); }

    // ---------------------------------------------------------------- analysis (pure)
    /** The string entries of the list under {@code key} in SNBT-ish text; null when the key is not there. Quotes, commas and {@code //} comments are tolerated. */
    public static List<String> listOf(String snbt, String key) {
        if (snbt == null) return null;
        String text = stripComments(snbt);
        Matcher m = Pattern.compile("(?<![\\w])\"?" + Pattern.quote(key) + "\"?\\s*[:=]\\s*\\[").matcher(text);
        if (!m.find()) return null;
        int depth = 1, i = m.end();
        StringBuilder body = new StringBuilder();
        for (; i < text.length() && depth > 0; i++) {
            char c = text.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') { depth--; if (depth == 0) break; }
            body.append(c);
        }
        List<String> out = new ArrayList<>();
        for (String tok : body.toString().replace('"', ' ').replace('\'', ' ').replace(',', ' ').trim().split("\\s+")) if (!tok.isEmpty()) out.add(tok);
        return out;
    }

    private static String stripComments(String s) {
        StringBuilder b = new StringBuilder();
        for (String line : s.split("\\R", -1)) {
            int c = line.indexOf("//");
            b.append(c >= 0 ? line.substring(0, c) : line).append('\n');
        }
        return b.toString();
    }

    /** Does a list entry (exact id, {@code namespace:*}, {@code namespace*}, {@code *}) match the dimension id? */
    public static boolean matches(String entry, String dimensionId) {
        if (entry == null || dimensionId == null) return false;
        if (entry.equals(dimensionId) || entry.equals("*") || entry.equals("*:*")) return true;
        if (entry.endsWith(":*")) return dimensionId.startsWith(entry.substring(0, entry.length() - 1));
        if (entry.endsWith("*")) return dimensionId.startsWith(entry.substring(0, entry.length() - 1));
        return false;
    }

    /** Which of {@code dimensions} can still be claimed according to the file text. */
    public static Result analyse(String snbt, List<String> dimensions, String source) {
        if (snbt == null) return unknown();
        List<String> black = listOf(snbt, BLACKLIST_KEY), white = listOf(snbt, WHITELIST_KEY);
        List<String> open = new ArrayList<>();
        for (String d : dimensions) {
            boolean blocked = false;
            if (black != null) for (String e : black) if (matches(e, d)) { blocked = true; break; }
            if (!blocked && white != null && !white.isEmpty()) {
                boolean inWhite = false;
                for (String e : white) if (matches(e, d)) { inWhite = true; break; }
                blocked = !inWhite;
            }
            if (!blocked) open.add(d);
        }
        return new Result(open.isEmpty() ? Status.COVERED : Status.OPEN, open, source);
    }

    /** The line an operator should have in FTB Chunks' config to block claims in these dimensions (exact ids, the form the check is surest of). */
    public static String suggestion(List<String> dimensions) {
        StringBuilder b = new StringBuilder(BLACKLIST_KEY).append(": [");
        for (int i = 0; i < dimensions.size(); i++) b.append(i == 0 ? " " : ", ").append('"').append(dimensions.get(i)).append('"');
        return b.append(" ]").toString();
    }

    // ---------------------------------------------------------------- files (soft)
    /** The places FTB Chunks' world config may be, most specific first. */
    public static List<Path> candidates(Path worldDir, Path gameDir) {
        Set<Path> out = new LinkedHashSet<>();
        if (worldDir != null) out.add(worldDir.resolve("serverconfig").resolve(FILE));
        if (gameDir != null) {
            out.add(gameDir.resolve("config").resolve("ftbchunks").resolve(FILE));
            out.add(gameDir.resolve("config").resolve(FILE));
            out.add(gameDir.resolve("defaultconfigs").resolve(FILE));
            out.add(gameDir.resolve("defaultconfigs").resolve("ftbchunks").resolve(FILE));
        }
        return new ArrayList<>(out);
    }

    /** Reads the first candidate that exists; never throws. */
    public static Optional<String[]> find(Path worldDir, Path gameDir) {
        for (Path p : candidates(worldDir, gameDir)) {
            try {
                if (Files.isRegularFile(p)) return Optional.of(new String[]{p.toString(), Files.readString(p, StandardCharsets.UTF_8)});
            } catch (IOException | RuntimeException ignored) {
                // unreadable: try the next place
            }
        }
        return Optional.empty();
    }

    /** Reads the config on disk and judges it for {@code dimensions}. */
    public static Result check(Path worldDir, Path gameDir, List<String> dimensions) {
        Optional<String[]> f = find(worldDir, gameDir);
        return f.isEmpty() ? unknown() : analyse(f.get()[1], dimensions, f.get()[0]);
    }
}
