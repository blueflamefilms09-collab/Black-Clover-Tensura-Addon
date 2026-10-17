package com.newuniverse.nusmp.labyrinth.mega;

import net.minecraft.server.level.ServerLevel;

/**
 * Why: the builders of the Mega-Dungeon work apart and meet only here. The generator announces what happened to a sector and every other
 * system reacts through its own listener, registered in its own registry class ({@code MegaDungeon.addListener}): the ecology assigns
 * factions when a sector was generated, the Crucible rolls nothing until the vault opens, the encounter director starts watching a sector
 * when it becomes playable. Default no-op methods mean a listener only overrides what it needs, and nothing in the generator imports a
 * single class of the systems that listen (so a builder's compile error can never break the generator's package).
 */
public interface SectorListener {
    /** The sector's pieces were placed and its rooms, markers and landmarks were saved (state is ACTIVE). */
    default void onSectorGenerated(ServerLevel level, Sector sector) {}

    /** The Labyrinth Guardian of the sector died (state GUARDIAN_DEAD; its Mana Core exists). */
    default void onGuardianDefeated(ServerLevel level, Sector sector) {}

    /** The sector's Gatekeeper door was opened with the Mana Core (state UNSEALED; the next sector was queued). */
    default void onSectorUnsealed(ServerLevel level, Sector sector) {}

    /** The Crucible of the sector was resolved (a relic was chosen and its catastrophe ran out, or the vault was abandoned). */
    default void onCrucibleResolved(ServerLevel level, Sector sector) {}
}
