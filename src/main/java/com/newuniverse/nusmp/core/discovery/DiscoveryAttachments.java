package com.newuniverse.nusmp.core.discovery;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.newuniverse.nusmp.NUSMP;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The player attachment of the discovery system: the set of found keys and the data version. Saved with the player, copied to the new body on
 * death (and unioned again on every clone by {@link DiscoveryTracker}); not synced by NeoForge, the client gets {@link DiscoveryNet.Sync}.
 * Register {@link #ATTACHMENTS} on the mod bus.
 */
public final class DiscoveryAttachments {
    private DiscoveryAttachments() {}

    public static final Codec<DiscoveryData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("v", 0).forGetter(DiscoveryData::version),
            Codec.STRING.listOf().optionalFieldOf("keys", java.util.List.of()).forGetter(DiscoveryData::snapshot)
    ).apply(i, DiscoveryData::new));

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, NUSMP.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<DiscoveryData>> DATA = ATTACHMENTS.register("discovery",
            () -> AttachmentType.builder(() -> new DiscoveryData()).serialize(CODEC).copyOnDeath().build());
}
