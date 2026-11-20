package com.newuniverse.nusmp.client.gui.bossbar;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.boss.bars.BarStyle;
import com.newuniverse.nusmp.boss.bars.BarStyleIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * Why: the client's table of boss bar styles, read from {@code assets/anime_multiverse/boss_bars/*.json} (so a resource pack can restyle or add one) and rebuilt on every resource reload. Each loaded style
 * keeps the locations of its textures, built once here so the painter never builds a {@link ResourceLocation} in a frame. A file that does not parse is logged and skipped: that style then
 * falls back to the animated bar. Names that start with an underscore ({@code _index.json}) are not styles.
 */
public final class BarStyles implements ResourceManagerReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** A style and its art. */
    public static final class Loaded {
        public final BarStyle style;
        public final ResourceLocation atlas;
        /** Source position of the emblem inside {@code emblems.png}. */
        public final int emblemU, emblemV;
        /** Index in {@link BarStyle#ENTRANCES} / {@link BarStyle#DEATHS}: 0 slide, 1 rise, 2 fade, 3 flash; 0 shatter, 1 dissolve. */
        public final int entrance, death;
        /** Source position of the emblem used from phase {@link #ALT_PHASE} on (the Mangekyo of the second Naruto bar); the same as the main one for the other styles. */
        public final int altEmblemU, altEmblemV;
        /** True for the styles whose phase notches are the 'tomoe' emblem instead of the plain triangles (the second Naruto bar). */
        public final boolean tomoeMarkers;

        Loaded(BarStyle style) {
            this.style = style;
            this.atlas = ResourceLocation.fromNamespaceAndPath("anime_multiverse", "textures/gui/boss_bar/frame_" + style.id() + ".png");
            int i = Math.max(0, style.emblemIndex());
            this.emblemU = (i % 8) * 32;
            this.emblemV = (i / 8) * 32;
            int alt = ALT_EMBLEM.containsKey(style.emblem()) ? BarStyle.EMBLEMS.indexOf(ALT_EMBLEM.get(style.emblem())) : -1;
            this.altEmblemU = alt >= 0 ? (alt % 8) * 32 : this.emblemU;
            this.altEmblemV = alt >= 0 ? (alt / 8) * 32 : this.emblemV;
            this.tomoeMarkers = alt >= 0;
            this.entrance = Math.max(0, BarStyle.ENTRANCES.indexOf(style.entrance()));
            this.death = Math.max(0, BarStyle.DEATHS.indexOf(style.death()));
        }
    }

    public static final ResourceLocation EMBLEMS = tex("emblems"), FILLFX = tex("fillfx"), CHAINS = tex("chains");
    /** The emblem sheet: 8 cells of 32 per row, as many rows as {@link BarStyle#EMBLEMS} needs (it grows when emblems are appended). */
    public static final int SHEET_W = 256, SHEET_H = ((BarStyle.EMBLEMS.size() + 7) / 8) * 32;
    /** The phase from which a style with an alternative emblem shows it (phase III: Kakashi's Mangekyo). */
    public static final int ALT_PHASE = 3;
    /** Emblem -> the emblem shown from {@link #ALT_PHASE} on. */
    private static final Map<String, String> ALT_EMBLEM = Map.of("sharingan_v2", "mangekyo_v2");
    /** Where the tomoe marker sits in {@code emblems.png}. */
    public static final int TOMOE_U = (BarStyle.EMBLEMS.indexOf("tomoe") % 8) * 32, TOMOE_V = (BarStyle.EMBLEMS.indexOf("tomoe") / 8) * 32;

    private static volatile Map<String, Loaded> styles = Map.of();

    private static ResourceLocation tex(String n) { return ResourceLocation.fromNamespaceAndPath("anime_multiverse", "textures/gui/boss_bar/" + n + ".png"); }

    /** The style of an id, or null when it is not loaded. */
    public static Loaded get(String id) { return styles.get(id); }

    /** The id the client actually draws for the id the server named: {@code naruto} becomes {@code naruto_leaf} while the setting is on and that style is loaded. Looks only; nothing is sent. */
    public static String effective(String id) {
        return BarStyleIds.NARUTO.equals(id) && BossBarOptions.narutoLeafBar() && styles.containsKey(BarStyleIds.NARUTO_LEAF) ? BarStyleIds.NARUTO_LEAF : id;
    }

    public static int count() { return styles.size(); }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        Map<String, Loaded> next = new HashMap<>();
        for (Map.Entry<ResourceLocation, Resource> e : manager.listResources("boss_bars", rl -> {
            String p = rl.getPath();
            return p.endsWith(".json") && !p.substring(p.lastIndexOf('/') + 1).startsWith("_");
        }).entrySet()) {
            try (Reader r = e.getValue().openAsReader()) {
                BarStyle s = BarStyle.parse(new String(readAll(r)));
                next.put(s.id(), new Loaded(s));
            } catch (IOException | RuntimeException ex) {
                LOGGER.warn("[anime_multiverse] boss bar style {} skipped: {}", e.getKey(), ex.getMessage());
            }
        }
        styles = Map.copyOf(next);
    }

    private static char[] readAll(Reader r) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[4096];
        int n;
        while ((n = r.read(buf)) > 0) sb.append(buf, 0, n);
        char[] out = new char[sb.length()];
        sb.getChars(0, out.length, out, 0);
        return out;
    }
}
