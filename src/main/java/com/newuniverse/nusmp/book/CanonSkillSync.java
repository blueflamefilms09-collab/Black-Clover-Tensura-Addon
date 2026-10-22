package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.GrimoireItem;
import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.blackclover.GrimoireSlot;
import com.newuniverse.nusmp.grimoire.CanonNaming;
import com.newuniverse.nusmp.skill.GrimoireMagicSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the Unique Skill of a grimoire shows the bound character's name ({@link GrimoireSkillInstance}), and the name is read from the skill instance's own tag, which the client receives with the
 * instance. This keeps that tag equal to the character of the grimoire in the player's Grimoire Slot: every two seconds (server only, one slot read and a pass over the player's own few
 * grimoire skills) the instance of the slot's magic gets "Canon" = the slot's character id, every other grimoire instance has it removed, and an instance whose tag changed is marked dirty so
 * ManasCore syncs it. All state is per player (the player's own skills); nothing is static. Called from {@code GrimoireKeeper.heal}.
 */
public final class CanonSkillSync {
    private CanonSkillSync() {}

    private static final int EVERY = 40, PHASE = 5;

    /** The character id the player's Grimoire Slot book gives the grimoire skill of {@code magicName} ("" when the slot holds no book of that magic or it has no character). */
    public static String canonOf(ServerPlayer p, String magicName) {
        ItemStack slot = GrimoireSlot.get(p);
        if (!GrimoireItem.isOwnedBy(slot, p.getUUID())) return "";
        CompoundTag d = GrimoireItem.data(slot);
        return CanonNaming.wanted(d.getString("Magic"), d.getString(CanonNaming.TAG), magicName);
    }

    public static void sync(ServerPlayer p) {
        if (p.tickCount % EVERY != PHASE) return;
        ItemStack slot = GrimoireSlot.get(p);
        String slotMagic = "", slotCanon = "";
        if (GrimoireItem.isOwnedBy(slot, p.getUUID())) {
            CompoundTag d = GrimoireItem.data(slot);
            slotMagic = d.getString("Magic");
            slotCanon = d.getString(CanonNaming.TAG);
        }
        List<ManasSkillInstance> mine = new ArrayList<>();
        for (ManasSkillInstance i : SkillAPI.getSkillsFrom(p).getLearnedSkills())
            if (i.getSkill() instanceof GrimoireBook || i.getSkill() instanceof GrimoireMagicSkill) mine.add(i);
        for (ManasSkillInstance i : mine) {
            String want = CanonNaming.wanted(slotMagic, slotCanon, GrimoirePages.magicOf(i).name());
            CompoundTag t = i.getTag();
            String have = t == null ? "" : t.getString(CanonNaming.TAG);
            if (have.equals(want)) continue;
            if (want.isEmpty()) i.getOrCreateTag().remove(CanonNaming.TAG);
            else i.getOrCreateTag().putString(CanonNaming.TAG, want);
            i.markDirty();
        }
    }
}
