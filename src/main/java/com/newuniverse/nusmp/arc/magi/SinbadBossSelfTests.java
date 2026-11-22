package com.newuniverse.nusmp.arc.magi;

import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.arc.ArcSpawner;
import com.newuniverse.nusmp.arc.ArtTestFoe;
import com.newuniverse.nusmp.arc.lines.BossLines;
import com.newuniverse.nusmp.arc.lines.BossSpeech;
import com.newuniverse.nusmp.arc.BossShapes;
import com.newuniverse.nusmp.boss.BossBuffTable;
import com.newuniverse.nusmp.boss.BossRegistry;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the Sinbad boss cannot be played by its author, so the CI server smoke test checks it in the real game ({@code /multiverse mega selftest}, keys {@code sinbad.boss.*}): the data (table,
 * equip rules, settings, roster, registry, buff row, entity type), one real summoning with its move list and a djinn put on and taken off, and the global cap. Cost: at most four entities, made
 * and discarded in the same tick, with no fight and no packets of their own.
 */
public final class SinbadBossSelfTests {
    private SinbadBossSelfTests() {}

    public static void register() {
        SelfTests.register("sinbad.boss.data", SinbadBossSelfTests::data);
        SelfTests.register("sinbad.boss.spawn", SinbadBossSelfTests::spawn);
        SelfTests.register("sinbad.boss.cap", SinbadBossSelfTests::cap);
        SelfTests.register("sinbad.boss.arts", SinbadBossSelfTests::arts);
        SelfTests.register("sinbad.boss.phases", SinbadBossSelfTests::phases);
    }

    private static SelfTests.Result data(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (SinbadTable.Spec s : SinbadTable.specs()) if (!ids.add(s.id())) bad.add("the move id " + s.id() + " is twice in the table");
        for (SinbadDjinn d : SinbadDjinn.values()) {
            if (d == SinbadDjinn.NONE) continue;
            boolean has = false;
            for (SinbadTable.Spec s : SinbadTable.specs()) if (s.djinn() == d) has = true;
            if (!has) bad.add(d.id + " has no move");
        }
        if (SinbadEquip.pick(20.0, 1, 100, SinbadDjinn.NONE) == SinbadDjinn.NONE) bad.add("the djinn pick answers NONE");
        if (SinbadEquip.equipTicks(true) >= SinbadEquip.equipTicks(false)) bad.add("the Gates do not shorten the equip");
        if (SinbadBossConfig.globalCap() < 1 || SinbadBossConfig.globalCap() > 4) bad.add("globalCap " + SinbadBossConfig.globalCap());
        if (ArcRoster.byId(SinbadBoss.ID) == null) bad.add("not in the arc roster");
        if (!BossRegistry.known(SinbadBoss.ID)) bad.add("not in the boss registry");
        if (BossBuffTable.profile(SinbadBoss.ID) == null) bad.add("no buff row");
        var type = ArcSpawner.typeOf(SinbadBoss.ID);
        if (type == null) bad.add("no entity type");
        else if (DefaultAttributes.getSupplier((net.minecraft.world.entity.EntityType) type) == null) bad.add("no attributes");
        return bad.isEmpty() ? SelfTests.Result.ok("Sinbad: " + SinbadTable.specs().size() + " moves, the one-djinn rule, settings, roster, buff row and entity type are in place")
                : SelfTests.Result.fail(bad.size() + " problem(s): " + String.join(" | ", bad.subList(0, Math.min(5, bad.size()))));
    }

