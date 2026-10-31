package com.newuniverse.nusmp.arc.tier;

import java.util.List;

import static com.newuniverse.nusmp.arc.tier.TierMove.Effect.*;
import static com.newuniverse.nusmp.arc.tier.TierMove.Kind.*;
import static com.newuniverse.nusmp.arc.tier.TierMove.of;

/**
 * Why: tier D ("Below average") of the owner's list: ten light bosses for the first twenty floors, a step above tier F. Move names were written without the wiki; each
 * carries its {@code TierMove.Basis} (NAMED = the series' name from memory, THEME = something the character really has or does, FILLER = invented). Where I am not
 * sure who a character is or what they do (Rachel, Code, Ao) the kit is mostly FILLER and docs/bosses/tier_f_d.md says so.
 *
 * <p>3.39.0 (docs/bosses/tier_powers.md): every row also carries its anime POWER, one or two {@link TierTrait}s (Malty's guards and Karma, Rachel's climb, Jugram's Balance, Big Mom's soul marks
 * and fusion, Code's karma and sight, Bertholdt's steam and fusion, Morgan's shield break, Foxy's slow field, Tucker's fusion and spores, Ao's sight and system lock), and its moves are re-cut
 * to the new statuses (shield break). The numbers of the moves were retuned with {@link TierBalance} so that every boss of the tier lies inside the pressure band; nothing here is verified
 * against the wikis.
 */
public final class TierRosterD {
    private TierRosterD() {}

