package com.newuniverse.nusmp.labyrinth.mega.guard;

/**
 * Why: whether a Mana Core opens a sector's Gatekeeper door is a small decision with four ways to say no, and it is the one place a
 * mistake would either lock a party out of the next sector or open a door to the wrong key. It is written as a pure function of three
 * facts the door gathers (the sector named by the core, the sector that holds the clicked door, whether that sector may be opened) so the
 * whole table is unit-tested and the door block only translates the verdict into a message. The order of the checks is the order of the
 * questions a player would ask: is this core forged for anything, does this door belong to a sector, is it THIS sector's core, and is the
 * sector in a state where a door may open.
 */
public final class SectorDoorRules {
    private SectorDoorRules() {}

    public enum Verdict {
        /** The core names this door's sector and the sector may open: consume it, open the slab, unseal. */
        OPEN,
        /** The core carries no sector (a blank one from the creative tab): it hums and does nothing. */
        DORMANT,
        /** The door stands in no sector (an admin-placed door outside the generator's regions): nothing to unseal. */
        NOT_HERE,
        /** A core of another sector: "the core hums, but this is not its seal". */
        WRONG_SECTOR,
        /** The sector is still being generated or failed: opening now would unseal something that is not there. */
        NOT_READY
    }

    /**
     * @param coreSector  the sector id the core names, or a negative number when it names none
     * @param doorSector  the id of the sector that holds the clicked door, or a negative number when it is in none
     * @param sectorReady the sector is playable (any state but GENERATING and FAILED)
     */
    public static Verdict check(int coreSector, int doorSector, boolean sectorReady) {
        if (coreSector < 0) return Verdict.DORMANT;
        if (doorSector < 0) return Verdict.NOT_HERE;
        if (coreSector != doorSector) return Verdict.WRONG_SECTOR;
        if (!sectorReady) return Verdict.NOT_READY;
        return Verdict.OPEN;
    }
}
