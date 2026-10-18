package com.newuniverse.nusmp.elite.world;

import com.newuniverse.nusmp.elite.api.CalamityDamageEvent;
import com.newuniverse.nusmp.elite.api.CalamityEvents;
import com.newuniverse.nusmp.elite.api.CalamityKillEvent;
import com.newuniverse.nusmp.elite.api.EliteHooks;
import com.newuniverse.nusmp.elite.world.block.CrystalBudBlock;
import com.newuniverse.nusmp.elite.world.block.CrystalLogic;
import com.newuniverse.nusmp.elite.world.block.MotherRockBlock;
import com.newuniverse.nusmp.elite.world.calamity.CalamityData;
import com.newuniverse.nusmp.elite.world.calamity.CalamityManager;
import com.newuniverse.nusmp.elite.world.entity.CalamityBoss;
import com.newuniverse.nusmp.elite.world.entity.GaiaSpikeEntity;
import com.newuniverse.nusmp.elite.world.entity.GraveboundColossus;
import com.newuniverse.nusmp.elite.world.entity.LichBoss;
import com.newuniverse.nusmp.elite.world.entity.PhylacteryEntity;
import com.newuniverse.nusmp.elite.world.entity.VoidBoltProjectile;
import com.newuniverse.nusmp.elite.world.rules.CalamityDef;
import com.newuniverse.nusmp.elite.world.rules.CalamityDirector;
import com.newuniverse.nusmp.elite.world.rules.CrystalStage;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * Why: nothing here can be played by the author, so the world engine registers in-game self-tests (key prefix {@code elite.world.}) that the CI server smoke test runs in the real game
 * ({@code /multiverse mega selftest elite.world.<name>}). Each drives the subsystem through its OWN entry points on a high platform in the overworld (far from anything), asserts, and cleans up.
 * They need the master switch on and the overworld in the dimension list; otherwise they report themselves as skipped (a pass with the reason), because a deliberately switched off engine is not a failure.
 */
public final class WorldSelfTests {
    private WorldSelfTests() {}

    /** The platform: far away, high up, in the overworld. */
    private static final BlockPos ORIGIN = new BlockPos(1208, 240, 1208);

    public static void register() {
        SelfTests.register("elite.world.density", WorldSelfTests::density);
        SelfTests.register("elite.world.crystal", WorldSelfTests::crystal);
        SelfTests.register("elite.world.conversion", WorldSelfTests::conversion);
        SelfTests.register("elite.world.touch", WorldSelfTests::touch);
        SelfTests.register("elite.world.calamity", WorldSelfTests::calamityRules);
        SelfTests.register("elite.world.bosses", WorldSelfTests::bosses);
        SelfTests.register("elite.world.entities", WorldSelfTests::entities);
    }

    private static String skip(ServerLevel level) {
        if (!WorldConfig.enabled()) return "the world engine is switched off in the config";
        if (!WorldGuard.allowed(level)) return "the overworld is not simulated by the config";
        return null;
    }

    private static LevelChunk chunk(ServerLevel lv, BlockPos pos) { return lv.getChunk(pos.getX() >> 4, pos.getZ() >> 4); }

    private static void clearArea(ServerLevel lv) {
        for (BlockPos p : BlockPos.betweenClosed(ORIGIN.offset(-8, -3, -8), ORIGIN.offset(8, 6, 8))) lv.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
        LevelChunk ch = chunk(lv, ORIGIN);
        ch.removeData(WorldAttachments.CHUNK);
        ch.setUnsaved(true);
    }

    private static void platform(ServerLevel lv) {
        for (int dx = -6; dx <= 6; dx++) for (int dz = -6; dz <= 6; dz++) lv.setBlock(ORIGIN.offset(dx, -1, dz), Blocks.STONE.defaultBlockState(), 3);
    }

    // ================================================================ 1. density

