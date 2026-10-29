package com.newuniverse.nusmp.arc.tier;

import java.util.List;

import static com.newuniverse.nusmp.arc.tier.TierMove.Effect.*;
import static com.newuniverse.nusmp.arc.tier.TierMove.Kind.*;
import static com.newuniverse.nusmp.arc.tier.TierMove.of;

/**
 * Why: the gag boss of rank F of the owner's new ladder (docs/bosses/roster.md, docs/bosses/light_yagami.md): Light Yagami of Death Note. He is NOT a fight: his one real act is the scripted
 * Death Note scene ({@link LightScene}); he has very little health (28), full knockback vulnerability and five token moves so the house rules ({@link TierRules}) still hold, all of them
 * tiny jokes (a pen, a potato chip, a notebook). His look is the owner's skin reference (messy brown hair, red Kira eyes, a khaki jacket over a white shirt, a red tie, a black pen on the sleeve).
 * Kept in its own file so the older rosters ({@link TierRosterF}, {@link TierRosterD}) are untouched; {@link TierRoster#all()} appends it last.
 */
public final class TierRosterGag {
    private TierRosterGag() {}

    /** The id of the gag boss (entity {@code nusmp:light_yagami}). */
    public static final String LIGHT = "light_yagami";

    public static List<TierRow> rows() {
        return List.of(
                TierRow.of(TierLetter.F, LIGHT, "Light Yagami", "Death Note").title("Kira, the God of the New World").colors(0x9B1B1F, 0xF4F4F2).floors(6, 18).height(1.85f).scale(0.04, 0.2)
                        .lines("Ah. A visitor. Fortunately, you are exactly where I need you.", "...", "This... was not in the plan...")
                        .move(of("pen_jab", "Pen Jab", CLEAVE).wind(16).cd(70).range(0, 6).weight(4).dmg(0.8).size(4.0).theme())
                        .move(of("chip_toss", "Potato Chip Toss", BOLT).wind(18).cd(90).range(4, 30).weight(4).dmg(0.7).theme())
                        .move(of("notebook_slam", "Notebook Slam", SLAM).wind(20).cd(110).range(0, 7).weight(3).dmg(0.8).size(3.5).knock(0.4).theme())
                        .move(of("kira_gaze", "Kira's Gaze", MARKED).power(30).wind(22).cd(130).range(5, 24).weight(3).dmg(0.8).size(2.4).fx(WEAKEN, 40).theme())
                        .move(of("ryuk_does_something", "Ryuk, Do Something", SWARM).wind(46).range(5, 30).count(2).size(2.4).dmg(0.8).crowd().ult().theme())
                        .build()
        );
    }
}
