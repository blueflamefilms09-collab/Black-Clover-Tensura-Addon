package com.newuniverse.nusmp.elite.items.crate;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.DoubleSupplier;

/**
 * Why: one kind of crate reward ({@code item}, {@code coins}, {@code skill_book}, {@code sp}, {@code xp}). A crate table names a type by its id; the types are registered in
 * {@link RewardSerializers}, a small codec registry, so the shop or the seasons builder can add a type (a title, a plushie of the month) with one {@code register} call and no change to the crate.
 * A type only ever GIVES something: it has no way to run a command.
 */
public interface RewardType {
    /** The id a table's {@code type} field names. */
    String id();

    /** Checks an entry when the table loads; null when it is fine, else the problem (an unknown item id, a missing field). Called once per entry on a reload, not per opening. */
    default String validate(RewardEntry entry) { return null; }

    /**
     * Gives {@code amount} of the entry to the player.
     *
     * @return the line to show the player ("3x Iron Ingot"), or null when nothing could be given (the economy is off, the skill tree is off, the item no longer exists); the opening then
     * re-rolls that slot with this type excluded, so a switched-off system never costs the player a reward
     */
    Component grant(ServerPlayer player, RewardEntry entry, int amount, DoubleSupplier dice);
}
