package com.newuniverse.nusmp.labyrinth.mega.crucible;

import com.newuniverse.nusmp.labyrinth.mega.gen.BossFloors;
import com.newuniverse.nusmp.dungeon.DungeonEffects;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import com.newuniverse.nusmp.labyrinth.mega.SectorListener;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.crucible.CrucibleRules.Phase;
import com.newuniverse.nusmp.labyrinth.mega.theme.RelicThemeMap;
import com.newuniverse.nusmp.labyrinth.mega.theme.RelicThemes;
import com.newuniverse.nusmp.labyrinth.mega.theme.ThemeConfig;
import com.newuniverse.nusmp.labyrinth.vault.RelicExclusivity;
import com.newuniverse.nusmp.labyrinth.vault.RelicPedestalBlockEntity;
import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicLoot;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.order.UniqueLedger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Why: the Crucible is the rule "exactly one relic per region, whoever clicks and whatever happens", and nothing in a unit test can prove it in the
 * real game: the pedestal block entity, the God of Order's ledger, the data on disk and the catastrophes only meet there. The CI server smoke test runs
 * these self-tests in the booted game ({@link SelfTests}) against the sector 0 that {@code /multiverse mega expand} generated, with a fake player in
 * place of a person, and each one drives the system through the SAME entry points the game calls: the vault ticker's {@code visit} rolls the offer, the
 * pedestal's own {@code take} picks, and the catastrophes advance through the runner's tick with a made-up game clock (a thirty second flood or a ten
 * minute spirit is walked in milliseconds). A fake player is listed by no level and its own position is not real, so the position is handed in and
 * the players the catastrophes would hurt are stood in for by a pig. {@code theme.crucible_flow}: roll, three distinct relics of Unique or above on the
 * pedestals and none of them issued, one pick through the pedestal, exactly one relic issued and given, the other pedestals empty and sealed, exactly one
 * catastrophe started, the vault RESOLVED and the listeners told once at the moment of the pick (that releases the next sector), a second pick refused at
 * every pedestal, the catastrophe walked to its end (resolving nothing a second time), the next region's theme known. {@code theme.single_relic}: in the Mega-Dungeon nothing but a Crucible hands out a relic (the relic roll, a Mimic and the vault loot
 * tables give none), two regions of the same theme never offer the same relic, and each pick adds exactly one relic to the world. {@code
 * theme.catastrophe_undo}: each catastrophe ends by itself, changes no block and no saved state, leaves no spirit behind, resolves nothing when it is only
 * a test, can be stopped half way, and a vault left CHOSEN by a restart is resolved. Everything a test changes is put back (the vault is reset, the
 * listeners restored, the fake player emptied); the relics it issued stay issued, which is the real behaviour of a pick and harmless in a throw-away world.
 */
public final class CrucibleSelfTests {
    private CrucibleSelfTests() {}

    /** Registers the three tests; called once from {@code ThemeRegistry.init}. */
    public static void register() {
        SelfTests.register("theme.crucible_flow", CrucibleSelfTests::flow);
        SelfTests.register("theme.single_relic", CrucibleSelfTests::singleRelic);
        SelfTests.register("theme.catastrophe_undo", CrucibleSelfTests::catastropheUndo);
    }

    // ---------------------------------------------------------------- the harness
    /** A failed assertion; its message is the whole result line. */
    private static final class Failure extends RuntimeException {
        Failure(String message) { super(message, null, false, false); }
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new Failure(message);
    }

    /** Counts the Crucible resolutions the listeners are told about, while the real listeners are set aside. */
    private static final class Probe implements SectorListener {
        final List<Integer> resolved = new ArrayList<>();

        @Override
        public void onCrucibleResolved(ServerLevel level, Sector sector) { resolved.add(sector.id); }
    }

    /**
     * Sector 0's vault, borrowed for one test. Opening it loads the chunks of the vault box, checks that every pedestal marker is a real Relic Pedestal,
     * resets the vault to UNOPENED, sets the other systems' listeners aside (a synthetic resolution must not wake their reactions) and stands a fake
     * player next to the first pedestal; closing it gives everything back.
     */
    private static final class Rig implements AutoCloseable {
        final MinecraftServer server;
        final ServerLevel level;
        final MegaData data;
        final Sector sector;
        final List<BlockPos> slots;
        final BlockPos center;
        final FakePlayer fake;
        final BlockPos fakeAt;
        final Probe probe = new Probe();
        private final List<SectorListener> savedListeners;
        private final Sector.State savedState;