    private static Result density(MinecraftServer server) throws Exception {
        ServerLevel lv = server.overworld();
        String why = skip(lv);
        if (why != null) return Result.ok("skipped: " + why);
        try {
            LevelChunk ch = chunk(lv, ORIGIN);
            ch.removeData(WorldAttachments.CHUNK);
            double natural = MagiculeApi.stored(lv, ORIGIN);
            if (natural < 0.0 || natural > 1.0) return Result.fail("the natural density is not in 0..1: " + natural);
            MagiculeApi.update(lv, ch);
            ChunkMagicule d = MagiculeApi.data(ch);
            if (!d.initialised()) return Result.fail("update did not stamp the chunk");
            if (Math.abs(d.density - natural) > 0.02) return Result.fail("the first update must start the chunk at its natural value: " + d.density + " vs " + natural);
            MagiculeApi.force(lv, ch, 0.9);
            if (Math.abs(MagiculeApi.stored(lv, ORIGIN) - 0.9) > 0.01) return Result.fail("force did not set the density");
            double deep = MagiculeApi.effective(lv, new BlockPos(ORIGIN.getX(), -40, ORIGIN.getZ()));
            if (deep < MagiculeApi.effective(lv, ORIGIN) - 1e-9) return Result.fail("deeper must not be thinner");
            ServerPlayer fake = SelfTests.standIn(lv, "elite_world_density", ORIGIN.getX() + 0.5, ORIGIN.getY(), ORIGIN.getZ() + 0.5);
            double hook = EliteHooks.chunkMagicule.applyAsDouble(fake);
            if (Math.abs(hook - MagiculeApi.effectiveAt(fake)) > 1e-9 || hook < 0.85) return Result.fail("EliteHooks.chunkMagicule is not wired to the engine: " + hook);
            MagiculeApi.bump(lv, ORIGIN, -0.3, 0);
            if (d.event >= 0f) return Result.fail("a negative bump must lower the event term");
            int updated = MagiculeEngine.updatePass(server);
            if (updated < 0) return Result.fail("updatePass");
            MagiculeSync.tick(server);                                                    // must not throw with no players or with players
            return Result.ok("natural " + String.format("%.2f", natural) + ", forced 0.90, deep " + String.format("%.2f", deep) + ", hook " + String.format("%.2f", hook) + ", pass over " + updated + " chunks");
        } finally {
            chunk(lv, ORIGIN).removeData(WorldAttachments.CHUNK);
        }
    }

    // ================================================================ 2. crystals

    private static BlockState bud(CrystalStage stage) { return WorldBlocks.blockOf(stage).defaultBlockState(); }

