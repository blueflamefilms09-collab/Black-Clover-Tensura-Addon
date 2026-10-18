package com.newuniverse.nusmp.skilltree.item;

import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.guard.LabyrinthGuardianEntity;
import com.newuniverse.nusmp.skilltree.SkillTreeApi;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.loot.BossBookDrops;
import com.newuniverse.nusmp.skilltree.loot.LootConfig;
import com.newuniverse.nusmp.skilltree.loot.LootDrops;
import com.newuniverse.nusmp.skilltree.loot.LootRules;
import com.newuniverse.nusmp.skilltree.loot.LootVictims;
import com.newuniverse.nusmp.skilltree.loot.SkillBookLootModifier;
import com.newuniverse.nusmp.skilltree.state.IPlayerSkillTree;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;

/**
 * Why: the items system is mostly WIRING (a registry entry, a component, a codec, a JSON type, a line in the global list of loot modifiers, a listener on the API, a death
 * event) and wiring is exactly what a unit test cannot see and what silently does nothing when one link is missing: the book would simply never work, or the drop would
 * never happen. So the two self-tests, run by the CI server smoke test through {@code /multiverse mega selftest} with a stand-in player, drive the real thing.
 * {@code tree.items.use} reads books the way a player does, through {@link BookUse}: an SP Book gives its points and is used up, a second reading inside the cooldown does
 * nothing, a creative reader keeps the book, a blank or crafted-negative book gives nothing, the gamble stays inside its table, a Skill Book of a route that is loaded
 * unlocks it (and a second copy becomes Skill Points or is refused, as the setting says), an unknown book is blank, and the creative list and the factories agree with the
 * registry. {@code tree.items.loot} makes up kills: an ordinary monster in an Abyssal sector with the chances forced to 1 must add books through the modifier's own entry
 * point, and must add NOTHING on the Holy road, in the Overworld, for a death no player caused, for a servant, a boss, the Guardian (which the modifier leaves to its death
 * listener) or a harmless animal; a made-up ENTITY loot table is then rolled through the whole loot pipeline with the real odds, so the codec, the JSON type and the global
 * list are exercised (the SP Book count must land on the configured chance within six standard deviations); and the Guardian's death listener is driven with a made-up site.
 * Where the world lacks something (no loaded route, no Abyssal sector yet) the test says so in its answer and checks what it can instead of failing for a reason that is not
 * the items' fault. The stand-in players are put back as they were found. Statistics use a fixed seed and six standard deviations, so the tests never flake.
 */
public final class ItemsSelfTests {
    private ItemsSelfTests() {}

