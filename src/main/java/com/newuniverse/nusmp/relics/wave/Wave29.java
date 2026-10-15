package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Act;
import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.Look;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicSlot;
import net.neoforged.neoforge.common.NeoForge;

import java.util.function.Consumer;

/**
 * Why: Relics of Many Worlds, part 4, wave 29: four potions (Pocket Portal, Potion of Vampirism, Romlo's Potion of Sleep, Spider Brew). The long card text of
 * each, as game rules, is in the lang keys {@code item.nusmp.<id>.card}; the numbers are in {@link Wave29Rules} (pure), the portal in {@link Wave29Portal},
 * the three drinks in {@link Wave29Brews} and the handlers in {@link Wave29Events}.
 * <p>
 * Tabletop became Minecraft: a round is 6 s, 5 ft is a block, a long rest (or "a day") is a Minecraft day, a saving throw is a d20 roll against a difficulty
 * class. Potions are used up when they work; one that finds nothing to work on is handed back.
 */
public final class Wave29 {
    private Wave29() {}

    private static boolean hooked;

    public static void defs(Consumer<RelicDef> out) {
        out.accept(RelicDef.of("pocket_portal", "Pocket Portal", Grade.UNIQUE, RelicSlot.CONSUMABLE)
                .look(Look.POTION).palette(0x8A5CD0, 0x3A2466, 0xA0C4FF)
                .desc("A glass orb of swirling purple. Sneak to brew it in the spot you stand; smash it elsewhere and a ring of light opens for twelve seconds that carries everyone who steps in back to that spot.")
                .act(Act.of("smash_pocket_portal", "Smash the Orb", Wave29Portal::smash)));

        out.accept(RelicDef.of("potion_of_vampirism", "Potion of Vampirism", Grade.UNIQUE, RelicSlot.CONSUMABLE)
                .look(Look.POTION).palette(0xF0E6D0, 0xD0182C, 0xFF3050)
                .desc("A skull-shaped flask with red eyes. Drink and roll a d20: most often a curse (sunlight and holy light burn you for three days), sometimes the Children of the Night (wolves and bats answer your wounds for five days), rarely Regeneration for good.")
                .act(Act.of("drink_vampirism", "Drink the Potion", Wave29Brews::vampirism)));

        out.accept(RelicDef.of("romlos_potion_of_sleep", "Romlo's Potion of Sleep", Grade.SPECIAL, RelicSlot.CONSUMABLE)
                .look(Look.POTION).palette(0x9CF03C, 0x1E1E24, 0xCFFF9A)
                .desc("Bright lime liquid. Whoever drinks it must save or sleep for five minutes; sneak to pour it into the mouth of the creature in front of you. Damage wakes the sleeper.")
                .act(Act.of("drink_sleep", "Drink or Pour", Wave29Brews::sleep)));

        out.accept(RelicDef.of("spider_brew", "Spider Brew", Grade.SPECIAL, RelicSlot.CONSUMABLE)
                .look(Look.POTION).palette(0x3A78E0, 0xE8C048, 0x40E0C8)
                .desc("A bottle of blue liquid wrapped in cobweb. Drink to become a giant spider for five to twenty minutes with your own mind: climb walls, a poisonous bite, more health and armour, night vision.")
                .act(Act.of("drink_spider_brew", "Drink the Brew", Wave29Brews::spiderBrew)));
    }

    /** Registers the game-bus listeners of this wave; called once from the mod constructor. */
    public static void hooks() {
        if (hooked) return;
        hooked = true;
        NeoForge.EVENT_BUS.addListener(Wave29Events::onIncoming);
        NeoForge.EVENT_BUS.addListener(Wave29Events::onDamagePost);
        NeoForge.EVENT_BUS.addListener(Wave29Events::onChangeTarget);
        NeoForge.EVENT_BUS.addListener(Wave29Events::onUseItem);
        NeoForge.EVENT_BUS.addListener(Wave29Events::onUseBlock);
        NeoForge.EVENT_BUS.addListener(Wave29Events::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(Wave29Events::onDeath);
        NeoForge.EVENT_BUS.addListener(Wave29Events::onLogout);
        NeoForge.EVENT_BUS.addListener(Wave29Events::onServerTick);
        NeoForge.EVENT_BUS.addListener(Wave29Events::onStopping);
    }
}
