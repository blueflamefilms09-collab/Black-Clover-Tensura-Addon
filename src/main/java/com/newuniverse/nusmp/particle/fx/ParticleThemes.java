package com.newuniverse.nusmp.particle.fx;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: every magic, relic grade, relic element, skill family and boss of the mod must have its OWN recognisable particles instead of the same end-rod, and the owner must be able to change
 * them without touching code: {@code data/nusmp/particle_themes.json} maps a theme id ({@code magic.FLAME}, {@code relic.grade.GOD}, {@code relic.element.fire}, {@code skill.sword},
 * {@code boss.zenon}, {@code dungeon.vault}, ...) to two to four library particles and a colour, plus an optional {@code dark} flavour (the shadowed twin). This class is the pure part: it
 * parses the document, resolves an id to its theme (exact id, else the id with its last dotted part dropped, again and again, else the table default) and hands out {@link Theme} values. The
 * parsed table is cached; {@link #reload} swaps it (tests, an admin command) and bumps {@link #epoch}, which the facade's per-theme caches watch.
 */
public final class ParticleThemes {
    private ParticleThemes() {}

    public static final String RESOURCE = "/data/nusmp/particle_themes.json";
    public static final int MAX_PARTICLES = 4;

    /** A resolved theme: ids of its particles (1..4, slot order), its colour, and the dark flavour. */
    public record Theme(String id, List<String> particles, int rgb, List<String> darkParticles, int darkRgb) {
        public String particle(FxSlot slot, boolean dark) {
            List<String> l = dark ? darkParticles : particles;
            return l.get(slot.index() % l.size());
        }

        public int color(boolean dark) {
            return dark ? darkRgb : rgb;
        }
    }

    /** The parsed document. */
    public static final class Table {
        private final Map<String, Theme> themes;
        private final Theme fallback;

        Table(Map<String, Theme> themes, Theme fallback) {
            this.themes = themes;
            this.fallback = fallback;
        }

        public Map<String, Theme> themes() {
            return themes;
        }

        public Theme fallback() {
            return fallback;
        }

        public boolean has(String id) {
            return themes.containsKey(id);
        }

        /** Exact id, else the nearest dotted parent, else the table default. Never null. */
        public Theme resolve(String id) {
            if (id == null) return fallback;
            String k = id;
            while (true) {
                Theme t = themes.get(k);
                if (t != null) return t;
                int dot = k.lastIndexOf('.');
                if (dot <= 0) return fallback;
                k = k.substring(0, dot);
            }
        }
    }

    private static volatile Table table;
    private static volatile int epoch;

    public static int epoch() {
        return epoch;
    }

    /** The cached table (loaded from the mod's resources on first use). */
    public static Table table() {
        Table t = table;
        if (t == null) {
            synchronized (ParticleThemes.class) {
                t = table;
                if (t == null) table = t = loadResource();
            }
        }
        return t;
    }

    public static Theme resolve(String id) {
        return table().resolve(id);
    }

    /** Replaces the table (tests, tools); passing null drops the cache so the resource is read again. */
    public static synchronized void reload(JsonElement doc) {
        table = doc == null ? null : parse(doc);
        epoch++;
    }

    private static Table loadResource() {
        try (InputStream in = ParticleThemes.class.getResourceAsStream(RESOURCE)) {
            if (in != null) {
                try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    return parse(JsonParser.parseReader(r));
                }
            }
        } catch (IOException | RuntimeException e) {
            // fall through to the built-in default: a missing or broken table must never stop the game
        }
        return parse(null);
    }

    static final Theme BUILT_IN = new Theme("default", List.of("mote", "glint", "star4", "mote_soft"), 0xFFFFFF, List.of("void_speck", "soot_puff", "shadow_tendril", "soul_wisp"), 0x5A3A8C);

    /** Parses a themes document. A null or broken document gives a table with only the built-in default. Public for the tests. */
    public static Table parse(JsonElement doc) {
        Map<String, Theme> out = new LinkedHashMap<>();
        Theme fb = BUILT_IN;
        if (doc != null && doc.isJsonObject()) {
            JsonObject root = doc.getAsJsonObject();
            JsonObject def = obj(root, "default");
            if (def != null) fb = theme("default", def, BUILT_IN);
            JsonObject th = obj(root, "themes");
            if (th != null) {
                for (Map.Entry<String, JsonElement> e : th.entrySet()) {
                    if (e.getValue().isJsonObject()) out.put(e.getKey(), theme(e.getKey(), e.getValue().getAsJsonObject(), fb));
                }
            }
        }
        return new Table(Collections.unmodifiableMap(out), fb);
    }

    private static Theme theme(String id, JsonObject o, Theme base) {
        List<String> p = ids(o.get("particles"));
        if (p.isEmpty()) p = base.particles();
        int rgb = o.has("color") && o.get("color").isJsonPrimitive() ? FxTint.parseColor(o.get("color").getAsString()) : base.rgb();
        if (rgb < 0) rgb = base.rgb();
        JsonObject d = obj(o, "dark");
        List<String> dp = d == null ? List.of() : ids(d.get("particles"));
        if (dp.isEmpty()) dp = base.darkParticles();
        int drgb;
        if (d != null && d.has("color") && d.get("color").isJsonPrimitive() && FxTint.parseColor(d.get("color").getAsString()) >= 0) drgb = FxTint.parseColor(d.get("color").getAsString());
        else drgb = darken(rgb);
        return new Theme(id, p, rgb, dp, drgb);
    }

    /** The default dark colour of a theme: its colour at 40 percent brightness pulled a little towards violet. */
    public static int darken(int rgb) {
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        int nr = Math.round(r * 0.40f + 0x2A * 0.20f);
        int ng = Math.round(g * 0.40f + 0x10 * 0.20f);
        int nb = Math.round(b * 0.40f + 0x48 * 0.20f);
        return (Math.min(255, nr) << 16) | (Math.min(255, ng) << 8) | Math.min(255, nb);
    }

    private static List<String> ids(JsonElement e) {
        List<String> out = new ArrayList<>(MAX_PARTICLES);
        if (e != null && e.isJsonArray()) {
            for (JsonElement x : (JsonArray) e) {
                if (x.isJsonPrimitive() && out.size() < MAX_PARTICLES) out.add(x.getAsString());
            }
        }
        return Collections.unmodifiableList(out);
    }

    private static JsonObject obj(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
    }
}
