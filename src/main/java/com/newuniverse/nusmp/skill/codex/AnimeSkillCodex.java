package com.newuniverse.nusmp.skill.codex;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The live Anime Skill Codex. Shipped skills load from {@code data/nusmp/skills/codex/*.json} (a file holds one skill, or
 * {"skills": [...]}); skills an op accepted from the inbox load from {@code config/nusmp/codex/}. Every skill is validated by
 * {@link SkillSandbox}; a rejected one is logged and skipped, never half-loaded.
 */
public final class AnimeSkillCodex {
    private AnimeSkillCodex() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static volatile Map<String, AnimeSkill> skills = Collections.emptyMap();

    public static Path inbox() { return FMLPaths.CONFIGDIR.get().resolve("nusmp").resolve("codex-inbox"); }
    public static Path accepted() { return FMLPaths.CONFIGDIR.get().resolve("nusmp").resolve("codex"); }

    public static Collection<AnimeSkill> all() { return skills.values(); }
    public static AnimeSkill get(String id) { return skills.get(id); }

    /** Registered with AddReloadListenerEvent: re-reads the codex whenever data packs reload. */
    public static final class Loader extends SimpleJsonResourceReloadListener {
        public Loader() { super(new Gson(), "skills/codex"); }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
            Map<String, AnimeSkill> out = new LinkedHashMap<>();
            files.forEach((file, json) -> readInto(out, json, file.toString()));
            readAccepted(out);
            skills = out;
            LOG.info("[nusmp] Anime Skill Codex: {} skills", out.size());
        }
    }

    static void readInto(Map<String, AnimeSkill> out, JsonElement json, String source) {
        List<JsonObject> objects = new ArrayList<>();
        if (json.isJsonObject() && json.getAsJsonObject().has("skills") && json.getAsJsonObject().get("skills").isJsonArray())
            json.getAsJsonObject().getAsJsonArray("skills").forEach(e -> { if (e.isJsonObject()) objects.add(e.getAsJsonObject()); });
        else if (json.isJsonObject()) objects.add(json.getAsJsonObject());
        for (JsonObject o : objects) {
            try {
                AnimeSkill s = AnimeSkill.parse(o);
                String why = SkillSandbox.validate(s);
                if (why != null) LOG.warn("[nusmp] codex skill rejected ({}): {}", source, why);
                else out.put(s.id(), s);
            } catch (RuntimeException e) {
                LOG.warn("[nusmp] codex skill unreadable ({}): {}", source, e.toString());
            }
        }
    }

    private static void readAccepted(Map<String, AnimeSkill> out) {
        Path dir = accepted();
        if (!Files.isDirectory(dir)) return;
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                try (Reader r = Files.newBufferedReader(p)) {
                    readInto(out, com.google.gson.JsonParser.parseReader(r), p.getFileName().toString());
                } catch (IOException | RuntimeException e) {
                    LOG.warn("[nusmp] codex file {} unreadable: {}", p.getFileName(), e.toString());
                }
            });
        } catch (IOException e) {
            LOG.warn("[nusmp] codex folder unreadable: {}", e.toString());
        }
    }

    /** Moves inbox/{id}.json to the accepted folder and adds it to the live codex. Returns an error message, or null on success. */
    public static String accept(String file) {
        if (!file.matches("[A-Za-z0-9_.-]+")) return "bad file name";
        Path from = inbox().resolve(file.endsWith(".json") ? file : file + ".json");
        if (!Files.isRegularFile(from)) return "no such file in config/nusmp/codex-inbox";
        try (Reader r = Files.newBufferedReader(from)) {
            Map<String, AnimeSkill> add = new LinkedHashMap<>();
            readInto(add, com.google.gson.JsonParser.parseReader(r), from.getFileName().toString());
            if (add.isEmpty()) return "the skill failed validation (see the log)";
            Files.createDirectories(accepted());
            Files.move(from, accepted().resolve(from.getFileName()), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Map<String, AnimeSkill> merged = new LinkedHashMap<>(skills);
            merged.putAll(add);
            skills = merged;
            return null;
        } catch (IOException | RuntimeException e) {
            return e.getMessage() == null ? e.toString() : e.getMessage();
        }
    }
}
