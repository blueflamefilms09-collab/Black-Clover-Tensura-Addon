package com.newuniverse.nusmp.avatar.chatter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.avatar.AvatarPersonality;
import com.newuniverse.nusmp.avatar.ChatEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the "massive dictionary" of Avatar speech is data, not code, so packs can add or replace voices. A file
 * {@code data/nusmp/avatar_chatter/<personality>.json} looks like
 * {@code {"personality":"TSUNDERE","events":{"COMBAT_START":["line", ...], ...}}}. This class parses and holds the result as an immutable snapshot
 * (swapped atomically on every reload), and has NO Minecraft imports so the schema test can read the shipped files directly.
 */
public final class ChatterDictionary {
    /** Immutable lines per personality and event. */
    private final Map<AvatarPersonality, Map<ChatEvent, List<String>>> lines;

    public static final ChatterDictionary EMPTY = new ChatterDictionary(new EnumMap<>(AvatarPersonality.class));
    private static volatile ChatterDictionary current = EMPTY;

    private ChatterDictionary(Map<AvatarPersonality, Map<ChatEvent, List<String>>> lines) { this.lines = lines; }

    public static ChatterDictionary current() { return current; }

    public static void install(ChatterDictionary d) { current = d == null ? EMPTY : d; }

    /** The lines for this personality and event, never null (empty when none). */
    public List<String> lines(AvatarPersonality p, ChatEvent e) {
        Map<ChatEvent, List<String>> m = lines.get(p);
        List<String> l = m == null ? null : m.get(e);
        return l == null ? List.of() : l;
    }

    public boolean has(AvatarPersonality p) { return lines.containsKey(p); }

    /** Result of reading one file: the personality and its events, or an error message. */
    public record Parsed(AvatarPersonality personality, Map<ChatEvent, List<String>> events, String error) {
        public boolean ok() { return error == null; }
    }

    /** Parses one personality file. Unknown events and non-string entries are an error, so a typo fails loudly in the test and the log. */
    public static Parsed parseFile(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            AvatarPersonality p = AvatarPersonality.byName(root.has("personality") ? root.get("personality").getAsString() : null);
            if (p == null) return new Parsed(null, Map.of(), "unknown or missing personality");
            JsonObject ev = root.getAsJsonObject("events");
            if (ev == null) return new Parsed(p, Map.of(), "no events object");
            Map<ChatEvent, List<String>> out = new EnumMap<>(ChatEvent.class);
            for (Map.Entry<String, JsonElement> e : ev.entrySet()) {
                ChatEvent ce;
                try { ce = ChatEvent.valueOf(e.getKey()); } catch (IllegalArgumentException bad) { return new Parsed(p, Map.of(), "unknown event " + e.getKey()); }
                JsonArray arr = e.getValue().getAsJsonArray();
                List<String> l = new ArrayList<>();
                for (JsonElement x : arr) {
                    if (!x.isJsonPrimitive() || !x.getAsJsonPrimitive().isString()) return new Parsed(p, Map.of(), "non-string line in " + e.getKey());
                    l.add(x.getAsString());
                }
                out.put(ce, Collections.unmodifiableList(l));
            }
            return new Parsed(p, out, null);
        } catch (RuntimeException bad) {
            return new Parsed(null, Map.of(), "unreadable json: " + bad.getMessage());
        }
    }

    /** Builds a snapshot from parsed files; a later file of the same personality ADDS its lines to the earlier one (packs extend a voice). */
    public static ChatterDictionary build(List<Parsed> files) {
        Map<AvatarPersonality, Map<ChatEvent, List<String>>> all = new EnumMap<>(AvatarPersonality.class);
        for (Parsed f : files) {
            if (!f.ok()) continue;
            Map<ChatEvent, List<String>> m = all.computeIfAbsent(f.personality(), k -> new EnumMap<>(ChatEvent.class));
            for (Map.Entry<ChatEvent, List<String>> e : f.events().entrySet()) {
                List<String> merged = new ArrayList<>(m.getOrDefault(e.getKey(), List.of()));
                for (String s : e.getValue()) if (!merged.contains(s)) merged.add(s);
                m.put(e.getKey(), Collections.unmodifiableList(merged));
            }
        }
        Map<AvatarPersonality, Map<ChatEvent, List<String>>> frozen = new EnumMap<>(AvatarPersonality.class);
        all.forEach((k, v) -> frozen.put(k, Collections.unmodifiableMap(v)));
        return new ChatterDictionary(frozen);
    }
}
