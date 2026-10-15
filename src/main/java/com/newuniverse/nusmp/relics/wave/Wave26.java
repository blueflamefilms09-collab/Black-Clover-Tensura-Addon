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
 * Relics of Many Worlds, part 4, wave 26: twelve relics for the head (Frank, the Goggles of Dwarvenkin, the Helm of Whispers, the Helmet of
 * Headbutting, the Jellyfish Cap, the Lens of Straight Creepin', the Masks of Difference, Indifference, Mind Melting, the Raven Queen's Disciple
 * and the Witchdoctor, and the Mind stone). The long card text of each, as game rules, is in the lang keys {@code item.nusmp.<id>.card}; the
 * numbers are in {@link Wave26Rules} (pure), the mind spells in {@link Wave26Mind}, Frank in {@link Wave26Frank}, the physical acts in
 * {@link Wave26Acts}, what is done while worn in {@link Wave26Wear}, the handlers in {@link Wave26Events}.
 * <p>
 * Tabletop became Minecraft: a long rest is a Minecraft day, an hour is twenty minutes, a saving throw is a die roll or an existence (Tensura EP)
 * comparison, "advantage" is a damage factor, a luck bonus or a lent Tensura resistance, and spells with no Minecraft form (Dominate Person,
 * Mass Suggestion, Polymorph) are charms and effects that never kill and never outlast their time.
 */
public final class Wave26 {
    private Wave26() {}

    private static final Passive ANY = Passive.flag(Wave26Rules.F_ANY);
    private static boolean hooked;

