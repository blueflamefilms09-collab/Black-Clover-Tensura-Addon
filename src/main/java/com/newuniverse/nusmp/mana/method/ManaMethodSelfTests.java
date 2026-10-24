package com.newuniverse.nusmp.mana.method;

import com.newuniverse.nusmp.avatar.cast.AvatarSkillList;
import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The CI self-tests of the Mana Method (run by the server smoke test with a stand-in player, see labyrinth.mega.SelfTests); the smoke list entry is the prefix
 * {@code magic.mana_method}:
 * <ul>
 *   <li>{@code magic.mana_method}: the five pages are the last pages of every grimoire (after the spirit pages), closed for a fresh player, off the Avatar's list; the rune
 *       construct (caps per caster, auto-despawn, clean-up on a dropped caster, the spell's NBT round trip); a Homing rune turns toward and strikes a target it was not aimed
 *       at; a Growth rune's hit volume and damage grow with the distance.</li>
 *   <li>{@code magic.mana_method.true_blocks}: True Magic places real blocks (water sources, a stone mound, fire) only into free cells the caster may build in, within the
 *       per-cast, per-caster and world caps, and takes every one back.</li>
 * </ul>
 */
public final class ManaMethodSelfTests {
    private ManaMethodSelfTests() {}

    public static void register() {
        SelfTests.register("magic.mana_method", ManaMethodSelfTests::core);
        SelfTests.register("magic.mana_method.true_blocks", ManaMethodSelfTests::trueBlocks);
    }

    private static Result core(MinecraftServer server) {
        ServerLevel sl = server.overworld();
        sl.getChunk(0, 0);                                                          // a test may load a chunk; the game code never does
        // ---- the pages
        List<String> want = ManaMethodPages.ids();
        int books = 0;
        for (var h : NUSkills.BOOKS) {
            GrimoireBook b = h.get();
            List<BookPage> pages = b.pages();
            if (pages.stream().noneMatch(pg -> pg.id().equals(GrimoireBook.STRIKE_ID))) continue;     // the Forbidden book has no universal pages
            int n = pages.size();
            if (n < want.size()) return Result.fail(b.magicId() + " has too few pages");
            Set<String> ids = new HashSet<>();
            for (BookPage pg : pages) if (!ids.add(pg.id())) return Result.fail(b.magicId() + " has the page " + pg.id() + " twice");
            for (int k = 0; k < want.size(); k++)
                if (!pages.get(n - want.size() + k).id().equals(want.get(k))) return Result.fail(b.magicId() + ": page " + (n - want.size() + k) + " should be " + want.get(k));
            books++;
        }
        if (books < 10) return Result.fail("only " + books + " grimoires carry the Mana Method pages");
        for (String id : want) if (AvatarSkillList.allowedPage(id, 10.0)) return Result.fail("the Avatar's cast list allows " + id);
        FakePlayer p = SelfTests.standIn(sl, "mana_method", 8.5, 120, 8.5);
        p.setYRot(0f);
        p.setXRot(0f);
        GrimoireBook book = NUSkills.BOOK_REINFORCEMENT.get();
        var pages = book.pages();
        for (int m = pages.size() - want.size(); m < pages.size(); m++)
            if (book.usable(null, p, m)) return Result.fail("page " + pages.get(m).id() + " is usable for a player who fails the late-game gate");

        // ---- the spell's NBT round trip and the rune's look
        for (RuneElement e : RuneElement.values())
            for (int mods = 0; mods <= RuneMods.ALL; mods++)
                for (boolean t : new boolean[]{false, true}) {
                    RuneSpec s = new RuneSpec(e, mods, t);
                    if (!RuneSpecNbt.read(RuneSpecNbt.write(s)).equals(s)) return Result.fail("the NBT of " + s + " does not read back");
                    if (!RuneSpec.unpack(s.pack()).equals(s)) return Result.fail("the packed form of " + s + " does not read back");
                }

        // ---- the construct: caps, charge, auto-despawn, clean-up
        ManasSkillInstance inst = book.createDefaultInstance();
        RuneManager.cancelOwner(p.getUUID());
        long t = server.getTickCount() + 1000;                                      // game time is the test's own: the manager only compares differences
        int per = ManaMethodConfig.perCaster();
        for (int k = 0; k < per; k++) if (RuneManager.begin(book, inst, p, 0, new RuneSpec(RuneElement.FIRE, RuneMods.GROWTH, false), t) != RuneManager.Begin.OK) return Result.fail("cast " + (k + 1) + " of " + per + " was refused");
        if (RuneManager.begin(book, inst, p, 0, new RuneSpec(RuneElement.FIRE, RuneMods.GROWTH, false), t) != RuneManager.Begin.CASTER_FULL) return Result.fail("a cast over the per-caster cap was not refused");
        if (RuneManager.activeCount(p.getUUID()) != per) return Result.fail("the caster has " + RuneManager.activeCount(p.getUUID()) + " casts, expected " + per);
        RuneManager.cancelOwner(p.getUUID());
        if (RuneManager.activeCount(p.getUUID()) != 0) return Result.fail("a dropped caster's casts were not cleaned up");
        // an empty sky: the rune charges, the bolt flies its whole flight and the construct despawns by itself
        RuneSpec plain = new RuneSpec(RuneElement.WIND, RuneMods.NONE, false);
        p.setXRot(-60f);                                                            // looking up
        if (RuneManager.begin(book, inst, p, 0, plain, t) != RuneManager.Begin.OK) return Result.fail("the cast into the sky was refused");
        int charge = RuneRules.chargeTicks(plain), ticks = 0;
        while (RuneManager.activeCount(p.getUUID()) > 0 && ticks < 400) { RuneManager.tickAll(server, t + ++ticks); }
        if (RuneManager.activeCount(p.getUUID()) != 0) return Result.fail("the construct did not despawn by itself");
        if (ticks < charge + 2 || ticks > charge + RuneRules.MAX_FLIGHT_TICKS + 3) return Result.fail("the construct lived " + ticks + " ticks, expected " + charge + " to " + (charge + RuneRules.MAX_FLIGHT_TICKS + 3));
        p.setXRot(0f);

        // ---- Homing: a target 3 blocks to the side of the line of fire is struck all the same
        Zombie z = EntityType.ZOMBIE.create(sl);
        if (z == null) return Result.fail("could not create a test zombie");
        z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
        z.setHealth(200f);
        z.setNoAi(true);
        z.setNoGravity(true);
        z.setPos(8.5 + 3.0, 120.0, 8.5 + 12.0);
        sl.addFreshEntity(z);
        float before = z.getHealth();
        RuneSpec homing = new RuneSpec(RuneElement.WATER, RuneMods.HOMING, false);
        if (RuneManager.begin(book, inst, p, 0, homing, t) != RuneManager.Begin.OK) { z.discard(); return Result.fail("the Homing cast was refused"); }
        ticks = 0;
        while (RuneManager.activeCount(p.getUUID()) > 0 && ticks < 400) { RuneManager.tickAll(server, t + 5000 + ++ticks); }
        float after = z.getHealth();
        z.discard();
        RuneManager.cancelOwner(p.getUUID());
        if (!(after < before)) return Result.fail("the Homing rune did not strike a target 3 blocks off its line (health " + before + " -> " + after + ")");

        // ---- Growth: the numbers the bolt uses
        RuneSpec grow = new RuneSpec(RuneElement.FIRE, RuneMods.GROWTH, false);
        if (!(RuneRules.hitRadius(grow, 20) > RuneRules.hitRadius(grow, 2)) || RuneRules.hitRadius(grow, 1e6) > RuneRules.BASE_HIT_RADIUS * RuneRules.GROWTH_RADIUS_MAX + 1e-9)
            return Result.fail("the Growth radius does not grow, or is not capped");
        if (!(RuneRules.damageFactor(grow, 20) > 1.0) || RuneRules.damageFactor(grow, 1e6) > RuneRules.GROWTH_DAMAGE_MAX + 1e-9) return Result.fail("the Growth damage does not grow, or is not capped");
        return Result.ok(books + " grimoires carry the 5 pages last; closed for a fresh player; casts capped at " + per + " per caster, despawn after " + ticks + " ticks of flight and charge; Homing struck a target off its line (" + before + " -> " + after + "); NBT round trip for "
                + (RuneElement.values().length * (RuneMods.ALL + 1) * 2) + " specs");
    }

    private static Result trueBlocks(MinecraftServer server) {
        ServerLevel sl = server.overworld();
        sl.getChunk(0, 0);
        UUID owner = UUID.nameUUIDFromBytes("nusmp-selftest:mana_method_blocks".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        long now = server.getTickCount();
        TrueBlocks.expireAllForTest(server);
        // a 7x7 stone floor in the open sky for the kinds that need the ground
        BlockPos floor = new BlockPos(8, 119, 8);
        List<BlockPos> platform = new ArrayList<>();
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) platform.add(floor.offset(dx, 0, dz));
        try {
            for (BlockPos q : platform) sl.setBlock(q, Blocks.COBBLESTONE.defaultBlockState(), 3);
            Vec3 at = Vec3.atCenterOf(floor.above());
            // a spot the caster may not build in gets nothing
            if (TrueBlocks.place(sl, owner, at, RuneElement.EARTH, (l, q) -> false, now) != 0) return Result.fail("blocks were placed where the caster may not build");
            // wind and lightning are not blocks
            if (TrueBlocks.place(sl, owner, at, RuneElement.WIND, (l, q) -> true, now) != 0 || TrueBlocks.place(sl, owner, at, RuneElement.LIGHTNING, (l, q) -> true, now) != 0) return Result.fail("wind or lightning placed blocks");
            // earth: a stone mound on the floor
            int earth = TrueBlocks.place(sl, owner, at, RuneElement.EARTH, (l, q) -> true, now);
            if (earth < 1 || earth > TrueBlockRules.PER_CAST) return Result.fail("True Earth placed " + earth + " blocks");
            for (BlockPos q : TrueBlocks.positionsOf(owner)) if (!sl.getBlockState(q).is(Blocks.STONE) || q.getY() != floor.getY() + 1) return Result.fail("a True Earth block is not stone on the floor: " + q);
            // water: real source blocks
            int water = TrueBlocks.place(sl, owner, at.add(0, 3, 0), RuneElement.WATER, (l, q) -> true, now);
            if (water < 1 || water > TrueBlockRules.PER_CAST) return Result.fail("True Water placed " + water + " blocks");
            boolean source = false;
            for (BlockPos q : TrueBlocks.positionsOf(owner)) if (sl.getBlockState(q).is(Blocks.WATER) && sl.getFluidState(q).isSource()) source = true;
            if (!source) return Result.fail("True Water placed no water source block");
            // fire (only while doFireTick is on)
            int fire = 0;
            if (sl.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK) && ManaMethodConfig.trueFire()) {
                fire = TrueBlocks.place(sl, owner, at, RuneElement.FIRE, (l, q) -> true, now);
                if (fire < 0 || fire > TrueBlockRules.PER_CAST) return Result.fail("True Fire placed " + fire + " blocks");
            }
            // the caps: however often it is cast, one caster never has more than the per-caster limit and the world never more than the global one
            for (int k = 0; k < 12; k++) {
                TrueBlocks.place(sl, owner, at, RuneElement.EARTH, (l, q) -> true, now);
                TrueBlocks.place(sl, owner, at.add(0, 3, 0), RuneElement.WATER, (l, q) -> true, now);
            }
            if (TrueBlocks.liveOf(owner) > TrueBlockRules.PER_CASTER) return Result.fail("one caster has " + TrueBlocks.liveOf(owner) + " blocks standing, over " + TrueBlockRules.PER_CASTER);
            if (TrueBlocks.count() > TrueBlockRules.GLOBAL) return Result.fail("the world has " + TrueBlocks.count() + " blocks standing, over " + TrueBlockRules.GLOBAL);
            List<BlockPos> standing = TrueBlocks.positionsOf(owner);
            // not yet expired: the periodic pass leaves them; once expired it takes every one back
            TrueBlocks.tick(server, now - now % 10 + 10);
            for (BlockPos q : standing) if (sl.getBlockState(q).isAir()) return Result.fail("a block was taken back before its time at " + q);
            TrueBlocks.tick(server, now - now % 10 + 10 + TrueBlockRules.MAX_SECONDS * 20L + 20);
            for (BlockPos q : standing) {
                var s = sl.getBlockState(q);
                if (s.is(Blocks.STONE) || s.is(Blocks.FIRE) || (s.is(Blocks.WATER) && sl.getFluidState(q).isSource())) return Result.fail("a True Magic block is still standing at " + q + " after it expired");
            }
            if (TrueBlocks.liveOf(owner) != 0) return Result.fail("the ledger still lists " + TrueBlocks.liveOf(owner) + " blocks after the expiry");
            return Result.ok("earth " + earth + ", water " + water + ", fire " + fire + " blocks; none where the caster may not build; wind and lightning place none; caps " + TrueBlockRules.PER_CASTER + "/" + TrueBlockRules.GLOBAL
                    + " held over 24 more casts; all taken back after the expiry");
        } finally {
            TrueBlocks.expireAllForTest(server);
            for (BlockPos q : platform) sl.setBlock(q, Blocks.AIR.defaultBlockState(), 3);
            for (int dy = 1; dy <= 6; dy++)
                for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
                    BlockPos q = floor.offset(dx, dy, dz);
                    if (!sl.getBlockState(q).isAir() && !sl.getFluidState(q).isEmpty()) sl.setBlock(q, Blocks.AIR.defaultBlockState(), 3);     // water that flowed from a removed source
                }
        }
    }
}
