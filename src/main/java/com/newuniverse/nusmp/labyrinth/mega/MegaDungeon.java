package com.newuniverse.nusmp.labyrinth.mega;

import com.newuniverse.nusmp.labyrinth.Labyrinth;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Why: the single door every other system uses to talk to the Mega-Dungeon. The builders of the ecology, the encounter director, the rest
 * system, the guardian and the crucible must not import each other (they are written in parallel and a mistake in one must not stop another
 * from compiling), so they share only this facade and the plain-data classes beside it. Queries are read-only and cheap (a linear scan over a
 * few hundred sectors, cached by nothing: sectors do not move); the three EVENTS of the progression ({@link #guardianDefeated},
 * {@link #unseal}, {@link #crucibleResolved}) change the sector's state, save it, tell the listeners and, for {@link #unseal}, ask the
 * {@link Expander} to grow the dungeon. All server thread only; every query tolerates a client level or a null (it answers "no").
 */
public final class MegaDungeon {
    private MegaDungeon() {}

    private static final List<SectorListener> LISTENERS = new CopyOnWriteArrayList<>();
    private static volatile Expander expander;

    // ---------------------------------------------------------------- wiring
    /** Registers a listener (once, from a registry's init; never removed). */
    public static void addListener(SectorListener l) { if (l != null && !LISTENERS.contains(l)) LISTENERS.add(l); }

    public static List<SectorListener> listeners() { return LISTENERS; }

    /** The generator registers itself here at start-up. */
    public static void setExpander(Expander e) { expander = e; }

    public static Expander expander() { return expander; }

    // ---------------------------------------------------------------- where
    /** Is this the Mega-Dungeon dimension? Null-safe, both sides. */
    public static boolean in(Level level) { return Labyrinth.inMega(level); }

    /** The mega dimension's level, or null when the server does not have it. */
    public static ServerLevel level(MinecraftServer server) { return server == null ? null : server.getLevel(Labyrinth.MEGA_DIMENSION); }

    public static MegaData data(MinecraftServer server) { return MegaData.get(server); }

    /** The sector whose region holds the position; null outside the Mega-Dungeon, on a client, or in the gaps between sectors. */
    public static Sector sectorAt(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel sl) || !in(level) || pos == null) return null;
        return MegaData.get(sl.getServer()).sectorAt(pos);
    }

    /** The room (placed piece) holding the position, or null. */
    public static Room roomAt(Level level, BlockPos pos) {
        Sector s = sectorAt(level, pos);
        return s == null ? null : s.roomAt(pos);
    }

    /** Is the position inside a Landing or a Safe Zone (the rest anchors, the no-spawn rooms)? */
    public static boolean inSafeZone(Level level, BlockPos pos) {
        Room r = roomAt(level, pos);
        return r != null && r.kind.isSafe();
    }

    /** The threat level at the position (the sector's, 1 and up), or 0 outside any sector. */
    public static int threatAt(Level level, BlockPos pos) {
        Sector s = sectorAt(level, pos);
        return s == null ? 0 : s.threat;
    }

    /** The theme of the sector at the position, or null. */
    public static RegionTheme themeAt(Level level, BlockPos pos) {
        Sector s = sectorAt(level, pos);
        return s == null ? null : s.theme;
    }

    // ---------------------------------------------------------------- progression events
    /** Called by the guard builder when the Guardian of a sector died and its Mana Core exists. Moves ACTIVE to GUARDIAN_DEAD (once) and tells the listeners. */
    public static void guardianDefeated(ServerLevel level, int sectorId) {
        MegaData d = MegaData.get(level.getServer());
        Sector s = d.sector(sectorId);
        if (s == null || s.state != Sector.State.ACTIVE) return;
        s.state = Sector.State.GUARDIAN_DEAD;
        d.setDirty();
        for (SectorListener l : LISTENERS) l.onGuardianDefeated(level, s);
    }

    /**
     * Called by the Gatekeeper door when the Mana Core opened it: GUARDIAN_DEAD (or ACTIVE, for an admin) becomes UNSEALED, the expansion
     * level rises, the listeners hear it and the {@link Expander} is asked for the next sector. Returns true the first time only.
     */
    public static boolean unseal(ServerLevel level, int sectorId) {
        MegaData d = MegaData.get(level.getServer());
        Sector s = d.sector(sectorId);
        if (s == null || s.state == Sector.State.UNSEALED || s.state == Sector.State.COMPLETE || s.state == Sector.State.GENERATING || s.state == Sector.State.FAILED) return false;
        s.state = Sector.State.UNSEALED;
        d.expansionLevel = Math.max(d.expansionLevel, sectorId + 1);
        d.setDirty();
        for (SectorListener l : LISTENERS) l.onSectorUnsealed(level, s);
        Expander e = expander;
        if (e != null && com.newuniverse.nusmp.labyrinth.mega.MegaConfig.get(MegaConfig.EXPANSION)) e.requestNext(level, s);
        return true;
    }

    /** Called by the crucible builder when a vault is done. UNSEALED becomes COMPLETE (once) and the listeners hear it. */
    public static void crucibleResolved(ServerLevel level, int sectorId) {
        MegaData d = MegaData.get(level.getServer());
        Sector s = d.sector(sectorId);
        if (s == null) return;
        if (s.state == Sector.State.UNSEALED) s.state = Sector.State.COMPLETE;
        d.setDirty();
        for (SectorListener l : LISTENERS) l.onCrucibleResolved(level, s);
    }

    /** Called by the generator when a sector's pieces are placed and its data saved (it sets ACTIVE itself first). */
    public static void generated(ServerLevel level, Sector s) {
        for (SectorListener l : LISTENERS) l.onSectorGenerated(level, s);
    }
}
