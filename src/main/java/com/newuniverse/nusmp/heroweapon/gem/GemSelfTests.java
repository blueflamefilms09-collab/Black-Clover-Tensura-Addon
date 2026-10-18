package com.newuniverse.nusmp.heroweapon.gem;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Why: nothing of the acquisition can be played by its author, so the CI server smoke test runs two self-tests in the real game ({@code /multiverse mega selftest},
 * keys {@code hero.gem} and {@code hero.gem.ritual}; the smoke script expects the prefix {@code hero}). {@code hero.gem} checks what a unit test cannot: the nineteen
 * items are registered under the right ids with the right weapon each (seventeen distinct Seals), the nineteen recipes were loaded by the real recipe manager,
 * give the right result and take exactly the ingredients the table in {@link GemRecipes} says (so a vanilla item id that does not exist, which makes the game drop
 * a recipe silently, fails here), and the tolerant reader of the core's registry file reads the core's own layout and the other shapes it accepts, and nothing from noise. {@code hero.gem.ritual} drives the whole
 * Choosing with a stand-in player through the same entry points the item calls: a ritual that breaks by a step away, by a blow, by a covered sky and by letting
 * go; a ritual that completes and binds a Seal's weapon and spends the gem; a gem that refuses because the player already carries a weapon and is kept; and the
 * Holy Calling Gem that binds one of the free Legendary Weapons (or, when none is free, keeps the gem). The facts that depend on the blocks round the stand-in
 * player are handed in, so the test does not depend on where the world's spawn is. Everything the test bound is released again and its records forgotten.
 */
final class GemSelfTests {
    private GemSelfTests() {}

    private static final String PLAYER = "hero_gem_tester";

    static void register() {
        SelfTests.register("hero.gem", GemSelfTests::items);
        SelfTests.register("hero.gem.ritual", GemSelfTests::ritual);
    }

    private static SelfTests.Result fail(List<String> bad) {
        return SelfTests.Result.fail(bad.size() + " problem(s): " + String.join(" | ", bad.subList(0, Math.min(5, bad.size()))));
    }

    // ================================================================ items, recipes, the registry reader
    private static SelfTests.Result items(MinecraftServer server) {
        List<String> bad = new ArrayList<>();

        // the items
        Set<HeroKind> kinds = new HashSet<>();
        for (String id : GemCatalog.allItemIds()) {
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath("nusmp", id);
            if (!BuiltInRegistries.ITEM.containsKey(rl)) {
                bad.add("no item " + rl);
                continue;
            }
            Item item = BuiltInRegistries.ITEM.get(rl);
            if (id.equals(GemCatalog.HOLY_ID)) {
                if (!(item instanceof CallingGemItem g) || g.kind() != null) bad.add(id + " is not the Holy Calling Gem");
            } else if (id.equals(GemCatalog.FRAGMENT_ID)) {
                if (!(item instanceof GemFragmentItem)) bad.add(id + " is not the Gem Fragment");
            } else if (!(item instanceof CallingGemItem g) || g.kind() == null || !GemCatalog.sealId(g.kind()).equals(id)) {
                bad.add(id + " is not the Seal it is named after");
            } else if (!kinds.add(g.kind())) {
                bad.add("two items call " + g.kind());
            }
        }
        if (kinds.size() != GemCatalog.sealKinds().size()) bad.add(kinds.size() + " distinct Seals, expected " + GemCatalog.sealKinds().size());
        for (HeroKind k : kinds) if (k.isHoly()) bad.add("a Seal calls the Holy Weapon " + k);

        // the recipes, as the real recipe manager loaded them
        int recipes = 0;
        for (String id : GemCatalog.allItemIds()) {
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath("nusmp", id);
            Optional<RecipeHolder<?>> holder = server.getRecipeManager().byKey(rl);
            if (holder.isEmpty()) {
                bad.add("recipe " + rl + " was not loaded (an ingredient id the game does not know, or a broken file; see the log)");
                continue;
            }
            recipes++;
            Recipe<?> recipe = holder.get().value();
            Item item = BuiltInRegistries.ITEM.containsKey(rl) ? BuiltInRegistries.ITEM.get(rl) : null;
            ItemStack result = recipe.getResultItem(server.registryAccess());
            if (item == null || !result.is(item) || result.getCount() != 1) bad.add("recipe " + id + " does not give one " + id);
            List<String> expected = expectedIngredients(id);
            if (expected == null) {
                bad.add("no table row for " + id);
                continue;
            }
            String mismatch = matchIngredients(recipe, expected);
            if (mismatch != null) bad.add("recipe " + id + ": " + mismatch);
        }

        // the reader of the core's registry file
        String reader = checkRegistryReader();
        if (reader != null) bad.add(reader);

        if (!bad.isEmpty()) return fail(bad);
        return SelfTests.Result.ok(GemCatalog.allItemIds().size() + " items, " + recipes + " recipes with the table's ingredients, " + kinds.size() + " distinct Seals");
    }

