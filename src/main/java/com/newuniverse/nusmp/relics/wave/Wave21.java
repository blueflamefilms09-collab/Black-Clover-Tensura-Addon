package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Act;
import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.Look;
import com.newuniverse.nusmp.relics.Passive;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

import java.util.function.Consumer;

/**
 * Why: Relics of Many Worlds part 4, wave 21: twelve weapons (Rift Breaker, Scimitar of the Seasons, Silver-Wind Mountain Glaive, Spider Sword, Stick of
 * Youth, Sun Gift, The Last Horseman, The Legacy, The Librarian's Maul, Time hammer, Vanguard Hammer, Willbreaker). {@link #defs} lists them for the
 * catalog (the core registers the items), {@link #hooks} registers the game-bus listeners once. The act bodies are in {@link Wave21Acts}, the listeners in
 * {@link Wave21Events}, the numbers in {@link Wave21Rules}. The long card texts, with every tabletop rule turned into a game rule, are in the lang
 * fragment relics_wave21.lang.json and in doc_wave21.md.
 */
public final class Wave21 {
    private Wave21() {}

    private static final String REACH = "minecraft:player.entity_interaction_range";

    public static void defs(Consumer<RelicDef> out) {
        // ---------------------------------------------------------------- Rift Breaker (rare, attunement): a spear
        out.accept(RelicDef.of("rift_breaker", "Rift Breaker", Grade.RARE, RelicSlot.MAIN_HAND).attune().model3d()
                .palette(0x6A3A94, 0xFF5AC0, 0xFFE27A).look(Look.STAFF).melee(6.5, 1.3)
                .engrave("barrier_piercing", "magic_weapon", "swift")
                .desc("A spear with a crystal tip sharp enough to pierce reality. Once a day a Dimension Door; four times a day it strikes through a rift at a creature up to 18 blocks away (sneak: it flies the rift line and cuts everything on it). Once only, drive it into the ground to open an escape circle: the spear is destroyed.")
                .passive(Passive.attr(REACH, 0.5, Operation.ADD_VALUE))
                .passive(Passive.flag(Wave21Rules.F_RIFT))
                .act(Act.of("rift_strike", "Rift Strike", Wave21Acts::riftStrike).cooldown(60).usesPerDay(4))
                .act(Act.of("dimension_door", "Dimension Door", Wave21Acts::dimensionDoor).cooldown(100).usesPerDay(1))
                .act(Act.of("escape_rift", "Escape Rift", Wave21Acts::escapeRift).needsSneak().hold(40).cooldown(200).mana(3)));

        // ---------------------------------------------------------------- Scimitar of the Seasons (very rare)
        out.accept(RelicDef.of("scimitar_of_the_seasons", "Scimitar of the Seasons", Grade.UNIQUE, RelicSlot.MAIN_HAND).model3d()
                .palette(0x56D6AA, 0x4A2C2C, 0xD42A50).look(Look.SWORD).melee(7.5, 1.7)
                .engrave("elemental_boost", "growth", "severance")
                .desc("A jade scimitar on a twisted branch. Speak the word (use it) and every blow bites for 2d6 extra of the season's kind: radiant in spring, fire in summer, necrotic in autumn, cold in winter. A season lasts eight Minecraft days. Use it again to dismiss the effect.")
                .passive(Passive.flag(Wave21Rules.F_SEASONS))
                .act(Act.of("command_word", "Command Word", Wave21Acts::commandWord).toggle().cooldown(20)));

        // ---------------------------------------------------------------- Silver-Wind Mountain Glaive (rare, attunement)
        out.accept(RelicDef.of("silver_wind_mountain_glaive", "Silver-Wind Mountain Glaive", Grade.RARE, RelicSlot.MAIN_HAND).attune().model3d()
                .palette(0x3C3E48, 0xF4F0F8, 0x9CCBFF).look(Look.STAFF).melee(10.0, 0.9)
                .engrave("elemental_boost", "sturdy", "magic_interference")
                .desc("A cloud giant's double-headed glaive with ten charges. Forceful Strike blasts a gale in front of you, Whirlwind roots you in a spinning storm that drags foes in, Become the Hurricane strikes up to five foes like a steel wind. Charges come back, about seven a day.")
                .passive(Passive.attr(REACH, 1.5, Operation.ADD_VALUE))
                .passive(Passive.skill("tensura:wind_attack_resistance"))
                .passive(Passive.flag(Wave21Rules.F_GLAIVE))
                .act(Act.of("forceful_strike", "Forceful Strike", Wave21Acts::forcefulStrike).cooldown(60))
                .act(Act.of("whirlwind", "Whirlwind Maneuver", Wave21Acts::whirlwind).cooldown(240).usesPerDay(2))
                .act(Act.of("hurricane", "Become the Hurricane", Wave21Acts::hurricane).cooldown(400).epPercent(1.0)));

        // ---------------------------------------------------------------- Spider Sword (rare, attunement)
        out.accept(RelicDef.of("spider_sword", "Spider Sword", Grade.RARE, RelicSlot.MAIN_HAND).attune().model3d()
                .palette(0x40424E, 0xBE2822, 0xFF5030).look(Look.SWORD).melee(5.5, 2.0)
                .engrave("enervation", "severance", "swift")
                .desc("A scimitar coated in poisonous red resin: every hit adds poison. Dash and the soles stick: for ten seconds you climb walls like a spider. Spin a web to bind a thief in place.")
                .passive(Passive.skill("tensura:poison_resistance"))
                .passive(Passive.flag(Wave21Rules.F_SPIDER))
                .act(Act.of("spider_climb", "Spider Climb", Wave21Acts::spiderClimb).cooldown(200))
                .act(Act.of("web_snare", "Web Snare", Wave21Acts::webSnare).cooldown(160)));

        // ---------------------------------------------------------------- Stick of Youth (no rarity printed: rare)
        out.accept(RelicDef.of("stick_of_youth", "Stick of Youth", Grade.RARE, RelicSlot.MAIN_HAND).model3d()
                .palette(0x96683E, 0xC8965F, 0x70B040).look(Look.STAFF).melee(5.0, 1.5)
                .engrave("growth", "vitality", "sturdy")
                .desc("A knotted wooden stick that looks like a plain club. In young hands it can take the form of any common weapon: use it to turn it from club to sword, axe, spear and dagger, and each form strikes the way that weapon does.")
                .passive(Passive.flag(Wave21Rules.F_STICK))
                .act(Act.of("take_form", "Take Form", Wave21Acts::takeForm).cooldown(10)));

        // ---------------------------------------------------------------- Sun Gift (no rarity printed: rare, attunement)
        out.accept(RelicDef.of("sun_gift", "Sun Gift", Grade.RARE, RelicSlot.MAIN_HAND).attune().model3d()
                .palette(0xC4A046, 0xDEE2EC, 0xFFF0AA).look(Look.STAFF).melee(11.0, 1.0)
                .engrave("holy_weapon", "elemental_boost", "magic_weapon", "severance")
                .desc("A twin-bladed glaive that drinks sunlight: every blow adds radiant damage. Four charges (they come back in the sun) cast Guiding Bolt, Fireball and Flame Strike; a Firebolt needs no charge.")
                .passive(Passive.attr(REACH, 1.5, Operation.ADD_VALUE))
                .passive(Passive.skill("tensura:light_attack_resistance"))
                .passive(Passive.flag(Wave21Rules.F_SUN))
                .act(Act.of("guiding_bolt", "Guiding Bolt", Wave21Acts::guidingBolt).cooldown(60))
                .act(Act.of("fireball", "Fireball", Wave21Acts::fireball).cooldown(120))
                .act(Act.of("flame_strike", "Flame Strike", Wave21Acts::flameStrike).cooldown(200))
                .act(Act.of("firebolt", "Firebolt", Wave21Acts::firebolt).cooldown(30)));

        // ---------------------------------------------------------------- The Last Horseman (no rarity printed: rare, attunement)
        out.accept(RelicDef.of("the_last_horseman", "The Last Horseman", Grade.RARE, RelicSlot.MAIN_HAND).attune().model3d()
                .palette(0x8032C0, 0xC8A03C, 0xD8A0FF).look(Look.STAFF).melee(11.0, 0.9)
                .engrave("soul_eater", "ruination", "enervation", "severance")
                .desc("A scythe wreathed in violet flame. Its blows add necrotic damage and eat away at the target's maximum health. It can steady the dying, sense the dead, summon a Nightmare to ride, and with its two charges raise undead or speak a Word of Ruin.")
                .passive(Passive.attr(REACH, 1.5, Operation.ADD_VALUE))
                .passive(Passive.skill("tensura:darkness_attack_resistance"))
                .passive(Passive.flag(Wave21Rules.F_HORSEMAN))
                .act(Act.of("spare_the_dying", "Spare the Dying", Wave21Acts::spareTheDying).cooldown(300))
                .act(Act.of("speak_with_dead", "Speak With Dead", Wave21Acts::speakWithDead).cooldown(200))
                .act(Act.of("find_steed", "Find Steed: Nightmare", Wave21Acts::findSteed).cooldown(1200).usesPerDay(1).mana(2))
                .act(Act.of("create_undead", "Create Undead", Wave21Acts::createUndead).cooldown(600).epPercent(1.0))
                .act(Act.of("word_of_ruin", "Word of Ruin", Wave21Acts::wordOfRuin).cooldown(400).epPercent(2.0)));

        // ---------------------------------------------------------------- The Legacy (no rarity printed: rare, attunement)
        out.accept(RelicDef.of("the_legacy", "The Legacy", Grade.RARE, RelicSlot.MAIN_HAND).attune().model3d()
                .palette(0x68707C, 0x28B4A8, 0x78DCFF).look(Look.HAMMER).melee(10.0, 0.95)
                .engrave("elemental_boost", "crushing", "magic_weapon")
                .desc("A warhammer whose head levitates above its handle: it reaches 3.5 blocks farther and every blow adds thunder. Toss the head up to 36 blocks to hit everything on the line; until it returns the hammer is a poor improvised club.")
                .passive(Passive.attr(REACH, 3.5, Operation.ADD_VALUE))
                .passive(Passive.skill("tensura:electricity_resistance"))
                .passive(Passive.flag(Wave21Rules.F_LEGACY))
                .act(Act.of("toss_head", "Toss the Head", Wave21Acts::tossHead).cooldown(100))
                .act(Act.of("recall_head", "Recall", Wave21Acts::recallHead).cooldown(40).needsSneak()));

        // ---------------------------------------------------------------- The Librarian's Maul
        out.accept(RelicDef.of("the_librarians_maul", "The Librarian's Maul", Grade.RARE, RelicSlot.MAIN_HAND).model3d()
                .palette(0x4A9AA0, 0xA8AAA8, 0x90F0E0).look(Look.HAMMER).melee(9.0, 0.9)
                .engrave("crushing", "magic_capacity", "sturdy")
                .desc("A granite book on a pole. Strength and learning both land the blow: every experience level adds damage, and with the off hand empty it hits like the two-handed 1d12. Quiet Please slams the book shut: nearby foes are hurt, slowed and silenced.")
                .passive(Passive.flag(Wave21Rules.F_MAUL))
                .act(Act.of("quiet_please", "Quiet Please", Wave21Acts::quietPlease).cooldown(160)));

        // ---------------------------------------------------------------- Time hammer (legendary, attunement)
        out.accept(RelicDef.of("time_hammer", "Time hammer", Grade.LEGEND, RelicSlot.MAIN_HAND).attune().model3d().vessel()
                .palette(0xB8B2AA, 0xFF7A3C, 0xFF9650).look(Look.HAMMER).melee(12.0, 0.9)
                .engrave("stagnation", "lethargy", "crushing", "magic_weapon")
                .desc("A chronomancer's stone hammer. Three times a day raise it and say Stop: every foe near you freezes for five seconds while you move at haste. Strike in the last two seconds and call Hammertime for a thunderous smite.")
                .passive(Passive.skill("tensura:spatial_attack_resistance"))
                .passive(Passive.flag(Wave21Rules.F_TIME))
                .act(Act.of("stop", "Stop", Wave21Acts::stopTime).cooldown(600).usesPerDay(3).epPercent(1.5)));

        // ---------------------------------------------------------------- Vanguard Hammer (rare, attunement)
        out.accept(RelicDef.of("vanguard_hammer", "Vanguard Hammer", Grade.RARE, RelicSlot.MAIN_HAND).attune().model3d()
                .palette(0x969AA4, 0xC81C38, 0xFF5070).look(Look.HAMMER).melee(8.5, 1.0)
                .engrave("crushing", "sturdy", "vigor")
                .desc("A banded steel warhammer with a crimson ribbon. Once a day let out a war cry and smash the ground: every creature in a 12-block cone is frightened for a minute and runs from you.")
                .passive(Passive.flag(Wave21Rules.F_VANGUARD))
                .act(Act.of("war_cry", "War Cry", Wave21Acts::warCry).cooldown(200).usesPerDay(1)));

        // ---------------------------------------------------------------- Willbreaker (no rarity printed: rare)
        out.accept(RelicDef.of("willbreaker", "Willbreaker", Grade.RARE, RelicSlot.MAIN_HAND).model3d()
                .palette(0x6C6E7C, 0xD2242C, 0xFF5A50).look(Look.HAMMER).melee(8.0, 1.0)
                .engrave("crushing", "enervation")
                .desc("A war hammer that carries the battle cries of its dead owners. A critical hit fills the air with them: the target, and the foes beside it, may be frightened. Call the cries on purpose to frighten everything close.")
                .passive(Passive.flag(Wave21Rules.F_WILL))
                .act(Act.of("battle_cries", "Battle Cries", Wave21Acts::battleCries).cooldown(400)));
    }

    /** Registers the wave's game-bus listeners (called once from the mod constructor). */
    public static void hooks() {
        Wave21Events.register();
    }
}