    private static Result crystal(MinecraftServer server) {
        ServerLevel lv = server.overworld();
        String why = skip(lv);
        if (why != null) return Result.ok("skipped: " + why);
        if (!WorldConfig.crystals()) return Result.ok("skipped: crystal growth is switched off");
        try {
            LevelChunk ch = chunk(lv, ORIGIN);
            platform(lv);
            MagiculeApi.force(lv, ch, 1.0);
            BlockPos c = ORIGIN;
            lv.setBlock(c, bud(CrystalStage.SMALL_BUD), 3);
            if (MagiculeApi.data(ch).crystalScore != CrystalStage.SMALL_BUD.weight) return Result.fail("a placed bud must add its weight to the crystal score: " + MagiculeApi.data(ch).crystalScore);
            for (CrystalStage st : new CrystalStage[]{CrystalStage.SMALL_BUD, CrystalStage.MEDIUM_BUD, CrystalStage.LARGE_BUD}) {
                CrystalStage next = st.next();
                int tries = 0;
                while (lv.getBlockState(c).getBlock() != WorldBlocks.blockOf(next) && tries++ < 4000) {
                    BlockState s = lv.getBlockState(c);
                    if (!(s.getBlock() instanceof CrystalBudBlock b)) return Result.fail("the crystal turned into " + s);
                    CrystalLogic.ripen(b, s, lv, c, lv.getRandom());
                }
                if (lv.getBlockState(c).getBlock() != WorldBlocks.blockOf(next)) return Result.fail(st + " did not ripen to " + next + " at density 1 in 4000 ticks");
            }
            if (MagiculeApi.data(ch).crystalScore != CrystalStage.LARGE_BUD.weight) return Result.fail("the score must follow the stages (large bud = 3): " + MagiculeApi.data(ch).crystalScore);
            // density too low: it must not grow
            MagiculeApi.force(lv, ch, 0.05);
            for (int i = 0; i < 300; i++) {
                BlockState s = lv.getBlockState(c);
                CrystalLogic.ripen((CrystalBudBlock) s.getBlock(), s, lv, c, lv.getRandom());
            }
            if (lv.getBlockState(c).getBlock() != WorldBlocks.BUD_LARGE.get()) return Result.fail("a large bud must not ripen at density 0.05");
            MagiculeApi.force(lv, ch, 1.0);
            int tries = 0;
            while (lv.getBlockState(c).getBlock() != WorldBlocks.CLUSTER.get() && tries++ < 4000) {
                BlockState s = lv.getBlockState(c);
                CrystalLogic.ripen((CrystalBudBlock) s.getBlock(), s, lv, c, lv.getRandom());
            }
            if (lv.getBlockState(c).getBlock() != WorldBlocks.CLUSTER.get()) return Result.fail("a large bud did not ripen to a cluster");
            if (WorldConfig.motherCap() == 0) return Result.ok("cluster reached; the Mother Rock step is switched off (motherRocksPerChunk = 0)");
            // a grove around it, then the Mother Rock
            for (BlockPos g : new BlockPos[]{c.east(2), c.west(2), c.south(2), c.north(2)}) lv.setBlock(g, bud(CrystalStage.CLUSTER), 3);
            tries = 0;
            while (!(lv.getBlockState(c).getBlock() instanceof MotherRockBlock) && tries++ < 20000) {
                BlockState s = lv.getBlockState(c);
                if (s.getBlock() instanceof CrystalBudBlock b) CrystalLogic.ripen(b, s, lv, c, lv.getRandom());
            }
            if (!(lv.getBlockState(c).getBlock() instanceof MotherRockBlock)) return Result.fail("a cluster in a grove at density 1 did not become a Mother Rock");
            if (MagiculeApi.mothersAt(lv, c) != 1) return Result.fail("the chunk must know its Mother Rock: " + MagiculeApi.mothersAt(lv, c));
            // charge and seed
            int budsBefore = countBuds(lv);
            tries = 0;
            while (tries++ < 6000) {
                BlockState s = lv.getBlockState(c);
                CrystalLogic.motherTick((MotherRockBlock) s.getBlock(), s, lv, c, lv.getRandom());
                if (countBuds(lv) > budsBefore) break;
            }
            if (countBuds(lv) <= budsBefore) return Result.fail("a Mother Rock did not seed a bud near it in 6000 ticks");
            double withRock = MagiculeApi.target(lv, ch, MagiculeApi.data(ch));
            lv.setBlock(c, Blocks.AIR.defaultBlockState(), 3);
            if (MagiculeApi.mothersAt(lv, c) != 0) return Result.fail("a broken Mother Rock must leave the chunk's list");
            if (MagiculeApi.data(ch).event >= 0f) return Result.fail("breaking a Mother Rock must thin the chunk (negative event)");
            return Result.ok("small -> medium -> large -> cluster -> Mother Rock -> seeded a bud; target with the rock " + String.format("%.2f", withRock));
        } finally {
            clearArea(lv);
        }
    }

