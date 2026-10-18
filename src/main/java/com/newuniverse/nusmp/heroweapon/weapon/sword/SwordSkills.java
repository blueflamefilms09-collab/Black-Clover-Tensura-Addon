package com.newuniverse.nusmp.heroweapon.weapon.sword;

import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.heroweapon.skill.SimpleSkill;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the list of the Legendary Sword's techniques, with their names and the one or two sentences the tooltip and the Weapon Book show, in one place. Each skill takes
 * its cooldown and cost from its row in {@link SwordNumbers} (so the budget test and the skill can never disagree) and its body from {@link SwordOps}. The order is
 * the order of the table: the ordinary tree first, then the Greed and Gluttony series.
 */
final class SwordSkills {
    private SwordSkills() {}

    private static SimpleSkill of(SwordNumbers.Spec s, String desc, SimpleSkill.Body body) {
        SimpleSkill k = SimpleSkill.of(s.id(), s.name(), desc, s.cooldown(), s.cost(), body);
        return s.kind() == SwordNumbers.Kind.ULTIMATE ? k.asUltimate() : k;
    }

    static List<HeroSkill> all() {
        List<HeroSkill> out = new ArrayList<>();
        out.add(of(SwordNumbers.BLITZ, "Blitz Strike: after a blink of warning (a line of blue dust) you dash up to 7 blocks and cut everything in the way for 8 damage. "
                + "The next blow within 3 seconds leaves a ghost strike.", SwordOps::blitz));
        out.add(of(SwordNumbers.TEMPEST, "Tempest Dance: two sweeps of the blade round you, 5 damage each to every foe within 4.5 blocks, shoving them back. A ring of dust marks the reach.",
                SwordOps::tempest));
        out.add(of(SwordNumbers.AFTERIMAGE, "Afterimage Step: you step 5 blocks the way you were moving (backwards if you stood still) and leave a double where you were. "
                + "Faster for 3 seconds, a little tougher for 1, and your next blow leaves a ghost strike.", SwordOps::afterimage));
        out.add(of(SwordNumbers.FLASHING, "Flashing Sword: the blade flashes in front of you. Foes in the cone take 4 damage and are lit up; mobs are blinded for 2 seconds "
                + "(players are only slowed for 1). You run faster for 3 seconds.", SwordOps::flashing));
        out.add(of(SwordNumbers.WIND_CUTTER, "Wind Cutter Whistle: three whistling crescents of wind fly out in a fan to 12 blocks, 4 damage each, through everything they meet.",
                SwordOps::windCutter));
        out.add(of(SwordNumbers.METEOR, "Meteor Sword: after a moment (dust marks the ring) four stars fall in a ring where you point, 3 damage each to foes near them.", SwordOps::meteor));
        out.add(of(SwordNumbers.ASSASSIN, "Assassin Sword: you blink behind the foe you look at (up to 12 blocks) and stab it for 8 damage, 12 if it was not facing you.", SwordOps::assassin));
        out.add(of(SwordNumbers.EAGLE_BLADE, "Eagle Blade: after a glint you lunge up to 5 blocks at the first foe in the line. The point goes through armour: 10 damage, up to 16 on heavy armour.",
                SwordOps::eagleBlade));
        out.add(of(SwordNumbers.GRAVITY, "Gravity Sword: the weight round the foe you look at grows for 3 pulses. Everything within 3.5 blocks of it is slowed, pulled to the ground and cut for 3 "
                + "damage a pulse. A boss is held for one second at most.", SwordOps::gravity));
        out.add(of(SwordNumbers.CLEAVE, "Skyfall Cleave: after a crouch (a ring of dust shows the landing) you leap 5 blocks and slam the blade down: 16 damage to everything within 3.5 blocks, "
                + "thrown up and back.", SwordOps::cleave));
        out.add(of(SwordNumbers.PHOENIX_GALE, "Phoenix Gale Blade: the sword gathers red energy for a moment, then a storm of flame burns the cone in front of you (9 damage) and a "
                + "bird of fire flies on (9 more). Both set the foe alight.", SwordOps::phoenixGale));
        out.add(of(SwordNumbers.FLOAT_BLADES, "Float Sword: four swords circle you for 8 seconds. Once a second each of them cuts a different foe within 3.5 blocks (3 damage, 12 at most on one foe) "
                + "and together they stop up to three shots.", SwordOps::floatBlades));
        out.add(of(SwordNumbers.HUNDRED_SWORDS, "Hundred Swords: the blade splits into a hundred. After a moment (a ring of dust) they rain on the circle where you point, 4 damage a "
                + "wave for four waves.", SwordOps::hundredSwords));
        out.add(of(SwordNumbers.LIGHTNING_HUNDRED, "Lightning Hundred Swords: a long gathering (a great ring of dust and a circle of light), then eight waves of lightning blades over the ground within 7 blocks "
                + "of where you point, 3.6 damage a wave.", SwordOps::lightningHundred));
        out.add(of(SwordNumbers.GUILLOTINE, "Guillotine: a column of gold dust marks the spot where the foe stands; a moment later a great blade falls there for 16 damage and takes a share "
                + "of the magicules of what it cuts. A foe that moves away is missed.", SwordOps::guillotine));
        out.add(of(SwordNumbers.GOLD_REBELLION, "Gold Rebellion: a ring of golden blades cuts every foe within 5 blocks for 10 damage and gilds them for 6 seconds; your next blow on a gilded foe takes more.",
                SwordOps::goldRebellion));
        out.add(of(SwordNumbers.MANEATER, "Maneater: after a moment (the cone is marked) a maw closes on up to three foes in front of you for 14 damage each, and feeds you 4 percent of your health for each.",
                SwordOps::maneater));
        out.add(of(SwordNumbers.STRONG_DECLINE, "Strong Decline: a line of dust shows a split in the ground; a moment later it opens 10 blocks ahead, fangs rise from it and cut for 16 damage "
                + "and slow what they catch.", SwordOps::strongDecline));
        out.add(of(SwordNumbers.INSATIABLE_MAW, "Insatiable Maw: after a long wind-up (a banquet field and a ring of dust where you stand) three great maws close over everything within 7 blocks of that ring, "
                + "9.6 damage each, and each wave that bites feeds you 6 percent of your health.", SwordOps::insatiableMaw));
        return out;
    }
}
