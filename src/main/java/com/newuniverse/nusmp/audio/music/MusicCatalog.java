package com.newuniverse.nusmp.audio.music;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Why: which music belongs to which place is ONE table, pure and shared by the server (what to say), the client (what to play), the sound registry (one event per track), the
 * generator {@code tools/audio/gen_music.py} (which writes the files, and mirrors these ids) and the tests (which compare all of them against {@code sounds.json} and the files).
 * <p>
 * The 27 Region Themes are grouped into TEN musical families (stone, storm, blood, flame, frost, verdant, spatial, holy, deep, crypt): composing 27 pairs of loops would only make
 * the files bigger, and a family already gives each place its own key, scale, tempo and instruments. A theme that has no entry here (a datapack theme) is given a family by a
 * stable hash of its id, so it still sounds like SOMETHING and always the same thing. Each family has two loops (explore, battle), the Holy and the Abyssal road each have a boss
 * loop, and the safe zone, the hub and the rift have one each: 25 tracks. The track name is the file name ({@code assets/nusmp/sounds/music/<track>.ogg}) and the sound event is
 * {@code nusmp:music.<family>.<layer>} ({@link #eventId}); a resource pack that ships the same file names replaces the music without a line of code.
 */
public final class MusicCatalog {
    private MusicCatalog() {}

    /** The ten families, in the generator's order. */
    public static final List<String> FAMILIES = List.of("stone", "storm", "blood", "flame", "frost", "verdant", "spatial", "holy", "deep", "crypt");

    /** The family a family falls back to (an empty or missing theme: the gaps between sectors). */
    public static final String DEFAULT_FAMILY = "stone";

    private static final Map<String, String> THEME_FAMILY;

    static {
        Map<String, String> m = new LinkedHashMap<>();
        put(m, "stone", "gravity", "dwarven_forgehold");
        put(m, "storm", "storm", "sky_citadel");
        put(m, "blood", "blood", "vampire_castle", "cursed_archive");
        put(m, "flame", "flame", "infernal_foundry");
        put(m, "frost", "frost", "astral", "crystal_geode");
        put(m, "verdant", "verdant", "feywild_grove", "fungal_underdark");
        put(m, "spatial", "spatial", "shadow", "spider_warrens");
        put(m, "holy", "radiant", "holy_church", "celestial_court", "mountain_monastery");
        put(m, "deep", "abyss", "sunken_temple", "drowned_crypt");
        put(m, "crypt", "necropolis", "haunted_manor");
        THEME_FAMILY = Collections.unmodifiableMap(m);
    }

    private static void put(Map<String, String> m, String family, String... themes) {
        for (String t : themes) m.put(t, family);
    }

    /** Every theme id the table names, with its family (read-only; for the tests and the docs). */
    public static Map<String, String> themeTable() { return THEME_FAMILY; }

    /** The family of a theme id: the table, else a stable hash of the id (a datapack theme), else {@link #DEFAULT_FAMILY} for an empty id. */
    public static String familyOf(String themeId) {
        if (themeId == null || themeId.isEmpty()) return DEFAULT_FAMILY;
        String id = themeId.toLowerCase(Locale.ROOT);
        String f = THEME_FAMILY.get(id);
        if (f != null) return f;
        return FAMILIES.get(Math.floorMod(id.hashCode(), FAMILIES.size()));
    }

    /**
     * The track (file name without extension) for a layer in a theme, or null for {@link MusicLayer#OFF}.
     *
     * @param polarity the sign of the road: positive Holy, negative Abyssal (only the boss loop reads it)
     */
    public static String trackFor(MusicLayer layer, String themeId, int polarity) {
        if (layer == null) return null;
        switch (layer) {
            case EXPLORE: return familyOf(themeId) + "_explore";
            case BATTLE: return familyOf(themeId) + "_battle";
            case BOSS: return polarity < 0 ? "boss_abyssal" : "boss_holy";
            case SAFE: return "safe";
            case HUB: return "hub";
            case RIFT: return "rift";
            default: return null;
        }
    }

    /** Every track in the generator's order: each family's explore and battle loop, then boss_holy, boss_abyssal, safe, hub, rift. */
    public static List<String> trackNames() {
        List<String> out = new ArrayList<>();
        for (String f : FAMILIES) {
            out.add(f + "_explore");
            out.add(f + "_battle");
        }
        out.add("boss_holy");
        out.add("boss_abyssal");
        out.add("safe");
        out.add("hub");
        out.add("rift");
        return Collections.unmodifiableList(out);
    }

    /** The sound event path of a track: {@code music.stone.explore}, {@code music.boss.holy}, {@code music.safe} (the same rule as the generator's {@code event_id}). */
    public static String eventId(String track) {
        if (track.startsWith("boss_")) return "music.boss." + track.substring(5);
        int i = track.indexOf('_');
        return i < 0 ? "music." + track : "music." + track.substring(0, i) + "." + track.substring(i + 1);
    }

    /** Every sound event path, in the order of {@link #trackNames()}. */
    public static List<String> eventIds() {
        List<String> out = new ArrayList<>();
        for (String t : trackNames()) out.add(eventId(t));
        return Collections.unmodifiableList(out);
    }

    /** The resource path of the sound file of a track, relative to {@code assets/nusmp/sounds/}, with the extension: {@code music/stone_explore.ogg}. */
    public static String filePath(String track) { return "music/" + track + ".ogg"; }

    /** The lang key of a track's title (the optional "now playing" line). */
    public static String titleKey(String track) { return "music.nusmp.track." + track; }
}
