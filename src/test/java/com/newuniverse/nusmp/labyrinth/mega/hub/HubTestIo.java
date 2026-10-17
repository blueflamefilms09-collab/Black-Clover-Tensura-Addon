package com.newuniverse.nusmp.labyrinth.mega.hub;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Why: the Hub's data tests must read its JSON files and its template WITHOUT a JSON library and without Minecraft classes (the pure tests run in the
 * same way on a bare JDK as in Gradle, and the existing data tests of the Labyrinth are written the same way). Two tiny readers are enough: a strict
 * recursive-descent JSON parser (objects become {@link LinkedHashMap}, arrays {@link ArrayList}, numbers {@link Double}, strings, booleans, null) and a
 * Java-edition NBT reader (gzip or raw; compounds become maps, lists become lists, every integer type a {@link Long}, floats {@link Double}). They live
 * in the test sources only: nothing in the mod uses them. Paths are relative to the project root, which is the working directory of Gradle's test task.
 */
final class HubTestIo {
    private HubTestIo() {}

    static final Path DATA = Path.of("src/main/resources/data/nusmp");

    static String read(Path p) throws IOException { return Files.readString(p, StandardCharsets.UTF_8); }

    // ------------------------------------------------------------------ JSON
    static Object json(Path p) throws IOException { return new Json(read(p)).parseAll(); }

    @SuppressWarnings("unchecked")
    static Map<String, Object> obj(Object o) {
        if (!(o instanceof Map)) throw new IllegalArgumentException("not a JSON object: " + o);
        return (Map<String, Object>) o;
    }

    @SuppressWarnings("unchecked")
    static List<Object> arr(Object o) {
        if (!(o instanceof List)) throw new IllegalArgumentException("not a JSON array: " + o);
        return (List<Object>) o;
    }

    private static final class Json {
        private final String s;
        private int i;

        Json(String s) { this.s = s; }

        Object parseAll() {
            Object v = value();
            ws();
            if (i != s.length()) throw new IllegalArgumentException("trailing text at " + i);
            return v;
        }

        private void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }

        private char peek() {
            ws();
            if (i >= s.length()) throw new IllegalArgumentException("unexpected end");
            return s.charAt(i);
        }

        private void expect(char c) {
            if (peek() != c) throw new IllegalArgumentException("expected '" + c + "' at " + i + " but found '" + s.charAt(i) + "'");
            i++;
        }

        private Object value() {
            char c = peek();
            if (c == '{') return object();
            if (c == '[') return array();
            if (c == '"') return string();
            if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
            if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
            if (s.startsWith("null", i)) { i += 4; return null; }
            int j = i;
            while (j < s.length() && "+-0123456789.eE".indexOf(s.charAt(j)) >= 0) j++;
            if (j == i) throw new IllegalArgumentException("bad value at " + i);
            Double d = Double.valueOf(s.substring(i, j));
            i = j;
            return d;
        }

        private Map<String, Object> object() {
            Map<String, Object> m = new LinkedHashMap<>();
            expect('{');
            if (peek() == '}') { i++; return m; }
            while (true) {
                String k = string();
                expect(':');
                if (m.put(k, value()) != null) throw new IllegalArgumentException("duplicate key " + k);
                char c = peek();
                i++;
                if (c == '}') return m;
                if (c != ',') throw new IllegalArgumentException("expected ',' or '}' at " + (i - 1));
            }
        }

        private List<Object> array() {
            List<Object> l = new ArrayList<>();
            expect('[');
            if (peek() == ']') { i++; return l; }
            while (true) {
                l.add(value());
                char c = peek();
                i++;
                if (c == ']') return l;
                if (c != ',') throw new IllegalArgumentException("expected ',' or ']' at " + (i - 1));
            }
        }

        private String string() {
            expect('"');
            StringBuilder b = new StringBuilder();
            while (true) {
                char c = s.charAt(i++);
                if (c == '"') return b.toString();
                if (c == '\\') {
                    char e = s.charAt(i++);
                    switch (e) {
                        case 'n' -> b.append('\n');
                        case 't' -> b.append('\t');
                        case 'u' -> { b.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; }
                        default -> b.append(e);
                    }
                } else b.append(c);
            }
        }
    }

    // ------------------------------------------------------------------ NBT
    static Map<String, Object> nbt(Path p) throws IOException {
        byte[] raw = Files.readAllBytes(p);
        InputStream in = new ByteArrayInputStream(raw);
        if (raw.length > 2 && (raw[0] & 0xff) == 0x1f && (raw[1] & 0xff) == 0x8b) in = new GZIPInputStream(in);
        DataInputStream d = new DataInputStream(in);
        int type = d.readByte();
        if (type != 10) throw new IOException("root tag is " + type + ", not a compound");
        d.readUTF();
        return obj(payload(d, 10));
    }

    private static Object payload(DataInputStream d, int type) throws IOException {
        switch (type) {
            case 1: return (long) d.readByte();
            case 2: return (long) d.readShort();
            case 3: return (long) d.readInt();
            case 4: return d.readLong();
            case 5: return (double) d.readFloat();
            case 6: return d.readDouble();
            case 7: { byte[] b = new byte[d.readInt()]; d.readFully(b); return b; }
            case 8: return d.readUTF();
            case 9: {
                int t = d.readByte(), n = d.readInt();
                List<Object> l = new ArrayList<>();
                for (int k = 0; k < n; k++) l.add(payload(d, t));
                return l;
            }
            case 10: {
                Map<String, Object> m = new LinkedHashMap<>();
                while (true) {
                    int t = d.readByte();
                    if (t == 0) return m;
                    String name = d.readUTF();
                    m.put(name, payload(d, t));
                }
            }
            case 11: { int n = d.readInt(); List<Object> l = new ArrayList<>(); for (int k = 0; k < n; k++) l.add((long) d.readInt()); return l; }
            case 12: { int n = d.readInt(); List<Object> l = new ArrayList<>(); for (int k = 0; k < n; k++) l.add(d.readLong()); return l; }
            default: throw new IOException("unknown tag type " + type);
        }
    }
}