    private static int countBuds(ServerLevel lv) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(ORIGIN.offset(-6, -1, -6), ORIGIN.offset(6, 6, 6))) if (lv.getBlockState(p).getBlock() instanceof CrystalBudBlock) n++;
        return n;
    }

    // ================================================================ 3. ore conversion

    private static Result conversion(MinecraftServer server) {
        ServerLevel lv = server.overworld();
        String why = skip(lv);
        if (why != null) return Result.ok("skipped: " + why);
        if (WorldData.ores().isEmpty()) return Result.fail("the ore conversion map is empty (data/nusmp/elite/world/ore_conversion.json did not load)");
        try {
            LevelChunk ch = chunk(lv, ORIGIN);
            platform(lv);
            MagiculeApi.force(lv, ch, 0.95);
            ChunkMagicule d = MagiculeApi.data(ch);
            d.ore.windowStart = 0;
            d.ore.usedInWindow = 0;
            d.ore.total = 0;
            lv.setBlock(ORIGIN, Blocks.IRON_ORE.defaultBlockState(), 3);
            Optional<String> target = OreConverter.targetFor("minecraft:iron_ore", 0.95, ORIGIN.getY());
            Random r = new Random(1);
            BlockPos done = OreConverter.pass(lv, ch, d, lv.getRandom());
            boolean tensura = target.isPresent();
            if (tensura && done == null) return Result.fail("iron ore at density 0.95 was not converted although the target " + target.get() + " exists");
            if (!tensura && done != null) return Result.fail("converted to a block that does not exist");
            if (done != null) {
                if (lv.getBlockState(done).is(Blocks.IRON_ORE)) return Result.fail("the block did not change");
                // the budget: one per window, so a second ore in the same window stays
                lv.setBlock(ORIGIN.east(), Blocks.COAL_ORE.defaultBlockState(), 3);
                if (OreConverter.pass(lv, ch, d, lv.getRandom()) != null) return Result.fail("the budget of one per window was exceeded");
                // below the threshold nothing happens even with a fresh window
                d.ore.windowStart = 0;
                d.ore.usedInWindow = 0;
                MagiculeApi.force(lv, ch, 0.3);
                if (OreConverter.pass(lv, ch, d, lv.getRandom()) != null) return Result.fail("converted below the density threshold");
            }
            return Result.ok(tensura ? "iron ore became " + target.get() + ", one per window, nothing below the threshold" : "no Tensura target is registered: correctly nothing converted");
        } finally {
            clearArea(lv);
        }
    }

    // ================================================================ 4. touched mobs and the protected levels

    private static Result touch(MinecraftServer server) {
        ServerLevel lv = server.overworld();
        String why = skip(lv);
        if (why != null) return Result.ok("skipped: " + why);
        Zombie z = EntityType.ZOMBIE.create(lv);
        if (z == null) return Result.fail("could not create a zombie");
        double hp = z.getMaxHealth();
        MobTouchHandler.apply(z, 2);
        double want = hp * WorldData.mobs().stats(2).health();
        if (Math.abs(z.getMaxHealth() - want) > 0.01) return Result.fail("tier 2 max health " + z.getMaxHealth() + " expected " + want);
        if (MobTouchHandler.tierOf(z) != 2) return Result.fail("the tier mark is missing");
        if (!z.hasCustomName()) return Result.fail("the name marker is missing");
        if (z.getAttributeValue(Attributes.ARMOR) < 4.0) return Result.fail("tier 2 must add armor");
        if (!WorldData.mobs().allows("minecraft:zombie") || WorldData.mobs().allows("minecraft:cow")) return Result.fail("the eligible table is wrong");
        // the protected levels: no action at all
        List<String> refused = new ArrayList<>();
        for (var key : new net.minecraft.resources.ResourceKey[]{com.newuniverse.nusmp.labyrinth.Labyrinth.MEGA_DIMENSION, com.newuniverse.nusmp.labyrinth.Labyrinth.SANCTUARY_DIMENSION,
                com.newuniverse.nusmp.labyrinth.Labyrinth.DIMENSION}) {
            @SuppressWarnings("unchecked") ServerLevel other = server.getLevel(key);
            if (other == null) continue;
            if (WorldGuard.allowed(other)) return Result.fail(key.location() + " must be refused by the world engine");
            refused.add(key.location().getPath());
        }
        return Result.ok("tier 2 zombie " + hp + " -> " + z.getMaxHealth() + " hp; refused levels: " + (refused.isEmpty() ? "(none exist in this world)" : String.join(", ", refused)));
    }

    // ================================================================ 5. the calamity data and the director on the real files

    private static Result calamityRules(MinecraftServer server) {
        List<CalamityDef> defs = WorldData.calamities();
        if (defs.size() < 2) return Result.fail("expected the two shipped calamities, found " + defs.size());
        for (CalamityDef d : defs) {
            ResourceLocation rl = ResourceLocation.tryParse(d.entity());
            if (rl == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(rl)) return Result.fail("calamity " + d.id() + " names an unregistered entity " + d.entity());
            if (!(BuiltInRegistries.ENTITY_TYPE.get(rl).create(server.overworld()) instanceof CalamityBoss b)) return Result.fail("calamity " + d.id() + " does not name a calamity boss");
            if (b.getMaxHealth() <= 0) return Result.fail("zero health: " + d.id());
            b.discard();
        }
        // the director fires on the real triggers of every automatic calamity
        int fired = 0;
        UUID p = UUID.nameUUIDFromBytes("elite_world_director".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        for (CalamityDef d : defs) {
            CalamityDirector dir = new CalamityDirector();
            CalamityDef.Trigger t = null;
            for (CalamityDef.Trigger x : d.triggers()) if (x.automatic()) { t = x; break; }
            if (t == null) continue;
            long now = 10_000_000L;
            Optional<CalamityDirector.Decision> dec = Optional.empty();
            int step = 100, checks = Math.max(1, t.seconds() * 20 / step + 1);
            for (int i = 0; i < checks + 1 && dec.isEmpty(); i++) {
                boolean night = true;
                dec = dir.check(List.of(d), new CalamityDirector.Observation(p, d.dimensions().isEmpty() ? "minecraft:overworld" : d.dimensions().get(0), 1.0, 9, night, t.moonPhase() >= 0 ? t.moonPhase() : 0, now), step, false, id -> -999_999_999L, new Random(3));
            }
            if (dec.isEmpty() && t.chance() >= 0.99) return Result.fail("the director never woke " + d.id() + " on its own first trigger " + t.type());
            if (dec.isPresent()) fired++;
        }
        return Result.ok(defs.size() + " calamities load, their entities exist, the director woke " + fired + " of them from their first trigger");
    }

    // ================================================================ 6. the bosses: phases, events, shield, core

    private static final class Catch {
        final List<CalamityDamageEvent> damage = new ArrayList<>();
        final List<CalamityKillEvent> kills = new ArrayList<>();
        final List<CalamityEvents.Base> announce = new ArrayList<>();

        @SubscribeEvent public void onDamage(CalamityDamageEvent e) { damage.add(e); }

        @SubscribeEvent public void onKill(CalamityKillEvent e) { kills.add(e); }

        @SubscribeEvent public void onAny(CalamityEvents.Base e) { announce.add(e); }
    }

    private static void tickBoss(CalamityBoss b, int n) {
        for (int i = 0; i < n && b.isAlive(); i++) b.tick();
    }

    private static Result bosses(MinecraftServer server) {
        ServerLevel lv = server.overworld();
        String why = skip(lv);
        if (why != null) return Result.ok("skipped: " + why);
        if (!WorldConfig.calamities()) return Result.ok("skipped: the calamity system is switched off");
        Catch got = new Catch();
        NeoForge.EVENT_BUS.register(got);
        List<CalamityBoss> made = new ArrayList<>();
        try {
            chunk(lv, ORIGIN);
            platform(lv);
            ServerPlayer fake = SelfTests.standIn(lv, "elite_world_boss", ORIGIN.getX() + 0.5, ORIGIN.getY() + 2, ORIGIN.getZ() + 5.5);
            // ---- the Gravebound Colossus: phases, the guard against skipping a phase, the damage event
            GraveboundColossus g = new GraveboundColossus(WorldEntities.GRAVEBOUND_COLOSSUS.get(), lv);
            made.add(g);
            g.moveTo(ORIGIN.getX() + 0.5, ORIGIN.getY(), ORIGIN.getZ() + 0.5, 0f, 0f);
            g.setup(GraveboundColossus.ID);
            lv.addFreshEntity(g);
            tickBoss(g, 3);
            if (g.phase() != 0 || g.phaseCount() != 3) return Result.fail("a new Colossus must be in phase 0 of 3: " + g.phase() + "/" + g.phaseCount());
            DamageSource hit = lv.damageSources().playerAttack(fake);
            float before = g.getHealth();
            g.dpsBudget().bypass(() -> g.setHealth(g.getMaxHealth()));
            g.hurt(hit, 5f);
            if (got.damage.isEmpty() || !got.damage.get(0).toBoss() || got.damage.get(0).amount() <= 0f) return Result.fail("no CalamityDamageEvent for a player hitting the Colossus");
            if (!g.fighters().containsKey(fake.getUUID())) return Result.fail("the fighter was not recorded");
            // a huge blow stops on the 66% line
            g.hurt(hit, 100000f);
            float frac = g.getHealth() / g.getMaxHealth();
            if (frac < 0.66f - 0.02f) return Result.fail("one blow crossed the 66% line: " + frac);
            tickBoss(g, 2);
            if (g.phase() != 1) return Result.fail("crossing 66% must begin phase 1: " + g.phase());
            if (g.hurt(hit, 5f)) return Result.fail("the Colossus must be invulnerable while it transforms");
            tickBoss(g, CalamityBoss.TRANSFORM_TICKS + 5);
            if (g.helperCount() <= 0) return Result.fail("phase 1 must call the dead");
            g.dpsBudget().bypass(() -> g.setHealth(g.getMaxHealth() * 0.2f));
            tickBoss(g, 2);
            if (g.phase() != 2) return Result.fail("under 33% must be phase 2: " + g.phase());
            tickBoss(g, CalamityBoss.TRANSFORM_TICKS + 5);
            if (!got.announce.stream().anyMatch(x -> x instanceof CalamityEvents.Rise)) return Result.fail("no Rise event");
            if (got.announce.stream().filter(x -> x instanceof CalamityEvents.PhaseChange).count() != 2) return Result.fail("expected two PhaseChange events, got " + got.announce.stream().filter(x -> x instanceof CalamityEvents.PhaseChange).count());
            // ---- the kill report
            g.dpsBudget().bypass(() -> g.setHealth(1f));
            g.hurt(hit, 100000f);
            tickBoss(g, 1);
            if (got.kills.isEmpty()) return Result.fail("no CalamityKillEvent when the Colossus fell");
            CalamityKillEvent k = got.kills.get(0);
            if (!k.calamityId().equals(GraveboundColossus.ID) || !k.participants().containsKey(fake.getUUID()) || k.phaseCount() != 3) return Result.fail("the kill event is wrong: " + k.calamityId() + " " + k.participants() + " " + k.phaseCount());
            if (!got.announce.stream().anyMatch(x -> x instanceof CalamityEvents.Defeat)) return Result.fail("no Defeat event");

            // ---- the Lich: the shield and its phylacteries
            LichBoss l = new LichBoss(WorldEntities.LICH_BOSS.get(), lv);
            made.add(l);
            l.moveTo(ORIGIN.getX() + 0.5, ORIGIN.getY(), ORIGIN.getZ() + 0.5, 0f, 0f);
            l.setup(LichBoss.ID);
            lv.addFreshEntity(l);
            tickBoss(l, 3);
            l.dpsBudget().bypass(() -> l.setHealth(l.getMaxHealth() * 0.6f));
            tickBoss(l, 2);
            if (l.phase() != 1 || !l.shielded()) return Result.fail("the Lich must be shielded in phase 1: phase " + l.phase() + " shield " + l.shielded());
            int vessels = l.phylacteriesLeft();
            if (vessels != 3) return Result.fail("phase 1 must raise three phylacteries: " + vessels);
            tickBoss(l, CalamityBoss.TRANSFORM_TICKS + 5);
            float hp = l.getHealth();
            l.hurt(hit, 200f);
            if (hp - l.getHealth() > 10f) return Result.fail("a shielded Lich lost " + (hp - l.getHealth()) + " health to a 200 blow");
            List<PhylacteryEntity> vs = lv.getEntitiesOfClass(PhylacteryEntity.class, new AABB(ORIGIN).inflate(30));
            if (vs.size() != 3) return Result.fail("found " + vs.size() + " phylactery entities");
            for (PhylacteryEntity p : vs) p.hurt(lv.damageSources().genericKill(), 10000f);
            if (l.shielded()) return Result.fail("the shield must break with the last phylactery");
            for (int i = 0; i < 3; i++) tickBoss(l, 1);
            return Result.ok("Colossus 3 phases + damage/kill/announce events, Lich shield with 3 phylacteries; " + got.damage.size() + " damage events, " + got.announce.size() + " announcements");
        } finally {
            NeoForge.EVENT_BUS.unregister(got);
            for (CalamityBoss b : made) {
                b.killHelpers();
                b.discard();
            }
            for (PhylacteryEntity p : lv.getEntitiesOfClass(PhylacteryEntity.class, new AABB(ORIGIN).inflate(40))) p.discard();
            for (net.minecraft.world.entity.Entity e : lv.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, new AABB(ORIGIN).inflate(40), x -> x instanceof net.minecraft.world.entity.item.ItemEntity || x instanceof GaiaSpikeEntity || x instanceof net.minecraft.world.entity.Mob m && MobTouchHandler.isHelper(m))) e.discard();
            CalamityManager.clearAll(server);
            clearArea(lv);
        }
    }

    // ================================================================ 7. the spike and the bolt, the omen -> rise flow

    private static Result entities(MinecraftServer server) {
        ServerLevel lv = server.overworld();
        String why = skip(lv);
        if (why != null) return Result.ok("skipped: " + why);
        List<CalamityBoss> bosses = new ArrayList<>();
        Catch got = new Catch();
        NeoForge.EVENT_BUS.register(got);
        try {
            chunk(lv, ORIGIN);
            platform(lv);
            // the spike: warning, then it strikes a zombie standing on it, then it is gone
            Zombie victim = EntityType.ZOMBIE.create(lv);
            if (victim == null) return Result.fail("no zombie");
            victim.moveTo(ORIGIN.getX() + 0.5, ORIGIN.getY(), ORIGIN.getZ() + 0.5, 0f, 0f);
            victim.setNoAi(true);
            lv.addFreshEntity(victim);
            GaiaSpikeEntity s = GaiaSpikeEntity.raise(lv, new Vec3(ORIGIN.getX() + 0.5, ORIGIN.getY(), ORIGIN.getZ() + 0.5), null, 3, 4f);
            float hp = victim.getHealth();
            for (int i = 0; i < 6; i++) s.tick();
            if (victim.getHealth() >= hp) return Result.fail("the Gaia spike did not hurt a zombie standing on it");
            for (int i = 0; i < GaiaSpikeEntity.UP_TICKS + 4; i++) s.tick();
            if (!s.isRemoved()) return Result.fail("the Gaia spike must remove itself");
            victim.discard();
            // the bolt flies and ends
            VoidBoltProjectile b = VoidBoltProjectile.launch(lv, null, new Vec3(ORIGIN.getX() + 0.5, ORIGIN.getY() + 3, ORIGIN.getZ() + 0.5), new Vec3(1, 0, 0), 1.0f, 5f, 0);
            for (int i = 0; i < VoidBoltProjectile.LIFE_TICKS + 5 && !b.isRemoved(); i++) b.tick();
            if (!b.isRemoved()) return Result.fail("a void bolt must end");
            // the omen and the rise
            CalamityDef def = WorldData.calamity(GraveboundColossus.ID);
            if (def == null) return Result.fail("the Colossus's data file is missing");
            CalamityManager.clearAll(server);
            CalamityManager.startAt(lv, new Vec3(ORIGIN.getX() + 0.5, ORIGIN.getY(), ORIGIN.getZ() + 0.5), def, "test", null, 0);
            if (got.announce.stream().noneMatch(x -> x instanceof CalamityEvents.Omen)) return Result.fail("no Omen event");
            if (CalamityData.get(server).omens.size() != 1) return Result.fail("the omen was not stored");
            CalamityManager.tickNow(server);
            List<GraveboundColossus> rose = lv.getEntitiesOfClass(GraveboundColossus.class, new AABB(ORIGIN).inflate(10));
            if (rose.isEmpty()) return Result.fail("the Colossus did not rise after its omen");
            bosses.addAll(rose);
            for (CalamityBoss c : rose) tickBoss(c, 2);
            if (CalamityData.get(server).active.isEmpty()) return Result.fail("the risen Colossus is not in the list of living calamities");
            if (got.announce.stream().noneMatch(x -> x instanceof CalamityEvents.Rise)) return Result.fail("no Rise event after the omen");
            return Result.ok("spike warned, struck and vanished; bolt ended; omen -> rise -> listed as alive");
        } finally {
            NeoForge.EVENT_BUS.unregister(got);
            for (CalamityBoss c : bosses) {
                c.killHelpers();
                c.discard();
            }
            for (net.minecraft.world.entity.Entity e : lv.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, new AABB(ORIGIN).inflate(40), x -> x instanceof GaiaSpikeEntity || x instanceof Zombie)) e.discard();
            CalamityManager.clearAll(server);
            clearArea(lv);
        }
    }
}
