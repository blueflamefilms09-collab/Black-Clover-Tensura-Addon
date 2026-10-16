package com.newuniverse.nusmp.quests;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the data tests of the quest line read advancement JSON without the game on the classpath, and a test should not depend on a JSON library
 * the test runtime may not carry. This is the smallest strict parser that does the job: objects become LinkedHashMap, arrays List, numbers Double
 * (or Long when integral), plus String, Boolean and null. Anything malformed throws, which is exactly what the tests want.
 */
final class QstMiniJson {
    private final String s;
    private int p;

    private QstMiniJson(String s) { this.s = s; }

    static Object parse(String text) {
        QstMiniJson j = new QstMiniJson(text);
        Object v = j.value();
        j.ws();
        if (j.p != j.s.length()) throw new IllegalArgumentException("trailing data at " + j.p);
        return v;
    }

    private void ws() {
        while (p < s.length() && Character.isWhitespace(s.charAt(p))) p++;
    }

    private Object value() {
        ws();
        if (p >= s.length()) throw new IllegalArgumentException("unexpected end");
        char c = s.charAt(p);
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

    private void expect(String w) {
        if (!s.startsWith(w, p)) throw new IllegalArgumentException("expected " + w + " at " + p);
        p += w.length();
    }

    private Map<String, Object> object() {
        Map<String, Object> m = new LinkedHashMap<>();
        p++;
        ws();
        if (s.charAt(p) == '}') { p++; return m; }
        while (true) {
            ws();
            String k = string();
            ws();
            if (s.charAt(p++) != ':') throw new IllegalArgumentException("expected : at " + p);
            m.put(k, value());
            ws();
            char c = s.charAt(p++);
            if (c == '}') return m;
            if (c != ',') throw new IllegalArgumentException("expected , or } at " + p);
        }
    }

    private List<Object> array() {
        List<Object> l = new ArrayList<>();
        p++;
        ws();
        if (s.charAt(p) == ']') { p++; return l; }
        while (true) {
            l.add(value());
            ws();
            char c = s.charAt(p++);
            if (c == ']') return l;
            if (c != ',') throw new IllegalArgumentException("expected , or ] at " + p);
        }
    }

    private String string() {
        if (s.charAt(p) != '"') throw new IllegalArgumentException("expected string at " + p);
        p++;
        StringBuilder b = new StringBuilder();
        while (true) {
            char c = s.charAt(p++);
            if (c == '"') return b.toString();
            if (c == '\\') {
                char e = s.charAt(p++);
                switch (e) {
                    case 'n': b.append('\n'); break;
                    case 't': b.append('\t'); break;
                    case 'r': b.append('\r'); break;
                    case 'b': b.append('\b'); break;
                    case 'f': b.append('\f'); break;
                    case 'u': b.append((char) Integer.parseInt(s.substring(p, p + 4), 16)); p += 4; break;
                    default: b.append(e);
                }
            } else {
                b.append(c);
            }
        }
    }

    private Object number() {
        int st = p;
        while (p < s.length() && "+-0123456789.eE".indexOf(s.charAt(p)) >= 0) p++;
        if (st == p) throw new IllegalArgumentException("unexpected character at " + p);
        String t = s.substring(st, p);
        if (t.matches("-?\\d+")) return Long.parseLong(t);
        return Double.parseDouble(t);
    }
}
