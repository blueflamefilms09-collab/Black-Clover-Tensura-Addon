package com.newuniverse.nusmp.arc.sds;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;

/**
 * Why: the three records of Meliodas, Escanor and Ban (the Dragon's, the Lion's and the Fox's Sins of The Seven Deadly Sins) live in one pure class, not
 * in the boss classes, so that a unit test can check what no game is needed for and what a typo would break silently: that every Charter skill id is
 * a real Tensura skill, that the grimoires obey the Charter's laws, that the stats stay under Lucius (3600) and in the tier-3 health band, that the
 * floors, roads and prerequisites make sense. Each boss class exposes its record as {@code CHARACTER}, which is what the roster registers.
 *
 * <p>The three are open in any order within the saga: they share one set of prerequisites, the bosses of the Eye of the Midnight Sun (Saga 1). Those
 * ids belong to another builder; an id the roster does not know is skipped by the story rules, so a missing one never locks the group.
 */
public final class SdsCharacters {
    private SdsCharacters() {}

    /** The Eye of the Midnight Sun bosses of Saga 1 that open the Seven Deadly Sins (ids as the Eye's builder is expected to name them). */
    static final String[] EYE_OF_THE_MIDNIGHT_SUN = {"fana", "patolli", "rades_spirito", "rhya"};

    /**
     * Meliodas, the Dragon's Sin of Wrath: Demon King scaling. The skills: Reflector is the Full Counter, Wrath is the rage that grows, Black Flame and
     * Hellfire are the Hellblaze, Demon Lord Haki the Demon King's presence, Aura Sword the Lostvayne in hand.
     */
    public static final ArcCharacter MELIODAS = ArcCharacter.boss("meliodas", "Meliodas")
            .title("the Dragon's Sin of Wrath").source("The Seven Deadly Sins").saga(ArcSaga.SEVEN_DEADLY_SINS).tier(3)
            .colors(0x7A1432, 0xFF5E6E).magic("demon_fire")
            .stats(3200, 20, 0.36, 14)
            .floors(null, 60, 90)
            .themes("flame", "infernal_foundry", "abyss", "blood")
            .after(EYE_OF_THE_MIDNIGHT_SUN)
            .charter(0.30, "reflector", "wrath", "black_flame", "hellfire", "demon_lord_haki", "aura_sword", "strengthen_body", "darkness_attack_resistance")
            .lines("Heh. You want to fight the captain of the Seven Deadly Sins? Then don't hold back. I won't.",
                    "That's enough playing. This is Wrath, and I'm done holding it in!",
                    "Not bad at all. You hit like someone with something to protect.")
            .build();

    /**
     * Escanor, the Lion's Sin of Pride: Sunshine. The skills: Pride is the name, Flame Domination, Heat Wave and the three solar spells are the Cruel
     * Sun, and the two resistances are what lets a player stand near him (the heat round his body is the counter these skills answer).
     */
    public static final ArcCharacter ESCANOR = ArcCharacter.boss("escanor", "Escanor")
            .title("the Lion's Sin of Pride").source("The Seven Deadly Sins").saga(ArcSaga.SEVEN_DEADLY_SINS).tier(3)
            .colors(0xE8A317, 0xFFE680).magic("flame")
            .stats(3000, 24, 0.30, 16)
            .floors(Polarity.HOLY, 20, 80)
            .themes("radiant", "celestial_court", "sky_citadel", "holy_church")
            .after(EYE_OF_THE_MIDNIGHT_SUN)
            .charter(0.30, "pride", "flame_domination", "heat_wave", "solar_flare", "solar_beam", "solar_rain", "flame_attack_resistance", "heat_resistance", "strengthen_body")
            .lines("I am Escanor, the Lion's Sin of Pride. Kneel to the sun, or fall to it.",
                    "The sun does not set on my pride!",
                    "Ha... even the sun sets. It was a good day for a fight.")
            .build();

    /**
     * Ban, the Fox's Sin of Greed: the Fountain of Youth. The skills: Greed and Envy are the taking, Sticky Steel Thread the thread of the Snatch,
     * the two regenerations and the poison nullification the Fountain's gifts, Instant-move the fox's speed.
     */
    public static final ArcCharacter BAN = ArcCharacter.boss("ban", "Ban")
            .title("the Fox's Sin of Greed").source("The Seven Deadly Sins").saga(ArcSaga.SEVEN_DEADLY_SINS).tier(3)
            .colors(0x3C9E6B, 0xD9E8A0).magic("world_tree")
            .stats(2400, 16, 0.38, 8)
            .floors(Polarity.ABYSSAL, 20, 80)
            .themes("verdant", "cursed_archive", "fungal_underdark", "shadow")
            .after(EYE_OF_THE_MIDNIGHT_SUN)
            .charter(0.30, "greed", "envy", "sticky_steel_thread", "ultraspeed_regeneration", "self_regeneration", "poison_nullification", "instant_move", "strengthen_body")
            .lines("A fight, is it? Good. I'll take everything you've got, and then a bit more.",
                    "You were counting my deaths, weren't you? Wrong thing to count. I take; I don't run out.",
                    "Hah... so this is how an end feels. Elaine would never let me hear the last of it.")
            .build();
}
