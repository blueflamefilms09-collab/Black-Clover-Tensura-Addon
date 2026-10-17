package com.newuniverse.nusmp.labyrinth.mega.flow;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Why: the table that says which casts are Light and which are Dark is data ({@code data/nusmp/mega/flow/alignments.json}), so a datapack can retune a
 * family, add a word or move a magic to neutral without a mod update, and a data reload ({@code /reload}, a world load) must rebuild it. A plain
 * {@code ResourceManagerReloadListener} (the pattern this repo already compiles for the avatar races and the Mega factions) reads the file, hands the
 * parsed tree to the pure {@link MagicAlignments#parse}, logs each complaint once with its file, and installs the table in one step. The file the
 * resource manager ranks highest wins as a whole (a pack that wants to add one word ships the whole file). A missing or unreadable file installs the
 * EMPTY table and says so in the log: every cast is then neutral, so the feature does nothing instead of doing something wrong. The Gson tree is
 * converted to plain maps and lists here, and only here, so the parser stays library-free and testable.
 */
public final class AlignmentReload implements ResourceManagerReloadListener {
    private static final Logger LOG = LogUtils.getLogger();
    /** The file, as a resource location in the server data folders: {@code data/nusmp/mega/flow/alignments.json}. */
    static final ResourceLocation FILE = ResourceLocation.fromNamespaceAndPath("nusmp", "mega/flow/alignments.json");

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        MagicAlignments table = MagicAlignments.EMPTY;
        try {
            Optional<Resource> res = manager.getResource(FILE);
            if (res.isEmpty()) {
                LOG.warn("[nusmp] Mega flow: {} is missing, so every cast reads as neutral and the Rukh flow does nothing", FILE);
            } else {
                try (Reader reader = res.get().openAsReader()) {
                    JsonElement root = JsonParser.parseReader(reader);
                    Object tree = root.isJsonObject() ? toTree(root.getAsJsonObject()) : null;
                    @SuppressWarnings("unchecked") MagicAlignments.Parsed parsed = MagicAlignments.parse((Map<String, Object>) tree);
                    for (String p : parsed.problems()) LOG.warn("[nusmp] Mega flow file {}: {}", FILE, p);
                    table = parsed.table();
                }
            }
        } catch (Exception ex) {
            LOG.warn("[nusmp] Mega flow file {} could not be read, so every cast reads as neutral: {}", FILE, ex.toString());
        }
        AlignmentRegistry.install(table);
        LOG.info("[nusmp] Loaded the Mega-Dungeon Rukh flow table: {} light words, {} dark words, {} exact entries", table.lightKeywords().size(), table.darkKeywords().size(), table.exactEntries().size());
    }

    /** A Gson element as plain Java: objects to insertion-ordered maps, arrays to lists, numbers to Double, booleans, strings, null. */
    static Object toTree(JsonElement el) {
        if (el == null || el.isJsonNull()) return null;
        if (el.isJsonObject()) {
            JsonObject o = el.getAsJsonObject();
            Map<String, Object> m = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> e : o.entrySet()) m.put(e.getKey(), toTree(e.getValue()));
            return m;
        }
        if (el.isJsonArray()) {
            JsonArray a = el.getAsJsonArray();
            List<Object> l = new ArrayList<>();
            for (JsonElement x : a) l.add(toTree(x));
            return l;
        }
        JsonPrimitive p = el.getAsJsonPrimitive();
        if (p.isBoolean()) return p.getAsBoolean();
        if (p.isNumber()) return p.getAsDouble();
        return p.getAsString();
    }
}
