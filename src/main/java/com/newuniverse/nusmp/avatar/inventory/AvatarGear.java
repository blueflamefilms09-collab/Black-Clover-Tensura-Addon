package com.newuniverse.nusmp.avatar.inventory;

import com.newuniverse.nusmp.avatar.AvatarAiConfig;
import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.ai.LootClassifier;
import com.newuniverse.nusmp.avatar.ai.LootRules;
import com.newuniverse.nusmp.avatar.ai.WeaponKind;
import com.newuniverse.nusmp.blackclover.GrimoireItem;
import com.newuniverse.nusmp.charter.CharterGrimoireItem;
import com.newuniverse.nusmp.wizard.GrimoireSummonsItem;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.common.ItemAbilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Why: the Avatar's gear decisions need to read item stacks (what kind of weapon is this, how much armour does it give, is it a staff), and then act: put on
 * better armour, take a better sword, keep the old piece in a pocket, give arrows to the bow, and on death drop everything or hand it to the creator. The numbers
 * are judged by the pure {@link GearScore}; this class is the thin layer that reads the stacks and moves them. Everything an Avatar carries goes through the
 * {@link AvatarInventory}, so the relic bonds and the relic marker stay right whichever way an item arrives.
 */
public final class AvatarGear {
    private AvatarGear() {}

