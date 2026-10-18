package com.newuniverse.nusmp.heroweapon.forms;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.Sin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the Weapon Book runs on the client, and the client cannot read the server's data pack, so the forms of the bonded weapon travel in a packet. The byte
 * layout of a {@link HeroForm} is written once, here, against two tiny interfaces ({@link Out}, {@link In}) instead of Minecraft's buffer class, so a JUnit test
 * can round-trip random forms and prove the reader and the writer agree, and so the same writer can feed a checksum that tells the client whether its copy is
 * current. The network class adapts these interfaces to {@code FriendlyByteBuf}. The reader refuses absurd sizes (it throws {@link IllegalArgumentException}) so a
 * corrupt packet cannot make the client allocate without bound.
 */
public final class FormsWire {
    private FormsWire() {}

    /** The sink of the writer. */
    public interface Out {
        void utf(String s);

        void varInt(int v);

        void dbl(double d);

        void bool(boolean b);
    }

    /** The source of the reader. */
    public interface In {
        String utf();

        int varInt();

        double dbl();

        boolean bool();
    }

    private static final int MAX_LIST = 64, MAX_FORMS = 64, MAX_TEXT = 600;

    public static void writeForms(Out out, List<HeroForm> forms) {
        out.varInt(forms.size());
        for (HeroForm f : forms) write(out, f);
    }

    public static List<HeroForm> readForms(In in, HeroKind kind) {
        int n = in.varInt();
        if (n < 0 || n > MAX_FORMS) throw new IllegalArgumentException("form count " + n);
        List<HeroForm> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add(read(in, kind));
        return Collections.unmodifiableList(out);
    }

    public static void write(Out out, HeroForm f) {
        out.utf(f.id());
        out.utf(f.name());
        out.utf(f.desc());
        out.varInt(f.tier());
        strings(out, f.parents());
        out.varInt(f.requires().size());
        for (HeroForm.Requirement r : f.requires()) {
            out.utf(r.item() == null ? "" : r.item());
            out.utf(r.tag() == null ? "" : r.tag());
            out.varInt(r.count());
        }
        out.varInt(f.mastery());
        doubles(out, f.stats());
        doubles(out, f.abilityBonus());
        out.varInt(f.skills().size());
        for (Map.Entry<String, String> e : f.skills().entrySet()) {
            out.utf(e.getKey());
            out.utf(e.getValue());
        }
        strings(out, f.passives());
        out.utf(f.look());
        out.bool(f.curse());
        out.utf(f.sin() == null ? "" : f.sin().id());
        out.varInt(f.corruptionPerSecond());
    }

    public static HeroForm read(In in, HeroKind kind) {
        String id = text(in), name = text(in), desc = text(in);
        int tier = in.varInt();
        List<String> parents = strings(in);
        int nr = size(in.varInt());
        List<HeroForm.Requirement> requires = new ArrayList<>(nr);
        for (int i = 0; i < nr; i++) requires.add(new HeroForm.Requirement(text(in), text(in), in.varInt()));
        int mastery = in.varInt();
        Map<String, Double> stats = doubles(in), bonus = doubles(in);
        int ns = size(in.varInt());
        Map<String, String> skills = new LinkedHashMap<>();
        for (int i = 0; i < ns; i++) skills.put(text(in), text(in));
        List<String> passives = strings(in);
        String look = text(in);
        boolean curse = in.bool();
        String sinId = text(in);
        int cps = in.varInt();
        return new HeroForm(kind, id, name, desc, tier, List.copyOf(parents), List.copyOf(requires), mastery, Collections.unmodifiableMap(stats),
                Collections.unmodifiableMap(bonus), Collections.unmodifiableMap(skills), List.copyOf(passives), look, curse, sinId.isEmpty() ? null : Sin.byId(sinId), cps);
    }

    /** A 32-bit checksum of the forms as they would be written: equal forms give equal numbers on any machine (it does not use {@code hashCode} of an enum). */
    public static int checksum(List<HeroForm> forms) {
        Fnv h = new Fnv();
        writeForms(h, forms);
        return h.value;
    }

    // ------------------------------------------------------------------------------------------------ helpers
    private static void strings(Out out, List<String> l) {
        out.varInt(l.size());
        for (String s : l) out.utf(s);
    }

    private static List<String> strings(In in) {
        int n = size(in.varInt());
        List<String> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add(text(in));
        return out;
    }

    private static void doubles(Out out, Map<String, Double> m) {
        out.varInt(m.size());
        for (Map.Entry<String, Double> e : m.entrySet()) {
            out.utf(e.getKey());
            out.dbl(e.getValue());
        }
    }

    private static Map<String, Double> doubles(In in) {
        int n = size(in.varInt());
        Map<String, Double> out = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) out.put(text(in), in.dbl());
        return out;
    }

    private static int size(int n) {
        if (n < 0 || n > MAX_LIST) throw new IllegalArgumentException("list size " + n);
        return n;
    }

    private static String text(In in) {
        String s = in.utf();
        if (s == null || s.length() > MAX_TEXT) throw new IllegalArgumentException("text too long");
        return s;
    }

    /** FNV-1a over everything written. */
    private static final class Fnv implements Out {
        int value = 0x811C9DC5;

        private void mix(int b) {
            value ^= b & 0xFF;
            value *= 0x01000193;
        }

        private void mixInt(int v) {
            mix(v);
            mix(v >>> 8);
            mix(v >>> 16);
            mix(v >>> 24);
        }

        @Override
        public void utf(String s) {
            mixInt(s.length());
            for (int i = 0; i < s.length(); i++) {
                mix(s.charAt(i));
                mix(s.charAt(i) >>> 8);
            }
        }

        @Override
        public void varInt(int v) { mixInt(v); }

        @Override
        public void dbl(double d) {
            long bits = Double.doubleToLongBits(d);
            mixInt((int) bits);
            mixInt((int) (bits >>> 32));
        }

        @Override
        public void bool(boolean b) { mix(b ? 1 : 0); }
    }
}
