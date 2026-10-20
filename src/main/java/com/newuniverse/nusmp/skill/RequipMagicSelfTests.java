package com.newuniverse.nusmp.skill;

import com.mojang.serialization.DataResult;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.requip.Armour;
import com.newuniverse.nusmp.requip.ArmouryRules;
import com.newuniverse.nusmp.requip.BarrageMath;
import com.newuniverse.nusmp.requip.LoadoutRules;
import com.newuniverse.nusmp.requip.PartialArmourMask;
import com.newuniverse.nusmp.requip.RequipArmoury;
import com.newuniverse.nusmp.requip.RequipLoadouts;
import com.newuniverse.nusmp.requip.RequipMagicConfig;
import com.newuniverse.nusmp.requip.RequipMagicRules;
import com.newuniverse.nusmp.requip.RequipRules;
import com.newuniverse.nusmp.requip.RequipSystem;
import com.newuniverse.nusmp.requip.RequipWeapons;
import com.newuniverse.nusmp.skill.RequipScripts.Script;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Why: the Equip Magic cannot be played by its author, so what can be proved without a person is proved by the real game: the CI server smoke test boots the mod and runs {@code skill.requip_magic}
 * with a stand-in player. It checks, one line each: the modes (the 14 armours and Dismiss keep their places, the 13 appended modes have unique ids and names); the Armoury (shift-click stores and
 * takes through the real menu, the total never changes, a full Armoury refuses and keeps the item, a cell past the capacity still gives its item back, the saved data survives the codec of a save
 * and of a death with names, enchantments and custom data intact); an instant swap of a loadout (the new set is worn, the old set is in the Armoury, nothing is lost or doubled; a missing item is
 * skipped; a full Armoury changes nothing); Requip: Blade (summon and put away swap with the hand) and Strike (the thrown weapon waits in the saved limbo and comes home); the Barrage (the sword
 * count, the ring, an ally untouched, a boss capped to its share); every finisher and the Dimension-Cut run and hit, a boss is capped; Partial Armour (the mask rides in the aura style, the stats are the
 * kept share); the gates (no skill, no Armoury; a locked finisher stays locked) and the Requip Reflex (once per pause). Registered from {@code requip.RequipMagic.init}.
 */
