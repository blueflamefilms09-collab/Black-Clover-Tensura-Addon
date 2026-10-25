package com.newuniverse.nusmp.star;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.charset.CharacterSets;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.perf.PerfBudgets;
import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: "Star Magic for everybody" (docs/star_vfx_for_all.md) must hold on a dedicated server, so the CI smoke test casts the pages that send the new Star effects as a Star user who is NOT a Grinberryall heir
 * (a stand-in with no Spade cover, no character, no Raid: the basic-grimoire case) and counts the effect packets the casts produce (key {@code magic.star_nodes_all}, covered by the existing smoke prefix
 * {@code magic.star_nodes}). With {@code star.vfxForAll} on (default): the shared pages open (no page block), cast and send their packets; Black Neverland still refuses and sends nothing. With it off: every heir page
 * refuses and sends nothing (the 3.7.0 behaviour). Cost: a handful of casts of one stand-in, at most about 40 packets and 16 small entities, all removed again; nothing touches a client class.
 */
public final class StarAccessSelfTests {
    private StarAccessSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.star_nodes_all", StarAccessSelfTests::check);
    }

    private static Result check(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        pure(bad);
        String game;
        try {
            game = game(server, bad);
        } catch (RuntimeException e) {
            return Result.fail("a Star cast threw " + e);
        }
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(8, bad.size()))));
        return Result.ok("access table (pure); " + game + "; star.vfxForAll = " + StarConfig.vfxForAll());
    }

    // ---------------------------------------------------------------------------------------------- the pure table
    private static void pure(List<String> bad) {
        for (boolean all : new boolean[]{true, false}) {
            List<String> pages = new ArrayList<>(StarRules.NEW_PAGES);
            pages.add(StarRules.FLAGELLUM);
            for (String page : pages) {
                for (String kingdom : new String[]{"CLOVER", "SPADE", "HEART", "DIAMOND", null}) {
                    boolean shared = StarAccess.SHARED.contains(page), neverland = StarAccess.ALWAYS_HEIR.contains(page);
                    boolean nobodyHeir = StarAccess.mayCast(page, true, kingdom, 0.4, 1.0, false, all);
                    boolean fullHeir = StarAccess.mayCast(page, true, kingdom, 1.0, 1.0, true, all);
                    boolean expectNobody = neverland ? false : (shared ? all : true);
                    boolean expectHeir = "SPADE".equals(kingdom) || !(neverland || (shared && !all));
                    if (nobodyHeir != expectNobody) bad.add("access of " + page + " for a non-heir of " + kingdom + " with vfxForAll " + all);
                    if (fullHeir != expectHeir) bad.add("access of " + page + " for a full mastery mage of " + kingdom + " with vfxForAll " + all);
                    if (StarAccess.mayCast(page, false, kingdom, 1.0, 1.0, true, all)) bad.add(page + " allowed with no Star grimoire");
                }
            }
        }
        if (!StarAccess.whip(false, true) || StarAccess.whip(false, false) || !StarAccess.whip(true, false)) bad.add("the whip rule");
    }

    // ---------------------------------------------------------------------------------------------- the casts of a non-heir
    private static int packets() { return (int) PerfBudgets.EFFECT_PACKETS.asked(); }

    private static BookPage page(GrimoireBook book, String id) {
        for (int m = 0; m < book.pages().size(); m++) if (book.page(m).id().equals(id)) return book.page(m);
        return null;
    }

    private static int mode(GrimoireBook book, String id) {
        for (int m = 0; m < book.pages().size(); m++) if (book.page(m).id().equals(id)) return m;
        return -1;
    }

    /** Casts one page of the Star book directly (the cooldown and cost belong to the book, the gate and the packets to the cast) and returns whether it went off. */
    private static boolean cast(GrimoireBook book, ManasSkillInstance inst, ServerPlayer p, String id, int[] sent) {
        BookPage pg = page(book, id);
        if (pg == null || pg.cast() == null) return false;
        int before = packets();
        boolean ok = pg.cast().cast(book, inst, p, mode(book, id));
        sent[0] = packets() - before;
        return ok;
    }

    private static String game(MinecraftServer server, List<String> bad) {
        if (!StarConfig.nodes()) return "star.nodes is off on this server: casts not exercised";
        if (!(NUSkills.grimoireSkillFor(MagicType.STAR) instanceof GrimoireBook book)) { bad.add("no Star book"); return "no book"; }
        ServerLevel level = server.overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        double y = Math.min(level.getMaxBuildHeight() - 20, spawn.getY() + 24);
        ServerPlayer p = SelfTests.standIn(level, "star-all", spawn.getX() + 0.5, y, spawn.getZ() + 0.5);
        StarGame.forget(p);
        ManasSkillInstance inst = book.createDefaultInstance();
        boolean all = StarConfig.vfxForAll();
        if (StarGame.isHeir(p)) bad.add("the stand-in is an heir (it must be a plain Star user)");
        // 1. the pages are listed in the basic grimoire and wait only for what the setting says
        for (String id : StarRules.NEW_PAGES) {
            int m = mode(book, id);
            if (m < 0) { bad.add(id + " is not a page of the Star book"); continue; }
            if (CharacterSets.hidden(inst, m)) bad.add(id + " is hidden in the basic grimoire");
            boolean blocked = StarPages.block(book, book.page(m), p) != null;
            boolean expect = StarRules.HEIR_PAGES.contains(id) && StarAccess.needsHeir(id, all);      // what StarPages.block asks of a non-heir
            if (blocked != expect) bad.add(id + (blocked ? " is blocked" : " is open") + " for a non-heir with star.vfxForAll " + all);
        }
        int[] sent = {0};
        Vec3 at = new Vec3(spawn.getX() + 0.5, y, spawn.getZ() + 0.5);
        // 2. Star Seed always works and sends its node effect
        if (!cast(book, inst, p, StarRules.SEED, sent) || sent[0] < 1) bad.add("Star Seed as a plain Star user: ok " + false + ", packets " + sent[0]);
        int cap = StarFx.CAST_PACKETS + 3;                       // the cast's own ten plus the chant circle and the impact flash
        // 3. the shared pages
        String[] shared = {StarRules.SCUTUM, StarRules.HASTA, StarRules.FLAGELLUM};
        int[] minPackets = {4, 4, 4};
        for (int k = 0; k < shared.length; k++) {
            StarGame.forget(p);
            boolean ok = cast(book, inst, p, shared[k], sent);
            if (shared[k].equals(StarRules.FLAGELLUM) && !all) continue;                 // star.vfxForAll = false: the Flagellum is the old cone (other shape, other test)
            if (all && (!ok || sent[0] < minPackets[k] || sent[0] > cap)) bad.add(shared[k] + " as a plain Star user: went off " + ok + ", " + sent[0] + " packets (wanted " + minPackets[k] + " to " + cap + ")");
            if (!all && (ok || sent[0] != 0)) bad.add(shared[k] + " went off for a non-heir with star.vfxForAll false: " + ok + ", " + sent[0] + " packets");
        }
        // 4. a Quintile page (five real stars)
        StarGame.forget(p);
        for (int i = 0; i < 5; i++) StarGame.placeAt(p, at.add(i - 2, 0, 3), false);
        boolean qok = cast(book, inst, p, StarRules.QUINTILE_SCUTUM, sent);
        if (all && (!qok || sent[0] < 5 || sent[0] > cap)) bad.add("Quintile: Atrum Scutum as a plain Star user: went off " + qok + ", " + sent[0] + " packets");
        if (!all && (qok || sent[0] != 0)) bad.add("Quintile: Atrum Scutum went off for a non-heir with star.vfxForAll false");
        // 5. Black Neverland is the heir's whatever the setting says
        StarGame.forget(p);
        boolean bn = cast(book, inst, p, StarRules.BLACK_NEVERLAND, sent);
        if (bn || sent[0] != 0) bad.add("Black Neverland went off for a non-heir: " + bn + ", " + sent[0] + " packets");
        StarGame.forget(p);
        if (StarGame.NODES.count(p.getUUID()) != 0 || StarGame.PANES.size() != 0) bad.add("the casts left stars or a pane behind");
        return "a plain Star user (no Spade cover, no character, no Raid): Seed, Scutum, Hasta, Flagellum, Quintile Scutum " + (all ? "cast and sent packets" : "refused") + ", Black Neverland refused with no packet";
    }
}
