package com.newuniverse.nusmp.labyrinth.loot;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Why: the loot cannot be played by its author, so the CI server smoke test runs this in the real game: all eighty items exist, every food is a real restorative (nutrition at
 * least one, an effect, no hunger effect), every piece of gear makes a pre-rolled stack whose weapon damage sits inside its grade and whose armour keeps its material's armour,
 * a roll survives the save codec, the global loot modifier drops loot in a dungeon level and NOTHING for a vanilla monster outside it (and for a monster of the mod, by the
 * config), never more than the cap and never a duplicate, the anvil does its three jobs, the trinket bridge applies and removes its modifiers, and the recipes of the
 * recipe manager include the crafted uses. Key {@code loot.functional}, run by {@code /multiverse mega selftest}.
 */
final class LootSelfTests {
    private LootSelfTests() {}

    static void register() { SelfTests.register("loot.functional", LootSelfTests::functional); }

    private static SelfTests.Result fail(String detail) { return SelfTests.Result.fail(detail); }

    private static SelfTests.Result functional(MinecraftServer server) {
        LootRoster roster = LootRoster.get();
        String items = items(roster);
        if (items != null) return fail(items);
        String foods = foods(roster);
        if (foods != null) return fail(foods);
        String gear = gear(roster, server);
        if (gear != null) return fail(gear);
        ServerLevel mega = SelfTests.mega(server);
        if (mega == null) return fail("no mega dimension");
        String drops = drops(server, mega);
        if (drops != null) return fail(drops);
        String anvil = anvil(mega);
        if (anvil != null) return fail(anvil);
        String bridge = bridge(mega);
        if (bridge != null) return fail(bridge);
        String recipes = recipes(server);
        if (recipes != null) return fail(recipes);
        return SelfTests.Result.ok(roster.ids().size() + " items registered, " + roster.foods().size() + " foods restorative and hunger-safe, " + (roster.weapons().size() + roster.armour().size() + roster.trinkets().size())
                + " gear pieces roll and keep their grade, the modifier drops in the dungeon and not outside it, the anvil, the trinket bridge and the recipes work");
    }

    // ---------------------------------------------------------------- items
    private static String items(LootRoster r) {
        if (!r.problem().isEmpty()) return r.problem();
        if (r.ids().size() < 80) return "the roster lists only " + r.ids().size() + " items";
        for (String id : r.ids()) {
            Item item = LootItems.get(id);
            if (item == null) return id + " is not registered";
            if (!item.getDescriptionId().equals("item.nusmp." + id)) return id + " has the description id " + item.getDescriptionId();
        }
        if (r.materials().size() < 24) return "fewer than 24 monster parts";
        return null;
    }

