package com.newuniverse.nusmp.relics.order;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

/**
 * Where a relic was seen: held by a player, in a container that a player opened, on the ground, on a mob, or nowhere yet (just issued). The ledger
 * keeps the last one of the canonical copy; a duplicate's spot tells the Order where to go.
 * @param holder  the player (PLAYER), the player who opened it (CONTAINER), or the entity (GROUND, MOB); null if none
 * @param pos     {@link BlockPos#asLong()} of the player, the container block, the item entity or the mob
 */
record Spot(Kind kind, UUID holder, String holderName, String dim, long pos) {
    enum Kind { PLAYER, CONTAINER, GROUND, MOB, UNKNOWN }

    static final Spot NOWHERE = new Spot(Kind.UNKNOWN, null, "", "", 0L);

    static String dimOf(ServerLevel level) { return level.dimension().location().toString(); }

    static Spot player(ServerPlayer p) {
        return new Spot(Kind.PLAYER, p.getUUID(), p.getGameProfile().getName(), dimOf(p.serverLevel()), p.blockPosition().asLong());
    }

    /** A container that 'opener' has open: at the block 'at', or at the opener when the container has no place of its own. */
    static Spot container(ServerPlayer opener, BlockPos at) {
        BlockPos where = at == null ? opener.blockPosition() : at;
        return new Spot(Kind.CONTAINER, opener.getUUID(), opener.getGameProfile().getName(), dimOf(opener.serverLevel()), where.asLong());
    }

    static Spot entity(Kind kind, Entity e, ServerLevel level) {
        return new Spot(kind, e.getUUID(), e.getName().getString(), dimOf(level), e.blockPosition().asLong());
    }

    BlockPos blockPos() { return BlockPos.of(pos); }

    /** The same place for the Order's purposes: the same player, the same entity, or the same container block. */
    boolean sameAs(Spot o) {
        if (o == null || kind != o.kind) return false;
        return switch (kind) {
            case CONTAINER -> dim.equals(o.dim) && pos == o.pos;
            case UNKNOWN -> true;
            default -> holder != null && holder.equals(o.holder);
        };
    }

    String describe() {
        return switch (kind) {
            case PLAYER -> "with " + holderName;
            case CONTAINER -> "in a container at " + blockPos().toShortString() + " (" + dim + ")";
            case GROUND -> "on the ground at " + blockPos().toShortString() + " (" + dim + ")";
            case MOB -> "on " + holderName + " at " + blockPos().toShortString() + " (" + dim + ")";
            default -> "nowhere yet";
        };
    }
}
