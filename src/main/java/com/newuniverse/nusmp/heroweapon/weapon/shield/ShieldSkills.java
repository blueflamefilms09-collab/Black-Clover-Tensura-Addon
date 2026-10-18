package com.newuniverse.nusmp.heroweapon.weapon.shield;

import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.heroweapon.skill.SimpleSkill;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the list of the Legendary Shield's techniques, with their names and the one or two sentences the tooltip and the Weapon Book show, in one place. Each skill
 * takes its cooldown and cost from its row in {@link ShieldNumbers} (so the budget test and the skill can never disagree) and its body from {@link ShieldOps}.
 * The order is the order of the table: the ordinary tree first, then the Wrath series.
 */
final class ShieldSkills {
    private ShieldSkills() {}

    private static SimpleSkill of(ShieldNumbers.Spec s, String desc, SimpleSkill.Body body) {
        SimpleSkill k = SimpleSkill.of(s.id(), s.name(), desc, s.cooldown(), s.cost(), body);
        return s.kind() == ShieldNumbers.Kind.ULTIMATE ? k.asUltimate() : k;
    }

    static List<HeroSkill> all() {
        List<HeroSkill> out = new ArrayList<>();
        out.add(of(ShieldNumbers.CHANGE_ATTACK, "The shield turns into its striking form: a cleaving blow in front of you after a short wind-up (green dust shows the cone). "
                + "It hits for 6, and for up to 12 with a full Counter, and spends all the Counter. Needs a foe in reach.", ShieldOps::changeAttack));
        out.add(of(ShieldNumbers.AIR_STRIKE, "A floating shield stands where you look (up to 12 blocks) for 8 seconds. It stops six shots from your enemies and nudges foes that "
                + "touch it away. Two can stand at once.", ShieldOps::airStrike));
        out.add(of(ShieldNumbers.SECOND_SHIELD, "A second shield circles the friend you look at, or the nearest friend, or you. It soaks the next blows they take: 15 percent "
                + "of their health (more as the bond grows, never above 30 points), for 10 seconds. You keep three of them at most.", ShieldOps::secondShield));
        out.add(of(ShieldNumbers.COVER, "Protect: for 8 seconds 40 percent of every blow your friends within 12 blocks take is taken by you instead, and charges your Counter. "
                + "It never takes you under a quarter of your health.", ShieldOps::cover));
        out.add(of(ShieldNumbers.PROVOKE, "Hostile mobs within 10 blocks (not bosses) turn on you for 4 seconds, and you take a fifth less damage while they do.", ShieldOps::provoke));
        out.add(of(ShieldNumbers.HERB_MEND, "The Leaf Shield's herbs: you and your friends within 5 blocks are mended by a tenth of their health (more as the bond grows), then regenerate for 5 seconds.",
                ShieldOps::herbMend));
        out.add(of(ShieldNumbers.PRISON, "A cage of shields shuts round the foe you look at (up to 14 blocks). It cannot leave the cage and is slowed, for up to 4 seconds "
                + "(a boss is held for one). Iron Maiden needs a foe held like this.", ShieldOps::prison));
        out.add(of(ShieldNumbers.CHAIN, "Chains bind up to four foes near where you look. For 3 seconds they are drawn together and bitten by the links, four pulses of 3 damage each.",
                ShieldOps::chain));
        out.add(of(ShieldNumbers.HOOK, "A hooked chain strikes the foe you look at for 4 damage and drags it to you. A boss is only staggered.", ShieldOps::hook));
        out.add(of(ShieldNumbers.IRON_MAIDEN, "An iron maiden closes on a foe held in your Shield Prison, after a short wind-up (a column of dust marks it). 16 damage, "
                + "up to 20 with a full Counter, which it spends.", ShieldOps::ironMaiden));
        out.add(of(ShieldNumbers.NEEDLE_BURST, "The Needle Shield's needles all at once: after a short gathering (lines of dust show the reach) every foe within 4.5 blocks takes 4 damage, up to 10 with a full Counter, and half of the Counter is spent.",
                ShieldOps::needleBurst));
        out.add(of(ShieldNumbers.SHOOTING_STAR, "After a flash of light at the shield (a fifth of a second) a comet of glass flies about 22 blocks and strikes the first foe for 9 damage and a shove.", ShieldOps::shootingStar));
        out.add(of(ShieldNumbers.SOUL_AEGIS, "The Soul Eater Shield gives the souls it has eaten (up to five, from your kills) as Absorption to you and the friends within 6 blocks "
                + "for 12 seconds.", ShieldOps::soulAegis));
        out.add(of(ShieldNumbers.UNDEAD_CONTROL, "The undead within 12 blocks lose their will to attack for 6 seconds, unless you strike them. A boss is not affected.",
                ShieldOps::undeadControl));
        out.add(of(ShieldNumbers.ABSOLUTE_DEFENCE, "Absolute Defence: for 5 seconds a dome of iron stands where you cast it. Friends inside take a great deal less damage (you most), foes "
                + "are pushed out and shots are stopped, but you move slowly.", ShieldOps::absoluteDefence));
        out.add(of(ShieldNumbers.RAGE_BASH, "The Shield of Rage lunges up to 4 blocks and slams, for 10 damage and a burn to everything near the point of impact.", ShieldOps::rageBash));
        out.add(of(ShieldNumbers.RAGE_ROAR, "A roar shoves back the foes in front of you (4 damage, slowed) and fills you with strength for 6 seconds.", ShieldOps::rageRoar));
        out.add(of(ShieldNumbers.BLOOD_SACRIFICE, "Blood Sacrifice: a ring of blood is drawn under the foe you look at; a second later a trap of blood closes on everything in the ring: 18 "
                + "damage to that foe, 8 to the others, and a short hold. You give a quarter of your health (never below 30 percent) only if it catches something, "
                + "and then you are weakened and slowed for 15 seconds.", ShieldOps::bloodSacrifice));
        out.add(of(ShieldNumbers.MEGIDDO, "Megiddo Burst: after a long wind-up (a growing ring of dust where you stand) you give 30 percent of your health (never below a quarter) and fire burns "
                + "everything within 9 blocks of that ring for 28 damage. You are left weakened.", ShieldOps::megiddo));
        return out;
    }
}
