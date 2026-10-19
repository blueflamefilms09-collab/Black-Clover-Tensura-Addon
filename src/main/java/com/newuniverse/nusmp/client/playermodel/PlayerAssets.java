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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Why: the kit's data (the table of looks, the hair styles, the resource locations of skins and sheets) is read from the resource packs once per reload, not per frame, and a missing or broken
 * file must never throw in the render thread: it is logged once and the character is drawn with the default look. This is the one place that holds those caches ({@link #look},
 * {@link #hair}, {@link #skin}, {@link #exists}); {@link #init} clears them on a model reload, so a resource pack that replaces looks.json, a skin or a hair sheet takes effect on F3+T.
 */
public final class PlayerAssets {
    private PlayerAssets() {}

    private static final Logger LOG = LogUtils.getLogger();
    public static final String LOOKS = "playermodel/looks.json", HAIR = "playermodel/hair_styles.json";
    private static final Map<String, PlayerLook> LOOKS_BY_ID = new HashMap<>();
    private static final Map<String, HairStyleData.Style> HAIR_BY_ID = new HashMap<>();
    private static final Map<String, ResourceLocation> LOCATIONS = new HashMap<>();
    private static final Map<ResourceLocation, Boolean> EXISTS = new HashMap<>();
    private static final Set<String> WARNED = new HashSet<>();
    private static boolean loadedLooks, loadedHair;

    /** Adds the reload hook (call once from the client constructor). */
    public static void init(IEventBus modBus) { modBus.addListener((ModelEvent.BakingCompleted e) -> clear()); }

    public static synchronized void clear() {
        LOOKS_BY_ID.clear();
        HAIR_BY_ID.clear();
        LOCATIONS.clear();
        EXISTS.clear();
        loadedLooks = false;
        loadedHair = false;
    }

    public static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath(SkinPath.NAMESPACE, path); }

    private static JsonObject readJson(String path) {
        try {
            Optional<Resource> r = Minecraft.getInstance().getResourceManager().getResource(rl(path));
            if (r.isEmpty()) {
                warnOnce("missing:" + path, "[nusmp] player-model data file {} not found: characters use the default look", path);
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
        JsonObject o = readJson(LOOKS);
        if (o == null) return;
        try {
            LOOKS_BY_ID.putAll(PlayerLook.parseTable(o));
        } catch (RuntimeException ex) {
            warnOnce("looks-parse", "[nusmp] looks.json is malformed ({}): characters use the default look", ex.toString());
        }
    }

    private static void loadHair() {
        loadedHair = true;
        JsonObject o = readJson(HAIR);
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
        String key = "skin:" + name;
        ResourceLocation rl = LOCATIONS.get(key);
        if (rl == null) {
            String path = SkinPath.resolve(name);
            if (path.isEmpty()) path = SkinPath.defaultPath();
            rl = rl(path);
            LOCATIONS.put(key, rl);
        }
        return rl;
    }

    /** The glow map next to a skin. */
    public static synchronized ResourceLocation glowOf(String skinName) {
        String key = "glow:" + skinName;
        ResourceLocation rl = LOCATIONS.get(key);
        if (rl == null) {
            String p = SkinPath.glowOf(SkinPath.resolve(skinName));
            rl = rl(p.isEmpty() ? SkinPath.glowOf(SkinPath.defaultPath()) : p);
            LOCATIONS.put(key, rl);
        }
        return rl;
    }

    /** A texture under textures/entity/ by plain name (the textures of extras). */
    public static synchronized ResourceLocation extraTexture(String name, boolean glow) {
        String key = (glow ? "xg:" : "x:") + name;
        ResourceLocation rl = LOCATIONS.get(key);
        if (rl == null) {
            String p = SkinPath.extraTexture(glow ? name + "_glow" : name);
            rl = rl(p.isEmpty() ? SkinPath.defaultPath() : p);
            LOCATIONS.put(key, rl);
        }
        return rl;
    }

    /** The hair sheet of a style. */
    public static synchronized ResourceLocation hairSheet(HairStyleData.Style s) {
        String key = "hair:" + s.id();
        ResourceLocation rl = LOCATIONS.get(key);
        if (rl == null) {
            String p = SkinPath.hairSheet(s.texture());
            rl = rl(p.isEmpty() ? SkinPath.defaultPath() : p);
            LOCATIONS.put(key, rl);
        }
        return rl;
    }

    /** Does the resource exist? (cached; nothing is logged for a miss: glow maps are optional). */
    public static synchronized boolean exists(ResourceLocation rl) {
        return EXISTS.computeIfAbsent(rl, k -> {
            try {
                return Minecraft.getInstance().getResourceManager().getResource(k).isPresent();
            } catch (RuntimeException ex) {
                return false;
            }
        });
    }
}