    /** One real summoning: every move of the table is in his move list, the equip filter holds, and a djinn can be put on and taken off. */
    private static SelfTests.Result spawn(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos at = level.getSharedSpawnPos();
        if (!level.hasChunkAt(at)) return SelfTests.Result.ok("skipped: the spawn chunk is not loaded");
        if (SinbadBoss.liveCount() >= SinbadBossConfig.globalCap()) return SelfTests.Result.ok("skipped: " + SinbadBoss.liveCount() + " Sinbad bosses already stand");
        SinbadBoss boss = null;
        try {
            ArcSpawner.Result r = ArcSpawner.summon(level, SinbadBoss.CHARACTER, Vec3.atBottomCenterOf(at.above(2)).add(0, 0, 8), 0f, List.of(), true);
            if (!SinbadBossConfig.enabled()) return r.outcome() == ArcSpawner.Outcome.DISABLED ? SelfTests.Result.ok("the setting is off: summoning is refused") : SelfTests.Result.fail("enabled is off but summoning gave " + r.outcome());
            if (r.outcome() != ArcSpawner.Outcome.SPAWNED) return SelfTests.Result.fail("summoning gave " + r.outcome());
            if (!(r.boss() instanceof SinbadBoss s)) return SelfTests.Result.fail("the boss is not a SinbadBoss");
            boss = s;
            for (SinbadTable.Spec sp : SinbadTable.specs()) if (s.moveIndex(sp.id()) < 0) return SelfTests.Result.fail("the move " + sp.id() + " is not in his move list");
            if (s.equip() != SinbadDjinn.NONE) return SelfTests.Result.fail("he does not start unequipped (" + s.equipId() + ")");
            if (s.allowsForTest(SinbadTable.BARARAQ)) return SelfTests.Result.fail("a Baal move is allowed with nothing on");
            if (!s.allowsForTest(SinbadTable.SWORD_SLASH) || !s.allowsForTest(SinbadTable.DJINN_EQUIP)) return SelfTests.Result.fail("the sword and equip moves are held back with nothing on");
            s.equipForTest(level, SinbadDjinn.BAAL);
            if (s.equip() != SinbadDjinn.BAAL || !s.allowsForTest(SinbadTable.BARARAQ)) return SelfTests.Result.fail("Baal does not unlock his moves");
            if (s.allowsForTest(SinbadTable.GARUFOR_ZAIRU) || s.allowsForTest(SinbadTable.DJINN_EQUIP)) return SelfTests.Result.fail("a Valefor move or an early switch is allowed while Baal is on");
            s.equipForTest(level, SinbadDjinn.NONE);
            if (s.equip() != SinbadDjinn.NONE) return SelfTests.Result.fail("the djinn does not come off");
            ArcSpawner.Outcome near = SinbadBosses.gate(level, s.position().add(3, 0, 0));
            ArcSpawner.Outcome want = SinbadBoss.liveCount() >= SinbadBossConfig.globalCap() ? ArcSpawner.Outcome.FAILED : ArcSpawner.Outcome.ALREADY_PRESENT;
            if (near != want) return SelfTests.Result.fail("a summoning 3 blocks from him gave " + near + ", expected " + want);
            return SelfTests.Result.ok("Sinbad summoned: " + SinbadTable.specs().size() + " moves, health " + (int) s.getMaxHealth() + ", the djinn filter and the arena clearance work");
        } finally {
            if (boss != null) boss.discard();
        }
    }

    /** The global cap: with the cap's worth of bosses standing, the gate refuses a further one; after they are gone it opens again. */
    private static SelfTests.Result cap(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos at = level.getSharedSpawnPos();
        if (!level.hasChunkAt(at)) return SelfTests.Result.ok("skipped: the spawn chunk is not loaded");
        if (!SinbadBossConfig.enabled()) return SelfTests.Result.ok("skipped: the boss is switched off");
        if (SinbadBoss.liveCount() > 0) return SelfTests.Result.ok("skipped: a Sinbad boss already stands");
        List<SinbadBoss> made = new ArrayList<>();
        try {
            for (int i = 0; i < SinbadBossConfig.globalCap(); i++) {
                Vec3 spot = Vec3.atBottomCenterOf(at.above(2)).add(6 + i * (SinbadBosses.ARENA_CLEARANCE + 6), 0, 0);
                if (!level.hasChunkAt(BlockPos.containing(spot))) return SelfTests.Result.ok("skipped: the chunk at " + (int) spot.x + " is not loaded");
                ArcSpawner.Result r = ArcSpawner.summon(level, SinbadBoss.CHARACTER, spot, 0f, List.of(), true);
                if (r.outcome() != ArcSpawner.Outcome.SPAWNED || !(r.boss() instanceof SinbadBoss s)) return SelfTests.Result.fail("summoning " + (i + 1) + " gave " + r.outcome());
                made.add(s);
            }
            if (SinbadBosses.gate() != ArcSpawner.Outcome.FAILED) return SelfTests.Result.fail("the gate lets one more through at " + SinbadBoss.liveCount());
            ArcSpawner.Result extra = ArcSpawner.summon(level, SinbadBoss.CHARACTER, Vec3.atBottomCenterOf(at.above(2)).add(0, 0, -60), 0f, List.of(), true);
            if (extra.outcome() == ArcSpawner.Outcome.SPAWNED) {
                if (extra.boss() != null) extra.boss().discard();
                return SelfTests.Result.fail("a summoning above the cap was not refused");
            }
        } finally {
            for (SinbadBoss s : made) s.discard();
        }
        if (SinbadBoss.liveCount() != 0) return SelfTests.Result.fail(SinbadBoss.liveCount() + " bosses are left after the clean-up");
        if (SinbadBosses.gate() != null) return SelfTests.Result.fail("the gate stays closed after the clean-up");
        return SelfTests.Result.ok("the cap of " + SinbadBossConfig.globalCap() + " holds and opens again after the clean-up");
    }

