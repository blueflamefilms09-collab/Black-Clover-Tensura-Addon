package com.newuniverse.nusmp.mesh;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * 3.32.0: the compact binary form of the big mesh files (weapon_meshes*, djinn). A packed file keeps its path and its ".txt" name; the first five bytes are
 * {@code N M S H 1} and what follows is one deflate stream. {@link #text(byte[])} turns either form back into the text the existing parsers read, so every
 * loader changes only its open call and plain text files (all of them in the source tree and in the tests) keep working.
 *
 * <p>What is packed: the two quad dialects of the weapon / djinn meshes, {@code qn layer (x y z u v nx ny nz) * 4} and {@code q layer (x y z u v shade) * 4}.
 * Each distinct vertex is stored once per file (a quad then names four vertices by index) as integers at the smallest power-of-ten scale that holds the whole
 * column exactly (so no value is rounded), column by column with the difference to the line above. Every other line (comments, {@code place}, {@code g}, ...)
 * and every quad line whose numbers do not fit that shape is stored as its own text, in order. The only change a round trip makes is the spelling of a number:
 * the same value with the column's fixed number of decimals, and {@code -0.000} comes back as {@code 0.000} (equal as a float).
 *
 * <p>No Minecraft classes: it runs in the build (the jar task) and in the game.
 */
public final class MeshPack {
    private static final byte[] MAGIC = {'N', 'M', 'S', 'H', 1};
    private static final int MAX_DECIMALS = 4;
    private static final int MAX_INT_DIGITS = 8;

    /** A quad dialect: the keyword, the numbers per vertex and the numbers after the four vertices (the flat normal of a {@code q} line). */
    private enum Kind {
        QN("qn", 8, 0), Q("q", 5, 3);

        final String word;
        final int cols, extras, tokens;

        Kind(String word, int cols, int extras) {
            this.word = word;
            this.cols = cols;
            this.extras = extras;
            this.tokens = 2 + 4 * cols + extras;
        }

        int numbers() { return 4 * cols + extras; }

        /** Statistics columns: the vertex columns, then the extras. */
        int columns() { return cols + extras; }

        int column(int number) { return number < 4 * cols ? number % cols : cols + (number - 4 * cols); }
    }

    private static final int OP_END = 0, OP_TEXT = 1, OP_QUAD = 2;                       // OP_QUAD + kind ordinal

    private MeshPack() {}

    public static boolean isPacked(byte[] b) {
        if (b == null || b.length < MAGIC.length) return false;
        for (int i = 0; i < MAGIC.length; i++) if (b[i] != MAGIC[i]) return false;
        return true;
    }

    /** The text of a mesh file, whether it is stored packed or as plain UTF-8. */
    public static String text(byte[] b) throws IOException {
        return isPacked(b) ? unpack(b) : new String(b, StandardCharsets.UTF_8);
    }

    // ---------------------------------------------------------------- pack
    /** How many quad lines of the packable dialects the text holds (the build packs a file only when this is worth it). */
    public static int packableQuads(String text) {
        int n = 0;
        for (String line : lines(text)) if (parse(line) != null) n++;
        return n;
    }

    public static byte[] pack(String text) {
        List<String> lines = lines(text);
        List<Quad> quads = new ArrayList<>(lines.size());
        int[][] decimals = new int[Kind.values().length][];
        for (Kind k : Kind.values()) decimals[k.ordinal()] = new int[k.columns()];
        for (String line : lines) {
            Quad q = parse(line);
            quads.add(q);
            if (q == null) continue;
            for (int n = 0; n < q.kind.numbers(); n++) {
                int c = q.kind.column(n);
                decimals[q.kind.ordinal()][c] = Math.max(decimals[q.kind.ordinal()][c], q.dec[n]);
            }
        }
        // vertex tables (one per dialect), vertices numbered in order of first use
        List<long[]>[] table = newTables();
        Map<Key, Integer>[] index = newIndexes();
        ByteArrayOutputStream ops = new ByteArrayOutputStream(Math.max(1024, text.length() / 8));
        for (int i = 0; i < lines.size(); i++) {
            Quad q = quads.get(i);
            if (q == null) {
                byte[] raw = lines.get(i).getBytes(StandardCharsets.UTF_8);
                ops.write(OP_TEXT);
                varint(ops, raw.length);
                ops.write(raw, 0, raw.length);
                continue;
            }
            int ord = q.kind.ordinal();
            ops.write(OP_QUAD + ord);
            varint(ops, q.layer);
            for (int v = 0; v < 4; v++) {
                long[] vert = new long[q.kind.cols];
                for (int c = 0; c < q.kind.cols; c++) vert[c] = q.scaled(v * q.kind.cols + c, decimals[ord][c]);
                Key key = new Key(vert);
                Integer at = index[ord].get(key);
                if (at == null) {
                    at = table[ord].size();
                    index[ord].put(key, at);
                    table[ord].add(vert);
                    varint(ops, 0);                                                      // a new vertex is the next one: zero distance
                } else {
                    varint(ops, zig((long) at - (table[ord].size())));                   // an old one: how far back from the next new index
                }
            }
            for (int e = 0; e < q.kind.extras; e++) varint(ops, zig(q.scaled(4 * q.kind.cols + e, decimals[ord][q.kind.cols + e])));
        }
        ops.write(OP_END);

        ByteArrayOutputStream body = new ByteArrayOutputStream(ops.size() + 1024);
        for (Kind k : Kind.values()) {
            int ord = k.ordinal();
            for (int c = 0; c < k.columns(); c++) body.write(decimals[ord][c]);
            varint(body, table[ord].size());
            for (int c = 0; c < k.cols; c++) {
                long prev = 0;
                for (long[] vert : table[ord]) {
                    varint(body, zig(vert[c] - prev));
                    prev = vert[c];
                }
            }
        }
        byte[] opBytes = ops.toByteArray();
        body.write(opBytes, 0, opBytes.length);
        return deflate(body.toByteArray());
    }

    // ---------------------------------------------------------------- unpack
    public static String unpack(byte[] packed) throws IOException {
        if (!isPacked(packed)) throw new IOException("not a packed mesh (no NMSH header)");
        byte[] body = inflate(packed, MAGIC.length);
        int[] pos = {0};
        long[][][] table = new long[Kind.values().length][][];
        int[][] decimals = new int[Kind.values().length][];
        for (Kind k : Kind.values()) {
            int ord = k.ordinal();
            decimals[ord] = new int[k.columns()];
            for (int c = 0; c < k.columns(); c++) {
                decimals[ord][c] = body[pos[0]++];
                if (decimals[ord][c] < 0 || decimals[ord][c] > MAX_DECIMALS) throw new IOException("bad decimals " + decimals[ord][c]);
            }
            int n = (int) readVarint(body, pos);
            if (n < 0 || n > body.length) throw new IOException("bad vertex count " + n);
            table[ord] = new long[n][k.cols];
            for (int c = 0; c < k.cols; c++) {
                long prev = 0;
                for (int i = 0; i < n; i++) {
                    prev += unzig(readVarint(body, pos));
                    table[ord][i][c] = prev;
                }
            }
        }
        StringBuilder out = new StringBuilder(body.length * 6);
        int[] seen = new int[Kind.values().length];
        while (true) {
            int op = body[pos[0]++] & 0xFF;
            if (op == OP_END) break;
            if (op == OP_TEXT) {
                int len = (int) readVarint(body, pos);
                out.append(new String(body, pos[0], len, StandardCharsets.UTF_8)).append('\n');
                pos[0] += len;
                continue;
            }
            int ord = op - OP_QUAD;
            if (ord < 0 || ord >= Kind.values().length) throw new IOException("bad op " + op);
            Kind k = Kind.values()[ord];
            out.append(k.word).append(' ').append(readVarint(body, pos));
            for (int v = 0; v < 4; v++) {
                long d = unzig(readVarint(body, pos));
                int at = (int) (seen[ord] + d);
                if (d == 0) {
                    at = seen[ord]++;
                } else if (at < 0 || at >= seen[ord]) {
                    throw new IOException("bad vertex index " + at);
                }
                long[] vert = table[ord][at];
                for (int c = 0; c < k.cols; c++) {
                    out.append(' ');
                    number(out, vert[c], decimals[ord][c]);
                }
            }
            for (int e = 0; e < k.extras; e++) {
                out.append(' ');
                number(out, unzig(readVarint(body, pos)), decimals[ord][k.cols + e]);
            }
            out.append('\n');
        }
        return out.toString();
    }

    /** True when the two texts hold the same lines and every token that is a number has the same float value (what a loader would parse). */
    public static String firstDifference(String a, String b) {
        List<String> x = lines(a), y = lines(b);
        if (x.size() != y.size()) return "line count " + x.size() + " vs " + y.size();
        for (int i = 0; i < x.size(); i++) {
            String[] s = x.get(i).trim().split("\\s+"), t = y.get(i).trim().split("\\s+");
            if (s.length != t.length) return "line " + (i + 1) + ": token count";
            for (int j = 0; j < s.length; j++) {
                if (s[j].equals(t[j])) continue;
                try {
                    if (Float.parseFloat(s[j]) == Float.parseFloat(t[j])) continue;
                } catch (NumberFormatException ignored) { }
                return "line " + (i + 1) + " token " + j + ": " + s[j] + " vs " + t[j];
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- one quad line
    private static final class Quad {
        Kind kind;
        long layer;
        long[] units = new long[32];                                                      // the digits of each number without the dot
        int[] dec = new int[32];                                                          // decimals written for it
        boolean[] neg = new boolean[32];

        /** Number {@code i} of the line at the given number of decimals. */
        long scaled(int i, int decimals) {
            long v = units[i];
            for (int d = dec[i]; d < decimals; d++) v *= 10;
            return neg[i] ? -v : v;
        }
    }

    private static Quad parse(String line) {
        if (line.isEmpty() || line.charAt(0) != 'q') return null;
        String[] t = line.trim().split("\\s+");
        Kind kind = null;
        for (Kind k : Kind.values()) if (t.length == k.tokens && t[0].equals(k.word)) kind = k;
        if (kind == null) return null;
        Quad q = new Quad();
        q.kind = kind;
        if (!t[1].matches("\\d{1,6}")) return null;
        q.layer = Long.parseLong(t[1]);
        for (int i = 0; i < kind.numbers(); i++) {
            String s = t[2 + i];
            int p = 0;
            boolean neg = false;
            if (s.startsWith("-")) {
                neg = true;
                p = 1;
            }
            int dot = s.indexOf('.');
            String whole = dot < 0 ? s.substring(p) : s.substring(p, dot), frac = dot < 0 ? "" : s.substring(dot + 1);
            if (whole.isEmpty() || whole.length() > MAX_INT_DIGITS || frac.length() > MAX_DECIMALS || (dot >= 0 && frac.isEmpty())) return null;
            if (!digits(whole) || !digits(frac)) return null;
            q.units[i] = Long.parseLong(whole + frac);
            q.dec[i] = frac.length();
            q.neg[i] = neg;
        }
        return q;
    }

    private static boolean digits(String s) {
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) < '0' || s.charAt(i) > '9') return false;
        return true;
    }

    private static void number(StringBuilder out, long scaled, int decimals) {
        long v = scaled;
        if (v < 0) {
            out.append('-');
            v = -v;
        }
        if (decimals == 0) {
            out.append(v);
            return;
        }
        long p = 1;
        for (int i = 0; i < decimals; i++) p *= 10;
        out.append(v / p).append('.');
        String f = Long.toString(v % p);
        for (int i = f.length(); i < decimals; i++) out.append('0');
        out.append(f);
    }

    // ---------------------------------------------------------------- plumbing
    private record Key(long[] v) {
        @Override public boolean equals(Object o) { return o instanceof Key k && java.util.Arrays.equals(v, k.v); }
        @Override public int hashCode() { return java.util.Arrays.hashCode(v); }
    }

    @SuppressWarnings("unchecked")
    private static List<long[]>[] newTables() {
        List<long[]>[] t = new List[Kind.values().length];
        for (int i = 0; i < t.length; i++) t[i] = new ArrayList<>();
        return t;
    }

    @SuppressWarnings("unchecked")
    private static Map<Key, Integer>[] newIndexes() {
        Map<Key, Integer>[] t = new Map[Kind.values().length];
        for (int i = 0; i < t.length; i++) t[i] = new HashMap<>();
        return t;
    }

    private static List<String> lines(String text) {
        List<String> out = new ArrayList<>();
        int from = 0, n = text.length();
        while (from < n) {
            int nl = text.indexOf('\n', from);
            int end = nl < 0 ? n : nl;
            int e = end;
            if (e > from && text.charAt(e - 1) == '\r') e--;
            out.add(text.substring(from, e));
            from = end + 1;
        }
        return out;
    }

    private static long zig(long v) { return (v << 1) ^ (v >> 63); }

    private static long unzig(long v) { return (v >>> 1) ^ -(v & 1); }

    private static void varint(ByteArrayOutputStream out, long v) {
        while ((v & ~0x7FL) != 0) {
            out.write((int) ((v & 0x7F) | 0x80));
            v >>>= 7;
        }
        out.write((int) v);
    }

    private static long readVarint(byte[] b, int[] pos) throws IOException {
        long v = 0;
        for (int shift = 0; shift < 64; shift += 7) {
            if (pos[0] >= b.length) throw new IOException("packed mesh ends early");
            int x = b[pos[0]++] & 0xFF;
            v |= (long) (x & 0x7F) << shift;
            if ((x & 0x80) == 0) return v;
        }
        throw new IOException("bad varint");
    }

    private static byte[] deflate(byte[] body) {
        Deflater d = new Deflater(Deflater.BEST_COMPRESSION, true);
        d.setInput(body);
        d.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream(body.length / 3 + 64);
        out.write(MAGIC, 0, MAGIC.length);
        byte[] buf = new byte[64 * 1024];
        while (!d.finished()) {
            int n = d.deflate(buf);
            out.write(buf, 0, n);
        }
        d.end();
        return out.toByteArray();
    }

    private static byte[] inflate(byte[] packed, int from) throws IOException {
        Inflater inf = new Inflater(true);
        inf.setInput(packed, from, packed.length - from);
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(1024, (packed.length - from) * 4));
        byte[] buf = new byte[64 * 1024];
        try {
            while (!inf.finished()) {
                int n = inf.inflate(buf);
                if (n == 0 && (inf.needsInput() || inf.needsDictionary())) throw new IOException("packed mesh is cut short");
                out.write(buf, 0, n);
            }
        } catch (DataFormatException e) {
            throw new IOException("packed mesh is damaged: " + e.getMessage(), e);
        } finally {
            inf.end();
        }
        return out.toByteArray();
    }
}