        private Rig(MinecraftServer server, ServerLevel level, MegaData data, Sector sector, List<BlockPos> slots, BlockPos center, FakePlayer fake, BlockPos fakeAt) {
            this.server = server;
            this.level = level;
            this.data = data;
            this.sector = sector;
            this.slots = slots;
            this.center = center;
            this.fake = fake;
            this.fakeAt = fakeAt;
            this.savedListeners = new ArrayList<>(MegaDungeon.listeners());
            this.savedState = sector.state;
            MegaDungeon.listeners().clear();
            MegaDungeon.listeners().add(probe);
        }

        static Rig open(MinecraftServer server, String who) {
            ServerLevel level = SelfTests.mega(server);
            check(level != null, "the server has no Mega-Dungeon dimension");
            MegaData data = MegaData.get(server);
            Sector s = BossFloors.first(data);
            check(s != null, "no boss floor exists (the smoke test grows the Holy road to floor +10 with /multiverse mega expand first)");
            List<BlockPos> slots = CruciblePedestals.ordered(s);
            check(!slots.isEmpty(), "sector 0 has no pedestal marker: its vault template or the vault pool is wrong");
            BlockPos center = CruciblePedestals.center(s);
            check(center != null, "sector 0 has no vault centre");
            int reach = CrucibleRules.BOX_HORIZONTAL;
            for (int cx = (center.getX() - reach) >> 4; cx <= (center.getX() + reach) >> 4; cx++)
                for (int cz = (center.getZ() - reach) >> 4; cz <= (center.getZ() + reach) >> 4; cz++) level.getChunk(cx, cz);       // the whole vault box is loaded for the test
            for (BlockPos p : slots) {
                level.getChunk(p.getX() >> 4, p.getZ() >> 4);
                check(level.getBlockEntity(p) instanceof RelicPedestalBlockEntity, "the pedestal marker at " + p.toShortString() + " is " + level.getBlockState(p).getBlock() + ", not a Relic Pedestal");
            }
            CatastropheRunner.stop(level, s.id);
            CrucibleVault.reset(level, s);
            BlockPos first = slots.get(0);
            double x = first.getX() + 0.5, y = first.getY() + 1.0, z = first.getZ() + 1.5;
            FakePlayer fake = SelfTests.player(level, who, x, y, z);
            fake.getInventory().clearContent();
            fake.removeAllEffects();
            return new Rig(server, level, data, s, slots, center, fake, BlockPos.containing(x, y, z));
        }

        @Override
        public void close() {
            try {
                CatastropheRunner.stop(level, sector.id);
            } catch (RuntimeException ignored) {
                // a failing clean-up must not hide the test's own result
            }
            MegaDungeon.listeners().clear();
            MegaDungeon.listeners().addAll(savedListeners);
            sector.state = savedState;
            try {
                CrucibleVault.reset(level, sector);
            } catch (RuntimeException ignored) {
                // see above
            }
            fake.getInventory().clearContent();
            fake.removeAllEffects();
            data.setDirty();
        }
    }

    private static RelicPedestalBlockEntity pedestal(Rig r, BlockPos pos) {
        check(r.level.getBlockEntity(pos) instanceof RelicPedestalBlockEntity, "the pedestal at " + pos.toShortString() + " is gone");
        return (RelicPedestalBlockEntity) r.level.getBlockEntity(pos);
    }

