package com.newuniverse.nusmp.devil.dual;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Why: a devil is not called into the light. A contract ritual may begin only when (a) somebody at the circle carries a dark magic, or (b) the place itself is dark: a dark dimension, a dark biome, or a spot
 * with a very low light level (docs/devil_double_magic.md). The decision is this pure class; the facts (who stands there, the biome, the light) are read on the server by {@code DevilRituals}, once when the ritual
 * is asked for and, for a ritual that relied on the dark of the place, once a second while it runs ({@link #lightBroken}). What counts as dark magic is data, not code: the list
 * {@code assets/anime_multiverse/devil_dark_magic.json} (grimoire magics by {@code MagicType} name, devil attributes, skill ids of any mod: an id of a mod that is not installed is simply never found).
 */
public final class DarkRitualRules {
    private DarkRitualRules() {}

    /** Light levels above the limit by more than this break a running ritual (a torch placed by a bystander does not, a lit room does). */
    public static final int LIGHT_GRACE = 2;
    /** The most skill ids one check looks up (a data file can not make the check long). */
    public static final int MAX_SKILLS = 64;

    /** The dark-magic list. */
    public record Dark(Set<String> magics, Set<String> attributes, List<String> skills) {}

    public enum Why { MAGIC, DIMENSION, BIOME, LIGHT, OFF, NONE }

    private static volatile Dark table;

    public static Dark parse(JsonObject root) {
        return new Dark(names(root, "magics", true), names(root, "devilAttributes", true), new ArrayList<>(names(root, "skills", false)).subList(0, Math.min(MAX_SKILLS, names(root, "skills", false).size())));
    }

    private static Set<String> names(JsonObject root, String key, boolean upper) {
        Set<String> out = new LinkedHashSet<>();
        if (root != null && root.has(key) && root.get(key).isJsonArray()) {
            JsonArray a = root.getAsJsonArray(key);
            for (JsonElement e : a) if (e.isJsonPrimitive()) out.add(upper ? e.getAsString().toUpperCase(Locale.ROOT) : e.getAsString().toLowerCase(Locale.ROOT));
        }
        return out;
    }

    public static Dark table() {
        Dark t = table;
        if (t != null) return t;
        try (InputStream in = DarkRitualRules.class.getResourceAsStream("/assets/anime_multiverse/devil_dark_magic.json")) {
            t = in == null ? parse(null) : parse(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
        } catch (Exception unreadable) {
            t = parse(null);
        }
        table = t;
        return t;
    }

    /** Is this grimoire magic or devil attribute (names, any case) on the dark-magic list? Either may be null. */
    public static boolean isDark(Dark d, String magicName, String devilAttribute) {
        return (magicName != null && d.magics().contains(magicName.toUpperCase(Locale.ROOT))) || (devilAttribute != null && d.attributes().contains(devilAttribute.toUpperCase(Locale.ROOT)));
    }

    /**
     * May the ritual begin, and on what grounds? {@code ruleOn} false = the old behaviour (always). A dark magic at the circle wins; then a dark dimension, a dark biome, a low light level
     * ({@code light <= maxLight}). NONE = refused.
     */
    public static Why allow(boolean ruleOn, boolean darkMagic, boolean darkDimension, boolean darkBiome, int light, int maxLight) {
        if (!ruleOn) return Why.OFF;
        if (darkMagic) return Why.MAGIC;
        if (darkDimension) return Why.DIMENSION;
        if (darkBiome) return Why.BIOME;
        if (light <= Math.max(0, Math.min(15, maxLight))) return Why.LIGHT;
        return Why.NONE;
    }

    /** Does a ritual that relied on the dark of its place (biome or light, not a dark magic or a dark dimension) break now? */
    public static boolean needsLightCheck(Why why) { return why == Why.LIGHT; }

    public static boolean lightBroken(int light, int maxLight) { return light > Math.max(0, Math.min(15, maxLight)) + LIGHT_GRACE; }
}
