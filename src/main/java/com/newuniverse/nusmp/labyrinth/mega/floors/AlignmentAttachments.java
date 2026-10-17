package com.newuniverse.nusmp.labyrinth.mega.floors;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.newuniverse.nusmp.NUSMP;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Why: this registers the player attachment {@code nusmp:dungeon_alignment} that holds a {@link PlayerDungeonAlignment}. NeoForge 1.21 has no capability
 * for player data; an attachment is its replacement: it is saved with the player, found with {@code player.getData(...)} (created on first use, so no
 * caller null-checks), and {@code copyOnDeath()} carries it to the new body, which is the whole point of a lock that must survive dying. It is serialised
 * with a codec built from the five primitives of the state ({@link PlayerDungeonAlignment#of} and the getters), every field optional, so a save written by
 * an older build, a hand-edited one or one with fields missing still loads. The predicate on the serializer skips the default state, so a player who never
 * touched the dungeon adds nothing to their save file. NeoForge does not send an attachment to the client by itself unless asked; {@link AlignmentNet}
 * sends a small packet of our own (the contract names it {@code nusmp:alignment_sync}) so the sync points are ours to choose: login, respawn, dimension change
 * and every change. Register {@link #ATTACHMENTS} on the mod bus ({@code FloorsRegistry.init} does).
 */
public final class AlignmentAttachments {
    private AlignmentAttachments() {}

    /** The stored form: {@code {chosen: "holy"|"abyssal"|"", locked, holy, abyssal, lockedAt}}, every key optional. */
    public static final Codec<PlayerDungeonAlignment> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("chosen", "").forGetter(PlayerDungeonAlignment::chosenId),
            Codec.BOOL.optionalFieldOf("locked", false).forGetter(PlayerDungeonAlignment::locked),
            Codec.INT.optionalFieldOf("holy", 0).forGetter(PlayerDungeonAlignment::deepestHoly),
            Codec.INT.optionalFieldOf("abyssal", 0).forGetter(PlayerDungeonAlignment::deepestAbyssal),
            Codec.LONG.optionalFieldOf("lockedAt", 0L).forGetter(PlayerDungeonAlignment::lockedAt)
    ).apply(i, PlayerDungeonAlignment::of));

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, NUSMP.MODID);

    /** {@code nusmp:dungeon_alignment}: saved with the player (unless it is still the default), copied on death and on the End return. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerDungeonAlignment>> ALIGNMENT = ATTACHMENTS.register("dungeon_alignment",
            () -> AttachmentType.builder(PlayerDungeonAlignment::new).serialize(CODEC, a -> !a.isDefault()).copyOnDeath().build());

    /** The player's alignment (created empty on first use). The returned object is live: changing it through {@link AlignmentRules} changes the player's save. */
    public static PlayerDungeonAlignment get(Player player) { return player.getData(ALIGNMENT); }
}
