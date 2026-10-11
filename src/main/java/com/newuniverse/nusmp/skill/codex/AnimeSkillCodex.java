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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Anime Skill Codex: skills loaded from {@code data/nusmp/riven_codex/*.json} (a data reload listener, so it works on a dedicated server),
 * plus the ones an op accepted from {@code config/nusmp/codex-inbox/}. Every skill is validated against {@link SkillSandbox}; a bad file is
 * logged and skipped.
 */
public final class AnimeSkillCodex extends SimpleJsonResourceReloadListener {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Map<String, AnimeSkill> SHIPPED = new ConcurrentHashMap<>();
    private static final Map<String, AnimeSkill> ACCEPTED = new ConcurrentHashMap<>();

    public AnimeSkillCodex() { super(new Gson(), "riven_codex"); }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager rm, ProfilerFiller profiler) {
        SHIPPED.clear();
        for (var e : files.entrySet()) {
            try {
                if (!e.getValue().isJsonObject()) throw new IllegalArgumentException("not an object");
                AnimeSkill s = AnimeSkill.parse(e.getValue().getAsJsonObject());
                SHIPPED.put(s.id(), s);
            } catch (RuntimeException ex) {
                LOG.warn("Riven codex: skipped {}: {}", e.getKey(), ex.getMessage());
            }
        }
        loadAccepted();
        LOG.info("Riven codex: {} skills ({} accepted from the inbox)", all().size(), ACCEPTED.size());
    }

    public static Path inbox() { return FMLPaths.CONFIGDIR.get().resolve("nusmp").resolve("codex-inbox"); }

    public static Path accepted() { return FMLPaths.CONFIGDIR.get().resolve("nusmp").resolve("codex-accepted"); }

    /** Every skill the boss may use. */
    public static List<AnimeSkill> all() {
        List<AnimeSkill> l = new ArrayList<>(SHIPPED.values());
        l.addAll(ACCEPTED.values());
        return l;
    }

    public static AnimeSkill get(String id) {
        AnimeSkill s = ACCEPTED.get(id);
        return s != null ? s : SHIPPED.get(id);
    }

    /** /nusmp riven accept: validates inbox/&lt;file&gt;.json, moves it to the accepted folder and puts it in the live codex. */
    public static String accept(String name) {
        if (!name.matches("[a-z0-9_\\-]+")) return "Bad file name.";
        Path from = inbox().resolve(name + ".json");
        if (!Files.isRegularFile(from)) return "No such file in " + inbox();
        try {
            JsonObject o = com.google.gson.JsonParser.parseString(Files.readString(from, StandardCharsets.UTF_8)).getAsJsonObject();
            AnimeSkill s = AnimeSkill.parse(o);
            Files.createDirectories(accepted());
            Files.move(from, accepted().resolve(name + ".json"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            ACCEPTED.put(s.id(), s);
            return "Accepted " + s.id() + " (" + s.name() + ").";
        } catch (IOException | RuntimeException ex) {
            return "Rejected: " + ex.getMessage();
        }
    }

    private static void loadAccepted() {
        ACCEPTED.clear();
        Path dir = accepted();
        if (!Files.isDirectory(dir)) return;
        try (var files = Files.list(dir)) {
            files.filter(f -> f.toString().endsWith(".json")).forEach(f -> {
                try {
                    AnimeSkill s = AnimeSkill.parse(com.google.gson.JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject());
                    ACCEPTED.put(s.id(), s);
                } catch (IOException | RuntimeException ex) {
                    LOG.warn("Riven codex: skipped accepted {}: {}", f.getFileName(), ex.getMessage());
                }
            });
        } catch (IOException ex) {
            LOG.warn("Riven codex: cannot read {}: {}", dir, ex.getMessage());
        }
    }
}
