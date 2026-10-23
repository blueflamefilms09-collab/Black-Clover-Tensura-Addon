package com.newuniverse.nusmp.book.pages15;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test helper (no test inside): the page ids the "15 spells for every grimoire" batches append to a book, read from the {@code register("book_x", ...)} blocks of {@code book/pages15/Batch1..8.java}.
 * The character set scan tests use it so that a set may list a page a batch added (a set file naming a page of the book must find it).
 */
public final class Pages15Added {
    private Pages15Added() {}

    private static final Pattern DEF = Pattern.compile("\\b(?:starter|mid|zone|signature)\\(\\s*\"([a-z0-9_]+)\"");

    /** The ids the batches append to {@code book}, in written order (empty when none). */
    public static List<String> of(String book) throws IOException {
        List<String> out = new ArrayList<>();
        Path dir = Path.of("").toAbsolutePath().resolve("src/main/java/com/newuniverse/nusmp/book/pages15");
        for (int n = 1; n <= 8; n++) {
            Path f = dir.resolve("Batch" + n + ".java");
            if (!Files.isRegularFile(f)) continue;
            String c = Files.readString(f, StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)(?<!:)//.*$", "");
            Matcher reg = Pattern.compile("register\\(\\s*\"" + Pattern.quote(book) + "\"\\s*,").matcher(c);
            if (!reg.find()) continue;
            int open = c.indexOf('(', reg.start()), depth = 0, end = c.length();
            for (int i = open; i < c.length(); i++) {
                char ch = c.charAt(i);
                if (ch == '"') {
                    for (i++; i < c.length() && c.charAt(i) != '"'; i++) if (c.charAt(i) == '\\') i++;
                } else if (ch == '(') depth++;
                else if (ch == ')' && --depth == 0) { end = i; break; }
            }
            Matcher d = DEF.matcher(c.substring(reg.end(), end));
            while (d.find()) out.add(d.group(1));
        }
        return out;
    }
}
