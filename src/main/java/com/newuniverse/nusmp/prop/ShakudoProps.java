package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.book.ShakudoKit;
import com.newuniverse.nusmp.prop.ShakudoPropKit.*;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;

/**
 * 1.93.0 Shakudo Magic: the server behaviours of this magic's two constructs (the animation and the look are in client.prop.ShakudoPropPainter; the
 * numbers and the shared behaviours are in {@link ShakudoPropKit}).
 * <ul>
 *   <li>SHAKUDO_1, the golem: a 2.3 block alloy construct that follows its owner, walks up to the nearest hostile within 12 blocks and brings both fists down (a 2 block ring, knock-back).</li>
 *   <li>SHAKUDO_2, the bulwark: a solid slab of alloy that stands where it was raised, turns away every projectile that flies at it and body-blocks; it can be broken.</li>
 * </ul>
 * Also registers the armour's damage hook ({@link ShakudoKit#hooks}).
 */
public final class ShakudoProps {
    private ShakudoProps() {}

    public static final int COLOR = 0xFFE6B450;

    /** Called once by PropRegistry. */
    public static void init() {
        ShakudoKit.hooks();
        com.newuniverse.nusmp.book.ShakudoSelfTests.register();       // the five self-tests magic.<key>_rebuild
        MagicProps.register(PropKind.SHAKUDO_1, new ShakudoPropKit.Companion(new CompanionCfg("shakudo-golem", 1.3f, 2.5f, 0.20, 2.6, 11, 24, 34, 9f, 2.0, TensuraDamageTypes.EARTH_ELEMENTAL, ShakudoPropKit.knock(0.55, 0.25), 26, COLOR, VfxShape.SHAKUDO_FX3)));
        MagicProps.register(PropKind.SHAKUDO_2, new ShakudoPropKit.Guard(new GuardCfg("shakudo-bulwark", 1.7f, 2.8f, 0, true, 60f, 2.4, false, 0f, COLOR, VfxShape.SHAKUDO_FX3)));
    }
}
