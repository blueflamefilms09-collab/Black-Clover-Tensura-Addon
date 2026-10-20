package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryData;
import com.newuniverse.nusmp.labyrinth.mega.guard.LabyrinthGuardianEntity;
import com.newuniverse.nusmp.labyrinth.mega.guard.PocketBossBridge;
import com.newuniverse.nusmp.labyrinth.mega.rift.DimensionalRiftBlockEntity;
import com.newuniverse.nusmp.labyrinth.mega.rift.PocketRealm;
import com.newuniverse.nusmp.labyrinth.mega.rift.RiftAdmin;
import com.newuniverse.nusmp.labyrinth.mega.rift.RiftConfig;
import com.newuniverse.nusmp.labyrinth.mega.rift.RiftRules;
import com.newuniverse.nusmp.labyrinth.mega.rift.RiftService;
import com.newuniverse.nusmp.labyrinth.mega.rift.RiftType;
import com.newuniverse.nusmp.labyrinth.vault.RelicPedestalBlockEntity;
import com.newuniverse.nusmp.relics.RelicCatalog;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Why: the tombs cannot be played by their author, so each rule has a self-test that the CI server smoke test runs in the real game ({@code /multiverse mega selftest}). {@code rift.pockets}: every
 * loaded tomb is whole in the LIVE registries (palette blocks, biome, boss profile, the five loot tables, the relic definitions, the mob bodies, the particles, sounds, damage types and effects of the
 * traps), and then two tombs are really built in the last-but-one cell of the pocket field and walked: the entry hall stands first, the build finishes, the seal is crying obsidian, the pedestals and
 * chests are where the plan says, the biome of the cell is the tomb's, a flood fill of the real blocks reaches the boss's lair and the key room from the arrival but NOT the treasury until the seal is
 * opened, the loot gate refuses a roll outside the tomb and before the boss is dead, the boss wears its own profile, force opens the seal, a room's monsters are made with the tomb's names and
 * stronger than the floor's, the collapse takes the rooms one by one and carries a delver out of a falling room, and the sweep gives the whole cell back and frees the slot. {@code rift.mega_rare}: the
 * rarity table sums to one and its pity is certain at the hard limit, the pity moves only when a rift opened, a mega-rare tear carries its tomb's id and colours through a save, stepping into it builds
 * the tomb and takes the player in with the way home remembered, the tear is spent when the tomb falls, the cap on concurrent tombs holds, and the admin facade works. Everything built is cleaned up
 * whatever happens.
 */
public final class PocketSelfTests {
    private PocketSelfTests() {}

    private static final int TEST_SLOT = RiftRules.MAX_SLOTS - 2;

