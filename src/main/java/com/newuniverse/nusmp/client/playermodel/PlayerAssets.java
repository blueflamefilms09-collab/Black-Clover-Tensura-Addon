package com.newuniverse.nusmp.client.playermodel;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Why: the kit's data (the tables of looks, the hair styles, the resource locations of skins and sheets) is read from the resource packs once per reload, not per frame, and a missing or broken
 * file must never throw in the render thread: it is logged once and the character is drawn with the default look. This is the one place that holds those caches ({@link #look},
 * {@link #hair}, {@link #skin}, {@link #exists}); {@link #init} clears them on a model reload, so a resource pack that replaces a table, a skin or a hair sheet takes effect on F3+T.
 *
 * <p>1.99.0 (phase 2): the looks come from every table of {@link PlayerLookTables#FILES} (the arcs', the abyssal bosses', the dungeon's), merged add-only; and the lookups of the render thread no
 * longer build a key string per call: every cache is a map of its own keyed by the plain name, so a frame allocates nothing here, and {@link #generation} lets a body cache what it derived from a
 * look (the foundry's look of an id) until the next reload.
 */
public final class PlayerAssets {
    private PlayerAssets() {}

    private static final Logger LOG = LogUtils.getLogger();
    public static final String LOOKS = PlayerLookTables.FILES.get(0), HAIR = "playermodel/hair_styles.json";
    private static final Map<String, PlayerLook> LOOKS_BY_ID = new HashMap<>();
    private static final Map<String, HairStyleData.Style> HAIR_BY_ID = new HashMap<>();
    private static final Map<String, ResourceLocation> SKINS = new HashMap<>(), GLOWS = new HashMap<>(), EXTRAS = new HashMap<>(), EXTRAS_GLOW = new HashMap<>(), HAIR_SHEETS = new HashMap<>();
    private static final Map<ResourceLocation, Boolean> EXISTS = new HashMap<>();
    private static final Set<String> WARNED = new HashSet<>();
    private static boolean loadedLooks, loadedHair;
    private static int generation;

    /** Adds the reload hook (call once from the client constructor). */
    public static void init(IEventBus modBus) { modBus.addListener((ModelEvent.BakingCompleted e) -> clear()); }

    public static synchronized void clear() {
        LOOKS_BY_ID.clear();
        HAIR_BY_ID.clear();
        SKINS.clear();
        GLOWS.clear();
        EXTRAS.clear();
        EXTRAS_GLOW.clear();
        HAIR_SHEETS.clear();
        EXISTS.clear();
        loadedLooks = false;
        loadedHair = false;
        generation++;
    }

    /** Counts the reloads: anything derived from the looks (a body's cache of the look of an id) is valid for as long as this number is the same. */
    public static synchronized int generation() { return generation; }

    public static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath(SkinPath.NAMESPACE, path); }

    private static JsonObject readJson(String path, boolean optional) {
        try {
            Optional<Resource> r = Minecraft.getInstance().getResourceManager().getResource(rl(path));
            if (r.isEmpty()) {
                if (!optional) warnOnce("missing:" + path, "[nusmp] player-model data file {} not found: characters use the default look", path);
                return null;
            }
            try (Reader in = r.get().openAsReader()) {
                return JsonParser.parseReader(in).getAsJsonObject();
            }
        } catch (Exception ex) {
            warnOnce("broken:" + path, "[nusmp] player-model data file {} could not be read ({}): characters use the default look", path, ex.toString());
            return null;
        }
    }

    private static void warnOnce(String key, String msg, Object... args) {
        if (WARNED.add(key)) LOG.warn(msg, args);
    }

    private static void loadLooks() {
        loadedLooks = true;
        for (int i = 0; i < PlayerLookTables.FILES.size(); i++) {
            String path = PlayerLookTables.FILES.get(i);
            JsonObject o = readJson(path, i > 0);                  // looks.json is required; the abyssal and dungeon tables are optional
            if (o == null) continue;
            try {
                List<String> problems = new ArrayList<>();
                PlayerLookTables.merge(LOOKS_BY_ID, PlayerLook.parseTable(o), path, problems);
                for (String p : problems) warnOnce("dup:" + p, "[nusmp] {}", p);
            } catch (RuntimeException ex) {
                warnOnce("looks-parse:" + path, "[nusmp] {} is malformed ({}): its characters use the default look", path, ex.toString());
            }
        }
    }

    private static void loadHair() {
        loadedHair = true;
        JsonObject o = readJson(HAIR, false);
        if (o == null) return;
        try {
            HAIR_BY_ID.putAll(HairStyleData.parse(o));
        } catch (RuntimeException ex) {
            warnOnce("hair-parse", "[nusmp] hair_styles.json is malformed ({}): characters are drawn bald", ex.toString());
        }
    }

    /** The built-in last resort when even the default look is missing: a plain skin, no hair, no extras. */
    private static final PlayerLook EMPTY = new PlayerLook("npc_default", SkinPath.DEFAULT_ID, false, false, PlayerLook.WHITE, "", 0x3A281C, 1f, 1f, java.util.List.of(), "", 0f, 0f);

    /** The look of an id, or the default look when there is no such id (a character added after the table was written). */
    public static synchronized PlayerLook look(String id) {
        if (!loadedLooks) loadLooks();
        PlayerLook l = id == null ? null : LOOKS_BY_ID.get(id);
        if (l != null) return l;
        if (id != null && !id.isEmpty()) warnOnce("nolook:" + id, "[nusmp] no player-model look '{}': drawn with the default look", id);
        PlayerLook d = LOOKS_BY_ID.get(SkinPath.DEFAULT_ID);
        return d != null ? d : EMPTY;
    }

    /** Is there a look with this id (no fallback, no warning)? */
    public static synchronized boolean hasLook(String id) {
        if (!loadedLooks) loadLooks();
        return id != null && LOOKS_BY_ID.containsKey(id);
    }

    /** The look of an id, or null when there is none (no fallback, no warning): for a renderer that chooses between the kit and an older renderer. */
    public static synchronized PlayerLook lookOrNull(String id) {
        if (!loadedLooks) loadLooks();
        return id == null ? null : LOOKS_BY_ID.get(id);
    }

    /** The hair style of an id, or null for none (bald, an unknown id). */
    public static synchronized HairStyleData.Style hair(String id) {
        if (id == null || id.isEmpty() || id.equals("bald")) return null;
        if (!loadedHair) loadHair();
        HairStyleData.Style s = HAIR_BY_ID.get(id);
        if (s == null) warnOnce("nohair:" + id, "[nusmp] no hair style '{}': drawn bald", id);
        return s;
    }

    /** The texture location of a skin name or path (cached); the default skin when the name is not valid. */
    public static synchronized ResourceLocation skin(String name) {
        ResourceLocation rl = SKINS.get(name);
        if (rl == null) {
            String path = SkinPath.resolve(name);
            if (path.isEmpty()) path = SkinPath.defaultPath();
            rl = rl(path);
            SKINS.put(name, rl);
        }
        return rl;
    }

    /** The glow map next to a skin. */
    public static synchronized ResourceLocation glowOf(String skinName) {
        ResourceLocation rl = GLOWS.get(skinName);
        if (rl == null) {
            String p = SkinPath.glowOf(SkinPath.resolve(skinName));
            rl = rl(p.isEmpty() ? SkinPath.glowOf(SkinPath.defaultPath()) : p);
            GLOWS.put(skinName, rl);
        }
        return rl;
    }

    /** A texture under textures/entity/ by plain name (the textures of extras). */
    public static synchronized ResourceLocation extraTexture(String name, boolean glow) {
        Map<String, ResourceLocation> cache = glow ? EXTRAS_GLOW : EXTRAS;
        ResourceLocation rl = cache.get(name);
        if (rl == null) {
            String p = SkinPath.extraTexture(glow ? name + "_glow" : name);
            rl = rl(p.isEmpty() ? SkinPath.defaultPath() : p);
            cache.put(name, rl);
        }
        return rl;
    }

    /** The hair sheet of a style. */
    public static synchronized ResourceLocation hairSheet(HairStyleData.Style s) {
        ResourceLocation rl = HAIR_SHEETS.get(s.id());
        if (rl == null) {
            String p = SkinPath.hairSheet(s.texture());
            rl = rl(p.isEmpty() ? SkinPath.defaultPath() : p);
            HAIR_SHEETS.put(s.id(), rl);
        }
        return rl;
    }

    /** Does the resource exist? (cached; nothing is logged for a miss: glow maps are optional). */
    public static synchronized boolean exists(ResourceLocation rl) {
        Boolean known = EXISTS.get(rl);
        if (known != null) return known;
        boolean ok;
        try {
            ok = Minecraft.getInstance().getResourceManager().getResource(rl).isPresent();
        } catch (RuntimeException ex) {
            ok = false;
        }
        EXISTS.put(rl, ok);
        return ok;
    }
}
