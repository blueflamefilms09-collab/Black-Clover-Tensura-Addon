package com.newuniverse.nusmp.arc.tier;

import java.util.List;

import static com.newuniverse.nusmp.arc.tier.TierMove.Effect.*;
import static com.newuniverse.nusmp.arc.tier.TierMove.Kind.*;
import static com.newuniverse.nusmp.arc.tier.TierMove.of;

/**
 * Why: tier F ("Bad") of the owner's list, the weakest characters, who stand on the first floors of the Mega-Dungeon (docs/bosses/tier_f_d.md). Six light bosses. The
 * move names were written WITHOUT the wiki (it could not be reached): each move says in its {@code TierMove.Basis} whether the name is the series' (NAMED), a thing the
 * character really has or does (THEME) or an invented filler.
 */
public final class TierRosterF {
    private TierRosterF() {}

    public static List<TierRow> rows() {
        return List.of(
                TierRow.of(TierLetter.F, "charlos", "Charlos", "One Piece").title("the Celestial Dragon").colors(0xE0C050, 0xF4F0D8).floors(1, 9).height(1.8f).scale(0.9, 0.9)
                        .lines("Kneel, commoner. A Celestial Dragon does not wait.", "How dare you hurt me! Bring the Marines!", "Impossible... I am a World Noble...")
                        .move(of("pistol_shot", "Pistol Shot", BOLT).wind(18).cd(80).range(4, 32).weight(4).dmg(1.0).theme().magic())
                        .move(of("tantrum", "Spoiled Tantrum", SLAM).wind(20).cd(90).range(0, 6).weight(4).dmg(1.0).size(3.5).knock(0.6).filler())
                        .move(of("slave_collar", "Slave Collar Detonation", MARKED).power(30).wind(22).cd(140).range(6, 24).weight(3).dmg(1.1).size(2.6).fx(SLOW, 60).theme())
                        .move(of("noble_charge", "Noble's Charge", RUSH).power(40).wind(24).cd(160).range(4, 14).weight(2).dmg(1.1).size(10).knock(0.7).filler())
                        .move(of("call_marines", "Call the Marines", SWARM).wind(46).range(5, 30).count(4).size(2.4).dmg(1.2).crowd().ult().theme())
                        .build(),
                TierRow.of(TierLetter.F, "zetsu", "Zetsu", "Naruto").title("the Plant Clone").colors(0x2E7D32, 0xE8F5E9).floors(1, 10).height(1.9f).scale(0.95, 0.9)
                        .lines("Heh heh... we were here before you noticed.", "Enough hiding. The plan goes on!", "The moon... will still... rise...")
                        .move(of("flytrap_bite", "Flytrap Bite", CLEAVE).wind(16).cd(70).range(0, 6).weight(4).dmg(1.0).size(4.5).fx(WEAKEN, 60).theme())
                        .move(of("spore_cloud", "Spore Cloud", SLAM).wind(22).cd(110).range(0, 9).weight(3).dmg(0.7).size(4.5).fx(SLOW, 80).crowd().theme())
                        .move(of("earth_ambush", "Earth Ambush", MARKED).wind(20).cd(120).range(3, 24).weight(4).dmg(1.1).size(2.2).theme())
                        .move(of("clone_army", "White Zetsu Clones", SWARM).power(40).wind(26).cd(190).range(5, 30).count(3).size(2.2).dmg(0.9).crowd().theme())
                        .move(of("moon_plan", "Plan of the Moon's Eye", WAVE).wind(46).range(0, 20).size(12).dmg(1.3).fx(BLIND, 60).ult().theme())
                        .build(),
                TierRow.of(TierLetter.F, "yammy_llargo", "Yammy Llargo", "Bleach").title("the Zero Espada").colors(0x8D6E63, 0xFFCCBC).floors(3, 14).height(3.2f).scale(1.15, 1.0)
                        .lines("Out of my way, little things!", "Resurrección... Ira!", "I'm... the strongest...")
                        .move(of("heavy_fist", "Heavy Fist", SLAM).wind(18).cd(80).range(0, 7).weight(4).dmg(1.1).size(4.2).knock(0.8).filler())
                        .move(of("cero", "Cero", BOLT).wind(22).cd(110).range(5, 32).weight(4).dmg(1.2).named().magic())
                        .move(of("hierro_charge", "Hierro Charge", RUSH).power(30).wind(24).cd(150).range(4, 16).weight(3).dmg(1.2).size(10).knock(1.0).theme())
                        .move(of("ground_pound", "Ground Pound", WAVE).power(40).wind(26).cd(170).range(0, 12).weight(3).dmg(1.0).size(9).crowd().filler())
                        .move(of("ira_rampage", "Ira: Cero Rampage", VOLLEY).wind(48).range(6, 32).count(5).size(50).dmg(1.2).magic().ult().named())
                        .build(),
                TierRow.of(TierLetter.F, "sogou_nobuyuki", "Sogou Nobuyuki", "Sword Art Online").title("Oberon, the Fairy King").colors(0xFFD54F, 0xB3E5FC).floors(2, 10).height(1.85f).scale(0.95, 0.95)
                        .lines("This world is mine, and you are my toys.", "Administrator rights... all of them!", "No... a god cannot lose to a child...")
                        .move(of("kings_blade", "Fairy King's Blade", CLEAVE).wind(16).cd(70).range(0, 6).weight(4).dmg(1.0).size(4.8).theme())
                        .move(of("gravity_spell", "Gravity Spell", MARKED).power(30).wind(22).cd(130).range(4, 24).weight(3).dmg(0.9).size(3.0).fx(SLOW, 100).magic().theme())
                        .move(of("system_lock", "System Lock", BOLT).wind(20).cd(120).range(5, 30).weight(3).dmg(0.8).fx(SLOW, 80).magic().theme())
                        .move(of("wing_dash", "Wing Dash", RUSH).power(40).wind(22).cd(150).range(4, 14).weight(2).dmg(1.1).size(9).filler())
                        .move(of("gm_authority", "Game Master Authority", WAVE).wind(46).range(0, 20).size(11).dmg(1.2).fx(WEAKEN, 100).crowd().ult().theme())
                        .build(),
                TierRow.of(TierLetter.F, "mr_2_bon_clay", "Mr. 2 Bon Clay", "One Piece").title("Bentham").colors(0xF8BBD0, 0xFFFFFF).floors(1, 9).height(2.0f).scale(0.95, 0.9)
                        .lines("Don't you dare underestimate the Okama Way!", "Now, a performance only I can give!", "Goodbye... friend...")
                        .move(of("swan_kick", "Okama Kenpo: Swan Kick", RUSH).wind(20).cd(100).range(3, 21).weight(4).dmg(1.0).size(14).knock(0.7).theme())
                        .move(of("ballet_flurry", "Okama Kenpo: Ballet Flurry", CLEAVE).wind(16).cd(70).range(0, 6).weight(4).dmg(0.9).size(4.2).theme())
                        .move(of("mane_mane", "Mane Mane no Mi: Face Copy", SLAM).power(30).wind(22).cd(130).range(0, 8).weight(3).dmg(0.5).size(5).fx(BLIND, 50).named())
                        .move(of("swan_volley", "Swan Volley", VOLLEY).power(30).wind(24).cd(140).range(6, 28).count(3).size(36).weight(3).dmg(0.8).theme())
                        .move(of("okama_spectacle", "Okama Way Spectacle", WAVE).wind(46).range(0, 20).size(10).dmg(1.2).knock(1.0).crowd().ult().theme())
                        .build(),
                TierRow.of(TierLetter.F, "kurozumi_orochi", "Kurozumi Orochi", "One Piece").title("the Shogun of Wano").colors(0x7B1FA2, 0xE1BEE7).floors(4, 14).height(2.0f).scale(1.05, 1.0)
                        .lines("The Shogun's word is law. Kneel.", "Behold! The Eight-Headed Orochi!", "I am... the Shogun... of Wano...")
                        .move(of("shogun_blade", "Shogun's Blade", CLEAVE).wind(16).cd(70).range(0, 6).weight(4).dmg(1.0).size(4.8).filler())
                        .move(of("venom_mist", "Venom Mist", SLAM).wind(22).cd(110).range(0, 8).weight(3).dmg(0.7).size(4.5).fx(SLOW, 80).crowd().filler())
                        .move(of("eight_heads", "Eight Heads Strike", VOLLEY).power(30).wind(24).cd(150).range(6, 28).count(4).size(50).weight(3).dmg(0.9).theme())
                        .move(of("shogun_decree", "Shogun's Decree", MARKED).wind(22).cd(130).range(4, 24).weight(3).dmg(1.0).size(2.6).count(2).theme())
                        .move(of("eight_headed_orochi", "Eight-Headed Orochi", WAVE).wind(48).range(0, 20).size(12).dmg(1.4).knock(0.9).fx(WEAKEN, 80).ult().theme())
                        .build()
        );
    }
}
