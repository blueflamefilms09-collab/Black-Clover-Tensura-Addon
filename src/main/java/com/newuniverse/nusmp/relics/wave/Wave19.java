package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Act;
import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.Look;
import com.newuniverse.nusmp.relics.Passive;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.neoforged.neoforge.common.NeoForge;

import java.util.function.Consumer;

/**
 * Relics of Many Worlds, second set, wave 19: the scrolls, potions and other consumables (and one spellbook). The long card text of each, as game
 * rules, is in the lang keys {@code item.nusmp.<id>.card}; the numbers are in {@link Wave19Rules} (pure), the scrolls in {@link Wave19Scrolls}, the
 * drinks in {@link Wave19Goods}, the book in {@link Wave19Book}, the wild magic table in {@link Wave19Surge}, what stays in the world in
 * {@link Wave19Field} and the handlers in {@link Wave19Events}.
 * <p>
 * Tabletop became Minecraft: a long rest is a Minecraft day, an hour is twenty minutes, a round is six seconds, a saving throw is a roll against a
 * difficulty class (or the target's health, for the great ones), "disadvantage" is bad luck. Scrolls and potions are used up when they work; one
 * that finds nothing to work on is handed back.
 */
public final class Wave19 {
    private Wave19() {}

    private static boolean hooked;