    public static List<TierRow> rows() {
        return List.of(
                TierRow.of(TierLetter.D, "malty_melromarc", "Malty S. Melromarc", "The Rising of the Shield Hero").title("the Princess").colors(0xC62828, 0xFFCDD2).floors(5, 16).height(1.7f).scale(0.95, 0.95)
                        .lines("You will be called a criminal, again.", "Guards! Guards! Kill him!", "No... it was all... your fault...")
                        .move(of("fire_bolt", "Fire Magic", BOLT).wind(18).cd(80).range(4, 32).weight(4).dmg(1.0).fx(BURN, 40).magic().theme())
                        .move(of("staff_strike", "Staff Strike", CLEAVE).wind(16).cd(70).range(0, 6).weight(3).dmg(0.9).size(4.0).filler())
                        .move(of("false_accusation", "False Accusation", SLAM).power(30).wind(22).cd(130).range(0, 9).weight(3).dmg(0.8).size(5.5).fx(WEAKEN, 80).crowd().theme())
                        .move(of("slave_crest", "Slave Crest Curse", MARKED).wind(44).range(5, 24).count(3).dmg(1.1).size(2.8).fx(SLOW, 80).crowd().ult().theme())
                        .move(of("royal_guard", "Call the Royal Guard", SWARM).power(40).wind(30).cd(300).range(5, 30).weight(3).count(3).size(2.4).dmg(1.0).crowd().theme())
                        .trait(TierTrait.UNTOUCHABLE, "royal_guard")
                        .trait(TierTrait.KARMA, "false_accusation")
                        .build(),
                TierRow.of(TierLetter.D, "rachel", "Rachel", "Tower of God").title("the Climber").colors(0x80DEEA, 0xFFFFFF).floors(7, 18).height(1.7f).scale(0.95, 1.0)
                        .lines("The stars were mine. I'll take them back.", "I'll climb over you!", "I only wanted... to reach it...")
                        .move(of("shinsu_bolt", "Shinsu Bolt", BOLT).wind(16).cd(80).range(4, 32).weight(4).dmg(0.9).magic().theme())
                        .move(of("betrayal_strike", "Betrayal Strike", CLEAVE).wind(16).cd(70).range(0, 6).weight(3).dmg(0.9).size(4.2).filler())
                        .move(of("falling_star", "Falling Star", MARKED).power(30).wind(22).cd(130).range(5, 24).weight(3).dmg(0.8).size(2.6).count(2).filler())
                        .move(of("star_volley", "Star Volley", VOLLEY).power(40).wind(24).cd(190).range(6, 28).count(3).size(36).weight(3).dmg(0.6).magic().filler())
                        .move(of("ascension", "Ascension", WAVE).wind(46).range(0, 20).size(11).dmg(1.3).fx(SLOW, 80).crowd().ult().filler())
                        .trait(TierTrait.CLIMB)
                        .build(),
                TierRow.of(TierLetter.D, "jugram_haschwalth", "Jugram Haschwalth", "Bleach").title("Stern Ritter B, the Balance").colors(0xECEFF1, 0x90CAF9).floors(8, 20).height(1.9f).scale(1.1, 1.05)
                        .lines("I serve His Majesty. That is all.", "The Balance tips against you.", "Your Majesty... forgive me...")
                        .move(of("heilig_pfeil", "Heilig Pfeil", BOLT).wind(16).cd(70).range(4, 32).weight(4).dmg(1.0).named().magic())
                        .move(of("quincy_blade", "Quincy Blade Slash", CLEAVE).wind(16).cd(70).range(0, 6).weight(3).dmg(1.0).size(4.5).theme())
                        .move(of("hirenkyaku", "Hirenkyaku Dash", RUSH).power(30).wind(20).cd(130).range(4, 16).weight(3).dmg(1.0).size(9).named())
                        .move(of("pfeil_barrage", "Heilig Pfeil Barrage", VOLLEY).power(40).wind(26).cd(180).range(6, 30).count(5).size(55).weight(3).dmg(0.6).magic().crowd().theme())
                        .move(of("the_balance", "The Balance", WAVE).wind(48).range(0, 20).size(12).dmg(1.3).fx(WEAKEN, 100).crowd().ult().named())
                        .trait(TierTrait.BALANCE, "the_balance")
                        .build(),
                TierRow.of(TierLetter.D, "big_mom", "\"Big Mom\" Charlotte Linlin", "One Piece").title("the Emperor of the Sea").colors(0xF06292, 0xFFE082).floors(10, 20).height(3.6f).scale(1.25, 1.1)
                        .lines("Mama Mama Mama! Hand over the sweets!", "Hungry... I'm so hungry!", "My... sweets... my family...")
                        .move(of("napoleon_slash", "Napoleon: Sword Slash", CLEAVE).wind(18).cd(110).range(0, 8).weight(4).dmg(1.0).size(6.5).knock(0.7).named())
                        .move(of("zeus_bolt", "Zeus: Thunderbolt", BOLT).wind(20).cd(130).range(5, 34).weight(4).dmg(1.05).named().magic())
                        .move(of("prometheus_blaze", "Prometheus: Sun Blaze", SLAM).power(30).wind(24).cd(170).range(0, 10).weight(3).dmg(1.0).size(6).fx(BURN, 60).crowd().named())
                        .move(of("hungry_stomp", "Hungry Stomp", WAVE).power(40).wind(26).cd(220).range(0, 12).weight(3).dmg(0.9).size(10).knock(0.9).filler())
                        .move(of("soul_pocus", "Soul Pocus: Homies", VOLLEY).wind(48).range(6, 34).count(4).size(50).dmg(0.95).magic().ult().named())
                        .trait(TierTrait.SOUL_MARK)
                        .trait(TierTrait.FUSION)
                        .build(),
                TierRow.of(TierLetter.D, "code", "Code", "Boruto").title("Kara's Inner").colors(0x455A64, 0x80DEEA).floors(8, 19).height(1.9f).scale(1.0, 1.0)
                        .lines("The Kara do not lose to you.", "Time to show the Karma's power.", "Kara... will go on...")
                        .move(of("karma_burst", "Karma Burst", BOLT).wind(18).cd(80).range(4, 32).weight(4).dmg(1.15).magic().filler())
                        .move(of("kara_blade", "Kara Blade", CLEAVE).wind(16).cd(70).range(0, 6).weight(3).dmg(1.1).size(4.5).filler())
                        .move(of("inner_strike", "Inner Strike", RUSH).power(30).wind(20).cd(130).range(4, 16).weight(3).dmg(1.1).size(9).filler())
                        .move(of("kara_seal", "Kara Seal", MARKED).power(30).wind(22).cd(140).range(5, 24).weight(3).dmg(1.1).size(2.6).fx(SLOW, 80).filler())
                        .move(of("karma_overdrive", "Karma Overdrive", WAVE).wind(46).range(0, 20).size(11).dmg(1.3).crowd().ult().filler())
                        .trait(TierTrait.KARMA, "karma_burst")
                        .trait(TierTrait.SIGHT)
                        .build(),
                TierRow.of(TierLetter.D, "bertholdt_hoover", "Bertholdt Hoover", "Attack on Titan").title("the Colossal Titan").colors(0xBF360C, 0xFFCCBC).floors(10, 20).height(3.4f).scale(1.2, 1.05)
                        .lines("I'm sorry. I have to do this.", "Then I'll become the Colossal Titan.", "Reiner... I'm... sorry...")
                        .move(of("titan_swipe", "Titan Swipe", CLEAVE).wind(18).cd(80).range(0, 8).weight(3).dmg(1.0).size(6).knock(0.7).theme())
                        .move(of("colossal_stomp", "Colossal Stomp", SLAM).wind(20).cd(100).range(0, 8).weight(4).dmg(1.0).size(5.5).knock(0.8).named())
                        .move(of("wall_fragments", "Wall Fragments", VOLLEY).wind(24).cd(170).range(6, 30).count(3).size(40).weight(3).dmg(0.6).crowd().theme())
                        .move(of("steam_burst", "Steam Burst", WAVE).power(40).wind(26).cd(190).range(0, 12).weight(3).dmg(1.0).size(9).fx(BURN, 80).crowd().named())
                        .move(of("colossal_blast", "Colossal Transformation Blast", WAVE).wind(48).range(0, 20).size(13).dmg(1.4).knock(1.1).ult().named())
                        .trait(TierTrait.STEAM)
                        .trait(TierTrait.FUSION)
                        .build(),
                TierRow.of(TierLetter.D, "axe_hand_morgan", "Morgan", "One Piece").title("Axe-Hand Morgan").colors(0x78909C, 0xFFE0B2).floors(5, 16).height(2.6f).scale(1.0, 1.0)
                        .lines("Nobody defies Captain Morgan!", "You'll regret this, on my axe!", "A captain... falls...")
                        .move(of("axe_hand_swing", "Axe Hand Swing", CLEAVE).wind(16).cd(70).range(0, 7).weight(4).dmg(1.1).size(5.5).fx(SHIELD_BREAK, 100).named())
                        .move(of("iron_jaw", "Iron Jaw Strike", CLEAVE).wind(18).cd(80).range(0, 5).weight(2).dmg(1.0).size(3.5).fx(WEAKEN, 40).theme())
                        .move(of("axe_throw", "Axe Throw", BOLT).wind(20).cd(100).range(5, 30).weight(3).dmg(1.0).filler())
                        .move(of("tyrants_decree", "Tyrant's Decree", MARKED).power(30).wind(22).cd(130).range(4, 24).weight(3).dmg(1.0).size(2.6).filler())
                        .move(of("axe_whirl", "Axe-Hand Whirl", SLAM).wind(44).range(0, 10).size(6.5).dmg(1.5).knock(1.0).ult().theme())
                        .trait(TierTrait.SHIELD_BREAK)
                        .trait(TierTrait.CLIMB)
                        .build(),
                TierRow.of(TierLetter.D, "foxy", "Foxy", "One Piece").title("Silver Fox").colors(0xB0BEC5, 0xFF8A65).floors(5, 16).height(1.9f).scale(0.95, 0.95)
                        .lines("Foxy's gonna win this Davy Back Fight!", "Noro Noro Beam, on everyone!", "I'll... get you... next time...")
                        .move(of("noro_beam", "Noro Noro Beam", BOLT).wind(18).cd(90).range(5, 32).weight(4).dmg(0.7).fx(SLOW, 120).named().magic())
                        .move(of("silver_fox_dash", "Silver Fox Dash", RUSH).power(30).wind(20).cd(130).range(4, 16).weight(3).dmg(1.0).size(9).filler())
                        .move(of("foxy_punch", "Foxy Punch", CLEAVE).wind(16).cd(70).range(0, 6).weight(3).dmg(1.0).size(4.2).filler())
                        .move(of("slow_field", "Slow-Slow Field", SLAM).power(30).wind(22).cd(130).range(0, 9).weight(3).dmg(0.6).size(5.5).fx(SLOW, 100).crowd().theme())
                        .move(of("noro_volley", "Noro Noro Beam: Total Slow", VOLLEY).wind(46).range(6, 32).count(5).size(60).dmg(0.8).fx(SLOW, 100).magic().ult().named())
                        .trait(TierTrait.SLOW_FIELD)
                        .trait(TierTrait.CLIMB)
                        .build(),
                TierRow.of(TierLetter.D, "shou_tucker", "Shou Tucker", "Fullmetal Alchemist").title("the Sewing-Life Alchemist").colors(0x8D6E63, 0xC5E1A5).floors(7, 18).height(1.85f).scale(0.95, 1.0)
                        .lines("A chimera is only a matter of the right circle.", "Alexander, to me!", "Nina... I only wanted... to keep my post...")
                        .move(of("chimera_lash", "Chimera Lash", CLEAVE).wind(16).cd(70).range(0, 6).weight(3).dmg(0.9).size(4.5).theme())
                        .move(of("alchemic_spike", "Alchemic Spike", MARKED).wind(20).cd(130).range(4, 24).weight(4).dmg(0.8).size(2.4).count(2).theme())
                        .move(of("alexander", "Alexander the Chimera", SWARM).power(40).wind(26).cd(190).range(5, 30).count(2).size(2.4).dmg(1.0).theme())
                        .move(of("transmutation_circle", "Transmutation Circle", SLAM).power(30).wind(22).cd(130).range(0, 9).weight(3).dmg(0.8).size(5.5).fx(WEAKEN, 80).crowd().theme())
                        .move(of("chimera_rampage", "Chimera Rampage", WAVE).wind(48).range(0, 20).size(11).dmg(1.4).knock(0.9).ult().theme())
                        .trait(TierTrait.FUSION)
                        .trait(TierTrait.SPORE_POISON)
                        .build(),
                TierRow.of(TierLetter.D, "ao", "Ao", "Boruto").title("the Byakugan Seer").colors(0x4FC3F7, 0xFFFFFF).floors(10, 19).height(1.85f).scale(1.0, 1.0)
                        .lines("My eyes see every move you will make.", "Byakugan... open!", "Seen... and still... defeated...")
                        .move(of("byakugan_strike", "Byakugan Strike", MARKED).wind(20).cd(120).range(3, 24).weight(4).dmg(1.3).size(2.4).theme())
                        .move(of("palm_strike", "Palm Strike", CLEAVE).wind(16).cd(70).range(0, 6).weight(3).dmg(1.1).size(4.2).filler())
                        .move(of("air_bolt", "Air Bolt", BOLT).wind(18).cd(80).range(4, 32).weight(3).dmg(1.0).magic().filler())
                        .move(of("spinning_guard", "Spinning Guard", SLAM).power(30).wind(20).cd(130).range(0, 8).weight(3).dmg(1.0).size(5).knock(1.0).filler())
                        .move(of("palm_barrage", "Palm Barrage", WAVE).wind(46).range(0, 20).size(10).dmg(1.5).knock(0.9).ult().filler())
                        .trait(TierTrait.SIGHT)
                        .trait(TierTrait.SYSTEM_LOCK)
                        .build()
        );
    }
}