    public static void defs(Consumer<RelicDef> out) {
        out.accept(RelicDef.of("frank", "Frank", Grade.UNIQUE, RelicSlot.HEAD)
                .attune().look(Look.HOOD).palette(0xA8683A, 0xD9D9D2, 0xC8A0FF).armor(2, 0.5)
                .desc("A wooden bunny mask that is a trickster spirit. Cursed: set it down and it becomes a little brown bunny that follows you, and your blows weaken. Once a day it grants an hour of Foresight, if you do as Frank says.")
                .passive(ANY).passive(Passive.flag(Wave26Rules.F_FRANK))
                .act(Act.of("foresight", "Foresight", Wave26Frank::foresight).cooldown(100).usesPerDay(1)));

        out.accept(RelicDef.of("goggles_of_dwarvenkin", "Goggles of Dwarvenkin", Grade.SPECIAL, RelicSlot.HEAD)
                .look(Look.HAT).palette(0xA8623A, 0xFFB62E, 0xFFD060).armor(1, 0.0)
                .desc("Copper goggles with amber rune-gems. See in the dark, understand dwarven speech, mine a little faster, and once in a while grow a magnificent beard. Stone Cunning shows ore, hidden stone and traps.")
                .passive(ANY).passive(Passive.flag(Wave26Rules.F_GOGGLES)).passive(Passive.effect("minecraft:night_vision", 0))
                .passive(Passive.attr("minecraft:player.block_break_speed", 0.10, Operation.ADD_MULTIPLIED_BASE)).passive(Passive.skill("tensura:thought_communication"))
                .act(Act.of("stone_cunning", "Stone Cunning", Wave26Acts::stoneSense).cooldown(Wave26Rules.GOGGLES_COOLDOWN)));

        out.accept(RelicDef.of("helm_of_whispers", "Helm of Whispers", Grade.LEGEND, RelicSlot.HEAD)
                .attune().vessel().look(Look.PLATE).palette(0x7A4A2A, 0x3A2A1C, 0x4CFF7A).armor(3, 1.5)
                .desc("A spiked helm wreathed in green whispers. Read minds at will, dominate a creature three times a day, scream into every mind around you once a day. The voices never stop, and may drive you mad at dawn.")
                .passive(ANY).passive(Passive.flag(Wave26Rules.F_HELM)).passive(Passive.skill("tensura:telepathy")).passive(Passive.skill("tensura:abnormal_condition_resistance"))
                .act(Act.of("read_thoughts", "Detect Thoughts", Wave26Mind::readThoughts).cooldown(300))
                .act(Act.of("dominate_person", "Dominate Person", Wave26Mind::dominate).cooldown(60).usesPerDay(3))
                .act(Act.of("psychic_scream", "Psychic Scream", Wave26Mind::scream).cooldown(100).usesPerDay(1).epPercent(3.0)));

        out.accept(RelicDef.of("helmet_of_headbutting", "Helmet of Headbutting", Grade.RARE, RelicSlot.HEAD)
                .look(Look.PLATE).palette(0x8C9098, 0x8A5A32, 0x7CE0A0).armor(3, 0.5)
                .desc("A riveted horned helmet. Headbutt for 1d8 and a stun on a critical hit. You are stubborn (hard to push), you cannot concentrate, and your head splits now and then.")
                .passive(ANY).passive(Passive.flag(Wave26Rules.F_BUTT)).passive(Passive.attr("minecraft:generic.knockback_resistance", 0.25, Operation.ADD_VALUE))
                .passive(Passive.attr("tensura:chant_speed", -0.30, Operation.ADD_MULTIPLIED_BASE))
                .act(Act.of("headbutt", "Headbutt", Wave26Acts::headbutt).cooldown(Wave26Rules.HEADBUTT_COOLDOWN)));

        out.accept(RelicDef.of("jellyfish_cap", "Jellyfish Cap", Grade.UNIQUE, RelicSlot.HEAD)
                .attune().look(Look.HAT).palette(0xA8E8D4, 0xE0B33A, 0x7CFFE0).armor(1, 0.0)
                .desc("A translucent jellyfish cap. Poison cannot touch you, you breathe and swim like a fish, and guardians leave you be unless you strike first. A creature that strikes you from nearby is stung by poison cells.")
                .passive(ANY).passive(Passive.flag(Wave26Rules.F_JELLY)).passive(Passive.effect("minecraft:water_breathing", 0)).passive(Passive.effect("minecraft:dolphins_grace", 0))
                .passive(Passive.attr("neoforge:swim_speed", 0.5, Operation.ADD_MULTIPLIED_BASE)).passive(Passive.skill("tensura:poison_nullification"))
                .passive(Passive.skill("tensura:water_breathing")));

        out.accept(RelicDef.of("lens_of_straight_creepin", "Lens of Straight Creepin'", Grade.SPECIAL, RelicSlot.HEAD)
                .look(Look.CIRCLET).palette(0x2A2A30, 0xFF5A1C, 0xFF7A3C).armor(1, 0.0)
                .desc("A red-orange monocle on a bead chain. While you wear it, it remembers what moves past. Once a day it shows the tracks left nearby in the last ten minutes, and makes every creature close by glow.")
                .passive(ANY).passive(Passive.flag(Wave26Rules.F_LENS))
                .act(Act.of("see_tracks", "See Tracks", Wave26Acts::seeTracks).cooldown(100).usesPerDay(1)));

        out.accept(RelicDef.of("mask_of_difference", "Mask of Difference", Grade.RARE, RelicSlot.HEAD)
                .attune().look(Look.HOOD).palette(0x2F7BE0, 0x9CC8FF, 0x5CC8FF).armor(1, 0.0)
                .desc("A swirling blue mask with a mind of her own: chatty, restless and always up for adventure. She never lets you sleep, brings luck, wins friends at will, and can suggest and command once a day.")
                .passive(ANY).passive(Passive.flag(Wave26Rules.F_DIFF)).passive(Passive.attr("minecraft:generic.luck", 1.0, Operation.ADD_VALUE))
                .passive(Passive.skill("tensura:abnormal_condition_resistance"))
                .act(Act.of("friends", "Friends", Wave26Mind::friends).cooldown(400))
                .act(Act.of("suggest", "Suggestion", Wave26Mind::suggestion).cooldown(100).usesPerDay(1))
                .act(Act.of("command", "Command", Wave26Mind::command).cooldown(100).usesPerDay(1)));

        out.accept(RelicDef.of("mask_of_indifference", "Mask of Indifference", Grade.RARE, RelicSlot.HEAD)
                .attune().look(Look.HOOD).palette(0xB4B8C4, 0x30323A, 0xD0D4E0).armor(1, 0.0)
                .desc("A blank grey mask of a quiet mind. Fear and charm slip off you, nobody can read you, and what you say angers no one. Calm Emotions once a day. Your own luck suffers for your lack of charm.")
                .passive(ANY).passive(Passive.flag(Wave26Rules.F_INDIFF)).passive(Passive.effect("tensura:presence_concealment", 0))
                .passive(Passive.attr("minecraft:generic.luck", -1.0, Operation.ADD_VALUE)).passive(Passive.skill("tensura:abnormal_condition_resistance"))
                .act(Act.of("calm_emotions", "Calm Emotions", Wave26Mind::calm).cooldown(100).usesPerDay(1)));

        out.accept(RelicDef.of("mask_of_mind_melting", "Mask of Mind Melting", Grade.SPECIAL, RelicSlot.HEAD)
                .look(Look.HOOD).palette(0x3E5CB8, 0xB8325A, 0xFF4A6A).armor(1, 0.0)
                .desc("A squid-headed mask with three red eyes. Press it to a creature that is out cold: its mind is destroyed and you keep one memory as experience. The mask may take one of yours in exchange.")
                .passive(ANY)
                .act(Act.of("retrieve_memory", "Retrieve Memory", Wave26Mind::melt).cooldown(Wave26Rules.MELT_COOLDOWN)));

        out.accept(RelicDef.of("mask_of_the_raven_queens_disciple", "Mask of the Raven Queen's Disciple", Grade.RARE, RelicSlot.HEAD)
                .attune().look(Look.HOOD).palette(0x1E2848, 0xC8D0E0, 0xB46CFF).armor(2, 0.5)
                .desc("A navy and silver raven mask with a purple gem. It steadies the mind and sharpens the senses, and it strikes a little harder for her cause. Once a day it spares one who is dying.")
                .passive(ANY).passive(Passive.skill("tensura:spiritual_attack_resistance")).passive(Passive.skill("tensura:magic_sense"))
                .passive(Passive.attr("minecraft:generic.attack_damage", 1.0, Operation.ADD_VALUE))
                .act(Act.of("spare_the_dying", "Spare the Dying", Wave26Mind::spare).cooldown(20).usesPerDay(1)));

        out.accept(RelicDef.of("mask_of_the_witchdoctor", "Mask of the Witchdoctor", Grade.SPECIAL, RelicSlot.HEAD)
                .attune().look(Look.HOOD).palette(0x7A4A28, 0x66C23A, 0x7CFF5A).armor(1, 0.0)
                .desc("A tribal mask of stacked planks with a green gem. Three times a day the gem casts a random spell on a creature: Witch Bolt, Hex, a frog polymorph or Hypnotic Pattern. You resist poison and are immune to disease and hypnosis.")
                .passive(ANY).passive(Passive.flag(Wave26Rules.F_WITCH)).passive(Passive.skill("tensura:poison_resistance"))
                .act(Act.of("voodoo_remedy", "Voodoo Remedy", Wave26Mind::voodoo).cooldown(40).usesPerDay(3)));

        out.accept(RelicDef.of("mind_stone", "Mind stone", Grade.LEGEND, RelicSlot.HEAD)
                .attune().vessel().look(Look.PLATE).palette(0xE8ECEA, 0xC2D84A, 0xE6FF70).armor(3, 1.5)
                .desc("An infinity stone of Vision and Mind set in a silver helm crowned with pale leaves. Thought runs quick, magic and blasts glance off you, and once a day it speaks Mass Suggestion to everything near.")
                .passive(ANY).passive(Passive.flag(Wave26Rules.F_MIND)).passive(Passive.skill("tensura:thought_acceleration"))
                .passive(Passive.resist("minecraft:magic", 0.20)).passive(Passive.resist("minecraft:is_explosion", 0.20))
                .passive(Passive.attr("minecraft:generic.luck", 1.0, Operation.ADD_VALUE))
                .act(Act.of("mass_suggestion", "Mass Suggestion", Wave26Mind::massSuggestion).cooldown(100).usesPerDay(1).epPercent(4.0)));
    }

    /** Registers the game-bus listeners of this wave; called once from the mod constructor. */
    public static void hooks() {
        if (hooked) return;
        hooked = true;
        NeoForge.EVENT_BUS.addListener(Wave26Events::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(Wave26Events::onServerTick);
        NeoForge.EVENT_BUS.addListener(Wave26Events::onIncoming);
        NeoForge.EVENT_BUS.addListener(Wave26Events::onDamagePost);
        NeoForge.EVENT_BUS.addListener(Wave26Events::onChangeTarget);
        NeoForge.EVENT_BUS.addListener(Wave26Events::onAttack);
        NeoForge.EVENT_BUS.addListener(Wave26Events::onToss);
        NeoForge.EVENT_BUS.addListener(Wave26Events::onUseFinish);
        NeoForge.EVENT_BUS.addListener(Wave26Events::onLogout);
        NeoForge.EVENT_BUS.addListener(Wave26Events::onStopping);
    }
}
