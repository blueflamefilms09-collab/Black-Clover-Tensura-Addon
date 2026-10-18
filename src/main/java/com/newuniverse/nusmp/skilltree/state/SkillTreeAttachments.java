package com.newuniverse.nusmp.skilltree.state;

import com.mojang.serialization.Codec;
import com.newuniverse.nusmp.NUSMP;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Why: where the {@link IPlayerSkillTree} of a player lives: a NeoForge attachment {@code nusmp:skill_tree}, saved with the player (and only for a player who has something: an empty tree writes
 * nothing, so a world full of players who never opened a book does not grow), copied to the new body when the player dies or comes back from the End (an unlocked route must survive a death,
 * exactly like the dungeon alignment and the arc story do), and readable on the client for the LOCAL player once the sync packet has set it. NeoForge 21.1 does not sync attachments by itself,
 * so {@code SkillTreeApi.resync} sends the packet; that is a few lines and keeps the rule "the server decides, the client only shows" explicit.
 */
public final class SkillTreeAttachments {
    private SkillTreeAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, NUSMP.MODID);

    private static final Codec<IPlayerSkillTree> CODEC = CompoundTag.CODEC.xmap(PlayerSkillTree::fromTag, IPlayerSkillTree::toTag);

    /** {@code nusmp:skill_tree}. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<IPlayerSkillTree>> TREE = ATTACHMENTS.register("skill_tree",
            () -> AttachmentType.<IPlayerSkillTree>builder(() -> new PlayerSkillTree()).serialize(CODEC, t -> !t.isEmpty()).copyOnDeath().build());

    /** The player's tree (created empty on first use). The returned object is live: changing it changes the player's save. */
    public static IPlayerSkillTree get(Player player) { return player.getData(TREE); }
}
