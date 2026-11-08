package com.newuniverse.nusmp.saiyan;

import com.mojang.serialization.Codec;
import com.newuniverse.nusmp.NUSMP;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Why: where the Saiyan progress of a player is saved (3.63.0): a NeoForge attachment written with the player and copied to the new body on death, never synced by NeoForge (the client gets
 * {@link SaiyanNet.Sync}). Reading never CREATES the attachment for a player who is not a Saiyan ({@link #peek}), so the system costs nothing for everyone else. The tag format is hand-written and clamped
 * on read, so a damaged or edited save can not give a value outside the rules. Register {@link #ATTACHMENTS} on the mod bus. Same shape as the Beast Will store.
 */
public final class SaiyanStore {
    private SaiyanStore() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, NUSMP.MODID);

    public static final Codec<SaiyanState> CODEC = CompoundTag.CODEC.xmap(SaiyanTag::fromTag, SaiyanTag::toTag);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<SaiyanState>> STATE = ATTACHMENTS.register("saiyan",
            () -> AttachmentType.builder(SaiyanState::new).serialize(CODEC, s -> !s.isEmpty()).copyOnDeath().build());

    /** The state of the player, created empty if there is none (use for writing). */
    public static SaiyanState of(Player p) { return p.getData(STATE); }

    /** The state if the player has one, else null (use for reading: it never creates an attachment). */
    public static SaiyanState peek(Player p) { return p.hasData(STATE) ? p.getData(STATE) : null; }

    /** True for a player who is a Saiyan (one lookup, no attachment created). */
    public static boolean isSaiyan(Player p) {
        SaiyanState s = peek(p);
        return s != null && s.saiyan;
    }
}