    /** The registry ids a recipe must take, one per slot, from the table in {@link GemRecipes}; null for an id that is not one of ours. */
    private static List<String> expectedIngredients(String itemId) {
        if (itemId.equals(GemCatalog.HOLY_ID)) return GemRecipes.holySlotIds();
        if (itemId.equals(GemCatalog.FRAGMENT_ID)) return GemRecipes.fragmentSlotIds();
        for (GemRecipes.Seal s : GemRecipes.seals()) if (GemCatalog.sealId(s.kind()).equals(itemId)) return s.slotIds();
        return null;
    }

    /** Null when the recipe's non-empty ingredients are exactly the expected items (each ingredient used once), else what differs. */
    private static String matchIngredients(Recipe<?> recipe, List<String> expected) {
        List<Ingredient> have = new ArrayList<>();
        for (Ingredient i : recipe.getIngredients()) if (!i.isEmpty()) have.add(i);
        if (have.size() != expected.size()) return have.size() + " ingredients, the table says " + expected.size();
        List<Ingredient> free = new ArrayList<>(have);
        for (String id : expected) {
            ResourceLocation rl = ResourceLocation.parse(id);
            if (!BuiltInRegistries.ITEM.containsKey(rl)) return id + " is not an item of this game";
            ItemStack probe = new ItemStack(BuiltInRegistries.ITEM.get(rl));
            Ingredient hit = null;
            for (Ingredient i : free) {
                if (i.test(probe)) {
                    hit = i;
                    break;
                }
            }
            if (hit == null) return "nothing in the recipe takes " + id;
            free.remove(hit);
        }
        return null;
    }

