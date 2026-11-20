package com.newuniverse.nusmp.lords;

import com.mojang.serialization.Codec;
import com.newuniverse.nusmp.NUSMP;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Why: where the burdens and masteries of a player are saved: a NeoForge attachment written with the player and copied to the new body on death (the same shape as {@code GreedStore}). Register {@link #ATTACHMENTS} on the mod bus. */
public final class LordStore {
    private LordStore() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, NUSMP.MODID);

    public static final Codec<LordState> CODEC = CompoundTag.CODEC.xmap(LordState::fromTag, LordState::toTag);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<LordState>> STATE = ATTACHMENTS.register("lords",
            () -> AttachmentType.builder(LordState::new).serialize(CODEC, s -> !s.isEmpty()).copyOnDeath().build());

    /** The state of the player, created empty if there is none (use for writing). */
    public static LordState of(Player p) { return p.getData(STATE); }

    /** The state if the player has one, else null (use for reading). */
    public static LordState peek(Player p) { return p.hasData(STATE) ? p.getData(STATE) : null; }
}
