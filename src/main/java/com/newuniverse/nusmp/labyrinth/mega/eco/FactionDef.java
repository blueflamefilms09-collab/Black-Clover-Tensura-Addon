package com.newuniverse.nusmp.labyrinth.mega.eco;

import com.newuniverse.nusmp.labyrinth.dnd.economy.EconomyMath;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: one faction of the Mega-Dungeon as the data file {@code data/nusmp/mega/factions/<id>.json} describes it, held as plain values so the
 * rivalry graph, the room assignment, the turf-war rules and the drop table can all be written and tested without Minecraft. The file name is the
 * id; membership is NOT stored here (the entity-type tag {@link #tag()} is the membership, so a pack author adds a mob to a faction in a tag
 * and never touches this record). {@code range} and {@code nativeBonus} are {@link #UNSET} when the file has no {@code aggro} block, which
 * tells the rules to use the server config's value at the moment of use; baking the config in at load time would freeze it, because a datapack
 * reload runs before the server config is read. Immutable.
 *
 * @param id          lower-case id, the file name ({@code goblin})
 * @param name        English display name, the fallback of the lang key {@code message.nusmp.mega.mec.faction.<id>}
 * @param color       RGB colour of the faction in messages
 * @param tag         the entity-type tag that lists its members ({@code nusmp:mega/goblin})
 * @param rivals      factions whose members this faction's members hunt (made symmetric by the graph unless {@code oneWay})
 * @param allies      factions this faction never fights (always symmetric; an ally beats a rival if both are listed)
 * @param range       blocks within which a member notices a rival, or {@link #UNSET}
 * @param nativeBonus extra blocks when the rival stands inside the member's native room, or {@link #UNSET}
 * @param territorial also attacks any non-allied intruder that stands in its native room (the constructs), not only rivals
 * @param oneWay      its rivalries are not mirrored: it hunts them, they do not hunt it on their own account
 * @param themeWeights relative weight per region theme id when a room needs a native faction (a missing theme counts 1, 0 excludes)
 * @param meat        the raw cut it leaves (a name from {@link #CUTS})
 * @param meatChance  base chance per player kill of that cut
 */
public record FactionDef(String id, String name, int color, String tag, List<String> rivals, List<String> allies, int range, int nativeBonus,
                         boolean territorial, boolean oneWay, Map<String, Double> themeWeights, String meat, double meatChance) {
    /** The value of {@code range} / {@code nativeBonus} when the file gives none. */
    public static final int UNSET = -1;
    /** Hard ceilings, so a datapack cannot ask for an entity query the size of the dimension. */
    public static final int MAX_RANGE = 48, MAX_BONUS = 32;
    /** The raw cuts a faction may leave: the five items of {@code DungeonFood}, by registry path. */
    public static final List<String> CUTS = List.of("raw_slime_jelly", "raw_armor_flank", "raw_basilisk_fillet", "raw_direwolf_haunch", "raw_monster_meat");

    public FactionDef {
        rivals = List.copyOf(rivals);
        allies = List.copyOf(allies);
        themeWeights = Collections.unmodifiableMap(new LinkedHashMap<>(themeWeights));
    }

    /** The cut as an {@link EconomyMath} kind (the numbers {@code DungeonFood.rawFor} takes); an unknown name is plain monster meat. */
    public int cutKind() {
        return cutKindOf(meat);
    }

    /** The {@link EconomyMath} kind of a cut name, with or without the {@code nusmp:} prefix. */
    public static int cutKindOf(String cut) {
        String c = cut == null ? "" : (cut.startsWith("nusmp:") ? cut.substring(6) : cut);
        return switch (c) {
            case "raw_slime_jelly" -> EconomyMath.JELLY;
            case "raw_armor_flank" -> EconomyMath.FLANK;
            case "raw_basilisk_fillet" -> EconomyMath.FILLET;
            case "raw_direwolf_haunch" -> EconomyMath.HAUNCH;
            default -> EconomyMath.PLAIN;
        };
    }
}
