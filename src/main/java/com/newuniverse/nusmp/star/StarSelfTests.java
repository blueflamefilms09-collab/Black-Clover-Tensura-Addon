package com.newuniverse.nusmp.star;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.skill.NUSkills;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Why: Star nodes are placed, capped, used and cleaned up on the server and cannot be played by the author, so the CI server smoke test runs this in the real game (key prefix {@code magic.star_nodes}):
 * (1) the pure rules (caps and their clamps, oldest-first eviction, the heir gate, what each page consumes, the ally limit, the dual plan, the pane and look geometry); (2) with a stand-in player in the
 * overworld: the per-player cap holds with real prop entities (the evicted body is gone), a star the player no longer owns is dropped when they ask, logout / death / dimension cleanup leaves no body and no record,
 * the pane book bounds and absorbs a bolt; (3) the book: the seven new pages are the LAST family pages of {@code book_star} after the fifteen older ones, in order, with the shapes the layer owns. Nothing here
 * touches a client class; it spawns at most ten small entities and removes them again.
 */
public final class StarSelfTests {
    private StarSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.star_nodes", StarSelfTests::check);
    }

    private static Result check(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        pure(bad);
        String game = game(server, bad);
        book(bad);
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(8, bad.size()))));
        return Result.ok("rules + caps + eviction + heir gate + consume table + panes (pure); " + game + "; seven new pages last in book_star; star.nodes default " + StarRules.DEFAULT_NODES);
    }

    // ---------------------------------------------------------------------------------------------- the pure rules
    private static void pure(List<String> bad) {
        if (StarRules.perPlayerCap(false, 6, 8) != 6 || StarRules.perPlayerCap(true, 6, 8) != 8 || StarRules.perPlayerCap(true, 8, 2) != 8 || StarRules.perPlayerCap(false, 99, 99) != 8) bad.add("per-player caps");
        if (StarRules.serverCap(1) != 16 || StarRules.serverCap(9999) != 512 || StarRules.lifeTicks(1) != 200 || StarRules.placeRange(99) != 32) bad.add("clamps");
        StarNodes n = new StarNodes();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        for (int i = 0; i < 8; i++) n.add(a, new StarNodes.Node(UUID.randomUUID(), "d", i, 0, 0, n.nextSeq(), 100), 6, 10);
        if (n.count(a) != 6 || n.total() != 6) bad.add("the per-player cap does not hold: " + n.count(a));
        for (int i = 0; i < 6; i++) n.add(b, new StarNodes.Node(UUID.randomUUID(), "d", i, 0, 0, n.nextSeq(), 100), 6, 10);
        if (n.total() != 10 || n.count(b) != 6 || n.count(a) != 4) bad.add("the server cap did not evict the oldest of anybody: " + n.total() + " " + n.count(a) + " " + n.count(b));
        if (!n.live(a, "d", 101, null).isEmpty() || n.count(a) != 0) bad.add("expired stars are not dropped");
        if (n.clear(b).size() != 6 || n.total() != 0) bad.add("clear leaks");
        if (StarRules.isHeir(false, 1, 1, true) || StarRules.isHeir(true, 0.9, 1, true) || StarRules.isHeir(true, 1, 1, false) || !StarRules.isHeir(true, 1, 1, true)) bad.add("the heir gate");
        if (!StarRules.consumesStars(StarRules.HASTA) || !StarRules.consumesStars(StarRules.CONJUNCTION) || StarRules.consumesStars(StarRules.SCUTUM) || StarRules.consumesStars(StarRules.FLAGELLUM)) bad.add("the consume table");
        if (StarRules.allyLimit(99, 6) != 5 || StarRules.allyLimit(3, 6) != 3 || StarRules.allyLimit(3, 2) != 1 || StarRules.allyLimit(-4, 6) != 0) bad.add("the ally limit");
        if (StarRules.dualPlan(StarRules.DualMode.FUSION, "STAR", "FLAME", "STAR").extra() != null || StarRules.dualPlan(StarRules.DualMode.SECOND_GRIMOIRE, "WIND", "WIND", "EARTH").extra() != null
                || !"STAR".equals(StarRules.dualPlan(StarRules.DualMode.SECOND_GRIMOIRE, "SAND", "WIND", "STAR").extra()) || StarRules.parseDual("garbage") != StarRules.DualMode.FUSION) bad.add("the dual plan");
        StarPaneBook panes = new StarPaneBook();
        StarMath.Pane geom = StarMath.pane(List.of(new StarMath.P(0, 2, 0), new StarMath.P(4, 2, 0), new StarMath.P(4, 5, 0), new StarMath.P(0, 5, 0)), new StarMath.P(2, 2, 8), 6, 5);
        UUID o = UUID.randomUUID();
        for (int i = 0; i < 12; i++) panes.add(UUID.randomUUID(), i < 6 ? o : UUID.randomUUID(), "d", geom, 10f, 100);
        if (panes.size() > StarRules.PANES_SERVER) bad.add("more than eight panes");
        if (!StarMath.crosses(geom, new StarMath.P(2, 3.5, 6), new StarMath.P(2, 3.5, -2)) || StarMath.crosses(geom, new StarMath.P(20, 3.5, 6), new StarMath.P(20, 3.5, -2))) bad.add("pane geometry");
        if (StarMath.pickByLook(List.of(new StarMath.P(0, 0, 10), new StarMath.P(10, 0, 0)), new StarMath.P(0, 0, 0), new StarMath.P(0, 0, 1), 20, 48) != 0) bad.add("look pick");
        if (StarFx.SHAPES.size() != 7) bad.add("the shapes");
        if (StarDispelRules.clampChance(9) != 1.0 || StarDispelRules.clampChance(-1) != 0.0 || Math.abs(StarDispelRules.chance(0.35, StarDispelRules.Role.PANE, false) - 0.6) > 1e-6
                || StarDispelRules.chance(0.35, StarDispelRules.Role.NODE, true) >= StarDispelRules.chance(0.35, StarDispelRules.Role.NODE, false) || StarDispelRules.breaks(0.0, 0.0) || !StarDispelRules.breaks(1.0, 0.99)) bad.add("the dispel chances");
        if (StarConfig.dispelChance() < 0.0 || StarConfig.dispelChance() > 1.0) bad.add("star.dispelChance out of range");
    }

    // ---------------------------------------------------------------------------------------------- with a stand-in player
    private static String game(MinecraftServer server, List<String> bad) {
        ServerLevel level = server.overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        double y = Math.min(level.getMaxBuildHeight() - 20, spawn.getY() + 24);
        ServerPlayer p = SelfTests.player(level, "star-nodes", spawn.getX() + 0.5, y, spawn.getZ() + 0.5);
        StarGame.forget(p);
        if (!StarConfig.nodes()) return "star.nodes is off on this server: placement not exercised";
        List<UUID> bodies = new ArrayList<>();
        int placed = 0;
        for (int i = 0; i < 9; i++) {
            Vec3 at = new Vec3(spawn.getX() + 0.5 + i, y, spawn.getZ() + 0.5);
            StarGame.Placed r = StarGame.placeAt(p, at, false);
            if (r.node() == null) { bad.add("a star was refused: " + r.problem()); break; }
            bodies.add(r.node().entity());
            placed++;
        }
        int cap = StarConfig.perPlayer(false);
        if (placed == 9 && StarNodes.class != null && StarGame.NODES.count(p.getUUID()) != cap) bad.add("the player holds " + StarGame.NODES.count(p.getUUID()) + " stars, the cap is " + cap);
        int alive = 0;
        for (UUID id : bodies) if (level.getEntity(id) != null) alive++;
        if (placed == 9 && alive != cap) bad.add(alive + " star bodies stand for a cap of " + cap + " (an evicted star must leave)");
        List<StarNodes.Node> live = StarGame.live(p);
        if (placed == 9 && live.size() != cap) bad.add("live() lists " + live.size());
        if (placed == 9 && live.size() >= 2) {
            level.getEntity(live.get(0).entity()).discard();
            if (StarGame.live(p).size() != live.size() - 1) bad.add("a star whose body is gone is still listed");
        }
        StarGame.forget(p);
        int left = 0;
        for (UUID id : bodies) if (level.getEntity(id) != null) left++;
        if (left != 0 || StarGame.NODES.count(p.getUUID()) != 0) bad.add("logout cleanup left " + left + " bodies");
        // the pane absorbs a bolt that crosses it, and only one pane per owner
        StarMath.Pane geom = StarMath.pane(List.of(new StarMath.P(0, 2, 0), new StarMath.P(4, 2, 0), new StarMath.P(4, 5, 0), new StarMath.P(0, 5, 0)), new StarMath.P(2, 2, 8), 6, 5);
        UUID e1 = UUID.randomUUID(), e2 = UUID.randomUUID();
        StarGame.PANES.add(e1, p.getUUID(), StarGame.dimOf(level), geom, 10f, level.getGameTime() + 100);
        List<StarPaneBook.Pane> replaced = StarGame.PANES.add(e2, p.getUUID(), StarGame.dimOf(level), geom, 10f, level.getGameTime() + 100);
        if (replaced.size() != 1 || StarGame.PANES.size() != 1) bad.add("a second pane of one owner must replace the first");
        StarGame.forget(p);
        if (StarGame.PANES.size() != 0) bad.add("pane cleanup");
        String dispel = dispel(level, p, spawn, y, bad);
        StarGame.forget(p);
        return dispel + "; " + placed + " stars placed (cap " + cap + "), evicted bodies removed, cleanup clean";
    }

    // ---------------------------------------------------------------------------------------------- the dispel hook (stars sometimes die to Anti-Magic)
    /**
     * Real stars and a pane in the overworld, dispelled through the real hook with the chance forced: 0 breaks nothing, 1 breaks what is inside the sphere and spares the caster's own, a star outside the
     * sphere stays, a survivor of a roll is left alone for a while, the hook leaves the registries empty after cleanup, and {@code Nullification.shatterBarriers} (the mod's real dispel) reaches the hook.
     */
    private static String dispel(ServerLevel level, ServerPlayer owner, BlockPos spawn, double y, List<String> bad) {
        StarGame.forget(owner);
        Vec3 c = new Vec3(spawn.getX() + 0.5, y, spawn.getZ() + 0.5);
        StarGame.Placed near = StarGame.placeAt(owner, c.add(2, 0, 0), false);
        StarGame.Placed near2 = StarGame.placeAt(owner, c.add(0, 0, 2), false);
        StarGame.Placed far = StarGame.placeAt(owner, c.add(15, 0, 0), false);
        if (near.node() == null || near2.node() == null || far.node() == null) { bad.add("the dispel test could not place its stars: " + near.problem() + far.problem()); return "dispel not exercised"; }
        net.minecraft.util.RandomSource rnd = level.getRandom();
        int none = StarDispel.onDispel(level, c, 6, null, 0.0, rnd);
        if (none != 0 || StarGame.NODES.count(owner.getUUID()) != 3) bad.add("a dispel chance of 0 broke " + none + " stars");
        int own = StarDispel.onDispel(level, c, 6, owner, 1.0, rnd);
        if (own != 0 || StarGame.NODES.count(owner.getUUID()) != 3) bad.add("the caster's own stars must be spared");
        int fresh = StarDispel.onDispel(level, c, 6, null, 1.0, rnd);
        boolean farLeft = level.getEntity(far.node().entity()) != null && StarGame.NODES.count(owner.getUUID()) == 1;
        if (fresh != 2 || !farLeft) bad.add("a certain dispel of 6 blocks should break the two near stars and spare the far one: broke " + fresh + ", left " + StarGame.NODES.count(owner.getUUID()));
        if (level.getEntity(near.node().entity()) != null) bad.add("a broken star left its body");
        // the survivor of a roll is left alone for a while (chance 0.0001 cannot be told from a miss here: use the grace table directly)
        StarGame.GRACE.survived(far.node().entity(), level.getGameTime());
        int graced = StarDispel.onDispel(level, c, 20, null, 1.0, rnd);
        if (graced != 0) bad.add("a star that just survived a roll was rolled again");
        StarGame.GRACE.forget(far.node().entity());
        // a pane in the sphere breaks too, one packet at most
        StarMath.Pane geom = StarMath.pane(List.of(new StarMath.P(c.x - 2, y, c.z), new StarMath.P(c.x + 2, y, c.z), new StarMath.P(c.x + 2, y + 3, c.z), new StarMath.P(c.x - 2, y + 3, c.z)), new StarMath.P(c.x, y, c.z + 8), 6, 5);
        StarGame.PANES.add(UUID.randomUUID(), owner.getUUID(), StarGame.dimOf(level), geom, 10f, level.getGameTime() + 100);
        int pane = StarDispel.onDispel(level, c, 20, null, 1.0, rnd);
        if (StarGame.PANES.size() != 0 || pane < 2) bad.add("a certain dispel should break the pane and the last star: broke " + pane);
        // the real entry: the mod's dispel calls the hook (nothing is left to break, so this is a no-op that must not throw)
        int viaMod = com.newuniverse.nusmp.antimagic.Nullification.shatterBarriers(level, c, 4, null);
        if (viaMod < 0) bad.add("shatterBarriers");
        StarGame.forget(owner);
        if (StarGame.NODES.total() != 0 || StarGame.GRACE.size() > StarDispelRules.GRACE_CAP) bad.add("dispel leaks");
        return "dispel hook: chance 0 / own / outside / grace / pane (default chance " + StarConfig.dispelChance() + ")";
    }

    // ---------------------------------------------------------------------------------------------- the book
    private static void book(List<String> bad) {
        if (!(NUSkills.grimoireSkillFor(MagicType.STAR) instanceof GrimoireBook star)) { bad.add("no Star book"); return; }
        List<BookPage> pages = star.pages();
        int fam = star.familyCount();
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < Math.min(fam, pages.size()); i++) ids.add(pages.get(i).id());
        int first = ids.indexOf(StarRules.SEED);
        if (first < 0) { bad.add("star_seed is not in the Star book (is magic.pagesFifteen off?)"); return; }
        if (first != fam - StarRules.NEW_PAGES.size() || !ids.subList(first, fam).equals(StarRules.NEW_PAGES)) bad.add("the new pages are not the last family pages in order: " + ids.subList(Math.max(0, first - 1), fam));
        for (String id : List.of("star_shot", "starfall", "constellation", "comet", "star_shield", "supernova", "quartile_flagellum", "twinkle_dart", "star_conjunction", "never_never_land", "star_beam", "star_chain",
                "orbit_barrage", "stardust_field", "meteor_swarm"))
            if (ids.indexOf(id) < 0 || ids.indexOf(id) > first) bad.add("old page " + id + " moved or vanished");
        if (VfxShape.STAR_MAGIC.ordinal() >= VfxShape.STAR_NODE.ordinal()) bad.add("the new shapes are not appended");
    }
}
