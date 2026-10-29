package com.newuniverse.nusmp.core.magic.profile;

import com.mojang.serialization.Codec;
import com.newuniverse.nusmp.NUSMP;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: Celestial Spirit Magic and the Requip skill were removed, but players may still carry their saved attachments ({@code nusmp:celestial}, the key collection, and
 * {@code nusmp:requip_armoury}, the pocket armoury with the stored items). The ids stay registered as inert pass-through holders so a world and its players load without an
 * "unknown attachment" error and the raw data survives a save (nothing reads or writes it any more; it is the append-only rule for save-related ids). Register
 * {@link #ATTACHMENTS} on the mod bus. Cost: none per tick; the data is written only when it is not empty.
 */
public final class RetiredAttachments {
    private RetiredAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, NUSMP.MODID);

    /** {@code nusmp:celestial}: the old key masks (a list of longs); kept as is. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<List<Long>>> CELESTIAL = ATTACHMENTS.register("celestial",
            () -> AttachmentType.<List<Long>>builder(() -> new ArrayList<Long>()).serialize(Codec.LONG.listOf(), l -> !l.isEmpty()).copyOnDeath().build());

    /** {@code nusmp:requip_armoury}: the old pocket armoury (cells, loadouts, limbo, blade ...); kept as a raw compound so the stored stacks are not lost. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CompoundTag>> REQUIP_ARMOURY = ATTACHMENTS.register("requip_armoury",
            () -> AttachmentType.builder(() -> new CompoundTag()).serialize(CompoundTag.CODEC, t -> !t.isEmpty()).copyOnDeath().build());
}