    public static void defs(Consumer<RelicDef> out) {
        // ------------------------------------------------------------ the spellbook
        out.accept(RelicDef.of("spellbook_of_concentration", "Spellbook of Concentration", Grade.RARE, RelicSlot.CHARM)
                .attune().look(Look.TOME).palette(0xA02A2A, 0xD9B25F, 0xFFE9A0)
                .desc("A red spellbook that glows softly and steadies the mind: more magicules and a firmer stance. Once a day it prepares the two spells on its marked page; once a day a short revision lends another page for five minutes.")
                .passive(Passive.attr("tensura:max_magicule", 0.10, Operation.ADD_MULTIPLIED_BASE))
                .passive(Passive.attr("minecraft:generic.knockback_resistance", 0.15, Operation.ADD_VALUE))
                .act(Act.of("prepare_spells", "Prepare Spells", Wave19Book::prepare).cooldown(20).usesPerDay(1))
                .act(Act.of("turn_page", "Turn the Page", Wave19Book::turn).cooldown(10).needsSneak())
                .act(Act.of("revise_spells", "Revise", Wave19Book::revise).hold(100).usesPerDay(1)));

        // ------------------------------------------------------------ potions and sweets
        out.accept(RelicDef.of("fey_honey", "Fey Honey", Grade.RARE, RelicSlot.CONSUMABLE)
                .look(Look.POTION).palette(0xC8D2E0, 0xB0302A, 0xE6F2FF)
                .desc("Silver honey in a jar. Drink it for twenty minutes of irresistible charm: Luck III, Hero of the Village and Thought Communication. When it wears off, fail the save and a wild magic surge bursts out of you.")
                .act(Act.of("drink_fey_honey", "Fey Honey", Wave19Goods::honey)));

        out.accept(RelicDef.of("blood_chocolates", "Blood Chocolates", Grade.SPECIAL, RelicSlot.CONSUMABLE)
                .look(Look.POTION).palette(0x3A1F18, 0x7A1020, 0x9B30FF)
                .desc("Dark chocolate with a bitter, metallic centre. Heals ten health at once, but for seven minutes holy light hurts you twice as much.")
                .act(Act.of("eat_blood_chocolate", "Blood Chocolate", Wave19Goods::chocolate)));

        out.accept(RelicDef.of("ale_lemental", "Ale-lemental", Grade.RARE, RelicSlot.CONSUMABLE)
                .look(Look.POTION).palette(0xC87A30, 0x5A3A1A, 0x58B8FF)
                .desc("A tankard of strange liquid. Pour it out and a water elemental of ale rises to fight at your side for half a minute.")
                .act(Act.of("pour_ale", "Pour the Ale", Wave19Goods::ale)));

        out.accept(RelicDef.of("mug_of_the_boss", "Mug of The Boss", Grade.RARE, RelicSlot.CONSUMABLE)
                .look(Look.POTION).palette(0xF4F4F4, 0x333333, 0xFFD25A)
                .desc("\"World's Best Boss.\" Once a day, chug it for a twenty-minute title (Boss, General, Lich, Chef or Wizard) and awe the creatures that see it. Sneak to change the title. You will sing in the bath and be hopeless at the helm.")
                .act(Act.of("chug_mug", "Chug the Mug", Wave19Goods::mug)));

        out.accept(RelicDef.of("holy_water", "Holy Water", Grade.RARE, RelicSlot.CONSUMABLE)
                .look(Look.POTION).palette(0xCFE8FF, 0xB02A2A, 0xFFFFFF)
                .desc("A flask of blessed water. Hurl it up to twelve blocks: eight radiant damage to fiends and the undead in a small splash, nothing to anyone else.")
                .act(Act.of("hurl_holy_water", "Hurl Holy Water", Wave19Goods::holy)));

        // ------------------------------------------------------------ scrolls
        out.accept(RelicDef.of("scroll_of_unreadable_spells", "Scroll of Unreadable Spells", Grade.RARE, RelicSlot.CONSUMABLE)
                .look(Look.POTION).palette(0xD8CCB0, 0x8B5A2B, 0xFF3030)
                .desc("Red letters no one can read. Read one aloud and roll the wild magic table: seven letters a day, back at dawn. No outcome can kill you.")
                .act(Act.of("read_unreadable", "Read a Letter", Wave19Scrolls::unreadable).cooldown(10).usesPerDay(7)));

        out.accept(RelicDef.of("scroll_of_absorption", "Scroll of Absorption", Grade.LEGEND, RelicSlot.CONSUMABLE)
                .vessel().look(Look.POTION).palette(0xD9C39B, 0xD8A838, 0xB48CFF)
                .desc("Held in a hand, it draws in the next spell aimed only at you and cancels it. Read it afterwards to cast that spell back at a foe.")
                .act(Act.of("release_absorbed", "Cast the Absorbed Spell", Wave19Scrolls::release)));

        out.accept(RelicDef.of("scroll_of_charm_parson_bargain_bin_scrolls_1", "Scroll of Charm Parson (Bargain Bin Scrolls #1)", Grade.RARE, RelicSlot.CONSUMABLE)
                .look(Look.POTION).palette(0xE0D2AC, 0x8B2A2A, 0xFF9AD0)
                .desc("A misspelt Charm Person that works only on clergy. Cleric villagers, holy knights and creatures in holy gear nearby are charmed for five minutes.")
                .act(Act.of("charm_parson", "Charm Parson", Wave19Scrolls::charmParson)));

        out.accept(RelicDef.of("scroll_of_firball_bargain_bin_scrolls_2", "Scroll of Firball (Bargain Bin Scrolls #2)", Grade.RARE, RelicSlot.CONSUMABLE)
                .look(Look.POTION).palette(0xE0D2AC, 0x8B2A2A, 0x2E7D32)
                .desc("A misspelt Fireball: a swirling ball of pine needles that bursts for 12 piercing damage in a small radius. No fire.")
                .act(Act.of("firball", "Firball", Wave19Scrolls::firball)));

        out.accept(RelicDef.of("scroll_of_speak_with_rocks_bargain_bin_scrolls_3", "Scroll of Speak with Rocks (Bargain Bin Scrolls #3)", Grade.RARE, RelicSlot.CONSUMABLE)
                .look(Look.POTION).palette(0xE0D2AC, 0x8B2A2A, 0x9A9A9A)
                .desc("The rocks answer, and have nothing interesting to say, but they know where the nearby ores are.")
                .act(Act.of("speak_with_rocks", "Speak with Rocks", Wave19Scrolls::rocks)));

        out.accept(RelicDef.of("scroll_of_summon_greater_crab_bargain_bin_scrolls_4", "Scroll of Summon Greater Crab (Bargain Bin Scrolls #4)", Grade.RARE, RelicSlot.CONSUMABLE)
                .look(Look.POTION).palette(0xE0D2AC, 0x8B2A2A, 0xE04020)
                .desc("A prototype summoning that was abandoned for a reason: a giant crab claws up in a marked ring and is hostile to everyone, you included. It dissolves after twenty seconds.")
                .act(Act.of("summon_greater_crab", "Summon Greater Crab", Wave19Scrolls::crab)));

        out.accept(RelicDef.of("scroll_of_mace_hand_bargain_bin_scrolls_5", "Scroll of Mace Hand (Bargain Bin Scrolls #5)", Grade.RARE, RelicSlot.CONSUMABLE)
                .look(Look.POTION).palette(0xE0D2AC, 0x8B2A2A, 0xB8B8C8)
                .desc("Your hand turns to solid iron for twenty minutes: with the main hand empty, every punch hits for 5 more.")
                .act(Act.of("mace_hand", "Mace Hand", Wave19Scrolls::mace)));

        out.accept(RelicDef.of("scroll_of_facsimile_copy_bargain_bin_scrolls_6", "Scroll of Facsimile Copy (Bargain Bin Scrolls #6)", Grade.RARE, RelicSlot.CONSUMABLE)
                .look(Look.POTION).palette(0xE0D2AC, 0x8B2A2A, 0x9AD8FF)
                .desc("Hold a tool, weapon, armour piece, food or potion in your other hand and read it: a poorer copy appears in your pack and crumbles within minutes. Copies of copies are poorer still.")
                .act(Act.of("facsimile_copy", "Facsimile Copy", Wave19Scrolls::facsimile)));
    }

    /** Registers the game-bus listeners of this wave; called once from the mod constructor. */
    public static void hooks() {
        if (hooked) return;
        hooked = true;
        NeoForge.EVENT_BUS.addListener(Wave19Events::onIncoming);
        NeoForge.EVENT_BUS.addListener(Wave19Events::onDamagePost);
        NeoForge.EVENT_BUS.addListener(Wave19Events::onChangeTarget);
        NeoForge.EVENT_BUS.addListener(Wave19Events::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(Wave19Events::onServerTick);
        NeoForge.EVENT_BUS.addListener(Wave19Events::onDeath);
        NeoForge.EVENT_BUS.addListener(Wave19Events::onDrops);
        NeoForge.EVENT_BUS.addListener(Wave19Events::onExperience);
        NeoForge.EVENT_BUS.addListener(Wave19Events::onLogout);
        NeoForge.EVENT_BUS.addListener(Wave19Events::onJoin);
        NeoForge.EVENT_BUS.addListener(Wave19Events::onStopping);
    }
}