    private static String foods(LootRoster r) {
        for (LootRoster.FoodDef f : r.foods()) {
            ItemStack s = new ItemStack(LootItems.get(f.id()));
            FoodProperties fp = s.get(DataComponents.FOOD);
            if (fp == null) return f.id() + " has no food component";
            if (fp.nutrition() < 1) return f.id() + " feeds nothing";
            if (fp.saturation() < 0.0F) return f.id() + " has negative saturation";
            if (!fp.canAlwaysEat()) return f.id() + " cannot always be eaten";
            if (fp.effects().isEmpty() && !f.elixir()) return f.id() + " has no buff";
            for (FoodProperties.PossibleEffect e : fp.effects()) {
                boolean refuses = e.effect().getEffect().equals(MobEffects.HUNGER); if (refuses) return f.id() + " carries the hunger effect";
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- gear
    private static String gear(LootRoster r, MinecraftServer server) {
        for (String id : LootItems.gearIds()) {
            Item item = LootItems.get(id);
            ItemStack def = item.getDefaultInstance();
            GearRoll roll = GearStacks.rollOf(def);
            if (roll == null) return id + " has no roll on its default instance";
            if (roll.lines().size() < 2 || roll.lines().size() > 4) return id + " rolled " + roll.lines().size() + " lines";
            LootGear g = (LootGear) item;
            for (LootRarity rar : LootRarity.values()) {
                ItemStack s = GearStacks.create(item, rar, 99L + id.hashCode(), 150);
                GearRoll gr = GearStacks.rollOf(s);
                if (gr == null || gr.rarity() != rar) return id + " lost its rarity " + rar;
                ItemAttributeModifiers mods = s.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
                if (g.gearKind() == GearKind.WEAPON) {
                    double[] dmg = {0};
                    mods.forEach(EquipmentSlotGroup.MAINHAND, (a, m) -> {
                        if (a.equals(Attributes.ATTACK_DAMAGE)) dmg[0] += m.amount();
                    });
                    double total = dmg[0] + GearStacks.PLAYER_DAMAGE;
                    int[] range = com.newuniverse.nusmp.relics.GradeDamage.range(rar.grade());
                    if (total < range[0] - 1e-6 || total > range[1] + 1e-6) return id + " (" + rar + ") hits for " + total + ", outside its grade " + range[0] + ".." + range[1];
                } else if (g.gearKind() == GearKind.ARMOUR) {
                    double[] armor = {0};
                    mods.forEach(g.slotGroup(), (a, m) -> {
                        if (a.equals(Attributes.ARMOR)) armor[0] += m.amount();
                    });
                    LootRoster.ArmourDef ad = r.armourPiece(id);
                    if (armor[0] < ad.defense() - 1e-6) return id + " lost its material's armour: " + armor[0] + " of " + ad.defense();
                } else if (!mods.modifiers().isEmpty()) {
                    return id + " (a trinket) carries vanilla modifiers that nothing would apply";
                }
                Optional<Tag> enc = LootComponents.CODEC.encodeStart(NbtOps.INSTANCE, gr).result();
                if (enc.isEmpty()) return id + " does not encode";
                Optional<GearRoll> back = LootComponents.CODEC.parse(NbtOps.INSTANCE, enc.get()).result();
                if (back.isEmpty() || !back.get().equals(gr)) return id + " does not survive the save codec";
            }
            ItemStack bare = new ItemStack(item);
            if (!GearStacks.ensureRolled(bare, 40, 5L) || !bare.has(LootComponents.GEAR_ROLL.get())) return id + " is not rolled when it first sits in an inventory";
        }
        return null;
    }

    // ---------------------------------------------------------------- the modifier
    private static String drops(MinecraftServer server, ServerLevel mega) {
        FakePlayer killer = SelfTests.player(mega, "loot_killer", 0.5, 70, 0.5);
        ServerLevel over = server.overworld();
        int inDungeon = 0, outside = 0, maxPer = 0;
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 700; i++) {
            ObjectArrayList<ItemStack> loot = roll(mega, EntityType.ZOMBIE, killer);
            if (loot == null) return "no loot context could be made";
            inDungeon += loot.size();
            maxPer = Math.max(maxPer, loot.size());
            Set<String> ids = new HashSet<>();
            for (ItemStack s : loot) {
                if (!ids.add(BuiltInRegistries.ITEM.getKey(s.getItem()).getPath())) return "a duplicate in one kill's loot";
                seen.add(BuiltInRegistries.ITEM.getKey(s.getItem()).getPath());
            }
            ObjectArrayList<ItemStack> away = roll(over, EntityType.ZOMBIE, killer);
            outside += away == null ? 0 : away.size();
        }
        if (inDungeon == 0) return "700 zombies in the dungeon dropped nothing";
        if (outside != 0) return "a vanilla zombie outside the dungeon dropped " + outside + " loot items";
        if (maxPer > LootConfig.get(LootConfig.MAX_PER_KILL)) return "a kill dropped " + maxPer + " items, over the cap";
        for (String id : seen) {
            LootTables.Kind k = LootDrops.tables().kindOf(id);
            if (k == null) return id + " dropped but is in no table";
        }
        return null;
    }

    private static ObjectArrayList<ItemStack> roll(ServerLevel level, EntityType<?> type, FakePlayer killer) {
        net.minecraft.world.entity.Entity e = type.create(level);
        if (!(e instanceof LivingEntity mob)) return null;
        mob.moveTo(0.5, 70, 0.5);
        DamageSource src = level.damageSources().playerAttack(killer);
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY, mob).withParameter(LootContextParams.ORIGIN, new Vec3(0.5, 70, 0.5))
                .withParameter(LootContextParams.DAMAGE_SOURCE, src).withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer).create(LootContextParamSets.ENTITY);
        LootContext ctx = new LootContext.Builder(params).create(Optional.empty());
        ObjectArrayList<ItemStack> loot = new ObjectArrayList<>();
        LootDrops.add(loot, ctx);
        return loot;
    }

