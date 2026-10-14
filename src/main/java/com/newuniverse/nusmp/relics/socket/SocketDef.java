package com.newuniverse.nusmp.relics.socket;

import java.util.List;

/**
 * One thing that can live in a socket: a Spirit of an element or an Underworld Devil. {@code id} is what the {@code nusmp:socket} component stores
 * ("spirit:fire", "devil:megicula"). Pure data; {@link SocketCatalog} holds the table.
 *
 * @param key    the part after the colon ("fire", "megicula")
 * @param name   English display name (the lang file carries the translated one under {@code socket.nusmp.<id with _>})
 * @param rgb    the colour of the emissive pass and the particles
 * @param fx     the ambient particle family
 * @param boons  the passive gifts while the relic is equipped
 * @param act    the active ability
 */
public record SocketDef(SocketKind kind, String key, String name, SocketElement element, int rgb, SocketFxKind fx, List<Boon> boons, ActSpec act) {
    /** "spirit:fire". */
    public String id() { return kind.prefix() + ":" + key; }

    /** The registry path of the Core item: "spirit_core_fire", "devil_core_megicula". */
    public String coreItemId() { return kind.prefix() + "_core_" + key; }

    /** The lang key of the display name. */
    public String nameKey() { return "socket.nusmp." + kind.prefix() + "_" + key; }
}
