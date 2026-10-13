package com.newuniverse.nusmp.relics.djinn;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The player attachment of the Djinn system: one small compound ({@link DjinnState} reads and writes it). It is saved with the player, copied to
 * the new body on death (the code decides what a death clears) and not synced by NeoForge: the client gets {@link DjinnNet.Sync} instead.
 * Register {@link #ATTACHMENTS} on the mod bus.
 */
public final class DjinnAttachments {
    private DjinnAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, NUSMP.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CompoundTag>> STATE = ATTACHMENTS.register("djinn_state",
            () -> AttachmentType.builder(() -> new CompoundTag()).serialize(CompoundTag.CODEC, t -> !t.isEmpty()).copyOnDeath().build());

    public static void init() {}
}
