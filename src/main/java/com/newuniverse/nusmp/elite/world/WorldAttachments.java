package com.newuniverse.nusmp.elite.world;

import com.newuniverse.nusmp.NUSMP;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Why: the chunk magicule storage is a NeoForge data attachment on the chunk, so it is saved and loaded with the chunk by the game itself and costs nothing for a chunk nobody visited.
 * ({@link ChunkMagicule} says what it holds.)
 */
public final class WorldAttachments {
    private WorldAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, NUSMP.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ChunkMagicule>> CHUNK = ATTACHMENTS.register("chunk_magicule",
            () -> AttachmentType.builder(ChunkMagicule::new).serialize(ChunkMagicule.CODEC).build());
}
