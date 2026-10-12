package com.newuniverse.nusmp.dungeon;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

/**
 * What the Dungeon's skills side keeps on a player: the skills a meal has lent, the meal attunement, the Golden Ward and the Magic Sense
 * counters. It lives in the PlayerPersisted tag like mana.ManaLaw's data and is copied to the new body on death ({@link MealSkills#onClone}),
 * so dying can never turn a lent skill into an owned one.
 */
final class SkillsData {
    private SkillsData() {}

    static final String KEY = "nusmp_dungeon_skills";

    /** The player's compound, made if it is not there yet. */
    static CompoundTag of(Player p) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) pd.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        CompoundTag root = pd.getCompound(Player.PERSISTED_NBT_TAG);
        if (!root.contains(KEY, Tag.TAG_COMPOUND)) root.put(KEY, new CompoundTag());
        return root.getCompound(KEY);
    }

    /** The player's compound if they have one, else null (reading it leaves nothing behind). */
    static CompoundTag peek(Player p) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) return null;
        CompoundTag root = pd.getCompound(Player.PERSISTED_NBT_TAG);
        return root.contains(KEY, Tag.TAG_COMPOUND) ? root.getCompound(KEY) : null;
    }

    /** A named part of the compound, if it is there. */
    static CompoundTag peek(Player p, String part) {
        CompoundTag d = peek(p);
        return d != null && d.contains(part, Tag.TAG_COMPOUND) ? d.getCompound(part) : null;
    }

    /** A named part of the compound, made if it is not there yet. */
    static CompoundTag part(Player p, String part) {
        CompoundTag d = of(p);
        if (!d.contains(part, Tag.TAG_COMPOUND)) d.put(part, new CompoundTag());
        return d.getCompound(part);
    }

    /** Hands a copy of everything to the new body of a player who died or came back from the End. */
    static void copy(Player from, Player to) {
        CompoundTag mine = peek(from);
        if (mine == null) return;
        CompoundTag pd = to.getPersistentData();
        if (!pd.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) pd.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        pd.getCompound(Player.PERSISTED_NBT_TAG).put(KEY, mine.copy());
    }
}
