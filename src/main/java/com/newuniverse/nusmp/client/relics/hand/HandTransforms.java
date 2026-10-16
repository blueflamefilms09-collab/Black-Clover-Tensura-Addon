package com.newuniverse.nusmp.client.relics.hand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Why: a relic carried in the hand, lying on the ground or hanging in an item frame has to sit in the world the way that kind of thing is held (a necklace dangles from
 * the fingers, a hood is held by the brim, a flask is gripped round the neck), and doing that per item in Java would make every new relic a code change. This class is
 * the data side of it: it parses assets/nusmp/relic_hand/transforms.txt into named hold styles (how big the object is, which point of it sits in the hand, how it is
 * turned, how much it sways, and a display transform per context) and says which style a relic uses, by its id, by its slot and look, or by its slot. Pure (no Minecraft
 * types), so the file is checked in tests without a game.
 * <pre>
 * style &lt;name&gt; [size=0.9] [hold=hx,hy,hz] [pre=rx,ry,rz] [sway=deg] [only=n] [from=&lt;style&gt;]
 * xf &lt;style&gt; &lt;ctx&gt; rx ry rz tx ty tz scale          ctx: ground fixed tp_right tp_left fp_right fp_left head
 * use &lt;key[,key...]&gt; &lt;style&gt; [size=..] [hold=..] [pre=..] [sway=..] [only=..]       key: id:&lt;relic&gt; | look:&lt;SLOT&gt;:&lt;LOOK&gt; | slot:&lt;SLOT&gt;
 * </pre>
 * {@code size} is the longest side of the object in blocks, {@code hold} the point of the object (fractions of its half extent, y up, z toward the viewer) that sits at
 * the origin of the display transform (in a hand only: on the ground and in a frame the object is centred), {@code pre} turns the object about that point before the display
 * transform (everywhere), {@code sway} swings it about the hold point by that many degrees (in a hand), {@code only} draws just the first n pieces (one glove of a pair). A style without a value (or a context without an {@code xf}) takes it from its
 * {@code from} style, and the style {@code default} must give every context. The {@code xf} numbers are those of a model json display block (degrees, sixteenths of a
 * block, scale); the left hand is written out in full, not mirrored by the game.
 */
public final class HandTransforms {
    /** The contexts a held object is drawn in. */
    public enum Ctx {
        GROUND, FIXED, TP_RIGHT, TP_LEFT, FP_RIGHT, FP_LEFT, HEAD;

        /** The context of a file name ("tp_right" ...), or null. */
        public static Ctx byName(String s) {
            if (s == null) return null;
            for (Ctx c : values()) if (c.name().equalsIgnoreCase(s.trim())) return c;
            return null;
        }
    }

    /** A display transform: rotation in degrees, translation in sixteenths of a block, uniform scale. */
    public record Xf(float rx, float ry, float rz, float tx, float ty, float tz, float s) {
        public static final Xf IDENTITY = new Xf(0f, 0f, 0f, 0f, 0f, 0f, 1f);
    }

    /** How the object itself is placed (before the display transform). */
    public record Fit(float size, float hx, float hy, float hz, float prx, float pry, float prz, float sway, int only) {
        public static final Fit DEFAULT = new Fit(0.9f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0);
    }

    /** One style as written in the file, before inheritance: any part may be missing. */
    private static final class Raw {
        String name, from;
        Float size, sway;
        float[] hold, pre;
        Integer only;
        final Xf[] xf = new Xf[Ctx.values().length];
    }

    /** A style with its inheritance resolved. */
    public static final class Style {
        public final String name;
        private final Fit fit;
        private final Xf[] xf;

        Style(String name, Fit fit, Xf[] xf) {
            this.name = name;
            this.fit = fit;
            this.xf = xf;
        }

        public Fit fit() { return fit; }

        /** The display transform of a context: never null (a missing one is the identity). */
        public Xf xf(Ctx c) { return xf[c.ordinal()] == null ? Xf.IDENTITY : xf[c.ordinal()]; }

        /** Does this style give a transform for the context itself (no fallback)? */
        public boolean has(Ctx c) { return xf[c.ordinal()] != null; }
    }

    private record Use(String style, Raw over) {}

    private final Map<String, Raw> raws = new HashMap<>();
    private final Map<String, Style> styles = new HashMap<>();
    private final Map<String, Use> uses = new HashMap<>();
    private final Map<String, Style> usedCache = new HashMap<>();
    private final List<String> problems = new ArrayList<>();

    /** Name of the style that must exist and give every context. */
    public static final String DEFAULT = "default";
    /** The style of the flat sprite fallback (the transforms of a vanilla generated item), used when an item has no model. */
    public static final String SPRITE = "sprite";

    public static HandTransforms parse(Iterable<String> lines) {
        HandTransforms t = new HandTransforms();
        int ln = 0;
        for (String raw : lines) {
            ln++;
            String line = raw.trim();
            if (line.isEmpty() || line.charAt(0) == '#') continue;
            String[] v = line.split("\\s+");
            try {
                switch (v[0]) {
                    case "style" -> {
                        Raw r = new Raw();
                        r.name = v[1];
                        apply(r, v, 2);
                        t.raws.put(r.name, r);
                    }
                    case "xf" -> {
                        Raw r = t.raws.get(v[1]);
                        if (r == null) throw new IllegalArgumentException("xf for a style that is not declared yet: " + v[1]);
                        Ctx c = Ctx.byName(v[2]);
                        if (c == null) throw new IllegalArgumentException("unknown context " + v[2]);
                        if (v.length != 10) throw new IllegalArgumentException("xf takes 7 numbers");
                        r.xf[c.ordinal()] = new Xf(f(v[3]), f(v[4]), f(v[5]), f(v[6]), f(v[7]), f(v[8]), f(v[9]));
                    }
                    case "use" -> {
                        Raw o = new Raw();
                        apply(o, v, 3);
                        for (String key : v[1].split(",")) t.uses.put(key, new Use(v[2], o));
                    }
                    default -> { }
                }
            } catch (RuntimeException bad) {
                t.problems.add("line " + ln + ": " + bad.getMessage());
            }
        }
        t.resolveAll();
        return t;
    }