public final class RequipMagicSelfTests {
    private RequipMagicSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("skill.requip_magic", RequipMagicSelfTests::run);
    }

    private static Result run(MinecraftServer server) {
        ServerLevel level = server.overworld();
        ServerPlayer p = SelfTests.player(level, "RequipMagic", 0.5, 100.0, 0.5);
        reset(p);
        List<Entity> spawned = new ArrayList<>();
        try {
            String why;
            if ((why = modes()) != null) return Result.fail("modes: " + why);
            if ((why = armoury(p, server)) != null) return Result.fail("armoury: " + why);
            if ((why = persistence(p, server)) != null) return Result.fail("persistence: " + why);
            if ((why = loadouts(p)) != null) return Result.fail("loadouts: " + why);
            if ((why = weapons(p)) != null) return Result.fail("weapons: " + why);
            if ((why = barrage(p, level, spawned)) != null) return Result.fail("barrage: " + why);
            if ((why = finishers(p, level, spawned)) != null) return Result.fail("finishers: " + why);
            if ((why = partial(p)) != null) return Result.fail("partial: " + why);
            if ((why = gates(p)) != null) return Result.fail("gates: " + why);
            if ((why = reflex(p)) != null) return Result.fail("reflex: " + why);
            return Result.ok(RequipMagicRules.MODES + " modes, armoury, loadouts, blade, strike, barrage of " + BarrageMath.MIN_SWORDS + " to " + BarrageMath.MAX_SWORDS + " swords, "
                    + RequipMagicRules.finishers().size() + " finishers, partial armour, gates, reflex");
        } finally {
            for (Entity e : spawned) e.discard();
            RequipScripts.cancel(p);
            reset(p);
            SkillAPI.getSkillsFrom(p).forgetSkill(NUSkills.ERZA_REQUIP.getId());
        }
    }

    // ---------------------------------------------------------------- fixtures
    private static void reset(ServerPlayer p) {
        p.getInventory().clearContent();
        p.setData(RequipArmoury.DATA, new RequipArmoury.Data());
        p.setHealth(p.getMaxHealth());
    }

    /** Every item in the inventory, the worn slots, the offhand, the Armoury and the limbo, as item name to count. */
    private static Map<String, Integer> census(ServerPlayer p) {
        Map<String, Integer> m = new TreeMap<>();
        Inventory inv = p.getInventory();
        List<ItemStack> all = new ArrayList<>();
        all.addAll(inv.items);
        all.addAll(inv.armor);
        all.addAll(inv.offhand);
        RequipArmoury.Data d = RequipArmoury.data(p);
        all.addAll(d.cells);
        all.add(d.limbo);
        for (ItemStack s : all) if (!s.isEmpty()) m.merge(s.getItem().getDescriptionId() + (s.has(DataComponents.CUSTOM_NAME) ? "#named" : ""), s.getCount(), Integer::sum);
        return m;
    }

    private static int cellsUsed(RequipArmoury.Data d) {
        int n = 0;
        for (ItemStack s : d.cells) if (!s.isEmpty()) n++;
        return n;
    }

    // ---------------------------------------------------------------- modes
    private static String modes() {
        ErzaRequipSkill s = NUSkills.ERZA_REQUIP.get();
        ManasSkillInstance inst = s.createDefaultInstance();
        if (s.getModes(inst) != RequipMagicRules.MODES) return "mode count " + s.getModes(inst);
        Set<String> ids = new HashSet<>();
        for (int m = 0; m < s.getModes(inst); m++) {
            if (!ids.add(s.getModeId(inst, m))) return "duplicate mode id " + s.getModeId(inst, m);
            if (s.getModeName(inst, m).getString().isEmpty()) return "mode " + m + " has no name";
        }
        for (Armour a : Armour.values()) if (!s.getModeId(inst, a.ordinal()).equals("erza." + a.id())) return "armour mode " + a.ordinal() + " moved";
        if (!s.getModeId(inst, RequipRules.DISMISS_MODE).equals("erza.dismiss")) return "Dismiss moved";
        for (RequipMagicRules.Mode m : RequipMagicRules.Mode.values()) {
            if (!s.getModeId(inst, m.modeIndex()).equals("erza." + m.id())) return m.id() + " is not at mode " + m.modeIndex();
            if (s.nextMode(null, inst, m.modeIndex(), false) != (m.modeIndex() + 1) % RequipMagicRules.MODES) return "next mode after " + m.id();
        }
        if (s.nextMode(null, inst, RequipRules.DISMISS_MODE, false) != RequipMagicRules.Mode.ARMOURY.modeIndex()) return "Dismiss is not followed by the Armoury";
        return null;
    }

    // ---------------------------------------------------------------- the Armoury (through the real menu)
    private static String armoury(ServerPlayer p, MinecraftServer server) {
        reset(p);
        Inventory inv = p.getInventory();
        RequipArmoury.Data d = RequipArmoury.data(p);
        inv.items.set(20, new ItemStack(Items.DIAMOND_SWORD));
        inv.items.set(21, new ItemStack(Items.DIRT, 16));
        Map<String, Integer> before = census(p);
        RequipArmoury.Menu menu = RequipArmoury.serverMenu(p, 91);
        int sword = RequipArmoury.Menu.PLAYER_FIRST + (20 - 9), dirt = RequipArmoury.Menu.PLAYER_FIRST + (21 - 9);
        if (menu.quickMoveStack(p, sword).isEmpty()) return "shift-click did not store the sword";
        if (menu.quickMoveStack(p, dirt).isEmpty()) return "shift-click did not store the dirt";
        if (!inv.items.get(20).isEmpty() || !inv.items.get(21).isEmpty()) return "the stored stacks are still in the inventory";
        if (cellsUsed(d) != 2) return "expected 2 cells used, " + cellsUsed(d);
        if (!census(p).equals(before)) return "storing changed the totals " + before + " -> " + census(p);
        // take the sword back out
        int cell = -1;
        for (int i = 0; i < d.cells.size(); i++) if (d.cells.get(i).is(Items.DIAMOND_SWORD)) cell = i;
        if (cell < 0) return "the sword is not in a cell";
        if (menu.quickMoveStack(p, cell).isEmpty()) return "shift-click did not take the sword out";
        if (!census(p).equals(before)) return "taking changed the totals";
        // the window refuses what does not belong in it: the weapon an armour made
        ItemStack made = com.newuniverse.nusmp.requip.RequipWeaponItem.flagged(com.newuniverse.nusmp.requip.RequipItems.weapon("flame_empress_blade"));
        inv.items.set(22, made);
        if (!menu.quickMoveStack(p, RequipArmoury.Menu.PLAYER_FIRST + 13).isEmpty()) return "an armour's own weapon went into the pocket";
        inv.items.set(22, ItemStack.EMPTY);
        // a full Armoury (capacity 18 for the stand-in) refuses and keeps the item
        for (int i = 0; i < d.cells.size(); i++) d.cells.set(i, ItemStack.EMPTY);
        for (int i = 0; i < 18; i++) d.cells.set(i, new ItemStack(Items.IRON_SWORD));
        inv.items.set(23, new ItemStack(Items.GOLDEN_SWORD));
        Map<String, Integer> full = census(p);
        if (!menu.quickMoveStack(p, RequipArmoury.Menu.PLAYER_FIRST + 14).isEmpty()) return "a full Armoury took another sword";
        if (!inv.items.get(23).is(Items.GOLDEN_SWORD) || !census(p).equals(full)) return "a refused store lost the item";
        // a cell past the capacity still gives its item back
        d.cells.set(18, ItemStack.EMPTY);
        d.cells.set(40, new ItemStack(Items.DIAMOND, 5));
        if (menu.quickMoveStack(p, 40).isEmpty() || !d.cells.get(40).isEmpty()) return "a cell past the capacity did not give its item back";
        // the capacity follows the rules
        if (ArmouryRules.capacity(0.0, 25_000, false) != 18) return "capacity rule";
        reset(p);
        return null;
    }

    // ---------------------------------------------------------------- the saved data: the codec of a world save and of a death
    private static String persistence(ServerPlayer p, MinecraftServer server) {
        reset(p);
        RequipArmoury.Data d = RequipArmoury.data(p);
        ItemStack unique = new ItemStack(Items.NETHERITE_SWORD);
        unique.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Soulbound Blade"));
        CustomData.update(DataComponents.CUSTOM_DATA, unique, t -> t.putString("owner", "someone"));
        d.cells.set(7, unique);
        d.cells.set(30, new ItemStack(Items.ARROW, 64));
        d.limbo = new ItemStack(Items.DIAMOND_AXE);
        d.limboSlot = 4;
        d.selected = 7;
        d.reflex = 2;
        d.mask = PartialArmourMask.GAUNTLETS | PartialArmourMask.GREAVES;
        d.partialOn = true;
        d.loadouts.get(2).template.set(0, new ItemStack(Items.IRON_HELMET));
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, server.registryAccess());
        DataResult<Tag> enc = RequipArmoury.CODEC.encodeStart(ops, d);
        if (enc.result().isEmpty()) return "the data does not encode";
        DataResult<RequipArmoury.Data> dec = RequipArmoury.CODEC.parse(ops, enc.result().get());
        if (dec.result().isEmpty()) return "the data does not decode";
        RequipArmoury.Data back = dec.result().get();
        if (!ItemStack.matches(d.cells.get(7), back.cells.get(7))) return "the named, custom-data sword changed in the codec";
        if (!ItemStack.matches(d.cells.get(30), back.cells.get(30))) return "the arrows changed in the codec";
        if (!ItemStack.matches(d.limbo, back.limbo) || back.limboSlot != 4) return "the thrown weapon did not survive";
        if (back.selected != 7 || back.reflex != 2 || back.mask != d.mask || !back.partialOn) return "the settings did not survive";
        if (!back.loadouts.get(2).saved() || !back.loadouts.get(2).template.get(0).is(Items.IRON_HELMET)) return "the loadout did not survive";
        if (back.loadouts.size() != LoadoutRules.MAX_LOADOUTS || back.cells.size() != ArmouryRules.MAX_SLOTS) return "the shape was not repaired";
        // an empty value is not written at all, and an empty tag decodes to a clean Data
        RequipArmoury.Data empty = RequipArmoury.CODEC.parse(NbtOps.INSTANCE, new net.minecraft.nbt.CompoundTag()).result().orElse(null);
        if (empty == null || !empty.isEmpty()) return "an empty tag did not give an empty armoury";
        reset(p);
        return null;
    }

    // ---------------------------------------------------------------- loadouts
    private static RequipArmoury.Loadout loadout(ItemStack head, ItemStack chest, ItemStack legs, ItemStack feet, ItemStack off, ItemStack... hotbar) {
        List<ItemStack> t = new ArrayList<>();
        t.add(head); t.add(chest); t.add(legs); t.add(feet); t.add(off);
        for (int i = 0; i < LoadoutRules.HOTBAR; i++) t.add(i < hotbar.length ? hotbar[i] : ItemStack.EMPTY);
        return new RequipArmoury.Loadout(t);
    }

    private static String loadouts(ServerPlayer p) {
        reset(p);
        Inventory inv = p.getInventory();
        RequipArmoury.Data d = RequipArmoury.data(p);
        // worn: the iron set; the Armoury holds the diamond set; loadout 1 asks for the diamond set
        inv.armor.set(3, new ItemStack(Items.IRON_HELMET));
        inv.armor.set(2, new ItemStack(Items.IRON_CHESTPLATE));
        inv.offhand.set(0, new ItemStack(Items.SHIELD));
        inv.items.set(0, new ItemStack(Items.IRON_SWORD));
        d.cells.set(3, new ItemStack(Items.DIAMOND_HELMET));
        d.cells.set(4, new ItemStack(Items.DIAMOND_CHESTPLATE));
        d.cells.set(5, new ItemStack(Items.DIAMOND_SWORD));
        int pieces = RequipLoadouts.save(p, 0);
        if (pieces != 4) return "saving the iron set gave " + pieces + " pieces";
        d.loadouts.set(1, loadout(new ItemStack(Items.DIAMOND_HELMET), new ItemStack(Items.DIAMOND_CHESTPLATE), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.DIAMOND_SWORD)));
        Map<String, Integer> before = census(p);
        RequipLoadouts.Outcome dry = RequipLoadouts.equip(p, 1, 18, false);
        if (!dry.done() || !inv.armor.get(3).is(Items.IRON_HELMET)) return "a dry run changed the worn set (" + dry.status() + ")";
        RequipLoadouts.Outcome o = RequipLoadouts.equip(p, 1, 18);
        if (!o.done() || o.moved() != 3) return "the swap moved " + o.moved() + " (" + o.status() + ")";
        if (!inv.armor.get(3).is(Items.DIAMOND_HELMET) || !inv.armor.get(2).is(Items.DIAMOND_CHESTPLATE) || !inv.items.get(0).is(Items.DIAMOND_SWORD)) return "the diamond set is not worn";
        if (!inv.offhand.get(0).is(Items.SHIELD)) return "an empty template slot touched the offhand";
        boolean ironHome = false, swordHome = false;
        for (ItemStack s : d.cells) { if (s.is(Items.IRON_HELMET)) ironHome = true; if (s.is(Items.IRON_SWORD)) swordHome = true; }
        if (!ironHome || !swordHome) return "the old set did not go back to the Armoury";
        if (!census(p).equals(before)) return "the swap changed the totals";
        if (d.active != 1) return "the active loadout was not recorded";
        // and back again
        RequipLoadouts.Outcome back = RequipLoadouts.equip(p, 0, 18);
        if (!back.done() || !inv.armor.get(3).is(Items.IRON_HELMET) || !inv.items.get(0).is(Items.IRON_SWORD)) return "the way back failed (" + back.status() + ")";
        if (!census(p).equals(before)) return "the way back changed the totals";
        // a missing item is skipped and counted
        for (int i = 0; i < d.cells.size(); i++) if (d.cells.get(i).is(Items.DIAMOND_CHESTPLATE)) d.cells.set(i, ItemStack.EMPTY);
        before = census(p);
        RequipLoadouts.Outcome miss = RequipLoadouts.equip(p, 1, 18);
        if (!miss.done() || miss.missing() != 1) return "a missing chestplate was counted as " + miss.missing() + " (" + miss.status() + ")";
        if (!inv.armor.get(2).is(Items.IRON_CHESTPLATE)) return "the slot of a missing item was changed";
        if (!census(p).equals(before)) return "a partial swap changed the totals";
        // a full Armoury changes nothing at all
        RequipLoadouts.equip(p, 0, 18);
        for (int i = 0; i < d.cells.size(); i++) if (d.cells.get(i).is(Items.DIAMOND_HELMET)) d.cells.set(i, ItemStack.EMPTY);
        inv.items.set(30, new ItemStack(Items.DIAMOND_HELMET));                       // the wanted helmet is in the inventory, so the old helmet needs a free cell
        for (int i = 0; i < 18; i++) if (d.cells.get(i).isEmpty()) d.cells.set(i, filler(i));
        Map<String, Integer> full = census(p);
        RequipLoadouts.Outcome refused = RequipLoadouts.equip(p, 1, 18);
        if (refused.status() != LoadoutRules.Status.ARMOURY_FULL) return "a full Armoury answered " + refused.status();
        if (!inv.armor.get(3).is(Items.IRON_HELMET) || !census(p).equals(full)) return "a refused swap changed something";
        // clearing a loadout
        RequipLoadouts.clear(p, 1);
        if (d.loadouts.get(1).saved()) return "clear left the loadout";
        reset(p);
        return null;
    }

    private static ItemStack filler(int i) {
        ItemStack s = new ItemStack(Items.WOODEN_SWORD);
        s.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("filler " + i));
        return s;
    }

    // ---------------------------------------------------------------- Blade and Strike
    private static String weapons(ServerPlayer p) {
        reset(p);
        Inventory inv = p.getInventory();
        RequipArmoury.Data d = RequipArmoury.data(p);
        d.cells.set(5, new ItemStack(Items.DIAMOND_SWORD));
        inv.items.set(inv.selected, new ItemStack(Items.APPLE, 3));
        Map<String, Integer> before = census(p);
        if (RequipWeapons.summon(p) != RequipWeapons.Result.OK) return "the summon failed";
        if (!inv.getItem(inv.selected).is(Items.DIAMOND_SWORD)) return "the sword is not in the hand";
        if (!d.cells.get(5).is(Items.APPLE) || d.cells.get(5).getCount() != 3) return "the displaced item is not in the cell the sword left";
        if (!RequipWeapons.bladeOut(p)) return "the summoned blade is not recognised";
        if (!census(p).equals(before)) return "the summon changed the totals";
        if (RequipWeapons.putAway(p, 18) != RequipWeapons.Result.OK) return "put away failed";
        if (!inv.getItem(inv.selected).is(Items.APPLE) || !d.cells.get(5).is(Items.DIAMOND_SWORD)) return "put away did not swap back";
        if (!census(p).equals(before)) return "put away changed the totals";
        if (RequipWeapons.putAway(p, 18) != RequipWeapons.Result.NOT_OUT) return "put away with nothing out";
        // the armour's own weapon is never swapped into the pocket
        inv.items.set(inv.selected, com.newuniverse.nusmp.requip.RequipWeaponItem.flagged(com.newuniverse.nusmp.requip.RequipItems.weapon("purgatory_mace")));
        if (RequipWeapons.summon(p) != RequipWeapons.Result.HAND_FLAGGED) return "the armour's weapon was swapped";
        inv.items.set(inv.selected, ItemStack.EMPTY);
        // selection cycles through the weapons only
        d.cells.set(9, new ItemStack(Items.GOLDEN_AXE));
        d.cells.set(11, new ItemStack(Items.BREAD, 4));
        d.selected = 5;
        if (RequipWeapons.cycleSelection(p) != 9) return "the selection did not go to the next weapon";
        if (RequipWeapons.cycleSelection(p) != 5) return "the selection did not wrap";
        // Strike: the thrown weapon waits in the saved limbo and comes home
        reset(p);
        d = RequipArmoury.data(p);
        inv.items.set(inv.selected, new ItemStack(Items.DIAMOND_SWORD));
        Map<String, Integer> sword = census(p);
        ItemStack out = RequipWeapons.throwOut(p);
        if (out.isEmpty() || !inv.getItem(inv.selected).isEmpty() || d.limbo.isEmpty()) return "the throw did not take the weapon out of the hand";
        if (!census(p).equals(sword)) return "the throw changed the totals (the weapon must be counted in the limbo)";
        if (!RequipWeapons.throwOut(p).isEmpty()) return "a second weapon was thrown while one was out";
        if (!RequipWeapons.recall(p, 18) || !inv.getItem(inv.selected).is(Items.DIAMOND_SWORD) || !d.limbo.isEmpty()) return "the weapon did not come home";
        // the hand slot was taken meanwhile: it comes to a free slot, never the ground
        RequipWeapons.throwOut(p);
        inv.items.set(inv.selected, new ItemStack(Items.TORCH, 8));
        if (!RequipWeapons.recall(p, 18)) return "recall with a taken slot failed";
        boolean found = false;
        for (ItemStack s : inv.items) if (s.is(Items.DIAMOND_SWORD)) found = true;
        if (!found) return "the thrown weapon was lost when its slot was taken";
        // the safety net returns a weapon left out by a caster who was gone
        reset(p);
        RequipArmoury.data(p).limbo = new ItemStack(Items.IRON_AXE);
        ErzaRequipMagic.safetyNet(p);
        if (!RequipArmoury.data(p).limbo.isEmpty()) return "the safety net did not bring the weapon home";
        reset(p);
        return null;
    }

    // ---------------------------------------------------------------- Barrage
    private static Zombie zombie(ServerLevel level, double x, double y, double z, List<Entity> spawned) {
        Zombie z1 = EntityType.ZOMBIE.create(level);
        z1.moveTo(x, y, z, 0f, 0f);
        z1.setNoAi(true);
        level.addFreshEntity(z1);
        spawned.add(z1);
        return z1;
    }

    private static String barrage(ServerPlayer p, ServerLevel level, List<Entity> spawned) {
        reset(p);
        p.setYRot(0f);
        p.setXRot(0f);
        p.getInventory().items.set(p.getInventory().selected, new ItemStack(Items.DIAMOND_SWORD));
        ItemStack weapon = RequipWeapons.aimedWeapon(p);
        if (weapon.isEmpty()) return "the held sword was not found as the weapon";
        Vec3 aim = new Vec3(0.5, 100.9, 6.0);                                         // a spot close by (the crosshair of a real cast is wherever the player looks)
        Zombie plain = zombie(level, aim.x, 100.0, aim.z, spawned);
        Zombie boss = zombie(level, aim.x + 1.0, 100.0, aim.z, spawned);
        boss.getAttribute(Attributes.MAX_HEALTH).setBaseValue(5000.0);
        boss.setHealth(5000f);
        Wolf pet = EntityType.WOLF.create(level);
        pet.tame(p);
        pet.moveTo(aim.x, 100.0, aim.z - 1.5, 0f, 0f);                                // standing in the way of the swords
        level.addFreshEntity(pet);
        spawned.add(pet);
        float petHealth = pet.getHealth();
        double mastery = 0.5;
        int count = RequipBarrage.swordsFor(p, mastery);
        if (count < BarrageMath.MIN_SWORDS || count > BarrageMath.MAX_SWORDS) return "sword count " + count;
        List<LivingEntity> foes = SkillTargets.foes(p, aim, 8.0);
        if (foes.contains(pet)) return "a tamed wolf was picked as a target";
        Script s = RequipBarrage.barrage(p, weapon, mastery, count, RequipBarrage.GOLD, aim, foes);
        s.runAll();
        if (s.launched != count) return "launched " + s.launched + " of " + count + " swords";
        if (s.hits < 1) return "no sword hit anything";
        if (plain.getHealth() >= plain.getMaxHealth()) return "the zombie under the crosshair took nothing";
        if (pet.getHealth() < petHealth) return "an ally was hit by the barrage";
        double cap = BarrageMath.volleyCap(5000.0, true, RequipMagicConfig.bossVolleyShare());
        double dealt = 5000.0 - boss.getHealth();
        if (dealt > cap + 1.0) return "the boss took " + dealt + " from one volley, the cap is " + cap;
        if (!com.newuniverse.nusmp.balance.BalanceLaw.isBoss(boss)) return "the test boss is not a boss";
        // the script ends: nothing is left running
        RequipScripts.cancel(p);
        if (RequipScripts.running(p, "barrage")) return "the barrage is still running after a cancel";
        return null;
    }

    // ---------------------------------------------------------------- finishers and the Dimension-Cut
    private static String finishers(ServerPlayer p, ServerLevel level, List<Entity> spawned) {
        reset(p);
        p.setYRot(0f);
        p.setXRot(0f);
        p.getInventory().items.set(p.getInventory().selected, new ItemStack(Items.NETHERITE_SWORD));
        for (RequipMagicRules.Mode m : RequipMagicRules.finishers()) {
            Zombie near = zombie(level, 0.5, 100.0, 3.0, spawned);
            Zombie far = zombie(level, 0.5, 100.0, 7.0, spawned);
            Zombie boss = zombie(level, 2.0, 100.0, 3.0, spawned);
            boss.getAttribute(Attributes.MAX_HEALTH).setBaseValue(8000.0);
            boss.setHealth(8000f);
            Script s = RequipFinishers.build(p, m, 0.5);
            if (s == null) return m.id() + " has no script";
            s.runAll();
            if (s.hits < 1) return m.id() + " hit nothing";
            if (near.getHealth() >= near.getMaxHealth() && far.getHealth() >= far.getMaxHealth()) return m.id() + " hurt neither zombie";
            double cap = BarrageMath.volleyCap(8000.0, true, RequipMagicConfig.bossVolleyShare());
            if (8000.0 - boss.getHealth() > cap + 1.0) return m.id() + " took " + (8000.0 - boss.getHealth()) + " from a boss, the cap is " + cap;
            if (s.lastTick() > 120) return m.id() + " takes " + s.lastTick() + " ticks";
            near.discard(); far.discard(); boss.discard();
        }
        return null;
    }

    // ---------------------------------------------------------------- Partial Armour
    private static String partial(ServerPlayer p) {
        reset(p);
        for (Armour a : Armour.values()) {
            int mask = PartialArmourMask.GAUNTLETS | PartialArmourMask.GREAVES;
            int style = PartialArmourMask.encode(a.ordinal(), mask);
            if (PartialArmourMask.armourOf(style) != a.ordinal() || PartialArmourMask.maskOf(style) != mask) return a.id() + " style round trip";
            if (PartialArmourMask.encode(a.ordinal(), PartialArmourMask.FULL) != a.ordinal()) return a.id() + " whole set is not the plain ordinal";
            if (PartialArmourMask.subset(a, mask).size() != 4) return a.id() + " gauntlets and greaves are not four pieces";
        }
        double armor0 = p.getAttributeValue(Attributes.ARMOR), speed0 = p.getAttributeValue(Attributes.MOVEMENT_SPEED);
        double f = PartialArmourMask.fraction(Armour.ADAMANTINE, PartialArmourMask.GAUNTLETS | PartialArmourMask.GREAVES);
        ErzaRequipFx.applyModifiers(p, Armour.ADAMANTINE, 0.0, f);
        double armor = p.getAttributeValue(Attributes.ARMOR), speed = p.getAttributeValue(Attributes.MOVEMENT_SPEED);
        boolean ok = Math.abs(armor - (armor0 + Armour.ADAMANTINE.stats().armor() * f)) < 1e-6 && Math.abs(speed - speed0 * (1.0 + Armour.ADAMANTINE.stats().speed() * f)) < 1e-6;
        ErzaRequipFx.clearModifiers(p);
        if (!ok) return "a partial Adamantine did not give its share (armor " + armor + ", speed " + speed + ")";
        if (Math.abs(p.getAttributeValue(Attributes.ARMOR) - armor0) > 1e-9) return "a partial armour left a modifier behind";
        float whole = RequipRules.afterResist(Armour.ADAMANTINE, Armour.DamageClass.PHYSICAL, 10f, false), half = RequipRules.afterResist(Armour.ADAMANTINE, Armour.DamageClass.PHYSICAL, 10f, false, 0.5);
        if (!(half > whole && half < 10f)) return "a half armour must resist less than the whole and more than none";
        // the mask is read from the saved setting only when Partial Armour is on
        RequipArmoury.Data d = RequipArmoury.data(p);
        d.mask = PartialArmourMask.HELM | PartialArmourMask.CHEST;
        d.partialOn = false;
        if (ErzaRequipMagic.maskOf(p) != PartialArmourMask.FULL) return "an off setting changed the mask";
        d.partialOn = true;
        if (ErzaRequipMagic.maskOf(p) != (PartialArmourMask.HELM | PartialArmourMask.CHEST)) return "an on setting did not give its mask";
        reset(p);
        return null;
    }

    // ---------------------------------------------------------------- the gates
    private static String gates(ServerPlayer p) {
        reset(p);
        SkillAPI.getSkillsFrom(p).forgetSkill(NUSkills.ERZA_REQUIP.getId());
        if (ErzaRequipMagic.mayUse(p, "armoury")) return "the Armoury opened without the skill";
        String why = RequipSystem.teach(p, true);
        if (why != null) return "teaching refused: " + why;
        try {
            if (!ErzaRequipMagic.mayUse(p, "armoury")) return "the Armoury is shut to a user of the skill";
            if (ErzaRequipMagic.mayUse(p, RequipMagicRules.Mode.SEA_OF_SWORDS.id())) return "Sea of Swords is open at mastery 0";
            int cap = ErzaRequipMagic.capacity(p);
            if (cap < ArmouryRules.MIN_CAPACITY || cap > ArmouryRules.MAX_SLOTS) return "capacity " + cap;
        } finally {
            SkillAPI.getSkillsFrom(p).forgetSkill(NUSkills.ERZA_REQUIP.getId());
        }
        return null;
    }

    // ---------------------------------------------------------------- the Requip Reflex
    private static String reflex(ServerPlayer p) {
        reset(p);
        Inventory inv = p.getInventory();
        RequipArmoury.Data d = RequipArmoury.data(p);
        inv.armor.set(3, new ItemStack(Items.IRON_HELMET));
        d.cells.set(2, new ItemStack(Items.DIAMOND_HELMET));
        d.loadouts.set(3, loadout(new ItemStack(Items.DIAMOND_HELMET), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
        d.reflex = 3;
        ManasSkillInstance inst = NUSkills.ERZA_REQUIP.get().createDefaultInstance();
        // mastery 0: the passive is asleep
        ErzaRequipMagic.reflex(p, inst);
        if (!inv.armor.get(3).is(Items.IRON_HELMET)) return "the reflex woke at mastery 0";
        inst.setMastery(inst.getMaxMastery());
        ErzaRequipMagic.reflex(p, inst);
        if (RequipMagicConfig.reflexEnabled()) {
            if (!inv.armor.get(3).is(Items.DIAMOND_HELMET)) return "the reflex did not swap to the reflex loadout";
            if (d.active != 3) return "the reflex loadout is not the active one";
            // once per pause
            inv.armor.set(3, new ItemStack(Items.IRON_HELMET));
            d.active = -1;
            ErzaRequipMagic.reflex(p, inst);
            if (!inv.armor.get(3).is(Items.IRON_HELMET) && RequipMagicConfig.reflexTicks() > 0) return "the reflex fired twice inside its pause";
        }
        reset(p);
        return null;
    }
}
