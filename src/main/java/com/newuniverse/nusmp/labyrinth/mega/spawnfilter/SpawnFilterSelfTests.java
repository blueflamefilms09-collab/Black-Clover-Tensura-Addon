package com.newuniverse.nusmp.labyrinth.mega.spawnfilter;

import com.newuniverse.nusmp.dungeon.wild.Wildlife;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.eco.FactionRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

import java.util.List;
import java.util.Optional;

/**
 * Why: the spawn filter cannot be watched by its author, so it has SELF-TESTS the CI server smoke test runs in the real game
 * ({@code /multiverse mega selftest dungeon.guard_exempt.<name>}), each driving the very handlers the event bus calls with constructed events.
 * {@code gorgon}: the shipped blacklist names the Gorgon, the tags no longer hold it, and (when Ice and Fire is installed) a real Gorgon is refused by both
 * the spawn event and the join net in the Mega-Dungeon, loaded from a save or fresh, while a cow is let through. {@code blacklist}: the parser's forms and
 * the ban-versus-operator rule. {@code theme}: the palettes made from the live faction files keep drowned things out of a dry crypt and let them into a
 * flooded temple, and in a real sector a creature of a faction the floor does not like is refused while an operator's and a saved one are not. Every
 * entity a test creates is discarded.
 */
public final class SpawnFilterSelfTests {
    private SpawnFilterSelfTests() {}

    public static void register() {
        SelfTests.register("dungeon.guard_exempt.gorgon", SpawnFilterSelfTests::gorgon);
        SelfTests.register("dungeon.guard_exempt.blacklist", SpawnFilterSelfTests::blacklist);
        SelfTests.register("dungeon.guard_exempt.theme", SpawnFilterSelfTests::theme);
    }

    private static SelfTests.Result fail(String s) { return SelfTests.Result.fail(s); }

    private static SelfTests.Result ok(String s) { return SelfTests.Result.ok(s); }