    private static float f(String s) {
        float x = Float.parseFloat(s);
        if (Float.isNaN(x) || Float.isInfinite(x)) throw new NumberFormatException("not a finite number: " + s);
        return x;
    }

    private static float[] triple(String s) {
        String[] p = s.split(",");
        if (p.length != 3) throw new IllegalArgumentException("three numbers expected: " + s);
        return new float[]{f(p[0]), f(p[1]), f(p[2])};
    }

    private static void apply(Raw r, String[] v, int from) {
        for (int i = from; i < v.length; i++) {
            int eq = v[i].indexOf('=');
            if (eq < 0) throw new IllegalArgumentException("expected key=value: " + v[i]);
            String k = v[i].substring(0, eq), val = v[i].substring(eq + 1);
            switch (k) {
                case "size" -> r.size = f(val);
                case "hold" -> r.hold = triple(val);
                case "pre" -> r.pre = triple(val);
                case "sway" -> r.sway = f(val);
                case "only" -> r.only = Integer.parseInt(val);
                case "from" -> r.from = val;
                default -> throw new IllegalArgumentException("unknown key " + k);
            }
        }
    }

    private void resolveAll() {
        for (String name : raws.keySet()) {
            try {
                styles.put(name, resolve(name, new ArrayList<>()));
            } catch (RuntimeException bad) {
                problems.add("style " + name + ": " + bad.getMessage());
            }
        }
    }

    private Style resolve(String name, List<String> chain) {
        Raw r = raws.get(name);
        if (r == null) throw new IllegalArgumentException("unknown style " + name);
        if (chain.contains(name)) throw new IllegalArgumentException("style loop through " + name);
        chain.add(name);
        Fit base = Fit.DEFAULT;
        Xf[] xf = new Xf[Ctx.values().length];
        if (r.from != null) {
            Style p = resolve(r.from, chain);
            base = p.fit;
            System.arraycopy(p.xf, 0, xf, 0, xf.length);
        }
        for (int i = 0; i < xf.length; i++) if (r.xf[i] != null) xf[i] = r.xf[i];
        return new Style(name, over(base, r), xf);
    }

    private static Fit over(Fit b, Raw r) {
        return new Fit(r.size != null ? r.size : b.size(), r.hold != null ? r.hold[0] : b.hx(), r.hold != null ? r.hold[1] : b.hy(), r.hold != null ? r.hold[2] : b.hz(),
                r.pre != null ? r.pre[0] : b.prx(), r.pre != null ? r.pre[1] : b.pry(), r.pre != null ? r.pre[2] : b.prz(), r.sway != null ? r.sway : b.sway(),
                r.only != null ? r.only : b.only());
    }

    /** Lines that could not be read or resolved (empty for a good file). */
    public List<String> problems() { return problems; }

    /** The named style, or null. */
    public Style style(String name) { return styles.get(name); }

    public int styleCount() { return styles.size(); }

    public int useCount() { return uses.size(); }

    /** All keys of the {@code use} lines. */
    public Iterable<String> useKeys() { return uses.keySet(); }

    /** The style a use line names for this key (without its overrides), or null. */
    public String useStyleName(String key) {
        Use u = uses.get(key);
        return u == null ? null : u.style();
    }

    /** The key under which {@link #styleFor} finds a relic: {@code id:..} first, then {@code look:SLOT:LOOK}, then {@code slot:SLOT}; null if none matches. */
    public String keyFor(String id, String slot, String look) {
        String[] keys = {"id:" + id, "look:" + up(slot) + ":" + up(look), "slot:" + up(slot)};
        for (String k : keys) if (uses.containsKey(k)) return k;
        return null;
    }

    private static String up(String s) { return s == null ? "" : s.toUpperCase(Locale.ROOT); }

    /**
     * The style of a relic with the overrides of its use line applied: the first of its id, its slot and look, or its slot that has a {@code use} line, else
     * {@link #DEFAULT}. Never null once the file has a default style; cached per key.
     */
    public Style styleFor(String id, String slot, String look) {
        String key = keyFor(id, slot, look);
        if (key == null) return styles.get(DEFAULT);
        Style s = usedCache.get(key);
        if (s != null) return s;
        Use u = uses.get(key);
        Style base = styles.get(u.style());
        if (base == null) base = styles.get(DEFAULT);
        if (base == null) return null;
        s = new Style(base.name, over(base.fit, u.over()), base.xf);
        usedCache.put(key, s);
        return s;
    }

    /** True when every context has a transform in the default style and in the sprite style (what the renderer relies on). */
    public boolean complete() {
        for (String n : new String[]{DEFAULT, SPRITE}) {
            Style s = styles.get(n);
            if (s == null) return false;
            for (Ctx c : Ctx.values()) if (!s.has(c)) return false;
        }
        return true;
    }
}
