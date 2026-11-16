package com.newuniverse.nusmp.boss.rework;

import com.newuniverse.nusmp.boss.rework3.Rework3Rows;
import com.newuniverse.nusmp.client.bossmesh.MeshPose;
import com.newuniverse.nusmp.labyrinth.mega.decennial.DecennialFamilies;
import com.newuniverse.nusmp.labyrinth.mega.decennial.DecennialRules;
import com.newuniverse.nusmp.mesh.EntityMeshData;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Boss rework wave 3 (docs/bosses/rework_wave3.md): the Blender bodies of Lucifero and the God of Order, the 16 appended arc-boss moves, the Decennial tier families. Pure:
 * tables, the game's own mesh parser and file reads (the working directory is the project root).
 */
class BossRework3Test {
    private static final String SRC = "src/main/java/com/newuniverse/nusmp/";
    private static final String ASSETS = "src/main/resources/assets/anime_multiverse/";

    private static String read(String path) throws IOException { return Files.readString(Path.of(path)); }

    private static EntityMeshData mesh(String file) throws IOException {
        try (BufferedReader in = Files.newBufferedReader(Path.of(ASSETS + "entity_meshes/" + file))) {
            return EntityMeshData.parse(in);
        }
    }

    // ================================================================ the bodies
    @Test
    void theNewBodiesFitTheBudgetAndKeepTheirRig() throws IOException {
        for (String id : new String[]{"lucifero", "lucifero_form2", "god_of_order", "god_of_order_gates"}) {
            EntityMeshData m = mesh(id + ".txt");
            assertEquals(id, m.id);
            for (String role : MeshPose.ROLE_NAMES) assertTrue(m.part(role) >= 0 && m.body[m.part(role)].length > 0, id + " part " + role);
            assertTrue(m.quads() <= 2800, id + " within the build budget: " + m.quads());
            assertTrue(m.quads(EntityMeshData.GLOW) > 0, id + " glows");
            float[] h = m.heightBlocks();
            assertTrue(h[0] > -0.2f && h[0] < 0.2f, id + " stands on the floor: " + h[0]);
            EntityMeshData lod = mesh(id + "_lod.txt");
            assertTrue(lod.quads() < m.quads() * 0.6, id + " far copy is lighter: " + lod.quads());
            assertEquals(0, lod.quads(EntityMeshData.GLOW), id + " far copy has no glow pass");
        }
        for (String id : new String[]{"lucifero", "lucifero_form2"}) {
            EntityMeshData m = mesh(id + ".txt");
            assertEquals(m.part("torso"), m.parent[m.part("wing_l")], "the wings hang from the back");
            assertTrue(m.body[m.part("wing_l")].length > 0 && m.body[m.part("wing_r")].length > 0, "both wings have quads");
            assertEquals(2, java.util.stream.IntStream.range(0, m.parts.length).filter(i -> m.parts[i].startsWith("wing_")).count());
        }
    }

    @Test
    void theSkinsAndThePreviews() throws IOException {
        for (String id : new String[]{"lucifero", "lucifero_form2", "god_of_order", "god_of_order_gates"}) {
            for (String s : new String[]{id + ".png", id + "_glow.png"}) {
                BufferedImage img = ImageIO.read(Path.of(ASSETS + "textures/entity/boss_mesh/" + s).toFile());
                assertNotNull(img, s);
                assertEquals(img.getWidth(), img.getHeight(), s + " is square");
                assertTrue(img.getWidth() <= 1024 && Integer.bitCount(img.getWidth()) == 1, s + " is a power of two up to 1024 px");
            }
        }
        for (String p : new String[]{"lucifero_blender.png", "lucifero_form2_blender.png", "god_of_order_blender.png", "god_of_order_gates_blender.png"}) {
            Path f = Path.of("docs/bosses/previews/" + p);
            assertTrue(Files.exists(f), p);
            assertTrue(Files.size(f) < 300 * 1024, p + " under 300 KB");
        }
    }

