package com.newuniverse.nusmp.relics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Why: "my relics cannot be worn" had no single place that said what a relic needs to be wearable. A relic worn in a Curios slot needs four things, all of them DATA, and a missing one is
 * silent in the game (the stack just does not fit the slot): (1) the slot type has to exist (a slot file {@code data/<ns>/curios/slots/<id>.json}); (2) the slot type has to be given to the
 * player ({@code data/<ns>/curios/entities/*.json}); (3) the item has to be in the slot's item tag ({@code data/curios/tags/item/<id>.json}); (4) the slot size has to be at least one.
 * Until the relics_wear change the mod defined only {@code dungeon_relic} and {@code celestial_key} and relied on Curios to ship the other five slot types ({@code necklace, back, belt, hands, charm}) that
 * its entity file hands to the player; nothing could prove that, and if a build of Curios did not ship one, relics of that slot could not be put on at all. Now the mod defines every slot
 * type its relics use, and this class is the check of all four, used by the JUnit data test (over the real files and the wave sources) and by the self-test {@code relic.wear} (over the
 * jar and the registered relics). Armour relics (head, chest, feet) are not Curios and are checked against the equipment slot of their item type instead. Pure: no Minecraft type.
 */
public final class RelicWear {
    private RelicWear() {}

    /** The slot ids Curios itself is known to ship. A slot outside this list that the mod does not define either is a hole. */
    public static final Set<String> CURIOS_SHIPPED = Set.of("necklace", "back", "belt", "hands", "charm", "ring", "head", "body", "bracelet", "curio");

    /** The slot ids the relics of the mod use besides the five of the {@link RelicSlot} enum (the dungeon relics and the celestial keys have slots of their own). */
    public static final List<String> OTHER_SLOTS = List.of("dungeon_relic", "celestial_key", "relic_head", "relic_feet");      // 3.10.0: relic_head / relic_feet hold the head and feet relics besides the armour slots

    /** The mod's slot files that must exist (the relic slots, in the order of {@link RelicSlot}). */
    public static final List<String> RELIC_SLOTS = List.of("necklace", "back", "belt", "hands", "charm");

    /** The three files of the mod's Curios data: slots (id -> size), item tags (slot id -> the item ids listed), and the slots given to the player. */
    public record Data(Map<String, Integer> slotSizes, Map<String, Set<String>> tags, Set<String> assigned) {
        public Data {
            slotSizes = Map.copyOf(slotSizes);
            Map<String, Set<String>> t = new HashMap<>();
            tags.forEach((k, v) -> t.put(k, Set.copyOf(v)));
            tags = Map.copyOf(t);
            assigned = Set.copyOf(assigned);
        }

        public boolean defines(String slot) { return slotSizes.getOrDefault(slot, 0) >= 1; }
    }

    /** The Curios slot id of a relic slot name ({@code NECK} gives {@code necklace}), or null if the slot is worn some other way (armour, hand, consumable). */
    public static String curiosSlot(String relicSlotName) {
        RelicSlot s = RelicSlot.byName(relicSlotName);
        return s == null ? null : s.curiosSlot();
    }

    /** The vanilla equipment slot name an armour relic's slot is worn in ({@code HEAD}, {@code CHEST}, {@code FEET}), or null for a slot that is not armour. */
    public static String armourEquipSlot(String relicSlotName) {
        RelicSlot s = RelicSlot.byName(relicSlotName);
        return s != null && s.isArmor() ? s.name() : null;
    }

    /**
     * What is wrong with the slot data itself, whatever relics there are: every slot the relics use must be defined by the mod with a size of at least one, given to the player, and have
     * a tag file. An empty list means the data is complete.
     */
    public static List<String> checkSlots(Data d) {
        List<String> out = new ArrayList<>();
        List<String> all = new ArrayList<>(RELIC_SLOTS);
        all.addAll(OTHER_SLOTS);
        for (String id : all) {
            if (!d.slotSizes().containsKey(id)) out.add("slot type \"" + id + "\" has no file in data/nusmp/curios/slots (the mod would rely on another mod to define it)");
            else if (!d.defines(id)) out.add("slot type \"" + id + "\" is defined with a size below one: nothing can be worn in it");
            if (!d.assigned().contains(id)) out.add("slot type \"" + id + "\" is not given to the player by any data/nusmp/curios/entities file");
            if (!d.tags().containsKey(id)) out.add("slot type \"" + id + "\" has no item tag file data/curios/tags/item/" + id + ".json");
        }
        return out;
    }

    /**
     * What is wrong for these relics (item id such as {@code nusmp:ring_of_x} to {@link RelicSlot} name): each Curios relic's slot must be defined (by the mod, or at least a slot Curios is known to ship), given to the
     * player, and list the relic in its tag; an armour relic's slot must have an equipment slot. Hand and consumable relics need none of this. Empty means every relic can be put on.
     */
    public static List<String> checkRelics(Map<String, String> relicSlots, Data d) {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, String> e : new TreeMap<>(relicSlots).entrySet()) {
            String id = e.getKey(), name = e.getValue();
            RelicSlot slot = RelicSlot.byName(name);
            if (slot == null) { out.add(id + ": unknown relic slot " + name); continue; }
            if (slot.isArmor()) {
                if (armourEquipSlot(name) == null) out.add(id + ": armour relic without an equipment slot");
                continue;
            }
            String curios = slot.curiosSlot();
            if (curios == null) continue;
            if (!d.defines(curios) && !CURIOS_SHIPPED.contains(curios)) out.add(id + ": its Curios slot \"" + curios + "\" is defined nowhere");
            else if (!d.defines(curios)) out.add(id + ": its Curios slot \"" + curios + "\" is not defined by this mod (it relies on Curios shipping it)");
            if (!d.assigned().contains(curios)) out.add(id + ": slot \"" + curios + "\" is not given to the player");
            Set<String> tag = d.tags().get(curios);
            if (tag == null || !tag.contains(id)) out.add(id + ": not listed in data/curios/tags/item/" + curios + ".json");
        }
        return out;
    }

    // ---------------------------------------------------------------- reading the files (a class path or a folder; JUnit and the self-test share this)
    /** Reads a text resource or returns null. */
    public interface Source {
        InputStream open(String path) throws IOException;
    }

    private static final String SLOTS = "data/nusmp/curios/slots/", ENTITIES = "data/nusmp/curios/entities/", TAGS = "data/curios/tags/item/";

    /** The entity files the mod ships. A new one is added here and nowhere else. */
    public static final List<String> ENTITY_FILES = List.of("relics", "dungeon_relic", "celestial_key");

    /** Reads the slot, entity and tag files of the mod through {@code src}; a file that is missing is simply not in the result (the checks name it). */
    public static Data read(Source src) throws IOException {
        Map<String, Integer> sizes = new HashMap<>();
        Map<String, Set<String>> tags = new HashMap<>();
        Set<String> assigned = new HashSet<>();
        List<String> ids = new ArrayList<>(RELIC_SLOTS);
        ids.addAll(OTHER_SLOTS);
        for (String id : ids) {
            JsonObject slot = json(src, SLOTS + id + ".json");
            if (slot != null) sizes.put(id, slot.has("size") ? slot.get("size").getAsInt() : 1);
            JsonObject tag = json(src, TAGS + id + ".json");
            if (tag != null) tags.put(id, tagValues(tag));
        }
        for (String f : ENTITY_FILES) {
            JsonObject ent = json(src, ENTITIES + f + ".json");
            if (ent == null || !ent.has("slots")) continue;
            boolean player = false;
            for (JsonElement el : ent.getAsJsonArray("entities")) if ("player".equals(el.getAsString())) player = true;
            if (player) for (JsonElement el : ent.getAsJsonArray("slots")) assigned.add(el.getAsString());
        }
        return new Data(sizes, tags, assigned);
    }

    private static Set<String> tagValues(JsonObject tag) {
        Set<String> out = new TreeSet<>();
        JsonArray values = tag.has("values") ? tag.getAsJsonArray("values") : new JsonArray();
        for (JsonElement v : values) out.add(v.isJsonObject() ? v.getAsJsonObject().get("id").getAsString() : v.getAsString());
        return out;
    }

    private static JsonObject json(Source src, String path) throws IOException {
        try (InputStream in = src.open(path)) {
            if (in == null) return null;
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    /** A source over a class loader (the jar in the game, the resources folder in a test). */
    public static Source classPath(ClassLoader cl) { return path -> cl.getResourceAsStream(path); }
}
