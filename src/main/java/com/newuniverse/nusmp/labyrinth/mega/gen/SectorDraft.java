package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;

/**
 * Why: since 1.55.0 the dungeon is two roads (the Holy chain climbs, the Abyssal chain descends) and a sector is created in two different ways: the first sector of a
 * road at that road's lane origin, and every later one behind the Way Onward of the sector before it. Everything that is DECIDED when a sector is created (which
 * road, which place in the chain, how many floors, how hard, which theme, how many relics, which seed) is the same arithmetic in both cases, and it is the part of
 * the creation that the rest of the mod relies on: the floors module numbers rooms from {@code polarity + chainIndex + perRegion}, the encounter director, the
 * Guardian, the factions and the rest ambush read {@code threat}, the Crucible reads {@code relicCount}, the theme processor reads {@code theme}. So it is made here,
 * pure (no Minecraft type, only the pure enums), and pinned by tests, and the game-side queue only copies the answer into a {@code Sector}.
 *
 * <p>The three rules that are easy to get wrong:
 * <ul>
 * <li><b>The seed belongs to the PLACE in the road, not to the id.</b> Sector ids are global (the newest sector + 1), so which id a road's third sector gets depends on
 *     which road the players opened first. If the id fed the seed, two worlds with the same dungeon seed would build different dungeons depending on who walked where
 *     first. {@link #seedKey} is the number of the place (road and chain index), so the Holy road's third sector is the same sector in every world with that seed.
 *     The key of the first Holy sector is 0, the number 1.54.0 used for its sector 0, so a plan of that sector is unchanged.</li>
 * <li><b>The theme is the Crucible's choice first.</b> The relic taken in the previous sector's vault may force the next theme; only when nobody forced one does the
 *     seeded, road-biased draw ({@link RegionTheme#pick(long, int, RegionTheme, Polarity)}) decide, and the first sector of a road wears the road's signature theme.
 *     1.58.0, the floor model: a theme belongs to a DECADE of floors, so only the first floor of each decade (floor 1, 11, 21 ...) draws one (the draw is made for the decade
 *     index) or takes the Crucible's choice; the other nine floors keep the theme of the floor before.</li>
 * <li><b>Threat counts floors.</b> It is {@link SectorThreat#of}: the distance from Floor 0 of the sector's first floor, not {@code 1 + id}.</li>
 * </ul>
 *
 * @param id         the global sector id
 * @param road       which road the sector belongs to
 * @param chainIndex 0-based place in its road
 * @param perRegion  floors per sector, the same for the whole road
 * @param threat     {@link SectorThreat#of}
 * @param seed       the sector's own seed ({@code SectorPlanner.sectorSeed(dungeonSeed, seedKey(road, chainIndex))})
 * @param theme      the sector's theme
 * @param relicCount how many relics its Crucible offers
 */
public record SectorDraft(int id, Polarity road, int chainIndex, int perRegion, int threat, long seed, RegionTheme theme, int relicCount) {

    /** The number of a PLACE in the dungeon: two per chain index, the Holy road even and the Abyssal road odd. */
    public static int seedKey(Polarity road, int chainIndex) { return Math.max(0, chainIndex) * 2 + (road == Polarity.ABYSSAL ? 1 : 0); }

    /** The first sector of a road: chain index 0, the default number of floors, the road's signature theme. */
    public static SectorDraft first(long dungeonSeed, int id, Polarity road) {
        return of(dungeonSeed, id, road, 0, FloorMap.DEFAULT_PER_REGION, null, null);
    }

    /** 1.58.0: the first sector of a road with {@code perRegion} floors per sector (1 = the floor model). */
    public static SectorDraft first(long dungeonSeed, int id, Polarity road, int perRegion) {
        return of(dungeonSeed, id, road, 0, perRegion, null, null);
    }

    /**
     * The sector at {@code chainIndex} of a road. {@code previous} is the theme of the sector before it (null for the first) and {@code override} the theme the
     * Crucible's chosen relic forces (null for no opinion).
     */
    public static SectorDraft of(long dungeonSeed, int id, Polarity road, int chainIndex, int perRegion, RegionTheme previous, RegionTheme override) {
        int idx = Math.max(0, chainIndex);
        int per = Math.max(1, perRegion);
        RegionTheme theme;
        if (FloorMap.isFloorModel(per)) {
            // the floor model: a theme belongs to a DECADE of floors (1..10, 11..20 ...). Only the first floor of a decade draws one (or takes the one the Crucible's relic
            // chose, which is why the override is ignored on every other floor); the rest of the decade wears the theme of the floor before.
            boolean starts = FloorMap.startsDecade(idx, per);
            if (!starts && previous != null) theme = previous;
            else theme = override != null && starts ? override : RegionTheme.pick(dungeonSeed, FloorMap.decadeOf(idx, per), previous, road);
        } else {
            theme = override != null ? override : RegionTheme.pick(dungeonSeed, idx, previous, road);
        }
        int key = seedKey(road, idx);
        return new SectorDraft(id, road, idx, per, SectorThreat.of(road, idx, per), SectorPlanner.sectorSeed(dungeonSeed, key), theme, theme.relicCount(dungeonSeed, key));
    }
}