    private static final String READER = "tree_items_reader", KILLER = "tree_items_killer";
    private static final ResourceLocation TABLE_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "entities/skill_book_selftest");

    /** Registers the two tests; called from {@link ItemsSystem#init}. */
    public static void register() {
        SelfTests.register("tree.items.use", ItemsSelfTests::use);
        SelfTests.register("tree.items.loot", ItemsSelfTests::loot);
    }

    private static SelfTests.Result ok(String d) { return SelfTests.Result.ok(d); }

    private static SelfTests.Result fail(String d) { return SelfTests.Result.fail(d); }

    // =================================================================================================== tree.items.use
    private static SelfTests.Result use(MinecraftServer server) {
        ServerLevel level = server.overworld();
        ServerPlayer p = SelfTests.player(level, READER, 0, 120, 0);
        IPlayerSkillTree tree = SkillTreeApi.of(p);
        int sp0 = tree.sp();
        String class0 = tree.activeClass();
        long classAt0 = tree.classChangedAt();
        boolean creative0 = p.getAbilities().instabuild;
        List<String> routesBefore = new ArrayList<>(tree.unlockedRoutes());
        StringBuilder said = new StringBuilder();
        try {
            tree.setSp(0);
            p.getAbilities().instabuild = false;
            clearCooldowns(p);

            // 1. an SP Book: the kind's points, used up, then the cooldown, then creative keeps it
            ItemStack spBook = SPBookItem.stack(SpKind.MAGIC_BOOSTER);
            spBook.setCount(3);
            BookUse.Result r = BookUse.readSpBook(p, spBook, new Random(1)::nextDouble);
            if (r == BookUse.Result.DISABLED) return ok("skipped: the Skill Tree is switched off (enabled=false in nusmp-skilltree-server.toml)");
            if (r != BookUse.Result.SP_GRANTED) return fail("a Magic Booster read gave " + r + ", not SP_GRANTED");
            if (SkillTreeApi.sp(p) != 5) return fail("a Magic Booster gave " + SkillTreeApi.sp(p) + " Skill Points, not 5");
            if (spBook.getCount() != 2) return fail("a survival reader should have used one of three books up, " + spBook.getCount() + " are left");
            r = BookUse.readSpBook(p, spBook, new Random(1)::nextDouble);
            if (r != BookUse.Result.COOLDOWN || SkillTreeApi.sp(p) != 5 || spBook.getCount() != 2) return fail("a second reading inside the cooldown must do nothing, got " + r);
            clearCooldowns(p);
            p.getAbilities().instabuild = true;
            r = BookUse.readSpBook(p, spBook, new Random(1)::nextDouble);
            if (r != BookUse.Result.SP_GRANTED || SkillTreeApi.sp(p) != 10 || spBook.getCount() != 2) return fail("a creative reader must get the points and keep the book, got " + r + " sp=" + SkillTreeApi.sp(p) + " left=" + spBook.getCount());
            p.getAbilities().instabuild = false;
            said.append("SP Book ok; ");

            // 2. blank and crafted books give nothing and keep the book
            tree.setSp(0);
            clearCooldowns(p);
            ItemStack bare = new ItemStack(SkillItems.SP_BOOK.get());
            if (BookUse.readSpBook(p, bare, new Random(2)::nextDouble) != BookUse.Result.BLANK || bare.getCount() != 1 || SkillTreeApi.sp(p) != 0) return fail("an SP Book with no kind and no amount must be blank");
            clearCooldowns(p);
            ItemStack negative = new ItemStack(SkillItems.SP_BOOK.get());
            negative.set(SkillItems.SP_AMOUNT.get(), -5);
            if (BookUse.readSpBook(p, negative, new Random(2)::nextDouble) != BookUse.Result.BLANK || SkillTreeApi.sp(p) != 0) return fail("a crafted negative amount must give nothing, sp=" + SkillTreeApi.sp(p));
            clearCooldowns(p);
            ItemStack unknownBook = SkillBookItem.stack("nusmp:no_such_book_selftest");
            if (BookUse.readSkillBook(p, unknownBook) != BookUse.Result.BLANK || unknownBook.getCount() != 1) return fail("a Skill Book of no route must be blank and kept");
            said.append("blank books ok; ");

            // 3. the gamble stays inside its table (creative keeps the one stack; each reading is a fresh roll)
            p.getAbilities().instabuild = true;
            ItemStack box = SPBookItem.stack(SpKind.JACK_IN_THE_BOX);
            Random random = new Random(62_000L);
            long total = 0;
            int rolls = 150, jackpots = 0;
            for (int i = 0; i < rolls; i++) {
                tree.setSp(0);
                clearCooldowns(p);
                if (BookUse.readSpBook(p, box, random::nextDouble) != BookUse.Result.SP_GRANTED) return fail("a Jack-in-the-Box reading " + i + " did not give points");
                int gain = SkillTreeApi.sp(p);
                boolean inTable = (gain >= JackInTheBox.FLOOR && gain <= JackInTheBox.TOP) || gain == JackInTheBox.JACKPOT;
                if (!inTable) return fail("a Jack-in-the-Box gave " + gain + " Skill Points, which is not in its table (1 to 9, or 20)");
                if (gain == JackInTheBox.JACKPOT) jackpots++;
                total += gain;
            }
            double mean = (double) total / rolls;
            if (mean < 2.0 || mean > 12.0) return fail("the mean of " + rolls + " Jack-in-the-Box readings was " + String.format(Locale.ROOT, "%.2f", mean) + ", the table's mean is about 5");
            p.getAbilities().instabuild = false;
            said.append("gamble ").append(rolls).append(" readings mean ").append(String.format(Locale.ROOT, "%.1f", mean)).append(" (").append(jackpots).append(" jackpots); ");

            // 4. a Skill Book of a loaded route
            SkillRoute route = firstRoute();
            if (route == null) {
                said.append("no route is loaded, so the unlock path was not exercised (the content builders ship the routes); ");
            } else {
                String problem = skillBookPath(p, tree, route, said);
                if (problem != null) return problem.startsWith("skipped") ? ok(problem + "; " + said) : fail(problem);
            }

            // 5. the creative list and the factories agree with the registry and the kind table
            List<ItemStack> list = ItemsSystem.creativeItems();
            int books = 0, primers = 0;
            for (ItemStack s : list) {
                if (s.getItem() instanceof SkillBookItem) {
                    books++;
                    if (RouteRegistry.byBook(SkillItems.bookIdOf(s)) == null) return fail("a creative Skill Book names a book no route lists: " + SkillItems.bookIdOf(s));
                } else if (s.getItem() instanceof SPBookItem) {
                    primers++;
                    if (SpKind.byId(SkillItems.spKindOf(s)) == null) return fail("a creative SP Book has no known kind");
                }
            }
            if (books != RouteRegistry.all().size()) return fail("the creative list holds " + books + " Skill Books for " + RouteRegistry.all().size() + " routes");
            if (primers != SpKind.values().length) return fail("the creative list holds " + primers + " SP Books for " + SpKind.values().length + " kinds");
            if (!SPBookItem.stack("not_a_kind").isEmpty()) return fail("an unknown SP kind must give an empty stack, not a book of something else");
            said.append("creative list ").append(books).append(" books + ").append(primers).append(" primers");
            return ok(said.toString());
        } finally {
            p.getAbilities().instabuild = creative0;
            clearCooldowns(p);
            tree.setSp(sp0);
            for (String id : new ArrayList<>(tree.unlockedRoutes())) if (!routesBefore.contains(id)) tree.removeRoute(id);
            tree.setActiveClass(class0);
            tree.setClassChangedAt(classAt0);
        }
    }

    /** The unlock, cooldown, duplicate and creative paths of a Skill Book for {@code route}. Null when all is well, "skipped..." when the tree is off, otherwise the problem. */
    private static String skillBookPath(ServerPlayer p, IPlayerSkillTree tree, SkillRoute route, StringBuilder said) {
        tree.removeRoute(route.id());
        p.getAbilities().instabuild = false;
        clearCooldowns(p);
        ItemStack book = SkillBookItem.stackFor(route);
        book.setCount(2);
        int shows = UnlockFx.pendingCount();
        BookUse.Result r = BookUse.readSkillBook(p, book);
        if (r == BookUse.Result.DISABLED) return "skipped: the Skill Tree is switched off (enabled=false)";
        if (r != BookUse.Result.UNLOCKED) return "reading the book of " + route.id() + " gave " + r + ", not UNLOCKED";
        if (UnlockFx.pendingCount() != shows + 1) return "the unlock show did not start (its second stage is not queued): is UnlockFx registered with SkillTreeApi.addUnlockListener, and does unlockRoute call its listeners?";
        if (!SkillTreeApi.hasRoute(p, route.id())) return "the book was read but the route " + route.id() + " is not unlocked";
        if (book.getCount() != 1) return "the unlocking book should have been used up (2 -> 1), " + book.getCount() + " are left";
        if (BookUse.readSkillBook(p, book) != BookUse.Result.COOLDOWN || book.getCount() != 1) return "a second reading inside the cooldown must do nothing";
        clearCooldowns(p);
        int before = SkillTreeApi.sp(p);
        r = BookUse.readSkillBook(p, book);
        boolean wantsSp = SkillTreeApi.duplicateBookGivesSp() && route.duplicateSp() > 0;
        if (wantsSp) {
            if (r != BookUse.Result.DUPLICATE_SP) return "a copy of a known route should become Skill Points, got " + r;
            if (SkillTreeApi.sp(p) <= before) return "a copy of a known route gave no Skill Points";
            if (!book.isEmpty()) return "the copy should have been used up";
        } else {
            if (r != BookUse.Result.DUPLICATE_REFUSED) return "a copy should be refused by the setting, got " + r;
            if (book.getCount() != 1 || SkillTreeApi.sp(p) != before) return "a refused copy must keep the book and give nothing";
        }
        // creative: the book is kept even when it unlocks
        tree.removeRoute(route.id());
        clearCooldowns(p);
        p.getAbilities().instabuild = true;
        ItemStack kept = SkillBookItem.stackFor(route);
        r = BookUse.readSkillBook(p, kept);
        p.getAbilities().instabuild = false;
        if (r != BookUse.Result.UNLOCKED || kept.getCount() != 1) return "a creative reader must unlock and keep the book, got " + r + " left=" + kept.getCount();
        // the factories round trip
        if (!route.book().equals(SkillItems.bookIdOf(SkillBookItem.stackFor(route))) || RouteRegistry.byBook(route.book()) != route) return "the stack factory and the registry disagree about " + route.id();
        said.append("Skill Book of ").append(route.id()).append(" ok (copy ").append(wantsSp ? "became " + route.duplicateSp() + " SP" : "refused").append("); ");
        return null;
    }

    private static SkillRoute firstRoute() {
        for (SkillRoute r : RouteRegistry.all()) if (r.book() != null && !r.book().isEmpty()) return r;
        return null;
    }

    private static void clearCooldowns(ServerPlayer p) {
        p.getCooldowns().removeCooldown(SkillItems.SKILL_BOOK.get());
        p.getCooldowns().removeCooldown(SkillItems.SP_BOOK.get());
    }

    // =================================================================================================== tree.items.loot
    private static SelfTests.Result loot(MinecraftServer server) {
        ServerLevel mega = SelfTests.mega(server);
        if (mega == null) return fail("no mega dimension");
        if (!LootConfig.get(LootConfig.ENABLED)) return ok("skipped: the skill-book drops are switched off (skill_books.enabled)");
        if (!NeoForgeRegistries.GLOBAL_LOOT_MODIFIER_SERIALIZERS.containsKey(ResourceLocation.fromNamespaceAndPath("nusmp", "skill_books"))) {
            return fail("the loot modifier type nusmp:skill_books is not registered");
        }
        MegaData data = MegaData.get(server);
        BlockPos abyssal = positionIn(data, Polarity.ABYSSAL), holy = positionIn(data, Polarity.HOLY);
        BlockPos any = abyssal != null ? abyssal : holy != null ? holy : BlockPos.ZERO.above(100);
        ServerPlayer killer = SelfTests.player(mega, KILLER, any.getX(), any.getY(), any.getZ());
        ServerLevel overworld = server.overworld();
        LootRules.Chances always = new LootRules.Chances(1, 1, 1, 1, 3, false);
        LootRules.Chances never = new LootRules.Chances(0, 0, 0, 0, 0, false);
        boolean routes = !RouteRegistry.all().isEmpty() && RouteRegistry.all().stream().anyMatch(r -> r.lootWeight() > 0);
        StringBuilder said = new StringBuilder();

        // 1. the modifier's own entry point, on made-up kills (it must ADD beside what the table gave, and only where the rule says)
        if (abyssal != null) {
            Zombie z = zombieAt(mega, abyssal);
            ObjectArrayList<ItemStack> loot = withBread();
            int added = SkillBookLootModifier.add(loot, killContext(mega, z, killer), always, new Random(3)::nextDouble);
            if (added < 1 || loot.size() != 1 + added) return fail("an Abyssal monster killed by a player with every chance at 1 added " + added + " stacks (list size " + loot.size() + ")");
            if (!loot.get(0).is(Items.BREAD)) return fail("the modifier replaced or reordered the loot the table gave");
            int routeBooks = 0, primers = 0;
            for (int i = 1; i < loot.size(); i++) {
                if (loot.get(i).getItem() instanceof SkillBookItem) routeBooks++;
                else if (loot.get(i).getItem() instanceof SPBookItem) primers++;
                else return fail("the modifier added something that is no book: " + loot.get(i));
            }
            if (primers != 1 || routeBooks != (routes ? 1 : 0)) return fail("with every chance at 1 an Abyssal monster should add 1 SP Book and " + (routes ? 1 : 0) + " route book, got " + primers + " and " + routeBooks);
            if (SkillBookLootModifier.add(withBread(), killContext(mega, z, killer), never, new Random(3)::nextDouble) != 0) return fail("with every chance at 0 the modifier still added a book");
            if (SkillBookLootModifier.add(withBread(), killContext(mega, z, null), always, new Random(3)::nextDouble) != 0) return fail("a death no player caused still dropped a book");
            Zombie servant = zombieAt(mega, abyssal);
            servant.addTag("nusmp_servant");
            if (SkillBookLootModifier.add(withBread(), killContext(mega, servant, killer), always, new Random(3)::nextDouble) != 0) return fail("a servant's death dropped a book");
            Zombie boss = zombieAt(mega, abyssal);
            boss.addTag("nusmp_peak_boss");
            if (SkillBookLootModifier.add(withBread(), killContext(mega, boss, killer), always, new Random(3)::nextDouble) != 0) return fail("a boss (not the Guardian) dropped a book through the monster path");
            Zombie guardian = zombieAt(mega, abyssal);
            guardian.addTag(LabyrinthGuardianEntity.SECTOR_TAG);
            if (SkillBookLootModifier.add(withBread(), killContext(mega, guardian, killer), always, new Random(3)::nextDouble) != 0) return fail("the modifier acted on the Guardian, which is the death listener's");
            LivingEntity cow = EntityType.COW.create(mega);
            if (cow != null) {
                cow.setPos(abyssal.getX() + 0.5, abyssal.getY(), abyssal.getZ() + 0.5);
                if (SkillBookLootModifier.add(withBread(), killContext(mega, cow, killer), always, new Random(3)::nextDouble) != 0) return fail("a harmless animal dropped a book");
            }
            said.append("Abyssal kill adds ").append(added).append(" (route book ").append(routeBooks).append(", SP ").append(primers).append("); ");
        } else {
            said.append("no Abyssal sector exists in this world yet, so the live Abyssal path was not driven (the rule itself is unit tested); ");
        }
        if (holy != null) {
            Zombie z = zombieAt(mega, holy);
            if (SkillBookLootModifier.add(withBread(), killContext(mega, z, killer), always, new Random(4)::nextDouble) != 0) return fail("a monster of the HOLY road dropped a book");
            said.append("Holy road adds nothing; ");
        }
        Zombie outside = zombieAt(overworld, BlockPos.ZERO.above(100));
        ServerPlayer overworldKiller = SelfTests.player(overworld, KILLER + "_ow", 0, 100, 0);
        if (SkillBookLootModifier.add(withBread(), killContext(overworld, outside, overworldKiller), always, new Random(5)::nextDouble) != 0) return fail("a monster of the Overworld dropped a book");
        said.append("Overworld adds nothing; ");

        // 2. the whole loot pipeline with the real odds: the codec, the JSON type and the global list are exercised
        if (abyssal != null) {
            LootRules.Chances real = LootConfig.chances();
            LootTable table = madeUpEntityTable();
            Zombie z = zombieAt(mega, abyssal);
            RandomSource random = RandomSource.create(62_000_1L);
            int n = 6000, sp = 0, books = 0, bread = 0;
            for (int i = 0; i < n; i++) {
                ObjectArrayList<ItemStack> got = table.getRandomItems(killParams(mega, z, killer), random);
                for (ItemStack s : got) {
                    if (s.is(Items.BREAD)) bread++;
                    else if (s.getItem() instanceof SPBookItem) sp++;
                    else if (s.getItem() instanceof SkillBookItem) books++;
                }
            }
            if (bread != n) return fail("the pipeline lost the table's own loot: " + bread + " breads in " + n + " rolls");
            double ps = real.abyssalSp(), sigma = Math.sqrt(n * ps * (1 - ps));
            if (Math.abs(sp - n * ps) > 6 * sigma + 3) {
                return fail(sp + " SP Books in " + n + " Abyssal kills through the loot pipeline, the chance says " + String.format(Locale.ROOT, "%.1f", n * ps) + " +- " + String.format(Locale.ROOT, "%.1f", 6 * sigma)
                        + (sp == 0 && ps > 0 ? " (nothing at all: is nusmp:skill_books named in data/neoforge/loot_modifiers/global_loot_modifiers.json?)" : ""));
            }
            double pb = routes ? real.abyssalBook() : 0.0, sb = Math.sqrt(n * pb * (1 - pb));
            if (Math.abs(books - n * pb) > 6 * sb + 3) return fail(books + " route books in " + n + " Abyssal kills, the chance says " + String.format(Locale.ROOT, "%.1f", n * pb));
            said.append("pipeline ").append(n).append(" Abyssal kills: ").append(sp).append(" SP Books (expect ").append(String.format(Locale.ROOT, "%.0f", n * ps)).append("), ").append(books).append(" route books (expect ")
                    .append(String.format(Locale.ROOT, "%.0f", n * pb)).append("); ");
            Zombie ow = zombieAt(overworld, BlockPos.ZERO.above(100));
            int stray = 0;
            for (int i = 0; i < 1500; i++) {
                for (ItemStack s : table.getRandomItems(killParams(overworld, ow, overworldKiller), random)) if (s.getItem() instanceof SkillBookItem || s.getItem() instanceof SPBookItem) stray++;
            }
            if (stray != 0) return fail(stray + " books fell in 1500 Overworld kills through the pipeline");
        }

        // 3. the materialiser and the Guardian's death listener (the Guardian rolls no table, so this is its only door)
        List<ItemStack> made = LootDrops.materialize(new LootRules.Plan(true, 2, true), new Random(6)::nextDouble);
        int madePrimers = 0, madeBooks = 0;
        for (ItemStack s : made) {
            if (s.getItem() instanceof SPBookItem) madePrimers++;
            else if (s.getItem() instanceof SkillBookItem) madeBooks++;
        }
        if (madePrimers != 2 || madeBooks != (routes ? 1 : 0)) return fail("a plan of one route book and two SP Books made " + madeBooks + " and " + madePrimers);
        if (!mega.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
            return ok(said + "the doMobLoot game rule is off in this world, so the Guardian's death listener (which honours it) was not driven");
        }
        Zombie guardian = zombieAt(mega, any);
        guardian.addTag(LabyrinthGuardianEntity.SECTOR_TAG);
        DamageSource byPlayer = mega.damageSources().playerAttack(killer);
        List<ItemEntity> drops = new ArrayList<>();
        int dropped = BossBookDrops.process(new LivingDropsEvent(guardian, byPlayer, drops, true), new Random(7)::nextDouble, always, new LootVictims.Site(Polarity.ABYSSAL, -10));
        if (dropped < 1 || drops.size() != dropped) return fail("the Guardian of an Abyssal boss floor dropped " + dropped + " stacks (" + drops.size() + " item entities)");
        List<ItemEntity> none = new ArrayList<>();
        if (BossBookDrops.process(new LivingDropsEvent(guardian, byPlayer, none, true), new Random(7)::nextDouble, always, new LootVictims.Site(Polarity.HOLY, 10)) != 0) return fail("the Guardian of a Holy boss floor dropped a book with the Holy switch off");
        if (BossBookDrops.process(new LivingDropsEvent(guardian, byPlayer, none, true), new Random(7)::nextDouble, always, new LootVictims.Site(Polarity.ABYSSAL, -7)) != 0) return fail("a Guardian of a floor that is not a multiple of ten dropped a book");
        if (BossBookDrops.process(new LivingDropsEvent(guardian, mega.damageSources().generic(), none, true), new Random(7)::nextDouble, always, new LootVictims.Site(Polarity.ABYSSAL, -10)) != 0) return fail("a Guardian no player killed dropped a book");
        Zombie plain = zombieAt(mega, any);
        if (BossBookDrops.process(new LivingDropsEvent(plain, byPlayer, none, true), new Random(7)::nextDouble, always, new LootVictims.Site(Polarity.ABYSSAL, -10)) != 0) return fail("a monster without the Guardian tag dropped through the Guardian's listener");
        guardian.getPersistentData().putInt(LabyrinthGuardianEntity.SECTOR_KEY, -1);
        if (BossBookDrops.process(new LivingDropsEvent(guardian, byPlayer, none, true), new Random(7)::nextDouble, always, null) != 0) return fail("a test Guardian that belongs to no sector dropped a book");
        said.append("Guardian listener: ").append(dropped).append(" stacks on an Abyssal boss floor, none on a Holy one, an ordinary floor, a non-player kill or a Guardian without a sector");
        return ok(said.toString());
    }

    // ------------------------------------------------------------------------------------------------ pieces
    /** A cell inside the first sector of a road that has rooms, or null when the road has none yet. */
    private static BlockPos positionIn(MegaData data, Polarity road) {
        for (Sector s : data.sectors()) {
            if (s.polarity == road && !s.rooms.isEmpty() && s.state != Sector.State.FAILED && s.state != Sector.State.GENERATING) return s.rooms.get(0).center();
        }
        return null;
    }

    private static ObjectArrayList<ItemStack> withBread() {
        ObjectArrayList<ItemStack> loot = new ObjectArrayList<>();
        loot.add(new ItemStack(Items.BREAD));
        return loot;
    }

    private static Zombie zombieAt(ServerLevel level, BlockPos at) {
        Zombie z = EntityType.ZOMBIE.create(level);
        z.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return z;
    }

    private static LootParams killParams(ServerLevel level, LivingEntity victim, ServerPlayer killer) {
        LootParams.Builder b = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, victim)
                .withParameter(LootContextParams.ORIGIN, victim.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, killer == null ? level.damageSources().generic() : level.damageSources().playerAttack(killer))
                .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, killer)
                .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, killer);
        if (killer != null) b.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer).withLuck(killer.getLuck());
        return b.create(LootContextParamSets.ENTITY);
    }

    private static LootContext killContext(ServerLevel level, LivingEntity victim, ServerPlayer killer) {
        return new LootContext.Builder(killParams(level, victim, killer)).withOptionalRandomSource(RandomSource.create(99L)).create(Optional.empty());
    }

    /** A loot table that is in no registry: one bread per roll, the entity parameter set, and an id of its own (NeoForge hands the id to the modifiers). */
    private static LootTable madeUpEntityTable() {
        LootTable table = LootTable.lootTable().setParamSet(LootContextParamSets.ENTITY)
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1f)).add(LootItem.lootTableItem(Items.BREAD))).build();
        table.setLootTableId(TABLE_ID);
        return table;
    }
}
