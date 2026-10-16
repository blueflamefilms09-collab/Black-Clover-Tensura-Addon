package com.newuniverse.nusmp.quests;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: FTB Quests stores its book as SNBT, and the quest pack test must prove that the generated files are readable without FTB Library on the
 * classpath. This is a port of FTB Library's SNBTParser grammar (comment lines, unquoted keys and words, newline or comma separators, typed
 * numbers with b/s/l/f/d suffixes, bare decimals as doubles, typed arrays). Compounds become LinkedHashMap, lists List, numbers the matching
 * Java box (Byte, Short, Integer, Long, Float, Double) so a test can tell a long tag from an int tag, strings String, booleans Boolean.
 */
final class QstMiniSnbt {
    private final String s;
    private int p;

    private QstMiniSnbt(String text) {
        StringBuilder b = new StringBuilder();
        for (String line : text.split("\n", -1)) {
            String t = line.strip();
            if (!t.startsWith("//") && !t.startsWith("#")) b.append(line);
            b.append('\n');
        }
        this.s = b.toString();
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> parse(String text) {
        QstMiniSnbt j = new QstMiniSnbt(text);
        return (Map<String, Object>) j.tag(j.nextNs());
    }

    private char next() {
        if (p >= s.length()) throw new IllegalArgumentException("EOF");
        return s.charAt(p++);
    }

    private char nextNs() {
        while (true) {
            char c = next();
            if (c > ' ') return c;
        }
    }

    private static boolean simple(char c) {
        return Character.isLetterOrDigit(c) || c == '.' || c == '_' || c == '-' || c == '+' || c == '∞';
    }

    private String word(char first) {
        StringBuilder b = new StringBuilder().append(first);
        while (true) {
            char c = next();
            if (simple(c)) b.append(c);
            else { p--; return b.toString(); }
        }
    }

    private String quoted(char stop) {
        StringBuilder b = new StringBuilder();
        boolean esc = false;
        while (true) {
            char c = next();
            if (c == '\n') throw new IllegalArgumentException("newline inside a string near " + p);
            if (esc) {
                esc = false;
                switch (c) {
                    case '"': b.append('"'); break;
                    case '\\': b.append('\\'); break;
                    case 't': b.append('\t'); break;
                    case 'b': b.append('\b'); break;
                    case 'n': b.append('\n'); break;
                    case 'r': b.append('\r'); break;
                    case 'f': b.append('\f'); break;
                    default: break;
                }
            } else if (c == '\\') esc = true;
            else if (c == stop) return b.toString();
            else b.append(c);
        }
    }

    private Object tag(char first) {
        switch (first) {
            case '{': return compound();
            case '[': return collection();
            case '"': return quoted('"');
            case '\'': return quoted('\'');
            default: break;
        }
        String w = word(first);
        if (w.equals("true")) return Boolean.TRUE;
        if (w.equals("false")) return Boolean.FALSE;
        return number(w);
    }

    private static boolean isInt(String x) { return x.matches("[+-]?\\d+"); }

    private static boolean isFloat(String x) { return x.matches("[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?"); }

    private static Object number(String w) {
        char last = Character.toLowerCase(w.charAt(w.length() - 1));
        String start = w.substring(0, w.length() - 1);
        if (Character.isDigit(last) && isInt(w)) {
            try { return Integer.parseInt(w); } catch (NumberFormatException e) { return w; }
        }
        if (last == 'b' && isInt(start)) return Byte.parseByte(start);
        if (last == 's' && isInt(start)) return Short.parseShort(start);
        if (last == 'l' && isInt(start)) return Long.parseLong(start);
        if (last == 'f' && isFloat(start)) return Float.parseFloat(start);
        if (last == 'd' && isFloat(start)) return Double.parseDouble(start);
        if (isFloat(w)) return Double.parseDouble(w);
        return w;
    }

    private Map<String, Object> compound() {
        Map<String, Object> m = new LinkedHashMap<>();
        while (true) {
            char c = nextNs();
            if (c == '}') return m;
            if (c == ',' || c == '\n') continue;
            String key = (c == '"' || c == '\'') ? quoted(c) : word(c);
            char n = nextNs();
            if (n != ':' && n != '=') throw new IllegalArgumentException("expected ':' near " + p);
            m.put(key, tag(nextNs()));
        }
    }

    private Object collection() {
        int prev = p;
        char n1 = nextNs();
        char n2 = nextNs();
        if (n2 == ';' && "IiLlBb".indexOf(n1) >= 0) return array();
        p = prev;
        List<Object> items = new ArrayList<>();
        while (true) {
            char c = nextNs();
            if (c == ']') return items;
            if (c == ',') continue;
            Object t = tag(c);
            if (!items.isEmpty() && items.get(0).getClass() != t.getClass())
                throw new IllegalArgumentException("mixed tag types in a list near " + p);
            items.add(t);
        }
    }

    private Object array() {
        List<Number> out = new ArrayList<>();
        while (true) {
            char c = nextNs();
            if (c == ']') return out;
            if (c == ',') continue;
            Object t = tag(c);
            if (!(t instanceof Number)) throw new IllegalArgumentException("array needs numbers near " + p);
            out.add((Number) t);
        }
    }
}
