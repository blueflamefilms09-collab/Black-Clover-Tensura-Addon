package com.newuniverse.nusmp.core.magic.merge;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Pure parsing of the tail of {@code /multiverse magic merge <a> <b> [<c> [<d>]] [name]}: after the first two parents, each word that
 * names a magic is another parent (up to four in all); the first word that does not starts the name and takes everything after it. A
 * word in double quotes always starts the name, so a name that is also a magic's name ("Soul") can still be given: {@code merge flame
 * water "Soul"}.
 */
public final class MergeArgs {
    private MergeArgs() {}

    /** @param parents the parents in the order typed (the first two always, then those that follow); name may be null */
    public record Parsed(List<String> parents, String name) {}

    /** Splits on spaces; a double-quoted run is one token (kept with its quotes so the caller knows it was quoted). */
    public static List<String> tokenize(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) return out;
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"') { quoted = !quoted; cur.append(c); }
            else if (c == ' ' && !quoted) { if (cur.length() > 0) { out.add(cur.toString()); cur.setLength(0); } }
            else cur.append(c);
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }

    private static boolean quoted(String t) { return t.startsWith("\""); }

    private static String unquote(String t) {
        String s = t;
        if (s.startsWith("\"")) s = s.substring(1);
        if (s.endsWith("\"")) s = s.substring(0, s.length() - 1);
        return s;
    }

    /** @param a the first parent, @param b the second, @param rest the remaining text (may be null or empty) */
    public static Parsed parse(String a, String b, String rest, Predicate<String> isMagic) {
        List<String> parents = new ArrayList<>();
        parents.add(a);
        parents.add(b);
        List<String> tokens = tokenize(rest);
        int i = 0;
        while (i < tokens.size() && parents.size() < MergeLimits.MAX_PARENTS && !quoted(tokens.get(i)) && isMagic.test(tokens.get(i))) parents.add(tokens.get(i++));
        StringBuilder name = new StringBuilder();
        for (; i < tokens.size(); i++) {
            if (name.length() > 0) name.append(' ');
            name.append(unquote(tokens.get(i)));
        }
        String n = name.toString().trim();
        return new Parsed(parents, n.isEmpty() ? null : n);
    }
}
