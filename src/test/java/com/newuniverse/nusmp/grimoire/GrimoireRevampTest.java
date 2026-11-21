package com.newuniverse.nusmp.grimoire;

import com.newuniverse.nusmp.anim.CastAnim;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The revamped grimoire (docs/grimoire_revamp.md), headless: the Blender mesh parses and is well formed, every look stays inside the item cube and under the NEW quad budget, the baked
 * atlases cover every quad's texel, the classic plan is untouched, and the effect numbers and the level-of-detail tiers hold their bounds. Plus a few source pins (the setting, the
 * tier check in the floating renderer, the fixed particle counts).
 */
class GrimoireRevampTest {
    private static final File TEX = new File("src/main/resources/assets/anime_multiverse/textures/item/grimoire_book");
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");

    private static List<BookLook.Key> keys(int open) {
        List<BookLook.Key> out = new java.util.ArrayList<>();
        for (String c : new String[]{"THREE_LEAF", "FOUR_LEAF", "FIVE_LEAF", "SPADE", "HEART", "DIAMOND", "GOD_TIER"}) {
            for (boolean held : new boolean[]{false, true}) {
                out.add(new BookLook.Key(c, BookMotif.forCover(c), c.equals("FIVE_LEAF"), held, open));
            }
        }
        return out;
    }

    @Test
    void theMeshParsesAndIsComplete() {
        GrimoireRevampPlan.Mesh m = GrimoireRevampPlan.mesh();
        assertNotNull(m, "revamp_mesh.txt is missing or broken");
        assertTrue(m.poses().get("closed").size() > 150, "the closed book is a real model");
        for (int s = 1; s <= 3; s++) assertTrue(m.poses().get("open" + s).size() > 150, "open step " + s);
        assertEquals(m.poses().get("open1").size(), m.poses().get("open3").size(), "the three opening steps are the same halves turned");
        assertTrue(m.covers().get("closed.front").z() > 9.4f && m.covers().get("closed.back").z() < 6.6f, "the cover planes sit outside the old boards (thicker, layered cover)");
    }

    @Test
    void aBrokenMeshFileIsRefusedAndTheClassicPlanTakesOver() {
        assertThrows(IllegalArgumentException.class, () -> GrimoireRevampPlan.parse(new BufferedReader(new StringReader("q L 1 2 3\n"))));
        assertThrows(IllegalArgumentException.class, () -> GrimoireRevampPlan.parse(new BufferedReader(new StringReader("mesh closed\n"))));
        assertFalse(GrimoireBookPlan.build(new BookLook.Key("THREE_LEAF", BookMotif.THREE_LEAF, false, false)).isEmpty());
    }