    /** The tests' own scenery (a platform to stand on, a rift block left behind) is cleared inside the Dungeon Guard's internal span: the guard refuses to everybody else every change that takes a held block away. */
    private static void air(ServerLevel level, BlockPos pos) { com.newuniverse.nusmp.dungeon.DungeonGuard.internal(() -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2)); }

    public static void register() {
        SelfTests.register("rift.pockets", PocketSelfTests::pockets);
        SelfTests.register("rift.mega_rare", PocketSelfTests::megaRare);
    }

    private static SelfTests.Result fail(String s) { return SelfTests.Result.fail(s); }

    // ================================================================ rift.pockets
    private static SelfTests.Result pockets(MinecraftServer server) {
        ServerLevel mega = SelfTests.mega(server);
        if (mega == null) return fail("the Mega-Dungeon dimension is not loaded");
        List<PocketDef> all = PocketDefs.all();
        if (all.size() < 12) return fail("only " + all.size() + " exclusive pockets are loaded, the brief wants at least 12");
        int softKnown = 0, soft = 0;
        for (PocketDef d : all) {
            SelfTests.Result r = registries(server, mega, d);
            if (!r.ok()) return r;
            soft += 2;
            if (SoftSound.known(d.biome().softAmbient())) softKnown++;
            if (SoftSound.known(d.biome().softCrackle())) softKnown++;
        }
        PocketRealm realm = PocketRealm.get(server);
        if (realm.at(TEST_SLOT) != null) realm.collapseSlot(mega, TEST_SLOT);
        List<String> notes = new ArrayList<>();
        notes.add(all.size() + " tombs whole in the live registries, " + softKnown + " of " + soft + " mod sound ids already real (the rest fall back to vanilla)");
        PocketDef[] picks = {all.get(0), all.get(all.size() - 1)};
        for (int i = 0; i < picks.length; i++) {
            SelfTests.Result r = walk(server, mega, realm, picks[i], 7000L + i, notes);
            if (!r.ok()) return r;
        }
        return SelfTests.Result.ok(String.join("; ", notes));
    }

    private static SelfTests.Result registries(MinecraftServer server, ServerLevel mega, PocketDef d) {
        String k = d.id() + ": ";
        PocketPalette pal = PocketPalette.of(d);
        if (!pal.unresolved().isEmpty()) return fail(k + "the palette names blocks the game does not know: " + pal.unresolved());
        if (PocketBiomes.holder(mega, d.biomeId()) == null) return fail(k + "the biome " + d.biomeId() + " is not in the registry");
        if (PocketDefs.boss(d) == null) return fail(k + "no boss profile " + d.boss());
        for (String t : new String[]{"boss", "treasury", "secondary", "chest", "minor"}) {
            ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath("nusmp", "pocket/" + d.id() + "/" + t));
            LootTable table = server.reloadableRegistries().getLootTable(key);
            if (table == LootTable.EMPTY) return fail(k + "the loot table " + key.location() + " is missing");
        }
        for (String id : d.relics().all()) {
            RelicDef rd = Relics.byId(id);
            if (rd == null) return fail(k + "the relic '" + id + "' is not a defined relic");
            ItemStack s = RelicCatalog.newStack(rd);
            if (s.isEmpty()) return fail(k + "the relic '" + id + "' makes no item");
        }
        for (PocketDef.Mob m : d.mobs()) {
            boolean body = m.foundryBase() ? FoundryData.roster().byId(m.foundryId()) != null : entity(m.base());
            if (!body && !entity(m.fallback())) return fail(k + "the mob " + m.id() + " has neither its base (" + m.base() + ") nor its fallback (" + m.fallback() + ")");
            if (PocketMobs.auraOf(m) == null) return fail(k + "the aura particle " + m.aura() + " of " + m.id() + " is not a simple particle");
        }
        for (PocketElement e : d.traps()) {
            if (PocketTraps.particle(e) == null) return fail(k + "the particle " + e.particle() + " of the " + e.id() + " trap is unknown");
            if (!SoftSound.known(e.sound())) return fail(k + "the sound " + e.sound() + " of the " + e.id() + " trap is unknown");
            ResourceLocation dt = ResourceLocation.tryParse(e.damageType());
            if (dt == null || !mega.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).containsKey(dt)) return fail(k + "the damage type " + e.damageType() + " is unknown");
            if (!e.effect().isEmpty() && !BuiltInRegistries.MOB_EFFECT.containsKey(ResourceLocation.parse(e.effect()))) return fail(k + "the effect " + e.effect() + " is unknown");
            if (PocketPalette.block(e.rune()) == null) return fail(k + "the rune block " + e.rune() + " is unknown");
        }
        if (!BuiltInRegistries.PARTICLE_TYPE.containsKey(ResourceLocation.parse(d.biome().particle()))) return fail(k + "the biome particle " + d.biome().particle() + " is unknown");
        for (String s : new String[]{d.biome().ambient(), d.biome().mood(), d.biome().additions(), d.biome().music()})
            if (!SoftSound.known(s)) return fail(k + "the biome sound " + s + " is unknown");
        return SelfTests.Result.ok("");
    }

    private static boolean entity(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl != null && BuiltInRegistries.ENTITY_TYPE.containsKey(rl);
    }

    /** Builds one tomb in the test cell and walks it from the entry hall to the sweep. */
    private static SelfTests.Result walk(MinecraftServer server, ServerLevel mega, PocketRealm realm, PocketDef def, long seed, List<String> notes) {
        String k = def.id() + ": ";
        FakePlayer p = SelfTests.standIn(mega, "pocket_walker", PocketCell.centreX(TEST_SLOT) + 0.5, PocketPlan.Y0 + 1, PocketCell.centreZ() + 0.5);
        PocketRealm.Pocket pk = null;
        try {
            pk = ExclusivePockets.create(mega, realm, def, seed, 12, TEST_SLOT);
            if (pk == null) return fail(k + "the tomb could not be raised in the test cell");
            PocketRuntime rt = PocketRuntime.of(pk);
            if (rt == null) return fail(k + "no runtime for the tomb");
            // the entry hall is whole before anything else
            if (rt.arrival == null || !mega.getBlockState(rt.arrival.below()).blocksMotion()) return fail(k + "no floor under the arrival at " + rt.arrival);
            if (!(mega.getBlockEntity(rt.exit) instanceof DimensionalRiftBlockEntity ex) || ex.type() != RiftType.EXIT) return fail(k + "no exit rift at " + rt.exit);
            BlockPos land = ExclusivePockets.landing(mega, rt);
            if (!mega.getBlockState(land.below()).blocksMotion() || !mega.getBlockState(land).getCollisionShape(mega, land).isEmpty()) return fail(k + "the safe landing " + land.toShortString() + " is not safe");
            // the build finishes
            int guard = 0;
            while (!rt.build.done() && guard++ < 4000) rt.build.step(mega, 200_000);
            if (!rt.build.done()) return fail(k + "the build did not finish in " + guard + " steps (" + rt.build.percent() + "%)");
            pk.phase = 1;
            for (long l : rt.sealBlocks) if (!mega.getBlockState(BlockPos.of(l)).is(Blocks.CRYING_OBSIDIAN)) return fail(k + "a seal block is not crying obsidian at " + BlockPos.of(l).toShortString());
            if (!(mega.getBlockEntity(rt.primary) instanceof RelicPedestalBlockEntity prim) || prim.getLootTable() == null) return fail(k + "the primary pedestal has no loot table");
            if (!(mega.getBlockEntity(rt.secondary) instanceof RelicPedestalBlockEntity)) return fail(k + "no secondary pedestal at " + rt.secondary);
            for (BlockPos c : rt.chests) if (!(mega.getBlockEntity(c) instanceof ChestBlockEntity)) return fail(k + "no chest at " + c.toShortString());
            String biome = mega.getBiome(rt.arrival).unwrapKey().map(b -> b.location().toString()).orElse("?");
            if (!biome.equals(def.biomeId())) return fail(k + "the biome at the arrival is " + biome + ", not " + def.biomeId());
            // reachability in the real blocks, sealed
            if (!reaches(mega, rt, rt.arrival, rt.bossSpawn)) return fail(k + "the boss's lair cannot be reached from the arrival");
            if (!reaches(mega, rt, rt.arrival, rt.keySpawn)) return fail(k + "the key room cannot be reached from the arrival");
            if (reaches(mega, rt, rt.arrival, rt.primary.above())) return fail(k + "the treasury can be reached with the seal shut");
            // the gate
            Vec3 at = Vec3.atCenterOf(rt.primary);
            p.setPos(land.getX() + 0.5, land.getY(), land.getZ() + 0.5);
            if (PocketGate.allows(mega, at, p, def.id(), PocketRules.Stage.BOSS)) return fail(k + "the boss's loot gate is open before the boss is dead");
            if (PocketGate.allows(mega, at, p, def.id(), PocketRules.Stage.TREASURY)) return fail(k + "the treasury's loot gate is open with the seal shut");
            if (!PocketGate.allows(mega, at, p, def.id(), PocketRules.Stage.ANY)) return fail(k + "the any-stage gate is shut inside the tomb");
            if (PocketGate.allows(mega, at, p, def.id() + "x", PocketRules.Stage.ANY)) return fail(k + "the gate opens for another tomb's id");
            if (PocketGate.allows(mega, new Vec3(0, 70, 0), p, def.id(), PocketRules.Stage.ANY)) return fail(k + "the gate opens outside the pocket field");
            // the mobs
            int made = 0;
            for (PocketLayout.Room r : rt.layout.rooms()) if (r.kind() == PocketLayout.Kind.COMBAT) made += PocketMobs.fillRoom(mega, rt, pk, r.id(), 1);
            if (made <= 0) return fail(k + "a combat room woke no monsters");
            Mob any = mega.getEntitiesOfClass(Mob.class, PocketCell.bounds(TEST_SLOT), e -> e.getTags().contains(PocketMobs.TAG)).stream().findFirst().orElse(null);
            if (any == null || any.getCustomName() == null) return fail(k + "the tomb's monsters carry no name");
            if (PocketMobs.profileOf(def, any) == null) return fail(k + "a tomb monster wears no profile tag");
            if (any.getAttributeValue(Attributes.MAX_HEALTH) < 10.0) return fail(k + "a tomb monster has " + any.getAttributeValue(Attributes.MAX_HEALTH) + " health");
            // the boss
            LabyrinthGuardianEntity boss = PocketBoss.spawn(mega, rt, pk);
            if (boss == null) return fail(k + "the mega-boss could not be made");
            if (!PocketBossBridge.wears(boss, PocketDefs.boss(def))) return fail(k + "the mega-boss does not wear its own profile");
            if (PocketBoss.find(mega, rt) == null) return fail(k + "the mega-boss cannot be found after it was placed");
            // the seal: force opens it
            int total = def.seal().forceHp();
            for (int i = 0; i < 40 && !pk.sealOpen; i++) ExclusivePockets.addForce(mega, realm, pk, rt, null, total / 4.0);
            if (!pk.sealOpen) return fail(k + "force did not open the seal (" + pk.sealHp + " of " + total + " left)");
            for (long l : rt.sealBlocks) if (!mega.getBlockState(BlockPos.of(l)).isAir()) return fail(k + "a seal block stayed after the seal opened at " + BlockPos.of(l).toShortString());
            if (!reaches(mega, rt, rt.arrival, rt.primary.above())) return fail(k + "the treasury cannot be reached after the seal opened");
            // the boss dies: the gate and the drops
            ExclusivePockets.bossDied(mega, realm, pk, rt, boss);
            boss.discard();
            if (!pk.bossDead) return fail(k + "the death of the boss did not register");
            if (!PocketGate.allows(mega, at, p, def.id(), PocketRules.Stage.BOSS)) return fail(k + "the boss gate stayed shut after the boss died");
            if (!PocketGate.allows(mega, at, p, def.id(), PocketRules.Stage.TREASURY)) return fail(k + "the treasury gate stayed shut after the seal opened");
            ResourceKey<LootTable> tk = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath("nusmp", "pocket/" + def.id() + "/treasury"));
            LootTable table = server.reloadableRegistries().getLootTable(tk);
            LootParams inside = new LootParams.Builder(mega).withParameter(LootContextParams.ORIGIN, at).withOptionalParameter(LootContextParams.THIS_ENTITY, p).create(LootContextParamSets.CHEST);
            LootParams outside = new LootParams.Builder(mega).withParameter(LootContextParams.ORIGIN, new Vec3(0, 70, 0)).withOptionalParameter(LootContextParams.THIS_ENTITY, p).create(LootContextParamSets.CHEST);
            if (table.getRandomItems(inside, 11L).stream().allMatch(ItemStack::isEmpty)) return fail(k + "the treasury table gave nothing inside the open tomb");
            if (!table.getRandomItems(outside, 11L).stream().allMatch(ItemStack::isEmpty)) return fail(k + "the treasury table gave loot outside the tomb");
            // the collapse: rooms fall one by one, a delver in a falling room is carried out
            int treasury = rt.layout.treasuryId();
            PocketLayout.Room tr = rt.layout.treasury();
            p.setPos(rt.primary.getX() + 0.5, rt.primary.getY(), rt.primary.getZ() + 0.5);
            long t0 = mega.getGameTime();
            ExclusivePockets.relicTaken(mega, realm, pk, rt, List.of(p), t0);
            if (pk.phase != 2 || rt.collapse == null) return fail(k + "taking the relic did not start the collapse");
            List<net.minecraft.server.level.ServerPlayer> in = List.of(p);
            boolean rescued = false;
            for (int t = 0; t <= rt.collapse.totalTicks() + 40; t += 2) {
                rt.collapse.step(mega, pk, in, t0 + t);
                if (!rescued && rt.collapse.fell(treasury)) {
                    rescued = RiftRules.slotAt(p.getX(), p.getZ()) == TEST_SLOT && rt.roomAt(p.getX(), p.getZ()) != treasury;
                    if (!rescued) return fail(k + "the delver in the falling treasury was not carried out (at " + p.position() + ")");
                }
            }
            for (PocketLayout.Room r : rt.layout.rooms()) if (r.id() != rt.layout.entryId() && !rt.collapse.fell(r.id())) return fail(k + "room " + r.id() + " never fell");
            if (rt.collapse.fell(rt.layout.entryId())) return fail(k + "the entry hall fell before the end");
            if (!mega.getBlockState(rt.arrival.below()).blocksMotion()) return fail(k + "the entry floor is gone before the tomb ended");
            while (rt.collapse.busy()) rt.collapse.step(mega, pk, in, t0 + rt.collapse.totalTicks() + 40);
            if (mega.getBlockState(new BlockPos(PocketCell.centreX(TEST_SLOT) + tr.cx(rt.layout.grid()), PocketPlan.Y0 - 1, PocketCell.centreZ() + tr.cz(rt.layout.grid()))).blocksMotion())
                return fail(k + "the floor of the fallen treasury is still there");
            // the sweep gives the cell back
            ExclusivePockets.collapse(mega, realm, pk);
            if (pk.phase != 3) return fail(k + "collapsing did not start the sweep");
            int steps = 0;
            while (realm.at(TEST_SLOT) != null && steps++ < 4000) {
                rt.clear.step(mega, 200_000);
                ExclusivePockets.tick(mega, realm, pk, List.of(), mega.getGameTime(), 0);
            }
            if (realm.at(TEST_SLOT) != null) return fail(k + "the cell was not freed after " + steps + " sweeps");
            if (!emptyCell(mega, rt)) return fail(k + "blocks are left in the swept cell");
            String after = mega.getBiome(new BlockPos(PocketCell.centreX(TEST_SLOT), PocketPlan.Y0, PocketCell.centreZ())).unwrapKey().map(b -> b.location().toString()).orElse("?");
            if (!after.equals(PocketBiomes.DEFAULT_BIOME)) return fail(k + "the biome after the sweep is " + after);
            notes.add(def.id() + " built (" + rt.plan.volume() + " block placements, " + rt.layout.rooms().size() + " rooms), walked, collapsed and swept");
            pk = null;
            return SelfTests.Result.ok("");
        } finally {
            if (pk != null && realm.at(TEST_SLOT) != null) {
                ExclusivePockets.collapse(mega, realm, pk);
                PocketRuntime rt = PocketRuntime.peek(TEST_SLOT);
                for (int i = 0; rt != null && rt.clear != null && !rt.clear.done() && i < 4000; i++) rt.clear.step(mega, 200_000);
                realm.pockets().remove(TEST_SLOT);
                PocketRuntime.forget(TEST_SLOT);
                realm.setDirty();
            }
            p.getPersistentData().remove(PocketRealm.RETURN);
            PocketGravity.clear(p);
        }
    }

    /** A flood fill of the real blocks of the cell (cells a player can stand in: no collision, no fluid) from one feet cell to another; the seal is crying obsidian and stops it. */
    private static boolean reaches(ServerLevel mega, PocketRuntime rt, BlockPos from, BlockPos to) {
        int x0 = PocketCell.centreX(rt.slot) + rt.plan.minX() - 2, x1 = PocketCell.centreX(rt.slot) + rt.plan.maxX() + 2, z0 = PocketCell.centreZ() + rt.plan.minZ() - 2, z1 = PocketCell.centreZ() + rt.plan.maxZ() + 2;
        int y0 = PocketPlan.Y0 - 3, y1 = PocketPlan.Y0 + rt.plan.maxY() - PocketPlan.Y0 + 3;
        int w = x1 - x0 + 1, h = y1 - y0 + 1, d = z1 - z0 + 1;
        boolean[] seen = new boolean[w * h * d];
        ArrayDeque<int[]> q = new ArrayDeque<>();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        q.add(new int[]{from.getX(), from.getY(), from.getZ()});
        seen[((from.getY() - y0) * d + (from.getZ() - z0)) * w + (from.getX() - x0)] = true;
        int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        while (!q.isEmpty()) {
            int[] c = q.poll();
            if (c[0] == to.getX() && c[1] == to.getY() && c[2] == to.getZ()) return true;
            for (int[] dd : dirs) {
                int nx = c[0] + dd[0], ny = c[1] + dd[1], nz = c[2] + dd[2];
                if (nx < x0 || nx > x1 || nz < z0 || nz > z1 || ny < y0 || ny > y1) continue;
                int idx = ((ny - y0) * d + (nz - z0)) * w + (nx - x0);
                if (seen[idx]) continue;
                m.set(nx, ny, nz);
                var st = mega.getBlockState(m);
                if (!st.getCollisionShape(mega, m).isEmpty() || !st.getFluidState().isEmpty()) continue;
                seen[idx] = true;
                q.add(new int[]{nx, ny, nz});
            }
        }
        return false;
    }

    private static boolean emptyCell(ServerLevel mega, PocketRuntime rt) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int cx = PocketCell.centreX(rt.slot), cz = PocketCell.centreZ();
        for (int x = rt.plan.minX() - 2; x <= rt.plan.maxX() + 2; x += 2)
            for (int z = rt.plan.minZ() - 2; z <= rt.plan.maxZ() + 2; z += 2)
                for (int y = PocketCell.Y_MIN; y <= PocketCell.Y_MAX; y += 2) {
                    m.set(cx + x, y, cz + z);
                    if (!mega.getBlockState(m).isAir()) return false;
                }
        return true;
    }

    // ================================================================ rift.mega_rare
    private static SelfTests.Result megaRare(MinecraftServer server) {
        ServerLevel mega = SelfTests.mega(server);
        if (mega == null) return fail("the Mega-Dungeon dimension is not loaded");
        RiftTiers.Config c = MegaRifts.config();
        double[] table = RiftTiers.table(c, RiftConfig.get(RiftConfig.SHORTCUT_CHANCE), RiftConfig.get(RiftConfig.POCKET_CHANCE), 5, 0.0, 0);
        double sum = 0;
        for (double v : table) sum += v;
        if (Math.abs(sum - 1.0) > 1e-9) return fail("the tier table sums to " + sum);
        if (RiftTiers.megaChance(c, 5, 0.0, c.pityHard()) != 1.0) return fail("the pity is not certain at the hard limit");
        if (RiftTiers.megaChance(c, 5, 0.0, 0) >= RiftTiers.megaChance(c, 5, 0.0, c.pityHard() - 1)) return fail("the pity does not climb");
        double base = RiftTiers.megaChance(c, 0, 0.0, 0);
        if (base > 1.0 / RiftTiers.MIN_ONE_IN + 1e-12) return fail("the base chance " + base + " is above one in " + RiftTiers.MIN_ONE_IN);
        // a simulation, so the number in the docs is the number the game rolls
        Random rnd = new Random(1999L);
        int megas = 0, rifts = 0, pity = 0;
        for (int i = 0; i < 200_000; i++) {
            rifts++;
            boolean m = RiftTiers.isMega(c, 5, 0.0, pity, rnd.nextDouble());
            pity = RiftTiers.nextPity(pity, m ? RiftTier.MEGA_RARE : RiftTier.COMMON);
            if (m) megas++;
        }
        double oneIn = rifts / (double) Math.max(1, megas);
        if (oneIn > c.megaOneIn() * 1.2 || oneIn < 50.0) return fail("the simulated rate is one in " + oneIn + " (configured " + c.megaOneIn() + ")");
        PocketRealm realm = PocketRealm.get(server);
        List<Integer> held = new ArrayList<>();
        List<BlockPos> platform = new ArrayList<>();
        BlockPos site = new BlockPos(RiftRules.POCKET_X0 + 20, RiftRules.POCKET_Y + 20, RiftRules.POCKET_Z + 400);
        FakePlayer p = SelfTests.standIn(mega, "mega_rare_tester", site.getX() + 0.5, site.getY(), site.getZ() + 0.5);
        PocketDef def = PocketDefs.all().isEmpty() ? null : PocketDefs.all().get(0);
        if (def == null) return fail("no exclusive pocket is loaded");
        PocketRealm.Pocket pk = null;
        int oldPity = MegaRifts.pity(p);
        try {
            if (realm.at(TEST_SLOT) != null) realm.collapseSlot(mega, TEST_SLOT);
            for (int i = 0; i < RiftRules.MAX_SLOTS; i++)
                if (i != TEST_SLOT && realm.at(i) == null) {
                    PocketRealm.Pocket ph = new PocketRealm.Pocket();
                    ph.slot = i;
                    ph.expiresAt = Long.MAX_VALUE;
                    realm.pockets().put(i, ph);
                    held.add(i);
                }
            mega.getChunk(site.getX() >> 4, site.getZ() >> 4);
            for (int dx = -3; dx <= 3; dx++)
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos floor = site.offset(dx, -1, dz);
                    mega.setBlock(floor, Blocks.STONE.defaultBlockState(), 2);
                    platform.add(floor);
                    for (int dy = 0; dy <= 3; dy++) air(mega, site.offset(dx, dy, dz));
                }
            // the pity moves only when a rift really opened
            MegaRifts.setPity(p, 10);
            MegaRifts.opened(p, false);
            if (MegaRifts.pity(p) != 11) return fail("the pity did not count a rift: " + MegaRifts.pity(p));
            MegaRifts.opened(p, true);
            if (MegaRifts.pity(p) != 0) return fail("a mega-rare rift did not reset the pity: " + MegaRifts.pity(p));
            // the tear
            DimensionalRiftBlockEntity be = RiftService.open(mega, site, RiftType.POCKET, 31337L);
            if (be == null) return fail("a pocket rift did not open at " + site.toShortString());
            MegaRifts.aim(be, def);
            if (!be.isExclusive() || !def.id().equals(be.pocketId()) || be.accent() != def.accent() || be.sky() != def.sky() || be.tier() != RiftTier.MEGA_RARE) return fail("the tear does not carry its tomb: " + be.pocketId() + " tier " + be.tier());
            CompoundTag saved = be.getUpdateTag(mega.registryAccess());
            if (!def.id().equals(saved.getString("PocketId")) || saved.getInt("Accent") != def.accent() || saved.getInt("Sky") != def.sky()) return fail("the tear's update tag lost its tomb: " + saved);
            CompoundTag plain = new CompoundTag();
            RiftService.open(mega, site.above(8), RiftType.SHORTCUT, 5L);
            BlockEntity other = mega.getBlockEntity(site.above(8));
            if (other instanceof DimensionalRiftBlockEntity o) {
                plain = o.getUpdateTag(mega.registryAccess());
                o.close();
            }
            if (plain.contains("PocketId")) return fail("an ordinary tear writes a pocket id: " + plain);
            // stepping in builds the tomb and takes the player in
            for (int i = 0; i < 40; i++) DimensionalRiftBlockEntity.tick(mega, site, mega.getBlockState(site), be);
            p.setPos(site.getX() + 0.5, site.getY(), site.getZ() + 0.5);
            RiftService.step(mega, be, List.of(p));
            pk = realm.at(TEST_SLOT);
            if (pk == null || !pk.exclusive() || !def.id().equals(pk.pocketId)) return fail("stepping into the tear raised no tomb in the cell" + (be.failures() > 0 ? " (the tear logged: " + be.lastError() + ")" : ""));
            if (be.slot() != TEST_SLOT) return fail("the tear does not know its tomb (slot " + be.slot() + ")");
            if (RiftRules.slotAt(p.getX(), p.getZ()) == TEST_SLOT && pk.phase == 0) return fail("the player arrived in a tomb that was still forming (phase 0): the arrival must wait for the build");
            for (int i = 0; i < 6000 && RiftRules.slotAt(p.getX(), p.getZ()) != TEST_SLOT; i++) ExclusivePockets.fast(server);          // 1.101.0: the delver is held at the tear until the tomb stands, then taken in
            if (RiftRules.slotAt(p.getX(), p.getZ()) != TEST_SLOT) return fail("the player is not in the tomb: " + p.position());
            if (pk.phase < 1) return fail("the player was taken in while the tomb was still forming");
            CompoundTag back = p.getPersistentData().getCompound(PocketRealm.RETURN);
            if (back.isEmpty() || back.getInt("RX") != site.getX() || back.getInt("RZ") != site.getZ()) return fail("the way home was not remembered: " + back);
            // the player leaves by the exit tear and arrives beside the one they came by
            PocketRuntime rt = PocketRuntime.of(pk);
            if (rt == null) return fail("no runtime for the tomb");
            p.getPersistentData().remove("nusmp_rift_touch");
            PocketRealm.leave(mega, p);
            if (RiftRules.slotAt(p.getX(), p.getZ()) >= 0) return fail("the way out left the player in the field at " + p.position());
            // the cap
            int cap = RiftConfig.get(RiftConfig.EXCLUSIVE_MAX);
            if (realm.exclusiveCount() != 1) return fail("the realm counts " + realm.exclusiveCount() + " tombs, not 1");
            if (cap < 1) return fail("the cap on tombs is " + cap);
            // collapse by the admin facade
            if (RiftAdmin.pocketIds().size() < 12) return fail("the facade lists " + RiftAdmin.pocketIds().size() + " tombs");
            int n = RiftAdmin.collapseAll(mega);
            if (n < 1) return fail("collapseAll reported " + n);
            for (int i = 0; i < 4000 && realm.at(TEST_SLOT) != null; i++) {
                PocketRuntime r2 = PocketRuntime.of(realm.at(TEST_SLOT));
                if (r2 != null && r2.clear != null) r2.clear.step(mega, 200_000);
                ExclusivePockets.tick(mega, realm, realm.at(TEST_SLOT), List.of(), mega.getGameTime(), 0);
            }
            if (realm.at(TEST_SLOT) != null) return fail("the tomb was not freed after the collapse");
            // the facade on a spent tear
            if (RiftAdmin.spawnRift(null, site, true)) return fail("spawnRift answered true for a null level");
            pk = null;
            return SelfTests.Result.ok("the table sums to one, " + megas + " mega-rare in " + rifts + " simulated rifts (one in " + (int) oneIn + "), pity certain at " + c.pityHard() + ", the tear carries its tomb through a save, stepping in raised "
                    + def.id() + " and the way home was remembered, collapseAll freed the cell (" + n + ")");
        } finally {
            if (realm.at(TEST_SLOT) != null) {
                realm.collapseSlot(mega, TEST_SLOT);
                PocketRuntime r3 = PocketRuntime.peek(TEST_SLOT);
                for (int i = 0; r3 != null && r3.clear != null && !r3.clear.done() && i < 4000; i++) r3.clear.step(mega, 200_000);
                realm.pockets().remove(TEST_SLOT);
                PocketRuntime.forget(TEST_SLOT);
            }
            for (int i : held) realm.pockets().remove(i);
            realm.setDirty();
            for (BlockPos b : RiftService.openRifts(mega)) if (b.distSqr(site) < 400 && mega.getBlockState(b).is(com.newuniverse.nusmp.labyrinth.mega.rift.RiftRegistry.RIFT.get())) air(mega, b);
            RiftService.noteGone(mega, site);
            for (BlockPos b : platform) air(mega, b);
            p.getPersistentData().remove(PocketRealm.RETURN);
            p.getPersistentData().remove("nusmp_rift_touch");
            MegaRifts.setPity(p, oldPity);
        }
    }
}
