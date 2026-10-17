package com.newuniverse.nusmp.arc.early;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;

import java.util.List;

/**
 * Why: the five early bosses of Saga 1 (Black Clover, from the Magic Knights Entrance Arc to the Seabed Temple Arc) are defined once, here, as pure
 * {@link ArcCharacter} records with no game types. Each boss class exposes its record as {@code CHARACTER}, so the roster, the floor-boss picker, the story
 * chain, the Charter Grimoire and the boss class all read the same numbers; and because nothing here needs Minecraft, one test ({@code EarlyRosterTest}) can
 * prove what no in-game check can in a build without a game: the story chain is in the order the anime met them, each health sits in its tier's band, every
 * floor range reaches a boss floor (a multiple of ten), every Charter skill is a real Tensura id, and no line speaks out of character.
 *
 * <p>Order of the story (each follows the one before; the Eye of the Midnight Sun bosses of the next builder follow all five): Revchi Salik (the thief of
 * the first chapter), Heath Grice (the attack on Saussy Village), Lotus Whomalt and Mars (the Dungeon Exploration Arc), Vetto (the Seabed Temple Arc).
 * The theme ids are the lower-case ids of {@code RegionTheme}; the ones from the 1.59.0 theme builders are named in {@code docs/labyrinth/mega_v2.md}
 * section 5. The ids of the Charter skills are the Tensura 2.0.1.3 skill ids ({@code tensura_skill_ids.txt} in the test resources).
 */
public final class EarlyRoster {
    private EarlyRoster() {}

    /** The thief of the first chapter: Chain Magic, the Magic Binding. Tier 1, either road, floors 10 and 20. */
    public static final ArcCharacter REVCHI_SALIK = ArcCharacter.boss("revchi_salik", "Revchi Salik").title("the Chain Thief").source("Black Clover")
            .saga(ArcSaga.ELF_REINCARNATION).tier(1).colors(0x6E7A8C, 0xD4DCE8).magic("chain").stats(750, 10, 0.30, 4)
            .floors(null, 10, 20).themes("shadow", "spatial", "gravity", "infernal_foundry", "dwarven_forgehold")
            .charter(0.40, "sticky_steel_thread", "shadow_bind", "curse_bind", "earth_lock", "magic_jamming", "steel_strength", "strengthen_body", "pain_resistance")
            .lines("Nice grimoire. Hand it over and I will let you keep your teeth.",
                    "The Purple Orca threw me away like trash! So I forged chains that never let go!",
                    "Chains... cut like thread... what is that blade...")
            .build();

    /** The Eye of the Midnight Sun's ice mage with the pocket watch: Ice Magic, the Mist, the Timetable. Tier 2, Abyssal road, floors 10 to 30. */
    public static final ArcCharacter HEATH_GRICE = ArcCharacter.boss("heath_grice", "Heath Grice").title("of the Eye of the Midnight Sun").source("Black Clover")
            .saga(ArcSaga.ELF_REINCARNATION).tier(2).colors(0x7FD0FF, 0xEAF8FF).magic("ice").stats(1300, 14, 0.28, 6)
            .floors(Polarity.ABYSSAL, 10, 30).themes("frost", "drowned_crypt", "haunted_manor", "abyss", "shadow")
            .after(REVCHI_SALIK.id())
            .charter(0.30, "icicle_lance", "icicle_rain", "icicle_spear", "ice_wall", "ice_breath", "ice_blizzard", "freeze", "cold_resistance")
            .lines("Tick. Tock. You are standing in my schedule, commoner.",
                    "I will not run late! Everything in the book, at once, now!",
                    "This... was not in the plan...")
            .build();

    /** The sideline general of the Diamond Kingdom: Smoke Magic, the slow poison. Tier 1, either road, floors 10 to 30. */
    public static final ArcCharacter LOTUS_WHOMALT = ArcCharacter.boss("lotus_whomalt", "Lotus Whomalt").title("the Sideline General").source("Black Clover")
            .saga(ArcSaga.ELF_REINCARNATION).tier(1).colors(0x7A6E94, 0xC9C0DC).magic("smoke").stats(1050, 11, 0.26, 5)
            .floors(null, 10, 30).themes("shadow", "gravity", "haunted_manor", "fungal_underdark", "storm")
            .after(HEATH_GRICE.id())
            .charter(0.40, "sleep_mist", "haze", "poisonous_breath", "paralysing_breath", "invisible", "shadow_motion", "darkness", "poison_resistance")
            .lines("Ugh, visitors. Can a general not nap in peace? Fine. Breathe deep, it will not take long.",
                    "All right, all right, I am getting serious. I hate getting serious.",
                    "Ow, ow... this is why I stay at the back... retreating is a plan too...")
            .build();

    /** The Diamond Kingdom's weapon with mage stones in his body: Crystal Magic. Tier 2, Holy road, floors 20 to 40. */
    public static final ArcCharacter MARS = ArcCharacter.boss("mars", "Mars").title("the Crystal Weapon").source("Black Clover")
            .saga(ArcSaga.ELF_REINCARNATION).tier(2).colors(0xA48CE8, 0xE6F4FF).magic("crystal").stats(1600, 15, 0.31, 8)
            .floors(Polarity.HOLY, 20, 40).themes("crystal_geode", "astral", "radiant", "dwarven_forgehold", "spatial")
            .after(LOTUS_WHOMALT.id())
            .charter(0.30, "earth_spikes", "earth_wall", "earth_jail", "body_double", "body_armor", "scale_armor", "physical_attack_resistance", "magic_aura")
            .lines("Intruders. My orders are simple. Nothing leaves this place alive.",
                    "The stones in my body answer me! This is what I was made for!",
                    "So this is what it is... to be broken...")
            .build();

    /** The elf in a human body, the Third Eye's brute: Beast Magic. Tier 2, Abyssal road, floors 30 to 60. */
    public static final ArcCharacter VETTO = ArcCharacter.boss("vetto", "Vetto").title("the Beast of the Third Eye").source("Black Clover")
            .saga(ArcSaga.ELF_REINCARNATION).tier(2).colors(0xD9622B, 0xFFC27A).magic("beast").stats(2000, 20, 0.32, 9)
            .floors(Polarity.ABYSSAL, 30, 60).themes("abyss", "verdant", "spider_warrens", "fungal_underdark", "drowned_crypt")
            .after(MARS.id())
            .charter(0.25, "beast_transformation", "strengthen_body", "agility", "swipe", "elephant_stampede", "physical_attack_resistance", "pain_resistance",
                    "self_regeneration", "danger_sense")
            .lines("Fresh prey! Come, little ones, show me you are worth the hunt!",
                    "Beasts that never lived, come out of me! All of you!",
                    "Hah... a fine hunt... the wild never ends...")
            .build();

    /** The five, in the order of the story. */
    public static List<ArcCharacter> all() { return List.of(REVCHI_SALIK, HEATH_GRICE, LOTUS_WHOMALT, MARS, VETTO); }
}
