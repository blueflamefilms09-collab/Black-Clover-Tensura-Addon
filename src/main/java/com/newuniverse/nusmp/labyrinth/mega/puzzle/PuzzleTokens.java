package com.newuniverse.nusmp.labyrinth.mega.puzzle;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

/**
 * Why: the carried-item puzzles (offerings, keys, elemental shards) hand out REAL items, and a real item must never be confusable with loot, never leak out of the puzzle and always be
 * traceable: every issued token is an ordinary vanilla item (a gold ingot, a tripwire hook ...) with a custom name and two tags in its custom data, the token word and the key of the
 * puzzle that issued it. {@link #tokenOf} reads a held stack back into a word: a stack with the tags of THIS puzzle counts as its token, and an ordinary stack counts as the token it stands
 * for only for the riddle offerings (a gold ingot is {@code gold}); a key or a shard from another puzzle, or an untagged tripwire hook, is nothing. {@link #purge} takes every token of a
 * puzzle out of a player's pack when the puzzle ends.
 */
final class PuzzleTokens {
    private PuzzleTokens() {}

    static final String TAG = "nusmp_puzzle_token", KEY = "nusmp_puzzle_key";

    static ItemStack make(String token, String instanceKey) {
        Item item = item(Tokens.ITEM_ID.getOrDefault(token, "minecraft:paper"));
        ItemStack s = new ItemStack(item);
        CompoundTag t = new CompoundTag();
        t.putString(TAG, token);
        t.putString(KEY, instanceKey);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        s.set(DataComponents.CUSTOM_NAME, name(token));
        return s;
    }

    static Component name(String token) { return Component.translatableWithFallback(PuzzleText.langKey("token." + token), PuzzleText.en("token." + token) == null ? token : PuzzleText.en("token." + token)); }

    private static Item item(String id) {
        try {
            return BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id)).orElse(Items.PAPER);
        } catch (RuntimeException e) {
            return Items.PAPER;
        }
    }

    /** The token a held stack stands for in this puzzle, or null. */
    static String tokenOf(ItemStack s, String instanceKey) {
        if (s == null || s.isEmpty()) return null;
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        if (d != null) {
            CompoundTag t = d.copyTag();
            if (t.contains(TAG)) return instanceKey.equals(t.getString(KEY)) ? t.getString(TAG) : null;
        }
        String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
        for (String token : Tokens.ITEMS) if (Tokens.ITEM_ID.get(token).equals(id)) return token;
        return null;
    }

    static boolean isTagged(ItemStack s, String instanceKey) {
        if (s == null || s.isEmpty()) return false;
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        return d != null && instanceKey.equals(d.copyTag().getString(KEY));
    }

    /** Takes every token of the puzzle out of the player's pack; returns how many stacks went. */
    static int purge(ServerPlayer p, String instanceKey) {
        int n = 0;
        for (ItemStack s : p.getInventory().items) {
            if (isTagged(s, instanceKey)) { s.setCount(0); n++; }
        }
        for (ItemStack s : p.getInventory().offhand) {
            if (isTagged(s, instanceKey)) { s.setCount(0); n++; }
        }
        return n;
    }
}
