package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.Devil;
import com.newuniverse.nusmp.blackclover.GrimoireAcceptance;
import com.newuniverse.nusmp.blackclover.GrimoireCover;
import com.newuniverse.nusmp.blackclover.GrimoireItem;
import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Keeps book skill and grimoire item together:
 * - learning a book (acceptance, /nusmp, /tensura grant) always gives or re-labels the grimoire;
 * - if another mod deletes the book skill while the player still owns the grimoire, it is re-taught.
 */
public final class GrimoireKeeper {
    private GrimoireKeeper() {}

    public static void ensureGrimoire(ServerPlayer p, GrimoireBook book, ManasSkillInstance inst) {
        CompoundTag tag = inst.getOrCreateTag();
        if (!tag.contains("Magic")) tag.putString("Magic", book.magic.name());
        GrimoireCover cover = GrimoirePages.coverOf(inst);
        if (!tag.contains("Cover")) { tag.putString("Cover", cover.name()); tag.putInt("Leaves", cover.tier); }
        Devil devil = Devil.byName(tag.getString("Devil"));
        inst.markDirty();
        GrimoireAcceptance.markRolled(p, true);

        var inv = p.getInventory();
        var skills = SkillAPI.getSkillsFrom(p);
        if (com.newuniverse.nusmp.blackclover.GrimoireSlot.holdsOwn(p, book.magic)) return;               // already in the Grimoire Slot
        int orphan = -1;
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack s = inv.getItem(slot);
            if (!GrimoireItem.isOwnedBy(s, p.getUUID())) continue;
            var itsMagic = com.newuniverse.nusmp.blackclover.MagicType.byName(GrimoireItem.data(s).getString("Magic"));
            if (itsMagic == book.magic) return;                                                   // already has it
            boolean stillUsed = skills.getSkill(NUSkills.grimoireSkillFor(itsMagic).getRegistryName()).isPresent();
            if (!stillUsed && orphan < 0) orphan = slot;                                           // a book whose magic is gone
        }
        if (orphan >= 0) { inv.setItem(orphan, GrimoireItem.create(p, cover, book.magic, devil)); return; }
        inv.placeItemBackInInventory(GrimoireItem.create(p, cover, book.magic, devil));
        p.displayClientMessage(Component.literal("Your grimoire flies into your hands.").withStyle(ChatFormatting.GOLD), true);
    }

    /** Every 5 s: own a bound grimoire but the book skill vanished? Teach it again. */
    public static void heal(ServerPlayer p) {
        if (p.tickCount % 100 != 0 || GrimoirePages.grimoireOf(p).isPresent()) return;
        var books = new java.util.ArrayList<ItemStack>();
        books.add(com.newuniverse.nusmp.blackclover.GrimoireSlot.get(p));
        books.addAll(p.getInventory().items);
        for (ItemStack s : books) {
            if (!GrimoireItem.isOwnedBy(s, p.getUUID())) continue;
            CompoundTag d = GrimoireItem.data(s);
            var magic = com.newuniverse.nusmp.blackclover.MagicType.byName(d.getString("Magic"));
            var skill = NUSkills.grimoireSkillFor(magic);
            ManasSkillInstance inst = skill.createDefaultInstance();
            inst.getOrCreateTag().putString("Magic", magic.name());
            inst.getOrCreateTag().putString("Cover", GrimoireItem.cover(s).name());
            inst.getOrCreateTag().putInt("Leaves", GrimoireItem.cover(s).tier);
            if (d.contains("Devil")) inst.getOrCreateTag().putString("Devil", d.getString("Devil"));
            if (SkillAPI.getSkillsFrom(p).learnSkill(inst, Component.literal("Your grimoire restores its magic.").withStyle(ChatFormatting.GOLD))) return;
            if (p.tickCount % 12000 == 0) {
                p.displayClientMessage(Component.literal("Your grimoire's magic was removed by another mod (Unique-skill limits?). "
                        + "Check configs such as tensura_awv_addon 'uniqueSkillsAreGlobal'.").withStyle(ChatFormatting.RED), false);
            }
            return;
        }
    }
}
