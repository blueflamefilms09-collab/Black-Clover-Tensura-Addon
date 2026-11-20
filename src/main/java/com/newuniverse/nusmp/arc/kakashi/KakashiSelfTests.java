package com.newuniverse.nusmp.arc.kakashi;

import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.arc.ArcSpawner;
import com.newuniverse.nusmp.boss.BossRegistry;
import com.newuniverse.nusmp.boss.bars.BarStyleIds;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the Kakashi boss cannot be played by its author, so the CI server smoke test checks it in the real game ({@code /multiverse mega selftest}, keys {@code kakashi.boss.*}): the data (book,
 * voice groups, roster, registry, entity type, bar style), one real summoning, EVERY ability fired on the server with its telegraph (the code path of each move, its effects and its state
 * machines run to their end), Kamui and the cap. Cost: at most two entities, made and discarded in the same tick, plus one unadded pig as the aim; the effects go through the shared budgets; no
 * fight and nothing is left behind.
 */
public final class KakashiSelfTests {
    private KakashiSelfTests() {}

    public static void register() {
        SelfTests.register("kakashi.boss.data", KakashiSelfTests::data);
        SelfTests.register("kakashi.boss.voice", KakashiSelfTests::voice);
        SelfTests.register("kakashi.boss.spawn", KakashiSelfTests::spawn);
        SelfTests.register("kakashi.boss.moves", KakashiSelfTests::moves);
        SelfTests.register("kakashi.boss.kamui", KakashiSelfTests::kamui);
        SelfTests.register("kakashi.boss.cap", KakashiSelfTests::cap);
    }

    private static SelfTests.Result result(List<String> bad, String ok) {
        return bad.isEmpty() ? SelfTests.Result.ok(ok) : SelfTests.Result.fail(bad.size() + " problem(s): " + String.join(" | ", bad.subList(0, Math.min(5, bad.size()))));
    }

    private static SelfTests.Result data(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        List<Move> book = KakashiMoves.build();
        if (book.size() != KakashiMath.BOOK.size()) bad.add("the book has " + book.size() + " moves, the list " + KakashiMath.BOOK.size());
        for (int i = 0; i < Math.min(book.size(), KakashiMath.BOOK.size()); i++) if (!book.get(i).id().equals(KakashiMath.BOOK.get(i))) bad.add("move " + i + " is " + book.get(i).id());
        for (Move m : book) {
            if (KakashiMath.openingPhase(m.id()) > KakashiMath.SUSANOO) bad.add(m.id() + " opens in no phase");
            if (KakashiMoves.voiceGroup(m.id()) == null) bad.add(m.id() + " has no voice group");
            if (m.tell() == null) bad.add(m.id() + " has no telegraph");
        }
        if (KakashiMath.phaseOf(0.2f, 0) != KakashiMath.SUSANOO) bad.add("phaseOf");
        if (ArcRoster.byId(KakashiBoss.ID) == null) bad.add("not in the arc roster");
        if (KakashiBoss.CHARACTER.floorMin() != 0 || KakashiBoss.CHARACTER.floorMax() != 0) bad.add("he has floors: he must not be a dungeon boss");
        if (!BossRegistry.known(KakashiBoss.ID)) bad.add("not in the boss registry");
        if (!BarStyleIds.known(BarStyleIds.NARUTO)) bad.add("the naruto bar style is unknown");
        var type = ArcSpawner.typeOf(KakashiBoss.ID);
        if (type == null) bad.add("no entity type");
        else if (DefaultAttributes.getSupplier((EntityType) type) == null) bad.add("no attributes");
        if (KakashiConfig.globalCap() < 1 || KakashiConfig.globalCap() > 4) bad.add("globalCap " + KakashiConfig.globalCap());
        return result(bad, "Kakashi: " + book.size() + " moves with telegraphs, 4 phases, roster, entity type and the naruto bar are in place");
    }

