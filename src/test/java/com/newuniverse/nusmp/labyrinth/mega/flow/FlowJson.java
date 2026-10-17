package com.newuniverse.nusmp.labyrinth.mega.flow;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the flow tests read the REAL data file ({@code alignments.json}) the way the game's loader would, so a typo in the shipped file fails the
 * build, and no JSON library is on the test class path of this project. This is the smallest strict reader that does the job: objects (insertion
 * ordered, a repeated key is an error), arrays, strings with the usual escapes, numbers (as {@code Double}), booleans and null, producing the same
 * tree shape the game-side loader hands to {@link MagicAlignments#parse}. It throws {@link IllegalArgumentException} with the offset on bad input.
 */
final class FlowJson {
    private final String s;
    private int i;

    private FlowJson(String s) { this.s = s; }

    /** Parses a document that must be a JSON object. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> object(String text) {
        FlowJson r = new FlowJson(text);
        Object v = r.value();
        r.skip();
        if (r.i != text.length()) throw new IllegalArgumentException("trailing characters at " + r.i);
        if (!(v instanceof Map)) throw new IllegalArgumentException("expected an object but got " + v);
        return (Map<String, Object>) v;
    }

    private void skip() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }

    private char peek() {
        skip();
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
        int start = i;
        while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
        if (start == i) throw new IllegalArgumentException("unexpected character '" + c + "' at " + i);
        return Double.valueOf(s.substring(start, i));
    }

    private String string() {
        expect('"');
        StringBuilder b = new StringBuilder();
        while (i < s.length()) {
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
        throw new IllegalArgumentException("unterminated string");
    }
}
