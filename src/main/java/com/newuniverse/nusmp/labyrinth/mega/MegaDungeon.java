package com.newuniverse.nusmp.labyrinth.mega;

import com.newuniverse.nusmp.labyrinth.Labyrinth;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
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

    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();
    private static final List<SectorListener> LISTENERS = new CopyOnWriteArrayList<>();
    private static final List<FloorListener> FLOOR_LISTENERS = new CopyOnWriteArrayList<>();
    private static volatile Expander expander;
    private static volatile ChainTravel chainTravel;
    private static volatile AlignmentPolicy alignmentPolicy;
    private static volatile ThemeOverride themeOverride;

    // ---------------------------------------------------------------- wiring
    /** Registers a listener (once, from a registry's init; never removed). */
    public static void addListener(SectorListener l) { if (l != null && !LISTENERS.contains(l)) LISTENERS.add(l); }

    public static List<SectorListener> listeners() { return LISTENERS; }

    /** The generator registers itself here at start-up. */
    public static void setExpander(Expander e) { expander = e; }

    public static Expander expander() { return expander; }

    /** 1.55.0: the generator registers how a player is taken into a polarity's chain (the Hub's gates call {@link #enterChain}). */
    public static void setChainTravel(ChainTravel t) { chainTravel = t; }

    /** 1.55.0: the alignment module registers the Directional Lock here; every door asks {@link #mayEnter}. */
    public static void setAlignmentPolicy(AlignmentPolicy a) { alignmentPolicy = a; }

    /** 1.55.0: the theme module registers how a taken relic decides the next sector's theme. */
    public static void setThemeOverride(ThemeOverride t) { themeOverride = t; }

    /** 1.55.0: the theme the sector after {@code previous} must wear because of the relic taken in {@code previous}, or null (no opinion, or no override registered). */
    public static RegionTheme themeOverride(Sector previous) {
        ThemeOverride t = themeOverride;
        if (previous == null) return null;
        RegionTheme chosen = null;
        if (t != null) {
            try { chosen = t.themeAfter(previous); } catch (RuntimeException e) { LOG.warn("theme override failed: {}", e.toString()); }
        }
        if (chosen == null) chosen = com.newuniverse.nusmp.labyrinth.mega.decennial.DecennialThemes.themeAfter(previous);       // 1.99.0: the relic wins; else the Underworld Root darkens the floors past each gate
        return chosen;
    }

    /** 1.55.0: registers a floor listener (once, from a registry's init; never removed). */
    public static void addFloorListener(FloorListener l) { if (l != null && !FLOOR_LISTENERS.contains(l)) FLOOR_LISTENERS.add(l); }

    public static List<FloorListener> floorListeners() { return FLOOR_LISTENERS; }

    /** Takes the player into the chain of a polarity. False when no generator is registered or it refused (it tells the player why). */
    public static boolean enterChain(ServerPlayer p, Polarity polarity) {
        ChainTravel t = chainTravel;
        return t != null && p != null && polarity != null && t.enterChain(p, polarity);
    }

    /** May this player take the road of the polarity (the Directional Lock)? True when no policy is registered. The policy tells the player why when it says no. */
    public static boolean mayEnter(ServerPlayer p, Polarity polarity) {
        AlignmentPolicy a = alignmentPolicy;
        return a == null || p == null || polarity == null || a.mayEnter(p, polarity);
    }

    /** Called by the floor tracker when a player steps into a room of another floor. A listener that throws is skipped and the rest still hear it. */
    public static void fireFloorEntered(ServerPlayer p, int floor, int previous) {
        for (FloorListener l : FLOOR_LISTENERS) {
            try { l.onFloorEntered(p, floor, previous); } catch (RuntimeException e) { LOG.warn("floor listener failed on enter: {}", e.toString()); }
        }
    }

    /** Called by the floor tracker when a player moves on from a floor to a deeper one of the same polarity (once per player and floor). */
    public static void fireFloorCleared(ServerPlayer p, int floor) {
        for (FloorListener l : FLOOR_LISTENERS) {
            try { l.onFloorCleared(p, floor); } catch (RuntimeException e) { LOG.warn("floor listener failed on clear: {}", e.toString()); }
        }
    }

    // ---------------------------------------------------------------- what lives here
    /** The entity tag the Dungeon Master puts on every monster it spawns (a plain scoreboard tag, so it survives a reload with the mob). */
    public static final String DM_TAG = "nusmp_dm";

    /**
     * Is this one of the Dungeon Master's monsters? Tensura's monsters are tamable animals, not the vanilla {@code Enemy} interface the 1.36.0 economy and HP buffer
     * key on, so those two (and the faction drops) ask this as well: a direwolf the Director sent must drop stones and carry the buffer like a zombie does.
     */
    public static boolean isDungeonMonster(Entity e) { return e != null && e.getTags().contains(DM_TAG); }

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

    /**
     * The threat level at the position, 1 and up, or 0 outside any sector. 1.55.0: it follows the FLOOR (a sector is ten floors and threat grows by one per
     * floor, {@link FloorMap#threat}) when the position's floor is known, and falls back to the sector's own number (its first floor) otherwise, so the
     * monsters, loot and ambushes of floor +9 are tougher than those of floor +1 of the same sector.
     */
    public static int threatAt(Level level, BlockPos pos) {
        Sector s = sectorAt(level, pos);
        if (s == null) return 0;
        int floor = floorAt(level, pos);
        return floor != 0 ? FloorMap.threat(floor) : s.threat;
    }

    /**
     * 1.55.0: the SIGNED floor at a position (Holy positive, Abyssal negative, see {@link FloorMap}), or 0 outside the Mega-Dungeon and in the gaps between
     * sectors. A room that was banded by the floor assigner answers with its own floor; a position in a sector that has no bands yet (a 1.54.0 save, or
     * a sector still generating) answers with the first floor of that sector.
     */
    public static int floorAt(Level level, BlockPos pos) {
        Sector s = sectorAt(level, pos);
        if (s == null) return 0;
        Room r = s.roomAt(pos);
        if (r != null && r.floor != 0) return r.floor;
        return FloorMap.floorOf(s.polarity, s.chainIndex, 1, s.perRegion);
    }

    /** 1.55.0: the polarity of the sector at the position, or null outside any sector. */
    public static Polarity polarityAt(Level level, BlockPos pos) {
        Sector s = sectorAt(level, pos);
        return s == null ? null : s.polarity;
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
        if (e != null && com.newuniverse.nusmp.labyrinth.mega.MegaConfig.get(MegaConfig.EXPANSION)) {
            boolean wait = MegaConfig.get(MegaConfig.EXPAND_AFTER_VAULT) && s.vaultCenter != null && s.crucible.phase != Sector.Crucible.Phase.RESOLVED;
            if (wait) {
                s.expansionPending = true;      // the vault comes first: crucibleResolved() or a player at the Way Onward ({@link #requestPending}) starts the next sector
                d.setDirty();
            } else {
                e.requestNext(level, s);
            }
        }
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
        requestPending(level, s);
    }

    /**
     * 1.55.0: starts the next sector of a sector whose Gatekeeper is open and whose expansion was held back (see {@link #unseal}). Called when its Crucible is
     * resolved and by the Way Onward trigger when a player reaches the seal. A no-op when nothing is pending, so every caller may call it freely.
     */
    public static boolean requestPending(ServerLevel level, Sector s) {
        if (s == null || !s.expansionPending) return false;
        Expander e = expander;
        if (e == null || !MegaConfig.get(MegaConfig.EXPANSION)) return false;
        s.expansionPending = false;
        MegaData.get(level.getServer()).setDirty();
        return e.requestNext(level, s);
    }

    /** Called by the generator when a sector's pieces are placed and its data saved (it sets ACTIVE itself first). */
    public static void generated(ServerLevel level, Sector s) {
        for (SectorListener l : LISTENERS) l.onSectorGenerated(level, s);
    }
}
