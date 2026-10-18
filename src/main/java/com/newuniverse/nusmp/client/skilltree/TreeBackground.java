package com.newuniverse.nusmp.client.skilltree;

import com.newuniverse.nusmp.client.skilltree.layout.Rect;
import com.newuniverse.nusmp.client.ui.theme.UiMath;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Why: each line has its own picture behind its tree (a magma cavern, a void, a frozen waste, ...), generated into {@code assets/nusmp/textures/gui/skill_trees/<line id>.png} by another job, and the
 * canvas must show it without ever depending on it: a missing file, a file that is not a PNG, or a route that names no picture all fall back to the plain canvas the screen always had, with no error and
 * no purple-and-black square. So the lookup is defensive and happens once per route: the resource is opened a single time, its width and height are read from the PNG header (the first 24 bytes; the texture
 * size is needed to draw a part of it) and the answer, present or not, is remembered. A route that gives no {@code gui_background} is looked up under the default path made from its own id, so the eight
 * older routes get their picture the day the file exists, with no data change. The picture is drawn to COVER the canvas (scaled so that no edge is empty, centred, shifted a little against the pan for depth),
 * clipped to the canvas, then darkened with a flat veil and a stronger fade toward the bottom, so the nodes and lines stay readable on any picture. Drawing allocates nothing: the cached info is a record
 * found by the route's id string. Cleared on leaving the world (the next world may ship other pictures).
 */
final class TreeBackground {
    private TreeBackground() {}

    /** A texture that exists, with its size; {@code source} is the route field it came from, to notice a changed route. */
    private record Info(ResourceLocation texture, int w, int h, String source) {}

    private static final Info MISSING = new Info(null, 0, 0, "");
    private static final Map<String, Info> CACHE = new HashMap<>();
    /** The veil over the picture: flat everywhere, a bit stronger when the line is not owned (inspect mode). */
    private static final float VEIL = 0.58f, VEIL_SEALED = 0.74f;

    /** The texture a route should have: its own {@code gui_background}, else the default path made from its id. */
    static ResourceLocation locationOf(SkillRoute r) {
        if (r.guiBackground() != null && !r.guiBackground().isEmpty()) {
            ResourceLocation named = ResourceLocation.tryParse(r.guiBackground());
            if (named != null) return named;
        }
        int colon = r.id().indexOf(':');
        String ns = colon > 0 ? r.id().substring(0, colon) : "nusmp", path = colon > 0 ? r.id().substring(colon + 1) : r.id();
        return ResourceLocation.fromNamespaceAndPath(ns, "textures/gui/skill_trees/" + path + ".png");
    }

    private static Info info(SkillRoute r) {
        String source = r.guiBackground() == null ? "" : r.guiBackground();
        Info known = CACHE.get(r.id());
        if (known != null && known.source().equals(source)) return known;
        Info made = probe(locationOf(r), source);
        CACHE.put(r.id(), made);
        return made;
    }

    /** Opens the resource once and reads the PNG header. Anything unexpected is "missing". */
    private static Info probe(ResourceLocation rl, String source) {
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(rl);
            if (res.isEmpty()) return new Info(null, 0, 0, source);
            try (InputStream in = res.get().open()) {
                byte[] h = in.readNBytes(24);
                if (h.length < 24 || (h[0] & 0xFF) != 0x89 || h[1] != 'P' || h[2] != 'N' || h[3] != 'G') return new Info(null, 0, 0, source);
                int w = ((h[16] & 0xFF) << 24) | ((h[17] & 0xFF) << 16) | ((h[18] & 0xFF) << 8) | (h[19] & 0xFF);
                int ht = ((h[20] & 0xFF) << 24) | ((h[21] & 0xFF) << 16) | ((h[22] & 0xFF) << 8) | (h[23] & 0xFF);
                if (w < 16 || ht < 16 || w > 8192 || ht > 8192) return new Info(null, 0, 0, source);
                return new Info(rl, w, ht, source);
            }
        } catch (Exception | LinkageError e) {
            return new Info(null, 0, 0, source);
        }
    }

    /** True when the route has a usable picture. */
    static boolean has(SkillRoute r) { return r != null && info(r).texture() != null; }

    /**
     * Draws the picture of {@code route} over {@code vp} (call after the canvas well, before the magic circle and the nodes). Returns false and draws nothing when there is no picture, so the caller keeps
     * its plain look.
     */
    static boolean draw(GuiGraphics g, Rect vp, SkillRoute route, float panX, float panY, boolean owned) {
        if (route == null || vp.w() < 8 || vp.h() < 8) return false;
        Info in = info(route);
        if (in.texture() == null) return false;
        float scale = Math.max(vp.w() / (float) in.w(), vp.h() / (float) in.h()) * 1.06f;      // cover, with a little spare so the shift never shows an edge
        int srcW = Math.min(in.w(), (int) Math.ceil(vp.w() / scale)), srcH = Math.min(in.h(), (int) Math.ceil(vp.h() / scale));
        float u = (in.w() - srcW) / 2f - panX * 0.04f, v = (in.h() - srcH) / 2f - panY * 0.04f;
        u = Math.max(0f, Math.min(in.w() - srcW, u));
        v = Math.max(0f, Math.min(in.h() - srcH, v));
        g.enableScissor(vp.x() + 1, vp.y() + 1, vp.right() - 1, vp.bottom() - 1);
        g.blit(in.texture(), vp.x(), vp.y(), vp.w(), vp.h(), u, v, srcW, srcH, in.w(), in.h());
        g.fill(vp.x(), vp.y(), vp.right(), vp.bottom(), UiMath.withAlpha(0x05070E, owned ? VEIL : VEIL_SEALED));
        g.fillGradient(vp.x(), vp.y() + vp.h() / 2, vp.right(), vp.bottom(), UiMath.withAlpha(0x05070E, 0f), UiMath.withAlpha(0x05070E, 0.35f));
        g.disableScissor();
        return true;
    }

    /** Forgets what was looked up (the player left the world). */
    static void clear() { CACHE.clear(); }
}
