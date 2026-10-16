package com.newuniverse.nusmp.avatar.menu;

import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.net.S2C_OpenActionMenuPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Why: a player-made Avatar is renamed and inspected by right-clicking it with an empty hand; this is the server half of that: it checks the player is
 * the creator, then sends one packet that opens the small Name / Info screen with everything the Info tab needs. (Appearance cannot be changed after
 * creation: there is deliberately no action for it.) AV1's interaction hook calls {@link #open}.
 */
public final class AvatarActionMenu {
    private AvatarActionMenu() {}

    public static void open(ServerPlayer player, AvatarEntity avatar) { open(player, avatar, List.of()); }

    /** @param infoLines extra lines for the Info tab (stats the caller knows, such as the rolled D&D scores) */
    public static void open(ServerPlayer player, AvatarEntity avatar, List<String> infoLines) {
        if (player == null || avatar == null || !player.getUUID().equals(avatar.creatorUuid())) return;
        String sec = avatar.secondary() == null ? "" : avatar.secondary().name();
        PacketDistributor.sendToPlayer(player, new S2C_OpenActionMenuPayload(avatar.getUUID(), avatar.avatarName(), avatar.isNamed(), avatar.raceId(),
                avatar.primary() == null ? "" : avatar.primary().name(), sec, avatar.storedLuck(), avatar.appearance().scale(), infoLines == null ? List.of() : infoLines));
    }
}
