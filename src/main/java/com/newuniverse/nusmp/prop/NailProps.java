package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.book.ShakudoKit;
import com.newuniverse.nusmp.prop.ShakudoPropKit.*;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;

/**
 * 1.93.0 Nail Magic: the server behaviours of this magic's two constructs (the animation and the look are in client.prop.NailPropPainter; the
 * numbers and the shared behaviours are in {@link ShakudoPropKit}).
 * <ul>
 *   <li>NAIL_1, the bolt: a giant nail that flies straight, pins whatever it hits (Slowness) and stays driven into a wall.</li>
 *   <li>NAIL_2, the hammer: a spectral war hammer that comes down on its target after 14 ticks (a 2.2 block ring) and pins it to the ground (the hold is shortened for bosses).</li>
 * </ul>
 * Also registers the armour's damage hook ({@link ShakudoKit#hooks}).
 */
public final class NailProps {
    private NailProps() {}

    public static final int COLOR = 0xFFC7B9A6;

    /** Called once by PropRegistry. */
    public static void init() {
        ShakudoKit.hooks();
        MagicProps.register(PropKind.NAIL_1, new ShakudoPropKit.Missile(new MissileCfg("nail-bolt", 0.5f, 0.5f, 0.55, 6f, 0.0, TensuraDamageTypes.MAGIC_GENERIC, 0.0, true, ShakudoPropKit.slow(40, 2), COLOR, VfxShape.NAIL_FX3)));
        MagicProps.register(PropKind.NAIL_2, new ShakudoPropKit.Strike(new StrikeCfg("nail-hammer", 1.8f, 2.0f, 14, 2.2, 12f, TensuraDamageTypes.MAGIC_GENERIC, true, 36, 0, 0f, ShakudoPropKit.slow(50, 3), COLOR, VfxShape.NAIL_FX3)));
    }
}