    /** The item tag a data pack can add its own staves, wands and tomes to ({@code nusmp:avatar_magic_focus}); the grimoire items of this mod are in it. */
    public static final TagKey<Item> MAGIC_FOCUS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("nusmp", "avatar_magic_focus"));
    private static final List<String> FOCUS_WORDS = List.of("staff", "wand", "scepter", "sceptre", "tome", "spellbook", "grimoire", "codex");

    // ---------------------------------------------------------------- reading a stack
    /** What kind of thing the stack is for the weapon AI. */
    public static WeaponKind kindOf(ItemStack s) {
        if (s == null || s.isEmpty()) return WeaponKind.NONE;
        Item it = s.getItem();
        if (it instanceof BowItem) return WeaponKind.BOW;
        if (it instanceof CrossbowItem) return WeaponKind.CROSSBOW;
        if (it instanceof TridentItem) return WeaponKind.TRIDENT;
        if (it instanceof ShieldItem || s.canPerformAction(ItemAbilities.SHIELD_BLOCK)) return WeaponKind.SHIELD;
        if (isMagicFocus(s)) return WeaponKind.FOCUS;
        if (damageOf(s) > 0.0 || it instanceof SwordItem || it instanceof DiggerItem || it instanceof MaceItem) return WeaponKind.MELEE;
        return WeaponKind.NONE;
    }

    /** Is the stack a grimoire, a charter grimoire, a staff, a wand or a tome: the thing a mage holds? */
    public static boolean isMagicFocus(ItemStack s) {
        if (s == null || s.isEmpty()) return false;
        Item it = s.getItem();
        if (it instanceof GrimoireItem || it instanceof CharterGrimoireItem || it instanceof GrimoireSummonsItem) return true;
        if (s.is(MAGIC_FOCUS)) return true;
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(it);
        if (key == null) return false;
        for (String token : key.getPath().toLowerCase(Locale.ROOT).split("_")) if (FOCUS_WORDS.contains(token)) return true;
        return false;
    }

    /** The attack damage the stack adds in the main hand (the sum of its additive attack-damage modifiers). */
    public static double damageOf(ItemStack s) {
        double[] d = {0.0};
        s.forEachModifier(EquipmentSlot.MAINHAND, (attr, mod) -> {
            if (attr.equals(Attributes.ATTACK_DAMAGE) && mod.operation() == AttributeModifier.Operation.ADD_VALUE) d[0] += mod.amount();
        });
        return d[0];
    }

    private static double modifierSum(ItemStack s, EquipmentSlot slot, Holder<Attribute> wanted) {
        double[] d = {0.0};
        s.forEachModifier(slot, (attr, mod) -> {
            if (attr.equals(wanted) && mod.operation() == AttributeModifier.Operation.ADD_VALUE) d[0] += mod.amount();
        });
        return d[0];
    }

    private static int enchantPoints(ItemStack s) {
        int n = 0;
        for (Holder<Enchantment> e : s.getEnchantments().keySet()) n += s.getEnchantments().getLevel(e);
        return n;
    }

    /** The armour numbers of a stack worn in {@code slot}. */
    public static GearScore.Armor armorStats(ItemStack s, EquipmentSlot slot) {
        if (s == null || s.isEmpty()) return new GearScore.Armor(0, 0.0, 0.0, 0, 0, 0);
        int max = s.isDamageableItem() ? s.getMaxDamage() : 0;
        int left = max <= 0 ? 0 : max - s.getDamageValue();
        return new GearScore.Armor((int) Math.round(modifierSum(s, slot, Attributes.ARMOR)), modifierSum(s, slot, Attributes.ARMOR_TOUGHNESS),
                modifierSum(s, slot, Attributes.KNOCKBACK_RESISTANCE), enchantPoints(s), left, max);
    }

    /** The weapon numbers of a stack. */
    public static GearScore.Weapon weaponStats(ItemStack s) {
        if (s == null || s.isEmpty()) return new GearScore.Weapon(0.0, 0, 0, 0);
        int max = s.isDamageableItem() ? s.getMaxDamage() : 0;
        int left = max <= 0 ? 0 : max - s.getDamageValue();
        return new GearScore.Weapon(damageOf(s), enchantPoints(s), left, max);
    }

    /** Does the stack hold something a bow or crossbow can fire? */
    public static boolean isArrow(ItemStack s) { return s != null && !s.isEmpty() && s.getItem() instanceof ArrowItem; }

    // ---------------------------------------------------------------- ammunition
    /** Has the Avatar something to shoot: always when arrows are free, otherwise an arrow in a pocket or in the off hand. */
    public static boolean hasAmmo(AvatarEntity a) {
        if (!AvatarAiConfig.consumeAmmo()) return true;
        return isArrow(a.getOffhandItem()) || a.inventory().countInPockets(AvatarGear::isArrow) > 0;
    }

    /** One arrow for the next shot: a plain one when arrows are free, otherwise one taken from the pockets (the off hand last); EMPTY when there is none. */
    public static ItemStack takeArrow(AvatarEntity a) {
        if (!AvatarAiConfig.consumeAmmo()) return new ItemStack(Items.ARROW);
        ItemStack one = a.inventory().takeOneFromPockets(AvatarGear::isArrow);
        if (one != null) return one;
        ItemStack off = a.getOffhandItem();
        if (isArrow(off)) {
            ItemStack taken = off.copyWithCount(1);
            off.shrink(1);
            return taken;
        }
        return ItemStack.EMPTY;
    }

    // ---------------------------------------------------------------- taking things in
    /**
     * Offers a found stack to the Avatar. Better armour and a better weapon of the kind it holds are put on (the old piece goes into a pocket; no swap when there
     * is no pocket for it); everything else goes into the pockets. Returns what the Avatar did not take (EMPTY when all of it went in).
     */
    public static ItemStack take(AvatarEntity a, ItemStack found) {
        if (found == null || found.isEmpty()) return ItemStack.EMPTY;
        AvatarInventory inv = a.inventory();
        if (AvatarAiConfig.autoEquip() && found.getCount() == 1 && tryEquip(a, inv, found)) return ItemStack.EMPTY;
        return inv.addToPockets(found);
    }

    private static boolean tryEquip(AvatarEntity a, AvatarInventory inv, ItemStack found) {
        int index = equipIndex(a, found);
        return index >= 0 && swapIn(a, inv, index, found);
    }

    /** Would the Avatar put this stack on (or into its hand)? True for an upgrade; used by the looting goal to want such an item more. */
    public static boolean wouldUpgrade(AvatarEntity a, ItemStack found) {
        return AvatarAiConfig.autoEquip() && found != null && found.getCount() == 1 && equipIndex(a, found) >= 0;
    }

    /** The equipment index a found stack would take over (better armour, a better weapon of the kind held, a shield for an empty off hand), or -1 for none. */
    static int equipIndex(AvatarEntity a, ItemStack found) {
        EquipmentSlot slot = a.getEquipmentSlotForItem(found);
        if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && found.getItem() instanceof ArmorItem) {
            ItemStack worn = a.getItemBySlot(slot);
            return GearScore.betterArmor(armorStats(found, slot), worn.isEmpty() ? null : armorStats(worn, slot)) ? AvatarInventory.indexOf(slot) : -1;
        }
        WeaponKind kind = kindOf(found);
        if (kind == WeaponKind.SHIELD) return a.getOffhandItem().isEmpty() ? InventoryLayout.OFF : -1;
        if (kind == WeaponKind.NONE) return -1;
        GearScore.Weapon foundStats = weaponStats(found);
        if (foundStats.wear() < GearScore.MIN_DURABILITY) return -1;
        ItemStack held = a.getMainHandItem();
        return GearScore.equipMainHand(kindOf(held), GearScore.weapon(weaponStats(held)), kind, GearScore.weapon(foundStats), hasAmmo(a)) ? InventoryLayout.MAIN : -1;
    }

    /** Puts {@code found} into equipment slot {@code index}; the piece that was there moves to a pocket (and the swap is refused when it has none). */
    private static boolean swapIn(AvatarEntity a, AvatarInventory inv, int index, ItemStack found) {
        ItemStack old = inv.getItem(index);
        if (!old.isEmpty() && !inv.hasRoomFor(old)) return false;
        ItemStack oldCopy = old.copy();
        inv.setItem(index, found.copy());
        if (!oldCopy.isEmpty()) {
            AvatarRelicBridge.onLeave(a, oldCopy);
            inv.addToPockets(oldCopy);
        }
        return true;
    }

    // ---------------------------------------------------------------- giving and dropping
    /** The pockets worth giving the creator: the valuable and the rare ones (see {@code LootRules}), as pocket indices. */
    public static List<Integer> worthGiving(AvatarEntity a) {
        List<Boolean> flags = new ArrayList<>();
        for (ItemStack s : a.inventory().pocketStacks()) flags.add(!s.isEmpty() && LootClassifier.worthGiving(s));
        return LootRules.handOverIndices(flags, InventoryLayout.POCKETS);
    }

    /** Hands the valuable pockets to the creator (into their inventory; what does not fit is dropped at their feet). Returns how many stacks moved. */
    public static int handOver(AvatarEntity a, ServerPlayer creator) {
        int moved = 0;
        for (int i : worthGiving(a)) {
            ItemStack s = a.inventory().takePocket(i);
            if (s.isEmpty()) continue;
            giveTo(creator, s);
            moved++;
        }
        return moved;
    }

    private static void giveTo(ServerPlayer p, ItemStack s) {
        if (!p.getInventory().add(s) && !s.isEmpty()) p.drop(s, false);
    }

    /**
     * The Avatar is dead: everything it carries (hands, armour, trinkets, pockets) is dropped where it fell; with {@code avatar.dropGearOnDeath} off it goes to the
     * creator instead (into their inventory, overflow at their feet; dropped here when the creator is not online). The trinkets are taken out of the relic pack
     * first, so the relic system's own death drop finds it empty and nothing is dropped twice.
     */
    public static void onDeath(AvatarEntity a, ServerLevel level) {
        AvatarInventory inv = a.inventory();
        inv.refresh();
        List<ItemStack> all = new ArrayList<>();
        for (int i = 0; i < InventoryLayout.SIZE; i++) {
            ItemStack s = inv.removeItemNoUpdate(i);
            if (!s.isEmpty()) all.add(s);
        }
        a.getPersistentData().remove(AvatarRelicBridge.PACK);
        a.getPersistentData().remove(AvatarRelicBridge.GIVER);
        ServerPlayer creator = AvatarAiConfig.dropGearOnDeath() ? null : a.creatorPlayer();
        for (ItemStack s : all) {
            AvatarRelicBridge.onLeave(a, s);
            if (creator != null) giveTo(creator, s); else a.spawnAtLocation(s);
        }
    }
}