    @Test
    void everyLookIsInsideTheCubeUnderTheBudgetAndWoundOutward() {
        int worstClosed = 0, worstOpen = 0;
        for (int open = 0; open <= GrimoireBookPlan.OPEN_STEPS; open++) {
            for (BookLook.Key key : keys(open)) {
                List<GrimoireBookPlan.Quad> quads = GrimoireRevampPlan.build(key);
                if (open == 0) worstClosed = Math.max(worstClosed, quads.size()); else worstOpen = Math.max(worstOpen, quads.size());
                assertTrue(quads.size() <= (open == 0 ? GrimoireRevampPlan.MAX_CLOSED_QUADS : GrimoireRevampPlan.MAX_OPEN_QUADS), "quad count " + quads.size() + " open " + open);
                Set<String> known = new HashSet<>(GrimoireBookPlan.textures());
                known.addAll(GrimoireRevampPlan.textures());
                boolean glow = false;
                for (GrimoireBookPlan.Quad q : quads) {
                    float[] p = q.pos();
                    for (float v : p) assertTrue(v >= 0 && v <= 16, "outside the item cube: " + v + " open " + open + " " + q.texture());
                    for (float u : q.uv()) assertTrue(u >= -0.001f && u <= 16.001f, "uv outside the texture: " + u);
                    assertTrue(known.contains(q.texture()), "unknown texture " + q.texture());
                    float[] n = q.normal();
                    assertEquals(1f, (float) Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]), 1e-3f);
                    float[] axis = GrimoireBookPlan.axis(q.face());
                    assertTrue(n[0] * axis[0] + n[1] * axis[1] + n[2] * axis[2] > 0.5f, "face is the nearest axis of the normal");
                    float ax = p[3] - p[0], ay = p[4] - p[1], az = p[5] - p[2], bx = p[6] - p[0], by = p[7] - p[1], bz = p[8] - p[2];
                    float cx = ay * bz - az * by, cy = az * bx - ax * bz, cz = ax * by - ay * bx;
                    float len = (float) Math.sqrt(cx * cx + cy * cy + cz * cz);
                    if (len > 1e-6f) assertTrue((cx * n[0] + cy * n[1] + cz * n[2]) / len > 0.99f, "winding disagrees with the normal on " + q.texture());
                    glow |= q.emissive();
                }
                assertEquals(key.held(), glow, "only a held book glows");
            }
        }
        assertTrue(worstClosed > 200, "the revamp is the detailed model (worst closed " + worstClosed + ")");
        assertTrue(worstOpen <= GrimoireRevampPlan.MAX_OPEN_QUADS && worstClosed <= GrimoireRevampPlan.MAX_CLOSED_QUADS);
    }

    @Test
    void onlyAHeldOpenBookShowsTheSpellRunes() {
        for (int step = 1; step <= GrimoireBookPlan.OPEN_STEPS; step++) {
            assertTrue(GrimoireRevampPlan.build(new BookLook.Key("FOUR_LEAF", BookMotif.FOUR_LEAF, false, true, step)).stream().anyMatch(q -> q.texture().equals("page_runes")), "runes at step " + step);
            assertTrue(GrimoireRevampPlan.build(new BookLook.Key("FOUR_LEAF", BookMotif.FOUR_LEAF, false, false, step)).stream().noneMatch(q -> q.texture().equals("page_runes")));
        }
    }

    @Test
    void aTatteredBookHasNoFrameRingAndUsesTheWornAtlas() {
        List<GrimoireBookPlan.Quad> plain = GrimoireRevampPlan.build(new BookLook.Key("FOUR_LEAF", BookMotif.FOUR_LEAF, false, false));
        List<GrimoireBookPlan.Quad> torn = GrimoireRevampPlan.build(new BookLook.Key("FIVE_LEAF", BookMotif.TATTERED, true, false));
        assertTrue(torn.size() < plain.size(), "the frame ring is dropped on a tattered book");
        assertTrue(torn.stream().anyMatch(q -> q.texture().equals(GrimoireRevampPlan.ATLAS_TATTERED)));
        assertTrue(plain.stream().noneMatch(q -> q.texture().equals(GrimoireRevampPlan.ATLAS_TATTERED)));
    }

    @Test
    void theAtlasesAreThereAndCoverEveryBodyQuad() throws IOException {
        for (String atlas : GrimoireRevampPlan.textures()) {
            File f = new File(TEX, atlas + ".png");
            assertTrue(f.isFile(), "missing atlas " + atlas);
            BufferedImage img = ImageIO.read(f);
            assertEquals(512, img.getWidth());
            assertEquals(512, img.getHeight());
            int bad = 0, total = 0;
            for (GrimoireBookPlan.Quad q : GrimoireRevampPlan.build(new BookLook.Key("THREE_LEAF", BookMotif.THREE_LEAF, false, false))) {
                if (!q.texture().equals(atlas)) continue;
                float u = 0, v = 0;
                for (int i = 0; i < 4; i++) { u += q.uv()[i * 2] / 64f; v += q.uv()[i * 2 + 1] / 64f; }       // centroid, 0..1
                int px = Math.min(511, Math.max(0, (int) (u * 512))), py = Math.min(511, Math.max(0, (int) (v * 512)));
                total++;
                if ((img.getRGB(px, py) >>> 24) < 200) bad++;
            }
            if (atlas.equals(GrimoireRevampPlan.ATLAS)) assertTrue(total > 200, "the atlas is used by the body");
            assertTrue(bad <= total / 50, bad + " of " + total + " quads sample an empty texel of " + atlas);
        }
    }

    @Test
    void theClassicPlanIsStillTheOldBook() {
        BookLook.Key key = new BookLook.Key("THREE_LEAF", BookMotif.THREE_LEAF, false, false);
        int n = GrimoireBookPlan.build(key).size();
        assertTrue(n > 40 && n < 200, "classic closed quad count " + n);
        for (int open = 1; open <= 3; open++) assertTrue(GrimoireBookPlan.build(new BookLook.Key("THREE_LEAF", BookMotif.THREE_LEAF, false, true, open)).size() < 420);
        assertTrue(GrimoireBookPlan.build(key).stream().noneMatch(q -> q.texture().startsWith("revamp")), "the classic plan never touches the revamp atlas");
    }

    // ---------------------------------------------------------------- the effect numbers and the LOD
    @Test
    void tiersAndBudgets() {
        assertEquals(GrimoireRevampMath.Tier.FULL, GrimoireRevampMath.tier(23.9 * 23.9));
        assertEquals(GrimoireRevampMath.Tier.LOD, GrimoireRevampMath.tier(24.1 * 24.1));
        assertEquals(GrimoireRevampMath.Tier.LOD, GrimoireRevampMath.tier(63.9 * 63.9));
        assertEquals(GrimoireRevampMath.Tier.CULL, GrimoireRevampMath.tier(64.1 * 64.1));
        assertTrue(GrimoireRevampMath.SUMMON_BURST <= 16 && GrimoireRevampMath.maxCast() <= 12, "a few dozen particles at most per cast");
        assertEquals(GrimoireRevampMath.maxCast(), Math.max(GrimoireRevampMath.burstFor(1f), Math.max(GrimoireRevampMath.burstFor(0.65f), GrimoireRevampMath.burstFor(0.35f))));
        assertEquals(0, GrimoireRevampMath.burstFor(0f));
    }

    @Test
    void flareStrengthFollowsTheRealClipNames() {
        assertEquals(1f, GrimoireRevampMath.flareStrength(CastAnim.SIGNATURE));
        for (String c : new String[]{CastAnim.THRUST, CastAnim.SIDE, CastAnim.UP, CastAnim.SWEEP, CastAnim.SLAM, CastAnim.WEAPON_SWORD, CastAnim.WEAPON_HEAVY, CastAnim.WEAPON_KATANA, CastAnim.WEAPON_DANCE}) assertEquals(0.65f, GrimoireRevampMath.flareStrength(c), c);
        assertEquals(0.35f, GrimoireRevampMath.flareStrength(CastAnim.CHANT));
        assertEquals(0.35f, GrimoireRevampMath.flareStrength(CastAnim.MANA_ZONE));
        for (String c : new String[]{CastAnim.FAIL, CastAnim.KEY_TURN, CastAnim.DICE_TOSS, CastAnim.SAIYAN_CHARGE, "", null}) assertEquals(0f, GrimoireRevampMath.flareStrength(c), String.valueOf(c));
    }

    @Test
    void flareEnvelopeAndAlphasStayInRange() {
        assertEquals(0f, GrimoireRevampMath.flare(-1f));
        assertEquals(0f, GrimoireRevampMath.flare(GrimoireRevampMath.FLARE_LIFE));
        float peak = 0;
        for (float a = 0; a < GrimoireRevampMath.FLARE_LIFE; a += 0.5f) {
            float f = GrimoireRevampMath.flare(a);
            assertTrue(f >= 0f && f <= 1f);
            peak = Math.max(peak, f);
            for (float open : new float[]{0f, 0.33f, 1f}) {
                for (float strength : new float[]{0f, 0.35f, 1f}) {
                    float ring = GrimoireRevampMath.ringAlpha(open, f, strength), halo = GrimoireRevampMath.haloAlpha(open, f, strength, a * 3);
                    assertTrue(ring >= 0f && ring <= 1f && halo >= 0f && halo <= 1f);
                }
            }
        }
        assertEquals(1f, peak, 1e-6f);
        assertEquals(0f, GrimoireRevampMath.ringAlpha(0f, 0f, 0f), "a closed book has no ring");
        assertTrue(GrimoireRevampMath.ringAlpha(1f, 1f, 1f) > GrimoireRevampMath.ringAlpha(1f, 0f, 1f), "a flare brightens the ring");
        assertTrue(GrimoireRevampMath.ringRadius(1f, 1f, 0f) > GrimoireRevampMath.ringRadius(0f, 1f, 0f), "a flare swells the ring");
    }

    @Test
    void accentSlotsAndFlutterGate() {
        assertEquals(2, GrimoireRevampMath.accentSlot("WIND", true), "anti-magic crackles whatever its soul");
        assertEquals(2, GrimoireRevampMath.accentSlot("FLAME", false));
        assertEquals(3, GrimoireRevampMath.accentSlot("WATER", false));
        assertEquals(1, GrimoireRevampMath.accentSlot("WIND", false));
        assertEquals(0, GrimoireRevampMath.accentSlot("LIGHT", false));
        assertEquals(0, GrimoireRevampMath.accentSlot(null, false));
        assertFalse(GrimoireRevampMath.flutterDue(100f, 0f, 2), "only a fully open book flutters");
        assertFalse(GrimoireRevampMath.flutterDue(100f, 95f, 3));
        assertTrue(GrimoireRevampMath.flutterDue(100f, 91f, 3));
    }

    // ---------------------------------------------------------------- source pins
    @Test
    void theSettingTheLodAndTheEffectsStayWired() throws IOException {
        String cfg = Files.readString(SRC.resolve("grimoire/GrimoireRevampConfig.java"));
        assertTrue(cfg.contains("b.push(\"visual\")") && cfg.contains("\"classicGrimoires\"") && cfg.contains(".define(\"classicGrimoires\", false)"), "visual.classicGrimoires, default false");
        assertTrue(Files.readString(SRC.resolve("NUSMP.java")).contains("GrimoireRevampConfig.SPEC, \"nusmp-grimoire-client.toml\""), "the setting is registered as a CLIENT config");
        String model = Files.readString(SRC.resolve("client/grimoire/GrimoireBakedModel.java"));
        assertTrue(model.contains("GrimoireRevampConfig.classic()") && model.contains("GrimoireItem.classicView(stack)"), "the baked model honours the setting and the LOD copy");
        String fl = Files.readString(SRC.resolve("client/grimoire/GrimoireFloatClient.java"));
        assertTrue(fl.contains("GrimoireRevampMath.tier(") && fl.contains("Tier.CULL) continue") && fl.contains("lod(stack)"), "the floating renderer culls and level-of-details by distance");
        assertTrue(fl.contains("GrimoireRevampFx.draw("), "the effects are drawn for the open book");
        String fx = Files.readString(SRC.resolve("client/grimoire/GrimoireRevampFx.java"));
        assertEquals(1, fx.split("fromNamespaceAndPath", -1).length - 1, "only the one static halo location, none built per frame");
        assertFalse(fx.contains("ParticleTypes."), "particles go through the Fx facade");
        assertTrue(fx.contains("PerfProbe.begin()") && fx.contains("Fx.sprayTinted("), "the layer is profiled and uses the particle facade");
        assertTrue(fx.contains("GrimoireRevampConfig.classic()"), "visual.classicGrimoires switches the layer off");
        assertTrue(Files.readString(SRC.resolve("client/CastAnimClient.java")).contains("GrimoireClient.onCastClip("), "a cast flares the book through the existing body-animation packet");
        for (String common : new String[]{"grimoire/GrimoireRevampPlan.java", "grimoire/GrimoireRevampMath.java", "grimoire/GrimoireRevampConfig.java", "grimoire/GrimoireRevampSelfTests.java"}) {
            String s = Files.readString(SRC.resolve(common));
            assertFalse(s.contains("net.minecraft.client") || s.contains("com.mojang.blaze3d") || s.contains("nusmp.client."), common + " must stay free of client classes");
        }
    }

    @Test
    void theLangFragmentAndPreviewsExist() {
        assertTrue(new File("tools/lang/grimoire_revamp.json").isFile(), "the lang fragment");
        assertTrue(new File("docs/grimoire_revamp.md").isFile(), "the doc");
        assertTrue(new File("docs/previews/grimoire_revamp").isDirectory(), "the previews");
    }
}
