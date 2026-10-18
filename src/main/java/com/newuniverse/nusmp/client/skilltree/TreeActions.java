package com.newuniverse.nusmp.client.skilltree;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.skilltree.net.SetClassPayload;
import com.newuniverse.nusmp.skilltree.net.UnlockNodePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Why: the screen sends exactly two things to the server, "buy a level of this node" and "make this class active", and both are NeoForge custom payloads (records with a stream codec,
 * registered by the core system's {@code TreeNet}), not a Forge-style channel. They are sent from this one class so that the screen never names a payload type: if the core ever changes
 * a payload's constructor, or routes it through a proxy, this is the single place that changes. The packets carry only ids (route, node, class), never a price or a level: the client is
 * not trusted, the server re-checks everything and answers with the synced tree, and a packet that cannot name a Grimoire page cannot touch one (the isolation rule). A send is never
 * allowed to throw into the screen: {@code PacketDistributor.sendToServer} throws when the connection is gone (the player is being disconnected while the click lands), and an exception
 * out of a click handler would crash the game for what is only a request that no longer matters, so a failure is logged once and the click does nothing.
 */
final class TreeActions {
    private TreeActions() {}

    private static boolean warned;

    /** Asks the server to buy one level of {@code nodeId} in {@code routeId}. */
    static void buy(String routeId, String nodeId) {
        send(new UnlockNodePayload(routeId, nodeId));
    }

    /** Asks the server to make the class {@code classId} (the class id of an unlocked route) the active one. */
    static void setClass(String classId) {
        send(new SetClassPayload(classId));
    }

    /** 1.82.0: asks the server to switch the passive {@code nodeId} of {@code routeId} on or off. The server checks ownership and that the node is bought and has a switch. */
    static void toggle(String routeId, String nodeId, boolean on) {
        send(new com.newuniverse.nusmp.skilltree.line.LinePayloads.TogglePassive(routeId, nodeId, on));
    }

    /** 1.82.0: asks the server to use the art {@code nodeId} of the equipped line {@code routeId}. */
    static void cast(String routeId, String nodeId) {
        send(new com.newuniverse.nusmp.skilltree.line.LinePayloads.CastArt(routeId, nodeId));
    }

    /** 1.82.0: an administrator asks the server for the full tree of the line {@code routeId} (an empty id asks for the cards of every line again). The server ignores anyone else. */
    static void inspect(String routeId) {
        send(new com.newuniverse.nusmp.skilltree.line.LinePayloads.Inspect(routeId));
    }

    private static void send(CustomPacketPayload payload) {
        try {
            PacketDistributor.sendToServer(payload);
        } catch (RuntimeException e) {
            if (warned) return;
            warned = true;
            LogUtils.getLogger().warn("[nusmp] The Skill Tree request could not be sent (is the connection gone?)", e);
        }
    }
}
