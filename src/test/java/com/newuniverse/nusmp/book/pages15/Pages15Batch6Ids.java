package com.newuniverse.nusmp.book.pages15;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Why: the older character-set tests (CharacterSetBullsTest, CharacterSetGoldTest, CharacterSetRestSourceScanTest) check that every page a character file names exists in the book by reading the book's own
 * sources; the pages that batch 6 of the "15 spells" project appends live in {@code Batch6.java}, so those tests ask this helper for them (pure, regular expressions over the source, no Minecraft class).
 */
public final class Pages15Batch6Ids {
    private Pages15Batch6Ids() {}

    private static final Path BATCH = Path.of("src/main/java/com/newuniverse/nusmp/book/pages15/Batch6.java");

    /** The page ids Batch6 appends to {@code book} (registry path such as {@code book_spatial}), in written order; empty for any other book. */
    public static List<String> of(String book) {
        try {
            String code = Files.readString(BATCH, StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)(?<!:)//.*$", "");
            Matcher reg = Pattern.compile("register\\(\\s*\"" + Pattern.quote(book) + "\"\\s*,").matcher(code);
            List<String> out = new ArrayList<>();
            if (!reg.find()) return out;
            int depth = 0, i = code.indexOf('(', reg.start()), end = code.length();
            for (int k = i; k < code.length(); k++) {
                char ch = code.charAt(k);
                if (ch == '"') {
                    for (k++; k < code.length() && code.charAt(k) != '"'; k++) if (code.charAt(k) == '\\') k++;
                } else if (ch == '(') depth++;
                else if (ch == ')' && --depth == 0) { end = k; break; }
            }
            Matcher d = Pattern.compile("\\b(?:starter|mid|zone|signature)\\(\\s*\"([a-z0-9_]+)\"").matcher(code.substring(reg.end(), end));
            while (d.find()) out.add(d.group(1));
            return out;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
