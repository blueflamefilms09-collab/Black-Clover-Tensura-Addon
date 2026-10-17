package com.newuniverse.nusmp.labyrinth.mega.gear;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the gear tests read the REAL shipped table ({@code data/nusmp/mega/gear/tables.json}) so a typo in it fails the build, and no JSON library is on
 * the test class path of this project (the other data tests carry their own reader for the same reason; this is a copy of the faction tests' reader,
 * because that one is package-private). It is the smallest reader that does the job: objects (insertion ordered), arrays, strings with the usual
 * escapes, numbers (as {@code Double}), booleans and null, producing exactly the tree shape {@link GearTablesParser} takes. It throws
 * {@link IllegalArgumentException} with the offset on bad input, so a broken file fails with a message that says where.
 */
final class MiniJson {
    private final String s;
    private int i;

    private MiniJson(String s) { this.s = s; }

    /** Parses a whole document; trailing characters are an error. */
    static Object parse(String text) {
        MiniJson r = new MiniJson(text);
        Object v = r.value();
        r.ws();
        if (r.i != text.length()) throw new IllegalArgumentException("trailing characters after the JSON value at " + r.i);
        return v;
    }

    /** Parses a document that must be an object. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> object(String text) {
        Object v = parse(text);
        if (!(v instanceof Map)) throw new IllegalArgumentException("expected a JSON object but got " + v);
        return (Map<String, Object>) v;
    }

    private void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }

    private char peek() {
        ws();
        if (i >= s.length()) throw new IllegalArgumentException("unexpected end of JSON");
        return s.charAt(i);
    }

    private void expect(char c) {
        if (peek() != c) throw new IllegalArgumentException("expected '" + c + "' at " + i);
        i++;
    }

    private Object value() {
        char c = peek();
        if (c == '{') {
            i++;
            Map<String, Object> m = new LinkedHashMap<>();
            if (peek() == '}') { i++; return m; }
            while (true) {
                String k = string();
                expect(':');
                if (m.put(k, value()) != null) throw new IllegalArgumentException("duplicate key '" + k + "' before " + i);
                if (peek() == ',') { i++; continue; }
                expect('}');
                return m;
            }
        }
        if (c == '[') {
            i++;
            List<Object> l = new ArrayList<>();
            if (peek() == ']') { i++; return l; }
            while (true) {
                l.add(value());
                if (peek() == ',') { i++; continue; }
                expect(']');
                return l;
            }
        }
        if (c == '"') return string();
        if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
        if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
        if (s.startsWith("null", i)) { i += 4; return null; }
        int st = i;
        while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
        if (i == st) throw new IllegalArgumentException("bad JSON value at " + st);
        return Double.valueOf(s.substring(st, i));
    }

    private String string() {
        expect('"');
        StringBuilder b = new StringBuilder();
        while (true) {
            if (i >= s.length()) throw new IllegalArgumentException("unterminated string");
            char c = s.charAt(i++);
            if (c == '"') return b.toString();
            if (c != '\\') { b.append(c); continue; }
            char e = s.charAt(i++);
            switch (e) {
                case 'n' -> b.append('\n');
                case 't' -> b.append('\t');
                case 'r' -> b.append('\r');
                case 'b' -> b.append('\b');
                case 'f' -> b.append('\f');
                case 'u' -> { b.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; }
                default -> b.append(e);
            }
        }
    }
}
