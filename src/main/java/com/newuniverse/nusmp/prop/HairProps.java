package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.book.ShakudoKit;
import com.newuniverse.nusmp.prop.ShakudoPropKit.*;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;

/**
 * 1.93.0 Hair Magic: the server behaviours of this magic's two constructs (the animation and the look are in client.prop.HairPropPainter; the
 * numbers and the shared behaviours are in {@link ShakudoPropKit}).
 * <ul>
 *   <li>HAIR_1, the tendril: a grasping lock of ten segments that rises under its target, lashes it after 10 ticks and holds it (the hold is shortened for bosses).</li>
 *   <li>HAIR_2, the cocoon: an egg of woven hair round the owner that stops projectiles, mends 1.5 hit points a second and opens in a burst of healing when it ends.</li>
 * </ul>
 * Also registers the armour's damage hook ({@link ShakudoKit#hooks}).
 */
public final class HairProps {
    private HairProps() {}

    public static final int COLOR = 0xFFFF6E9A;

    /** Called once by PropRegistry. */
    public static void init() {
        ShakudoKit.hooks();
        MagicProps.register(PropKind.HAIR_1, new ShakudoPropKit.Strike(new StrikeCfg("hair-tendril", 0.9f, 3.0f, 10, 1.3, 5f, TensuraDamageTypes.MAGIC_GENERIC, true, 50, 0, 0f, ShakudoPropKit.slow(60, 2), COLOR, VfxShape.HAIR_FX3)));
        MagicProps.register(PropKind.HAIR_2, new ShakudoPropKit.Guard(new GuardCfg("hair-cocoon", 1.5f, 2.3f, -1, false, 0f, 1.8, false, 1.5f, COLOR, VfxShape.HAIR_FX3)));
    }
}
