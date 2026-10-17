package com.newuniverse.nusmp.avatar.inventory;

import com.newuniverse.nusmp.avatar.AvatarAiConfig;
import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.Stance;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why: the creator tells an Avatar what to do in two places, the buttons of its inventory window and the {@code /avatar} command (for an Avatar that is not within
 * reach: one out in the dungeon cannot be walked up to). Both must do exactly the same thing, so the orders live here once: follow (the default), stay (hold the spot,
 * fight within a leash of it), delve (go into the Labyrinth on its own between rests), recall (follow again, which also calls a delving Avatar home) and share (hand the
 * valuables in its pockets over). Each returns the line to tell the creator. Server side.
 */
public final class AvatarOrders {
    private AvatarOrders() {}

    public enum Order { FOLLOW, STAY, DELVE, RECALL, SHARE }

    /** Gives the order to this Avatar on behalf of its creator; the result is a short line for the action bar. */
    public static String give(ServerPlayer creator, AvatarEntity avatar, Order order) {
        if (creator == null || avatar == null || !avatar.isAlive() || !avatar.isCreator(creator)) return "That is not your Avatar.";
        if (!avatar.isLoyal()) return avatar.avatarName() + " no longer takes orders.";
        String name = avatar.avatarName();
        switch (order) {
            case FOLLOW -> {
                avatar.setStance(Stance.FOLLOW);
                return name + " follows you.";
            }
            case RECALL -> {
                avatar.setStance(Stance.FOLLOW);
                return name + " is called back.";
            }
            case STAY -> {
                avatar.setStance(Stance.STAY);
                return name + " stays here.";
            }
            case DELVE -> {
                if (!AvatarAiConfig.delveEnabled()) return "Delving is switched off on this server.";
                avatar.setStance(Stance.DELVE);
                return name + " will go into the Labyrinth when it is ready.";
            }
            case SHARE -> {
                if (!AvatarAiConfig.lootShareWithCreator()) return "Sharing loot is switched off on this server.";
                if (avatar.level() != creator.level() || avatar.distanceToSqr(creator) > 16.0 * 16.0) return name + " is too far away to hand anything over.";
                int n = AvatarGear.handOver(avatar, creator);
                return n > 0 ? name + " hands you " + n + (n == 1 ? " find." : " finds.") : name + " has nothing worth handing over.";
            }
        }
        return "";
    }

    /** Gives the order and tells the creator on the action bar. Returns true when the order was understood. */
    public static boolean tell(ServerPlayer creator, AvatarEntity avatar, Order order) {
        String line = give(creator, avatar, order);
        if (creator != null) creator.displayClientMessage(Component.literal(line), true);
        return !line.isEmpty();
    }
}
