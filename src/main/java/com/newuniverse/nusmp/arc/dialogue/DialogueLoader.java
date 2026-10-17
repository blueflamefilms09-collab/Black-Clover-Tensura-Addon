package com.newuniverse.nusmp.arc.dialogue;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: loads {@code data/nusmp/arc/dialogue/<character_id>.json} once per data reload into an immutable snapshot of {@link DialogueBook}s, so talking in
 * game never touches the disk, and a datapack that supplies the same file replaces a character's words. A broken file is reported (the log, and
 * {@code /multiverse arc info}) and skipped, never fatal. The file {@value #FALLBACK_ID} is what any character without a file of its own says; if that
 * is missing too, a one-line book built into the code is used, so a character is never mute. Registered by {@code ArcSystem.init} through
 * {@code AddReloadListenerEvent}.
 */
public final class DialogueLoader implements ResourceManagerReloadListener {
    private static final Logger LOG = LogUtils.getLogger();
    public static final String NAMESPACE = "nusmp", ROOT = "arc/dialogue", FALLBACK_ID = DialogueBook.FALLBACK_ID;

    /** Used only when even {@value #FALLBACK_ID} could not be read. */
    private static final String LAST_RESORT = "{\"lines\":{\"greeting\":[\"...\"],\"hint\":[\"...\"]}}";

    private static volatile Map<String, DialogueBook> books = Map.of();
    private static volatile Map<String, List<String>> problems = Map.of();
    private static final DialogueBook BUILT_IN = DialogueBook.parse(FALLBACK_ID, LAST_RESORT).book();

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        Map<String, DialogueBook> found = new LinkedHashMap<>();
        Map<String, List<String>> bad = new LinkedHashMap<>();
        try {
            Map<ResourceLocation, Resource> files = manager.listResources(ROOT, rl -> rl.getNamespace().equals(NAMESPACE) && rl.getPath().endsWith(".json"));
            List<ResourceLocation> keys = new ArrayList<>(files.keySet());
            keys.sort(java.util.Comparator.comparing(ResourceLocation::toString));
            for (ResourceLocation rl : keys) {
                String id = idOf(rl.getPath());
                if (id == null) continue;
                try (InputStream in = files.get(rl).open()) {
                    DialogueBook.Parsed p = DialogueBook.parse(id, new String(in.readAllBytes(), StandardCharsets.UTF_8));
                    if (!p.problems().isEmpty()) bad.put(id, p.problems());
                    if (p.ok()) found.put(id, p.book());
                    else LOG.warn("[nusmp] arc dialogue file {} skipped: {}", rl, String.join("; ", p.problems()));
                } catch (IOException | RuntimeException e) {
                    bad.put(id, List.of("could not be read: " + e));
                    LOG.warn("[nusmp] arc dialogue file {} could not be read: {}", rl, e.toString());
                }
            }
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] arc dialogue could not scan its data folder", e);
        }
        for (Map.Entry<String, List<String>> e : bad.entrySet()) if (found.containsKey(e.getKey())) LOG.info("[nusmp] arc dialogue {}: {}", e.getKey(), String.join("; ", e.getValue()));
        books = Collections.unmodifiableMap(found);
        problems = Collections.unmodifiableMap(bad);
    }

    /** {@code arc/dialogue/yami.json} gives {@code yami}; a file in a sub-folder is not a character's. */
    static String idOf(String path) {
        String p = path.startsWith(ROOT + "/") ? path.substring(ROOT.length() + 1) : path;
        if (!p.endsWith(".json")) return null;
        p = p.substring(0, p.length() - 5);
        return p.isEmpty() || p.contains("/") ? null : p;
    }

    /** The book of a character, or null when it has no file of its own. */
    public static DialogueBook book(String characterId) { return characterId == null ? null : books.get(characterId); }

    public static boolean has(String characterId) { return book(characterId) != null; }

    /** What a character without a book says. Never null. */
    public static DialogueBook fallback() {
        DialogueBook b = books.get(FALLBACK_ID);
        return b != null ? b : BUILT_IN;
    }

    /** The book to use for a character: its own, else the fallback. */
    public static DialogueBook bookOrFallback(String characterId) {
        DialogueBook b = book(characterId);
        return b != null ? b : fallback();
    }

    /** The problems found in each file at the last reload (a file with problems may still be in use). */
    public static Map<String, List<String>> problems() { return problems; }

    public static int loaded() { return books.size(); }

    /** Forgets everything (server stopped). */
    public static void clear() {
        books = Map.of();
        problems = Map.of();
    }
}
