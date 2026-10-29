package com.newuniverse.nusmp.client.playermodel;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Why: the kit's looks live in more than one file, written by different generators, and the game has to read them as ONE table. {@code looks.json} is phase 1 (the arcs, the Zogratis family, the
 * hub), {@code looks_abyssal.json} is what {@code tools/abyssal/gen_abyssal.py} writes for the nine humanoid Decennial bosses (id {@code abyssal_<look>_p<phase>}: the loader that makes a boss
 * profile's {@code look} pick its player-model look is {@code DungeonLooks.guardian}, which asks this table), and {@code looks_dungeon.json} is phase 2 (the Mob Foundry's people, the named
 * archetypes, the Living Armor, the Floor Warden, the Goliath, the soul-corpse puppets). The rule of the merge is the owner's: add the new, keep the old. An earlier file wins a duplicate id (a
 * later one never silently replaces a look that is already there) and the duplicate is reported, so a test and the CI self-test fail on it instead of one boss changing its face. This class is
 * pure (Gson and the plain records, no game class): the client's {@link PlayerAssets} feeds it the files it read through the resource manager, the tests and the self-test feed it the class path.
 */
public final class PlayerLookTables {
    private PlayerLookTables() {}

    /**
     * The tables, in the order they are merged; the first is required, the others are optional (a pack may drop one). {@code looks_decennial.json} is reserved for the 200 Decennial bosses' own
     * humanoid looks (the same {@code abyssal_<look>_p<phase>} ids, the same format): a builder of those bosses writes that file and the loader picks it up with no code change.
     * {@code looks_tier.json} (3.33.0) is the Tier List bosses' table, written by {@code tools/tier_bosses/gen_tier_looks.py} (id = the character's id).
     */
    public static final List<String> FILES = List.of("playermodel/looks.json", "playermodel/looks_abyssal.json", "playermodel/looks_dungeon.json", "playermodel/looks_decennial.json", "playermodel/looks_tier.json");

    /** The merged table and what went wrong while it was made (an unreadable file, a duplicate id). */
    public record Merged(Map<String, PlayerLook> looks, List<String> problems) {}

    /** Adds the looks of {@code more} that {@code into} does not have yet; an id it already has is kept and reported as a problem naming {@code from}. */
    public static void merge(Map<String, PlayerLook> into, Map<String, PlayerLook> more, String from, List<String> problems) {
        for (Map.Entry<String, PlayerLook> e : more.entrySet()) {
            if (into.containsKey(e.getKey())) problems.add("the look '" + e.getKey() + "' of " + from + " is already defined by an earlier table: the earlier one is kept");
            else into.put(e.getKey(), e.getValue());
        }
    }

    /** Reads and merges every table of {@link #FILES} through {@code open} (a path under {@code assets/nusmp/} to a stream, or null when the file does not exist). */
    public static Merged load(Function<String, InputStream> open) {
        Map<String, PlayerLook> out = new LinkedHashMap<>();
        List<String> problems = new ArrayList<>();
        for (int i = 0; i < FILES.size(); i++) {
            String file = FILES.get(i);
            try (InputStream in = open.apply(file)) {
                if (in == null) {
                    if (i == 0) problems.add(file + " is missing");
                    continue;
                }
                JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                merge(out, PlayerLook.parseTable(root), file, problems);
            } catch (Exception e) {
                problems.add(file + ": " + e);
            }
        }
        return new Merged(Collections.unmodifiableMap(out), Collections.unmodifiableList(problems));
    }
}
