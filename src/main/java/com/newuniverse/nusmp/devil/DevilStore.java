package com.newuniverse.nusmp.devil;

import com.mojang.serialization.Codec;
import com.newuniverse.nusmp.NUSMP;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Why: where the Devil Contract of a player is saved (3.48.0): a NeoForge data attachment, written with the player and copied to the new body on death ({@code copyOnDeath}), exactly like the Beast Will
 * ({@code BeastWillStore}). It is never synced by NeoForge: the client gets the small packets of {@link DevilNet}. Reading never CREATES the attachment for a player with no contract ({@link #peek}), so the
 * system costs one lookup for everybody else. The tag format is hand-written ({@link DevilContract#toTag}) and every value is clamped on read, so a damaged or edited save can not give a number outside the rules. Register
 * {@link #ATTACHMENTS} on the mod bus.
 */
public final class DevilStore {
    private DevilStore() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, NUSMP.MODID);

    public static final Codec<DevilContract> CODEC = CompoundTag.CODEC.xmap(DevilContract::fromTag, DevilContract::toTag);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<DevilContract>> CONTRACT = ATTACHMENTS.register("devil_contract",
            () -> AttachmentType.builder(DevilContract::new).serialize(CODEC, c -> !c.isEmpty()).copyOnDeath().build());

    /** The contract of the player, created empty if there is none (use for writing). */
    public static DevilContract of(Player p) { return p.getData(CONTRACT); }

    /** The contract if the player has one, else null (use for reading: it never creates an attachment). */
    public static DevilContract peek(Player p) { return p.hasData(CONTRACT) ? p.getData(CONTRACT) : null; }
}
