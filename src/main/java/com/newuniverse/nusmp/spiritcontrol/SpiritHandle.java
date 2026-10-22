package com.newuniverse.nusmp.spiritcontrol;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

/**
 * Why: the game half of a spirit that the control plane drives. Each spirit package has one small class that implements this on top of the spirit's own service (link, unlink, powered form, abilities per
 * player UUID) and registers it with one line in its Boot; nothing else in the package changes. Every method runs on the server thread and decides everything itself: it re-checks that the player may have
 * the spirit, because the grimoire page and the command ask the same way.
 */
public interface SpiritHandle extends SpiritInfo {
    /** May this player have this spirit right now (the package's own rule: the canon grimoire, the Earth skill, Light Magic ...; its own switch must be on)? */
    boolean eligible(ServerPlayer p);

    /** Eligible for the book of this magic. The Spirit Lord overrides it (its bond type picks the book). */
    default boolean eligibleFor(ServerPlayer p, String magic) { return magics().contains(magic) && eligible(p); }

    /** Is the spirit out beside this player right now? Cheap: a map lookup and an entity id lookup. */
    boolean isOut(ServerPlayer p);

    /** Calls the spirit (the same thing its old key did when it was away). True when it is out afterwards. */
    boolean summon(ServerPlayer p);

    /** Sends the spirit away (the same thing its old key did when it was out). */
    void release(ServerPlayer p);

    /**
     * Uses one special ability (an id of {@link #abilities()}); {@code arg} is the 0-based waypoint slot for the abilities that take one. True when the ability really happened (the page then keeps its price
     * and starts its cooldown; a refusal gives the price back).
     */
    boolean ability(ServerPlayer p, String abilityId, int arg);

    /** Where this spirit's combat spells leave from (its own body when out, else the owner's shoulder). */
    Vec3 origin(ServerPlayer p);

    /** The bond (0..100) that scales its pool and its power; the default is the Spirit Bond's trust when there is one, else a middling bond. */
    default int trust(ServerPlayer p) { return com.newuniverse.nusmp.book.SpiritBond.bonded(p) ? com.newuniverse.nusmp.book.SpiritBond.trust(p) : 50; }

    /** How many waypoint slots the spirit has (the Space spirit only; the slot ability pages cycle through them). */
    default int slots() { return 1; }

    /** The spells this spirit casts for this player (the Spirit Lord's follow the element of its bond). */
    default List<SpiritSpellDef> spellsFor(ServerPlayer p) { return spells(); }

    /** The grimoire magics whose books carry this spirit's pages. */
    @Override
    Set<String> magics();
}