    @Test
    void theNewPoseActionsStartAndEndAtRestAndTheWingsMove() {
        MeshPose rest = new MeshPose(), p = new MeshPose();
        rest.solve(40f, 0f, 0f, MeshPose.NONE, 0f);
        for (int a : new int[]{MeshPose.BEAM, MeshPose.BOW}) {
            for (float t : new float[]{0f, 1f}) {
                p.solve(40f, 0f, 0f, a, t);
                for (int i = 0; i < rest.rot.length; i++) assertEquals(rest.rot[i], p.rot[i], 1e-3, "action " + a + " at " + t + " joint " + i);
            }
        }
        p.solve(40f, 0f, 0f, MeshPose.BEAM, 0.6f);
        assertTrue(Math.abs(p.get(MeshPose.ARM_R, 0) - rest.get(MeshPose.ARM_R, 0)) > 0.5f, "the staff arm rises for the beam");
        MeshPose a = new MeshPose(), b = new MeshPose();
        a.solve(10f, 0f, 0f, MeshPose.NONE, 0f);
        b.solve(30f, 0f, 0f, MeshPose.NONE, 0f);
        assertTrue(Math.abs(a.get(MeshPose.WING_L, 2) - b.get(MeshPose.WING_L, 2)) > 1e-3f, "the wings beat while he stands");
        assertEquals(MeshPose.BEAM, MeshPose.action("beam"));
        assertEquals(MeshPose.BOW, MeshPose.action("bow"));
        assertEquals(MeshPose.ROAR, MeshPose.action("read"));
        assertEquals(8, MeshPose.ROLES, "the old rigs still have eight roles");
        assertEquals(MeshPose.ALL_ROLES, MeshPose.ALL_ROLE_NAMES.length);
    }

    @Test
    void theSettingsTheRoutingAndTheClientRule() throws IOException {
        String opts = read(SRC + "client/playermodel/PlayerModelOptions.java");
        assertTrue(opts.contains(".define(\"luciferoBlenderModel\", true)") && opts.contains(".define(\"godOfOrderBlenderModel\", true)"), "client settings, on by default");
        assertTrue(opts.contains(".define(\"minotaurBlenderModel\", true)"), "the wave 2 setting stays");
        String routing = read(SRC + "client/bossmesh/BossMeshRouting.java");
        assertTrue(routing.contains("new PlayerModelRenderer<>(ctx, new PlayerBodies.Arc())") && routing.contains("new PlayerModelRenderer<>(ctx, new PlayerBodies.Order())"), "the kit stays as the fallback");
        assertTrue(routing.contains("PlayerModelOptions.blender(id)"), "behind the setting");
        assertTrue(read(SRC + "client/playermodel/PlayerModelRegistry.java").contains("BossMeshRouting.register(e)"), "registered after the kit");
        assertFalse(read(SRC + "mesh/EntityMeshData.java").contains("nusmp.client"), "the parser stays pure common code");
        for (String f : new String[]{"boss_lucifero.py", "boss_god_of_order.py"}) assertTrue(Files.exists(Path.of("tools/blender/" + f)), f);
        assertTrue(read(SRC + "client/bossmesh/BossMeshDraw.java").contains("MeshPose.ALL_ROLES"), "the draw loop moves wings too");
    }

    // ================================================================ the appended moves
    @Test
    void sixteenRowsEachBoundedWithARealShape() throws IOException {
        assertEquals(16, Rework3Rows.ROWS.size());
        Set<String> bosses = new HashSet<>(), moves = new HashSet<>();
        String shapes = read(SRC + "vfx/VfxShape.java");
        String arcSources = allArcSources();
        for (Rework3Rows.Row r : Rework3Rows.ROWS) {
            assertTrue(bosses.add(r.boss()), "one move per boss: " + r.boss());
            assertTrue(moves.add(r.move().id()), "unique move id " + r.move().id());
            assertNull(Rework3Rows.problem(r), r.boss());
            assertTrue(shapes.contains(r.shape()), "VfxShape has " + r.shape());
            assertTrue(arcSources.contains("boss(\"" + r.boss() + "\""), "an arc boss " + r.boss());
            assertTrue(Rework3Rows.is(r.move().id()));
            assertTrue(r.move().minPower() < 60 && !r.move().ultimate(), "a normal move, never an ultimate");
        }
        assertNull(Rework3Rows.of("nobody"));
        assertNull(Rework3Rows.of(null));
    }