    private static TagKey<EntityType<?>> tag(String path) { return TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("nusmp", path)); }

    // ================================================================ gorgon
    private static SelfTests.Result gorgon(MinecraftServer server) {
        ServerLevel mega = SelfTests.mega(server);
        if (mega == null) return fail("the Mega-Dungeon dimension is not loaded");
        if (!SpawnBlacklist.parse(SpawnBlacklist.DEFAULT).matches("iceandfire:gorgon")) return fail("the shipped default list does not name the Gorgon");
        if (!SpawnFilterEvents.blacklist().matches("iceandfire:gorgon")) return fail("the live spawn.blacklist does not name iceandfire:gorgon (edited in this world's config?)");
        Optional<EntityType<?>> g = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.fromNamespaceAndPath("iceandfire", "gorgon"));
        EntityType<?> cowType = EntityType.COW;
        Entity cow = cowType.create(mega);
        Entity gorgon = null;
        try {
            if (cow == null) return fail("could not create a cow for the control");
            cow.setPos(mega.getSharedSpawnPos().getX() + 0.5, mega.getSharedSpawnPos().getY(), mega.getSharedSpawnPos().getZ() + 0.5);
            if (SpawnFilterEvents.verdictFor(cow, true, false) == SpawnFilterRules.Verdict.DENY_BANNED) return fail("a cow is banned");
            EntityJoinLevelEvent cowJoin = new EntityJoinLevelEvent(cow, mega);
            SpawnFilterEvents.onEntityJoin(cowJoin);
            if (cowJoin.isCanceled()) return fail("the join net refused a cow");
            if (g.isEmpty()) return ok("Ice and Fire is not installed here, so there is no Gorgon to spawn; the live blacklist names iceandfire:gorgon, a cow passes the join net");
            EntityType<?> t = g.get();
            if (!t.is(SpawnFilterEvents.BANNED)) return fail("the Gorgon is not in the nusmp:dungeon/banned tag");
            if (t.is(Wildlife.TIER3) || t.is(Wildlife.TIER2) || t.is(Wildlife.TIER1) || t.is(Wildlife.LEGENDARY)) return fail("the Gorgon is still in a dungeon tier tag");
            if (t.is(tag("mega/beast"))) return fail("the Gorgon is still in the mega beast faction tag");
            gorgon = t.create(mega);
            if (gorgon == null) return fail("could not create a Gorgon");
            gorgon.setPos(cow.getX(), cow.getY(), cow.getZ());
            if (SpawnFilterEvents.verdictFor(gorgon, true, false) != SpawnFilterRules.Verdict.DENY_BANNED) return fail("a fresh Gorgon is not refused");
            if (SpawnFilterEvents.verdictFor(gorgon, false, false) != SpawnFilterRules.Verdict.DENY_BANNED) return fail("a Gorgon loaded from a save is not refused (old worlds would keep theirs)");
            EntityJoinLevelEvent join = new EntityJoinLevelEvent(gorgon, mega);
            SpawnFilterEvents.onEntityJoin(join);
            if (!join.isCanceled()) return fail("the join net let a Gorgon into the Mega-Dungeon");
            if (gorgon instanceof Mob m) {
                BlockPos at = BlockPos.containing(m.position());
                FinalizeSpawnEvent natural = new FinalizeSpawnEvent(m, mega, m.getX(), m.getY(), m.getZ(), mega.getCurrentDifficultyAt(at), MobSpawnType.NATURAL, null, null);
                SpawnFilterEvents.onFinalizeSpawn(natural);
                if (!natural.isCanceled() || !natural.isSpawnCancelled()) return fail("the spawn event let a natural Gorgon through");
                FinalizeSpawnEvent summoned = new FinalizeSpawnEvent(m, mega, m.getX(), m.getY(), m.getZ(), mega.getCurrentDifficultyAt(at), MobSpawnType.COMMAND, null, null);
                SpawnFilterEvents.onFinalizeSpawn(summoned);
                if (!summoned.isCanceled()) return fail("an operator's /summon brought in a Gorgon although blacklistAllowsAdmin is off (never means never)");
            }
            return ok("a Gorgon (fresh, loaded from a save, natural, or /summoned) is refused by the join net and the spawn event in the Mega-Dungeon; it is in no dungeon tier and the banned tag holds it; a cow passes");
        } finally {
            if (cow != null) cow.discard();
            if (gorgon != null) gorgon.discard();
        }
    }

    // ================================================================ blacklist
    private static SelfTests.Result blacklist(MinecraftServer server) {
        SpawnBlacklist b = SpawnBlacklist.parse(List.of(" IceAndFire:Gorgon ", "tensura:*", "lich", "", "# note", "bad entry", 5));
        if (!b.matches("iceandfire:gorgon") || !b.matches("tensura:orc") || !b.matches("somemod:lich")) return fail("the list forms (exact, namespace wildcard, bare path) did not match");
        if (b.matches("iceandfire:fire_dragon") || b.matches("minecraft:zombie") || b.matches(null)) return fail("the list matched something it should not");
        if (SpawnFilterRules.judge(true, true, false, false, true, true, null, null) != SpawnFilterRules.Verdict.DENY_BANNED) return fail("an operator got past the ban with the bypass off");
        if (SpawnFilterRules.judge(true, true, true, false, true, true, null, null) != SpawnFilterRules.Verdict.ALLOW) return fail("the bypass switch does nothing");
        if (SpawnFilterRules.judge(true, false, true, false, true, true, null, null) != SpawnFilterRules.Verdict.DENY_BANNED) return fail("the bypass let a non-operator through");
        return ok("exact, 'mod:*' and bare-path entries match; blanks, comments and junk are skipped; the ban holds against an operator unless the bypass is on");
    }

    // ================================================================ theme
    private static SelfTests.Result theme(MinecraftServer server) {
        ServerLevel mega = SelfTests.mega(server);
        if (mega == null) return fail("the Mega-Dungeon dimension is not loaded");
        if (FactionRegistry.graph().ids().isEmpty()) return fail("no faction file is loaded");
        FloorThemeMobFilter.Palette dry = SpawnFilterEvents.paletteFor("necropolis"), wet = SpawnFilterEvents.paletteFor("sunken_temple");
        if (dry.open() || wet.open()) return fail("a shipped theme has no palette (the faction files carry no weights?)");
        if (dry.allows("aquatic")) return fail("drowned things are native to the Necropolis");
        if (!dry.allows("undead")) return fail("the undead are not native to the Necropolis");
        if (!wet.allows("aquatic")) return fail("drowned things are not native to the Sunken Temple");
        if (!SpawnFilterEvents.paletteFor("no_such_theme").open()) return fail("an unknown theme filters something");
        String guardian = FactionRegistry.factionOf(EntityType.GUARDIAN);
        if (!"aquatic".equals(guardian)) return fail("a guardian is not aquatic in the live tags (" + guardian + "), the live check below would be meaningless");
        String detail = "necropolis keeps out the aquatic and keeps the undead, sunken_temple takes the aquatic, an unknown theme is open";
        Sector s = null;
        for (Sector c : MegaData.get(server).sectors()) if (c.theme != null && c.region.getXSpan() > 1 && c.state != Sector.State.FAILED) { s = c; break; }
        if (s == null) return ok(detail + "; no sector is generated yet, so the live check inside a sector was skipped");
        Entity ent = EntityType.GUARDIAN.create(mega);
        try {
            if (ent == null) return fail("could not create a guardian");
            BlockPos mid = new BlockPos(s.region.getCenter());
            ent.setPos(mid.getX() + 0.5, mid.getY(), mid.getZ() + 0.5);
            boolean home = SpawnFilterEvents.paletteFor(s.theme.id()).allows("aquatic");
            SpawnFilterRules.Verdict v = SpawnFilterEvents.verdictFor(ent, true, false);
            boolean themeOn = SpawnFilterConfig.get(SpawnFilterConfig.THEME_FILTER);
            SpawnFilterRules.Verdict want = themeOn && !home ? SpawnFilterRules.Verdict.DENY_THEME : SpawnFilterRules.Verdict.ALLOW;
            if (v != want) return fail("in sector " + s.id + " (" + s.theme.id() + ") a fresh guardian got " + v + ", expected " + want);
            if (SpawnFilterEvents.verdictFor(ent, true, true) != SpawnFilterRules.Verdict.ALLOW) return fail("an operator's guardian was filtered");
            if (SpawnFilterEvents.verdictFor(ent, false, false) != SpawnFilterRules.Verdict.ALLOW) return fail("a guardian loaded from a save was filtered");
            return ok(detail + "; in sector " + s.id + " (" + s.theme.id() + ") a fresh guardian is " + v + ", an operator's and a saved one are allowed");
        } finally {
            if (ent != null) ent.discard();
        }
    }
}
