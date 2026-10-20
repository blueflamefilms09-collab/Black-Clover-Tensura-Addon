package com.newuniverse.nusmp.client.gui.bossbar;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.boss.bars.BarStyle;
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
 * Why: the client's table of boss bar styles, read from {@code assets/nusmp/boss_bars/*.json} (so a resource pack can restyle or add one) and rebuilt on every resource reload. Each loaded style
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

        Loaded(BarStyle style) {
            this.style = style;
            this.atlas = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/gui/boss_bar/frame_" + style.id() + ".png");
            int i = Math.max(0, style.emblemIndex());
            this.emblemU = (i % 8) * 32;
            this.emblemV = (i / 8) * 32;
            this.entrance = Math.max(0, BarStyle.ENTRANCES.indexOf(style.entrance()));
            this.death = Math.max(0, BarStyle.DEATHS.indexOf(style.death()));
        }
    }

    public static final ResourceLocation EMBLEMS = tex("emblems"), FILLFX = tex("fillfx"), CHAINS = tex("chains");

    private static volatile Map<String, Loaded> styles = Map.of();

    private static ResourceLocation tex(String n) { return ResourceLocation.fromNamespaceAndPath("nusmp", "textures/gui/boss_bar/" + n + ".png"); }

    /** The style of an id, or null when it is not loaded. */
    public static Loaded get(String id) { return styles.get(id); }

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
                LOGGER.warn("[nusmp] boss bar style {} skipped: {}", e.getKey(), ex.getMessage());
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
