package com.newuniverse.nusmp.labyrinth.mega.theme;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the data tests of the Mega-Dungeon read the shipped JSON files, and the project's rule for data tests is "no JSON library on the test class
 * path" (the Gson and Codec classes need the game's libraries). This is the smallest reader that is correct for the files we write: objects become
 * {@code LinkedHashMap}, arrays {@code ArrayList}, strings {@code String} (with the escapes JSON defines), integers {@code Long}, other numbers
 * {@code Double}, booleans {@code Boolean}, null {@code null}. It is the tree {@code ThemeDef.parse} and {@code SpiritRoster.parse} consume, so the
 * very bytes that ship are parsed by the very code that runs in game, minus only the conversion from Gson.
 */
public final class MiniJson {
    private final String s;
    private int i;

    private MiniJson(String s) { this.s = s; }

    /** Parses a complete JSON document; throws IllegalArgumentException on anything else. */
    public static Object parse(String text) {
        MiniJson p = new MiniJson(text);
        p.ws();
        Object v = p.value();
        p.ws();
        if (p.i != p.s.length()) throw p.error("trailing characters");
        return v;
    }

    private IllegalArgumentException error(String what) { return new IllegalArgumentException(what + " at offset " + i); }

    private void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }

    private char peek() {
        if (i >= s.length()) throw error("unexpected end");
        return s.charAt(i);
    }

    private Object value() {
        char c = peek();
        switch (c) {
            case '{': return object();
            case '[': return array();
            case '"': return string();
            case 't': expect("true"); return Boolean.TRUE;
            case 'f': expect("false"); return Boolean.FALSE;
            case 'n': expect("null"); return null;
            default: return number();
        }
    }

    private void expect(String word) {
        if (!s.startsWith(word, i)) throw error("expected " + word);
        i += word.length();
    }

    private Map<String, Object> object() {
        Map<String, Object> m = new LinkedHashMap<>();
        i++;
        ws();
        if (peek() == '}') { i++; return m; }
        while (true) {
            ws();
            if (peek() != '"') throw error("expected a key");
            String k = string();
            ws();
            if (peek() != ':') throw error("expected ':'");
            i++;
            ws();
            m.put(k, value());
            ws();
            char c = peek();
            i++;
            if (c == '}') return m;
            if (c != ',') throw error("expected ',' or '}'");
        }
    }

    private List<Object> array() {
        List<Object> l = new ArrayList<>();
        i++;
        ws();
        if (peek() == ']') { i++; return l; }
        while (true) {
            ws();
            l.add(value());
            ws();
            char c = peek();
            i++;
            if (c == ']') return l;
            if (c != ',') throw error("expected ',' or ']'");
        }
    }

    private String string() {
        StringBuilder b = new StringBuilder();
        i++;
        while (true) {
            char c = peek();
            i++;
            if (c == '"') return b.toString();
            if (c != '\\') { b.append(c); continue; }
            char e = peek();
            i++;
            switch (e) {
                case '"': b.append('"'); break;
                case '\\': b.append('\\'); break;
                case '/': b.append('/'); break;
                case 'b': b.append('\b'); break;
                case 'f': b.append('\f'); break;
                case 'n': b.append('\n'); break;
                case 'r': b.append('\r'); break;
                case 't': b.append('\t'); break;
                case 'u':
                    if (i + 4 > s.length()) throw error("short \\u escape");
                    b.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                    i += 4;
                    break;
                default: throw error("bad escape \\" + e);
            }
        }
    }

    private Object number() {
        int start = i;
        while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
        if (start == i) throw error("unexpected character '" + s.charAt(i) + "'");
        String t = s.substring(start, i);
        if (t.indexOf('.') < 0 && t.indexOf('e') < 0 && t.indexOf('E') < 0) return Long.parseLong(t);
        return Double.parseDouble(t);
    }
}
