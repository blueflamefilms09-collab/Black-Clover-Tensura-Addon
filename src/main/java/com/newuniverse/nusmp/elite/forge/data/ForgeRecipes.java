package com.newuniverse.nusmp.elite.forge.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.elite.forge.rules.ForgeRecipeDef;
import com.newuniverse.nusmp.elite.forge.rules.SkillBonusTable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Why: the forge's recipes and its skill-bonus table are data ({@code data/<ns>/elite/forge/recipes/*.json}, {@code data/nusmp/elite/forge/skill_bonus.json}), read
 * once per data reload into an immutable snapshot so that nothing touches the disk while playing. A recipe id is {@code <namespace>:<file name>}; a pack that ships a
 * file with the same path replaces the recipe. Each recipe is checked against the item registries: a recipe naming an item that does not exist (a Tensura id of
 * another version, a removed mod) is kept in the list but marked {@code available = false} with the missing ids named, and logged once, so a typo never breaks
 * the others. A broken file is skipped with a log line, never fatal. Registered through {@code AddReloadListenerEvent} by the registry.
 */
public final class ForgeRecipes implements ResourceManagerReloadListener {
    private static final Logger LOG = LogUtils.getLogger();
    static final String ROOT = "elite/forge/recipes";

    /** A parsed recipe and whether every item it names exists. */
    public record Loaded(ForgeRecipeDef def, boolean available, List<String> missing) {
        public Item outputItem() { return BuiltInRegistries.ITEM.get(ResourceLocation.parse(def.output().item())); }
    }

    private static volatile Map<String, Loaded> recipes = Collections.emptyMap();
    private static volatile SkillBonusTable skills = SkillBonusTable.fallback();

    public static Map<String, Loaded> all() { return recipes; }

    public static Loaded get(String id) { return id == null ? null : recipes.get(id); }

    public static SkillBonusTable skillTable() { return skills; }

    /** Replaces the snapshot (used by the reload and by the self-test). */
    public static void install(Map<String, Loaded> map, SkillBonusTable table) {
        recipes = Collections.unmodifiableMap(new LinkedHashMap<>(map));
        skills = table == null ? SkillBonusTable.fallback() : table;
    }

    /** Checks the items of a parsed recipe against the item registry. */
    public static Loaded check(ForgeRecipeDef def) {
        List<String> missing = new ArrayList<>();
        test(def.output().item(), missing);
        for (ForgeRecipeDef.Stack s : def.inputs()) test(s.item(), missing);
        for (ForgeRecipeDef.Premium p : def.premium()) test(p.item(), missing);
        return new Loaded(def, missing.isEmpty(), List.copyOf(missing));
    }

    private static void test(String id, List<String> missing) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) {
            if (!missing.contains(id)) missing.add(id);
        }
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        Map<String, Loaded> out = new LinkedHashMap<>();
        try {
            Map<ResourceLocation, Resource> found = manager.listResources(ROOT, rl -> rl.getPath().endsWith(".json"));
            List<ResourceLocation> keys = new ArrayList<>(found.keySet());
            keys.sort(Comparator.comparing(ResourceLocation::toString));
            for (ResourceLocation rl : keys) {
                String path = rl.getPath().substring(ROOT.length() + 1, rl.getPath().length() - ".json".length());
                if (path.isEmpty() || path.contains("/")) continue;
                String id = rl.getNamespace() + ":" + path;
                try (Reader in = found.get(rl).openAsReader()) {
                    JsonObject o = JsonParser.parseReader(in).getAsJsonObject();
                    Loaded l = check(ForgeRecipeDef.parse(id, o));
                    if (!l.available()) LOG.warn("[nusmp] forge recipe {} is unavailable: unknown items {}", id, l.missing());
                    out.put(id, l);
                } catch (IOException | RuntimeException e) {
                    LOG.warn("[nusmp] forge recipe file {} skipped: {}", rl, e.toString());
                }
            }
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] forge recipes could not scan their data folder", e);
        }
        SkillBonusTable table = null;
        try {
            Optional<Resource> res = manager.getResource(ResourceLocation.fromNamespaceAndPath("nusmp", "elite/forge/skill_bonus.json"));
            if (res.isPresent()) {
                try (Reader in = res.get().openAsReader()) {
                    table = SkillBonusTable.parse(JsonParser.parseReader(in).getAsJsonObject());
                }
            }
        } catch (IOException | RuntimeException e) {
            LOG.warn("[nusmp] forge skill table could not be read, using the built-in one: {}", e.toString());
        }
        install(out, table);
        LOG.info("[nusmp] forge: {} recipes loaded", out.size());
    }

    /** The recipes a station of the given tier can show, in file order, as a list. */
    public static List<Loaded> list() { return new ArrayList<>(recipes.values()); }
}
