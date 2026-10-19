package com.newuniverse.nusmp.celestial;

import com.mojang.serialization.Codec;
import com.newuniverse.nusmp.NUSMP;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Why: the key collection, the contracts and the king's record are SERVER-side state of a player: they must survive logout and death and must not be something a client
 * can edit. This is the player attachment that holds {@link CelestialData}, saved with the player and copied to the new body on death. Register {@link #ATTACHMENTS} on
 * the mod bus.
 */
public final class CelestialAttachments {
    private CelestialAttachments() {}

    public static final Codec<CelestialData> CODEC = Codec.LONG.listOf().xmap(CelestialData::fromList, CelestialData::toList);

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, NUSMP.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CelestialData>> DATA = ATTACHMENTS.register("celestial",
            () -> AttachmentType.builder(() -> new CelestialData()).serialize(CODEC).copyOnDeath().build());

    /** The player's record (created empty on first use). */
    public static CelestialData of(Player p) { return p.getData(DATA); }
}
