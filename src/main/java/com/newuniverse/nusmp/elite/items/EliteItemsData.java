package com.newuniverse.nusmp.elite.items;

import com.mojang.serialization.Codec;
import com.newuniverse.nusmp.NUSMP;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Why: what this subsystem remembers about a PLAYER, in a NeoForge attachment {@code nusmp:elite_items} that is saved with the player and copied to the new body on death (a collection must survive a
 * death): the set of plushie kinds they have held (the collection counter) and how many Potential Catalysts they have used (the catalyst's total limit). Written only when it holds something, so a
 * world full of players who never touched an elite item does not grow. The plain class has no behaviour beyond the two facts, so the rules stay in the items that use them.
 */
public final class EliteItemsData {
    private final Set<String> plushies = new LinkedHashSet<>();
    private int catalystUses;

    public Set<String> plushies() { return plushies; }

    public int catalystUses() { return catalystUses; }

    public void setCatalystUses(int n) { catalystUses = Math.max(0, n); }

    public boolean isEmpty() { return plushies.isEmpty() && catalystUses == 0; }

    public CompoundTag toTag() {
        CompoundTag t = new CompoundTag();
        ListTag l = new ListTag();
        for (String s : plushies) l.add(StringTag.valueOf(s));
        if (!l.isEmpty()) t.put("plushies", l);
        if (catalystUses > 0) t.putInt("catalyst", catalystUses);
        return t;
    }

    public static EliteItemsData fromTag(CompoundTag t) {
        EliteItemsData d = new EliteItemsData();
        if (t == null) return d;
        ListTag l = t.getList("plushies", Tag.TAG_STRING);
        for (int i = 0; i < l.size(); i++) d.plushies.add(l.getString(i));
        d.catalystUses = Math.max(0, t.getInt("catalyst"));
        return d;
    }

    // ------------------------------------------------------------------ the attachment
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, NUSMP.MODID);

    private static final Codec<EliteItemsData> CODEC = CompoundTag.CODEC.xmap(EliteItemsData::fromTag, EliteItemsData::toTag);

    /** {@code nusmp:elite_items}. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<EliteItemsData>> TYPE = ATTACHMENTS.register("elite_items",
            () -> AttachmentType.<EliteItemsData>builder(() -> new EliteItemsData()).serialize(CODEC, d -> !d.isEmpty()).copyOnDeath().build());

    /** The player's data (created empty on first use). Live: changing it changes the player's save. */
    public static EliteItemsData of(Player player) { return player.getData(TYPE); }

    public static void init(IEventBus modBus) { ATTACHMENTS.register(modBus); }
}
