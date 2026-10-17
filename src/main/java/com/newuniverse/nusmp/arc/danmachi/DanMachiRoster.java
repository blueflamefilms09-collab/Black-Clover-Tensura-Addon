package com.newuniverse.nusmp.arc.danmachi;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.arc.ArcSaga;

import java.util.List;

/**
 * Why: the four monsters of the Dungeon are records of data (their name, tier, health, floors, story order, Charter Grimoire and lines) before they are entities,
 * and keeping the records here, apart from the entity classes, means the whole of their numbers can be read and tested without a game: that every tier has the
 * health of its band, that the floors cover the Dungeon from the 5th floor to the 50th on both roads, that the story order is a chain, that every skill of every
 * grimoire is a real Tensura skill. Each boss class registers its record from here, so the roster holds the same object the entity class uses.
 *
 * <p>The numbers (health bands of the tiers: 1 is 700 to 1,100, 2 is 1,300 to 2,000, 3 is 2,200 to 3,200): the Minotaur is tier 1 (1,000), the Black Goliath and the
 * Amphisbaena tier 2 (1,800 and 1,900), Udaeus tier 3 (2,600). The floors overlap on purpose (one floor can draw either of two monsters): the Minotaur 5 to 14, the Black
 * Goliath 12 to 26, the Amphisbaena 22 to 36, Udaeus 32 to 50, on both roads. The story order is the order of the floors of the anime they come from (5th, 17th and 18th, 27th, 37th): the Black Goliath also follows the stone Goliath of the floor bosses (the roster's id {@code goliath}; a prerequisite that is not in the roster is skipped), because it is that giant's irregular kin. No record names an arena: the arena of the saga, the Pit of the Dungeon, is the one they are fought in.
 */
public final class DanMachiRoster {
    private DanMachiRoster() {}

    public static final ArcCharacter MINOTAUR = ArcCharacter.boss("minotaur", "Minotaur")
            .title("the Bull of the Labyrinth").source("DanMachi").saga(ArcSaga.DANMACHI).tier(1)
            .colors(0xB4522E, 0xF0B070)
            .stats(1000, 16, 0.30, 8)
            .floors(null, 5, 14).themes("dwarven_forgehold", "gravity", "verdant")
            .charter(0.25, "strengthen_body", "reinforcement", "berserk", "pain_resistance", "elephant_stampede", "earthshatter_kick", "physical_attack_resistance")
            .lines("VWOOOOOOH!", "VWOOOOOOOOOH! The horn grows back, black as the pit!", "...mooo...")
            .build();

    public static final ArcCharacter BLACK_GOLIATH = ArcCharacter.boss("black_goliath", "Black Goliath")
            .title("the Dungeon's Wrath").source("DanMachi").saga(ArcSaga.DANMACHI).tier(2)
            .colors(0xE0502A, 0xFFB060)
            .stats(1800, 24, 0.26, 12)
            .floors(null, 12, 26).themes("gravity", "dwarven_forgehold", "flame")
            .after("minotaur", "goliath")
            .charter(0.30, "giantification", "steel_strength", "earth_storm", "earth_jail", "earth_attack_resistance", "body_armor", "pierce_resistance")
            .lines("GRUOOOOOOH!", "The black hide cracks and the Dungeon roars through it. GRAAAAAAH!", "...gruoh...")
            .build();

    public static final ArcCharacter AMPHISBAENA = ArcCharacter.boss("amphisbaena", "Amphisbaena")
            .title("the Twin-Headed Water Dragon").source("DanMachi").saga(ArcSaga.DANMACHI).tier(2)
            .colors(0x4AB0FF, 0xFF6A5A)
            .stats(1900, 20, 0.27, 8)
            .floors(null, 22, 36).themes("sunken_temple", "storm", "verdant")
            .after("black_goliath")
            .charter(0.30, "flame_breath", "fire_breath", "water_jail", "water_current_control", "magic_jamming", "scale_armor", "water_attack_resistance", "flame_attack_resistance")
            .lines("SHAAAAAAA!", "Both heads rear, the flame and the mist! SHAAAAROOOOAR!", "...shaaa...")
            .build();

    public static final ArcCharacter UDAEUS = ArcCharacter.boss("udaeus", "Udaeus")
            .title("the Skeleton Giant of the White Palace").source("DanMachi").saga(ArcSaga.DANMACHI).tier(3)
            .colors(0x9A8AFF, 0xE8E2CC)
            .stats(2600, 26, 0.25, 10)
            .floors(null, 32, 50).themes("necropolis", "shadow", "cursed_archive")
            .after("amphisbaena")
            .charter(0.30, "create_lesser_undead", "create_greater_undead", "heavy_slash", "aura_sword", "aura_slash", "darkness_attack_resistance", "spiritual_attack_resistance", "curse")
            .lines("KRAAAAAAAH!", "The White Palace rattles: a thousand bones answer! KRAAAAAAH!", "...clatter...")
            .build();

    private static final List<ArcCharacter> BOSSES = List.of(MINOTAUR, BLACK_GOLIATH, AMPHISBAENA, UDAEUS);

    /** The four monsters in the order of the story. */
    public static List<ArcCharacter> bosses() { return BOSSES; }

    /**
     * Adds the four records and the six friends to the roster, WITHOUT loading the entity classes (so it is safe in a test with no game). The game calls
     * {@link DanMachiCast#registerAll()} instead, which also loads the four boss classes so their entity types are registered before the registry events.
     */
    public static void registerRecords() {
        for (ArcCharacter c : BOSSES) ArcRoster.register(c);
        DanMachiNpcs.registerAll();
    }
}
