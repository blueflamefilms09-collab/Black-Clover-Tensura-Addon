package com.newuniverse.nusmp.labyrinth.mega.guard;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: which D&D traits a Decennial boss has, and how it is named, is data and not code, so a server owner can retune a boss or add a twenty-first with a datapack
 * ({@code data/<ns>/mega/dnd_bosses/<id>.json}) and no rebuild, the same reason the skill routes are JSON. A profile is a plain record with no Minecraft type, parsed by a small strict
 * reader that says in plain words what is wrong with a file (an unknown action id, a wrong number of forms) and clamps every number through {@link DndRules}, so one bad file costs one boss its
 * profile (it gets the built-in default) and never the whole load. The text of a profile ({@code name}, the three {@code forms}) is the boss's visible identity: the anime and the D&D creature
 * it fuses are listed in {@code anime} and {@code dnd} so that a reader of the file, and the docs, can see the inspiration without guessing.
 */
public record DndProfile(String id, String side, int tier, int variant, String name, List<String> anime, List<String> dnd, List<String> forms, int resistances, int legendaryPoints,
                         List<String> legendary, List<Integer> costs, List<String> lair, boolean antiMagicAura, boolean frightfulAura, boolean absorbMagic, boolean mythic, boolean execution) {

    /** The profile of a boss no file describes: three resistances, three points, two actions, one lair action, nothing else. */
    public static final DndProfile DEFAULT = new DndProfile("default", "holy", 1, 0, "Labyrinth Guardian", List.of(), List.of(), List.of("Guardian", "Awakened", "Ascendant"), 3, 3,
            List.of("tail_sweep", "cantrip"), List.of(1, 1), List.of("tremor"), false, false, false, true, false);

    public int cost(int i) { return costs.get(i); }

    /** The form of a phase (1 to 3). */
    public String form(int phase) { return forms.get(Math.max(0, Math.min(2, phase - 1))); }

    /** Reads a file; throws {@link IllegalArgumentException} with a plain sentence when it is wrong. */
    public static DndProfile parse(String id, JsonObject o) {
        String side = str(o, "side");
        if (!side.equals("holy") && !side.equals("evil")) throw new IllegalArgumentException(id + ": side must be holy or evil, not '" + side + "'");
        int tier = num(o, "tier", 1, 5), variant = num(o, "variant", 0, 1);
        String name = str(o, "name");
        List<String> forms = strings(o, "forms");
        if (forms.size() != 3) throw new IllegalArgumentException(id + ": forms needs exactly three names (phase 1, phase 2, mythic), not " + forms.size());
        List<String> legendary = new ArrayList<>();
        List<Integer> costs = new ArrayList<>();
        JsonArray acts = o.has("legendary_actions") ? o.getAsJsonArray("legendary_actions") : new JsonArray();
        for (JsonElement el : acts) {
            JsonObject a = el.getAsJsonObject();
            String aid = str(a, "id");
            if (!DndRules.LEGENDARY_IDS.contains(aid)) throw new IllegalArgumentException(id + ": unknown legendary action '" + aid + "' (known: " + DndRules.LEGENDARY_IDS + ")");
            legendary.add(aid);
            costs.add(num(a, "cost", 1, 3));
        }
        List<String> lair = strings(o, "lair_actions");
        for (String l : lair) if (!DndRules.LAIR_IDS.contains(l)) throw new IllegalArgumentException(id + ": unknown lair action '" + l + "' (known: " + DndRules.LAIR_IDS + ")");
        return new DndProfile(id, side, tier, variant, name, strings(o, "anime"), strings(o, "dnd"), forms, DndRules.clampResistances(opt(o, "resistances", 3)),
                DndRules.points(opt(o, "legendary_points", 3), false), List.copyOf(legendary), List.copyOf(costs), List.copyOf(lair), flag(o, "anti_magic_aura"), flag(o, "frightful_aura"),
                flag(o, "absorb_magic"), !o.has("mythic") || flag(o, "mythic"), flag(o, "execution"));
    }

    private static String str(JsonObject o, String k) {
        if (!o.has(k) || !o.get(k).isJsonPrimitive()) throw new IllegalArgumentException("missing text field '" + k + "'");
        return o.get(k).getAsString();
    }

    private static int num(JsonObject o, String k, int lo, int hi) {
        if (!o.has(k)) throw new IllegalArgumentException("missing number '" + k + "'");
        int v = o.get(k).getAsInt();
        if (v < lo || v > hi) throw new IllegalArgumentException("'" + k + "' is " + v + ", outside " + lo + ".." + hi);
        return v;
    }

    private static int opt(JsonObject o, String k, int dflt) { return o.has(k) ? o.get(k).getAsInt() : dflt; }

    private static boolean flag(JsonObject o, String k) { return o.has(k) && o.get(k).getAsBoolean(); }

    private static List<String> strings(JsonObject o, String k) {
        List<String> out = new ArrayList<>();
        if (o.has(k)) for (JsonElement el : o.getAsJsonArray(k)) out.add(el.getAsString());
        return List.copyOf(out);
    }
}
