package com.newuniverse.nusmp.audio;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: a Decennial boss profile ({@code data/<ns>/mega/dnd_bosses/<id>.json}) may name the family it sounds like in an optional {@code "sound_set"} field ({@code "lich"},
 * {@code "dragon"}, any id of {@link MobSoundSet}); with no field the boss sounds like its look ({@code "look": "kraken"} is aquatic), then like the first creature its name or its
 * D&amp;D fusion names, then like its side (evil demonic, holy celestial). The profile reader ({@code DndProfile.parse}) hands every file to {@link #note} once, so the field is part of the
 * loader without the profile record growing a component (the boss builders own that record); the Guardian asks {@link #family} when it needs a voice. A file that names a family the
 * catalogue does not have is refused in plain words, like every other bad field of a profile.
 */
public final class BossSoundSets {
    private BossSoundSets() {}

    private static final Map<String, String> EXPLICIT = new ConcurrentHashMap<>();

    /** Reads the optional {@code sound_set} of a profile file (throws {@link IllegalArgumentException} for an unknown family); a file with no field clears any earlier entry of that id. */
    public static void note(String profileId, JsonObject o) {
        JsonElement el = o == null ? null : o.get("sound_set");
        if (el == null || el.isJsonNull()) {
            EXPLICIT.remove(profileId);
            return;
        }
        String id = el.getAsString().trim();
        if (!MobSoundSet.has(id)) throw new IllegalArgumentException(profileId + ": unknown sound_set '" + id + "' (known: " + MobSoundSet.ids() + ")");
        EXPLICIT.put(profileId, id);
    }

    /** The family a profile names, or null. */
    public static String explicit(String profileId) { return profileId == null ? null : EXPLICIT.get(profileId); }

    /** The family of a boss (see the class comment for the order). */
    public static String family(String profileId, String look, String name, List<String> creatures, String side) {
        return MobSoundSet.forBoss(explicit(profileId), look, name, creatures, side);
    }
}