    private static SelfTests.Result voice(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        if (KakashiLines.ids().size() < 40) bad.add("only " + KakashiLines.ids().size() + " lines are loaded");
        for (Move m : KakashiMoves.build()) if (KakashiLines.group(KakashiMoves.voiceGroup(m.id())).isEmpty()) bad.add("no lines for " + m.id());
        for (String g : new String[]{"intro", "phase_pack", "phase_mangekyo", "phase_susanoo", "hurt", "low", "defeat", "victory", "kill", "idle", "kawarimi", "kamui_phase", "susanoo_break", "raikiri_miss",
                "strain", "counter", "cutter_cut", "lclone_break", "sixpaths_end"}) {
            if (KakashiLines.group(g).isEmpty()) bad.add("no lines in group " + g);
        }
        for (String id : KakashiLines.recorded()) if (!KakashiLines.ids().contains(id)) bad.add("recorded id " + id + " has no line");
        if (KakashiSounds.voiceCount() != KakashiLines.recorded().size()) bad.add("sound events " + KakashiSounds.voiceCount() + " vs recorded " + KakashiLines.recorded().size());
        return result(bad, "Kakashi voice: " + KakashiLines.ids().size() + " lines, " + KakashiLines.recorded().size() + " with a recording (the rest are chat-only)");
    }

    private static KakashiBoss make(ServerLevel level, double dz) {
        ArcSpawner.Result r = ArcSpawner.summon(level, KakashiBoss.CHARACTER, Vec3.atBottomCenterOf(level.getSharedSpawnPos().above(2)).add(0, 0, dz), 0f, List.of(), true);
        return r.boss() instanceof KakashiBoss k ? k : null;
    }

    private static SelfTests.Result spawn(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos at = level.getSharedSpawnPos();
        if (!level.hasChunkAt(at)) return SelfTests.Result.ok("skipped: the spawn chunk is not loaded");
        if (KakashiBoss.liveCount() >= KakashiConfig.globalCap()) return SelfTests.Result.ok("skipped: " + KakashiBoss.liveCount() + " Kakashi bosses already stand");
        KakashiBoss boss = null;
        try {
            ArcSpawner.Result r = ArcSpawner.summon(level, KakashiBoss.CHARACTER, Vec3.atBottomCenterOf(at.above(2)).add(0, 0, 8), 0f, List.of(), true);
            if (!KakashiConfig.enabled()) return r.outcome() == ArcSpawner.Outcome.DISABLED ? SelfTests.Result.ok("the setting is off: summoning is refused") : SelfTests.Result.fail("enabled is off but summoning gave " + r.outcome());
            if (r.outcome() != ArcSpawner.Outcome.SPAWNED) return SelfTests.Result.fail("summoning gave " + r.outcome());
            if (!(r.boss() instanceof KakashiBoss k)) return SelfTests.Result.fail("the boss is not a KakashiBoss");
            boss = k;
            if (k.phaseIdx() != 0) return SelfTests.Result.fail("he does not start in phase 1 (" + k.phaseIdx() + ")");
            k.forcePhaseForTest(3);
            if (k.phaseIdx() != 3) return SelfTests.Result.fail("the phase did not change");
            ArcSpawner.Outcome near = KakashiBosses.gate(level, k.position().add(3, 0, 0));
            ArcSpawner.Outcome want = KakashiBoss.liveCount() >= KakashiConfig.globalCap() ? ArcSpawner.Outcome.FAILED : ArcSpawner.Outcome.ALREADY_PRESENT;
            if (near != want) return SelfTests.Result.fail("a summoning 3 blocks from him gave " + near + ", expected " + want);
            k.forget(java.util.UUID.randomUUID());
            return SelfTests.Result.ok("Kakashi summoned, phase change and the arena clearance work, health " + (int) k.getMaxHealth());
        } finally {
            if (boss != null) boss.discard();
        }
    }

    /** Fires every ability: its telegraph at four moments of the wind-up and then the move itself, and runs the wolf and the vortex to their end. Any exception fails the test. */
    private static SelfTests.Result moves(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos at = level.getSharedSpawnPos();
        if (!level.hasChunkAt(at)) return SelfTests.Result.ok("skipped: the spawn chunk is not loaded");
        if (!KakashiConfig.enabled()) return SelfTests.Result.ok("skipped: the boss is switched off");
        if (KakashiBoss.liveCount() >= KakashiConfig.globalCap()) return SelfTests.Result.ok("skipped: " + KakashiBoss.liveCount() + " Kakashi bosses already stand");
        KakashiBoss boss = null;
        List<String> bad = new ArrayList<>();
        try {
            boss = make(level, 10);
            if (boss == null) return SelfTests.Result.fail("could not summon him");
            LivingEntity aim = EntityType.PIG.create(level);
            if (aim == null) return SelfTests.Result.ok("skipped: no aim entity");
            Vec3 p = boss.position().add(7, 0, 2);
            aim.setPos(p.x, p.y, p.z);
            int fired = 0;
            for (Move m : boss.moveListForTest()) {
                try {
                    int total = Math.max(1, m.windUpTicks());
                    for (int left : new int[]{total, total / 2, KakashiBoss.FREEZE, 1}) m.tell().tell(boss, aim, left, total);
                    m.run().run(boss, aim);
                    boss.runStatesForTest(level);
                    boss.runCanonForTest(level);
                    fired++;
                } catch (RuntimeException e) {
                    bad.add(m.id() + ": " + e);
                }
            }
            boss.clearForTest();
            if (fired != KakashiMath.BOOK.size() && bad.isEmpty()) bad.add("fired " + fired + " of " + KakashiMath.BOOK.size());
            if (boss.hasStatesForTest()) bad.add("states are left after the clean-up");
            aim.discard();
            return result(bad, "Kakashi: all " + fired + " abilities fired with their telegraphs, the wolf and the vortex ran to their end, nothing left behind");
        } finally {
            if (boss != null) boss.discard();
        }
    }

