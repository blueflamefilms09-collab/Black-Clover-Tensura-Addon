package com.newuniverse.nusmp.labyrinth.mega.hub;

import com.newuniverse.nusmp.labyrinth.gate.GateRegistry;
import com.newuniverse.nusmp.labyrinth.mega.HubLayout;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: nothing in this package can be played by its author, so each rule of Floor 0 has a SELF-TEST that the CI server smoke test runs in the real game
 * ({@code /multiverse mega selftest}), and each fails loudly with the reason. {@code hub.dimension}: the Sanctuary level exists, the chunk at the arrival
 * cell generates, the block under the arrival cell is solid and the three cells above it are free. {@code hub.plaza}: {@code ensurePlaza} leaves the home
 * gate plane in place and the arrival column clear. {@code hub.no_hostile}: the spawn filter cancels a zombie's natural spawn and lets a cow, a
 * command-spawned zombie and an owned zombie through, and the sweep removes a stray zombie but not a named one or one an admin asked for.
 * {@code hub.no_structures}: a 5 x 5 chunk square round the plaza has no structure start (the third layer of "no foreign structures"). {@code hub.gates}: the
 * three gate centres classify as themselves, a position near no gate classifies as none and {@code useGate} on it leaves the player where they are, and the
 * protected volumes answer as configured. {@code hub.biome}: the live biome (after every mod's biome modifiers ran) lists no monster, only creatures that
 * are not monsters, and only vanilla features. They drive the same public entry points the event handlers use. No test teleports anybody (a fake player
 * has no connection and cannot change dimension); every entity a test creates is discarded whatever happens.
 */
public final class HubSelfTests {
    private HubSelfTests() {}

    private static final String PLAYER = "hub_tester";

    /** Registers the tests; called once from {@code HubRegistry.init}. */
    public static void register() {
        SelfTests.register("hub.dimension", HubSelfTests::dimension);
        SelfTests.register("hub.plaza", HubSelfTests::plaza);
        SelfTests.register("hub.no_hostile", HubSelfTests::noHostile);
        SelfTests.register("hub.no_structures", HubSelfTests::noStructures);
        SelfTests.register("hub.gates", HubSelfTests::gates);
        SelfTests.register("hub.biome", HubSelfTests::biome);
    }

    private static SelfTests.Result ok(String detail) { return SelfTests.Result.ok(detail); }

    private static SelfTests.Result fail(String detail) { return SelfTests.Result.fail(detail); }

    private static String at(BlockPos p) { return p.getX() + " " + p.getY() + " " + p.getZ(); }

    /** Is there nothing solid in the cell (air, a flower, tall grass) and no fluid? */
    private static boolean free(ServerLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return s.getCollisionShape(level, pos).isEmpty() && s.getFluidState().isEmpty();
    }

    // ------------------------------------------------------------------------------------------------ the dimension
    private static SelfTests.Result dimension(MinecraftServer server) {
        ServerLevel hub = HubTravel.level(server);
        if (hub == null) return fail("no nusmp:sanctuary dimension (data/nusmp/dimension/sanctuary.json did not load)");
        BlockPos spawn = HubTravel.arrivalPos();
        hub.getChunk(spawn.getX() >> 4, spawn.getZ() >> 4);
        BlockPos ground = new BlockPos(spawn.getX(), HubLayout.SURFACE_Y - 1, spawn.getZ());
        BlockState g = hub.getBlockState(ground);
        if (g.getCollisionShape(hub, ground).isEmpty()) return fail("the block under the arrival cell " + at(ground) + " is not solid: " + g.getBlock().getDescriptionId());
        for (int k = 0; k < 3; k++) {
            BlockPos cell = new BlockPos(spawn.getX(), HubLayout.SURFACE_Y + k, spawn.getZ());
            if (!free(hub, cell)) return fail("the arrival column is blocked at " + at(cell) + " by " + hub.getBlockState(cell).getBlock().getDescriptionId());
        }
        return ok("arrival cell " + at(spawn) + ": ground " + g.getBlock().getDescriptionId() + ", three free cells above, chunk generated");
    }

    // ------------------------------------------------------------------------------------------------ the plaza
    private static SelfTests.Result plaza(MinecraftServer server) {
        ServerLevel hub = HubTravel.level(server);
        if (hub == null) return fail("no nusmp:sanctuary dimension");
        if (!HubTravel.ensurePlaza(hub)) return fail("ensurePlaza could not guarantee the home gate (see the log)");
        List<String> missing = new ArrayList<>();
        for (int x = HubLayout.HOME_GATE_X - HubPlazaLayout.GATE_HALF_WIDTH; x <= HubLayout.HOME_GATE_X + HubPlazaLayout.GATE_HALF_WIDTH; x++) {
            for (int y = HubPlazaLayout.gateBottomY(); y <= HubPlazaLayout.gateTopY(); y++) {
                BlockPos p = new BlockPos(x, y, HubLayout.HOME_GATE_Z);
                if (!hub.getBlockState(p).is(GateRegistry.LABYRINTH_GATE.get())) missing.add(at(p));
            }
        }
        if (!missing.isEmpty()) return fail("the home gate plane lacks the gate block at " + missing);
        BlockPos centre = new BlockPos(HubLayout.HOME_GATE_X, HubLayout.HOME_GATE_Y, HubLayout.HOME_GATE_Z);
        if (!hub.getBlockState(centre).is(GateRegistry.LABYRINTH_GATE.get())) return fail("no gate block at HOME_GATE " + at(centre));
        BlockPos spawn = HubTravel.arrivalPos();
        for (int k = 0; k < 3; k++) {
            BlockPos cell = new BlockPos(spawn.getX(), HubLayout.SURFACE_Y + k, spawn.getZ());
            if (!free(hub, cell)) return fail("the arrival column is blocked at " + at(cell) + " by " + hub.getBlockState(cell).getBlock().getDescriptionId());
        }
        HubData data = HubData.get(hub);
        if (!data.built()) return fail("the plaza record says the plaza was never stamped");
        return ok("home gate plane in place at " + at(centre) + ", plaza record: build version " + data.plazaVersion + (data.fallback ? " (fallback)" : " (template)"));
    }

    // ------------------------------------------------------------------------------------------------ passive mobs only
    private static SelfTests.Result noHostile(MinecraftServer server) {
        ServerLevel hub = HubTravel.level(server);
        if (hub == null) return fail("no nusmp:sanctuary dimension");
        if (!HubConfig.get(HubConfig.PASSIVE_ONLY)) return ok("skipped: hub.passiveOnly is false in the config");
        BlockPos spawn = HubTravel.arrivalPos();
        hub.getChunk(spawn.getX() >> 4, spawn.getZ() >> 4);
        List<Mob> made = new ArrayList<>();
        try {
            Zombie zombie = EntityType.ZOMBIE.create(hub);
            Mob cow = EntityType.COW.create(hub);
            if (zombie == null || cow == null) return fail("could not create a zombie and a cow");
            made.add(zombie);
            made.add(cow);
            if (!HubRules.isHostile(zombie) || HubRules.isHostile(cow)) return fail("isHostile: zombie " + HubRules.isHostile(zombie) + ", cow " + HubRules.isHostile(cow));
            if (!HubRules.cancelsSpawn(MobSpawnType.NATURAL, true, false)) return fail("a natural hostile spawn is not cancelled");
            if (HubRules.cancelsSpawn(MobSpawnType.NATURAL, false, false)) return fail("a natural passive spawn is cancelled");
            if (HubRules.cancelsSpawn(MobSpawnType.COMMAND, true, false) || HubRules.cancelsSpawn(MobSpawnType.SPAWN_EGG, true, false)) return fail("an admin's spawn is cancelled");
            if (HubRules.cancelsSpawn(MobSpawnType.NATURAL, true, true)) return fail("an owned hostile mob's spawn is cancelled");
            // the real handler, driven with constructed events (not posted: the other mods' listeners are none of this test's business)
            FinalizeSpawnEvent natural = spawnEvent(hub, zombie, MobSpawnType.NATURAL, spawn);
            HubRules.onFinalizeSpawn(natural);
            if (!natural.isSpawnCancelled()) return fail("the handler did not cancel a natural zombie spawn");
            FinalizeSpawnEvent passive = spawnEvent(hub, cow, MobSpawnType.NATURAL, spawn);
            HubRules.onFinalizeSpawn(passive);
            if (passive.isSpawnCancelled()) return fail("the handler cancelled a cow");
            Zombie commanded = EntityType.ZOMBIE.create(hub);
            made.add(commanded);
            FinalizeSpawnEvent byCommand = spawnEvent(hub, commanded, MobSpawnType.COMMAND, spawn);
            HubRules.onFinalizeSpawn(byCommand);
            if (byCommand.isSpawnCancelled()) return fail("the handler cancelled a command-spawned zombie");
            if (!commanded.getPersistentData().getBoolean(HubKeys.ALLOWED)) return fail("the handler did not mark the command-spawned zombie as allowed");
            // the sweep: a stray zombie goes, a named one, an admin's and an owned one stay
            Zombie stray = place(hub, EntityType.ZOMBIE, spawn.above(8), made);
            Zombie named = place(hub, EntityType.ZOMBIE, spawn.above(8).east(3), made);
            named.setCustomName(net.minecraft.network.chat.Component.literal("hub test: named"));
            Zombie admin = place(hub, EntityType.ZOMBIE, spawn.above(8).east(6), made);
            admin.getPersistentData().putBoolean(HubKeys.ALLOWED, true);
            Zombie servant = place(hub, EntityType.ZOMBIE, spawn.above(8).east(9), made);
            servant.addTag("nusmp_servant");
            Mob calf = place(hub, EntityType.COW, spawn.above(8).east(12), made);
            int removed = HubRules.sweepAround(hub, spawn.getX(), spawn.getZ());
            if (!stray.isRemoved()) return fail("the sweep left a stray zombie alive (removed " + removed + ")");
            if (named.isRemoved() || admin.isRemoved() || servant.isRemoved() || calf.isRemoved()) {
                return fail("the sweep removed something it must keep: named " + named.isRemoved() + ", admin's " + admin.isRemoved() + ", servant " + servant.isRemoved() + ", cow " + calf.isRemoved());
            }
            return ok("zombie cancelled, cow, command and owned spawns pass; sweep removed " + removed + " stray and kept the named, admin's, servant and cow");
        } finally {
            for (Mob m : made) m.discard();
        }
    }

    private static FinalizeSpawnEvent spawnEvent(ServerLevel hub, Mob mob, MobSpawnType type, BlockPos at) {
        return new FinalizeSpawnEvent(mob, hub, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, hub.getCurrentDifficultyAt(at), type, null, null);
    }

    private static <T extends Mob> T place(ServerLevel hub, EntityType<T> type, BlockPos at, List<Mob> made) {
        T m = type.create(hub);
        if (m == null) throw new IllegalStateException("could not create " + type);
        m.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        m.setNoGravity(true);
        m.setNoAi(true);
        made.add(m);
        hub.addFreshEntity(m);
        return m;
    }

    // ------------------------------------------------------------------------------------------------ no foreign structures
    private static SelfTests.Result noStructures(MinecraftServer server) {
        ServerLevel hub = HubTravel.level(server);
        if (hub == null) return fail("no nusmp:sanctuary dimension");
        int cx0 = HubLayout.PLAZA_X >> 4, cz0 = HubLayout.PLAZA_Z >> 4, chunks = 0;
        List<String> bad = new ArrayList<>();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                ChunkAccess chunk = hub.getChunk(cx0 + dx, cz0 + dz);
                chunks++;
                if (!chunk.getAllStarts().isEmpty()) bad.add(chunk.getPos() + " has " + chunk.getAllStarts().size());
            }
        }
        return bad.isEmpty() ? ok(chunks + " chunks round the plaza, no structure start in any") : fail("structure starts in the Sanctuary: " + bad);
    }

    // ------------------------------------------------------------------------------------------------ the gates
    private static SelfTests.Result gates(MinecraftServer server) {
        if (HubLayout.gateNear(HubLayout.HOME_GATE_X, HubLayout.HOME_GATE_Y, HubLayout.HOME_GATE_Z) != HubLayout.GateKind.HOME) return fail("the home gate centre is not HOME");
        if (HubLayout.gateNear(HubLayout.HOLY_GATE_X, HubLayout.HOLY_GATE_Y, HubLayout.HOLY_GATE_Z) != HubLayout.GateKind.HOLY) return fail("the Holy gate centre is not HOLY");
        if (HubLayout.gateNear(HubLayout.ABYSS_GATE_X, HubLayout.ABYSS_GATE_Y, HubLayout.ABYSS_GATE_Z) != HubLayout.GateKind.ABYSSAL) return fail("the Abyssal gate centre is not ABYSSAL");
        BlockPos open = new BlockPos(HubLayout.PLAZA_X + 30, HubLayout.SURFACE_Y, HubLayout.PLAZA_Z + 30);
        if (HubLayout.gateNear(open.getX(), open.getY(), open.getZ()) != null) return fail("open ground " + at(open) + " classifies as a gate");
        boolean build = HubConfig.get(HubConfig.BUILD_ENABLED), protect = HubConfig.get(HubConfig.PROTECT_PLAZA);
        BlockPos home = new BlockPos(HubLayout.HOME_GATE_X, HubLayout.HOME_GATE_Y, HubLayout.HOME_GATE_Z);
        if (HubRules.refuses(home) != (!build || protect)) return fail("the home gate's protection does not follow the config (build " + build + ", protect " + protect + ")");
        if (HubRules.refuses(open) != !build) return fail("open ground's protection does not follow hub.buildEnabled");
        ServerLevel hub = HubTravel.level(server);
        if (hub == null) return fail("no nusmp:sanctuary dimension");
        FakePlayer p = SelfTests.player(hub, PLAYER, HubLayout.SPAWN_X + 0.5, HubLayout.SPAWN_Y, HubLayout.SPAWN_Z + 0.5);
        net.minecraft.world.level.Level before = p.level();
        boolean left = HubTravel.useGate(p, open);
        if (left) return fail("useGate took the player through a gate at open ground");
        if (p.level() != before) return fail("useGate moved the player although no gate is near");
        if (!p.getPersistentData().contains(HubKeys.GATE_USED)) return fail("useGate did not stamp the gate cooldown");
        return ok("home, Holy and Abyssal centres classify, open ground does not, protection follows the config, useGate at open ground leaves the player");
    }

    // ------------------------------------------------------------------------------------------------ the live biome
    private static SelfTests.Result biome(MinecraftServer server) {
        ServerLevel hub = HubTravel.level(server);
        if (hub == null) return fail("no nusmp:sanctuary dimension");
        BlockPos spawn = HubTravel.arrivalPos();
        Holder<Biome> holder = hub.getBiome(spawn);
        ResourceLocation want = ResourceLocation.fromNamespaceAndPath("nusmp", "sanctuary_plains");
        if (!holder.is(want)) return fail("the biome at the arrival cell is " + holder.unwrapKey().map(k -> k.location().toString()).orElse("?") + ", not " + want);
        Biome biome = holder.value();
        List<String> problems = new ArrayList<>();
        int creatures = 0;
        for (MobCategory category : MobCategory.values()) {
            for (var data : biome.getMobSettings().getMobs(category).unwrap()) {
                if (category != MobCategory.CREATURE) problems.add(category.getName() + " has " + data.type.getDescriptionId());
                else if (data.type.getCategory() == MobCategory.MONSTER) problems.add("the creature list has the monster " + data.type.getDescriptionId());
                else creatures++;
            }
        }
        if (creatures == 0) problems.add("no creature may spawn: the plains would be empty");
        int features = 0;
        for (HolderSet<PlacedFeature> step : biome.getGenerationSettings().features()) {
            for (Holder<PlacedFeature> f : step) {
                features++;
                String ns = f.unwrapKey().map(k -> k.location().getNamespace()).orElse("(inline)");
                if (!ns.equals("minecraft")) problems.add("a feature from " + ns + ": " + f.unwrapKey().map(k -> k.location().toString()).orElse("?"));
            }
        }
        if (features == 0) problems.add("the biome has no features at all: no grass, flowers or trees");
        return problems.isEmpty() ? ok(creatures + " creature types, no other spawner, " + features + " vanilla features") : fail(String.join("; ", problems));
    }
}