    // ---------------------------------------------------------------- the anvil
    private static String anvil(ServerLevel level) {
        FakePlayer p = SelfTests.player(level, "loot_smith", 0.5, 70, 0.5);
        Item blade = LootItems.get("shadowsteel_katana"), shard = LootItems.get("shadow_core_shard"), golem = LootItems.get("golem_core"), sac = LootItems.get("venom_sac");
        if (blade == null || shard == null || golem == null || sac == null) return "an anvil item is missing";
        ItemStack sword = GearStacks.create(blade, LootRarity.RARE, 11L, 60);
        int before = GearStacks.rollOf(sword).lines().size();
        AnvilUpdateEvent t = new AnvilUpdateEvent(sword, new ItemStack(shard), "", 0L, p);
        AnvilUses.onAnvil(t);
        GearRoll after = t.getOutput().isEmpty() ? null : GearStacks.rollOf(t.getOutput());
        if (after == null || !after.tempered() || after.lines().size() != before + 1) return "the Shadow Core Shard did not temper the blade";
        AnvilUpdateEvent again = new AnvilUpdateEvent(t.getOutput(), new ItemStack(shard), "", 0L, p);
        AnvilUses.onAnvil(again);
        if (!again.getOutput().isEmpty()) return "a blade was tempered twice";
        ItemStack worn = sword.copy();
        worn.setDamageValue(worn.getMaxDamage() / 2);
        AnvilUpdateEvent mend = new AnvilUpdateEvent(worn, new ItemStack(golem), "", 0L, p);
        AnvilUses.onAnvil(mend);
        if (mend.getOutput().isEmpty() || mend.getOutput().getDamageValue() != 0) return "the Golem Core did not mend the blade";
        AnvilUpdateEvent coat = new AnvilUpdateEvent(sword, new ItemStack(sac), "", 0L, p);
        AnvilUses.onAnvil(coat);
        if (coat.getOutput().isEmpty() || GearTooltip.venom(coat.getOutput()) != AnvilUses.VENOM_STRIKES) return "the Venom Sac did not coat the blade";
        AnvilUpdateEvent none = new AnvilUpdateEvent(new ItemStack(net.minecraft.world.item.Items.STICK), new ItemStack(shard), "", 0L, p);
        AnvilUses.onAnvil(none);
        if (!none.getOutput().isEmpty()) return "the anvil changed an unrelated pair";
        return null;
    }

    // ---------------------------------------------------------------- the trinkets
    private static String bridge(ServerLevel level) {
        FakePlayer p = SelfTests.player(level, "loot_wearer", 0.5, 70, 0.5);
        try {
            Item ring = LootItems.get("orc_tusk_ring"), amulet = LootItems.get("tide_pearl_amulet");
            ItemStack a = GearStacks.create(ring, LootRarity.EPIC, 3L, 80), b = GearStacks.create(amulet, LootRarity.EPIC, 4L, 80);
            double hp0 = p.getAttributeValue(Attributes.MAX_HEALTH);
            GearRoll ra = GearStacks.rollOf(a), rb = GearStacks.rollOf(b);
            TrinketBridge.apply(p, List.of(a, b));
            double want = ra.sum(StatKind.MAX_HEALTH) + rb.sum(StatKind.MAX_HEALTH);
            double hp1 = p.getAttributeValue(Attributes.MAX_HEALTH);
            if (Math.abs(hp1 - hp0 - want) > 1e-6) return "worn trinkets gave " + (hp1 - hp0) + " max health, expected " + want;
            TrinketBridge.apply(p, List.of());
            if (Math.abs(p.getAttributeValue(Attributes.MAX_HEALTH) - hp0) > 1e-6) return "taking the trinkets off left a modifier behind";
            return null;
        } finally {
            TrinketBridge.clear(p);
        }
    }

    // ---------------------------------------------------------------- the recipes
    private static String recipes(MinecraftServer server) {
        Set<String> results = new HashSet<>();
        for (RecipeHolder<?> h : server.getRecipeManager().getRecipes()) {
            ItemStack out = h.value().getResultItem(server.registryAccess());
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(out.getItem());
            if (id != null && "nusmp".equals(id.getNamespace()) && LootItems.isLoot(id.getPath())) results.add(id.getPath());
        }
        List<String> missing = new ArrayList<>();
        for (String want : new String[]{"goblin_ear_ward", "shadowsteel_ingot", "bone_arrow", "drakescale_vest", "mana_elixir", "orc_tusk_ring", "bone_lance", "slime_jelly_pudding"}) if (!results.contains(want)) missing.add(want);
        if (!missing.isEmpty()) return "recipes missing for " + missing + " (the recipe manager knows " + results.size() + " loot results)";
        if (results.size() < 30) return "only " + results.size() + " loot recipes loaded";
        return null;
    }
}
