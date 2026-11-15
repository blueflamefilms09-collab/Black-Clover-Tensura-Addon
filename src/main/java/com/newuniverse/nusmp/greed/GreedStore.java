package com.newuniverse.nusmp.greed;

import com.mojang.serialization.Codec;
import com.newuniverse.nusmp.NUSMP;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Why: where the devoured stats of the Black Sword's wielder are saved: a NeoForge attachment written with the player and copied to the new body on death (the same shape as the Saiyan store). Reading never creates one for anybody else ({@link #peek}). Register {@link #ATTACHMENTS} on the mod bus. */
public final class GreedStore {
    private GreedStore() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, NUSMP.MODID);

    public static final Codec<GreedState> CODEC = CompoundTag.CODEC.xmap(GreedState::fromTag, GreedState::toTag);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<GreedState>> STATE = ATTACHMENTS.register("greed",
            () -> AttachmentType.builder(GreedState::new).serialize(CODEC, s -> !s.isEmpty()).copyOnDeath().build());

    /** The state of the player, created empty if there is none (use for writing). */
    public static GreedState of(Player p) { return p.getData(STATE); }

    /** The state if the player has one, else null (use for reading). */
    public static GreedState peek(Player p) { return p.hasData(STATE) ? p.getData(STATE) : null; }
}