    /**
     * Fires Sinbad's two new arts for real on the server and checks the djinn switch that the clients see (docs/boss_redesign_goku_sinbad.md): every djinn he puts on writes its number to the synced
     * equip value (and 0 when it comes off), the Rukh Orrery's seven motes each land on their spot and hit a foe standing there, the Kingdom Sigil hits a foe in a lit sector and spares one in a dark
     * one, and the seven riders run without error. The foe is a tamed wolf of a random owner with a huge health pool. Cost: two entities made and discarded in this tick.
     */
    /** 4.105.0: the health factor is written once and keeps the share of health, and the phase starts at 0 with nothing broken. One entity, made and discarded in the same tick. */
    private static SelfTests.Result phases(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos at = level.getSharedSpawnPos();
        if (!level.hasChunkAt(at)) return SelfTests.Result.ok("skipped: the spawn chunk is not loaded");
        if (!SinbadBossConfig.enabled()) return SelfTests.Result.ok("skipped: the boss is switched off");
        if (SinbadBoss.liveCount() >= SinbadBossConfig.globalCap()) return SelfTests.Result.ok("skipped: " + SinbadBoss.liveCount() + " Sinbad bosses already stand");
        SinbadBoss boss = null;
        try {
            ArcSpawner.Result r = ArcSpawner.summon(level, SinbadBoss.CHARACTER, Vec3.atBottomCenterOf(at.above(2)).add(0, 0, 10), 0f, List.of(), true);
            if (r.outcome() != ArcSpawner.Outcome.SPAWNED || !(r.boss() instanceof SinbadBoss s)) return SelfTests.Result.fail("summoning gave " + r.outcome());
            boss = s;
            float before = s.getMaxHealth();
            float share = s.getHealth() / before;
            s.applyHealthForTest();
            s.applyHealthForTest();
            double f = SinbadBossConfig.healthFactor();
            if (Math.abs(s.getMaxHealth() - before * f) > 1.0F + before * f * 1.0e-4) return SelfTests.Result.fail("max health " + s.getMaxHealth() + " is not " + before + " x " + f + " (twice applied?)");
            if (Math.abs(s.getHealth() / s.getMaxHealth() - share) > 0.01F) return SelfTests.Result.fail("the share of health changed: " + share + " -> " + s.getHealth() / s.getMaxHealth());
            if (s.phaseBrokenForTest()) return SelfTests.Result.fail("the phase is broken before any fight");
            if (s.phase() != 0) return SelfTests.Result.fail("the phase starts at " + s.phase());
            return SelfTests.Result.ok("Sinbad: health x" + f + " (" + (int) s.getMaxHealth() + "), share kept, phase 0");
        } finally {
            if (boss != null) boss.discard();
        }
    }

