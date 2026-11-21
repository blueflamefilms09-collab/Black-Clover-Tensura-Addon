package com.newuniverse.nusmp.grimoire;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;

/**
 * Self-test {@code visual.grimoire_revamp} of the CI server smoke test (docs/grimoire_revamp.md): inside the booted mod, the Blender-made grimoire mesh is in the jar and parses, every look of
 * the revamped plan (closed and the three open steps, held or not, tattered or not) stays inside the item cube and under the quad budget, the baked atlases are there and 512 px, and the
 * level-of-detail tiers and effect numbers are sane. Plain data and the JDK image reader only: runs on a dedicated server, touches no client class and no player state.
 */
public final class GrimoireRevampSelfTests {
    private GrimoireRevampSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("visual.grimoire_revamp", server -> run());
    }

    private static SelfTests.Result run() throws Exception {
        GrimoireRevampPlan.Mesh mesh = GrimoireRevampPlan.mesh();
        if (mesh == null) return SelfTests.Result.fail("the grimoire revamp mesh is missing or does not parse");
        int worst = 0, looks = 0;
        for (String emblem : new String[]{"THREE_LEAF", "FIVE_LEAF", "SPADE", "HEART", "GOD_TIER"}) {
            for (boolean held : new boolean[]{false, true}) {
                for (int open = 0; open <= GrimoireBookPlan.OPEN_STEPS; open++) {
                    BookLook.Key key = new BookLook.Key(emblem, BookMotif.forCover(emblem), emblem.equals("FIVE_LEAF"), held, open);
                    List<GrimoireBookPlan.Quad> quads = GrimoireRevampPlan.build(mesh, key);
                    int budget = open == 0 ? GrimoireRevampPlan.MAX_CLOSED_QUADS : GrimoireRevampPlan.MAX_OPEN_QUADS;
                    if (quads.size() < 100 || quads.size() > budget) return SelfTests.Result.fail(emblem + " open " + open + ": " + quads.size() + " quads (budget " + budget + ")");
                    worst = Math.max(worst, quads.size());
                    for (GrimoireBookPlan.Quad q : quads) {
                        for (float v : q.pos()) if (v < 0f || v > 16f) return SelfTests.Result.fail("a quad leaves the item cube: " + v);
                        for (float v : q.uv()) if (v < -0.001f || v > 16.001f) return SelfTests.Result.fail("a uv leaves the texture: " + v);
                    }
                    looks++;
                }
            }
        }
        for (String atlas : GrimoireRevampPlan.textures()) {
            try (InputStream in = GrimoireRevampSelfTests.class.getResourceAsStream("/assets/anime_multiverse/textures/item/grimoire_book/" + atlas + ".png")) {
                if (in == null) return SelfTests.Result.fail("the atlas " + atlas + " is missing");
                BufferedImage img = ImageIO.read(in);
                if (img.getWidth() != 512 || img.getHeight() != 512) return SelfTests.Result.fail("the atlas " + atlas + " is not 512 px");
            }
        }
        if (GrimoireRevampMath.tier(0) != GrimoireRevampMath.Tier.FULL || GrimoireRevampMath.tier(30.0 * 30.0) != GrimoireRevampMath.Tier.LOD
                || GrimoireRevampMath.tier(70.0 * 70.0) != GrimoireRevampMath.Tier.CULL) return SelfTests.Result.fail("the level-of-detail tiers are wrong");
        if (GrimoireRevampMath.maxCast() > 12 || GrimoireRevampMath.SUMMON_BURST > 16) return SelfTests.Result.fail("an effect burst is over the few-dozen-particles rule");
        return SelfTests.Result.ok(looks + " looks, worst " + worst + " quads, 2 atlases");
    }
}
