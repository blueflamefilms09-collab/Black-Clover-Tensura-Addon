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
 * Relics of Many Worlds, second set, wave 15: one pair of boots, eight cloaks, capes and mantles, and eight bracers and bracelets (plus the Crest of
 * Foresight, which hangs at the neck). The long card text of each, as game rules, is in the lang keys {@code item.nusmp.<id>.card}; the numbers and
 * small state machines are in {@link Wave15Rules} (pure), the abilities in {@link Wave15Wear} and {@link Wave15Hands}, what stays in the world in
 * {@link Wave15Field}, the handlers in {@link Wave15Events}.
 * <p>
 * Tabletop became Minecraft: a long rest is a Minecraft day (uses per day come back at dawn), AC is armour, a saving throw is the target's existence
 * (Tensura EP) against the wearer's or an effect, stealth is Tensura's presence concealment, advantage is a Resistance or a Luck bonus, spells are the
 * nearest vanilla or Tensura effect.
 */
public final class Wave15 {
    private Wave15() {}

    private static final Passive ANY = Passive.flag(Wave15Rules.F_ANY);
    private static boolean hooked;

    public static void defs(Consumer<RelicDef> out) {
        // ------------------------------------------------------------ feet
        out.accept(RelicDef.of("boots_of_burglery", "Boots of Burglery", Grade.UNIQUE, RelicSlot.FEET)
                .look(Look.BOOTS).palette(0x24242C, 0x9AA0AE, 0xE8F4FF).armor(2, 1.0)
                .desc("A thief's black boots. You creep faster and Tensura's presence concealment stays on you while you sneak. Three times a day, Misty Step: blink up to nine blocks, or, sneaking, slip through a wall in front of you to the far side.")
                .passive(ANY).passive(Passive.flag(Wave15Rules.F_BOOTS))
                .passive(Passive.attr("minecraft:player.sneaking_speed", 0.25, Operation.ADD_VALUE))
                .passive(Passive.attr("minecraft:generic.safe_fall_distance", 2.0, Operation.ADD_VALUE))
                .act(Act.of("misty_step", "Misty Step", Wave15Wear::mistyStep).cooldown(40).usesPerDay(3)));

        // ------------------------------------------------------------ back
        out.accept(RelicDef.of("handy_towel_cape", "Handy Towel-Cape", Grade.RARE, RelicSlot.BACK)
                .attune().look(Look.CAPE).palette(0x8A2331, 0xE6C34A, 0x7CC96A)
                .desc("A warm, soft, very absorbent hooded cape: +1 armour and a quarter less frost damage. Wrap it round you for complete cold immunity (but slower, hands full); hold still under the night sky for a double rest; snap it dry in a moment.")
                .passive(ANY).passive(Passive.flag(Wave15Rules.F_TOWEL))
                .passive(Passive.attr("minecraft:generic.armor", 1.0, Operation.ADD_VALUE))
                .passive(Passive.attr("minecraft:generic.knockback_resistance", 0.1, Operation.ADD_VALUE))
                .passive(Passive.resist("minecraft:is_freezing", 0.25))
                .act(Act.of("wrap", "Bound Across Cold Moons", Wave15Wear::wrap).toggle().cooldown(20))
                .act(Act.of("star_rest", "Relaxing Beneath the Stars", Wave15Wear::starRest).hold(100).needsSneak().cooldown(1200))
                .act(Act.of("dry_off", "Great Practical Value", Wave15Wear::dryOff).cooldown(60)));

        out.accept(RelicDef.of("cape_of_bees", "Cape of Bees", Grade.RARE, RelicSlot.BACK)
                .look(Look.CAPE).palette(0x1E1C1A, 0xF2C230, 0xFFE066)
                .desc("Black and yellow stripes. Once a day flap it open and a swarm of bees follows the creature you look at for six seconds, stinging up to four things around it and poisoning them.")
                .passive(ANY).passive(Passive.flag(Wave15Rules.F_BEES))
                .act(Act.of("swarm", "Release the Swarm", Wave15Wear::swarm).cooldown(100).usesPerDay(1)));

        out.accept(RelicDef.of("cloak_of_the_first_snow", "Cloak of the First Snow", Grade.RARE, RelicSlot.BACK)
                .attune().look(Look.CLOAK).palette(0x1F3E66, 0xDCEBFF, 0x9FE0FF)
                .desc("Winter's embrace: +1 armour, sixty percent less frost damage and Tensura's cold resistance. Twice a day, Gone in a Flurry: for eighteen seconds snow swirls round you, you run faster and one melee blow each second is turned aside.")
                .passive(ANY).passive(Passive.flag(Wave15Rules.F_SNOW))
                .passive(Passive.attr("minecraft:generic.armor", 1.0, Operation.ADD_VALUE))
                .passive(Passive.resist("minecraft:is_freezing", 0.6))
                .passive(Passive.skill("tensura:cold_resistance"))
                .act(Act.of("flurry", "Gone in a Flurry", Wave15Wear::flurry).cooldown(100).usesPerDay(2)));

        out.accept(RelicDef.of("autumn_cloak", "Autumn Cloak", Grade.SPECIAL, RelicSlot.BACK)
                .look(Look.CLOAK).palette(0x8C2A1E, 0xE08A2A, 0xF2C050)
                .desc("A cloak of falling leaves: +1 armour among the trees (forests and taiga). Once a day, on command, the leaves swirl and blur you for a minute: a quarter of the blows against you miss.")
                .passive(ANY).passive(Passive.flag(Wave15Rules.F_AUTUMN))
                .act(Act.of("leaf_swirl", "Leaf Swirl", Wave15Wear::leafSwirl).cooldown(100).usesPerDay(1)));

        out.accept(RelicDef.of("darkened_cloak_of_elvenkind", "Darkened Cloak of Elvenkind", Grade.UNIQUE, RelicSlot.BACK)
                .attune().look(Look.CLOAK).palette(0x2A2D35, 0xB8C0CC, 0x6A7BA8)
                .desc("Hood up, and creatures more than eight blocks away cannot pick you out; you sneak faster. Five charges (two to five come back each dawn) buy Pass Without a Trace, Darkness and Greater Invisibility. Lower the hood and they end.")
                .passive(ANY).passive(Passive.flag(Wave15Rules.F_ELVEN))
                .passive(Passive.attr("minecraft:player.sneaking_speed", 0.2, Operation.ADD_VALUE))
                .act(Act.of("hood", "Hood Up or Down", Wave15Wear::hood).toggle().cooldown(10))
                .act(Act.of("pass_without_trace", "Pass Without a Trace", Wave15Wear::passWithoutTrace).cooldown(40))
                .act(Act.of("darkness", "Darkness", Wave15Wear::darkness).cooldown(100))
                .act(Act.of("greater_invisibility", "Greater Invisibility", Wave15Wear::greaterInvisibility).cooldown(100)));

        out.accept(RelicDef.of("cloak_of_levitation", "Cloak of Levitation", Grade.LEGEND, RelicSlot.BACK)
                .attune().vessel().look(Look.CLOAK).palette(0xB02A2A, 0xE6B94A, 0xFFD070)
                .desc("A sentient crimson cloak. +3 armour, no fall damage, and a toggle to hover and fly. Smother wraps a creature in reach, blind and held fast; struck below two fifths of your health, the cloak smothers your attacker by itself.")
                .passive(ANY).passive(Passive.flag(Wave15Rules.F_LEVITATE))
                .passive(Passive.attr("minecraft:generic.armor", 3.0, Operation.ADD_VALUE))
                .passive(Passive.attr("minecraft:generic.armor_toughness", 1.0, Operation.ADD_VALUE))
                .act(Act.of("levitate", "Levitate", Wave15Wear::levitate).toggle().cooldown(20))
                .act(Act.of("smother", "Smother", Wave15Wear::smother).cooldown(400).epPercent(2.0)));

        out.accept(RelicDef.of("cape_of_projection", "Cape of Projection", Grade.RARE, RelicSlot.BACK)
                .look(Look.CAPE).palette(0x1A1A20, 0xE8C050, 0xF5F0E0)
                .desc("Your hair is flawless and you are lucky with the charming. Whoever strikes you takes a quarter of the blow back (at most 6), and you loudly say it was their fault. Smoulder makes soft creatures near you lose interest for five seconds.")
                .passive(ANY).passive(Passive.flag(Wave15Rules.F_PROJECTION))
                .passive(Passive.attr("minecraft:generic.luck", 1.0, Operation.ADD_VALUE))
                .act(Act.of("smoulder", "Smoulder", Wave15Wear::smoulder).cooldown(900)));

        out.accept(RelicDef.of("mantle_of_selflessness", "Mantle of Selflessness", Grade.UNIQUE, RelicSlot.BACK)
                .attune().look(Look.MANTLE).palette(0xE6E8F0, 0xD8B450, 0xFFF2B0)
                .desc("A white and gold mantle that heals everyone but its wearer. Healing Wave mends every friend within eight blocks (7 to 10 hit points; one time in twenty, twice) for a little mana; badly hurt friends near you are eased a little unasked.")
                .passive(ANY).passive(Passive.flag(Wave15Rules.F_MANTLE))
                .act(Act.of("healing_wave", "Healing Wave", Wave15Wear::healingWave).cooldown(300).mana(4)));

        // ------------------------------------------------------------ hands
        out.accept(RelicDef.of("bracers_of_phasing", "Bracers of Phasing", Grade.UNIQUE, RelicSlot.HANDS)
                .attune().look(Look.GAUNTLETS).palette(0x8A6A32, 0xD8B454, 0xFFD860)
                .desc("Four times a day, phase out: for thirty seconds you walk through walls (you become a ghost, unable to touch or strike anything). The bracers lift you clear of any stone you are still inside. The Dungeon's stone cannot be parted.")
                .passive(ANY).passive(Passive.flag(Wave15Rules.F_PHASING))
                .act(Act.of("phase", "Phase", Wave15Hands::phase).cooldown(200).usesPerDay(4)));

        out.accept(RelicDef.of("bracer_of_initiative", "Bracer of Initiative", Grade.RARE, RelicSlot.HANDS)
                .look(Look.GAUNTLETS).palette(0x3A2C22, 0xB8903C, 0x62C8E8)
                .desc("Dice dangle from the straps. You swing a little faster, you are lucky, and your first blow on a creature that is not yet fighting you lands 3 harder. Roll Initiative: eight seconds of Speed II and Haste II.")
                .passive(ANY).passive(Passive.flag(Wave15Rules.F_INITIATIVE))
                .passive(Passive.attr("minecraft:generic.attack_speed", 0.1, Operation.ADD_MULTIPLIED_BASE))
                .passive(Passive.attr("minecraft:generic.luck", 2.0, Operation.ADD_VALUE))
                .act(Act.of("roll_initiative", "Roll Initiative", Wave15Hands::rollInitiative).cooldown(600)));

        out.accept(RelicDef.of("phantoms_bracelet", "Phantom's Bracelet", Grade.RARE, RelicSlot.HANDS)
                .look(Look.CHARM).palette(0xC8CCD8, 0x6A6E80, 0xC8A8F0)
                .desc("Once a day your hand goes ghost and reaches through a wall to the first chest, lever or button within six blocks. Flip a coin: heads, you work it; tails, your fingers pass over it.")
                .passive(ANY).passive(Passive.flag(Wave15Rules.F_PHANTOM))
                .act(Act.of("ghost_hand", "Ghost Hand", Wave15Hands::ghostHand).cooldown(20).usesPerDay(1)));

        out.accept(RelicDef.of("bracers_of_manhandling", "Bracers of Manhandling", Grade.RARE, RelicSlot.HANDS)
                .look(Look.GAUNTLETS).palette(0x6A7890, 0xC8D4E4, 0xFF6AD0)
                .desc("YEET. Seize a creature in reach and fling it along your look: small ones twice as far, large ones half, giants not at all; it takes a hard landing. Your blows knock back a little harder.")
                .passive(ANY).passive(Passive.flag(Wave15Rules.F_MANHANDLING))
                .passive(Passive.attr("minecraft:generic.attack_knockback", 0.5, Operation.ADD_VALUE))
                .act(Act.of("seize_and_throw", "Seize and Throw", Wave15Hands::seizeAndThrow).cooldown(200)));

        out.accept(RelicDef.of("bracers_of_arcane_blades", "Bracers of Arcane Blades", Grade.RARE, RelicSlot.HANDS)
                .look(Look.GAUNTLETS).palette(0x7C4A30, 0xD8B050, 0x9A7CFF)
                .desc("Reach for a blade and a dagger of arcane energy is there. Throw two at once (5 force damage that ignores armour, or 3.5 each on two foes); or turn the daggers on to strike bare-handed for 3 more.")
                .passive(ANY).passive(Passive.flag(Wave15Rules.F_BLADES))
                .act(Act.of("arcane_daggers", "Throw Arcane Daggers", Wave15Hands::throwDaggers).cooldown(14))
                .act(Act.of("blade_mode", "Arcane Blades", Wave15Hands::bladeMode).toggle().cooldown(10)));

        out.accept(RelicDef.of("bear_bracer", "Bear Bracer", Grade.RARE, RelicSlot.HANDS)
                .attune().look(Look.GAUNTLETS).palette(0x808890, 0xC27838, 0x58A8FF)
                .desc("Say the command word and your hand is a bear's paw: bare-handed blows cut for 3 more and you stand firmer against knockback. Say it again to put the hand back.")
                .passive(ANY).passive(Passive.flag(Wave15Rules.F_BEAR))
                .passive(Passive.attr("minecraft:generic.knockback_resistance", 0.2, Operation.ADD_VALUE))
                .act(Act.of("bear_paw", "Bear Paw", Wave15Hands::bearPaw).toggle().cooldown(20)));

        out.accept(RelicDef.of("bracelets_of_faithful_friendship", "Bracelets of Faithful Friendship", Grade.RARE, RelicSlot.HANDS)
                .attune().look(Look.CHARM).palette(0x9AA4B0, 0x2FB5B0, 0x9FF0E8)
                .desc("Bind a friend (a player, or a pet of yours) as the other wearer. While you are within ten blocks you both carry Resistance; once a day each pulls the other back from a killing blow; waking close together mends you both.")
                .passive(ANY).passive(Passive.flag(Wave15Rules.F_FRIENDS))
                .act(Act.of("bind_friend", "Bind a Friend", Wave15Hands::bind).cooldown(40))
                .act(Act.of("release_bond", "Release the Bond", Wave15Hands::unbind).cooldown(20)));

        // ------------------------------------------------------------ neck
        out.accept(RelicDef.of("crest_of_foresight", "Crest of Foresight", Grade.LEGEND, RelicSlot.NECK)
                .vessel().look(Look.AMULET).palette(0xE0B83A, 0x7A5A18, 0xFFF0A0)
                .desc("A golden crest with a clock of too many hands. Tensura's danger sense is yours; explosions, shots and falls hurt less. Once a day, the moment of foresight: +2 armour for ten minutes and twenty seconds of future vision.")
                .passive(ANY).passive(Passive.flag(Wave15Rules.F_CREST))
                .passive(Passive.skill("tensura:danger_sense"))
                .passive(Passive.resist("minecraft:is_explosion", 0.2))
                .passive(Passive.resist("minecraft:is_projectile", 0.15))
                .passive(Passive.resist("minecraft:is_fall", 0.3))
                .act(Act.of("foresight", "Moment of Foresight", Wave15Hands::foresight).cooldown(200).usesPerDay(1).epPercent(1.0)));
    }

    /** Registers the game-bus listeners of this wave; called once from the mod constructor. */
    public static void hooks() {
        if (hooked) return;
        hooked = true;
        NeoForge.EVENT_BUS.addListener(Wave15Events::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(Wave15Events::onServerTick);
        NeoForge.EVENT_BUS.addListener(Wave15Events::onIncoming);
        NeoForge.EVENT_BUS.addListener(Wave15Events::onDamagePre);
        NeoForge.EVENT_BUS.addListener(Wave15Events::onDamagePost);
        NeoForge.EVENT_BUS.addListener(Wave15Events::onFall);
        NeoForge.EVENT_BUS.addListener(Wave15Events::onChangeTarget);
        NeoForge.EVENT_BUS.addListener(Wave15Events::onWake);
        NeoForge.EVENT_BUS.addListener(Wave15Events::onLogin);
        NeoForge.EVENT_BUS.addListener(Wave15Events::onLogout);
        NeoForge.EVENT_BUS.addListener(Wave15Events::onRespawn);
        NeoForge.EVENT_BUS.addListener(Wave15Events::onStopping);
    }
}