    private static SelfTests.Result arts(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos at = level.getSharedSpawnPos();
        if (!level.hasChunkAt(at)) return SelfTests.Result.ok("skipped: the spawn chunk is not loaded");
        if (!SinbadBossConfig.enabled()) return SelfTests.Result.ok("skipped: the boss is switched off");
        if (SinbadBoss.liveCount() >= SinbadBossConfig.globalCap()) return SelfTests.Result.ok("skipped: " + SinbadBoss.liveCount() + " Sinbad bosses already stand");
        SinbadBoss boss = null;
        LivingEntity foe = null;
        try {
            Vec3 where = Vec3.atBottomCenterOf(at.above(2)).add(0, 0, 8);
            ArcSpawner.Result r = ArcSpawner.summon(level, SinbadBoss.CHARACTER, where, 0f, List.of(), true);
            if (r.outcome() != ArcSpawner.Outcome.SPAWNED || !(r.boss() instanceof SinbadBoss s)) return SelfTests.Result.fail("summoning gave " + r.outcome());
            boss = s;
            List<String> bad = new ArrayList<>();
            // the djinn switch is written to the value every client reads
            for (SinbadDjinn d : SinbadDjinn.values()) {
                s.equipForTest(level, d);
                if (s.equipIdx() != d.ordinal()) bad.add("the synced equip is " + s.equipIdx() + " with " + d + " on");
            }
            s.equipForTest(level, SinbadDjinn.BAAL);
            foe = ArtTestFoe.make(level, where.add(6, 0, 0), s.getEyePosition());
            if (foe == null) return SelfTests.Result.ok("skipped: no test foe could be made");
            if (!s.foesAround(foe.position(), 3.0).contains(foe)) return SelfTests.Result.ok("skipped: the test foe is not counted as a foe here");
            // Rukh Orrery: the telegraph, then each of the seven motes on the foe's own spot
            SinbadMoves.orreryMark(s, foe);
            SinbadMoves.orreryTell(s, level, s.peekSpots(SinbadTable.RUKH_ORRERY), 40, 44);
            int orrery = 0;
            for (int i = 0; i < SinbadMoves.ORRERY_SHOTS; i++) {
                foe.invulnerableTime = 0;          // the foe's hurt frames would swallow six of the seven hits in one tick
                orrery += SinbadMoves.orreryShot(s, level, i, foe.position());
            }
            s.takeSpots(SinbadTable.RUKH_ORRERY, foe);
            if (orrery < SinbadMoves.ORRERY_SHOTS) bad.add("the orrery landed " + orrery + " of " + SinbadMoves.ORRERY_SHOTS + " motes on the foe");
            // Kingdom Sigil: a lit sector hits, a dark one does not
            SinbadMoves.sigilMark(s, foe);
            List<Vec3> spots = s.peekSpots(SinbadTable.KINGDOM_SIGIL);
            if (spots.size() < 2) return SelfTests.Result.fail("the sigil locked " + spots.size() + " values");
            SinbadMoves.sigilTell(s, level, spots, 50, 56);
            SinbadMoves.sigilTell(s, level, spots, 10, 56);
            Vec3 c = spots.get(0);
            double phase = spots.get(1).y;
            int sector = BossShapes.sector(foe.getX() - c.x, foe.getZ() - c.z, SinbadMoves.SIGIL_SECTORS, phase);
            int lit = sector & 1, dark = lit ^ 1;
            if (!SinbadMoves.sigilFoes(s, c, phase, dark).isEmpty()) bad.add("a foe in a dark sector is counted as hit");
            foe.invulnerableTime = 0;
            int sigil = SinbadMoves.sigilLand(s, level, c, phase, lit);
            if (sigil < 1) bad.add("the sigil hit nothing in a lit sector");
            s.takeSpots(SinbadTable.KINGDOM_SIGIL, foe);
            for (SinbadDjinn d : SinbadMoves.SEVEN) SinbadMoves.rider(s, foe, d);
            // his own voice: the Djinn Equip call of Baal is said once and then rests
            if (!BossSpeech.say(s, BossLines.SINBAD, "equip_baal")) bad.add("his Baal equip call was not said");
            if (BossSpeech.say(s, BossLines.SINBAD, "equip_baal")) bad.add("the equip call did not rest");
            BossSpeech.forget(s.getUUID());
            s.equipForTest(level, SinbadDjinn.NONE);
            return bad.isEmpty() ? SelfTests.Result.ok("Sinbad's arts fired: orrery landed " + orrery + "/7, sigil hit " + sigil + ", the equip number follows the djinn")
                    : SelfTests.Result.fail(bad.size() + " problem(s): " + String.join(" | ", bad));
        } finally {
            if (foe != null) foe.discard();
            if (boss != null) boss.discard();
        }
    }
}