    private static String checkRegistryReader() {
        UUID shield = UUID.nameUUIDFromBytes("gem-test-shield".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        UUID sword = UUID.nameUUIDFromBytes("gem-test-sword".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        UUID spear = UUID.nameUUIDFromBytes("gem-test-spear".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        UUID scythe1 = UUID.nameUUIDFromBytes("gem-test-scythe-1".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        UUID scythe2 = UUID.nameUUIDFromBytes("gem-test-scythe-2".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        CompoundTag root = new CompoundTag(), data = new CompoundTag(), holders = new CompoundTag(), byKind = new CompoundTag(), byHolder = new CompoundTag(), entry = new CompoundTag();
        // the layout the core saves: {holders: {<weapon id>: [<uuid text>, ...]}}
        ListTag shieldList = new ListTag(), scytheList = new ListTag();
        shieldList.add(StringTag.valueOf(shield.toString()));
        scytheList.add(StringTag.valueOf(scythe1.toString()));
        scytheList.add(StringTag.valueOf(scythe2.toString()));
        holders.put(HeroKind.SHIELD.itemId(), shieldList);
        holders.put(HeroKind.SCYTHE.itemId(), scytheList);
        data.put("holders", holders);
        entry.putString("kind", HeroKind.SWORD.itemId());                                      // a weapon id next to a uuid saved as text
        entry.putString("uuid", sword.toString());
        ListTag list = new ListTag();
        list.add(entry);
        data.put("entries", list);
        byHolder.putString(spear.toString(), HeroKind.SPEAR.itemId());                         // a holder's uuid mapping to a weapon id
        data.put("by_player", byHolder);
        byKind.putUUID(HeroKind.BOW.itemId(), shield);                                         // a weapon id mapping to a uuid saved as four ints
        data.put("single", byKind);
        root.put("data", data);
        Map<HeroKind, List<UUID>> read = GemHolders.parse(root);
        if (!List.of(shield).equals(read.get(HeroKind.SHIELD))) return "the registry reader missed the core's own layout (a weapon id and a list of uuid texts)";
        if (!List.of(scythe1, scythe2).equals(read.get(HeroKind.SCYTHE))) return "the registry reader lost one of two holders of a weapon";
        if (!List.of(sword).equals(read.get(HeroKind.SWORD))) return "the registry reader missed a kind / uuid pair";
        if (!List.of(spear).equals(read.get(HeroKind.SPEAR))) return "the registry reader missed a uuid mapped to a weapon id";
        if (!List.of(shield).equals(read.get(HeroKind.BOW))) return "the registry reader missed a weapon id mapped to four ints";
        if (read.size() != 5) return "the registry reader invented holders: " + read;
        if (!GemHolders.parse(new CompoundTag()).isEmpty()) return "the registry reader read holders out of an empty file";
        CompoundTag noise = new CompoundTag();
        noise.putString("kind", "not_a_weapon");
        noise.putString("uuid", "not-a-uuid");
        noise.putInt("count", 4);
        if (!GemHolders.parse(noise).isEmpty()) return "the registry reader made holders out of noise";
        return null;
    }

    // ================================================================ the Choosing, end to end
    private static SelfTests.Result ritual(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        ServerPlayer p = SelfTests.player(level, PLAYER, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
        UUID id = p.getUUID();
        List<String> bad = new ArrayList<>();
        String note = "";
        try {
            if (HeroBonds.has(p)) HeroBonds.release(p);
            GemChoosing.forget(id);

            HeroKind seal = null;
            String refusal = null;
            for (HeroKind k : GemCatalog.sealKinds()) {
                refusal = HeroBonds.refusal(p, k);
                if (refusal == null) {
                    seal = k;
                    break;
                }
            }
            GemRules.Facts good = new GemRules.Facts(true, false, seal != null, 4, false, false, false, true, 0.0, 16);

            // a ritual breaks by a step away, a blow, a covered sky, letting go
            GemRules.Denial d = GemChoosing.begin(p, seal, good);
            if (d != GemRules.Denial.NONE || !GemChoosing.running(id)) bad.add("a good start was refused: " + d);
            if (GemChoosing.step(p, 99.0, true) != GemRitual.Step.INTERRUPT || GemChoosing.running(id)) bad.add("a step away did not end the ritual");
            GemChoosing.begin(p, seal, good);
            GemChoosing.hurt(p);
            if (GemChoosing.step(p, 0.0, true) != GemRitual.Step.INTERRUPT || GemChoosing.running(id)) bad.add("a blow did not end the ritual");
            GemChoosing.begin(p, seal, good);
            if (GemChoosing.step(p, 0.0, false) != GemRitual.Step.INTERRUPT || GemChoosing.running(id)) bad.add("a covered sky did not end the ritual");
            GemChoosing.begin(p, seal, good);
            GemChoosing.released(p);
            if (GemChoosing.running(id)) bad.add("letting go did not end the ritual");
            GemRules.Denial closed = GemChoosing.begin(p, seal, new GemRules.Facts(false, false, seal != null, 4, false, false, false, true, 0.0, 16));
            if (closed != GemRules.Denial.ROUTE_CLOSED || GemChoosing.running(id)) bad.add("a closed route started a ritual");

            // a Seal completes, binds its weapon and is spent
            if (seal == null) {
                note = ", no Seal bound (every Seal is refused by a gate: " + refusal + ")";
            } else {
                CallingGemItem gem = GemItems.seal(seal);
                ItemStack stack = new ItemStack(gem);
                bad.addAll(runToCompletion(p, seal, good));
                ItemStack back = GemChoosing.finish(p, gem, stack);
                if (HeroBonds.kindOf(p) != seal) bad.add("the Seal of " + seal + " bound " + HeroBonds.kindOf(p));
                if (!back.isEmpty()) bad.add("the Seal was not spent");
                if (!GemChoosing.summonPending(id)) bad.add("the chosen weapon is not waiting to be put in the hand");
                GemChoosing.forget(id);

                // a gem that arrives while the player carries a weapon refuses and is kept
                HeroKind other = null;
                for (HeroKind k : GemCatalog.sealKinds()) if (k != seal && HeroBonds.refusal(p, k) == null) other = k;
                if (!GemChoosing.facts(p, other).bound()) bad.add("the facts do not know the player carries a weapon");
                if (other != null) {
                    GemRules.Facts lied = new GemRules.Facts(true, false, true, 0, false, false, false, true, 0.0, 16);
                    CallingGemItem gem2 = GemItems.seal(other);
                    ItemStack stack2 = new ItemStack(gem2);
                    bad.addAll(runToCompletion(p, other, lied));
                    GemChoosing.finish(p, gem2, stack2);
                    if (stack2.isEmpty() || HeroBonds.kindOf(p) != seal) bad.add("a gem answered a player who already carried a weapon");
                }
                HeroBonds.release(p);
                if (HeroBonds.has(p)) bad.add("the bond could not be released");
            }

            // the Holy Calling Gem
            boolean someFree = !GemChoosing.holyCandidates(p).isEmpty();
            GemRules.Facts holy = new GemRules.Facts(true, false, false, someFree ? 4 : 0, false, false, false, true, 0.0, 16);
            GemRules.Denial hd = GemChoosing.begin(p, null, holy);
            if (someFree) {
                if (hd != GemRules.Denial.NONE) {
                    bad.add("the Holy Calling Gem was refused although a Holy Weapon is free: " + hd);
                } else {
                    bad.addAll(runToCompletion(p, null, holy));
                    ItemStack stack = GemItems.holyStack();
                    GemChoosing.finish(p, GemItems.CALLING_GEM.get(), stack);
                    HeroKind got = HeroBonds.kindOf(p);
                    if (got == null) note += ", the Holy Calling Gem bound nothing although a Holy Weapon was free";
                    else if (!got.isHoly()) bad.add("the Holy Calling Gem bound " + got + ", which is not a Legendary Weapon");
                    else if (!stack.isEmpty()) bad.add("the Holy Calling Gem was not spent");
                }
            } else {
                if (hd != GemRules.Denial.NONE_FREE && hd != GemRules.Denial.GATE_REFUSED) bad.add("with no Holy Weapon free the gem said " + hd);
                note += ", every Holy Weapon is taken or refused: the refusal path was checked instead";
            }
        } finally {
            GemChoosing.forget(id);
            if (HeroBonds.has(p)) HeroBonds.release(p);
            GemChoosing.forget(id);
        }
        if (!bad.isEmpty()) return fail(bad);
        return SelfTests.Result.ok("the ritual breaks four ways, a Seal binds its weapon and is spent, a gem is kept when it must refuse, the Holy gem chooses among the free" + note);
    }

    /** Starts a ritual and channels it to its last tick; returns what went wrong (an empty list when it ran to COMPLETE). */
    private static List<String> runToCompletion(ServerPlayer p, HeroKind seal, GemRules.Facts facts) {
        List<String> bad = new ArrayList<>();
        GemRules.Denial d = GemChoosing.begin(p, seal, facts);
        if (d != GemRules.Denial.NONE) {
            bad.add("the ritual did not start: " + d);
            return bad;
        }
        int total = GemConfig.ritualTicks();
        for (int i = 1; i < total; i++) {
            if (GemChoosing.step(p, 0.0, true) != GemRitual.Step.CONTINUE) {
                bad.add("the ritual stopped early at tick " + i);
                return bad;
            }
        }
        if (GemChoosing.step(p, 0.0, true) != GemRitual.Step.COMPLETE) bad.add("the ritual did not complete on tick " + total);
        return bad;
    }
}
