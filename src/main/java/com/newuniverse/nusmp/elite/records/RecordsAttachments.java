package com.newuniverse.nusmp.elite.records;

import com.mojang.serialization.Codec;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.elite.records.rule.RecordData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Why: the one saved thing of the records system, a data attachment {@code nusmp:elite_records} on the player: the unlocked records and held titles with their timestamps, the counters, the best values
 * and the found sets (see {@link RecordData}). It is serialised through a tolerant compound codec ({@link RecordDataNbt}), is skipped while empty (so a player who never touched the system gets no tag), and
 * is copied to the new body on death and on the End return, because records and titles are for good. Register {@link #ATTACHMENTS} on the mod bus. The client does not receive the attachment; it gets
 * the packets of {@link RecordsNet}.
 */
public final class RecordsAttachments {
    private RecordsAttachments() {}

    private static final Codec<RecordData> CODEC = CompoundTag.CODEC.xmap(RecordDataNbt::read, RecordDataNbt::write);

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, NUSMP.MODID);

    /** {@code nusmp:elite_records}. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<RecordData>> DATA = ATTACHMENTS.register("elite_records",
            () -> AttachmentType.builder(() -> new RecordData()).serialize(CODEC, d -> !d.isEmpty()).copyOnDeath().build());

    /** The player's data (created empty on first use). The returned object is live: changing it changes the save. */
    public static RecordData of(Player player) { return player.getData(DATA); }
}
