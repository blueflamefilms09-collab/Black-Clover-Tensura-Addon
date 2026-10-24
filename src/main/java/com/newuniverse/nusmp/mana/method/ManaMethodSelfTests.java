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
 *   <li>3.5.0, each under the same prefix: {@code .charge} (the release's charge is taken once and goes stale; a tap is weaker and quicker than a full hold),
 *       {@code .physical} (True Magic hurts with a vanilla physical type credited to the caster, a normal rune with the Tensura type), {@code .cage} (the plant's cage: free cells only,
 *       caps, gone when the caster leaves and by time), {@code .lightning} (at most the per-second allowance, never into an unloaded chunk), {@code .air_step} (a bounded barrier
 *       platform that follows the feet, sneak drops, nothing is left when it ends), {@code .dash} (the push, the cooldown, the marked motion, the small fall grace).</li>
 * </ul>
 */
public final class ManaMethodSelfTests {
    private ManaMethodSelfTests() {}

    public static void register() {
        SelfTests.register("magic.mana_method", ManaMethodSelfTests::core);
        SelfTests.register("magic.mana_method.true_blocks", ManaMethodSelfTests::trueBlocks);
        SelfTests.register("magic.mana_method.charge", ManaMethodSelfTests::charge);
        SelfTests.register("magic.mana_method.physical", ManaMethodSelfTests::physical);
        SelfTests.register("magic.mana_method.cage", ManaMethodSelfTests::cage);
        SelfTests.register("magic.mana_method.lightning", ManaMethodSelfTests::lightning);
        SelfTests.register("magic.mana_method.air_step", ManaMethodSelfTests::airStep);
        SelfTests.register("magic.mana_method.dash", ManaMethodSelfTests::dash);
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

        // ---- the written rune: the layout the clients draw is bounded for every spell at every moment, and the textures it needs ship in the jar
        int worst = 0;
        RuneGlyphLayout.Layout layout = new RuneGlyphLayout.Layout();
        for (RuneElement e : RuneElement.values())
            for (int mods = 0; mods <= RuneMods.ALL; mods++)
                for (boolean tm : new boolean[]{false, true}) {
                    RuneSpec s2 = new RuneSpec(e, mods, tm);
                    var params = RuneGeneratorUtil.paramsFor(s2);
                    var frame = RuneGeneratorUtil.frameFacing(1, 70, 2, 0.3, -0.2, 1.0);
                    for (int level = RuneGlyphLayout.LEVEL_FULL; level <= RuneGlyphLayout.LEVEL_MIN; level++)
                        for (int age = 0; age <= RuneRules.chargeTicks(s2) + 12; age += 3) {
                            RuneGlyphLayout.fill(layout, s2, params, frame, age, RuneRules.chargeTicks(s2), level, 0, 72, 0);
                            worst = Math.max(worst, layout.quads());
                            if (layout.quads() > RuneGlyphLayout.quadsAt(params, level) || layout.quads() > RuneGlyphLayout.MAX_QUADS) return Result.fail("the rune of " + s2 + " draws " + layout.quads() + " quads at level " + level + ", over its bound");
                        }
                }
        if (RuneGlyphLayout.MAX_QUADS * 4 > 1200) return Result.fail("a rune may draw " + RuneGlyphLayout.MAX_QUADS * 4 + " vertices, over the 1200 its layer asks for");
        String[] tints = {"gold", "flame", "water", "wind", "earth", "lightning", "plant"};
        for (String tint : tints) if (ManaMethodSelfTests.class.getResourceAsStream("/assets/nusmp/textures/particle/rune_stroke_" + tint + ".png") == null) return Result.fail("the stroke texture of " + tint + " is not in the jar");
        for (String tex : new String[]{"rune_line_ring", "rune_arc_strip"}) if (ManaMethodSelfTests.class.getResourceAsStream("/assets/nusmp/textures/particle/" + tex + ".png") == null) return Result.fail("the texture " + tex + " is not in the jar");
        try { com.newuniverse.nusmp.vfx.VfxShape.valueOf("RUNE_WRITING"); } catch (IllegalArgumentException missing) { return Result.fail("the shape RUNE_WRITING is missing"); }
        return Result.ok(books + " grimoires carry the 5 pages last; closed for a fresh player; casts capped at " + per + " per caster, despawn after " + ticks + " ticks of flight and charge; Homing struck a target off its line (" + before + " -> " + after + "); NBT round trip for "
                + (RuneElement.values().length * (RuneMods.ALL + 1) * 2) + " specs; the written rune fills at most " + worst + " of " + RuneGlyphLayout.MAX_QUADS + " quads, its 9 textures ship");
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

    // ================================================================ 3.5.0
    /** A 200 hp-ish dummy a few blocks in front of the stand-in at (8.5, 120, 8.5) looking along +z, with the hurt time cleared. */
    private static Zombie dummy(ServerLevel sl, double dz) {
        Zombie z = EntityType.ZOMBIE.create(sl);
        if (z == null) return null;
        z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000.0);
        z.setHealth(1000f);
        z.setNoAi(true);
        z.setNoGravity(true);
        z.setPos(8.5, 120.0, 8.5 + dz);
        sl.addFreshEntity(z);
        return z;
    }

    private static float shoot(MinecraftServer server, ServerLevel sl, FakePlayer p, GrimoireBook book, ManasSkillInstance inst, Zombie z, RuneSpec spec, RuneCharge.Charge charge, long t) {
        z.setHealth(1000f);
        z.invulnerableTime = 0;
        z.hurtTime = 0;
        RuneManager.cancelOwner(p.getUUID());
        if (RuneManager.begin(book, inst, p, 0, spec, t, charge) != RuneManager.Begin.OK) return -1f;
        int ticks = 0;
        while (RuneManager.activeCount(p.getUUID()) > 0 && ticks < 400) RuneManager.tickAll(server, t + ++ticks);
        RuneManager.cancelOwner(p.getUUID());
        return 1000f - z.getHealth();
    }

    private static Result charge(MinecraftServer server) {
        ServerLevel sl = server.overworld();
        sl.getChunk(0, 0);
        FakePlayer p = SelfTests.standIn(sl, "mana_method_charge", 8.5, 120, 8.5);
        p.setYRot(0f);
        p.setXRot(0f);
        GrimoireBook book = NUSkills.BOOK_REINFORCEMENT.get();
        ManasSkillInstance inst = book.createDefaultInstance();
        int min = ManaMethodConfig.minHold(), max = ManaMethodConfig.maxHold();
        // the server's side of the hold: a release waits for its cast, is taken once, goes stale, and a dropped player loses it
        long now = server.getTickCount() + 2000;
        RuneHold.cancelOwner(p.getUUID());
        RuneHold.releaseForTest(p.getUUID(), 3, max, now);
        if (RuneHold.take(p.getUUID(), 4, now + 1) != null) return Result.fail("a release was taken by another page");
        RuneHold.releaseForTest(p.getUUID(), 3, max, now);
        RuneCharge.Charge full = RuneHold.take(p.getUUID(), 3, now + 5);
        if (full == null || full.stage() != RuneCharge.Stage.FULL) return Result.fail("a full hold did not give the full charge: " + full);
        if (RuneHold.take(p.getUUID(), 3, now + 5) != null) return Result.fail("a release was taken twice");
        RuneHold.releaseForTest(p.getUUID(), 3, 1, now);
        if (RuneHold.take(p.getUUID(), 3, now + RuneHold.RELEASED_TTL + 5) != null) return Result.fail("a stale release was used");
        RuneHold.releaseForTest(p.getUUID(), 3, 1, now);
        RuneHold.cancelOwner(p.getUUID());
        if (RuneHold.take(p.getUUID(), 3, now + 1) != null) return Result.fail("a dropped player kept a release");
        RuneCharge.Charge tap = RuneCharge.of(1, min, max), mid = RuneCharge.of((min + max) / 2, min, max);
        if (tap.stage() != RuneCharge.Stage.BASIC || mid.stage() != RuneCharge.Stage.CHARGED) return Result.fail("the stages of a tap and a half hold are wrong");
        // the cast itself: a tap is smaller and weaker than a half hold, which is weaker than a full one; the charge decides how long the rune takes
        Zombie z = dummy(sl, 8.0);
        if (z == null) return Result.fail("could not create a test zombie");
        try {
            long t = server.getTickCount() + 7000;
            RuneSpec spec = new RuneSpec(RuneElement.WIND, RuneMods.NONE, false);
            RuneManager.cancelOwner(p.getUUID());
            if (RuneManager.begin(book, inst, p, 0, spec, t, tap) != RuneManager.Begin.OK) return Result.fail("the tap was refused");
            var cast = RuneManager.castOf(p.getUUID());
            if (cast == null || cast.charge != RuneCharge.FINISH_BASIC) return Result.fail("a tap should finish in " + RuneCharge.FINISH_BASIC + " ticks, got " + (cast == null ? -1 : cast.charge));
            RuneManager.cancelOwner(p.getUUID());
            if (RuneManager.begin(book, inst, p, 0, spec, t, full) != RuneManager.Begin.OK) return Result.fail("the full hold was refused");
            cast = RuneManager.castOf(p.getUUID());
            if (cast == null || cast.charge != RuneCharge.FINISH_FULL) return Result.fail("a full hold should finish in " + RuneCharge.FINISH_FULL + " ticks, got " + (cast == null ? -1 : cast.charge));
            RuneManager.cancelOwner(p.getUUID());
            float dTap = shoot(server, sl, p, book, inst, z, spec, tap, t), dMid = shoot(server, sl, p, book, inst, z, spec, mid, t + 3000), dFull = shoot(server, sl, p, book, inst, z, spec, full, t + 6000), dOld = shoot(server, sl, p, book, inst, z, spec, RuneCharge.Charge.LEGACY, t + 9000);
            if (dTap <= 0f || dMid <= 0f || dFull <= 0f || dOld <= 0f) return Result.fail("a rune missed the dummy: tap " + dTap + ", half " + dMid + ", full " + dFull + ", old " + dOld);
            if (!(dTap <= dMid && dMid <= dFull && dTap < dFull)) return Result.fail("damage does not rise with the hold: tap " + dTap + ", half " + dMid + ", full " + dFull);
            if (!(dTap <= dOld && dOld <= dFull)) return Result.fail("the old spell should sit between the tap and the full hold: " + dTap + " / " + dOld + " / " + dFull);
            // a tap never writes the second rune, even sneaking
            p.setShiftKeyDown(true);
            int mods = RuneCharge.modsFor(tap, RuneElement.FIRE, p.isShiftKeyDown());
            p.setShiftKeyDown(false);
            if (mods != RuneElement.FIRE.defaultMods()) return Result.fail("a tapped rune wrote the second rune");
            return Result.ok("tap/half/full/old damage " + dTap + " / " + dMid + " / " + dFull + " / " + dOld + "; finish " + RuneCharge.FINISH_BASIC + " to " + RuneCharge.FINISH_FULL + " ticks; the release is taken once, goes stale, and is dropped with the player");
        } finally {
            z.discard();
            RuneManager.cancelOwner(p.getUUID());
            RuneHold.cancelOwner(p.getUUID());
        }
    }

    private static Result physical(MinecraftServer server) {
        ServerLevel sl = server.overworld();
        sl.getChunk(0, 0);
        FakePlayer p = SelfTests.standIn(sl, "mana_method_physical", 8.5, 120, 8.5);
        p.setYRot(0f);
        p.setXRot(0f);
        GrimoireBook book = NUSkills.BOOK_REINFORCEMENT.get();
        ManasSkillInstance inst = book.createDefaultInstance();
        Zombie z = dummy(sl, 8.0);
        if (z == null) return Result.fail("could not create a test zombie");
        try {
            long t = server.getTickCount() + 12000;
            float normal = shoot(server, sl, p, book, inst, z, new RuneSpec(RuneElement.WIND, RuneMods.NONE, false), RuneCharge.Charge.LEGACY, t);
            var src = z.getLastDamageSource();
            if (normal <= 0f || src == null) return Result.fail("the normal rune did not hurt the dummy");
            if (!src.typeHolder().unwrapKey().map(k -> k.location().getNamespace().equals("tensura")).orElse(false)) return Result.fail("a normal rune is not the grimoire's Tensura damage: " + src.getMsgId());
            float tm = shoot(server, sl, p, book, inst, z, new RuneSpec(RuneElement.WIND, RuneMods.NONE, true), RuneCharge.Charge.LEGACY, t + 3000);
            var psrc = z.getLastDamageSource();
            if (tm <= 0f || psrc == null) return Result.fail("True Magic did not hurt the dummy");
            boolean physicalWanted = ManaMethodConfig.physicalDamage();
            if (physicalWanted) {
                if (!psrc.is(net.minecraft.world.damagesource.DamageTypes.FLY_INTO_WALL)) return Result.fail("True Wind should hurt as a blow against a wall, got " + psrc.getMsgId());
                if (psrc.getEntity() != p) return Result.fail("True Magic damage is not credited to the caster");
                if (com.newuniverse.nusmp.antimagic.AntiMagic.isMagic(psrc)) return Result.fail("the physical route is still read as magic damage");
            }
            return Result.ok("normal rune " + normal + " as " + src.getMsgId() + "; True Wind " + tm + " as " + psrc.getMsgId() + " credited to the caster (physicalDamage " + physicalWanted + ")");
        } finally {
            z.discard();
            RuneManager.cancelOwner(p.getUUID());
        }
    }

    private static Result cage(MinecraftServer server) {
        ServerLevel sl = server.overworld();
        sl.getChunk(0, 0);
        UUID owner = UUID.nameUUIDFromBytes("nusmp-selftest:mana_method_cage".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        long now = server.getTickCount();
        TrueBlocks.expireAllForTest(server);
        BlockPos feet = new BlockPos(8, 120, 8);
        List<BlockPos> cells = new ArrayList<>();
        for (int dx = -2; dx <= 2; dx++) for (int dy = -1; dy <= 4; dy++) for (int dz = -2; dz <= 2; dz++) cells.add(feet.offset(dx, dy, dz));
        try {
            for (BlockPos q : cells) sl.setBlock(q, Blocks.AIR.defaultBlockState(), 3);
            if (!ManaMethodConfig.plantCage() || !ManaMethodConfig.trueBlocks()) return Result.ok("skipped: the cage or the True blocks are switched off in this config");
            if (TrueBlocks.placeCage(sl, owner, feet, (l, q) -> false, now) != 0) return Result.fail("a cage was built where the caster may not build");
            int n = TrueBlocks.placeCage(sl, owner, feet, (l, q) -> true, now);
            if (n < 1 || n > TrueWorldRules.CAGE_PER_CAST || n > TrueWorldRules.CAGE_CELLS) return Result.fail("the cage placed " + n + " blocks");
            int logs = 0, leaves = 0;
            for (BlockPos q : TrueBlocks.positionsOf(owner)) {
                var s = sl.getBlockState(q);
                if (s.is(Blocks.OAK_LOG)) logs++;
                else if (s.is(Blocks.OAK_LEAVES)) leaves++;
                else return Result.fail("a cage block at " + q + " is neither a log nor a leaf: " + s);
                if (q.getX() == feet.getX() && q.getZ() == feet.getZ() && q.getY() < feet.getY() + 2) return Result.fail("a cage block stands in the target's own cell at " + q);
            }
            if (logs == 0 || leaves == 0) return Result.fail("the cage should have posts and sides: " + logs + " logs, " + leaves + " leaves");
            for (int k = 0; k < 10; k++) TrueBlocks.placeCage(sl, owner, feet.offset(k * 3, 0, 0), (l, q) -> true, now);
            if (TrueBlocks.liveOf(owner) > TrueWorldRules.CAGE_PER_CASTER) return Result.fail("one caster has " + TrueBlocks.liveOf(owner) + " cage blocks, over " + TrueWorldRules.CAGE_PER_CASTER);
            if (TrueBlocks.count() > TrueWorldRules.CAGE_GLOBAL) return Result.fail("the world has " + TrueBlocks.count() + " cage blocks, over " + TrueWorldRules.CAGE_GLOBAL);
            List<BlockPos> standing = new ArrayList<>(TrueBlocks.positionsOf(owner));
            // the caster leaves (logout, death, dimension change): every block of theirs goes at once
            TrueBlocks.cancelOwner(server, owner);
            if (TrueBlocks.liveOf(owner) != 0) return Result.fail("the ledger still lists " + TrueBlocks.liveOf(owner) + " blocks after the caster left");
            for (BlockPos q : standing) if (sl.getBlockState(q).is(Blocks.OAK_LOG) || sl.getBlockState(q).is(Blocks.OAK_LEAVES)) return Result.fail("a cage block still stands at " + q + " after the caster left");
            // and by time
            int again = TrueBlocks.placeCage(sl, owner, feet, (l, q) -> true, now);
            TrueBlocks.tick(server, now - now % 10 + 10 + TrueWorldRules.CAGE_MAX_SECONDS * 20L + 20);
            if (TrueBlocks.liveOf(owner) != 0) return Result.fail("the cage did not expire: " + TrueBlocks.liveOf(owner) + " blocks left");
            return Result.ok("a cage of " + n + " blocks (" + logs + " logs, " + leaves + " leaves) round the target, none where the caster may not build, caps " + TrueWorldRules.CAGE_PER_CASTER + "/" + TrueWorldRules.CAGE_GLOBAL + " held, gone when the caster left, gone by time (" + again + ")");
        } finally {
            TrueBlocks.cancelOwner(server, owner);
            TrueBlocks.expireAllForTest(server);
            for (BlockPos q : cells) { var s = sl.getBlockState(q); if (s.is(Blocks.OAK_LOG) || s.is(Blocks.OAK_LEAVES)) sl.setBlock(q, Blocks.AIR.defaultBlockState(), 3); }
            for (int k = 1; k < 10; k++)
                for (int dx = -2; dx <= 2; dx++) for (int dy = -1; dy <= 4; dy++) for (int dz = -2; dz <= 2; dz++) {
                    BlockPos q = feet.offset(k * 3 + dx, dy, dz);
                    var s = sl.getBlockState(q);
                    if (s.is(Blocks.OAK_LOG) || s.is(Blocks.OAK_LEAVES)) sl.setBlock(q, Blocks.AIR.defaultBlockState(), 3);
                }
        }
    }

    private static Result lightning(MinecraftServer server) {
        ServerLevel sl = server.overworld();
        sl.getChunk(0, 0);
        FakePlayer p = SelfTests.standIn(sl, "mana_method_lightning", 8.5, 120, 8.5);
        long now = server.getTickCount();
        Vec3 at = new Vec3(8.5, 140.0, 40.5);
        sl.getChunk(2, 2);                                                          // a test may load a chunk; the strike never does
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(at, at).inflate(3.0);
        TrueLightning.resetForTest();
        List<net.minecraft.world.entity.LightningBolt> made = new ArrayList<>();
        try {
            if (TrueLightning.strike(sl, p, at, (l, q) -> false, now)) return Result.fail("a bolt struck where the caster may not build");
            if (!ManaMethodConfig.lightningBolt()) return Result.ok("skipped: the lightning is switched off in this config");
            int struck = 0;
            for (int k = 0; k < 8; k++) if (TrueLightning.strike(sl, p, at, (l, q) -> true, now)) struck++;
            made.addAll(sl.getEntitiesOfClass(net.minecraft.world.entity.LightningBolt.class, box));
            if (struck != TrueWorldRules.LIGHTNING_PER_SECOND) return Result.fail(struck + " bolts struck in one second, expected " + TrueWorldRules.LIGHTNING_PER_SECOND);
            if (made.size() != struck) return Result.fail(made.size() + " bolt entities exist, " + struck + " strikes were counted");
            // an unloaded chunk gets nothing, and nothing is loaded for it
            TrueLightning.resetForTest();
            if (TrueLightning.strike(sl, p, new Vec3(20000.5, 100.0, 20000.5), (l, q) -> true, now)) return Result.fail("a bolt struck into an unloaded chunk");
            if (sl.isLoaded(BlockPos.containing(20000.5, 100.0, 20000.5))) return Result.fail("the strike loaded a chunk");
            return Result.ok(struck + " bolts in a second (cap " + TrueWorldRules.LIGHTNING_PER_SECOND + "), one entity each, none where the caster may not build, none into an unloaded chunk (visual-only unless lightningRealBolt)");
        } finally {
            for (var b : made) b.discard();
            for (var b : sl.getEntitiesOfClass(net.minecraft.world.entity.LightningBolt.class, box)) b.discard();
            TrueLightning.resetForTest();
        }
    }

    private static Result airStep(MinecraftServer server) {
        ServerLevel sl = server.overworld();
        sl.getChunk(0, 0);
        FakePlayer p = SelfTests.standIn(sl, "mana_method_air_step", 8.5, 120, 8.5);
        UUID id = p.getUUID();
        long now = server.getTickCount() + 20000;
        AirStep.cancelOwner(id);
        BlockPos under = BlockPos.containing(8.5, 119, 8.5);
        try {
            for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) sl.setBlock(under.offset(dx, 0, dz), Blocks.AIR.defaultBlockState(), 3);
            if (!ManaMethodConfig.mobility()) return Result.ok("skipped: the mobility runes are switched off in this config");
            if (!sl.mayInteract(p, under)) return Result.ok("skipped: the spot is under spawn protection for the stand-in");
            p.setOnGround(false);
            p.setShiftKeyDown(false);
            if (!AirStep.start(p, ManaMethodConfig.airStepSeconds(), now)) return Result.fail("the Air Step Rune was refused");
            AirStep.tickAll(now + 1);
            int cells = AirStep.cellsOf(id);
            if (cells < 1 || cells > MobilityRules.MAX_CELLS_PER_CASTER) return Result.fail("the platform has " + cells + " blocks, expected 1 to " + MobilityRules.MAX_CELLS_PER_CASTER);
            for (BlockPos q : AirStep.positionsOf(id)) if (!sl.getBlockState(q).is(Blocks.BARRIER) || q.getY() != under.getY()) return Result.fail("a platform block at " + q + " is not a barrier under the feet");
            // the platform follows the feet and never grows past the cap
            for (int k = 0; k < 12; k++) { p.setPos(8.5 + k * 0.9, 120, 8.5); AirStep.tickAll(now + 2 + k); }
            if (AirStep.cellsOf(id) > MobilityRules.MAX_CELLS_PER_CASTER || AirStep.globalCells() > MobilityRules.MAX_CELLS_GLOBAL) return Result.fail("the platform grew past its caps: " + AirStep.cellsOf(id) + " / " + AirStep.globalCells());
            p.setPos(8.5, 120, 8.5);
            AirStep.tickAll(now + 20);
            // sneaking drops the player through
            p.setShiftKeyDown(true);
            AirStep.tickAll(now + 21);
            p.setShiftKeyDown(false);
            for (BlockPos q : footprintCells(8.5, 120, 8.5)) if (sl.getBlockState(q).is(Blocks.BARRIER)) return Result.fail("the barrier under the sneaking player at " + q + " was not removed");
            // it ends with the player (logout, death, dimension change)
            AirStep.tickAll(now + 22);
            AirStep.cancelOwner(id);
            if (AirStep.sessions() != 0 && AirStep.cellsOf(id) != 0) return Result.fail("the session survived the end");
            for (int dx = -14; dx <= 14; dx++) for (int dz = -2; dz <= 2; dz++) if (sl.getBlockState(under.offset(dx, 0, dz)).is(Blocks.BARRIER)) return Result.fail("a barrier is left at " + under.offset(dx, 0, dz));
            return Result.ok("a platform of at most " + MobilityRules.MAX_CELLS_PER_CASTER + " barriers follows the feet, sneak drops, every barrier is gone when the session ends; world cap " + MobilityRules.MAX_CELLS_GLOBAL);
        } finally {
            AirStep.cancelOwner(id);
            for (int dx = -14; dx <= 14; dx++) for (int dz = -2; dz <= 2; dz++) if (sl.getBlockState(under.offset(dx, 0, dz)).is(Blocks.BARRIER)) sl.setBlock(under.offset(dx, 0, dz), Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private static List<BlockPos> footprintCells(double x, double y, double z) {
        int[] out = new int[12];
        int n = MobilityRules.footprint(x, y, z, out);
        List<BlockPos> cells = new ArrayList<>(n);
        for (int i = 0; i < n; i++) cells.add(new BlockPos(out[i * 3], out[i * 3 + 1], out[i * 3 + 2]));
        return cells;
    }

    private static Result dash(MinecraftServer server) {
        ServerLevel sl = server.overworld();
        sl.getChunk(0, 0);
        FakePlayer p = SelfTests.standIn(sl, "mana_method_dash", 8.5, 120, 8.5);
        p.setYRot(0f);
        p.setXRot(-20f);
        long now = server.getTickCount() + 30000;
        Dash.cancelOwner(p.getUUID());
        try {
            if (!ManaMethodConfig.mobility()) return Result.ok("skipped: the mobility runes are switched off in this config");
            p.setDeltaMovement(0, 0, 0);
            p.fallDistance = 10f;
            RuneCharge.Charge tap = RuneCharge.of(0, ManaMethodConfig.minHold(), ManaMethodConfig.maxHold()), full = RuneCharge.of(ManaMethodConfig.maxHold(), ManaMethodConfig.minHold(), ManaMethodConfig.maxHold());
            if (!Dash.cast(p, tap, now)) return Result.fail("the dash was refused");
            double tapSpeed = p.getDeltaMovement().length();
            if (!p.hurtMarked) return Result.fail("the dash did not mark the player's motion for the client");
            if (tapSpeed <= 0.05 || tapSpeed > MobilityRules.MAX_SPEED + 1e-6) return Result.fail("the dash speed is " + tapSpeed);
            if (p.getDeltaMovement().z <= 0) return Result.fail("the dash does not go the way the player looks");
            if (p.fallDistance < 10f - (float) MobilityRules.FALL_GRACE - 1e-3f || p.fallDistance > 10f) return Result.fail("the fall after a dash is " + p.fallDistance + ", expected the 10 blocks minus the grace");
            if (Dash.cast(p, tap, now + 1)) return Result.fail("a second dash in the same moment was not refused");
            if (!Dash.cast(p, full, now + MobilityRules.DASH_COOLDOWN)) return Result.fail("the dash after the cooldown was refused");
            double fullSpeed = p.getDeltaMovement().length();
            if (!(fullSpeed > tapSpeed)) return Result.fail("a full hold should dash faster than a tap: " + fullSpeed + " vs " + tapSpeed);
            if (fullSpeed > MobilityRules.MAX_SPEED + 1e-6) return Result.fail("a dash is faster than the maximum: " + fullSpeed);
            Dash.cancelOwner(p.getUUID());
            if (!Dash.cast(p, tap, now)) return Result.fail("a dropped player kept a cooldown");
            return Result.ok("tap " + String.format("%.2f", tapSpeed) + " and full " + String.format("%.2f", fullSpeed) + " blocks/tick (max " + MobilityRules.MAX_SPEED + "), motion marked for the client, cooldown " + MobilityRules.DASH_COOLDOWN + " ticks, fall forgiven only " + MobilityRules.FALL_GRACE + " blocks");
        } finally {
            Dash.cancelOwner(p.getUUID());
            p.setDeltaMovement(0, 0, 0);
            p.fallDistance = 0f;
        }
    }
}
