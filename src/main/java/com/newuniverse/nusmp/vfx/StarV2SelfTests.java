package com.newuniverse.nusmp.vfx;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the Star V2 look (placed stars, lines, Conjunction, pane, lance, whip, dome) is drawn by the client and cannot be seen in a game by its author, so the real game proves what can be proved without eyes
 * (the CI server smoke test runs it; key prefix {@code magic.star_nodes}, key {@code magic.star_nodes_look}): (1) the seven shapes it owns are exactly the appended ones and STAR_MAGIC is not among them;
 * (2) the worst case of every effect is under its cap and its vertex budget is the cap in quads times four, never over the activation limit of 400; (3) all 24 textures are in the jar with the right size
 * and clear corners; (4) the options default to V2 / flashes on and parse V1; (5) the reduced-flashes rules and the per-frame cost of placed stars (far stars, the frame ceiling). Pure checks: nothing touches a
 * client class, so it runs on the dedicated server.
 */
public final class StarV2SelfTests {
    private StarV2SelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.star_nodes_look", StarV2SelfTests::check);
    }

    private static Result check(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        List<VfxShape> owned = StarV2.SHAPES;
        if (owned.size() != 7 || !owned.equals(com.newuniverse.nusmp.star.StarFx.SHAPES) || owned.contains(VfxShape.STAR_MAGIC)) bad.add("the owned shapes are wrong: " + owned);
        int worst = 0;
        for (VfxShape s : owned) {
            int q = StarV2.worstQuads(s);
            worst = Math.max(worst, q);
            if (q <= 0 || q > StarV2.capQuads(s)) bad.add(s + " draws up to " + q + " quads, over the cap " + StarV2.capQuads(s));
            if (StarV2.vertexBudget(s) != StarV2.capQuads(s) * 4 || StarV2.vertexBudget(s) > 400) bad.add(s + " has a vertex budget of " + StarV2.vertexBudget(s));
        }
        int checked = 0;
        for (String path : StarV2.allTextures()) {
            String file = path.substring(path.lastIndexOf('/') + 1, path.length() - 4);
            String name = file.substring(StarV2.PREFIX.length());
            try {
                SquareBugAudit.Pixels px = SquareBugAudit.particle(file);
                if (px == null) { bad.add("missing texture " + file); continue; }
                String problem = StarV2.checkTexture(name, px.w(), px.h(), px.argb());
                if (problem != null) bad.add(file + ": " + problem);
                checked++;
            } catch (java.io.IOException | RuntimeException e) {
                bad.add(file + " cannot be read: " + e);
            }
        }
        if (StarV2.DEFAULT_STYLE != StarV2.Style.V2 || StarV2.DEFAULT_REDUCED_FLASHES || StarV2.parse("garbage") != StarV2.Style.V2 || StarV2.parse(null) != StarV2.Style.V2
                || StarV2.parse("v1") != StarV2.Style.V1 || StarV2.parse(" V2 ") != StarV2.Style.V2)
            bad.add("the vfx.element.star option does not default to V2 / parse V1, or reduced flashes is not off by default");
        if (StarV2.hardFlash(0, 5, true) != 0f || StarV2.hardFlash(0, 5, false) != 1f || StarV2.hardFlash(5, 5, false) != 0f || StarV2.softFlash(0, 5, false) != 0f
                || StarV2.softFlash(0, 5, true) != 0.5f || StarV2.softFlash(5, 5, true) > StarV2.softFlash(2, 5, true) || StarV2.framesPerSecond(true) > 3f
                || StarV2.twinkle(3f, 0.5f, 0.4f, true) != StarV2.twinkle(9f, 0.5f, 0.4f, true) || StarV2.spin(1f, true) != 0f)
            bad.add("the reduced-flash rules are wrong");
        if (StarV2.nodeQuads(10, 0) != StarV2.NODE_QUADS_NEAR || StarV2.nodeQuads(40, 0) != StarV2.NODE_QUADS_FAR || StarV2.nodeQuads(10, 48) != StarV2.NODE_QUADS_FAR || StarV2.nodeQuads(70, 0) != 0)
            bad.add("the placed-star level of detail is wrong");
        if (StarV2.worstFrameQuads(120, 8) > 600) bad.add("120 stars and 8 panes in one view cost " + StarV2.worstFrameQuads(120, 8) + " quads");
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(8, bad.size()))));
        return Result.ok(checked + " textures (clear corners); worst quads per effect " + worst + " (every one under its cap, vertex budget under 400); 120 stars + 8 panes = " + StarV2.worstFrameQuads(120, 8)
                + " quads a frame; default V2, V1 restores the old beam; reduced flashes drop the hard flash, rays and spin");
    }
}
