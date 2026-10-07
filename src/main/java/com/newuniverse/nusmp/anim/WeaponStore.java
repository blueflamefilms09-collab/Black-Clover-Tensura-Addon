package com.newuniverse.nusmp.anim;

import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.item.MagicWeaponItem;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.CustomData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 0.54: "Store Weapon" and "Draw Weapon", the last two pages of every grimoire (docs/grimoire_weapon_store.md). As in the show, the
 * weapon in your hand goes back into the book (the draw motion played backwards, 24 ticks, the weapon sinking into the pages at tick
 * 12) and comes out again when you draw. Any weapon will do: swords, axes, spears, tridents, maces, bows and crossbows, this mod's
 * magic weapons and the Tensura weapons (by name). The grimoire keeps up to {@link #MAX} stacks in its own data (the skill's tag, so
 * it follows the player); a weapon that was only SUMMONED (a Sword Magic sword with a timer) is simply dismissed, the page that
 * summoned it brings it back. A weapon bound to someone else cannot be stored.
 */
public final class WeaponStore {
    private WeaponStore() {}

    public static final int MAX = 8;
    static final String KEY = "StoredWeapons";
    private static final String[] WORDS = {"sword", "katana", "blade", "spear", "lance", "scythe", "dagger", "kodachi", "rapier", "saber", "sabre", "claymore",
            "scimitar", "halberd", "trident", "mace", "axe", "bow", "staff", "club", "hammer", "glaive"};
    /** player -> game time until which a store / draw is already playing (a second press is ignored). */
    private static final Map<UUID, Long> BUSY = new HashMap<>();

    public static boolean isWeapon(ItemStack s) {
        if (s.isEmpty()) return false;
        Item it = s.getItem();
        if (it instanceof MagicWeaponItem || it instanceof SwordItem || it instanceof AxeItem || it instanceof TridentItem || it instanceof MaceItem
                || it instanceof BowItem || it instanceof CrossbowItem || s.is(ItemTags.SWORDS) || s.is(ItemTags.AXES)) return true;
        String id = BuiltInRegistries.ITEM.getKey(it).getPath();
        for (String w : WORDS) if (id.contains(w)) return true;
        return false;
    }

    /** A sword drawn for a limited time (Sword Magic): it is dismissed, not kept. */
    static boolean isSummoned(ItemStack s) {
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        return d != null && d.copyTag().contains("ExpiresAt");
    }

    private static boolean busy(ServerPlayer p) {
        long now = p.level().getGameTime();
        BUSY.values().removeIf(t -> t < now);
        return BUSY.getOrDefault(p.getUUID(), 0L) > now;
    }

    private static void say(ServerPlayer p, String text, ChatFormatting colour) {
        p.displayClientMessage(Component.literal(text).withStyle(colour), true);
    }

    /** The page Store Weapon: the weapon in the main hand (else the off hand) goes into the grimoire. */
    public static void store(ServerPlayer p, GrimoireBook book, ManasSkillInstance inst) {
        if (busy(p)) return;
        InteractionHand hand = isWeapon(p.getMainHandItem()) ? InteractionHand.MAIN_HAND : isWeapon(p.getOffhandItem()) ? InteractionHand.OFF_HAND : null;
        if (hand == null) { say(p, "Hold a weapon to store it in your grimoire.", ChatFormatting.GRAY); return; }
        ItemStack held = p.getItemInHand(hand);
        if (held.getItem() instanceof MagicWeaponItem && !MagicWeaponItem.usableBy(held, p)) { say(p, "That weapon is bound to someone else.", ChatFormatting.GRAY); return; }
        boolean temporary = isSummoned(held);
        if (!temporary && inst.getOrCreateTag().getList(KEY, Tag.TAG_COMPOUND).size() >= MAX) {
            say(p, "Your grimoire holds no more weapons (" + MAX + "). Draw one first.", ChatFormatting.GRAY);
            return;
        }
        Item kind = held.getItem();
        String name = held.getHoverName().getString();
        BUSY.put(p.getUUID(), p.level().getGameTime() + SwordDraw.DURATION);
        SwordDraw.store(p, SwordDraw.styleFor(book.magic), () -> {
            ItemStack now = p.getItemInHand(hand);
            if (now.isEmpty() || now.getItem() != kind) return;                           // moved away during the motion: nothing is stored
            if (!temporary) {
                CompoundTag tag = inst.getOrCreateTag();
                ListTag list = tag.getList(KEY, Tag.TAG_COMPOUND);
                if (list.size() >= MAX) return;
                CompoundTag entry = new CompoundTag();
                entry.put("Item", now.save(p.registryAccess()));
                list.add(entry);
                tag.put(KEY, list);
                inst.markDirty();
            }
            p.setItemInHand(hand, ItemStack.EMPTY);
            say(p, temporary ? name + " returns to your grimoire." : name + " is stored in your grimoire.", ChatFormatting.AQUA);
        });
    }

    /** The page Draw Weapon: the weapon that was stored first comes out into the main hand (or the inventory). */
    public static void draw(ServerPlayer p, GrimoireBook book, ManasSkillInstance inst) {
        if (busy(p)) return;
        if (inst.getOrCreateTag().getList(KEY, Tag.TAG_COMPOUND).isEmpty()) {
            say(p, "Your grimoire holds no stored weapons. Hold one and use Store Weapon.", ChatFormatting.GRAY);
            return;
        }
        BUSY.put(p.getUUID(), p.level().getGameTime() + SwordDraw.DURATION);
        SwordDraw.drawThen(p, SwordDraw.styleFor(book.magic), () -> {
            CompoundTag tag = inst.getOrCreateTag();
            ListTag list = tag.getList(KEY, Tag.TAG_COMPOUND);
            if (list.isEmpty()) return;
            Tag raw = list.getCompound(0).get("Item");
            list.remove(0);
            tag.put(KEY, list);
            inst.markDirty();
            ItemStack s = raw == null ? ItemStack.EMPTY : ItemStack.parse(p.registryAccess(), raw).orElse(ItemStack.EMPTY);
            if (s.isEmpty()) return;
            SwordDraw.give(p, s, true);
            say(p, s.getHoverName().getString() + " is drawn from your grimoire.", ChatFormatting.AQUA);
        });
    }
}