    /** The ids of the relics a fake player carries, in inventory order. */
    private static List<String> relicsIn(FakePlayer p) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            RelicDef d = Relics.defOf(p.getInventory().getItem(i));
            if (d != null) out.add(d.id());
        }
        return out;
    }

    /** Are there Cursed Spirits (tagged mobs) anywhere in the level? */
    private static int spiritsIn(ServerLevel level) {
        return level.getEntities(EntityTypeTest.<Entity, Mob>forClass(Mob.class), m -> m.getTags().contains(CursedSpirits.TAG)).size();
    }

    /** A hash of every block state in the vault box, so "changes no block" is something that can be compared. */
    private static int blocksOf(Rig r) {
        int reach = CrucibleRules.BOX_HORIZONTAL, up = CrucibleRules.BOX_VERTICAL;
        int h = 1;
        for (BlockPos p : BlockPos.betweenClosed(r.center.offset(-reach, -up, -reach), r.center.offset(reach, up, reach))) h = 31 * h + r.level.getBlockState(p).hashCode();
        return h;
    }

    /** The ticks a catastrophe of this kind may take, with some slack: its configured length, or for the spirit the minutes it waits for a player who never comes. */
    private static int capTicks(String kind) {
        return switch (kind) {
            case CrucibleRules.COLLAPSE -> CatastropheRules.collapseTotalTicks(ThemeConfig.get(ThemeConfig.COLLAPSE_SECONDS)) + 40;
            case CrucibleRules.FLOOD -> CatastropheRules.floodTotalTicks(ThemeConfig.get(ThemeConfig.FLOOD_SECONDS)) + 40;
            default -> ThemeConfig.get(ThemeConfig.SPIRIT_MINUTES) * 1200 + 400;
        };
    }

    /** Advances the runner on a made-up clock until the sector's catastrophe ends or {@code cap} ticks passed; returns the ticks used. */
    private static int drive(Rig r, long t0, int from, int cap) {
        int used = from;
        while (CatastropheRunner.active(r.sector.id) && used < cap) {
            used++;
            CatastropheRunner.tick(r.server, r.level, t0 + used);
        }
        return used;
    }

    // ---------------------------------------------------------------- theme.crucible_flow
    private static SelfTests.Result flow(MinecraftServer server) {
        try (Rig r = Rig.open(server, "crucible_flow")) {
            return flowIn(r);
        } catch (Failure f) {
            return SelfTests.Result.fail(f.getMessage());
        }
    }

    private static SelfTests.Result flowIn(Rig r) {
        Sector s = r.sector;
        Sector.Crucible c = s.crucible;
        Set<String> live0 = UniqueLedger.liveIds(r.server);
        int started0 = CatastropheRunner.started();

        // ---- nothing is rolled before somebody comes
        check(Phase.of(c.phase.name()) == Phase.UNOPENED, "the vault is " + c.phase + " after the reset, not UNOPENED");
        check(NextTheme.nextThemeFor(s) == null, "an UNOPENED vault already names the next theme");
        for (BlockPos p : r.slots) {
            RelicPedestalBlockEntity be = pedestal(r, p);
            check(be.getHeld().isEmpty() && !be.isTaken(), "the pedestal at " + p.toShortString() + " shows a relic or is sealed before the offer is rolled");
        }

        // ---- the first survival player in the vault box rolls the offer (the ticker's own entry point)
        check(CruciblePedestals.inBox(s, r.fakeAt), "the fake player at " + r.fakeAt.toShortString() + " is not inside the vault box (centre " + r.center.toShortString() + ")");
        check(!CruciblePedestals.inBox(s, r.center.offset(CrucibleRules.BOX_HORIZONTAL + 1, 0, 0)), "the vault box is wider than " + CrucibleRules.BOX_HORIZONTAL);
        check(CrucibleVault.visit(r.level, r.data, r.fake, r.fakeAt, 0) == s, "a survival player next to a pedestal was not seen in sector 0's vault");
        int want = CrucibleRules.offerCount(ThemeConfig.get(ThemeConfig.OFFER_COUNT), r.slots.size());
        check(Phase.of(c.phase.name()) == Phase.ROLLED, "the vault is " + c.phase + " after the first visit, not ROLLED (the world may have no unissued relic of " + ThemeConfig.minGrade() + " or above)");
        check(c.offered.size() == want, "the offer has " + c.offered.size() + " slots, expected " + want + " (offerCount " + ThemeConfig.get(ThemeConfig.OFFER_COUNT) + ", " + r.slots.size() + " pedestals)");
        List<String> offered = new ArrayList<>(c.offered);
        Grade min = ThemeConfig.minGrade();
        Set<String> distinct = new HashSet<>();
        StringBuilder shown = new StringBuilder();
        for (int i = 0; i < offered.size(); i++) {
            String id = offered.get(i);
            check(id != null && !id.isEmpty(), "slot " + i + " of the offer is empty: fewer than " + want + " unissued relics of " + min + " or above exist");
            check(distinct.add(id), "the relic " + id + " is offered twice");
            RelicDef d = Relics.byId(id);
            check(d != null, "the offered relic " + id + " is not registered");
            check(d.grade().atLeast(min), id + " is " + d.grade() + ", below the minimum grade " + min);
            check(!d.hasPair(), id + " is a pair: a pick must be exactly one item");
            check(!live0.contains(id), id + " is offered although a copy of it is already alive");
            RelicPedestalBlockEntity be = pedestal(r, r.slots.get(i));
            check(Relics.defOf(be.getHeld()) == d, "pedestal " + i + " does not show " + id);
            check(!be.isTaken(), "pedestal " + i + " is sealed before the pick");
            shown.append(shown.length() == 0 ? "" : ", ").append(id).append(" (").append(d.grade().shortName()).append(')');
        }
        check(UniqueLedger.liveIds(r.server).equals(live0), "rolling the offer issued a relic: the unchosen ones would be burnt");

        // ---- walking in again does not roll again
        CrucibleVault.visit(r.level, r.data, r.fake, r.fakeAt, 40);
        check(c.offered.equals(offered), "a second visit changed the offer");

        // ---- the pick: the method the pedestal block calls
        int k = Math.min(1, offered.size() - 1);
        long t0 = r.level.getGameTime();
        check(pedestal(r, r.slots.get(k)).take(r.fake), "take() refused a legal pick at slot " + k);
        check(CatastropheRunner.started() == started0 + 1, "the pick started " + (CatastropheRunner.started() - started0) + " catastrophes, not exactly one");
        check(Phase.of(c.phase.name()) == Phase.RESOLVED, "the vault is " + c.phase + " right after the pick, not RESOLVED (the pick releases the next sector)");
        check(r.probe.resolved.equals(List.of(s.id)), "the pick told the listeners of resolutions " + r.probe.resolved + ", exactly one for sector " + s.id + " was expected right away");
        String chosen = c.chosen;
        check(chosen.equals(offered.get(k)), "the chosen relic is " + chosen + ", not the one on pedestal " + k + " (" + offered.get(k) + "): it went live meanwhile and was swapped");
        check(r.fake.getUUID().equals(c.chooser), "the chooser was not saved");
        String kind = c.catastrophe;
        check(CrucibleRules.KINDS.contains(kind), "the drawn catastrophe '" + kind + "' is not one of " + CrucibleRules.KINDS);
        check(CatastropheRunner.active(s.id) && CatastropheRunner.kindOf(s.id).equals(kind), "the catastrophe '" + kind + "' is not running (the runner has '" + CatastropheRunner.kindOf(s.id) + "')");
        Set<String> live1 = UniqueLedger.liveIds(r.server);
        Set<String> issued = new HashSet<>(live1);
        issued.removeAll(live0);
        check(issued.equals(Set.of(chosen)), "the pick issued " + issued + ", exactly [" + chosen + "] was expected");
        for (String id : offered) if (!id.equals(chosen)) check(!live1.contains(id), id + " was issued although it was not chosen");
        List<String> held = relicsIn(r.fake);
        check(held.equals(List.of(chosen)), "the player holds " + held + " after the pick, exactly [" + chosen + "] was expected");
        for (int i = 0; i < r.slots.size(); i++) {
            RelicPedestalBlockEntity be = pedestal(r, r.slots.get(i));
            check(be.getHeld().isEmpty(), "pedestal " + i + " still shows a relic after the pick");
            check(be.isTaken(), "pedestal " + i + " is not sealed after the pick");
            check(i == k ? c.offered.get(i).equals(chosen) : c.offered.get(i).isEmpty(), "the saved offer of slot " + i + " is '" + c.offered.get(i) + "' after the pick");
        }
        RegionTheme next = NextTheme.nextThemeFor(s);
        check(next != null, "a CHOSEN vault does not name the next theme");
        RelicDef cd = Relics.byId(chosen);
        check(next == RelicThemes.themeOf(chosen, cd == null ? "" : cd.cardName(), RelicThemeMap.explicit(), NextTheme.keywords()), "the next theme " + next + " is not the one RelicThemes gives " + chosen);

        // ---- a second pick is refused, at every pedestal, and changes nothing
        for (int i = 0; i < r.slots.size(); i++) check(!pedestal(r, r.slots.get(i)).take(r.fake), "a second take() at slot " + i + " was not refused");
        check(CatastropheRunner.started() == started0 + 1, "a refused pick started another catastrophe");
        check(UniqueLedger.liveIds(r.server).equals(live1), "a refused pick changed the ledger");
        check(relicsIn(r.fake).equals(held), "a refused pick gave something");
        check(Phase.of(c.phase.name()) == Phase.RESOLVED, "a refused pick moved the vault to " + c.phase);

        // ---- the catastrophe runs out by itself, on a made-up clock
        UUID spirit = CatastropheRunner.spiritOf(s.id);
        check((spirit != null) == CrucibleRules.SPIRIT.equals(kind), "a spirit entity " + (spirit == null ? "is missing for" : "exists for") + " the '" + kind + "' catastrophe");
        int used = drive(r, t0, 0, capTicks(kind));
        check(!CatastropheRunner.active(s.id), "the " + kind + " was still running after " + used + " ticks");
        check(Phase.of(c.phase.name()) == Phase.RESOLVED, "the vault is " + c.phase + " after the " + kind + " ended, not RESOLVED");
        check(r.probe.resolved.equals(List.of(s.id)), "the listeners were told of resolutions " + r.probe.resolved + " by the end of the " + kind + ", exactly one for sector " + s.id + " was expected (the catastrophe resolves nothing)");
        if (spirit != null) check(r.level.getEntity(spirit) == null, "the Cursed Spirit is still in the world after the catastrophe ended");
        check(spiritsIn(r.level) == 0, "a Cursed Spirit is left in the Mega-Dungeon");
        check(CatastropheRunner.started() == started0 + 1, "more than one catastrophe ran");

        // ---- and the finished vault stays finished
        for (int i = 0; i < r.slots.size(); i++) check(!pedestal(r, r.slots.get(i)).take(r.fake), "a pick at slot " + i + " of a RESOLVED vault was not refused");
        check(NextTheme.nextThemeFor(s) == next, "the next theme changed after the vault was resolved");
        check(UniqueLedger.liveIds(r.server).equals(live1) && relicsIn(r.fake).equals(held), "a pick in a RESOLVED vault changed something");
        return SelfTests.Result.ok("offered " + shown + " on " + r.slots.size() + " pedestals; picked " + chosen + " at slot " + k + ": exactly one relic issued and given, the others untouched and "
                + "their pedestals sealed; one " + kind + " ran " + used + " ticks and ended; vault RESOLVED, listeners told once; second picks refused; next theme " + next.id());
    }

    // ---------------------------------------------------------------- theme.single_relic
    private static SelfTests.Result singleRelic(MinecraftServer server) {
        try (Rig r = Rig.open(server, "single_relic")) {
            return singleRelicIn(r);
        } catch (Failure f) {
            return SelfTests.Result.fail(f.getMessage());
        }
    }

    private static SelfTests.Result singleRelicIn(Rig r) {
        MinecraftServer server = r.server;
        ServerLevel mega = r.level;
        Sector s = r.sector;
        Set<String> live0 = UniqueLedger.liveIds(server);
        RandomSource rng = mega.getRandom();

        // ---- A. in the Mega-Dungeon nothing but a Crucible hands out a relic
        check(!RelicLoot.anyUnissued(mega, Grade.NORMAL, Grade.GENESIS), "RelicLoot still sees unissued relics to roll in the Mega-Dungeon");
        check(RelicLoot.rollUnissuedAll(mega, rng, Grade.NORMAL, Grade.GENESIS).isEmpty(), "RelicLoot rolled a relic in the Mega-Dungeon");
        check(RelicLoot.rollUnissued(mega, rng, Grade.UNIQUE, Grade.GENESIS).isEmpty(), "RelicLoot.rollUnissued gave a relic in the Mega-Dungeon");
        check(!RelicExclusivity.allowsMobDrop(mega), "a Mimic may still drop a relic in the Mega-Dungeon");
        check(UniqueLedger.liveIds(server).equals(live0), "the relic roll of the Mega-Dungeon changed the ledger");
        ServerLevel overworld = server.overworld();
        boolean control = RelicLoot.anyUnissued(overworld, Grade.NORMAL, Grade.GENESIS);            // the same question one dimension over: the guard is what differs
        for (String path : List.of("chests/labyrinth_pedestal", "chests/dungeon_relic_vault")) {
            ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath("nusmp", path));
            LootTable table = server.reloadableRegistries().getLootTable(key);
            for (int i = 0; i < 4; i++) {
                LootParams params = new LootParams.Builder(mega).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(r.center)).create(LootContextParamSets.CHEST);
                for (ItemStack st : table.getRandomItems(params, 7000L + i)) check(Relics.defOf(st) == null, "the table nusmp:" + path + " rolled the relic " + st.getHoverName().getString() + " in the Mega-Dungeon");
            }
        }
        check(UniqueLedger.liveIds(server).equals(live0), "rolling the vault loot tables in the Mega-Dungeon changed the ledger");

        // ---- B. region A: the real vault; region B: another region of the same theme and seed, which must never offer what A offers
        check(CrucibleVault.visit(mega, r.data, r.fake, r.fakeAt, 0) == s, "the fake player was not seen in the vault");
        check(Phase.of(s.crucible.phase.name()) == Phase.ROLLED, "region A is " + s.crucible.phase + ", not ROLLED");
        int want = CrucibleRules.offerCount(ThemeConfig.get(ThemeConfig.OFFER_COUNT), r.slots.size());
        List<String> a1 = new ArrayList<>(s.crucible.offered);
        check(a1.size() == want && !a1.contains(""), "region A offers " + a1 + ", " + want + " relics were expected");
        Sector b = new Sector(s.id);
        b.theme = s.theme;
        b.seed = s.seed;
        b.threat = s.threat;
        b.relicCount = s.relicCount;
        List<RelicDef> b1 = CrucibleOffer.pick(mega, want, CrucibleOffer.offeredElsewhere(r.data, b));
        check(b1.size() == want, "region B could only offer " + b1.size() + " of " + want + " relics");
        Set<String> seen = new HashSet<>(a1);
        check(seen.size() == a1.size(), "region A offers the same relic twice: " + a1);
        for (RelicDef d : b1) check(seen.add(d.id()), "regions A and B both offer " + d.id());

        // ---- C. a pick in A adds exactly one relic to the world, and B can no longer be offered it
        check(pedestal(r, r.slots.get(0)).take(r.fake), "the pick in region A was refused");
        String c1 = s.crucible.chosen;
        Set<String> live1 = UniqueLedger.liveIds(server);
        Set<String> added1 = new HashSet<>(live1);
        added1.removeAll(live0);
        check(added1.equals(Set.of(c1)), "the first region's pick added " + added1 + " to the world, exactly [" + c1 + "] was expected");
        check(relicsIn(r.fake).equals(List.of(c1)), "the player holds " + relicsIn(r.fake) + " after the first pick");
        for (RelicDef d : CrucibleOffer.pick(mega, want, CrucibleOffer.offeredElsewhere(r.data, b))) check(!d.id().equals(c1), "region B would be offered " + c1 + ", which region A's party already holds");
        for (int i = 0; i < r.slots.size(); i++) check(!pedestal(r, r.slots.get(i)).take(r.fake), "region A let a second relic be taken at slot " + i);
        check(UniqueLedger.liveIds(server).equals(live1), "a refused pick in region A changed the world");

        // ---- D. the next region of the same theme and seed (the vault is reset, which also ends the running catastrophe cleanly)
        CrucibleVault.reset(mega, s);
        check(!CatastropheRunner.active(s.id), "resetting the vault left its catastrophe running");
        check(CrucibleVault.visit(mega, r.data, r.fake, r.fakeAt, 0) == s && Phase.of(s.crucible.phase.name()) == Phase.ROLLED, "the second region did not roll");
        List<String> a2 = new ArrayList<>(s.crucible.offered);
        check(a2.size() == want && !a2.contains(""), "the second region offers " + a2 + ", " + want + " relics were expected");
        check(!a2.contains(c1), "the second region offers " + c1 + ", which is already alive");
        check(pedestal(r, r.slots.get(Math.min(1, a2.size() - 1))).take(r.fake), "the pick in the second region was refused");
        String c2 = s.crucible.chosen;
        check(!c2.equals(c1), "both regions gave the relic " + c1);
        Set<String> added2 = new HashSet<>(UniqueLedger.liveIds(server));
        added2.removeAll(live1);
        check(added2.equals(Set.of(c2)), "the second region's pick added " + added2 + " to the world, exactly [" + c2 + "] was expected");
        List<String> carried = relicsIn(r.fake);
        check(carried.size() == 2 && carried.contains(c1) && carried.contains(c2), "the player holds " + carried + ", exactly [" + c1 + ", " + c2 + "] was expected");

        // ---- E. a third roll offers neither
        CrucibleVault.reset(mega, s);
        check(CrucibleVault.visit(mega, r.data, r.fake, r.fakeAt, 0) == s, "the third region did not roll");
        List<String> a3 = new ArrayList<>(s.crucible.offered);
        check(!a3.contains(c1) && !a3.contains(c2), "the third region offers " + a3 + ", which holds a relic that is already alive");
        return SelfTests.Result.ok("the relic roll, the Mimic and the vault loot tables give no relic in the Mega-Dungeon" + (control ? " (the overworld still rolls)" : " (control skipped: every relic is already alive)")
                + "; region A offered " + a1 + ", the same-theme region B " + b1.stream().map(RelicDef::id).toList() + " (disjoint); the picks added exactly one relic each (" + c1 + ", " + c2 + "), never the same twice");
    }

    // ---------------------------------------------------------------- theme.catastrophe_undo
    private static SelfTests.Result catastropheUndo(MinecraftServer server) {
        try (Rig r = Rig.open(server, "catastrophe_undo")) {
            return catastropheUndoIn(r);
        } catch (Failure f) {
            return SelfTests.Result.fail(f.getMessage());
        }
    }

    /** The saved state of a Crucible and the world around it, to compare before and after. */
    private record Snapshot(Sector.Crucible.Phase phase, List<String> offered, String chosen, UUID chooser, String catastrophe, long since, int blocks, int mobs) {
        static Snapshot of(Rig r) {
            Sector.Crucible c = r.sector.crucible;
            return new Snapshot(c.phase, List.copyOf(c.offered), c.chosen, c.chooser, c.catastrophe, c.since, blocksOf(r),
                    r.level.getEntities(EntityTypeTest.<Entity, Entity>forClass(Entity.class), e -> true).size());
        }
    }

    private static SelfTests.Result catastropheUndoIn(Rig r) {
        Sector s = r.sector;
        StringBuilder detail = new StringBuilder();

        // ---- each kind, as the admin's test run: it ends by itself and leaves nothing behind
        for (String kind : CrucibleRules.KINDS) {
            boolean noSpirit = CrucibleRules.SPIRIT.equals(kind) && !CursedSpirits.available();
            String expect = noSpirit ? CrucibleRules.COLLAPSE : kind;
            Snapshot before = Snapshot.of(r);
            int started0 = CatastropheRunner.started();
            long t0 = r.level.getGameTime();
            check(CatastropheRunner.start(r.level, s, kind, r.fake, r.center, false), "the " + kind + " could not start");
            check(CatastropheRunner.active(s.id) && CatastropheRunner.kindOf(s.id).equals(expect), "the " + kind + " test run is '" + CatastropheRunner.kindOf(s.id) + "', expected '" + expect + "'" + (noSpirit ? " (no roster entity exists in this game)" : ""));
            check(CatastropheRunner.started() == started0 + 1, "starting the " + kind + " counted " + (CatastropheRunner.started() - started0) + " catastrophes");
            UUID spirit = CatastropheRunner.spiritOf(s.id);
            int used = 0;
            if (CrucibleRules.SPIRIT.equals(expect)) {
                check(spirit != null, "the spirit catastrophe has no spirit");
                Entity e = r.level.getEntity(spirit);
                check(e instanceof Mob m && m.getTags().contains(CursedSpirits.TAG) && m.isNoAi() && m.isInvulnerable(), "the spirit does not stand still and untouchable in its summoning circle");
                used = drive(r, t0, 0, CatastropheRules.SPIRIT_SUMMON_TICKS - 1);
                check(((Mob) r.level.getEntity(spirit)).isNoAi(), "the spirit woke before its summoning circle ended");
                used = drive(r, t0, used, CatastropheRules.SPIRIT_SUMMON_TICKS);
                Entity awake = r.level.getEntity(spirit);
                check(awake instanceof Mob m && !m.isNoAi() && !m.isInvulnerable(), "the spirit did not wake and become vulnerable when its circle ended");
            }
            used = drive(r, t0, used, capTicks(expect));
            check(!CatastropheRunner.active(s.id), "the " + expect + " was still running after " + used + " ticks");
            if (spirit != null) check(r.level.getEntity(spirit) == null, "the Cursed Spirit is still in the world after its catastrophe ended");
            check(spiritsIn(r.level) == 0, "a Cursed Spirit is left in the Mega-Dungeon after the " + expect);
            Snapshot after = Snapshot.of(r);
            check(after.blocks() == before.blocks(), "the " + expect + " changed a block of the vault");
            check(after.mobs() == before.mobs(), "the " + expect + " left " + (after.mobs() - before.mobs()) + " more entities in the Mega-Dungeon");
            check(after.equals(before), "the " + expect + " test run changed the saved Crucible: " + before + " became " + after);
            check(r.probe.resolved.isEmpty(), "the " + expect + " test run resolved the Crucible");
            detail.append(detail.length() == 0 ? "" : ", ").append(expect).append(' ').append(used).append(" ticks");
        }

        // ---- what the catastrophes do to someone in the vault, on a pig that stands in for a player (a fake player is invulnerable and in no level's list)
        Pig pig = EntityType.PIG.create(r.level);
        check(pig != null, "a pig could not be made");
        pig.moveTo(r.center.getX() + 0.5, r.center.getY(), r.center.getZ() + 0.5, 0f, 0f);
        float max = pig.getMaxHealth();
        CatastropheRunner.pulse(r.level, s.threat, pig);
        float lost = max - pig.getHealth();
        check(lost > 0f, "a collapse pulse did no damage");
        check(lost <= 0.35f * max + 0.01f, "a collapse pulse took " + lost + " of " + max + " health, more than 35 %");
        int amplifier = CrucibleRules.floodAmplifier(s.threat, ThemeConfig.get(ThemeConfig.FLOOD_AMPLIFIER_PER_THREAT));
        CatastropheRunner.floodEffects(pig, amplifier);
        MobEffectInstance sick = pig.getEffect(DungeonEffects.MAGICULE_SICKNESS);
        check(sick != null && sick.getAmplifier() == amplifier, "the flood did not give magicule sickness of level " + amplifier);
        check(sick.getDuration() > 0 && sick.getDuration() <= 60, "the flood's sickness lasts " + sick.getDuration() + " ticks: it must lapse within three seconds of the last renewal");
        MobEffectInstance slow = pig.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        check(slow != null && slow.getDuration() > 0 && slow.getDuration() <= 60, "the flood's slowness is missing or lasts longer than three seconds");
        pig.discard();

        // ---- a catastrophe that is stopped half way (an admin's reset) leaves nothing and resolves nothing
        if (CursedSpirits.available()) {
            long t0 = r.level.getGameTime();
            check(CatastropheRunner.start(r.level, s, CrucibleRules.SPIRIT, r.fake, r.center, true), "the spirit could not start for the reset test");
            s.crucible.phase = Sector.Crucible.Phase.CHOSEN;                                       // as the pick leaves it
            UUID spirit = CatastropheRunner.spiritOf(s.id);
            check(spirit != null || CatastropheRunner.kindOf(s.id).equals(CrucibleRules.COLLAPSE), "the spirit run has no spirit");
            drive(r, t0, 0, 100);
            CrucibleVault.reset(r.level, s);
            check(!CatastropheRunner.active(s.id), "a reset left the catastrophe running");
            if (spirit != null) check(r.level.getEntity(spirit) == null, "a reset left the Cursed Spirit in the world");
            check(spiritsIn(r.level) == 0, "a Cursed Spirit is left after the reset");
            check(Phase.of(s.crucible.phase.name()) == Phase.UNOPENED, "the reset vault is " + s.crucible.phase);
            drive(r, t0, 100, 130);
            check(r.probe.resolved.isEmpty(), "a stopped catastrophe resolved the Crucible");
            detail.append(", reset mid-run clean");
        }

        // ---- a restart in the middle of a catastrophe ends it: the vault is not left CHOSEN with no clock
        check(CatastropheRunner.start(r.level, s, CrucibleRules.COLLAPSE, r.fake, r.center, true), "the collapse could not start for the restart test");
        s.crucible.phase = Sector.Crucible.Phase.CHOSEN;
        CatastropheRunner.clear();                                                                // what a server stop does to the in-memory state
        check(!CatastropheRunner.active(s.id), "clearing the runner left the catastrophe running");
        CrucibleVault.sweepChosen(r.level, r.data);
        check(Phase.of(s.crucible.phase.name()) == Phase.RESOLVED, "a vault left CHOSEN by a restart is " + s.crucible.phase + ", not RESOLVED");
        check(r.probe.resolved.equals(List.of(s.id)), "the restart resolution told the listeners " + r.probe.resolved + ", exactly one for sector " + s.id + " was expected");
        detail.append(", restart resolves a CHOSEN vault");

        // ---- the roster: every creature that exists can be made into a spirit
        int built = 0;
        List<String> missing = new ArrayList<>();
        for (SpiritRoster.Entry e : CursedSpirits.roster()) {
            if (!CursedSpirits.exists(e.entity())) {
                missing.add(e.entity());
                continue;
            }
            Mob m = CursedSpirits.spawnEntry(r.level, s.threat, e, r.center);
            check(m != null, "the roster entry " + e.entity() + " exists but could not be made into a spirit");
            check(m.getTags().contains(CursedSpirits.TAG) && m.isNoAi() && m.isInvulnerable() && m.hasCustomName(), e.entity() + " is not a held, named, tagged spirit when it appears");
            m.discard();
            built++;
        }
        check(CursedSpirits.roster().isEmpty() || built > 0, "no creature of the spirit roster exists in this game (" + missing + "): the SPIRIT catastrophe can never happen");
        check(spiritsIn(r.level) == 0, "a roster spirit is left in the Mega-Dungeon");
        return SelfTests.Result.ok("each kind ran to its end on a made-up clock and left no block, entity or saved state changed (" + detail + "); a collapse pulse is capped at 35 % health and the flood's effects lapse within 3 s; "
                + built + " of " + CursedSpirits.roster().size() + " roster creatures can be made into spirits" + (missing.isEmpty() ? "" : ", not in this game: " + missing));
    }
}
