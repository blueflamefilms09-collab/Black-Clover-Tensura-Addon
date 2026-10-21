package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Act;
import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.GradeDamage;
import com.newuniverse.nusmp.relics.Look;
import com.newuniverse.nusmp.relics.Passive;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicSlot;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

import java.util.function.Consumer;

/**
 * Relics of Many Worlds, wave 101 (release 1.111.0): fifteen new Genesis grade relics, the rarest and strongest grade. Seven are the Millennium Items of Yu-Gi-Oh (the Puzzle, the Eye, the
 * Ring, the Rod, the Key, the Scale and the Necklace), each with its own slot and its own mechanic from the series (a second self and a shadow game; reading a mind; dowsing and drawing power;
 * controlling a mind; opening the soul room; weighing a heart; foresight) and a set bonus when several are worn together ({@link Wave101Set}, {@link Wave101Rules}). Eight are world-level
 * artifacts of eight other franchises, none of them in the mod before: Ea of Fate, the Requiem Arrow of JoJo, the Truth-Seeking Orbs of Naruto, the Hogyoku of Bleach, the Dragon Balls of
 * Dragon Ball, the Straw Hat of One Piece, the Law of Cycles gown of Madoka Magica and the Greed Island binder of Hunter x Hunter. Three are weapons in the main hand (the Rod, Ea and the Arrow:
 * 3D models, Tensura engravings, damage from {@link GradeDamage}), the others are charms, a belt, a back piece, a neck piece and armour. The Millennium Items need no attunement, so all seven can
 * be worn at once; each is one of a kind in the world like every relic. Passives that need a game event are in {@link Wave101Events}, the abilities in {@link Wave101Acts} and the shapes of {@link Wave71Acts}.
 */
public final class Wave101 {
    private Wave101() {}

    static final double DMG_MILLENNIUM_ROD = GradeDamage.damage("genesis", "staff");
    static final double DMG_EA_SWORD_OF_RUPTURE = GradeDamage.damage("genesis", "longsword");
    static final double DMG_REQUIEM_STAND_ARROW = GradeDamage.damage("genesis", "dagger");

    /** The ids of this wave, in order (the self-test and the tests read them): the seven Millennium Items first, then the eight others. */
    public static final String[] IDS = {
            "millennium_puzzle", "millennium_eye", "millennium_ring", "millennium_rod", "millennium_key", "millennium_scale", "millennium_necklace",
            "ea_sword_of_rupture", "requiem_stand_arrow", "truth_seeking_orbs_of_the_sage", "hogyoku_of_the_crystal_dissolution",
            "seven_dragon_balls_of_shenron", "straw_hat_of_joy_boy", "law_of_cycles_gown", "greed_island_original_binder",
    };