    private static String allArcSources() throws IOException {
        StringBuilder b = new StringBuilder();
        try (Stream<Path> s = Files.walk(Path.of(SRC + "arc"))) {
            for (Path p : (Iterable<Path>) s.filter(f -> f.toString().endsWith(".java"))::iterator) b.append(Files.readString(p));
        }
        return b.toString();
    }

    @Test
    void theHooksSettingsAndSelfTest() throws IOException {
        String zb = read(SRC + "entity/zogratis/ZogratisBoss.java");
        assertTrue(zb.contains("protected List<Move> appendedMoves() { return List.of(); }") && zb.contains("all.addAll(extra)"), "appended after moves(), so no index moves");
        assertTrue(read(SRC + "arc/ArcBoss.java").contains("Rework3Moves.forBoss(character().id())"));
        String moves = read(SRC + "boss/rework3/Rework3Moves.java");
        assertTrue(moves.contains("if (row == null || !on()) return List.of();"), "off gives back the old list");
        assertTrue(moves.contains("ReworkFx.plain"), "one effect through the budget");
        String rules = read(SRC + "boss/rules/BossRulesConfig.java");
        assertTrue(rules.contains(".define(\"bossRework3Moves\", true)") && rules.contains(".define(\"bossRework3DecennialMoves\", true)"), "server switches, on by default");
        assertTrue(read(SRC + "boss/rework3/Rework3SelfTests.java").contains("\"boss.rework3\""));
        assertTrue(read(SRC + "boss/BossHooks.java").contains("Rework3SelfTests.register()"));
        String smoke = read(".github/workflows/smoke.yml");
        assertTrue(smoke.contains(",boss.rework1,boss.rework3,boss.stats,tier\""), "boss.rework3 sits right after boss.rework1");
        assertTrue(read(SRC + "arc/sds/DianeBoss.java").contains("Rework3Rows.is(id)"), "Diane's own chooser lets the appended move through");
    }

    // ================================================================ the decennial families
    @Test
    void everyLevelHasAFamilyInsideTheBounds() throws IOException {
        Map<String, DecennialFamilies.Family> fams = DecennialFamilies.parse(read("src/main/resources/data/anime_multiverse/mega/decennial/tier_families.json"));
        assertEquals(10, fams.size(), "two sides, five tiers");
        String shapes = read(SRC + "vfx/VfxShape.java");
        Set<String> look = new HashSet<>();
        for (DecennialFamilies.Family f : fams.values()) {
            assertTrue(shapes.contains(f.shape()), f.id() + " names a real shape");
            assertTrue(f.share() <= DecennialFamilies.MAX_SHARE && f.share() >= DecennialFamilies.MIN_SHARE);
            assertTrue(f.radius() <= DecennialFamilies.MAX_RADIUS && f.radius() >= DecennialFamilies.MIN_RADIUS);
            assertTrue(f.period() >= DecennialFamilies.MIN_PERIOD && f.warn() >= DecennialFamilies.MIN_WARN, "a warning and a gap");
            assertTrue(f.effectTicks() <= DecennialFamilies.MAX_EFFECT_TICKS);
            assertTrue(look.add(f.shape() + "/" + f.rgb()), "ten distinct signatures: " + f.id());
        }
        for (int l = -DecennialRules.LEVELS_PER_SIDE; l <= DecennialRules.LEVELS_PER_SIDE; l++) {
            if (l == 0) continue;
            String id = DecennialFamilies.idOf(l);
            assertTrue(fams.containsKey(id), "level " + l + " -> " + id);
            assertEquals(l < 0 ? "evil" : "holy", id.substring(0, 4));
        }
        assertEquals("evil_1", DecennialFamilies.idOf(-1));
        assertEquals("evil_1", DecennialFamilies.idOf(-20));
        assertEquals("evil_2", DecennialFamilies.idOf(-21));
        assertEquals("holy_5", DecennialFamilies.idOf(100));
    }

