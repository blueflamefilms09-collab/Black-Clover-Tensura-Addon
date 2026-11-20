package com.newuniverse.nusmp.skill.icons;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Why: the skill icon rework (docs/skill_icons_rework.md, tools/skills/gen_icons_v3.py) repainted 121 skill icons and animated the Ultimates among them. Which ids are repainted and which are
 * animated is data ({@code assets/anime_multiverse/skill_icons.json}, written by the generator), so the client helper, the self-test and the unit tests all read one list and nothing is typed twice.
 * Pure and Minecraft-free (Gson only), so it is safe on a dedicated server and in plain unit tests. Every value is validated: a bad file gives the empty index, never an exception.
 */
public final class SkillIconIndex {
    private SkillIconIndex() {}

    /** The most frames an animated strip may have (a loop of at most 16 pictures; the redesign of docs/skill_icons_v3.md uses 12, like Tensura No Index's 12 to 16 frame icons). */
    public static final int MAX_FRAMES = 16;
    /** The most pictures the list of replaced icons (skill_icons_old.json) may name; bounds the client's once-a-second check. */
    public static final int MAX_OLD = 640;
    public static final String OLD_INDEX_RESOURCE = "/assets/anime_multiverse/skill_icons_old.json";
    /** The most icons the index may name (bounds the client's once-a-second check). */
    public static final int MAX_STYLED = 256;
    public static final int DEFAULT_FRAMETIME = 6;
    public static final String INDEX_RESOURCE = "/assets/anime_multiverse/skill_icons.json";

    private static final Pattern ID = Pattern.compile("[a-z0-9_]{1,64}");
    private static final Pattern REL = Pattern.compile("[a-z0-9_]{1,64}(/[a-z0-9_]{1,64}){0,2}\\.png");

    /** styled: every repainted skill id; animated: the subset that has a strip; frames: frames per strip; frameTime: game ticks per frame. */
    public record Index(List<String> styled, List<String> animated, int frames, int frameTime) {
        public static final Index EMPTY = new Index(List.of(), List.of(), 1, DEFAULT_FRAMETIME);

        public boolean isEmpty() { return styled.isEmpty(); }
    }

    public static boolean validId(String id) { return id != null && ID.matcher(id).matches(); }

    /** The icon the skill names (the new picture, or the still frame of an animated one). */
    public static String stillPath(String id) { return "textures/skill/" + id + ".png"; }

    /** Ultimates: the vertical strip of frames (32 x 32N). */
    public static String stripPath(String id) { return "textures/skill/anim/" + id + "_anim.png"; }

    /** The icon that shipped up to 4.4.0, shown when the client setting skillIcons.newStyle is off. */
    public static String classicPath(String id) { return "textures/skill/classic/v440/" + id + ".png"; }

    /** A replaced icon's path relative to textures/skill (for example {@code grimoire/flame.png}): lower-case segments only, so it cannot leave the skill folder. */
    public static boolean validRel(String rel) { return rel != null && REL.matcher(rel).matches(); }

    /** Where the icon is drawn from (the new picture). */
    public static String livePath(String rel) { return "textures/skill/" + rel; }

    /** The icon as it was before the v3 redesign (docs/skill_icons_v3.md), shown when the client setting skillIcons.classic is on. */
    public static String oldPath(String rel) { return "textures/skill_old/" + rel; }

    /** Parses the list of replaced icons (skill_icons_old.json: {@code {"paths": ["grimoire/flame.png", ...]}}); any problem (not JSON, a bad or repeated path, more than {@link #MAX_OLD}) gives the empty list. */
    public static List<String> parseOld(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            JsonArray arr = JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("paths");
            if (arr == null || arr.size() > MAX_OLD) return List.of();
            List<String> out = new ArrayList<>(arr.size());
            for (JsonElement e : arr) {
                String rel = e.getAsString();
                if (!validRel(rel) || out.contains(rel)) return List.of();
                out.add(rel);
            }
            return List.copyOf(out);
        } catch (RuntimeException notTheShape) {
            return List.of();
        }
    }

    /** Reads the list shipped in the mod jar. */
    public static List<String> loadOld() {
        try (InputStream in = SkillIconIndex.class.getResourceAsStream(OLD_INDEX_RESOURCE)) {
            if (in == null) return List.of();
            return parseOld(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException unreadable) {
            return List.of();
        }
    }

    /** Parses the index text; any problem (not JSON, bad id, animated id that is not styled, too many ids, frames out of range) gives {@link Index#EMPTY}. */
    public static Index parse(String json) {
        if (json == null || json.isBlank()) return Index.EMPTY;
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            List<String> styled = ids(root.getAsJsonArray("styled"));
            List<String> animated = ids(root.getAsJsonArray("animated"));
            if (styled == null || animated == null || styled.isEmpty() || styled.size() > MAX_STYLED || !styled.containsAll(animated)) return Index.EMPTY;
            int frames = root.has("frames") ? root.get("frames").getAsInt() : 1;
            int frameTime = root.has("frametime") ? root.get("frametime").getAsInt() : DEFAULT_FRAMETIME;
            if (frames < 1 || frames > MAX_FRAMES) return Index.EMPTY;
            return new Index(List.copyOf(styled), List.copyOf(animated), frames, Math.max(1, Math.min(40, frameTime)));
        } catch (RuntimeException notTheShape) {
            return Index.EMPTY;
        }
    }

    private static List<String> ids(JsonArray arr) {
        if (arr == null) return null;
        List<String> out = new ArrayList<>(arr.size());
        for (JsonElement e : arr) {
            String id = e.getAsString();
            if (!validId(id) || out.contains(id)) return null;
            out.add(id);
        }
        return out;
    }

    /** Reads the index shipped in the mod jar (the same file a resource pack could override on the client). */
    public static Index load() {
        try (InputStream in = SkillIconIndex.class.getResourceAsStream(INDEX_RESOURCE)) {
            if (in == null) return Index.EMPTY;
            return parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException unreadable) {
            return Index.EMPTY;
        }
    }
}
