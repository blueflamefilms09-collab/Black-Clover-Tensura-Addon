package com.newuniverse.nusmp.skilltree.net;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Why: NeoForge 21.1 does not sync an attachment to the client by itself, and the Skill Tree screen, the cast gate that the skill menu asks on the client, and every tooltip need the player's
 * tree on the client. The tree is a few hundred bytes (routes, Skill Points, node levels, the class), so the server sends ONE full snapshot, as the compound the tree already saves itself as
 * ({@code IPlayerSkillTree.toTag}), and the client replaces its copy with it: no delta format to get out of step, no merge to get wrong, and the saved form and the wire form can never
 * differ. It is server-to-client only (the client never sends its tree; it sends the two small requests {@link UnlockNodePayload} and {@link SetClassPayload}, and the server decides).
 * Payload id {@code nusmp:tree_sync}.
 *
 * @param tree the whole tree of the receiving player as written by {@code toTag()}
 */
public record TreeSyncPayload(CompoundTag tree) implements CustomPacketPayload {
    public static final Type<TreeSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "tree_sync"));
    public static final StreamCodec<FriendlyByteBuf, TreeSyncPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, TreeSyncPayload::tree, TreeSyncPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
