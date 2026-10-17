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
 * <li><b>A party has seeds of its own</b> (1.58.0). Every party walks its own chain of floors, and two parties that walked the very same floors would make the
 *     dungeon feel like one room copied for each of them, so the party is mixed into the dungeon seed ({@link #partySeed}) before the theme, the relic count and the
 *     sector seed are drawn. Party 0, the shared lane, uses the dungeon seed as it is, so a world saved before parties and a server in {@code shared} mode plan exactly
 *     what they always planned.</li>
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
 * @param party      1.58.0: the party whose chain the sector belongs to (0 = the shared lane)
 */
public record SectorDraft(int id, Polarity road, int chainIndex, int perRegion, int threat, long seed, RegionTheme theme, int relicCount, int party) {

    /** The draft of a sector of the shared lane (party 0): what every caller before 1.58.0 constructed. */
    public SectorDraft(int id, Polarity road, int chainIndex, int perRegion, int threat, long seed, RegionTheme theme, int relicCount) {
        this(id, road, chainIndex, perRegion, threat, seed, theme, relicCount, 0);
    }

    /**
     * The seed a party's sectors derive from: the dungeon seed itself for party 0 (the shared lane, so nothing planned before parties changes), and a mix of the dungeon
     * seed and the party index for every other party.
     */
    public static long partySeed(long dungeonSeed, int party) { return party <= 0 ? dungeonSeed : Rng.mix(dungeonSeed, 0x50415254_0000L + party); }

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

    /** 1.58.0 (party): the first sector of a road on the lane of party {@code party}. */
    public static SectorDraft first(long dungeonSeed, int id, Polarity road, int perRegion, int party) {
        return of(dungeonSeed, id, road, 0, perRegion, null, null, party);
    }

    /**
     * The sector at {@code chainIndex} of a road. {@code previous} is the theme of the sector before it (null for the first) and {@code override} the theme the
     * Crucible's chosen relic forces (null for no opinion).
     */
    public static SectorDraft of(long dungeonSeed, int id, Polarity road, int chainIndex, int perRegion, RegionTheme previous, RegionTheme override) {
        return of(dungeonSeed, id, road, chainIndex, perRegion, previous, override, 0);
    }

    /** 1.58.0 (party): {@link #of(long, int, Polarity, int, int, RegionTheme, RegionTheme)} for a sector of party {@code party}'s chain (see the class comment on its seeds). */
    public static SectorDraft of(long dungeonSeed, int id, Polarity road, int chainIndex, int perRegion, RegionTheme previous, RegionTheme override, int party) {
        long partySeed = partySeed(dungeonSeed, party);
        int idx = Math.max(0, chainIndex);
        int per = Math.max(1, perRegion);
        RegionTheme theme;
        if (FloorMap.isFloorModel(per)) {
            // the floor model: a theme belongs to a DECADE of floors (1..10, 11..20 ...). Only the first floor of a decade draws one (or takes the one the Crucible's relic
            // chose, which is why the override is ignored on every other floor); the rest of the decade wears the theme of the floor before.
            boolean starts = FloorMap.startsDecade(idx, per);
            if (!starts && previous != null) theme = previous;
            else theme = override != null && starts ? override : RegionTheme.pick(partySeed, FloorMap.decadeOf(idx, per), previous, road);
        } else {
            theme = override != null ? override : RegionTheme.pick(partySeed, idx, previous, road);
        }
        int key = seedKey(road, idx);
        return new SectorDraft(id, road, idx, per, SectorThreat.of(road, idx, per), SectorPlanner.sectorSeed(partySeed, key), theme, theme.relicCount(partySeed, key), Math.max(0, party));
    }
}