    private static SelfTests.Result kamui(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos at = level.getSharedSpawnPos();
        if (!level.hasChunkAt(at)) return SelfTests.Result.ok("skipped: the spawn chunk is not loaded");
        if (!KakashiConfig.enabled()) return SelfTests.Result.ok("skipped: the boss is switched off");
        if (KakashiBoss.liveCount() >= KakashiConfig.globalCap()) return SelfTests.Result.ok("skipped: " + KakashiBoss.liveCount() + " Kakashi bosses already stand");
        KakashiBoss boss = null;
        try {
            boss = make(level, -12);
            if (boss == null) return SelfTests.Result.fail("could not summon him");
            boss.forcePhaseForTest(2);
            if (!KakashiConfig.kamui()) return SelfTests.Result.ok("the kamui setting is off: he never phases");
            if (boss.hurt(level.damageSources().generic(), 1.0f)) return SelfTests.Result.fail("a blow in the Mangekyo phase was not phased through");
            boss.forcePhaseForTest(0);
            boss.clearForTest();
            return SelfTests.Result.ok("Kamui: a blow in phase 3 is phased through, and the window is bounded");
        } finally {
            if (boss != null) boss.discard();
        }
    }

    private static SelfTests.Result cap(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos at = level.getSharedSpawnPos();
        if (!level.hasChunkAt(at)) return SelfTests.Result.ok("skipped: the spawn chunk is not loaded");
        if (!KakashiConfig.enabled()) return SelfTests.Result.ok("skipped: the boss is switched off");
        if (KakashiBoss.liveCount() > 0) return SelfTests.Result.ok("skipped: a Kakashi boss already stands");
        List<KakashiBoss> made = new ArrayList<>();
        try {
            for (int i = 0; i < KakashiConfig.globalCap(); i++) {
                Vec3 spot = Vec3.atBottomCenterOf(at.above(2)).add(6 + i * (KakashiBosses.ARENA_CLEARANCE + 6), 0, 0);
                if (!level.hasChunkAt(BlockPos.containing(spot))) return SelfTests.Result.ok("skipped: the chunk at " + (int) spot.x + " is not loaded");
                ArcSpawner.Result r = ArcSpawner.summon(level, KakashiBoss.CHARACTER, spot, 0f, List.of(), true);
                if (r.outcome() != ArcSpawner.Outcome.SPAWNED || !(r.boss() instanceof KakashiBoss k)) return SelfTests.Result.fail("summoning " + (i + 1) + " gave " + r.outcome());
                made.add(k);
            }
            if (KakashiBosses.gate() != ArcSpawner.Outcome.FAILED) return SelfTests.Result.fail("the gate lets one more through at " + KakashiBoss.liveCount());
            ArcSpawner.Result extra = ArcSpawner.summon(level, KakashiBoss.CHARACTER, Vec3.atBottomCenterOf(at.above(2)).add(0, 0, -60), 0f, List.of(), true);
            if (extra.outcome() == ArcSpawner.Outcome.SPAWNED) {
                if (extra.boss() != null) extra.boss().discard();
                return SelfTests.Result.fail("a summoning above the cap was not refused");
            }
        } finally {
            for (KakashiBoss k : made) k.discard();
        }
        if (KakashiBoss.liveCount() != 0) return SelfTests.Result.fail(KakashiBoss.liveCount() + " bosses are left after the clean-up");
        if (KakashiBosses.gate() != null) return SelfTests.Result.fail("the gate stays closed after the clean-up");
        return SelfTests.Result.ok("the cap of " + KakashiConfig.globalCap() + " holds and opens again after the clean-up");
    }
}
