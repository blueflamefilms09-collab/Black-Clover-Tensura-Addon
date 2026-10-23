package com.newuniverse.nusmp.charset;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Why: the character-set tests pin the page list of their books (the pages a book had before the "15 spells for every grimoire" project), but the project's batches append shared pages
 * to those books ({@code book/pages15/Batch1..8.java}) and the owner wants the character grimoires to list them. This reads the ids a batch registers for a book straight from the sources, so
 * a set file may name them. Pure (java.nio, regular expressions); the working directory is the project root. Test helper only, nothing in the game uses it.
 */
final class Pages15Added {
    private Pages15Added() {}

    private static final Path DIR = Path.of("").toAbsolutePath().resolve("src/main/java/com/newuniverse/nusmp/book/pages15");
    private static final Pattern ID = Pattern.compile("Pages15Kit\\.(?:starter|mid|zone|signature)\\(\\s*\"([a-z0-9_]+)\"");

    /** The page ids the batches append to {@code book}, in order (empty when none). */
    static List<String> ids(String book) throws IOException {
        List<String> out = new ArrayList<>();
        for (int b = 1; b <= 8; b++) {
            Path f = DIR.resolve("Batch" + b + ".java");
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
            Matcher m = ID.matcher(c.substring(reg.end(), end));
            while (m.find()) out.add(m.group(1));
        }
        return out;
    }
}