    @Test
    void aBadFileCannotMakeAnUnboundedMove() {
        String json = "{\"x\":{\"pattern\":\"circle\",\"effect\":\"slow\",\"shape\":\"S\",\"color\":\"#FFFFFF\",\"radius\":500,\"period\":0,\"warn\":0,\"share\":9,\"effectTicks\":9999,\"label\":\"l\"}}";
        DecennialFamilies.Family f = DecennialFamilies.parse(json).get("x");
        assertEquals(DecennialFamilies.MAX_RADIUS, f.radius(), 1e-9);
        assertEquals(DecennialFamilies.MIN_PERIOD, f.period());
        assertEquals(DecennialFamilies.MIN_WARN, f.warn());
        assertEquals(DecennialFamilies.MAX_SHARE, f.share(), 1e-9);
        assertEquals(DecennialFamilies.MAX_EFFECT_TICKS, f.effectTicks());
    }

    @Test
    void theShapesHitWhereTheyShow() {
        assertTrue(DecennialFamilies.inside(DecennialFamilies.Pattern.CIRCLE, 5, 0, 0, 0, 1, 3, 3));
        assertFalse(DecennialFamilies.inside(DecennialFamilies.Pattern.CIRCLE, 5, 0, 0, 0, 1, 4, 4));
        assertFalse(DecennialFamilies.inside(DecennialFamilies.Pattern.DONUT, 10, 0, 0, 0, 1, 1, 1), "the middle of a donut is safe");
        assertTrue(DecennialFamilies.inside(DecennialFamilies.Pattern.DONUT, 10, 0, 0, 0, 1, 7, 0));
        assertFalse(DecennialFamilies.inside(DecennialFamilies.Pattern.DONUT, 10, 0, 0, 0, 1, 11, 0));
        assertTrue(DecennialFamilies.inside(DecennialFamilies.Pattern.LINE, 16, 0, 0, 0, 1, 1, 10), "along the line");
        assertFalse(DecennialFamilies.inside(DecennialFamilies.Pattern.LINE, 16, 0, 0, 0, 1, 3, 10), "beside the line");
        assertFalse(DecennialFamilies.inside(DecennialFamilies.Pattern.LINE, 16, 0, 0, 0, 1, 0, -3), "behind the caster");
        assertFalse(DecennialFamilies.inside(DecennialFamilies.Pattern.LINE, 16, 0, 0, 0, 1, 0, 17), "past its end");
    }

    @Test
    void theLayerHookAndTheGeneratorAreWired() throws IOException {
        String layer = read(SRC + "labyrinth/mega/decennial/DecennialLayer.java");
        assertTrue(layer.contains("family = new FamilyMoves(DecennialFamilies.of(level))") && layer.contains("family.tick(g, sl, now, players)") && layer.contains("family.reset()"));
        assertTrue(layer.contains("sig.tick(g, sl, now, players, addsAlive, fightTicks)"), "the old signature mechanic stays");
        String fm = read(SRC + "labyrinth/mega/decennial/FamilyMoves.java");
        assertTrue(fm.contains("DecennialFx.hurt(g, p, amount, true)"), "every hit passes the player's hit cap");
        assertTrue(fm.contains("BossRulesConfig.rework3Decennial()"), "behind the server setting");
        assertTrue(read("tools/gen_decennial_roster.py").contains("tier_families.json"), "the data is written by the roster generator");
    }
}