    public static void defs(Consumer<RelicDef> out) {
        // ================================================================ the seven Millennium Items (Yu-Gi-Oh!), no attunement so that all seven can be worn together
        // ---------------------------------------------------------------- Millennium Puzzle (Genesis, neck pendant)
        out.accept(RelicDef.of("millennium_puzzle", "Millennium Puzzle", Grade.GENESIS, RelicSlot.NECK)
                .palette(0x2A4FC0, 0xF0C050, 0xFFE890).look(Look.PENDANT)
                .desc("A golden pyramid on a cord, with the Eye of Wdjat on its face, in which a second soul sleeps. When a blow would kill you, the other soul takes it and you wake at 40% health (once in five minutes). Use: Other Me and Shadow Game. Worn with the other Millennium Items it grows stronger.")
                .passive(Passive.flag("w101_puzzle"))
                .passive(Passive.attr("minecraft:generic.max_health", 12.0, Operation.ADD_VALUE))
                .passive(Passive.attr("tensura:max_magicule", 0.25, Operation.ADD_MULTIPLIED_TOTAL))
                .passive(Passive.attr("minecraft:generic.luck", 3.0, Operation.ADD_VALUE))
                .passive(Passive.resist("minecraft:magic", 0.4))
                .passive(Passive.resist("tensura:curse", 0.5))
                .passive(Passive.skill("tensura:thought_acceleration"))
                .act(Act.of("other_me", "Other Me", Wave71Acts.bolster("minecraft:strength", 2, "minecraft:resistance", 1, 20, 80f, 0f)).cooldown(1800).mana(10))
                .act(Act.of("shadow_game", "Shadow Game", Wave101Acts.shadowGame(14.0, 110f, 8)).cooldown(4800).mana(14).needsSneak().usesPerDay(3)));

        // ---------------------------------------------------------------- Millennium Eye (Genesis, head: the brow piece)
        out.accept(RelicDef.of("millennium_eye", "Millennium Eye", Grade.GENESIS, RelicSlot.HEAD)
                .palette(0x1A3A9A, 0xE8B040, 0x58F0E8).look(Look.CIRCLET)
                .desc("A golden eye set in the brow of a lapis circlet. It sees what is true and what is meant, and nothing near can hide its thoughts. Use: Mind Read and Truth Gaze. Worn with the other Millennium Items it grows stronger.")
                .passive(Passive.attr("minecraft:generic.luck", 4.0, Operation.ADD_VALUE))
                .passive(Passive.attr("tensura:max_magicule", 0.2, Operation.ADD_MULTIPLIED_TOTAL))
                .passive(Passive.resist("minecraft:magic", 0.3))
                .passive(Passive.skill("tensura:eye_of_truth"))
                .passive(Passive.skill("tensura:magic_sense"))
                .passive(Passive.skill("tensura:farsight"))
                .act(Act.of("mind_read", "Mind Read", Wave101Acts.readMinds(24.0, 12)).cooldown(900).mana(8))
                .act(Act.of("truth_gaze", "Truth Gaze", Wave101Acts.eyeBeam(32.0, 1.6, 130f, 8)).cooldown(3000).mana(12).needsSneak()));

        // ---------------------------------------------------------------- Millennium Ring (Genesis, hands: a ring on the finger)
        out.accept(RelicDef.of("millennium_ring", "Millennium Ring", Grade.GENESIS, RelicSlot.HANDS)
                .palette(0x233E8C, 0xF2C860, 0xFFD870).look(Look.CHARM)
                .desc("A golden ring hung with five needles that swing to whatever means you harm and to whatever holds power. Monsters that hunt you glow through the walls. Use: Dowsing Needles and Draw Power. Worn with the other Millennium Items it grows stronger.")
                .passive(Passive.flag("w101_ring"))
                .passive(Passive.attr("tensura:max_magicule", 0.3, Operation.ADD_MULTIPLIED_TOTAL))
                .passive(Passive.attr("minecraft:player.entity_interaction_range", 1.0, Operation.ADD_VALUE))
                .passive(Passive.resist("minecraft:magic", 0.3))
                .passive(Passive.skill("tensura:magic_sense"))
                .act(Act.of("dowsing_needles", "Dowsing Needles", Wave101Acts.dowse(48.0, 10)).cooldown(600).mana(6))
                .act(Act.of("draw_power", "Draw Power", Wave101Acts.drawPower(12.0, 80f)).cooldown(2400).mana(6)));

        // ---------------------------------------------------------------- Millennium Rod (Genesis, a held rod: weapon)
        out.accept(RelicDef.of("millennium_rod", "Millennium Rod", Grade.GENESIS, RelicSlot.MAIN_HAND).vessel().model3d()
                .look(Look.STAFF).palette(0x30389C, 0xEEB844, 0x9AE8FF)
                .desc("A golden rod with a winged eye on its head, that bends the mind of whoever it is pointed at. 70% more damage against a foe under 30% of its health, which it also weakens. Use: Mind Control, Pharaoh's Command and Judgement Strike. Worn with the other Millennium Items it grows stronger.")
                .passive(Passive.flag("w101_millennium_rod"))
                .passive(Passive.attr("minecraft:player.entity_interaction_range", 1.0, Operation.ADD_VALUE))
                .passive(Passive.attr("tensura:max_magicule", 0.25, Operation.ADD_MULTIPLIED_TOTAL))
                .passive(Passive.resist("minecraft:magic", 0.4))
                .passive(Passive.skill("tensura:thought_acceleration"))
                .act(Act.of("mind_control", "Mind Control", Wave101Acts.mindControl(24.0, 240)).cooldown(1200).mana(10))
                .act(Act.of("pharaohs_command", "Pharaoh's Command", Wave101Acts.command(16.0, 3, 160)).cooldown(2400).mana(12).needsSneak())
                .act(Act.of("judgement_strike", "Judgement Strike", Wave71Acts.line(28.0, 1.8, (float) (DMG_MILLENNIUM_ROD * 1.2), "minecraft:magic", "minecraft:slowness", 6, 3, VfxShape.MIRROR_RAY)).cooldown(6000).mana(14).usesPerDay(2))
                .melee(DMG_MILLENNIUM_ROD, 1.3)
                .engrave("magic_weapon@4", "magic_interference", "barrier_piercing", "enervation", "soul_eater"));

        // ---------------------------------------------------------------- Millennium Key (Genesis, belt: the ankh key)
        out.accept(RelicDef.of("millennium_key", "Millennium Key", Grade.GENESIS, RelicSlot.BELT)
                .palette(0x2C46A8, 0xE4AE3C, 0xFFD060).look(Look.BELT)
                .desc("A golden ankh on a lapis belt: the key to the room of the soul. No fear, confusion or sleep can reach the one who carries it. Use: Open the Soul Room and Mind Garden. Worn with the other Millennium Items it grows stronger.")
                .passive(Passive.flag("w101_key"))
                .passive(Passive.attr("minecraft:generic.max_health", 10.0, Operation.ADD_VALUE))
                .passive(Passive.attr("minecraft:generic.luck", 2.0, Operation.ADD_VALUE))
                .passive(Passive.resist("tensura:curse", 0.5))
                .passive(Passive.skill("tensura:abnormal_condition_resistance"))
                .act(Act.of("open_the_soul_room", "Open the Soul Room", Wave101Acts.soulRoom(12.0, 95f)).cooldown(1800).mana(8))
                .act(Act.of("mind_garden", "Mind Garden", Wave101Acts.mindGarden(14.0, 12, 60f)).cooldown(6000).mana(12).usesPerDay(2)));

        // ---------------------------------------------------------------- Millennium Scale (Genesis, a charm held in the hand)
        out.accept(RelicDef.of("millennium_scale", "Millennium Scale", Grade.GENESIS, RelicSlot.CHARM)
                .palette(0x1F4AB4, 0xF4C85A, 0x78F0D8).look(Look.CHARM)
                .desc("A small golden balance with a feather on one pan. It weighs the heart of a monster and judges it: a heavy heart is struck down, a light one is spared. Use: Weigh the Heart and Ammit's Verdict. Worn with the other Millennium Items it grows stronger.")
                .passive(Passive.attr("minecraft:generic.luck", 4.0, Operation.ADD_VALUE))
                .passive(Passive.attr("minecraft:generic.max_health", 10.0, Operation.ADD_VALUE))
                .passive(Passive.resist("minecraft:magic", 0.4))
                .passive(Passive.resist("tensura:curse", 0.4))
                .passive(Passive.skill("tensura:analytical_appraisal"))
                .act(Act.of("weigh_the_heart", "Weigh the Heart", Wave101Acts.weighHeart(24.0, 150f)).cooldown(1500).mana(10))
                .act(Act.of("ammits_verdict", "Ammit's Verdict", Wave101Acts.verdict(14.0, 120f)).cooldown(6000).mana(14).needsSneak().usesPerDay(2)));

        // ---------------------------------------------------------------- Millennium Necklace (Genesis, the broad collar over the chest)
        out.accept(RelicDef.of("millennium_necklace", "Millennium Necklace", Grade.GENESIS, RelicSlot.CHEST)
                .palette(0x1C3C90, 0xEBBB52, 0x62E6F0).look(Look.PLATE)
                .desc("A broad collar of gold and lapis that shows what is to come. Once every eight seconds the first heavy blow is seen coming and halved. Use: Foresight and Prophecy. Worn with the other Millennium Items it grows stronger.")
                .passive(Passive.flag("w101_necklace"))
                .passive(Passive.attr("minecraft:generic.max_health", 14.0, Operation.ADD_VALUE))
                .passive(Passive.attr("minecraft:generic.movement_speed", 0.05, Operation.ADD_MULTIPLIED_BASE))
                .passive(Passive.resist("minecraft:magic", 0.35))
                .passive(Passive.resist("minecraft:is_projectile", 0.3))
                .passive(Passive.skill("tensura:danger_sense"))
                .act(Act.of("foresight", "Foresight", Wave101Acts.foresee(24.0, 12)).cooldown(1500).mana(8))
                .act(Act.of("prophecy", "Prophecy", Wave101Acts.prophecy()).cooldown(6000).mana(14).needsSneak().usesPerDay(2)));

        // ================================================================ eight world-level artifacts of other worlds
        // ---------------------------------------------------------------- Ea, Sword of Rupture (Genesis, Fate)
        out.accept(RelicDef.of("ea_sword_of_rupture", "Ea, Sword of Rupture", Grade.GENESIS, RelicSlot.MAIN_HAND).attune().vessel().model3d()
                .look(Look.SWORD).palette(0x3A2A24, 0xE8B84A, 0xFF4A2A)
                .desc("The sword of rupture, three cylinders of black and gold that turn against each other: it was forged before the earth and heaven were parted, and it is the one that parted them. 60% more damage against a foe with 100 health or more. Use: Rupture Slash, Space-Cut Wind and Enuma Elish.")
                .passive(Passive.flag("w101_ea_sword_of_rupture"))
                .passive(Passive.attr("minecraft:generic.attack_damage", 4.0, Operation.ADD_VALUE))
                .passive(Passive.attr("minecraft:generic.max_health", 12.0, Operation.ADD_VALUE))
                .passive(Passive.resist("minecraft:magic", 0.3))
                .passive(Passive.skill("tensura:spatial_attack_resistance"))
                .act(Act.of("rupture_slash", "Rupture Slash", Wave71Acts.cone(9.0, 160, (float) (DMG_EA_SWORD_OF_RUPTURE * 0.9), "minecraft:player_attack", "minecraft:weakness", 8, 1, VfxShape.SLASH_BLADES)).cooldown(440).mana(8))
                .act(Act.of("space_cut_wind", "Space-Cut Wind", Wave71Acts.line(36.0, 2.6, (float) (DMG_EA_SWORD_OF_RUPTURE * 1.3), "minecraft:magic", "minecraft:slowness", 8, 3, VfxShape.SPACE_PORTAL)).cooldown(1200).mana(12).needsSneak())
                .act(Act.of("enuma_elish", "Enuma Elish", Wave71Acts.nova(16.0, (float) (DMG_EA_SWORD_OF_RUPTURE * 1.4), "minecraft:player_attack", "minecraft:weakness", 10, 2, 2.4, VfxShape.EARTH_FISSURE)).cooldown(6000).mana(14).usesPerDay(2))
                .melee(DMG_EA_SWORD_OF_RUPTURE, 1.5)
                .engrave("severance@4", "barrier_piercing", "transcendence", "ruination", "magic_interference"));

        // ---------------------------------------------------------------- Requiem Stand Arrow (Genesis, JoJo's Bizarre Adventure)
        out.accept(RelicDef.of("requiem_stand_arrow", "Requiem Stand Arrow", Grade.GENESIS, RelicSlot.MAIN_HAND).attune().vessel().model3d()
                .look(Look.SWORD).palette(0xE6C04A, 0x3AA860, 0xFFF0A0)
                .desc("A golden arrow with a beetle's head, the one that wakes what sleeps in a soul. What it pierces is returned to zero. 80% more damage while you are under 40% of your health; 5% of the damage comes back to you as life. Use: Piercing Requiem, Return to Zero and Stand Awakened.")
                .passive(Passive.flag("w101_requiem_stand_arrow"))
                .passive(Passive.attr("minecraft:generic.attack_speed", 0.4, Operation.ADD_VALUE))
                .passive(Passive.attr("minecraft:generic.max_health", 10.0, Operation.ADD_VALUE))
                .passive(Passive.resist("minecraft:magic", 0.3))
                .passive(Passive.skill("tensura:danger_sense"))
                .act(Act.of("piercing_requiem", "Piercing Requiem", Wave71Acts.line(20.0, 1.0, (float) (DMG_REQUIEM_STAND_ARROW * 1.1), "minecraft:player_attack", "minecraft:glowing", 8, 0, VfxShape.LIGHT_BLADE)).cooldown(400).mana(8))
                .act(Act.of("return_to_zero", "Return to Zero", Wave101Acts.both(Wave71Acts.ward(100f, 8, "minecraft:resistance", 6, 1, 6.0, 1.4), ctx -> Wave31Acts.cleanse(ctx))).cooldown(1800).mana(10).needsSneak())
                .act(Act.of("stand_awakened", "Stand Awakened", Wave71Acts.bolster("minecraft:strength", 3, "minecraft:speed", 2, 20, 120f, (float) (DMG_REQUIEM_STAND_ARROW * 0.4))).cooldown(6000).mana(14).usesPerDay(2))
                .melee(DMG_REQUIEM_STAND_ARROW, 2.2)
                .engrave("severance@3", "soul_eater", "energy_steal", "enervation", "transcendence"));

        // ---------------------------------------------------------------- Truth-Seeking Orbs of the Sage (Genesis, Naruto, back)
        out.accept(RelicDef.of("truth_seeking_orbs_of_the_sage", "Truth-Seeking Orbs of the Sage", Grade.GENESIS, RelicSlot.BACK).attune()
                .palette(0x14141C, 0xC8C8D8, 0xE0E0FF).look(Look.MANTLE)
                .desc("Nine black orbs that float behind the shoulders of whoever carries the sage's power. They turn aside what is thrown, push the world away and pull it in. Use: Truth-Seeking Barrage, Shinra Tensei and Chibaku Tensei.")
                .passive(Passive.attr("tensura:max_magicule", 0.3, Operation.ADD_MULTIPLIED_TOTAL))
                .passive(Passive.attr("minecraft:generic.max_health", 10.0, Operation.ADD_VALUE))
                .passive(Passive.resist("minecraft:magic", 0.4))
                .passive(Passive.resist("minecraft:is_projectile", 0.5))
                .passive(Passive.skill("tensura:magic_sense"))
                .passive(Passive.skill("tensura:thought_acceleration"))
                .act(Act.of("truth_seeking_barrage", "Truth-Seeking Barrage", Wave71Acts.volley(8, 30.0, 110f, "minecraft:magic", "minecraft:slowness", 4, 1, VfxShape.THREAD_LINE)).cooldown(500).mana(8))
                .act(Act.of("shinra_tensei", "Shinra Tensei", Wave71Acts.nova(14.0, 120f, "minecraft:magic", "minecraft:slowness", 6, 2, 2.6, VfxShape.WIND_RING)).cooldown(1400).mana(10).needsSneak())
                .act(Act.of("chibaku_tensei", "Chibaku Tensei", Wave71Acts.pullBind(16.0, 1.8, 140f, "minecraft:magic", "minecraft:slowness", 10, 4, VfxShape.ASH_FORMATION)).cooldown(6000).mana(14).usesPerDay(2)));

        // ---------------------------------------------------------------- Hogyoku, the Crystal of Dissolution (Genesis, Bleach, charm)
        out.accept(RelicDef.of("hogyoku_of_the_crystal_dissolution", "Hogyoku, the Crystal of Dissolution", Grade.GENESIS, RelicSlot.CHARM).attune()
                .palette(0x8A5AC8, 0xE8D8FF, 0xD8A0FF).look(Look.ORB)
                .desc("A crystal that dissolves the line between a soul and what it is meant to be, and gives to the one who holds it what it was about to become. Use: Fusion, Boundary Dissolution and Transcendence.")
                .passive(Passive.attr("tensura:max_magicule", 0.35, Operation.ADD_MULTIPLIED_TOTAL))
                .passive(Passive.attr("minecraft:generic.max_health", 12.0, Operation.ADD_VALUE))
                .passive(Passive.resist("minecraft:magic", 0.45))
                .passive(Passive.resist("tensura:curse", 0.3))
                .passive(Passive.skill("tensura:magic_resistance"))
                .passive(Passive.skill("tensura:self_regeneration"))
                .act(Act.of("fusion", "Fusion", Wave71Acts.bolster("minecraft:strength", 2, "minecraft:regeneration", 2, 20, 80f, 40f)).cooldown(1800).mana(10))
                .act(Act.of("boundary_dissolution", "Boundary Dissolution", Wave71Acts.nova(12.0, 130f, "minecraft:magic", "minecraft:weakness", 8, 2, 1.0, VfxShape.MAGIC_CIRCLE_EXPLOSION)).cooldown(3600).mana(12).needsSneak())
                .act(Act.of("transcendence", "Transcendence", Wave71Acts.bolster("minecraft:strength", 3, "minecraft:resistance", 2, 25, 160f, 100f)).cooldown(12000).mana(14).usesPerDay(1)));

        // ---------------------------------------------------------------- Seven Dragon Balls of Shenron (Genesis, Dragon Ball, belt)
        out.accept(RelicDef.of("seven_dragon_balls_of_shenron", "Seven Dragon Balls of Shenron", Grade.GENESIS, RelicSlot.BELT).attune()
                .palette(0xF08A20, 0xC83A2A, 0xFFC040).look(Look.BELT)
                .desc("A belt-pouch with the seven orange spheres, one star to seven. Together they call the eternal dragon, who makes one wish: it makes the wearer whole, once a day. Use: Dragon Radar, Eternal Dragon's Roar and Shenron's Wish.")
                .passive(Passive.attr("minecraft:generic.max_health", 16.0, Operation.ADD_VALUE))
                .passive(Passive.attr("minecraft:generic.luck", 3.0, Operation.ADD_VALUE))
                .passive(Passive.resist("tensura:curse", 0.3))
                .passive(Passive.skill("tensura:self_regeneration"))
                .passive(Passive.skill("tensura:strengthen_body"))
                .act(Act.of("dragon_radar", "Dragon Radar", Wave101Acts.dowse(48.0, 12)).cooldown(600).mana(4))
                .act(Act.of("eternal_dragons_roar", "Eternal Dragon's Roar", Wave71Acts.line(32.0, 2.4, 150f, "minecraft:magic", "minecraft:slowness", 6, 2, VfxShape.FIRE_SPEAR)).cooldown(1500).mana(10).needsSneak())
                .act(Act.of("shenrons_wish", "Shenron's Wish", Wave101Acts.wish()).cooldown(24000).mana(14).usesPerDay(1)));

        // ---------------------------------------------------------------- Straw Hat of Joy Boy (Genesis, One Piece, head)
        out.accept(RelicDef.of("straw_hat_of_joy_boy", "Straw Hat of Joy Boy", Grade.GENESIS, RelicSlot.HEAD).attune()
                .palette(0xE8C870, 0xC8302A, 0xFFF0B0).look(Look.HAT)
                .desc("A worn straw hat with a red band, the promise of a man who laughed at the end of the world. The wearer is hard to hurt, hard to hold down and glad of it: lightning, falls and explosions mean little. Use: Gear Fifth, Rubber Bajrang and Drums of Liberation.")
                .passive(Passive.attr("minecraft:generic.max_health", 10.0, Operation.ADD_VALUE))
                .passive(Passive.attr("minecraft:generic.knockback_resistance", 0.5, Operation.ADD_VALUE))
                .passive(Passive.attr("minecraft:generic.movement_speed", 0.08, Operation.ADD_MULTIPLIED_BASE))
                .passive(Passive.resist("minecraft:lightning_bolt", 0.8))
                .passive(Passive.resist("minecraft:is_fall", 0.9))
                .passive(Passive.resist("minecraft:is_explosion", 0.3))
                .act(Act.of("gear_fifth", "Gear Fifth", Wave71Acts.bolster("minecraft:speed", 3, "minecraft:strength", 2, 24, 100f, 0f)).cooldown(2400).mana(12))
                .act(Act.of("rubber_bajrang", "Rubber Bajrang", Wave71Acts.rush(14.0, 1.6, 150f, "minecraft:player_attack", "minecraft:slowness", 4, 2, VfxShape.FIRE_BURST)).cooldown(800).mana(8).needsSneak())
                .act(Act.of("drums_of_liberation", "Drums of Liberation", Wave71Acts.rally(16.0, "minecraft:strength", "minecraft:speed", 30, 1, 60f)).cooldown(6000).mana(14).usesPerDay(2)));

        // ---------------------------------------------------------------- Law of Cycles Gown (Genesis, Puella Magi Madoka Magica, chest)
        out.accept(RelicDef.of("law_of_cycles_gown", "Law of Cycles Gown", Grade.GENESIS, RelicSlot.CHEST).attune()
                .palette(0xF8E8F0, 0xF080B8, 0xFFC0E0).look(Look.ROBE)
                .desc("The long white and pink gown of a girl who became a law, so that no one has to despair. It turns curses aside and mends those who stand near. Use: Arrow of Hope, Purify Despair and Law of Cycles.")
                .passive(Passive.attr("minecraft:generic.max_health", 14.0, Operation.ADD_VALUE))
                .passive(Passive.attr("tensura:max_magicule", 0.3, Operation.ADD_MULTIPLIED_TOTAL))
                .passive(Passive.resist("minecraft:magic", 0.5))
                .passive(Passive.resist("tensura:curse", 0.6))
                .passive(Passive.skill("tensura:holy_attack_resistance"))
                .passive(Passive.skill("tensura:magic_resistance"))
                .act(Act.of("arrow_of_hope", "Arrow of Hope", Wave71Acts.line(40.0, 1.2, 160f, "minecraft:magic", "minecraft:glowing", 8, 0, VfxShape.LIGHT_BLADE)).cooldown(600).mana(8))
                .act(Act.of("purify_despair", "Purify Despair", Wave101Acts.mindGarden(16.0, 12, 80f)).cooldown(3600).mana(12).needsSneak())
                .act(Act.of("law_of_cycles", "Law of Cycles", Wave71Acts.nova(20.0, 180f, "minecraft:magic", "minecraft:weakness", 12, 2, 1.2, VfxShape.LIGHT_FLARE)).cooldown(24000).mana(14).usesPerDay(1)));

        // ---------------------------------------------------------------- Greed Island Original Binder (Genesis, Hunter x Hunter, charm)
        out.accept(RelicDef.of("greed_island_original_binder", "Greed Island Original Binder", Grade.GENESIS, RelicSlot.CHARM).attune()
                .palette(0x2E7A4A, 0xE0C060, 0x90FFB0).look(Look.TOME)
                .desc("The binder of the game that was built to be won: a green book with forty slots for cards, and a pocket of spells that cannot be bought. Use: Spell Card Leave, Spell Card Reflection and Spell Card Magnetic Force.")
                .passive(Passive.attr("minecraft:generic.luck", 5.0, Operation.ADD_VALUE))
                .passive(Passive.attr("minecraft:generic.max_health", 8.0, Operation.ADD_VALUE))
                .passive(Passive.attr("tensura:max_magicule", 0.2, Operation.ADD_MULTIPLIED_TOTAL))
                .passive(Passive.skill("tensura:analytical_appraisal"))
                .act(Act.of("spell_card_leave", "Spell Card: Leave", ctx -> Wave31Acts.blink(ctx, 32.0)).cooldown(600).mana(4))
                .act(Act.of("spell_card_reflection", "Spell Card: Reflection", Wave71Acts.ward(100f, 10, "minecraft:resistance", 6, 1, 8.0, 1.0)).cooldown(2400).mana(10))
                .act(Act.of("spell_card_magnetic_force", "Spell Card: Magnetic Force", Wave71Acts.pullBind(18.0, 2.0, 100f, "minecraft:magic", "minecraft:slowness", 8, 3, VfxShape.ASH_FORMATION)).cooldown(3000).mana(10).needsSneak()));
    }

    /** Registers the game-bus listeners (the set, the second soul, the foresight, the marks, the weapon rules) and the self-test of this wave (called once from the mod constructor). */
    public static void hooks() {
        Wave101Events.register();
        Wave101SelfTest.register();
    }
}
