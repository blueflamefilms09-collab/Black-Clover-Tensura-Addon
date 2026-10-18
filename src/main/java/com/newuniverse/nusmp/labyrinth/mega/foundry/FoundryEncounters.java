package com.newuniverse.nusmp.labyrinth.mega.foundry;

import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.eco.FactionRegistry;
import com.newuniverse.nusmp.labyrinth.mega.enc.EncounterRoll;
import net.minecraft.world.entity.Mob;

import java.util.Random;

/**
 * Why: this is the whole of the Mob Foundry's contact with the Dungeon Master, kept in one small class so the encounter picker needs only four one-line calls and the rest of
 * the Director stays exactly as it was. The Director asks {@link #pick} for each kind of monster of an encounter entry it has just resolved: with the configured chance
 * ({@code weight}, default 60 percent) the answer is a foundry mob of the FLOOR's tier and the sector's road (Holy road: Holy mobs; Abyssal road: Abyssal ones), preferring the
 * faction the room or the entry asked for and the factions the region's theme likes; otherwise null, and the Director keeps its own pick (so the existing tables keep working
 * unchanged and keep 40 percent of the spawns). The answer is a {@link EncounterRoll.Pick} whose entity text is {@code nusmp:foundry_mob#<id>}; the Director passes that text on
 * unchanged to the budget and the plan (the cost is the mob's health in weak-monster equivalents, so a tier-5 brute eats the budget of five goblins), {@link #baseId} gives the
 * registry id of the type to create, {@link #expect} remembers which mob the next spawn is for, and {@link #prepare} (called by the spawner right after it creates the mob, before it
 * looks for a place) turns the blank mob into that one, so its real size is what the placement checks. The remembered mob is a plain static: the Director runs on the server
 * thread and spawns one mob at a time.
 */
public final class FoundryEncounters {
    private FoundryEncounters() {}

    private static String pendingKey;
    private static int pendingFloor;

    /** The signed floor the foundry tier is read from: the Director's own reading of the party's floor, or, when the floor threat switch is off and it reads 0, the sector's first floor. */
    public static int floorOf(com.newuniverse.nusmp.labyrinth.mega.Sector sector, int viewFloor) {
        if (viewFloor != 0 || sector == null) return viewFloor;
        return com.newuniverse.nusmp.labyrinth.mega.FloorMap.floorOf(sector.polarity, sector.chainIndex, 1, sector.perRegion);
    }

    /** Does a spawn become a foundry one? {@code weight} is a percent, 0..100. */
    public static boolean roll(int weight, Random rnd) { return FoundryRoster.chance(weight, rnd); }

    /**
     * The foundry mob that replaces one monster of an encounter, or null to keep the table's own.
     *
     * @param wantFaction the faction the room or the entry asked for, or null
     */
    public static EncounterRoll.Pick pick(Polarity road, String themeId, int floor, String wantFaction, Random rnd) {
        try {
            if (road == null || !FoundryConfig.get(FoundryConfig.ENABLED) || !roll(FoundryConfig.get(FoundryConfig.WEIGHT), rnd)) return null;
            FoundryRoster roster = FoundryData.roster();
            if (roster.size() == 0) return null;
            FoundryDef d = roster.pick(road, FoundryScaling.tierOf(floor), wantFaction, f -> FactionRegistry.themeWeight(f, themeId), rnd);
            if (d == null) return null;
            double cost = EncounterRoll.cost(FoundryScaling.toughness(FoundryData.archetypeOf(d), d.tier(), floor));
            return new EncounterRoll.Pick(d.entityKey(), cost);
        } catch (RuntimeException | LinkageError ex) {
            return null;                    // never a reason to lose the encounter: the Director keeps its own pick
        }
    }

    /** The faction of the mob a pick names (the Director tags the monster with it), or "" when the text is not a foundry key. */
    public static String factionOf(String key) {
        FoundryDef d = defOf(key);
        return d == null ? "" : d.faction();
    }

    /** The registry id of the entity type to create: {@code nusmp:foundry_mob} for a foundry key, the text itself otherwise. */
    public static String baseId(String key) { return FoundryDef.baseId(key); }

    private static FoundryDef defOf(String key) {
        String id = FoundryDef.idOfKey(key);
        return id == null ? null : FoundryData.roster().byId(id);
    }

    /** The Director is about to spawn this key on this floor: remember it for {@link #prepare}. A non-foundry key clears the memory. */
    public static void expect(String key, int floor) {
        pendingKey = FoundryDef.idOfKey(key) == null ? null : key;
        pendingFloor = floor;
    }

    /** The spawner has just created {@code mob}: when a foundry mob was expected, it becomes that one now (identity, stats, size) and the memory is cleared. */
    public static void prepare(Mob mob) {
        String key = pendingKey;
        pendingKey = null;
        if (key == null || !(mob instanceof FoundryMobEntity f)) return;
        FoundryDef d = defOf(key);
        if (d != null) f.setFoundry(d, pendingFloor);
    }
}
